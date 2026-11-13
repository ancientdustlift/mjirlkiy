// Extracted from WindowsProject1.cpp. Include this file inside the original App
// class body after its capture-related types and shared services are declared.
// This is not standalone: it uses the app's D3D11 device/context, capture region,
// preprocessor callbacks, logging/error helpers, frame counters, and optional
// WGC wrapper. Keep AIM_ENABLE_WGC consistent with the original project.

    void CreateDevice()
    {
        UINT flags = D3D11_CREATE_DEVICE_BGRA_SUPPORT;
#ifdef _DEBUG
        flags |= D3D11_CREATE_DEVICE_DEBUG;
#endif
        D3D_FEATURE_LEVEL fls[] = { D3D_FEATURE_LEVEL_11_1,D3D_FEATURE_LEVEL_11_0 };
        HR(D3D11CreateDevice(nullptr, D3D_DRIVER_TYPE_HARDWARE, nullptr,
            flags, fls, _countof(fls), D3D11_SDK_VERSION, &mDev, nullptr, &mCtx),
            "D3D11CreateDevice");

        // Pri zero-copy vkhode CUDA obrashchaetsya k resursam D3D11 iz potoka
        // detektora, poetomu device dolzhen byt potokobezopasnym.
        ComPtr<ID3D10Multithread> mt;
        if (SUCCEEDED(mDev.As(&mt))) mt->SetMultithreadProtected(TRUE);

        ComPtr<IDXGIDevice> dxgiDev;
        HR(mDev.As(&dxgiDev), "QI IDXGIDevice");
        dxgiDev->SetGPUThreadPriority(7);

        ComPtr<IDXGIAdapter> adapter;
        HR(dxgiDev->GetAdapter(&adapter), "GetAdapter");
        DXGI_ADAPTER_DESC desc{};
        if (SUCCEEDED(adapter->GetDesc(&desc))) {
            char identity[200]{};
            std::snprintf(identity, sizeof(identity),
                "capture vendor=0x%04X device=0x%04X LUID=%08X:%08X",
                unsigned(desc.VendorId), unsigned(desc.DeviceId),
                unsigned(desc.AdapterLuid.HighPart), unsigned(desc.AdapterLuid.LowPart));
            mCaptureDeviceDetails = WideToUtf8(desc.Description) + "; " + identity;
        }
        ComPtr<IDXGIOutput> out;
        HR(adapter->EnumOutputs(0, &out), "EnumOutputs");
        HR(out.As(&mOutput1), "QI IDXGIOutput1");
    }

    void EnsureCaptureTexture()
    {
        const UINT width = mCaptureRegion.width;
        const UINT height = mCaptureRegion.height;
        if (!width || !height) return;
        if (mFrameTex && mFrameSRV && mFrameW == width && mFrameH == height) return;

        mFrameSRV.Reset();
        mFrameTex.Reset();

        D3D11_TEXTURE2D_DESC td{};
        td.Width = width; td.Height = height; td.MipLevels = td.ArraySize = 1;
        td.Format = DXGI_FORMAT_B8G8R8A8_UNORM; td.SampleDesc = { 1,0 };
        td.Usage = D3D11_USAGE_DEFAULT; td.BindFlags = D3D11_BIND_SHADER_RESOURCE;
        HR(mDev->CreateTexture2D(&td, nullptr, &mFrameTex), "Create capture texture");
        HR(mDev->CreateShaderResourceView(mFrameTex.Get(), nullptr, &mFrameSRV),
            "Create capture SRV");
        mFrameW = width;
        mFrameH = height;
        mFrameNeedsFullCopy = true;
    }

    void StartDuplication()
    {
        mDesktopSrvs.Reset();
        mDupl.Reset();
        HRESULT hr = mOutput1->DuplicateOutput(mDev.Get(), &mDupl);
        if (hr == E_ACCESSDENIED) { Sleep(50); return; }
        HR(hr, "DuplicateOutput");

        DXGI_OUTDUPL_DESC d{}; mDupl->GetDesc(&d);
        if (d.ModeDesc.Width != mDeskW || d.ModeDesc.Height != mDeskH)
        {
            mDeskW = d.ModeDesc.Width; mDeskH = d.ModeDesc.Height;
            mFrameSRV.Reset(); mFrameTex.Reset();
            mFrameW = mFrameH = 0;
        }
        // Staraya tekstura bolshe nedeystvitelna - sbrasyvaem keshirovannyy SRV.
        mDirectCapture = false;
        // A same-resolution duplication restart is still a new image history.
        // Dirty rectangles cannot repair a stale ROI without a fresh base copy.
        mFrameNeedsFullCopy = true;
        mDirectSrvSupported = true;
        mCaptureRegion = MakeCenteredRegion(mDeskW, mDeskH, mAim.Config().radius);
        EnsureCaptureTexture();
    }

    UINT CaptureTimeoutMs() const
    {
        if (mCaptureMethod == CaptureMethod::DirectXFull) return mCapFailures > 0 ? 5u : 1u;
        return 1u;
    }

    ID3D11ShaderResourceView* DesktopSrv(ID3D11Texture2D* src)
    {
        return mDesktopSrvs.Get(mDev.Get(), src, mDirectSrvSupported);
    }

#ifdef AIM_ENABLE_WGC
    bool CaptureWgcFrame()
    {
        const double begin=NowMs();
        // Leave the newest frame in the mailbox while all processing slots are
        // occupied. The producer can replace it; no stale active frame is reused.
        if(mDetectEnabled && mDet.Ready() && !HasPreprocessCapacity()) {
            mCaptureMs.store(NowMs()-begin,std::memory_order_relaxed);return false;
        }
        auto frame=mWgc.Take();
        if(!frame){mCaptureMs.store(NowMs()-begin,std::memory_order_relaxed);return false;}
        ++mCapturedFrameId;mCapturedFrameMs=frame->receivedMs;mPresentedFrameMs=frame->presentedMs;
        auto* src=frame->texture.Get();
        auto* direct=DesktopSrv(src);
        if(direct) {
            mDirectCapture=true;
            if(mDetectEnabled && mDet.Ready() && WantsPreprocess())
                SubmitPreprocess(direct,mDeskW,mDeskH,mCaptureRegion,mCaptureRegion.left,mCaptureRegion.top);
        } else {
            mDirectCapture=false;EnsureCaptureTexture();CopyRegionFull(src);mFrameNeedsFullCopy=false;
        }
        // The owning WGC frame remains alive until all texture operations are issued.
        mCaptureMs.store(NowMs()-begin,std::memory_order_relaxed);return true;
    }

#endif

    bool CaptureFrame(UINT ms)
    {
#ifdef AIM_ENABLE_WGC
        if(mUseWgc)return CaptureWgcFrame();
#endif
        const double captureStart = NowMs();
        auto finishCapture = [this, captureStart]() {
            mCaptureMs.store(NowMs() - captureStart, std::memory_order_relaxed);
            };
        if (!mDupl) { StartDuplication(); finishCapture(); return false; }
        ComPtr<IDXGIResource> res; DXGI_OUTDUPL_FRAME_INFO info{};
        HRESULT hr = mDupl->AcquireNextFrame(ms, &info, &res);
        if (hr == DXGI_ERROR_WAIT_TIMEOUT)
        {
            mDxgiTimeouts.fetch_add(1, std::memory_order_relaxed);
            finishCapture();
            return false;
        }
        if (hr == DXGI_ERROR_ACCESS_LOST || hr == DXGI_ERROR_INVALID_CALL)
        {
            // Aimmy posle 5 podryad sboev peresozdaet duplikaciyu - delaem tak zhe.
            if (++mCapFailures >= kMaxCaptureFailures) mCapFailures = 0;
            StartDuplication();
            finishCapture();
            return false;
        }
        mCapFailures = 0;
        HR(hr, "AcquireNextFrame");
        // Exceptions from resource/shader work must not leave an acquired
        // duplication frame locked and break all subsequent acquisitions.
        struct FrameLease {
            IDXGIOutputDuplication* duplication;
            void Release() noexcept {
                if (duplication) { duplication->ReleaseFrame(); duplication = nullptr; }
            }
            ~FrameLease() { Release(); }
        } frameLease{ mDupl.Get() };
        bool upd = false;
        if (info.LastPresentTime.QuadPart != 0)
        {
            ComPtr<ID3D11Texture2D> src;
            if (SUCCEEDED(res.As(&src)))
            {
                ++mCapturedFrameId;
                // Timestamp when this process obtained the desktop frame.
                // Display/scanout latency before acquisition is not included.
                mCapturedFrameMs = NowMs();
                static const double qpcToMs=[] {
                    LARGE_INTEGER f{};QueryPerformanceFrequency(&f);
                    return f.QuadPart>0 ? 1000.0/double(f.QuadPart) : 0.0;
                }();
                mPresentedFrameMs=double(info.LastPresentTime.QuadPart)*qpcToMs;
                ID3D11ShaderResourceView* direct = DesktopSrv(src.Get());
                if (direct)
                {
                    // Zero-copy: shader sam vyrezaet oblast FOV iz kadra.
                    // Delaem eto do ReleaseFrame - posle nego tekstura nevalidna.
                    mDirectCapture = true;
                    if (mDetectEnabled && mDet.Ready() && WantsPreprocess())
                    {
                        SubmitPreprocess(direct, mDeskW, mDeskH, mCaptureRegion,
                            mCaptureRegion.left, mCaptureRegion.top);
                    }
                }
                else
                {
                    mDirectCapture = false;
                    EnsureCaptureTexture();
                    if (mCaptureMethod == CaptureMethod::DirectXFull || mFrameNeedsFullCopy)
                    {
                        CopyRegionFull(src.Get());
                        mFrameNeedsFullCopy = false;
                    }
                    else
                    {
                        CopyDirty(info, src.Get());
                    }
                }
                upd = true;
            }
        }
        frameLease.Release();
        finishCapture();
        return upd;
    }

    void CopyDirty(const DXGI_OUTDUPL_FRAME_INFO& info, ID3D11Texture2D* src)
    {
        UINT metaSize = info.TotalMetadataBufferSize;
        bool full = (metaSize == 0);
        static std::vector<uint8_t> meta;
        if (!full)
        {
            if (meta.size() < metaSize) meta.resize(metaSize);
            UINT mb = 0, db = 0;
            if (FAILED(mDupl->GetFrameMoveRects(metaSize,
                reinterpret_cast<DXGI_OUTDUPL_MOVE_RECT*>(meta.data()), &mb))) full = true;
            if (!full && mb > 0) full = true;
            if (!full)
            {
                if (FAILED(mDupl->GetFrameDirtyRects(metaSize,
                    reinterpret_cast<RECT*>(meta.data()), &db))) full = true;
                else
                {
                    const RECT* rects = reinterpret_cast<const RECT*>(meta.data());
                    for (UINT i = 0; i < db / sizeof(RECT); ++i)
                    {
                        // Kopiruem tolko peresechenie s oblastyu zakhvata.
                        const LONG cl = mCaptureRegion.left;
                        const LONG ct = mCaptureRegion.top;
                        const LONG cr = cl + LONG(mCaptureRegion.width);
                        const LONG cb = ct + LONG(mCaptureRegion.height);
                        const LONG l = std::max<LONG>(rects[i].left, cl);
                        const LONG t = std::max<LONG>(rects[i].top, ct);
                        const LONG r = std::min<LONG>(rects[i].right, cr);
                        const LONG b = std::min<LONG>(rects[i].bottom, cb);
                        if (r <= l || b <= t) continue;
                        D3D11_BOX box{ UINT(l),UINT(t),0,UINT(r),UINT(b),1 };
                        mCtx->CopySubresourceRegion(mFrameTex.Get(), 0,
                            UINT(l - cl), UINT(t - ct), 0, src, 0, &box);
                    }
                }
            }
        }
        if (full) CopyRegionFull(src);
    }

    void CopyRegionFull(ID3D11Texture2D* src)
    {
        D3D11_BOX box{ UINT(std::max<LONG>(mCaptureRegion.left, 0)),
                       UINT(std::max<LONG>(mCaptureRegion.top, 0)), 0,
                       UINT(std::min<LONG>(mCaptureRegion.left + LONG(mCaptureRegion.width), LONG(mDeskW))),
                       UINT(std::min<LONG>(mCaptureRegion.top + LONG(mCaptureRegion.height), LONG(mDeskH))), 1 };
        if (box.right > box.left && box.bottom > box.top)
            mCtx->CopySubresourceRegion(mFrameTex.Get(), 0, 0, 0, 0, src, 0, &box);
    }
