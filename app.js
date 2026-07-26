// FuelLog starts locally first. Firebase is loaded lazily so a CDN/Auth problem
// can never disable navigation, forms, theme switching, or local records.
const FIREBASE_SDK_VERSION = '12.13.0';
let app = null, auth = null, db = null, storage = null;
let GoogleAuthProvider, signInWithPopup, signInWithRedirect, getRedirectResult,
    signOut, onAuthStateChanged, setPersistence, browserLocalPersistence,
    doc, setDoc, getDoc, getDocs, collection, writeBatch, serverTimestamp,
    ref, uploadBytes, getDownloadURL;
let firebaseReadyPromise = null;
let firebaseLoadError = null;

async function initFirebase(){
  if(firebaseReadyPromise) return firebaseReadyPromise;
  firebaseReadyPromise = (async()=>{
    const base = `https://www.gstatic.com/firebasejs/${FIREBASE_SDK_VERSION}`;
    const [{firebaseConfig}, appMod, authMod, fireMod, storageMod] = await Promise.all([
      import('./firebase-config.js?v=5.0.3'),
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
    ({doc, setDoc, getDoc, getDocs, collection, writeBatch, serverTimestamp} = fireMod);
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
const KEY = 'fuellog-v5-data';
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
let user = null, vehicleUnsub = null, nearbyCache = null, gpsTrack = null;

function seed(){
  const oldVehicles = JSON.parse(store.getItem('fuel-vehicles')||'null');
  const oldEntries = JSON.parse(store.getItem('fuel-entries')||'[]');
  const oldCosts = JSON.parse(store.getItem('fuel-costs')||'[]');
  const oldReminders = JSON.parse(store.getItem('fuel-reminders')||'[]');
  const vehicles = oldVehicles?.length ? oldVehicles : [{id:uid(),name:'รถของฉัน'}];
  return {vehicles, entries:oldEntries.map(x=>({...x,id:x.id||uid(),vehicleId:x.vehicleId||vehicles[0].id})), expenses:oldCosts.map(x=>({...x,id:x.id||uid(),vehicleId:x.vehicleId||vehicles[0].id,amount:Number(x.amount||x.total||0)})), reminders:oldReminders.map(x=>({...x,id:x.id||uid(),vehicleId:x.vehicleId||vehicles[0].id})), trips:[], currentVehicleId:store.getItem('current-vehicle-id')||vehicles[0].id, theme:'dark', units:{distance:'km',volume:'liters'}};
}
function load(){ try{ state = JSON.parse(store.getItem(KEY)) || seed(); }catch{ state=seed(); } if(!state.vehicles?.length)state=seed(); state.entries ||= [];state.expenses ||= [];state.reminders ||= [];state.trips ||= [];state.currentVehicleId ||= state.vehicles[0].id; state.units ||= {distance:'km',volume:'liters'}; save(); }
function save(){ store.setItem(KEY,JSON.stringify(state)); }
const vehicle = () => state.vehicles.find(v=>v.id===state.currentVehicleId) || state.vehicles[0];
const entries = () => state.entries.filter(x=>x.vehicleId===state.currentVehicleId).sort((a,b)=>new Date(a.date)-new Date(b.date)||(+a.odometer)-(+b.odometer));
const expenses = () => state.expenses.filter(x=>x.vehicleId===state.currentVehicleId).sort((a,b)=>new Date(b.date)-new Date(a.date));
const reminders = () => state.reminders.filter(x=>x.vehicleId===state.currentVehicleId);
const trips = () => state.trips.filter(x=>x.vehicleId===state.currentVehicleId).sort((a,b)=>String(b.date).localeCompare(String(a.date)));
const currentOdo = () => Math.max(0,...entries().map(x=>+x.odometer||0),...expenses().map(x=>+x.odometer||0));
function metrics(list=entries()){
  let dist=0,lit=0,valid=[];
  for(let i=1;i<list.length;i++){const a=list[i-1],b=list[i],d=(+b.odometer)-(+a.odometer);if(a.full&&b.full&&d>0&&+b.liters>0){const k=d/(+b.liters);if(k>2&&k<80){dist+=d;lit+=+b.liters;valid.push({...b,kml:k,distance:d});}}}
  const spent=list.reduce((s,x)=>s+(+x.total||0),0); return {dist,lit,spent,kml:lit?dist/lit:0,costKm:dist?spent/dist:0,valid};
}
function monthKey(v){return String(v||'').slice(0,7)}
function withinPeriod(date,p){if(p==='all')return true;const d=new Date(date),n=new Date();if(p==='month')return d.getFullYear()===n.getFullYear()&&d.getMonth()===n.getMonth();return d.getFullYear()===n.getFullYear();}
function monthSeries(n=6){const out=[],now=new Date();for(let i=n-1;i>=0;i--){const d=new Date(now.getFullYear(),now.getMonth()-i,1),k=d.toISOString().slice(0,7);out.push({label:d.toLocaleDateString('th-TH',{month:'short'}),value:entries().filter(x=>monthKey(x.date)===k).reduce((s,x)=>s+(+x.total||0),0)+expenses().filter(x=>monthKey(x.date)===k).reduce((s,x)=>s+(+x.amount||0),0)});}return out;}
function dueItems(){const odo=currentOdo(),now=new Date();return reminders().map(r=>{let status='ok',label='ปกติ',score=999999;if(r.nextOdo){const left=(+r.nextOdo)-odo;score=left;label=left<0?`เกิน ${fmtDist(-left)}`:`อีก ${fmtDist(left)}`;status=left<0?'over':left<1000?'soon':'ok';}if(r.nextDate){const days=Math.ceil((new Date(r.nextDate)-now)/864e5);if(days<score){score=days;label=days<0?`เกิน ${-days} วัน`:`อีก ${days} วัน`;status=days<0?'over':days<30?'soon':'ok';}}return {...r,status,label};}).sort((a,b)=>({over:0,soon:1,ok:2}[a.status]-({over:0,soon:1,ok:2}[b.status])));}
function health(){let score=100;dueItems().forEach(x=>score-=x.status==='over'?18:x.status==='soon'?7:0);const v=metrics().valid;if(v.length>=6){const a=v.slice(-3).reduce((s,x)=>s+x.kml,0)/3,b=v.slice(-6,-3).reduce((s,x)=>s+x.kml,0)/3;if(a<b*.9)score-=10;}return Math.max(20,score);}

function renderNav(page='home'){$$('.page').forEach(x=>x.classList.toggle('active',x.dataset.page===page));$$('[data-nav]').forEach(x=>x.classList.toggle('active',x.dataset.nav===page));const names={home:'ภาพรวม',fuel:'เติมน้ำมัน',expense:'ค่าใช้จ่าย',maintenance:'บำรุงรักษา',more:'เพิ่มเติม'};$('#pageTitle').textContent=names[page];$('#pageEyebrow').textContent=vehicle()?.name||'รถของฉัน';}
function metric(label,val,sub=''){return `<article class="metric"><small>${label}</small><b>${val}</b><em>${sub}</em></article>`;}
function renderVehicleStrip(){ $('#vehicleStrip').innerHTML=state.vehicles.map(v=>`<button class="vehicle-chip ${v.id===state.currentVehicleId?'active':''}" data-vehicle="${v.id}">${esc(v.name)}</button>`).join(''); }
function renderHome(){const m=metrics(),score=health(),mk=monthKey(today()),monthFuel=entries().filter(x=>monthKey(x.date)===mk).reduce((s,x)=>s+(+x.total||0),0),monthExp=expenses().filter(x=>monthKey(x.date)===mk).reduce((s,x)=>s+(+x.amount||0),0);$('#avgKml').textContent=m.kml?fmt(toDisplayEfficiency(m.kml),1):'—';$('#kmlUnit')&&($('#kmlUnit').textContent=efficiencyUnit());$('#healthScore').textContent=score;$('#healthScore').style.borderColor=score>=85?'var(--green)':score>=65?'var(--accent)':'var(--red)';$('#homeMetrics').innerHTML=metric('ค่าใช้จ่ายเดือนนี้',money(monthFuel+monthExp),`${entries().filter(x=>monthKey(x.date)===mk).length+expenses().filter(x=>monthKey(x.date)===mk).length} รายการ`)+metric('ต้นทุนเชื้อเพลิง',m.costKm?`${money(toDisplayCostPerDist(m.costKm))}/${distUnit()}`:'—',`รวม ${money(m.spent)}`)+metric(`เลข${distUnit()}ล่าสุด`,currentOdo()?fmtDist(currentOdo()):'—',`${entries().length} ครั้งเติม`)+metric('ค่าใช้จ่ายสะสม',money(m.spent+expenses().reduce((s,x)=>s+(+x.amount||0),0)),'รวมทั้งหมด');drawChart();const latest=[...entries().slice(-3).reverse().map(x=>({icon:'⛽',title:x.station||x.fuelType||'เติมน้ำมัน',sub:`${x.date} • ${fmtDist(x.odometer)}`,amount:money(x.total)})),...expenses().slice(0,2).map(x=>({icon:'🔧',title:x.title||x.category,sub:`${x.date} • ${x.category||'อื่นๆ'}`,amount:money(x.amount)}))].slice(0,4);$('#latestList').innerHTML=latest.length?latest.map(rowHtml).join(''):'<div class="empty">ยังไม่มีข้อมูล</div>';const due=dueItems().slice(0,4);$('#dueList').innerHTML=due.length?due.map(x=>`<div class="due"><span>${esc(x.name)}</span><b class="status-${x.status}">${x.label}</b></div>`).join(''):'<div class="empty">ยังไม่ได้ตั้งรอบบำรุง</div>';}
function rowHtml(x){return `<article class="record"><div class="ico">${x.icon}</div><div><b>${esc(x.title)}</b><small>${esc(x.sub)}</small></div><div class="amount">${x.amount}</div></article>`;}
function drawChart(){const data=monthSeries(),svg=$('#monthlyChart'),max=Math.max(1,...data.map(x=>x.value)),w=320,h=115,p=15;const pts=data.map((x,i)=>[p+i*((w-p*2)/(Math.max(1,data.length-1))),h-p-(x.value/max)*(h-p*2)]);svg.innerHTML=`<path d="${pts.map((q,i)=>(i?'L':'M')+q.join(' ')).join(' ')}" fill="none" stroke="var(--accent)" stroke-width="3" stroke-linecap="round"/><path d="M${pts[0]?.[0]||0} ${h-p} ${pts.map(q=>'L'+q.join(' ')).join(' ')} L${pts.at(-1)?.[0]||0} ${h-p}Z" fill="rgba(244,168,59,.10)"/>${pts.map((q,i)=>`<circle cx="${q[0]}" cy="${q[1]}" r="3" fill="var(--accent)"/><text x="${q[0]}" y="128" fill="var(--muted)" font-size="9" text-anchor="middle">${data[i].label}</text>`).join('')}`;const a=data.at(-2)?.value||0,b=data.at(-1)?.value||0;$('#trendText').textContent=a?`${b>=a?'▲':'▼'} ${fmt(Math.abs((b-a)/a*100))}%`:' ';}
function renderFuel(){const q=$('#fuelSearch').value.toLowerCase(),p=$('#fuelPeriod').value;const arr=entries().slice().reverse().filter(x=>withinPeriod(x.date,p)&&JSON.stringify(x).toLowerCase().includes(q));$('#fuelList').innerHTML=arr.length?arr.map(x=>`<article class="record" data-edit-fuel="${x.id}"><div class="ico">⛽</div><div><b>${esc(x.station||x.fuelType||'เติมน้ำมัน')}</b><small>${x.date}${x.time?' '+x.time:''} • ${fmtDist(x.odometer)} • ${fmtVol(x.liters)}</small></div><div class="amount">${money(x.total)}<small>${x.pricePerLiter?fmt(toDisplayPricePerVol(x.pricePerLiter),2)+' บ./'+volUnit():''}</small></div></article>`).join(''):'<div class="empty">ไม่พบรายการ</div>';}
function renderExpenses(){const q=$('#expenseSearch').value.toLowerCase(),p=$('#expensePeriod').value,arr=expenses().filter(x=>withinPeriod(x.date,p)&&JSON.stringify(x).toLowerCase().includes(q));const sum=arr.reduce((s,x)=>s+(+x.amount||0),0);$('#expenseMetrics').innerHTML=metric('ยอดรวม',money(sum),`${arr.length} รายการ`)+metric('เฉลี่ย/รายการ',arr.length?money(sum/arr.length):'—','ตามตัวกรอง');$('#expenseList').innerHTML=arr.length?arr.map(x=>`<article class="record" data-edit-expense="${x.id}"><div class="ico">🔧</div><div><b>${esc(x.title||x.category)}</b><small>${x.date} • ${esc(x.category||'อื่นๆ')}${x.odometer?' • '+fmtDist(x.odometer):''}</small></div><div class="amount">${money(x.amount)}</div></article>`).join(''):'<div class="empty">ยังไม่มีค่าใช้จ่าย</div>';}
function renderMaintenance(){const arr=dueItems();$('#maintenanceList').innerHTML=arr.length?arr.map(x=>`<article class="record" data-edit-reminder="${x.id}"><div class="ico">🔧</div><div><b>${esc(x.name)}</b><small>${x.nextOdo?'ที่ '+fmtDist(x.nextOdo):''}${x.nextDate?' • '+x.nextDate:''}${(x.repeatOdo||x.repeatMonths)?' • 🔁 ทำซ้ำ':''}</small></div><div style="text-align:right;"><div class="amount status-${x.status}">${x.label}</div><button class="secondary" data-done-reminder="${x.id}" style="margin-top:6px;padding:6px 10px;font-size:11px;">✓ เสร็จแล้ว</button></div></article>`).join(''):'<div class="empty">ยังไม่มีรายการเตือน</div>';}
function renderAll(){renderVehicleStrip();renderHome();renderFuel();renderExpenses();renderMaintenance();$('#pageEyebrow').textContent=vehicle()?.name||'รถของฉัน';save();}

function showForm(type,obj={}){const d=$('#formDialog'),b=$('#formBody');$('#formTitle').textContent=type==='fuel'?(obj.id?'แก้ไขการเติมน้ำมัน':'เพิ่มรายการเติมน้ำมัน'):type==='expense'?(obj.id?'แก้ไขค่าใช้จ่าย':'เพิ่มค่าใช้จ่าย'):'ตั้งเตือนบำรุงรักษา';d.dataset.type=type;d.dataset.id=obj.id||'';
 if(type==='fuel')b.innerHTML=`<div class="photo-grid"><label class="photo-pick">🧾 รูปใบเสร็จ<input hidden type="file" id="receiptFile" accept="image/*" capture="environment"></label><label class="photo-pick">🔢 รูปเรือนไมล์<input hidden type="file" id="odoFile" accept="image/*" capture="environment"></label></div><div class="form-grid"><div class="field"><label>วันที่</label><input name="date" type="date" value="${obj.date||today()}"></div><div class="field"><label>เวลา</label><input name="time" type="time" value="${obj.time||nowTime()}"></div><div class="field"><label>เลข${distUnit()}</label><input name="odometer" type="number" step=".01" value="${dispDistVal(obj.odometer)}"></div><div class="field"><label>${volUnit()}</label><input name="liters" type="number" step=".001" value="${dispVolVal(obj.liters)}"></div><div class="field"><label>ราคา/${volUnit()}</label><input name="pricePerLiter" type="number" step=".01" value="${obj.pricePerLiter?fmt(toDisplayPricePerVol(obj.pricePerLiter),2):''}"></div><div class="field"><label>ยอดรวม</label><input name="total" type="number" step=".01" value="${obj.total||''}"></div><div class="field"><label>เชื้อเพลิง</label><input name="fuelType" value="${esc(obj.fuelType||'')}"></div><div class="field full"><label>ปั๊ม</label><input id="stationInput" name="station" value="${esc(obj.station||'')}" placeholder="กำลังค้นหาปั๊มใกล้ฉัน…"><div id="formNearby" class="nearby-options"></div></div><div class="field full"><label>หมายเหตุ</label><textarea name="note">${esc(obj.note||'')}</textarea></div><label class="field full"><input name="full" type="checkbox" ${obj.full!==false?'checked':''}> เติมเต็มถัง</label></div>`;
 if(type==='expense')b.innerHTML=`<div class="form-grid"><div class="field"><label>วันที่</label><input name="date" type="date" value="${obj.date||today()}"></div><div class="field"><label>เลข${distUnit()}</label><input name="odometer" type="number" step=".01" value="${dispDistVal(obj.odometer)}"></div><div class="field full"><label>รายการ</label><input name="title" value="${esc(obj.title||'')}"></div><div class="field"><label>หมวด</label><select name="category">${['น้ำมันเครื่อง','ของเหลว/ไส้กรอง','เบรก','ยางและล้อ','ช่วงล่าง','แบตเตอรี่/ไฟฟ้า','เครื่องยนต์','เกียร์','แอร์','ไฮบริด/EV','ประกัน','พ.ร.บ.','ภาษี','ค่าจอด','ทางด่วน','ล้างรถ','อื่นๆ'].map(x=>`<option ${obj.category===x?'selected':''}>${x}</option>`).join('')}</select></div><div class="field"><label>จำนวนเงิน</label><input name="amount" type="number" step=".01" value="${obj.amount||''}"></div><div class="field full"><label>หมายเหตุ</label><textarea name="note">${esc(obj.note||'')}</textarea></div></div>`;
 if(type==='reminder')b.innerHTML=`<div class="form-grid"><div class="field full"><label>รายการ</label><input name="name" value="${esc(obj.name||'เปลี่ยนน้ำมันเครื่อง')}"></div><div class="field"><label>กำหนดที่เลข${distUnit()}</label><input name="nextOdo" type="number" step=".01" value="${dispDistVal(obj.nextOdo)}"></div><div class="field"><label>กำหนดวันที่</label><input name="nextDate" type="date" value="${obj.nextDate||''}"></div><div class="field"><label>ทำซ้ำทุก (${distUnit()}) — ถ้ามี</label><input name="repeatOdo" type="number" step=".01" value="${dispDistVal(obj.repeatOdo)}"></div><div class="field"><label>ทำซ้ำทุก (เดือน) — ถ้ามี</label><input name="repeatMonths" type="number" value="${obj.repeatMonths||''}"></div><p class="muted full" style="grid-column:1/-1;">ถ้าใส่ "ทำซ้ำ" ไว้ กด "✓ เสร็จแล้ว" ที่รายการนี้ในหน้าบำรุงรักษาจะเลื่อนกำหนดครั้งถัดไปให้อัตโนมัติ แทนที่จะลบทิ้ง</p></div>`;
 d.showModal(); if(type==='fuel'&&!obj.station)setTimeout(autoNearby,150);}
async function saveForm(e){e.preventDefault();const d=$('#formDialog'),type=d.dataset.type,idv=d.dataset.id||uid(),data=Object.fromEntries(new FormData($('#dynamicForm')));
 if(type==='fuel'){
   const odoRaw=+data.odometer||0, litersRaw=+data.liters||0;
   if(odoRaw<=0||litersRaw<=0){ alert(`กรอกเลข${distUnit()}และปริมาณ${volUnit()}ให้ครบก่อนบันทึก`); return; }
   data.id=idv;data.vehicleId=state.currentVehicleId;data.odometer=toCanonicalDist(odoRaw);data.liters=toCanonicalVol(litersRaw);data.pricePerLiter=toCanonicalPricePerVol(+data.pricePerLiter||0);data.total=+data.total||0;data.full=$('[name="full"]')?.checked??true;if(!data.pricePerLiter&&data.total&&data.liters)data.pricePerLiter=data.total/data.liters;const old=state.entries.findIndex(x=>x.id===idv);old>=0?state.entries[old]=data:state.entries.push(data);await uploadAttachedPhotos(idv);
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
async function autoNearby(){const box=$('#formNearby'),input=$('#stationInput');if(!box)return;box.innerHTML='<div class="muted">กำลังค้นหาตำแหน่ง…</div>';try{const stations=await fetchNearbyStations();nearbyCache=stations;if(stations[0]&&!input.value)input.value=stations[0].name;renderNearby(stations,box);renderNearby(stations,$('#nearbyList'));}catch(e){box.innerHTML='<div class="muted">ค้นหาไม่ได้ กรุณาอนุญาตตำแหน่งหรือพิมพ์ชื่อปั๊มเอง</div>';}}
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
async function refreshFuelNearby(){
  const box=$('#nearbyList');if(!box)return;
  box.innerHTML='<div class="muted">กำลังค้นหาตำแหน่ง…</div>';
  try{
    const stations=await fetchNearbyStations();
    nearbyCache=stations;
    renderNearby(stations, box);
  }catch(e){ box.innerHTML='<div class="muted">ค้นหาไม่ได้ กรุณาอนุญาตตำแหน่ง</div>'; }
}

function renderTodayPrices(data){
  if(!data||data.status!=='success'||!data.response||typeof data.response!=='object') return null;
  const r=data.response;
  const brands=[{key:'ptt',label:'ปตท.'},{key:'bcp',label:'บางจาก'},{key:'shell',label:'เชลล์'},{key:'esso',label:'เอสโซ่'},{key:'caltex',label:'คาลเท็กซ์'},{key:'pt',label:'พีที'},{key:'susco',label:'ซัสโก้'}];
  const fuels=[{key:'gasohol_95',label:'95'},{key:'gasohol_91',label:'91'},{key:'diesel_b7',label:'ดีเซล B7'}];
  const isValid=v=>{ if(v===null||v===undefined||v==='') return false; const n=parseFloat(v); return !isNaN(n)&&n>0&&n<200; };
  const rows=brands.map(b=>{
    const st=r.stations&&typeof r.stations==='object'?r.stations[b.key]:null;
    if(!st||typeof st!=='object') return '';
    const parts=fuels.map(f=>(st[f.key]&&isValid(st[f.key].price))?`${f.label} ฿${parseFloat(st[f.key].price).toFixed(2)}`:null).filter(Boolean);
    if(!parts.length) return '';
    return `<div class="list-row"><b>${b.label}</b><span>${parts.join(' · ')}</span></div>`;
  }).filter(Boolean).join('');
  if(!rows) return null;
  const dateLine=(typeof r.date==='string'&&r.date.trim())?`<div class="muted" style="margin-bottom:6px;font-size:10.5px;">อัปเดตล่าสุด: ${esc(r.date)}</div>`:'';
  return dateLine+rows;
}
async function loadTodayPrices(){
  const box=$('#todayPriceList');if(!box)return;
  box.innerHTML='<div class="muted">กำลังโหลด…</div>';
  const SRC='https://api.chnwt.dev/thai-oil-api/latest';
  const attempts=[
    ()=>fetch(SRC),
    ()=>fetch('https://api.allorigins.win/raw?url='+encodeURIComponent(SRC)), // fallback in case the API blocks direct browser (CORS) requests
  ];
  for(const attempt of attempts){
    try{
      const res=await attempt();
      if(!res.ok) continue;
      const data=await res.json();
      const html=renderTodayPrices(data);
      if(html){ box.innerHTML=html; return; }
    }catch(e){ /* try next method */ }
  }
  box.innerHTML='<div class="muted">โหลดราคาไม่สำเร็จตอนนี้ (แหล่งข้อมูลอาจไม่พร้อมใช้งานชั่วคราว) — <a href="https://gasprice.kapook.com/" target="_blank" rel="noopener" style="color:var(--accent)">เปิดดูที่ kapook แทน</a></div>';
}

async function uploadAttachedPhotos(logId){if(!user)return;const files=[['receipt',$('#receiptFile')?.files[0]],['odometer',$('#odoFile')?.files[0]]].filter(x=>x[1]);for(const [type,file] of files){const path=`vehicles/${state.currentVehicleId}/fuel/${logId}/${type}-${Date.now()}-${file.name.replace(/[^\w.-]/g,'_')}`;const sr=ref(storage,path);await uploadBytes(sr,file,{contentType:file.type,customMetadata:{uploadedBy:user.uid}});const url=await getDownloadURL(sr);await setDoc(doc(db,'vehicles',state.currentVehicleId,'photos',uid()),{type,path,url,name:file.name,logId,uploadedBy:user.uid,createdAt:serverTimestamp()});}}

function openPanel(name){const d=$('#panelDialog');$('#panelTitle').textContent={family:'ครอบครัวและ Cloud',trips:'ทริปและหน้างาน',gallery:'รูปและเอกสาร',reports:'รายงาน',backup:'สำรองและนำเข้า',vehicles:'จัดการรถ',search:'ค้นหาทุกอย่าง',settings:'การตั้งค่า'}[name];d.dataset.panel=name;renderPanel(name);d.showModal();}
function renderPanel(name){const b=$('#panelBody');if(name==='family')b.innerHTML=familyPanel();if(name==='trips')b.innerHTML=tripsPanel();if(name==='gallery'){b.innerHTML='<div id="galleryBody" class="muted">กำลังโหลด…</div>';loadGallery();}if(name==='reports')b.innerHTML=reportsPanel();if(name==='backup')b.innerHTML=backupPanel();if(name==='vehicles')b.innerHTML=vehiclesPanel();if(name==='search')b.innerHTML=searchPanel();if(name==='settings')b.innerHTML=settingsPanel();bindPanel();}
function familyPanel(){return user?`<div class="card"><div class="user-card"><img src="${esc(user.photoURL||'icon-192.png')}" referrerpolicy="no-referrer"><div><b>${esc(user.displayName||user.email)}</b><small>${esc(user.email)}</small></div></div><div class="panel-actions" style="margin-top:12px"><button class="secondary" id="signOutBtn">ออกจากระบบ</button><button class="primary" id="syncBtn">ซิงก์ข้อมูลยานพาหนะ</button></div></div><div class="card"><h2>สมาชิก</h2><div id="membersBody" class="muted">กำลังโหลด…</div></div><div class="card"><h2>สร้างรหัสเชิญ</h2><input id="inviteEmail" type="email" placeholder="Gmail สมาชิก"><select id="inviteRole"><option value="editor">Editor — เพิ่มและแก้ไข</option><option value="viewer">Viewer — ดูอย่างเดียว</option></select><button class="primary" id="inviteBtn" style="margin-top:8px">สร้างรหัส</button><div id="inviteResult" class="muted"></div></div><div class="card"><h2>เข้าร่วมรถ</h2><input id="joinCode" maxlength="8" placeholder="รหัสเชิญ 8 ตัว"><button class="primary" id="joinBtn" style="margin-top:8px">เข้าร่วม</button></div>`:`<div class="card"><h2>แชร์รถกับครอบครัว</h2><p class="muted">ใช้บัญชี Google เดียวสำหรับ Firebase, Firestore และ Storage ไม่ต้องล็อกอิน Google Drive แยก</p><button class="google-btn" id="loginBtn"><b style="color:#4285f4">G</b> เข้าสู่ระบบด้วย Google</button><div id="authMessage" class="muted"></div></div>`;}
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
  const m=metrics(),exp=expenses().reduce((s,x)=>s+(+x.amount||0),0),total=m.spent+exp;
  const segs=categoryBreakdown(), cmp=periodComparison();
  return `<div class="metric-grid">${metric('น้ำมันทั้งหมด',money(m.spent),fmtVol(m.lit))}${metric('ค่าใช้จ่ายอื่น',money(exp),`${expenses().length} รายการ`)}${metric(efficiencyUnit(),m.kml?fmt(toDisplayEfficiency(m.kml),1):'—','เติมเต็มถัง')}${metric(`ต้นทุน/${distUnit()}`,m.dist?money(toDisplayCostPerDist(total/m.dist)):'—',m.dist?fmtDist(m.dist):'')}</div>
  <div class="card"><h2>สัดส่วนค่าใช้จ่ายตามหมวด</h2><div style="display:flex;align-items:center;gap:16px;flex-wrap:wrap;">${donutSVG(segs)}<div style="flex:1;min-width:140px;">${segs.length?segs.map(s=>`<div class="list-row" style="padding:6px 0;"><div style="display:flex;align-items:center;gap:8px;"><span style="width:10px;height:10px;border-radius:50%;background:${s.color};display:inline-block;"></span>${esc(s.label)}</div><b>${money(s.value)}</b></div>`).join(''):'<div class="muted">ยังไม่มีข้อมูล</div>'}</div></div></div>
  <div class="card"><h2>เทียบเดือนนี้กับเดือนก่อน</h2><div class="metric-grid">${metric('เดือนนี้',money(cmp.cur),'')}${metric('เดือนก่อน',money(cmp.prev),'')}</div><div class="muted" style="margin-top:8px;">${cmp.diff===null?'ยังไม่มีข้อมูลเดือนก่อนสำหรับเทียบ':`${cmp.diff>=0?'▲ เพิ่มขึ้น':'▼ ลดลง'} ${fmt(Math.abs(cmp.diff),1)}%`}</div></div>
  <div class="panel-actions"><button class="primary" id="exportJsonBtn">Export JSON</button><button class="secondary" id="exportCsvBtn">Export CSV</button><button class="secondary" id="printBtn">พิมพ์/PDF</button></div>`;
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
  return `<div class="card"><h2>หน่วยที่ใช้แสดงผล</h2>
    <div class="field"><label>ระยะทาง</label><select id="unitDistance"><option value="km" ${state.units?.distance!=='mi'?'selected':''}>กิโลเมตร (กม.)</option><option value="mi" ${state.units?.distance==='mi'?'selected':''}>ไมล์ (mi)</option></select></div>
    <div class="field"><label>ปริมาตรน้ำมัน</label><select id="unitVolume"><option value="liters" ${state.units?.volume!=='gal'?'selected':''}>ลิตร (L)</option><option value="gal" ${state.units?.volume==='gal'?'selected':''}>แกลลอน (US gal)</option></select></div>
    <p class="muted">ข้อมูลจะยังเก็บเป็นกิโลเมตร/ลิตรอยู่เบื้องหลังเสมอ (เผื่อย้ายเครื่องหรือประเทศ) แค่แสดงผลเป็นหน่วยที่เลือกไว้เท่านั้น เปลี่ยนได้ตลอดเวลาไม่กระทบข้อมูลเดิม</p></div>`;
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
async function pullVehicle(){await ensureUser();for(const [name,target] of [['entries',state.entries],['expenses',state.expenses],['reminders',state.reminders],['trips',state.trips]]){const s=await getDocs(collection(db,'vehicles',state.currentVehicleId,name));const map=new Map(target.map((x,i)=>[x.id,i]));s.forEach(d=>{const x=d.data();map.has(x.id)?target[map.get(x.id)]=x:target.push(x)});}save();renderAll();toast('ดึงข้อมูลแล้ว');}
async function loadMembers(){const box=$('#membersBody');if(!box||!user)return;const s=await getDoc(doc(db,'vehicles',state.currentVehicleId));if(!s.exists()){box.innerHTML='รถคันนี้ยังไม่ขึ้น Cloud';return;}const d=s.data();box.innerHTML=Object.values(d.members||{}).map(m=>`<div class="list-row"><div><b>${esc(m.displayName||m.email)}</b><small>${esc(m.email||'')}</small></div><b>${esc(m.role)}</b></div>`).join('');}
async function invite(){try{const email=$('#inviteEmail').value.trim().toLowerCase(),role=$('#inviteRole').value,s=await ensureCloudVehicle();if(s.data().ownerUid!==user.uid)throw new Error('เฉพาะ Owner สร้างคำเชิญได้');const code=Math.random().toString(36).slice(2,10).toUpperCase();await setDoc(doc(db,'invites',code),{vehicleId:state.currentVehicleId,vehicleName:vehicle().name,emailLower:email,role,ownerUid:user.uid,expiresAt:Date.now()+7*864e5,createdAt:serverTimestamp()});$('#inviteResult').innerHTML=`รหัสเชิญ: <b>${code}</b> (7 วัน)`;}catch(e){alert(e.message)}}
async function join(){try{await ensureUser();const code=$('#joinCode').value.trim().toUpperCase(),ir=doc(db,'invites',code),is=await getDoc(ir);if(!is.exists())throw new Error('ไม่พบรหัส');const inv=is.data();if(inv.expiresAt<Date.now())throw new Error('รหัสหมดอายุ');if(inv.emailLower&&inv.emailLower!==user.email.toLowerCase())throw new Error('รหัสนี้ไม่ใช่ของบัญชีนี้');const vr=doc(db,'vehicles',inv.vehicleId),vs=await getDoc(vr),vd=vs.data(),members={...(vd.members||{}),[user.uid]:{role:inv.role,email:user.email,displayName:user.displayName||''}};await setDoc(vr,{members,lastJoinCode:code},{merge:true});if(!state.vehicles.some(v=>v.id===inv.vehicleId))state.vehicles.push({id:inv.vehicleId,name:inv.vehicleName||vd.name});state.currentVehicleId=inv.vehicleId;save();renderAll();toast('เข้าร่วมสำเร็จ');}catch(e){alert(e.message)}}
async function loadGallery(){const box=$('#galleryBody');await initFirebase();if(!user){box.innerHTML='กรุณาเข้าสู่ระบบก่อน';return;}try{const s=await getDocs(collection(db,'vehicles',state.currentVehicleId,'photos')),arr=s.docs.map(d=>({id:d.id,...d.data()}));box.innerHTML=`<div class="panel-actions"><label class="primary">＋ อัปโหลด<input hidden type="file" id="galleryUpload" accept="image/*,application/pdf"></label></div><div class="gallery">${arr.map(x=>`<article>${String(x.contentType||'').startsWith('image/')?`<img src="${esc(x.url)}">`:'<div style="padding:30px;text-align:center">📄</div>'}<div><b>${esc(x.name)}</b><br><a href="${esc(x.url)}" target="_blank">เปิด</a></div></article>`).join('')}</div>`;$('#galleryUpload')?.addEventListener('change',uploadGallery);}catch(e){box.textContent=e.message;}}
async function uploadGallery(e){const f=e.target.files[0];if(!f)return;await ensureCloudVehicle();const path=`vehicles/${state.currentVehicleId}/gallery/${Date.now()}-${f.name.replace(/[^\w.-]/g,'_')}`,r=ref(storage,path);await uploadBytes(r,f,{contentType:f.type,customMetadata:{uploadedBy:user.uid}});const url=await getDownloadURL(r);await setDoc(doc(db,'vehicles',state.currentVehicleId,'photos',uid()),{name:f.name,path,url,contentType:f.type,uploadedBy:user.uid,createdAt:serverTimestamp()});loadGallery();}

function bindPanel(){ $('#loginBtn')?.addEventListener('click',login);$('#signOutBtn')?.addEventListener('click',async()=>{try{await requireFirebase();await signOut(auth);}catch(e){alert(e.message);}});$('#syncBtn')?.addEventListener('click',syncVehicle);$('#inviteBtn')?.addEventListener('click',invite);$('#joinBtn')?.addEventListener('click',join);$('#gpsStartBtn')?.addEventListener('click',startGpsTrip);$('#gpsStopBtn')?.addEventListener('click',stopGpsTrip);$('#saveTripBtn')?.addEventListener('click',()=>{const x={id:uid(),vehicleId:state.currentVehicleId,name:$('#tripName').value||'ทริป',date:$('#tripDate').value,distance:toCanonicalDist(+$('#tripDistance').value||0),fuel:+$('#tripFuel').value||0,toll:+$('#tripToll').value||0,parking:+$('#tripParking').value||0,food:+$('#tripFood').value||0,other:+$('#tripOther').value||0};state.trips.push(x);save();renderPanel('trips');if(user)syncVehicle();});$$('#exportJsonBtn').forEach(x=>x.onclick=exportJSON);$$('#exportCsvBtn').forEach(x=>x.onclick=exportCSV);$('#printBtn')?.addEventListener('click',()=>window.print());$('#importBtn')?.addEventListener('click',()=>$('#importFile').click());$('#importFile')?.addEventListener('change',e=>e.target.files[0]&&importFile(e.target.files[0]));$('#addVehicleBtn')?.addEventListener('click',()=>{const name=prompt('ชื่อรถ');if(name){const v={id:uid(),name};state.vehicles.push(v);state.currentVehicleId=v.id;save();renderAll();renderPanel('vehicles');}});$$('[data-rename-vehicle]').forEach(x=>x.onchange=()=>{state.vehicles.find(v=>v.id===x.dataset.renameVehicle).name=x.value||'รถ';save();renderAll();});$$('[data-delete-vehicle]').forEach(x=>x.onclick=()=>{if(state.vehicles.length<2)return alert('ต้องมีรถอย่างน้อย 1 คัน');if(confirm('ลบรถและข้อมูลในเครื่องของรถนี้?')){const idv=x.dataset.deleteVehicle;state.vehicles=state.vehicles.filter(v=>v.id!==idv);['entries','expenses','reminders','trips'].forEach(k=>state[k]=state[k].filter(a=>a.vehicleId!==idv));state.currentVehicleId=state.vehicles[0].id;save();renderAll();renderPanel('vehicles');}});$('#globalSearchInput')?.addEventListener('input',runGlobalSearch);$('#unitDistance')?.addEventListener('change',e=>{state.units=state.units||{};state.units.distance=e.target.value;save();renderAll();});$('#unitVolume')?.addEventListener('change',e=>{state.units=state.units||{};state.units.volume=e.target.value;save();renderAll();});if(user)loadMembers();}
function download(name,text,type='application/json'){const a=document.createElement('a');a.href=URL.createObjectURL(new Blob([text],{type}));a.download=name;a.click();setTimeout(()=>URL.revokeObjectURL(a.href),500);}
function exportJSON(){download(`fuellog-${today()}.json`,JSON.stringify({version:5,...state,exportedAt:new Date().toISOString()},null,2));}
function exportCSV(){const rows=[['type','vehicleId','date','odometer','liters','amount','category','title','station','note']];state.entries.forEach(x=>rows.push(['fuel',x.vehicleId,x.date,x.odometer,x.liters,x.total,'','',x.station||'',x.note||'']));state.expenses.forEach(x=>rows.push(['expense',x.vehicleId,x.date,x.odometer||'','',x.amount,x.category||'',x.title||'','',x.note||'']));download(`fuellog-${today()}.csv`,rows.map(r=>r.map(v=>`"${String(v??'').replaceAll('"','""')}"`).join(',')).join('\n'),'text/csv;charset=utf-8');}
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
function bind(){$$('[data-nav]').forEach(x=>x.onclick=()=>{renderNav(x.dataset.nav);if(x.dataset.nav==='fuel'){renderFuel();refreshFuelNearby();}if(x.dataset.nav==='expense')renderExpenses();if(x.dataset.nav==='maintenance')renderMaintenance();if(x.dataset.nav==='home')refreshHomeNearby();});$$('[data-go]').forEach(x=>x.onclick=()=>{renderNav(x.dataset.go);});document.addEventListener('click',e=>{const v=e.target.closest('[data-vehicle]');if(v){state.currentVehicleId=v.dataset.vehicle;renderAll();}const done=e.target.closest('[data-done-reminder]');if(done){e.stopPropagation();markReminderDone(done.dataset.doneReminder);return;}const f=e.target.closest('[data-edit-fuel]');if(f)showForm('fuel',state.entries.find(x=>x.id===f.dataset.editFuel));const c=e.target.closest('[data-edit-expense]');if(c)showForm('expense',state.expenses.find(x=>x.id===c.dataset.editExpense));const r=e.target.closest('[data-edit-reminder]');if(r)showForm('reminder',state.reminders.find(x=>x.id===r.dataset.editReminder));const st=e.target.closest('[data-station]');if(st){const si=$('#stationInput');if(si){si.value=st.dataset.station;}else{showForm('fuel',{station:st.dataset.station});}}});$('#addFuelBtn').onclick=()=>showForm('fuel');$('#addExpenseBtn').onclick=()=>showForm('expense');$('#addReminderBtn').onclick=()=>showForm('reminder');$('#formCloseX').onclick=()=>$('#formDialog').close();$('#formCancelBtn').onclick=()=>$('#formDialog').close();$('#dynamicForm').addEventListener('submit',saveForm);$('#fuelSearch').oninput=renderFuel;$('#fuelPeriod').onchange=renderFuel;$('#expenseSearch').oninput=renderExpenses;$('#expensePeriod').onchange=renderExpenses;$('#refreshNearby').onclick=refreshFuelNearby;$('#refreshHomeNearby').onclick=refreshHomeNearby;$('#refreshTodayPrice').onclick=loadTodayPrices;$$('[data-panel]').forEach(x=>x.onclick=()=>openPanel(x.dataset.panel));$('#closePanel').onclick=()=>$('#panelDialog').close();$('#themeBtn').onclick=()=>{state.theme=state.theme==='dark'?'light':'dark';document.body.classList.toggle('light',state.theme==='light');save();};}

function boot(){
  try{
    load();
    document.body.classList.toggle('light',state.theme==='light');
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
  if('serviceWorker' in navigator) navigator.serviceWorker.register('./sw.js?v=5.0.3').catch(console.warn);
}

if(document.readyState === 'loading') document.addEventListener('DOMContentLoaded', boot, {once:true});
else boot();
