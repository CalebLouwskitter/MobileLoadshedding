/**
 * Test script for Firebase Cloud Functions
 * Run with: node test-notifications.js
 * 
 * Prerequisites:
 * 1. Deploy functions: firebase deploy --only functions
 * 2. Install axios: npm install axios
 * 3. Update PROJECT_ID below with your Firebase project ID
 */

const axios = require('axios');

// ⚠️ UPDATE THIS WITH YOUR FIREBASE PROJECT ID
const PROJECT_ID = 'your-project-id';
const REGION = 'us-central1';
const BASE_URL = `https://${REGION}-${PROJECT_ID}.cloudfunctions.net`;

// Test FCM token (replace with real token from debug panel)
const TEST_TOKEN = 'your-test-fcm-token-here';

/**
 * Call a Cloud Function
 */
async function callFunction(functionName, data) {
  try {
    console.log(`\n🚀 Calling ${functionName}...`);
    console.log('📤 Payload:', JSON.stringify(data, null, 2));
    
    const response = await axios.post(
      `${BASE_URL}/${functionName}`,
      { data },
      {
        headers: {
          'Content-Type': 'application/json',
        },
      }
    );
    
    console.log('✅ Success:', response.data);
    return response.data;
  } catch (error) {
    console.error('❌ Error:', error.response?.data || error.message);
    throw error;
  }
}

/**
 * Test all notification functions
 */
async function runTests() {
  console.log('🧪 Firebase Cloud Functions Test Suite');
  console.log('=' .repeat(50));

  try {
    // Test 1: Generic notification
    await callFunction('triggerNotification', {
      title: 'Test Notification',
      body: 'This is a test from the test script',
      targetTopic: 'all_users',
    });

    // Test 2: Event Started
    await callFunction('sendEventStarted', {
      eventName: 'Double Energy Weekend',
      eventId: 'event_test_001',
      duration: '48 hours',
      rewards: '2x Energy + Bonus Items',
      targetTopic: 'all_users',
    });

    // Test 3: Event Ending Soon
    await callFunction('sendEventEndingSoon', {
      eventName: 'Double Energy Weekend',
      eventId: 'event_test_001',
      timeRemaining: '30 minutes',
      targetTopic: 'all_users',
    });

    // Test 4: Friend Request (requires real FCM token)
    if (TEST_TOKEN !== 'your-test-fcm-token-here') {
      await callFunction('sendFriendRequest', {
        fromPlayerName: 'TestPlayer123',
        fromPlayerId: 'player_test_001',
        recipientToken: TEST_TOKEN,
      });
    } else {
      console.log('\n⚠️  Skipping friend request test - update TEST_TOKEN');
    }

    // Test 5: Friend Accepted (requires real FCM token)
    if (TEST_TOKEN !== 'your-test-fcm-token-here') {
      await callFunction('sendFriendAccepted', {
        playerName: 'TestPlayer456',
        playerId: 'player_test_002',
        recipientToken: TEST_TOKEN,
      });
    } else {
      console.log('\n⚠️  Skipping friend accepted test - update TEST_TOKEN');
    }

    // Test 6: Maintenance Notification
    await callFunction('sendMaintenanceNotification', {
      maintenanceTime: '2025-11-20 02:00 UTC',
      duration: '2 hours',
      targetTopic: 'all_users',
    });

    // Test 7: System Announcement
    await callFunction('sendSystemAnnouncement', {
      title: 'New Features Available',
      message: 'Check out the new district upgrades and friend leaderboards!',
      announcementId: 'ann_test_001',
      targetTopic: 'all_users',
    });

    // Test 8: General Notification
    await callFunction('sendGeneralNotification', {
      title: 'Daily Login Bonus',
      body: 'You\'ve received 1000 energy for logging in today!',
      data: { bonusAmount: '1000' },
      targetTopic: 'all_users',
    });

    // Test 9: Leaderboard Update (requires real FCM token)
    if (TEST_TOKEN !== 'your-test-fcm-token-here') {
      await callFunction('sendLeaderboardUpdate', {
        newRank: 5,
        oldRank: 12,
        leaderboardName: 'Global Energy Leaders',
        recipientToken: TEST_TOKEN,
      });
    } else {
      console.log('\n⚠️  Skipping leaderboard test - update TEST_TOKEN');
    }

    // Test 10: Energy Milestone (requires real FCM token)
    if (TEST_TOKEN !== 'your-test-fcm-token-here') {
      await callFunction('sendEnergyMilestone', {
        milestone: '1M Energy Generated',
        energyAmount: '1,000,000 kWh',
        reward: 'Legendary Generator',
        recipientToken: TEST_TOKEN,
      });
    } else {
      console.log('\n⚠️  Skipping energy milestone test - update TEST_TOKEN');
    }

    // Test 11: Offline Earnings (requires real FCM token)
    if (TEST_TOKEN !== 'your-test-fcm-token-here') {
      await callFunction('sendOfflineEarningsReady', {
        energyEarned: '50,000 kWh',
        timeAway: '8 hours',
        recipientToken: TEST_TOKEN,
      });
    } else {
      console.log('\n⚠️  Skipping offline earnings test - update TEST_TOKEN');
    }

    console.log('\n✅ All tests completed successfully!');
  } catch (error) {
    console.error('\n❌ Test suite failed:', error.message);
    process.exit(1);
  }
}

/**
 * Test individual function
 */
async function testFunction(functionName, data) {
  await callFunction(functionName, data);
}

// Run tests
if (require.main === module) {
  // Check if PROJECT_ID is set
  if (PROJECT_ID === 'your-project-id') {
    console.error('❌ Error: Please update PROJECT_ID in the script');
    process.exit(1);
  }

  // Check for command line arguments
  const args = process.argv.slice(2);
  if (args.length >= 2) {
    // Test specific function: node test-notifications.js functionName '{"key":"value"}'
    const functionName = args[0];
    const data = JSON.parse(args[1]);
    testFunction(functionName, data).catch(console.error);
  } else {
    // Run all tests
    runTests().catch(console.error);
  }
}

module.exports = { callFunction, testFunction };
