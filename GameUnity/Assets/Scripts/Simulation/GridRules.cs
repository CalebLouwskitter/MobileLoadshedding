using System;

namespace IdleLoadshedding.Simulation
{
    public enum StepOutcome
    {
        /// <summary>Nothing happened: the town is waiting for the player in Prep.</summary>
        Frozen = 0,
        Ticked = 1,
        CutFinished = 2,
        /// <summary>A cut is due and needs the player. No tick was consumed.</summary>
        GateReached = 3
    }

    /// <summary>The fixed-tick phase machine. Every path that advances time goes through <see cref="Step"/>.</summary>
    public static class GridRules
    {
        public const int RulesetVersion = 1;

        public static SimState NewGame(GameContent content, long nowUtcMs)
        {
            int n = content.DistrictCount;
            var first = content.FindScenario(content.Campaign[0]);
            var state = new SimState
            {
                TownId = content.TownId,
                Tick = 0,
                Phase = first.LeadTicks == 0 ? Phase.Prep : Phase.Normal,
                CampaignIndex = 0,
                CutScenarioId = first.Id,
                CutStartTick = first.LeadTicks,
                CashCents = content.StartingCashCents,
                BatteryCapacityWs = content.Battery.CapacityWs,
                BatteryChargeWs = content.Battery.StartChargeWs,
                FuelMl = content.Generator.StartFuelMl,
                FuelMlPerKWh = content.Generator.FuelMlPerKWh,
                TankCapacityMl = content.Generator.TankCapacityMl,
                Draft = DispatchPlan.Default(content),
                Powered = AllPowered(n),
                WatermarkUtcMs = nowUtcMs
            };
            return state;
        }

        public static bool[] AllPowered(int count)
        {
            var powered = new bool[count];
            for (int i = 0; i < count; i++) powered[i] = true;
            return powered;
        }

        public static ScenarioDef CurrentScenario(GameContent content, SimState state) =>
            content.FindScenario(state.CutScenarioId);

        public static bool IsRoutine(GameContent content, SimState state) =>
            state.CampaignIndex >= content.Campaign.Length;

        /// <summary>Whether elapsed real time may advance the town without the player.</summary>
        public static bool IsIdleEligible(SimState state) =>
            state.Phase == Phase.Normal || (state.Phase == Phase.Running && !state.CutManual);

        /// <summary>Starts the current cut with the given plan.</summary>
        public static void BeginCut(GameContent content, SimState state, DispatchPlan plan, bool manual)
        {
            var scenario = CurrentScenario(content, state);
            int n = content.DistrictCount;
            state.Phase = Phase.Running;
            state.CutManual = manual;
            state.FastForward = false;
            state.CutStartTick = state.Tick;
            state.Cut = new CutStats
            {
                ScenarioId = scenario.Id,
                PlanUsed = plan.Clone(),
                Manual = manual,
                StartTick = state.Tick,
                TotalTicks = scenario.TotalTicks,
                ServedTicks = new int[n],
                DarkTicks = new int[n]
            };
        }

        /// <summary>Advances the town by one tick, or reports why it cannot.</summary>
        public static StepOutcome Step(GameContent content, SimState state)
        {
            switch (state.Phase)
            {
                case Phase.Prep:
                    return StepOutcome.Frozen;

                case Phase.Normal:
                    if (state.Tick >= state.CutStartTick)
                    {
                        var scenario = CurrentScenario(content, state);
                        if (scenario.RequiresPlayer || state.Policy == null)
                        {
                            state.Phase = Phase.Prep;
                            state.FastForward = false;
                            state.Powered = AllPowered(content.DistrictCount);
                            return StepOutcome.GateReached;
                        }
                        BeginCut(content, state, state.Policy, manual: false);
                        return RunningTick(content, state);
                    }
                    NormalTick(content, state);
                    return StepOutcome.Ticked;

                case Phase.Running:
                    return RunningTick(content, state);

                default:
                    throw new InvalidOperationException("Unknown phase " + state.Phase);
            }
        }

        /// <summary>A tick of normal supply: every feeder is on and the battery recharges.</summary>
        public static void NormalTick(GameContent content, SimState state)
        {
            int n = content.DistrictCount;
            long income = 0;
            for (int i = 0; i < n; i++)
            {
                state.Powered[i] = true;
                income += content.Districts[i].RevenuePerTickCents;
            }
            state.CashCents += income;
            state.LifetimeIncomeCents += income;

            long recharge = content.Battery.RechargeW * content.Timing.SimSecondsPerTick;
            state.BatteryChargeWs = Math.Min(state.BatteryCapacityWs, state.BatteryChargeWs + recharge);
            state.Tick++;
        }

        private static StepOutcome RunningTick(GameContent content, SimState state)
        {
            var scenario = CurrentScenario(content, state);
            var cut = state.Cut;
            int blockIndex = scenario.BlockAt(cut.TicksElapsed);
            var d = Dispatcher.Dispatch(content, scenario, blockIndex, cut.PlanUsed,
                state.BatteryChargeWs, state.FuelMl, state.FuelCarry, state.FuelMlPerKWh);

            long seconds = content.Timing.SimSecondsPerTick;
            state.Powered = d.Powered;
            state.BatteryChargeWs -= d.BatteryUsedWs;
            state.FuelMl -= d.FuelUsedMl;
            state.FuelCarry = d.FuelCarryAfter;
            state.CashCents += d.IncomeCents;
            state.LifetimeIncomeCents += d.IncomeCents;

            for (int i = 0; i < d.Powered.Length; i++)
            {
                if (d.Powered[i]) cut.ServedTicks[i]++;
                else cut.DarkTicks[i]++;
            }
            cut.IncomeCents += d.IncomeCents;
            cut.FuelUsedMl += d.FuelUsedMl;
            cut.BatteryUsedWs += d.BatteryUsedWs;
            cut.ServedWs += d.ServedW * seconds;
            cut.DemandWs += d.DemandW * seconds;
            cut.BackupWs += d.BackupW * seconds;
            cut.TicksElapsed++;
            state.Tick++;

            CheckInvariants(state, d);

            if (cut.TicksElapsed < cut.TotalTicks) return StepOutcome.Ticked;
            FinishCut(content, state);
            return StepOutcome.CutFinished;
        }

        private static void FinishCut(GameContent content, SimState state)
        {
            var finished = state.Cut;
            state.LastReport = finished;
            // Policy-run cuts are summarised by the return receipt; a cut the player ran gets a results card.
            state.ReportAcknowledged = !finished.Manual;
            state.Cut = null;
            state.Phase = Phase.Normal;
            state.CutManual = false;
            state.CutsCompleted++;
            state.Powered = AllPowered(content.DistrictCount);

            if (state.CampaignIndex < content.Campaign.Length && content.Campaign[state.CampaignIndex] == finished.ScenarioId)
                state.CampaignIndex++;

            string next = state.CampaignIndex < content.Campaign.Length
                ? content.Campaign[state.CampaignIndex]
                : content.RoutineScenarioId;
            state.CutScenarioId = next;
            state.CutStartTick = state.Tick + content.FindScenario(next).LeadTicks;
        }

        private static void CheckInvariants(SimState state, DispatchResult d)
        {
            if (state.BatteryChargeWs < 0 || state.BatteryChargeWs > state.BatteryCapacityWs)
                throw new InvalidOperationException("Battery charge out of bounds: " + state.BatteryChargeWs);
            if (state.FuelMl < 0)
                throw new InvalidOperationException("Fuel went negative: " + state.FuelMl);
            if (state.FuelCarry < 0 || state.FuelCarry >= Dispatcher.WsPerKWh)
                throw new InvalidOperationException("Fuel carry out of range: " + state.FuelCarry);
            if (d.ServedW > d.ImportCapW + d.BackupAvailableW || d.BackupW > d.BackupAvailableW)
                throw new InvalidOperationException("Served more power than was available");
        }

        /// <summary>Simulated wall clock for display: day number (from 1) and minute of day.</summary>
        public static void ClockAt(GameContent content, long tick, out int day, out int minuteOfDay)
        {
            long minutes = content.Timing.StartMinuteOfDay + tick * content.Timing.SimSecondsPerTick / 60;
            day = (int)(minutes / 1440) + 1;
            minuteOfDay = (int)(minutes % 1440);
        }
    }
}
