using System;
using System.Collections.Generic;

namespace IdleLoadshedding.Simulation
{
    /// <summary>The three dispatch plans the player can choose for a cut.</summary>
    public enum PlanKind
    {
        Rotate = 0,
        Battery = 1,
        Diesel = 2
    }

    public static class PlanKinds
    {
        public static readonly PlanKind[] All = { PlanKind.Rotate, PlanKind.Battery, PlanKind.Diesel };

        public static string ToId(PlanKind plan)
        {
            switch (plan)
            {
                case PlanKind.Rotate: return "rotate";
                case PlanKind.Battery: return "battery";
                case PlanKind.Diesel: return "diesel";
                default: throw new ArgumentOutOfRangeException(nameof(plan));
            }
        }

        public static bool TryParse(string id, out PlanKind plan)
        {
            switch (id)
            {
                case "rotate": plan = PlanKind.Rotate; return true;
                case "battery": plan = PlanKind.Battery; return true;
                case "diesel": plan = PlanKind.Diesel; return true;
                default: plan = PlanKind.Rotate; return false;
            }
        }
    }

    public sealed class DistrictDef
    {
        public string Id;
        public string Name;
        public string FeederLabel;
        /// <summary>Presentation hint: homes, market or clinic.</summary>
        public string Kind;
        public long DemandW;
        public long RevenuePerTickCents;
        public bool ProtectedByDefault;
        public string Consequence;
    }

    public sealed class BatteryDef
    {
        public string Id;
        public string Name;
        public long MaxDischargeW;
        public long CapacityWs;
        public long StartChargeWs;
        public long RechargeW;
    }

    public sealed class GeneratorDef
    {
        public string Id;
        public string Name;
        public long MaxOutputW;
        public long FuelMlPerKWh;
        public long TankCapacityMl;
        public long StartFuelMl;
        public long FuelPriceCentsPerLitre;
    }

    public sealed class DemandOverride
    {
        public string DistrictId;
        public long DemandW;
        /// <summary>Income per powered tick during this block, or -1 to keep the district default.</summary>
        public long RevenuePerTickCents = -1;
    }

    public sealed class BlockDef
    {
        public int Ticks;
        public long ImportCapW;
        public DemandOverride[] Overrides = new DemandOverride[0];
    }

    public sealed class ScenarioDef
    {
        public string Id;
        public string Title;
        public string Briefing;
        public string StageLabel;
        public bool RequiresPlayer;
        /// <summary>Normal-operation ticks between the previous cut ending and this one starting.</summary>
        public int LeadTicks;
        /// <summary>Whether the player may fast-forward the lead time (campaign only).</summary>
        public bool AllowSkipLead;
        public BlockDef[] Blocks = new BlockDef[0];

        public int TotalTicks
        {
            get
            {
                int total = 0;
                foreach (var b in Blocks) total += b.Ticks;
                return total;
            }
        }

        /// <summary>Returns the block index containing the given tick offset into the cut.</summary>
        public int BlockAt(int tickIntoCut)
        {
            int acc = 0;
            for (int i = 0; i < Blocks.Length; i++)
            {
                acc += Blocks[i].Ticks;
                if (tickIntoCut < acc) return i;
            }
            return Blocks.Length - 1;
        }

        public int BlockStartTick(int blockIndex)
        {
            int acc = 0;
            for (int i = 0; i < blockIndex; i++) acc += Blocks[i].Ticks;
            return acc;
        }
    }

    public static class UpgradeEffectTypes
    {
        public const string BatteryCapacityWs = "batteryCapacityWs";
        public const string BatteryChargeWs = "batteryChargeWs";
        public const string GeneratorFuelMlPerKWh = "generatorFuelMlPerKWh";
        public const string FuelMl = "fuelMl";

        public static bool IsKnown(string type) =>
            type == BatteryCapacityWs || type == BatteryChargeWs || type == GeneratorFuelMlPerKWh || type == FuelMl;
    }

    public sealed class UpgradeEffect
    {
        public string Type;
        public long Value;
    }

    public sealed class UpgradeDef
    {
        public string Id;
        public string Name;
        public string Description;
        public long CostCents;
        public bool Repeatable;
        public UpgradeEffect[] Effects = new UpgradeEffect[0];
    }

    public sealed class TimingDef
    {
        public int SimSecondsPerTick = 60;
        public int StartMinuteOfDay = 17 * 60;
        public int RunTicksPerSecond = 10;
        public int SkipTicksPerSecond = 60;
        public long IdleRealMsPerTick = 60000;
        public long OfflineCapMs = 4L * 60 * 60 * 1000;
        public long CheckpointIntervalMs = 10000;
        public int MaxTicksPerFrame = 30;
        public long MaxFrameDeltaMs = 250;
    }

    /// <summary>Validated, immutable-by-convention authored data for one town.</summary>
    public sealed class GameContent
    {
        public int ContentVersion;
        public string TownId;
        public string TownName;
        public string Region;
        public long StartingCashCents;
        public TimingDef Timing = new TimingDef();
        /// <summary>Display order; arrays in <see cref="SimState"/> use these indices.</summary>
        public DistrictDef[] Districts = new DistrictDef[0];
        public string[] DefaultPriority = new string[0];
        public BatteryDef Battery;
        public GeneratorDef Generator;
        public ScenarioDef[] Scenarios = new ScenarioDef[0];
        public string[] Campaign = new string[0];
        public string RoutineScenarioId;
        public UpgradeDef[] Upgrades = new UpgradeDef[0];

        public int DistrictCount => Districts.Length;

        public int DistrictIndex(string id)
        {
            for (int i = 0; i < Districts.Length; i++)
                if (Districts[i].Id == id) return i;
            return -1;
        }

        public ScenarioDef FindScenario(string id)
        {
            foreach (var s in Scenarios)
                if (s.Id == id) return s;
            return null;
        }

        public UpgradeDef FindUpgrade(string id)
        {
            foreach (var u in Upgrades)
                if (u.Id == id) return u;
            return null;
        }

        public string[] SortedDistrictIds()
        {
            var ids = new string[Districts.Length];
            for (int i = 0; i < ids.Length; i++) ids[i] = Districts[i].Id;
            Array.Sort(ids, StringComparer.Ordinal);
            return ids;
        }

        public HashSet<string> DistrictIdSet()
        {
            var set = new HashSet<string>(StringComparer.Ordinal);
            foreach (var d in Districts) set.Add(d.Id);
            return set;
        }
    }
}
