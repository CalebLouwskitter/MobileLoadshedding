using IdleLoadshedding.Session;
using IdleLoadshedding.Simulation;
using NUnit.Framework;

namespace IdleLoadshedding.Tests
{
    /// <summary>Acceptance 1, 4 and 5 at the session level: boot, persistence before success, relaunch.</summary>
    public class SessionCoordinatorTests
    {
        private const string Dir = "/data/saves";
        private const long Minute = 60_000;
        private GameContent _c;
        private FakeClock _clock;

        [SetUp]
        public void SetUp()
        {
            _c = TestContent.Load();
            _clock = new FakeClock(Sim.T0);
        }

        private SessionCoordinator Launch(ISaveFileSystem fs)
        {
            var session = new SessionCoordinator(_c, new SaveStore(fs, Dir, _c, _clock), _clock);
            session.Boot();
            return session;
        }

        private void Frames(SessionCoordinator session, int count, long ms = 33)
        {
            for (int i = 0; i < count; i++)
            {
                _clock.Advance(ms);
                session.Update(ms);
            }
        }

        private void RunManualCut(SessionCoordinator session)
        {
            for (int guard = 0; guard < 100000 && session.CopyState().Phase == Phase.Running; guard++)
                Frames(session, 1);
        }

        [Test]
        public void FreshInstallBootsOfflineStraightToTheFirstDecision()
        {
            var fs = new InMemoryFileSystem();
            var session = new SessionCoordinator(_c, new SaveStore(fs, Dir, _c, _clock), _clock);
            Assert.That(session.InputEnabled, Is.False);
            Assert.That(session.Submit(GameCommand.StartCut("early")).Reason, Is.EqualTo(SessionCoordinator.NotReady));

            session.Boot();
            var snap = session.Snapshot;
            Assert.That(session.InputEnabled, Is.True);
            Assert.That(snap.Phase, Is.EqualTo(Phase.Prep), "the outage decision is available with no waiting");
            Assert.That(snap.Forecasts.Length, Is.EqualTo(3));
            Assert.That(snap.SelectedForecast.PeakShortfallW, Is.EqualTo(30000));
            Assert.That(fs.Exists(System.IO.Path.Combine(Dir, SaveStore.PrimaryName)), Is.True, "the new town is saved before input is enabled");
            Assert.That(session.Revision, Is.EqualTo(1));
        }

        [Test]
        public void ForceStopAfterUpgradeRestoresUpgradeBalancePolicyAndLights()
        {
            var fs = new InMemoryFileSystem();
            var session = Launch(fs);
            Assert.That(session.Submit(GameCommand.SelectPlan("p", PlanKind.Battery)).Accepted, Is.True);
            Assert.That(session.Submit(GameCommand.StartCut("s")).Accepted, Is.True);
            RunManualCut(session);
            Assert.That(session.Submit(GameCommand.BuyUpgrade("u", "upgrade.battery-module")).Accepted, Is.True);
            Assert.That(session.Submit(GameCommand.SavePolicy("pol")).Accepted, Is.True);
            var before = session.Snapshot;
            var stateBefore = session.CopyState();

            // Force-stop: no Pause, no quit callback. Relaunch from disk at the same instant.
            var relaunched = Launch(fs);
            var after = relaunched.Snapshot;
            var stateAfter = relaunched.CopyState();
            Assert.That(stateAfter.UpgradeCount("upgrade.battery-module"), Is.EqualTo(1));
            Assert.That(after.CashCents, Is.EqualTo(before.CashCents));
            Assert.That(after.BatteryCapacityWs, Is.EqualTo(before.BatteryCapacityWs));
            Assert.That(after.Policy.SameAs(before.Policy), Is.True);
            Assert.That(after.LightsSignature, Is.EqualTo(before.LightsSignature));
            Assert.That(StateHash.Simulation(_c, stateAfter), Is.EqualTo(StateHash.Simulation(_c, stateBefore)));
            Assert.That(after.PendingReceipt, Is.Null, "no time passed, so nothing is awarded");
        }

        [Test]
        public void KillMidCutRestoresTheLastCheckpointWithTheCutPaused()
        {
            var fs = new InMemoryFileSystem();
            var session = Launch(fs);
            session.Submit(GameCommand.SelectPlan("p", PlanKind.Rotate));
            session.Submit(GameCommand.StartCut("s"));
            Frames(session, 20);            // ~6.6 s of a 12 s run
            session.Pause();                // screen lock / Home writes a checkpoint
            var paused = session.CopyState();
            Assert.That(paused.Phase, Is.EqualTo(Phase.Running));

            _clock.Advance(2 * 60 * Minute); // killed in the background for two hours
            var relaunched = Launch(fs);
            var restored = relaunched.CopyState();
            Assert.That(restored.Phase, Is.EqualTo(Phase.Running));
            Assert.That(restored.Cut.TicksElapsed, Is.EqualTo(paused.Cut.TicksElapsed), "a player-run cut waits for the player");
            Assert.That(relaunched.Snapshot.LightsSignature, Is.EqualTo(new SessionSnapshotProbe(paused, _c).Lights));
        }

        [Test]
        public void IdleTimeIsAwardedExactlyOnceAcrossKillAndRelaunch()
        {
            // Reference: one session left running for ten minutes.
            var refFs = new InMemoryFileSystem();
            var reference = Launch(refFs);
            reference.Submit(GameCommand.StartCut("s"));
            RunManualCut(reference);
            long t = _clock.UtcNowMs;
            for (int i = 0; i < 600; i++) { _clock.Advance(1000); reference.Update(1000); }
            long expected = reference.CopyState().CashCents;

            // Same play, killed after ten minutes without a pause, then relaunched.
            _clock.UtcNowMs = Sim.T0;
            var fs = new InMemoryFileSystem();
            var session = Launch(fs);
            session.Submit(GameCommand.StartCut("s"));
            RunManualCut(session);
            Assert.That(_clock.UtcNowMs, Is.EqualTo(t));
            for (int i = 0; i < 600; i++) { _clock.Advance(1000); session.Update(1000); }
            var relaunched = Launch(fs);
            Assert.That(relaunched.CopyState().CashCents, Is.EqualTo(expected));

            // And repeated resumes at the same instant add nothing.
            relaunched.Resume();
            relaunched.Resume();
            Assert.That(relaunched.CopyState().CashCents, Is.EqualTo(expected));
        }

        [Test]
        public void ReturnAfterHoursShowsOneReceiptThatSurvivesARelaunch()
        {
            var fs = new InMemoryFileSystem();
            var session = Launch(fs);
            session.Submit(GameCommand.StartCut("s"));
            RunManualCut(session);
            session.Submit(GameCommand.AcknowledgeReport("ack"));
            session.Pause();
            long cash = session.CopyState().CashCents;

            _clock.Advance(90 * Minute);
            session.Resume();
            var receipt = session.Snapshot.PendingReceipt;
            Assert.That(receipt, Is.Not.Null);
            Assert.That(receipt.Ticks, Is.EqualTo(90));
            Assert.That(session.CopyState().CashCents, Is.EqualTo(cash + 90 * 700));

            // Relaunch before acknowledging: the same receipt, the same balance.
            var relaunched = Launch(fs);
            Assert.That(relaunched.Snapshot.PendingReceipt.Id, Is.EqualTo(receipt.Id));
            Assert.That(relaunched.CopyState().CashCents, Is.EqualTo(cash + 90 * 700));
            Assert.That(relaunched.Submit(GameCommand.AcknowledgeReceipt("r", receipt.Id)).Accepted, Is.True);
            Assert.That(Launch(fs).Snapshot.PendingReceipt, Is.Null);
        }

        [Test]
        public void FailedSaveMeansNoSuccessAndNoChange()
        {
            var fs = new FaultyFileSystem();
            var session = Launch(fs);
            session.Submit(GameCommand.StartCut("s"));
            RunManualCut(session);
            var before = session.CopyState();
            int version = session.Snapshot.Version;

            fs.FailAllWrites = true;
            var result = session.Submit(GameCommand.BuyUpgrade("buy-1", "upgrade.generator-tuneup"));
            Assert.That(result.Reason, Is.EqualTo(SessionCoordinator.SaveFailed));
            Assert.That(session.CopyState().CashCents, Is.EqualTo(before.CashCents));
            Assert.That(session.CopyState().UpgradeCount("upgrade.generator-tuneup"), Is.EqualTo(0));
            Assert.That(session.Snapshot.Version, Is.EqualTo(version), "no success is published");

            // Retrying the same intent once storage works applies it exactly once.
            fs.FailAllWrites = false;
            Assert.That(session.Submit(GameCommand.BuyUpgrade("buy-1", "upgrade.generator-tuneup")).Accepted, Is.True);
            Assert.That(session.Submit(GameCommand.BuyUpgrade("buy-1", "upgrade.generator-tuneup")).Status, Is.EqualTo(CommandStatus.Duplicate));
            Assert.That(session.CopyState().CashCents, Is.EqualTo(before.CashCents - 90000));
        }

        [Test]
        public void CheckpointsDuringPlayAreThrottled()
        {
            var fs = new InMemoryFileSystem();
            var session = Launch(fs);
            session.Submit(GameCommand.StartCut("s"));
            long revision = session.Revision;
            Frames(session, 30);   // ~1 s of a running cut
            Assert.That(session.Revision, Is.EqualTo(revision), "no write every frame");
            Frames(session, 400);  // past the end of the cut
            Assert.That(session.Revision, Is.GreaterThan(revision), "cut end and the 10 s interval write checkpoints");
        }

        /// <summary>Builds the lights signature for a state the same way the snapshot does.</summary>
        private sealed class SessionSnapshotProbe
        {
            public readonly string Lights;

            public SessionSnapshotProbe(SimState s, GameContent c)
            {
                Lights = SessionSnapshot.Build(c, s, 0, BootState.Ready, true, 0, "", "", false).LightsSignature;
            }
        }
    }
}
