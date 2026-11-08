using System.Collections.Generic;
using IdleLoadshedding.Simulation;
using NUnit.Framework;

namespace IdleLoadshedding.Tests
{
    /// <summary>Acceptance 2: the three dispatch plans for the first cut.</summary>
    public class DispatchTests
    {
        private GameContent _c;
        private int _homes, _market, _clinic;

        [SetUp]
        public void SetUp()
        {
            _c = TestContent.Load();
            _homes = Sim.Index(_c, "homes");
            _market = Sim.Index(_c, "market");
            _clinic = Sim.Index(_c, "clinic");
        }

        [Test]
        public void ForecastShowsThirtyKilowattShortfall()
        {
            var s = Sim.NewGame(_c);
            Assert.That(s.Phase, Is.EqualTo(Phase.Prep));
            var f = Forecast.ForPlan(_c, s, s.Draft);
            Assert.That(f.Blocks[0].DemandW, Is.EqualTo(90000));
            Assert.That(f.Blocks[0].ImportCapW, Is.EqualTo(60000));
            Assert.That(f.PeakShortfallW, Is.EqualTo(30000));
        }

        [Test]
        public void RotationProtectsTheClinicAndRotatesHomesAndMarket()
        {
            var s = Sim.NewGame(_c);
            var perTick = new List<bool[]>();
            var report = Sim.RunCut(_c, s, Sim.Plan(_c, PlanKind.Rotate), st => perTick.Add((bool[])st.Powered.Clone()));

            Assert.That(report.ServedTicks[_clinic], Is.EqualTo(120), "clinic powered for both blocks");
            Assert.That(report.ServedTicks[_homes], Is.EqualTo(60));
            Assert.That(report.ServedTicks[_market], Is.EqualTo(60));
            for (int t = 0; t < 59; t++)
            {
                Assert.That(perTick[t][_homes], Is.True, "homes on in block 1");
                Assert.That(perTick[t][_market], Is.False, "market off in block 1");
            }
            for (int t = 60; t < 119; t++)
            {
                Assert.That(perTick[t][_homes], Is.False, "homes off in block 2");
                Assert.That(perTick[t][_market], Is.True, "market on in block 2");
            }
            Assert.That(report.IncomeCents, Is.EqualTo(60 * 200 + 60 * 500), "R420 from one block each");
            Assert.That(report.FuelUsedMl, Is.EqualTo(0));
            Assert.That(report.BatteryUsedWs, Is.EqualTo(0));
            Assert.That(s.BatteryChargeWs, Is.EqualTo(_c.Battery.StartChargeWs));
        }

        [Test]
        public void BatteryCoversExactlyOneFullBlockAtThirtyKilowatts()
        {
            var s = Sim.NewGame(_c);
            var charge = new List<long>();
            var perTick = new List<bool[]>();
            var report = Sim.RunCut(_c, s, Sim.Plan(_c, PlanKind.Battery), st =>
            {
                charge.Add(st.BatteryChargeWs);
                perTick.Add((bool[])st.Powered.Clone());
            });

            long perTickWs = 30000L * 60;
            Assert.That(charge[0], Is.EqualTo(_c.Battery.CapacityWs - perTickWs), "30 kW for one minute");
            Assert.That(charge[58], Is.EqualTo(perTickWs), "one tick of charge left before the block ends");
            Assert.That(charge[59], Is.EqualTo(0), "empty exactly at the end of block 1");
            for (int t = 0; t < 60; t++)
                Assert.That(perTick[t][_homes] && perTick[t][_market] && perTick[t][_clinic], Is.True, "all feeders on in block 1");
            for (int t = 60; t < 119; t++)
            {
                Assert.That(charge[t], Is.EqualTo(0));
                Assert.That(perTick[t][_clinic] && perTick[t][_homes], Is.True, "priority order keeps clinic then homes");
                Assert.That(perTick[t][_market], Is.False);
            }
            Assert.That(report.BatteryUsedWs, Is.EqualTo(_c.Battery.CapacityWs));
            Assert.That(report.ServedTicks[_market], Is.EqualTo(60));
            Assert.That(report.IncomeCents, Is.EqualTo(60 * 700 + 60 * 200), "R540");
        }

        [Test]
        public void BatteryFallbackFollowsPriorityTieBreak()
        {
            var s = Sim.NewGame(_c);
            var report = Sim.RunCut(_c, s, Sim.Plan(_c, PlanKind.Battery, "clinic", "market", "homes"));
            Assert.That(report.ServedTicks[_market], Is.EqualTo(120), "market ranked above homes keeps block 2");
            Assert.That(report.ServedTicks[_homes], Is.EqualTo(60));
        }

        [Test]
        public void DieselCoversBothBlocksWhenFuelIsSufficient()
        {
            var s = Sim.NewGame(_c);
            Assert.That(s.FuelMl, Is.EqualTo(20000));
            var report = Sim.RunCut(_c, s, Sim.Plan(_c, PlanKind.Diesel));
            foreach (var i in new[] { _homes, _market, _clinic })
                Assert.That(report.ServedTicks[i], Is.EqualTo(120));
            Assert.That(report.FuelUsedMl, Is.EqualTo(18000), "0.3 L/kWh x 30 kW x 2 h");
            Assert.That(s.FuelMl, Is.EqualTo(2000));
            Assert.That(s.FuelCarry, Is.EqualTo(0));
            Assert.That(report.IncomeCents, Is.EqualTo(120 * 700));
            Assert.That(report.FuelCostCents(_c), Is.EqualTo(18 * 2400));
            Assert.That(report.ReliabilityBasisPoints, Is.EqualTo(10000));
        }

        [Test]
        public void DieselRunsOutAndFallsBackWhenFuelIsShort()
        {
            var s = Sim.NewGame(_c);
            s.FuelMl = 2000;
            var report = Sim.RunCut(_c, s, Sim.Plan(_c, PlanKind.Diesel));
            Assert.That(report.ServedTicks[_market], Is.EqualTo(13), "2 L lasts 13 minutes at 30 kW");
            Assert.That(report.ServedTicks[_homes], Is.EqualTo(120));
            Assert.That(report.ServedTicks[_clinic], Is.EqualTo(120));
            Assert.That(report.FuelUsedMl, Is.EqualTo(1950));
            Assert.That(s.FuelMl, Is.EqualTo(50), "the remainder cannot cover a whole feeder");
        }

        [Test]
        public void NightMarketRewardsADifferentChoice()
        {
            // Battery with no upgrade: block 2 still has a 5 kW gap, so the market goes dark.
            var plain = Sim.NewGame(_c);
            Sim.RunCut(_c, plain, Sim.Plan(_c, PlanKind.Battery));
            Sim.AdvanceToGate(_c, plain);
            Assert.That(plain.CutScenarioId, Is.EqualTo("cut.night-market"));
            Assert.That(plain.BatteryChargeWs, Is.EqualTo(_c.Battery.CapacityWs), "three hours at 10 kW refills 30 kWh");
            var plainReport = Sim.RunCut(_c, plain, Sim.Plan(_c, PlanKind.Battery));
            Assert.That(plainReport.DarkTicks[_market], Is.EqualTo(60));

            // With the battery module the same plan covers both blocks.
            var upgraded = Sim.NewGame(_c);
            Sim.RunCut(_c, upgraded, Sim.Plan(_c, PlanKind.Battery));
            Assert.That(CommandRules.Apply(_c, upgraded, GameCommand.BuyUpgrade("buy", "upgrade.battery-module")).Accepted, Is.True);
            Sim.AdvanceToGate(_c, upgraded);
            var upgradedReport = Sim.RunCut(_c, upgraded, Sim.Plan(_c, PlanKind.Battery));
            foreach (var i in new[] { _homes, _market, _clinic })
                Assert.That(upgradedReport.DarkTicks[i], Is.EqualTo(0));
        }
    }

    /// <summary>The preview and the live run come from the same rules, so they must agree exactly.</summary>
    public class ForecastParityTests
    {
        private static readonly string[][] Orders =
        {
            new[] { "clinic", "homes", "market" }, new[] { "clinic", "market", "homes" },
            new[] { "homes", "clinic", "market" }, new[] { "homes", "market", "clinic" },
            new[] { "market", "clinic", "homes" }, new[] { "market", "homes", "clinic" }
        };

        [Test]
        public void ForecastMatchesLiveRunForEveryPlanAndPriority([Values(0, 1, 2)] int planIndex, [Values(false, true)] bool protectClinic, [Values(false, true)] bool secondCut)
        {
            var c = TestContent.Load();
            foreach (var order in Orders)
            {
                var s = Sim.NewGame(c);
                if (secondCut)
                {
                    Sim.RunCut(c, s, Sim.Plan(c, PlanKind.Diesel));
                    // Forecast from Normal: it must include the recharge during the lead time.
                    var plan0 = MakePlan(c, planIndex, order, protectClinic);
                    var early = Forecast.ForPlan(c, s, plan0);
                    Sim.AdvanceToGate(c, s);
                    var atGate = Forecast.ForPlan(c, s, plan0);
                    AssertSame(early, atGate);
                }

                var plan = MakePlan(c, planIndex, order, protectClinic);
                var forecast = Forecast.ForPlan(c, s, plan);
                var scenario = GridRules.CurrentScenario(c, s);
                var livePerBlock = new int[scenario.Blocks.Length, c.DistrictCount];
                var previous = new int[c.DistrictCount];
                int tick = 0;
                var report = Sim.RunCut(c, s, plan, st =>
                {
                    int block = scenario.BlockAt(tick++);
                    var cut = st.Cut ?? st.LastReport;
                    for (int i = 0; i < c.DistrictCount; i++)
                    {
                        if (cut.ServedTicks[i] > previous[i]) livePerBlock[block, i]++;
                        previous[i] = cut.ServedTicks[i];
                    }
                });

                string label = PlanKinds.All[planIndex] + " " + string.Join(">", order) + " protect=" + protectClinic;
                Assert.That(forecast.Totals.ServedTicks, Is.EqualTo(report.ServedTicks), label);
                Assert.That(forecast.Totals.DarkTicks, Is.EqualTo(report.DarkTicks), label);
                Assert.That(forecast.Totals.IncomeCents, Is.EqualTo(report.IncomeCents), label);
                Assert.That(forecast.Totals.FuelUsedMl, Is.EqualTo(report.FuelUsedMl), label);
                Assert.That(forecast.Totals.BatteryUsedWs, Is.EqualTo(report.BatteryUsedWs), label);
                Assert.That(forecast.BatteryEndWs, Is.EqualTo(s.BatteryChargeWs), label);
                Assert.That(forecast.FuelEndMl, Is.EqualTo(s.FuelMl), label);
                for (int b = 0; b < scenario.Blocks.Length; b++)
                    for (int i = 0; i < c.DistrictCount; i++)
                        Assert.That(forecast.Blocks[b].PoweredTicks[i], Is.EqualTo(livePerBlock[b, i]), label + " block " + b + " district " + i);
            }
        }

        private static DispatchPlan MakePlan(GameContent c, int planIndex, string[] order, bool protectClinic)
        {
            var plan = Sim.Plan(c, PlanKinds.All[planIndex], order);
            plan.Protected = protectClinic ? new[] { "clinic" } : new string[0];
            return plan;
        }

        private static void AssertSame(ForecastResult a, ForecastResult b)
        {
            Assert.That(a.Totals.ServedTicks, Is.EqualTo(b.Totals.ServedTicks));
            Assert.That(a.Totals.IncomeCents, Is.EqualTo(b.Totals.IncomeCents));
            Assert.That(a.BatteryStartWs, Is.EqualTo(b.BatteryStartWs));
        }
    }
}
