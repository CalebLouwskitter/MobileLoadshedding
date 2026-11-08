using System;
using System.Collections;
using System.IO;
using IdleLoadshedding.Session;
using IdleLoadshedding.Simulation;
using UnityEngine;
using UnityEngine.SceneManagement;

namespace IdleLoadshedding.Game
{
    public sealed class GameAppOptions
    {
        /// <summary>Save slot folder. Defaults to persistentDataPath/saves/slot-1.</summary>
        public string SaveDirectory;
        /// <summary>Wall clock. Defaults to the system clock.</summary>
        public IClock Clock;
        public bool BuildCity = true;
    }

    /// <summary>
    /// Composition root. Owns the content, the save store, the session coordinator and the two
    /// views, and forwards Unity's frame and lifecycle callbacks. Holds no game rules itself.
    /// </summary>
    public sealed class GameApp : MonoBehaviour
    {
        public const string BootSceneName = "Boot";
        public const string ContentResource = "Content/town-content";

        /// <summary>Tests turn this off so they can launch with their own clock and save folder.</summary>
        public static bool AutoStartEnabled = true;

        public static GameApp Instance { get; private set; }

        private GameAppOptions _options;
        private GameContent _content;
        private SessionCoordinator _session;
        private CityView _city;
        private HudView _hud;
        private int _renderedVersion = -1;

        public SessionCoordinator Session => _session;
        public HudView Hud => _hud;
        public CityView City => _city;
        public string SaveDirectory { get; private set; }
        public bool InputEnabled => _session != null && _session.InputEnabled;

        [RuntimeInitializeOnLoadMethod(RuntimeInitializeLoadType.AfterSceneLoad)]
        private static void AutoStart()
        {
            if (!AutoStartEnabled || Instance != null) return;
            if (SceneManager.GetActiveScene().name != BootSceneName) return;
            Launch(new GameAppOptions());
        }

        public static GameApp Launch(GameAppOptions options)
        {
            var go = new GameObject("IdleLoadshedding");
            go.SetActive(false);
            var app = go.AddComponent<GameApp>();
            app._options = options ?? new GameAppOptions();
            go.SetActive(true);
            return app;
        }

        private void Awake()
        {
            Instance = this;
            if (_options == null) _options = new GameAppOptions();
            Application.targetFrameRate = 30;
            Screen.sleepTimeout = SleepTimeout.SystemSetting;

            _hud = HudView.Create(transform);
            _hud.ShowLoading("Loading Kwanele Bay...");

            var asset = Resources.Load<TextAsset>(ContentResource);
            if (asset == null)
            {
                Fatal("Town data is missing from the build (" + ContentResource + ").");
                return;
            }
            try
            {
                _content = ContentLoader.Parse(asset.text);
            }
            catch (ContentException e)
            {
                Fatal("Town data is invalid: " + e.Message);
                return;
            }

            SaveDirectory = _options.SaveDirectory ?? Path.Combine(Application.persistentDataPath, "saves", "slot-1");
            var clock = _options.Clock ?? new WallClock();
            var store = new SaveStore(new LocalFileSystem(), SaveDirectory, _content, clock);
            _session = new SessionCoordinator(_content, store, clock)
            {
                Log = message => Debug.Log("[ILS] " + message)
            };

            if (_options.BuildCity)
            {
                _city = CityView.Create(transform, _content);
                _hud.BindCity(_city);
            }
            _hud.Bind(_content, Submit, useBackup => _session.ResolveRecovery(useBackup), () => _session.Boot());
        }

        private IEnumerator Start()
        {
            if (_session == null) yield break;
            // Let the loading screen draw once before the synchronous load and settle.
            yield return null;
            _session.Boot();
            Render();
        }

        private void Update()
        {
            if (_session == null) return;
            long frameMs = (long)Math.Round(Time.unscaledDeltaTime * 1000.0);
            _session.Update(frameMs);
            Render();
        }

        private void Render()
        {
            var snapshot = _session.Snapshot;
            if (snapshot.Version == _renderedVersion) return;
            _renderedVersion = snapshot.Version;
            if (_city != null) _city.Render(snapshot);
            _hud.Render(snapshot);
        }

        private CommandResult Submit(GameCommand command)
        {
            var result = _session.Submit(command);
            if (result.Status == CommandStatus.Rejected) Debug.Log("[ILS] rejected " + command + ": " + result.Reason);
            Render();
            return result;
        }

        private void OnApplicationPause(bool paused)
        {
            if (_session == null) return;
            if (paused) _session.Pause();
            else _session.Resume();
            Render();
        }

        private void OnApplicationFocus(bool hasFocus)
        {
            if (_session != null && !hasFocus) _session.Pause();
        }

        private void OnApplicationQuit()
        {
            // Optional: play already checkpoints on every command, phase change and pause.
            if (_session != null) _session.Pause();
        }

        private void OnDestroy()
        {
            if (Instance == this) Instance = null;
        }

        private void Fatal(string message)
        {
            Debug.LogError("[ILS] " + message);
            _hud.ShowFatal(message);
        }
    }
}
