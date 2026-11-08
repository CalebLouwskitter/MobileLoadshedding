# IdleLoadshedding: first playable slice (Unity 6.3 LTS)

A standalone rebuild of IdleLoadshedding. You run the grid of **Kwanele Bay**, a fictional town on the Eastern Cape coast. A scheduled cut is coming:

1. Preview the shortfall.
2. Choose how to cover it.
3. Watch the town's lights react.
4. Reinvest in one upgrade.
5. Save a standing policy that keeps running while you are away.

This folder is a separate Unity project. The legacy Kotlin host, the generated Unity export, the API, SQL and CircleCI elsewhere in the repository are untouched. `UNITYGAME-NEVER-MERGE` remains the reference branch for the old export.

| | |
| --- | --- |
| Editor | Unity **6000.3.28f1** (6.3 LTS), pinned in `ProjectSettings/ProjectVersion.txt` |
| Packages | URP 17.3.0, Test Framework 1.6.0, built-in modules only (`Packages/manifest.json`) |
| Android | IL2CPP, ARM64 only, portrait, dev application ID `com.prog7314.idleloadshedding.dev` |
| Legacy app | `com.prog7314.IdleLoadsheddingMobile`. The IDs differ, so both can be installed side by side |
| Network | None. The slice is local and account-free |

## Status: what was verified and what was not

This slice was written in a cloud container with **no Unity editor, Android SDK or phone**. Unity's and Google's download hosts are blocked there, and Unity licensing needs credentials. The pure C# rules, saves and session were run and tested. The Unity side was written conservatively and compile-checked where possible, but **it has never been opened in an editor or run on a device**.

| Check | Result |
| --- | --- |
| .NET SDK 8.0.133 (Ubuntu 24.04 archive) | Installed and used |
| `IdleLoadshedding.Core` built as netstandard2.1, C# 9, warnings as errors (Unity 6 API surface) | **Passes** |
| 77 NUnit tests from `Assets/Tests/EditMode` run under `dotnet test` | **77/77 pass** |
| Mutation checks: break the rotation; stop consuming time at a decision gate; publish before the save commits | Each makes tests fail, as it should |
| `Assets/Scripts/Game` and `Assets/Tests/PlayMode` compiled against UnityEngine 2021.3.33 reference assemblies (NuGet `UnityEngine.Modules`, scratch build only, not committed) | **Compiles**. This caught a real `SystemClock` name clash, now fixed |
| Workflow YAML parse; `bash -n` on `Tools/device-smoke.sh` | Pass |
| The 6000.3.28f1 patch exists with Android modules | Confirmed from the GameCI image `unityci/editor:ubuntu-6000.3.28f1-android-3.2.2` (published 2026-11-03). The changeset hash was not available, so Unity adds it to `ProjectVersion.txt` on first open |
| Unity editor, Hub or Pipeline | **Blocked**: none installed. `download.unity3d.com`, `unity.com`, `packages.unity.com` and `public-cdn.cloud.unity3d.com` returned 403 from the session proxy |
| Editor scripts (`ProjectBootstrapper`, `BuildScript`), URP asset creation, USS/TSS import | **Not compiled or run**: they need UnityEditor and URP assemblies |
| Unity Edit Mode and Play Mode test runs | **Not run** |
| ARM64 IL2CPP APK | **Not built**: Android SDK/NDK unavailable (`dl.google.com` returned 403) |
| Phone: 60-second airplane-mode boot, force-stop, background kill, performance | **Not run**: no ADB or device |

## Run the verified tests

```bash
dotnet test GameUnity/Tools/DotnetTests/Tests/IdleLoadshedding.Tests.csproj
```

The same test files run inside Unity's Test Runner (Edit Mode), and they read the shipping `town-content.json`.

## Remaining steps (exact)

1. Install Unity Hub, then editor **6000.3.28f1** with **Android Build Support** (including OpenJDK and the Android SDK & NDK tools).
2. Open `GameUnity/` in that editor. On first open, `ProjectBootstrapper` runs and logs `[ILS] Project setup complete`. It creates:
   - the URP pipeline and renderer in `Assets/Settings/`;
   - the two template materials in `Assets/Resources/Materials/`;
   - an empty `Assets/Scenes/Boot.unity`, added to build settings;
   - the Android player settings above.

   You can rerun it any time from **IdleLoadshedding > Set Up Project**. If the Package Manager offers a different URP or Test Framework patch for this editor, accept the editor-bundled version: both are core packages locked to the editor.
3. Press Play in `Boot.unity`. Look for these Console lines: `[ILS] boot rev=1 status=NewGame phase=prep ...`, then a `[ILS] commit ...` line after each action.
4. Commit what the editor generated: `ProjectSettings/`, `Packages/packages-lock.json`, `Assets/Settings/`, `Assets/Scenes/`, `Assets/Resources/Materials/`, and any new `.meta` files (the USS/TSS ones were left for Unity to create).
5. Run the Unity tests in batch mode from the repository root:

   ```bash
   Unity -batchmode -projectPath GameUnity -runTests -testPlatform EditMode -testResults GameUnity/TestResults/editmode.xml -logFile -
   ```

   ```bash
   Unity -batchmode -projectPath GameUnity -runTests -testPlatform PlayMode -testResults GameUnity/TestResults/playmode.xml -logFile -
   ```

6. Build the development APK:

   ```bash
   Unity -batchmode -quit -projectPath GameUnity -buildTarget Android -executeMethod IdleLoadshedding.EditorTools.BuildScript.BuildAndroidDevelopment -logFile -
   ```

   Output: `GameUnity/Builds/Android/IdleLoadshedding-dev.apk` (git-ignored).
7. On a physical phone with USB debugging enabled, run the lifecycle smoke test. It turns airplane mode on, times the first boot, waits while you play the first cut and buy an upgrade, then force-stops, background-kills and relaunches, comparing the logged state:

   ```bash
   GameUnity/Tools/device-smoke.sh
   ```

   Then check by hand:
   - screen lock and Home near a cut;
   - set the device clock back and forward;
   - ten minutes of play with five background cycles.

   Starting targets from the research brief: p90 cold first display within 3 s, playable within 5 s, p95 tap feedback within 100 ms, p95 frame time within 34 ms at 30 fps, and PSS within 350 MB. Profile one lower-tier and one midrange phone.

**Or use CI.** `.github/workflows/gameunity.yml` runs the .NET tests on every push that touches `GameUnity/`. Its manual run (*Actions > GameUnity > Run workflow*) also runs Unity's Edit/Play Mode tests through GameCI and uploads the APK artifact. Add the repository secrets `UNITY_LICENSE`, or `UNITY_EMAIL` and `UNITY_PASSWORD`, first. That lane has not been run yet.

## The slice

| District | Demand | Income while powered | When dark |
| --- | ---: | ---: | --- |
| Homes (F1 Ridge Road) | 40 kW | R120/h | Windows and street lamps go dark |
| Market (F2 Harbour Market) | 35 kW | R300/h | Awnings shutter and income stops |
| Clinic (F3, protected by default) | 15 kW | none | Red cross and windows go dark. Its service target is to stay on |

**Cut 1, "Stage 2 tonight" (17:00–19:00).** The grid supplies at most 60 kW for two one-hour blocks. Demand is 90 kW, so the forecast shows a **30 kW shortfall**. The table below is what the tests assert:

| Plan | Outcome over two blocks | Income | Cost |
| --- | --- | ---: | --- |
| Rotate feeders | Clinic on for both blocks; Homes in block 1, Market in block 2 | R420 | none |
| Discharge battery (30 kW / 30 kWh) | All on in block 1; empty exactly at 18:00; block 2 sheds by priority (default: Clinic, Homes) | R540 | 30 kWh, recharges at 10 kW |
| Run diesel (30 kW) | All on for both blocks | R840 | 18.0 L of the 20 L in the tank (R432 at R24/L) |

You start with R800. After cut 1 you can afford at least one upgrade, but never all three (a test enforces this for every plan). The options are:
- **Battery module**: +30 kWh, arrives charged. R1 200.
- **Generator tune-up**: 0.2 L/kWh. R900.
- **Fuel delivery**: +20 L. R480, repeatable.

**Cut 2, "Night market wind-down" (22:00).** The market drops to 10 kW in the second block. The plain battery now leaves the market dark for an hour, the battery module covers both blocks, and diesel depends on what is left in the tank. After cut 2, routine cuts arrive every 6 simulated hours. Your **standing policy** runs them while you are away. Without a policy, each one waits for you.

**Pacing.** One tick is one simulated minute. A cut you run plays at 10 ticks per second, so 2 hours takes 12 s. Skipping the campaign lead runs at 60 ticks per second. While idle or away, the town advances one simulated minute per real minute, capped at 4 hours per absence. All tuning lives in `Assets/Resources/Content/town-content.json`.

## Architecture

```
Unity frame / lifecycle ─► GameApp ─► SessionCoordinator (single writer) ─► SaveStore ─► save.json
                                          │  commands, fixed ticks, settlement
                                          ▼
                                   GridRules / Dispatcher / Forecast / OfflineSettlement   (pure C#)
                                          │
                                   SessionSnapshot (immutable) ─► CityView + HudView
```

**Simulation** (`Assets/Scripts/Simulation`, no UnityEngine reference):
- Power is in W, energy in W·s, fuel in mL and money in rand cents, all as `long`. There are no floats, clocks or randomness.
- Arrays follow the content's district order, and saves key them by stable ID.
- One dispatcher drives the forecast, the live cut and offline settlement, so the preview cannot disagree with what happens.
- Feeders are all-or-nothing and placed first-fit: protected first, then the plan's order (rotation shifts the unprotected feeders by block).
- Invariants are checked every cut tick: battery charge within bounds, fuel and fuel carry never negative, and served power never above supply.

**Commands** carry an ID. A repeat is reported as a duplicate and spends nothing. A rejected command changes nothing.

**Session** (`Assets/Scripts/Session`, no UnityEngine reference). `SessionCoordinator` is the only writer:
- **Commands:** it applies a command to a copy, commits the save, and only then swaps the copy in and publishes. So success is never shown before persistence.
- **Frames:** a frame's delta feeds a fixed-tick accumulator, so results do not depend on frame rate.
- **Checkpoints:** passive progress is saved on phase changes, every 10 s and on pause. If the process dies, it replays from the last checkpoint.

**Save slot** (`persistentDataPath/saves/slot-1/`):
- `save.json`: a versioned envelope with format, schema, ruleset and content versions, slot and city IDs, revision, save time, an FNV-1a checksum of the canonical payload, and the payload.
- Commit sequence: write `save.tmp`, flush, read it back and verify, move `save.json` to `save.bak`, then move `save.tmp` to `save.json`.
- On boot, the valid file with the highest revision wins: an interrupted commit is finished, and a damaged current file falls back to `save.bak`.
- Damaged or newer-schema files are first **copied to `recovery/` and never deleted**. A newer-schema save is never overwritten silently; the player chooses between the previous copy and a new town.

**Offline settlement.** Elapsed time is measured from a watermark that only moves forward:
- A clock rollback awards nothing.
- Time beyond the 4 h cap is consumed.
- Settlement stops at the next decision (a campaign cut, or a routine cut with no policy), and the rest of the interval is consumed.
- A cut the player started stays paused.
- The award, the watermark and the receipt change together in one commit. An unacknowledged receipt is merged on the next resume and keeps the balance.

**Presentation** (`Assets/Scripts/Game`):
- `GameApp` composes everything at runtime from the empty `Boot` scene.
- `CityView` builds a primitive low-poly town under a fixed orthographic isometric camera, with lit and dark colour swaps.
- `HudView` is UI Toolkit built in code, styled by `Resources/UI/Hud.uss`, with a runtime `PanelSettings` and theme.
- Every state shown in colour also has text or an icon: POWERED/DARK, ON/OFF pills, and filled or hollow bulb icons.
- The layout is safe-area aware and portrait only.
- Each button keeps one command ID for its lifetime, so a double tap is deduplicated.

## Design assumptions

- **Content format.** Authored data is JSON (`Resources/Content/town-content.json`) rather than ScriptableObjects. It can be edited without an editor, it diffs cleanly, and the .NET tests validate the exact file the game ships. The integer-only reader rejects fractional tuning values.
- **Feeders and fallback.** Feeders are all-or-nothing. When the battery or fuel runs out, feeders are shed in priority order. Rotation starts with the highest-priority unprotected feeder.
- **Fuel accounting.** Fuel is inventory. Burning it does not move cash; the results card reports its replacement value as operating cost. Battery recharge is free but limited to 10 kW.
- **Clinic.** The clinic earns nothing. Its value is the protected service target, and it does not affect income.
- **Idle rate and cap.** The idle rate (1 simulated minute per real minute) and the 4 h cap are tuning hypotheses held in content.
- **Engine features.** There is no Input System package and no camera pan or pinch: UI Toolkit handles touch and the camera is fixed. There are no realtime shadows, MSAA or HDR. 30 fps is the target.
- **Boot scene.** The `Boot` scene is empty, and the game composes itself at runtime, so no scene YAML had to be hand-written. Editor-only assets come from `ProjectBootstrapper`.
- **Town name.** "Kwanele Bay" and the feeder labels are fictional.

## Layout

```
GameUnity/
  Assets/Resources/Content/town-content.json   authored tuning data
  Assets/Resources/UI/                          Hud.uss, IdleRuntimeTheme.tss
  Assets/Scripts/Simulation/                    pure rules (asmdef, noEngineReferences)
  Assets/Scripts/Session/                       saves, coordinator, snapshot, formatting (pure)
  Assets/Scripts/Game/                          Unity views and lifecycle
  Assets/Scripts/Editor/                        project bootstrapper and build script
  Assets/Tests/EditMode/                        NUnit tests (also run by Tools/DotnetTests)
  Assets/Tests/PlayMode/                        boot and HUD interaction tests (Unity only)
  Tools/DotnetTests/                            netstandard2.1 core build and net8.0 test runner
  Tools/device-smoke.sh                         ADB lifecycle check (not yet run)
```

## Not in this milestone

Cloud backup is the next milestone after device recovery is proven. That covers optional sign-in, verified identity, revision checks and idempotent writes. Also deferred: legacy save import, ads and monetisation, leaderboards, and live outage data. The legacy API and its deployment are out of scope.
