# Unity-Android UI Integration Setup Guide

## Overview
This solution establishes communication between Unity and the Android UI to track button taps and energy generation without modifying Unity scripts. The system uses intelligent monitoring to detect tap events by analyzing energy changes.

## How It Works

### 1. Existing Unity Flow (Unchanged)
- Unity `EnergyTapButton` calls `IdleStatsManager.AddBurstEnergy()` when tapped
- `IdleStatsManager` sends stats updates to Android via `UnityBridge.updateStats()`
- Android receives JSON stats updates every 5 seconds or immediately after energy changes

### 2. New Android-Side Tap Detection
- **TapTracker**: Analyzes energy changes to detect tap events
- **Enhanced Stats Class**: Processes stats updates and detects rapid energy increases
- **GameStatsFragment**: Provides visual feedback for taps and energy changes
- **Enhanced UI**: Shows tap counts, last tap amounts, and visual animations

### 3. Smart Tap Detection Algorithm
The `TapTracker` class analyzes incoming stats updates to detect taps by:
1. Calculating expected passive energy generation based on `energyPerSecond`
2. Comparing actual energy increase with expected passive increase
3. Identifying "unexpected" energy increases that indicate manual taps
4. Distinguishing between regular taps (3+ energy) and energy bursts (10+ energy)

## Key Components Added

### 1. Enhanced UnityBridge (`core/UnityBridge.kt`)
- Added `notifyEnergyTap(amount)` and `notifyEnergyBurst(amount)` methods
- Extended Listener interface with tap event callbacks
- Maintained backward compatibility with existing Unity calls

### 2. TapTracker (`utils/TapTracker.kt`)
- Intelligent tap detection based on energy change analysis
- Configurable thresholds for tap and burst detection
- Time-window based analysis to prevent false positives
- Event listeners for UI feedback

### 3. Enhanced Stats Object (`unity/Stats.kt`)
- Integrated TapTracker for automatic tap detection
- Added tap counting and tracking functionality
- Provides observer pattern for UI updates
- Maintains existing API compatibility

### 4. Enhanced GameStatsFragment (`GameUI/GameStatsFragment.kt`)
- Real-time tap counter display
- Last tap/burst amount display
- Visual feedback animations (flash, scale effects)
- Responsive UI updates

### 5. Updated Layout (`layout/fragment_game_stats.xml`)
- Added "Tap Count" display
- Added "Last Action" display showing recent tap amounts
- Maintained existing stats display

### 6. GameEventTracker (`core/GameEventTracker.kt`)
- Future-ready system for direct Unity integration
- Can be called from Unity if scripts are modified later
- Provides centralized game event tracking

## Current Behavior

### When Unity Button is Tapped:
1. Unity increases energy and sends stats update to Android
2. TapTracker analyzes the energy increase
3. If increase exceeds passive generation + threshold, it's detected as a tap
4. Android UI immediately shows:
   - Updated energy values
   - Incremented tap counter
   - Last tap amount
   - Visual feedback animation

### Visual Feedback:
- **Tap Detection**: Current energy display flashes (alpha animation)
- **Energy Burst**: Current energy display scales up/down for emphasis
- **Real-time Updates**: All stats update immediately when Unity sends data

## Configuration

### TapTracker Settings (in `TapTracker.kt`):
```kotlin
private const val TAP_DETECTION_THRESHOLD = 3.0 // Minimum energy for tap detection
private const val BURST_DETECTION_THRESHOLD = 10.0 // Minimum energy for burst detection
private const val TIME_WINDOW_MS = 2000L // Analysis time window
```

### IdleStatsManager Settings (Unity side):
- `statsDispatchInterval = 5f` // Regular update interval
- `tapBurstEnergy = 5.0f` // Energy per tap (should match Android threshold)

## Future Enhancements (No Unity Changes Required)

### Option 1: More Sophisticated Detection
- Analyze tap patterns and timing
- Detect different types of interactions
- Machine learning-based event classification

### Option 2: Enhanced Visual Feedback
- Particle effects for taps
- Sound feedback
- Haptic feedback for mobile devices

### Option 3: Analytics Integration
- Track tap frequency and patterns
- User engagement metrics
- Performance analytics

## If Unity Scripts Can Be Modified Later

If you later decide to modify Unity scripts, you can add direct calls:

```csharp
// In EnergyTapButton.OnTapButton() method:
private void OnTapButton()
{
    // ... existing code ...
    
#if UNITY_ANDROID && !UNITY_EDITOR
    try
    {
        using (var tracker = new AndroidJavaClass("com.prog7314.IdleLoadsheddingMobile.core.GameEventTracker"))
        {
            tracker.CallStatic("notifyButtonTap", "energy_tap", energyToAdd.ToString());
        }
    }
    catch (Exception e)
    {
        Debug.LogWarning($"Failed to notify Android of tap: {e}");
    }
#endif
}
```

## Testing the Integration

1. **Build and deploy** the Android app with these changes
2. **Tap the energy button** in Unity
3. **Observe the Android UI** for:
   - Immediate energy value updates
   - Tap counter incrementing
   - "Last tap" amount display
   - Visual animations (flash/scale effects)
4. **Check logs** for tap detection messages:
   - Look for "TapTracker" and "Stats" log entries
   - Verify energy analysis calculations

## Troubleshooting

### If Taps Aren't Detected:
1. Check that Unity is sending stats updates (`IdleStatsManager` logs)
2. Verify TapTracker thresholds match Unity's tap energy amounts
3. Ensure Android UI is properly connected to Stats observer
4. Check logs for energy analysis calculations

### If UI Doesn't Update:
1. Verify GameStatsFragment is registered as Stats observer
2. Check that layout includes new TextViews (tvTapCounter, tvLastTapAmount)
3. Ensure views are properly initialized in onViewCreated

This solution provides immediate tap tracking and responsive UI updates while maintaining full compatibility with existing Unity code.