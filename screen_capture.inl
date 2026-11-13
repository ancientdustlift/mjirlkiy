    // Class members this file needs in addition to the existing ones:
    //   ComPtr<IDXGIOutput5> mOutput5;
    //   DXGI_FORMAT mDuplFormat = DXGI_FORMAT_B8G8R8A8_UNORM;
    //   bool mFrameHeld = false;
    //   std::atomic<uint64_t> mDxgiMissedFrames{ 0 };
    // No longer used here and safe to delete: mFrameNeedsFullCopy.

    // Wait for the next desktop frame. AcquireNextFrame returns as soon as a frame exists,
    // so a longer wait costs no latency; it only removes idle wakeups.
    static constexpr UINT kCaptureWaitMs = 8;

    // Take the desktop in its native HDR format (FP16 scRGB) instead of letting Windows
    // convert each frame to BGRA8. Enable only if preprocessing tone-maps scRGB input.
    static constexpr bool kCaptureHdrNative = false;

    // Duplication only works on the adapter that owns the output, and creating the device
    // there keeps the frame from crossing adapters on hybrid-GPU machines.
    void FindPrimaryOutput(ComPtr<IDXGIAdapter1>& adapter, ComPtr<IDXGIOutput>& output)
    {
        ComPtr<IDXGIFactory1> factory;
        HR(CreateDXGIFactory1(IID_PPV_ARGS(&factory)), "CreateDXGIFactory1");
        const HMONITOR primary = MonitorFromPoint(POINT{ 0, 0 }, MONITOR_DEFAULTTOPRIMARY);
        for (UINT a = 0; factory->EnumAdapters1(a, &adapter) != DXGI_ERROR_NOT_FOUND; ++a)
        {
            for (UINT o = 0; adapter->EnumOutputs(o, &output) != DXGI_ERROR_NOT_FOUND; ++o)
            {
                DXGI_OUTPUT_DESC desc{};
                if (SUCCEEDED(output->GetDesc(&desc)) && desc.AttachedToDesktop
                    && desc.Monitor == primary) return;
            }
        }
        adapter.Reset();
        output.Reset();
        HR(DXGI_ERROR_NOT_FOUND, "Find primary output");
    }

    void CreateDevice()
    {
        ComPtr<IDXGIAdapter1> adapter;
        ComPtr<IDXGIOutput> output;
        FindPrimaryOutput(adapter, output);

        UINT flags = D3D11_CREATE_DEVICE_BGRA_SUPPORT;
#ifdef _DEBUG
        flags |= D3D11_CREATE_DEVICE_DEBUG;
#endif
        const D3D_FEATURE_LEVEL levels[] = { D3D_FEATURE_LEVEL_11_1, D3D_FEATURE_LEVEL_11_0 };
        HR(D3D11CreateDevice(adapter.Get(), D3D_DRIVER_TYPE_UNKNOWN, nullptr,
            flags, levels, _countof(levels), D3D11_SDK_VERSION, &mDev, nullptr, &mCtx),
            "D3D11CreateDevice");

        ComPtr<ID3D10Multithread> mt;
        if (SUCCEEDED(mDev.As(&mt))) mt->SetMultithreadProtected(TRUE);

        ComPtr<IDXGIDevice> dxgiDev;
        HR(mDev.As(&dxgiDev), "QI IDXGIDevice");
        dxgiDev->SetGPUThreadPriority(7);

        DXGI_ADAPTER_DESC1 desc{};
        if (SUCCEEDED(adapter->GetDesc1(&desc)))
        {
            char identity[200]{};
            std::snprintf(identity, sizeof(identity),
                "capture vendor=0x%04X device=0x%04X LUID=%08X:%08X",
                unsigned(desc.VendorId), unsigned(desc.DeviceId),
                unsigned(desc.AdapterLuid.HighPart), unsigned(desc.AdapterLuid.LowPart));
            mCaptureDeviceDetails = WideToUtf8(desc.Description) + "; " + identity;
        }

        HR(output.As(&mOutput1), "QI IDXGIOutput1");
        output.As(&mOutput5);   // optional, Windows 10 1803+
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
        td.Format = mDuplFormat; td.SampleDesc = { 1, 0 };
        td.Usage = D3D11_USAGE_DEFAULT; td.BindFlags = D3D11_BIND_SHADER_RESOURCE;
        HR(mDev->CreateTexture2D(&td, nullptr, &mFrameTex), "Create capture texture");
        HR(mDev->CreateShaderResourceView(mFrameTex.Get(), nullptr, &mFrameSRV),
            "Create capture SRV");
        mFrameW = width;
        mFrameH = height;
    }

    HRESULT CreateDuplication()
    {
        if (mOutput5)
        {
            // DuplicateOutput1 needs per-monitor-v2 DPI awareness; fall back if it refuses.
            const DXGI_FORMAT formats[] = { DXGI_FORMAT_B8G8R8A8_UNORM,
                                            DXGI_FORMAT_R16G16B16A16_FLOAT };
            const HRESULT hr = mOutput5->DuplicateOutput1(mDev.Get(), 0,
                kCaptureHdrNative ? 2u : 1u, formats, &mDupl);
            if (SUCCEEDED(hr) || hr == E_ACCESSDENIED) return hr;
        }
        return mOutput1->DuplicateOutput(mDev.Get(), &mDupl);
    }

    void ReleaseHeldFrame()
    {
        if (mFrameHeld && mDupl) mDupl->ReleaseFrame();
        mFrameHeld = false;
    }

    void StartDuplication()
    {
        ReleaseHeldFrame();
        mDesktopSrvs.Reset();
        mDupl.Reset();
        const HRESULT hr = CreateDuplication();
        if (hr == E_ACCESSDENIED) { Sleep(50); return; }
        HR(hr, "DuplicateOutput");

        DXGI_OUTDUPL_DESC d{}; mDupl->GetDesc(&d);
        if (d.ModeDesc.Width != mDeskW || d.ModeDesc.Height != mDeskH
            || d.ModeDesc.Format != mDuplFormat)
        {
            mDeskW = d.ModeDesc.Width; mDeskH = d.ModeDesc.Height;
            mDuplFormat = d.ModeDesc.Format;
            mFrameSRV.Reset(); mFrameTex.Reset();
            mFrameW = mFrameH = 0;
        }

        mDirectCapture = false;
        mDirectSrvSupported = true;
        mCaptureRegion = MakeCenteredRegion(mDeskW, mDeskH, mAim.Config().radius);
        EnsureCaptureTexture();
    }

    UINT CaptureTimeoutMs() const { return kCaptureWaitMs; }

    ID3D11ShaderResourceView* DesktopSrv(ID3D11Texture2D* src)
    {
        return mDesktopSrvs.Get(mDev.Get(), src, mDirectSrvSupported);
    }

    // Samples the desktop texture in place when it can be bound as a shader resource,
    // otherwise copies the capture region into mFrameTex.
    void ConsumeFrame(ID3D11Texture2D* src)
    {
        if (ID3D11ShaderResourceView* direct = DesktopSrv(src))
        {
            mDirectCapture = true;
            if (mDetectEnabled && mDet.Ready() && WantsPreprocess())
                SubmitPreprocess(direct, mDeskW, mDeskH, mCaptureRegion,
                    mCaptureRegion.left, mCaptureRegion.top);
            return;
        }
        mDirectCapture = false;
        EnsureCaptureTexture();
        CopyRegion(src);
    }

#ifdef AIM_ENABLE_WGC
    bool CaptureWgcFrame()
    {
        // Leave the frame in the pool while preprocessing has no room for it.
        if (mDetectEnabled && mDet.Ready() && !HasPreprocessCapacity()) return false;
        auto frame = mWgc.Take();
        if (!frame) return false;
        ++mCapturedFrameId;
        mCapturedFrameMs = frame->receivedMs;
        mPresentedFrameMs = frame->presentedMs;
        ConsumeFrame(frame->texture.Get());
        return true;
    }
#endif

    bool CaptureDxgiFrame(UINT ms)
    {
        if (!mDupl) { StartDuplication(); return false; }

        // The previous frame stays held until here, so GPU work that samples the desktop
        // texture directly never races the next desktop update.
        ReleaseHeldFrame();
        ComPtr<IDXGIResource> res; DXGI_OUTDUPL_FRAME_INFO info{};
        const HRESULT hr = mDupl->AcquireNextFrame(ms, &info, &res);
        if (hr == DXGI_ERROR_WAIT_TIMEOUT)
        {
            mDxgiTimeouts.fetch_add(1, std::memory_order_relaxed);
            return false;
        }
        if (hr == DXGI_ERROR_ACCESS_LOST || hr == DXGI_ERROR_INVALID_CALL)
        {
            if (++mCapFailures >= kMaxCaptureFailures) mCapFailures = 0;
            StartDuplication();
            return false;
        }
        mCapFailures = 0;
        HR(hr, "AcquireNextFrame");
        mFrameHeld = true;

        if (info.LastPresentTime.QuadPart == 0) return false;   // pointer-only update
        if (info.AccumulatedFrames > 1)
            mDxgiMissedFrames.fetch_add(info.AccumulatedFrames - 1, std::memory_order_relaxed);

        ComPtr<ID3D11Texture2D> src;
        if (FAILED(res.As(&src))) return false;

        static const double qpcToMs = [] {
            LARGE_INTEGER f{}; QueryPerformanceFrequency(&f);
            return f.QuadPart > 0 ? 1000.0 / double(f.QuadPart) : 0.0;
        }();
        ++mCapturedFrameId;
        mCapturedFrameMs = NowMs();
        mPresentedFrameMs = double(info.LastPresentTime.QuadPart) * qpcToMs;
        ConsumeFrame(src.Get());
        return true;
    }

    bool CaptureFrame(UINT ms)
    {
        const double begin = NowMs();
#ifdef AIM_ENABLE_WGC
        const bool updated = mUseWgc ? CaptureWgcFrame() : CaptureDxgiFrame(ms);
#else
        const bool updated = CaptureDxgiFrame(ms);
#endif
        mCaptureMs.store(NowMs() - begin, std::memory_order_relaxed);
        return updated;
    }

    void CopyRegion(ID3D11Texture2D* src)
    {
        const LONG left = std::max<LONG>(mCaptureRegion.left, 0);
        const LONG top = std::max<LONG>(mCaptureRegion.top, 0);
        const LONG right = std::min<LONG>(mCaptureRegion.left + LONG(mCaptureRegion.width), LONG(mDeskW));
        const LONG bottom = std::min<LONG>(mCaptureRegion.top + LONG(mCaptureRegion.height), LONG(mDeskH));
        if (right <= left || bottom <= top) return;
        const D3D11_BOX box{ UINT(left), UINT(top), 0, UINT(right), UINT(bottom), 1 };
        mCtx->CopySubresourceRegion(mFrameTex.Get(), 0,
            UINT(left - mCaptureRegion.left), UINT(top - mCaptureRegion.top), 0, src, 0, &box);
    }
