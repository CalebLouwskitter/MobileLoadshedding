# Debugging Guide: Unity-Android Tap Tracking

## The Problem
Nothing is generating from tap requests - the Android UI is not responding to Unity button taps.

## What SHOULD Happen (Without Changing Unity Code)

### Unity Side (Already Working):
1. User taps energy button in Unity
2. `EnergyTapButton.OnTapButton()` is called
3. `IdleStatsManager.AddBurstEnergy(amount)` is called
4. Energy values are updated (`currentEnergy` and `totalEnergyGenerated`)
5. `SendStatsToAndroid()` is called **immediately**
6. Unity sends JSON via `AndroidJavaClass("com.prog7314.IdleLoadsheddingMobile.unity.UnityBridge").CallStatic("updateStats", json)`

### Android Side (What We Built):
1. `UnityBridge.updateStats(json)` receives the call from Unity
2. Delegates to `CoreUnityBridge.updateStats(json)`
3. Calls `Stats.onStatsUpdated(json)` via listener
4. `Stats` parses JSON and calls `TapTracker.analyzeStatsUpdate()`
5. `TapTracker` detects rapid energy increase and calls `onTapDetected()`
6. `Stats` notifies `GameStatsFragment.onEnergyTapped()`
7. UI updates with visual feedback

## How to Debug

### Step 1: Check if Unity is Sending Data

**In Android Studio Logcat, filter by:**
- `Stats` - Should see "=== STATS UPDATE RECEIVED ===" 
- `UnityBridge` - May see bridge calls

**When you tap the Unity button, you should see:**
```
D/Stats: === STATS UPDATE RECEIVED ===
D/Stats: JSON: {"totalEnergyGenerated":...,"currentEnergy":...}
D/Stats: Parsed snapshot: currentEnergy=XX.XX, totalEnergy=YY.YY
```

**If you DON'T see this:**
- Unity is not calling the Android bridge
- Check that Unity build has the correct package name
- Verify `google-services.json` and Unity Android settings
- Ensure the app is built with Unity's Android export

### Step 2: Check if TapTracker is Working

**Filter Logcat by: `TapTracker`**

**You should see:**
```
D/TapTracker: Stats update: energyIncrease=5.0, timeDiff=123ms
D/TapTracker: TAP DETECTED! Amount: 5.0, Total taps: 1
```

**If you see "Stats update" but NO "TAP DETECTED":**
- The detection thresholds might be wrong
- Check the `energyIncrease` value - is it positive?
- Check the `timeDiff` - is it under 1000ms?

**Current Detection Logic:**
- Any energy increase > 1.0
- That happens within 1000ms of previous update
- = Detected as a tap

### Step 3: Check if UI is Updating

**Filter Logcat by: `GameStatsFragment`**

**You should see:**
```
D/GameStatsFragment: === UI UPDATE ===
D/GameStatsFragment: Energy: XX.XX, EPS: Y.YY
D/GameStatsFragment: UI TextViews updated successfully
D/GameStatsFragment: === TAP EVENT === Amount: 5.0, Total: 1
D/GameStatsFragment: Tap UI updated
```

**If you DON'T see this:**
- `GameStatsFragment` might not be visible/attached
- Check if fragment is added to your Activity
- Verify `Stats.addObserver(this)` is called

### Step 4: Check if UI Elements Exist

**In your Activity/Fragment that shows `GameStatsFragment`:**
- Is the fragment actually displayed?
- Are the TextViews with IDs `tvTapCounter` and `tvLastTapAmount` in the layout?
- Check if `fragment_game_stats.xml` is being inflated correctly

## Common Issues and Fixes

### Issue 1: Unity Not Calling Android
**Symptom:** No logs in Logcat from `Stats` or `UnityBridge`

**Fix:**
1. Verify package name matches: `com.prog7314.IdleLoadsheddingMobile`
2. Rebuild Unity Android project
3. Ensure running on actual Android device (not editor)
4. Check Unity Player Settings > Android > Package Name

### Issue 2: TapTracker Not Detecting Taps
**Symptom:** See "Stats update" but no "TAP DETECTED"

**Current thresholds:**
```kotlin
private const val MIN_TAP_ENERGY = 1.0 // Energy must increase by at least 1.0
private const val RAPID_UPDATE_THRESHOLD_MS = 1000L // Within 1 second
```

**Temporary Debug Fix:**
Reduce thresholds in `TapTracker.kt`:
```kotlin
private const val MIN_TAP_ENERGY = 0.1 // Detect any increase
private const val RAPID_UPDATE_THRESHOLD_MS = 5000L // Allow 5 seconds
```

### Issue 3: UI Not Visible
**Symptom:** See tap detection but no visual changes

**Check:**
1. Is `GameStatsFragment` added to your Activity?
2. Is the fragment view actually visible on screen?
3. Are you looking at the right screen?
4. Check if TextViews are not null in logs

### Issue 4: Wrong Energy Values
**Symptom:** Energy in Unity doesn't match Android UI

**Check Unity's `IdleStatsManager`:**
- `tapBurstEnergy` value (should be 5.0 by default)
- Make sure `AddBurstEnergy()` is being called
- Check if Unity is actually adding energy

## Quick Test

### Manual Test Commands:

**1. In Unity (C# Debug.Log should work):**
Add temporary logging in `EnergyTapButton.OnTapButton()`:
```csharp
Debug.Log($"TAP! Adding energy: {energyToAdd}");
```

**2. In Android Logcat:**
Filter for your package: `com.prog7314.IdleLoadsheddingMobile`

**3. Tap the energy button multiple times fast**
You should see a flood of logs showing the entire chain

## Current Implementation Status

✅ **Implemented:**
- UnityBridge receives Unity calls
- Stats object processes updates
- TapTracker detects energy increases
- GameStatsFragment provides UI feedback
- Comprehensive logging added

❌ **Not Changed (As Requested):**
- Unity C# scripts (completely unchanged)
- Unity build settings
- Unity scene setup

## Next Steps If Still Not Working

1. **Share Logcat output** when tapping the button
2. **Verify Unity is calling Android** - add logs in Unity C#
3. **Check if GameStatsFragment is visible** - add background color to layout
4. **Simplify detection** - make TapTracker even more permissive
5. **Alternative approach** - Display raw stats updates without tap detection

## Alternative: Display Every Stats Update

If tap detection is too complex, we can simply update the UI on **every** stats update from Unity, which happens:
- Every 5 seconds (automatic)
- Immediately after taps (from `AddBurstEnergy`)
- Immediately after energy consumption

This would at least show that the connection works, even if tap counting isn't perfect.