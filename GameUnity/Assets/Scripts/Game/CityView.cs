using System.Collections.Generic;
using IdleLoadshedding.Session;
using IdleLoadshedding.Simulation;
using UnityEngine;
using UnityEngine.Rendering;

namespace IdleLoadshedding.Game
{
    /// <summary>A point above a district where the HUD pins that district's label.</summary>
    public sealed class CityAnchor
    {
        public string DistrictId;
        public Vector3 WorldPosition;
    }

    /// <summary>
    /// Placeholder low-poly town built from primitives under a fixed isometric camera. It only reads
    /// snapshots: lit or dark windows, open or shuttered stalls, the clinic sign, feeder lines and
    /// the battery and fuel gauges all follow the simulation's powered flags and balances.
    /// </summary>
    public sealed class CityView : MonoBehaviour
    {
        private static readonly Color Night = new Color(0.055f, 0.10f, 0.17f);
        private static readonly Color Ground = new Color(0.20f, 0.26f, 0.20f);
        private static readonly Color Road = new Color(0.16f, 0.17f, 0.19f);
        private static readonly Color Sand = new Color(0.55f, 0.50f, 0.38f);
        private static readonly Color Sea = new Color(0.07f, 0.20f, 0.33f);
        private static readonly Color WallLit = new Color(0.80f, 0.76f, 0.68f);
        private static readonly Color WallDark = new Color(0.30f, 0.30f, 0.33f);
        private static readonly Color Roof = new Color(0.55f, 0.24f, 0.18f);
        private static readonly Color WindowLit = new Color(1.00f, 0.83f, 0.42f);
        private static readonly Color WindowDark = new Color(0.10f, 0.12f, 0.17f);
        private static readonly Color LineLive = new Color(0.30f, 0.85f, 1.00f);
        private static readonly Color LineDead = new Color(0.22f, 0.24f, 0.28f);
        private static readonly Color ClinicCrossLit = new Color(0.95f, 0.15f, 0.15f);
        private static readonly Color ClinicCrossDark = new Color(0.25f, 0.08f, 0.08f);
        private static readonly Color Shutter = new Color(0.35f, 0.36f, 0.38f);
        private static readonly Color[] Awnings =
        {
            new Color(0.85f, 0.25f, 0.20f), new Color(0.95f, 0.75f, 0.20f), new Color(0.20f, 0.65f, 0.35f), new Color(0.20f, 0.45f, 0.80f)
        };
        private static readonly Color BatteryBar = new Color(0.30f, 0.90f, 0.45f);
        private static readonly Color FuelBar = new Color(1.00f, 0.65f, 0.15f);
        private static readonly Color GeneratorOn = new Color(1.00f, 0.55f, 0.10f);
        private static readonly Color GeneratorOff = new Color(0.20f, 0.18f, 0.16f);

        private sealed class DistrictVisual
        {
            public string Id;
            public string Kind;
            public readonly List<Renderer> Windows = new List<Renderer>();
            public readonly List<Renderer> Walls = new List<Renderer>();
            public readonly List<Renderer> Lamps = new List<Renderer>();
            public readonly List<Renderer> Awnings = new List<Renderer>();
            public readonly List<Color> AwningColors = new List<Color>();
            public readonly List<Renderer> Signs = new List<Renderer>();
            public Renderer FeederLine;
        }

        private readonly Dictionary<string, DistrictVisual> _districts = new Dictionary<string, DistrictVisual>();
        private readonly List<CityAnchor> _anchors = new List<CityAnchor>();
        private readonly Dictionary<Color, Material> _litCache = new Dictionary<Color, Material>();
        private readonly Dictionary<Color, Material> _unlitCache = new Dictionary<Color, Material>();
        private Material _litTemplate;
        private Material _unlitTemplate;
        private Transform _batteryBar;
        private Transform _fuelBar;
        private Renderer _generatorLamp;
        private Vector2Int _framedFor;

        public Camera Camera { get; private set; }
        public IReadOnlyList<CityAnchor> Anchors => _anchors;

        /// <summary>The powered state last drawn per district, for tests and device logs.</summary>
        public string LightsSignature { get; private set; } = "";

        public static CityView Create(Transform parent, GameContent content)
        {
            var go = new GameObject("City");
            go.transform.SetParent(parent, false);
            var view = go.AddComponent<CityView>();
            view.Build(content);
            return view;
        }

        private void Build(GameContent content)
        {
            LoadMaterials();
            RenderSettings.ambientMode = AmbientMode.Flat;
            RenderSettings.ambientLight = new Color(0.30f, 0.33f, 0.42f);

            var camGo = new GameObject("City Camera");
            camGo.transform.SetParent(transform, false);
            Camera = camGo.AddComponent<Camera>();
            Camera.orthographic = true;
            Camera.clearFlags = CameraClearFlags.SolidColor;
            Camera.backgroundColor = Night;
            Camera.nearClipPlane = 0.1f;
            Camera.farClipPlane = 200f;
            camGo.transform.rotation = Quaternion.Euler(35f, 45f, 0f);

            var lightGo = new GameObject("Moonlight");
            lightGo.transform.SetParent(transform, false);
            lightGo.transform.rotation = Quaternion.Euler(50f, -30f, 0f);
            var moon = lightGo.AddComponent<Light>();
            moon.type = LightType.Directional;
            moon.color = new Color(0.65f, 0.72f, 0.95f);
            moon.intensity = 0.7f;
            moon.shadows = LightShadows.None;

            // Land, sea, beach and roads.
            Box("Ground", new Vector3(0f, -0.05f, -1f), new Vector3(26f, 0.1f, 18f), Ground, false);
            Box("Beach", new Vector3(0f, -0.04f, 8.5f), new Vector3(26f, 0.1f, 1.6f), Sand, false);
            Box("Sea", new Vector3(0f, -0.12f, 16f), new Vector3(40f, 0.1f, 13.5f), Sea, true);
            Box("Main Road", new Vector3(0f, 0.01f, -3.6f), new Vector3(24f, 0.04f, 1.1f), Road, false);
            Box("Harbour Road", new Vector3(2.4f, 0.01f, 2f), new Vector3(1.0f, 0.04f, 10f), Road, false);

            var substation = new Vector3(0f, 0f, -6.2f);
            Box("Substation", substation + new Vector3(0f, 0.5f, 0f), new Vector3(2.2f, 1.0f, 1.6f), new Color(0.45f, 0.47f, 0.50f), false);
            Box("Transformer", substation + new Vector3(-0.5f, 1.25f, 0f), new Vector3(0.6f, 0.5f, 0.6f), new Color(0.30f, 0.32f, 0.36f), false);
            Box("Transformer", substation + new Vector3(0.5f, 1.25f, 0f), new Vector3(0.6f, 0.5f, 0.6f), new Color(0.30f, 0.32f, 0.36f), false);
            var hub = substation + new Vector3(0f, 1.8f, 0f);

            BuildBattery(new Vector3(-3.6f, 0f, -6.4f));
            BuildGenerator(new Vector3(3.6f, 0f, -6.4f));

            foreach (var d in content.Districts)
            {
                var visual = new DistrictVisual { Id = d.Id, Kind = d.Kind };
                _districts[d.Id] = visual;
                Vector3 centre;
                switch (d.Kind)
                {
                    case "market": centre = BuildMarket(visual); break;
                    case "clinic": centre = BuildClinic(visual); break;
                    default: centre = BuildHomes(visual); break;
                }
                visual.FeederLine = Line("Feeder " + d.Id, hub, centre + new Vector3(0f, 1.8f, 0f), LineDead);
                Pole(centre + new Vector3(0f, 0f, -1.6f));
                _anchors.Add(new CityAnchor { DistrictId = d.Id, WorldPosition = centre + new Vector3(0f, 2.6f, 0f) });
            }
            Frame();
        }

        private Vector3 BuildHomes(DistrictVisual v)
        {
            var centre = new Vector3(-6.5f, 0f, -0.2f);
            int n = 0;
            for (int row = 0; row < 2; row++)
            {
                for (int col = 0; col < 3; col++)
                {
                    var p = centre + new Vector3((col - 1) * 2.2f, 0f, (row - 0.5f) * 2.8f);
                    v.Walls.Add(Box("House", p + new Vector3(0f, 0.55f, 0f), new Vector3(1.4f, 1.1f, 1.3f), WallLit, false));
                    Box("Roof", p + new Vector3(0f, 1.3f, 0f), new Vector3(1.05f, 1.05f, 1.4f), Roof, false, Quaternion.Euler(0f, 0f, 45f));
                    v.Windows.Add(Box("Window", p + new Vector3(-0.35f, 0.6f, -0.66f), new Vector3(0.35f, 0.35f, 0.04f), WindowDark, true));
                    v.Windows.Add(Box("Window", p + new Vector3(0.35f, 0.6f, -0.66f), new Vector3(0.35f, 0.35f, 0.04f), WindowDark, true));
                    if (n++ % 2 == 0) v.Lamps.Add(StreetLamp(p + new Vector3(1.1f, 0f, -1.1f)));
                }
            }
            return centre;
        }

        private Vector3 BuildMarket(DistrictVisual v)
        {
            var centre = new Vector3(5.6f, 0f, 2.6f);
            int i = 0;
            for (int row = 0; row < 2; row++)
            {
                for (int col = 0; col < 2; col++)
                {
                    var p = centre + new Vector3((col - 0.5f) * 2.0f, 0f, (row - 0.5f) * 2.2f);
                    v.Walls.Add(Box("Stall", p + new Vector3(0f, 0.4f, 0f), new Vector3(1.4f, 0.8f, 1.0f), WallLit, false));
                    var colour = Awnings[i % Awnings.Length];
                    v.Awnings.Add(Box("Awning", p + new Vector3(0f, 1.05f, -0.25f), new Vector3(1.6f, 0.08f, 1.2f), colour, false, Quaternion.Euler(12f, 0f, 0f)));
                    v.AwningColors.Add(colour);
                    v.Windows.Add(Box("Stall Light", p + new Vector3(0f, 0.85f, -0.55f), new Vector3(0.9f, 0.12f, 0.05f), WindowDark, true));
                    i++;
                }
            }
            // String lights across the market square.
            for (int k = 0; k < 6; k++)
                v.Lamps.Add(Sphere("Bulb", centre + new Vector3(-2.5f + k, 1.9f, -1.8f), 0.18f, WindowDark));
            Box("Quay", centre + new Vector3(0f, 0.05f, 3.2f), new Vector3(5.5f, 0.2f, 1.2f), new Color(0.42f, 0.40f, 0.36f), false);
            return centre;
        }

        private Vector3 BuildClinic(DistrictVisual v)
        {
            var centre = new Vector3(-0.8f, 0f, 3.6f);
            v.Walls.Add(Box("Clinic", centre + new Vector3(0f, 0.85f, 0f), new Vector3(3.2f, 1.7f, 2.2f), new Color(0.92f, 0.93f, 0.95f), false));
            for (int k = 0; k < 4; k++)
                v.Windows.Add(Box("Window", centre + new Vector3(-1.1f + k * 0.73f, 0.95f, -1.11f), new Vector3(0.45f, 0.45f, 0.04f), WindowDark, true));
            v.Signs.Add(Box("Cross", centre + new Vector3(0f, 2.15f, 0f), new Vector3(0.9f, 0.28f, 0.28f), ClinicCrossDark, true));
            v.Signs.Add(Box("Cross", centre + new Vector3(0f, 2.15f, 0f), new Vector3(0.28f, 0.9f, 0.28f), ClinicCrossDark, true));
            v.Lamps.Add(StreetLamp(centre + new Vector3(2.0f, 0f, -1.4f)));
            return centre;
        }

        private void BuildBattery(Vector3 at)
        {
            Box("Battery Container", at + new Vector3(0f, 0.55f, 0f), new Vector3(2.2f, 1.1f, 1.1f), new Color(0.85f, 0.88f, 0.90f), false);
            Box("Battery Gauge Back", at + new Vector3(0f, 0.55f, -0.58f), new Vector3(1.8f, 0.3f, 0.04f), new Color(0.12f, 0.13f, 0.15f), true);
            _batteryBar = Box("Battery Gauge", at + new Vector3(0f, 0.55f, -0.61f), new Vector3(1.8f, 0.24f, 0.04f), BatteryBar, true).transform;
        }

        private void BuildGenerator(Vector3 at)
        {
            Box("Generator", at + new Vector3(0f, 0.5f, 0f), new Vector3(1.8f, 1.0f, 1.1f), new Color(0.20f, 0.45f, 0.30f), false);
            Cylinder("Exhaust", at + new Vector3(0.6f, 1.35f, 0.2f), new Vector3(0.18f, 0.35f, 0.18f), new Color(0.25f, 0.25f, 0.27f));
            _generatorLamp = Sphere("Run Lamp", at + new Vector3(-0.6f, 1.15f, -0.3f), 0.22f, GeneratorOff);
            Box("Fuel Gauge Back", at + new Vector3(0f, 0.5f, -0.58f), new Vector3(1.4f, 0.26f, 0.04f), new Color(0.12f, 0.13f, 0.15f), true);
            _fuelBar = Box("Fuel Gauge", at + new Vector3(0f, 0.5f, -0.61f), new Vector3(1.4f, 0.2f, 0.04f), FuelBar, true).transform;
        }

        /// <summary>Applies one snapshot to the scene.</summary>
        public void Render(SessionSnapshot snap)
        {
            if (snap == null || snap.Districts.Length == 0) return;
            bool cut = snap.Phase == Phase.Running;
            foreach (var d in snap.Districts)
            {
                if (!_districts.TryGetValue(d.Id, out var v)) continue;
                bool on = d.Powered;
                foreach (var r in v.Windows) Paint(r, on ? WindowLit : WindowDark, true);
                foreach (var r in v.Lamps) Paint(r, on ? WindowLit : WindowDark, true);
                foreach (var r in v.Walls) Paint(r, on ? (v.Kind == "clinic" ? new Color(0.92f, 0.93f, 0.95f) : WallLit) : WallDark, false);
                for (int i = 0; i < v.Awnings.Count; i++) Paint(v.Awnings[i], on ? v.AwningColors[i] : Shutter, false);
                foreach (var r in v.Signs) Paint(r, on ? ClinicCrossLit : ClinicCrossDark, true);
                if (v.FeederLine != null) Paint(v.FeederLine, on ? LineLive : LineDead, true);
            }

            SetGauge(_batteryBar, snap.BatteryCapacityWs <= 0 ? 0f : (float)snap.BatteryChargeWs / snap.BatteryCapacityWs, 1.8f);
            SetGauge(_fuelBar, snap.TankCapacityMl <= 0 ? 0f : (float)snap.FuelMl / snap.TankCapacityMl, 1.4f);
            bool generatorRunning = cut && snap.LiveCut != null && snap.LiveCut.PlanUsed.Plan == PlanKind.Diesel && snap.CurrentBackupW > 0;
            Paint(_generatorLamp, generatorRunning ? GeneratorOn : GeneratorOff, true);
            LightsSignature = snap.LightsSignature;
        }

        private void LateUpdate()
        {
            if (_framedFor.x != Screen.width || _framedFor.y != Screen.height) Frame();
        }

        /// <summary>Fits the town to the screen width and places it in the upper part of the screen, above the HUD sheet.</summary>
        private void Frame()
        {
            _framedFor = new Vector2Int(Screen.width, Screen.height);
            var rotation = Camera.transform.rotation;
            var inverse = Quaternion.Inverse(rotation);
            float halfW = 0f, halfH = 0f;
            foreach (var x in new[] { -11f, 11f })
                foreach (var z in new[] { -8f, 9.5f })
                    foreach (var y in new[] { 0f, 3f })
                    {
                        var local = inverse * new Vector3(x, y, z);
                        halfW = Mathf.Max(halfW, Mathf.Abs(local.x));
                        halfH = Mathf.Max(halfH, Mathf.Abs(local.y));
                    }
            float aspect = Screen.height > 0 ? (float)Screen.width / Screen.height : 0.5f;
            float size = Mathf.Max(halfH * 1.05f, halfW * 1.05f / Mathf.Max(0.1f, aspect));
            Camera.orthographicSize = size;
            // Shift the view so the town centre sits about 72% of the way up the screen.
            float shift = (0.72f - 0.5f) * 2f * size;
            var target = new Vector3(0f, 0f, 0.5f);
            Camera.transform.position = target - Camera.transform.forward * 60f - Camera.transform.up * shift;
        }

        private static void SetGauge(Transform bar, float fraction, float fullWidth)
        {
            if (bar == null) return;
            fraction = Mathf.Clamp01(fraction);
            var scale = bar.localScale;
            var position = bar.localPosition;
            // Keep the left edge fixed so the bar drains towards it.
            float left = position.x - scale.x * 0.5f;
            scale.x = Mathf.Max(0.001f, fullWidth * fraction);
            bar.localScale = scale;
            position.x = left + scale.x * 0.5f;
            bar.localPosition = position;
        }

        private void LoadMaterials()
        {
            _litTemplate = Resources.Load<Material>("Materials/CityLit");
            _unlitTemplate = Resources.Load<Material>("Materials/CityUnlit");
            if (_litTemplate == null) _litTemplate = FallbackMaterial(true);
            if (_unlitTemplate == null) _unlitTemplate = FallbackMaterial(false);
        }

        private static Material FallbackMaterial(bool lit)
        {
            // Only reached if the editor bootstrapper has not generated the Resources materials yet.
            bool urp = GraphicsSettings.currentRenderPipeline != null;
            string[] names = lit
                ? new[] { urp ? "Universal Render Pipeline/Simple Lit" : "Standard", "Universal Render Pipeline/Lit", "Standard" }
                : new[] { urp ? "Universal Render Pipeline/Unlit" : "Unlit/Color", "Unlit/Color", "Sprites/Default" };
            foreach (var name in names)
            {
                var shader = Shader.Find(name);
                if (shader != null) return new Material(shader);
            }
            Debug.LogWarning("[ILS] No shader found for city materials; run IdleLoadshedding > Set Up Project in the editor.");
            return new Material(Shader.Find("Hidden/InternalErrorShader"));
        }

        private void Paint(Renderer r, Color color, bool unlit)
        {
            if (r == null) return;
            var cache = unlit ? _unlitCache : _litCache;
            if (!cache.TryGetValue(color, out var material))
            {
                material = new Material(unlit ? _unlitTemplate : _litTemplate) { color = color };
                cache[color] = material;
            }
            if (r.sharedMaterial != material) r.sharedMaterial = material;
        }

        private Renderer Box(string name, Vector3 position, Vector3 scale, Color color, bool unlit, Quaternion? rotation = null) =>
            Primitive(PrimitiveType.Cube, name, position, scale, color, unlit, rotation);

        private Renderer Sphere(string name, Vector3 position, float diameter, Color color) =>
            Primitive(PrimitiveType.Sphere, name, position, Vector3.one * diameter, color, true, null);

        private Renderer Cylinder(string name, Vector3 position, Vector3 scale, Color color) =>
            Primitive(PrimitiveType.Cylinder, name, position, scale, color, false, null);

        private Renderer Primitive(PrimitiveType type, string name, Vector3 position, Vector3 scale, Color color, bool unlit, Quaternion? rotation)
        {
            var go = GameObject.CreatePrimitive(type);
            go.name = name;
            var collider = go.GetComponent<Collider>();
            if (collider != null) Destroy(collider);
            go.transform.SetParent(transform, false);
            go.transform.localPosition = position;
            go.transform.localRotation = rotation ?? Quaternion.identity;
            go.transform.localScale = scale;
            var renderer = go.GetComponent<Renderer>();
            renderer.shadowCastingMode = ShadowCastingMode.Off;
            renderer.receiveShadows = false;
            Paint(renderer, color, unlit);
            return renderer;
        }

        private Renderer StreetLamp(Vector3 at)
        {
            Cylinder("Lamp Post", at + new Vector3(0f, 0.7f, 0f), new Vector3(0.08f, 0.7f, 0.08f), new Color(0.25f, 0.26f, 0.28f));
            return Sphere("Lamp", at + new Vector3(0f, 1.45f, 0f), 0.25f, WindowDark);
        }

        private void Pole(Vector3 at)
        {
            Cylinder("Pole", at + new Vector3(0f, 0.9f, 0f), new Vector3(0.1f, 0.9f, 0.1f), new Color(0.35f, 0.27f, 0.18f));
        }

        private Renderer Line(string name, Vector3 from, Vector3 to, Color color)
        {
            var delta = to - from;
            var rotation = Quaternion.LookRotation(delta.normalized, Vector3.up);
            return Box(name, (from + to) * 0.5f, new Vector3(0.07f, 0.07f, delta.magnitude), color, true, rotation);
        }
    }
}
