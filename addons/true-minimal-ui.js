(() => {
  const ready = (fn) => document.readyState === 'loading' ? document.addEventListener('DOMContentLoaded', fn) : fn();
  ready(() => {
    if (document.querySelector('.fl-min-nav')) return;
    const nav = document.createElement('nav');
    nav.className = 'fl-min-nav';
    nav.setAttribute('aria-label','เมนูหลัก');
    nav.innerHTML = `
      <button class="active" data-action="home">⌂<span>หน้าหลัก</span></button>
      <button data-action="fuel">⛽<span>เติมน้ำมัน</span></button>
      <button data-action="cost">▣<span>ค่าใช้จ่าย</span></button>
      <button data-action="maint">🔧<span>บำรุงรักษา</span></button>
      <button data-action="more">☰<span>เพิ่มเติม</span></button>`;
    document.body.appendChild(nav);
    const activate = btn => { nav.querySelectorAll('button').forEach(x=>x.classList.toggle('active',x===btn)); };
    nav.addEventListener('click', e => {
      const btn=e.target.closest('button'); if(!btn) return; activate(btn);
      switch(btn.dataset.action){
        case 'home': window.scrollTo({top:0,behavior:'smooth'}); break;
        case 'fuel': document.getElementById('addBtn')?.click(); break;
        case 'cost': document.getElementById('tabCosts')?.click(); setTimeout(()=>document.getElementById('addBtn')?.click(),60); break;
        case 'maint': document.getElementById('remindersBtn')?.click(); break;
        case 'more': document.getElementById('settingsBtn')?.click(); break;
      }
    });
  });
})();
