const {onCall} = require('firebase-functions/v2/https');
const {getMessaging} = require('firebase-admin/messaging');
const admin = require('firebase-admin');

// Initialize Admin SDK once per cold start
admin.initializeApp();

/**
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
