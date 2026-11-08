using System.IO;
using UnityEditor;
using UnityEditor.Build;
using UnityEditor.SceneManagement;
using UnityEngine;
using UnityEngine.Rendering;
using UnityEngine.Rendering.Universal;

namespace IdleLoadshedding.EditorTools
{
    /// <summary>
    /// Creates the editor-generated assets the repository cannot author by hand: the URP pipeline
    /// and renderer, the two template materials the city loads from Resources, the empty Boot
    /// scene (the game composes itself at runtime), and the Android player settings. Idempotent.
    /// Runs automatically the first time the project opens, from the menu, and from BuildScript.
    /// </summary>
    [InitializeOnLoad]
    public static class ProjectBootstrapper
    {
        public const string ApplicationId = "com.prog7314.idleloadshedding.dev";
        public const string ProductName = "IdleLoadshedding Dev";
        public const string CompanyName = "PROG7314";
        public const string BundleVersion = "0.1.0";
        public const int BundleVersionCode = 1;

        public const string ScenePath = "Assets/Scenes/Boot.unity";
        public const string RendererPath = "Assets/Settings/IdleLoadshedding_Renderer.asset";
        public const string PipelinePath = "Assets/Settings/IdleLoadshedding_URP.asset";
        public const string LitMaterialPath = "Assets/Resources/Materials/CityLit.mat";
        public const string UnlitMaterialPath = "Assets/Resources/Materials/CityUnlit.mat";

        static ProjectBootstrapper()
        {
            // Batch callers (CI, BuildScript) run Setup explicitly.
            if (Application.isBatchMode) return;
            EditorApplication.delayCall += () =>
            {
                if (NeedsSetup()) Setup();
            };
        }

        public static bool NeedsSetup() =>
            !File.Exists(ScenePath)
            || AssetDatabase.LoadAssetAtPath<Material>(LitMaterialPath) == null
            || AssetDatabase.LoadAssetAtPath<Material>(UnlitMaterialPath) == null
            || GraphicsSettings.defaultRenderPipeline == null
            || PlayerSettings.GetApplicationIdentifier(NamedBuildTarget.Android) != ApplicationId;

        [MenuItem("IdleLoadshedding/Set Up Project")]
        public static void Setup()
        {
            EnsureFolder("Assets", "Settings");
            EnsureFolder("Assets", "Scenes");
            EnsureFolder("Assets/Resources", "Materials");

            EnsurePipeline();
            EnsureMaterial(LitMaterialPath, "Universal Render Pipeline/Simple Lit");
            EnsureMaterial(UnlitMaterialPath, "Universal Render Pipeline/Unlit");
            EnsureBootScene();
            ApplyPlayerSettings();

            AssetDatabase.SaveAssets();
            AssetDatabase.Refresh();
            Debug.Log("[ILS] Project setup complete: URP pipeline, city materials, Boot scene and Android player settings.");
        }

        private static void EnsureFolder(string parent, string name)
        {
            if (!AssetDatabase.IsValidFolder(parent + "/" + name)) AssetDatabase.CreateFolder(parent, name);
        }

        private static void EnsurePipeline()
        {
            var pipeline = AssetDatabase.LoadAssetAtPath<UniversalRenderPipelineAsset>(PipelinePath);
            if (pipeline == null)
            {
                var renderer = AssetDatabase.LoadAssetAtPath<UniversalRendererData>(RendererPath);
                if (renderer == null)
                {
                    renderer = ScriptableObject.CreateInstance<UniversalRendererData>();
                    AssetDatabase.CreateAsset(renderer, RendererPath);
                }
                pipeline = UniversalRenderPipelineAsset.Create(renderer);
                // Mobile greybox: no MSAA, no HDR, no realtime shadows. Revisit after profiling on a phone.
                pipeline.msaaSampleCount = 1;
                pipeline.supportsHDR = false;
                pipeline.shadowDistance = 0f;
                AssetDatabase.CreateAsset(pipeline, PipelinePath);
            }

            if (GraphicsSettings.defaultRenderPipeline != pipeline) GraphicsSettings.defaultRenderPipeline = pipeline;

            // Quality levels with their own pipeline override must use the same asset.
            int current = QualitySettings.GetQualityLevel();
            for (int i = 0; i < QualitySettings.names.Length; i++)
            {
                QualitySettings.SetQualityLevel(i, false);
                if (QualitySettings.renderPipeline != null && QualitySettings.renderPipeline != pipeline)
                    QualitySettings.renderPipeline = pipeline;
            }
            QualitySettings.SetQualityLevel(current, false);
        }

        private static void EnsureMaterial(string path, string shaderName)
        {
            if (AssetDatabase.LoadAssetAtPath<Material>(path) != null) return;
            var shader = Shader.Find(shaderName);
            if (shader == null)
            {
                Debug.LogError("[ILS] Shader '" + shaderName + "' not found. Is the URP package installed?");
                return;
            }
            var material = new Material(shader) { color = Color.white, enableInstancing = true };
            AssetDatabase.CreateAsset(material, path);
        }

        private static void EnsureBootScene()
        {
            if (!File.Exists(ScenePath))
            {
                // Additive, so an open scene with unsaved work is left alone.
                var scene = EditorSceneManager.NewScene(NewSceneSetup.EmptyScene, NewSceneMode.Additive);
                EditorSceneManager.SaveScene(scene, ScenePath);
                EditorSceneManager.CloseScene(scene, true);
            }

            var scenes = EditorBuildSettings.scenes;
            if (scenes.Length != 1 || scenes[0].path != ScenePath || !scenes[0].enabled)
                EditorBuildSettings.scenes = new[] { new EditorBuildSettingsScene(ScenePath, true) };
        }

        private static void ApplyPlayerSettings()
        {
            var android = NamedBuildTarget.Android;
            PlayerSettings.companyName = CompanyName;
            PlayerSettings.productName = ProductName;
            PlayerSettings.bundleVersion = BundleVersion;
            PlayerSettings.SetApplicationIdentifier(android, ApplicationId);
            PlayerSettings.Android.bundleVersionCode = BundleVersionCode;
            PlayerSettings.SetScriptingBackend(android, ScriptingImplementation.IL2CPP);
            PlayerSettings.Android.targetArchitectures = AndroidArchitecture.ARM64;
            PlayerSettings.SetApiCompatibilityLevel(android, ApiCompatibilityLevel.NET_Standard);
            PlayerSettings.defaultInterfaceOrientation = UIOrientation.Portrait;
            PlayerSettings.runInBackground = false;
        }
    }
}
