const theme = {
  current() {
    const saved = localStorage.getItem('vault-theme');
    if (saved === 'dark' || saved === 'light') return saved;
    return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
  },
  apply(mode) {
    document.documentElement.setAttribute('data-bs-theme', mode);
    localStorage.setItem('vault-theme', mode);
    document.querySelectorAll('.theme-toggle').forEach(btn => {
      btn.innerHTML = mode === 'dark'
        ? '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="4"/><path d="M12 2v2"/><path d="M12 20v2"/><path d="m4.93 4.93 1.41 1.41"/><path d="m17.66 17.66 1.41 1.41"/><path d="M2 12h2"/><path d="M20 12h2"/><path d="m6.34 17.66-1.41 1.41"/><path d="m19.07 4.93-1.41 1.41"/></svg>'
        : '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 3a6 6 0 0 0 9 9 9 9 0 1 1-9-9Z"/></svg>';
      btn.title = mode === 'dark' ? 'Switch to Light Mode' : 'Switch to Dark Mode';
    });
  },
  toggle() {
    this.apply(this.current() === 'dark' ? 'light' : 'dark');
  },
  init() {
    this.apply(this.current());
    if (!document.querySelector('#navbar') && !document.querySelector('.theme-toggle')) {
      const floating = document.createElement('button');
      floating.type = 'button';
      floating.className = 'theme-toggle theme-toggle-floating';
      floating.setAttribute('aria-label', 'Toggle color theme');
      floating.addEventListener('click', () => this.toggle());
      document.body.appendChild(floating);
      this.apply(this.current());
    }
  }
};

theme.apply(theme.current());
document.addEventListener('DOMContentLoaded', () => theme.init());

const api = {
  async request(path, options = {}) {
    const headers = new Headers(options.headers || {});
    if (options.body && !(options.body instanceof FormData)) headers.set('Content-Type', 'application/json');
    const response = await fetch(path, { credentials: 'same-origin', ...options, headers });
    if (response.status === 401 && !path.endsWith('/auth/login') && !path.endsWith('/auth/register')) {
      window.location.href = 'login.html';
      throw new Error('Please sign in to continue.');
    }
    if (!response.ok) {
      let message = `Request failed (${response.status})`;
      let fields = null;
      try {
        const data = await response.json();
        message = data.message || message;
        fields = data.fields || null;
      } catch (_) { }
      const err = new Error(message);
      err.fields = fields;
      throw err;
    }
    if (response.status === 204) return null;
    const type = response.headers.get('content-type') || '';
    return type.includes('application/json') ? response.json() : response;
  },
  get(path) { return this.request(path); },
  post(path, data) { return this.request(path, { method: 'POST', body: data instanceof FormData ? data : JSON.stringify(data) }); },
  put(path, data) { return this.request(path, { method: 'PUT', body: JSON.stringify(data) }); },
  delete(path) { return this.request(path, { method: 'DELETE' }); }
};

const ui = {
  esc(value) { return String(value ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c])); },
  alert(message, kind = 'danger') { const box = document.querySelector('#message'); if (box) { box.className = `alert alert-${kind} shadow-sm rounded-3`; box.textContent = message; box.hidden = false; } },
  hideAlert() { const box = document.querySelector('#message'); if (box) box.hidden = true; },
  async shell() {
    const slot = document.querySelector('#navbar'); if (!slot) return null;
    let user; try { user = await api.get('/api/auth/me'); } catch (_) { return null; }
    const role = user.role;
    const currentFile = location.pathname.split('/').pop() || 'dashboard.html';
    const links = role === 'ADMIN'
      ? [['Overview','admin.html'],['Users & claims','admin.html#claims'],['Batch jobs','admin.html#batch-jobs']]
      : role === 'SERVICE_CENTER'
        ? [['Assigned claims','service-centre.html']]
        : [['Dashboard','dashboard.html'],['Purchases','purchases.html'],['My claims','claims.html']];
    const initials = (user.name || 'U').split(/\s+/).map(p => p[0]).join('').slice(0, 2).toUpperCase();
    const navLinks = links.map(([label, url]) => {
      const [file, hash] = url.split('#');
      const active = currentFile === file && (hash ? location.hash === `#${hash}` : !location.hash);
      return `<li class="nav-item"><a class="nav-link ${active ? 'active' : ''}" href="${url}">${label}</a></li>`;
    }).join('');
    slot.innerHTML = `<nav class="navbar navbar-expand-lg"><div class="container"><a class="navbar-brand" href="${role === 'ADMIN' ? 'admin.html' : role === 'SERVICE_CENTER' ? 'service-centre.html' : 'dashboard.html'}"><img class="brand-logo" src="images/warrify-mark.svg" alt=""> Warrify</a><button class="navbar-toggler" data-bs-toggle="collapse" data-bs-target="#navContent" aria-label="Toggle navigation"><span class="navbar-toggler-icon"></span></button><div class="collapse navbar-collapse" id="navContent"><ul class="navbar-nav me-auto gap-1">${navLinks}${role !== 'ADMIN' && role !== 'SERVICE_CENTER' ? `<li class="nav-item"><a class="nav-link ${currentFile === 'notifications.html' ? 'active' : ''}" href="notifications.html">Notifications <span id="unread-count" class="count-pill">0</span></a></li>` : ''}</ul><div class="nav-user"><button type="button" class="theme-toggle" id="nav-theme-toggle" aria-label="Toggle color theme"></button><div class="user-chip"><span class="user-avatar">${ui.esc(initials)}</span><span>${ui.esc(user.name)}</span><span class="role-tag">${ui.esc(role.replace('_',' '))}</span></div><button class="btn btn-sm btn-outline-secondary" id="logout">Sign out</button></div></div></div></nav>`;
    theme.apply(theme.current());
    document.querySelector('#nav-theme-toggle')?.addEventListener('click', () => theme.toggle());
    document.querySelector('#logout')?.addEventListener('click', async () => { try { await api.post('/api/auth/logout', {}); } finally { location.href = 'login.html'; } });
    if (role !== 'ADMIN' && role !== 'SERVICE_CENTER') {
      api.get('/api/notifications').then(items => { const count = items.filter(n => !n.read).length; const badge = document.querySelector('#unread-count'); if (badge) badge.textContent = count; }).catch(() => {});
    }
    return user;
  },
  badge(status) {
    const s = String(status || '').toUpperCase();
    const tone = (s === 'ACTIVE' || s === 'COMPLETED' || s === 'RETURNED' || s === 'REPAIRED')
      ? 'success'
      : (s === 'EXPIRING' || s === 'IN_REPAIR' || s === 'RECEIVED' || s === 'RAISED')
        ? 'warning'
        : (s === 'EXPIRED' || s === 'REJECTED' || s === 'FAILED')
          ? 'danger'
          : 'info';
    const label = s === 'EXPIRING' ? 'Expiring soon' : s.charAt(0) + s.slice(1).toLowerCase().replaceAll('_',' ');
    return `<span class="status-pill tone-${tone}">${ui.esc(label)}</span>`;
  },
  date(value) { return value ? new Date(`${value}T00:00:00`).toLocaleDateString() : '—'; }
};
