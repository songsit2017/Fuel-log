/* FuelLog Pro v4 Family Sharing (Firebase compat SDK) */
(() => {
  'use strict';
  const cfg = window.FUELLOG_FIREBASE_CONFIG || {};
  const configured = !!(cfg.apiKey && cfg.authDomain && cfg.projectId && cfg.appId);
  let auth = null, db = null, user = null, roleByVehicle = new Map();
  let unsubscribers = [], applyingRemote = false, syncTimer = null;

  const E = (id) => document.getElementById(id);
  const escFS = (v='') => String(v).replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  const toastFS = (msg) => { if (typeof toast === 'function') toast(msg); else alert(msg); };
  const selectedVehicle = () => vehicles.find(v => v.id === currentVehicleId);
  const isCloudVehicle = (v=selectedVehicle()) => !!v?.cloudId;
  const role = (v=selectedVehicle()) => roleByVehicle.get(v?.cloudId) || v?.cloudRole || '';
  const canEdit = () => ['owner','editor'].includes(role());
  const canManage = () => role() === 'owner';
  const clean = (obj) => JSON.parse(JSON.stringify(obj, (k,v) => v === undefined ? null : v));

  function injectUI(){
    const overlay = document.createElement('div');
    overlay.className = 'modal-overlay'; overlay.id = 'familyOverlay'; overlay.style.display = 'none';
    overlay.innerHTML = `<div class="modal" style="max-width:680px">
      <div class="modal-head"><div><div class="modal-title">👨‍👩‍👧‍👦 Family Sharing</div><div style="font-size:11px;color:var(--muted)">แชร์รถให้สมาชิกในบ้านบันทึกข้อมูลร่วมกัน</div></div><button class="close-btn" id="closeFamily">✕</button></div>
      <div id="familyConfigWarning" style="display:none;background:rgba(224,101,79,.12);border:1px solid var(--red);border-radius:12px;padding:12px;font-size:12px;line-height:1.6;margin-bottom:12px"></div>
      <div id="familyAuthBox" class="tool-card" style="margin:0 0 12px"><div class="tool-title">บัญชีผู้ใช้</div><div id="familyUser" style="font-size:12px;color:var(--muted);margin:8px 0">ยังไม่ได้เข้าสู่ระบบ</div><button class="action-btn primary" id="familyLogin">เข้าสู่ระบบด้วย Google</button><button class="action-btn" id="familyLogout" style="display:none;margin-top:8px">ออกจากระบบ</button></div>
      <div id="familyCloudTools" style="display:none">
        <div class="tool-card" style="margin:0 0 12px"><div class="tool-title">รถที่กำลังเลือก</div><div id="familyVehicleState" style="font-size:12px;color:var(--muted);margin:8px 0"></div><button class="action-btn primary" id="publishVehicle">☁️ นำรถคันนี้ขึ้น Cloud</button><button class="action-btn" id="syncVehicleNow" style="display:none;margin-top:8px">🔄 ซิงก์ตอนนี้</button></div>
        <div class="tool-card" id="ownerShareBox" style="display:none;margin:0 0 12px"><div class="tool-title">เชิญสมาชิก</div><div style="font-size:11px;color:var(--muted);margin:6px 0 10px">สร้างรหัสแล้วส่งให้สมาชิก รหัสมีอายุ 7 วัน</div><button class="action-btn primary" id="createInvite">สร้างรหัสเชิญ</button><div id="inviteResult" style="margin-top:10px"></div><div id="memberList" style="margin-top:12px"></div></div>
        <div class="tool-card" style="margin:0"><div class="tool-title">เข้าร่วมรถของคนอื่น</div><div style="display:flex;gap:8px;margin-top:10px"><input id="joinCode" maxlength="10" placeholder="กรอกรหัสเชิญ" style="flex:1;text-transform:uppercase;background:var(--surface2);border:1px solid var(--line);border-radius:10px;padding:10px;color:var(--text)"><button class="action-btn primary" id="joinVehicle" style="flex:none">เข้าร่วม</button></div></div>
      </div>
    </div>`;
    document.body.appendChild(overlay);

    const settingsModal = E('settingsOverlay')?.querySelector('.modal');
    if(settingsModal){
      const block = document.createElement('div');
      block.innerHTML = `<div style="height:1px;background:var(--line);margin:18px 0"></div><button class="action-btn primary" id="openFamily" style="width:100%">👨‍👩‍👧‍👦 แชร์ยานพาหนะกับครอบครัว</button><div id="familyMiniStatus" style="font-size:11px;color:var(--muted);margin-top:7px"></div>`;
      settingsModal.insertBefore(block, settingsModal.querySelector('.save-row'));
    }

    const style = document.createElement('style');
    style.textContent = `.cloud-chip{font-size:9px;margin-left:4px;padding:2px 5px;border-radius:9px;background:rgba(61,174,140,.15);color:var(--teal)}.role-chip{font-size:10px;border-radius:999px;padding:3px 7px;background:var(--surface2);color:var(--muted)}.member-row{display:flex;justify-content:space-between;align-items:center;padding:8px 0;border-bottom:1px solid var(--line);font-size:12px}.invite-code{font-size:24px;font-weight:800;letter-spacing:4px;color:var(--amber);text-align:center;padding:12px;background:var(--surface2);border-radius:12px}`;
    document.head.appendChild(style);

    E('openFamily')?.addEventListener('click', openFamily);
    E('closeFamily')?.addEventListener('click', () => overlay.style.display='none');
    overlay.addEventListener('click', e => { if(e.target === overlay) overlay.style.display='none'; });
    E('familyLogin')?.addEventListener('click', signIn);
    E('familyLogout')?.addEventListener('click', () => auth?.signOut());
    E('publishVehicle')?.addEventListener('click', publishCurrentVehicle);
    E('syncVehicleNow')?.addEventListener('click', () => syncCurrentVehicle(true));
    E('createInvite')?.addEventListener('click', createInvite);
    E('joinVehicle')?.addEventListener('click', joinVehicle);
  }

  function updateUI(){
    const mini = E('familyMiniStatus');
    if(mini) mini.textContent = !configured ? 'ยังไม่ได้ตั้งค่า Firebase' : user ? `เชื่อมต่อ Cloud: ${user.email}` : 'ยังไม่ได้เข้าสู่ระบบ Family Sharing';
    if(!E('familyOverlay')) return;
    E('familyConfigWarning').style.display = configured ? 'none' : 'block';
    E('familyConfigWarning').innerHTML = 'ยังไม่ได้ใส่ค่า Firebase Config กรุณาทำตามไฟล์ <b>FIREBASE-SETUP-TH.md</b> แล้วแก้ไฟล์ <b>firebase-config.js</b>';
    E('familyLogin').style.display = user ? 'none' : '';
    E('familyLogout').style.display = user ? '' : 'none';
    E('familyCloudTools').style.display = user ? '' : 'none';
    E('familyUser').innerHTML = user ? `<b>${escFS(user.displayName || user.email)}</b><br>${escFS(user.email || '')}` : 'ยังไม่ได้เข้าสู่ระบบ';
    const v = selectedVehicle();
    if(user && v){
      E('familyVehicleState').innerHTML = isCloudVehicle(v) ? `☁️ ซิงก์ร่วมกันแล้ว • สิทธิ์ <b>${escFS(role() || 'member')}</b>` : '📱 รถคันนี้เก็บอยู่ในเครื่องเท่านั้น';
      E('publishVehicle').style.display = isCloudVehicle(v) ? 'none' : '';
      E('syncVehicleNow').style.display = isCloudVehicle(v) && canEdit() ? '' : 'none';
      E('ownerShareBox').style.display = isCloudVehicle(v) && canManage() ? '' : 'none';
      if(canManage()) renderMembers();
    }
  }

  function openFamily(){ E('settingsOverlay').style.display='none'; E('familyOverlay').style.display='flex'; updateUI(); }

  async function signIn(){
    if(!configured) return alert('กรุณาตั้งค่า Firebase ก่อน');
    const provider = new firebase.auth.GoogleAuthProvider();
    provider.setCustomParameters({prompt:'select_account'});
    try { await auth.signInWithPopup(provider); }
    catch(e){ if(e.code === 'auth/popup-blocked' || e.code === 'auth/cancelled-popup-request') await auth.signInWithRedirect(provider); else alert('เข้าสู่ระบบไม่สำเร็จ: '+e.message); }
  }

  async function loadCloudVehicles(){
    if(!user) return;
    const links = await db.collection('users').doc(user.uid).collection('vehicles').get();
    for(const link of links.docs){
      const vid = link.id, data = link.data();
      const vd = await db.collection('vehicles').doc(vid).get();
      if(!vd.exists) continue;
      const cloud = vd.data();
      roleByVehicle.set(vid, data.role || 'viewer');
      let local = vehicles.find(v => v.cloudId === vid || v.id === vid);
      if(!local){ local = {id: vid, cloudId: vid, name: cloud.name || 'รถที่แชร์', cloudRole:data.role}; vehicles.push(local); }
      else { local.cloudId=vid; local.cloudRole=data.role; if(cloud.name) local.name=cloud.name; }
    }
    persistVehicles();
    render();
    patchVehicleChips();
    subscribeCurrentCloudVehicle();
  }

  function patchVehicleChips(){
    document.querySelectorAll('.vehicle-chip').forEach(btn => {
      const v = vehicles.find(x => x.id === btn.dataset.id);
      if(v?.cloudId && !btn.querySelector('.cloud-chip')) btn.insertAdjacentHTML('beforeend', '<span class="cloud-chip">CLOUD</span>');
    });
  }

  async function publishCurrentVehicle(){
    if(!user) return signIn();
    const v = selectedVehicle(); if(!v) return;
    if(v.cloudId) return;
    if(!confirm(`นำ “${v.name}” พร้อมข้อมูลเติมน้ำมันและค่าใช้จ่ายขึ้น Cloud เพื่อแชร์กับครอบครัว?`)) return;
    const vid = v.id;
    const batch = db.batch();
    batch.set(db.collection('vehicles').doc(vid), {name:v.name, ownerId:user.uid, createdAt:firebase.firestore.FieldValue.serverTimestamp(), updatedAt:firebase.firestore.FieldValue.serverTimestamp()});
    batch.set(db.collection('vehicles').doc(vid).collection('members').doc(user.uid), {uid:user.uid,email:user.email||'',displayName:user.displayName||'',role:'owner',joinedAt:firebase.firestore.FieldValue.serverTimestamp()});
    batch.set(db.collection('users').doc(user.uid).collection('vehicles').doc(vid), {vehicleId:vid,role:'owner',name:v.name,updatedAt:firebase.firestore.FieldValue.serverTimestamp()});
    await batch.commit();
    v.cloudId=vid; v.cloudRole='owner'; roleByVehicle.set(vid,'owner'); persistVehicles();
    await syncCurrentVehicle(true); subscribeCurrentCloudVehicle(); render(); updateUI(); toastFS('เปิด Family Sharing แล้ว');
  }

  async function syncCollection(vid, name, items){
    const ref = db.collection('vehicles').doc(vid).collection(name);
    const existing = await ref.get();
    const keep = new Set(items.map(x => String(x.id)));
    let batch = db.batch(), n=0;
    const flush = async()=>{ if(n){ await batch.commit(); batch=db.batch(); n=0; } };
    for(const d of existing.docs){ if(!keep.has(d.id)){ batch.delete(d.ref); if(++n>=400) await flush(); } }
    for(const item of items){
      const data = clean({...item, vehicleId:vid, updatedBy:user.uid, updatedByName:user.displayName||user.email||'', updatedAt:firebase.firestore.FieldValue.serverTimestamp()});
      batch.set(ref.doc(String(item.id)), data, {merge:true}); if(++n>=400) await flush();
    }
    await flush();
  }

  async function syncCurrentVehicle(manual=false){
    if(applyingRemote || !user) return;
    const v=selectedVehicle(); if(!v?.cloudId || !canEdit()) return;
    try{
      await Promise.all([
        syncCollection(v.cloudId,'entries',entries.filter(x=>x.vehicleId===v.id)),
        syncCollection(v.cloudId,'costs',costs.filter(x=>x.vehicleId===v.id)),
        syncCollection(v.cloudId,'reminders',reminders.filter(x=>x.vehicleId===v.id || !x.vehicleId).map(x=>({...x,vehicleId:v.id})))
      ]);
      await db.collection('vehicles').doc(v.cloudId).set({name:v.name,updatedAt:firebase.firestore.FieldValue.serverTimestamp()},{merge:true});
      if(manual) toastFS('ซิงก์ข้อมูลแล้ว');
    }catch(e){ console.error(e); if(manual) alert('ซิงก์ไม่สำเร็จ: '+e.message); }
  }

  function scheduleSync(){ clearTimeout(syncTimer); syncTimer=setTimeout(()=>syncCurrentVehicle(false),800); }

  function clearSubscriptions(){ unsubscribers.forEach(fn=>{try{fn()}catch{}}); unsubscribers=[]; }
  function subscribeCurrentCloudVehicle(){
    clearSubscriptions();
    const v=selectedVehicle(); if(!user || !v?.cloudId) return;
    const apply = async(name,snap)=>{
      applyingRemote=true;
      const docs=snap.docs.map(d=>({...d.data(),id:d.id,vehicleId:v.id}));
      if(name==='entries'){ entries=entries.filter(x=>x.vehicleId!==v.id).concat(docs); localStorage.setItem('fuel-entries',JSON.stringify(entries)); }
      if(name==='costs'){ costs=costs.filter(x=>x.vehicleId!==v.id).concat(docs); localStorage.setItem('fuel-costs',JSON.stringify(costs)); }
      if(name==='reminders'){ reminders=reminders.filter(x=>x.vehicleId!==v.id).concat(docs); localStorage.setItem('fuel-reminders',JSON.stringify(reminders)); }
      render(); patchVehicleChips(); applyingRemote=false;
    };
    ['entries','costs','reminders'].forEach(name=>unsubscribers.push(db.collection('vehicles').doc(v.cloudId).collection(name).onSnapshot(s=>apply(name,s),e=>console.warn('family snapshot',e))));
  }

  async function createInvite(){
    const v=selectedVehicle(); if(!user || !v?.cloudId || !canManage()) return;
    const chars='ABCDEFGHJKLMNPQRSTUVWXYZ23456789'; let code=''; for(let i=0;i<8;i++) code+=chars[Math.floor(Math.random()*chars.length)];
    const expiresAt=firebase.firestore.Timestamp.fromDate(new Date(Date.now()+7*864e5));
    await db.collection('invites').doc(code).set({code,vehicleId:v.cloudId,vehicleName:v.name,ownerId:user.uid,role:'editor',active:true,expiresAt,createdAt:firebase.firestore.FieldValue.serverTimestamp()});
    E('inviteResult').innerHTML=`<div class="invite-code">${code}</div><button class="action-btn" id="copyInvite" style="width:100%;margin-top:8px">คัดลอกรหัส</button>`;
    E('copyInvite').onclick=async()=>{await navigator.clipboard.writeText(code);toastFS('คัดลอกรหัสแล้ว')};
  }

  async function joinVehicle(){
    if(!user) return signIn();
    const code=E('joinCode').value.trim().toUpperCase(); if(!code) return;
    try{
      const invRef=db.collection('invites').doc(code), inv=await invRef.get();
      if(!inv.exists) throw new Error('ไม่พบรหัสเชิญ'); const d=inv.data();
      if(!d.active || d.expiresAt?.toMillis() < Date.now()) throw new Error('รหัสหมดอายุหรือถูกยกเลิกแล้ว');
      const batch=db.batch();
      batch.set(db.collection('vehicles').doc(d.vehicleId).collection('members').doc(user.uid),{uid:user.uid,email:user.email||'',displayName:user.displayName||'',role:d.role||'editor',inviteCode:code,joinedAt:firebase.firestore.FieldValue.serverTimestamp()});
      batch.set(db.collection('users').doc(user.uid).collection('vehicles').doc(d.vehicleId),{vehicleId:d.vehicleId,role:d.role||'editor',name:d.vehicleName||'รถที่แชร์',updatedAt:firebase.firestore.FieldValue.serverTimestamp()});
      await batch.commit(); E('joinCode').value=''; await loadCloudVehicles(); toastFS('เข้าร่วมรถแล้ว'); updateUI();
    }catch(e){alert('เข้าร่วมไม่สำเร็จ: '+e.message)}
  }

  async function renderMembers(){
    const v=selectedVehicle(); if(!v?.cloudId || !canManage()) return;
    const snap=await db.collection('vehicles').doc(v.cloudId).collection('members').get();
    E('memberList').innerHTML='<div class="tool-title">สมาชิก</div>'+snap.docs.map(d=>{const m=d.data();return `<div class="member-row"><span>${escFS(m.displayName||m.email||'สมาชิก')}<br><small style="color:var(--muted)">${escFS(m.email||'')}</small></span><span class="role-chip">${escFS(m.role)}</span></div>`}).join('');
  }

  function wrapPersistence(){
    const oldPersist=window.persist || persist; persist = function(){ oldPersist.apply(this,arguments); scheduleSync(); };
    const oldCosts=window.persistCosts || persistCosts; persistCosts = function(){ oldCosts.apply(this,arguments); scheduleSync(); };
    const oldVehicles=window.persistVehicles || persistVehicles; persistVehicles = function(){ oldVehicles.apply(this,arguments); scheduleSync(); };
    const oldRem=window.persistReminders || persistReminders; persistReminders = function(){ oldRem.apply(this,arguments); scheduleSync(); };
    const oldRender=window.render || render; render = function(){ oldRender.apply(this,arguments); patchVehicleChips(); updateUI(); };
    document.addEventListener('click',e=>{ if(e.target.closest('.vehicle-chip')) setTimeout(subscribeCurrentCloudVehicle,0); });
  }

  async function init(){
    injectUI(); wrapPersistence(); updateUI();
    if(!configured || !window.firebase){ updateUI(); return; }
    try{
      if(!firebase.apps.length) firebase.initializeApp(cfg);
      auth=firebase.auth(); db=firebase.firestore();
      try{ await db.enablePersistence({synchronizeTabs:true}); }catch(e){ console.info('Firestore persistence:',e.code); }
      auth.onAuthStateChanged(async u=>{ user=u; updateUI(); if(u) await loadCloudVehicles(); else {roleByVehicle.clear();clearSubscriptions();} });
    }catch(e){ console.error(e); E('familyConfigWarning').style.display='block';E('familyConfigWarning').textContent='Firebase เริ่มทำงานไม่สำเร็จ: '+e.message; }
  }

  if(document.readyState==='loading') document.addEventListener('DOMContentLoaded',init); else init();
})();
