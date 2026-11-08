using System;
using System.Collections.Generic;

namespace IdleLoadshedding.Simulation
{
    /// <summary>The outcome of dispatching power for one tick. Resource deltas are not yet applied.</summary>
    public sealed class DispatchResult
    {
        public bool[] Powered;
        public long DemandW;
        public long ImportCapW;
        public long ServedW;
        public long GridW;
        public long BackupW;
        public long BackupAvailableW;
        public long IncomeCents;
        public long BatteryUsedWs;
        public long FuelUsedMl;
        public long FuelCarryAfter;
    }

    /// <summary>
    /// Decides which feeders are energised during a cut tick. Feeders are all-or-nothing and are
    /// placed first-fit in service order, so unserved load can never be counted as served.
    /// </summary>
    public static class Dispatcher
    {
        /// <summary>W·s in one kWh.</summary>
        public const long WsPerKWh = 3600000;

        public static long DemandOf(GameContent content, BlockDef block, int districtIndex)
        {
            var district = content.Districts[districtIndex];
            if (block != null)
            {
                foreach (var o in block.Overrides)
                    if (o.DistrictId == district.Id) return o.DemandW;
            }
            return district.DemandW;
        }

        public static long RevenueOf(GameContent content, BlockDef block, int districtIndex)
        {
            var district = content.Districts[districtIndex];
            if (block != null)
            {
                foreach (var o in block.Overrides)
                    if (o.DistrictId == district.Id && o.RevenuePerTickCents >= 0) return o.RevenuePerTickCents;
            }
            return district.RevenuePerTickCents;
        }

        public static long TotalDemandW(GameContent content, BlockDef block)
        {
            long total = 0;
            for (int i = 0; i < content.DistrictCount; i++) total += DemandOf(content, block, i);
            return total;
        }

        /// <summary>
        /// Protected feeders first (in priority order), then the rest. A rotation plan rotates the
        /// unprotected feeders by block so each takes a turn; other plans keep priority order.
        /// </summary>
        public static string[] ServiceOrder(DispatchPlan plan, int blockIndex)
        {
            var order = new List<string>(plan.Priority.Length);
            var rest = new List<string>(plan.Priority.Length);
            foreach (var id in plan.Priority)
            {
                if (plan.IsProtected(id)) order.Add(id);
                else rest.Add(id);
            }
            if (plan.Plan == PlanKind.Rotate && rest.Count > 1)
            {
                int shift = blockIndex % rest.Count;
                for (int i = 0; i < rest.Count; i++) order.Add(rest[(i + shift) % rest.Count]);
            }
            else
            {
                order.AddRange(rest);
            }
            return order.ToArray();
        }

        /// <summary>Most backup power the plan's source can deliver for one whole tick.</summary>
        public static long BackupAvailableW(GameContent content, PlanKind plan, long chargeWs, long fuelMl, long fuelCarry, long fuelMlPerKWh)
        {
            long seconds = content.Timing.SimSecondsPerTick;
            switch (plan)
            {
                case PlanKind.Battery:
                    return Math.Min(content.Battery.MaxDischargeW, Math.Max(0, chargeWs) / seconds);
                case PlanKind.Diesel:
                {
                    if (fuelMlPerKWh <= 0) return 0;
                    long fineAvailable = fuelMl * WsPerKWh - fuelCarry;
                    if (fineAvailable <= 0) return 0;
                    long maxWs = fineAvailable / fuelMlPerKWh;
                    return Math.Min(content.Generator.MaxOutputW, maxWs / seconds);
                }
                default:
                    return 0;
            }
        }

        public static DispatchResult Dispatch(
            GameContent content,
            ScenarioDef scenario,
            int blockIndex,
            DispatchPlan plan,
            long chargeWs,
            long fuelMl,
            long fuelCarry,
            long fuelMlPerKWh)
        {
            var block = scenario.Blocks[blockIndex];
            long seconds = content.Timing.SimSecondsPerTick;
            int n = content.DistrictCount;

            var result = new DispatchResult
            {
                Powered = new bool[n],
                ImportCapW = block.ImportCapW,
                DemandW = TotalDemandW(content, block),
                BackupAvailableW = BackupAvailableW(content, plan.Plan, chargeWs, fuelMl, fuelCarry, fuelMlPerKWh),
                FuelCarryAfter = fuelCarry
            };

            long remaining = block.ImportCapW + result.BackupAvailableW;
            foreach (var id in ServiceOrder(plan, blockIndex))
            {
                int index = content.DistrictIndex(id);
                long demand = DemandOf(content, block, index);
                if (demand <= remaining)
                {
                    result.Powered[index] = true;
                    remaining -= demand;
                    result.ServedW += demand;
                    result.IncomeCents += RevenueOf(content, block, index);
                }
            }

            result.GridW = Math.Min(result.ServedW, block.ImportCapW);
            result.BackupW = result.ServedW - result.GridW;

            if (result.BackupW > 0)
            {
                long backupWs = result.BackupW * seconds;
                if (plan.Plan == PlanKind.Battery)
                {
                    result.BatteryUsedWs = backupWs;
                }
                else if (plan.Plan == PlanKind.Diesel)
                {
                    long fine = backupWs * fuelMlPerKWh + fuelCarry;
                    result.FuelUsedMl = fine / WsPerKWh;
                    result.FuelCarryAfter = fine % WsPerKWh;
                }
            }

            return result;
        }
    }
}
