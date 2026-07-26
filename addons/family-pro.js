import { firebaseConfig } from '../firebase-config.js?v=5.1.1';
import { initializeApp } from 'https://www.gstatic.com/firebasejs/12.13.0/firebase-app.js';
import {
  getAuth, GoogleAuthProvider, signInWithPopup, signInWithRedirect, getRedirectResult,
  signOut, onAuthStateChanged, setPersistence, browserLocalPersistence
} from 'https://www.gstatic.com/firebasejs/12.13.0/firebase-auth.js';
import {
  getFirestore, doc, setDoc, getDoc, collection, getDocs, deleteDoc,
  writeBatch, serverTimestamp, onSnapshot
} from 'https://www.gstatic.com/firebasejs/12.13.0/firebase-firestore.js';
import {
  getStorage, ref, uploadBytes, getDownloadURL, deleteObject
} from 'https://www.gstatic.com/firebasejs/12.13.0/firebase-storage.js';

const B = window.FuelLogBridge;
if (!B) throw new Error('FuelLogBridge not found');

const app = initializeApp(firebaseConfig);
const auth = getAuth(app);
const db = getFirestore(app);
const storage = getStorage(app);
setPersistence(auth, browserLocalPersistence).catch(() => {});

let user = null;
let tab = 'family';
let unsubscribeVehicle = null;
let ocrResult = {};

const $ = (s) => document.querySelector(s);
const esc = (v) => String(v ?? '').replace(/[&<>"']/g, (c) => ({
  '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
}[c]));
const vehicle = () => B.vehicles.find((v) => v.id === B.currentVehicleId) || { id: B.currentVehicleId, name: 'รถ' };
const tripsKey = () => `fuel-trips-${B.currentVehicleId}`;
const getTrips = () => JSON.parse(localStorage.getItem(tripsKey()) || '[]');
const saveTrips = (x) => localStorage.setItem(tripsKey(), JSON.stringify(x));
const money = (n) => `฿${Number(n || 0).toLocaleString('th-TH', { maximumFractionDigits: 2 })}`;

function ensureUI() {
  if ($('#familyFab')) return;
  document.body.insertAdjacentHTML('beforeend', `
    <button id="familyFab" title="Family & Pro" aria-label="Family & Pro">👥</button>
    <div id="familyOverlay" class="fl-overlay" aria-hidden="true">
      <section class="fl-sheet">
        <header class="fl-head">
          <div><h2>Family & Pro</h2><div class="fl-muted">แชร์รถ · ทริป · รูป · OCR · Cloud</div></div>
          <button class="fl-close" id="familyClose" aria-label="ปิด">×</button>
        </header>
        <div class="fl-tabs">
          <button data-ftab="family">ครอบครัว</button>
          <button data-ftab="trip">ทริป</button>
          <button data-ftab="gallery">รูป</button>
          <button data-ftab="ocr">OCR</button>
          <button data-ftab="cloud">Cloud</button>
        </div>
        <div id="familyBody"></div>
      </section>
    </div>`);

  $('#familyFab').onclick = open;
  $('#familyClose').onclick = close;
  $('#familyOverlay').onclick = (e) => { if (e.target.id === 'familyOverlay') close(); };
  document.querySelectorAll('[data-ftab]').forEach((button) => {
    button.onclick = () => { tab = button.dataset.ftab; render(); };
  });
}

function open() {
  $('#familyOverlay').classList.add('open');
  $('#familyOverlay').setAttribute('aria-hidden', 'false');
  render();
}
function close() {
  $('#familyOverlay').classList.remove('open');
  $('#familyOverlay').setAttribute('aria-hidden', 'true');
}
function markActiveTab() {
  document.querySelectorAll('[data-ftab]').forEach((x) => x.classList.toggle('active', x.dataset.ftab === tab));
}

function googleLogo() {
  return `<svg class="fl-google-logo" viewBox="0 0 24 24" aria-hidden="true"><path fill="#4285F4" d="M21.6 12.23c0-.74-.07-1.45-.19-2.14H12v4.05h5.38a4.6 4.6 0 0 1-2 3.02v2.62h3.24c1.9-1.75 2.98-4.33 2.98-7.55Z"/><path fill="#34A853" d="M12 22c2.7 0 4.97-.9 6.63-2.42l-3.24-2.62c-.9.6-2.05.96-3.39.96-2.61 0-4.82-1.76-5.61-4.13H3.04v2.7A10 10 0 0 0 12 22Z"/><path fill="#FBBC05" d="M6.39 13.79A6.02 6.02 0 0 1 6.08 12c0-.62.11-1.22.31-1.79V7.5H3.04A10 10 0 0 0 2 12c0 1.61.38 3.14 1.04 4.5l3.35-2.71Z"/><path fill="#EA4335" d="M12 6.08c1.47 0 2.8.51 3.84 1.5l2.87-2.88A9.62 9.62 0 0 0 12 2a10 10 0 0 0-8.96 5.5l3.35 2.71C7.18 7.84 9.39 6.08 12 6.08Z"/></svg>`;
}

function familyView() {
  const v = vehicle();
  return `<div class="fl-grid">
    <div class="fl-card full fl-auth-card">
      ${user ? `<div class="fl-account-panel"><div class="fl-user"><img src="${esc(user.photoURL || '')}" referrerpolicy="no-referrer"><div><div class="fl-account-name">${esc(user.displayName || user.email)}</div><div class="fl-muted">${esc(user.email)}</div><span class="fl-connected-badge">● เชื่อมต่อแล้ว</span></div></div><button class="fl-btn fl-signout-btn" id="flSignOut">ออกจากระบบ</button></div>` : `<div class="fl-auth-intro"><div class="fl-auth-copy"><div class="fl-auth-title">Google Cloud Sync</div><div class="fl-muted">ซิงก์ข้อมูลระหว่างมือถือและแชร์รถกับคนในบ้าน</div></div><button type="button" class="fl-google-btn" id="flLogin">${googleLogo()}<span>เข้าสู่ระบบด้วย Google</span></button><div id="flAuthStatus" class="fl-auth-status" role="status" aria-live="polite"></div><div class="fl-auth-benefits"><span>✓ ข้อมูลจำกัดสิทธิ์ตามสมาชิก</span><span>✓ รูปเก็บใน Firebase Storage</span><span>✓ จำบัญชีบนอุปกรณ์นี้</span></div></div>`}
    </div>
    <div class="fl-card"><h3>รถที่เลือก</h3><div class="fl-kpi">${esc(v.name)}</div><div class="fl-muted">${esc(v.id)}</div></div>
    <div class="fl-card"><h3>สิทธิ์ของคุณ</h3><div id="cloudState" class="fl-kpi">—</div><div class="fl-muted">อัปเดตแบบเรียลไทม์</div></div>
    <div class="fl-card full"><h3>สมาชิก</h3><div id="memberList" class="fl-list"><div class="fl-muted">เข้าสู่ระบบเพื่อดูสมาชิก</div></div></div>
    <div class="fl-card full"><h3>สร้างรหัสเชิญ</h3><div class="fl-row"><input id="inviteEmail" class="fl-input" type="email" placeholder="Gmail สมาชิก"><select id="inviteRole" class="fl-input"><option value="editor">Editor — เพิ่ม/แก้ไข</option><option value="viewer">Viewer — ดูอย่างเดียว</option></select><button id="makeInvite" class="fl-btn primary">สร้าง</button></div><div id="inviteResult" class="fl-muted" style="margin-top:8px"></div></div>
    <div class="fl-card full"><h3>เข้าร่วมด้วยรหัส</h3><div class="fl-row"><input id="joinCode" class="fl-input" maxlength="8" placeholder="รหัสเชิญ 8 ตัว"><button id="joinInvite" class="fl-btn primary">เข้าร่วม</button></div></div>
  </div>`;
}

function tripView() {
  const trips = getTrips().sort((a, b) => String(b.date).localeCompare(String(a.date)));
  const total = trips.reduce((sum, x) => sum + (+x.fuel || 0) + (+x.toll || 0) + (+x.parking || 0) + (+x.food || 0) + (+x.other || 0), 0);
  return `<div class="fl-grid">
    <div class="fl-card"><h3>จำนวนทริป</h3><div class="fl-kpi">${trips.length}</div></div>
    <div class="fl-card"><h3>ต้นทุนรวม</h3><div class="fl-kpi">${money(total)}</div></div>
    <div class="fl-card full"><h3>เพิ่มทริป/หน้างาน</h3><div class="fl-grid">
      <input id="tName" class="fl-input" placeholder="ชื่องาน/ปลายทาง">
      <input id="tDate" class="fl-input" type="date" value="${new Date().toISOString().slice(0, 10)}">
      <input id="tFuel" class="fl-input" type="number" placeholder="ค่าน้ำมัน">
      <input id="tToll" class="fl-input" type="number" placeholder="ค่าทางด่วน">
      <input id="tParking" class="fl-input" type="number" placeholder="ค่าจอด">
      <input id="tFood" class="fl-input" type="number" placeholder="ค่าอาหาร/ที่พัก">
      <input id="tOther" class="fl-input" type="number" placeholder="อื่น ๆ">
      <input id="tNote" class="fl-input" placeholder="หมายเหตุ">
      <button id="saveTrip" class="fl-btn primary">บันทึกทริป</button>
    </div></div>
    <div class="fl-card full"><h3>ประวัติทริป</h3><div class="fl-list">${trips.length ? trips.map((x) => {
      const sum = (+x.fuel || 0) + (+x.toll || 0) + (+x.parking || 0) + (+x.food || 0) + (+x.other || 0);
      return `<div class="fl-item"><div><b>${esc(x.name)}</b><div class="fl-muted">${esc(x.date)}${x.note ? ` · ${esc(x.note)}` : ''}</div></div><div><b>${money(sum)}</b><button class="fl-close" data-deltrip="${esc(x.id)}" style="font-size:16px">×</button></div></div>`;
    }).join('') : '<div class="fl-muted">ยังไม่มีทริป</div>'}</div></div>
  </div>`;
}

function galleryView() {
  return `<div class="fl-grid">
    <div class="fl-card full"><h3>อัปโหลดรูปและเอกสาร</h3><div class="fl-row"><select id="photoType" class="fl-input"><option value="receipt">ใบเสร็จ</option><option value="odometer">เรือนไมล์</option><option value="maintenance">งานซ่อม</option><option value="documents">เอกสารรถ</option></select><input id="cloudPhoto" class="fl-input" type="file" accept="image/*,application/pdf"><button id="uploadPhoto" class="fl-btn primary">อัปโหลด</button></div><div id="photoMsg" class="fl-muted" style="margin-top:8px"></div></div>
    <div class="fl-card full"><div class="fl-between"><h3>คลังรูปของรถคันนี้</h3><button id="refreshGallery" class="fl-btn">รีเฟรช</button></div><div id="galleryList" class="fl-gallery"><div class="fl-muted">เข้าสู่ระบบเพื่อโหลดรูป</div></div></div>
  </div>`;
}

function ocrView() {
  const r = ocrResult;
  return `<div class="fl-grid">
    <div class="fl-card full"><h3>OCR ใบเสร็จ / เรือนไมล์</h3><p class="fl-muted">ประมวลผลบนเครื่องด้วย Tesseract.js รูปไม่ถูกส่งไปบริการ OCR ภายนอก</p><div class="fl-row"><select id="ocrMode" class="fl-input"><option value="receipt">ใบเสร็จน้ำมัน</option><option value="odometer">เรือนไมล์</option></select><input id="ocrFile" class="fl-input" type="file" accept="image/*" capture="environment"><button id="runOcr" class="fl-btn primary">เริ่มอ่าน</button></div><div class="fl-progress"><span id="ocrProgress"></span></div><div id="ocrStatus" class="fl-muted"></div></div>
    <div class="fl-card full"><h3>ผลที่ตรวจพบ</h3><div class="fl-grid">
      <label class="fl-field">เลขไมล์<input id="ocrOdo" class="fl-input" type="number" value="${esc(r.odometer || '')}"></label>
      <label class="fl-field">ยอดรวม<input id="ocrTotal" class="fl-input" type="number" value="${esc(r.total || '')}"></label>
      <label class="fl-field">ลิตร<input id="ocrLiters" class="fl-input" type="number" step="0.01" value="${esc(r.liters || '')}"></label>
      <label class="fl-field">ราคา/ลิตร<input id="ocrPrice" class="fl-input" type="number" step="0.01" value="${esc(r.price || '')}"></label>
      <label class="fl-field">ปั๊ม<input id="ocrStation" class="fl-input" value="${esc(r.station || '')}"></label>
      <label class="fl-field">วันที่<input id="ocrDate" class="fl-input" type="date" value="${esc(r.date || '')}"></label>
    </div><div class="fl-row" style="margin-top:10px"><button id="applyOcr" class="fl-btn primary">นำไปใส่ฟอร์มเติมน้ำมัน</button><button id="showOcrText" class="fl-btn">ดูข้อความดิบ</button></div><pre id="ocrRaw" class="fl-raw" hidden>${esc(r.raw || '')}</pre></div>
  </div>`;
}

function cloudView() {
  return `<div class="fl-grid">
    <div class="fl-card full"><h3>ซิงก์รถที่เลือก</h3><p class="fl-muted">รวมรายการเติมน้ำมัน ค่าใช้จ่าย งานบำรุง และทริป โดยข้อมูลในเครื่องยังคงอยู่</p><div class="fl-row"><button id="pushCloud" class="fl-btn primary">อัปโหลดขึ้น Cloud</button><button id="pullCloud" class="fl-btn">ดึงจาก Cloud</button></div><div id="syncMsg" class="fl-muted" style="margin-top:8px"></div></div>
    <div class="fl-card"><h3>รายการในเครื่อง</h3><div class="fl-kpi">${B.entries.filter((x) => x.vehicleId === B.currentVehicleId).length}</div><div class="fl-muted">รายการเติมน้ำมัน</div></div>
    <div class="fl-card"><h3>ค่าใช้จ่าย</h3><div class="fl-kpi">${B.costs.filter((x) => x.vehicleId === B.currentVehicleId).length}</div><div class="fl-muted">รายการอื่น ๆ</div></div>
    <div class="fl-card full"><h3>สถานะ</h3><div class="fl-muted">Firebase Auth จำบัญชีในเครื่อง · Firestore ซิงก์ข้อมูล · Storage จำกัดสิทธิ์ตามสมาชิก</div></div>
  </div>`;
}

function render() {
  markActiveTab();
  const body = $('#familyBody');
  if (!body) return;
  body.innerHTML = tab === 'family' ? familyView() : tab === 'trip' ? tripView() : tab === 'gallery' ? galleryView() : tab === 'ocr' ? ocrView() : cloudView();
  bindView();
  if (tab === 'family' && user) watchVehicle();
  if (tab === 'gallery' && user) loadGallery();
}

function authErrorText(error) {
  const code = error?.code || '';
  if (code === 'auth/unauthorized-domain') return 'โดเมนนี้ยังไม่ได้รับอนุญาต: ไปที่ Firebase > Authentication > Settings > Authorized domains แล้วเพิ่ม songsit2017.github.io';
  if (code === 'auth/popup-blocked') return 'เบราว์เซอร์บล็อกหน้าต่างเข้าสู่ระบบ กำลังเปลี่ยนไปใช้การเข้าสู่ระบบแบบเปลี่ยนหน้า';
  if (code === 'auth/popup-closed-by-user') return 'หน้าต่างเข้าสู่ระบบถูกปิดก่อนเสร็จ';
  if (code === 'auth/operation-not-allowed') return 'ยังไม่ได้เปิด Google Sign-in ใน Firebase Authentication';
  if (code === 'auth/network-request-failed') return 'เชื่อมต่อ Google ไม่สำเร็จ กรุณาตรวจอินเทอร์เน็ตแล้วลองใหม่';
  if (code === 'auth/api-key-not-valid') {
    const key = String(firebaseConfig?.apiKey || '');
    return `Firebase API key ใช้งานไม่ได้ (โหลดคีย์ลงท้าย ${key.slice(-6) || 'ไม่มีค่า'}) กรุณาตรวจ API key และข้อจำกัดของคีย์ใน Google Cloud Console`;
  }
  return error?.message || 'เข้าสู่ระบบไม่สำเร็จ';
}

function setLoginStatus(message, isError = false) {
  const status = $('#flAuthStatus');
  if (status) {
    status.textContent = message || '';
    status.classList.toggle('fl-error', !!isError);
    status.classList.toggle('show', !!message);
  }
  const button = $('#flLogin');
  if (button) {
    const loading = !!message && !isError;
    button.disabled = loading;
    button.classList.toggle('loading', loading);
    button.innerHTML = loading
      ? `<span class="fl-login-spinner" aria-hidden="true"></span><span>${esc(message || 'กำลังเชื่อมต่อ…')}</span>`
      : `${googleLogo()}<span>เข้าสู่ระบบด้วย Google</span>`;
  }
}

function isStandaloneApp() {
  return window.matchMedia('(display-mode: standalone)').matches || window.navigator.standalone === true;
}

async function loginGoogle() {
  const provider = new GoogleAuthProvider();
  provider.setCustomParameters({ prompt: 'select_account' });
  setLoginStatus('กำลังเปิด Google…');
  try {
    if (isStandaloneApp()) {
      await signInWithRedirect(auth, provider);
      return;
    }
    await signInWithPopup(auth, provider);
    setLoginStatus('');
  } catch (error) {
    const fallbackCodes = ['auth/popup-blocked', 'auth/cancelled-popup-request', 'auth/operation-not-supported-in-this-environment'];
    if (fallbackCodes.includes(error?.code)) {
      setLoginStatus('กำลังเปลี่ยนไปหน้าล็อกอิน Google…');
      try {
        await signInWithRedirect(auth, provider);
        return;
      } catch (redirectError) {
        setLoginStatus(authErrorText(redirectError), true);
        return;
      }
    }
    setLoginStatus(authErrorText(error), true);
  }
}

function bindView() {
  $('#flLogin')?.addEventListener('click', loginGoogle);
  $('#flSignOut')?.addEventListener('click', () => signOut(auth));
  $('#saveTrip')?.addEventListener('click', saveTrip);
  document.querySelectorAll('[data-deltrip]').forEach((b) => b.onclick = () => {
    saveTrips(getTrips().filter((x) => x.id !== b.dataset.deltrip));
    render();
  });
  $('#makeInvite')?.addEventListener('click', makeInvite);
  $('#joinInvite')?.addEventListener('click', joinInvite);
  $('#pushCloud')?.addEventListener('click', pushCloud);
  $('#pullCloud')?.addEventListener('click', pullCloud);
  $('#uploadPhoto')?.addEventListener('click', uploadPhoto);
  $('#refreshGallery')?.addEventListener('click', loadGallery);
  $('#runOcr')?.addEventListener('click', runOcr);
  $('#applyOcr')?.addEventListener('click', applyOcr);
  $('#showOcrText')?.addEventListener('click', () => { const p = $('#ocrRaw'); p.hidden = !p.hidden; });
  document.querySelectorAll('[data-remove-member]').forEach((b) => b.onclick = () => removeMember(b.dataset.removeMember));
  document.querySelectorAll('[data-photo-delete]').forEach((b) => b.onclick = () => deletePhoto(b.dataset.photoDelete, b.dataset.photoPath));
}

function saveTrip() {
  const name = $('#tName').value.trim();
  if (!name) return alert('กรุณาใส่ชื่อทริป');
  const trip = {
    id: `trip-${Date.now()}`,
    vehicleId: B.currentVehicleId,
    name,
    date: $('#tDate').value,
    fuel: +$('#tFuel').value || 0,
    toll: +$('#tToll').value || 0,
    parking: +$('#tParking').value || 0,
    food: +$('#tFood').value || 0,
    other: +$('#tOther').value || 0,
    note: $('#tNote').value.trim(),
    createdBy: user?.uid || 'local'
  };
  const trips = getTrips();
  trips.push(trip);
  saveTrips(trips);
  render();
}

async function ensureUser() {
  if (!user) throw new Error('กรุณาเข้าสู่ระบบ Google ก่อน');
  await setDoc(doc(db, 'users', user.uid), {
    email: user.email,
    emailLower: user.email.toLowerCase(),
    displayName: user.displayName || '',
    photoURL: user.photoURL || '',
    updatedAt: serverTimestamp()
  }, { merge: true });
}
const vehicleRef = () => doc(db, 'vehicles', B.currentVehicleId);

async function ensureOwnerVehicle() {
  await ensureUser();
  const vr = vehicleRef();
  const snap = await getDoc(vr);
  if (!snap.exists()) {
    await setDoc(vr, {
      name: vehicle().name,
      ownerUid: user.uid,
      members: { [user.uid]: { role: 'owner', email: user.email, displayName: user.displayName || '' } },
      createdAt: serverTimestamp()
    });
  }
  return getDoc(vr);
}

function watchVehicle() {
  unsubscribeVehicle?.();
  unsubscribeVehicle = onSnapshot(vehicleRef(), (snap) => {
    const state = $('#cloudState');
    const list = $('#memberList');
    if (!state || !list) return;
    if (!snap.exists()) {
      state.textContent = 'ยังไม่ขึ้น Cloud';
      list.innerHTML = '<div class="fl-muted">กดอัปโหลดขึ้น Cloud ก่อนสร้างสมาชิก</div>';
      return;
    }
    const data = snap.data();
    const role = data.members?.[user.uid]?.role || 'ไม่มีสิทธิ์';
    state.textContent = role;
    list.innerHTML = Object.entries(data.members || {}).map(([uid, m]) => `<div class="fl-item"><div><b>${esc(m.displayName || m.email || uid)}</b><div class="fl-muted">${esc(m.email || '')}</div></div><div><span class="fl-role">${esc(m.role)}</span>${data.ownerUid === user.uid && uid !== user.uid ? `<button class="fl-close" data-remove-member="${esc(uid)}" title="นำสมาชิกออก">×</button>` : ''}</div></div>`).join('') || '<div class="fl-muted">ยังไม่มีสมาชิก</div>';
    bindView();
  }, () => {
    if ($('#cloudState')) $('#cloudState').textContent = 'อ่านไม่ได้';
  });
}

async function makeInvite() {
  try {
    const email = $('#inviteEmail').value.trim().toLowerCase();
    const role = $('#inviteRole').value;
    if (!email) throw new Error('ใส่ Gmail สมาชิก');
    const snap = await ensureOwnerVehicle();
    if (snap.data().ownerUid !== user.uid) throw new Error('เฉพาะ Owner สร้างรหัสเชิญได้');
    const code = Math.random().toString(36).slice(2, 10).toUpperCase();
    await setDoc(doc(db, 'invites', code), {
      vehicleId: B.currentVehicleId,
      vehicleName: vehicle().name,
      emailLower: email,
      role,
      ownerUid: user.uid,
      expiresAt: Date.now() + 7 * 864e5,
      createdAt: serverTimestamp()
    });
    $('#inviteResult').innerHTML = `รหัสเชิญ: <b class="fl-code">${code}</b> · อายุ 7 วัน`;
  } catch (e) { alert(e.message); }
}

async function joinInvite() {
  try {
    await ensureUser();
    const code = $('#joinCode').value.trim().toUpperCase();
    const inviteRef = doc(db, 'invites', code);
    const inviteSnap = await getDoc(inviteRef);
    if (!inviteSnap.exists()) throw new Error('ไม่พบรหัสเชิญ');
    const invite = inviteSnap.data();
    if (invite.expiresAt < Date.now()) throw new Error('รหัสหมดอายุ');
    if (invite.emailLower && invite.emailLower !== user.email.toLowerCase()) throw new Error('รหัสนี้สร้างไว้สำหรับ Gmail อื่น');
    const vr = doc(db, 'vehicles', invite.vehicleId);
    const vehicleSnap = await getDoc(vr);
    if (!vehicleSnap.exists()) throw new Error('ไม่พบรถ');
    const data = vehicleSnap.data();
    const members = { ...(data.members || {}), [user.uid]: { role: invite.role, email: user.email, displayName: user.displayName || '' } };
    await setDoc(vr, { members, lastJoinCode: code }, { merge: true });
    if (!B.vehicles.find((x) => x.id === invite.vehicleId)) {
      B.vehicles.push({ id: invite.vehicleId, name: invite.vehicleName || data.name || 'รถที่แชร์' });
      B.persistAll();
      B.refresh();
    }
    alert('เข้าร่วมรถสำเร็จ');
  } catch (e) { alert(e.message); }
}

async function removeMember(uid) {
  if (!confirm('นำสมาชิกนี้ออกจากรถใช่ไหม?')) return;
  try {
    await ensureUser();
    const snap = await getDoc(vehicleRef());
    if (!snap.exists() || snap.data().ownerUid !== user.uid) throw new Error('เฉพาะ Owner จัดการสมาชิกได้');
    const members = { ...(snap.data().members || {}) };
    delete members[uid];
    await setDoc(vehicleRef(), { members }, { merge: true });
  } catch (e) { alert(e.message); }
}

async function pushCloud() {
  const msg = $('#syncMsg');
  try {
    await ensureOwnerVehicle();
    const batch = writeBatch(db);
    const groups = [
      ['entries', B.entries.filter((x) => x.vehicleId === B.currentVehicleId)],
      ['costs', B.costs.filter((x) => x.vehicleId === B.currentVehicleId)],
      ['reminders', B.reminders.filter((x) => x.vehicleId === B.currentVehicleId)],
      ['trips', getTrips()]
    ];
    for (const [name, items] of groups) {
      for (const item of items) {
        batch.set(doc(db, 'vehicles', B.currentVehicleId, name, item.id), { ...item, updatedBy: user.uid, updatedAt: serverTimestamp() }, { merge: true });
      }
    }
    await batch.commit();
    msg.textContent = 'อัปโหลดสำเร็จ';
  } catch (e) { msg.textContent = e.message; }
}

async function pullCloud() {
  const msg = $('#syncMsg');
  try {
    await ensureUser();
    for (const [name, target] of [['entries', B.entries], ['costs', B.costs], ['reminders', B.reminders]]) {
      const snap = await getDocs(collection(db, 'vehicles', B.currentVehicleId, name));
      const index = new Map(target.map((x, i) => [x.id, i]));
      snap.forEach((d) => {
        const item = d.data();
        if (index.has(item.id)) target[index.get(item.id)] = item;
        else target.push(item);
      });
    }
    const tripSnap = await getDocs(collection(db, 'vehicles', B.currentVehicleId, 'trips'));
    saveTrips(tripSnap.docs.map((d) => d.data()));
    B.persistAll();
    B.refresh();
    msg.textContent = 'ดึงข้อมูลและรวมรายการสำเร็จ';
  } catch (e) { msg.textContent = e.message; }
}

async function uploadPhoto() {
  const msg = $('#photoMsg');
  try {
    await ensureUser();
    const f = $('#cloudPhoto').files[0];
    if (!f) throw new Error('เลือกไฟล์ก่อน');
    if (f.size > 8 * 1024 * 1024) throw new Error('ไฟล์ต้องไม่เกิน 8 MB');
    const type = $('#photoType').value;
    const id = String(Date.now());
    const safeName = f.name.replace(/[^a-zA-Z0-9._-]/g, '_');
    const path = `vehicles/${B.currentVehicleId}/${type}/${id}-${safeName}`;
    const storageRef = ref(storage, path);
    await uploadBytes(storageRef, f, { contentType: f.type, customMetadata: { uploadedBy: user.uid } });
    const url = await getDownloadURL(storageRef);
    await setDoc(doc(db, 'vehicles', B.currentVehicleId, 'photos', id), {
      id, type, path, url, name: f.name, contentType: f.type, size: f.size,
      uploadedBy: user.uid, uploadedByName: user.displayName || user.email,
      createdAt: serverTimestamp()
    });
    msg.textContent = 'อัปโหลดสำเร็จ';
    loadGallery();
  } catch (e) { msg.textContent = e.message; }
}

async function loadGallery() {
  const box = $('#galleryList');
  if (!box) return;
  box.innerHTML = '<div class="fl-muted">กำลังโหลด…</div>';
  try {
    await ensureUser();
    const snap = await getDocs(collection(db, 'vehicles', B.currentVehicleId, 'photos'));
    const items = snap.docs.map((d) => d.data()).sort((a, b) => String(b.id).localeCompare(String(a.id)));
    box.innerHTML = items.length ? items.map((x) => {
      const image = String(x.contentType || '').startsWith('image/');
      return `<article class="fl-photo">${image ? `<img src="${esc(x.url)}" loading="lazy" alt="${esc(x.name)}">` : '<div class="fl-file">📄</div>'}<div class="fl-photo-info"><b>${esc(x.name)}</b><span>${esc(x.type)} · ${esc(x.uploadedByName || '')}</span><div><a href="${esc(x.url)}" target="_blank" rel="noopener">เปิด</a><button data-photo-delete="${esc(x.id)}" data-photo-path="${esc(x.path)}">ลบ</button></div></div></article>`;
    }).join('') : '<div class="fl-muted">ยังไม่มีรูปหรือเอกสาร</div>';
    bindView();
  } catch (e) { box.innerHTML = `<div class="fl-error">${esc(e.message)}</div>`; }
}

async function deletePhoto(id, path) {
  if (!confirm('ลบไฟล์นี้ใช่ไหม?')) return;
  try {
    await deleteObject(ref(storage, path));
    await deleteDoc(doc(db, 'vehicles', B.currentVehicleId, 'photos', id));
    loadGallery();
  } catch (e) { alert(e.message); }
}

function loadTesseract() {
  if (window.Tesseract) return Promise.resolve(window.Tesseract);
  return new Promise((resolve, reject) => {
    const script = document.createElement('script');
    script.src = 'https://cdn.jsdelivr.net/npm/tesseract.js@5/dist/tesseract.min.js';
    script.onload = () => resolve(window.Tesseract);
    script.onerror = () => reject(new Error('โหลด OCR library ไม่สำเร็จ'));
    document.head.appendChild(script);
  });
}

function normalizeNumber(value) {
  if (!value) return '';
  const n = Number(String(value).replace(/,/g, '').replace(/[^0-9.]/g, ''));
  return Number.isFinite(n) ? n : '';
}

function parseOcr(text, mode) {
  const clean = text.replace(/\r/g, '');
  const lines = clean.split('\n').map((x) => x.trim()).filter(Boolean);
  const result = { raw: clean };
  const dateMatch = clean.match(/\b(20\d{2})[-/.](\d{1,2})[-/.](\d{1,2})\b/) || clean.match(/\b(\d{1,2})[-/.](\d{1,2})[-/.](20\d{2})\b/);
  if (dateMatch) result.date = dateMatch[1].length === 4 ? `${dateMatch[1]}-${dateMatch[2].padStart(2, '0')}-${dateMatch[3].padStart(2, '0')}` : `${dateMatch[3]}-${dateMatch[2].padStart(2, '0')}-${dateMatch[1].padStart(2, '0')}`;
  const odoCandidates = [...clean.matchAll(/\b(\d{4,7})\s*(?:km|กม|ก\.ม\.)?\b/gi)].map((m) => +m[1]).filter((n) => n >= 1000 && n <= 9999999);
  if (odoCandidates.length) result.odometer = Math.max(...odoCandidates);
  if (mode === 'odometer') return result;
  const literMatch = clean.match(/(?:liter|litre|liters|ลิตร|volume|ปริมาณ)\s*[:=]?\s*(\d{1,3}(?:[.,]\d{1,3})?)/i) || clean.match(/(\d{1,3}(?:[.,]\d{1,3})?)\s*(?:L|ลิตร)\b/i);
  if (literMatch) result.liters = normalizeNumber(literMatch[1].replace(',', '.'));
  const priceMatch = clean.match(/(?:price\s*\/\s*l|unit\s*price|ราคา\/?ลิตร)\s*[:=]?\s*(\d{1,3}(?:[.,]\d{1,2})?)/i);
  if (priceMatch) result.price = normalizeNumber(priceMatch[1].replace(',', '.'));
  const totalMatch = clean.match(/(?:grand\s*total|net\s*total|total|ยอดสุทธิ|รวมทั้งสิ้น|ยอดรวม)\s*[:=]?\s*(?:฿|THB)?\s*([0-9,]+(?:\.\d{1,2})?)/i);
  if (totalMatch) result.total = normalizeNumber(totalMatch[1]);
  if (!result.total) {
    const moneyCandidates = [...clean.matchAll(/(?:฿|THB)\s*([0-9,]+(?:\.\d{1,2})?)/gi)].map((m) => normalizeNumber(m[1])).filter((n) => n > 0 && n < 1000000);
    if (moneyCandidates.length) result.total = Math.max(...moneyCandidates);
  }
  const brands = ['PTT', 'ปตท', 'PT', 'Bangchak', 'บางจาก', 'Shell', 'เชลล์', 'Caltex', 'คาลเท็กซ์', 'Susco', 'ซัสโก้', 'Esso', 'เอสโซ่'];
  result.station = brands.find((b) => clean.toLowerCase().includes(b.toLowerCase())) || lines[0] || '';
  if (!result.price && result.total && result.liters) result.price = +(result.total / result.liters).toFixed(2);
  if (!result.liters && result.total && result.price) result.liters = +(result.total / result.price).toFixed(2);
  return result;
}

async function runOcr() {
  const file = $('#ocrFile').files[0];
  const status = $('#ocrStatus');
  const bar = $('#ocrProgress');
  if (!file) return alert('เลือกรูปก่อน');
  try {
    status.textContent = 'กำลังโหลดระบบ OCR…';
    const Tesseract = await loadTesseract();
    const result = await Tesseract.recognize(file, 'tha+eng', {
      logger: (m) => {
        if (m.status === 'recognizing text') bar.style.width = `${Math.round((m.progress || 0) * 100)}%`;
        status.textContent = `${m.status || 'กำลังประมวลผล'} ${m.progress ? `${Math.round(m.progress * 100)}%` : ''}`;
      }
    });
    ocrResult = parseOcr(result.data.text, $('#ocrMode').value);
    status.textContent = 'อ่านเสร็จแล้ว กรุณาตรวจสอบตัวเลขก่อนบันทึก';
    render();
  } catch (e) {
    status.textContent = e.message;
  }
}

function applyOcr() {
  ocrResult = {
    ...ocrResult,
    odometer: $('#ocrOdo').value,
    total: $('#ocrTotal').value,
    liters: $('#ocrLiters').value,
    price: $('#ocrPrice').value,
    station: $('#ocrStation').value,
    date: $('#ocrDate').value
  };
  B.applyOcr(ocrResult);
  close();
}

getRedirectResult(auth).catch((error) => {
  sessionStorage.setItem('fuellog-auth-error', authErrorText(error));
});

onAuthStateChanged(auth, async (u) => {
  user = u;
  if (u) await ensureUser().catch(() => {});
  if ($('#familyOverlay')?.classList.contains('open')) {
    render();
    const authError = sessionStorage.getItem('fuellog-auth-error');
    if (authError) {
      sessionStorage.removeItem('fuellog-auth-error');
      setLoginStatus(authError, true);
    }
  }
});

ensureUI();
