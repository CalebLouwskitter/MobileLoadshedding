using System;
using System.Collections;
using System.IO;
using IdleLoadshedding.Game;
using IdleLoadshedding.Session;
using IdleLoadshedding.Simulation;
using NUnit.Framework;
using UnityEngine;
using UnityEngine.TestTools;
using Object = UnityEngine.Object;

namespace IdleLoadshedding.Tests
{
    /// <summary>
    /// Boots the real composition root (views, session, file-backed save) in a throwaway save
    /// folder and drives it through the HUD's buttons.
    /// </summary>
    public class BootAndInteractionTests
    {
        private string _saveDir;
        private GameApp _app;

        [SetUp]
        public void SetUp()
        {
            GameApp.AutoStartEnabled = false;
            _saveDir = Path.Combine(Application.temporaryCachePath, "ils-playmode-" + Guid.NewGuid().ToString("N"));
        }

        [TearDown]
        public void TearDown()
        {
            if (_app != null) Object.Destroy(_app.gameObject);
            if (Directory.Exists(_saveDir)) Directory.Delete(_saveDir, true);
        }

        private IEnumerator LaunchAndWait()
        {
            _app = GameApp.Launch(new GameAppOptions { SaveDirectory = _saveDir });
            yield return WaitFor(() => _app.InputEnabled, 20f);
        }

        private static IEnumerator WaitFor(Func<bool> condition, float timeoutSeconds)
        {
            float deadline = Time.realtimeSinceStartup + timeoutSeconds;
            while (!condition())
            {
                if (Time.realtimeSinceStartup > deadline) Assert.Fail("Timed out after " + timeoutSeconds + " s");
                yield return null;
            }
        }

        [UnityTest]
        public IEnumerator FreshBootReachesTheOutageDecisionQuickly()
        {
            float start = Time.realtimeSinceStartup;
            yield return LaunchAndWait();
            var snap = _app.Session.Snapshot;
            Assert.That(snap.Phase, Is.EqualTo(Phase.Prep));
            Assert.That(snap.SelectedForecast.PeakShortfallW, Is.EqualTo(30000));
            Assert.That(_app.Hud.HasButton("run-cut"), Is.True);
            Assert.That(_app.Hud.HasButton("plan-battery"), Is.True);
            Assert.That(Time.realtimeSinceStartup - start, Is.LessThan(60f));
            Assert.That(File.Exists(Path.Combine(_saveDir, SaveStore.PrimaryName)), Is.True, "saved before input was enabled");
        }

        [UnityTest]
        public IEnumerator RunButtonPlaysTheCutAndTheCityFollowsTheRules()
        {
            yield return LaunchAndWait();
            Assert.That(_app.Hud.Press("plan-rotate"), Is.True);
            yield return null;
            Assert.That(_app.Hud.Press("run-cut"), Is.True);
            yield return WaitFor(() => _app.Session.Snapshot.Phase == Phase.Running && _app.Session.Snapshot.CutTicksElapsed >= 3, 10f);
            Assert.That(_app.City.LightsSignature, Is.EqualTo("homes:1,market:0,clinic:1"), "block 1 of the rotation");

            yield return WaitFor(() => _app.Session.Snapshot.Phase == Phase.Normal, 40f);
            Assert.That(_app.Session.Snapshot.ShowReport, Is.True);
            Assert.That(_app.Hud.HasButton("report-done"), Is.True);
            Assert.That(_app.City.LightsSignature, Is.EqualTo("homes:1,market:1,clinic:1"), "normal supply after the cut");
        }

        [UnityTest]
        public IEnumerator UpgradeSurvivesARelaunchWithoutAPause()
        {
            yield return LaunchAndWait();
            _app.Hud.Press("plan-battery");
            yield return null;
            _app.Hud.Press("run-cut");
            yield return WaitFor(() => _app.Session.Snapshot.Phase == Phase.Normal, 40f);
            Assert.That(_app.Hud.Press("open-shop"), Is.True);
            yield return null;
            Assert.That(_app.Hud.Press("buy-upgrade.battery-module"), Is.True);
            yield return null;
            Assert.That(_app.Hud.Press("save-policy"), Is.True);
            yield return null;

            var before = _app.Session.CopyState();
            var content = _app.Session.Content;
            Object.Destroy(_app.gameObject);  // no pause or quit callback, like a force-stop
            _app = null;
            yield return null;

            yield return LaunchAndWait();
            var after = _app.Session.CopyState();
            Assert.That(after.UpgradeCount("upgrade.battery-module"), Is.EqualTo(1));
            Assert.That(after.CashCents, Is.EqualTo(before.CashCents));
            Assert.That(after.Policy.SameAs(before.Policy), Is.True);
            Assert.That(StateHash.Simulation(content, after), Is.EqualTo(StateHash.Simulation(content, before)));
            Assert.That(_app.City.LightsSignature, Is.EqualTo(_app.Session.Snapshot.LightsSignature));
        }

        [UnityTest]
        public IEnumerator PauseWritesACheckpointMidCut()
        {
            yield return LaunchAndWait();
            _app.Hud.Press("run-cut");
            yield return WaitFor(() => _app.Session.Snapshot.CutTicksElapsed >= 10, 10f);
            long revision = _app.Session.Revision;
            _app.SendMessage("OnApplicationPause", true);
            Assert.That(_app.Session.Revision, Is.GreaterThan(revision));
            int ticksAtPause = _app.Session.CopyState().Cut.TicksElapsed;

            Object.Destroy(_app.gameObject);
            _app = null;
            yield return null;
            yield return LaunchAndWait();
            var restored = _app.Session.CopyState();
            Assert.That(restored.Phase, Is.EqualTo(Phase.Running));
            Assert.That(restored.Cut.TicksElapsed, Is.GreaterThanOrEqualTo(ticksAtPause));
        }
    }
}
