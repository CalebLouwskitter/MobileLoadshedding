using System.IO;
using UnityEditor;
using UnityEditor.Build;
using UnityEditor.Build.Reporting;
using UnityEngine;

namespace IdleLoadshedding.EditorTools
{
    /// <summary>
    /// Batch entry points. Run from the repository root, for example:
    ///   Unity -batchmode -quit -projectPath GameUnity -buildTarget Android \
    ///     -executeMethod IdleLoadshedding.EditorTools.BuildScript.BuildAndroidDevelopment -logFile -
    /// </summary>
    public static class BuildScript
    {
        /// <summary>Relative to the Unity project folder (GameUnity/).</summary>
        public const string ApkPath = "Builds/Android/IdleLoadshedding-dev.apk";

        /// <summary>Generates the editor-owned assets and settings without building.</summary>
        public static void Setup()
        {
            ProjectBootstrapper.Setup();
        }

        /// <summary>ARM64 IL2CPP development APK with the dev application ID.</summary>
        public static void BuildAndroidDevelopment()
        {
            ProjectBootstrapper.Setup();

            if (EditorUserBuildSettings.activeBuildTarget != BuildTarget.Android)
            {
                Debug.Log("[ILS] Switching the active build target to Android (pass -buildTarget Android to skip this).");
                EditorUserBuildSettings.SwitchActiveBuildTarget(BuildTargetGroup.Android, BuildTarget.Android);
            }
            EditorUserBuildSettings.buildAppBundle = false;
            EditorUserBuildSettings.development = true;

            Directory.CreateDirectory(Path.GetDirectoryName(ApkPath));
            var options = new BuildPlayerOptions
            {
                scenes = new[] { ProjectBootstrapper.ScenePath },
                locationPathName = ApkPath,
                target = BuildTarget.Android,
                targetGroup = BuildTargetGroup.Android,
                options = BuildOptions.Development
            };

            var report = BuildPipeline.BuildPlayer(options);
            var summary = report.summary;
            Debug.Log("[ILS] Build " + summary.result + ": " + summary.outputPath + " (" + summary.totalSize + " bytes, " +
                      summary.totalErrors + " errors, " + summary.totalWarnings + " warnings, " + summary.totalTime + ")");

            if (summary.result != BuildResult.Succeeded)
            {
                if (Application.isBatchMode) EditorApplication.Exit(1);
                throw new BuildFailedException("Android build failed: " + summary.result);
            }
        }
    }
}
