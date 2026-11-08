using System;
using System.Globalization;
using System.Text;
using IdleLoadshedding.Simulation;

namespace IdleLoadshedding.Session
{
    /// <summary>Culture-independent display strings for the HUD. Integer inputs only.</summary>
    public static class DisplayFormat
    {
        /// <summary>Whole rands with space thousands separators, e.g. 124000 cents → "R1 240".</summary>
        public static string Rand(long cents)
        {
            bool negative = cents < 0;
            long rands = Math.Abs(cents) / 100;
            return (negative ? "-R" : "R") + Group(rands);
        }

        public static string SignedRand(long cents) => cents > 0 ? "+" + Rand(cents) : Rand(cents);

        /// <summary>Watts as kW, e.g. 30000 → "30 kW", 2500 → "2.5 kW".</summary>
        public static string Power(long watts) => Decimal1(watts, 1000) + " kW";

        /// <summary>W·s as kWh, e.g. 108000000 → "30 kWh".</summary>
        public static string Energy(long ws) => Decimal1(ws, Dispatcher.WsPerKWh) + " kWh";

        /// <summary>Millilitres as litres with one decimal, e.g. 18000 → "18.0 L".</summary>
        public static string Litres(long ml)
        {
            long tenths = ml / 100;
            return (tenths / 10).ToString(CultureInfo.InvariantCulture) + "." + Math.Abs(tenths % 10).ToString(CultureInfo.InvariantCulture) + " L";
        }

        public static string Clock(int minuteOfDay) =>
            (minuteOfDay / 60).ToString("00", CultureInfo.InvariantCulture) + ":" + (minuteOfDay % 60).ToString("00", CultureInfo.InvariantCulture);

        public static string DayClock(int day, int minuteOfDay) => "Day " + day.ToString(CultureInfo.InvariantCulture) + ", " + Clock(minuteOfDay);

        /// <summary>Simulated duration from a tick count, e.g. 90 ticks of 60 s → "1 h 30 min".</summary>
        public static string SimDuration(GameContent content, long ticks) =>
            Minutes(ticks * content.Timing.SimSecondsPerTick / 60);

        /// <summary>Real duration from milliseconds, rounded down to the minute.</summary>
        public static string RealDuration(long ms) => Minutes(Math.Max(0, ms) / 60000);

        public static string Minutes(long minutes)
        {
            if (minutes <= 0) return "0 min";
            long h = minutes / 60;
            long m = minutes % 60;
            if (h == 0) return m.ToString(CultureInfo.InvariantCulture) + " min";
            if (m == 0) return h.ToString(CultureInfo.InvariantCulture) + " h";
            return h.ToString(CultureInfo.InvariantCulture) + " h " + m.ToString(CultureInfo.InvariantCulture) + " min";
        }

        public static string Percent(int basisPoints) => (basisPoints / 100).ToString(CultureInfo.InvariantCulture) + "%";

        public static string PlanName(PlanKind plan)
        {
            switch (plan)
            {
                case PlanKind.Rotate: return "Rotate feeders";
                case PlanKind.Battery: return "Discharge battery";
                case PlanKind.Diesel: return "Run diesel";
                default: return plan.ToString();
            }
        }

        public static string PlanSummary(PlanKind plan)
        {
            switch (plan)
            {
                case PlanKind.Rotate: return "Keep protected feeders on and take turns with the rest. No cost.";
                case PlanKind.Battery: return "Cover the gap from storage until it runs out, then shed by priority.";
                case PlanKind.Diesel: return "Cover the gap with the generator for as long as the fuel lasts.";
                default: return "";
            }
        }

        public static string Reason(string reason)
        {
            switch (reason)
            {
                case CommandReasons.InsufficientFunds: return "Not enough cash";
                case CommandReasons.AlreadyOwned: return "Installed";
                case CommandReasons.TankFull: return "Tank too full";
                case CommandReasons.WrongPhase: return "Not during a cut";
                case CommandReasons.CannotSkip: return "Cannot skip this wait";
                case SessionCoordinator.SaveFailed: return "Could not save. Nothing was changed.";
                case SessionCoordinator.Busy: return "Still saving";
                case SessionCoordinator.NotReady: return "Still loading";
                case "": return "";
                default: return reason.Replace('_', ' ');
            }
        }

        public static string StopReasonText(StopReason stop)
        {
            switch (stop)
            {
                case StopReason.CapReached: return "Progress while away is capped at 4 hours.";
                case StopReason.DecisionNeeded: return "A new cut needs your decision.";
                case StopReason.CutPaused: return "Your cut is paused until you return.";
                default: return "";
            }
        }

        private static string Decimal1(long value, long unit)
        {
            long tenths = value * 10 / unit;
            long whole = tenths / 10;
            long frac = Math.Abs(tenths % 10);
            return frac == 0
                ? whole.ToString(CultureInfo.InvariantCulture)
                : whole.ToString(CultureInfo.InvariantCulture) + "." + frac.ToString(CultureInfo.InvariantCulture);
        }

        private static string Group(long value)
        {
            var digits = value.ToString(CultureInfo.InvariantCulture);
            var sb = new StringBuilder(digits.Length + digits.Length / 3);
            for (int i = 0; i < digits.Length; i++)
            {
                if (i > 0 && (digits.Length - i) % 3 == 0) sb.Append(' ');
                sb.Append(digits[i]);
            }
            return sb.ToString();
        }
    }
}
