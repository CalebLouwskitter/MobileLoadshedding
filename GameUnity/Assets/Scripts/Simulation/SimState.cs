using System;
using System.Collections.Generic;

namespace IdleLoadshedding.Simulation
{
    public enum Phase
    {
        /// <summary>A cut is due and needs the player's decision. Simulation time is frozen.</summary>
        Prep = 0,
        /// <summary>A cut is in progress, either run by the player or by the standing policy.</summary>
        Running = 1,
        /// <summary>Normal grid supply, counting down to the next scheduled cut.</summary>
        Normal = 2
    }

    public enum StopReason
    {
        None = 0,
        CapReached = 1,
        DecisionNeeded = 2,
        CutPaused = 3
    }

    /// <summary>Which plan runs a cut, plus the feeder priority order and protected feeders.</summary>
    public sealed class DispatchPlan
    {
        public PlanKind Plan;
        /// <summary>Every district ID, highest priority first.</summary>
        public string[] Priority = new string[0];
        /// <summary>Protected district IDs, sorted ordinally.</summary>
        public string[] Protected = new string[0];

        public static DispatchPlan Default(GameContent content)
        {
            var protectedIds = new List<string>();
            foreach (var d in content.Districts)
                if (d.ProtectedByDefault) protectedIds.Add(d.Id);
            protectedIds.Sort(StringComparer.Ordinal);
            return new DispatchPlan
            {
                Plan = PlanKind.Rotate,
                Priority = (string[])content.DefaultPriority.Clone(),
                Protected = protectedIds.ToArray()
            };
        }

        public bool IsProtected(string districtId) =>
            Array.BinarySearch(Protected, districtId, StringComparer.Ordinal) >= 0;

        public int PriorityRank(string districtId) => Array.IndexOf(Priority, districtId);

        public DispatchPlan Clone() => new DispatchPlan
        {
            Plan = Plan,
            Priority = (string[])Priority.Clone(),
            Protected = (string[])Protected.Clone()
        };

        public bool SameAs(DispatchPlan other)
        {
            if (other == null || other.Plan != Plan) return false;
            if (other.Priority.Length != Priority.Length || other.Protected.Length != Protected.Length) return false;
            for (int i = 0; i < Priority.Length; i++)
                if (Priority[i] != other.Priority[i]) return false;
            for (int i = 0; i < Protected.Length; i++)
                if (Protected[i] != other.Protected[i]) return false;
            return true;
        }
    }

    /// <summary>Running totals for one cut. Becomes the cut report when the cut ends.</summary>
    public sealed class CutStats
    {
        public string ScenarioId;
        public DispatchPlan PlanUsed;
        public bool Manual;
        public long StartTick;
        public int TotalTicks;
        public int TicksElapsed;
        public int[] ServedTicks = new int[0];
        public int[] DarkTicks = new int[0];
        public long IncomeCents;
        public long FuelUsedMl;
        public long BatteryUsedWs;
        public long ServedWs;
        public long DemandWs;
        public long BackupWs;

        public bool Finished => TicksElapsed >= TotalTicks;

        /// <summary>Share of demanded energy actually served, in basis points (10000 = 100%).</summary>
        public int ReliabilityBasisPoints => DemandWs <= 0 ? 10000 : (int)(ServedWs * 10000 / DemandWs);

        /// <summary>Replacement value of the diesel burned, in cents.</summary>
        public long FuelCostCents(GameContent content) =>
            FuelUsedMl * content.Generator.FuelPriceCentsPerLitre / 1000;

        public CutStats Clone() => new CutStats
        {
            ScenarioId = ScenarioId,
            PlanUsed = PlanUsed?.Clone(),
            Manual = Manual,
            StartTick = StartTick,
            TotalTicks = TotalTicks,
            TicksElapsed = TicksElapsed,
            ServedTicks = (int[])ServedTicks.Clone(),
            DarkTicks = (int[])DarkTicks.Clone(),
            IncomeCents = IncomeCents,
            FuelUsedMl = FuelUsedMl,
            BatteryUsedWs = BatteryUsedWs,
            ServedWs = ServedWs,
            DemandWs = DemandWs,
            BackupWs = BackupWs
        };
    }

    /// <summary>What the standing policy achieved while the player was away.</summary>
    public sealed class Receipt
    {
        public string Id;
        public long FromUtcMs;
        public long ToUtcMs;
        public long ElapsedMs;
        public long AwardedMs;
        public long ForfeitedMs;
        public long Ticks;
        public long IncomeCents;
        public long FuelUsedMl;
        public long BatteryUsedWs;
        public int CutsHandled;
        public long DarkFeederTicks;
        public StopReason Stop;

        public Receipt Clone() => (Receipt)MemberwiseClone();
    }

    public sealed class OwnedUpgrade
    {
        public string Id;
        public int Count;
    }

    /// <summary>
    /// The complete, serialisable simulation state. Only the rules mutate it, and the session
    /// coordinator always works on a clone until the change has been saved.
    /// </summary>
    public sealed class SimState
    {
        public const int RecentCommandCapacity = 32;

        public string TownId;
        public long Tick;
        public Phase Phase;
        /// <summary>Index of the campaign scenario still to be played; equals the campaign length once routine play begins.</summary>
        public int CampaignIndex;
        /// <summary>Scenario of the current cut (Prep or Running) or the next one (Normal).</summary>
        public string CutScenarioId;
        /// <summary>Absolute tick at which that cut starts or started.</summary>
        public long CutStartTick;
        public bool CutManual;
        /// <summary>The player asked to skip the campaign lead time; cleared at the next cut.</summary>
        public bool FastForward;

        public long CashCents;
        public long BatteryChargeWs;
        public long BatteryCapacityWs;
        public long FuelMl;
        /// <summary>Fuel burned but not yet deducted as a whole mL, in units of 1/3,600,000 mL.</summary>
        public long FuelCarry;
        public long FuelMlPerKWh;
        public long TankCapacityMl;

        public DispatchPlan Draft;
        public DispatchPlan Policy;
        /// <summary>Owned upgrades sorted by ID.</summary>
        public List<OwnedUpgrade> Upgrades = new List<OwnedUpgrade>();
        /// <summary>Whether each district (content order) is energised right now.</summary>
        public bool[] Powered = new bool[0];

        public CutStats Cut;
        public CutStats LastReport;
        public bool ReportAcknowledged = true;

        public long LifetimeIncomeCents;
        public int CutsCompleted;

        /// <summary>Real time (UTC ms) up to which elapsed time has been awarded or consumed.</summary>
        public long WatermarkUtcMs;
        public Receipt PendingReceipt;
        public List<string> RecentCommandIds = new List<string>();

        public int UpgradeCount(string upgradeId)
        {
            foreach (var u in Upgrades)
                if (u.Id == upgradeId) return u.Count;
            return 0;
        }

        public void AddUpgrade(string upgradeId)
        {
            foreach (var u in Upgrades)
            {
                if (u.Id == upgradeId)
                {
                    u.Count++;
                    return;
                }
            }
            Upgrades.Add(new OwnedUpgrade { Id = upgradeId, Count = 1 });
            Upgrades.Sort((a, b) => string.CompareOrdinal(a.Id, b.Id));
        }

        public bool HasSeenCommand(string commandId) => RecentCommandIds.Contains(commandId);

        public void RememberCommand(string commandId)
        {
            RecentCommandIds.Add(commandId);
            while (RecentCommandIds.Count > RecentCommandCapacity) RecentCommandIds.RemoveAt(0);
        }

        public SimState Clone()
        {
            var copy = (SimState)MemberwiseClone();
            copy.Draft = Draft?.Clone();
            copy.Policy = Policy?.Clone();
            copy.Upgrades = new List<OwnedUpgrade>(Upgrades.Count);
            foreach (var u in Upgrades) copy.Upgrades.Add(new OwnedUpgrade { Id = u.Id, Count = u.Count });
            copy.Powered = (bool[])Powered.Clone();
            copy.Cut = Cut?.Clone();
            copy.LastReport = LastReport?.Clone();
            copy.PendingReceipt = PendingReceipt?.Clone();
            copy.RecentCommandIds = new List<string>(RecentCommandIds);
            return copy;
        }
    }
}
