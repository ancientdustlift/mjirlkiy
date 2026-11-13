// Compares the original capture loop with the reworked one on the same machine.
//
// Build (MSVC):  cl /O2 /EHsc /std:c++17 capture_bench.cpp
//                (d3d11.lib and dxgi.lib are linked by the pragmas below; the linker looks
//                for them in the folder you build from and then in the SDK paths)
// Build (MinGW): g++ -O2 -std=c++17 capture_bench.cpp -o capture_bench.exe -ld3d11 -ldxgi -static
// Run:           capture_bench.exe [seconds=10] [region=640] [rounds=2]
//
// Desktop duplication only delivers a frame when the screen changes, so run it while the
// content you care about is animating on the primary monitor.
// Results are printed and appended to capture_bench_results.txt next to the exe.
#define _CRT_SECURE_NO_WARNINGS
#include <windows.h>
#include <d3d11.h>
#include <dxgi1_6.h>
#include <wrl/client.h>
#include <algorithm>
#include <cstdint>
#include <cstdarg>
#include <cstdio>
#include <cstdlib>
#include <cwchar>
#include <vector>

#ifdef _MSC_VER
#pragma comment(lib, "d3d11.lib")
#pragma comment(lib, "dxgi.lib")
#endif

using Microsoft::WRL::ComPtr;

static FILE* gResults = nullptr;

// Prints to the console and to the results file.
static void Log(const char* format, ...)
{
    va_list args;
    va_start(args, format);
    std::vprintf(format, args);
    va_end(args);
    std::fflush(stdout);
    if (!gResults) return;
    va_start(args, format);
    std::vfprintf(gResults, format, args);
    va_end(args);
    std::fflush(gResults);
}

static void OpenResultsFile()
{
    static const wchar_t kName[] = L"capture_bench_results.txt";
    wchar_t path[MAX_PATH + _countof(kName)]{};
    const DWORD length = GetModuleFileNameW(nullptr, path, MAX_PATH);
    if (!length || length >= MAX_PATH) return;
    wchar_t* slash = std::wcsrchr(path, L'\\');
    if (!slash) return;
    std::wcscpy(slash + 1, kName);
    gResults = _wfopen(path, L"a");
    if (!gResults) return;
    SYSTEMTIME now{}; GetLocalTime(&now);
    std::fprintf(gResults, "\n=== %04u-%02u-%02u %02u:%02u:%02u ===\n",
        unsigned(now.wYear), unsigned(now.wMonth), unsigned(now.wDay),
        unsigned(now.wHour), unsigned(now.wMinute), unsigned(now.wSecond));
}

static double NowMs()
{
    static const double toMs = [] {
        LARGE_INTEGER f{}; QueryPerformanceFrequency(&f); return 1000.0 / double(f.QuadPart);
    }();
    LARGE_INTEGER c{}; QueryPerformanceCounter(&c);
    return double(c.QuadPart) * toMs;
}

static double ThreadCpuMs()
{
    FILETIME created{}, exited{}, kernel{}, user{};
    GetThreadTimes(GetCurrentThread(), &created, &exited, &kernel, &user);
    const auto ticks = [](const FILETIME& t) {
        return (uint64_t(t.dwHighDateTime) << 32) | t.dwLowDateTime;
    };
    return double(ticks(kernel) + ticks(user)) / 10000.0;
}

struct Stats
{
    uint64_t frames = 0, timeouts = 0, missed = 0, lost = 0;
    double workMs = 0, wallMs = 0, cpuMs = 0;
};

struct Capture
{
    bool reworked = false;
    ComPtr<ID3D11Device> dev;
    ComPtr<ID3D11DeviceContext> ctx;
    ComPtr<IDXGIOutput> output;
    ComPtr<IDXGIOutputDuplication> dupl;
    ComPtr<ID3D11Texture2D> dst;
    UINT deskW = 0, deskH = 0, size = 0;
    LONG left = 0, top = 0;
    bool held = false, needsFullCopy = true;
    std::vector<uint8_t> meta;

    bool CreateDevice()
    {
        ComPtr<IDXGIAdapter1> adapter;
        if (reworked)
        {
            ComPtr<IDXGIFactory1> factory;
            if (FAILED(CreateDXGIFactory1(IID_PPV_ARGS(&factory)))) return false;
            const HMONITOR primary = MonitorFromPoint(POINT{ 0, 0 }, MONITOR_DEFAULTTOPRIMARY);
            bool found = false;
            for (UINT a = 0; !found && factory->EnumAdapters1(a, &adapter) != DXGI_ERROR_NOT_FOUND; ++a)
                for (UINT o = 0; !found && adapter->EnumOutputs(o, &output) != DXGI_ERROR_NOT_FOUND; ++o)
                {
                    DXGI_OUTPUT_DESC d{};
                    found = SUCCEEDED(output->GetDesc(&d)) && d.AttachedToDesktop && d.Monitor == primary;
                }
            if (!found) return false;
        }
        const D3D_FEATURE_LEVEL levels[] = { D3D_FEATURE_LEVEL_11_1, D3D_FEATURE_LEVEL_11_0 };
        if (FAILED(D3D11CreateDevice(adapter.Get(),
            adapter ? D3D_DRIVER_TYPE_UNKNOWN : D3D_DRIVER_TYPE_HARDWARE, nullptr,
            D3D11_CREATE_DEVICE_BGRA_SUPPORT, levels, 2, D3D11_SDK_VERSION, &dev, nullptr, &ctx)))
            return false;
        ComPtr<IDXGIDevice> dxgiDev;
        if (FAILED(dev.As(&dxgiDev))) return false;
        dxgiDev->SetGPUThreadPriority(7);
        if (!reworked)
        {
            ComPtr<IDXGIAdapter> def;
            if (FAILED(dxgiDev->GetAdapter(&def)) || FAILED(def->EnumOutputs(0, &output))) return false;
        }
        return true;
    }

    bool StartDuplication()
    {
        if (held && dupl) dupl->ReleaseFrame();
        held = false;
        dupl.Reset();
        HRESULT hr = E_NOINTERFACE;
        ComPtr<IDXGIOutput5> out5;
        if (reworked && SUCCEEDED(output.As(&out5)))
        {
            const DXGI_FORMAT format = DXGI_FORMAT_B8G8R8A8_UNORM;
            hr = out5->DuplicateOutput1(dev.Get(), 0, 1, &format, &dupl);
        }
        if (FAILED(hr))
        {
            ComPtr<IDXGIOutput1> out1;
            if (FAILED(output.As(&out1))) return false;
            hr = out1->DuplicateOutput(dev.Get(), &dupl);
        }
        if (FAILED(hr)) return false;

        DXGI_OUTDUPL_DESC d{}; dupl->GetDesc(&d);
        deskW = d.ModeDesc.Width; deskH = d.ModeDesc.Height;
        size = std::min({ size, deskW, deskH });
        left = LONG(deskW - size) / 2; top = LONG(deskH - size) / 2;

        D3D11_TEXTURE2D_DESC td{};
        td.Width = td.Height = size; td.MipLevels = td.ArraySize = 1;
        td.Format = d.ModeDesc.Format; td.SampleDesc = { 1, 0 };
        td.Usage = D3D11_USAGE_DEFAULT; td.BindFlags = D3D11_BIND_SHADER_RESOURCE;
        dst.Reset();
        needsFullCopy = true;
        return SUCCEEDED(dev->CreateTexture2D(&td, nullptr, &dst));
    }

    void CopyFull(ID3D11Texture2D* src)
    {
        const D3D11_BOX box{ UINT(left), UINT(top), 0, UINT(left) + size, UINT(top) + size, 1 };
        ctx->CopySubresourceRegion(dst.Get(), 0, 0, 0, 0, src, 0, &box);
    }

    // The original dirty-rectangle path.
    void CopyDirty(const DXGI_OUTDUPL_FRAME_INFO& info, ID3D11Texture2D* src)
    {
        const UINT metaSize = info.TotalMetadataBufferSize;
        bool full = metaSize == 0;
        if (!full)
        {
            if (meta.size() < metaSize) meta.resize(metaSize);
            UINT mb = 0, db = 0;
            if (FAILED(dupl->GetFrameMoveRects(metaSize,
                reinterpret_cast<DXGI_OUTDUPL_MOVE_RECT*>(meta.data()), &mb)) || mb > 0) full = true;
            else if (FAILED(dupl->GetFrameDirtyRects(metaSize,
                reinterpret_cast<RECT*>(meta.data()), &db))) full = true;
            else
            {
                const RECT* rects = reinterpret_cast<const RECT*>(meta.data());
                const LONG right = left + LONG(size), bottom = top + LONG(size);
                for (UINT i = 0; i < db / sizeof(RECT); ++i)
                {
                    const LONG l = std::max(rects[i].left, left), t = std::max(rects[i].top, top);
                    const LONG r = std::min(rects[i].right, right), b = std::min(rects[i].bottom, bottom);
                    if (r <= l || b <= t) continue;
                    const D3D11_BOX box{ UINT(l), UINT(t), 0, UINT(r), UINT(b), 1 };
                    ctx->CopySubresourceRegion(dst.Get(), 0, UINT(l - left), UINT(t - top), 0, src, 0, &box);
                }
            }
        }
        if (full) CopyFull(src);
    }

    Stats Run(double seconds)
    {
        Stats s;
        const UINT waitMs = reworked ? 8u : 1u;
        const double start = NowMs(), cpuStart = ThreadCpuMs();
        while (NowMs() - start < seconds * 1000.0)
        {
            if (reworked && held) { dupl->ReleaseFrame(); held = false; }
            ComPtr<IDXGIResource> res; DXGI_OUTDUPL_FRAME_INFO info{};
            const HRESULT hr = dupl->AcquireNextFrame(waitMs, &info, &res);
            if (hr == DXGI_ERROR_WAIT_TIMEOUT) { ++s.timeouts; continue; }
            if (FAILED(hr))
            {
                ++s.lost;
                while (!StartDuplication()) Sleep(50);
                continue;
            }
            held = true;
            const double workStart = NowMs();
            ComPtr<ID3D11Texture2D> src;
            if (info.LastPresentTime.QuadPart != 0 && SUCCEEDED(res.As(&src)))
            {
                ++s.frames;
                if (info.AccumulatedFrames > 1) s.missed += info.AccumulatedFrames - 1;
                if (reworked || needsFullCopy) { CopyFull(src.Get()); needsFullCopy = false; }
                else CopyDirty(info, src.Get());
                ctx->Flush();
            }
            if (!reworked) { dupl->ReleaseFrame(); held = false; }
            s.workMs += NowMs() - workStart;
        }
        s.wallMs = NowMs() - start;
        s.cpuMs = ThreadCpuMs() - cpuStart;
        return s;
    }
};

static void Print(const char* name, const Stats& s)
{
    Log("%-9s %8.1f fps  %7llu frames  %6llu missed  %7llu timeouts  %3llu lost  "
        "%7.4f ms/frame  %5.1f%% cpu\n", name,
        double(s.frames) * 1000.0 / s.wallMs, (unsigned long long)s.frames,
        (unsigned long long)s.missed, (unsigned long long)s.timeouts, (unsigned long long)s.lost,
        s.frames ? s.workMs / double(s.frames) : 0.0, 100.0 * s.cpuMs / s.wallMs);
}

int main(int argc, char** argv)
{
    const double seconds = argc > 1 ? std::atof(argv[1]) : 10.0;
    const UINT region = argc > 2 ? UINT(std::atoi(argv[2])) : 640u;
    const int rounds = argc > 3 ? std::atoi(argv[3]) : 2;
    SetProcessDpiAwarenessContext(DPI_AWARENESS_CONTEXT_PER_MONITOR_AWARE_V2);
    OpenResultsFile();

    DEVMODEW mode{}; mode.dmSize = sizeof(mode);
    if (EnumDisplaySettingsW(nullptr, ENUM_CURRENT_SETTINGS, &mode))
        Log("primary display: %lux%lu @ %lu Hz, %g s per run, %ux%u region\n",
            mode.dmPelsWidth, mode.dmPelsHeight, mode.dmDisplayFrequency, seconds, region, region);

    // Alternate the two loops so drift in what is on screen affects both equally.
    for (int round = 0; round < rounds; ++round)
        for (const bool reworked : { false, true })
        {
            Capture c;
            c.reworked = reworked;
            c.size = region;
            if (!c.CreateDevice() || !c.StartDuplication())
            {
                Log("%s: setup failed\n", reworked ? "reworked" : "original");
                return 1;
            }
            Print(reworked ? "reworked" : "original", c.Run(seconds));
        }
    return 0;
}
