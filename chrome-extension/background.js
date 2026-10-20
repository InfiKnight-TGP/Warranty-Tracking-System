importScripts('api.js');

const alarmName = 'warrify-notifications';

async function pollNotifications() {
  const { token, authExpired } = await chrome.storage.local.get(['token','authExpired']);
  if (!token) { await chrome.action.setBadgeText({text:authExpired ? '!' : ''}); return; }
  try {
    const items = await WarrifyApi.request('/api/notifications');
    const unread = items.filter(item => !item.read);
    await chrome.action.setBadgeBackgroundColor({color:'#167d78'});
    await chrome.action.setBadgeText({text:unread.length ? String(Math.min(unread.length,99)) : ''});
    chrome.runtime.sendMessage({ type: 'notification-count', count: unread.length }).catch(() => {});
    const { shownNotificationIds = [] } = await chrome.storage.local.get('shownNotificationIds');
    const shown = new Set(shownNotificationIds.map(String));
    for (const item of unread) {
      const id = String(item.id);
      if (shown.has(id)) continue;
      await chrome.notifications.create(`warrify-${id}`, {
        type:'basic', iconUrl:'icons/icon128.png', title:'Warrify notification',
        message:String(item.message).slice(0,240), priority:1
      });
      shown.add(id);
    }
    await chrome.storage.local.set({shownNotificationIds:[...shown].slice(-500)});
  } catch (error) {
    if (/sign-in expired|invalid|revoked/i.test(error.message)) {
      await WarrifyApi.clearToken();
      await chrome.action.setBadgeText({text:'!'});
    }
  }
}

async function ensureNotificationAlarm() {
  const alarm = await chrome.alarms.get(alarmName);
  if (!alarm) chrome.alarms.create(alarmName, { periodInMinutes: 1 });
}

chrome.runtime.onInstalled.addListener(() => {
  chrome.alarms.create(alarmName, { periodInMinutes: 1 });
  pollNotifications();
});
chrome.runtime.onStartup.addListener(() => {
  ensureNotificationAlarm();
  pollNotifications();
});
chrome.alarms.onAlarm.addListener(alarm => { if (alarm.name === alarmName) pollNotifications(); });
chrome.runtime.onMessage.addListener(message => {
  if (message?.type === 'auth-expired') chrome.action.setBadgeText({text:'!'});
  if (message?.type === 'poll-notifications') pollNotifications();
});

chrome.notifications.onClicked.addListener(async notificationId => {
  const base = await WarrifyApi.baseUrl();
  await chrome.tabs.create({ url: `${base}/notifications.html` });
  chrome.notifications.clear(notificationId);
});
