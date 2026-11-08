using System;
using IdleLoadshedding.Simulation;

namespace IdleLoadshedding.Session
{
    /// <summary>
    /// The single writer. Every change to the town goes through here: player commands are applied
    /// to a copy and only become live after the save commits, passive ticks run on a fixed-tick
    /// accumulator that ignores render frame rate, and elapsed real time is settled once against
    /// the saved watermark.
    /// </summary>
    public sealed class SessionCoordinator
    {
        public const string NotReady = "not_ready";
        public const string Busy = "busy";
        public const string SaveFailed = "save_failed";

        private readonly GameContent _content;
        private readonly SaveStore _store;
        private readonly IClock _clock;

        private SimState _state;
        private LoadOutcome _load;
        private long _runAccumulator;
        private long _lastCheckpointUtcMs;
        private bool _dirty;
        private bool _busy;
        private int _version;
        private string _notice = "";

        public SessionCoordinator(GameContent content, SaveStore store, IClock clock)
        {
            _content = content ?? throw new ArgumentNullException(nameof(content));
            _store = store ?? throw new ArgumentNullException(nameof(store));
            _clock = clock ?? throw new ArgumentNullException(nameof(clock));
            Snapshot = SessionSnapshot.Build(content, null, 0, BootState.NotStarted, false, 0, "", "", false);
        }

        public SessionSnapshot Snapshot { get; private set; }
        public BootState BootState { get; private set; } = BootState.NotStarted;
        public bool InputEnabled => BootState == BootState.Ready && !_busy;
        public string LastError { get; private set; } = "";
        public long Revision => _store.Revision;
        public GameContent Content => _content;

        /// <summary>Optional diagnostics sink (Unity routes it to the console and logcat).</summary>
        public Action<string> Log;

        /// <summary>A copy of the live state, for tests and diagnostics.</summary>
        public SimState CopyState() => _state?.Clone();

        /// <summary>Load, validate, settle offline time and commit — only then is input enabled.</summary>
        public void Boot()
        {
            if (BootState == BootState.Ready) return;
            try
            {
                _load = _store.Load();
            }
            catch (Exception e)
            {
                Fail("Could not read the save: " + e.Message);
                return;
            }

            switch (_load.Status)
            {
                case LoadStatus.NewGame:
                    CommitBoot(GridRules.NewGame(_content, _clock.UtcNowMs), settle: false, notice: "");
                    break;
                case LoadStatus.Loaded:
                case LoadStatus.RecoveredFromBackup:
                    CommitBoot(_load.State, settle: true, notice: _load.Notice);
                    break;
                case LoadStatus.NeedsRecovery:
                    BootState = BootState.NeedsRecovery;
                    Write("boot needs recovery: " + _load.ProblemKind + " " + _load.Problem);
                    Publish();
                    break;
            }
        }

        public LoadOutcome LastLoad => _load;

        /// <summary>Resolves a recovery prompt. The problem file is already preserved in recovery/.</summary>
        public void ResolveRecovery(bool useBackup)
        {
            if (BootState != BootState.NeedsRecovery) return;
            if (useBackup && _load.BackupState != null)
                CommitBoot(_load.BackupState, settle: true, notice: "Restored the previous copy of your town.");
            else
                CommitBoot(GridRules.NewGame(_content, _clock.UtcNowMs), settle: false, notice: "Started a new town. Your old save was kept for recovery.");
        }

        public CommandResult Submit(GameCommand command)
        {
            if (BootState != BootState.Ready) return CommandResult.No(NotReady);
            if (_busy) return CommandResult.No(Busy);
            _busy = true;
            try
            {
                long now = _clock.UtcNowMs;
                var work = _state.Clone();
                // Bring passive time up to date so the command sees current balances.
                OfflineSettlement.Settle(_content, work, now, createReceipt: false);
                var result = CommandRules.Apply(_content, work, command);
                if (!result.Accepted) return result;

                try
                {
                    _store.Commit(work);
                }
                catch (Exception e)
                {
                    LastError = "Could not save: " + e.Message;
                    Write("commit failed for " + command + ": " + e.Message);
                    return CommandResult.No(SaveFailed);
                }

                _state = work;
                _dirty = false;
                _lastCheckpointUtcMs = now;
                _runAccumulator = 0;
                _notice = SuccessNotice(command);
                Publish();
                return result;
            }
            finally
            {
                _busy = false;
            }
        }

        /// <summary>Advances the town for one rendered frame.</summary>
        public void Update(long frameDeltaMs)
        {
            if (BootState != BootState.Ready) return;
            long now = _clock.UtcNowMs;
            long dt = Math.Max(0, Math.Min(frameDeltaMs, _content.Timing.MaxFrameDeltaMs));
            var phaseBefore = _state.Phase;
            int cutsBefore = _state.CutsCompleted;
            bool ticked = false;

            int rate = ActiveTickRate(_state);
            if (rate > 0)
            {
                int maxTicks = _content.Timing.MaxTicksPerFrame;
                _runAccumulator = Math.Min(_runAccumulator + dt * rate, 1000L * maxTicks);
                long ticks = _runAccumulator / 1000;
                _runAccumulator -= ticks * 1000;
                for (long i = 0; i < ticks; i++)
                {
                    var outcome = GridRules.Step(_content, _state);
                    if (outcome == StepOutcome.Frozen) break;
                    ticked |= outcome != StepOutcome.GateReached;
                    if (outcome != StepOutcome.Ticked || ActiveTickRate(_state) != rate)
                    {
                        _runAccumulator = 0;
                        break;
                    }
                }
                // Real time spent watching a run is consumed rather than banked for idle play.
                if (_state.WatermarkUtcMs < now) _state.WatermarkUtcMs = now;
                _dirty = true;
            }
            else
            {
                _runAccumulator = 0;
                var settled = OfflineSettlement.Settle(_content, _state, now, createReceipt: false);
                if (settled.Ticks > 0)
                {
                    ticked = true;
                    _dirty = true;
                }
            }

            bool phaseChanged = _state.Phase != phaseBefore || _state.CutsCompleted != cutsBefore;
            if (phaseChanged || (_dirty && now - _lastCheckpointUtcMs >= _content.Timing.CheckpointIntervalMs))
                Checkpoint();

            if (ticked || phaseChanged) Publish();
        }

        /// <summary>App going to the background: settle and save so a process kill loses nothing.</summary>
        public void Pause()
        {
            if (BootState != BootState.Ready) return;
            _runAccumulator = 0;
            OfflineSettlement.Settle(_content, _state, _clock.UtcNowMs, createReceipt: false);
            _dirty = true;
            Checkpoint();
        }

        /// <summary>App returning to the foreground: award eligible time once and show a receipt.</summary>
        public void Resume()
        {
            if (BootState != BootState.Ready) return;
            _runAccumulator = 0;
            var settled = OfflineSettlement.Settle(_content, _state, _clock.UtcNowMs, createReceipt: true);
            if (settled.Changed && !settled.ConsumedOnly) _dirty = true;
            if (_dirty) Checkpoint();
            Publish();
        }

        /// <summary>Saves passive progress if anything changed. Returns false if the write failed.</summary>
        public bool Checkpoint()
        {
            if (BootState != BootState.Ready || !_dirty) return true;
            try
            {
                _store.Commit(_state);
                _dirty = false;
                _lastCheckpointUtcMs = _clock.UtcNowMs;
                return true;
            }
            catch (Exception e)
            {
                LastError = "Could not save: " + e.Message;
                Write("checkpoint failed: " + e.Message);
                return false;
            }
        }

        private int ActiveTickRate(SimState s)
        {
            if (s.Phase == Phase.Running && s.CutManual) return _content.Timing.RunTicksPerSecond;
            if (s.Phase == Phase.Normal && s.FastForward) return _content.Timing.SkipTicksPerSecond;
            return 0;
        }

        private void CommitBoot(SimState loaded, bool settle, string notice)
        {
            var work = loaded.Clone();
            if (settle) OfflineSettlement.Settle(_content, work, _clock.UtcNowMs, createReceipt: true);
            try
            {
                _store.Commit(work);
            }
            catch (Exception e)
            {
                Fail("Could not save: " + e.Message);
                return;
            }
            _state = work;
            _dirty = false;
            _lastCheckpointUtcMs = _clock.UtcNowMs;
            BootState = BootState.Ready;
            _notice = notice ?? "";
            Write("boot rev=" + _store.Revision + " status=" + (_load?.Status.ToString() ?? "new") + " " + Describe(work));
            Publish();
        }

        private void Fail(string message)
        {
            LastError = message;
            BootState = BootState.SaveError;
            Write(message);
            Publish();
        }

        private void Publish()
        {
            _version++;
            Snapshot = SessionSnapshot.Build(_content, _state, _version, BootState, BootState == BootState.Ready, _store.Revision, _notice, LastError, includeForecasts: true);
            if (BootState == BootState.NeedsRecovery) Snapshot.Recovery = _load;
            _notice = "";
        }

        private void Write(string message) => Log?.Invoke(message);

        /// <summary>One-line summary for logs and the device smoke test.</summary>
        public string Describe(SimState s)
        {
            if (s == null) return "no state";
            var upgrades = new System.Text.StringBuilder();
            foreach (var u in s.Upgrades)
            {
                if (upgrades.Length > 0) upgrades.Append('+');
                upgrades.Append(u.Id).Append('x').Append(u.Count);
            }
            var lights = new System.Text.StringBuilder();
            for (int i = 0; i < _content.DistrictCount; i++)
            {
                if (lights.Length > 0) lights.Append(',');
                lights.Append(_content.Districts[i].Id).Append(':').Append(s.Powered[i] ? '1' : '0');
            }
            string policy = s.Policy == null ? "none" : PlanKinds.ToId(s.Policy.Plan) + "/" + string.Join(">", s.Policy.Priority);
            return "phase=" + StateJson.PhaseId(s.Phase) + " tick=" + s.Tick + " cash=" + s.CashCents +
                   " upgrades=" + (upgrades.Length == 0 ? "none" : upgrades.ToString()) + " policy=" + policy +
                   " lights=" + lights + " hash=" + StateHash.Simulation(_content, s);
        }

        private static string SuccessNotice(GameCommand command)
        {
            switch (command.Type)
            {
                case CommandType.BuyUpgrade: return "Upgrade installed and saved.";
                case CommandType.SavePolicy: return "Standing policy saved.";
                default: return "";
            }
        }
    }
}
