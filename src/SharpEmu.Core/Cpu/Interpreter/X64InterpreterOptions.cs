// Copyright (C) 2026 SharpEmu Emulator Project
// SPDX-License-Identifier: GPL-2.0-or-later

namespace SharpEmu.Core.Cpu.Interpreter;

public readonly struct X64InterpreterOptions
{
    public bool Trace { get; init; }

    public int MaxInstructions { get; init; }
}
