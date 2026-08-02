// Copyright (C) 2026 SharpEmu Emulator Project
// SPDX-License-Identifier: GPL-2.0-or-later

using System.Diagnostics;
using SharpEmu.HLE;

namespace SharpEmu.Core.Cpu.Interpreter;

/// <summary>
/// <see cref="IGuestThreadScheduler"/> for the x64 interpreter backend.
/// Guest threads map 1:1 onto real host threads, each running its own
/// <see cref="X64InterpreterBackend"/> loop against a fresh
/// <see cref="CpuContext"/> that shares the session's guest address space.
/// Unlike <c>DirectExecutionBackend</c> (which busy-polls for joins because
/// natively executed guest code cannot be paused mid-instruction to
/// register a wake callback), the interpreter loop is a normal managed
/// method, so a join is just a host <see cref="Thread.Join()"/>.
/// </summary>
public sealed class X64InterpreterGuestThreadScheduler : IGuestThreadScheduler
{
    private enum GuestThreadRunState
    {
        Running,
        Exited,
        Faulted,
    }

    private sealed class GuestThreadState
    {
        public required ulong ThreadHandle { get; init; }

        public required string Name { get; init; }

        public required CpuContext Context { get; init; }

        public Thread? HostThread { get; set; }

        public volatile GuestThreadRunState RunState = GuestThreadRunState.Running;

        public ulong ExitValue;

        public string? FaultReason;

        public string? RecentInstructions;
    }

    private readonly object _gate = new();
    private readonly Dictionary<ulong, GuestThreadState> _threads = new();
    private readonly CpuDispatcher _dispatcher;
    private readonly IModuleManager _moduleManager;
    private readonly IReadOnlyDictionary<ulong, string> _importStubs;
    private readonly X64InterpreterOptions _options;

    // Backs GuestThreadExecution.RequestCurrentThreadBlock/WakeBlockedThreads
    // for this backend. HLE exports (sceKernelWaitSema, mutex/condvar waits,
    // ...) call RequestCurrentThreadBlock expecting the active scheduler to
    // actually suspend the calling thread until WakeBlockedThreads(wakeKey)
    // is called for it (or its deadline passes); DirectExecutionBackend does
    // this via a continuation-replay queue because it can't pause natively
    // executing code mid-instruction. This backend doesn't need that
    // complexity: each guest thread already is a real, blockable host
    // Thread, so waiting is just a real wait on a per-wakeKey signal.
    private readonly object _blockGate = new();
    private readonly Dictionary<string, List<ManualResetEventSlim>> _blockedByWakeKey = new();

    public X64InterpreterGuestThreadScheduler(
        CpuDispatcher dispatcher,
        IModuleManager moduleManager,
        IReadOnlyDictionary<ulong, string> importStubs,
        X64InterpreterOptions options)
    {
        _dispatcher = dispatcher;
        _moduleManager = moduleManager;
        _importStubs = importStubs;
        _options = options;
    }

    public bool SupportsGuestContextTransfer => false;

    public void RegisterGuestThreadContext(ulong threadHandle, CpuContext context)
    {
        lock (_gate)
        {
            if (!_threads.ContainsKey(threadHandle))
            {
                _threads[threadHandle] = new GuestThreadState
                {
                    ThreadHandle = threadHandle,
                    Name = "primary",
                    Context = context,
                };
            }
        }
    }

    public bool TryStartThread(CpuContext creatorContext, GuestThreadStartRequest request, out string? error)
    {
        var stackBase = _dispatcher.TryMapStackRegion();
        if (stackBase == 0)
        {
            error = "failed to map guest thread stack";
            return false;
        }

        var tlsBase = _dispatcher.TryMapTlsRegion();
        if (tlsBase == 0)
        {
            error = "failed to map guest thread TLS";
            return false;
        }

        var innerMemory = creatorContext.Memory is ICpuMemoryWrapper wrapper ? wrapper.Inner : creatorContext.Memory;
        var context = new CpuContext(new TrackedCpuMemory(innerMemory), creatorContext.TargetGeneration)
        {
            Rip = request.EntryPoint,
            Rflags = 0x202,
            FsBase = tlsBase,
            GsBase = tlsBase,
        };
        context[CpuRegister.Rsp] = stackBase + CpuDispatcher.StackSize - sizeof(ulong);

        // A literal 0 return address lets the interpreter's existing
        // "Rip == 0" clean-exit check (see X64InterpreterBackend.Execute)
        // detect a natural `ret` out of the thread routine without any new
        // sentinel-trapping machinery.
        if (!context.TryWriteUInt64(context[CpuRegister.Rsp], 0) ||
            !CpuDispatcher.InitializeGuestFrameChainSentinel(context) ||
            !CpuDispatcher.InitializeTls(context, tlsBase))
        {
            error = "failed to initialize guest thread context";
            return false;
        }

        context[CpuRegister.Rdi] = request.Argument;

        var state = new GuestThreadState
        {
            ThreadHandle = request.ThreadHandle,
            Name = string.IsNullOrWhiteSpace(request.Name) ? $"thread-0x{request.ThreadHandle:X}" : request.Name,
            Context = context,
        };

        lock (_gate)
        {
            _threads[request.ThreadHandle] = state;
        }

        var hostThread = new Thread(() => RunGuestThread(state))
        {
            IsBackground = true,
            Name = state.Name,
        };
        state.HostThread = hostThread;
        hostThread.Start();

        error = null;
        return true;
    }

    private void RunGuestThread(GuestThreadState state)
    {
        var previousHandle = GuestThreadExecution.EnterGuestThread(state.ThreadHandle);
        try
        {
            var backend = new X64InterpreterBackend(_moduleManager, this);
            var result = backend.Execute(state.Context, state.Context.Rip, _importStubs, _options);
            state.ExitValue = state.Context[CpuRegister.Rax];
            state.RunState = result.Result == OrbisGen2Result.ORBIS_GEN2_OK
                ? GuestThreadRunState.Exited
                : GuestThreadRunState.Faulted;
            if (state.RunState == GuestThreadRunState.Faulted)
            {
                state.FaultReason = result.NotImplementedInfo?.Detail
                    ?? (result.MemoryFaultInfo is { } memoryFault
                        ? $"memory fault rip=0x{memoryFault.InstructionPointer:X16} {(memoryFault.Access.IsWrite ? "write" : "read")}@0x{memoryFault.Access.Address:X16} size={memoryFault.Access.Size}"
                        : result.TrapInfo is { } trap
                            ? $"trap rip=0x{trap.InstructionPointer:X16} opcode=0x{trap.Opcode:X2}"
                            : result.Reason.ToString());
                state.RecentInstructions = result.RecentInstructions;
            }
        }
        catch (Exception ex)
        {
            state.RunState = GuestThreadRunState.Faulted;
            state.FaultReason = $"{ex.GetType().Name}: {ex.Message}";
        }
        finally
        {
            GuestThreadExecution.RestoreGuestThread(previousHandle);
        }
    }

    public bool TryJoinThread(CpuContext callerContext, ulong threadHandle, out ulong returnValue, out string? error)
    {
        returnValue = 0;
        GuestThreadState? state;
        lock (_gate)
        {
            _threads.TryGetValue(threadHandle, out state);
        }

        if (state is null)
        {
            error = "unknown thread handle";
            return false;
        }

        if (ReferenceEquals(state.Context, callerContext))
        {
            error = "thread cannot join itself";
            return false;
        }

        state.HostThread?.Join();

        // Printed here (on the joining thread, which is guaranteed to still
        // be alive) rather than from the worker thread itself: the worker
        // runs as a background thread, and diagnostics written right before
        // it exits can be lost if the process tears down concurrently.
        if (state.RunState == GuestThreadRunState.Faulted)
        {
            Console.Error.WriteLine(
                $"[LOADER][ERROR] guest thread '{state.Name}' (0x{state.ThreadHandle:X}) stopped: {state.FaultReason} last_rip=0x{state.Context.Rip:X16}");
            if (!string.IsNullOrEmpty(state.RecentInstructions))
            {
                Console.Error.WriteLine(
                    $"[LOADER][ERROR] guest thread '{state.Name}' recent instructions:{Environment.NewLine}{state.RecentInstructions}");
            }
        }

        returnValue = state.ExitValue;
        error = state.RunState == GuestThreadRunState.Faulted ? state.FaultReason : null;
        return true;
    }

    public void Pump(CpuContext callerContext, string reason)
    {
    }

    public int WakeBlockedThreads(string wakeKey, int maxCount = int.MaxValue)
    {
        lock (_blockGate)
        {
            if (!_blockedByWakeKey.TryGetValue(wakeKey, out var waiters) || waiters.Count == 0)
            {
                return 0;
            }

            var count = Math.Min(maxCount, waiters.Count);
            for (var i = 0; i < count; i++)
            {
                waiters[i].Set();
            }

            return count;
        }
    }

    /// <summary>
    /// Actually blocks the calling host thread until <see cref="WakeBlockedThreads"/>
    /// is called for <paramref name="wakeKey"/> or <paramref name="deadlineTimestamp"/>
    /// (a <see cref="Stopwatch.GetTimestamp"/>-scale value, 0 = wait forever) passes.
    /// Returns true if woken by a signal, false on timeout.
    /// </summary>
    internal bool TryBlockCurrentThread(string wakeKey, long deadlineTimestamp)
    {
        using var signal = new ManualResetEventSlim(false);
        lock (_blockGate)
        {
            if (!_blockedByWakeKey.TryGetValue(wakeKey, out var waiters))
            {
                waiters = new List<ManualResetEventSlim>();
                _blockedByWakeKey[wakeKey] = waiters;
            }

            waiters.Add(signal);
        }

        try
        {
            if (deadlineTimestamp <= 0)
            {
                signal.Wait();
                return true;
            }

            var remainingTicks = deadlineTimestamp - Stopwatch.GetTimestamp();
            if (remainingTicks <= 0)
            {
                return signal.IsSet;
            }

            var remainingMs = remainingTicks * 1000.0 / Stopwatch.Frequency;
            var waitMs = remainingMs >= int.MaxValue ? int.MaxValue : (int)remainingMs;
            return signal.Wait(Math.Max(0, waitMs));
        }
        finally
        {
            lock (_blockGate)
            {
                if (_blockedByWakeKey.TryGetValue(wakeKey, out var waiters))
                {
                    waiters.Remove(signal);
                    if (waiters.Count == 0)
                    {
                        _blockedByWakeKey.Remove(wakeKey);
                    }
                }
            }
        }
    }

    public bool TrySetGuestThreadPriority(ulong guestThreadHandle, int guestPriority) => true;

    public bool TrySetGuestThreadAffinity(ulong guestThreadHandle, ulong affinityMask) => true;

    public IReadOnlyList<GuestThreadSnapshot> SnapshotThreads()
    {
        lock (_gate)
        {
            var snapshots = new List<GuestThreadSnapshot>(_threads.Count);
            foreach (var state in _threads.Values)
            {
                snapshots.Add(new GuestThreadSnapshot(
                    state.ThreadHandle,
                    state.Name,
                    state.RunState.ToString(),
                    ImportCount: 0,
                    LastImportNid: null,
                    LastReturnRip: state.Context.Rip,
                    BlockReason: null));
            }

            return snapshots;
        }
    }

    public bool TryCallGuestFunction(
        CpuContext callerContext,
        ulong entryPoint,
        ulong arg0,
        ulong arg1,
        ulong stackAddress,
        ulong stackSize,
        string reason,
        out string? error)
    {
        error = "guest function calls are not supported by the interpreter scheduler yet";
        return false;
    }

    public bool TryCallGuestFunction(
        CpuContext callerContext,
        ulong entryPoint,
        ulong arg0,
        ulong arg1,
        ulong arg2,
        ulong stackAddress,
        ulong stackSize,
        string reason,
        out ulong returnValue,
        out string? error)
    {
        returnValue = 0;
        error = "guest function calls are not supported by the interpreter scheduler yet";
        return false;
    }

    public bool TryCallGuestContinuation(
        CpuContext callerContext,
        GuestCpuContinuation continuation,
        string reason,
        out string? error)
    {
        error = "guest context transfer is not supported by the interpreter scheduler";
        return false;
    }

    public bool TryRaiseGuestException(
        CpuContext callerContext,
        ulong threadHandle,
        ulong handler,
        int exceptionType,
        out string? error)
    {
        error = "guest exception injection is not supported by the interpreter scheduler yet";
        return false;
    }
}
