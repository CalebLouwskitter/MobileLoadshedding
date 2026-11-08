using System;

namespace IdleLoadshedding.Simulation
{
    public enum FeederBlockState
    {
        On,
        Off,
        Partial
    }

    public sealed class BlockForecast
    {
        public int BlockIndex;
        public int Ticks;
        public long DemandW;
        public long ImportCapW;
        public long ShortfallW;
        /// <summary>Powered ticks per district (content order) within this block.</summary>
        public int[] PoweredTicks = new int[0];

        public FeederBlockState StateOf(int districtIndex)
        {
            int powered = PoweredTicks[districtIndex];
            if (powered >= Ticks) return FeederBlockState.On;
            if (powered <= 0) return FeederBlockState.Off;
            return FeederBlockState.Partial;
        }
    }

    /// <summary>The predicted outcome of running the upcoming cut with one plan.</summary>
    public sealed class ForecastResult
    {
        public PlanKind Plan;
        public string ScenarioId;
        public long StartsInTicks;
        public BlockForecast[] Blocks = new BlockForecast[0];
        public CutStats Totals;
        public long BatteryStartWs;
        public long BatteryEndWs;
        public long FuelStartMl;
        public long FuelEndMl;
        public long PeakShortfallW;

        public bool FullyServed
        {
            get
            {
                foreach (var dark in Totals.DarkTicks)
                    if (dark > 0) return false;
                return true;
            }
        }
    }

    /// <summary>
    /// Predicts a cut by running the real rules on a copy of the state, so the preview and the
    /// live city can never disagree.
    /// </summary>
    public static class Forecast
    {
        /// <summary>Forecast for the upcoming cut, or null while a cut is already running.</summary>
        public static ForecastResult ForPlan(GameContent content, SimState state, DispatchPlan plan)
        {
            if (state.Phase == Phase.Running) return null;

            var sim = state.Clone();
            var scenario = GridRules.CurrentScenario(content, sim);
            long startsIn = Math.Max(0, sim.CutStartTick - sim.Tick);
            while (sim.Tick < sim.CutStartTick) GridRules.NormalTick(content, sim);

            var result = new ForecastResult
            {
                Plan = plan.Plan,
                ScenarioId = scenario.Id,
                StartsInTicks = startsIn,
                BatteryStartWs = sim.BatteryChargeWs,
                FuelStartMl = sim.FuelMl,
                Blocks = new BlockForecast[scenario.Blocks.Length]
            };

            int n = content.DistrictCount;
            for (int b = 0; b < scenario.Blocks.Length; b++)
            {
                var block = scenario.Blocks[b];
                long demand = Dispatcher.TotalDemandW(content, block);
                result.Blocks[b] = new BlockForecast
                {
                    BlockIndex = b,
                    Ticks = block.Ticks,
                    DemandW = demand,
                    ImportCapW = block.ImportCapW,
                    ShortfallW = Math.Max(0, demand - block.ImportCapW),
                    PoweredTicks = new int[n]
                };
                result.PeakShortfallW = Math.Max(result.PeakShortfallW, result.Blocks[b].ShortfallW);
            }

            sim.Phase = Phase.Prep;
            GridRules.BeginCut(content, sim, plan, manual: true);
            var stats = sim.Cut;
            var previous = new int[n];
            while (true)
            {
                int blockIndex = scenario.BlockAt(stats.TicksElapsed);
                var outcome = GridRules.Step(content, sim);
                for (int i = 0; i < n; i++)
                {
                    if (stats.ServedTicks[i] > previous[i]) result.Blocks[blockIndex].PoweredTicks[i]++;
                    previous[i] = stats.ServedTicks[i];
                }
                if (outcome == StepOutcome.CutFinished) break;
                if (outcome != StepOutcome.Ticked)
                    throw new InvalidOperationException("Forecast stopped unexpectedly: " + outcome);
            }

            result.Totals = stats.Clone();
            result.BatteryEndWs = sim.BatteryChargeWs;
            result.FuelEndMl = sim.FuelMl;
            return result;
        }

        /// <summary>Forecasts each plan using the draft's priorities and protected feeders.</summary>
        public static ForecastResult[] ForAllPlans(GameContent content, SimState state)
        {
            if (state.Phase == Phase.Running) return new ForecastResult[0];
            var results = new ForecastResult[PlanKinds.All.Length];
            for (int i = 0; i < PlanKinds.All.Length; i++)
            {
                var plan = state.Draft.Clone();
                plan.Plan = PlanKinds.All[i];
                results[i] = ForPlan(content, state, plan);
            }
            return results;
        }
    }
}
