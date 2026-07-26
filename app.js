// FuelLog starts locally first. Firebase is loaded lazily so a CDN/Auth problem
// can never disable navigation, forms, theme switching, or local records.
const FIREBASE_SDK_VERSION = '12.13.0';
let app = null, auth = null, db = null, storage = null;
let GoogleAuthProvider, signInWithPopup, signInWithRedirect, getRedirectResult,
    signOut, onAuthStateChanged, setPersistence, browserLocalPersistence,
    doc, setDoc, updateDoc, getDoc, getDocs, collection, writeBatch, serverTimestamp,
    ref, uploadBytes, getDownloadURL;
let firebaseReadyPromise = null;
let firebaseLoadError = null;

async function initFirebase(){
  if(firebaseReadyPromise) return firebaseReadyPromise;
  firebaseReadyPromise = (async()=>{
    const base = `https://www.gstatic.com/firebasejs/${FIREBASE_SDK_VERSION}`;
    const [{firebaseConfig}, appMod, authMod, fireMod, storageMod] = await Promise.all([
      import('./firebase-config.js?v=5.0.4'),
      import(`${base}/firebase-app.js`),
      import(`${base}/firebase-auth.js`),
      import(`${base}/firebase-firestore.js`),
      import(`${base}/firebase-storage.js`)
    ]);

    app = appMod.initializeApp(firebaseConfig);
    auth = authMod.getAuth(app);
    db = fireMod.getFirestore(app);
    storage = storageMod.getStorage(app);

    ({GoogleAuthProvider, signInWithPopup, signInWithRedirect, getRedirectResult,
      signOut, onAuthStateChanged, setPersistence, browserLocalPersistence} = authMod);
    ({doc, setDoc, updateDoc, getDoc, getDocs, collection, writeBatch, serverTimestamp} = fireMod);
    ({ref, uploadBytes, getDownloadURL} = storageMod);

    await setPersistence(auth, browserLocalPersistence).catch(console.warn);
    getRedirectResult(auth).catch(console.warn);
    onAuthStateChanged(auth, async u=>{
      user = u;
      if(u) await ensureUser().catch(console.warn);
      if($('#panelDialog')?.open && $('#panelDialog').dataset.panel==='family') renderPanel('family');
    });
    firebaseLoadError = null;
    return true;
  })().catch(err=>{
    firebaseLoadError = err;
    console.error('FuelLog Firebase load failed:', err);
    return false;
  });
  return firebaseReadyPromise;
}

async function requireFirebase(){
  const ok = await initFirebase();
  if(!ok) throw new Error(`โหลด Firebase ไม่สำเร็จ: ${firebaseLoadError?.message || 'กรุณาตรวจอินเทอร์เน็ตแล้วลองใหม่'}`);
}

const $ = s => document.querySelector(s), $$ = s => [...document.querySelectorAll(s)];

const THAI_MONTHS = ['มกราคม','กุมภาพันธ์','มีนาคม','เมษายน','พฤษภาคม','มิถุนายน','กรกฎาคม','สิงหาคม','กันยายน','ตุลาคม','พฤศจิกายน','ธันวาคม'];
function thaiMonthLabel(dateStr){
  const d = new Date(dateStr);
  if(isNaN(d.getTime())) return '';
  return `${THAI_MONTHS[d.getMonth()]} ${d.getFullYear()}`;
}
// Color + initial badges (not a reproduction of any brand's actual logo/trademark) so entries feel recognizable at a glance.
const STATION_BRANDS = [
  { match:['ปตท','ptt'], initial:'ป', bg:'#00A19C', fg:'#fff' },
  { match:['บางจาก','bcp'], initial:'บ', bg:'#00A651', fg:'#fff' },
  { match:['เชลล์','shell'], initial:'S', bg:'#FBCE07', fg:'#D3242A' },
  { match:['เอสโซ่','esso'], initial:'E', bg:'#1B4F9C', fg:'#fff' },
  { match:['คาลเท็กซ์','caltex'], initial:'C', bg:'#DA291C', fg:'#fff' },
  { match:['พีที'], initial:'P', bg:'#EC008C', fg:'#fff' },
  { match:['ซัสโก้','susco'], initial:'ซ', bg:'#0072BC', fg:'#fff' },
];
function stationBadge(stationName){
  const name = (stationName||'').toLowerCase();
  const brand = STATION_BRANDS.find(b => b.match.some(m => name.includes(m)));
  if(brand) return `<div class="ico brand-badge" style="background:${brand.bg};color:${brand.fg}">${brand.initial}</div>`;
  return `<div class="ico">⛽</div>`;
}
const KEY = 'fuellog-v5-data';
const APP_VERSION = '6.10.0';
const memoryStore = new Map();
const store = (()=>{
  try{
    const testKey = '__fuellog_storage_test__';
    window.localStorage.setItem(testKey,'1');
    window.localStorage.removeItem(testKey);
    return window.localStorage;
  }catch{
    return {
      getItem:key=>memoryStore.has(key)?memoryStore.get(key):null,
      setItem:(key,value)=>memoryStore.set(key,String(value)),
      removeItem:key=>memoryStore.delete(key)
    };
  }
})();
const uid = () => crypto.randomUUID?.() || `id-${Date.now()}-${Math.random().toString(36).slice(2)}`;
const today = () => new Date().toISOString().slice(0,10);
const nowTime = () => new Date().toTimeString().slice(0,5);
const fmt = (n,d=0) => new Intl.NumberFormat('th-TH',{minimumFractionDigits:d,maximumFractionDigits:d}).format(Number(n)||0);
const money = n => `฿${fmt(n,0)}`;
const esc = s => String(s??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const toast = t => { const x=$('#toast'); x.textContent=t; x.classList.add('show'); setTimeout(()=>x.classList.remove('show'),1800); };

// ---------- Units (data is always stored as canonical km / liters; these only affect display) ----------
const KM_PER_MI = 1.60934, LITERS_PER_GAL = 3.78541;
const distUnit = () => state.units?.distance==='mi' ? 'ไมล์' : 'กม.';
const volUnit = () => state.units?.volume==='gal' ? 'แกลลอน' : 'ลิตร';
const distFactor = () => state.units?.distance==='mi' ? 1/KM_PER_MI : 1;
const volFactor = () => state.units?.volume==='gal' ? 1/LITERS_PER_GAL : 1;
const toDisplayDist = km => (Number(km)||0)*distFactor();
const toCanonicalDist = v => (Number(v)||0)/distFactor();
const toDisplayVol = l => (Number(l)||0)*volFactor();
const toCanonicalVol = v => (Number(v)||0)/volFactor();
const toDisplayPricePerVol = pricePerLiter => (Number(pricePerLiter)||0)/volFactor();
const toCanonicalPricePerVol = pricePerDisplayVol => (Number(pricePerDisplayVol)||0)*volFactor();
const toDisplayCostPerDist = costPerKm => (Number(costPerKm)||0)/distFactor();
const dispDistVal = km => km?Math.round(toDisplayDist(km)*100)/100:'';
const dispVolVal = l => l?Math.round(toDisplayVol(l)*1000)/1000:'';
const fmtDist = (km,d=0) => `${fmt(toDisplayDist(km),d)} ${distUnit()}`;
const fmtVol = (l,d=2) => `${fmt(toDisplayVol(l),d)} ${volUnit()}`;
const efficiencyUnit = () => `${distUnit()}/${volUnit()}`;
const toDisplayEfficiency = kml => (Number(kml)||0)*distFactor()/volFactor();

let state = { vehicles:[], entries:[], expenses:[], reminders:[], trips:[], currentVehicleId:null, theme:'dark', units:{distance:'km',volume:'liters'} };
let user = null, vehicleUnsub = null, nearbyCache = null, gpsTrack = null, reportTab = 'fillups';

function seed(){
  const oldVehicles = JSON.parse(store.getItem('fuel-vehicles')||'null');
  const oldEntries = JSON.parse(store.getItem('fuel-entries')||'[]');
  const oldCosts = JSON.parse(store.getItem('fuel-costs')||'[]');
  const oldReminders = JSON.parse(store.getItem('fuel-reminders')||'[]');
  const vehicles = oldVehicles?.length ? oldVehicles : [{id:uid(),name:'รถของฉัน'}];
  return {vehicles, entries:oldEntries.map(x=>({...x,id:x.id||uid(),vehicleId:x.vehicleId||vehicles[0].id})), expenses:oldCosts.map(x=>({...x,id:x.id||uid(),vehicleId:x.vehicleId||vehicles[0].id,amount:Number(x.amount||x.total||0)})), reminders:oldReminders.map(x=>({...x,id:x.id||uid(),vehicleId:x.vehicleId||vehicles[0].id})), trips:[], currentVehicleId:store.getItem('current-vehicle-id')||vehicles[0].id, theme:'dark', units:{distance:'km',volume:'liters'}};
}
function load(){
  try{ state = JSON.parse(store.getItem(KEY)) || seed(); }catch{ state=seed(); }
  if(!state.vehicles?.length) state=seed();
  state.entries ||= []; state.expenses ||= []; state.reminders ||= []; state.trips ||= [];
  state.currentVehicleId ||= state.vehicles[0].id;
  state.units ||= {distance:'km',volume:'liters'};
  if(!state.themeMigratedAuto){ state.theme='auto'; state.themeMigratedAuto=true; }
  state.homeCards ||= {nearby:true,todayPrice:true,chart:true,latest:true,due:true};
  state.fontFamily ||= 'system';
  // Compatibility: old records only had total. New records keep grossTotal + discount,
  // while total remains the actual net amount used by all cost/km calculations.
  state.entries = state.entries.map(x=>{
    const discount=Math.max(0,Number(x.discount)||0);
    const net=Number(x.total)||0;
    const gross=Number(x.grossTotal);
    return {...x,discount,grossTotal:Number.isFinite(gross)&&gross>0?gross:net+discount,total:net};
  });
  save();
}
function save(){ store.setItem(KEY,JSON.stringify(state)); }
const vehicle = () => state.vehicles.find(v=>v.id===state.currentVehicleId) || state.vehicles[0];
const entries = () => state.entries.filter(x=>x.vehicleId===state.currentVehicleId).sort((a,b)=>new Date(a.date)-new Date(b.date)||(+a.odometer)-(+b.odometer));
const expenses = () => state.expenses.filter(x=>x.vehicleId===state.currentVehicleId).sort((a,b)=>new Date(b.date)-new Date(a.date));
const reminders = () => state.reminders.filter(x=>x.vehicleId===state.currentVehicleId);
const trips = () => state.trips.filter(x=>x.vehicleId===state.currentVehicleId).sort((a,b)=>String(b.date).localeCompare(String(a.date)));
const currentOdo = () => Math.max(0,...entries().map(x=>+x.odometer||0),...expenses().map(x=>+x.odometer||0));
function metrics(list=entries()){
  let dist=0,lit=0,valid=[];
  for(let i=1;i<list.length;i++){const a=list[i-1],b=list[i],d=(+b.odometer)-(+a.odometer);if(a.full&&b.full&&!b.previousFillMissed&&d>0&&+b.liters>0){const k=d/(+b.liters);if(k>2&&k<80){dist+=d;lit+=+b.liters;valid.push({...b,kml:k,distance:d});}}}
  const spent=list.reduce((s,x)=>s+(+x.total||0),0); return {dist,lit,spent,kml:lit?dist/lit:0,costKm:dist?spent/dist:0,valid};
}
function monthKey(v){return String(v||'').slice(0,7)}
function withinPeriod(date,p){if(p==='all')return true;const d=new Date(date),n=new Date();if(p==='month')return d.getFullYear()===n.getFullYear()&&d.getMonth()===n.getMonth();return d.getFullYear()===n.getFullYear();}
function monthSeries(n=6){const out=[],now=new Date();for(let i=n-1;i>=0;i--){const d=new Date(now.getFullYear(),now.getMonth()-i,1),k=d.toISOString().slice(0,7);out.push({label:d.toLocaleDateString('th-TH',{month:'short'}),value:entries().filter(x=>monthKey(x.date)===k).reduce((s,x)=>s+(+x.total||0),0)+expenses().filter(x=>monthKey(x.date)===k).reduce((s,x)=>s+(+x.amount||0),0)});}return out;}
function dueItems(){const odo=currentOdo(),now=new Date();return reminders().map(r=>{let status='ok',label='ปกติ',score=999999;if(r.nextOdo){const left=(+r.nextOdo)-odo;score=left;label=left<0?`เกิน ${fmtDist(-left)}`:`อีก ${fmtDist(left)}`;status=left<0?'over':left<1000?'soon':'ok';}if(r.nextDate){const days=Math.ceil((new Date(r.nextDate)-now)/864e5);if(days<score){score=days;label=days<0?`เกิน ${-days} วัน`:`อีก ${days} วัน`;status=days<0?'over':days<30?'soon':'ok';}}return {...r,status,label};}).sort((a,b)=>({over:0,soon:1,ok:2}[a.status]-({over:0,soon:1,ok:2}[b.status])));}
function health(){let score=100;dueItems().forEach(x=>score-=x.status==='over'?18:x.status==='soon'?7:0);const v=metrics().valid;if(v.length>=6){const a=v.slice(-3).reduce((s,x)=>s+x.kml,0)/3,b=v.slice(-6,-3).reduce((s,x)=>s+x.kml,0)/3;if(a<b*.9)score-=10;}return Math.max(20,score);}

// ---------- Theme: 'auto' follows the phone/OS setting, or user can pin light/dark ----------
let systemThemeMedia = null;
function computeIsLight(){
  if(state.theme==='light') return true;
  if(state.theme==='dark') return false;
  return !!(window.matchMedia && window.matchMedia('(prefers-color-scheme: light)').matches);
}
function applyTheme(){
  const isLight = computeIsLight();
  document.body.classList.toggle('light', isLight);
  const btn = $('#themeBtn');
  if(btn) btn.textContent = state.theme==='light' ? '☀' : state.theme==='dark' ? '☾' : '◐';
  const meta = document.querySelector('meta[name="theme-color"]');
  if(meta) meta.setAttribute('content', isLight ? '#f5f6f8' : '#0f1115');
}
function watchSystemTheme(){
  if(!window.matchMedia || systemThemeMedia) return;
  systemThemeMedia = window.matchMedia('(prefers-color-scheme: light)');
  const handler = () => { if(state.theme==='auto') applyTheme(); };
  if(systemThemeMedia.addEventListener) systemThemeMedia.addEventListener('change', handler);
  else if(systemThemeMedia.addListener) systemThemeMedia.addListener(handler); // older WebView fallback
}

// ---------- Home page: which cards to show ----------
function applyHomeCardVisibility(){
  const c = state.homeCards || {};
  const map = { nearby:'homeNearbyCard', todayPrice:'todayPriceCard', chart:'chartCard', latest:'latestCard', due:'dueCard' };
  Object.entries(map).forEach(([key,id])=>{
    const el = $('#'+id);
    if(el) el.style.display = (c[key]===false) ? 'none' : '';
  });
}

// ---------- Font family ----------
const FONT_OPTIONS = {
  system: { label:'ระบบ (ค่าเริ่มต้น)', stack:"-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif", google:null },
  sarabun: { label:'Sarabun', stack:"'Sarabun',-apple-system,sans-serif", google:'Sarabun:wght@400;600;700' },
  kanit: { label:'Kanit', stack:"'Kanit',-apple-system,sans-serif", google:'Kanit:wght@400;600;700' },
  prompt: { label:'Prompt', stack:"'Prompt',-apple-system,sans-serif", google:'Prompt:wght@400;600;700' },
};
function loadGoogleFontIfNeeded(key){
  const opt = FONT_OPTIONS[key];
  if(!opt || !opt.google) return;
  const id = 'gfont-'+key;
  if(document.getElementById(id)) return;
  const link = document.createElement('link');
  link.id = id; link.rel = 'stylesheet';
  link.href = `https://fonts.googleapis.com/css2?family=${opt.google}&display=swap`;
  document.head.appendChild(link);
}
function applyFont(){
  const key = FONT_OPTIONS[state.fontFamily] ? state.fontFamily : 'system';
  loadGoogleFontIfNeeded(key);
  document.body.style.fontFamily = FONT_OPTIONS[key].stack;
}

function renderNav(page='home'){$$('.page').forEach(x=>x.classList.toggle('active',x.dataset.page===page));$$('[data-nav]').forEach(x=>x.classList.toggle('active',x.dataset.nav===page||(page==='reports'&&x.dataset.nav==='more')));const names={home:'ภาพรวม',fuel:'เติมน้ำมัน',expense:'ค่าใช้จ่าย',maintenance:'บำรุงรักษา',more:'เพิ่มเติม',reports:'รายงาน'};$('#pageTitle').textContent=names[page];$('#pageEyebrow').textContent=vehicle()?.name||'รถของฉัน';$('#reportsBackBtn').style.display=page==='reports'?'':'none';}
function metric(label,val,sub=''){return `<article class="metric"><small>${label}</small><b>${val}</b><em>${sub}</em></article>`;}
function renderVehicleStrip(){ $('#vehicleStrip').innerHTML=state.vehicles.map(v=>`<button class="vehicle-chip ${v.id===state.currentVehicleId?'active':''}" data-vehicle="${v.id}">${esc(v.name)}</button>`).join(''); }
function renderHome(){const m=metrics(),score=health(),mk=monthKey(today()),monthFuel=entries().filter(x=>monthKey(x.date)===mk).reduce((s,x)=>s+(+x.total||0),0),monthExp=expenses().filter(x=>monthKey(x.date)===mk).reduce((s,x)=>s+(+x.amount||0),0);$('#avgKml').textContent=m.kml?fmt(toDisplayEfficiency(m.kml),1):'—';$('#kmlUnit')&&($('#kmlUnit').textContent=efficiencyUnit());$('#healthScore').textContent=score;$('#healthScore').style.borderColor=score>=85?'var(--green)':score>=65?'var(--accent)':'var(--red)';$('#homeMetrics').innerHTML=metric('ค่าใช้จ่ายเดือนนี้',money(monthFuel+monthExp),`${entries().filter(x=>monthKey(x.date)===mk).length+expenses().filter(x=>monthKey(x.date)===mk).length} รายการ`)+metric('ต้นทุนเชื้อเพลิง',m.costKm?`${money(toDisplayCostPerDist(m.costKm))}/${distUnit()}`:'—',`รวม ${money(m.spent)}`)+metric(`เลข${distUnit()}ล่าสุด`,currentOdo()?fmtDist(currentOdo()):'—',`${entries().length} ครั้งเติม`)+metric('ค่าใช้จ่ายสะสม',money(m.spent+expenses().reduce((s,x)=>s+(+x.amount||0),0)),'รวมทั้งหมด');drawChart();const latest=[...entries().slice(-3).reverse().map(x=>({icon:'⛽',title:x.station||x.fuelType||'เติมน้ำมัน',sub:`${x.date} • ${fmtDist(x.odometer)}`,amount:money(x.total)})),...expenses().slice(0,2).map(x=>({icon:'🔧',title:x.title||x.category,sub:`${x.date} • ${x.category||'อื่นๆ'}`,amount:money(x.amount)}))].slice(0,4);$('#latestList').innerHTML=latest.length?latest.map(rowHtml).join(''):'<div class="empty">ยังไม่มีข้อมูล</div>';const due=dueItems().slice(0,4);$('#dueList').innerHTML=due.length?due.map(x=>`<div class="due"><span>${esc(x.name)}</span><b class="status-${x.status}">${x.label}</b></div>`).join(''):'<div class="empty">ยังไม่ได้ตั้งรอบบำรุง</div>';applyHomeCardVisibility();}
function rowHtml(x){return `<article class="record"><div class="ico">${x.icon}</div><div><b>${esc(x.title)}</b><small>${esc(x.sub)}</small></div><div class="amount">${x.amount}</div></article>`;}
function drawChart(){const data=monthSeries(),svg=$('#monthlyChart'),max=Math.max(1,...data.map(x=>x.value)),w=320,h=115,p=15;const pts=data.map((x,i)=>[p+i*((w-p*2)/(Math.max(1,data.length-1))),h-p-(x.value/max)*(h-p*2)]);svg.innerHTML=`<path d="${pts.map((q,i)=>(i?'L':'M')+q.join(' ')).join(' ')}" fill="none" stroke="var(--accent)" stroke-width="3" stroke-linecap="round"/><path d="M${pts[0]?.[0]||0} ${h-p} ${pts.map(q=>'L'+q.join(' ')).join(' ')} L${pts.at(-1)?.[0]||0} ${h-p}Z" fill="rgba(244,168,59,.10)"/>${pts.map((q,i)=>`<circle cx="${q[0]}" cy="${q[1]}" r="3" fill="var(--accent)"/><text x="${q[0]}" y="128" fill="var(--muted)" font-size="9" text-anchor="middle">${data[i].label}</text>`).join('')}`;const a=data.at(-2)?.value||0,b=data.at(-1)?.value||0;$('#trendText').textContent=a?`${b>=a?'▲':'▼'} ${fmt(Math.abs((b-a)/a*100))}%`:' ';}
function renderFuel(){
  const q=$('#fuelSearch').value.toLowerCase(),p=$('#fuelPeriod').value;
  const arr=entries().slice().reverse().filter(x=>withinPeriod(x.date,p)&&JSON.stringify(x).toLowerCase().includes(q));
  if(!arr.length){ $('#fuelList').innerHTML='<div class="empty">ไม่พบรายการ</div>'; return; }
  let html='', lastMonthKey=null;
  for(const x of arr){
    const monthKeyStr=(x.date||'').slice(0,7);
    if(monthKeyStr!==lastMonthKey){ html+=`<div class="month-header">${thaiMonthLabel(x.date)}</div>`; lastMonthKey=monthKeyStr; }
    const discount=Number(x.discount)||0;
    const gross=Number(x.grossTotal)||((Number(x.total)||0)+discount);
    const discountLine=discount>0?`<small>ก่อนลด ${money(gross)} • ลด ${money(discount)}</small>`:'';
    const meta=[x.driver,x.paymentMethod,x.reason].filter(Boolean).map(esc).join(' • ');
    const metaLine=meta?`<small>${meta}</small>`:'';
    const missedLine=x.previousFillMissed?`<small style="color:var(--accent)">⚠ พลาดการบันทึกครั้งก่อน</small>`:'';
    html+=`<article class="record" data-edit-fuel="${x.id}">${stationBadge(x.station)}<div><b>${esc(x.station||x.fuelType||'เติมน้ำมัน')}</b><small>${x.date}${x.time?' '+x.time:''} • ${fmtDist(x.odometer)} • ${fmtVol(x.liters)}</small>${discountLine}${metaLine}${missedLine}</div><div class="amount">${money(x.total)}<small>${x.pricePerLiter?fmt(toDisplayPricePerVol(x.pricePerLiter),2)+' บ./'+volUnit():''}</small></div></article>`;
  }
  $('#fuelList').innerHTML=html;
}
function renderExpenses(){const q=$('#expenseSearch').value.toLowerCase(),p=$('#expensePeriod').value,arr=expenses().filter(x=>withinPeriod(x.date,p)&&JSON.stringify(x).toLowerCase().includes(q));const sum=arr.reduce((s,x)=>s+(+x.amount||0),0);$('#expenseMetrics').innerHTML=metric('ยอดรวม',money(sum),`${arr.length} รายการ`)+metric('เฉลี่ย/รายการ',arr.length?money(sum/arr.length):'—','ตามตัวกรอง');$('#expenseList').innerHTML=arr.length?arr.map(x=>`<article class="record" data-edit-expense="${x.id}"><div class="ico">🔧</div><div><b>${esc(x.title||x.category)}</b><small>${x.date} • ${esc(x.category||'อื่นๆ')}${x.odometer?' • '+fmtDist(x.odometer):''}</small></div><div class="amount">${money(x.amount)}</div></article>`).join(''):'<div class="empty">ยังไม่มีค่าใช้จ่าย</div>';}
function renderMaintenance(){const arr=dueItems();$('#maintenanceList').innerHTML=arr.length?arr.map(x=>`<article class="record" data-edit-reminder="${x.id}"><div class="ico">🔧</div><div><b>${esc(x.name)}</b><small>${x.nextOdo?'ที่ '+fmtDist(x.nextOdo):''}${x.nextDate?' • '+x.nextDate:''}${(x.repeatOdo||x.repeatMonths)?' • 🔁 ทำซ้ำ':''}</small></div><div style="text-align:right;"><div class="amount status-${x.status}">${x.label}</div><button class="secondary" data-done-reminder="${x.id}" style="margin-top:6px;padding:6px 10px;font-size:11px;">✓ เสร็จแล้ว</button></div></article>`).join(''):'<div class="empty">ยังไม่มีรายการเตือน</div>';}
function renderAll(){renderVehicleStrip();renderHome();renderFuel();renderExpenses();renderMaintenance();$('#pageEyebrow').textContent=vehicle()?.name||'รถของฉัน';save();}


let pendingMediaKind=null;
function openMediaPicker(kind){
  pendingMediaKind=kind;
  const title=kind==='receipt'?'เลือกรูปใบเสร็จ':'เลือกรูปเรือนไมล์';
  const el=$('#mediaPickerTitle'); if(el)el.textContent=title;
  const d=$('#mediaPickerDialog');
  if(d?.showModal)d.showModal(); else d?.setAttribute('open','');
}
function closeMediaPicker(){
  const d=$('#mediaPickerDialog');
  if(d?.open&&d.close)d.close(); else d?.removeAttribute('open');
}
function chooseMediaSource(source){
  const kind=pendingMediaKind;
  closeMediaPicker();
  if(!kind)return;
  const id=kind==='receipt'
    ?(source==='camera'?'receiptCameraFile':'receiptGalleryFile')
    :(source==='camera'?'odoCameraFile':'odoGalleryFile');
  document.getElementById(id)?.click();
}
function bindMediaPickerForForm(){
  $$('[data-media-picker]').forEach(btn=>btn.onclick=()=>openMediaPicker(btn.dataset.mediaPicker));
  const inputs=[
    ['receiptCameraFile','receiptFileStatus'],['receiptGalleryFile','receiptFileStatus'],
    ['odoCameraFile','odoFileStatus'],['odoGalleryFile','odoFileStatus']
  ];
  inputs.forEach(([inputId,statusId])=>{
    const input=document.getElementById(inputId),status=document.getElementById(statusId);
    if(input)input.onchange=()=>{if(input.files?.[0]&&status)status.textContent=input.files[0].name||'เลือกรูปแล้ว';};
  });
}

function showForm(type,obj={}){const d=$('#formDialog'),b=$('#formBody');$('#formTitle').textContent=type==='fuel'?(obj.id?'แก้ไขการเติมน้ำมัน':'เพิ่มรายการเติมน้ำมัน'):type==='expense'?(obj.id?'แก้ไขค่าใช้จ่าย':'เพิ่มค่าใช้จ่าย'):'ตั้งเตือนบำรุงรักษา';d.dataset.type=type;d.dataset.id=obj.id||'';
 if(type==='fuel')b.innerHTML=`<div class="photo-grid media-actions">
<button type="button" class="photo-pick" data-media-picker="receipt">🧾 เพิ่มรูปใบเสร็จ<small id="receiptFileStatus">กล้องหรือแกลเลอรี</small></button>
<button type="button" class="photo-pick" data-media-picker="odometer">🔢 เพิ่มรูปเรือนไมล์<small id="odoFileStatus">กล้องหรือแกลเลอรี</small></button>
<label class="photo-pick">✨ สแกนบิล<input hidden type="file" id="fuelOcrFile" accept="image/*"></label>
<input hidden type="file" id="receiptCameraFile" accept="image/*" capture="environment">
<input hidden type="file" id="receiptGalleryFile" accept="image/*">
<input hidden type="file" id="odoCameraFile" accept="image/*" capture="environment">
<input hidden type="file" id="odoGalleryFile" accept="image/*">
</div><div id="ocrStatus" class="muted" style="margin:8px 0;min-height:20px;"></div><div class="form-grid"><div class="field"><label>วันที่</label><input name="date" type="date" value="${obj.date||today()}"></div><div class="field"><label>เวลา</label><input name="time" type="time" value="${obj.time||nowTime()}"></div><div class="field"><label>เลข${distUnit()}</label><input name="odometer" type="number" step=".01" value="${dispDistVal(obj.odometer)}"></div><div class="field"><label>${volUnit()}</label><input name="liters" type="number" step=".001" value="${dispVolVal(obj.liters)}"></div><div class="field"><label>ราคา/${volUnit()}</label><input name="pricePerLiter" type="number" step=".01" value="${obj.pricePerLiter?fmt(toDisplayPricePerVol(obj.pricePerLiter),2):''}"></div>
<div class="field"><label>ยอดก่อนส่วนลด</label><input name="grossTotal" type="number" step=".01" value="${obj.grossTotal||((+obj.total||0)+(+obj.discount||0))||''}"></div>
<div class="field"><label>ส่วนลด (บาท)</label><input name="discount" type="number" min="0" step=".01" value="${obj.discount||''}"></div>
<div class="field full"><label>ยอดสุทธิที่ใช้คำนวณต้นทุนจริง</label><input name="total" type="number" step=".01" value="${obj.total||''}" readonly><small class="muted">ยอดสุทธิ = ยอดก่อนส่วนลด − ส่วนลด และใช้ค่านี้คำนวณบาท/${distUnit()}</small></div><div class="field"><label>ชนิดน้ำมัน</label><select name="fuelType">${[
  'แก๊สโซฮอล์ 95','แก๊สโซฮอล์ 91','แก๊สโซฮอล์ E20','แก๊สโซฮอล์ E85',
  'เบนซิน 95','ดีเซล B7','ดีเซล B10','ดีเซล B20','ดีเซลพรีเมียม',
  'LPG','NGV','ไฟฟ้า','อื่นๆ'
].map(x=>`<option value="${x}" ${obj.fuelType===x?'selected':''}>${x}</option>`).join('')}${obj.fuelType&&![
  'แก๊สโซฮอล์ 95','แก๊สโซฮอล์ 91','แก๊สโซฮอล์ E20','แก๊สโซฮอล์ E85',
  'เบนซิน 95','ดีเซล B7','ดีเซล B10','ดีเซล B20','ดีเซลพรีเมียม',
  'LPG','NGV','ไฟฟ้า','อื่นๆ'
].includes(obj.fuelType)?`<option value="${esc(obj.fuelType)}" selected>${esc(obj.fuelType)}</option>`:''}</select></div><div class="field full"><label>ปั๊ม</label><input id="stationInput" name="station" value="${esc(obj.station||'')}" placeholder="กำลังค้นหาปั๊มใกล้ฉัน…"><div id="formNearby" class="nearby-options"></div></div>
<div class="field"><label>ผู้ขับขี่</label><input name="driver" value="${esc(obj.driver||'')}" placeholder="ชื่อผู้ขับขี่"></div>
<div class="field"><label>วิธีการชำระเงิน</label><select name="paymentMethod">
${['เงินสด','บัตรเครดิต','บัตรเดบิต','โอน/QR','บัตรน้ำมัน','Wallet','บริษัทจ่าย','อื่นๆ'].map(x=>`<option value="${x}" ${obj.paymentMethod===x?'selected':''}>${x}</option>`).join('')}
${obj.paymentMethod&&!['เงินสด','บัตรเครดิต','บัตรเดบิต','โอน/QR','บัตรน้ำมัน','Wallet','บริษัทจ่าย','อื่นๆ'].includes(obj.paymentMethod)?`<option value="${esc(obj.paymentMethod)}" selected>${esc(obj.paymentMethod)}</option>`:''}
</select></div>
<div class="field full"><label>เหตุผล / วัตถุประสงค์</label><input name="reason" value="${esc(obj.reason||'')}" placeholder="เช่น เดินทางไปงาน, ใช้งานส่วนตัว, เติมก่อนออกต่างจังหวัด"></div>
<label class="field full"><input name="previousFillMissed" type="checkbox" ${obj.previousFillMissed?'checked':''}> พลาดการบันทึกการเติมครั้งก่อนหน้า</label>
<div class="field full"><label>แนบไฟล์เพิ่มเติม</label><input id="extraAttachmentFile" type="file" accept="image/*,application/pdf"></div>
<div class="field full"><label>หมายเหตุ</label><textarea name="note">${esc(obj.note||'')}</textarea></div>
<label class="field full"><input name="full" type="checkbox" ${obj.full!==false?'checked':''}> เติมเต็มถัง</label></div>`;
 if(type==='expense')b.innerHTML=`<div class="photo-grid"><label class="photo-pick">✨ สแกนบิล<input hidden type="file" id="expenseOcrFile" accept="image/*"></label></div><div id="ocrStatus" class="muted" style="margin:8px 0;min-height:20px;"></div><div class="form-grid"><div class="field"><label>วันที่</label><input name="date" type="date" value="${obj.date||today()}"></div><div class="field"><label>เลข${distUnit()}</label><input name="odometer" type="number" step=".01" value="${dispDistVal(obj.odometer)}"></div><div class="field full"><label>รายการ</label><input name="title" value="${esc(obj.title||'')}"></div><div class="field"><label>หมวด</label><select name="category">${['น้ำมันเครื่อง','ของเหลว/ไส้กรอง','เบรก','ยางและล้อ','ช่วงล่าง','แบตเตอรี่/ไฟฟ้า','เครื่องยนต์','เกียร์','แอร์','ไฮบริด/EV','ประกัน','พ.ร.บ.','ภาษี','ค่าจอด','ทางด่วน','ล้างรถ','อื่นๆ'].map(x=>`<option ${obj.category===x?'selected':''}>${x}</option>`).join('')}</select></div><div class="field"><label>จำนวนเงิน</label><input name="amount" type="number" step=".01" value="${obj.amount||''}"></div><div class="field full"><label>หมายเหตุ</label><textarea name="note">${esc(obj.note||'')}</textarea></div></div>`;
 if(type==='reminder')b.innerHTML=`<div class="form-grid"><div class="field full"><label>รายการ</label><input name="name" value="${esc(obj.name||'เปลี่ยนน้ำมันเครื่อง')}"></div><div class="field"><label>กำหนดที่เลข${distUnit()}</label><input name="nextOdo" type="number" step=".01" value="${dispDistVal(obj.nextOdo)}"></div><div class="field"><label>กำหนดวันที่</label><input name="nextDate" type="date" value="${obj.nextDate||''}"></div><div class="field"><label>ทำซ้ำทุก (${distUnit()}) — ถ้ามี</label><input name="repeatOdo" type="number" step=".01" value="${dispDistVal(obj.repeatOdo)}"></div><div class="field"><label>ทำซ้ำทุก (เดือน) — ถ้ามี</label><input name="repeatMonths" type="number" value="${obj.repeatMonths||''}"></div><p class="muted full" style="grid-column:1/-1;">ถ้าใส่ "ทำซ้ำ" ไว้ กด "✓ เสร็จแล้ว" ที่รายการนี้ในหน้าบำรุงรักษาจะเลื่อนกำหนดครั้งถัดไปให้อัตโนมัติ แทนที่จะลบทิ้ง</p></div>`;
 d.showModal();
 configureDeleteButton(type,obj);
 bindMediaPickerForForm();
 $('#fuelOcrFile')?.addEventListener('change',e=>e.target.files[0]&&scanReceipt(e.target.files[0],'fuel'));
 $('#expenseOcrFile')?.addEventListener('change',e=>e.target.files[0]&&scanReceipt(e.target.files[0],'expense'));
 if(type==='fuel'){
   const recalc=()=>{
     const liters=+$('[name="liters"]')?.value||0;
     const price=+$('[name="pricePerLiter"]')?.value||0;
     const grossEl=$('[name="grossTotal"]'), discountEl=$('[name="discount"]'), netEl=$('[name="total"]');
     if(grossEl && !grossEl.value && liters>0 && price>0) grossEl.value=(liters*price).toFixed(2);
     const gross=+grossEl?.value||0, discount=Math.max(0,+discountEl?.value||0);
     if(netEl) netEl.value=Math.max(0,gross-discount).toFixed(2);
   };
   ['liters','pricePerLiter','grossTotal','discount'].forEach(n=>$(`[name="${n}"]`)?.addEventListener('input',recalc));
   recalc();
 }
 if(type==='fuel'&&!obj.station)setTimeout(autoNearby,150);}

let tesseractLoadPromise=null;
function loadTesseract(){
  if(window.Tesseract) return Promise.resolve(window.Tesseract);
  if(tesseractLoadPromise) return tesseractLoadPromise;
  tesseractLoadPromise=loadScript('https://cdn.jsdelivr.net/npm/tesseract.js@5/dist/tesseract.min.js').then(()=>window.Tesseract);
  return tesseractLoadPromise;
}
function firstMoneyValue(text){
  const matches=[...String(text).matchAll(/(?:total|grand total|ยอดรวม|รวมทั้งสิ้น|สุทธิ|amount)[^\d]{0,15}(\d[\d,]*\.\d{2})/gi)];
  if(matches.length) return parseFloat(matches.at(-1)[1].replaceAll(',',''));
  const nums=[...String(text).matchAll(/\b(\d[\d,]*\.\d{2})\b/g)].map(m=>parseFloat(m[1].replaceAll(',',''))).filter(n=>n>0&&n<1000000);
  return nums.length?Math.max(...nums):null;
}
function receiptDate(text){
  const s=String(text);
  let m=s.match(/\b(\d{1,2})[\/\-.](\d{1,2})[\/\-.](20\d{2}|25\d{2})\b/);
  if(!m) return null;
  let y=+m[3]; if(y>2400)y-=543;
  return `${String(y).padStart(4,'0')}-${String(+m[2]).padStart(2,'0')}-${String(+m[1]).padStart(2,'0')}`;
}
function namedNumber(text,patterns){
  for(const p of patterns){
    const m=String(text).match(p);
    if(m){const n=parseFloat(m[1].replace(',','.'));if(Number.isFinite(n))return n;}
  }
  return null;
}
function guessMerchant(text){
  return String(text).split(/\r?\n/).map(x=>x.trim()).filter(x=>x.length>=3&&!/^\d[\d\s:./-]*$/.test(x)).slice(0,3).join(' ').slice(0,80);
}
async function scanReceiptWithClaude(file,type,status){
  if(status) status.textContent='กำลังส่งรูปให้ AI อ่าน…';
  const base64=await new Promise((resolve,reject)=>{
    const r=new FileReader();
    r.onload=()=>resolve(r.result.split(',')[1]);
    r.onerror=reject;
    r.readAsDataURL(file);
  });
  const mediaType=file.type||'image/jpeg';
  const isFuel=type==='fuel';
  const schemaHint=isFuel
    ? '{"date":"YYYY-MM-DD หรือ null","liters":number หรือ null,"pricePerLiter":number หรือ null,"total":number หรือ null,"station":string หรือ null}'
    : '{"date":"YYYY-MM-DD หรือ null","title":string หรือ null,"amount":number หรือ null}';
  const res=await fetch('https://api.anthropic.com/v1/messages',{
    method:'POST',
    headers:{'Content-Type':'application/json','x-api-key':state.anthropicApiKey,'anthropic-version':'2023-06-01','anthropic-dangerous-direct-browser-access':'true'},
    body:JSON.stringify({model:'claude-sonnet-4-6',max_tokens:600,messages:[{role:'user',content:[
      {type:'image',source:{type:'base64',media_type:mediaType,data:base64}},
      {type:'text',text:`นี่คือรูปใบเสร็จ${isFuel?'เติมน้ำมัน':''} อ่านข้อมูลแล้วตอบกลับเป็น JSON เท่านั้น ห้ามมีข้อความอื่นหรือ markdown fence รูปแบบ: ${schemaHint} ถ้าอ่านค่าใดไม่ได้ให้ใส่ null แปลงปี พ.ศ. เป็น ค.ศ. ถ้าจำเป็น`}
    ]}]})
  });
  const data=await res.json();
  if(data.error) throw new Error(data.error.message||'Anthropic API error');
  const textBlock=(data.content||[]).find(c=>c.type==='text');
  if(!textBlock) throw new Error('ไม่มีคำตอบจาก AI');
  return JSON.parse(textBlock.text.replace(/```json|```/g,'').trim());
}
async function scanReceipt(file,type){
  const status=$('#ocrStatus');
  if(state.anthropicApiKey){
    try{
      const parsed=await scanReceiptWithClaude(file,type,status);
      if(parsed.date&&$('[name="date"]')) $('[name="date"]').value=parsed.date;
      if(type==='fuel'){
        if(parsed.liters&&$('[name="liters"]')) $('[name="liters"]').value=parsed.liters;
        if(parsed.pricePerLiter&&$('[name="pricePerLiter"]')) $('[name="pricePerLiter"]').value=parsed.pricePerLiter;
        if(parsed.total&&$('[name="grossTotal"]')) $('[name="grossTotal"]').value=parsed.total;
        if(parsed.station&&$('#stationInput')&&!$('#stationInput').value) $('#stationInput').value=parsed.station;
        $('[name="grossTotal"]')?.dispatchEvent(new Event('input',{bubbles:true}));
      }else{
        if(parsed.amount&&$('[name="amount"]')) $('[name="amount"]').value=parsed.amount;
        if(parsed.title&&$('[name="title"]')&&!$('[name="title"]').value) $('[name="title"]').value=parsed.title;
      }
      const val=parsed.total||parsed.amount;
      if(status) status.textContent=`อ่านบิลด้วย AI เสร็จแล้ว${val?` • พบยอด ฿${fmt(val,2)}`:' • กรุณาตรวจค่าที่กรอก'}`;
      return;
    }catch(e){
      console.error('Claude OCR failed, falling back to Tesseract',e);
      if(status) status.textContent='อ่านด้วย AI ไม่สำเร็จ กำลังลองด้วยตัวอ่านสำรอง…';
      // fall through to the free local OCR below
    }
  }
  if(status) status.textContent='กำลังเตรียม OCR…';
  try{
    const T=await loadTesseract();
    const result=await T.recognize(file,'tha+eng',{logger:m=>{
      if(status&&m.status==='recognizing text') status.textContent=`กำลังอ่านข้อความ ${Math.round((m.progress||0)*100)}%`;
    }});
    const text=result?.data?.text||'';
    const total=firstMoneyValue(text);
    const date=receiptDate(text);
    if(date&&$('[name="date"]')) $('[name="date"]').value=date;
    if(total){
      const el=type==='fuel'?$('[name="grossTotal"]'):$('[name="amount"]');
      if(el) el.value=total;
    }
    if(type==='fuel'){
      const liters=namedNumber(text,[
        /(?:liters?|litres?|ลิตร)[^\d]{0,10}(\d+(?:[.,]\d+)?)/i,
        /(\d+(?:[.,]\d+)?)\s*(?:liters?|litres?|ลิตร)/i
      ]);
      const ppl=namedNumber(text,[
        /(?:price\/?l|บาท\/ลิตร|ราคา\/ลิตร)[^\d]{0,10}(\d+(?:[.,]\d+)?)/i,
        /(\d+(?:[.,]\d+)?)\s*(?:บาท\/ลิตร)/i
      ]);
      if(liters&&$('[name="liters"]')) $('[name="liters"]').value=liters;
      if(ppl&&$('[name="pricePerLiter"]')) $('[name="pricePerLiter"]').value=ppl;
      const station=guessMerchant(text);
      if(station&&$('#stationInput')&&!$('#stationInput').value) $('#stationInput').value=station;
      if(!total&&liters&&ppl&&$('[name="grossTotal"]')) $('[name="grossTotal"]').value=(liters*ppl).toFixed(2);
      $('[name="grossTotal"]')?.dispatchEvent(new Event('input',{bubbles:true}));
    }else{
      const merchant=guessMerchant(text);
      if(merchant&&$('[name="title"]')&&!$('[name="title"]').value) $('[name="title"]').value=merchant;
    }
    if(status) status.textContent=`อ่านบิลเสร็จแล้ว (ตัวอ่านฟรี)${total?` • พบยอด ฿${fmt(total,2)}`:' • กรุณาตรวจค่าที่กรอก'}`;
  }catch(e){
    console.error('OCR failed',e);
    if(status) status.textContent='สแกนไม่สำเร็จ กรุณากรอกข้อมูลเองหรือเลือกรูปใหม่';
  }
}


function configureDeleteButton(type,obj){
  let btn=$('#deleteRecordBtn');
  if(!btn){
    btn=document.createElement('button');
    btn.type='button';
    btn.id='deleteRecordBtn';
    btn.className='secondary';
    btn.style.cssText='border-color:var(--red);color:var(--red);margin-right:auto;';
    btn.textContent='🗑️ ลบรายการ';
    const footer=$('#dynamicForm')?.querySelector('.dialog-actions')||$('#dynamicForm')?.lastElementChild;
    if(footer) footer.prepend(btn);
  }
  if(!obj?.id){
    btn.hidden=true;
    btn.onclick=null;
    return;
  }
  btn.hidden=false;
  btn.onclick=()=>{
    const labels={fuel:'รายการเติมน้ำมัน',expense:'ค่าใช้จ่าย',reminder:'รายการเตือน'};
    if(!confirm(`ลบ${labels[type]||'รายการ'}นี้ถาวร?`)) return;
    if(type==='fuel') state.entries=state.entries.filter(x=>x.id!==obj.id);
    if(type==='expense') state.expenses=state.expenses.filter(x=>x.id!==obj.id);
    if(type==='reminder') state.reminders=state.reminders.filter(x=>x.id!==obj.id);
    save();
    $('#formDialog')?.close();
    renderAll();
    toast('ลบรายการแล้ว');
    if(user) syncVehicle().catch(console.warn);
  };
}

async function saveForm(e){e.preventDefault();const d=$('#formDialog'),type=d.dataset.type,idv=d.dataset.id||uid(),data=Object.fromEntries(new FormData($('#dynamicForm')));
 if(type==='fuel'){
   const odoRaw=+data.odometer||0, litersRaw=+data.liters||0;
   if(odoRaw<=0||litersRaw<=0){ alert(`กรอกเลข${distUnit()}และปริมาณ${volUnit()}ให้ครบก่อนบันทึก`); return; }
   data.id=idv;data.vehicleId=state.currentVehicleId;data.odometer=toCanonicalDist(odoRaw);data.liters=toCanonicalVol(litersRaw);
   data.pricePerLiter=toCanonicalPricePerVol(+data.pricePerLiter||0);
   data.grossTotal=Math.max(0,+data.grossTotal||0);
   data.discount=Math.max(0,+data.discount||0);
   data.total=Math.max(0,data.grossTotal-data.discount);
   data.full=$('[name="full"]')?.checked??true;
   data.previousFillMissed=$('[name="previousFillMissed"]')?.checked??false;
   if(!data.grossTotal&&data.pricePerLiter&&data.liters){data.grossTotal=data.pricePerLiter*data.liters;data.total=Math.max(0,data.grossTotal-data.discount);}
   if(!data.pricePerLiter&&data.grossTotal&&data.liters)data.pricePerLiter=data.grossTotal/data.liters;
   const old=state.entries.findIndex(x=>x.id===idv);old>=0?state.entries[old]=data:state.entries.push(data);await uploadAttachedPhotos(idv);
 }
 if(type==='expense'){
   if(!(+data.amount>0)){ alert('กรอกจำนวนเงินให้ครบก่อนบันทึก'); return; }
   data.id=idv;data.vehicleId=state.currentVehicleId;data.odometer=data.odometer?toCanonicalDist(+data.odometer):null;data.amount=+data.amount||0;const old=state.expenses.findIndex(x=>x.id===idv);old>=0?state.expenses[old]=data:state.expenses.push(data);
 }
 if(type==='reminder'){
   if(!data.name?.trim()){ alert('กรอกชื่อรายการเตือนก่อนบันทึก'); return; }
   data.id=idv;data.vehicleId=state.currentVehicleId;data.nextOdo=data.nextOdo?toCanonicalDist(+data.nextOdo):null;data.repeatOdo=data.repeatOdo?toCanonicalDist(+data.repeatOdo):null;data.repeatMonths=+data.repeatMonths||null;const old=state.reminders.findIndex(x=>x.id===idv);old>=0?state.reminders[old]=data:state.reminders.push(data);
 }
 d.close();renderAll();toast('บันทึกแล้ว');if(user)syncVehicle().catch(()=>{});}

function markReminderDone(id){
  const r=state.reminders.find(x=>x.id===id); if(!r) return;
  if(r.repeatOdo || r.repeatMonths){
    if(r.repeatOdo) r.nextOdo = currentOdo() + Number(r.repeatOdo);
    if(r.repeatMonths){ const dt=new Date(); dt.setMonth(dt.getMonth()+Number(r.repeatMonths)); r.nextDate=dt.toISOString().slice(0,10); }
    save(); renderAll(); toast('ตั้งรอบถัดไปให้แล้ว');
  } else {
    if(!confirm(`ทำเครื่องหมาย "${r.name}" ว่าเสร็จแล้ว และลบรายการเตือนนี้?`)) return;
    state.reminders = state.reminders.filter(x=>x.id!==id);
    save(); renderAll(); toast('ลบรายการเตือนแล้ว');
  }
  if(user) syncVehicle().catch(()=>{});
}

async function fetchNearbyStations(){
  const pos=await new Promise((ok,no)=>navigator.geolocation.getCurrentPosition(ok,no,{enableHighAccuracy:true,timeout:10000,maximumAge:180000}));
  const {latitude:lat,longitude:lon}=pos.coords;
  const q=`[out:json];(node[amenity=fuel](around:7000,${lat},${lon});way[amenity=fuel](around:7000,${lat},${lon});relation[amenity=fuel](around:7000,${lat},${lon}););out center tags;`;
  const r=await fetch('https://overpass-api.de/api/interpreter',{method:'POST',body:q});
  const j=await r.json();
  return j.elements.map(x=>{const a=x.lat??x.center?.lat,b=x.lon??x.center?.lon,name=x.tags?.name||x.tags?.brand||x.tags?.operator||'ปั๊มน้ำมัน';return {name,dist:haversine(lat,lon,a,b)};}).sort((a,b)=>a.dist-b.dist).slice(0,6);
}
async function autoNearby(){const box=$('#formNearby'),input=$('#stationInput');if(!box)return;box.innerHTML='<div class="muted">กำลังค้นหาตำแหน่ง…</div>';try{const stations=await fetchNearbyStations();nearbyCache=stations;if(stations[0]&&!input.value)input.value=stations[0].name;renderNearby(stations,box);}catch(e){box.innerHTML='<div class="muted">ค้นหาไม่ได้ กรุณาอนุญาตตำแหน่งหรือพิมพ์ชื่อปั๊มเอง</div>';}}
function haversine(a,b,c,d){const R=6371,p=Math.PI/180,x=(c-a)*p,y=(d-b)*p,z=Math.sin(x/2)**2+Math.cos(a*p)*Math.cos(c*p)*Math.sin(y/2)**2;return R*2*Math.atan2(Math.sqrt(z),Math.sqrt(1-z));}
function renderNearby(arr,box){if(!box)return;box.innerHTML=arr?.length?arr.map(x=>`<button type="button" class="nearby-option" data-station="${esc(x.name)}"><b>${esc(x.name)}</b><small> ${fmtDist(x.dist,1)}</small></button>`).join(''):'<div class="muted">ไม่พบปั๊ม</div>';}

function findLastPriceForStation(stationName){
  const target=stationName.toLowerCase();
  const matches=state.entries.filter(x=>x.vehicleId===state.currentVehicleId&&x.station&&(target.includes(x.station.toLowerCase())||x.station.toLowerCase().includes(target))).sort((a,b)=>new Date(b.date)-new Date(a.date));
  return matches[0]||null;
}
async function refreshHomeNearby(){
  const box=$('#homeNearbyList');if(!box)return;
  box.innerHTML='<div class="muted">กำลังค้นหาตำแหน่ง…</div>';
  try{
    const stations=await fetchNearbyStations();
    nearbyCache=stations;
    box.innerHTML=stations.length?stations.map(s=>{const last=findLastPriceForStation(s.name);return `<div class="list-row"><div><b>${esc(s.name)}</b>${last?`<br><small style="color:var(--green)">฿${fmt(toDisplayPricePerVol(last.pricePerLiter),2)}/${volUnit()} เมื่อคุณเติมล่าสุด (${last.date})</small>`:''}</div><b>${fmtDist(s.dist,1)}</b></div>`;}).join(''):'<div class="muted">ไม่พบปั๊มในรัศมี 7 กม.</div>';
  }catch(e){ box.innerHTML='<div class="muted">ค้นหาไม่ได้ กรุณาอนุญาตตำแหน่ง</div>'; }
}


function extractBangchakGrades(list){
  const find = pred => { const item = list.find(pred); const n = item ? Number(item.PriceToday) : null; return Number.isFinite(n) && n>0 ? n : null; };
  return {
    gasohol_95: find(x => x.OilName.includes('95') && x.OilName.includes('แก๊สโซฮอล์')),
    gasohol_91: find(x => x.OilName.includes('91') && x.OilName.includes('แก๊สโซฮอล์')),
    diesel_b7: find(x => x.OilName.includes('ไฮดีเซล') && !x.OilName.includes('พรีเมียม')),
  };
}
function normalizeAggregatorStation(st){
  if(!st) return null;
  const num = v => { const n = parseFloat(v); return Number.isFinite(n) && n>0 ? n : null; };
  return { gasohol_95:num(st.gasohol_95?.price), gasohol_91:num(st.gasohol_91?.price), diesel_b7:num(st.diesel_b7?.price) };
}
function gradeRow(label, grades){
  if(!grades) return '';
  const parts = [];
  if(grades.gasohol_95) parts.push(`95 ฿${grades.gasohol_95.toFixed(2)}`);
  if(grades.gasohol_91) parts.push(`91 ฿${grades.gasohol_91.toFixed(2)}`);
  if(grades.diesel_b7) parts.push(`ดีเซล B7 ฿${grades.diesel_b7.toFixed(2)}`);
  return parts.length ? `<div class="list-row"><b>${esc(label)}</b><span>${parts.join(' · ')}</span></div>` : '';
}
async function fetchBangchakLocal(){
  try{
    const res = await fetch(`./oil-prices.json?v=${Date.now()}`,{cache:'no-store'});
    if(!res.ok) return null;
    const data = await res.json();
    const root = data?.data?.[0];
    if(!root?.OilList) return null;
    const list = JSON.parse(root.OilList);
    return { grades: extractBangchakGrades(list), dateLabel: root.OilRemark2 || root.OilPriceDate || '' };
  }catch(e){ return null; }
}
async function fetchAggregatorPrices(){
  const urls = [
    'https://api.chnwt.dev/thai-oil-api/latest',
    'https://api.allorigins.win/raw?url='+encodeURIComponent('https://api.chnwt.dev/thai-oil-api/latest'),
  ];
  for(const url of urls){
    try{
      const res = await fetch(url);
      if(!res.ok) continue;
      const data = await res.json();
      if(data?.status==='success' && data.response?.stations) return data.response;
    }catch(e){ /* try next */ }
  }
  return null;
}
async function loadTodayPrices(){
  const box=$('#todayPriceList'); if(!box)return;
  box.innerHTML='<div class="muted">กำลังโหลดราคาน้ำมัน…</div>';
  try{
    const [bangchak, aggregator] = await Promise.all([fetchBangchakLocal(), fetchAggregatorPrices()]);
    const rows = [];
    if(bangchak?.grades) rows.push(gradeRow('บางจาก (ทางการ)', bangchak.grades));
    else if(aggregator?.stations?.bcp) rows.push(gradeRow('บางจาก', normalizeAggregatorStation(aggregator.stations.bcp)));
    if(aggregator?.stations?.ptt) rows.push(gradeRow('ปตท.', normalizeAggregatorStation(aggregator.stations.ptt)));
    if(aggregator?.stations?.shell) rows.push(gradeRow('เชลล์', normalizeAggregatorStation(aggregator.stations.shell)));
    const html = rows.filter(Boolean).join('');
    if(html){
      const dateLine = bangchak?.dateLabel ? `<div class="muted" style="margin-bottom:6px;font-size:10.5px;">${esc(bangchak.dateLabel)}</div>` : '';
      const note = !aggregator ? `<div class="muted" style="margin-top:6px;font-size:10px;">เทียบกับ ปตท./เชลล์ ไม่ได้ตอนนี้ (โหลดไม่สำเร็จ)</div>` : '';
      const full = dateLine+html+note;
      box.innerHTML = full;
      store.setItem('fuellog-oil-cache', JSON.stringify({html:dateLine+html,time:Date.now()}));
      return;
    }
  }catch(e){ console.warn('Oil price load failed', e); }
  const cached = store.getItem('fuellog-oil-cache');
  if(cached){
    try{
      const c = JSON.parse(cached);
      if(c.html){ box.innerHTML = `<div class="muted" style="margin-bottom:6px;">แสดงข้อมูลล่าสุดที่บันทึกไว้</div>${c.html}`; return; }
    }catch{}
  }
  box.innerHTML='<div class="muted">ยังโหลดราคาไม่ได้ กรุณาตรวจว่าไฟล์ oil-prices.json อยู่ที่รากโปรเจกต์และ GitHub Action อัปเดตสำเร็จ</div>';
}

async function uploadAttachedPhotos(logId){
  if(!user)return;
  try{ await ensureCloudVehicle(); }catch(e){ /* if this fails, the upload below will too, surfaced naturally */ }
  const receipt=$('#receiptCameraFile')?.files[0]||$('#receiptGalleryFile')?.files[0];
  const odometer=$('#odoCameraFile')?.files[0]||$('#odoGalleryFile')?.files[0];
  const attachment=$('#extraAttachmentFile')?.files[0];
  const files=[['receipt',receipt],['odometer',odometer],['attachment',attachment]].filter(x=>x[1]);
  for(const [type,file] of files){
    const path=`vehicles/${state.currentVehicleId}/fuel/${logId}/${type}-${Date.now()}-${file.name.replace(/[^\w.-]/g,'_')}`;
    const sr=ref(storage,path);
    await uploadBytes(sr,file,{contentType:file.type,customMetadata:{uploadedBy:user.uid}});
    const url=await getDownloadURL(sr);
    await setDoc(doc(db,'vehicles',state.currentVehicleId,'photos',uid()),{type,path,url,name:file.name,logId,uploadedBy:user.uid,createdAt:serverTimestamp()});
  }
}

function openPanel(name){const d=$('#panelDialog');$('#panelTitle').textContent={family:'ครอบครัวและ Cloud',trips:'ทริปและหน้างาน',gallery:'รูปและเอกสาร',backup:'สำรองและนำเข้า',vehicles:'จัดการรถ',search:'ค้นหาทุกอย่าง',settings:'การตั้งค่า'}[name];d.dataset.panel=name;renderPanel(name);d.showModal();}
function renderPanel(name){const b=$('#panelBody');if(name==='family')b.innerHTML=familyPanel();if(name==='trips')b.innerHTML=tripsPanel();if(name==='gallery'){b.innerHTML='<div id="galleryBody" class="muted">กำลังโหลด…</div>';loadGallery();}if(name==='backup')b.innerHTML=backupPanel();if(name==='vehicles')b.innerHTML=vehiclesPanel();if(name==='search')b.innerHTML=searchPanel();if(name==='settings')b.innerHTML=settingsPanel();bindPanel();}

function openReportsPage(){renderNav('reports');renderReportsPage();}
function renderReportsPage(){$('#reportsPageBody').innerHTML=reportsPanel();bindReportsPage();}
function bindReportsPage(){
  $$('#reportsPageBody [data-report-tab]').forEach(b=>b.onclick=()=>{reportTab=b.dataset.reportTab;renderReportsPage();});
  $('#reportsPageBody #exportJsonBtn')?.addEventListener('click',exportJSON);
  $('#reportsPageBody #exportCsvBtn')?.addEventListener('click',exportCSV);
  $('#reportsPageBody #printBtn')?.addEventListener('click',()=>window.print());
}
function familyPanel(){return user?`<div class="card"><div class="user-card"><img src="${esc(user.photoURL||'icon-192.png')}" referrerpolicy="no-referrer"><div><b>${esc(user.displayName||user.email)}</b><small>${esc(user.email)}</small></div></div><div class="panel-actions" style="margin-top:12px"><button class="secondary" id="signOutBtn">ออกจากระบบ</button><button class="primary" id="syncBtn">ซิงก์ข้อมูลยานพาหนะ</button></div><div id="syncResult" class="muted" style="margin-top:10px;min-height:20px;"></div></div><div class="card"><h2>สมาชิก</h2><div id="membersBody" class="muted">กำลังโหลด…</div></div><div class="card"><h2>สร้างรหัสเชิญ</h2><input id="inviteEmail" type="email" placeholder="Gmail สมาชิก"><select id="inviteRole"><option value="editor">Editor — เพิ่มและแก้ไข</option><option value="viewer">Viewer — ดูอย่างเดียว</option></select><button class="primary" id="inviteBtn" style="margin-top:8px">สร้างรหัส</button><div id="inviteResult" class="muted"></div></div><div class="card"><h2>เข้าร่วมรถ</h2><input id="joinCode" maxlength="8" placeholder="รหัสเชิญ 8 ตัว"><button class="primary" id="joinBtn" style="margin-top:8px">เข้าร่วม</button></div>`:`<div class="card"><h2>แชร์รถกับครอบครัว</h2><p class="muted">ใช้บัญชี Google เดียวสำหรับ Firebase, Firestore และ Storage ไม่ต้องล็อกอิน Google Drive แยก</p><button class="google-btn" id="loginBtn"><b style="color:#4285f4">G</b> เข้าสู่ระบบด้วย Google</button><div id="authMessage" class="muted"></div></div>`;}
function tripsPanel(){const arr=trips(),sum=arr.reduce((s,x)=>s+(+x.fuel||0)+(+x.toll||0)+(+x.parking||0)+(+x.food||0)+(+x.other||0),0);return `<div class="metric-grid">${metric('จำนวนทริป',fmt(arr.length),'')}${metric('ต้นทุนรวม',money(sum),'')}</div><div class="card"><h2>บันทึกระยะทางด้วย GPS</h2><div class="panel-actions"><button class="primary" id="gpsStartBtn" style="${gpsTrack?'display:none':''}">▶ เริ่มติดตาม</button><button class="secondary" id="gpsStopBtn" style="${gpsTrack?'':'display:none'}">■ หยุดและบันทึกระยะทาง</button></div><div id="gpsStatus" class="muted" style="margin-top:6px;">${gpsTrack?'กำลังติดตามตำแหน่ง…':'กด "เริ่มติดตาม" ก่อนออกเดินทาง ระบบจะคำนวณระยะทางจาก GPS ให้อัตโนมัติ'}</div><div id="gpsDistance" class="muted">${gpsTrack?fmtDist(gpsTrack.distanceKm,2):''}</div></div><div class="card"><h2>เพิ่มทริป</h2><input id="tripName" placeholder="ชื่องาน/ปลายทาง"><input id="tripDate" type="date" value="${today()}"><input id="tripDistance" type="number" step=".01" placeholder="ระยะทาง (${distUnit()}) — ถ้ากด GPS ไว้จะใส่ให้อัตโนมัติ" value="${gpsTrack?dispDistVal(gpsTrack.distanceKm):''}"><div class="form-grid"><input id="tripFuel" type="number" placeholder="ค่าน้ำมัน"><input id="tripToll" type="number" placeholder="ทางด่วน"><input id="tripParking" type="number" placeholder="ที่จอด"><input id="tripFood" type="number" placeholder="อาหาร/ที่พัก"><input id="tripOther" type="number" placeholder="อื่น ๆ"></div><button class="primary" id="saveTripBtn" style="margin-top:8px">บันทึกทริป</button></div><div class="card">${arr.map(x=>{const t=(+x.fuel||0)+(+x.toll||0)+(+x.parking||0)+(+x.food||0)+(+x.other||0);return `<div class="list-row"><div><b>${esc(x.name)}</b><small>${x.date}${x.distance?' • '+fmtDist(x.distance):''}</small></div><b>${money(t)}</b></div>`}).join('')||'<div class="empty">ยังไม่มีทริป</div>'}</div>`;}

function startGpsTrip(){
  if(!navigator.geolocation){ alert('อุปกรณ์นี้ไม่รองรับ GPS'); return; }
  gpsTrack = { points:[], distanceKm:0, watchId:null };
  gpsTrack.watchId = navigator.geolocation.watchPosition(pos=>{
    const {latitude,longitude}=pos.coords;
    if(gpsTrack.points.length){
      const last=gpsTrack.points[gpsTrack.points.length-1];
      const d=haversine(last.lat,last.lon,latitude,longitude);
      if(d>0.005 && d<5) gpsTrack.distanceKm+=d; // ignore GPS jitter under 5m and implausible 5km+ jumps
    }
    gpsTrack.points.push({lat:latitude,lon:longitude,t:Date.now()});
    const el=$('#gpsDistance'); if(el) el.textContent=fmtDist(gpsTrack.distanceKm,2);
  }, err=>{ toast('GPS error: '+err.message); }, {enableHighAccuracy:true, maximumAge:5000, timeout:15000});
  renderPanel('trips');
}
function stopGpsTrip(){
  if(gpsTrack?.watchId!=null) navigator.geolocation.clearWatch(gpsTrack.watchId);
  const finalDist = gpsTrack?.distanceKm || 0;
  gpsTrack = null;
  renderPanel('trips');
  const distInput=$('#tripDistance'); if(distInput && finalDist) distInput.value=dispDistVal(finalDist);
  toast(finalDist ? `บันทึกระยะทาง ${fmtDist(finalDist,2)} แล้ว` : 'หยุดติดตามแล้ว');
}

// ---------- Reports: category donut chart + month-over-month comparison ----------
// ---------- hand-drawn flat illustrations for the report tabs (original artwork, not copied from any app) ----------
const ILLUS_FILLUPS = `<svg viewBox="0 0 200 180" xmlns="http://www.w3.org/2000/svg"><ellipse cx="100" cy="160" rx="70" ry="10" fill="#e4e8f5"/><!-- fuel drop --><path d="M148 38 C148 38 132 60 132 74 C132 84.5 140 92 148 92 C156 92 164 84.5 164 74 C164 60 148 38 148 38 Z" fill="#5b8def"/><ellipse cx="142" cy="72" rx="4.5" ry="7" fill="#ffffff" opacity=".45"/><!-- pump base --><rect x="52" y="140" width="80" height="14" rx="6" fill="#1f2a4d"/><!-- pump body --><rect x="58" y="46" width="68" height="98" rx="14" fill="#33448a"/><rect x="58" y="46" width="68" height="98" rx="14" fill="url(#pumpShade)"/><!-- screen --><rect x="68" y="58" width="48" height="30" rx="6" fill="#eef1fb"/><rect x="75" y="66" width="20" height="6" rx="3" fill="#2e9e5b"/><rect x="75" y="76" width="30" height="6" rx="3" fill="#28345c"/><!-- keypad dots --><circle cx="72" cy="100" r="3.5" fill="#8fa2e0"/><circle cx="84" cy="100" r="3.5" fill="#8fa2e0"/><circle cx="96" cy="100" r="3.5" fill="#8fa2e0"/><circle cx="108" cy="100" r="3.5" fill="#8fa2e0"/><circle cx="72" cy="112" r="3.5" fill="#8fa2e0"/><circle cx="84" cy="112" r="3.5" fill="#8fa2e0"/><circle cx="96" cy="112" r="3.5" fill="#8fa2e0"/><circle cx="108" cy="112" r="3.5" fill="#8fa2e0"/><!-- top light --><rect x="80" y="34" width="36" height="14" rx="7" fill="#28345c"/><circle cx="98" cy="41" r="4" fill="#f4a83b"/><!-- hose --><path d="M126 70 C150 70 150 100 132 108 C118 114 112 122 118 132" fill="none" stroke="#f4a83b" stroke-width="6" stroke-linecap="round"/><!-- nozzle --><path d="M112 128 L128 122 L134 134 L126 144 L112 140 Z" fill="#28345c"/><defs><linearGradient id="pumpShade" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#ffffff" stop-opacity=".08"/><stop offset="1" stop-color="#000000" stop-opacity=".06"/></linearGradient></defs></svg>`;
const ILLUS_COST = `<svg viewBox="0 0 200 180" xmlns="http://www.w3.org/2000/svg"><ellipse cx="100" cy="160" rx="70" ry="10" fill="#e4e8f5"/><!-- card peeking behind, rotated --><g transform="rotate(10 132 76)"><rect x="96" y="52" width="72" height="48" rx="9" fill="#5b8def"/><rect x="96" y="64" width="72" height="10" fill="#28345c"/><rect x="104" y="82" width="26" height="7" rx="3.5" fill="#ffffff" opacity=".85"/></g><!-- receipt, rotated slightly --><g transform="rotate(-7 90 92)"><path d="M50 40 h80 v104 l-8 -7 -8 7 -8 -7 -8 7 -8 -7 -8 7 -8 -7 -8 7 -8 -7 -8 7 z" fill="#ffffff" stroke="#e4e8f5" stroke-width="2"/><rect x="62" y="54" width="56" height="7" rx="3.5" fill="#28345c"/><rect x="62" y="68" width="40" height="5" rx="2.5" fill="#c3cbe8"/><rect x="62" y="80" width="48" height="5" rx="2.5" fill="#c3cbe8"/><rect x="62" y="92" width="34" height="5" rx="2.5" fill="#c3cbe8"/><rect x="62" y="104" width="44" height="5" rx="2.5" fill="#c3cbe8"/><line x1="62" y1="118" x2="118" y2="118" stroke="#e4e8f5" stroke-width="2"/><rect x="62" y="126" width="30" height="8" rx="4" fill="#c3cbe8"/><text x="94" y="133" text-anchor="end" font-family="Arial, sans-serif" font-size="13" font-weight="700" fill="#2e9e5b">฿</text></g><!-- coin --><circle cx="55" cy="128" r="26" fill="#f4a83b"/><circle cx="55" cy="128" r="26" fill="none" stroke="#d98f22" stroke-width="2"/><circle cx="55" cy="128" r="18" fill="none" stroke="#ffffff" stroke-width="2" opacity=".55"/><text x="55" y="134" text-anchor="middle" font-family="Arial, sans-serif" font-size="20" font-weight="800" fill="#ffffff">฿</text></svg>`;
const ILLUS_DISTANCE = `<svg viewBox="0 0 200 180" xmlns="http://www.w3.org/2000/svg"><!-- road --><rect x="10" y="132" width="180" height="26" rx="13" fill="#e4e8f5"/><rect x="26" y="143" width="14" height="5" rx="2.5" fill="#ffffff"/><rect x="54" y="143" width="14" height="5" rx="2.5" fill="#ffffff"/><rect x="82" y="143" width="14" height="5" rx="2.5" fill="#ffffff"/><!-- motion lines --><rect x="14" y="82" width="24" height="5" rx="2.5" fill="#c3cbe8"/><rect x="10" y="94" width="18" height="5" rx="2.5" fill="#c3cbe8"/><!-- car body --><path d="M38 128 L38 112 Q38 103 47 101 L60 98 Q70 80 90 78 L124 78 Q140 80 149 98 L160 101 Q168 103 168 112 L168 128 Z" fill="#33448a"/><!-- cabin window --><path d="M67 97 L78 82 Q82 79 88 79 L122 79 Q129 79 133 85 L142 97 Z" fill="#9db3ef"/><line x1="105" y1="79" x2="102" y2="97" stroke="#33448a" stroke-width="3"/><!-- headlight & taillight --><circle cx="163" cy="112" r="4" fill="#f4a83b"/><rect x="38" y="106" width="6" height="8" rx="2" fill="#e0533f"/><!-- door line --><line x1="103" y1="101" x2="103" y2="126" stroke="#28345c" stroke-width="2" opacity=".5"/><!-- wheels --><circle cx="70" cy="130" r="16" fill="#1f2a4d"/><circle cx="70" cy="130" r="7" fill="#c3cbe8"/><circle cx="146" cy="130" r="16" fill="#1f2a4d"/><circle cx="146" cy="130" r="7" fill="#c3cbe8"/><!-- speed / trend badge --><g transform="translate(140,30)"><circle cx="26" cy="26" r="26" fill="#ffffff"/><circle cx="26" cy="26" r="26" fill="none" stroke="#e4e8f5" stroke-width="2"/><path d="M12 32 L20 22 L27 28 L38 14" fill="none" stroke="#2e9e5b" stroke-width="4" stroke-linecap="round" stroke-linejoin="round"/><path d="M31 14 L38 14 L38 21" fill="none" stroke="#2e9e5b" stroke-width="4" stroke-linecap="round" stroke-linejoin="round"/></g></svg>`;
const ILLUS_OTHER = `<svg viewBox="0 0 200 180" xmlns="http://www.w3.org/2000/svg"><ellipse cx="100" cy="160" rx="70" ry="10" fill="#e4e8f5"/><!-- mini bar chart card --><g transform="translate(112,86)"><rect x="0" y="0" width="70" height="56" rx="12" fill="#ffffff"/><rect x="0" y="0" width="70" height="56" rx="12" fill="none" stroke="#e4e8f5" stroke-width="2"/><rect x="12" y="30" width="10" height="16" rx="3" fill="#9db3ef"/><rect x="30" y="20" width="10" height="26" rx="3" fill="#5b8def"/><rect x="48" y="10" width="10" height="36" rx="3" fill="#33448a"/></g><!-- gears --><g transform="translate(50,55)"><path d="M 26.0,0.0 L 18.48,7.65 L 18.38,18.38 L 7.65,18.48 L 0.0,26.0 L -7.65,18.48 L -18.38,18.38 L -18.48,7.65 L -26.0,0.0 L -18.48,-7.65 L -18.38,-18.38 L -7.65,-18.48 L -0.0,-26.0 L 7.65,-18.48 L 18.38,-18.38 L 18.48,-7.65 Z" fill="#f4a83b"/><circle cx="0" cy="0" r="10" fill="#eef1fb"/></g><g transform="translate(84,90)"><path d="M 17.0,0.0 L 12.1,5.01 L 12.02,12.02 L 5.01,12.1 L 0.0,17.0 L -5.01,12.1 L -12.02,12.02 L -12.1,5.01 L -17.0,0.0 L -12.1,-5.01 L -12.02,-12.02 L -5.01,-12.1 L -0.0,-17.0 L 5.01,-12.1 L 12.02,-12.02 L 12.1,-5.01 Z" fill="#28345c"/><circle cx="0" cy="0" r="6.3" fill="#eef1fb"/></g></svg>`;
const CHART_COLORS = ['#f4a83b','#4fc39b','#5b8def','#ef6b6b','#c084fc','#f472b6','#38bdf8','#facc15','#a3e635','#fb923c'];
function donutSVG(segments, size=140, thickness=22){
  const total = segments.reduce((s,x)=>s+x.value,0) || 1;
  const r = (size - thickness)/2, cx=size/2, cy=size/2, circ = 2*Math.PI*r;
  let offset = 0;
  const arcs = segments.filter(s=>s.value>0).map(seg=>{
    const frac = seg.value/total, len = frac*circ;
    const el = `<circle cx="${cx}" cy="${cy}" r="${r}" fill="none" stroke="${seg.color}" stroke-width="${thickness}" stroke-dasharray="${len} ${circ-len}" stroke-dashoffset="${-offset}" transform="rotate(-90 ${cx} ${cy})"/>`;
    offset += len;
    return el;
  }).join('');
  return `<svg viewBox="0 0 ${size} ${size}" width="${size}" height="${size}" style="flex:none;">${arcs}<circle cx="${cx}" cy="${cy}" r="${r-thickness/2-2}" fill="var(--surface)"/></svg>`;
}
function categoryBreakdown(){
  const m = metrics();
  const byCat = {};
  expenses().forEach(x=>{ const c=x.category||'อื่นๆ'; byCat[c]=(byCat[c]||0)+(+x.amount||0); });
  const segs = [{label:'น้ำมัน',value:m.spent,color:CHART_COLORS[0]}];
  Object.entries(byCat).forEach(([label,value],i)=>segs.push({label,value,color:CHART_COLORS[(i+1)%CHART_COLORS.length]}));
  return segs.filter(s=>s.value>0);
}
function periodComparison(){
  const now=new Date();
  const thisKey=monthKey(today());
  const lastKey=new Date(now.getFullYear(),now.getMonth()-1,1).toISOString().slice(0,7);
  const sum=k=>entries().filter(x=>monthKey(x.date)===k).reduce((s,x)=>s+(+x.total||0),0)+expenses().filter(x=>monthKey(x.date)===k).reduce((s,x)=>s+(+x.amount||0),0);
  const cur=sum(thisKey), prev=sum(lastKey);
  return { cur, prev, diff: prev ? ((cur-prev)/prev*100) : null };
}
function reportsPanel(){
  return `<div class="freport">
    <div class="freport-tabs">
      <button class="freport-tab ${reportTab==='fillups'?'active':''}" data-report-tab="fillups">เติม-เพิ่ม</button>
      <button class="freport-tab ${reportTab==='cost'?'active':''}" data-report-tab="cost">ค่าใช้จ่าย</button>
      <button class="freport-tab ${reportTab==='distance'?'active':''}" data-report-tab="distance">ระยะทาง</button>
      <button class="freport-tab ${reportTab==='other'?'active':''}" data-report-tab="other">อื่นๆ</button>
    </div>
    <div id="freportBody">${renderReportTab(reportTab)}</div>
  </div>
  <div class="panel-actions freport-actions"><button class="primary" id="exportJsonBtn">Export JSON</button><button class="secondary" id="exportCsvBtn">Export CSV</button><button class="secondary" id="printBtn">พิมพ์/PDF</button></div>`;
}
function renderReportTab(tab){
  if(!entries().length && tab!=='other') return `<div class="freport-hero-icons">${ILLUS_FILLUPS}</div><div class="freport-card"><div class="freport-empty">ยังไม่มีข้อมูลการเติมน้ำมันของรถคันนี้</div></div>`;
  if(tab==='fillups') return renderFillupsTab();
  if(tab==='cost') return renderCostTab();
  if(tab==='distance') return renderDistanceTab();
  return renderOtherTab();
}
function freportHero(label,big,compares){
  return `<div class="freport-card">
    <div class="freport-label">${esc(label)}</div>
    <div class="freport-big">${big}</div>
    <div class="freport-compare">${compares.map(c=>`<div class="freport-compare-cell"><span class="fc-icon">${c.icon}</span><div><b>${c.value}</b><small>${esc(c.label)}</small></div></div>`).join('')}</div>
  </div>`;
}
function freportMini(items){
  return `<div class="freport-minigrid">${items.map(c=>`<div class="freport-mini"><span class="fm-icon ${c.tone||''}">${c.icon}</span><div><b>${c.value}</b><small>${esc(c.label)}</small></div></div>`).join('')}</div>`;
}

// ---------- date-range helpers for year/month comparisons ----------
function isThisYear(d){ return new Date(d).getFullYear()===new Date().getFullYear(); }
function isLastYear(d){ return new Date(d).getFullYear()===new Date().getFullYear()-1; }
function isThisMonth(d){ const n=new Date(),x=new Date(d); return x.getFullYear()===n.getFullYear()&&x.getMonth()===n.getMonth(); }
function isLastMonth(d){ const n=new Date(),lm=new Date(n.getFullYear(),n.getMonth()-1,1),x=new Date(d); return x.getFullYear()===lm.getFullYear()&&x.getMonth()===lm.getMonth(); }
function daySpanOf(arr){ if(!arr.length) return 1; const first=new Date(arr[0].date).getTime(); return Math.max(1,Math.round((Date.now()-first)/864e5)); }
function distanceInRange(arr,inRangeFn){
  const sorted=[...arr].sort((a,b)=>(+a.odometer)-(+b.odometer));
  const within=sorted.filter(x=>inRangeFn(x.date));
  if(!within.length) return 0;
  const beforeList=sorted.filter(x=>!inRangeFn(x.date)&&new Date(x.date)<new Date(within[0].date));
  const before=beforeList.length?beforeList[beforeList.length-1]:null;
  const startOdo=before?+before.odometer:+within[0].odometer;
  const endOdo=+within[within.length-1].odometer;
  return Math.max(0,endOdo-startOdo);
}
function fillupIntervals(){
  const arr=entries(); const out=[];
  for(let i=1;i<arr.length;i++){
    const a=arr[i-1],b=arr[i],d=(+b.odometer)-(+a.odometer);
    if(a.full&&b.full&&!b.previousFillMissed&&d>0&&+b.liters>0){
      const k=d/(+b.liters);
      if(k>2&&k<80) out.push({distance:d,liters:+b.liters,kml:k,cost:+b.total||0,costPerKm:d>0?(+b.total||0)/d:0,date:b.date});
    }
  }
  return out;
}
function fillupsReport(){
  const arr=entries(), litersOf=x=>+x.liters||0, litersVals=arr.map(litersOf).filter(v=>v>0);
  return {
    count:arr.length,
    countThisYear:arr.filter(x=>isThisYear(x.date)).length, countLastYear:arr.filter(x=>isLastYear(x.date)).length,
    countThisMonth:arr.filter(x=>isThisMonth(x.date)).length, countLastMonth:arr.filter(x=>isLastMonth(x.date)).length,
    totalLiters:arr.reduce((s,x)=>s+litersOf(x),0),
    litersThisYear:arr.filter(x=>isThisYear(x.date)).reduce((s,x)=>s+litersOf(x),0),
    litersLastYear:arr.filter(x=>isLastYear(x.date)).reduce((s,x)=>s+litersOf(x),0),
    litersThisMonth:arr.filter(x=>isThisMonth(x.date)).reduce((s,x)=>s+litersOf(x),0),
    litersLastMonth:arr.filter(x=>isLastMonth(x.date)).reduce((s,x)=>s+litersOf(x),0),
    minFill:litersVals.length?Math.min(...litersVals):0, maxFill:litersVals.length?Math.max(...litersVals):0,
  };
}
function costReport(){
  const arr=entries(), totalOf=x=>+x.total||0;
  const totals=arr.map(totalOf).filter(v=>v>0), prices=arr.map(x=>+x.pricePerLiter||0).filter(v=>v>0);
  const ivals=fillupIntervals(), cpkVals=ivals.map(x=>x.costPerKm).filter(v=>v>0), m=metrics(arr);
  const total=arr.reduce((s,x)=>s+totalOf(x),0), ds=daySpanOf(arr);
  return {
    total,
    thisYear:arr.filter(x=>isThisYear(x.date)).reduce((s,x)=>s+totalOf(x),0),
    lastYear:arr.filter(x=>isLastYear(x.date)).reduce((s,x)=>s+totalOf(x),0),
    thisMonth:arr.filter(x=>isThisMonth(x.date)).reduce((s,x)=>s+totalOf(x),0),
    lastMonth:arr.filter(x=>isLastMonth(x.date)).reduce((s,x)=>s+totalOf(x),0),
    minBill:totals.length?Math.min(...totals):0, maxBill:totals.length?Math.max(...totals):0,
    bestPrice:prices.length?Math.min(...prices):0, worstPrice:prices.length?Math.max(...prices):0,
    costPerKm:m.dist?m.spent/m.dist:0,
    bestCostPerKm:cpkVals.length?Math.min(...cpkVals):0, worstCostPerKm:cpkVals.length?Math.max(...cpkVals):0,
    avgPerDay:total/ds, avgPerMonth:(total/ds)*30.44,
  };
}
function distanceReport(){
  const arr=entries(), sorted=[...arr].sort((a,b)=>(+a.odometer)-(+b.odometer));
  const totalDistance=sorted.length>=2?Math.max(0,(+sorted[sorted.length-1].odometer)-(+sorted[0].odometer)):0;
  const ds=daySpanOf(arr);
  return {
    totalDistance, lastOdo:currentOdo(),
    dThisYear:distanceInRange(arr,isThisYear), dLastYear:distanceInRange(arr,isLastYear),
    dThisMonth:distanceInRange(arr,isThisMonth), dLastMonth:distanceInRange(arr,isLastMonth),
    avgPerDay:totalDistance/ds, avgPerMonth:(totalDistance/ds)*30.44,
  };
}
function renderFillupsTab(){
  const r=fillupsReport(), m=metrics();
  return `<div class="freport-hero-icons">${ILLUS_FILLUPS}</div>
  ${freportHero('เติม-เพิ่ม',fmt(r.count),[
    {icon:'⛽',value:fmt(r.countThisYear),label:'ปีนี้'},{icon:'⛽',value:fmt(r.countLastYear),label:'ปีก่อนหน้านี้'},
    {icon:'⛽',value:fmt(r.countThisMonth),label:'เดือนนี้'},{icon:'⛽',value:fmt(r.countLastMonth),label:'เดือนก่อนหน้า'},
  ])}
  ${freportHero('เชื้อเพลิง',fmtVol(r.totalLiters),[
    {icon:'💧',value:fmtVol(r.litersThisYear),label:'ปีนี้'},{icon:'💧',value:fmtVol(r.litersLastYear),label:'ปีก่อนหน้านี้'},
    {icon:'💧',value:fmtVol(r.litersThisMonth),label:'เดือนนี้'},{icon:'💧',value:fmtVol(r.litersLastMonth),label:'เดือนก่อนหน้า'},
  ])}
  <div class="freport-card">${freportMini([
    {icon:'⬇️',value:fmtVol(r.minFill),label:'เติมต่ำสุด',tone:'good'},
    {icon:'⬆️',value:fmtVol(r.maxFill),label:'เติมสูงสุด',tone:'bad'},
  ])}</div>
  <div class="freport-card center">
    <div class="freport-label">ปริมาณการใช้เชื้อเพลิงเฉลี่ย</div>
    <div class="freport-big accent">${m.kml?fmt(toDisplayEfficiency(m.kml),2):'—'} <span class="unit">${efficiencyUnit()}</span></div>
  </div>`;
}
function renderCostTab(){
  const r=costReport();
  return `<div class="freport-hero-icons">${ILLUS_COST}</div>
  ${freportHero('ค่าใช้จ่าย',money(r.total),[
    {icon:'📉',value:money(r.thisYear),label:'ปีนี้'},{icon:'📈',value:money(r.lastYear),label:'ปีก่อนหน้านี้'},
    {icon:'📈',value:money(r.thisMonth),label:'เดือนนี้'},{icon:'💵',value:money(r.lastMonth),label:'เดือนก่อนหน้า'},
  ])}
  <div class="freport-row2">
    <div class="freport-card small"><div class="freport-label">บิล</div>${freportMini([
      {icon:'💲',value:money(r.minBill),label:'รายจ่ายต่ำสุด',tone:'good'},{icon:'💲',value:money(r.maxBill),label:'รายจ่ายสูงสุด',tone:'bad'},
    ])}</div>
    <div class="freport-card small"><div class="freport-label">ราคาเชื้อเพลิง</div>${freportMini([
      {icon:'⛽',value:'฿'+fmt(toDisplayPricePerVol(r.bestPrice),3),label:'ราคาดีที่สุด',tone:'good'},
      {icon:'⛽',value:'฿'+fmt(toDisplayPricePerVol(r.worstPrice),3),label:'ราคาแย่ที่สุด',tone:'bad'},
    ])}</div>
  </div>
  <div class="freport-card">
    <div class="freport-label">รายจ่ายเฉลี่ยต่อ${distUnit()}</div>
    <div class="freport-big">฿${fmt(toDisplayCostPerDist(r.costPerKm),3)}<span class="unit">/${distUnit()}</span></div>
    ${freportMini([
      {icon:'💲',value:'฿'+fmt(toDisplayCostPerDist(r.bestCostPerKm),3),label:'ดีที่สุดต่อ'+distUnit(),tone:'good'},
      {icon:'💲',value:'฿'+fmt(toDisplayCostPerDist(r.worstCostPerKm),3),label:'แย่ที่สุดต่อ'+distUnit(),tone:'bad'},
    ])}
  </div>
  <div class="freport-row2">
    <div class="freport-card small center"><div class="freport-label">ค่าใช้จ่ายเฉลี่ยต่อวัน</div><div class="freport-big small">${money(r.avgPerDay)}</div></div>
    <div class="freport-card small center"><div class="freport-label">ค่าใช้จ่ายเฉลี่ยต่อเดือน</div><div class="freport-big small">${money(r.avgPerMonth)}</div></div>
  </div>`;
}
function renderDistanceTab(){
  const r=distanceReport();
  return `<div class="freport-hero-icons">${ILLUS_DISTANCE}</div>
  <div class="freport-card"><div class="freport-label">ระยะทางที่ขับด้วย FuelLog Pro</div><div class="freport-big">${fmtDist(r.totalDistance)}</div></div>
  ${freportHero('ค่าการนับเลข'+distUnit()+'สุดท้าย',fmtDist(r.lastOdo),[
    {icon:'🧭',value:fmtDist(r.dThisYear),label:'ปีนี้'},{icon:'🧭',value:fmtDist(r.dLastYear),label:'ปีก่อนหน้านี้'},
    {icon:'🧭',value:fmtDist(r.dThisMonth),label:'เดือนนี้'},{icon:'🧭',value:fmtDist(r.dLastMonth),label:'เดือนก่อนหน้า'},
  ])}
  <div class="freport-row2">
    <div class="freport-card small center"><div class="freport-label">ระยะทางเฉลี่ยต่อวัน</div><div class="freport-big small">${fmtDist(r.avgPerDay,2)}</div></div>
    <div class="freport-card small center"><div class="freport-label">ระยะทางเฉลี่ยต่อเดือน</div><div class="freport-big small">${fmtDist(r.avgPerMonth,2)}</div></div>
  </div>`;
}
function renderOtherTab(){
  const m=metrics(),exp=expenses().reduce((s,x)=>s+(+x.amount||0),0);
  const segs=categoryBreakdown(), cmp=periodComparison();
  return `<div class="freport-hero-icons">${ILLUS_OTHER}</div>
  <div class="freport-card"><div class="freport-label">ค่าใช้จ่ายอื่นๆ (ไม่รวมน้ำมัน)</div><div class="freport-big">${money(exp)}</div><div class="freport-label" style="margin-top:4px;">${expenses().length} รายการ</div></div>
  <div class="freport-card">
    <div class="freport-label" style="margin-bottom:10px;">สัดส่วนค่าใช้จ่ายตามหมวด (รวมน้ำมัน)</div>
    <div style="display:flex;align-items:center;gap:16px;flex-wrap:wrap;">
      ${donutSVG(segs).replace('var(--surface)','var(--fr-card)')}
      <div style="flex:1;min-width:140px;">${segs.length?segs.map(s=>`<div class="freport-compare-cell" style="margin-bottom:8px;"><span style="width:10px;height:10px;border-radius:50%;background:${s.color};display:inline-block;"></span><div><b>${money(s.value)}</b><small>${esc(s.label)}</small></div></div>`).join(''):'<div class="freport-empty">ยังไม่มีข้อมูล</div>'}</div>
    </div>
  </div>
  <div class="freport-card">
    <div class="freport-label">เทียบเดือนนี้กับเดือนก่อน (น้ำมัน+ค่าใช้จ่ายอื่น)</div>
    <div class="freport-row2" style="margin-top:10px;">
      <div class="freport-card small center" style="margin:0;"><div class="freport-label">เดือนนี้</div><div class="freport-big small">${money(cmp.cur)}</div></div>
      <div class="freport-card small center" style="margin:0;"><div class="freport-label">เดือนก่อน</div><div class="freport-big small">${money(cmp.prev)}</div></div>
    </div>
    <div class="freport-label" style="margin-top:10px;">${cmp.diff===null?'ยังไม่มีข้อมูลเดือนก่อนสำหรับเทียบ':`${cmp.diff>=0?'▲ เพิ่มขึ้น':'▼ ลดลง'} ${fmt(Math.abs(cmp.diff),1)}%`}</div>
  </div>`;
}


// ---------- Unified search across entries / expenses / reminders / trips ----------
function searchPanel(){ return `<input id="globalSearchInput" placeholder="พิมพ์คำค้นหา เช่น ชื่อปั๊ม, หมวดหมู่, โน้ต…" style="margin-bottom:10px;"><div id="globalSearchResults" class="muted">พิมพ์เพื่อค้นหาในทุกรายการ (เติมน้ำมัน ค่าใช้จ่าย เตือนบำรุงรักษา ทริป)</div>`; }
function runGlobalSearch(){
  const q=($('#globalSearchInput')?.value||'').trim().toLowerCase();
  const box=$('#globalSearchResults'); if(!box) return;
  if(!q){ box.innerHTML='<div class="muted">พิมพ์เพื่อค้นหาในทุกรายการ</div>'; return; }
  const results=[];
  entries().forEach(x=>{ if(JSON.stringify(x).toLowerCase().includes(q)) results.push({type:'⛽ น้ำมัน',title:x.station||x.fuelType||'เติมน้ำมัน',sub:`${x.date} • ${fmtDist(x.odometer)}`,amount:money(x.total)}); });
  expenses().forEach(x=>{ if(JSON.stringify(x).toLowerCase().includes(q)) results.push({type:'🔧 ค่าใช้จ่าย',title:x.title||x.category,sub:`${x.date} • ${x.category||''}`,amount:money(x.amount)}); });
  reminders().forEach(x=>{ if(JSON.stringify(x).toLowerCase().includes(q)) results.push({type:'⏰ เตือน',title:x.name,sub:x.nextDate||(x.nextOdo?'ที่ '+fmtDist(x.nextOdo):''),amount:''}); });
  trips().forEach(x=>{ if(JSON.stringify(x).toLowerCase().includes(q)) results.push({type:'🛣️ ทริป',title:x.name,sub:x.date,amount:money((+x.fuel||0)+(+x.toll||0)+(+x.parking||0)+(+x.food||0)+(+x.other||0))}); });
  box.innerHTML = results.length ? results.map(r=>`<div class="list-row"><div><small style="color:var(--accent)">${r.type}</small><br><b>${esc(r.title)}</b><br><small class="muted">${esc(r.sub)}</small></div><b>${r.amount}</b></div>`).join('') : '<div class="empty">ไม่พบผลลัพธ์</div>';
}

// ---------- Settings: display units ----------
function settingsPanel(){
  const hc = state.homeCards || {};
  return `<div class="card"><h2>สแกนบิลด้วย AI (แม่นยำกว่าตัวอ่านฟรี)</h2>
    <p class="muted">ค่าเริ่มต้นแอปอ่านบิลด้วย Tesseract.js (ฟรี ทำงานในเครื่อง) ซึ่งความแม่นยำสำหรับภาษาไทยและบิลถ่ายเอียง/แสงไม่ดีจะสู้ AI ไม่ได้ — ใส่ Anthropic API key ของตัวเองเพื่อให้สแกนด้วย Claude แทน แม่นยำขึ้นมาก โดยแอปจะสลับกลับไปใช้ตัวอ่านฟรีให้อัตโนมัติถ้า AI อ่านไม่สำเร็จ</p>
    <div class="field"><label>Anthropic API key</label><input type="password" id="anthropicApiKey" placeholder="sk-ant-..." value="${esc(state.anthropicApiKey||'')}"></div>
    <p class="muted">เก็บไว้ในเครื่องนี้เท่านั้น ไม่ถูกส่งไปที่อื่นนอกจาก Anthropic API ตอนสแกนบิล — สมัคร/สร้าง key ได้ที่ <a href="https://console.anthropic.com" target="_blank" rel="noopener">console.anthropic.com</a> (มีค่าใช้จ่ายตามการใช้งานจริง ~เศษสตางค์ต่อการสแกน 1 ครั้ง) เว้นว่างไว้เพื่อใช้ตัวอ่านฟรีต่อไป</p></div>
  <div class="card"><h2>รูปแบบและลักษณะ</h2>
    <div class="field"><label>แบบอักษร (Font)</label><select id="fontFamily">${Object.entries(FONT_OPTIONS).map(([k,v])=>`<option value="${k}" ${state.fontFamily===k?'selected':''}>${v.label}</option>`).join('')}</select></div>
    <p class="muted">แบบอักษรนอกจาก "ระบบ" จะโหลดจาก Google Fonts ครั้งแรกที่เลือก (ต้องมีอินเทอร์เน็ต) หลังจากนั้นเบราว์เซอร์จะจำไว้ให้</p>
    <h3 style="font-size:13px;margin:16px 0 6px;color:var(--text)">การ์ดในหน้าแรก</h3>
    <label class="toggle-row"><span>📍 ปั๊มใกล้เคียง</span><input type="checkbox" id="homeCardNearby" ${hc.nearby!==false?'checked':''}></label>
    <label class="toggle-row"><span>⛽ ราคาน้ำมันวันนี้</span><input type="checkbox" id="homeCardTodayPrice" ${hc.todayPrice!==false?'checked':''}></label>
    <label class="toggle-row"><span>📈 กราฟค่าใช้จ่าย</span><input type="checkbox" id="homeCardChart" ${hc.chart!==false?'checked':''}></label>
    <label class="toggle-row"><span>🧾 รายการล่าสุด</span><input type="checkbox" id="homeCardLatest" ${hc.latest!==false?'checked':''}></label>
    <label class="toggle-row"><span>🔧 เตือนบำรุงรักษา</span><input type="checkbox" id="homeCardDue" ${hc.due!==false?'checked':''}></label>
    <p class="muted">ปิดการ์ดที่ไม่ได้ใช้เพื่อให้หน้าแรกดูโล่งและเลื่อนถึงข้อมูลที่สำคัญเร็วขึ้น</p></div>
  <div class="card"><h2>หน่วยที่ใช้แสดงผล</h2>
    <div class="field"><label>ระยะทาง</label><select id="unitDistance"><option value="km" ${state.units?.distance!=='mi'?'selected':''}>กิโลเมตร (กม.)</option><option value="mi" ${state.units?.distance==='mi'?'selected':''}>ไมล์ (mi)</option></select></div>
    <div class="field"><label>ปริมาตรน้ำมัน</label><select id="unitVolume"><option value="liters" ${state.units?.volume!=='gal'?'selected':''}>ลิตร (L)</option><option value="gal" ${state.units?.volume==='gal'?'selected':''}>แกลลอน (US gal)</option></select></div>
    <p class="muted">ข้อมูลจะยังเก็บเป็นกิโลเมตร/ลิตรอยู่เบื้องหลังเสมอ (เผื่อย้ายเครื่องหรือประเทศ) แค่แสดงผลเป็นหน่วยที่เลือกไว้เท่านั้น เปลี่ยนได้ตลอดเวลาไม่กระทบข้อมูลเดิม</p></div>
  <div class="card"><h2>ข้อมูล</h2><div class="about-list">
    <details class="about-item"><summary>เวอร์ชันแอป<span class="about-val">${APP_VERSION}</span></summary><div class="about-body">FuelLog Pro รุ่น ${APP_VERSION} — พัฒนาเพื่อใช้งานส่วนตัว/ในครอบครัวเท่านั้น ไม่ได้เผยแพร่บน Play Store หรือ App Store</div></details>

    <details class="about-item"><summary>ประวัติการอัปเดต</summary><div class="about-body"><ul>
      <li><b>6.10</b> — รายการเติมน้ำมันแยกเป็นกลุ่มตามเดือน พร้อมไอคอนสีตามยี่ห้อปั๊ม, แก้ช่องติ๊กเลือกการ์ดหน้าแรกในตั้งค่าที่เคยเรียงเพี้ยน</li>
      <li><b>6.9</b> — เทียบราคาน้ำมัน ปตท./เชลล์ กับบางจากในหน้าแรก, เพิ่มตัวเลือกสแกนบิลด้วย Claude AI (แม่นยำกว่าตัวอ่านฟรี)</li>
      <li><b>6.8</b> — เลือกฟอนต์ของแอปได้ (Sarabun/Kanit/Prompt/ระบบ), ปิด/เปิดการ์ดในหน้าแรกได้เอง</li>
      <li><b>6.7</b> — เพิ่มหน้า "ข้อมูล" ในตั้งค่า (เวอร์ชัน, ประวัติอัปเดต, สิทธิ์การใช้งาน, นโยบายความเป็นส่วนตัว)</li>
      <li><b>6.6</b> — ธีมสลับตามระบบโทรศัพท์อัตโนมัติ, ตัดปั๊มใกล้ฉันออกจากหน้าเติมน้ำมัน (เหลือหน้าแรก/ฟอร์มเพิ่มรายการ)</li>
      <li><b>6.5</b> — หน้า "รายงาน" เปลี่ยนเป็นหน้าเต็มจอแยก ไม่ใช่หน้าต่างป๊อปอัพ</li>
      <li><b>6.4</b> — เพิ่มภาพประกอบในหน้ารายงานทั้ง 4 แท็บ</li>
      <li><b>6.3</b> — ออกแบบหน้ารายงานใหม่ทั้งหมด แบบ 4 แท็บ (เติม-เพิ่ม/ค่าใช้จ่าย/ระยะทาง/อื่นๆ)</li>
      <li><b>6.2</b> — เพิ่มช่องผู้ขับขี่ วิธีชำระเงิน เหตุผล และไฟล์แนบเพิ่มเติม</li>
      <li>รุ่นก่อนหน้า — ระบบ GPS บันทึกทริป, เตือนบำรุงรักษาแบบวนซ้ำ, ตั้งค่าหน่วยระยะทาง/ปริมาตร, ค้นหารวมทุกประเภท, นำเข้าไฟล์ Fuelio หลายคันพร้อมรูปภาพ</li>
    </ul></div></details>

    <details class="about-item"><summary>บริการและซอฟต์แวร์ที่ใช้</summary><div class="about-body"><ul>
      <li><b>Firebase</b> (Google) — ระบบล็อกอิน, ฐานข้อมูลคลาวด์, พื้นที่เก็บรูปภาพ สำหรับซิงก์และแชร์รถกับครอบครัว</li>
      <li><b>Tesseract.js</b> (สัญญาอนุญาต Apache 2.0) — อ่านข้อความจากรูปใบเสร็จ (OCR) แบบฟรี ทำงานในเครื่องทั้งหมด ไม่ส่งรูปออกไปที่ไหน ใช้เป็นค่าเริ่มต้นหรือเป็นตัวสำรอง</li>
      <li><b>Claude (Anthropic API)</b> — ทางเลือกสแกนบิลด้วย AI ที่แม่นยำกว่า ใช้เมื่อใส่ API key ของตัวเองในตั้งค่าเท่านั้น ไม่บังคับใช้</li>
      <li><b>thai-oil-api (chnwt.dev)</b> — ราคาน้ำมัน ปตท./เชลล์ สำหรับเทียบกับราคาทางการของบางจาก</li>
      <li><b>JSZip</b> (สัญญาอนุญาต MIT) — ใช้แตกไฟล์สำรองข้อมูล .fuelio ตอนนำเข้า</li>
      <li><b>OpenStreetMap</b> ผ่าน Overpass API (สัญญาอนุญาต ODbL) — ข้อมูลตำแหน่งปั๊มน้ำมันใกล้เคียง</li>
      <li><b>ข้อมูลราคาน้ำมัน</b> จาก Bangchak Corporation PCL (Open Data)</li>
    </ul></div></details>

    <details class="about-item"><summary>นโยบายความเป็นส่วนตัว</summary><div class="about-body">
      <ul>
        <li>ข้อมูลการเติมน้ำมัน ค่าใช้จ่าย และรูปภาพ เก็บไว้ในเครื่องของพี่เป็นหลัก (localStorage)</li>
        <li>ถ้าเข้าสู่ระบบด้วย Google ข้อมูลจะซิงก์ขึ้น Firebase เพื่อสำรองและแชร์กับสมาชิกในครอบครัวที่พี่เชิญเท่านั้น ไม่มีใครอื่นเข้าถึงได้</li>
        <li>ไม่มีการขาย แชร์ หรือส่งข้อมูลให้บุคคล/บริษัทที่สาม ไม่มีโฆษณา ไม่มีการติดตามพฤติกรรมผู้ใช้ (analytics/tracking)</li>
        <li>ตำแหน่ง GPS ใช้เฉพาะตอนค้นหาปั๊มใกล้เคียงหรือบันทึกระยะทางทริปเท่านั้น ไม่ถูกเก็บสะสมหรือส่งไปนอกเหนือจากนั้น</li>
        <li>รูปภาพที่แนบ (ใบเสร็จ/เรือนไมล์) เก็บใน Firebase Storage เข้าถึงได้เฉพาะสมาชิกของรถคันนั้น</li>
        <li>ถ้าตั้งค่า Anthropic API key เพื่อสแกนบิลด้วย AI รูปบิลที่สแกนจะถูกส่งไปยัง Anthropic เพื่อประมวลผลเท่านั้น (ไม่ได้เก็บถาวรฝั่งเขา) — ถ้าไม่ตั้งค่าไว้ การสแกนจะทำในเครื่องทั้งหมด ไม่มีรูปออกจากเครื่องเลย</li>
      </ul>
    </div></details>

    <details class="about-item"><summary>ข้อกำหนดการใช้งาน</summary><div class="about-body">
      <ul>
        <li>แอปนี้พัฒนาขึ้นเพื่อใช้งานส่วนตัวและในครอบครัว ไม่ได้จำหน่ายหรือเผยแพร่เชิงพาณิชย์</li>
        <li>ข้อมูลราคาน้ำมันและตำแหน่งปั๊มเป็นข้อมูลอ้างอิงจากแหล่งภายนอก อาจคลาดเคลื่อนจากราคา/สถานะจริง ณ ปั๊ม</li>
        <li>ผู้ใช้เป็นผู้รับผิดชอบความถูกต้องของข้อมูลที่บันทึกด้วยตนเอง (เลขไมล์ ยอดเงิน ฯลฯ)</li>
        <li>แนะนำให้สำรองข้อมูล (Export) เป็นระยะ โดยเฉพาะก่อนเปลี่ยนเครื่องหรือล้างเบราว์เซอร์</li>
      </ul>
    </div></details>
  </div></div>`;
}

function backupPanel(){return `<div class="card"><h2>สำรองข้อมูล</h2><p class="muted">ข้อมูลใช้งานร่วมกันซิงก์ผ่าน Firebase ส่วนไฟล์สำรองใช้สำหรับเก็บฉุกเฉิน</p><div class="panel-actions"><button class="primary" id="exportJsonBtn">Export JSON</button><button class="secondary" id="exportCsvBtn">Export CSV</button><button class="secondary" id="importBtn">นำเข้า JSON / Fuelio</button><input hidden type="file" id="importFile" accept=".json,.csv,.fuelio,.zip"></div></div>`;}
function vehiclesPanel(){return `<div class="card"><div id="vehicleManage">${state.vehicles.map(v=>`<div class="list-row"><input data-rename-vehicle="${v.id}" value="${esc(v.name)}"><button class="secondary" data-delete-vehicle="${v.id}">ลบ</button></div>`).join('')}</div><button class="primary" id="addVehicleBtn">＋ เพิ่มรถ</button></div>`;}

async function login(){await requireFirebase();const p=new GoogleAuthProvider();p.setCustomParameters({prompt:'select_account'});try{await signInWithPopup(auth,p);}catch(e){if(['auth/popup-blocked','auth/operation-not-supported-in-this-environment'].includes(e.code))await signInWithRedirect(auth,p);else $('#authMessage').textContent=e.message;}}
async function ensureUser(){await requireFirebase();if(!user)throw new Error('กรุณาเข้าสู่ระบบ');await setDoc(doc(db,'users',user.uid),{email:user.email,emailLower:user.email.toLowerCase(),displayName:user.displayName||'',photoURL:user.photoURL||'',updatedAt:serverTimestamp()},{merge:true});}
async function ensureCloudVehicle(){
  await ensureUser();
  const vr=doc(db,'vehicles',state.currentVehicleId);
  let s=null;
  try{ s=await getDoc(vr); }catch(e){ s=null; } // a not-yet-created vehicle doc reads as permission-denied under the rules, not as "doesn't exist" — treat that as "needs creating"
  if(!s||!s.exists()){
    await setDoc(vr,{name:vehicle().name,ownerUid:user.uid,members:{[user.uid]:{role:'owner',email:user.email,displayName:user.displayName||''}},createdAt:serverTimestamp()});
  }
  return getDoc(vr);
}
async function syncVehicle(){await ensureCloudVehicle();const batch=writeBatch(db);for(const [name,arr] of [['entries',entries()],['expenses',expenses()],['reminders',reminders()],['trips',trips()]])for(const item of arr)batch.set(doc(db,'vehicles',state.currentVehicleId,name,item.id),{...item,updatedBy:user.uid,updatedAt:serverTimestamp()},{merge:true});await batch.commit();toast('ซิงก์แล้ว');}
async function syncVehicleWithStatus(){
  const btn=$('#syncBtn'), status=$('#syncResult');
  if(!btn) return;
  const original=btn.textContent;
  btn.disabled=true;
  btn.textContent='กำลังเตรียมข้อมูล…';
  if(status) status.textContent='กำลังตรวจสอบรถและสิทธิ์บน Cloud…';
  try{
    await ensureCloudVehicle();
    btn.textContent='กำลังอัปโหลด…';
    const groups=[['entries',entries()],['expenses',expenses()],['reminders',reminders()],['trips',trips()]];
    const total=groups.reduce((sum,[,arr])=>sum+arr.length,0);
    if(status) status.textContent=`กำลังส่งข้อมูล ${total} รายการ…`;
    const batch=writeBatch(db);
    for(const [name,arr] of groups){
      for(const item of arr){
        batch.set(doc(db,'vehicles',state.currentVehicleId,name,item.id),{...item,updatedBy:user.uid,updatedAt:serverTimestamp()},{merge:true});
      }
    }
    await batch.commit();
    btn.textContent='ซิงก์สำเร็จ ✓';
    if(status) status.textContent=`ซิงก์รถ “${vehicle()?.name||'รถของฉัน'}” สำเร็จ ${total} รายการ`;
    toast('ซิงก์ข้อมูลสำเร็จ');
    setTimeout(()=>{
      if($('#syncBtn')===btn){
        btn.disabled=false;
        btn.textContent=original;
      }
    },1800);
  }catch(e){
    console.error('Vehicle sync failed:',e);
    btn.disabled=false;
    btn.textContent='ลองซิงก์อีกครั้ง';
    if(status) status.textContent=`ซิงก์ไม่สำเร็จ: ${e.message||e}`;
  }
}
async function pullVehicle(){await ensureUser();for(const [name,target] of [['entries',state.entries],['expenses',state.expenses],['reminders',state.reminders],['trips',state.trips]]){const s=await getDocs(collection(db,'vehicles',state.currentVehicleId,name));const map=new Map(target.map((x,i)=>[x.id,i]));s.forEach(d=>{const x=d.data();map.has(x.id)?target[map.get(x.id)]=x:target.push(x)});}save();renderAll();toast('ดึงข้อมูลแล้ว');}
async function loadMembers(){const box=$('#membersBody');if(!box||!user)return;const s=await getDoc(doc(db,'vehicles',state.currentVehicleId));if(!s.exists()){box.innerHTML='รถคันนี้ยังไม่ขึ้น Cloud';return;}const d=s.data();box.innerHTML=Object.values(d.members||{}).map(m=>`<div class="list-row"><div><b>${esc(m.displayName||m.email)}</b><small>${esc(m.email||'')}</small></div><b>${esc(m.role)}</b></div>`).join('');}
async function invite(){try{const email=$('#inviteEmail').value.trim().toLowerCase(),role=$('#inviteRole').value,s=await ensureCloudVehicle();if(s.data().ownerUid!==user.uid)throw new Error('เฉพาะ Owner สร้างคำเชิญได้');const code=Math.random().toString(36).slice(2,10).toUpperCase();await setDoc(doc(db,'invites',code),{vehicleId:state.currentVehicleId,vehicleName:vehicle().name,emailLower:email,role,ownerUid:user.uid,expiresAt:Date.now()+7*864e5,createdAt:serverTimestamp()});$('#inviteResult').innerHTML=`รหัสเชิญ: <b>${code}</b> (7 วัน)`;}catch(e){alert(e.message)}}
async function join(){
  const btn=$('#joinBtn');
  const codeInput=$('#joinCode');
  const originalText=btn?.textContent||'เข้าร่วม';
  const setStatus=(text)=>{
    if(btn) btn.textContent=text;
  };
  try{
    if(btn){ btn.disabled=true; btn.setAttribute('aria-busy','true'); }
    setStatus('กำลังตรวจสอบรหัส…');

    await ensureUser();
    const code=(codeInput?.value||'').trim().toUpperCase();
    if(code.length!==8) throw new Error('กรอกรหัสเชิญให้ครบ 8 ตัว');

    const ir=doc(db,'invites',code);
    const is=await getDoc(ir);
    if(!is.exists()) throw new Error('ไม่พบรหัสเชิญ');

    const inv=is.data();
    const emailLower=(user.email||'').toLowerCase();
    if(inv.expiresAt<Date.now()) throw new Error('รหัสเชิญหมดอายุแล้ว');
    if(inv.emailLower&&inv.emailLower!==emailLower) throw new Error('รหัสนี้สร้างไว้สำหรับบัญชี Google อื่น');
    if(!['editor','viewer'].includes(inv.role)) throw new Error('สิทธิ์ในรหัสเชิญไม่ถูกต้อง');

    setStatus('กำลังเพิ่มสมาชิก…');
    const vr=doc(db,'vehicles',inv.vehicleId);
    await updateDoc(vr,{
      [`members.${user.uid}`]:{
        role:inv.role,
        email:user.email||'',
        emailLower,
        displayName:user.displayName||''
      },
      lastJoinCode:code,
      updatedAt:serverTimestamp()
    });

    if(!state.vehicles.some(v=>v.id===inv.vehicleId)){
      state.vehicles.push({id:inv.vehicleId,name:inv.vehicleName||'รถที่แชร์'});
    }
    state.currentVehicleId=inv.vehicleId;
    save();

    setStatus('กำลังดึงข้อมูลรถ…');
    await pullVehicle();
    renderAll();

    setStatus('เข้าร่วมสำเร็จ ✓');
    if(codeInput) codeInput.value='';
    await new Promise(resolve=>setTimeout(resolve,650));
    const panel=$('#panelDialog');
    if(panel?.open) panel.close();
    renderNav('home');
    toast(`เข้าร่วม ${inv.vehicleName||'รถที่แชร์'} สำเร็จ`);
  }catch(e){
    console.error('Join vehicle failed:',e);
    setStatus('เข้าร่วมไม่สำเร็จ');
    alert(e?.message||'เข้าร่วมรถไม่สำเร็จ');
  }finally{
    if(btn){
      btn.disabled=false;
      btn.removeAttribute('aria-busy');
      if(btn.textContent!=='เข้าร่วมสำเร็จ ✓') btn.textContent=originalText;
    }
  }
}
async function loadGallery(){const box=$('#galleryBody');await initFirebase();if(!user){box.innerHTML='กรุณาเข้าสู่ระบบก่อน';return;}try{const s=await getDocs(collection(db,'vehicles',state.currentVehicleId,'photos')),arr=s.docs.map(d=>({id:d.id,...d.data()}));box.innerHTML=`<div class="panel-actions"><label class="primary">＋ อัปโหลด<input hidden type="file" id="galleryUpload" accept="image/*,application/pdf"></label></div><div class="gallery">${arr.map(x=>`<article>${String(x.contentType||'').startsWith('image/')?`<img src="${esc(x.url)}">`:'<div style="padding:30px;text-align:center">📄</div>'}<div><b>${esc(x.name)}</b><br><a href="${esc(x.url)}" target="_blank">เปิด</a></div></article>`).join('')}</div>`;$('#galleryUpload')?.addEventListener('change',uploadGallery);}catch(e){box.textContent=e.message;}}
async function uploadGallery(e){const f=e.target.files[0];if(!f)return;await ensureCloudVehicle();const path=`vehicles/${state.currentVehicleId}/gallery/${Date.now()}-${f.name.replace(/[^\w.-]/g,'_')}`,r=ref(storage,path);await uploadBytes(r,f,{contentType:f.type,customMetadata:{uploadedBy:user.uid}});const url=await getDownloadURL(r);await setDoc(doc(db,'vehicles',state.currentVehicleId,'photos',uid()),{name:f.name,path,url,contentType:f.type,uploadedBy:user.uid,createdAt:serverTimestamp()});loadGallery();}

function bindPanel(){ $('#loginBtn')?.addEventListener('click',login);$('#signOutBtn')?.addEventListener('click',async()=>{try{await requireFirebase();await signOut(auth);}catch(e){alert(e.message);}});$('#syncBtn')?.addEventListener('click',syncVehicleWithStatus);$('#inviteBtn')?.addEventListener('click',invite);$('#joinBtn')?.addEventListener('click',join);$('#gpsStartBtn')?.addEventListener('click',startGpsTrip);$('#gpsStopBtn')?.addEventListener('click',stopGpsTrip);$('#saveTripBtn')?.addEventListener('click',()=>{const x={id:uid(),vehicleId:state.currentVehicleId,name:$('#tripName').value||'ทริป',date:$('#tripDate').value,distance:toCanonicalDist(+$('#tripDistance').value||0),fuel:+$('#tripFuel').value||0,toll:+$('#tripToll').value||0,parking:+$('#tripParking').value||0,food:+$('#tripFood').value||0,other:+$('#tripOther').value||0};state.trips.push(x);save();renderPanel('trips');if(user)syncVehicle();});$$('#exportJsonBtn').forEach(x=>x.onclick=exportJSON);$$('#exportCsvBtn').forEach(x=>x.onclick=exportCSV);$('#printBtn')?.addEventListener('click',()=>window.print());$('#importBtn')?.addEventListener('click',()=>$('#importFile').click());$('#importFile')?.addEventListener('change',e=>e.target.files[0]&&importFile(e.target.files[0]));$('#addVehicleBtn')?.addEventListener('click',()=>{const name=prompt('ชื่อรถ');if(name){const v={id:uid(),name};state.vehicles.push(v);state.currentVehicleId=v.id;save();renderAll();renderPanel('vehicles');}});$$('[data-rename-vehicle]').forEach(x=>x.onchange=()=>{state.vehicles.find(v=>v.id===x.dataset.renameVehicle).name=x.value||'รถ';save();renderAll();});$$('[data-delete-vehicle]').forEach(x=>x.onclick=()=>{if(state.vehicles.length<2)return alert('ต้องมีรถอย่างน้อย 1 คัน');if(confirm('ลบรถและข้อมูลในเครื่องของรถนี้?')){const idv=x.dataset.deleteVehicle;state.vehicles=state.vehicles.filter(v=>v.id!==idv);['entries','expenses','reminders','trips'].forEach(k=>state[k]=state[k].filter(a=>a.vehicleId!==idv));state.currentVehicleId=state.vehicles[0].id;save();renderAll();renderPanel('vehicles');}});$('#globalSearchInput')?.addEventListener('input',runGlobalSearch);$('#unitDistance')?.addEventListener('change',e=>{state.units=state.units||{};state.units.distance=e.target.value;save();renderAll();});$('#unitVolume')?.addEventListener('change',e=>{state.units=state.units||{};state.units.volume=e.target.value;save();renderAll();});$('#fontFamily')?.addEventListener('change',e=>{state.fontFamily=e.target.value;applyFont();save();});const homeCardMap={homeCardNearby:'nearby',homeCardTodayPrice:'todayPrice',homeCardChart:'chart',homeCardLatest:'latest',homeCardDue:'due'};Object.keys(homeCardMap).forEach(id=>{$('#'+id)?.addEventListener('change',e=>{state.homeCards=state.homeCards||{};state.homeCards[homeCardMap[id]]=e.target.checked;save();applyHomeCardVisibility();});});$('#anthropicApiKey')?.addEventListener('change',e=>{state.anthropicApiKey=e.target.value.trim();save();});if(user)loadMembers();}
function download(name,text,type='application/json'){const a=document.createElement('a');a.href=URL.createObjectURL(new Blob([text],{type}));a.download=name;a.click();setTimeout(()=>URL.revokeObjectURL(a.href),500);}
function exportJSON(){const {anthropicApiKey,...safeState}=state;download(`fuellog-${today()}.json`,JSON.stringify({version:5,...safeState,exportedAt:new Date().toISOString()},null,2));}
function exportCSV(){
  const rows=[['type','vehicleId','date','odometer','liters','grossAmount','discount','netAmount','driver','paymentMethod','reason','previousFillMissed','category','title','station','note']];
  state.entries.forEach(x=>rows.push(['fuel',x.vehicleId,x.date,x.odometer,x.liters,x.grossTotal||((+x.total||0)+(+x.discount||0)),x.discount||0,x.total,x.driver||'',x.paymentMethod||'',x.reason||'',x.previousFillMissed?'yes':'no','','',x.station||'',x.note||'']));
  state.expenses.forEach(x=>rows.push(['expense',x.vehicleId,x.date,x.odometer||'','','','',x.amount,'','','','',x.category||'',x.title||'','',x.note||'']));
  download(`fuellog-${today()}.csv`,rows.map(r=>r.map(v=>`"${String(v??'').replaceAll('"','""')}"`).join(',')).join('\n'),'text/csv;charset=utf-8');
}
function loadScript(src){return new Promise((ok,no)=>{if([...document.scripts].some(x=>x.src===src))return ok();const s=document.createElement('script');s.src=src;s.onload=ok;s.onerror=no;document.head.appendChild(s);});}
function ensureJSZip(){return window.JSZip?Promise.resolve():loadScript('https://cdn.jsdelivr.net/npm/jszip@3.10.1/dist/jszip.min.js');}

// ---------- Fuelio import (CSV / .fuelio / .zip — multi-vehicle, real quoted-section format, costs, pictures.data) ----------
function isFuelioSectionMarker(line){ if(!line) return false; const t=line.trim().replace(/^"+|"+$/g,''); return t.startsWith('##'); }
function fuelioSectionName(line){ const t=line.trim().replace(/^"+|"+$/g,''); return t.replace(/^##\s*/,'').trim().toLowerCase(); }
function splitFuelioCsvLine(line,delim){ const result=[]; let cur='',inQuotes=false; for(let i=0;i<line.length;i++){ const c=line[i]; if(c==='"'){ inQuotes=!inQuotes; continue; } if(c===delim&&!inQuotes){ result.push(cur); cur=''; continue; } cur+=c; } result.push(cur); return result.map(s=>s.trim()); }
function detectFuelioDelimiter(sampleLines){ const semi=sampleLines.reduce((a,l)=>a+((l.match(/;/g)||[]).length),0); const comma=sampleLines.reduce((a,l)=>a+((l.match(/,/g)||[]).length),0); return semi>=comma?';':','; }
function parseFuelioNum(str){ if(str==null) return NaN; let s=String(str).trim(); if(!s) return NaN; if(s.indexOf(',')>-1&&s.indexOf('.')===-1) s=s.replace(',','.'); s=s.replace(/[^\d.\-]/g,''); return parseFloat(s); }
function normalizeFuelioDate(raw){ if(!raw) return today(); const s=raw.trim(); let m=s.match(/^(\d{4})-(\d{2})-(\d{2})/); if(m) return `${m[1]}-${m[2]}-${m[3]}`; m=s.match(/^(\d{1,2})[\/\-.](\d{1,2})[\/\-.](\d{4})/); if(m){ const dd=m[1].padStart(2,'0'),mm=m[2].padStart(2,'0'),yyyy=m[3]; return `${yyyy}-${mm}-${dd}`; } const d=new Date(s); if(!isNaN(d.getTime())) return d.toISOString().slice(0,10); return today(); }
function extractFuelioTime(raw){ if(!raw) return ''; const m=raw.trim().match(/(\d{1,2}):(\d{2})(?::\d{2})?\s*$/); if(!m) return ''; const hh=m[1].padStart(2,'0'),mm=m[2]; if(+hh>23||+mm>59) return ''; return `${hh}:${mm}`; }

function findFuelioVehicleName(lines,beforeIdx){
  for(let i=0;i<beforeIdx;i++){
    const l=lines[i]; if(!l) continue;
    const low=l.toLowerCase();
    if(low.includes('name')&&(low.includes('plate')||low.includes('type')||low.includes('vehicle')||low.includes('tank')||low.includes('fuel unit')||low.includes('odometer unit'))){
      const delim=detectFuelioDelimiter([l,lines[i+1]||'']);
      const hdrs=splitFuelioCsvLine(l,delim).map(h=>h.toLowerCase());
      const nameIdx=hdrs.findIndex(h=>h==='name'||h.includes('name'));
      if(nameIdx>-1){
        for(let j=i+1;j<beforeIdx;j++){
          const dl=lines[j]; if(!dl||!dl.trim()||isFuelioSectionMarker(dl)) break;
          const cols=splitFuelioCsvLine(dl,delim);
          if(cols[nameIdx]&&cols[nameIdx].trim()) return cols[nameIdx].trim();
        }
      }
    }
  }
  return null;
}

function parseFuelioCSV(text){
  const lines=text.split(/\r\n|\n|\r/);
  let headerIdx=-1;
  for(let i=0;i<lines.length;i++){
    const l=lines[i]; if(!l||isFuelioSectionMarker(l)) continue;
    const low=l.toLowerCase();
    if(low.includes('odo')&&low.includes('fuel')){ headerIdx=i; break; }
  }
  if(headerIdx===-1) throw new Error('ไม่พบส่วนบันทึกการเติมน้ำมัน (Log) ในไฟล์ — ตรวจว่าเป็นไฟล์ backup จาก Fuelio จริงหรือไม่');

  const vehicleName=findFuelioVehicleName(lines,headerIdx);
  const delim=detectFuelioDelimiter(lines.slice(headerIdx,headerIdx+5));
  const headers=splitFuelioCsvLine(lines[headerIdx],delim);
  const findCol=pred=>headers.findIndex(h=>pred(h.toLowerCase()));
  const idxDate=findCol(h=>h==='date'||h==='data'||h.startsWith('date'));
  const idxOdo=findCol(h=>h.includes('odo'));
  const idxFuel=findCol(h=>h.includes('fuel')&&!h.includes('type')&&(h.includes('litre')||h.includes('liter')||h.includes('gallon')||h.includes('amount')));
  const idxFull=findCol(h=>h==='full'||h.startsWith('full'));
  const idxPrice=findCol(h=>h.includes('price')||h.includes('cost'));
  const idxStation=findCol(h=>h.includes('city')||h.includes('station'));
  const idxNote=findCol(h=>h.includes('note'));
  const idxMissed=findCol(h=>h==='missed');
  const idxUniqueId=findCol(h=>h.replace(/\s/g,'')==='uniqueid');
  if(idxDate===-1||idxOdo===-1||idxFuel===-1) throw new Error('รูปแบบคอลัมน์ไม่ตรงกับที่รองรับ');

  const odoIsMiles=headers[idxOdo].toLowerCase().includes('mi');
  const fuelIsGallons=headers[idxFuel].toLowerCase().includes('gallon');

  const results=[]; let skipped=0, i=headerIdx+1;
  for(;i<lines.length;i++){
    const raw=lines[i]; if(raw==null) continue;
    const trimmed=raw.trim(); if(!trimmed) break;
    if(isFuelioSectionMarker(trimmed)) break;
    const cols=splitFuelioCsvLine(raw,delim);
    if(cols.length<=Math.max(idxDate,idxOdo,idxFuel)){ skipped++; continue; }
    if(idxMissed>-1&&(cols[idxMissed]||'').trim()==='1'){ skipped++; continue; }
    let odo=parseFuelioNum(cols[idxOdo]), liters=parseFuelioNum(cols[idxFuel]);
    if(isNaN(odo)||isNaN(liters)){ skipped++; continue; }
    if(odoIsMiles) odo*=1.60934;
    if(fuelIsGallons) liters*=3.78541;
    const full=(cols[idxFull]||'').toString().trim()==='1';
    let total=idxPrice>-1?parseFuelioNum(cols[idxPrice]):NaN; if(isNaN(total)) total=0;
    const pricePerLiter=(liters>0&&total>0)?total/liters:0;
    results.push({
      id: uid(),
      date: normalizeFuelioDate(cols[idxDate]),
      time: extractFuelioTime(cols[idxDate]),
      odometer: Math.round(odo*10)/10,
      liters: Math.round(liters*100)/100,
      pricePerLiter: Math.round(pricePerLiter*100)/100,
      grossTotal: Math.round(total*100)/100,
      discount: 0,
      total: Math.round(total*100)/100,
      full, fuelType:'',
      station: idxStation>-1?(cols[idxStation]||'').trim():'',
      note: idxNote>-1?(cols[idxNote]||'').trim():'',
      _uniqueId: idxUniqueId>-1?(cols[idxUniqueId]||'').trim():'',
    });
  }
  const logEndIdx=i;

  const categoryMap={};
  let catHeaderIdx=-1;
  for(let k=logEndIdx;k<lines.length;k++) if(isFuelioSectionMarker(lines[k])&&fuelioSectionName(lines[k])==='costcategories'){ catHeaderIdx=k+1; break; }
  if(catHeaderIdx>-1&&lines[catHeaderIdx]){
    const cH=splitFuelioCsvLine(lines[catHeaderIdx],delim).map(h=>h.toLowerCase());
    const idIdx=cH.findIndex(h=>h==='costtypeid'), nameIdx=cH.findIndex(h=>h==='name');
    if(idIdx>-1&&nameIdx>-1) for(let k=catHeaderIdx+1;k<lines.length;k++){ const l=lines[k]; if(!l||!l.trim()||isFuelioSectionMarker(l)) break; const cols=splitFuelioCsvLine(l,delim); if(cols[idIdx]) categoryMap[cols[idIdx].trim()]=(cols[nameIdx]||'').trim(); }
  }

  const costRecords=[];
  let costsHeaderIdx=-1;
  for(let k=logEndIdx;k<lines.length;k++) if(isFuelioSectionMarker(lines[k])&&fuelioSectionName(lines[k])==='costs'){ costsHeaderIdx=k+1; break; }
  if(costsHeaderIdx>-1&&lines[costsHeaderIdx]){
    const coH=splitFuelioCsvLine(lines[costsHeaderIdx],delim).map(h=>h.toLowerCase());
    const titleIdx=coH.findIndex(h=>h==='costtitle'), dateIdx=coH.findIndex(h=>h==='date'), odoIdx=coH.findIndex(h=>h==='odo'), typeIdIdx=coH.findIndex(h=>h==='costtypeid'), noteIdx=coH.findIndex(h=>h==='notes'), amtIdx=coH.findIndex(h=>h==='cost'), templateIdx=coH.findIndex(h=>h==='istemplate');
    if(titleIdx>-1&&dateIdx>-1&&amtIdx>-1){
      for(let k=costsHeaderIdx+1;k<lines.length;k++){
        const l=lines[k]; if(!l||!l.trim()||isFuelioSectionMarker(l)) break;
        const cols=splitFuelioCsvLine(l,delim);
        if(cols.length<=Math.max(titleIdx,dateIdx,amtIdx)) continue;
        if(templateIdx>-1&&(cols[templateIdx]||'').trim()==='1') continue;
        const amount=parseFuelioNum(cols[amtIdx]), odo=odoIdx>-1?parseFuelioNum(cols[odoIdx]):NaN;
        const catName=typeIdIdx>-1?(categoryMap[(cols[typeIdIdx]||'').trim()]||'อื่นๆ'):'อื่นๆ';
        costRecords.push({ id:uid(), title:(cols[titleIdx]||'').trim(), date:normalizeFuelioDate(cols[dateIdx]), odometer:(!isNaN(odo)&&odo>0)?Math.round(odo*10)/10:null, category:catName, amount:isNaN(amount)?0:Math.round(amount*100)/100, note:noteIdx>-1?(cols[noteIdx]||'').trim():'' });
      }
    }
  }

  const pictureMap={};
  let picHeaderIdx=-1;
  for(let k=logEndIdx;k<lines.length;k++) if(isFuelioSectionMarker(lines[k])&&fuelioSectionName(lines[k])==='pictures'){ picHeaderIdx=k+1; break; }
  if(picHeaderIdx>-1&&lines[picHeaderIdx]){
    const pH=splitFuelioCsvLine(lines[picHeaderIdx],delim).map(h=>h.toLowerCase());
    const fIdx=pH.findIndex(h=>h==='filename'), tIdx=pH.findIndex(h=>h==='type'), idIdx=pH.findIndex(h=>h==='target_id');
    if(fIdx>-1&&tIdx>-1&&idIdx>-1) for(let k=picHeaderIdx+1;k<lines.length;k++){ const l=lines[k]; if(!l||!l.trim()||isFuelioSectionMarker(l)) break; const cols=splitFuelioCsvLine(l,delim); if((cols[tIdx]||'').trim()==='1'){ const tid=(cols[idIdx]||'').trim(),fn=(cols[fIdx]||'').trim(); if(tid&&fn){ if(!pictureMap[tid]) pictureMap[tid]=[]; pictureMap[tid].push(fn); } } }
  }

  return { results, skipped, vehicleName, pictureMap, costRecords };
}

function resizeImportedPhoto(blob,maxDim=1280,quality=0.72){
  return new Promise((resolve,reject)=>{
    const img=new Image(), url=URL.createObjectURL(blob);
    img.onload=()=>{
      let w=img.width,h=img.height;
      if(w>h){ if(w>maxDim){ h=Math.round(h*maxDim/w); w=maxDim; } } else { if(h>maxDim){ w=Math.round(w*maxDim/h); h=maxDim; } }
      const canvas=document.createElement('canvas'); canvas.width=w; canvas.height=h;
      canvas.getContext('2d').drawImage(img,0,0,w,h);
      canvas.toBlob(b=>{ URL.revokeObjectURL(url); resolve(b||blob); },'image/jpeg',quality);
    };
    img.onerror=()=>{ URL.revokeObjectURL(url); reject(new Error('อ่านรูปไม่สำเร็จ')); };
    img.src=url;
  });
}

async function uploadImportedPhoto(vehicleId,logId,type,blob,filename){
  const resized=await resizeImportedPhoto(blob).catch(()=>blob);
  const path=`vehicles/${vehicleId}/fuel/${logId}/${type}-${Date.now()}-${(filename||'photo').replace(/[^\w.-]/g,'_')}`;
  const sr=ref(storage,path);
  await uploadBytes(sr,resized,{contentType:'image/jpeg',customMetadata:{uploadedBy:user.uid}});
  const url=await getDownloadURL(sr);
  await setDoc(doc(db,'vehicles',vehicleId,'photos',uid()),{type,path,url,name:filename||'',logId,uploadedBy:user.uid,createdAt:serverTimestamp()});
}

async function importFuelioArchive(file){
  await ensureJSZip();
  const zip=await JSZip.loadAsync(file);
  const parsedVehicles=[];
  for(const name of Object.keys(zip.files)){
    if(zip.files[name].dir||!/\.csv$/i.test(name)) continue;
    try{ const text=await zip.files[name].async('string'); const attempt=parseFuelioCSV(text); if(attempt.results.length) parsedVehicles.push(attempt); }catch(e){ /* not a fuel log CSV */ }
  }
  if(!parsedVehicles.length) throw new Error('ไม่พบไฟล์ CSV ข้อมูลการเติมน้ำมันที่อ่านได้ในไฟล์นี้');

  let picturesZip=null;
  const picturesEntry=Object.keys(zip.files).find(n=>/(^|\/)pictures\.data$/i.test(n));
  if(picturesEntry){ try{ const b=await zip.files[picturesEntry].async('blob'); picturesZip=await JSZip.loadAsync(b); }catch(e){ picturesZip=null; } }
  const imageMap={};
  if(picturesZip) for(const name of Object.keys(picturesZip.files)){ if(picturesZip.files[name].dir) continue; imageMap[name.split('/').pop().toLowerCase()]=picturesZip.files[name]; }

  return { parsedVehicles, imageMap };
}

async function applyFuelioVehicle(parsed, imageMap, allowPhotos){
  const { results, skipped, vehicleName, pictureMap, costRecords } = parsed;
  let targetVehicleId = state.currentVehicleId;
  if(vehicleName){
    const existing = state.vehicles.find(v=>v.name.trim().toLowerCase()===vehicleName.trim().toLowerCase());
    if(existing) targetVehicleId = existing.id;
    else { const v={id:uid(),name:vehicleName}; state.vehicles.push(v); targetVehicleId=v.id; }
  }

  let photosMatched=0;
  for(const r of results){
    const filenames=(r._uniqueId&&pictureMap[r._uniqueId])||[];
    if(allowPhotos&&filenames.length){
      const slots=['receipt','odometer'];
      for(let s=0;s<Math.min(filenames.length,slots.length);s++){
        const base=filenames[s].split(/[\\/]/).pop().toLowerCase();
        const zf=imageMap[base]; if(!zf) continue;
        try{ const blob=await zf.async('blob'); await uploadImportedPhoto(targetVehicleId,r.id,slots[s],blob,filenames[s]); photosMatched++; }catch(e){ /* skip this photo */ }
      }
    }
    delete r._uniqueId;
  }

  state.entries.push(...results.map(r=>({...r,vehicleId:targetVehicleId})));
  if(costRecords&&costRecords.length) state.expenses.push(...costRecords.map(c=>({...c,vehicleId:targetVehicleId})));

  return { vehicleId:targetVehicleId, vehicleName:state.vehicles.find(v=>v.id===targetVehicleId)?.name, count:results.length, skipped, costCount:costRecords?.length||0, photosMatched };
}

async function importFile(file){
  try{
    if(file.name.endsWith('.json')){
      const d=JSON.parse(await file.text()); state={...state,...d}; save(); renderAll(); toast('นำเข้าแล้ว'); return;
    }
    if(/\.csv$/i.test(file.name)){
      const text=await file.text();
      let parsed;
      try{ parsed=parseFuelioCSV(text); }catch(e){ alert('อ่านไฟล์ CSV ไม่สำเร็จ: '+e.message); return; }
      const msg=`พบ ${parsed.results.length} รายการ${parsed.skipped?` (ข้าม ${parsed.skipped} แถวที่อ่านไม่ได้)`:''}`+
        (parsed.costRecords.length?`\nพบค่าใช้จ่ายอื่นๆ ${parsed.costRecords.length} รายการด้วย`:'')+
        (parsed.vehicleName?`\nไฟล์นี้เป็นข้อมูลรถ "${parsed.vehicleName}"`:'')+
        `\n\nนำเข้าไปยังรถ "${vehicle()?.name}" (หรือสร้างรถใหม่ถ้าตรวจพบชื่อรถอื่น) เลยไหม?`;
      if(!confirm(msg)) return;
      const r=await applyFuelioVehicle(parsed,{},false);
      save(); renderAll();
      toast(`นำเข้า ${r.count} รายการ${r.costCount?` + ค่าใช้จ่าย ${r.costCount} รายการ`:''}`);
      return;
    }
    if(/\.(fuelio|zip)$/i.test(file.name)){
      let archive;
      try{ archive=await importFuelioArchive(file); }catch(e){ alert('อ่านไฟล์ .fuelio ไม่สำเร็จ: '+e.message+'\n\nลองแตกไฟล์ zip เองแล้วนำเข้าเฉพาะไฟล์ .csv ข้างในแทนได้ครับ'); return; }
      const { parsedVehicles, imageMap } = archive;
      const totalEntries=parsedVehicles.reduce((s,v)=>s+v.results.length,0);
      const totalCosts=parsedVehicles.reduce((s,v)=>s+(v.costRecords?.length||0),0);
      const names=parsedVehicles.map(v=>v.vehicleName||'(ไม่ระบุชื่อรถ)').join(', ');
      const photoNote = user ? '\nจะพยายามแนบรูปภาพให้ด้วย (อาจใช้เวลาสักครู่ถ้ามีรูปเยอะ)' : '\n(ยังไม่ได้เข้าสู่ระบบ Google — จะนำเข้าเฉพาะข้อมูลตัวเลข ไม่แนบรูปภาพ)';
      if(!confirm(`พบข้อมูล ${parsedVehicles.length} คัน: ${names}\nรวม ${totalEntries} รายการเติมน้ำมัน, ค่าใช้จ่ายอื่นๆ ${totalCosts} รายการ${photoNote}\n\nนำเข้าทั้งหมดเลยไหม?`)) return;

      toast('กำลังนำเข้า...');
      let totalImported=0, totalCostImported=0, totalPhotos=0;
      for(const v of parsedVehicles){
        const r=await applyFuelioVehicle(v, imageMap, !!user);
        totalImported+=r.count; totalCostImported+=r.costCount; totalPhotos+=r.photosMatched;
      }
      save(); renderAll();
      toast(`นำเข้าสำเร็จ ${totalImported} รายการ${totalCostImported?` + ค่าใช้จ่าย ${totalCostImported}`:''}${totalPhotos?` + รูป ${totalPhotos}`:''}`);
      return;
    }
    alert('รองรับไฟล์ JSON, CSV และ .fuelio/.zip');
  }catch(e){
    alert(`นำเข้าไม่สำเร็จ: ${e.message}`);
  }
}
function bind(){$$('[data-nav]').forEach(x=>x.onclick=()=>{renderNav(x.dataset.nav);if(x.dataset.nav==='fuel')renderFuel();if(x.dataset.nav==='expense')renderExpenses();if(x.dataset.nav==='maintenance')renderMaintenance();if(x.dataset.nav==='home')refreshHomeNearby();});$$('[data-go]').forEach(x=>x.onclick=()=>{renderNav(x.dataset.go);});document.addEventListener('click',e=>{const v=e.target.closest('[data-vehicle]');if(v){state.currentVehicleId=v.dataset.vehicle;renderAll();}const done=e.target.closest('[data-done-reminder]');if(done){e.stopPropagation();markReminderDone(done.dataset.doneReminder);return;}const f=e.target.closest('[data-edit-fuel]');if(f)showForm('fuel',state.entries.find(x=>x.id===f.dataset.editFuel));const c=e.target.closest('[data-edit-expense]');if(c)showForm('expense',state.expenses.find(x=>x.id===c.dataset.editExpense));const r=e.target.closest('[data-edit-reminder]');if(r)showForm('reminder',state.reminders.find(x=>x.id===r.dataset.editReminder));const st=e.target.closest('[data-station]');if(st){const si=$('#stationInput');if(si){si.value=st.dataset.station;}else{showForm('fuel',{station:st.dataset.station});}}});$('#addFuelBtn').onclick=()=>showForm('fuel');$('#addExpenseBtn').onclick=()=>showForm('expense');$('#addReminderBtn').onclick=()=>showForm('reminder');$('#formCloseX').onclick=()=>$('#formDialog').close();$('#formCancelBtn').onclick=()=>$('#formDialog').close();$('#dynamicForm').addEventListener('submit',saveForm);$('#fuelSearch').oninput=renderFuel;$('#fuelPeriod').onchange=renderFuel;$('#expenseSearch').oninput=renderExpenses;$('#expensePeriod').onchange=renderExpenses;$('#refreshHomeNearby').onclick=refreshHomeNearby;$('#refreshTodayPrice').onclick=loadTodayPrices;$$('[data-panel]').forEach(x=>x.onclick=()=>openPanel(x.dataset.panel));$$('[data-page-link]').forEach(x=>x.onclick=()=>openReportsPage());$('#reportsBackBtn').onclick=()=>renderNav('more');$('#closePanel').onclick=()=>$('#panelDialog').close();$('#themeBtn').onclick=()=>{state.theme=state.theme==='auto'?'light':state.theme==='light'?'dark':'auto';applyTheme();save();};
$('#mediaCameraBtn')?.addEventListener('click',()=>chooseMediaSource('camera'));
$('#mediaGalleryBtn')?.addEventListener('click',()=>chooseMediaSource('gallery'));
$('#mediaCancelBtn')?.addEventListener('click',closeMediaPicker);
$('#mediaPickerDialog')?.addEventListener('click',e=>{if(e.target.id==='mediaPickerDialog')closeMediaPicker();});}

function boot(){
  try{
    load();
    applyTheme();
    watchSystemTheme();
    applyFont();
    bind();
    renderAll();
    renderNav('home');
    loadTodayPrices();
    refreshHomeNearby();
    document.documentElement.dataset.appReady = 'true';
  }catch(err){
    console.error('FuelLog boot failed:', err);
    const t = document.querySelector('#toast');
    if(t){t.textContent = `เปิดแอปไม่สำเร็จ: ${err.message}`;t.classList.add('show');}
  }
  // Cloud initialization is deliberately non-blocking.
  initFirebase();
  if('serviceWorker' in navigator) navigator.serviceWorker.register('./sw.js?v=6.10.0').catch(console.warn);
}

if(document.readyState === 'loading') document.addEventListener('DOMContentLoaded', boot, {once:true});
else boot();
