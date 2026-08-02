// Copyright (C) 2026 SharpEmu Emulator Project
// SPDX-License-Identifier: GPL-2.0-or-later

using SharpEmu.HLE;

namespace SharpEmu.Core.Cpu.Interpreter;

public readonly struct X64InterpreterResult
{
    public X64InterpreterResult(
        OrbisGen2Result result,
        CpuExitReason reason,
        ulong lastGuestRip,
        int totalInstructions,
        int importsHit,
        int uniqueNidsHit,
        CpuTrapInfo? trapInfo,
        CpuMemoryFaultInfo? memoryFaultInfo,
        CpuNotImplementedInfo? notImplementedInfo,
        string? trace,
        string? recentInstructions)
    {
        Result = result;
        Reason = reason;
        LastGuestRip = lastGuestRip;
        TotalInstructions = totalInstructions;
        ImportsHit = importsHit;
        UniqueNidsHit = uniqueNidsHit;
        TrapInfo = trapInfo;
        MemoryFaultInfo = memoryFaultInfo;
        NotImplementedInfo = notImplementedInfo;
        Trace = trace;
        RecentInstructions = recentInstructions;
    }

    public OrbisGen2Result Result { get; }

    public CpuExitReason Reason { get; }

    public ulong LastGuestRip { get; }

    public int TotalInstructions { get; }

    public int ImportsHit { get; }

    public int UniqueNidsHit { get; }

    public CpuTrapInfo? TrapInfo { get; }

    public CpuMemoryFaultInfo? MemoryFaultInfo { get; }

    public CpuNotImplementedInfo? NotImplementedInfo { get; }

    public string? Trace { get; }

    public string? RecentInstructions { get; }
}
