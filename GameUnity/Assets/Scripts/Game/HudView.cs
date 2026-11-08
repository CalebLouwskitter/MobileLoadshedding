using System;
using System.Collections.Generic;
using System.Text;
using IdleLoadshedding.Session;
using IdleLoadshedding.Simulation;
using UnityEngine;
using UnityEngine.UIElements;

namespace IdleLoadshedding.Game
{
    /// <summary>
    /// Screen-space HUD built with UI Toolkit in code. It renders snapshots and turns taps into
    /// commands. Each actionable button carries one command ID for its lifetime, so a double tap
    /// is deduplicated by the session rather than spending twice.
    /// </summary>
    public sealed class HudView : MonoBehaviour
    {
        private const string ThemeResource = "UI/IdleRuntimeTheme";
        private const string StyleResource = "UI/Hud";

        private UIDocument _document;
        private VisualElement _root;
        private VisualElement _safe;
        private VisualElement _topBar;
        private VisualElement _sheet;
        private VisualElement _modalLayer;
        private VisualElement _labelsLayer;
        private Label _toast;
        private float _toastSeconds;
        private int _noticeVersion = -1;

        private Label _cash, _fuel, _battery, _clock, _phase;
        private Label _countdown;
        private Label _runTitle, _runLive;
        private VisualElement _runFill;
        private readonly Dictionary<string, Label> _runRows = new Dictionary<string, Label>();
        private readonly Dictionary<string, VisualElement> _runBulbs = new Dictionary<string, VisualElement>();

        private readonly Dictionary<string, Action> _actions = new Dictionary<string, Action>();
        private readonly Dictionary<string, WorldLabel> _worldLabels = new Dictionary<string, WorldLabel>();

        private GameContent _content;
        private Func<GameCommand, CommandResult> _submit;
        private Action<bool> _recover;
        private Action _retry;
        private CityView _city;
        private SessionSnapshot _snapshot;
        private string _layoutKey = "";
        private bool _shopOpen;
        private Rect _appliedSafeArea;

        private sealed class WorldLabel
        {
            public VisualElement Root;
            public Label Status;
            public Label Detail;
            public VisualElement Bulb;
            public Vector3 Anchor;
        }

        public static HudView Create(Transform parent)
        {
            var go = new GameObject("HUD");
            go.transform.SetParent(parent, false);
            go.SetActive(false);
            var hud = go.AddComponent<HudView>();
            hud._document = go.AddComponent<UIDocument>();
            hud._document.panelSettings = CreatePanelSettings();
            go.SetActive(true);
            hud.BuildFrame();
            return hud;
        }

        private static PanelSettings CreatePanelSettings()
        {
            var settings = ScriptableObject.CreateInstance<PanelSettings>();
            settings.name = "IdleLoadshedding Panel";
            settings.themeStyleSheet = Resources.Load<ThemeStyleSheet>(ThemeResource);
            settings.scaleMode = PanelScaleMode.ScaleWithScreenSize;
            settings.referenceResolution = new Vector2Int(1080, 1920);
            settings.screenMatchMode = PanelScreenMatchMode.MatchWidthOrHeight;
            settings.match = 0f; // portrait: scale with width so text stays readable
            if (settings.themeStyleSheet == null)
                Debug.LogWarning("[ILS] Runtime theme missing at Resources/" + ThemeResource + "; text may not render.");
            return settings;
        }

        public void Bind(GameContent content, Func<GameCommand, CommandResult> submit, Action<bool> recover, Action retry)
        {
            _content = content;
            _submit = submit;
            _recover = recover;
            _retry = retry;
        }

        public void BindCity(CityView city)
        {
            _city = city;
        }

        /// <summary>Presses a named button the way a tap would, for Play Mode tests. Returns false if it is missing or disabled.</summary>
        public bool Press(string buttonName)
        {
            var button = _root.Q<Button>(buttonName);
            if (button == null || !button.enabledInHierarchy || !_actions.TryGetValue(buttonName, out var action)) return false;
            action();
            return true;
        }

        public bool HasButton(string buttonName) => _root.Q<Button>(buttonName) != null;

        private void BuildFrame()
        {
            _root = _document.rootVisualElement;
            var style = Resources.Load<StyleSheet>(StyleResource);
            if (style != null) _root.styleSheets.Add(style);
            else Debug.LogWarning("[ILS] HUD style sheet missing at Resources/" + StyleResource);
            _root.AddToClassList("hud-root");

            _labelsLayer = new VisualElement { name = "world-labels", pickingMode = PickingMode.Ignore };
            _labelsLayer.AddToClassList("layer");
            _root.Add(_labelsLayer);

            _safe = new VisualElement { name = "safe-area", pickingMode = PickingMode.Ignore };
            _safe.AddToClassList("layer");
            _safe.AddToClassList("safe");
            _root.Add(_safe);

            _topBar = new VisualElement { name = "top-bar" };
            _topBar.AddToClassList("top-bar");
            _cash = Stat("Cash");
            _fuel = Stat("Diesel");
            _battery = Stat("Battery");
            _clock = Stat("Town time");
            _safe.Add(_topBar);

            _phase = new Label { name = "phase-chip" };
            _phase.AddToClassList("phase-chip");
            _safe.Add(_phase);

            var spacer = new VisualElement { pickingMode = PickingMode.Ignore };
            spacer.AddToClassList("spacer");
            _safe.Add(spacer);

            _sheet = new ScrollView(ScrollViewMode.Vertical) { name = "sheet" };
            _sheet.AddToClassList("sheet");
            _safe.Add(_sheet);

            _modalLayer = new VisualElement { name = "modal-layer" };
            _modalLayer.AddToClassList("layer");
            _modalLayer.AddToClassList("modal-layer");
            _modalLayer.style.display = DisplayStyle.None;
            _root.Add(_modalLayer);

            _toast = new Label { name = "toast", pickingMode = PickingMode.Ignore };
            _toast.AddToClassList("toast");
            _toast.style.display = DisplayStyle.None;
            _root.Add(_toast);

            _root.RegisterCallback<GeometryChangedEvent>(_ => ApplySafeArea(force: true));
        }

        private Label Stat(string caption)
        {
            var box = new VisualElement();
            box.AddToClassList("stat");
            var label = new Label(caption);
            label.AddToClassList("stat-label");
            var value = new Label("-");
            value.AddToClassList("stat-value");
            box.Add(label);
            box.Add(value);
            _topBar.Add(box);
            return value;
        }

        public void ShowLoading(string text)
        {
            _sheet.Clear();
            _sheet.Add(Card("Starting up", text));
            _layoutKey = "loading";
        }

        public void ShowFatal(string text)
        {
            _sheet.Clear();
            _sheet.Add(Card("The game could not start", text));
            _layoutKey = "fatal";
        }

        public void ShowToast(string text)
        {
            if (string.IsNullOrEmpty(text)) return;
            _toast.text = text;
            _toast.style.display = DisplayStyle.Flex;
            _toastSeconds = 2.8f;
        }

        public void Render(SessionSnapshot snap)
        {
            _snapshot = snap;
            if (!string.IsNullOrEmpty(snap.Notice) && snap.Version != _noticeVersion)
            {
                _noticeVersion = snap.Version;
                ShowToast(snap.Notice);
            }

            string key = LayoutKey(snap);
            if (key != _layoutKey)
            {
                _layoutKey = key;
                Rebuild(snap);
            }
            UpdateDynamic(snap);
        }

        private void Update()
        {
            if (_toastSeconds > 0f)
            {
                _toastSeconds -= Time.unscaledDeltaTime;
                if (_toastSeconds <= 0f) _toast.style.display = DisplayStyle.None;
            }
            ApplySafeArea(force: false);
            PlaceWorldLabels();
        }

        // ----- Layout -----

        private string LayoutKey(SessionSnapshot s)
        {
            var sb = new StringBuilder();
            sb.Append(s.Boot).Append('|').Append(s.Phase).Append('|').Append(s.ShowReport).Append('|')
              .Append(s.PendingReceipt?.Id).Append('|').Append(_shopOpen).Append('|').Append(s.Scenario?.Id).Append('|')
              .Append(s.CanSkip).Append('|').Append(s.FastForward).Append('|').Append(s.IsRoutine).Append('|').Append(s.DraftIsPolicy).Append('|');
            if (s.Draft != null)
                sb.Append(s.Draft.Plan).Append(':').Append(string.Join(",", s.Draft.Priority)).Append(':').Append(string.Join(",", s.Draft.Protected)).Append('|');
            if (s.Policy != null) sb.Append("policy=").Append(s.Policy.Plan).Append(string.Join(",", s.Policy.Priority)).Append('|');
            foreach (var u in s.Shop) sb.Append(u.Id).Append(u.Owned).Append(u.CanBuy ? '+' : '-');
            foreach (var f in s.Forecasts)
                if (f != null) sb.Append('|').Append(f.Totals.IncomeCents).Append(',').Append(string.Join(".", f.Totals.ServedTicks)).Append(',').Append(f.FuelStartMl).Append(',').Append(f.BatteryStartWs);
            if (s.Recovery != null) sb.Append("|recovery=").Append(s.Recovery.ProblemKind);
            sb.Append('|').Append(s.Error);
            return sb.ToString();
        }

        private void Rebuild(SessionSnapshot s)
        {
            _actions.Clear();
            _sheet.Clear();
            _modalLayer.Clear();
            _modalLayer.style.display = DisplayStyle.None;
            _countdown = null;
            _runFill = null;
            _runRows.Clear();
            _runBulbs.Clear();
            EnsureWorldLabels(s);

            switch (s.Boot)
            {
                case BootState.NotStarted:
                    _sheet.Add(Card("Starting up", "Loading " + s.TownName + "..."));
                    return;
                case BootState.SaveError:
                    var error = Card("Could not save your town", s.Error + " Free some storage space, then try again.");
                    error.Add(PlainButton("retry-boot", "Try again", "btn-primary", () => _retry?.Invoke()));
                    _sheet.Add(error);
                    return;
                case BootState.NeedsRecovery:
                    ShowModal(RecoveryCard(s));
                    return;
            }

            switch (s.Phase)
            {
                case Phase.Prep: BuildPrep(s); break;
                case Phase.Running: BuildRunning(s); break;
                case Phase.Normal: BuildNormal(s); break;
            }

            if (s.PendingReceipt != null) ShowModal(ReceiptCard(s));
            else if (_shopOpen) ShowModal(ShopCard(s));
        }

        private void BuildPrep(SessionSnapshot s)
        {
            var header = Card(s.Scenario.Title, s.Scenario.Briefing);
            header.Insert(0, Tag(s.Scenario.StageLabel + "  " + CutWindow(s)));
            var f = s.SelectedForecast;
            if (f != null)
            {
                var gap = Text("Needed " + DisplayFormat.Power(f.Blocks[0].DemandW) + "   Grid limit " + DisplayFormat.Power(f.Blocks[0].ImportCapW) +
                               "   Short " + DisplayFormat.Power(f.PeakShortfallW), "headline");
                gap.name = "shortfall";
                header.Add(gap);
            }
            _sheet.Add(header);
            _sheet.Add(PlanSection(s, "Choose a plan"));
            _sheet.Add(PriorityCard(s));
            var run = CommandButton("run-cut", "Run the cut", "btn-primary", id => GameCommand.StartCut(id));
            run.AddToClassList("btn-wide");
            _sheet.Add(run);
        }

        private void BuildRunning(SessionSnapshot s)
        {
            var card = Card(s.Scenario.Title, "");
            _runTitle = Text("", "headline");
            card.Add(_runTitle);
            var bar = new VisualElement();
            bar.AddToClassList("progress");
            _runFill = new VisualElement();
            _runFill.AddToClassList("progress-fill");
            bar.Add(_runFill);
            card.Add(bar);
            _runLive = Text("", "body");
            card.Add(_runLive);
            foreach (var d in s.Districts)
            {
                var row = Row();
                var bulb = Bulb(d.Powered);
                row.Add(bulb);
                var label = Text("", "body");
                row.Add(label);
                card.Add(row);
                _runRows[d.Id] = label;
                _runBulbs[d.Id] = bulb;
            }
            card.Add(Text("Plan: " + DisplayFormat.PlanName(s.LiveCut.PlanUsed.Plan) + (s.CutManual ? "" : " (standing policy)"), "muted"));
            _sheet.Add(card);
        }

        private void BuildNormal(SessionSnapshot s)
        {
            if (s.ShowReport && s.LastReport != null)
            {
                _sheet.Add(ReportCard(s));
                return;
            }

            var next = Card("Next cut: " + s.Scenario.Title, "");
            _countdown = Text("", "headline");
            _countdown.name = "countdown";
            next.Add(_countdown);
            if (s.Policy != null && s.PolicyForecast != null)
                next.Add(Text("Standing policy: " + DisplayFormat.PlanName(s.Policy.Plan) + ". " + Outcome(s.PolicyForecast), "body"));
            else if (s.IsRoutine)
                next.Add(Text("No standing policy yet, so this cut will wait for you.", "body"));
            else
                next.Add(Text(s.Scenario.Briefing, "body"));
            if (s.FastForward) next.Add(Text("Skipping ahead...", "muted"));

            var buttons = Row();
            if (s.CanSkip) buttons.Add(CommandButton("skip", "Skip to the cut", "btn-primary", id => GameCommand.SkipToNextCut(id)));
            buttons.Add(PlainButton("open-shop", "Upgrades", "btn-secondary", () => { _shopOpen = true; ForceRebuild(); }));
            next.Add(buttons);
            _sheet.Add(next);

            if (s.Forecasts.Length > 0)
            {
                _sheet.Add(PlanSection(s, "Plan for the next cut"));
                _sheet.Add(PriorityCard(s));
            }
        }

        private VisualElement PlanSection(SessionSnapshot s, string title)
        {
            var card = Card(title, "");
            foreach (var plan in PlanKinds.All)
            {
                var f = s.Forecasts.Length > (int)plan ? s.Forecasts[(int)plan] : null;
                bool selected = s.Draft.Plan == plan;
                var button = CommandButton("plan-" + PlanKinds.ToId(plan), "", "plan-card", id => GameCommand.SelectPlan(id, plan));
                button.EnableInClassList("plan-card--selected", selected);
                var name = Text((selected ? "SELECTED  " : "") + DisplayFormat.PlanName(plan), "plan-title");
                name.pickingMode = PickingMode.Ignore;
                button.Add(name);
                var summary = Text(DisplayFormat.PlanSummary(plan), "muted");
                summary.pickingMode = PickingMode.Ignore;
                button.Add(summary);
                if (f != null)
                {
                    var outcome = Text(Outcome(f) + ResourceUse(f), "body");
                    outcome.pickingMode = PickingMode.Ignore;
                    button.Add(outcome);
                }
                card.Add(button);
            }

            var selectedForecast = s.SelectedForecast;
            if (selectedForecast != null) card.Add(PreviewGrid(s, selectedForecast));
            if (!s.DraftIsPolicy)
                card.Add(CommandButton("save-policy", "Save as standing policy", "btn-secondary", id => GameCommand.SavePolicy(id)));
            else
                card.Add(Text("This plan is your standing policy.", "muted"));
            return card;
        }

        private VisualElement PreviewGrid(SessionSnapshot s, ForecastResult f)
        {
            var grid = new VisualElement { name = "preview" };
            grid.AddToClassList("preview");
            var header = Row();
            header.Add(Text("Feeder", "grid-head"));
            var scenario = s.Scenario;
            for (int b = 0; b < f.Blocks.Length; b++)
            {
                long tick = s.Tick + f.StartsInTicks + scenario.BlockStartTick(b);
                GridRules.ClockAt(_content, tick, out _, out var minute);
                header.Add(Text(DisplayFormat.Clock(minute), "grid-head"));
            }
            grid.Add(header);
            for (int i = 0; i < s.Districts.Length; i++)
            {
                var row = Row();
                row.Add(Text(s.Districts[i].Name, "grid-name"));
                foreach (var block in f.Blocks)
                {
                    var state = block.StateOf(i);
                    string text = state == FeederBlockState.On ? "ON" : state == FeederBlockState.Off ? "OFF" : "ON " + DisplayFormat.SimDuration(_content, block.PoweredTicks[i]);
                    var pill = Text(text, "pill");
                    pill.AddToClassList(state == FeederBlockState.On ? "pill--on" : state == FeederBlockState.Off ? "pill--off" : "pill--part");
                    row.Add(pill);
                }
                grid.Add(row);
            }
            var shortRow = Row();
            shortRow.Add(Text("Short", "grid-name"));
            foreach (var block in f.Blocks) shortRow.Add(Text(DisplayFormat.Power(block.ShortfallW), "grid-cell"));
            grid.Add(shortRow);
            return grid;
        }

        private VisualElement PriorityCard(SessionSnapshot s)
        {
            var card = Card("Feeder priority", "Protected feeders are served first. The rest follow this order, or take turns when rotating.");
            var ordered = new List<DistrictView>(s.Districts);
            ordered.Sort((a, b) => a.PriorityRank.CompareTo(b.PriorityRank));
            foreach (var d in ordered)
            {
                var row = Row();
                row.AddToClassList("priority-row");
                row.Add(Text("#" + (d.PriorityRank + 1) + " " + d.Name + "  " + DisplayFormat.Power(d.DemandW), "body"));
                if (d.Protected) row.Add(Tag("PROTECTED"));
                var spacer = new VisualElement();
                spacer.AddToClassList("grow");
                row.Add(spacer);
                string id = d.Id;
                row.Add(CommandButton("protect-" + id, d.Protected ? "Unprotect" : "Protect", "btn-small", cmd => GameCommand.SetProtected(cmd, id, !d.Protected)));
                row.Add(CommandButton("up-" + id, "Up", "btn-small", cmd => GameCommand.MovePriority(cmd, id, -1), d.PriorityRank > 0));
                row.Add(CommandButton("down-" + id, "Down", "btn-small", cmd => GameCommand.MovePriority(cmd, id, 1), d.PriorityRank < s.Districts.Length - 1));
                card.Add(row);
            }
            return card;
        }

        private VisualElement ReportCard(SessionSnapshot s)
        {
            var r = s.LastReport;
            var scenario = _content.FindScenario(r.ScenarioId);
            var card = Card("Cut results: " + (scenario != null ? scenario.Title : r.ScenarioId), DisplayFormat.PlanName(r.PlanUsed.Plan));
            card.name = "report";
            for (int i = 0; i < s.Districts.Length; i++)
            {
                var d = s.Districts[i];
                var row = Row();
                row.Add(Bulb(r.DarkTicks[i] == 0));
                row.Add(Text(d.Name + ": powered " + DisplayFormat.SimDuration(_content, r.ServedTicks[i]) +
                             (r.DarkTicks[i] > 0 ? ", dark " + DisplayFormat.SimDuration(_content, r.DarkTicks[i]) : ""), "body"));
                card.Add(row);
            }
            card.Add(Text("Service income " + DisplayFormat.SignedRand(r.IncomeCents), "headline"));
            card.Add(Text("Diesel used " + DisplayFormat.Litres(r.FuelUsedMl) + " (operating cost " + DisplayFormat.Rand(r.FuelCostCents(_content)) + ")", "body"));
            card.Add(Text("Battery used " + DisplayFormat.Energy(r.BatteryUsedWs) + "   Reliability " + DisplayFormat.Percent(r.ReliabilityBasisPoints), "body"));

            var buttons = Row();
            if (!s.DraftIsPolicy) buttons.Add(CommandButton("save-policy", "Save as standing policy", "btn-secondary", id => GameCommand.SavePolicy(id)));
            buttons.Add(PlainButton("open-shop", "Upgrades", "btn-secondary", () => { _shopOpen = true; ForceRebuild(); }));
            card.Add(buttons);
            var done = CommandButton("report-done", "Continue", "btn-primary", id => GameCommand.AcknowledgeReport(id));
            done.AddToClassList("btn-wide");
            card.Add(done);
            return card;
        }

        private VisualElement ShopCard(SessionSnapshot s)
        {
            var card = Card("Upgrades", "Cash " + DisplayFormat.Rand(s.CashCents) + ". Purchases are saved before they are confirmed.");
            card.name = "shop";
            foreach (var u in s.Shop)
            {
                var item = new VisualElement();
                item.AddToClassList("shop-item");
                item.Add(Text(u.Name + (u.Owned > 0 && u.Repeatable ? "  (x" + u.Owned + ")" : ""), "plan-title"));
                item.Add(Text(u.Description, "muted"));
                string upgradeId = u.Id;
                string label = u.CanBuy ? "Buy " + DisplayFormat.Rand(u.CostCents) : DisplayFormat.Rand(u.CostCents) + "  " + DisplayFormat.Reason(u.BlockReason);
                item.Add(CommandButton("buy-" + upgradeId, label, "btn-primary", id => GameCommand.BuyUpgrade(id, upgradeId), u.CanBuy));
                card.Add(item);
            }
            card.Add(PlainButton("close-shop", "Close", "btn-secondary", () => { _shopOpen = false; ForceRebuild(); }));
            return card;
        }

        private VisualElement ReceiptCard(SessionSnapshot s)
        {
            var r = s.PendingReceipt;
            var card = Card("While you were away", "");
            card.name = "receipt";
            string counted = DisplayFormat.RealDuration(r.AwardedMs);
            card.Add(Text("Away " + DisplayFormat.RealDuration(r.ElapsedMs) + ", counted " + counted + " (cap " + DisplayFormat.RealDuration(_content.Timing.OfflineCapMs) + ")", "body"));
            card.Add(Text("Income " + DisplayFormat.SignedRand(r.IncomeCents), "headline"));
            if (r.CutsHandled > 0)
                card.Add(Text("Your policy handled " + r.CutsHandled + (r.CutsHandled == 1 ? " cut" : " cuts") + ". Feeders were dark for " +
                              DisplayFormat.SimDuration(_content, r.DarkFeederTicks) + " in total.", "body"));
            if (r.FuelUsedMl > 0) card.Add(Text("Diesel used " + DisplayFormat.Litres(r.FuelUsedMl), "body"));
            string stop = DisplayFormat.StopReasonText(r.Stop);
            if (stop.Length > 0) card.Add(Text(stop, "muted"));
            string receiptId = r.Id;
            var ok = CommandButton("receipt-ok", "OK", "btn-primary", id => GameCommand.AcknowledgeReceipt(id, receiptId));
            ok.AddToClassList("btn-wide");
            card.Add(ok);
            return card;
        }

        private VisualElement RecoveryCard(SessionSnapshot s)
        {
            var rec = s.Recovery;
            bool newer = rec != null && rec.ProblemKind == DecodeStatus.FutureSchema;
            var card = Card(newer ? "This save is from a newer version" : "Your save could not be opened",
                newer
                    ? "Update the game to keep playing this town. You can also continue from an older copy or start again."
                    : "The save file is damaged. You can continue from an older copy if one exists, or start a new town.");
            card.name = "recovery";
            if (rec != null && rec.BackupAvailable)
                card.Add(PlainButton("recover-backup", "Continue from the previous copy", "btn-primary", () => _recover?.Invoke(true)));
            card.Add(PlainButton("recover-new", "Start a new town", "btn-secondary", () => _recover?.Invoke(false)));
            card.Add(Text("The original file is kept in the recovery folder and is never deleted.", "muted"));
            return card;
        }

        // ----- Per-frame values -----

        private void UpdateDynamic(SessionSnapshot s)
        {
            if (s.Boot != BootState.Ready)
            {
                _phase.text = s.Boot == BootState.NeedsRecovery ? "RECOVERY" : "LOADING";
                return;
            }
            _cash.text = DisplayFormat.Rand(s.CashCents);
            _fuel.text = DisplayFormat.Litres(s.FuelMl) + " / " + DisplayFormat.Litres(s.TankCapacityMl);
            _battery.text = DisplayFormat.Energy(s.BatteryChargeWs) + " / " + DisplayFormat.Energy(s.BatteryCapacityWs);
            _clock.text = DisplayFormat.DayClock(s.Day, s.MinuteOfDay);
            _phase.text = s.Phase == Phase.Prep ? "DECISION NEEDED" : s.Phase == Phase.Running ? "CUT IN PROGRESS" : "NORMAL SUPPLY";
            _phase.EnableInClassList("phase-chip--alert", s.Phase != Phase.Normal);

            if (_countdown != null)
                _countdown.text = "Starts in " + DisplayFormat.SimDuration(_content, s.TicksUntilCut) + " (town time)";

            if (_runFill != null && s.CutTotalTicks > 0)
            {
                _runFill.style.width = new Length(100f * s.CutTicksElapsed / s.CutTotalTicks, LengthUnit.Percent);
                _runTitle.text = "Block " + (s.CurrentBlock + 1) + " of " + s.Scenario.Blocks.Length + "   " + DisplayFormat.Clock(s.MinuteOfDay);
                _runLive.text = "Demand " + DisplayFormat.Power(s.CurrentDemandW) + "   Grid " + DisplayFormat.Power(Math.Min(s.CurrentDemandW, s.CurrentImportCapW)) +
                                "   Backup " + DisplayFormat.Power(s.CurrentBackupW);
                foreach (var d in s.Districts)
                {
                    if (_runRows.TryGetValue(d.Id, out var label))
                        label.text = d.Name + (d.Powered ? "  ON " : "  DARK ") + DisplayFormat.Power(d.DemandW) + "   served " + DisplayFormat.SimDuration(_content, d.ServedTicks);
                    if (_runBulbs.TryGetValue(d.Id, out var bulb)) SetBulb(bulb, d.Powered);
                }
            }

            foreach (var d in s.Districts)
            {
                if (!_worldLabels.TryGetValue(d.Id, out var w)) continue;
                SetBulb(w.Bulb, d.Powered);
                w.Status.text = (d.Powered ? "POWERED " : "DARK ") + DisplayFormat.Power(d.DemandW);
                w.Detail.text = "#" + (d.PriorityRank + 1) + (d.Protected ? " PROTECTED" : "") + "  " + d.FeederLabel;
                w.Root.EnableInClassList("world-label--dark", !d.Powered);
            }
        }

        private void EnsureWorldLabels(SessionSnapshot s)
        {
            if (_city == null || _worldLabels.Count > 0 || s.Districts.Length == 0) return;
            foreach (var anchor in _city.Anchors)
            {
                DistrictView district = null;
                foreach (var d in s.Districts) if (d.Id == anchor.DistrictId) district = d;
                if (district == null) continue;
                var root = new VisualElement { name = "label-" + district.Id, pickingMode = PickingMode.Ignore };
                root.AddToClassList("world-label");
                var title = Text(district.Name, "world-title");
                title.pickingMode = PickingMode.Ignore;
                var row = Row();
                row.pickingMode = PickingMode.Ignore;
                var bulb = Bulb(district.Powered);
                var status = Text("", "world-status");
                status.pickingMode = PickingMode.Ignore;
                row.Add(bulb);
                row.Add(status);
                var detail = Text("", "world-detail");
                detail.pickingMode = PickingMode.Ignore;
                root.Add(title);
                root.Add(row);
                root.Add(detail);
                _labelsLayer.Add(root);
                _worldLabels[district.Id] = new WorldLabel { Root = root, Status = status, Detail = detail, Bulb = bulb, Anchor = anchor.WorldPosition };
            }
        }

        private void PlaceWorldLabels()
        {
            if (_city == null || _city.Camera == null || _root.panel == null) return;
            foreach (var w in _worldLabels.Values)
            {
                Vector2 p = RuntimePanelUtils.CameraTransformWorldToPanel(_root.panel, w.Anchor, _city.Camera);
                w.Root.style.left = p.x - 110f;
                w.Root.style.top = p.y - 70f;
            }
        }

        private void ApplySafeArea(bool force)
        {
            var area = Screen.safeArea;
            if (!force && area == _appliedSafeArea) return;
            float width = _root.layout.width;
            if (float.IsNaN(width) || width <= 0f || Screen.width <= 0) return;
            _appliedSafeArea = area;
            float scale = width / Screen.width;
            _safe.style.paddingLeft = area.xMin * scale;
            _safe.style.paddingRight = (Screen.width - area.xMax) * scale;
            _safe.style.paddingTop = (Screen.height - area.yMax) * scale;
            _safe.style.paddingBottom = area.yMin * scale;
        }

        // ----- Builders -----

        private void ForceRebuild()
        {
            _layoutKey = "";
            if (_snapshot != null) Render(_snapshot);
        }

        private void ShowModal(VisualElement card)
        {
            _modalLayer.Clear();
            card.AddToClassList("modal");
            _modalLayer.Add(card);
            _modalLayer.style.display = DisplayStyle.Flex;
        }

        private Button CommandButton(string name, string text, string cls, Func<string, GameCommand> make, bool enabled = true)
        {
            // One command ID per button lifetime: repeated taps on the same button are duplicates.
            string commandId = Guid.NewGuid().ToString("N");
            Button button = null;
            Action action = () =>
            {
                if (_submit == null) return;
                button.SetEnabled(false);
                var result = _submit(make(commandId));
                if (result.Status == CommandStatus.Rejected)
                {
                    ShowToast(DisplayFormat.Reason(result.Reason));
                    button.SetEnabled(true);
                }
            };
            button = MakeButton(name, text, cls, action, enabled);
            return button;
        }

        private Button PlainButton(string name, string text, string cls, Action onClick) => MakeButton(name, text, cls, onClick, true);

        private Button MakeButton(string name, string text, string cls, Action onClick, bool enabled)
        {
            var button = new Button { name = name, text = text };
            button.AddToClassList("btn");
            if (!string.IsNullOrEmpty(cls)) button.AddToClassList(cls);
            button.SetEnabled(enabled);
            button.clicked += () => { if (button.enabledInHierarchy) onClick(); };
            _actions[name] = onClick;
            return button;
        }

        private static VisualElement Card(string title, string body)
        {
            var card = new VisualElement();
            card.AddToClassList("card");
            if (!string.IsNullOrEmpty(title)) card.Add(Text(title, "title"));
            if (!string.IsNullOrEmpty(body)) card.Add(Text(body, "body"));
            return card;
        }

        private static Label Text(string text, string cls)
        {
            var label = new Label(text);
            if (!string.IsNullOrEmpty(cls)) label.AddToClassList(cls);
            return label;
        }

        private static Label Tag(string text)
        {
            var tag = new Label(text);
            tag.AddToClassList("tag");
            return tag;
        }

        private static VisualElement Row()
        {
            var row = new VisualElement();
            row.AddToClassList("row");
            return row;
        }

        /// <summary>A light-bulb icon drawn with styles: filled when powered, hollow when dark.</summary>
        private static VisualElement Bulb(bool on)
        {
            var bulb = new VisualElement { pickingMode = PickingMode.Ignore };
            bulb.AddToClassList("bulb");
            SetBulb(bulb, on);
            return bulb;
        }

        private static void SetBulb(VisualElement bulb, bool on)
        {
            bulb.EnableInClassList("bulb--on", on);
            bulb.EnableInClassList("bulb--off", !on);
        }

        private string CutWindow(SessionSnapshot s)
        {
            GridRules.ClockAt(_content, s.Tick, out _, out var start);
            GridRules.ClockAt(_content, s.Tick + s.Scenario.TotalTicks, out _, out var end);
            return DisplayFormat.Clock(start) + "-" + DisplayFormat.Clock(end);
        }

        private string Outcome(ForecastResult f)
        {
            if (f.FullyServed) return "Every feeder stays on.";
            var dark = new List<string>();
            for (int i = 0; i < _content.DistrictCount; i++)
                if (f.Totals.DarkTicks[i] > 0) dark.Add(_content.Districts[i].Name + " " + DisplayFormat.SimDuration(_content, f.Totals.DarkTicks[i]));
            return "Dark: " + string.Join(", ", dark) + ". Income " + DisplayFormat.Rand(f.Totals.IncomeCents) + ".";
        }

        private string ResourceUse(ForecastResult f)
        {
            switch (f.Plan)
            {
                case PlanKind.Battery:
                    return " Uses " + DisplayFormat.Energy(f.Totals.BatteryUsedWs) + " of " + DisplayFormat.Energy(f.BatteryStartWs) + ".";
                case PlanKind.Diesel:
                    return " Burns " + DisplayFormat.Litres(f.Totals.FuelUsedMl) + " of " + DisplayFormat.Litres(f.FuelStartMl) +
                           " (" + DisplayFormat.Rand(f.Totals.FuelCostCents(_content)) + ").";
                default:
                    return " No fuel or storage used.";
            }
        }
    }
}
