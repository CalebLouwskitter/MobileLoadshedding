using IdleLoadshedding.Simulation;
using NUnit.Framework;

namespace IdleLoadshedding.Tests
{
    /// <summary>Acceptance 3 (part): energy bounds hold whatever the player does.</summary>
    public class EnergyBoundsTests
    {
        [Test]
        public void ChargeFuelAndServiceStayInBoundsOverLongSeededPlay([Values(1UL, 7UL, 42UL, 2026UL)] ulong seed)
        {
            var c = TestContent.Load();
            var rng = new Lcg(seed);
            var s = Sim.NewGame(c);
            string[][] orders =
            {
                new[] { "clinic", "homes", "market" }, new[] { "market", "homes", "clinic" }, new[] { "homes", "clinic", "market" }
            };

            for (int round = 0; round < 40; round++)
            {
                // Random purchases between cuts (rejections must spend nothing).
                var upgrade = c.Upgrades[rng.Next(c.Upgrades.Length)];
                long cashBefore = s.CashCents;
                var buy = CommandRules.Apply(c, s, GameCommand.BuyUpgrade("buy-" + round, upgrade.Id));
                if (!buy.Accepted) Assert.That(s.CashCents, Is.EqualTo(cashBefore));

                var plan = Sim.Plan(c, PlanKinds.All[rng.Next(3)], orders[rng.Next(orders.Length)]);
                plan.Protected = rng.Next(2) == 0 ? new[] { "clinic" } : new string[0];
                s.Policy = rng.Next(3) == 0 ? null : plan.Clone();

                if (s.Phase == Phase.Prep)
                {
                    s.Draft = plan;
                    Assert.That(CommandRules.Apply(c, s, GameCommand.StartCut("cut-" + round)).Accepted, Is.True);
                }

                for (int t = 0; t < 600 && s.Phase != Phase.Prep; t++)
                {
                    long cap = s.BatteryCapacityWs;
                    var cut = s.Cut;
                    GridRules.Step(c, s);
                    Assert.That(s.BatteryChargeWs, Is.InRange(0L, cap));
                    Assert.That(s.FuelMl, Is.InRange(0L, s.TankCapacityMl));
                    Assert.That(s.FuelCarry, Is.InRange(0L, Dispatcher.WsPerKWh - 1));
                    Assert.That(s.CashCents, Is.GreaterThanOrEqualTo(0L));
                    var stats = cut ?? s.Cut;
                    if (stats != null)
                    {
                        Assert.That(stats.ServedWs, Is.LessThanOrEqualTo(stats.DemandWs), "served never exceeds demand");
                        for (int i = 0; i < c.DistrictCount; i++)
                            Assert.That(stats.ServedTicks[i] + stats.DarkTicks[i], Is.EqualTo(stats.TicksElapsed), "each tick is either served or dark");
                    }
                }
            }
        }

        [Test]
        public void DispatchNeverServesMoreThanSupplyAndFeedersAreWhole()
        {
            var c = TestContent.Load();
            var scenario = c.FindScenario("cut.night-market");
            foreach (var kind in PlanKinds.All)
            {
                for (long charge = 0; charge <= c.Battery.CapacityWs; charge += c.Battery.CapacityWs / 7)
                {
                    for (long fuel = 0; fuel <= 3000; fuel += 137)
                    {
                        for (int block = 0; block < scenario.Blocks.Length; block++)
                        {
                            var d = Dispatcher.Dispatch(c, scenario, block, Sim.Plan(c, kind), charge, fuel, 0, 300);
                            Assert.That(d.ServedW, Is.LessThanOrEqualTo(d.ImportCapW + d.BackupAvailableW));
                            long sum = 0;
                            for (int i = 0; i < c.DistrictCount; i++)
                                if (d.Powered[i]) sum += Dispatcher.DemandOf(c, scenario.Blocks[block], i);
                            Assert.That(d.ServedW, Is.EqualTo(sum), "served power is exactly the whole feeders switched on");
                            Assert.That(d.BatteryUsedWs, Is.LessThanOrEqualTo(charge));
                            Assert.That(d.FuelUsedMl, Is.LessThanOrEqualTo(fuel));
                        }
                    }
                }
            }
        }
    }

    /// <summary>Acceptance 3 (part): duplicate and invalid purchases.</summary>
    public class PurchaseTests
    {
        private GameContent _c;

        [SetUp]
        public void SetUp() { _c = TestContent.Load(); }

        private SimState AfterFirstCut()
        {
            var s = Sim.NewGame(_c);
            Sim.RunCut(_c, s, Sim.Plan(_c, PlanKind.Rotate));
            return s;
        }

        [Test]
        public void DoubleTapWithSameCommandIdSpendsOnce()
        {
            var s = AfterFirstCut();
            long before = s.CashCents;
            var first = CommandRules.Apply(_c, s, GameCommand.BuyUpgrade("tap-1", "supply.fuel-delivery"));
            var second = CommandRules.Apply(_c, s, GameCommand.BuyUpgrade("tap-1", "supply.fuel-delivery"));
            Assert.That(first.Status, Is.EqualTo(CommandStatus.Accepted));
            Assert.That(second.Status, Is.EqualTo(CommandStatus.Duplicate));
            Assert.That(s.CashCents, Is.EqualTo(before - 48000));
            Assert.That(s.FuelMl, Is.EqualTo(40000));
            Assert.That(s.UpgradeCount("supply.fuel-delivery"), Is.EqualTo(1));
        }

        [Test]
        public void OwnedUpgradeIsRejectedAndSpendsZero()
        {
            var s = AfterFirstCut();
            Assert.That(CommandRules.Apply(_c, s, GameCommand.BuyUpgrade("a", "upgrade.battery-module")).Accepted, Is.True);
            long cash = s.CashCents;
            long capacity = s.BatteryCapacityWs;
            var again = CommandRules.Apply(_c, s, GameCommand.BuyUpgrade("b", "upgrade.battery-module"));
            Assert.That(again.Reason, Is.EqualTo(CommandReasons.AlreadyOwned));
            Assert.That(s.CashCents, Is.EqualTo(cash));
            Assert.That(s.BatteryCapacityWs, Is.EqualTo(capacity));
        }

        [Test]
        public void UnaffordableUnknownAndMidCutPurchasesSpendZero()
        {
            var s = Sim.NewGame(_c);
            s.CashCents = 1000;
            var hash = StateHash.Simulation(_c, s);
            Assert.That(CommandRules.Apply(_c, s, GameCommand.BuyUpgrade("x", "upgrade.battery-module")).Reason, Is.EqualTo(CommandReasons.InsufficientFunds));
            Assert.That(CommandRules.Apply(_c, s, GameCommand.BuyUpgrade("y", "upgrade.solar-farm")).Reason, Is.EqualTo(CommandReasons.UnknownUpgrade));
            Assert.That(StateHash.Simulation(_c, s), Is.EqualTo(hash), "rejections change nothing");

            s.CashCents = 1000000;
            Assert.That(CommandRules.Apply(_c, s, GameCommand.StartCut("go")).Accepted, Is.True);
            long cash = s.CashCents;
            Assert.That(CommandRules.Apply(_c, s, GameCommand.BuyUpgrade("z", "upgrade.battery-module")).Reason, Is.EqualTo(CommandReasons.WrongPhase));
            Assert.That(s.CashCents, Is.EqualTo(cash));
        }

        [Test]
        public void FuelDeliveryIsRepeatableUntilTheTankIsFull()
        {
            var s = AfterFirstCut();
            s.FuelMl = 0;
            s.CashCents = 1000000;
            Assert.That(CommandRules.Apply(_c, s, GameCommand.BuyUpgrade("f1", "supply.fuel-delivery")).Accepted, Is.True);
            Assert.That(CommandRules.Apply(_c, s, GameCommand.BuyUpgrade("f2", "supply.fuel-delivery")).Accepted, Is.True);
            long cash = s.CashCents;
            Assert.That(CommandRules.Apply(_c, s, GameCommand.BuyUpgrade("f3", "supply.fuel-delivery")).Reason, Is.EqualTo(CommandReasons.TankFull));
            Assert.That(s.FuelMl, Is.EqualTo(40000));
            Assert.That(s.CashCents, Is.EqualTo(cash));
        }

        [Test]
        public void BatteryModuleArrivesCharged()
        {
            var s = Sim.NewGame(_c);
            Sim.RunCut(_c, s, Sim.Plan(_c, PlanKind.Battery));
            Assert.That(s.BatteryChargeWs, Is.EqualTo(0));
            Assert.That(CommandRules.Apply(_c, s, GameCommand.BuyUpgrade("m", "upgrade.battery-module")).Accepted, Is.True);
            Assert.That(s.BatteryCapacityWs, Is.EqualTo(2 * _c.Battery.CapacityWs));
            Assert.That(s.BatteryChargeWs, Is.EqualTo(_c.Battery.CapacityWs));
        }

        [Test]
        public void FirstCutIncomeAffordsOneUpgradeNotAll()
        {
            foreach (var kind in PlanKinds.All)
            {
                var s = Sim.NewGame(_c);
                Sim.RunCut(_c, s, Sim.Plan(_c, kind));
                long all = 0;
                int affordable = 0;
                foreach (var u in _c.Upgrades)
                {
                    all += u.CostCents;
                    if (u.CostCents <= s.CashCents) affordable++;
                }
                Assert.That(affordable, Is.GreaterThanOrEqualTo(1), kind + " leaves enough for an upgrade");
                Assert.That(s.CashCents, Is.LessThan(all), kind + " cannot buy everything at once");
            }
        }
    }
}
