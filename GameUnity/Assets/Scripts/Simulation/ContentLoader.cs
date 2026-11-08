using System;
using System.Collections.Generic;

namespace IdleLoadshedding.Simulation
{
    public sealed class ContentException : Exception
    {
        public ContentException(string message) : base(message) { }
    }

    /// <summary>Parses town-content.json and rejects anything the rules cannot run safely.</summary>
    public static class ContentLoader
    {
        public const int SupportedContentVersion = 1;

        public static GameContent Parse(string json)
        {
            JsonValue root;
            try
            {
                root = Json.Parse(json);
            }
            catch (JsonException e)
            {
                throw new ContentException("Content is not valid JSON: " + e.Message);
            }

            try
            {
                var content = Read(root);
                Validate(content);
                return content;
            }
            catch (JsonException e)
            {
                throw new ContentException("Content has the wrong shape: " + e.Message);
            }
        }

        private static GameContent Read(JsonValue root)
        {
            var c = new GameContent
            {
                ContentVersion = root.GetInt("contentVersion"),
                StartingCashCents = root.GetLong("startingCashCents")
            };
            var town = root.GetObject("town");
            c.TownId = town.GetString("id");
            c.TownName = town.GetString("name");
            c.Region = town.GetStringOr("region", "");

            var t = root.GetObject("timing");
            c.Timing = new TimingDef
            {
                SimSecondsPerTick = t.GetInt("simSecondsPerTick"),
                StartMinuteOfDay = t.GetInt("startMinuteOfDay"),
                RunTicksPerSecond = t.GetInt("runTicksPerSecond"),
                SkipTicksPerSecond = t.GetInt("skipTicksPerSecond"),
                IdleRealMsPerTick = t.GetLong("idleRealMsPerTick"),
                OfflineCapMs = t.GetLong("offlineCapMs"),
                CheckpointIntervalMs = t.GetLong("checkpointIntervalMs"),
                MaxTicksPerFrame = t.GetInt("maxTicksPerFrame"),
                MaxFrameDeltaMs = t.GetLong("maxFrameDeltaMs")
            };

            var districts = new List<DistrictDef>();
            foreach (var d in root.GetArray("districts").Items)
            {
                districts.Add(new DistrictDef
                {
                    Id = d.GetString("id"),
                    Name = d.GetString("name"),
                    FeederLabel = d.GetString("feederLabel"),
                    Kind = d.GetString("kind"),
                    DemandW = d.GetLong("demandW"),
                    RevenuePerTickCents = d.GetLong("revenuePerTickCents"),
                    ProtectedByDefault = d.GetBool("protectedByDefault"),
                    Consequence = d.GetStringOr("consequence", "")
                });
            }
            c.Districts = districts.ToArray();
            c.DefaultPriority = root.GetStringArray("defaultPriority");

            var b = root.GetObject("battery");
            c.Battery = new BatteryDef
            {
                Id = b.GetString("id"),
                Name = b.GetString("name"),
                MaxDischargeW = b.GetLong("maxDischargeW"),
                CapacityWs = b.GetLong("capacityWs"),
                StartChargeWs = b.GetLong("startChargeWs"),
                RechargeW = b.GetLong("rechargeW")
            };

            var g = root.GetObject("generator");
            c.Generator = new GeneratorDef
            {
                Id = g.GetString("id"),
                Name = g.GetString("name"),
                MaxOutputW = g.GetLong("maxOutputW"),
                FuelMlPerKWh = g.GetLong("fuelMlPerKWh"),
                TankCapacityMl = g.GetLong("tankCapacityMl"),
                StartFuelMl = g.GetLong("startFuelMl"),
                FuelPriceCentsPerLitre = g.GetLong("fuelPriceCentsPerLitre")
            };

            var scenarios = new List<ScenarioDef>();
            foreach (var s in root.GetArray("scenarios").Items)
            {
                var blocks = new List<BlockDef>();
                foreach (var bl in s.GetArray("blocks").Items)
                {
                    var overrides = new List<DemandOverride>();
                    var demand = bl.GetOptional("demand");
                    if (demand != null)
                    {
                        foreach (var o in demand.Items)
                        {
                            overrides.Add(new DemandOverride
                            {
                                DistrictId = o.GetString("district"),
                                DemandW = o.GetLong("demandW"),
                                RevenuePerTickCents = o.GetLongOr("revenuePerTickCents", -1)
                            });
                        }
                    }
                    blocks.Add(new BlockDef
                    {
                        Ticks = bl.GetInt("ticks"),
                        ImportCapW = bl.GetLong("importCapW"),
                        Overrides = overrides.ToArray()
                    });
                }
                scenarios.Add(new ScenarioDef
                {
                    Id = s.GetString("id"),
                    Title = s.GetString("title"),
                    Briefing = s.GetStringOr("briefing", ""),
                    StageLabel = s.GetStringOr("stageLabel", ""),
                    RequiresPlayer = s.GetBool("requiresPlayer"),
                    LeadTicks = s.GetInt("leadTicks"),
                    AllowSkipLead = s.GetBoolOr("allowSkipLead", false),
                    Blocks = blocks.ToArray()
                });
            }
            c.Scenarios = scenarios.ToArray();
            c.Campaign = root.GetStringArray("campaign");
            c.RoutineScenarioId = root.GetObject("routine").GetString("scenario");

            var upgrades = new List<UpgradeDef>();
            foreach (var u in root.GetArray("upgrades").Items)
            {
                var effects = new List<UpgradeEffect>();
                foreach (var e in u.GetArray("effects").Items)
                    effects.Add(new UpgradeEffect { Type = e.GetString("type"), Value = e.GetLong("value") });
                upgrades.Add(new UpgradeDef
                {
                    Id = u.GetString("id"),
                    Name = u.GetString("name"),
                    Description = u.GetStringOr("description", ""),
                    CostCents = u.GetLong("costCents"),
                    Repeatable = u.GetBool("repeatable"),
                    Effects = effects.ToArray()
                });
            }
            c.Upgrades = upgrades.ToArray();
            return c;
        }

        /// <summary>Throws <see cref="ContentException"/> listing every problem found.</summary>
        public static void Validate(GameContent c)
        {
            var errors = new List<string>();
            void Check(bool ok, string message) { if (!ok) errors.Add(message); }

            Check(c.ContentVersion == SupportedContentVersion, "contentVersion must be " + SupportedContentVersion);
            Check(IsId(c.TownId), "town.id is required");
            Check(c.StartingCashCents >= 0, "startingCashCents must be >= 0");

            var t = c.Timing;
            Check(t.SimSecondsPerTick > 0, "timing.simSecondsPerTick must be > 0");
            Check(t.StartMinuteOfDay >= 0 && t.StartMinuteOfDay < 1440, "timing.startMinuteOfDay must be 0..1439");
            Check(t.RunTicksPerSecond > 0 && t.RunTicksPerSecond <= 1000, "timing.runTicksPerSecond must be 1..1000");
            Check(t.SkipTicksPerSecond > 0 && t.SkipTicksPerSecond <= 1000, "timing.skipTicksPerSecond must be 1..1000");
            Check(t.IdleRealMsPerTick > 0, "timing.idleRealMsPerTick must be > 0");
            Check(t.OfflineCapMs > 0, "timing.offlineCapMs must be > 0");
            Check(t.CheckpointIntervalMs > 0, "timing.checkpointIntervalMs must be > 0");
            Check(t.MaxTicksPerFrame > 0, "timing.maxTicksPerFrame must be > 0");
            Check(t.MaxFrameDeltaMs > 0, "timing.maxFrameDeltaMs must be > 0");

            Check(c.Districts.Length > 0, "at least one district is required");
            var districtIds = new HashSet<string>(StringComparer.Ordinal);
            foreach (var d in c.Districts)
            {
                Check(IsId(d.Id), "district id '" + d.Id + "' is invalid");
                Check(districtIds.Add(d.Id ?? ""), "duplicate district id '" + d.Id + "'");
                Check(!string.IsNullOrEmpty(d.Name), "district '" + d.Id + "' needs a name");
                Check(d.DemandW > 0, "district '" + d.Id + "' demandW must be > 0");
                Check(d.RevenuePerTickCents >= 0, "district '" + d.Id + "' revenuePerTickCents must be >= 0");
            }

            Check(c.DefaultPriority.Length == c.Districts.Length && IsPermutation(c.DefaultPriority, districtIds),
                "defaultPriority must list every district exactly once");

            var b = c.Battery;
            Check(b != null, "battery is required");
            if (b != null)
            {
                Check(b.MaxDischargeW > 0, "battery.maxDischargeW must be > 0");
                Check(b.CapacityWs > 0, "battery.capacityWs must be > 0");
                Check(b.StartChargeWs >= 0 && b.StartChargeWs <= b.CapacityWs, "battery.startChargeWs must be 0..capacity");
                Check(b.RechargeW >= 0, "battery.rechargeW must be >= 0");
            }

            var g = c.Generator;
            Check(g != null, "generator is required");
            if (g != null)
            {
                Check(g.MaxOutputW > 0, "generator.maxOutputW must be > 0");
                Check(g.FuelMlPerKWh > 0, "generator.fuelMlPerKWh must be > 0");
                Check(g.TankCapacityMl > 0, "generator.tankCapacityMl must be > 0");
                Check(g.StartFuelMl >= 0 && g.StartFuelMl <= g.TankCapacityMl, "generator.startFuelMl must be 0..tank");
                Check(g.FuelPriceCentsPerLitre >= 0, "generator.fuelPriceCentsPerLitre must be >= 0");
            }

            var scenarioIds = new HashSet<string>(StringComparer.Ordinal);
            foreach (var s in c.Scenarios)
            {
                Check(IsId(s.Id), "scenario id '" + s.Id + "' is invalid");
                Check(scenarioIds.Add(s.Id ?? ""), "duplicate scenario id '" + s.Id + "'");
                Check(s.LeadTicks >= 0, "scenario '" + s.Id + "' leadTicks must be >= 0");
                Check(s.Blocks.Length > 0, "scenario '" + s.Id + "' needs at least one block");
                foreach (var bl in s.Blocks)
                {
                    Check(bl.Ticks > 0, "scenario '" + s.Id + "' block ticks must be > 0");
                    Check(bl.ImportCapW >= 0, "scenario '" + s.Id + "' importCapW must be >= 0");
                    var seen = new HashSet<string>(StringComparer.Ordinal);
                    foreach (var o in bl.Overrides)
                    {
                        Check(districtIds.Contains(o.DistrictId ?? ""), "scenario '" + s.Id + "' overrides unknown district '" + o.DistrictId + "'");
                        Check(seen.Add(o.DistrictId ?? ""), "scenario '" + s.Id + "' overrides '" + o.DistrictId + "' twice in one block");
                        Check(o.DemandW >= 0, "scenario '" + s.Id + "' override demandW must be >= 0");
                        Check(o.RevenuePerTickCents >= -1, "scenario '" + s.Id + "' override revenuePerTickCents must be >= 0");
                    }
                }
            }

            Check(c.Campaign.Length > 0, "campaign needs at least one scenario");
            foreach (var id in c.Campaign)
            {
                var s = c.FindScenario(id);
                Check(s != null, "campaign references unknown scenario '" + id + "'");
                if (s != null) Check(s.RequiresPlayer, "campaign scenario '" + id + "' must require the player");
            }
            var routine = c.FindScenario(c.RoutineScenarioId);
            Check(routine != null, "routine references unknown scenario '" + c.RoutineScenarioId + "'");
            if (routine != null)
            {
                Check(!routine.AllowSkipLead, "routine scenario cannot allow skipping its lead");
                Check(routine.LeadTicks > 0, "routine scenario needs leadTicks > 0");
            }

            var upgradeIds = new HashSet<string>(StringComparer.Ordinal);
            foreach (var u in c.Upgrades)
            {
                Check(IsId(u.Id), "upgrade id '" + u.Id + "' is invalid");
                Check(upgradeIds.Add(u.Id ?? ""), "duplicate upgrade id '" + u.Id + "'");
                Check(u.CostCents > 0, "upgrade '" + u.Id + "' costCents must be > 0");
                Check(u.Effects.Length > 0, "upgrade '" + u.Id + "' needs at least one effect");
                foreach (var e in u.Effects)
                {
                    Check(UpgradeEffectTypes.IsKnown(e.Type), "upgrade '" + u.Id + "' has unknown effect '" + e.Type + "'");
                    Check(e.Value > 0, "upgrade '" + u.Id + "' effect values must be > 0");
                }
            }

            if (errors.Count > 0) throw new ContentException(string.Join("; ", errors));
        }

        public static bool IsId(string id)
        {
            if (string.IsNullOrEmpty(id) || id.Length > 64) return false;
            foreach (var ch in id)
            {
                bool ok = (ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9') || ch == '.' || ch == '-' || ch == '_';
                if (!ok) return false;
            }
            return true;
        }

        public static bool IsPermutation(string[] values, HashSet<string> expected)
        {
            if (values == null || values.Length != expected.Count) return false;
            var seen = new HashSet<string>(StringComparer.Ordinal);
            foreach (var v in values)
            {
                if (v == null || !expected.Contains(v) || !seen.Add(v)) return false;
            }
            return true;
        }
    }
}
