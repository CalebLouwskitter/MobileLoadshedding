using System;
using System.Text;
using IdleLoadshedding.Simulation;

namespace IdleLoadshedding.Session
{
    public enum DecodeStatus
    {
        Ok,
        /// <summary>Unreadable, truncated, tampered or failing validation.</summary>
        Corrupt,
        /// <summary>Written by a newer build. Never overwritten or downgraded silently.</summary>
        FutureSchema,
        /// <summary>An older schema with no migration path.</summary>
        Unsupported
    }

    public sealed class DecodeResult
    {
        public DecodeStatus Status;
        public string Detail = "";
        public SimState State;
        public long Revision;
        public int SchemaVersion;
        public long SavedUtcMs;
    }

    /// <summary>
    /// The versioned save envelope. The payload checksum is FNV-1a 64 over the canonical payload
    /// JSON, so any edit, truncation or bit flip that still parses is caught.
    /// </summary>
    public static class SaveCodec
    {
        public const string Format = "idle-loadshedding-save";
        public const int CurrentSchemaVersion = 1;
        /// <summary>Saves larger than this are rejected before parsing.</summary>
        public const int MaxSaveBytes = 256 * 1024;

        private static readonly UTF8Encoding Utf8 = new UTF8Encoding(false, true);

        public static byte[] Encode(GameContent content, SimState state, long revision, long savedUtcMs, string slotId)
        {
            var payload = StateJson.Write(content, state);
            string checksum = StateHash.Hex(StateHash.Fnv1a64(Json.Write(payload)));
            var envelope = JsonValue.NewObject()
                .Set("format", Format)
                .Set("schemaVersion", CurrentSchemaVersion)
                .Set("rulesetVersion", GridRules.RulesetVersion)
                .Set("contentVersion", content.ContentVersion)
                .Set("slotId", slotId)
                .Set("cityId", content.TownId)
                .Set("revision", revision)
                .Set("savedUtcMs", savedUtcMs)
                .Set("payloadFnv1a64", checksum)
                .Set("payload", payload);
            return Utf8.GetBytes(Json.Write(envelope, indented: true));
        }

        public static DecodeResult Decode(GameContent content, byte[] bytes)
        {
            if (bytes == null || bytes.Length == 0) return Corrupt("empty file");
            if (bytes.Length > MaxSaveBytes) return Corrupt("file too large");

            string text;
            try
            {
                text = Utf8.GetString(bytes);
            }
            catch (ArgumentException)
            {
                return Corrupt("not UTF-8");
            }

            JsonValue envelope;
            try
            {
                envelope = Json.Parse(text);
                if (envelope.Kind != JsonKind.Object) return Corrupt("not a JSON object");
            }
            catch (JsonException e)
            {
                return Corrupt("unreadable JSON: " + e.Message);
            }

            try
            {
                if (envelope.GetString("format") != Format) return Corrupt("not an IdleLoadshedding save");
                int schema = envelope.GetInt("schemaVersion");
                long revision = envelope.GetLong("revision");
                if (schema > CurrentSchemaVersion)
                    return new DecodeResult { Status = DecodeStatus.FutureSchema, SchemaVersion = schema, Revision = revision, Detail = "schema " + schema + " is newer than " + CurrentSchemaVersion };
                if (schema < 1)
                    return new DecodeResult { Status = DecodeStatus.Unsupported, SchemaVersion = schema, Revision = revision, Detail = "schema " + schema + " has no migration" };
                // Migrations from older schemas would run here, oldest first, before validation.

                if (revision < 1) return Corrupt("revision must be >= 1");
                if (envelope.GetString("cityId") != content.TownId) return Corrupt("save belongs to another town");

                var payload = envelope.GetObject("payload");
                string expected = envelope.GetString("payloadFnv1a64");
                string actual = StateHash.Hex(StateHash.Fnv1a64(Json.Write(payload)));
                if (!string.Equals(expected, actual, StringComparison.Ordinal)) return Corrupt("checksum mismatch");

                var state = StateJson.Read(content, payload);
                return new DecodeResult
                {
                    Status = DecodeStatus.Ok,
                    State = state,
                    Revision = revision,
                    SchemaVersion = schema,
                    SavedUtcMs = envelope.GetLong("savedUtcMs")
                };
            }
            catch (JsonException e)
            {
                return Corrupt("bad envelope: " + e.Message);
            }
            catch (StateFormatException e)
            {
                return Corrupt("invalid state: " + e.Message);
            }
        }

        private static DecodeResult Corrupt(string detail) =>
            new DecodeResult { Status = DecodeStatus.Corrupt, Detail = detail };
    }
}
