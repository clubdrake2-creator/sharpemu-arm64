// Copyright (C) 2026 SharpEmu Emulator Project
// SPDX-License-Identifier: GPL-2.0-or-later

using SharpEmu.HLE;
using System;
using System.Diagnostics.CodeAnalysis;

namespace SharpEmu.Libs.Kernel;

internal static class KernelVirtualRangeAllocator
{
    /// <summary>
    /// Strips the ARM64 Top-Byte-Ignore (TBI) tag from a host-heap pointer before it becomes
    /// guest-visible. On Android, <c>Marshal.AllocHGlobal</c> goes through Mono's native allocator,
    /// which calls Bionic's malloc (Scudo) — Scudo's Secondary allocator embeds a per-process region
    /// tag in bits 56-63 of large allocations, a real (TBI-legal) pointer the ARM64 MMU dereferences
    /// identically with or without that byte set, but one every plain <c>ulong</c> comparison in this
    /// codebase (canonical-address checks, guest region lookups) treats as an astronomically invalid
    /// address. Confirmed on-device: a "malloc"-equivalent import handed the guest a raw tagged
    /// pointer (0xB4......), which the memset compat shim's canonical check rejected, and which the
    /// interpreter's own tracked-region lookup then hard-faulted on since it was never registered as
    /// a mapped guest region. Masking here — not at the call sites — keeps every guest-visible copy
    /// of the pointer (the value returned to the guest, and any dictionary key derived from it)
    /// consistent, while callers that must still pass the ORIGINAL tagged pointer to
    /// Marshal.FreeHGlobal (Scudo can require its own tag back for the free to succeed) keep doing so
    /// separately from whatever they hand the guest.
    /// </summary>
    internal static ulong NormalizeGuestVisibleHostPointer(nint pointer)
    {
        var address = unchecked((ulong)pointer);
        return OperatingSystem.IsAndroid() ? address & 0x00FF_FFFF_FFFF_FFFFUL : address;
    }

    internal static ulong NormalizeGuestVisibleHostPointer(ulong address) =>
        OperatingSystem.IsAndroid() ? address & 0x00FF_FFFF_FFFF_FFFFUL : address;

    public static bool TryReserve(
        CpuContext ctx,
        ulong desiredAddress,
        ulong length,
        bool executable,
        ulong alignment,
        bool allowSearch,
        bool allowAllocateAtAlternative,
        string traceName,
        out ulong mappedAddress,
        bool backPartialOverlap = false)
    {
        mappedAddress = 0;
        if (length == 0)
        {
            return false;
        }

        try
        {
            if (!TryResolveAddressSpace(ctx.Memory, out var addressSpace))
            {
                Console.Error.WriteLine($"[LOADER][TRACE] {traceName}: AllocateAt missing on {ctx.Memory.GetType().FullName}");
                return false;
            }

            if (allowSearch &&
                addressSpace.TryAllocateAtOrAbove(desiredAddress, length, executable, alignment, out var searchedAddress) &&
                searchedAddress != 0)
            {
                mappedAddress = searchedAddress;
                return true;
            }

            // Fixed mappings must cover the whole requested window even when part of
            // it is already backed by another allocation. The single-call AllocateAt
            // below is all-or-nothing and fails outright on partial overlap, leaving
            // the untouched pages unmapped for the guest to fault into. Fill the free
            // pages directly instead.
            if (backPartialOverlap &&
                addressSpace.TryBackFixedRange(desiredAddress, length, executable))
            {
                mappedAddress = desiredAddress;
                return true;
            }

            var allocated = addressSpace.AllocateAt(desiredAddress, length, executable, allowAllocateAtAlternative);
            if (allocated == 0)
            {
                Console.Error.WriteLine($"[LOADER][TRACE] {traceName}: AllocateAt returned {typeof(ulong).FullName} value=0");
                return false;
            }

            mappedAddress = allocated;
            return true;
        }
        catch
        {
            // Expected when a fixed-address request cannot be satisfied on
            // this host; the caller falls back or reports the failure.
            Console.Error.WriteLine(
                $"[LOADER][TRACE] {traceName}: no host mapping at 0x{desiredAddress:X16} len=0x{length:X}");
            return false;
        }
    }

    /// <summary>
    /// Finds the <see cref="IGuestAddressSpace"/> behind <paramref name="rootMemory"/>,
    /// unwrapping decorators (bounded, like the reflection walker this replaced).
    /// </summary>
    public static bool TryResolveAddressSpace(ICpuMemory rootMemory, [NotNullWhen(true)] out IGuestAddressSpace? addressSpace)
    {
        var target = rootMemory;
        for (var depth = 0; depth < 4; depth++)
        {
            if (target is IGuestAddressSpace resolved)
            {
                addressSpace = resolved;
                return true;
            }

            if (target is not ICpuMemoryWrapper wrapper)
            {
                break;
            }

            var inner = wrapper.Inner;
            if (inner is null || ReferenceEquals(inner, target))
            {
                break;
            }

            target = inner;
        }

        addressSpace = null;
        return false;
    }
}
