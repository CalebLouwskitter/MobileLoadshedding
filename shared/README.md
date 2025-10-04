# Shared Gradle Scripts

This folder contains common Gradle scripts used by the Unity-generated Android project.

## Files

- **common.gradle** - Disables test tasks when building in Unity's install folder
- **keepUnitySymbols.gradle** - Keeps debug symbols in runtime binaries for stacktrace resolving

## Usage

These scripts are automatically applied in `unityLibrary/build.gradle`:
```gradle
apply from: '../shared/common.gradle'
apply from: '../shared/keepUnitySymbols.gradle'
```

⚠️ **Do not delete this folder** - it's required for the Unity Android build to work properly.
