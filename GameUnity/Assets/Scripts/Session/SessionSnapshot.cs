using System;
using System.Text;
using IdleLoadshedding.Simulation;

namespace IdleLoadshedding.Session
{
    public enum BootState
    {
        NotStarted,
        Ready,
        NeedsRecovery,
        SaveError
    }

    public sealed class DistrictView
    {
        public string Id;
        public string Name;
        public string FeederLabel;
        public string Kind;
        public string Consequence;
        /// <summary>Demand in the current (or next) block.</summary>
        public long DemandW;
        public long RevenuePerTickCents;
        public bool Powered;
        public bool Protected;
        /// <summary>0 = highest priority.</summary>
        public int PriorityRank;
        public int ServedTicks;
        public int DarkTicks;
    }

    public sealed class UpgradeView
    {
        public string Id;
        public string Name;
        public string Description;
        public long CostCents;
        public int Owned;
        public bool Repeatable;
        public bool CanBuy;
        public string BlockReason;
    }

    /// <summary>
    /// An immutable read model for the views. Built after each committed command or tick batch;
    /// views never read <see cref="SimState"/> directly.
    /// </summary>
    public sealed class SessionSnapshot
    {
        public int Version;
        public BootState Boot;
        public bool InputEnabled;
        public string Notice = "";
        public string Error = "";
        public long Revision;

        public string TownName = "";
        public Phase Phase;
        public ScenarioDef Scenario;
        public bool IsRoutine;
        public bool CanSkip;
        public bool FastForward;
        public bool CutManual;

        public long Tick;
        public int Day;
        public int MinuteOfDay;
        public long TicksUntilCut;
        public int CutTicksElapsed;
        public int CutTotalTicks;
        public int CurrentBlock;
        public long CurrentDemandW;
        public long CurrentImportCapW;
        public long CurrentBackupW;

        public long CashCents;
        public long FuelMl;
        public long TankCapacityMl;
        public long FuelMlPerKWh;
        public long BatteryChargeWs;
        public long BatteryCapacityWs;
        public long BatteryMaxDischargeW;
        public long GeneratorMaxOutputW;

        public DistrictView[] Districts = new DistrictView[0];
        public DispatchPlan Draft;
        public DispatchPlan Policy;
        public bool DraftIsPolicy;

        /// <summary>Indexed by <see cref="PlanKind"/>; empty while a cut runs or during fast-forward.</summary>
        public ForecastResult[] Forecasts = new ForecastResult[0];
        public ForecastResult PolicyForecast;

        public CutStats LiveCut;
        public CutStats LastReport;
        public bool ShowReport;
        public Receipt PendingReceipt;
        public UpgradeView[] Shop = new UpgradeView[0];

        public LoadOutcome Recovery;

        public ForecastResult SelectedForecast =>
            Draft != null && Forecasts.Length > (int)Draft.Plan ? Forecasts[(int)Draft.Plan] : null;

        /// <summary>Compact light state, e.g. "homes:1,market:0,clinic:1", for logs and device checks.</summary>
        public string LightsSignature
        {
            get
            {
                var sb = new StringBuilder();
                foreach (var d in Districts)
                {
                    if (sb.Length > 0) sb.Append(',');
                    sb.Append(d.Id).Append(':').Append(d.Powered ? '1' : '0');
                }
                return sb.ToString();
            }
        }

        public static SessionSnapshot Build(GameContent content, SimState s, int version, BootState boot, bool inputEnabled, long revision, string notice, string error, bool includeForecasts)
        {
            var snap = new SessionSnapshot
            {
                Version = version,
                Boot = boot,
                InputEnabled = inputEnabled,
                Notice = notice ?? "",
                Error = error ?? "",
                Revision = revision,
                TownName = content.TownName
            };
            if (s == null) return snap;

            var scenario = GridRules.CurrentScenario(content, s);
            snap.Phase = s.Phase;
            snap.Scenario = scenario;
            snap.IsRoutine = GridRules.IsRoutine(content, s);
            snap.CanSkip = s.Phase == Phase.Normal && scenario.AllowSkipLead && !snap.IsRoutine && !s.FastForward;
            snap.FastForward = s.FastForward;
            snap.CutManual = s.CutManual;
            snap.Tick = s.Tick;
            GridRules.ClockAt(content, s.Tick, out snap.Day, out snap.MinuteOfDay);
            snap.TicksUntilCut = s.Phase == Phase.Normal ? Math.Max(0, s.CutStartTick - s.Tick) : 0;

            int block = 0;
            if (s.Phase == Phase.Running && s.Cut != null)
            {
                snap.CutTicksElapsed = s.Cut.TicksElapsed;
                snap.CutTotalTicks = s.Cut.TotalTicks;
                block = scenario.BlockAt(Math.Min(s.Cut.TicksElapsed, Math.Max(0, s.Cut.TotalTicks - 1)));
                snap.LiveCut = s.Cut.Clone();
            }
            else
            {
                snap.CutTotalTicks = scenario.TotalTicks;
            }
            snap.CurrentBlock = block;
            var blockDef = scenario.Blocks[block];
            snap.CurrentDemandW = Dispatcher.TotalDemandW(content, blockDef);
            snap.CurrentImportCapW = blockDef.ImportCapW;
            if (s.Phase == Phase.Running)
            {
                long served = 0;
                for (int i = 0; i < content.DistrictCount; i++)
                    if (s.Powered[i]) served += Dispatcher.DemandOf(content, blockDef, i);
                snap.CurrentBackupW = Math.Max(0, served - blockDef.ImportCapW);
            }

            snap.CashCents = s.CashCents;
            snap.FuelMl = s.FuelMl;
            snap.TankCapacityMl = s.TankCapacityMl;
            snap.FuelMlPerKWh = s.FuelMlPerKWh;
            snap.BatteryChargeWs = s.BatteryChargeWs;
            snap.BatteryCapacityWs = s.BatteryCapacityWs;
            snap.BatteryMaxDischargeW = content.Battery.MaxDischargeW;
            snap.GeneratorMaxOutputW = content.Generator.MaxOutputW;

            snap.Draft = s.Draft.Clone();
            snap.Policy = s.Policy?.Clone();
            snap.DraftIsPolicy = s.Policy != null && s.Policy.SameAs(s.Draft);

            var plan = s.Phase == Phase.Running && s.Cut != null ? s.Cut.PlanUsed : s.Draft;
            snap.Districts = new DistrictView[content.DistrictCount];
            for (int i = 0; i < content.DistrictCount; i++)
            {
                var d = content.Districts[i];
                snap.Districts[i] = new DistrictView
                {
                    Id = d.Id,
                    Name = d.Name,
                    FeederLabel = d.FeederLabel,
                    Kind = d.Kind,
                    Consequence = d.Consequence,
                    DemandW = Dispatcher.DemandOf(content, s.Phase == Phase.Running ? blockDef : null, i),
                    RevenuePerTickCents = Dispatcher.RevenueOf(content, s.Phase == Phase.Running ? blockDef : null, i),
                    Powered = s.Powered[i],
                    Protected = plan.IsProtected(d.Id),
                    PriorityRank = plan.PriorityRank(d.Id),
                    ServedTicks = s.Cut != null ? s.Cut.ServedTicks[i] : 0,
                    DarkTicks = s.Cut != null ? s.Cut.DarkTicks[i] : 0
                };
            }

            if (includeForecasts && s.Phase != Phase.Running && !s.FastForward)
            {
                snap.Forecasts = Forecast.ForAllPlans(content, s);
                if (s.Policy != null) snap.PolicyForecast = Forecast.ForPlan(content, s, s.Policy);
            }

            snap.LastReport = s.LastReport?.Clone();
            snap.ShowReport = s.LastReport != null && !s.ReportAcknowledged;
            snap.PendingReceipt = s.PendingReceipt?.Clone();

            snap.Shop = new UpgradeView[content.Upgrades.Length];
            for (int i = 0; i < content.Upgrades.Length; i++)
            {
                var u = content.Upgrades[i];
                string reason = CommandRules.UpgradeBlockReason(content, s, u);
                snap.Shop[i] = new UpgradeView
                {
                    Id = u.Id,
                    Name = u.Name,
                    Description = u.Description,
                    CostCents = u.CostCents,
                    Owned = s.UpgradeCount(u.Id),
                    Repeatable = u.Repeatable,
                    CanBuy = reason.Length == 0,
                    BlockReason = reason
                };
            }
            return snap;
        }
    }
}
