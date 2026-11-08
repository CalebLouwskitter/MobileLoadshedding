using System;
using System.Collections.Generic;
using IdleLoadshedding.Session;
using IdleLoadshedding.Simulation;
using NUnit.Framework;

namespace IdleLoadshedding.Tests
{
    /// <summary>Acceptance 3: the same command sequence gives the same result at any render frame rate.</summary>
    public class DeterminismTests
    {
        private sealed class Recorded
        {
            public long Tick;
            public Phase Phase;
            public GameCommand Command;
        }

        private sealed class Run
        {
            public string Hash;
            public SimState State;
            public List<Recorded> Log = new List<Recorded>();
            public int Frames;
        }

        private static Run PlaySession(GameContent c, Func<int, long> frameMs)
        {
            var clock = new FakeClock(Sim.T0);
            var store = new SaveStore(new InMemoryFileSystem(), "/saves", c, clock);
            var session = new SessionCoordinator(c, store, clock);
            session.Boot();
            Assert.That(session.BootState, Is.EqualTo(BootState.Ready));

            var run = new Run();
            int frame = 0;

            void Submit(GameCommand cmd)
            {
                var before = session.CopyState();
                run.Log.Add(new Recorded { Tick = before.Tick, Phase = before.Phase, Command = cmd });
                var result = session.Submit(cmd);
                Assert.That(result.Accepted, Is.True, cmd + " -> " + result);
            }

            void FramesUntil(Func<SimState, bool> done)
            {
                for (int guard = 0; guard < 200000; guard++)
                {
                    if (done(session.CopyState())) return;
                    long dt = frameMs(frame++);
                    clock.Advance(dt);
                    session.Update(dt);
                }
                throw new InvalidOperationException("session never reached the expected state");
            }

            Submit(GameCommand.SelectPlan("c1", PlanKind.Battery));
            Submit(GameCommand.MovePriority("c2", "market", -1));
            Submit(GameCommand.StartCut("c3"));
            FramesUntil(s => s.Phase == Phase.Normal);
            Submit(GameCommand.AcknowledgeReport("c4"));
            Submit(GameCommand.BuyUpgrade("c5", "upgrade.generator-tuneup"));
            Submit(GameCommand.SavePolicy("c6"));
            Submit(GameCommand.SkipToNextCut("c7"));
            FramesUntil(s => s.Phase == Phase.Prep);
            Submit(GameCommand.SelectPlan("c8", PlanKind.Diesel));
            Submit(GameCommand.StartCut("c9"));
            FramesUntil(s => s.Phase == Phase.Normal);

            run.State = session.CopyState();
            run.Hash = StateHash.Simulation(c, run.State);
            run.Frames = frame;
            return run;
        }

        [Test]
        public void SameCommandsGiveSameResultAt30And60FpsAndJitteredFrames()
        {
            var c = TestContent.Load();
            var at30 = PlaySession(c, f => f % 3 == 2 ? 34 : 33);
            var at60 = PlaySession(c, f => f % 3 == 2 ? 16 : 17);
            var jitter = new Lcg(99);
            var jittered = PlaySession(c, f => f % 50 == 49 ? 900 : 1 + jitter.Next(120));

            Assert.That(at60.Frames, Is.GreaterThan(at30.Frames), "the runs really used different frame counts");
            Assert.That(at60.Hash, Is.EqualTo(at30.Hash));
            Assert.That(jittered.Hash, Is.EqualTo(at30.Hash));
            Assert.That(at30.State.CutsCompleted, Is.EqualTo(2));
            Assert.That(at30.State.Tick, Is.EqualTo(120 + 180 + 120), "two cuts and the lead, independent of frames");
        }

        [Test]
        public void RecordedCommandLogReplaysToTheSameHash()
        {
            var c = TestContent.Load();
            var live = PlaySession(c, f => 33);

            var s = Sim.NewGame(c);
            foreach (var rec in live.Log)
            {
                while (s.Tick < rec.Tick || s.Phase != rec.Phase)
                {
                    var outcome = GridRules.Step(c, s);
                    Assert.That(outcome, Is.Not.EqualTo(StepOutcome.Frozen), "replay stuck before " + rec.Command);
                }
                Assert.That(CommandRules.Apply(c, s, rec.Command).Accepted, Is.True, rec.Command.ToString());
            }
            while (s.CutsCompleted < live.State.CutsCompleted) GridRules.Step(c, s);

            Assert.That(StateHash.Simulation(c, s), Is.EqualTo(live.Hash));
        }
    }
}
