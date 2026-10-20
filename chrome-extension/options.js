const input = document.querySelector('#serverUrl');
const statusBox = document.querySelector('#status-box');
const themeButton = document.querySelector('#theme-toggle');

function showStatus(text, success = false) {
  statusBox.textContent = text;
  statusBox.className = `notice ${success ? 'success' : ''}`;
  statusBox.hidden = !text;
}

function applyTheme(theme) {
  document.documentElement.dataset.theme = theme;
  document.querySelectorAll('.brand img.wordmark').forEach(image => {
    image.src = theme === 'dark' ? 'icons/warrify-logo-dark.svg' : 'icons/warrify-logo.svg';
  });
  themeButton.textContent = theme === 'dark' ? '☀' : '☾';
  themeButton.title = theme === 'dark' ? 'Switch to light theme' : 'Switch to dark theme';
}

async function ensureHostPermission(url) {
  const originPattern = `${url.origin}/*`;
  const hasPermission = await chrome.permissions.contains({ origins: [originPattern] });
  if (hasPermission) return true;
  return chrome.permissions.request({ origins: [originPattern] });
}

function parseServerUrl() {
  const url = new URL(input.value.trim().replace(/\/+$/, ''));
  if (!['http:', 'https:'].includes(url.protocol)) throw new Error('Enter a valid http or https URL.');
  return url;
}

chrome.storage.local.get({ serverUrl: 'http://localhost:8080', theme: 'light' }).then(values => {
  input.value = values.serverUrl;
  applyTheme(values.theme);
});

themeButton.addEventListener('click', async () => {
  const next = document.documentElement.dataset.theme === 'dark' ? 'light' : 'dark';
  await chrome.storage.local.set({ theme: next });
  applyTheme(next);
});

document.querySelector('#test-conn').addEventListener('click', async event => {
  const button = event.currentTarget;
  let url;
  try { url = parseServerUrl(); }
  catch (error) { showStatus(error.message); return; }
  button.disabled = true;
  showStatus('Checking Warrify server...', true);
  try {
    const allowed = await ensureHostPermission(url);
    if (!allowed) { showStatus('Chrome did not grant access to this server.'); return; }
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 8000);
    let response;
    try {
      // An unauthenticated 401 is enough to confirm the server and API are reachable.
      const base = url.href.replace(/\/+$/, '');
      response = await fetch(`${base}/api/auth/me`, { credentials: 'include', signal: controller.signal });
    } finally { clearTimeout(timeout); }
    if (response.ok || response.status === 401) showStatus(`Connected. Warrify is reachable at ${url.href.replace(/\/+$/, '')}.`, true);
    else showStatus(`Server responded with HTTP ${response.status}.`);
  } catch (error) {
    showStatus(error.name === 'AbortError' ? 'Connection timed out. Check that Warrify is running.' : `Could not reach ${url.origin}. Check that Warrify is running.`);
  } finally { button.disabled = false; }
});

document.querySelector('#options-form').addEventListener('submit', async event => {
  event.preventDefault();
  try {
    const url = parseServerUrl();
    const allowed = await ensureHostPermission(url);
    if (!allowed) { showStatus('Chrome did not grant access to this server URL.'); return; }
    const value = url.href.replace(/\/+$/, '');
    await chrome.storage.local.set({ serverUrl: value });
    showStatus('Server URL saved.', true);
  } catch (_) { showStatus('Enter a valid http or https URL.'); }
});
