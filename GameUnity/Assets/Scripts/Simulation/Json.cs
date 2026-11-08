using System;
using System.Collections.Generic;
using System.Globalization;
using System.Text;

namespace IdleLoadshedding.Simulation
{
    /// <summary>Kinds of value the integer-only JSON subset supports.</summary>
    public enum JsonKind
    {
        Null,
        Bool,
        Integer,
        String,
        Array,
        Object
    }

    /// <summary>Raised for malformed JSON or for a document that does not match the expected shape.</summary>
    public sealed class JsonException : Exception
    {
        public JsonException(string message) : base(message) { }
    }

    /// <summary>
    /// A small JSON value tree. Numbers are 64-bit integers only: content and saves never use floats,
    /// so the reader rejects fractions and exponents. Object members keep insertion order, which makes
    /// <see cref="Json.Write"/> canonical for checksums and state hashes.
    /// </summary>
    public sealed class JsonValue
    {
        private readonly bool _bool;
        private readonly long _integer;
        private readonly string _string;
        private readonly List<JsonValue> _items;
        private readonly List<KeyValuePair<string, JsonValue>> _members;

        private JsonValue(JsonKind kind, bool b = false, long i = 0, string s = null)
        {
            Kind = kind;
            _bool = b;
            _integer = i;
            _string = s;
            if (kind == JsonKind.Array) _items = new List<JsonValue>();
            if (kind == JsonKind.Object) _members = new List<KeyValuePair<string, JsonValue>>();
        }

        public JsonKind Kind { get; }

        public static readonly JsonValue Null = new JsonValue(JsonKind.Null);

        public static JsonValue From(bool value) => new JsonValue(JsonKind.Bool, b: value);
        public static JsonValue From(long value) => new JsonValue(JsonKind.Integer, i: value);
        public static JsonValue From(string value) => value == null ? Null : new JsonValue(JsonKind.String, s: value);
        public static JsonValue NewArray() => new JsonValue(JsonKind.Array);
        public static JsonValue NewObject() => new JsonValue(JsonKind.Object);

        public static JsonValue FromStrings(IEnumerable<string> values)
        {
            var array = NewArray();
            foreach (var v in values) array.Add(From(v));
            return array;
        }

        public static JsonValue FromLongs(IEnumerable<long> values)
        {
            var array = NewArray();
            foreach (var v in values) array.Add(From(v));
            return array;
        }

        public static JsonValue FromInts(IEnumerable<int> values)
        {
            var array = NewArray();
            foreach (var v in values) array.Add(From((long)v));
            return array;
        }

        public IReadOnlyList<JsonValue> Items
        {
            get { Require(JsonKind.Array); return _items; }
        }

        public IReadOnlyList<KeyValuePair<string, JsonValue>> Members
        {
            get { Require(JsonKind.Object); return _members; }
        }

        public bool AsBool { get { Require(JsonKind.Bool); return _bool; } }
        public long AsLong { get { Require(JsonKind.Integer); return _integer; } }
        public string AsString { get { Require(JsonKind.String); return _string; } }

        public int AsInt
        {
            get
            {
                var v = AsLong;
                if (v < int.MinValue || v > int.MaxValue) throw new JsonException("Integer out of range: " + v);
                return (int)v;
            }
        }

        public JsonValue Add(JsonValue value)
        {
            Require(JsonKind.Array);
            _items.Add(value ?? Null);
            return this;
        }

        /// <summary>Sets a member, replacing an existing key in place or appending a new one.</summary>
        public JsonValue Set(string key, JsonValue value)
        {
            Require(JsonKind.Object);
            if (key == null) throw new ArgumentNullException(nameof(key));
            for (int i = 0; i < _members.Count; i++)
            {
                if (_members[i].Key == key)
                {
                    _members[i] = new KeyValuePair<string, JsonValue>(key, value ?? Null);
                    return this;
                }
            }
            _members.Add(new KeyValuePair<string, JsonValue>(key, value ?? Null));
            return this;
        }

        public JsonValue Set(string key, long value) => Set(key, From(value));
        public JsonValue Set(string key, bool value) => Set(key, From(value));
        public JsonValue Set(string key, string value) => Set(key, From(value));

        public bool TryGet(string key, out JsonValue value)
        {
            Require(JsonKind.Object);
            for (int i = 0; i < _members.Count; i++)
            {
                if (_members[i].Key == key)
                {
                    value = _members[i].Value;
                    return true;
                }
            }
            value = null;
            return false;
        }

        public bool Has(string key) => TryGet(key, out _);

        public JsonValue Get(string key)
        {
            if (!TryGet(key, out var value)) throw new JsonException("Missing key '" + key + "'");
            return value;
        }

        public long GetLong(string key) => Expect(key, JsonKind.Integer).AsLong;
        public int GetInt(string key) => Expect(key, JsonKind.Integer).AsInt;
        public bool GetBool(string key) => Expect(key, JsonKind.Bool).AsBool;
        public string GetString(string key) => Expect(key, JsonKind.String).AsString;
        public JsonValue GetArray(string key) => Expect(key, JsonKind.Array);
        public JsonValue GetObject(string key) => Expect(key, JsonKind.Object);

        public long GetLongOr(string key, long fallback) =>
            TryGet(key, out var v) && v.Kind != JsonKind.Null ? Expect(key, JsonKind.Integer).AsLong : fallback;

        public bool GetBoolOr(string key, bool fallback) =>
            TryGet(key, out var v) && v.Kind != JsonKind.Null ? Expect(key, JsonKind.Bool).AsBool : fallback;

        public string GetStringOr(string key, string fallback) =>
            TryGet(key, out var v) && v.Kind != JsonKind.Null ? Expect(key, JsonKind.String).AsString : fallback;

        /// <summary>Returns the member, or null when it is absent or JSON null.</summary>
        public JsonValue GetOptional(string key) =>
            TryGet(key, out var v) && v.Kind != JsonKind.Null ? v : null;

        public string[] GetStringArray(string key)
        {
            var items = GetArray(key).Items;
            var result = new string[items.Count];
            for (int i = 0; i < items.Count; i++) result[i] = items[i].AsString;
            return result;
        }

        public int[] GetIntArray(string key)
        {
            var items = GetArray(key).Items;
            var result = new int[items.Count];
            for (int i = 0; i < items.Count; i++) result[i] = items[i].AsInt;
            return result;
        }

        private JsonValue Expect(string key, JsonKind kind)
        {
            var value = Get(key);
            if (value.Kind != kind)
                throw new JsonException("Key '" + key + "' should be " + kind + " but is " + value.Kind);
            return value;
        }

        private void Require(JsonKind kind)
        {
            if (Kind != kind) throw new JsonException("Expected " + kind + " but found " + Kind);
        }
    }

    /// <summary>Strict parser and canonical writer for <see cref="JsonValue"/>.</summary>
    public static class Json
    {
        private const int MaxDepth = 64;

        public static JsonValue Parse(string text)
        {
            if (text == null) throw new JsonException("No JSON text");
            var parser = new Parser(text);
            parser.SkipWhitespace();
            // Tolerate a UTF-8 byte order mark decoded as U+FEFF.
            if (parser.Peek() == '﻿') parser.Advance();
            parser.SkipWhitespace();
            var value = parser.ParseValue(0);
            parser.SkipWhitespace();
            if (!parser.AtEnd) throw new JsonException("Unexpected trailing characters at " + parser.Position);
            return value;
        }

        public static string Write(JsonValue value, bool indented = false)
        {
            var sb = new StringBuilder(256);
            WriteValue(sb, value ?? JsonValue.Null, indented, 0);
            return sb.ToString();
        }

        private static void WriteValue(StringBuilder sb, JsonValue value, bool indented, int depth)
        {
            switch (value.Kind)
            {
                case JsonKind.Null:
                    sb.Append("null");
                    break;
                case JsonKind.Bool:
                    sb.Append(value.AsBool ? "true" : "false");
                    break;
                case JsonKind.Integer:
                    sb.Append(value.AsLong.ToString(CultureInfo.InvariantCulture));
                    break;
                case JsonKind.String:
                    WriteString(sb, value.AsString);
                    break;
                case JsonKind.Array:
                {
                    var items = value.Items;
                    if (items.Count == 0) { sb.Append("[]"); break; }
                    sb.Append('[');
                    for (int i = 0; i < items.Count; i++)
                    {
                        if (i > 0) sb.Append(',');
                        NewLine(sb, indented, depth + 1);
                        WriteValue(sb, items[i], indented, depth + 1);
                    }
                    NewLine(sb, indented, depth);
                    sb.Append(']');
                    break;
                }
                case JsonKind.Object:
                {
                    var members = value.Members;
                    if (members.Count == 0) { sb.Append("{}"); break; }
                    sb.Append('{');
                    for (int i = 0; i < members.Count; i++)
                    {
                        if (i > 0) sb.Append(',');
                        NewLine(sb, indented, depth + 1);
                        WriteString(sb, members[i].Key);
                        sb.Append(indented ? ": " : ":");
                        WriteValue(sb, members[i].Value, indented, depth + 1);
                    }
                    NewLine(sb, indented, depth);
                    sb.Append('}');
                    break;
                }
            }
        }

        private static void NewLine(StringBuilder sb, bool indented, int depth)
        {
            if (!indented) return;
            sb.Append('\n');
            sb.Append(' ', depth * 2);
        }

        private static void WriteString(StringBuilder sb, string s)
        {
            sb.Append('"');
            foreach (var c in s)
            {
                switch (c)
                {
                    case '"': sb.Append("\\\""); break;
                    case '\\': sb.Append("\\\\"); break;
                    case '\n': sb.Append("\\n"); break;
                    case '\r': sb.Append("\\r"); break;
                    case '\t': sb.Append("\\t"); break;
                    case '\b': sb.Append("\\b"); break;
                    case '\f': sb.Append("\\f"); break;
                    default:
                        if (c < 0x20)
                            sb.Append("\\u").Append(((int)c).ToString("x4", CultureInfo.InvariantCulture));
                        else
                            sb.Append(c);
                        break;
                }
            }
            sb.Append('"');
        }

        private sealed class Parser
        {
            private readonly string _text;
            private int _pos;

            public Parser(string text) { _text = text; }

            public bool AtEnd => _pos >= _text.Length;
            public int Position => _pos;

            public char Peek() => _pos < _text.Length ? _text[_pos] : '\0';
            public void Advance() => _pos++;

            public void SkipWhitespace()
            {
                while (_pos < _text.Length)
                {
                    var c = _text[_pos];
                    if (c == ' ' || c == '\t' || c == '\n' || c == '\r') _pos++;
                    else break;
                }
            }

            public JsonValue ParseValue(int depth)
            {
                if (depth > MaxDepth) throw new JsonException("JSON nested too deeply");
                SkipWhitespace();
                if (AtEnd) throw new JsonException("Unexpected end of JSON");
                var c = _text[_pos];
                switch (c)
                {
                    case '{': return ParseObject(depth);
                    case '[': return ParseArray(depth);
                    case '"': return JsonValue.From(ParseString());
                    case 't': ExpectLiteral("true"); return JsonValue.From(true);
                    case 'f': ExpectLiteral("false"); return JsonValue.From(false);
                    case 'n': ExpectLiteral("null"); return JsonValue.Null;
                    default:
                        if (c == '-' || (c >= '0' && c <= '9')) return JsonValue.From(ParseInteger());
                        throw new JsonException("Unexpected character '" + c + "' at " + _pos);
                }
            }

            private JsonValue ParseObject(int depth)
            {
                var obj = JsonValue.NewObject();
                _pos++; // {
                SkipWhitespace();
                if (Peek() == '}') { _pos++; return obj; }
                while (true)
                {
                    SkipWhitespace();
                    if (Peek() != '"') throw new JsonException("Expected object key at " + _pos);
                    var key = ParseString();
                    if (obj.Has(key)) throw new JsonException("Duplicate key '" + key + "'");
                    SkipWhitespace();
                    if (Peek() != ':') throw new JsonException("Expected ':' at " + _pos);
                    _pos++;
                    var value = ParseValue(depth + 1);
                    obj.Set(key, value);
                    SkipWhitespace();
                    var c = Peek();
                    _pos++;
                    if (c == ',') continue;
                    if (c == '}') return obj;
                    throw new JsonException("Expected ',' or '}' at " + (_pos - 1));
                }
            }

            private JsonValue ParseArray(int depth)
            {
                var array = JsonValue.NewArray();
                _pos++; // [
                SkipWhitespace();
                if (Peek() == ']') { _pos++; return array; }
                while (true)
                {
                    array.Add(ParseValue(depth + 1));
                    SkipWhitespace();
                    var c = Peek();
                    _pos++;
                    if (c == ',') continue;
                    if (c == ']') return array;
                    throw new JsonException("Expected ',' or ']' at " + (_pos - 1));
                }
            }

            private string ParseString()
            {
                _pos++; // opening quote
                var sb = new StringBuilder();
                while (true)
                {
                    if (AtEnd) throw new JsonException("Unterminated string");
                    var c = _text[_pos++];
                    if (c == '"') return sb.ToString();
                    if (c < 0x20) throw new JsonException("Control character in string at " + (_pos - 1));
                    if (c != '\\') { sb.Append(c); continue; }
                    if (AtEnd) throw new JsonException("Unterminated escape");
                    var e = _text[_pos++];
                    switch (e)
                    {
                        case '"': sb.Append('"'); break;
                        case '\\': sb.Append('\\'); break;
                        case '/': sb.Append('/'); break;
                        case 'b': sb.Append('\b'); break;
                        case 'f': sb.Append('\f'); break;
                        case 'n': sb.Append('\n'); break;
                        case 'r': sb.Append('\r'); break;
                        case 't': sb.Append('\t'); break;
                        case 'u':
                            if (_pos + 4 > _text.Length) throw new JsonException("Truncated unicode escape");
                            if (!int.TryParse(_text.Substring(_pos, 4), NumberStyles.HexNumber, CultureInfo.InvariantCulture, out var code))
                                throw new JsonException("Bad unicode escape at " + _pos);
                            sb.Append((char)code);
                            _pos += 4;
                            break;
                        default:
                            throw new JsonException("Bad escape '\\" + e + "'");
                    }
                }
            }

            private long ParseInteger()
            {
                int start = _pos;
                bool negative = false;
                if (Peek() == '-') { negative = true; _pos++; }
                if (AtEnd || _text[_pos] < '0' || _text[_pos] > '9') throw new JsonException("Expected digit at " + _pos);
                if (_text[_pos] == '0' && _pos + 1 < _text.Length && _text[_pos + 1] >= '0' && _text[_pos + 1] <= '9')
                    throw new JsonException("Leading zero at " + _pos);
                long value = 0;
                while (!AtEnd && _text[_pos] >= '0' && _text[_pos] <= '9')
                {
                    int digit = _text[_pos] - '0';
                    // Accumulate as a negative number so long.MinValue parses without overflow.
                    if (value < (long.MinValue + digit) / 10)
                        throw new JsonException("Integer overflow at " + start);
                    value = value * 10 - digit;
                    _pos++;
                }
                if (!AtEnd && (_text[_pos] == '.' || _text[_pos] == 'e' || _text[_pos] == 'E'))
                    throw new JsonException("Only whole numbers are allowed (at " + start + ")");
                if (negative) return value;
                if (value == long.MinValue) throw new JsonException("Integer overflow at " + start);
                return -value;
            }

            private void ExpectLiteral(string literal)
            {
                if (string.CompareOrdinal(_text, _pos, literal, 0, literal.Length) != 0)
                    throw new JsonException("Expected '" + literal + "' at " + _pos);
                _pos += literal.Length;
            }
        }
    }
}
