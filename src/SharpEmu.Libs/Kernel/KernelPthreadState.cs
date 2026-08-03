// Copyright (C) 2026 SharpEmu Emulator Project
// SPDX-License-Identifier: GPL-2.0-or-later

using System.Collections.Concurrent;
using System.Runtime.InteropServices;
using System.Threading;
using SharpEmu.HLE;

namespace SharpEmu.Libs.Kernel;

internal static class KernelPthreadState
{
    private const int ThreadObjectSize = 0x1000;

    private static readonly ConcurrentDictionary<ulong, ThreadIdentity> Threads = new();
    private static readonly byte[] ZeroThreadObject = new byte[ThreadObjectSize];
    private static long _nextUniqueThreadId = 1;

    [ThreadStatic]
    private static ulong _currentThreadHandle;

    [ThreadStatic]
    private static ulong _currentThreadUniqueId;

    internal readonly record struct ThreadIdentity(ulong UniqueId, string Name);

    internal static ulong GetCurrentThreadHandle()
    {
        var guestThreadHandle = GuestThreadExecution.CurrentGuestThreadHandle;
        // Prefer the bound guest handle even when it is not yet in Threads.
        // Falling through to a synthetic ThreadStatic handle while a guest
        // thread is bound causes mutex owner mismatches (unlock PERM → hang).
        if (guestThreadHandle != 0)
        {
            EnsureGuestThreadIdentity(guestThreadHandle);
            return guestThreadHandle;
        }

        EnsureCurrentThreadRegistered();
        return _currentThreadHandle;
    }

    internal static ulong GetCurrentThreadUniqueId()
    {
        var guestThreadHandle = GuestThreadExecution.CurrentGuestThreadHandle;
        if (guestThreadHandle != 0)
        {
            return EnsureGuestThreadIdentity(guestThreadHandle).UniqueId;
        }

        EnsureCurrentThreadRegistered();
        return _currentThreadUniqueId;
    }

    internal static string DescribeThreadHandle(ulong threadHandle)
    {
        if (threadHandle == 0)
        {
            return "none";
        }

        return TryGetThreadIdentity(threadHandle, out var identity)
            ? $"0x{threadHandle:X16}('{identity.Name}')"
            : $"0x{threadHandle:X16}";
    }

    internal static ulong CreateThreadHandle(CpuContext ctx, string name)
    {
        var uniqueId = unchecked((ulong)Interlocked.Increment(ref _nextUniqueThreadId));
        return AllocateThreadHandle(ctx, uniqueId, name);
    }

    internal static bool TryGetThreadIdentity(ulong threadHandle, out ThreadIdentity identity)
    {
        return Threads.TryGetValue(threadHandle, out identity);
    }

    internal static bool TryGetCurrentThreadIdentity(
        out ulong threadHandle,
        out ThreadIdentity identity)
    {
        threadHandle = GuestThreadExecution.CurrentGuestThreadHandle;
        if (threadHandle != 0 && TryGetThreadIdentity(threadHandle, out identity))
        {
            return true;
        }

        threadHandle = _currentThreadHandle;
        if (threadHandle != 0 && TryGetThreadIdentity(threadHandle, out identity))
        {
            return true;
        }

        identity = default;
        return false;
    }

    private static ThreadIdentity EnsureGuestThreadIdentity(ulong guestThreadHandle)
    {
        if (Threads.TryGetValue(guestThreadHandle, out var existing))
        {
            return existing;
        }

        var uniqueId = unchecked((ulong)Interlocked.Increment(ref _nextUniqueThreadId));
        var identity = new ThreadIdentity(uniqueId, $"Guest-0x{guestThreadHandle:X}");
        return Threads.GetOrAdd(guestThreadHandle, identity);
    }

    private static void EnsureCurrentThreadRegistered()
    {
        if (_currentThreadHandle != 0)
        {
            return;
        }

        var uniqueId = unchecked((ulong)Interlocked.Increment(ref _nextUniqueThreadId));
        var name = $"Thread-{uniqueId:X}";
        // No CpuContext available here — this path assigns a synthetic identity to whatever host
        // thread is currently running SharpEmu code without a real bound guest thread (e.g. an
        // internal utility thread), not a real scePthreadCreate-spawned guest thread. See
        // AllocateThreadHandle's comment for why that distinction matters on Android.
        _currentThreadHandle = AllocateThreadHandle(ctx: null, uniqueId, name);
        _currentThreadUniqueId = uniqueId;
    }

    private static ulong AllocateThreadHandle(CpuContext? ctx, ulong uniqueId, string name)
    {
        // Android: route through SharpEmu's own tracked guest-memory allocator instead of
        // Marshal.AllocHGlobal, same architectural fix as KernelMemoryCompatExports.
        // TryAllocateLibcHeapCore — confirmed on-device that a spawned guest thread
        // ("spi_main_thread") faulted reading its own thread-handle object because
        // Marshal.AllocHGlobal memory is never registered in PhysicalVirtualMemory's tracked
        // region list, which the interpreter's guest-memory access path requires. Only takes this
        // path when a CpuContext is available (real scePthreadCreate calls, which are the ones that
        // hand this address to guest code); the synthetic host-thread-identity fallback in
        // EnsureCurrentThreadRegistered has no CpuContext and keeps the previous
        // Marshal.AllocHGlobal + TBI-tag-mask-only behavior.
        if (ctx is not null && OperatingSystem.IsAndroid())
        {
            if (ctx.Memory is IGuestMemoryAllocator androidAllocator &&
                androidAllocator.TryAllocateGuestMemory(ThreadObjectSize, alignment: 0x10, out var guestHandle))
            {
                ctx.Memory.TryWrite(guestHandle, ZeroThreadObject);
                Threads[guestHandle] = new ThreadIdentity(uniqueId, string.IsNullOrWhiteSpace(name) ? $"Thread-{uniqueId:X}" : name);
                return guestHandle;
            }
        }

        var pointer = Marshal.AllocHGlobal(ThreadObjectSize);
        Marshal.Copy(ZeroThreadObject, 0, pointer, ThreadObjectSize);

        // See KernelVirtualRangeAllocator.NormalizeGuestVisibleHostPointer: on Android this pointer
        // can carry Scudo's ARM64 TBI region tag, which every guest-visible use of this handle must
        // have stripped to avoid tripping SharpEmu's own canonical-address/region-tracking checks.
        var handle = KernelVirtualRangeAllocator.NormalizeGuestVisibleHostPointer(pointer);
        Threads[handle] = new ThreadIdentity(uniqueId, string.IsNullOrWhiteSpace(name) ? $"Thread-{uniqueId:X}" : name);

        return handle;
    }
}
