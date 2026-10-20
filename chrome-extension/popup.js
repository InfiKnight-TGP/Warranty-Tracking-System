const root = document.querySelector('#root');
const DEMO_EMAIL = 'user@vault.com';
const DEMO_PASSWORD = 'user123';
const escapeHtml = value => String(value ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));

function localDate() {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth()+1).padStart(2,'0')}-${String(now.getDate()).padStart(2,'0')}`;
}

function applyTheme(theme) {
  document.documentElement.dataset.theme = theme;
  document.querySelectorAll('.brand img.wordmark').forEach(image => {
    image.src = theme === 'dark' ? 'icons/warrify-logo-dark.svg' : 'icons/warrify-logo.svg';
  });
  const button = document.querySelector('#theme-toggle');
  if (button) {
    button.textContent = theme === 'dark' ? '☀' : '☾';
    button.title = theme === 'dark' ? 'Switch to light theme' : 'Switch to dark theme';
  }
}

async function setupTheme() {
  const { theme = 'light' } = await chrome.storage.local.get('theme');
  applyTheme(theme);
}

function showMessage(text, kind = '') {
  const box = document.querySelector('#message');
  if (box) { box.textContent = text; box.className = `notice ${kind}`; box.hidden = !text; }
}

async function render() {
  await setupTheme();
  const { token, name, role } = await chrome.storage.local.get(['token','name','role']);
  if (!token) return renderLogin();
  if (role !== 'USER') {
    await chrome.storage.local.remove(['token','name','role']);
    return renderLogin('Only USER accounts can use the Warrify extension.');
  }

  const initial = (name || 'U').trim().charAt(0).toUpperCase();
  root.innerHTML = `<section class="shell">
    <header class="top"><div class="brand"><img class="wordmark" src="icons/warrify-logo.svg" alt="Warrify"></div>
      <div class="top-actions"><div class="user-pill" title="${escapeHtml(name)}"><span class="avatar">${escapeHtml(initial)}</span><span>${escapeHtml(name)}</span></div><button type="button" class="icon-btn" id="theme-toggle" aria-label="Toggle theme"></button></div>
    </header>
    <nav class="tabs" aria-label="Extension sections">
      <button class="tab active" data-tab="add">Add purchase</button>
      <button class="tab" data-tab="expiring">Expiring <span class="tab-badge" id="expiring-count" hidden>0</span></button>
      <button class="tab" data-tab="notifications">Alerts <span class="tab-badge" id="notification-count" hidden>0</span></button>
    </nav>
    <div id="message" class="notice" hidden></div>
    <section id="panel-add" class="panel active"></section>
    <section id="panel-expiring" class="panel"></section>
    <section id="panel-notifications" class="panel"></section>
    <footer class="footer"><a id="open-app" href="#">Open web app</a><button id="logout" class="button secondary small">Log out</button></footer>
  </section>`;
  applyTheme(document.documentElement.dataset.theme || 'light');

  const base = await WarrifyApi.baseUrl();
  const open = document.querySelector('#open-app');
  open.href = base;
  open.target = '_blank';
  document.querySelectorAll('.tab').forEach(button => button.addEventListener('click', () => selectTab(button.dataset.tab)));
  document.querySelector('#logout').addEventListener('click', logout);
  document.querySelector('#theme-toggle').addEventListener('click', toggleTheme);
  chrome.runtime.sendMessage({ type: 'poll-notifications' }).catch(() => {});
  await renderAddForm();
  refreshTabBadges();
}

async function toggleTheme() {
  const next = document.documentElement.dataset.theme === 'dark' ? 'light' : 'dark';
  await chrome.storage.local.set({ theme: next });
  applyTheme(next);
}

async function renderLogin(message = '') {
  root.innerHTML = `<section class="login"><header class="top"><div class="brand"><img class="wordmark" src="icons/warrify-logo.svg" alt="Warrify"></div><button type="button" class="icon-btn" id="theme-toggle" aria-label="Toggle theme"></button></header>
    <div class="login-card"><h1>Sign in</h1><p>Connect with your Warrify USER account.</p><div id="message" class="notice" ${message ? '' : 'hidden'}>${escapeHtml(message)}</div>
      <form id="login-form"><div class="field"><label for="email">Email</label><input id="email" name="email" type="email" autocomplete="username" required></div>
        <div class="field"><div class="field-header"><label for="password">Password</label><button type="button" class="demo-fill" id="demo-fill">Fill demo account</button></div><input id="password" name="password" type="password" autocomplete="current-password" required></div>
        <button class="button full" type="submit">Sign in</button></form>
      <p class="muted" style="margin:10px 0 0">Back end must be running on localhost:8080.</p>
      <div class="footer"><a id="options" href="#">Server settings</a></div>
    </div></section>`;
  applyTheme(document.documentElement.dataset.theme || 'light');
  document.querySelector('#theme-toggle').addEventListener('click', toggleTheme);
  document.querySelector('#options').href = chrome.runtime.getURL('options.html');
  document.querySelector('#demo-fill').addEventListener('click', () => {
    document.querySelector('#email').value = DEMO_EMAIL;
    document.querySelector('#password').value = DEMO_PASSWORD;
    showMessage('Demo USER account filled in.', 'success');
  });
  document.querySelector('#login-form').addEventListener('submit', login);
}

async function login(event) {
  event.preventDefault();
  showMessage('');
  const form = event.currentTarget;
  const button = form.querySelector('button[type="submit"]');
  button.disabled = true;
  button.textContent = 'Signing in...';
  const data = new FormData(form);
  try {
    const result = await WarrifyApi.request('/api/auth/token', {
      method: 'POST', auth: false,
      body: JSON.stringify({ email: data.get('email'), password: data.get('password') })
    });
    if (result.role !== 'USER') {
      await WarrifyApi.request('/api/auth/token', {
        method: 'DELETE', auth: false, headers: { Authorization: `Bearer ${result.token}` }
      }).catch(() => {});
      showMessage('Only USER accounts can use the Warrify extension.');
      return;
    }
    await chrome.storage.local.set({ token: result.token, name: result.name, role: result.role, authExpired: false });
    chrome.action.setBadgeText({ text: '' });
    chrome.runtime.sendMessage({ type: 'poll-notifications' }).catch(() => {});
    await render();
  } catch (error) {
    showMessage(error.message);
  } finally {
    if (button.isConnected) { button.disabled = false; button.textContent = 'Sign in'; }
  }
}

function selectTab(tab) {
  document.querySelectorAll('.tab').forEach(button => button.classList.toggle('active', button.dataset.tab === tab));
  document.querySelectorAll('.panel').forEach(panel => panel.classList.toggle('active', panel.id === `panel-${tab}`));
  showMessage('');
  if (tab === 'expiring') loadExpiring();
  if (tab === 'notifications') loadNotifications();
}

async function activeTab() {
  const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });
  return tab;
}

function pageDetails(tab) {
  let storeName = '';
  try {
    const host = new URL(tab?.url || '').hostname.replace(/^www\./i, '');
    const domain = host.split('.').slice(0, -1).join('.').replace(/\.(com|co|in|uk|au|ca)$/i, '');
    const known = { amazon: 'Amazon', flipkart: 'Flipkart', walmart: 'Walmart', target: 'Target', bestbuy: 'Best Buy', ebay: 'eBay', etsy: 'Etsy' };
    const key = domain.split('.').pop()?.toLowerCase();
    storeName = known[key] || (key ? key.charAt(0).toUpperCase() + key.slice(1) : '');
  } catch (_) { }
  const title = (tab?.title || '').replace(/\s*[|–—-]\s*(Amazon(?:\.[a-z.]+)?|Flipkart|Walmart|Target|Best Buy|eBay|Etsy).*$/i, '').replace(/^(Amazon(?:\.[a-z.]+)?|Flipkart|Walmart|Target|Best Buy|eBay|Etsy)\s*:\s*/i, '').trim();
  return { productName: title.slice(0, 160), storeName };
}

async function renderAddForm() {
  const panel = document.querySelector('#panel-add');
  const tab = await activeTab();
  const initial = pageDetails(tab);
  panel.innerHTML = `<form id="purchase-form" class="form-card" novalidate>
    <div class="form-grid">
      <div class="field full"><div class="field-header"><label for="productName">Product name *</label><button type="button" class="autofill-btn" id="autofill-page">Use page details</button></div><input id="productName" name="productName" required maxlength="160"><div class="error" data-error="productName"></div></div>
      ${[['brand','Brand','text'],['category','Category','text'],['serialNumber','Serial number','text'],['storeName','Store','text'],['purchaseDate','Purchase date','date'],['price','Price (INR)','number'],['warrantyMonths','Warranty (months)','number'],['supportContact','Support contact','text']].map(([key,label,type]) => `<div class="field"><label for="${key}">${label}${['purchaseDate','price','warrantyMonths'].includes(key) ? ' *' : ''}</label><input id="${key}" name="${key}" type="${type}" ${type==='number'?(key==='price'?'min="0" step="0.01"':'min="1" max="120"'):''} ${['purchaseDate','price','warrantyMonths'].includes(key)?'required':''}><div class="error" data-error="${key}"></div></div>`).join('')}
    </div>
    <label class="check"><input type="checkbox" name="screenshot"> Attach screenshot of this page as bill</label>
    <button class="button full" id="save-btn" type="submit">Save purchase to vault</button>
  </form>`;
  const form = panel.querySelector('#purchase-form');
  form.elements.productName.value = initial.productName;
  form.elements.storeName.value = initial.storeName;
  form.elements.purchaseDate.value = localDate();
  form.elements.warrantyMonths.value = '12';
  panel.querySelector('#autofill-page').addEventListener('click', async () => {
    const fresh = pageDetails(await activeTab());
    if (fresh.productName) form.elements.productName.value = fresh.productName;
    if (fresh.storeName) form.elements.storeName.value = fresh.storeName;
  });
  form.addEventListener('submit', savePurchase);
}

async function savePurchase(event) {
  event.preventDefault();
  showMessage('');
  const form = event.currentTarget;
  form.querySelectorAll('.error').forEach(el => el.textContent = '');
  form.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
  const required = [
    ['productName', 'Product name is required.'],
    ['purchaseDate', 'Purchase date is required.'],
    ['price', 'Price is required.'],
    ['warrantyMonths', 'Warranty period is required.']
  ];
  let missingRequired = false;
  for (const [field, message] of required) {
    if (!form.elements[field].value.trim()) {
      form.querySelector(`[data-error="${field}"]`).textContent = message;
      form.elements[field].classList.add('is-invalid');
      missingRequired = true;
    }
  }
  if (missingRequired) {
    showMessage('Please fix the highlighted fields.');
    return;
  }
  const button = form.querySelector('#save-btn');
  button.disabled = true;
  button.textContent = 'Saving...';
  const data = Object.fromEntries(new FormData(form));
  data.price = Number(data.price);
  data.warrantyMonths = Number(data.warrantyMonths);
  const attachScreenshot = form.elements.screenshot.checked;
  delete data.screenshot;
  let savedPurchase = false;
  try {
    const purchase = await WarrifyApi.request('/api/purchases', { method: 'POST', body: JSON.stringify(data) });
    savedPurchase = true;
    if (attachScreenshot) {
      try {
        const tab = await activeTab();
        const screenshot = await chrome.tabs.captureVisibleTab(tab?.windowId, { format: 'png' });
        const blob = await (await fetch(screenshot)).blob();
        const file = new File([blob], 'page-screenshot.png', { type: 'image/png' });
        const upload = new FormData();
        upload.append('file', file);
        await WarrifyApi.request(`/api/purchases/${purchase.id}/bill`, { method: 'POST', body: upload });
      } catch (error) {
        showMessage(`Purchase saved, but screenshot upload failed: ${error.message}`);
        refreshTabBadges();
        return;
      }
    }
    showMessage('Purchase saved to Warrify.', 'success');
    form.reset();
    form.elements.purchaseDate.value = localDate();
    form.elements.warrantyMonths.value = '12';
    refreshTabBadges();
  } catch (error) {
    Object.entries(error.fields || {}).forEach(([field, message]) => {
      const target = form.querySelector(`[data-error="${CSS.escape(field)}"]`);
      const input = form.elements[field];
      if (target) target.textContent = message;
      if (input) input.classList.add('is-invalid');
    });
    showMessage(savedPurchase ? 'Purchase was saved.' : (Object.keys(error.fields || {}).length ? 'Please fix the highlighted validation errors.' : error.message), savedPurchase ? 'success' : '');
  } finally {
    button.disabled = false;
    button.textContent = 'Save purchase to vault';
  }
}

async function refreshTabBadges() {
  try {
    const [purchases, notifications] = await Promise.all([
      WarrifyApi.request('/api/purchases?status=EXPIRING'),
      WarrifyApi.request('/api/notifications')
    ]);
    setTabCount('#expiring-count', purchases.length);
    setTabCount('#notification-count', notifications.filter(item => !item.read).length);
  } catch (_) { }
}

function setTabCount(selector, count) {
  const badge = document.querySelector(selector);
  if (!badge) return;
  badge.textContent = String(Math.min(count, 99));
  badge.hidden = count < 1;
}

async function loadExpiring() {
  const panel = document.querySelector('#panel-expiring');
  panel.innerHTML = '<div class="loading">Loading warranties...</div>';
  try {
    const items = await WarrifyApi.request('/api/purchases?status=EXPIRING');
    setTabCount('#expiring-count', items.length);
    if (!items.length) {
      panel.innerHTML = '<div class="empty">No warranties expiring within 30 days.</div>';
      return;
    }
    panel.innerHTML = `<div class="list-scroll">${items.map(item => {
      const days = Number(item.daysLeft);
      const fullTermDays = Math.max((Number(item.warrantyMonths) || 12) * 30.4375, 1);
      const remaining = Math.max(4, Math.min(100, Math.round(days / fullTermDays * 100)));
      const color = days <= 7 ? 'var(--danger)' : 'var(--warn)';
      return `<article class="item"><div class="row"><div class="item-title" style="flex:1">${escapeHtml(item.productName)}</div><span class="pill ${days <= 7 ? 'expired' : 'soon'}">${days} days left</span></div>
        <div class="sub">${escapeHtml(item.brand || 'Unbranded')} · Expires ${escapeHtml(item.warrantyExpiry)}</div><div class="meter"><div class="meter-fill" style="width:${remaining}%;background:${color}"></div></div></article>`;
    }).join('')}</div>`;
  } catch (error) { panel.innerHTML = `<div class="notice">${escapeHtml(error.message)}</div>`; }
}

async function loadNotifications() {
  const panel = document.querySelector('#panel-notifications');
  panel.innerHTML = '<div class="loading">Loading notifications...</div>';
  try {
    const items = await WarrifyApi.request('/api/notifications');
    items.sort((a, b) => Number(a.read) - Number(b.read) || new Date(b.createdAt) - new Date(a.createdAt));
    const unreadItems = items.filter(item => !item.read);
    setTabCount('#notification-count', unreadItems.length);
    if (!items.length) {
      panel.innerHTML = '<div class="empty">You are all caught up. No notifications yet.</div>';
      return;
    }
    panel.innerHTML = `${unreadItems.length ? `<div class="panel-toolbar"><span class="muted">${unreadItems.length} unread alerts</span>${unreadItems.length > 1 ? '<button id="mark-all-read" class="button secondary small">Mark all read</button>' : ''}</div>` : ''}
      <div class="list-scroll">${items.map(item => `<article class="item ${item.read ? '' : 'unread'}"><div class="item-title">${escapeHtml(item.message)}</div>
        <div class="row" style="margin-top:5px"><span class="sub" style="flex:1">${escapeHtml(item.type.replaceAll('_', ' '))} · ${new Date(item.createdAt).toLocaleString()}</span>${item.read ? '' : `<button class="button secondary small" data-read="${Number(item.id)}">Mark read</button>`}</div>
      </article>`).join('')}</div>`;
    panel.querySelector('#mark-all-read')?.addEventListener('click', async () => {
      try {
        await Promise.all(unreadItems.map(item => WarrifyApi.request(`/api/notifications/${item.id}/read`, { method: 'PUT' })));
        chrome.runtime.sendMessage({ type: 'poll-notifications' }).catch(() => {});
        await loadNotifications();
        refreshTabBadges();
      } catch (error) { showMessage(error.message); }
    });
    panel.querySelectorAll('[data-read]').forEach(button => button.addEventListener('click', async () => {
      try {
        await WarrifyApi.request(`/api/notifications/${button.dataset.read}/read`, { method: 'PUT' });
        chrome.runtime.sendMessage({ type: 'poll-notifications' }).catch(() => {});
        await loadNotifications();
        refreshTabBadges();
      } catch (error) { showMessage(error.message); }
    }));
  } catch (error) { panel.innerHTML = `<div class="notice">${escapeHtml(error.message)}</div>`; }
}

async function logout() {
  try { await WarrifyApi.request('/api/auth/token', { method: 'DELETE' }); } catch (_) { }
  await chrome.storage.local.remove(['token','name','role']);
  await chrome.storage.local.set({ authExpired: false });
  chrome.action.setBadgeText({ text: '' });
  await render();
}

chrome.runtime.onMessage.addListener(message => {
  if (message?.type === 'auth-expired') renderLogin('Your sign-in expired. Please log in again.');
  if (message?.type === 'notification-count') setTabCount('#notification-count', message.count);
});

render();
