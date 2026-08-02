// Copyright (C) 2026 SharpEmu Emulator Project
// SPDX-License-Identifier: GPL-2.0-or-later

using System.Numerics;
using Iced.Intel;

namespace SharpEmu.Core.Cpu.Interpreter;

internal static class X64Flags
{
    private const ulong Carry = 1UL << 0;
    private const ulong Parity = 1UL << 2;
    private const ulong Zero = 1UL << 6;
    private const ulong Sign = 1UL << 7;
    private const ulong Overflow = 1UL << 11;

    public static bool GetCarry(ulong rflags) => (rflags & Carry) != 0;

    public static bool GetZero(ulong rflags) => (rflags & Zero) != 0;

    public static bool GetSign(ulong rflags) => (rflags & Sign) != 0;

    public static bool GetOverflow(ulong rflags) => (rflags & Overflow) != 0;

    public static ulong UpdateLogic(ulong rflags, ulong result, int bits)
    {
        rflags &= ~(Carry | Parity | Zero | Sign | Overflow);
        return ApplyCommon(rflags, result, bits);
    }

    public static ulong UpdateAdd(ulong rflags, ulong lhs, ulong rhs, ulong result, int bits)
    {
        var mask = Mask(bits);
        var sign = SignMask(bits);
        lhs &= mask;
        rhs &= mask;
        result &= mask;

        rflags &= ~(Carry | Parity | Zero | Sign | Overflow);
        if (result < lhs)
        {
            rflags |= Carry;
        }
        if (((~(lhs ^ rhs) & (lhs ^ result)) & sign) != 0)
        {
            rflags |= Overflow;
        }

        return ApplyCommon(rflags, result, bits);
    }

    public static ulong UpdateSub(ulong rflags, ulong lhs, ulong rhs, ulong result, int bits)
    {
        var mask = Mask(bits);
        var sign = SignMask(bits);
        lhs &= mask;
        rhs &= mask;
        result &= mask;

        rflags &= ~(Carry | Parity | Zero | Sign | Overflow);
        if (lhs < rhs)
        {
            rflags |= Carry;
        }
        if ((((lhs ^ rhs) & (lhs ^ result)) & sign) != 0)
        {
            rflags |= Overflow;
        }

        return ApplyCommon(rflags, result, bits);
    }

    public static ulong UpdatePopCount(ulong rflags, ulong source)
    {
        rflags &= ~(Carry | Parity | Zero | Sign | Overflow);
        if (source == 0)
        {
            rflags |= Zero;
        }

        return rflags;
    }

    public static ulong UpdateBitScanCount(ulong rflags, ulong source, ulong result)
    {
        rflags &= ~(Carry | Parity | Zero | Sign | Overflow);
        if (source == 0)
        {
            rflags |= Carry;
        }
        if (result == 0)
        {
            rflags |= Zero;
        }

        return rflags;
    }

    public static ulong UpdateMultiplyOverflow(ulong rflags, bool overflow)
    {
        rflags &= ~(Carry | Overflow);
        if (overflow)
        {
            rflags |= Carry | Overflow;
        }

        return rflags;
    }

    public static bool TryEvaluateCondition(Mnemonic mnemonic, ulong rflags, out bool isTaken)
    {
        var cf = GetCarry(rflags);
        var zf = GetZero(rflags);
        var sf = GetSign(rflags);
        var of = GetOverflow(rflags);

        isTaken = mnemonic switch
        {
            Mnemonic.Je => zf,
            Mnemonic.Cmove => zf,
            Mnemonic.Sete => zf,
            Mnemonic.Jne => !zf,
            Mnemonic.Cmovne => !zf,
            Mnemonic.Setne => !zf,
            Mnemonic.Jb => cf,
            Mnemonic.Cmovb => cf,
            Mnemonic.Setb => cf,
            Mnemonic.Jae => !cf,
            Mnemonic.Cmovae => !cf,
            Mnemonic.Setae => !cf,
            Mnemonic.Jbe => cf || zf,
            Mnemonic.Cmovbe => cf || zf,
            Mnemonic.Setbe => cf || zf,
            Mnemonic.Ja => !cf && !zf,
            Mnemonic.Cmova => !cf && !zf,
            Mnemonic.Seta => !cf && !zf,
            Mnemonic.Js => sf,
            Mnemonic.Cmovs => sf,
            Mnemonic.Sets => sf,
            Mnemonic.Jns => !sf,
            Mnemonic.Cmovns => !sf,
            Mnemonic.Setns => !sf,
            Mnemonic.Jo => of,
            Mnemonic.Cmovo => of,
            Mnemonic.Seto => of,
            Mnemonic.Jno => !of,
            Mnemonic.Cmovno => !of,
            Mnemonic.Setno => !of,
            Mnemonic.Jl => sf != of,
            Mnemonic.Cmovl => sf != of,
            Mnemonic.Setl => sf != of,
            Mnemonic.Jge => sf == of,
            Mnemonic.Cmovge => sf == of,
            Mnemonic.Setge => sf == of,
            Mnemonic.Jle => zf || sf != of,
            Mnemonic.Cmovle => zf || sf != of,
            Mnemonic.Setle => zf || sf != of,
            Mnemonic.Jg => !zf && sf == of,
            Mnemonic.Cmovg => !zf && sf == of,
            Mnemonic.Setg => !zf && sf == of,
            _ => false,
        };
        return mnemonic is
            Mnemonic.Je or
            Mnemonic.Cmove or
            Mnemonic.Sete or
            Mnemonic.Jne or
            Mnemonic.Cmovne or
            Mnemonic.Setne or
            Mnemonic.Jb or
            Mnemonic.Cmovb or
            Mnemonic.Setb or
            Mnemonic.Jae or
            Mnemonic.Cmovae or
            Mnemonic.Setae or
            Mnemonic.Jbe or
            Mnemonic.Cmovbe or
            Mnemonic.Setbe or
            Mnemonic.Ja or
            Mnemonic.Cmova or
            Mnemonic.Seta or
            Mnemonic.Js or
            Mnemonic.Cmovs or
            Mnemonic.Sets or
            Mnemonic.Jns or
            Mnemonic.Cmovns or
            Mnemonic.Setns or
            Mnemonic.Jo or
            Mnemonic.Cmovo or
            Mnemonic.Seto or
            Mnemonic.Jno or
            Mnemonic.Cmovno or
            Mnemonic.Setno or
            Mnemonic.Jl or
            Mnemonic.Cmovl or
            Mnemonic.Setl or
            Mnemonic.Jge or
            Mnemonic.Cmovge or
            Mnemonic.Setge or
            Mnemonic.Jle or
            Mnemonic.Cmovle or
            Mnemonic.Setle or
            Mnemonic.Jg or
            Mnemonic.Cmovg or
            Mnemonic.Setg;
    }

    private static ulong ApplyCommon(ulong rflags, ulong result, int bits)
    {
        var masked = result & Mask(bits);
        if (masked == 0)
        {
            rflags |= Zero;
        }
        if ((masked & SignMask(bits)) != 0)
        {
            rflags |= Sign;
        }
        if ((BitOperations.PopCount((uint)(masked & 0xFF)) & 1) == 0)
        {
            rflags |= Parity;
        }

        return rflags;
    }

    public static ulong Mask(int bits) => bits >= 64 ? ulong.MaxValue : (1UL << bits) - 1;

    private static ulong SignMask(int bits) => 1UL << (bits - 1);
}
