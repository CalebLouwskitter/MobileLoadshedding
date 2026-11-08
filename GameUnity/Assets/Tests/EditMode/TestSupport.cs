using System;
using System.Collections.Generic;
using System.IO;
using IdleLoadshedding.Session;
using IdleLoadshedding.Simulation;
using NUnit.Framework;

namespace IdleLoadshedding.Tests
{
    /// <summary>Loads the shipping town-content.json, so tests check exactly the data the game uses.</summary>
    public static class TestContent
    {
        private const string Relative = "Assets/Resources/Content/town-content.json";
        private static string _json;

        public static string Json
        {
            get
            {
                if (_json == null) _json = File.ReadAllText(Locate());
                return _json;
            }
        }

        public static GameContent Load() => ContentLoader.Parse(Json);

        public static string Locate()
        {
            var starts = new List<string> { Directory.GetCurrentDirectory() };
            try { starts.Add(TestContext.CurrentContext.TestDirectory); } catch (Exception) { /* not available in every runner */ }
            starts.Add(AppDomain.CurrentDomain.BaseDirectory);

            foreach (var start in starts)
            {
                var dir = new DirectoryInfo(start);
                while (dir != null)
                {
                    foreach (var candidate in new[] { Path.Combine(dir.FullName, Relative), Path.Combine(dir.FullName, "GameUnity", Relative) })
                        if (File.Exists(candidate)) return candidate;
                    dir = dir.Parent;
                }
            }
            throw new FileNotFoundException("Could not find " + Relative + " from " + string.Join(", ", starts));
        }
    }

    public sealed class FakeClock : IClock
    {
        public FakeClock(long start) { UtcNowMs = start; }
        public long UtcNowMs { get; set; }
        public void Advance(long ms) => UtcNowMs += ms;
    }

    /// <summary>Dictionary-backed file system. Renames are atomic, as on a POSIX file system.</summary>
    public class InMemoryFileSystem : ISaveFileSystem
    {
        public readonly Dictionary<string, byte[]> Files = new Dictionary<string, byte[]>(StringComparer.Ordinal);

        public virtual bool Exists(string path) => Files.ContainsKey(path);

        public virtual byte[] ReadAllBytes(string path)
        {
            if (!Files.TryGetValue(path, out var data)) throw new FileNotFoundException(path);
            return (byte[])data.Clone();
        }

        public virtual void WriteAllBytesDurable(string path, byte[] data) => Files[path] = (byte[])data.Clone();

        public virtual void Move(string from, string to)
        {
            if (!Files.TryGetValue(from, out var data)) throw new FileNotFoundException(from);
            if (Files.ContainsKey(to)) throw new IOException("Destination exists: " + to);
            Files.Remove(from);
            Files[to] = data;
        }

        public virtual void Copy(string from, string to)
        {
            if (!Files.TryGetValue(from, out var data)) throw new FileNotFoundException(from);
            Files[to] = (byte[])data.Clone();
        }

        public virtual void Delete(string path) => Files.Remove(path);

        public virtual void CreateDirectory(string path) { }

        public InMemoryFileSystem Snapshot()
        {
            var copy = new InMemoryFileSystem();
            foreach (var kv in Files) copy.Files[kv.Key] = (byte[])kv.Value.Clone();
            return copy;
        }
    }

    /// <summary>
    /// Simulates a crash at a chosen mutating operation. A failing write leaves a torn prefix behind,
    /// like a process killed mid-write.
    /// </summary>
    public sealed class FaultyFileSystem : InMemoryFileSystem
    {
        public int FailAtOperation = -1;
        public bool FailAllWrites;
        public int Operations;

        private void Tick(string what)
        {
            int op = Operations++;
            if (FailAllWrites || op == FailAtOperation) throw new IOException("Injected crash at operation " + op + " (" + what + ")");
        }

        public override void WriteAllBytesDurable(string path, byte[] data)
        {
            if (FailAllWrites || Operations == FailAtOperation)
            {
                var torn = new byte[data.Length / 2];
                Array.Copy(data, torn, torn.Length);
                Files[path] = torn;
            }
            Tick("write " + path);
            base.WriteAllBytesDurable(path, data);
        }

        public override void Move(string from, string to) { Tick("move " + from); base.Move(from, to); }
        public override void Copy(string from, string to) { Tick("copy " + from); base.Copy(from, to); }
        public override void Delete(string path) { Tick("delete " + path); base.Delete(path); }
    }

    public static class Sim
    {
        public const long T0 = 1_800_000_000_000L; // a fixed UTC instant in 2027

        public static SimState NewGame(GameContent content, long now = T0) => GridRules.NewGame(content, now);

        public static DispatchPlan Plan(GameContent content, PlanKind kind, params string[] priority)
        {
            var plan = DispatchPlan.Default(content);
            plan.Plan = kind;
            if (priority.Length > 0) plan.Priority = priority;
            return plan;
        }

        /// <summary>Starts the current Prep cut with the plan and steps until it finishes.</summary>
        public static CutStats RunCut(GameContent content, SimState state, DispatchPlan plan, Action<SimState> afterTick = null)
        {
            Assert.That(state.Phase, Is.EqualTo(Phase.Prep), "cut must be waiting in Prep");
            state.Draft = plan.Clone();
            var start = CommandRules.Apply(content, state, GameCommand.StartCut("start-" + state.Tick));
            Assert.That(start.Accepted, Is.True, start.ToString());
            return FinishRunningCut(content, state, afterTick);
        }

        public static CutStats FinishRunningCut(GameContent content, SimState state, Action<SimState> afterTick = null)
        {
            for (int guard = 0; guard < 100000; guard++)
            {
                var outcome = GridRules.Step(content, state);
                afterTick?.Invoke(state);
                if (outcome == StepOutcome.CutFinished) return state.LastReport;
                Assert.That(outcome, Is.EqualTo(StepOutcome.Ticked));
            }
            throw new InvalidOperationException("cut never finished");
        }

        /// <summary>Steps normal ticks until the next gate (Prep) is reached.</summary>
        public static void AdvanceToGate(GameContent content, SimState state)
        {
            for (int guard = 0; guard < 100000; guard++)
            {
                var outcome = GridRules.Step(content, state);
                if (outcome == StepOutcome.GateReached || state.Phase == Phase.Prep) return;
            }
            throw new InvalidOperationException("never reached a gate");
        }

        /// <summary>Plays both campaign cuts with rotation so the town is in routine play.</summary>
        public static SimState Routine(GameContent content, bool withPolicy)
        {
            var s = NewGame(content);
            RunCut(content, s, Plan(content, PlanKind.Rotate));
            AdvanceToGate(content, s);
            RunCut(content, s, Plan(content, PlanKind.Rotate));
            if (withPolicy) s.Policy = Plan(content, PlanKind.Rotate);
            return s;
        }

        public static int Index(GameContent content, string id) => content.DistrictIndex(id);
    }

    /// <summary>Deterministic pseudo-random sequence for property-style loops.</summary>
    public sealed class Lcg
    {
        private ulong _state;
        public Lcg(ulong seed) { _state = seed * 6364136223846793005UL + 1442695040888963407UL; }

        public int Next(int maxExclusive)
        {
            _state = _state * 6364136223846793005UL + 1442695040888963407UL;
            return (int)((_state >> 33) % (ulong)maxExclusive);
        }
    }
}
