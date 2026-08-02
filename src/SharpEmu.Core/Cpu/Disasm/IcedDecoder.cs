// Copyright (C) 2026 SharpEmu Emulator Project
// SPDX-License-Identifier: GPL-2.0-or-later

using Iced.Intel;
using SharpEmu.Core.Memory;
using SharpEmu.HLE;

namespace SharpEmu.Core.Cpu.Disasm;

public static class IcedDecoder
{
    private const int MaxInstructionBytes = 15;

    public static bool TryDecode(ulong rip, ReadOnlySpan<byte> codeBytes, out DecodedInst inst)
    {
        if (codeBytes.IsEmpty)
        {
            inst = default;
            return false;
        }

        var decodeLength = Math.Min(MaxInstructionBytes, codeBytes.Length);
        var decodeBytes = GC.AllocateUninitializedArray<byte>(decodeLength);
        codeBytes[..decodeLength].CopyTo(decodeBytes);

        try
        {
            var decoder = Decoder.Create(64, new ByteArrayCodeReader(decodeBytes));
            decoder.IP = rip;
            decoder.Decode(out var instruction);
            if (instruction.Code == Code.INVALID || instruction.Length <= 0)
            {
                inst = default;
                return false;
            }

            var formatter = new IntelFormatter();
            var output = new StringOutput();
            formatter.Format(instruction, output);

            var effectiveLength = Math.Min(instruction.Length, decodeBytes.Length);
            var effectiveBytes = new byte[effectiveLength];
            Array.Copy(decodeBytes, 0, effectiveBytes, 0, effectiveLength);

            inst = new DecodedInst(
                rip,
                instruction.Length,
                output.ToString(),
                instruction.Mnemonic.ToString(),
                instruction.FlowControl,
                GetNearBranchTarget(in instruction),
                GetMemoryAddress(in instruction),
                effectiveBytes);
            return true;
        }
        catch
        {
            inst = default;
            return false;
        }
    }

    public static bool TryReadGuestBytes(ICpuMemory memory, ulong rip, int maxLen, out byte[] bytes)
    {
        ArgumentNullException.ThrowIfNull(memory);
        var clampedLength = Math.Clamp(maxLen, 1, MaxInstructionBytes);

        // A single batched read covers the common case (the whole prefix is
        // mapped) in one call to the underlying memory subsystem instead of
        // one call per byte — this runs on every decoded instruction, so the
        // per-call region-lookup/locking overhead previously multiplied by
        // up to MaxInstructionBytes on every single instruction executed.
        var buffer = new byte[clampedLength];
        for (var length = clampedLength; length > 0; length--)
        {
            if (!memory.TryRead(rip, buffer.AsSpan(0, length)))
            {
                continue;
            }

            if (length == clampedLength)
            {
                bytes = buffer;
                return true;
            }

            bytes = new byte[length];
            Array.Copy(buffer, bytes, length);
            return true;
        }

        bytes = Array.Empty<byte>();
        return false;
    }

    /// <summary>
    /// Allocation-free variant of <see cref="TryReadGuestBytes(ICpuMemory, ulong, int, out byte[])"/>
    /// for the decode-cache hot path: a decode-cache <em>hit</em> only needs the fresh bytes
    /// transiently, to validate the cache entry still matches (self-modifying-code guard) — it never
    /// needs to keep them. Reading into a caller-owned (typically stack-allocated) span instead of a
    /// freshly heap-allocated array turns "one small array allocation per instruction executed" (the
    /// common case in any hot loop) into zero allocations on every cache hit.
    /// </summary>
    public static bool TryReadGuestBytes(ICpuMemory memory, ulong rip, int maxLen, Span<byte> destination, out int length)
    {
        ArgumentNullException.ThrowIfNull(memory);
        var clampedLength = Math.Min(Math.Clamp(maxLen, 1, MaxInstructionBytes), destination.Length);

        for (length = clampedLength; length > 0; length--)
        {
            if (memory.TryRead(rip, destination[..length]))
            {
                return true;
            }
        }

        return false;
    }

    public static bool TryReadGuestBytes(IVirtualMemory memory, ulong rip, int maxLen, out byte[] bytes)
    {
        ArgumentNullException.ThrowIfNull(memory);
        var clampedLength = Math.Clamp(maxLen, 1, MaxInstructionBytes);
        var buffer = new byte[clampedLength];
        Span<byte> oneByte = stackalloc byte[1];
        var readCount = 0;
        for (var i = 0; i < clampedLength; i++)
        {
            if (!memory.TryRead(rip + (ulong)i, oneByte))
            {
                break;
            }

            buffer[readCount] = oneByte[0];
            readCount++;
        }

        if (readCount == 0)
        {
            bytes = Array.Empty<byte>();
            return false;
        }

        if (readCount == clampedLength)
        {
            bytes = buffer;
            return true;
        }

        bytes = new byte[readCount];
        Array.Copy(buffer, bytes, readCount);
        return true;
    }

    public static string FormatBytes(ReadOnlySpan<byte> bytes)
    {
        if (bytes.IsEmpty)
        {
            return "??";
        }

        var parts = new string[bytes.Length];
        for (var i = 0; i < bytes.Length; i++)
        {
            parts[i] = bytes[i].ToString("X2");
        }

        return string.Join(' ', parts);
    }

    private static ulong? GetNearBranchTarget(in Instruction instruction)
    {
        for (var opIndex = 0; opIndex < instruction.OpCount; opIndex++)
        {
            switch (instruction.GetOpKind(opIndex))
            {
                case OpKind.NearBranch16:
                    return instruction.NearBranch16;
                case OpKind.NearBranch32:
                    return instruction.NearBranch32;
                case OpKind.NearBranch64:
                    return instruction.NearBranch64;
            }
        }

        return null;
    }

    private static ulong? GetMemoryAddress(in Instruction instruction)
    {
        var hasMemoryOperand = false;
        for (var opIndex = 0; opIndex < instruction.OpCount; opIndex++)
        {
            if (IsMemoryOpKind(instruction.GetOpKind(opIndex)))
            {
                hasMemoryOperand = true;
                break;
            }
        }

        if (!hasMemoryOperand)
        {
            return null;
        }

        if (instruction.IsIPRelativeMemoryOperand)
        {
            return instruction.IPRelativeMemoryAddress;
        }

        if (instruction.MemoryBase == Register.None &&
            instruction.MemoryIndex == Register.None &&
            instruction.MemoryDisplacement64 != 0)
        {
            return instruction.MemoryDisplacement64;
        }

        return null;
    }

    private static bool IsMemoryOpKind(OpKind opKind)
    {
        return opKind is
            OpKind.Memory or
            OpKind.MemorySegSI or
            OpKind.MemorySegESI or
            OpKind.MemorySegRSI or
            OpKind.MemorySegDI or
            OpKind.MemorySegEDI or
            OpKind.MemorySegRDI or
            OpKind.MemoryESDI or
            OpKind.MemoryESEDI or
            OpKind.MemoryESRDI;
    }
}
