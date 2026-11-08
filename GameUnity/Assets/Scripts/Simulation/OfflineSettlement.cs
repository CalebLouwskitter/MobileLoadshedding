using System;

namespace IdleLoadshedding.Simulation
{
    public sealed class SettlementResult
    {
        public long ElapsedMs;
        public long Ticks;
        public long IncomeCents;
        public int CutsHandled;
        public StopReason Stop;
        /// <summary>True when the state changed (ticks ran or the watermark moved).</summary>
        public bool Changed;
        /// <summary>Only the watermark moved because the town is waiting on the player.</summary>
        public bool ConsumedOnly;
    }

    /// <summary>
    /// Converts elapsed real time into simulated ticks under the standing policy.
    /// The watermark only moves forward, so a clock rollback or a repeated resume awards nothing twice.
    /// Ticks, balances, watermark and receipt change together on the same state object, which the
    /// session coordinator commits in one save.
    /// </summary>
    public static class OfflineSettlement
    {
        public static SettlementResult Settle(GameContent content, SimState state, long nowUtcMs, bool createReceipt)
        {
            var result = new SettlementResult();
            long elapsed = nowUtcMs - state.WatermarkUtcMs;
            if (elapsed <= 0)
            {
                // Clock rollback (or no time passed): award nothing and keep the watermark where it is.
                return result;
            }
            result.ElapsedMs = elapsed;

            if (!GridRules.IsIdleEligible(state))
            {
                // Waiting for a decision, or a cut the player is running: that time is consumed, not banked.
                state.WatermarkUtcMs = nowUtcMs;
                result.Changed = true;
                result.ConsumedOnly = true;
                result.Stop = state.Phase == Phase.Prep ? StopReason.DecisionNeeded : StopReason.CutPaused;
                return result;
            }

            var timing = content.Timing;
            long eligible = Math.Min(elapsed, timing.OfflineCapMs);
            long forfeited = elapsed - eligible;
            long available = eligible / timing.IdleRealMsPerTick;

            long cashBefore = state.CashCents;
            long fuelBefore = state.FuelMl;
            long watermarkBefore = state.WatermarkUtcMs;
            long batteryUsed = 0;
            long darkTicks = 0;
            long ticks = 0;

            while (ticks < available)
            {
                long chargeBefore = state.BatteryChargeWs;
                var cutBefore = state.Cut;
                long darkBefore = DarkSum(cutBefore);
                var outcome = GridRules.Step(content, state);
                if (outcome == StepOutcome.GateReached || outcome == StepOutcome.Frozen)
                {
                    result.Stop = StopReason.DecisionNeeded;
                    break;
                }
                ticks++;
                if (state.BatteryChargeWs < chargeBefore) batteryUsed += chargeBefore - state.BatteryChargeWs;

                // The cut that ran this tick is the same object whether it is still running or has
                // just become the last report; a cut may also have started on this tick.
                var cutAfter = cutBefore ?? state.Cut ?? (outcome == StepOutcome.CutFinished ? state.LastReport : null);
                darkTicks += DarkSum(cutAfter) - (cutBefore == null ? 0 : darkBefore);
                if (outcome == StepOutcome.CutFinished) result.CutsHandled++;
            }

            if (result.Stop == StopReason.None && forfeited > 0) result.Stop = StopReason.CapReached;

            // At a decision gate, or past the cap, the remaining time is consumed. Otherwise the
            // unconverted remainder stays behind the watermark for the next settlement.
            if (result.Stop != StopReason.None) state.WatermarkUtcMs = nowUtcMs;
            else state.WatermarkUtcMs += ticks * timing.IdleRealMsPerTick;

            result.Ticks = ticks;
            result.IncomeCents = state.CashCents - cashBefore;
            result.Changed = ticks > 0 || state.WatermarkUtcMs != watermarkBefore;

            if (createReceipt && (ticks > 0 || result.Stop == StopReason.DecisionNeeded || forfeited > 0))
            {
                var receipt = state.PendingReceipt?.Clone() ?? new Receipt { FromUtcMs = nowUtcMs - elapsed };
                receipt.ToUtcMs = nowUtcMs;
                receipt.ElapsedMs += elapsed;
                receipt.AwardedMs += ticks * timing.IdleRealMsPerTick;
                receipt.ForfeitedMs += forfeited;
                receipt.Ticks += ticks;
                receipt.IncomeCents += result.IncomeCents;
                receipt.FuelUsedMl += fuelBefore - state.FuelMl;
                receipt.BatteryUsedWs += batteryUsed;
                receipt.CutsHandled += result.CutsHandled;
                receipt.DarkFeederTicks += darkTicks;
                receipt.Stop = result.Stop;
                receipt.Id = "receipt-" + receipt.FromUtcMs + "-" + receipt.ToUtcMs;
                state.PendingReceipt = receipt;
            }

            return result;
        }

        private static long DarkSum(CutStats cut)
        {
            if (cut == null) return 0;
            long sum = 0;
            foreach (var dark in cut.DarkTicks) sum += dark;
            return sum;
        }
    }
}
