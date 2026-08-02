// Copyright (C) 2026 SharpEmu Emulator Project
// SPDX-License-Identifier: GPL-2.0-or-later

using SharpEmu.Core.Cpu;
using SharpEmu.Core.Cpu.Interpreter;
using SharpEmu.Core.Loader;
using SharpEmu.Core.Memory;
using SharpEmu.HLE;
using Xunit;

namespace SharpEmu.Libs.Tests.Cpu;

public sealed class X64InterpreterBackendTests
{
    private const ulong CodeBase = 0x1000;
    private const ulong DataBase = 0x2000;
    private const ulong StackBase = 0x7000;
    private const ulong StackSize = 0x1000;

    [Fact]
    public void Execute_ReturnsImmediateRax()
    {
        // mov eax, 42; ret
        var context = CreateContext([0xB8, 0x2A, 0, 0, 0, 0xC3]);
        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        Assert.Equal(42UL, context[CpuRegister.Rax]);
    }

    [Fact]
    public void Execute_AddsSysVArguments()
    {
        // mov rax, rdi; add rax, rsi; ret
        var context = CreateContext([0x48, 0x89, 0xF8, 0x48, 0x01, 0xF0, 0xC3]);
        context[CpuRegister.Rdi] = 7;
        context[CpuRegister.Rsi] = 35;

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        Assert.Equal(42UL, context[CpuRegister.Rax]);
    }

    [Fact]
    public void Execute_RunsConditionalLoop()
    {
        // Sum 5 + 4 + 3 + 2 + 1 using cmp/jnz.
        var context = CreateContext(
        [
            0xB8, 0x00, 0x00, 0x00, 0x00,
            0xB9, 0x05, 0x00, 0x00, 0x00,
            0x01, 0xC8,
            0x83, 0xE9, 0x01,
            0x83, 0xF9, 0x00,
            0x75, 0xF6,
            0xC3,
        ]);

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        Assert.Equal(15UL, context[CpuRegister.Rax]);
    }

    [Fact]
    public void Execute_ReadsAndWritesGuestMemory()
    {
        // mov [rdi], rsi; mov rax, [rdi]; ret
        var context = CreateContext([0x48, 0x89, 0x37, 0x48, 0x8B, 0x07, 0xC3]);
        context[CpuRegister.Rdi] = DataBase;
        context[CpuRegister.Rsi] = 0x1234_5678_9ABC_DEF0;

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        Assert.Equal(0x1234_5678_9ABC_DEF0UL, context[CpuRegister.Rax]);
        Assert.True(context.TryReadUInt64(DataBase, out var written));
        Assert.Equal(0x1234_5678_9ABC_DEF0UL, written);
    }

    [Fact]
    public void Execute_DecrementsRegisterAndPreservesCarry()
    {
        // dec rsi; ret
        var context = CreateContext([0x48, 0xFF, 0xCE, 0xC3]);
        context[CpuRegister.Rsi] = 2;
        context.Rflags |= 1;

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        Assert.Equal(1UL, context[CpuRegister.Rsi]);
        Assert.Equal(1UL, context.Rflags & 1UL);
    }

    [Fact]
    public void Execute_IncrementsRegisterAndPreservesCarry()
    {
        // inc rsi; ret
        var context = CreateContext([0x48, 0xFF, 0xC6, 0xC3]);
        context[CpuRegister.Rsi] = ulong.MaxValue;
        context.Rflags |= 1;

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        Assert.Equal(0UL, context[CpuRegister.Rsi]);
        Assert.Equal(1UL, context.Rflags & 1UL);
    }

    [Fact]
    public void Execute_AddsWithCarryInWithAdc()
    {
        // adc esi, 0; ret
        var context = CreateContext([0x83, 0xD6, 0x00, 0xC3]);
        context[CpuRegister.Rsi] = 5;
        context.Rflags |= 1UL;

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        Assert.Equal(6UL, context[CpuRegister.Rsi]);
        Assert.Equal(0UL, context.Rflags & 1UL);
    }

    [Fact]
    public void Execute_CarriesThroughAdcWhenLowByteWraps()
    {
        // adc al, 0; ret
        var context = CreateContext([0x14, 0x00, 0xC3]);
        context[CpuRegister.Rax] = 0xFF;
        context.Rflags |= 1UL;

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        Assert.Equal(0x00UL, context[CpuRegister.Rax] & 0xFF);
        Assert.True((context.Rflags & 1UL) != 0);
    }

    [Fact]
    public void Execute_SubtractsWithBorrowInWithSbb()
    {
        // sbb eax, ecx; ret
        var context = CreateContext([0x19, 0xC8, 0xC3]);
        context[CpuRegister.Rax] = 5;
        context[CpuRegister.Rcx] = 3;
        context.Rflags |= 1UL;

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        Assert.Equal(1UL, context[CpuRegister.Rax]);
        Assert.Equal(0UL, context.Rflags & 1UL);
    }

    [Fact]
    public void Execute_ReversesByteOrderWithBswap()
    {
        // bswap ecx; ret
        var context = CreateContext([0x0F, 0xC9, 0xC3]);
        context[CpuRegister.Rcx] = 0x1234_5678;
        context.Rflags = 0xAD7;

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        Assert.Equal(0x7856_3412UL, context[CpuRegister.Rcx]);
        Assert.Equal(0xAD7UL, context.Rflags);
    }

    [Fact]
    public void Execute_ComplementsRegisterWithNotAndLeavesFlagsUntouched()
    {
        // not eax; ret
        var context = CreateContext([0xF7, 0xD0, 0xC3]);
        context[CpuRegister.Rax] = 0x0000_0001;
        context.Rflags = 0xAD7;

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        Assert.Equal(0xFFFF_FFFEUL, context[CpuRegister.Rax]);
        Assert.Equal(0xAD7UL, context.Rflags);
    }

    [Fact]
    public void Execute_NegatesRegisterAndSetsCarry()
    {
        // neg eax; ret
        var context = CreateContext([0xF7, 0xD8, 0xC3]);
        context[CpuRegister.Rax] = 1;

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        Assert.Equal(0xFFFF_FFFFUL, context[CpuRegister.Rax]);
        Assert.True((context.Rflags & 1UL) != 0);
        Assert.True((context.Rflags & (1UL << 7)) != 0);
    }

    [Fact]
    public void Execute_NegatesZeroAndClearsCarry()
    {
        // neg eax; ret
        var context = CreateContext([0xF7, 0xD8, 0xC3]);
        context[CpuRegister.Rax] = 0;
        context.Rflags |= 1UL;

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        Assert.Equal(0UL, context[CpuRegister.Rax]);
        Assert.True((context.Rflags & 1UL) == 0);
        Assert.True((context.Rflags & (1UL << 6)) != 0);
    }

    [Fact]
    public void Execute_DividesSignedEdxEaxByRegister()
    {
        // idiv ecx; ret
        var context = CreateContext([0xF7, 0xF9, 0xC3]);
        context[CpuRegister.Rax] = unchecked((ulong)(uint)(-7));
        context[CpuRegister.Rdx] = 0xFFFF_FFFF;
        context[CpuRegister.Rcx] = 2;

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        Assert.Equal(0xFFFF_FFFDUL, context[CpuRegister.Rax]);
        Assert.Equal(0xFFFF_FFFFUL, context[CpuRegister.Rdx]);
    }

    [Fact]
    public void Execute_TrapsOnSignedDivideByZero()
    {
        // idiv ecx; ret
        var context = CreateContext([0xF7, 0xF9, 0xC3]);
        context[CpuRegister.Rax] = 10;
        context[CpuRegister.Rdx] = 0;
        context[CpuRegister.Rcx] = 0;

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_ERROR_CPU_TRAP, result.Result);
    }

    [Fact]
    public void Execute_DividesEdxEaxByRegister()
    {
        // div ecx; ret
        var context = CreateContext([0xF7, 0xF1, 0xC3]);
        context[CpuRegister.Rax] = 0xFEC0;
        context[CpuRegister.Rdx] = 0;
        context[CpuRegister.Rcx] = 0x100;

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        Assert.Equal(0xFEUL, context[CpuRegister.Rax]);
        Assert.Equal(0xC0UL, context[CpuRegister.Rdx]);
    }

    [Fact]
    public void Execute_DividesAxByByteRegister()
    {
        // div cl; ret
        var context = CreateContext([0xF6, 0xF1, 0xC3]);
        context[CpuRegister.Rax] = 0x34;
        context[CpuRegister.Rcx] = 0x10;

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        Assert.Equal(0x0403UL, context[CpuRegister.Rax]);
    }

    [Fact]
    public void Execute_MultipliesUnsignedRaxByRegisterWithMul()
    {
        // mul rdx; ret
        var context = CreateContext([0x48, 0xF7, 0xE2, 0xC3]);
        context[CpuRegister.Rax] = 6;
        context[CpuRegister.Rdx] = 7;
        context.Rflags = 0xAD7;

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        Assert.Equal(42UL, context[CpuRegister.Rax]);
        Assert.Equal(0UL, context[CpuRegister.Rdx]);
        Assert.True((context.Rflags & 1UL) == 0);
        Assert.True((context.Rflags & (1UL << 11)) == 0);
    }

    [Fact]
    public void Execute_SetsOverflowForWideningMul()
    {
        // mul rdx; ret
        var context = CreateContext([0x48, 0xF7, 0xE2, 0xC3]);
        context[CpuRegister.Rax] = 0xFFFF_FFFF_FFFF_FFFF;
        context[CpuRegister.Rdx] = 2;

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        Assert.Equal(0xFFFF_FFFF_FFFF_FFFEUL, context[CpuRegister.Rax]);
        Assert.Equal(1UL, context[CpuRegister.Rdx]);
        Assert.True((context.Rflags & 1UL) != 0);
        Assert.True((context.Rflags & (1UL << 11)) != 0);
    }

    [Fact]
    public void Execute_MultipliesSignedRegisterWithImul()
    {
        // imul ecx, eax; ret
        var context = CreateContext([0x0F, 0xAF, 0xC8, 0xC3]);
        context[CpuRegister.Rax] = 7;
        context[CpuRegister.Rcx] = 6;
        context.Rflags = 0xAD7;

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        Assert.Equal(42UL, context[CpuRegister.Rcx]);
        Assert.True((context.Rflags & 1UL) == 0);
        Assert.True((context.Rflags & (1UL << 11)) == 0);
    }

    [Fact]
    public void Execute_SetsOverflowForTruncatedImul()
    {
        // imul ecx, eax; ret
        var context = CreateContext([0x0F, 0xAF, 0xC8, 0xC3]);
        context[CpuRegister.Rax] = 2;
        context[CpuRegister.Rcx] = 0x8000_0000;

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        Assert.Equal(0UL, context[CpuRegister.Rcx]);
        Assert.True((context.Rflags & 1UL) != 0);
        Assert.True((context.Rflags & (1UL << 11)) != 0);
    }

    [Fact]
    public void Execute_ShiftsLeftWithShlxWithoutChangingFlags()
    {
        // shlx rax, rcx, rdx; ret
        var context = CreateContext([0xC4, 0xE2, 0xE9, 0xF7, 0xC1, 0xC3]);
        context[CpuRegister.Rcx] = 3;
        context[CpuRegister.Rdx] = 4;
        context.Rflags = 0x247;

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        Assert.Equal(48UL, context[CpuRegister.Rax]);
        Assert.Equal(0x247UL, context.Rflags);
    }

    [Fact]
    public void Execute_ShiftsRightWithShrImmediate()
    {
        // shr rdx, 6; ret
        var context = CreateContext([0x48, 0xC1, 0xEA, 0x06, 0xC3]);
        context[CpuRegister.Rdx] = 0xC0;

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        Assert.Equal(3UL, context[CpuRegister.Rdx]);
    }

    [Fact]
    public void Execute_ZeroesHighBitsWithBzhi()
    {
        // bzhi rsi, rsi, rbx; ret
        var context = CreateContext([0xC4, 0xE2, 0xE0, 0xF5, 0xF6, 0xC3]);
        context[CpuRegister.Rsi] = ulong.MaxValue;
        context[CpuRegister.Rbx] = 4;
        context.Rflags = 0xAD7;

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        Assert.Equal(0x0FUL, context[CpuRegister.Rsi]);
        Assert.Equal(0UL, context.Rflags & 1UL);
        Assert.Equal(0UL, context.Rflags & (1UL << 6));
    }

    [Fact]
    public void Execute_SetsCarryWhenBzhiIndexExceedsOperandSize()
    {
        // bzhi rsi, rsi, rbx; ret
        var context = CreateContext([0xC4, 0xE2, 0xE0, 0xF5, 0xF6, 0xC3]);
        context[CpuRegister.Rsi] = 0x1234;
        context[CpuRegister.Rbx] = 200;
        context.Rflags = 0;

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        Assert.Equal(0x1234UL, context[CpuRegister.Rsi]);
        Assert.True((context.Rflags & 1UL) != 0);
    }

    [Fact]
    public void Execute_RotatesRightWithoutAffectingFlagsWithRorx()
    {
        // rorx eax, ecx, 0Fh; ret
        var context = CreateContext([0xC4, 0xE3, 0x7B, 0xF0, 0xC1, 0x0F, 0xC3]);
        context[CpuRegister.Rcx] = 1;
        context.Rflags = 0xAD7;

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        Assert.Equal(0x0002_0000UL, context[CpuRegister.Rax]);
        Assert.Equal(0xAD7UL, context.Rflags);
    }

    [Fact]
    public void Execute_ExtractsBitFieldWithBextr()
    {
        // bextr ecx, edx, eax; ret
        var context = CreateContext([0xC4, 0xE2, 0x78, 0xF7, 0xCA, 0xC3]);
        context[CpuRegister.Rdx] = 0xABCD_1234;
        context[CpuRegister.Rax] = 0x0804; // start=4, length=8
        context.Rflags = 0xAD7;

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        Assert.Equal(0x23UL, context[CpuRegister.Rcx]);
        Assert.True((context.Rflags & (1UL << 6)) == 0);
    }

    [Fact]
    public void Execute_SetsZeroFlagWhenBextrLengthIsZero()
    {
        // bextr ecx, edx, eax; ret
        var context = CreateContext([0xC4, 0xE2, 0x78, 0xF7, 0xCA, 0xC3]);
        context[CpuRegister.Rdx] = 0xFFFF_FFFF;
        context[CpuRegister.Rax] = 0x0000; // length=0
        context[CpuRegister.Rcx] = 0x1234;
        context.Rflags = 0;

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        Assert.Equal(0UL, context[CpuRegister.Rcx]);
        Assert.True((context.Rflags & (1UL << 6)) != 0);
    }

    [Fact]
    public void Execute_BitTestCopiesSelectedBitToCarry()
    {
        // bt rcx, r9; ret
        var context = CreateContext([0x4C, 0x0F, 0xA3, 0xC9, 0xC3]);
        context[CpuRegister.Rcx] = 0b1000;
        context[CpuRegister.R9] = 3;
        context.Rflags = 0x246;

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        Assert.Equal(1UL, context.Rflags & 1UL);
        Assert.Equal(0x246UL, context.Rflags & ~1UL);
    }

    [Fact]
    public void Execute_SignExtendsEaxToRaxWithCdqe()
    {
        // cdqe; ret
        var context = CreateContext([0x48, 0x98, 0xC3]);
        context[CpuRegister.Rax] = 0xFFFF_FFFF_8000_0001;

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        Assert.Equal(0xFFFF_FFFF_8000_0001UL, context[CpuRegister.Rax]);
    }

    [Fact]
    public void Execute_KeepsPositiveEaxWithCdqe()
    {
        // cdqe; ret
        var context = CreateContext([0x48, 0x98, 0xC3]);
        context[CpuRegister.Rax] = 0xFFFF_FFFF_0000_002A;

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        Assert.Equal(0x2AUL, context[CpuRegister.Rax]);
    }

    [Fact]
    public void Execute_ExtendsRaxSignIntoRdxWithCqo()
    {
        // cqo; ret
        var context = CreateContext([0x48, 0x99, 0xC3]);
        context[CpuRegister.Rax] = 0x8000_0000_0000_0000;
        context[CpuRegister.Rdx] = 0x1234;

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        Assert.Equal(ulong.MaxValue, context[CpuRegister.Rdx]);
    }

    [Fact]
    public void Execute_ClearsRdxForPositiveRaxWithCqo()
    {
        // cqo; ret
        var context = CreateContext([0x48, 0x99, 0xC3]);
        context[CpuRegister.Rax] = 42;
        context[CpuRegister.Rdx] = 0x1234;

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        Assert.Equal(0UL, context[CpuRegister.Rdx]);
    }

    [Fact]
    public void Execute_SignExtendsDwordWithMovsxd()
    {
        // movsxd r12, r13d; ret
        var context = CreateContext([0x4D, 0x63, 0xE5, 0xC3]);
        context[CpuRegister.R13] = 0xFFFF_FFFF;

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        Assert.Equal(ulong.MaxValue, context[CpuRegister.R12]);
    }

    [Fact]
    public void Execute_SignExtendsByteWithMovsx()
    {
        // movsx esi, r12b; ret
        var context = CreateContext([0x41, 0x0F, 0xBE, 0xF4, 0xC3]);
        context[CpuRegister.R12] = 0xFF;

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        Assert.Equal(0xFFFF_FFFFUL, context[CpuRegister.Rsi]);
    }

    [Fact]
    public void Execute_ConditionallyMovesWhenCarryIsSet()
    {
        // cmovb rbx, rax; ret
        var context = CreateContext([0x48, 0x0F, 0x42, 0xD8, 0xC3]);
        context[CpuRegister.Rax] = 0x1234;
        context[CpuRegister.Rbx] = 0x5678;
        context.Rflags |= 1;

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        Assert.Equal(0x1234UL, context[CpuRegister.Rbx]);
    }

    [Fact]
    public void Execute_ConditionallyMovesWhenZeroIsSet()
    {
        // cmove ebx, ecx; ret
        var context = CreateContext([0x0F, 0x44, 0xD9, 0xC3]);
        context[CpuRegister.Rbx] = 0xFFFF_FFFF_1234_5678;
        context[CpuRegister.Rcx] = 0x89AB_CDEF;
        context.Rflags |= 1UL << 6;

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        Assert.Equal(0x89AB_CDEFUL, context[CpuRegister.Rbx]);
    }

    [Fact]
    public void Execute_ConditionallyMovesWhenBelowOrEqual()
    {
        // cmovbe edx, eax; ret
        var context = CreateContext([0x0F, 0x46, 0xD0, 0xC3]);
        context[CpuRegister.Rax] = 0x7E;
        context[CpuRegister.Rdx] = 0xFFFF_FFFF_1234_5678;
        context.Rflags |= 1UL << 6;

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        Assert.Equal(0x7EUL, context[CpuRegister.Rdx]);
    }

    [Fact]
    public void Execute_ConditionallyMovesWhenNotEqual()
    {
        // cmovne eax, r8d; ret
        var context = CreateContext([0x41, 0x0F, 0x45, 0xC0, 0xC3]);
        context[CpuRegister.Rax] = 0xFFFF_FFFF_1234_5678;
        context[CpuRegister.R8] = 0x89AB_CDEF;
        context.Rflags &= ~(1UL << 6);

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        Assert.Equal(0x89AB_CDEFUL, context[CpuRegister.Rax]);
    }

    [Fact]
    public void Execute_ConditionallyMovesWhenAbove()
    {
        // cmova rax, r13; ret
        var context = CreateContext([0x49, 0x0F, 0x47, 0xC5, 0xC3]);
        context[CpuRegister.Rax] = 0x1234;
        context[CpuRegister.R13] = 0x5678;
        context.Rflags &= ~1UL;
        context.Rflags &= ~(1UL << 6);

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        Assert.Equal(0x5678UL, context[CpuRegister.Rax]);
    }

    [Fact]
    public void Execute_ConditionallyKeepsDestinationWhenNotAbove()
    {
        // cmova rax, r13; ret
        var context = CreateContext([0x49, 0x0F, 0x47, 0xC5, 0xC3]);
        context[CpuRegister.Rax] = 0x1234;
        context[CpuRegister.R13] = 0x5678;
        context.Rflags |= 1UL;

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        Assert.Equal(0x1234UL, context[CpuRegister.Rax]);
    }

    [Fact]
    public void Execute_ConditionallyMovesWhenNotSign()
    {
        // cmovns ecx, eax; ret
        var context = CreateContext([0x0F, 0x49, 0xC8, 0xC3]);
        context[CpuRegister.Rax] = 0x1234_5678;
        context[CpuRegister.Rcx] = 1;
        context.Rflags &= ~(1UL << 7);

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        Assert.Equal(0x1234_5678UL, context[CpuRegister.Rcx]);
    }

    [Fact]
    public void Execute_SetsByteWhenZeroIsClear()
    {
        // setne cl; ret
        var context = CreateContext([0x0F, 0x95, 0xC1, 0xC3]);
        context[CpuRegister.Rcx] = 0x1234_5678;
        context.Rflags &= ~(1UL << 6);

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        Assert.Equal(0x1234_5601UL, context[CpuRegister.Rcx]);
    }

    [Fact]
    public void Execute_SetsByteWhenZeroIsSet()
    {
        // sete al; ret
        var context = CreateContext([0x0F, 0x94, 0xC0, 0xC3]);
        context[CpuRegister.Rax] = 0x1234_5678;
        context.Rflags |= 1UL << 6;

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        Assert.Equal(0x1234_5601UL, context[CpuRegister.Rax]);
    }

    [Fact]
    public void Execute_SetsByteWhenAbove()
    {
        // seta al; ret
        var context = CreateContext([0x0F, 0x97, 0xC0, 0xC3]);
        context[CpuRegister.Rax] = 0x1234_5678;
        context.Rflags &= ~1UL;
        context.Rflags &= ~(1UL << 6);

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        Assert.Equal(0x1234_5601UL, context[CpuRegister.Rax]);
    }

    [Fact]
    public void Execute_SetsByteWhenGreater()
    {
        // setg dil; ret
        var context = CreateContext([0x40, 0x0F, 0x9F, 0xC7, 0xC3]);
        context[CpuRegister.Rdi] = 0x1234_5678;
        context.Rflags &= ~(1UL << 6);
        context.Rflags &= ~(1UL << 7);
        context.Rflags &= ~(1UL << 11);

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        Assert.Equal(0x1234_5601UL, context[CpuRegister.Rdi]);
    }

    [Fact]
    public void Execute_ClearsByteWhenNotGreater()
    {
        // setg dil; ret
        var context = CreateContext([0x40, 0x0F, 0x9F, 0xC7, 0xC3]);
        context[CpuRegister.Rdi] = 0x1234_5678;
        context.Rflags |= 1UL << 6;

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        Assert.Equal(0x1234_5600UL, context[CpuRegister.Rdi]);
    }

    [Fact]
    public void Execute_ComparesAndExchangesMemoryWithCmpxchg()
    {
        // lock cmpxchg [rdi], ecx; ret
        var context = CreateContext([0xF0, 0x0F, 0xB1, 0x0F, 0xC3]);
        context[CpuRegister.Rdi] = DataBase;
        context[CpuRegister.Rax] = 0;
        context[CpuRegister.Rcx] = 1;
        Assert.True(context.TryWriteUInt32(DataBase, 0));

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        Assert.True(context.TryReadUInt32(DataBase, out var value));
        Assert.Equal(1U, value);
        Assert.Equal(0UL, context[CpuRegister.Rax]);
        Assert.True((context.Rflags & (1UL << 6)) != 0);
    }

    [Fact]
    public void Execute_LoadsAccumulatorWhenCmpxchgComparisonFails()
    {
        // lock cmpxchg [rdi], ecx; ret
        var context = CreateContext([0xF0, 0x0F, 0xB1, 0x0F, 0xC3]);
        context[CpuRegister.Rdi] = DataBase;
        context[CpuRegister.Rax] = 0;
        context[CpuRegister.Rcx] = 1;
        Assert.True(context.TryWriteUInt32(DataBase, 2));

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        Assert.True(context.TryReadUInt32(DataBase, out var value));
        Assert.Equal(2U, value);
        Assert.Equal(2UL, context[CpuRegister.Rax]);
        Assert.True((context.Rflags & (1UL << 6)) == 0);
    }

    [Fact]
    public void Execute_ExchangesAndAddsMemoryWithXadd()
    {
        // lock xadd [rdi], eax; ret
        var context = CreateContext([0xF0, 0x0F, 0xC1, 0x07, 0xC3]);
        context[CpuRegister.Rdi] = DataBase;
        context[CpuRegister.Rax] = 3;
        Assert.True(context.TryWriteUInt32(DataBase, 5));

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        Assert.True(context.TryReadUInt32(DataBase, out var value));
        Assert.Equal(8U, value);
        Assert.Equal(5UL, context[CpuRegister.Rax]);
        Assert.True((context.Rflags & 1UL) == 0);
        Assert.True((context.Rflags & (1UL << 6)) == 0);
    }

    [Fact]
    public void Execute_ExchangesAndAddsMemoryWithXaddWrap()
    {
        // lock xadd [rdi], eax; ret
        var context = CreateContext([0xF0, 0x0F, 0xC1, 0x07, 0xC3]);
        context[CpuRegister.Rdi] = DataBase;
        context[CpuRegister.Rax] = 1;
        Assert.True(context.TryWriteUInt32(DataBase, uint.MaxValue));

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        Assert.True(context.TryReadUInt32(DataBase, out var value));
        Assert.Equal(0U, value);
        Assert.Equal(uint.MaxValue, context[CpuRegister.Rax]);
        Assert.True((context.Rflags & 1UL) != 0);
        Assert.True((context.Rflags & (1UL << 6)) != 0);
    }

    [Fact]
    public void Execute_ExchangesMemoryAndRegisterWithXchg()
    {
        // xchg [r15+r9], eax; ret
        var context = CreateContext([0x43, 0x87, 0x04, 0x0F, 0xC3]);
        context[CpuRegister.R15] = DataBase;
        context[CpuRegister.R9] = 0x40;
        context[CpuRegister.Rax] = 1;
        context.Rflags = 0xAD7;
        Assert.True(context.TryWriteUInt32(DataBase + 0x40, 5));

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        Assert.True(context.TryReadUInt32(DataBase + 0x40, out var value));
        Assert.Equal(1U, value);
        Assert.Equal(5UL, context[CpuRegister.Rax]);
        Assert.Equal(0xAD7UL, context.Rflags);
    }

    [Fact]
    public void Execute_CountsBitsWithPopcnt()
    {
        // popcnt rax, rax; ret
        var context = CreateContext([0xF3, 0x48, 0x0F, 0xB8, 0xC0, 0xC3]);
        context[CpuRegister.Rax] = 0xF0F0;
        context.Rflags = 0xAD7;

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        Assert.Equal(8UL, context[CpuRegister.Rax]);
        Assert.Equal(0UL, context.Rflags & 1UL);
        Assert.Equal(0UL, context.Rflags & (1UL << 6));
        Assert.Equal(0UL, context.Rflags & (1UL << 7));
        Assert.Equal(0UL, context.Rflags & (1UL << 11));
    }

    [Fact]
    public void Execute_SetsZeroForPopcntSourceZero()
    {
        // popcnt eax, eax; ret
        var context = CreateContext([0xF3, 0x0F, 0xB8, 0xC0, 0xC3]);
        context[CpuRegister.Rax] = 0xFFFF_FFFF_0000_0000;
        context.Rflags = 0x202;

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        Assert.Equal(0UL, context[CpuRegister.Rax]);
        Assert.True((context.Rflags & (1UL << 6)) != 0);
    }

    [Fact]
    public void Execute_CountsLeadingZerosWithLzcnt()
    {
        // lzcnt rcx, rax; ret
        var context = CreateContext([0xF3, 0x48, 0x0F, 0xBD, 0xC8, 0xC3]);
        context[CpuRegister.Rax] = 0x0000_00FF;
        context.Rflags = 0xAD7;

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        Assert.Equal(56UL, context[CpuRegister.Rcx]);
        Assert.Equal(0UL, context.Rflags & 1UL);
        Assert.Equal(0UL, context.Rflags & (1UL << 6));
    }

    [Fact]
    public void Execute_SetsCarryAndZeroForLzcntOfZero()
    {
        // lzcnt eax, eax; ret
        var context = CreateContext([0xF3, 0x0F, 0xBD, 0xC0, 0xC3]);
        context[CpuRegister.Rax] = 0;
        context.Rflags = 0x202;

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        Assert.Equal(32UL, context[CpuRegister.Rax]);
        Assert.True((context.Rflags & 1UL) != 0);
        Assert.True((context.Rflags & (1UL << 6)) == 0);
    }

    [Fact]
    public void Execute_LoadsAndStoresYmmWithVmovups()
    {
        // vmovups ymm0, [rdi]; vmovups [rsi], ymm0; ret
        var context = CreateContext([0xC5, 0xFC, 0x10, 0x07, 0xC5, 0xFC, 0x11, 0x06, 0xC3]);
        var source = new byte[32];
        for (var i = 0; i < source.Length; i++)
        {
            source[i] = (byte)(i + 1);
        }

        Assert.True(context.Memory.TryWrite(DataBase, source));
        context[CpuRegister.Rdi] = DataBase;
        context[CpuRegister.Rsi] = DataBase + 0x100;

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        var destination = new byte[32];
        Assert.True(context.Memory.TryRead(DataBase + 0x100, destination));
        Assert.Equal(source, destination);
    }

    [Fact]
    public void Execute_ZerosXmmWithVxorps()
    {
        // vxorps xmm0, xmm0, xmm0; ret
        var context = CreateContext([0xC5, 0xF8, 0x57, 0xC0, 0xC3]);
        context.SetYmmRegister(
            0,
            0x1111_2222_3333_4444,
            0x5555_6666_7777_8888,
            0x9999_AAAA_BBBB_CCCC,
            0xDDDD_EEEE_FFFF_0001);

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        context.GetYmmRegister(0, out var lowLow, out var lowHigh, out var highLow, out var highHigh);
        Assert.Equal(0UL, lowLow);
        Assert.Equal(0UL, lowHigh);
        Assert.Equal(0UL, highLow);
        Assert.Equal(0UL, highHigh);
    }

    [Fact]
    public void Execute_ZerosXmmWithVpxor()
    {
        // vpxor xmm1, xmm1, xmm1; ret
        var context = CreateContext([0xC5, 0xF1, 0xEF, 0xC9, 0xC3]);
        context.SetYmmRegister(
            1,
            0x1111_2222_3333_4444,
            0x5555_6666_7777_8888,
            0x9999_AAAA_BBBB_CCCC,
            0xDDDD_EEEE_FFFF_0001);

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        context.GetYmmRegister(1, out var lowLow, out var lowHigh, out var highLow, out var highHigh);
        Assert.Equal(0UL, lowLow);
        Assert.Equal(0UL, lowHigh);
        Assert.Equal(0UL, highLow);
        Assert.Equal(0UL, highHigh);
    }

    [Fact]
    public void Execute_LoadsXmmWithVmovdqa()
    {
        // vmovdqa xmm2, [rdi]; ret
        var context = CreateContext([0xC5, 0xF9, 0x6F, 0x17, 0xC3]);
        var source = new byte[16];
        for (var i = 0; i < source.Length; i++)
        {
            source[i] = (byte)(0xA0 + i);
        }

        Assert.True(context.Memory.TryWrite(DataBase, source));
        context[CpuRegister.Rdi] = DataBase;

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        context.GetXmmRegister(2, out var low, out var high);
        Assert.Equal(0xA7A6_A5A4_A3A2_A1A0UL, low);
        Assert.Equal(0xAFAE_ADAC_ABAA_A9A8UL, high);
    }

    [Fact]
    public void Execute_LoadsXmmWithVmovupd()
    {
        // vmovupd xmm0, [rdi]; ret
        var context = CreateContext([0xC5, 0xF9, 0x10, 0x07, 0xC3]);
        var source = new byte[16];
        for (var i = 0; i < source.Length; i++)
        {
            source[i] = (byte)(0xA0 + i);
        }

        Assert.True(context.Memory.TryWrite(DataBase, source));
        context[CpuRegister.Rdi] = DataBase;

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        context.GetXmmRegister(0, out var low, out var high);
        Assert.Equal(0xA7A6_A5A4_A3A2_A1A0UL, low);
        Assert.Equal(0xAFAE_ADAC_ABAA_A9A8UL, high);
    }

    [Fact]
    public void Execute_LoadsHighQwordWithVmovhps()
    {
        // vmovhps xmm0, xmm0, [rdi]; ret
        var context = CreateContext([0xC5, 0xF8, 0x16, 0x07, 0xC3]);
        context[CpuRegister.Rdi] = DataBase;
        Assert.True(context.TryWriteUInt64(DataBase, 0x1122_3344_5566_7788));
        context.SetYmmRegister(
            0,
            0xAABB_CCDD_EEFF_0011,
            0x2222_3333_4444_5555,
            0x6666_7777_8888_9999,
            0xAAAA_BBBB_CCCC_DDDD);

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        context.GetYmmRegister(0, out var lowLow, out var lowHigh, out var highLow, out var highHigh);
        Assert.Equal(0xAABB_CCDD_EEFF_0011UL, lowLow);
        Assert.Equal(0x1122_3344_5566_7788UL, lowHigh);
        Assert.Equal(0UL, highLow);
        Assert.Equal(0UL, highHigh);
    }

    [Fact]
    public void Execute_ShufflesBytesWithVpshufb()
    {
        // vpshufb xmm1, xmm0, xmm1; ret
        var context = CreateContext([0xC4, 0xE2, 0x79, 0x00, 0xC9, 0xC3]);
        context.SetXmmRegister(0, 0x0807_0605_0403_0201, 0x100F_0E0D_0C0B_0A09);
        context.SetXmmRegister(1, 0x0F0E_0D0C_0B0A_0908, 0x8786_8584_8382_8180);

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        context.GetXmmRegister(1, out var low, out var high);
        Assert.Equal(0x100F_0E0D_0C0B_0A09UL, low);
        Assert.Equal(0UL, high);
    }

    [Fact]
    public void Execute_ComparesBytesWithVpcmpeqb()
    {
        // vpcmpeqb xmm0, xmm2, xmm1; ret
        var context = CreateContext([0xC5, 0xE9, 0x74, 0xC1, 0xC3]);
        context.SetXmmRegister(2, 0x0807_0605_0403_0201, 0x100F_0E0D_0C0B_0A09);
        context.SetXmmRegister(1, 0x0877_0605_4403_2201, 0x100F_EE0D_0CBB_0A09);
        context.SetYmmRegister(
            0,
            0x1111_2222_3333_4444,
            0x5555_6666_7777_8888,
            0x9999_AAAA_BBBB_CCCC,
            0xDDDD_EEEE_FFFF_0001);

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        context.GetYmmRegister(0, out var lowLow, out var lowHigh, out var highLow, out var highHigh);
        Assert.Equal(0xFF00_FFFF_00FF_00FFUL, lowLow);
        Assert.Equal(0xFFFF_00FF_FF00_FFFFUL, lowHigh);
        Assert.Equal(0UL, highLow);
        Assert.Equal(0UL, highHigh);
    }

    [Fact]
    public void Execute_ComparesDwordsWithVpcmpeqd()
    {
        // vpcmpeqd xmm1, xmm0, xmm1; ret
        var context = CreateContext([0xC5, 0xF9, 0x76, 0xC9, 0xC3]);
        context.SetXmmRegister(0, 0x2222_2222_1111_1111, 0x4444_4444_3333_3333);
        context.SetXmmRegister(1, 0x0000_0000_1111_1111, 0x4444_4444_0000_0000);

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        context.GetXmmRegister(1, out var low, out var high);
        Assert.Equal(0x0000_0000_FFFF_FFFFUL, low);
        Assert.Equal(0xFFFF_FFFF_0000_0000UL, high);
    }

    [Fact]
    public void Execute_TestsVectorMaskWithVptest()
    {
        // vptest xmm0, xmm0; ret
        var context = CreateContext([0xC4, 0xE2, 0x79, 0x17, 0xC0, 0xC3]);
        context.SetXmmRegister(0, 0, 0);
        context.Rflags = 0xAD7;

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        Assert.Equal(0x243UL, context.Rflags);
    }

    [Fact]
    public void Execute_ComparesImplicitByteStringWithVpcmpistri()
    {
        // vpcmpistri xmm1, xmm0, 0; ret
        var context = CreateContext([0xC4, 0xE3, 0x79, 0x63, 0xC8, 0x00, 0xC3]);
        context.SetXmmRegister(1, 0x41, 0);
        context.SetXmmRegister(0, 0x0000_0000_0041_7878, 0);
        context[CpuRegister.Rcx] = ulong.MaxValue;
        context.Rflags = 0xA16;

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        Assert.Equal(2UL, context[CpuRegister.Rcx]);
        Assert.Equal(0x2C3UL, context.Rflags);
    }

    [Fact]
    public void Execute_FindsNullByteWithVpcmpistriNegativeOrderedMode()
    {
        // vpcmpistri xmm2, xmm2, 38h; ret
        var context = CreateContext([0xC4, 0xE3, 0x79, 0x63, 0xD2, 0x38, 0xC3]);
        context.SetXmmRegister(2, 0x0000_0000_0043_4241, 0);
        context[CpuRegister.Rcx] = ulong.MaxValue;
        context.Rflags = 0xA16;

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        Assert.Equal(3UL, context[CpuRegister.Rcx]);
        Assert.Equal(0x2C3UL, context.Rflags);
    }

    [Fact]
    public void Execute_ClearsFlagsWhenScalarSingleIsGreaterWithVucomiss()
    {
        // vucomiss xmm0, xmm1; ret
        var context = CreateContext([0xC5, 0xF8, 0x2E, 0xC1, 0xC3]);
        context.SetXmmRegister(0, BitConverter.SingleToUInt32Bits(5.0f), 0);
        context.SetXmmRegister(1, BitConverter.SingleToUInt32Bits(3.0f), 0);
        context.Rflags = 0xAD7;

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        Assert.Equal(0UL, context.Rflags & 1UL);
        Assert.Equal(0UL, context.Rflags & (1UL << 6));
        Assert.Equal(0UL, context.Rflags & (1UL << 2));
    }

    [Fact]
    public void Execute_SetsZeroFlagWhenScalarSinglesAreEqualWithVucomiss()
    {
        // vucomiss xmm0, xmm1; ret
        var context = CreateContext([0xC5, 0xF8, 0x2E, 0xC1, 0xC3]);
        context.SetXmmRegister(0, BitConverter.SingleToUInt32Bits(3.0f), 0);
        context.SetXmmRegister(1, BitConverter.SingleToUInt32Bits(3.0f), 0);
        context.Rflags = 0;

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        Assert.True((context.Rflags & (1UL << 6)) != 0);
        Assert.Equal(0UL, context.Rflags & 1UL);
    }

    [Fact]
    public void Execute_DividesScalarSinglesWithVdivss()
    {
        // vdivss xmm0, xmm0, xmm1; ret
        var context = CreateContext([0xC5, 0xFA, 0x5E, 0xC1, 0xC3]);
        context.SetXmmRegister(0, (ulong)BitConverter.SingleToUInt32Bits(10.0f) | 0x1111_1111_0000_0000UL, 0x2222_2222_3333_3333UL);
        context.SetXmmRegister(1, BitConverter.SingleToUInt32Bits(4.0f), 0);

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        context.GetXmmRegister(0, out var low, out var high);
        Assert.Equal((ulong)BitConverter.SingleToUInt32Bits(2.5f) | 0x1111_1111_0000_0000UL, low);
        Assert.Equal(0x2222_2222_3333_3333UL, high);
    }

    [Fact]
    public void Execute_AddsScalarDoublesWithVaddsd()
    {
        // vaddsd xmm0, xmm0, xmm1; ret
        var context = CreateContext([0xC5, 0xFB, 0x58, 0xC1, 0xC3]);
        context.SetXmmRegister(0, BitConverter.DoubleToUInt64Bits(1.5), 0x2222_2222_3333_3333UL);
        context.SetXmmRegister(1, BitConverter.DoubleToUInt64Bits(2.25), 0);

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        context.GetXmmRegister(0, out var low, out var high);
        Assert.Equal(BitConverter.DoubleToUInt64Bits(3.75), low);
        Assert.Equal(0x2222_2222_3333_3333UL, high);
    }

    [Fact]
    public void Execute_ReturnsSmallerOperandWithVminss()
    {
        // vminss xmm4, xmm4, xmm6; ret
        var context = CreateContext([0xC5, 0xDA, 0x5D, 0xE6, 0xC3]);
        context.SetXmmRegister(4, BitConverter.SingleToUInt32Bits(3.0f), 0);
        context.SetXmmRegister(6, BitConverter.SingleToUInt32Bits(5.0f), 0);

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        context.GetXmmRegister(4, out var low, out _);
        Assert.Equal((ulong)BitConverter.SingleToUInt32Bits(3.0f), low);
    }

    [Fact]
    public void Execute_ReturnsSecondOperandWithVminssWhenFirstIsNaN()
    {
        // vminss xmm4, xmm4, xmm6; ret
        var context = CreateContext([0xC5, 0xDA, 0x5D, 0xE6, 0xC3]);
        context.SetXmmRegister(4, BitConverter.SingleToUInt32Bits(float.NaN), 0);
        context.SetXmmRegister(6, BitConverter.SingleToUInt32Bits(7.0f), 0);

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        context.GetXmmRegister(4, out var low, out _);
        Assert.Equal((ulong)BitConverter.SingleToUInt32Bits(7.0f), low);
    }

    [Fact]
    public void Execute_ComputesPackedMaxWithVmaxpd()
    {
        // vmaxpd xmm0, xmm1, xmm2; ret
        var context = CreateContext([0xC5, 0xF1, 0x5F, 0xC2, 0xC3]);
        context.SetXmmRegister(1, BitConverter.DoubleToUInt64Bits(1.0), BitConverter.DoubleToUInt64Bits(5.0));
        context.SetXmmRegister(2, BitConverter.DoubleToUInt64Bits(3.0), BitConverter.DoubleToUInt64Bits(2.0));

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        context.GetXmmRegister(0, out var low, out var high);
        Assert.Equal(BitConverter.DoubleToUInt64Bits(3.0), low);
        Assert.Equal(BitConverter.DoubleToUInt64Bits(5.0), high);
    }

    [Fact]
    public void Execute_FloorsScalarSingleWithVroundss()
    {
        // vroundss xmm1, xmm1, xmm1, 9 (floor, suppress precision exception); ret
        var context = CreateContext([0xC4, 0xE3, 0x71, 0x0A, 0xC9, 0x09, 0xC3]);
        context.SetXmmRegister(1, BitConverter.SingleToUInt32Bits(2.5f), 0xCCCC_CCCC_CCCC_CCCCUL);

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        context.GetXmmRegister(1, out var low, out var high);
        Assert.Equal((ulong)BitConverter.SingleToUInt32Bits(2.0f), low);
        Assert.Equal(0xCCCC_CCCC_CCCC_CCCCUL, high);
    }

    [Fact]
    public void Execute_CeilsPackedDoublesWithVroundpd()
    {
        // vroundpd xmm0, xmm1, 2 (ceil); ret
        var context = CreateContext([0xC4, 0xE3, 0x79, 0x09, 0xC1, 0x02, 0xC3]);
        context.SetXmmRegister(1, BitConverter.DoubleToUInt64Bits(1.2), BitConverter.DoubleToUInt64Bits(-1.2));

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        context.GetXmmRegister(0, out var low, out var high);
        Assert.Equal(BitConverter.DoubleToUInt64Bits(2.0), low);
        Assert.Equal(BitConverter.DoubleToUInt64Bits(-1.0), high);
    }

    [Fact]
    public void Execute_ConvertsFourPackedDoublesToSinglesWithVcvtpd2psFromYmm()
    {
        // vcvtpd2ps xmm0, ymm1; ret
        var context = CreateContext([0xC5, 0xFD, 0x5A, 0xC1, 0xC3]);
        context.SetYmmRegister(
            1,
            BitConverter.DoubleToUInt64Bits(1.5),
            BitConverter.DoubleToUInt64Bits(2.5),
            BitConverter.DoubleToUInt64Bits(3.5),
            BitConverter.DoubleToUInt64Bits(4.5));

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        context.GetXmmRegister(0, out var low, out var high);
        Assert.Equal((ulong)BitConverter.SingleToUInt32Bits(1.5f) | ((ulong)BitConverter.SingleToUInt32Bits(2.5f) << 32), low);
        Assert.Equal((ulong)BitConverter.SingleToUInt32Bits(3.5f) | ((ulong)BitConverter.SingleToUInt32Bits(4.5f) << 32), high);
    }

    [Fact]
    public void Execute_ConvertsTwoPackedDoublesToSinglesWithVcvtpd2psFromXmmAndZerosUpperBits()
    {
        // vcvtpd2ps xmm0, xmm1; ret
        var context = CreateContext([0xC5, 0xF9, 0x5A, 0xC1, 0xC3]);
        context.SetXmmRegister(1, BitConverter.DoubleToUInt64Bits(1.5), BitConverter.DoubleToUInt64Bits(2.5));

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        context.GetXmmRegister(0, out var low, out var high);
        Assert.Equal((ulong)BitConverter.SingleToUInt32Bits(1.5f) | ((ulong)BitConverter.SingleToUInt32Bits(2.5f) << 32), low);
        Assert.Equal(0UL, high);
    }

    [Fact]
    public void Execute_ConvertsTwoPackedSinglesToDoublesWithVcvtps2pd()
    {
        // vcvtps2pd xmm0, xmm1; ret
        var context = CreateContext([0xC5, 0xF8, 0x5A, 0xC1, 0xC3]);
        context.SetXmmRegister(
            1,
            (ulong)BitConverter.SingleToUInt32Bits(1.5f) | ((ulong)BitConverter.SingleToUInt32Bits(2.5f) << 32),
            0);

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        context.GetXmmRegister(0, out var low, out var high);
        Assert.Equal(BitConverter.DoubleToUInt64Bits(1.5), low);
        Assert.Equal(BitConverter.DoubleToUInt64Bits(2.5), high);
    }

    [Fact]
    public void Execute_ConvertsSingleToDoubleWithVcvtss2sd()
    {
        // vcvtss2sd xmm0, xmm1, xmm2; ret
        var context = CreateContext([0xC5, 0xF2, 0x5A, 0xC2, 0xC3]);
        context.SetXmmRegister(1, 0, 0xAAAA_AAAA_AAAA_AAAAUL);
        context.SetXmmRegister(2, BitConverter.SingleToUInt32Bits(2.5f), 0);

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        context.GetXmmRegister(0, out var low, out var high);
        Assert.Equal(BitConverter.DoubleToUInt64Bits(2.5), low);
        Assert.Equal(0xAAAA_AAAA_AAAA_AAAAUL, high);
    }

    [Fact]
    public void Execute_ConvertsDoubleToSingleWithVcvtsd2ssAndPreservesMergeUpperBits()
    {
        // vcvtsd2ss xmm0, xmm1, xmm2; ret
        var context = CreateContext([0xC5, 0xF3, 0x5A, 0xC2, 0xC3]);
        context.SetXmmRegister(1, 0x1234_5678_FFFF_FFFFUL, 0xBBBB_BBBB_BBBB_BBBBUL);
        context.SetXmmRegister(2, BitConverter.DoubleToUInt64Bits(3.75), 0);

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        context.GetXmmRegister(0, out var low, out var high);
        Assert.Equal(0x1234_5678_0000_0000UL | BitConverter.SingleToUInt32Bits(3.75f), low);
        Assert.Equal(0xBBBB_BBBB_BBBB_BBBBUL, high);
    }

    [Fact]
    public void Execute_ExtractsHighLaneWithVextracti128()
    {
        // vextracti128 xmm3, ymm3, 1; ret
        var context = CreateContext([0xC4, 0xE3, 0x7D, 0x39, 0xDB, 0x01, 0xC3]);
        context.SetYmmRegister(
            3,
            0x1111_1111_1111_1111UL,
            0x2222_2222_2222_2222UL,
            0x3333_3333_3333_3333UL,
            0x4444_4444_4444_4444UL);

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        context.GetXmmRegister(3, out var low, out var high);
        Assert.Equal(0x3333_3333_3333_3333UL, low);
        Assert.Equal(0x4444_4444_4444_4444UL, high);
    }

    [Fact]
    public void Execute_InsertsHighLaneWithVinserti128()
    {
        // vinserti128 ymm0, ymm1, xmm2, 1; ret
        var context = CreateContext([0xC4, 0xE3, 0x75, 0x38, 0xC2, 0x01, 0xC3]);
        context.SetYmmRegister(
            1,
            0x1111_1111_1111_1111UL,
            0x2222_2222_2222_2222UL,
            0x3333_3333_3333_3333UL,
            0x4444_4444_4444_4444UL);
        context.SetXmmRegister(2, 0x5555_5555_5555_5555UL, 0x6666_6666_6666_6666UL);

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        context.GetYmmRegister(0, out var lowLow, out var lowHigh, out var highLow, out var highHigh);
        Assert.Equal(0x1111_1111_1111_1111UL, lowLow);
        Assert.Equal(0x2222_2222_2222_2222UL, lowHigh);
        Assert.Equal(0x5555_5555_5555_5555UL, highLow);
        Assert.Equal(0x6666_6666_6666_6666UL, highHigh);
    }

    [Fact]
    public void Execute_BlendsPackedDoublesBySignBitWithVblendvpd()
    {
        // vblendvpd xmm4, xmm2, xmm4, xmm6; ret
        var context = CreateContext([0xC4, 0xE3, 0x69, 0x4B, 0xE4, 0x60, 0xC3]);
        context.SetXmmRegister(2, BitConverter.DoubleToUInt64Bits(1.0), BitConverter.DoubleToUInt64Bits(2.0));
        context.SetXmmRegister(4, BitConverter.DoubleToUInt64Bits(3.0), BitConverter.DoubleToUInt64Bits(4.0));
        context.SetXmmRegister(6, 0x0000_0000_0000_0000UL, 0x8000_0000_0000_0000UL);

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        context.GetXmmRegister(4, out var low, out var high);
        Assert.Equal(BitConverter.DoubleToUInt64Bits(1.0), low);
        Assert.Equal(BitConverter.DoubleToUInt64Bits(4.0), high);
    }

    [Fact]
    public void Execute_SetsAllOnesMaskWhenVcmpsdEqualPredicateMatches()
    {
        // vcmpsd xmm6, xmm3, xmm5, 0 (EQ_OQ); ret
        var context = CreateContext([0xC5, 0xE3, 0xC2, 0xF5, 0x00, 0xC3]);
        context.SetXmmRegister(3, BitConverter.DoubleToUInt64Bits(2.5), 0xAAAA_AAAA_BBBB_BBBBUL);
        context.SetXmmRegister(5, BitConverter.DoubleToUInt64Bits(2.5), 0);

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        context.GetXmmRegister(6, out var low, out var high);
        Assert.Equal(0xFFFF_FFFF_FFFF_FFFFUL, low);
        Assert.Equal(0xAAAA_AAAA_BBBB_BBBBUL, high);
    }

    [Fact]
    public void Execute_ClearsMaskWhenVcmpsdEqualPredicateDoesNotMatch()
    {
        // vcmpsd xmm6, xmm3, xmm5, 0 (EQ_OQ); ret
        var context = CreateContext([0xC5, 0xE3, 0xC2, 0xF5, 0x00, 0xC3]);
        context.SetXmmRegister(3, BitConverter.DoubleToUInt64Bits(1.0), 0);
        context.SetXmmRegister(5, BitConverter.DoubleToUInt64Bits(2.0), 0);

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        context.GetXmmRegister(6, out var low, out _);
        Assert.Equal(0UL, low);
    }

    [Fact]
    public void Execute_ComparesPackedDoublesElementwiseWithVcmppd()
    {
        // vcmppd xmm0, xmm1, xmm2, 0 (EQ_OQ); ret
        var context = CreateContext([0xC5, 0xF1, 0xC2, 0xC2, 0x00, 0xC3]);
        context.SetXmmRegister(1, BitConverter.DoubleToUInt64Bits(1.0), BitConverter.DoubleToUInt64Bits(2.0));
        context.SetXmmRegister(2, BitConverter.DoubleToUInt64Bits(1.0), BitConverter.DoubleToUInt64Bits(3.0));

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        context.GetXmmRegister(0, out var low, out var high);
        Assert.Equal(0xFFFF_FFFF_FFFF_FFFFUL, low);
        Assert.Equal(0UL, high);
    }

    [Fact]
    public void Execute_TruncatesDoubleToSignedIntegerWithVcvttsd2si()
    {
        // vcvttsd2si ecx, xmm3; ret
        var context = CreateContext([0xC5, 0xFB, 0x2C, 0xCB, 0xC3]);
        context.SetXmmRegister(3, BitConverter.DoubleToUInt64Bits(-2.75), 0);

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        Assert.Equal(unchecked((ulong)(uint)(-2)), context[CpuRegister.Rcx]);
    }

    [Fact]
    public void Execute_RoundsDoubleToSignedIntegerWithVcvtsd2si()
    {
        // vcvtsd2si ecx, xmm3; ret
        var context = CreateContext([0xC5, 0xFB, 0x2D, 0xCB, 0xC3]);
        context.SetXmmRegister(3, BitConverter.DoubleToUInt64Bits(-2.75), 0);

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        Assert.Equal(unchecked((ulong)(uint)(-3)), context[CpuRegister.Rcx]);
    }

    [Fact]
    public void Execute_PermutesPackedDoublesWithVpermilpdImmediateForm()
    {
        // vpermilpd xmm1, xmm0, 1; ret
        var context = CreateContext([0xC4, 0xE3, 0x79, 0x05, 0xC8, 0x01, 0xC3]);
        context.SetXmmRegister(0, 0x1111_1111_1111_1111UL, 0x2222_2222_2222_2222UL);

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        context.GetXmmRegister(1, out var low, out var high);
        Assert.Equal(0x2222_2222_2222_2222UL, low);
        Assert.Equal(0x1111_1111_1111_1111UL, high);
    }

    [Fact]
    public void Execute_TreatsPauseAsNoOp()
    {
        // pause; ret
        var context = CreateContext([0xF3, 0x90, 0xC3]);
        context[CpuRegister.Rax] = 0x1234_5678;

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        Assert.Equal(0x1234_5678UL, context[CpuRegister.Rax]);
    }

    [Fact]
    public void Execute_SubtractsPackedDoublesWithVsubpd()
    {
        // vsubpd xmm0, xmm0, xmm1; ret
        var context = CreateContext([0xC5, 0xF9, 0x5C, 0xC1, 0xC3]);
        context.SetXmmRegister(0, BitConverter.DoubleToUInt64Bits(5.0), BitConverter.DoubleToUInt64Bits(10.0));
        context.SetXmmRegister(1, BitConverter.DoubleToUInt64Bits(2.0), BitConverter.DoubleToUInt64Bits(3.0));

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        context.GetXmmRegister(0, out var low, out var high);
        Assert.Equal(BitConverter.DoubleToUInt64Bits(3.0), low);
        Assert.Equal(BitConverter.DoubleToUInt64Bits(7.0), high);
    }

    [Fact]
    public void Execute_AddsPackedSinglesWithVaddpsFromMemory()
    {
        // vaddps xmm0, xmm0, [rdi]; ret
        var context = CreateContext([0xC5, 0xF8, 0x58, 0x07, 0xC3]);
        context.SetXmmRegister(
            0,
            (ulong)BitConverter.SingleToUInt32Bits(1.0f) | ((ulong)BitConverter.SingleToUInt32Bits(2.0f) << 32),
            (ulong)BitConverter.SingleToUInt32Bits(3.0f) | ((ulong)BitConverter.SingleToUInt32Bits(4.0f) << 32));
        context[CpuRegister.Rdi] = DataBase;
        var memoryLow = (ulong)BitConverter.SingleToUInt32Bits(10.0f) | ((ulong)BitConverter.SingleToUInt32Bits(20.0f) << 32);
        var memoryHigh = (ulong)BitConverter.SingleToUInt32Bits(30.0f) | ((ulong)BitConverter.SingleToUInt32Bits(40.0f) << 32);
        Assert.True(context.TryWriteUInt64(DataBase, memoryLow));
        Assert.True(context.TryWriteUInt64(DataBase + 8, memoryHigh));

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        context.GetXmmRegister(0, out var low, out var high);
        Assert.Equal((ulong)BitConverter.SingleToUInt32Bits(11.0f) | ((ulong)BitConverter.SingleToUInt32Bits(22.0f) << 32), low);
        Assert.Equal((ulong)BitConverter.SingleToUInt32Bits(33.0f) | ((ulong)BitConverter.SingleToUInt32Bits(44.0f) << 32), high);
    }

    [Fact]
    public void Execute_LoadsScalarSingleFromMemoryWithVmovss()
    {
        // vmovss xmm1, [rdi]; ret
        var context = CreateContext([0xC5, 0xFA, 0x10, 0x0F, 0xC3]);
        Assert.True(context.TryWriteUInt32(DataBase, 0x1234_5678));
        context[CpuRegister.Rdi] = DataBase;
        context.SetXmmRegister(1, 0xFFFF_FFFF_FFFF_FFFF, 0xFFFF_FFFF_FFFF_FFFF);

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        context.GetXmmRegister(1, out var low, out var high);
        Assert.Equal(0x1234_5678UL, low);
        Assert.Equal(0UL, high);
    }

    [Fact]
    public void Execute_MergesScalarSingleWithVmovssRegisterForm()
    {
        // vmovss xmm0, xmm1, xmm2; ret
        var context = CreateContext([0xC5, 0xF2, 0x10, 0xC2, 0xC3]);
        context.SetXmmRegister(1, 0x1111_1111_2222_2222, 0x3333_3333_4444_4444);
        context.SetXmmRegister(2, 0xAAAA_AAAA_BBBB_BBBB, 0);

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        context.GetXmmRegister(0, out var low, out var high);
        Assert.Equal(0x1111_1111_BBBB_BBBBUL, low);
        Assert.Equal(0x3333_3333_4444_4444UL, high);
    }

    [Fact]
    public void Execute_ConvertsSignedIntegerToScalarSingleWithVcvtsi2ss()
    {
        // vcvtsi2ss xmm0, xmm1, rcx; ret
        var context = CreateContext([0xC4, 0xE1, 0xF2, 0x2A, 0xC1, 0xC3]);
        context.SetXmmRegister(1, 0x1111_1111_2222_2222, 0x3333_3333_4444_4444);
        context[CpuRegister.Rcx] = unchecked((ulong)(long)-5);

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        context.GetXmmRegister(0, out var low, out var high);
        Assert.Equal(0x1111_1111_C0A0_0000UL, low);
        Assert.Equal(0x3333_3333_4444_4444UL, high);
    }

    [Fact]
    public void Execute_ConvertsSignedIntegerToScalarDoubleWithVcvtsi2sd()
    {
        // vcvtsi2sd xmm0, xmm1, rcx; ret
        var context = CreateContext([0xC4, 0xE1, 0xF3, 0x2A, 0xC1, 0xC3]);
        context.SetXmmRegister(1, 0x1111_1111_2222_2222, 0x3333_3333_4444_4444);
        context[CpuRegister.Rcx] = 42;

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        context.GetXmmRegister(0, out var low, out var high);
        Assert.Equal(0x4045_0000_0000_0000UL, low);
        Assert.Equal(0x3333_3333_4444_4444UL, high);
    }

    [Fact]
    public void Execute_MovesGprToXmmWithVmovq()
    {
        // vmovq xmm0, rax; ret
        var context = CreateContext([0xC4, 0xE1, 0xF9, 0x6E, 0xC0, 0xC3]);
        context[CpuRegister.Rax] = 0x1234_5678_9ABC_DEF0;
        context.SetYmmRegister(
            0,
            0x1111_2222_3333_4444,
            0x5555_6666_7777_8888,
            0x9999_AAAA_BBBB_CCCC,
            0xDDDD_EEEE_FFFF_0001);

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        context.GetYmmRegister(0, out var lowLow, out var lowHigh, out var highLow, out var highHigh);
        Assert.Equal(0x1234_5678_9ABC_DEF0UL, lowLow);
        Assert.Equal(0UL, lowHigh);
        Assert.Equal(0UL, highLow);
        Assert.Equal(0UL, highHigh);
    }

    [Fact]
    public void Execute_MovesGprToXmmWithVmovd()
    {
        // vmovd xmm0, esi; ret
        var context = CreateContext([0xC5, 0xF9, 0x6E, 0xC6, 0xC3]);
        context[CpuRegister.Rsi] = 0xFFFF_FF80;
        context.SetYmmRegister(
            0,
            0x1111_2222_3333_4444,
            0x5555_6666_7777_8888,
            0x9999_AAAA_BBBB_CCCC,
            0xDDDD_EEEE_FFFF_0001);

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        context.GetYmmRegister(0, out var lowLow, out var lowHigh, out var highLow, out var highHigh);
        Assert.Equal(0xFFFF_FF80UL, lowLow);
        Assert.Equal(0UL, lowHigh);
        Assert.Equal(0UL, highLow);
        Assert.Equal(0UL, highHigh);
    }

    [Fact]
    public void Execute_InterleavesLowDwordsWithVpunpckldq()
    {
        // vpunpckldq xmm0, xmm0, xmm1; ret
        var context = CreateContext([0xC5, 0xF9, 0x62, 0xC1, 0xC3]);
        context.SetXmmRegister(0, 0x2222_2222_1111_1111, 0x4444_4444_3333_3333);
        context.SetXmmRegister(1, 0xBBBB_BBBB_AAAA_AAAA, 0xDDDD_DDDD_CCCC_CCCC);

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        context.GetXmmRegister(0, out var low, out var high);
        Assert.Equal(0xAAAA_AAAA_1111_1111UL, low);
        Assert.Equal(0xBBBB_BBBB_2222_2222UL, high);
    }

    [Fact]
    public void Execute_BlendsDwordsWithVpblendd()
    {
        // vpblendd xmm0, xmm0, xmm1, 5; ret
        var context = CreateContext([0xC4, 0xE3, 0x79, 0x02, 0xC1, 0x05, 0xC3]);
        context.SetXmmRegister(0, 0x1111_1111_1111_1111, 0x1111_1111_1111_1111);
        context.SetXmmRegister(1, 0x2222_2222_2222_2222, 0x2222_2222_2222_2222);

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        context.GetXmmRegister(0, out var low, out var high);
        Assert.Equal(0x1111_1111_2222_2222UL, low);
        Assert.Equal(0x1111_1111_2222_2222UL, high);
    }

    [Fact]
    public void Execute_BlendsWordsWithVpblendw()
    {
        // vpblendw xmm0, xmm0, xmm1, 0Ah; ret
        var context = CreateContext([0xC4, 0xE3, 0x79, 0x0E, 0xC1, 0x0A, 0xC3]);
        context.SetXmmRegister(0, 0x1111_1111_1111_1111, 0x1111_1111_1111_1111);
        context.SetXmmRegister(1, 0x2222_2222_2222_2222, 0x2222_2222_2222_2222);

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        context.GetXmmRegister(0, out var low, out var high);
        Assert.Equal(0x2222_1111_2222_1111UL, low);
        Assert.Equal(0x1111_1111_1111_1111UL, high);
    }

    [Fact]
    public void Execute_ShufflesDwordsWithVpshufd()
    {
        // vpshufd xmm1, xmm1, 50h; ret
        var context = CreateContext([0xC5, 0xF9, 0x70, 0xC9, 0x50, 0xC3]);
        context.SetXmmRegister(1, 0x2222_2222_1111_1111, 0x4444_4444_3333_3333);

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        context.GetXmmRegister(1, out var low, out var high);
        Assert.Equal(0x1111_1111_1111_1111UL, low);
        Assert.Equal(0x2222_2222_2222_2222UL, high);
    }

    [Fact]
    public void Execute_ShiftsQwordLanesLeftByImmediateWithVpsllq()
    {
        // vpsllq ymm0, ymm0, 8; ret
        var context = CreateContext([0xC5, 0xFD, 0x73, 0xF0, 0x08, 0xC3]);
        context.SetYmmRegister(
            0,
            0x0000_0000_0000_0001,
            0x8000_0000_0000_0000,
            0xFFFF_FFFF_FFFF_FFFF,
            0x1234_5678_9ABC_DEF0);

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        context.GetYmmRegister(0, out var lowLow, out var lowHigh, out var highLow, out var highHigh);
        Assert.Equal(0x0000_0000_0000_0100UL, lowLow);
        Assert.Equal(0x0000_0000_0000_0000UL, lowHigh);
        Assert.Equal(0xFFFF_FFFF_FFFF_FF00UL, highLow);
        Assert.Equal(0x3456_789A_BCDE_F000UL, highHigh);
    }

    [Fact]
    public void Execute_ZeroExtendsDwordsToQwordsWithVpmovzxdq()
    {
        // vpmovzxdq ymm0, xmm0; ret
        var context = CreateContext([0xC4, 0xE2, 0x7D, 0x35, 0xC0, 0xC3]);
        context.SetXmmRegister(0, 0x2222_2222_1111_1111, 0x4444_4444_3333_3333);

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        context.GetYmmRegister(0, out var lowLow, out var lowHigh, out var highLow, out var highHigh);
        Assert.Equal(0x0000_0000_1111_1111UL, lowLow);
        Assert.Equal(0x0000_0000_2222_2222UL, lowHigh);
        Assert.Equal(0x0000_0000_3333_3333UL, highLow);
        Assert.Equal(0x0000_0000_4444_4444UL, highHigh);
    }

    [Fact]
    public void Execute_SignExtendsBytesToDwordsWithVpmovsxbd()
    {
        // vpmovsxbd xmm1, xmm0; ret
        var context = CreateContext([0xC4, 0xE2, 0x79, 0x21, 0xC8, 0xC3]);
        context.SetXmmRegister(0, 0x0000_0000_FF01_7F80, 0);

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        context.GetXmmRegister(1, out var low, out var high);
        Assert.Equal(0x0000_007F_FFFF_FF80UL, low);
        Assert.Equal(0xFFFF_FFFF_0000_0001UL, high);
    }

    [Fact]
    public void Execute_ExtractsDwordFromLaneWithVpextrd()
    {
        // vpextrd ecx, xmm1, 3; ret
        var context = CreateContext([0xC4, 0xE3, 0x79, 0x16, 0xC9, 0x03, 0xC3]);
        context.SetXmmRegister(1, 0x2222_2222_1111_1111, 0x4444_4444_3333_3333);

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        Assert.Equal(0x4444_4444UL, context[CpuRegister.Rcx]);
    }

    [Fact]
    public void Execute_InsertsDwordIntoLaneWithVpinsrd()
    {
        // vpinsrd xmm0, xmm1, edx, 2; ret
        var context = CreateContext([0xC4, 0xE3, 0x71, 0x22, 0xC2, 0x02, 0xC3]);
        context.SetXmmRegister(1, 0x1111_2222_3333_4444, 0x5555_6666_7777_8888);
        context[CpuRegister.Rdx] = 0xDEAD_BEEF;

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        context.GetXmmRegister(0, out var low, out var high);
        Assert.Equal(0x1111_2222_3333_4444UL, low);
        Assert.Equal(0x5555_6666_DEAD_BEEFUL, high);
    }

    [Fact]
    public void Execute_ExtractsHighQwordWithVpextrq()
    {
        // vpextrq rbx, xmm0, 1; ret
        var context = CreateContext([0xC4, 0xE3, 0xF9, 0x16, 0xC3, 0x01, 0xC3]);
        context.SetXmmRegister(0, 0x1111_2222_3333_4444, 0x5555_6666_7777_8888);
        context[CpuRegister.Rbx] = 0;

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        Assert.Equal(0x5555_6666_7777_8888UL, context[CpuRegister.Rbx]);
    }

    [Fact]
    public void Execute_ExtractsLowQwordWithVpextrq()
    {
        // vpextrq rbx, xmm0, 0; ret
        var context = CreateContext([0xC4, 0xE3, 0xF9, 0x16, 0xC3, 0x00, 0xC3]);
        context.SetXmmRegister(0, 0x1111_2222_3333_4444, 0x5555_6666_7777_8888);
        context[CpuRegister.Rbx] = ulong.MaxValue;

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        Assert.Equal(0x1111_2222_3333_4444UL, context[CpuRegister.Rbx]);
    }

    [Fact]
    public void Execute_AddsPackedDwordsWithVpaddd()
    {
        // vpaddd ymm5, ymm0, ymm1; ret
        var context = CreateContext([0xC5, 0xFD, 0xFE, 0xE9, 0xC3]);
        context.SetYmmRegister(
            0,
            0x1111_1111_1111_1111,
            0x1111_1111_1111_1111,
            0x1111_1111_1111_1111,
            0x1111_1111_1111_1111);
        context.SetYmmRegister(
            1,
            0x2222_2222_2222_2222,
            0x2222_2222_2222_2222,
            0x2222_2222_2222_2222,
            0x2222_2222_2222_2222);

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        context.GetYmmRegister(5, out var lowLow, out var lowHigh, out var highLow, out var highHigh);
        Assert.Equal(0x3333_3333_3333_3333UL, lowLow);
        Assert.Equal(0x3333_3333_3333_3333UL, lowHigh);
        Assert.Equal(0x3333_3333_3333_3333UL, highLow);
        Assert.Equal(0x3333_3333_3333_3333UL, highHigh);
    }

    [Fact]
    public void Execute_SubtractsPackedBytesWithVpsubbAndWrapsPerLane()
    {
        // vpsubb xmm0, xmm0, xmm1; ret
        var context = CreateContext([0xC5, 0xF9, 0xF8, 0xC1, 0xC3]);
        context.SetXmmRegister(0, 0x0000_0000_0000_0000, 0);
        context.SetXmmRegister(1, 0x0101_0101_0101_0101, 0);

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        context.GetXmmRegister(0, out var low, out var high);
        Assert.Equal(0xFFFF_FFFF_FFFF_FFFFUL, low);
        Assert.Equal(0UL, high);
    }

    [Fact]
    public void Execute_ZerosXmmWithVxorpd()
    {
        // vxorpd xmm0, xmm0, xmm0; ret
        var context = CreateContext([0xC5, 0xF9, 0x57, 0xC0, 0xC3]);
        context.SetYmmRegister(
            0,
            0x1111_2222_3333_4444,
            0x5555_6666_7777_8888,
            0x9999_AAAA_BBBB_CCCC,
            0xDDDD_EEEE_FFFF_0001);

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        context.GetXmmRegister(0, out var low, out var high);
        Assert.Equal(0UL, low);
        Assert.Equal(0UL, high);
    }

    [Fact]
    public void Execute_ComputesBitwiseAndWithVpand()
    {
        // vpand xmm0, xmm0, xmm1; ret
        var context = CreateContext([0xC5, 0xF9, 0xDB, 0xC1, 0xC3]);
        context.SetXmmRegister(0, 0xFF00_FF00_FF00_FF00, 0x0F0F_0F0F_0F0F_0F0F);
        context.SetXmmRegister(1, 0xFFFF_0000_FFFF_0000, 0xFFFF_FFFF_0000_0000);

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        context.GetXmmRegister(0, out var low, out var high);
        Assert.Equal(0xFF00_0000_FF00_0000UL, low);
        Assert.Equal(0x0F0F_0F0F_0000_0000UL, high);
    }

    [Fact]
    public void Execute_ShiftsDwordLanesRightWithVpsrlvd()
    {
        // vpsrlvd xmm0, xmm1, xmm2; ret
        var context = CreateContext([0xC4, 0xE2, 0x71, 0x45, 0xC2, 0xC3]);
        context.SetXmmRegister(1, 0x0000_0001_FFFF_FFFF, 0);
        context.SetXmmRegister(2, 0x0000_0021_0000_0004, 0);

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        context.GetXmmRegister(0, out var low, out var high);
        Assert.Equal(0x0000_0000_0FFF_FFFFUL, low);
        Assert.Equal(0UL, high);
    }

    [Fact]
    public void Execute_BroadcastsOctwordFromMemoryToYmmWithVbroadcastf128()
    {
        // vbroadcastf128 ymm0, [rdi]; ret
        var context = CreateContext([0xC4, 0xE2, 0x7D, 0x1A, 0x07, 0xC3]);
        var source = new byte[16];
        for (var i = 0; i < source.Length; i++)
        {
            source[i] = (byte)(0xA0 + i);
        }

        Assert.True(context.Memory.TryWrite(DataBase, source));
        context[CpuRegister.Rdi] = DataBase;

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        context.GetYmmRegister(0, out var lowLow, out var lowHigh, out var highLow, out var highHigh);
        Assert.Equal(0xA7A6_A5A4_A3A2_A1A0UL, lowLow);
        Assert.Equal(0xAFAE_ADAC_ABAA_A9A8UL, lowHigh);
        Assert.Equal(0xA7A6_A5A4_A3A2_A1A0UL, highLow);
        Assert.Equal(0xAFAE_ADAC_ABAA_A9A8UL, highHigh);
    }

    [Fact]
    public void Execute_BroadcastsDwordFromMemoryToXmmWithVpbroadcastd()
    {
        // vpbroadcastd xmm0, [rdi]; ret
        var context = CreateContext([0xC4, 0xE2, 0x79, 0x58, 0x07, 0xC3]);
        context[CpuRegister.Rdi] = DataBase;
        Assert.True(context.TryWriteUInt32(DataBase, 0x1234_5678));

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        context.GetXmmRegister(0, out var low, out var high);
        Assert.Equal(0x1234_5678_1234_5678UL, low);
        Assert.Equal(0x1234_5678_1234_5678UL, high);
    }

    [Fact]
    public void Execute_BroadcastsQwordToYmmWithVpbroadcastq()
    {
        // vpbroadcastq ymm0, xmm0; ret
        var context = CreateContext([0xC4, 0xE2, 0x7D, 0x59, 0xC0, 0xC3]);
        context.SetXmmRegister(0, 0x1234_5678_9ABC_DEF0, 0);

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        context.GetYmmRegister(0, out var lowLow, out var lowHigh, out var highLow, out var highHigh);
        Assert.Equal(0x1234_5678_9ABC_DEF0UL, lowLow);
        Assert.Equal(0x1234_5678_9ABC_DEF0UL, lowHigh);
        Assert.Equal(0x1234_5678_9ABC_DEF0UL, highLow);
        Assert.Equal(0x1234_5678_9ABC_DEF0UL, highHigh);
    }

    [Fact]
    public void Execute_BroadcastsDwordToXmmWithVbroadcastss()
    {
        // vbroadcastss xmm0, [rdi]; ret
        var context = CreateContext([0xC4, 0xE2, 0x79, 0x18, 0x07, 0xC3]);
        context[CpuRegister.Rdi] = DataBase;
        Assert.True(context.TryWriteUInt32(DataBase, 0x3F80_0000));
        context.SetYmmRegister(
            0,
            0x1111_2222_3333_4444,
            0x5555_6666_7777_8888,
            0x9999_AAAA_BBBB_CCCC,
            0xDDDD_EEEE_FFFF_0001);

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        context.GetYmmRegister(0, out var lowLow, out var lowHigh, out var highLow, out var highHigh);
        Assert.Equal(0x3F80_0000_3F80_0000UL, lowLow);
        Assert.Equal(0x3F80_0000_3F80_0000UL, lowHigh);
        Assert.Equal(0UL, highLow);
        Assert.Equal(0UL, highHigh);
    }

    [Fact]
    public void Execute_StoresYmmWithVmovdqu()
    {
        // vmovdqu [rdi+rcx-0x20], ymm0; ret
        var context = CreateContext([0xC5, 0xFE, 0x7F, 0x44, 0x0F, 0xE0, 0xC3]);
        context.SetYmmRegister(
            0,
            0x1111_2222_3333_4444,
            0x5555_6666_7777_8888,
            0x9999_AAAA_BBBB_CCCC,
            0xDDDD_EEEE_FFFF_0001);
        context[CpuRegister.Rdi] = DataBase;
        context[CpuRegister.Rcx] = 0x40;

        var result = Execute(context);

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        Assert.True(context.TryReadUInt64(DataBase + 0x20, out var first));
        Assert.True(context.TryReadUInt64(DataBase + 0x28, out var second));
        Assert.True(context.TryReadUInt64(DataBase + 0x30, out var third));
        Assert.True(context.TryReadUInt64(DataBase + 0x38, out var fourth));
        Assert.Equal(0x1111_2222_3333_4444UL, first);
        Assert.Equal(0x5555_6666_7777_8888UL, second);
        Assert.Equal(0x9999_AAAA_BBBB_CCCCUL, third);
        Assert.Equal(0xDDDD_EEEE_FFFF_0001UL, fourth);
    }

    [Fact]
    public void Execute_StopsCleanlyWhenImportRequestsEntryExit()
    {
        // ud2 would trap here if execution incorrectly continued past the exit request.
        var context = CreateContext([0x0F, 0x0B]);
        var importStubs = new Dictionary<ulong, string> { [CodeBase] = "exit" };
        var backend = new X64InterpreterBackend(new ExitingModuleManager());

        var result = backend.Execute(
            context,
            CodeBase,
            importStubs,
            new X64InterpreterOptions { MaxInstructions = 1000 });

        Assert.Equal(OrbisGen2Result.ORBIS_GEN2_OK, result.Result);
        Assert.Equal(CpuExitReason.Exited, result.Reason);
        Assert.Equal(7UL, context[CpuRegister.Rax]);
    }

    private sealed class ExitingModuleManager : IModuleManager
    {
        public int RegisterExports(IReadOnlyList<ExportedFunction> exports) => 0;

        public void Freeze()
        {
        }

        public bool TryGetFunction(string nid, out Delegate function)
        {
            function = null!;
            return false;
        }

        public bool TryGetExport(string nid, out ExportedFunction export)
        {
            export = null!;
            return false;
        }

        public bool TryGetExportByName(string exportName, out ExportedFunction export)
        {
            export = null!;
            return false;
        }

        public bool TryDispatch(string nid, CpuContext context, out OrbisGen2Result result)
        {
            result = Dispatch(nid, context);
            return true;
        }

        public OrbisGen2Result Dispatch(string nid, CpuContext context)
        {
            GuestThreadExecution.RequestCurrentEntryExit(nid, 7);
            return OrbisGen2Result.ORBIS_GEN2_OK;
        }
    }

    private static CpuContext CreateContext(byte[] code)
    {
        var memory = new VirtualMemory();
        memory.Map(CodeBase, 0x1000, 0, code, ProgramHeaderFlags.Read | ProgramHeaderFlags.Execute);
        memory.Map(DataBase, 0x1000, 0, ReadOnlySpan<byte>.Empty, ProgramHeaderFlags.Read | ProgramHeaderFlags.Write);
        memory.Map(StackBase, StackSize, 0, ReadOnlySpan<byte>.Empty, ProgramHeaderFlags.Read | ProgramHeaderFlags.Write);

        var context = new CpuContext(memory, Generation.Gen5)
        {
            Rip = CodeBase,
            Rflags = 0x202,
        };
        context[CpuRegister.Rsp] = StackBase + StackSize;
        Assert.True(context.PushUInt64(0));
        return context;
    }

    private static X64InterpreterResult Execute(CpuContext context)
    {
        var backend = new X64InterpreterBackend(new ModuleManager());
        return backend.Execute(
            context,
            CodeBase,
            new Dictionary<ulong, string>(),
            new X64InterpreterOptions { MaxInstructions = 1000 });
    }
}
