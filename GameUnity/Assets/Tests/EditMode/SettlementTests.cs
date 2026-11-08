using IdleLoadshedding.Simulation;
using NUnit.Framework;

namespace IdleLoadshedding.Tests
{
    /// <summary>Acceptance 5 (part): offline progress, clock changes and repeated resumes.</summary>
    public class SettlementTests
    {
        private const long Minute = 60_000;
        private const long Hour = 60 * Minute;
        private GameContent _c;

        [SetUp]
        public void SetUp() { _c = TestContent.Load(); }

        /// <summary>After the first cut: Normal phase with a 180-tick lead to the night-market gate.</summary>
        private SimState AfterFirstCut()
        {
            var s = Sim.NewGame(_c);
            Sim.RunCut(_c, s, Sim.Plan(_c, PlanKind.Rotate));
            s.WatermarkUtcMs = Sim.T0;
            return s;
        }

        [Test]
        public void EachEligibleIntervalIsAwardedOnceAcrossRepeatedResumes()
        {
            var s = AfterFirstCut();
            long cash = s.CashCents;
            var first = OfflineSettlement.Settle(_c, s, Sim.T0 + 30 * Minute, createReceipt: true);
            Assert.That(first.Ticks, Is.EqualTo(30));
            Assert.That(s.CashCents, Is.EqualTo(cash + 30 * 700));

            for (int i = 0; i < 5; i++)
            {
                var again = OfflineSettlement.Settle(_c, s, Sim.T0 + 30 * Minute, createReceipt: true);
                Assert.That(again.Ticks, Is.EqualTo(0));
            }
            Assert.That(s.CashCents, Is.EqualTo(cash + 30 * 700));
            Assert.That(s.PendingReceipt.IncomeCents, Is.EqualTo(30 * 700));
        }

        [Test]
        public void ClockRollbackAwardsNothingAndKeepsTheWatermark()
        {
            var s = AfterFirstCut();
            OfflineSettlement.Settle(_c, s, Sim.T0 + 20 * Minute, createReceipt: false);
            long cash = s.CashCents;
            long watermark = s.WatermarkUtcMs;

            var back = OfflineSettlement.Settle(_c, s, Sim.T0 - 3 * Hour, createReceipt: true);
            Assert.That(back.Ticks, Is.EqualTo(0));
            Assert.That(back.Changed, Is.False);
            Assert.That(s.WatermarkUtcMs, Is.EqualTo(watermark));
            Assert.That(s.CashCents, Is.EqualTo(cash));
            Assert.That(s.PendingReceipt, Is.Null);

            // Moving the clock forward again only pays for time past the old watermark.
            var forward = OfflineSettlement.Settle(_c, s, Sim.T0 + 25 * Minute, createReceipt: false);
            Assert.That(forward.Ticks, Is.EqualTo(5));
        }

        [Test]
        public void RemainderCarriesForwardWithoutLossOrDuplication()
        {
            var s = AfterFirstCut();
            Assert.That(OfflineSettlement.Settle(_c, s, Sim.T0 + 90_000, false).Ticks, Is.EqualTo(1));
            Assert.That(s.WatermarkUtcMs, Is.EqualTo(Sim.T0 + Minute));
            Assert.That(OfflineSettlement.Settle(_c, s, Sim.T0 + 120_000, false).Ticks, Is.EqualTo(1));
            Assert.That(s.WatermarkUtcMs, Is.EqualTo(Sim.T0 + 2 * Minute));
        }

        [Test]
        public void ForwardClockJumpIsCappedAtFourHours()
        {
            var s = Sim.Routine(_c, withPolicy: true);
            s.WatermarkUtcMs = Sim.T0;
            long untilCut = s.CutStartTick - s.Tick;
            Assert.That(untilCut, Is.GreaterThan(240), "no cut inside the cap window");

            var r = OfflineSettlement.Settle(_c, s, Sim.T0 + 48 * Hour, createReceipt: true);
            Assert.That(r.Ticks, Is.EqualTo(240));
            Assert.That(r.Stop, Is.EqualTo(StopReason.CapReached));
            Assert.That(s.WatermarkUtcMs, Is.EqualTo(Sim.T0 + 48 * Hour), "the capped-out interval is consumed");
            Assert.That(s.PendingReceipt.ForfeitedMs, Is.EqualTo(44 * Hour));

            // Setting the clock back to "real" time afterwards earns nothing more.
            Assert.That(OfflineSettlement.Settle(_c, s, Sim.T0 + 5 * Hour, true).Ticks, Is.EqualTo(0));
        }

        [Test]
        public void StopsAtTheNextDecisionAndConsumesTheRest()
        {
            var s = AfterFirstCut();
            long lead = s.CutStartTick - s.Tick;
            Assert.That(lead, Is.EqualTo(180));

            var r = OfflineSettlement.Settle(_c, s, Sim.T0 + 4 * Hour, createReceipt: true);
            Assert.That(r.Ticks, Is.EqualTo(180));
            Assert.That(r.Stop, Is.EqualTo(StopReason.DecisionNeeded));
            Assert.That(s.Phase, Is.EqualTo(Phase.Prep), "the night market waits for the player");
            Assert.That(s.WatermarkUtcMs, Is.EqualTo(Sim.T0 + 4 * Hour));

            // While waiting in Prep, time passes but nothing is awarded.
            long cash = s.CashCents;
            var later = OfflineSettlement.Settle(_c, s, Sim.T0 + 6 * Hour, createReceipt: true);
            Assert.That(later.Ticks, Is.EqualTo(0));
            Assert.That(s.CashCents, Is.EqualTo(cash));
        }

        [Test]
        public void PolicyHandlesRoutineCutsWhileAway()
        {
            var s = Sim.Routine(_c, withPolicy: true);
            // Wind forward so the next routine cut starts in 100 ticks.
            while (s.CutStartTick - s.Tick > 100) GridRules.Step(_c, s);
            s.WatermarkUtcMs = Sim.T0;

            var r = OfflineSettlement.Settle(_c, s, Sim.T0 + 4 * Hour, createReceipt: true);
            Assert.That(r.Ticks, Is.EqualTo(240));
            Assert.That(r.CutsHandled, Is.EqualTo(1));
            Assert.That(s.PendingReceipt.CutsHandled, Is.EqualTo(1));
            Assert.That(s.PendingReceipt.DarkFeederTicks, Is.EqualTo(120), "rotation leaves one feeder dark per tick");
            Assert.That(s.LastReport.Manual, Is.False);
            Assert.That(s.ReportAcknowledged, Is.True, "policy cuts are summarised by the receipt");
        }

        [Test]
        public void RoutineCutWithoutPolicyIsADecisionGate()
        {
            var s = Sim.Routine(_c, withPolicy: false);
            while (s.CutStartTick - s.Tick > 100) GridRules.Step(_c, s);
            s.WatermarkUtcMs = Sim.T0;

            var r = OfflineSettlement.Settle(_c, s, Sim.T0 + 4 * Hour, createReceipt: true);
            Assert.That(r.Ticks, Is.EqualTo(100));
            Assert.That(r.Stop, Is.EqualTo(StopReason.DecisionNeeded));
            Assert.That(s.Phase, Is.EqualTo(Phase.Prep));
        }

        [Test]
        public void UnacknowledgedReceiptIsMergedAndBalancePreserved()
        {
            var s = AfterFirstCut();
            long cash = s.CashCents;
            OfflineSettlement.Settle(_c, s, Sim.T0 + 20 * Minute, true);
            string firstId = s.PendingReceipt.Id;
            OfflineSettlement.Settle(_c, s, Sim.T0 + 50 * Minute, true);

            Assert.That(s.PendingReceipt.Ticks, Is.EqualTo(50));
            Assert.That(s.PendingReceipt.IncomeCents, Is.EqualTo(50 * 700));
            Assert.That(s.CashCents, Is.EqualTo(cash + 50 * 700));
            Assert.That(s.PendingReceipt.Id, Is.Not.EqualTo(firstId), "a merged receipt gets a new id");

            var stale = CommandRules.Apply(_c, s, GameCommand.AcknowledgeReceipt("ack-1", firstId));
            Assert.That(stale.Reason, Is.EqualTo(CommandReasons.StaleReceipt));
            Assert.That(CommandRules.Apply(_c, s, GameCommand.AcknowledgeReceipt("ack-2", s.PendingReceipt.Id)).Accepted, Is.True);
            Assert.That(s.PendingReceipt, Is.Null);
            Assert.That(s.CashCents, Is.EqualTo(cash + 50 * 700), "acknowledging never changes the balance");
        }

        [Test]
        public void ManualCutStaysPausedWhileAway()
        {
            var s = Sim.NewGame(_c);
            Assert.That(CommandRules.Apply(_c, s, GameCommand.StartCut("go")).Accepted, Is.True);
            for (int i = 0; i < 30; i++) GridRules.Step(_c, s);
            var before = StateHash.Simulation(_c, s);

            var r = OfflineSettlement.Settle(_c, s, Sim.T0 + 3 * Hour, createReceipt: true);
            Assert.That(r.Ticks, Is.EqualTo(0));
            Assert.That(r.Stop, Is.EqualTo(StopReason.CutPaused));
            Assert.That(StateHash.Simulation(_c, s), Is.EqualTo(before));
            Assert.That(s.Cut.TicksElapsed, Is.EqualTo(30));
        }
    }
}
