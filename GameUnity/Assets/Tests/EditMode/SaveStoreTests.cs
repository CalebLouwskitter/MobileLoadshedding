using System;
using System.IO;
using System.Linq;
using System.Text;
using IdleLoadshedding.Session;
using IdleLoadshedding.Simulation;
using NUnit.Framework;

namespace IdleLoadshedding.Tests
{
    /// <summary>Acceptance 5 (part): interrupted writes, corrupt and newer-schema saves.</summary>
    public class SaveStoreTests
    {
        private const string Dir = "/data/saves";
        private GameContent _c;
        private FakeClock _clock;

        [SetUp]
        public void SetUp()
        {
            _c = TestContent.Load();
            _clock = new FakeClock(Sim.T0);
        }

        private SaveStore Store(ISaveFileSystem fs) => new SaveStore(fs, Dir, _c, _clock);

        private SimState StateA() => Sim.NewGame(_c);

        private SimState StateB()
        {
            var s = Sim.NewGame(_c);
            Sim.RunCut(_c, s, Sim.Plan(_c, PlanKind.Battery));
            CommandRules.Apply(_c, s, GameCommand.BuyUpgrade("b", "upgrade.battery-module"));
            return s;
        }

        [Test]
        public void RoundTripsOnARealFileSystemAndKeepsThePreviousCopy()
        {
            string dir = Path.Combine(Path.GetTempPath(), "ils-save-" + Guid.NewGuid().ToString("N"));
            try
            {
                var fs = new LocalFileSystem();
                var store = new SaveStore(fs, dir, _c, _clock);
                Assert.That(store.Load().Status, Is.EqualTo(LoadStatus.NewGame));
                store.Commit(StateA());
                store.Commit(StateB());
                Assert.That(store.Revision, Is.EqualTo(2));
                Assert.That(File.Exists(Path.Combine(dir, SaveStore.PrimaryName)), Is.True);
                Assert.That(File.Exists(Path.Combine(dir, SaveStore.BackupName)), Is.True);
                Assert.That(File.Exists(Path.Combine(dir, SaveStore.TempName)), Is.False);

                var reloaded = new SaveStore(fs, dir, _c, _clock).Load();
                Assert.That(reloaded.Status, Is.EqualTo(LoadStatus.Loaded));
                Assert.That(reloaded.Revision, Is.EqualTo(2));
                Assert.That(StateHash.Full(_c, reloaded.State), Is.EqualTo(StateHash.Full(_c, StateB())));

                var previous = SaveCodec.Decode(_c, File.ReadAllBytes(Path.Combine(dir, SaveStore.BackupName)));
                Assert.That(previous.Revision, Is.EqualTo(1));
            }
            finally
            {
                if (Directory.Exists(dir)) Directory.Delete(dir, true);
            }
        }

        [Test]
        public void CrashAtAnyStepOfACommitLeavesALoadableValidSave()
        {
            string hashA = StateHash.Full(_c, StateA());
            string hashB = StateHash.Full(_c, StateB());
            int crashPoints = 0;

            for (int failAt = 0; failAt < 20; failAt++)
            {
                var fs = new FaultyFileSystem();
                var store = Store(fs);
                store.Load();
                store.Commit(StateA());   // revision 1
                store.Commit(StateA());   // revision 2, so a backup exists too

                fs.Operations = 0;
                fs.FailAtOperation = failAt;
                bool committed;
                try
                {
                    store.Commit(StateB());
                    committed = true;
                }
                catch (IOException)
                {
                    committed = false;
                    crashPoints++;
                }
                fs.FailAtOperation = -1;

                var outcome = Store(fs).Load();
                Assert.That(outcome.Status, Is.EqualTo(LoadStatus.Loaded).Or.EqualTo(LoadStatus.RecoveredFromBackup), "crash at op " + failAt);
                string hash = StateHash.Full(_c, outcome.State);
                if (committed) Assert.That(hash, Is.EqualTo(hashB), "a completed commit must survive");
                else Assert.That(hash, Is.EqualTo(hashA).Or.EqualTo(hashB), "crash at op " + failAt);

                // The slot is usable again: the next commit and load succeed.
                var next = Store(fs);
                next.Load();
                next.Commit(StateB());
                Assert.That(Store(fs).Load().Status, Is.EqualTo(LoadStatus.Loaded));
            }
            Assert.That(crashPoints, Is.GreaterThanOrEqualTo(4), "write, delete, two renames were all exercised");
        }

        [Test]
        public void CrashAfterTheTempFileIsVerifiedKeepsTheNewProgress()
        {
            var fs = new InMemoryFileSystem();
            var store = Store(fs);
            store.Load();
            store.Commit(StateA());
            // Simulate: temp written and flushed, then the process died before the renames.
            fs.Files[Path.Combine(Dir, SaveStore.TempName)] = SaveCodec.Encode(_c, StateB(), 2, Sim.T0, "slot-1");

            var outcome = Store(fs).Load();
            Assert.That(outcome.Status, Is.EqualTo(LoadStatus.Loaded));
            Assert.That(StateHash.Full(_c, outcome.State), Is.EqualTo(StateHash.Full(_c, StateB())));
            Assert.That(fs.Exists(Path.Combine(Dir, SaveStore.TempName)), Is.False, "interrupted commit was finished");
            Assert.That(SaveCodec.Decode(_c, fs.Files[Path.Combine(Dir, SaveStore.PrimaryName)]).Revision, Is.EqualTo(2));
            Assert.That(SaveCodec.Decode(_c, fs.Files[Path.Combine(Dir, SaveStore.BackupName)]).Revision, Is.EqualTo(1));
        }

        [TestCase("truncated")]
        [TestCase("garbage")]
        [TestCase("tampered")]
        [TestCase("invalid")]
        public void DamagedSaveFallsBackToThePreviousCopyAndIsKept(string damage)
        {
            var fs = new InMemoryFileSystem();
            var store = Store(fs);
            store.Load();
            store.Commit(StateA());
            store.Commit(StateB());
            string primary = Path.Combine(Dir, SaveStore.PrimaryName);
            byte[] damaged = Damage(fs.Files[primary], damage);
            fs.Files[primary] = damaged;

            var outcome = Store(fs).Load();
            Assert.That(outcome.Status, Is.EqualTo(LoadStatus.RecoveredFromBackup), damage);
            Assert.That(outcome.ProblemKind, Is.EqualTo(DecodeStatus.Corrupt));
            Assert.That(StateHash.Full(_c, outcome.State), Is.EqualTo(StateHash.Full(_c, StateA())));
            Assert.That(outcome.Quarantined.Count, Is.EqualTo(1));
            Assert.That(fs.Files[outcome.Quarantined[0]], Is.EqualTo(damaged), "damaged file preserved byte for byte");
        }

        [Test]
        public void CorruptSaveWithNoBackupAsksThePlayerAndKeepsTheFile()
        {
            var fs = new InMemoryFileSystem();
            string primary = Path.Combine(Dir, SaveStore.PrimaryName);
            var junk = Encoding.UTF8.GetBytes("{\"format\":\"idle-loadshedding-save\",\"schemaVer");
            fs.Files[primary] = junk;

            var outcome = Store(fs).Load();
            Assert.That(outcome.Status, Is.EqualTo(LoadStatus.NeedsRecovery));
            Assert.That(outcome.BackupAvailable, Is.False);
            Assert.That(fs.Files[outcome.Quarantined.Single()], Is.EqualTo(junk));
        }

        [Test]
        public void NewerSchemaIsNeverOverwrittenSilently()
        {
            var fs = new InMemoryFileSystem();
            var store = Store(fs);
            store.Load();
            store.Commit(StateA());
            string primary = Path.Combine(Dir, SaveStore.PrimaryName);
            store.Commit(StateB());
            var text = Encoding.UTF8.GetString(fs.Files[primary]).Replace("\"schemaVersion\": 1", "\"schemaVersion\": 7");
            var future = Encoding.UTF8.GetBytes(text);
            fs.Files[primary] = future;

            var clock = _clock;
            var session = new SessionCoordinator(_c, new SaveStore(fs, Dir, _c, clock), clock);
            session.Boot();
            Assert.That(session.BootState, Is.EqualTo(BootState.NeedsRecovery));
            Assert.That(session.InputEnabled, Is.False);
            Assert.That(session.Snapshot.Recovery.ProblemKind, Is.EqualTo(DecodeStatus.FutureSchema));
            Assert.That(session.Snapshot.Recovery.BackupAvailable, Is.True);
            Assert.That(fs.Files[primary], Is.EqualTo(future), "untouched until the player chooses");
            string kept = session.Snapshot.Recovery.Quarantined.Single();
            Assert.That(fs.Files[kept], Is.EqualTo(future));

            session.ResolveRecovery(useBackup: true);
            Assert.That(session.BootState, Is.EqualTo(BootState.Ready));
            Assert.That(StateHash.Simulation(_c, session.CopyState()), Is.EqualTo(StateHash.Simulation(_c, StateA())));
            Assert.That(fs.Files[kept], Is.EqualTo(future), "the newer save is still preserved after recovery");
            Assert.That(session.Revision, Is.GreaterThan(2), "revisions keep increasing past the newer file");
        }

        private byte[] Damage(byte[] bytes, string damage)
        {
            switch (damage)
            {
                case "truncated":
                    return bytes.Take(bytes.Length * 2 / 3).ToArray();
                case "garbage":
                    var g = (byte[])bytes.Clone();
                    for (int i = 0; i < g.Length; i += 7) g[i] = 0xFF;
                    return g;
                case "tampered":
                    // Still valid JSON, but the cash value no longer matches the checksum.
                    return Encoding.UTF8.GetBytes(Encoding.UTF8.GetString(bytes).Replace("\"cashCents\": ", "\"cashCents\": 9"));
                case "invalid":
                {
                    // Re-checksummed, so only state validation can catch it: charge above capacity.
                    var envelope = Json.Parse(Encoding.UTF8.GetString(bytes));
                    var payload = envelope.GetObject("payload");
                    payload.Set("batteryChargeWs", payload.GetLong("batteryCapacityWs") + 1);
                    envelope.Set("payloadFnv1a64", StateHash.Hex(StateHash.Fnv1a64(Json.Write(payload))));
                    return Encoding.UTF8.GetBytes(Json.Write(envelope, indented: true));
                }
                default:
                    throw new ArgumentException(damage);
            }
        }
    }
}
