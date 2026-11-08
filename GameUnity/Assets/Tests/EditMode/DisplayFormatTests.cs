using IdleLoadshedding.Session;
using NUnit.Framework;

namespace IdleLoadshedding.Tests
{
    public class DisplayFormatTests
    {
        [TestCase(0L, "R0")]
        [TestCase(99L, "R0")]
        [TestCase(42000L, "R420")]
        [TestCase(124000L, "R1 240")]
        [TestCase(123456789L, "R1 234 567")]
        [TestCase(-48000L, "-R480")]
        public void RandUsesWholeRandsWithSpaceGrouping(long cents, string expected)
        {
            Assert.That(DisplayFormat.Rand(cents), Is.EqualTo(expected));
        }

        [Test]
        public void UnitsAreReadable()
        {
            Assert.That(DisplayFormat.Power(30000), Is.EqualTo("30 kW"));
            Assert.That(DisplayFormat.Power(2500), Is.EqualTo("2.5 kW"));
            Assert.That(DisplayFormat.Energy(108000000), Is.EqualTo("30 kWh"));
            Assert.That(DisplayFormat.Litres(18000), Is.EqualTo("18.0 L"));
            Assert.That(DisplayFormat.Litres(50), Is.EqualTo("0.0 L"));
            Assert.That(DisplayFormat.Clock(17 * 60 + 5), Is.EqualTo("17:05"));
            Assert.That(DisplayFormat.Minutes(90), Is.EqualTo("1 h 30 min"));
            Assert.That(DisplayFormat.Minutes(60), Is.EqualTo("1 h"));
            Assert.That(DisplayFormat.Minutes(13), Is.EqualTo("13 min"));
            Assert.That(DisplayFormat.Percent(8333), Is.EqualTo("83%"));
            Assert.That(DisplayFormat.SignedRand(42000), Is.EqualTo("+R420"));
        }
    }
}
