const {onCall} = require('firebase-functions/v2/https');
const {getMessaging} = require('firebase-admin/messaging');
const admin = require('firebase-admin');

// Initialize Admin SDK once per cold start
admin.initializeApp();

/**
 * Helper function to generate notification ID
 */
function generateNotificationId() {
  return `notif_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
}

/**
 * Helper function to send notification
 */
async function sendNotification({topic, title, body, type, data = {}, imageUrl = null}) {
  const notificationId = generateNotificationId();
  const message = {
    topic,
    notification: {title, body},
    data: {
      notificationId,
      type,
      timestampUtc: new Date().toISOString(),
      ...data,
    },
    android: {
      priority: 'high',
      notification: {
        channelId: getChannelId(type),
        sound: 'default',
      },
    },
  };

  if (imageUrl) {
    message.notification.imageUrl = imageUrl;
  }

  await getMessaging().send(message);

  return {
    status: 'ok',
    notificationId,
    topic,
    deliveredAt: new Date().toISOString(),
  };
}

/**
 * Get notification channel ID based on type
 */
function getChannelId(type) {
  if (type.startsWith('event_')) return 'idle_loadshedding_events';
  if (type.startsWith('friend_')) return 'idle_loadshedding_friends';
  if (type === 'maintenance_required' || type === 'system_announcement') {
    return 'idle_loadshedding_system';
  }
  return 'idle_loadshedding_default';
}

/**
 * Generic trigger notification function (backward compatible)
 * Callable function used by the Android debug panel to prove
 * the "device ➜ Firebase ➜ notification" loop.
 *
 * Expected payload:
 * {
 *   title: string,
 *   body: string,
 *   targetTopic?: string,
 *   data?: { [key: string]: string }
 * }
 */
exports.triggerNotification = onCall({region: 'us-central1'}, async (request) => {
  const {title, body, targetTopic = 'all_users', data = {}} = request.data || {};

  if (!title || !body) {
    throw new Error('title and body are required');
  }

  await getMessaging().send({
    topic: targetTopic,
    notification: {title, body},
    data,
  });

  return {
    status: 'ok',
    topic: targetTopic,
    deliveredAt: new Date().toISOString(),
  };
});

// ============================================================
// EVENT NOTIFICATIONS
// ============================================================

/**
 * Send EVENT_STARTED notification
 * Triggered when a new event begins
 * 
 * Expected payload:
 * {
 *   eventName: string,
 *   eventId: string,
 *   duration?: string,
 *   rewards?: string,
 *   targetTopic?: string
 * }
 */
exports.sendEventStarted = onCall({region: 'us-central1'}, async (request) => {
  const {eventName, eventId, duration, rewards, targetTopic = 'all_users'} = request.data || {};

  if (!eventName || !eventId) {
    throw new Error('eventName and eventId are required');
  }

  const title = `🎉 ${eventName} Has Started!`;
  const body = duration ?
    `The event is live now! You have ${duration} to participate and earn rewards.` :
    `The event is live now! Join in and compete for rewards!`;

  return await sendNotification({
    topic: targetTopic,
    title,
    body,
    type: 'event_started',
    data: {
      eventId,
      eventName,
      ...(duration && {duration}),
      ...(rewards && {rewards}),
    },
  });
});

/**
 * Send EVENT_ENDING_SOON notification
 * Triggered when an event is about to end
 * 
 * Expected payload:
 * {
 *   eventName: string,
 *   eventId: string,
 *   timeRemaining: string,
 *   targetTopic?: string
 * }
 */
exports.sendEventEndingSoon = onCall({region: 'us-central1'}, async (request) => {
  const {eventName, eventId, timeRemaining, targetTopic = 'all_users'} = request.data || {};

  if (!eventName || !eventId || !timeRemaining) {
    throw new Error('eventName, eventId, and timeRemaining are required');
  }

  const title = `⏰ ${eventName} Ending Soon!`;
  const body = `Only ${timeRemaining} left! Make your final push for rewards!`;

  return await sendNotification({
    topic: targetTopic,
    title,
    body,
    type: 'event_ending_soon',
    data: {
      eventId,
      eventName,
      timeRemaining,
    },
  });
});

// ============================================================
// FRIEND NOTIFICATIONS
// ============================================================

/**
 * Send FRIEND_REQUEST notification
 * Triggered when a player receives a friend request
 * 
 * Expected payload:
 * {
 *   fromPlayerName: string,
 *   fromPlayerId: string,
 *   recipientToken: string
 * }
 */
exports.sendFriendRequest = onCall({region: 'us-central1'}, async (request) => {
  const {fromPlayerName, fromPlayerId, recipientToken} = request.data || {};

  if (!fromPlayerName || !fromPlayerId || !recipientToken) {
    throw new Error('fromPlayerName, fromPlayerId, and recipientToken are required');
  }

  const notificationId = generateNotificationId();
  const message = {
    token: recipientToken,
    notification: {
      title: '👋 New Friend Request',
      body: `${fromPlayerName} wants to be your friend!`,
    },
    data: {
      notificationId,
      type: 'friend_request',
      fromPlayerId,
      fromPlayerName,
      timestampUtc: new Date().toISOString(),
    },
    android: {
      priority: 'high',
      notification: {
        channelId: 'idle_loadshedding_friends',
        sound: 'default',
      },
    },
  };

  await getMessaging().send(message);

  return {
    status: 'ok',
    notificationId,
    deliveredAt: new Date().toISOString(),
  };
});

/**
 * Send FRIEND_ACCEPTED notification
 * Triggered when a friend request is accepted
 * 
 * Expected payload:
 * {
 *   playerName: string,
 *   playerId: string,
 *   recipientToken: string
 * }
 */
exports.sendFriendAccepted = onCall({region: 'us-central1'}, async (request) => {
  const {playerName, playerId, recipientToken} = request.data || {};

  if (!playerName || !playerId || !recipientToken) {
    throw new Error('playerName, playerId, and recipientToken are required');
  }

  const notificationId = generateNotificationId();
  const message = {
    token: recipientToken,
    notification: {
      title: '🎊 Friend Request Accepted!',
      body: `${playerName} accepted your friend request. You're now connected!`,
    },
    data: {
      notificationId,
      type: 'friend_accepted',
      playerId,
      playerName,
      timestampUtc: new Date().toISOString(),
    },
    android: {
      priority: 'high',
      notification: {
        channelId: 'idle_loadshedding_friends',
        sound: 'default',
      },
    },
  };

  await getMessaging().send(message);

  return {
    status: 'ok',
    notificationId,
    deliveredAt: new Date().toISOString(),
  };
});

// ============================================================
// SYSTEM NOTIFICATIONS
// ============================================================

/**
 * Send MAINTENANCE_REQUIRED notification
 * Triggered when system maintenance is scheduled
 * 
 * Expected payload:
 * {
 *   maintenanceTime: string,
 *   duration: string,
 *   targetTopic?: string
 * }
 */
exports.sendMaintenanceNotification = onCall({region: 'us-central1'}, async (request) => {
  const {maintenanceTime, duration, targetTopic = 'all_users'} = request.data || {};

  if (!maintenanceTime || !duration) {
    throw new Error('maintenanceTime and duration are required');
  }

  const title = '🔧 Scheduled Maintenance';
  const body = `The game will be offline for ${duration} starting ${maintenanceTime}. Plan accordingly!`;

  return await sendNotification({
    topic: targetTopic,
    title,
    body,
    type: 'maintenance_required',
    data: {
      maintenanceTime,
      duration,
    },
  });
});

/**
 * Send SYSTEM_ANNOUNCEMENT notification
 * Triggered for important system-wide announcements
 * 
 * Expected payload:
 * {
 *   title: string,
 *   message: string,
 *   announcementId?: string,
 *   targetTopic?: string
 * }
 */
exports.sendSystemAnnouncement = onCall({region: 'us-central1'}, async (request) => {
  const {title, message, announcementId, targetTopic = 'all_users'} = request.data || {};

  if (!title || !message) {
    throw new Error('title and message are required');
  }

  return await sendNotification({
    topic: targetTopic,
    title: `📢 ${title}`,
    body: message,
    type: 'system_announcement',
    data: {
      ...(announcementId && {announcementId}),
    },
  });
});

/**
 * Send GENERAL notification
 * Generic notification for various purposes
 * 
 * Expected payload:
 * {
 *   title: string,
 *   body: string,
 *   data?: object,
 *   targetTopic?: string
 * }
 */
exports.sendGeneralNotification = onCall({region: 'us-central1'}, async (request) => {
  const {title, body, data = {}, targetTopic = 'all_users'} = request.data || {};

  if (!title || !body) {
    throw new Error('title and body are required');
  }

  return await sendNotification({
    topic: targetTopic,
    title,
    body,
    type: 'general',
    data,
  });
});

// ============================================================
// GAME PROGRESS NOTIFICATIONS
// ============================================================

/**
 * Send LEADERBOARD_UPDATE notification
 * Triggered when player's ranking changes significantly
 * 
 * Expected payload:
 * {
 *   newRank: number,
 *   oldRank?: number,
 *   leaderboardName: string,
 *   recipientToken: string
 * }
 */
exports.sendLeaderboardUpdate = onCall({region: 'us-central1'}, async (request) => {
  const {newRank, oldRank, leaderboardName, recipientToken} = request.data || {};

  if (!newRank || !leaderboardName || !recipientToken) {
    throw new Error('newRank, leaderboardName, and recipientToken are required');
  }

  const emoji = newRank <= 3 ? '🏆' : '📈';
  const title = `${emoji} Leaderboard Update`;
  const body = oldRank ?
    `You moved from #${oldRank} to #${newRank} on ${leaderboardName}!` :
    `You're now ranked #${newRank} on ${leaderboardName}!`;

  const notificationId = generateNotificationId();
  const message = {
    token: recipientToken,
    notification: {title, body},
    data: {
      notificationId,
      type: 'leaderboard_update',
      newRank: newRank.toString(),
      leaderboardName,
      ...(oldRank && {oldRank: oldRank.toString()}),
      timestampUtc: new Date().toISOString(),
    },
    android: {
      priority: 'high',
      notification: {
        channelId: 'idle_loadshedding_default',
        sound: 'default',
      },
    },
  };

  await getMessaging().send(message);

  return {
    status: 'ok',
    notificationId,
    deliveredAt: new Date().toISOString(),
  };
});

/**
 * Send ENERGY_MILESTONE notification
 * Triggered when player reaches an energy milestone
 * 
 * Expected payload:
 * {
 *   milestone: string,
 *   energyAmount: string,
 *   reward?: string,
 *   recipientToken: string
 * }
 */
exports.sendEnergyMilestone = onCall({region: 'us-central1'}, async (request) => {
  const {milestone, energyAmount, reward, recipientToken} = request.data || {};

  if (!milestone || !energyAmount || !recipientToken) {
    throw new Error('milestone, energyAmount, and recipientToken are required');
  }

  const title = `⚡ Energy Milestone Reached!`;
  const body = reward ?
    `Congratulations on reaching ${energyAmount}! You've earned: ${reward}` :
    `Amazing! You've generated ${energyAmount} of energy!`;

  const notificationId = generateNotificationId();
  const message = {
    token: recipientToken,
    notification: {title, body},
    data: {
      notificationId,
      type: 'energy_milestone',
      milestone,
      energyAmount,
      ...(reward && {reward}),
      timestampUtc: new Date().toISOString(),
    },
    android: {
      priority: 'high',
      notification: {
        channelId: 'idle_loadshedding_default',
        sound: 'default',
      },
    },
  };

  await getMessaging().send(message);

  return {
    status: 'ok',
    notificationId,
    deliveredAt: new Date().toISOString(),
  };
});

/**
 * Send OFFLINE_EARNINGS_READY notification
 * Triggered when player has significant offline earnings to collect
 * 
 * Expected payload:
 * {
 *   energyEarned: string,
 *   timeAway: string,
 *   recipientToken: string
 * }
 */
exports.sendOfflineEarningsReady = onCall({region: 'us-central1'}, async (request) => {
  const {energyEarned, timeAway, recipientToken} = request.data || {};

  if (!energyEarned || !timeAway || !recipientToken) {
    throw new Error('energyEarned, timeAway, and recipientToken are required');
  }

  const title = `💰 Offline Earnings Ready!`;
  const body = `While you were away for ${timeAway}, you earned ${energyEarned}! Come collect your rewards!`;

  const notificationId = generateNotificationId();
  const message = {
    token: recipientToken,
    notification: {title, body},
    data: {
      notificationId,
      type: 'offline_earnings_ready',
      energyEarned,
      timeAway,
      timestampUtc: new Date().toISOString(),
    },
    android: {
      priority: 'high',
      notification: {
        channelId: 'idle_loadshedding_default',
        sound: 'default',
      },
    },
  };

  await getMessaging().send(message);

  return {
    status: 'ok',
    notificationId,
    deliveredAt: new Date().toISOString(),
  };
});
