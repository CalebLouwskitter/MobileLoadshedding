using System;
using System.Collections.Generic;
using System.IO;
using IdleLoadshedding.Simulation;

namespace IdleLoadshedding.Session
{
    public enum LoadStatus
    {
        NewGame,
        Loaded,
        RecoveredFromBackup,
        /// <summary>No usable save could be chosen automatically; the player must pick an option.</summary>
        NeedsRecovery
    }

    public sealed class LoadOutcome
    {
        public LoadStatus Status;
        public SimState State;
        public long Revision;
        public string Notice = "";
        public DecodeStatus ProblemKind = DecodeStatus.Ok;
        public string Problem = "";
        /// <summary>A valid older copy the player can choose during recovery, if any.</summary>
        public SimState BackupState;
        public List<string> Quarantined = new List<string>();

        public bool BackupAvailable => BackupState != null;
    }

    public sealed class SaveException : Exception
    {
        public SaveException(string message) : base(message) { }
    }

    /// <summary>
    /// One local save slot: save.json (current), save.bak (previous good copy) and save.tmp
    /// (write in progress). Commits write and verify the temp file before touching the current
    /// file, so a crash at any point leaves at least one valid copy. Problem files are copied into
    /// recovery/ before anything else happens to them, and are never deleted from there.
    /// </summary>
    public sealed class SaveStore
    {
        public const string PrimaryName = "save.json";
        public const string BackupName = "save.bak";
        public const string TempName = "save.tmp";
        public const string RecoveryFolderName = "recovery";

        private readonly ISaveFileSystem _fs;
        private readonly GameContent _content;
        private readonly IClock _clock;
        private readonly string _slotId;
        private bool _primaryKnownBad;

        public SaveStore(ISaveFileSystem fs, string directory, GameContent content, IClock clock, string slotId = "slot-1")
        {
            _fs = fs ?? throw new ArgumentNullException(nameof(fs));
            _content = content ?? throw new ArgumentNullException(nameof(content));
            _clock = clock ?? throw new ArgumentNullException(nameof(clock));
            _slotId = slotId;
            Directory = directory;
            PrimaryPath = Path.Combine(directory, PrimaryName);
            BackupPath = Path.Combine(directory, BackupName);
            TempPath = Path.Combine(directory, TempName);
            RecoveryPath = Path.Combine(directory, RecoveryFolderName);
        }

        public string Directory { get; }
        public string PrimaryPath { get; }
        public string BackupPath { get; }
        public string TempPath { get; }
        public string RecoveryPath { get; }

        /// <summary>Revision of the last committed (or loaded) save. The next commit uses Revision + 1.</summary>
        public long Revision { get; private set; }

        private sealed class Candidate
        {
            public string Path;
            public string Name;
            public DecodeResult Result;
            public bool Ok => Result.Status == DecodeStatus.Ok;
        }

        public LoadOutcome Load()
        {
            var outcome = new LoadOutcome();
            var primary = Inspect(PrimaryPath, "save");
            var temp = Inspect(TempPath, "tmp");
            var backup = Inspect(BackupPath, "bak");

            long maxRevision = 0;
            foreach (var c in new[] { primary, temp, backup })
                if (c != null) maxRevision = Math.Max(maxRevision, c.Result.Revision);

            foreach (var c in new[] { primary, temp, backup })
            {
                if (c != null && !c.Ok)
                    outcome.Quarantined.Add(Quarantine(c.Path, c.Name, c.Result.Status));
            }

            // A temp file that does not decode is debris from an interrupted write. It has been
            // copied to recovery/, so remove it from the slot.
            if (temp != null && !temp.Ok)
            {
                _fs.Delete(TempPath);
                temp = null;
            }

            _primaryKnownBad = primary != null && !primary.Ok;

            if (primary != null && (primary.Result.Status == DecodeStatus.FutureSchema || primary.Result.Status == DecodeStatus.Unsupported))
            {
                outcome.Status = LoadStatus.NeedsRecovery;
                outcome.ProblemKind = primary.Result.Status;
                outcome.Problem = primary.Result.Detail;
                var fallback = Best(backup, temp);
                outcome.BackupState = fallback?.Result.State;
                Revision = maxRevision;
                return outcome;
            }

            var best = Best(primary, temp);
            if (best != null)
            {
                if (best == temp)
                {
                    FinishInterruptedCommit(primaryOk: primary != null && primary.Ok);
                    outcome.Notice = "Finished saving progress from your last session.";
                }
                else if (temp != null)
                {
                    _fs.Delete(TempPath);
                }
                outcome.Status = LoadStatus.Loaded;
                outcome.State = best.Result.State;
                Revision = maxRevision;
                outcome.Revision = Revision;
                return outcome;
            }

            if (backup != null && backup.Ok)
            {
                outcome.Status = LoadStatus.RecoveredFromBackup;
                outcome.State = backup.Result.State;
                outcome.ProblemKind = primary?.Result.Status ?? DecodeStatus.Ok;
                outcome.Problem = primary?.Result.Detail ?? "save file missing";
                outcome.Notice = "Your latest save could not be read, so the previous copy was restored. The damaged file was kept for recovery.";
                Revision = maxRevision;
                outcome.Revision = Revision;
                return outcome;
            }

            if (primary == null && backup == null)
            {
                outcome.Status = LoadStatus.NewGame;
                Revision = maxRevision;
                return outcome;
            }

            outcome.Status = LoadStatus.NeedsRecovery;
            outcome.ProblemKind = primary?.Result.Status ?? backup.Result.Status;
            outcome.Problem = primary?.Result.Detail ?? backup.Result.Detail;
            Revision = maxRevision;
            return outcome;
        }

        /// <summary>
        /// Writes the state as revision + 1. Throws on any failure, in which case <see cref="Revision"/>
        /// is unchanged and the previous valid save is still recoverable.
        /// </summary>
        public void Commit(SimState state)
        {
            long revision = Revision + 1;
            var bytes = SaveCodec.Encode(_content, state, revision, _clock.UtcNowMs, _slotId);

            _fs.CreateDirectory(Directory);
            _fs.WriteAllBytesDurable(TempPath, bytes);

            var check = SaveCodec.Decode(_content, _fs.ReadAllBytes(TempPath));
            if (check.Status != DecodeStatus.Ok || check.Revision != revision)
                throw new SaveException("Save verification failed: " + check.Status + " " + check.Detail);

            if (_fs.Exists(PrimaryPath))
            {
                if (_primaryKnownBad)
                {
                    // Already copied to recovery/ during Load; keep the older good backup instead.
                    _fs.Delete(PrimaryPath);
                }
                else
                {
                    if (_fs.Exists(BackupPath)) _fs.Delete(BackupPath);
                    _fs.Move(PrimaryPath, BackupPath);
                }
            }
            _fs.Move(TempPath, PrimaryPath);

            Revision = revision;
            _primaryKnownBad = false;
        }

        private void FinishInterruptedCommit(bool primaryOk)
        {
            if (_fs.Exists(PrimaryPath))
            {
                if (primaryOk)
                {
                    if (_fs.Exists(BackupPath)) _fs.Delete(BackupPath);
                    _fs.Move(PrimaryPath, BackupPath);
                }
                else
                {
                    _fs.Delete(PrimaryPath);
                }
            }
            _fs.Move(TempPath, PrimaryPath);
            _primaryKnownBad = false;
        }

        private Candidate Inspect(string path, string name)
        {
            if (!_fs.Exists(path)) return null;
            DecodeResult result;
            try
            {
                result = SaveCodec.Decode(_content, _fs.ReadAllBytes(path));
            }
            catch (IOException e)
            {
                result = new DecodeResult { Status = DecodeStatus.Corrupt, Detail = "unreadable: " + e.Message };
            }
            return new Candidate { Path = path, Name = name, Result = result };
        }

        private static Candidate Best(Candidate a, Candidate b)
        {
            Candidate best = null;
            foreach (var c in new[] { a, b })
            {
                if (c == null || !c.Ok) continue;
                if (best == null || c.Result.Revision > best.Result.Revision) best = c;
            }
            return best;
        }

        private string Quarantine(string path, string name, DecodeStatus status)
        {
            _fs.CreateDirectory(RecoveryPath);
            string reason = status.ToString().ToLowerInvariant();
            string stem = name + "-" + _clock.UtcNowMs + "-" + reason;
            string destination = Path.Combine(RecoveryPath, stem + ".json");
            for (int i = 1; _fs.Exists(destination); i++)
                destination = Path.Combine(RecoveryPath, stem + "-" + i + ".json");
            _fs.Copy(path, destination);
            return destination;
        }
    }
}
