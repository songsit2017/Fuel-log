(() => {
  const ready = (fn) => document.readyState === 'loading'
    ? document.addEventListener('DOMContentLoaded', fn, { once: true })
    : fn();

  ready(() => {
    if (document.querySelector('.fl-shell')) return;

    const $ = (id) => document.getElementById(id);
    const body = document.body;

    // ----- Build true SPA page containers -----
    const shell = document.createElement('main');
    shell.className = 'fl-shell';
    shell.innerHTML = `
      <section class="fl-page active" data-page="home" aria-label="หน้าหลัก"></section>
      <section class="fl-page" data-page="fuel" aria-label="รายการเติมน้ำมัน"></section>
      <section class="fl-page" data-page="cost" aria-label="ค่าใช้จ่าย"></section>
      <section class="fl-page" data-page="maint" aria-label="บำรุงรักษา"></section>
      <section class="fl-page" data-page="more" aria-label="เพิ่มเติม"></section>`;

    const firstContent = document.querySelector('.header');
    body.insertBefore(shell, firstContent || body.firstChild);

    const page = (name) => shell.querySelector(`[data-page="${name}"]`);
    const move = (node, target) => node && target && target.appendChild(node);
    const createHeader = (title, subtitle = '', actionHtml = '') => {
      const el = document.createElement('header');
      el.className = 'fl-page-head';
      el.innerHTML = `<div><h1>${title}</h1>${subtitle ? `<p>${subtitle}</p>` : ''}</div>${actionHtml}`;
      return el;
    };

    // Home: only the summary dashboard.
    move(document.querySelector('.header'), page('home'));
    move($('monthlyChartWrap'), page('home'));
    move($('chartWrap'), page('home'));
    move($('insightCard'), page('home'));

    // Fuel list page.
    page('fuel').appendChild(createHeader('เติมน้ำมัน', 'ประวัติการเติมและสถานีใกล้คุณ', '<button class="fl-head-add" data-add="fuel">＋ เพิ่ม</button>'));
    const fuelTools = document.createElement('div');
    fuelTools.className = 'fl-page-stack';
    page('fuel').appendChild(fuelTools);
    move(document.querySelector('.tool-card:not(#insightCard)'), fuelTools); // search/filter card
    move($('homeStationCard'), fuelTools);
    move($('todayPriceCard'), fuelTools);
    move($('list'), fuelTools);

    // Cost page.
    page('cost').appendChild(createHeader('ค่าใช้จ่าย', 'ซ่อมบำรุง ประกัน ภาษี และค่าใช้จ่ายอื่น', '<button class="fl-head-add" data-add="cost">＋ เพิ่ม</button>'));
    const costSummary = document.createElement('div');
    costSummary.className = 'fl-cost-summary';
    page('cost').appendChild(costSummary);
    move($('costStatGrid'), costSummary);
    move($('costList'), page('cost'));

    // Maintenance page.
    page('maint').appendChild(createHeader('บำรุงรักษา', 'งานที่ครบกำหนดตามวันหรือเลขไมล์', '<button class="fl-head-add" data-add="maint">＋ เพิ่มเตือน</button>'));
    move($('reminderSummaryCard'), page('maint'));
    const maintOpen = document.createElement('button');
    maintOpen.className = 'fl-large-action';
    maintOpen.innerHTML = '<span>🔧</span><div><b>จัดการตารางบำรุงรักษา</b><small>เพิ่ม แก้ไข หรือตรวจงานที่ใกล้ครบกำหนด</small></div><i>›</i>';
    maintOpen.addEventListener('click', () => $('remindersBtn')?.click());
    page('maint').appendChild(maintOpen);

    // More page.
    page('more').appendChild(createHeader('เพิ่มเติม', 'บัญชี การแชร์ ข้อมูล และเครื่องมือ'));
    const moreGrid = document.createElement('div');
    moreGrid.className = 'fl-more-grid';
    moreGrid.innerHTML = `
      <button data-more="family"><span>👥</span><b>สมาชิกครอบครัว</b><small>แชร์รถและกำหนดสิทธิ์</small></button>
      <button data-more="pro"><span>✨</span><b>รายงาน PRO</b><small>วิเคราะห์และส่งออกรายงาน</small></button>
      <button data-more="settings"><span>⚙️</span><b>ตั้งค่า</b><small>ธีม สำรองข้อมูล และ Google Drive</small></button>
      <button data-more="export"><span>⬇️</span><b>ส่งออกข้อมูล</b><small>สำรองเป็น JSON / CSV</small></button>
      <button data-more="import"><span>⬆️</span><b>นำเข้าข้อมูล</b><small>Fuelio, JSON และ CSV</small></button>
      <button data-more="diagnostics"><span>🩺</span><b>ตรวจระบบ</b><small>ตรวจไฟล์และสถานะ PWA</small></button>`;
    page('more').appendChild(moreGrid);

    // Elements made obsolete by the page navigation.
    document.querySelector('.view-tabs')?.remove();
    document.querySelector('.actions-row')?.remove();
    $('fabBtn')?.remove();

    // Keep utility controls available but out of the visual layout.
    const utility = document.createElement('div');
    utility.className = 'fl-utility-controls';
    ['addBtn','exportBtn','importBtn','importFile','tabFuel','tabCosts','remindersBtn','proDashboardBtn'].forEach(id => move($(id), utility));
    body.appendChild(utility);

    // ----- Bottom navigation -----
    const nav = document.createElement('nav');
    nav.className = 'fl-min-nav';
    nav.setAttribute('aria-label', 'เมนูหลัก');
    nav.innerHTML = `
      <button class="active" data-page-target="home"><b>⌂</b><span>หน้าหลัก</span></button>
      <button data-page-target="fuel"><b>⛽</b><span>เติมน้ำมัน</span></button>
      <button data-page-target="cost"><b>▣</b><span>ค่าใช้จ่าย</span></button>
      <button data-page-target="maint"><b>🔧</b><span>บำรุงรักษา</span></button>
      <button data-page-target="more"><b>☰</b><span>เพิ่มเติม</span></button>`;
    body.appendChild(nav);

    const setPage = (name, { preserveScroll = false } = {}) => {
      shell.querySelectorAll('.fl-page').forEach(el => el.classList.toggle('active', el.dataset.page === name));
      nav.querySelectorAll('button').forEach(btn => btn.classList.toggle('active', btn.dataset.pageTarget === name));
      body.dataset.flPage = name;
      if (!preserveScroll) window.scrollTo({ top: 0, behavior: 'instant' });

      // Preserve the old data rendering engine by switching its hidden tabs.
      if (name === 'fuel') $('tabFuel')?.click();
      if (name === 'cost') $('tabCosts')?.click();
      if (name === 'maint') {
        const card = $('reminderSummaryCard');
        if (card) card.style.display = '';
      }
      history.replaceState({ flPage: name }, '', `#${name}`);
    };

    nav.addEventListener('click', (event) => {
      const button = event.target.closest('[data-page-target]');
      if (button) setPage(button.dataset.pageTarget);
    });

    // Contextual page actions.
    shell.addEventListener('click', (event) => {
      const add = event.target.closest('[data-add]');
      if (!add) return;
      const kind = add.dataset.add;
      if (kind === 'fuel') {
        $('tabFuel')?.click();
        $('addBtn')?.click();
      } else if (kind === 'cost') {
        $('tabCosts')?.click();
        $('addBtn')?.click();
      } else if (kind === 'maint') {
        $('remindersBtn')?.click();
        setTimeout(() => $('r_title')?.focus(), 120);
      }
    });

    moreGrid.addEventListener('click', (event) => {
      const button = event.target.closest('[data-more]');
      if (!button) return;
      const action = button.dataset.more;
      if (action === 'family') $('familyFab')?.click();
      if (action === 'pro') $('proDashboardBtn')?.click();
      if (action === 'settings') $('settingsBtn')?.click();
      if (action === 'export') $('exportBtn')?.click();
      if (action === 'import') $('importBtn')?.click();
      if (action === 'diagnostics') location.href = 'diagnostics.html';
    });

    // ----- Convert existing modals into full-screen app pages -----
    const setupFullPageModal = (overlay, closeButton) => {
      if (!overlay) return;
      overlay.classList.add('fl-full-page-modal');
      const observer = new MutationObserver(() => {
        const isOpen = overlay.style.display !== 'none' && getComputedStyle(overlay).display !== 'none';
        body.classList.toggle('fl-modal-open', isOpen);
      });
      observer.observe(overlay, { attributes: true, attributeFilter: ['style', 'class'] });
      closeButton?.addEventListener('click', () => body.classList.remove('fl-modal-open'));
    };
    setupFullPageModal($('formOverlay'), $('closeForm'));
    setupFullPageModal($('costFormOverlay'), $('closeCostForm'));
    setupFullPageModal($('remindersOverlay'), $('closeReminders'));
    setupFullPageModal($('settingsOverlay'), $('closeSettings'));
    setupFullPageModal($('proOverlay'), $('closePro'));

    // Settings gear remains home-only and opens the More settings tool.
    $('settingsBtn')?.addEventListener('click', () => body.classList.add('fl-modal-open'));

    // Open requested page on reload/deep-link.
    const initial = location.hash.replace('#', '');
    setPage(['home','fuel','cost','maint','more'].includes(initial) ? initial : 'home', { preserveScroll: true });

    // Accessibility: back button closes full page modal first.
    window.addEventListener('popstate', () => {
      const open = [...document.querySelectorAll('.fl-full-page-modal')].find(el => getComputedStyle(el).display !== 'none');
      if (open) {
        open.querySelector('.close-btn, .fl-close')?.click();
      }
    });
  });
})();
