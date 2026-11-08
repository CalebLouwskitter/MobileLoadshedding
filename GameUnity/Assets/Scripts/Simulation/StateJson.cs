using System;
using System.Collections.Generic;

namespace IdleLoadshedding.Simulation
{
    public sealed class StateFormatException : Exception
    {
        public StateFormatException(string message) : base(message) { }
    }

    /// <summary>
    /// Canonical JSON form of <see cref="SimState"/>. Saves store this payload, and the state hash
    /// is computed over it. Per-district arrays are written as objects keyed by stable ID so a
    /// reordered content file cannot shift values between districts.
    /// </summary>
    public static class StateJson
    {
        public static JsonValue Write(GameContent content, SimState s, bool includeRealTime = true)
        {
            var o = JsonValue.NewObject();
            o.Set("townId", s.TownId);
            o.Set("tick", s.Tick);
            o.Set("phase", PhaseId(s.Phase));
            o.Set("campaignIndex", s.CampaignIndex);
            o.Set("cutScenarioId", s.CutScenarioId);
            o.Set("cutStartTick", s.CutStartTick);
            o.Set("cutManual", s.CutManual);
            o.Set("fastForward", s.FastForward);
            o.Set("cashCents", s.CashCents);
            o.Set("batteryChargeWs", s.BatteryChargeWs);
            o.Set("batteryCapacityWs", s.BatteryCapacityWs);
            o.Set("fuelMl", s.FuelMl);
            o.Set("fuelCarry", s.FuelCarry);
            o.Set("fuelMlPerKWh", s.FuelMlPerKWh);
            o.Set("tankCapacityMl", s.TankCapacityMl);
            o.Set("draft", WritePlan(s.Draft));
            o.Set("policy", s.Policy == null ? JsonValue.Null : WritePlan(s.Policy));

            var upgrades = JsonValue.NewArray();
            foreach (var u in s.Upgrades)
                upgrades.Add(JsonValue.NewObject().Set("id", u.Id).Set("count", u.Count));
            o.Set("upgrades", upgrades);

            o.Set("powered", WriteBools(content, s.Powered));
            o.Set("cut", s.Cut == null ? JsonValue.Null : WriteCut(content, s.Cut));
            o.Set("lastReport", s.LastReport == null ? JsonValue.Null : WriteCut(content, s.LastReport));
            o.Set("reportAcknowledged", s.ReportAcknowledged);
            o.Set("lifetimeIncomeCents", s.LifetimeIncomeCents);
            o.Set("cutsCompleted", s.CutsCompleted);

            if (includeRealTime)
            {
                o.Set("watermarkUtcMs", s.WatermarkUtcMs);
                o.Set("pendingReceipt", s.PendingReceipt == null ? JsonValue.Null : WriteReceipt(s.PendingReceipt));
                o.Set("recentCommandIds", JsonValue.FromStrings(s.RecentCommandIds));
            }
            return o;
        }

        public static SimState Read(GameContent content, JsonValue o)
        {
            SimState s;
            try
            {
                s = ReadInner(content, o);
            }
            catch (JsonException e)
            {
                throw new StateFormatException(e.Message);
            }
            Validate(content, s);
            return s;
        }

        private static SimState ReadInner(GameContent content, JsonValue o)
        {
            var s = new SimState
            {
                TownId = o.GetString("townId"),
                Tick = o.GetLong("tick"),
                Phase = ParsePhase(o.GetString("phase")),
                CampaignIndex = o.GetInt("campaignIndex"),
                CutScenarioId = o.GetString("cutScenarioId"),
                CutStartTick = o.GetLong("cutStartTick"),
                CutManual = o.GetBool("cutManual"),
                FastForward = o.GetBool("fastForward"),
                CashCents = o.GetLong("cashCents"),
                BatteryChargeWs = o.GetLong("batteryChargeWs"),
                BatteryCapacityWs = o.GetLong("batteryCapacityWs"),
                FuelMl = o.GetLong("fuelMl"),
                FuelCarry = o.GetLong("fuelCarry"),
                FuelMlPerKWh = o.GetLong("fuelMlPerKWh"),
                TankCapacityMl = o.GetLong("tankCapacityMl"),
                Draft = ReadPlan(o.GetObject("draft")),
                ReportAcknowledged = o.GetBool("reportAcknowledged"),
                LifetimeIncomeCents = o.GetLong("lifetimeIncomeCents"),
                CutsCompleted = o.GetInt("cutsCompleted"),
                WatermarkUtcMs = o.GetLong("watermarkUtcMs")
            };
            var policy = o.GetOptional("policy");
            s.Policy = policy == null ? null : ReadPlan(policy);

            foreach (var u in o.GetArray("upgrades").Items)
                s.Upgrades.Add(new OwnedUpgrade { Id = u.GetString("id"), Count = u.GetInt("count") });

            s.Powered = ReadBools(content, o.GetObject("powered"));
            var cut = o.GetOptional("cut");
            s.Cut = cut == null ? null : ReadCut(content, cut);
            var report = o.GetOptional("lastReport");
            s.LastReport = report == null ? null : ReadCut(content, report);
            var receipt = o.GetOptional("pendingReceipt");
            s.PendingReceipt = receipt == null ? null : ReadReceipt(receipt);
            s.RecentCommandIds = new List<string>(o.GetStringArray("recentCommandIds"));
            return s;
        }

        /// <summary>Rejects a state the rules could not continue from safely.</summary>
        public static void Validate(GameContent content, SimState s)
        {
            var errors = new List<string>();
            void Check(bool ok, string message) { if (!ok) errors.Add(message); }

            Check(s.TownId == content.TownId, "save belongs to town '" + s.TownId + "'");
            Check(s.Tick >= 0, "tick must be >= 0");
            Check(s.CampaignIndex >= 0 && s.CampaignIndex <= content.Campaign.Length, "campaignIndex out of range");
            var scenario = content.FindScenario(s.CutScenarioId);
            Check(scenario != null, "unknown scenario '" + s.CutScenarioId + "'");
            Check(s.CutStartTick >= 0, "cutStartTick must be >= 0");
            Check(s.CashCents >= 0, "cash must be >= 0");
            Check(s.BatteryCapacityWs > 0, "battery capacity must be > 0");
            Check(s.BatteryChargeWs >= 0 && s.BatteryChargeWs <= s.BatteryCapacityWs, "battery charge out of bounds");
            Check(s.TankCapacityMl > 0, "tank capacity must be > 0");
            Check(s.FuelMl >= 0 && s.FuelMl <= s.TankCapacityMl, "fuel out of bounds");
            Check(s.FuelCarry >= 0 && s.FuelCarry < Dispatcher.WsPerKWh, "fuel carry out of range");
            Check(s.FuelMlPerKWh > 0, "fuel rate must be > 0");
            Check(s.LifetimeIncomeCents >= 0, "lifetime income must be >= 0");
            Check(s.CutsCompleted >= 0, "cutsCompleted must be >= 0");
            Check(s.WatermarkUtcMs >= 0, "watermark must be >= 0");
            Check(s.RecentCommandIds.Count <= SimState.RecentCommandCapacity, "too many remembered commands");

            var ids = content.DistrictIdSet();
            CheckPlan(s.Draft, ids, "draft", errors);
            if (s.Policy != null) CheckPlan(s.Policy, ids, "policy", errors);

            string previous = null;
            foreach (var u in s.Upgrades)
            {
                var def = content.FindUpgrade(u.Id);
                Check(def != null, "unknown upgrade '" + u.Id + "'");
                Check(u.Count > 0 && (def == null || def.Repeatable || u.Count == 1), "bad count for upgrade '" + u.Id + "'");
                Check(previous == null || string.CompareOrdinal(previous, u.Id) < 0, "upgrades must be sorted and unique");
                previous = u.Id;
            }

            Check(s.Powered.Length == content.DistrictCount, "powered must cover every district");

            if (s.Phase == Phase.Running)
            {
                Check(s.Cut != null, "a running cut needs its stats");
                if (s.Cut != null && scenario != null)
                {
                    CheckCut(s.Cut, content, errors);
                    Check(s.Cut.ScenarioId == scenario.Id, "cut scenario mismatch");
                    Check(s.Cut.TotalTicks == scenario.TotalTicks, "cut length mismatch");
                    Check(s.Cut.TicksElapsed < s.Cut.TotalTicks, "running cut already finished");
                    Check(s.Cut.Manual == s.CutManual, "cut manual flag mismatch");
                }
            }
            else
            {
                Check(s.Cut == null, "only a running cut has live stats");
                Check(!s.CutManual, "manual flag set outside a cut");
            }
            if (s.LastReport != null) CheckCut(s.LastReport, content, errors);

            if (s.PendingReceipt != null)
            {
                var r = s.PendingReceipt;
                Check(!string.IsNullOrEmpty(r.Id), "receipt needs an id");
                Check(r.ElapsedMs >= 0 && r.AwardedMs >= 0 && r.ForfeitedMs >= 0 && r.Ticks >= 0, "receipt values must be >= 0");
            }

            if (errors.Count > 0) throw new StateFormatException(string.Join("; ", errors));
        }

        private static void CheckPlan(DispatchPlan plan, HashSet<string> ids, string name, List<string> errors)
        {
            if (plan == null) { errors.Add(name + " plan is missing"); return; }
            if (!ContentLoader.IsPermutation(plan.Priority, ids)) errors.Add(name + " priority must list every district once");
            string previous = null;
            foreach (var p in plan.Protected)
            {
                if (!ids.Contains(p)) errors.Add(name + " protects unknown district '" + p + "'");
                if (previous != null && string.CompareOrdinal(previous, p) >= 0) errors.Add(name + " protected list must be sorted and unique");
                previous = p;
            }
        }

        private static void CheckCut(CutStats cut, GameContent content, List<string> errors)
        {
            if (content.FindScenario(cut.ScenarioId) == null) errors.Add("cut references unknown scenario");
            if (cut.TicksElapsed < 0 || cut.TicksElapsed > cut.TotalTicks) errors.Add("cut progress out of range");
            if (cut.ServedTicks.Length != content.DistrictCount || cut.DarkTicks.Length != content.DistrictCount)
                errors.Add("cut must cover every district");
            if (cut.IncomeCents < 0 || cut.FuelUsedMl < 0 || cut.BatteryUsedWs < 0 || cut.ServedWs < 0 || cut.DemandWs < 0)
                errors.Add("cut totals must be >= 0");
            if (cut.ServedWs > cut.DemandWs) errors.Add("cut served more than demand");
            if (cut.PlanUsed == null) errors.Add("cut plan missing");
            else CheckPlan(cut.PlanUsed, content.DistrictIdSet(), "cut", errors);
        }

        public static string PhaseId(Phase phase)
        {
            switch (phase)
            {
                case Phase.Prep: return "prep";
                case Phase.Running: return "running";
                case Phase.Normal: return "normal";
                default: throw new ArgumentOutOfRangeException(nameof(phase));
            }
        }

        private static Phase ParsePhase(string id)
        {
            switch (id)
            {
                case "prep": return Phase.Prep;
                case "running": return Phase.Running;
                case "normal": return Phase.Normal;
                default: throw new JsonException("Unknown phase '" + id + "'");
            }
        }

        private static JsonValue WritePlan(DispatchPlan p) =>
            JsonValue.NewObject()
                .Set("plan", PlanKinds.ToId(p.Plan))
                .Set("priority", JsonValue.FromStrings(p.Priority))
                .Set("protected", JsonValue.FromStrings(p.Protected));

        private static DispatchPlan ReadPlan(JsonValue o)
        {
            if (!PlanKinds.TryParse(o.GetString("plan"), out var plan)) throw new JsonException("Unknown plan");
            return new DispatchPlan
            {
                Plan = plan,
                Priority = o.GetStringArray("priority"),
                Protected = o.GetStringArray("protected")
            };
        }

        private static JsonValue WriteBools(GameContent content, bool[] values)
        {
            var o = JsonValue.NewObject();
            for (int i = 0; i < content.DistrictCount; i++) o.Set(content.Districts[i].Id, values[i]);
            return o;
        }

        private static bool[] ReadBools(GameContent content, JsonValue o)
        {
            if (o.Members.Count != content.DistrictCount) throw new JsonException("District map has the wrong size");
            var values = new bool[content.DistrictCount];
            for (int i = 0; i < content.DistrictCount; i++) values[i] = o.GetBool(content.Districts[i].Id);
            return values;
        }

        private static JsonValue WriteInts(GameContent content, int[] values)
        {
            var o = JsonValue.NewObject();
            for (int i = 0; i < content.DistrictCount; i++) o.Set(content.Districts[i].Id, values[i]);
            return o;
        }

        private static int[] ReadInts(GameContent content, JsonValue o)
        {
            if (o.Members.Count != content.DistrictCount) throw new JsonException("District map has the wrong size");
            var values = new int[content.DistrictCount];
            for (int i = 0; i < content.DistrictCount; i++)
            {
                values[i] = o.GetInt(content.Districts[i].Id);
                if (values[i] < 0) throw new JsonException("Negative tick count");
            }
            return values;
        }

        private static JsonValue WriteCut(GameContent content, CutStats c) =>
            JsonValue.NewObject()
                .Set("scenarioId", c.ScenarioId)
                .Set("plan", WritePlan(c.PlanUsed))
                .Set("manual", c.Manual)
                .Set("startTick", c.StartTick)
                .Set("totalTicks", c.TotalTicks)
                .Set("ticksElapsed", c.TicksElapsed)
                .Set("servedTicks", WriteInts(content, c.ServedTicks))
                .Set("darkTicks", WriteInts(content, c.DarkTicks))
                .Set("incomeCents", c.IncomeCents)
                .Set("fuelUsedMl", c.FuelUsedMl)
                .Set("batteryUsedWs", c.BatteryUsedWs)
                .Set("servedWs", c.ServedWs)
                .Set("demandWs", c.DemandWs)
                .Set("backupWs", c.BackupWs);

        private static CutStats ReadCut(GameContent content, JsonValue o) =>
            new CutStats
            {
                ScenarioId = o.GetString("scenarioId"),
                PlanUsed = ReadPlan(o.GetObject("plan")),
                Manual = o.GetBool("manual"),
                StartTick = o.GetLong("startTick"),
                TotalTicks = o.GetInt("totalTicks"),
                TicksElapsed = o.GetInt("ticksElapsed"),
                ServedTicks = ReadInts(content, o.GetObject("servedTicks")),
                DarkTicks = ReadInts(content, o.GetObject("darkTicks")),
                IncomeCents = o.GetLong("incomeCents"),
                FuelUsedMl = o.GetLong("fuelUsedMl"),
                BatteryUsedWs = o.GetLong("batteryUsedWs"),
                ServedWs = o.GetLong("servedWs"),
                DemandWs = o.GetLong("demandWs"),
                BackupWs = o.GetLong("backupWs")
            };

        private static JsonValue WriteReceipt(Receipt r) =>
            JsonValue.NewObject()
                .Set("id", r.Id)
                .Set("fromUtcMs", r.FromUtcMs)
                .Set("toUtcMs", r.ToUtcMs)
                .Set("elapsedMs", r.ElapsedMs)
                .Set("awardedMs", r.AwardedMs)
                .Set("forfeitedMs", r.ForfeitedMs)
                .Set("ticks", r.Ticks)
                .Set("incomeCents", r.IncomeCents)
                .Set("fuelUsedMl", r.FuelUsedMl)
                .Set("batteryUsedWs", r.BatteryUsedWs)
                .Set("cutsHandled", r.CutsHandled)
                .Set("darkFeederTicks", r.DarkFeederTicks)
                .Set("stop", StopId(r.Stop));

        private static Receipt ReadReceipt(JsonValue o) =>
            new Receipt
            {
                Id = o.GetString("id"),
                FromUtcMs = o.GetLong("fromUtcMs"),
                ToUtcMs = o.GetLong("toUtcMs"),
                ElapsedMs = o.GetLong("elapsedMs"),
                AwardedMs = o.GetLong("awardedMs"),
                ForfeitedMs = o.GetLong("forfeitedMs"),
                Ticks = o.GetLong("ticks"),
                IncomeCents = o.GetLong("incomeCents"),
                FuelUsedMl = o.GetLong("fuelUsedMl"),
                BatteryUsedWs = o.GetLong("batteryUsedWs"),
                CutsHandled = o.GetInt("cutsHandled"),
                DarkFeederTicks = o.GetLong("darkFeederTicks"),
                Stop = ParseStop(o.GetString("stop"))
            };

        private static string StopId(StopReason stop)
        {
            switch (stop)
            {
                case StopReason.None: return "none";
                case StopReason.CapReached: return "cap";
                case StopReason.DecisionNeeded: return "decision";
                case StopReason.CutPaused: return "paused";
                default: throw new ArgumentOutOfRangeException(nameof(stop));
            }
        }

        private static StopReason ParseStop(string id)
        {
            switch (id)
            {
                case "none": return StopReason.None;
                case "cap": return StopReason.CapReached;
                case "decision": return StopReason.DecisionNeeded;
                case "paused": return StopReason.CutPaused;
                default: throw new JsonException("Unknown stop reason '" + id + "'");
            }
        }
    }

    /// <summary>FNV-1a 64-bit hashing for checksums and replay comparisons.</summary>
    public static class StateHash
    {
        public static ulong Fnv1a64(string text)
        {
            const ulong offset = 14695981039346656037UL;
            const ulong prime = 1099511628211UL;
            ulong hash = offset;
            var bytes = System.Text.Encoding.UTF8.GetBytes(text);
            foreach (var b in bytes)
            {
                hash ^= b;
                hash *= prime;
            }
            return hash;
        }

        public static string Hex(ulong value) => value.ToString("x16", System.Globalization.CultureInfo.InvariantCulture);

        /// <summary>Hash of the simulation outcome, excluding real-time bookkeeping such as the watermark.</summary>
        public static string Simulation(GameContent content, SimState state) =>
            Hex(Fnv1a64(Json.Write(StateJson.Write(content, state, includeRealTime: false))));

        public static string Full(GameContent content, SimState state) =>
            Hex(Fnv1a64(Json.Write(StateJson.Write(content, state, includeRealTime: true))));
    }
}
