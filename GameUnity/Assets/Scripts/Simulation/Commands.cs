using System;
using System.Collections.Generic;

namespace IdleLoadshedding.Simulation
{
    public enum CommandType
    {
        SelectPlan,
        MovePriority,
        SetProtected,
        StartCut,
        SkipToNextCut,
        BuyUpgrade,
        SavePolicy,
        AcknowledgeReport,
        AcknowledgeReceipt
    }

    /// <summary>A player intent. The ID makes a retried or double-tapped command apply at most once.</summary>
    public sealed class GameCommand
    {
        public string Id;
        public CommandType Type;
        public string Arg;
        public int Value;

        public static GameCommand SelectPlan(string id, PlanKind plan) =>
            new GameCommand { Id = id, Type = CommandType.SelectPlan, Arg = PlanKinds.ToId(plan) };

        /// <summary>direction -1 moves the feeder up the priority list, +1 moves it down.</summary>
        public static GameCommand MovePriority(string id, string districtId, int direction) =>
            new GameCommand { Id = id, Type = CommandType.MovePriority, Arg = districtId, Value = direction };

        public static GameCommand SetProtected(string id, string districtId, bool isProtected) =>
            new GameCommand { Id = id, Type = CommandType.SetProtected, Arg = districtId, Value = isProtected ? 1 : 0 };

        public static GameCommand StartCut(string id) => new GameCommand { Id = id, Type = CommandType.StartCut };
        public static GameCommand SkipToNextCut(string id) => new GameCommand { Id = id, Type = CommandType.SkipToNextCut };

        public static GameCommand BuyUpgrade(string id, string upgradeId) =>
            new GameCommand { Id = id, Type = CommandType.BuyUpgrade, Arg = upgradeId };

        public static GameCommand SavePolicy(string id) => new GameCommand { Id = id, Type = CommandType.SavePolicy };
        public static GameCommand AcknowledgeReport(string id) => new GameCommand { Id = id, Type = CommandType.AcknowledgeReport };

        public static GameCommand AcknowledgeReceipt(string id, string receiptId) =>
            new GameCommand { Id = id, Type = CommandType.AcknowledgeReceipt, Arg = receiptId };

        public override string ToString() => Type + "(" + Arg + "," + Value + ")#" + Id;
    }

    public enum CommandStatus
    {
        Accepted,
        Duplicate,
        Rejected
    }

    public sealed class CommandResult
    {
        public CommandStatus Status;
        /// <summary>Machine-readable reason for a rejection, e.g. "insufficient_funds".</summary>
        public string Reason = "";

        public bool Accepted => Status == CommandStatus.Accepted;

        public static CommandResult Ok() => new CommandResult { Status = CommandStatus.Accepted };
        public static CommandResult Dup() => new CommandResult { Status = CommandStatus.Duplicate, Reason = "duplicate" };
        public static CommandResult No(string reason) => new CommandResult { Status = CommandStatus.Rejected, Reason = reason };

        public override string ToString() => Status + (Reason.Length > 0 ? ":" + Reason : "");
    }

    public static class CommandReasons
    {
        public const string MissingId = "missing_id";
        public const string WrongPhase = "wrong_phase";
        public const string UnknownPlan = "unknown_plan";
        public const string UnknownDistrict = "unknown_district";
        public const string AtEdge = "at_edge";
        public const string UnknownUpgrade = "unknown_upgrade";
        public const string AlreadyOwned = "already_owned";
        public const string InsufficientFunds = "insufficient_funds";
        public const string TankFull = "tank_full";
        public const string CannotSkip = "cannot_skip";
        public const string StaleReceipt = "stale_receipt";
        public const string NothingToAcknowledge = "nothing_to_acknowledge";
    }

    /// <summary>
    /// Validates and applies commands. A rejected command leaves the state untouched; an accepted
    /// command records its ID so a repeat is reported as a duplicate and spends nothing.
    /// </summary>
    public static class CommandRules
    {
        public static CommandResult Apply(GameContent content, SimState state, GameCommand cmd)
        {
            if (cmd == null || string.IsNullOrEmpty(cmd.Id)) return CommandResult.No(CommandReasons.MissingId);
            if (state.HasSeenCommand(cmd.Id)) return CommandResult.Dup();

            var result = ApplyInner(content, state, cmd);
            if (result.Accepted) state.RememberCommand(cmd.Id);
            return result;
        }

        /// <summary>Checks a command without changing anything.</summary>
        public static CommandResult Check(GameContent content, SimState state, GameCommand cmd) =>
            Apply(content, state.Clone(), cmd);

        private static CommandResult ApplyInner(GameContent content, SimState state, GameCommand cmd)
        {
            switch (cmd.Type)
            {
                case CommandType.SelectPlan:
                {
                    if (state.Phase == Phase.Running) return CommandResult.No(CommandReasons.WrongPhase);
                    if (!PlanKinds.TryParse(cmd.Arg, out var plan)) return CommandResult.No(CommandReasons.UnknownPlan);
                    state.Draft.Plan = plan;
                    return CommandResult.Ok();
                }

                case CommandType.MovePriority:
                {
                    if (state.Phase == Phase.Running) return CommandResult.No(CommandReasons.WrongPhase);
                    int index = Array.IndexOf(state.Draft.Priority, cmd.Arg);
                    if (index < 0) return CommandResult.No(CommandReasons.UnknownDistrict);
                    int direction = Math.Sign(cmd.Value);
                    int target = index + direction;
                    if (direction == 0 || target < 0 || target >= state.Draft.Priority.Length)
                        return CommandResult.No(CommandReasons.AtEdge);
                    var priority = state.Draft.Priority;
                    var swap = priority[target];
                    priority[target] = priority[index];
                    priority[index] = swap;
                    return CommandResult.Ok();
                }

                case CommandType.SetProtected:
                {
                    if (state.Phase == Phase.Running) return CommandResult.No(CommandReasons.WrongPhase);
                    if (content.DistrictIndex(cmd.Arg) < 0) return CommandResult.No(CommandReasons.UnknownDistrict);
                    var set = new List<string>(state.Draft.Protected);
                    set.Remove(cmd.Arg);
                    if (cmd.Value != 0) set.Add(cmd.Arg);
                    set.Sort(StringComparer.Ordinal);
                    state.Draft.Protected = set.ToArray();
                    return CommandResult.Ok();
                }

                case CommandType.StartCut:
                {
                    if (state.Phase != Phase.Prep) return CommandResult.No(CommandReasons.WrongPhase);
                    state.ReportAcknowledged = true;
                    GridRules.BeginCut(content, state, state.Draft, manual: true);
                    return CommandResult.Ok();
                }

                case CommandType.SkipToNextCut:
                {
                    if (state.Phase != Phase.Normal) return CommandResult.No(CommandReasons.WrongPhase);
                    var next = GridRules.CurrentScenario(content, state);
                    if (!next.AllowSkipLead || GridRules.IsRoutine(content, state))
                        return CommandResult.No(CommandReasons.CannotSkip);
                    state.FastForward = true;
                    return CommandResult.Ok();
                }

                case CommandType.BuyUpgrade:
                    return BuyUpgrade(content, state, cmd.Arg);

                case CommandType.SavePolicy:
                {
                    if (state.Phase == Phase.Running) return CommandResult.No(CommandReasons.WrongPhase);
                    state.Policy = state.Draft.Clone();
                    return CommandResult.Ok();
                }

                case CommandType.AcknowledgeReport:
                {
                    if (state.ReportAcknowledged) return CommandResult.No(CommandReasons.NothingToAcknowledge);
                    state.ReportAcknowledged = true;
                    return CommandResult.Ok();
                }

                case CommandType.AcknowledgeReceipt:
                {
                    if (state.PendingReceipt == null) return CommandResult.No(CommandReasons.NothingToAcknowledge);
                    if (state.PendingReceipt.Id != cmd.Arg) return CommandResult.No(CommandReasons.StaleReceipt);
                    state.PendingReceipt = null;
                    return CommandResult.Ok();
                }

                default:
                    throw new ArgumentOutOfRangeException(nameof(cmd));
            }
        }

        /// <summary>Why an upgrade cannot be bought right now, or an empty string if it can.</summary>
        public static string UpgradeBlockReason(GameContent content, SimState state, UpgradeDef upgrade)
        {
            if (upgrade == null) return CommandReasons.UnknownUpgrade;
            if (state.Phase == Phase.Running) return CommandReasons.WrongPhase;
            if (!upgrade.Repeatable && state.UpgradeCount(upgrade.Id) > 0) return CommandReasons.AlreadyOwned;
            foreach (var e in upgrade.Effects)
            {
                if (e.Type == UpgradeEffectTypes.FuelMl && state.FuelMl + e.Value > state.TankCapacityMl)
                    return CommandReasons.TankFull;
            }
            if (state.CashCents < upgrade.CostCents) return CommandReasons.InsufficientFunds;
            return "";
        }

        private static CommandResult BuyUpgrade(GameContent content, SimState state, string upgradeId)
        {
            var upgrade = content.FindUpgrade(upgradeId);
            var reason = UpgradeBlockReason(content, state, upgrade);
            if (reason.Length > 0) return CommandResult.No(reason);

            state.CashCents -= upgrade.CostCents;
            // Capacity changes first so a "arrives charged" effect is clamped to the new capacity.
            foreach (var e in upgrade.Effects)
            {
                switch (e.Type)
                {
                    case UpgradeEffectTypes.BatteryCapacityWs:
                        state.BatteryCapacityWs += e.Value;
                        break;
                    case UpgradeEffectTypes.GeneratorFuelMlPerKWh:
                        state.FuelMlPerKWh = e.Value;
                        break;
                    case UpgradeEffectTypes.FuelMl:
                        state.FuelMl += e.Value;
                        break;
                }
            }
            foreach (var e in upgrade.Effects)
            {
                if (e.Type == UpgradeEffectTypes.BatteryChargeWs)
                    state.BatteryChargeWs = Math.Min(state.BatteryCapacityWs, state.BatteryChargeWs + e.Value);
            }
            state.AddUpgrade(upgrade.Id);
            return CommandResult.Ok();
        }
    }
}
