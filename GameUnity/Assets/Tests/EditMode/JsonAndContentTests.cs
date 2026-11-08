using IdleLoadshedding.Simulation;
using NUnit.Framework;

namespace IdleLoadshedding.Tests
{
    public class JsonTests
    {
        [Test]
        public void RoundTripIsCanonical()
        {
            var text = "{\"a\":1,\"b\":[true,false,null],\"c\":\"x\\\"y\\n\",\"d\":-9223372036854775808,\"e\":{}}";
            var value = Json.Parse(text);
            Assert.That(Json.Write(value), Is.EqualTo(text));
            Assert.That(Json.Write(Json.Parse(Json.Write(value, indented: true))), Is.EqualTo(text));
        }

        [TestCase("1.5")]
        [TestCase("1e3")]
        [TestCase("{\"a\":1,\"a\":2}")]
        [TestCase("9223372036854775808")]
        [TestCase("[1,]")]
        [TestCase("{\"a\":1")]
        [TestCase("01")]
        [TestCase("\"unterminated")]
        [TestCase("{} trailing")]
        public void RejectsMalformedOrFractionalInput(string text)
        {
            Assert.Throws<JsonException>(() => Json.Parse(text));
        }
    }

    public class ContentTests
    {
        [Test]
        public void ShippingContentMatchesTheFirstPlayableBrief()
        {
            var c = TestContent.Load();
            Assert.That(c.TownName, Is.EqualTo("Kwanele Bay"));
            Assert.That(c.Districts[Sim.Index(c, "homes")].DemandW, Is.EqualTo(40000));
            Assert.That(c.Districts[Sim.Index(c, "market")].DemandW, Is.EqualTo(35000));
            Assert.That(c.Districts[Sim.Index(c, "clinic")].DemandW, Is.EqualTo(15000));
            Assert.That(c.Districts[Sim.Index(c, "clinic")].ProtectedByDefault, Is.True);

            var first = c.FindScenario(c.Campaign[0]);
            Assert.That(first.Blocks.Length, Is.EqualTo(2));
            foreach (var block in first.Blocks)
            {
                Assert.That(block.ImportCapW, Is.EqualTo(60000));
                Assert.That(block.Ticks * c.Timing.SimSecondsPerTick, Is.EqualTo(3600), "each block is one simulated hour");
            }
            Assert.That(first.LeadTicks, Is.EqualTo(0), "the first decision is available immediately");

            Assert.That(c.Battery.MaxDischargeW, Is.EqualTo(30000));
            Assert.That(c.Battery.CapacityWs, Is.EqualTo(30L * 1000 * 3600), "30 kWh");
            Assert.That(c.Generator.MaxOutputW, Is.EqualTo(30000));
            Assert.That(c.Timing.OfflineCapMs, Is.EqualTo(4L * 3600 * 1000), "four-hour offline cap");
        }

        [Test]
        public void RejectsBrokenReferences()
        {
            var broken = TestContent.Json.Replace("\"defaultPriority\": [\"clinic\", \"homes\", \"market\"]", "\"defaultPriority\": [\"clinic\", \"homes\", \"harbour\"]");
            Assert.That(broken, Is.Not.EqualTo(TestContent.Json));
            var e = Assert.Throws<ContentException>(() => ContentLoader.Parse(broken));
            StringAssert.Contains("defaultPriority", e.Message);
        }

        [Test]
        public void RejectsFractionalTuningValues()
        {
            var broken = TestContent.Json.Replace("\"demandW\": 40000", "\"demandW\": 40000.5");
            Assert.Throws<ContentException>(() => ContentLoader.Parse(broken));
        }
    }
}
