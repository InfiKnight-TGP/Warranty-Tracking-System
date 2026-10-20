const WarrifyApi = (() => {
  async function baseUrl() {
    const { serverUrl = 'http://localhost:8080' } = await chrome.storage.local.get('serverUrl');
    return serverUrl.replace(/\/+$/, '');
  }

  async function clearToken() {
    await chrome.storage.local.remove(['token', 'name', 'role']);
    await chrome.storage.local.set({ authExpired: true });
    if (chrome.action) chrome.action.setBadgeText({ text: '!' });
    chrome.runtime.sendMessage({ type: 'auth-expired' }).catch(() => {});
  }

  async function request(path, options = {}) {
    const base = await baseUrl();
    const { token } = await chrome.storage.local.get('token');
    const headers = new Headers(options.headers || {});
    if (options.body && !(options.body instanceof FormData)) headers.set('Content-Type', 'application/json');
    if (token && options.auth !== false) headers.set('Authorization', `Bearer ${token}`);
    const { auth: _auth, timeoutMs = 8000, ...fetchOptions } = options;
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), timeoutMs);
    let response;
    try {
      response = await fetch(`${base}${path}`, {
        ...fetchOptions,
        headers,
        signal: controller.signal,
        credentials: 'include'
      });
    } catch (error) {
      if (error.name === 'AbortError') throw new Error('Warrify server did not respond within 8 seconds.');
      throw new Error('Cannot reach Warrify. Make sure the back end is running at ' + base + '.');
    } finally {
      clearTimeout(timeout);
    }
    if (response.status === 401) {
      await clearToken();
      throw new Error('Your sign-in expired. Please log in again.');
    }
    if (!response.ok) {
      let data = {};
      try { data = await response.json(); } catch (_) { }
      const error = new Error(data.message || `Request failed (${response.status})`);
      error.fields = data.fields || {};
      throw error;
    }
    if (response.status === 204) return null;
    const type = response.headers.get('content-type') || '';
    return type.includes('application/json') ? response.json() : response;
  }

  return { request, baseUrl, clearToken };
})();
