document.addEventListener('DOMContentLoaded', () => {
  const login = document.querySelector('#login-form');
  login?.addEventListener('submit', async e => { e.preventDefault(); try { const user = await api.post('/api/auth/login', Object.fromEntries(new FormData(login))); location.href = user.role === 'ADMIN' ? 'admin.html' : user.role === 'SERVICE_CENTER' ? 'service-centre.html' : 'dashboard.html'; } catch (err) { ui.alert(err.message); } });
  const register = document.querySelector('#register-form');
  register?.addEventListener('submit', async e => { e.preventDefault(); try { const data = Object.fromEntries(new FormData(register)); await api.post('/api/auth/register', data); await api.post('/api/auth/login', {email:data.email,password:data.password}); location.href='dashboard.html'; } catch(err) { ui.alert(err.message); } });
});
