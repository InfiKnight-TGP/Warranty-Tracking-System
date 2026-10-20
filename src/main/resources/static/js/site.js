const app = document.querySelector('#app');
const page = document.body.dataset.page;
const header = (eyebrow, title, subtitle, action = '') => `<div class="page-heading"><div><p class="eyebrow">${eyebrow}</p><h1>${title}</h1><p class="subtitle">${subtitle}</p></div>${action}</div><div id="message" class="alert" hidden></div>`;
const empty = (title, text) => `<div class="card empty-state"><strong>${title}</strong>${text}</div>`;
const money = v => new Intl.NumberFormat('en-IN',{style:'currency',currency:'INR'}).format(Number(v || 0));
const detail = (label, value) => `<div class="detail-item"><small>${label}</small><div class="fw-semibold">${value}</div></div>`;

const statIcons = {
  active: '\u2713',
  expiring: '\u23f3',
  expired: '\u26a0',
  openClaims: '\u2692'
};

function warrantyMeter(daysLeft, warrantyMonths) {
  const totalDays = Math.max((warrantyMonths || 12) * 30, 1);
  const pct = Math.max(0, Math.min(100, Math.round((daysLeft / totalDays) * 100)));
  const color = daysLeft < 0 ? '#ef4444' : daysLeft <= 30 ? '#f59e0b' : '#10b981';
  const text = daysLeft < 0 ? `${Math.abs(daysLeft)}d overdue` : `${daysLeft} days left`;
  return `<div><div class="small fw-semibold">${text}</div><div class="warranty-meter"><div class="warranty-meter-fill" style="width:${pct}%;background:${color}"></div></div></div>`;
}

function claimTrack(status) {
  if (status === 'REJECTED') {
    return `<div class="claim-Track"><div class="claim-step current" style="background:#ef4444">CLAIM REJECTED</div></div>`;
  }
  const stages = ['RAISED', 'RECEIVED', 'IN_REPAIR', 'REPAIRED', 'RETURNED'];
  const idx = stages.indexOf(status);
  return `<div class="claim-Track">${stages.map((st, i) => {
    const cls = i < idx ? 'done' : i === idx ? 'current' : '';
    return `<div class="claim-step ${cls}">${st.replace('_', ' ')}</div>`;
  }).join('')}</div>`;
}

async function boot() {
  const user = await ui.shell(); if (!user) return;
  try {
    if (page === 'dashboard') await dashboard();
    if (page === 'purchases') await purchases();
    if (page === 'purchase-form') await purchaseForm();
    if (page === 'purchase-detail') await purchaseDetail();
    if (page === 'claims') await claims();
    if (page === 'notifications') await notifications();
    if (page === 'service-centre') await serviceCentre(user);
    if (page === 'admin') await admin(user);
  } catch (e) { ui.alert(e.message); }
}

async function dashboard() {
  app.innerHTML = header('Your overview','Dashboard','A real-time snapshot of your product warranties and active claims.','<a class="btn btn-primary" href="purchase-form.html">+ Add purchase</a>')
    + `<div class="row g-3 mb-5">${[['active','Active warranties'],['expiring','Expiring soon'],['expired','Expired'],['openClaims','Open claims']].map(([id,label])=>`<div class="col-6 col-lg-3"><div class="card stat-card" data-tone="${id}"><div class="stat-header"><div class="stat-label">${label}</div><div class="stat-icon">${statIcons[id]}</div></div><div class="stat-value" id="${id}">—</div></div></div>`).join('')}</div>`
    + `<div class="page-heading"><div><p class="eyebrow">Action required</p><h2 class="h4 mb-1">Upcoming expiries</h2><p class="subtitle">Warranties expiring within the next 30 days.</p></div><a href="purchases.html" class="btn btn-outline-secondary">View all purchases</a></div><div id="upcoming"></div>`;
  const data = await api.get('/api/dashboard');
  for (const key of ['active','expiring','expired','openClaims']) document.getElementById(key).textContent = data[key];
  const items = await api.get('/api/purchases?status=EXPIRING');
  document.querySelector('#upcoming').innerHTML = items.length
    ? `<div class="card table-card"><table class="table"><thead><tr><th>Product</th><th>Brand & Store</th><th>Expiry date</th><th>Coverage remaining</th><th></th></tr></thead><tbody>${items.map(p=>`<tr><td><strong>${ui.esc(p.productName)}</strong><div class="small text-secondary">${ui.esc(p.serialNumber || 'No serial')}</div></td><td>${ui.esc(p.brand || '—')}<div class="small text-secondary">${ui.esc(p.storeName || '')}</div></td><td>${ui.date(p.warrantyExpiry)}</td><td>${warrantyMeter(p.daysLeft, p.warrantyMonths)}</td><td class="text-end"><a class="btn btn-sm btn-outline-secondary" href="purchase-detail.html?id=${p.id}">Inspect</a></td></tr>`).join('')}</tbody></table></div>`
    : empty('Nothing expiring soon','All of your registered warranties have plenty of coverage remaining.');
}

async function purchases() {
  app.innerHTML = header('Your collection','Purchases','Search, filter, or bulk-import your product bills and warranty records.','<a class="btn btn-primary" href="purchase-form.html">+ Add purchase</a>')
    + `<div class="toolbar"><input id="keyword" class="form-control" placeholder="Search name, brand, serial..."><input id="category" class="form-control" placeholder="Category (e.g. Electronics)"><select id="status" class="form-select"><option value="">All warranty statuses</option><option value="ACTIVE">Active</option><option value="EXPIRING">Expiring soon</option><option value="EXPIRED">Expired</option></select><button id="search" class="btn btn-primary">Apply Filter</button></div>`
    + `<div class="card p-3 mb-4"><form id="csv-import-form" class="d-flex flex-wrap align-items-center justify-content-between gap-2"><div class="d-flex align-items-center gap-2 flex-wrap"><span class="badge text-bg-primary">Spring Batch CSV</span><strong class="small">Bulk Import Purchases:</strong><input type="file" id="csv-file" class="form-control form-control-sm" style="max-width:260px" accept=".csv" required><button class="btn btn-sm btn-outline-secondary" type="submit">Upload & Run Job</button></div><span class="small text-secondary">Header: productName,brand,category,serialNumber,storeName,purchaseDate,price,warrantyMonths,supportContact</span></form></div>`
    + `<div id="results"></div>`;
  const load = async () => {
    const q = new URLSearchParams();
    for(const id of ['keyword','category','status']) if(document.getElementById(id).value) q.set(id,document.getElementById(id).value);
    const list=await api.get('/api/purchases?'+q);
    document.querySelector('#results').innerHTML=list.length
      ? `<div class="card table-card"><table class="table"><thead><tr><th>Product</th><th>Category</th><th>Purchase date</th><th>Expiry</th><th>Timeline</th><th>Status</th><th></th></tr></thead><tbody>${list.map(p=>`<tr><td><strong>${ui.esc(p.productName)}</strong><div class="small text-secondary">${ui.esc(p.brand || '')} ${p.serialNumber ? '· ' + ui.esc(p.serialNumber) : ''}</div></td><td>${ui.esc(p.category || '—')}</td><td>${ui.date(p.purchaseDate)}</td><td>${ui.date(p.warrantyExpiry)}</td><td>${warrantyMeter(p.daysLeft, p.warrantyMonths)}</td><td>${ui.badge(p.status)}</td><td class="text-end"><a class="btn btn-sm btn-outline-secondary" href="purchase-detail.html?id=${p.id}">Manage</a></td></tr>`).join('')}</tbody></table></div>`
      : empty('No matching purchases','Try clearing your filters or add a new purchase record.');
  };
  document.querySelector('#search').addEventListener('click',load);
  document.querySelector('#keyword').addEventListener('keydown',e=>{if(e.key==='Enter')load()});
  document.querySelector('#csv-import-form').addEventListener('submit', async e => {
    e.preventDefault();
    ui.hideAlert();
    const fileInput = document.querySelector('#csv-file');
    if (!fileInput.files[0]) return;
    const fd = new FormData();
    fd.append('file', fileInput.files[0]);
    try {
      const res = await api.post('/api/purchases/import', fd);
      ui.alert(`CSV import ${res.status}: ${res.writeCount} imported, ${res.skipCount} invalid rows skipped.`, 'success');
      fileInput.value = '';
      await load();
    } catch (err) {
      ui.alert(err.message);
    }
  });
  await load();
}

async function purchaseForm() {
  const id=new URLSearchParams(location.search).get('id');
  app.innerHTML=header('Your collection',id?'Edit purchase':'Register a purchase','Validated with custom Jakarta Bean Validation constraints (@ValidWarrantyPeriod & @ValidPurchaseDates).')
    + `<form id="form" class="card p-4" novalidate><div class="row g-3">${[['productName','Product name','text',true],['brand','Brand','text'],['category','Category','text'],['serialNumber','Serial number','text'],['storeName','Store','text'],['purchaseDate','Purchase date (2000-01-01 to today)','date',true],['price','Price (INR)','number',true],['warrantyMonths','Warranty period (1–120 months)','number',true],['supportContact','Support contact','text']].map(([n,l,t,r])=>`<div class="col-md-6"><label class="form-label">${l}${r?' *':''}</label><input class="form-control" name="${n}" type="${t}" ${r?'required':''} ${n==='price'?'min="0" step="0.01"':''} ${n==='warrantyMonths'?'min="1" max="120"':''}><div class="invalid-feedback" data-error-for="${n}"></div></div>`).join('')}<div id="bill-area" class="col-12"><label class="form-label">Invoice / Bill attachment (PDF, JPG, PNG; max 5 MB)</label><input class="form-control" type="file" id="bill" accept=".pdf,.jpg,.jpeg,.png"></div></div><div class="d-flex gap-2 mt-4"><button class="btn btn-primary">${id ? 'Update purchase' : 'Save purchase'}</button><a class="btn btn-outline-secondary" href="purchases.html">Cancel</a></div></form>`;
  const form=document.querySelector('#form');
  if(id){const p=await api.get('/api/purchases/'+id);for(const [k,v] of Object.entries(p)){if(form.elements[k])form.elements[k].value=v??''}}
  form.addEventListener('submit',async e=>{
    e.preventDefault();
    ui.hideAlert();
    form.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
    form.querySelectorAll('[data-error-for]').forEach(el => el.textContent = '');
    const data=Object.fromEntries(new FormData(form));
    data.price=Number(data.price);
    data.warrantyMonths=Number(data.warrantyMonths);
    try{
      const p=id?await api.put('/api/purchases/'+id,data):await api.post('/api/purchases',data);
      const file=document.querySelector('#bill').files[0];
      if(file){const fd=new FormData();fd.append('file',file);await api.post('/api/purchases/'+p.id+'/bill',fd)}
      location.href='purchase-detail.html?id='+p.id;
    }catch(err){
      if (err.fields) {
        for (const [field, msg] of Object.entries(err.fields)) {
          const input = form.elements[field];
          const feedback = form.querySelector(`[data-error-for="${field}"]`);
          if (input) input.classList.add('is-invalid');
          if (feedback) feedback.textContent = msg;
        }
      }
      ui.alert(err.message);
    }
  });
}

async function purchaseDetail(){
  const id=new URLSearchParams(location.search).get('id'); if(!id){location.href='purchases.html';return} const p=await api.get('/api/purchases/'+id);
  app.innerHTML=header('Purchase details',ui.esc(p.productName),`${ui.esc(p.brand||'Unbranded')} · ${ui.badge(p.status)}`,`<div class="d-flex gap-2"><a class="btn btn-outline-secondary" href="purchase-form.html?id=${p.id}">Edit</a><button id="delete" class="btn btn-outline-danger">Delete</button></div>`)
    + `<div class="card p-4"><div class="detail-grid">${detail('Category',ui.esc(p.category || '—'))}${detail('Serial number',ui.esc(p.serialNumber || '—'))}${detail('Store',ui.esc(p.storeName || '—'))}${detail('Purchase date',ui.date(p.purchaseDate))}${detail('Price',money(p.price))}${detail('Warranty term',p.warrantyMonths+' months')}${detail('Warranty expiry',ui.date(p.warrantyExpiry))}${detail('Remaining coverage',warrantyMeter(p.daysLeft, p.warrantyMonths))}${detail('Support contact',ui.esc(p.supportContact || '—'))}</div><div class="d-flex flex-wrap gap-2 mt-4">${p.billFilePath?`<a class="btn btn-outline-secondary" href="/api/purchases/${p.id}/bill">Download invoice</a>`:''}<button id="raise" class="btn btn-primary">Raise warranty claim</button><a class="btn btn-outline-secondary" href="purchases.html">Back to list</a></div><form id="claim" class="mt-4 pt-3 border-top" hidden><label class="form-label">Describe the hardware or service issue</label><textarea id="issue" class="form-control mb-3" rows="3" maxlength="2000" placeholder="Explain the defect or malfunction in detail..." required></textarea><button class="btn btn-primary">Submit warranty claim</button></form></div>`;
  document.querySelector('#delete').onclick=async()=>{if(confirm('Delete this purchase permanently?')){await api.delete('/api/purchases/'+id);location.href='purchases.html'}};
  document.querySelector('#raise').onclick=()=>document.querySelector('#claim').hidden=false;
  document.querySelector('#claim').onsubmit=async e=>{e.preventDefault();try{await api.post(`/api/purchases/${id}/claims`,{issueDescription:document.querySelector('#issue').value});location.href='claims.html'}catch(err){ui.alert(err.message)}};
}

async function claims(){
  app.innerHTML=header('After-sales support','My claims','Real-time claim progression and service centre audit trail.')+`<div id="list" class="d-grid gap-3"></div>`;
  const list=await api.get('/api/claims/my');
  if(!list.length){document.querySelector('#list').innerHTML=empty('No claims raised yet','Open any active purchase to file a warranty claim.');return}
  document.querySelector('#list').innerHTML=list.map(c=>`<article class="card p-4"><div class="d-flex justify-content-between align-items-start flex-wrap gap-2"><div><h2 class="h5 mb-1">${ui.esc(c.productName)}</h2><p class="text-secondary mb-1">${ui.esc(c.issueDescription)}</p></div>${ui.badge(c.status)}</div><p class="small text-secondary mb-2">Claim #${c.id} · Raised ${new Date(c.createdAt).toLocaleDateString()} ${c.serviceCentreName ? '· Assigned to ' + ui.esc(c.serviceCentreName) : '· Awaiting centre assignment'}</p>${claimTrack(c.status)}<div class="timeline" id="history-${c.id}">Loading history…</div></article>`).join('');
  for(const c of list){
    try{
      const history=await api.get(`/api/claims/${c.id}/history`);
      document.querySelector(`#history-${c.id}`).innerHTML=history.length
        ? history.map(h=>`<div class="timeline-item"><strong>${ui.esc(h.newStatus.replaceAll('_',' '))}</strong><div class="small text-secondary">${new Date(h.updatedAt).toLocaleString()} · ${ui.esc(h.updatedByName)}${h.note?' · "'+ui.esc(h.note)+'"':''}</div></div>`).join('')
        : `<div class="timeline-item"><strong>RAISED</strong><div class="small text-secondary">Claim submitted and awaiting service centre review</div></div>`;
    }catch(_){}
  }
}

async function notifications(){
  app.innerHTML=header('Stay informed','Notifications','Automated JMS warranty expiry alerts and service claim updates.')+'<div id="list" class="card"></div>';
  const list=await api.get('/api/notifications');
  const unreadBadge=document.querySelector('#unread-count');
  if(unreadBadge) unreadBadge.textContent=list.filter(n=>!n.read).length;
  if(!list.length){document.querySelector('#list').outerHTML=empty('You are all caught up','New JMS expiry reminders and claim updates will appear here.');return}
  document.querySelector('#list').innerHTML=list.map(n=>{
    const isClaimUpdate=n.type==='CLAIM_UPDATE';
    let summary=n.message;
    let serviceMessage='';
    if(isClaimUpdate){
      const separator='\nService centre message: ';
      let splitAt=summary.indexOf(separator);
      let separatorLength=separator.length;
      if(splitAt<0){
        splitAt=summary.indexOf(' Note: ');
        separatorLength=' Note: '.length;
      }
      if(splitAt>=0){
        serviceMessage=summary.slice(splitAt+separatorLength);
        summary=summary.slice(0,splitAt);
      }
    }
    const content=isClaimUpdate
      ? `<div class="notification-title">Claim update</div><div class="notification-summary">${ui.esc(summary)}</div>${serviceMessage?`<div class="service-message"><div class="service-message-label">Message from the service centre</div><p>${ui.esc(serviceMessage)}</p></div>`:''}`
      : `<div class="notification-title">${ui.esc(n.message)}</div>`;
    return `<div class="notification ${n.read?'':'unread'}"><div class="notification-copy">${content}<div class="small text-secondary mt-2">${ui.badge(n.type)} · ${new Date(n.createdAt).toLocaleString()}</div></div>${n.read?'':'<button class="btn btn-sm btn-outline-secondary" data-read="'+n.id+'">Mark read</button>'}</div>`;
  }).join('');
  document.querySelectorAll('[data-read]').forEach(button=>button.addEventListener('click',async()=>{
    button.disabled=true;
    try{
      await api.put(`/api/notifications/${button.dataset.read}/read`,{});
      await notifications();
    }catch(err){
      button.disabled=false;
      ui.alert(err.message);
    }
  }));
}

async function serviceCentre(user){
  if(user.role!=='SERVICE_CENTER'){location.href='login.html';return}
  app.innerHTML=header('Service centre portal','Assigned claims','Advance repair stages and publish JMS notifications to customers.')+'<div id="list" class="d-grid gap-3"></div>';
  const list=await api.get('/api/claims/assigned');
  if(!list.length){document.querySelector('#list').innerHTML=empty('No assigned claims','Claims assigned to your service centre by an administrator will appear here.');return}
  const next={RAISED:['RECEIVED','REJECTED'],RECEIVED:['IN_REPAIR','REJECTED'],IN_REPAIR:['REPAIRED'],REPAIRED:['RETURNED']};
  document.querySelector('#list').innerHTML=list.map(c=>`<article class="card p-4"><div class="d-flex justify-content-between align-items-start"><div><h2 class="h5 mb-1">${ui.esc(c.productName)} · Claim #${c.id}</h2><p class="mb-1">${ui.esc(c.issueDescription)}</p><p class="small text-secondary">Customer: ${ui.esc(c.raisedByName)}</p></div>${ui.badge(c.status)}</div>${claimTrack(c.status)}${(next[c.status]||[]).length ? `<form class="status-form mt-2" data-id="${c.id}"><div class="row g-2"><div class="col-md-4"><select class="form-select" name="newStatus">${(next[c.status]||[]).map(s=>`<option value="${s}">${s.replace('_',' ')}</option>`).join('')}</select></div><div class="col-md-6"><input class="form-control" name="note" placeholder="Technician resolution note (sent via JMS)"></div><div class="col-md-2"><button class="btn btn-primary w-100">Update</button></div></div></form>` : `<div class="small text-secondary">Terminal claim state reached.</div>`}</article>`).join('');
  document.querySelectorAll('.status-form').forEach(f=>f.onsubmit=async e=>{e.preventDefault();const d=Object.fromEntries(new FormData(f));try{await api.put('/api/claims/'+f.dataset.id+'/status',d);await serviceCentre(user)}catch(err){ui.alert(err.message)}});
}

async function admin(user){
  if(user.role!=='ADMIN'){location.href='login.html';return}
  app.innerHTML=header('Administration','System Overview','Users, service centre assignments, native SQL analytics, and Spring Batch jobs.')
    + `<div class="row g-3 mb-4">${[['active','Active'],['expiring','Expiring soon'],['expired','Expired'],['openClaims','Open claims']].map(([id,l])=>`<div class="col-6 col-lg-3"><div class="card stat-card" data-tone="${id}"><div class="stat-header"><div class="stat-label">${l}</div><div class="stat-icon">${statIcons[id]}</div></div><div class="stat-value" id="${id}">—</div></div></div>`).join('')}</div>`
    + `<div class="row g-3 mb-4"><div class="col-md-6"><div class="card p-4 h-100"><h3 class="h6 text-uppercase text-secondary mb-3">Purchases by Category (Native SQL)</h3><div id="cat-breakdown" class="d-flex flex-wrap gap-2"></div></div></div><div class="col-md-6"><div class="card p-4 h-100"><h3 class="h6 text-uppercase text-secondary mb-3">Claims by Status (Native SQL)</h3><div id="claim-breakdown" class="d-flex flex-wrap gap-2"></div></div></div></div>`
    + `<h2 class="h4 mb-3">Registered Users</h2><div class="card table-card mb-4"><table class="table"><thead><tr><th>Name</th><th>Email</th><th>Phone</th><th>Role</th></tr></thead><tbody id="users"></tbody></table></div>`
    + `<h2 class="h4 mb-3" id="claims">All Warranty Claims</h2><div class="card table-card mb-4"><table class="table"><thead><tr><th>Product</th><th>Customer</th><th>Status</th><th>Assign service centre</th></tr></thead><tbody id="claims-list"></tbody></table></div>`
    + `<div class="page-heading mt-5" id="batch-jobs"><div><p class="eyebrow">Spring Batch Orchestration</p><h2 class="h4 mb-1">Batch Jobs & Execution History</h2><p class="subtitle">Trigger reminder and CSV export jobs on demand and inspect step metrics.</p></div><div class="d-flex gap-2"><button id="run-reminders" class="btn btn-primary btn-sm">▶ Run Reminder Job</button><button id="run-export" class="btn btn-outline-secondary btn-sm">⬇ Export Purchases CSV</button></div></div>`
    + `<div class="card table-card"><table class="table"><thead><tr><th>Execution ID</th><th>Job Name</th><th>Status</th><th>Read</th><th>Write</th><th>Skip</th></tr></thead><tbody id="batch-executions"></tbody></table></div>`;

  const loadExecutions = async () => {
    const executions = await api.get('/api/admin/batch/executions');
    document.querySelector('#batch-executions').innerHTML = executions.length
      ? executions.map(ex => `<tr><td><strong>#${ex.executionId}</strong></td><td><code>${ui.esc(ex.jobName)}</code></td><td>${ui.badge(ex.status)}</td><td>${ex.readCount}</td><td>${ex.writeCount}</td><td>${ex.skipCount}</td></tr>`).join('')
      : `<tr><td colspan="6" class="text-center text-secondary py-4">No batch job executions recorded yet.</td></tr>`;
  };

  const [stats,users,claims]=await Promise.all([api.get('/api/admin/stats'),api.get('/api/admin/users'),api.get('/api/admin/claims')]);
  for(const k of ['active','expiring','expired','openClaims'])document.getElementById(k).textContent=stats[k];

  const catEntries = Object.entries(stats.purchasesByCategory || {});
  document.querySelector('#cat-breakdown').innerHTML = catEntries.length
    ? catEntries.map(([k,v]) => `<div class="detail-item py-2 px-3"><small>${ui.esc(k)}</small><strong class="fs-5">${v}</strong></div>`).join('')
    : '<span class="text-secondary small">No purchases recorded</span>';

  const claimEntries = Object.entries(stats.claimsByStatus || {});
  document.querySelector('#claim-breakdown').innerHTML = claimEntries.length
    ? claimEntries.map(([k,v]) => `<div class="detail-item py-2 px-3"><small>${ui.esc(k)}</small><strong class="fs-5">${v}</strong></div>`).join('')
    : '<span class="text-secondary small">No claims recorded</span>';

  document.querySelector('#users').innerHTML=users.map(u=>`<tr><td><strong>${ui.esc(u.name)}</strong></td><td>${ui.esc(u.email)}</td><td>${ui.esc(u.phone || '—')}</td><td><span class="role-tag">${ui.esc(u.role)}</span></td></tr>`).join('');
  const centres=users.filter(u=>u.role==='SERVICE_CENTER');
  document.querySelector('#claims-list').innerHTML=claims.length ? claims.map(c=>`<tr><td><strong>${ui.esc(c.productName)}</strong><div class="small text-secondary">Claim #${c.id}</div></td><td>${ui.esc(c.raisedByName)}</td><td>${ui.badge(c.status)}</td><td><select class="form-select form-select-sm assign" data-id="${c.id}"><option value="">${c.serviceCentreName?ui.esc(c.serviceCentreName):'Choose centre'}</option>${centres.map(u=>`<option value="${u.id}">${ui.esc(u.name)}</option>`).join('')}</select></td></tr>`).join('') : `<tr><td colspan="4" class="text-center text-secondary py-4">No claims filed yet.</td></tr>`;
  document.querySelectorAll('.assign').forEach(s=>s.onchange=async()=>{if(s.value){try{await api.put(`/api/admin/claims/${s.dataset.id}/assign`,{serviceCentreId:Number(s.value)});ui.alert('Service centre assigned.','success')}catch(err){ui.alert(err.message)}}});

  document.querySelector('#run-reminders').onclick = async () => {
    try {
      const ex = await api.post('/api/admin/batch/reminders/run', {});
      ui.alert(`Reminder job #${ex.executionId} finished (${ex.status}).`, 'success');
      await loadExecutions();
    } catch (err) { ui.alert(err.message); }
  };

  document.querySelector('#run-export').onclick = async () => {
    try {
      const response = await api.post('/api/admin/batch/export/run', {});
      const blob = await response.blob();
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = 'purchases-export.csv';
      a.click();
      URL.revokeObjectURL(url);
      await loadExecutions();
    } catch (err) { ui.alert(err.message); }
  };

  await loadExecutions();
}

document.addEventListener('DOMContentLoaded',boot);
