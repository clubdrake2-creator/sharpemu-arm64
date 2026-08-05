// Copyright (C) 2026 SharpEmu Emulator Project
// SPDX-License-Identifier: GPL-2.0-or-later

namespace SharpEmu.HLE;

public interface ICpuMemory
{
    bool TryRead(ulong virtualAddress, Span<byte> destination);

    bool TryWrite(ulong virtualAddress, ReadOnlySpan<byte> source);

    bool TryCompare(ulong virtualAddress, ReadOnlySpan<byte> expected) => false;

    bool TryCopy(ulong destinationAddress, ulong sourceAddress, ulong length) => false;

    /// <summary>
    /// Monotonically increasing stamp that changes whenever any guest mapping anywhere is
    /// created, removed, or reprotected -- <b>not</b> on ordinary reads/writes to already-mapped
    /// memory, which are far too frequent to track here. A caller that has confirmed
    /// <see cref="TryIsRegionNonWritable"/> for some address may keep trusting bytes it already
    /// read from that address for as long as this stamp stays equal to the value observed at that
    /// time -- a non-writable page's content cannot change except via a structural remap, and any
    /// such remap bumps this stamp. Default 0 (paired with <see cref="TryIsRegionNonWritable"/>
    /// always returning false) for implementations that don't track this, so the pairing is
    /// inert rather than wrong when unsupported.
    /// </summary>
    long MappingGeneration => 0;

    /// <summary>
    /// Reports whether <paramref name="address"/> currently falls in a region ordinary guest
    /// stores cannot write to (so self-modifying code is architecturally impossible there) --
    /// see <see cref="MappingGeneration"/> for how a caller combines this with the generation
    /// stamp to safely skip re-reading/re-validating that address later.
    /// </summary>
    bool TryIsRegionNonWritable(ulong address) => false;
}
