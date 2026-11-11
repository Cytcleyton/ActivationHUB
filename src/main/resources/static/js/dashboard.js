(() => {
  const { api, escapeHtml, formatDate, toast, toneClass, waitingText } = App;

  const FILTERS = [
    ['all', 'All'],
    ['stalled', 'Stalled'],
    ['salon', 'Waiting on salon'],
    ['meta', 'Waiting on Meta'],
    ['team', 'Waiting on us'],
    ['live', 'Live']
  ];

  const state = { rows: [], stats: null, filter: 'all' };

  const rowsEl = document.getElementById('rows');
  const statsEl = document.getElementById('stats');
  const chipsEl = document.getElementById('chips');
  const searchEl = document.getElementById('search');
  const featureEl = document.getElementById('feature');
  const sortEl = document.getElementById('sort');

  async function load() {
    const data = await api('GET', '/api/overview');
    state.rows = data.rows;
    state.stats = data.stats;
    render();
  }

  function matchesFilter(r) {
    switch (state.filter) {
      case 'stalled': return r.stalled;
      case 'salon': return r.waitingOn === 'Salon';
      case 'meta': return r.waitingOn === 'Meta';
      case 'team': return r.waitingOn === 'Onboarding team';
      case 'live': return r.statusTone === 'done';
      default: return true;
    }
  }

  // Stalled first, then problems, then other WhatsApp stages; live features last.
  function rank(r) {
    if (r.stalled) return 0;
    if (r.statusTone === 'problem') return 1;
    if (r.statusTone === 'waiting') return 2;
    if (r.statusTone === 'done') return 4;
    return 3;
  }

  function sorted(rows) {
    const byName = (a, b) => a.salonName.localeCompare(b.salonName) || a.featureLabel.localeCompare(b.featureLabel);
    switch (sortEl.value) {
      case 'name': return rows.sort(byName);
      case 'longest': return rows.sort((a, b) => b.secondsInStage - a.secondsInStage || byName(a, b));
      case 'stalled': return rows.sort((a, b) => rank(a) - rank(b) || b.secondsInStage - a.secondsInStage || byName(a, b));
      default: return rows;   // the API already returns newest first
    }
  }

  function render() {
    renderStats();
    chipsEl.innerHTML = FILTERS.map(([key, label]) =>
      `<button class="chip ${state.filter === key ? 'active' : ''}" data-filter="${key}"
         aria-pressed="${state.filter === key}">${label}</button>`).join('');

    const query = searchEl.value.trim().toLowerCase();
    const feature = featureEl.value;
    const visible = sorted(state.rows.filter((r) =>
      matchesFilter(r)
      && (!feature || r.featureType === feature)
      && (!query || r.salonName.toLowerCase().includes(query))));

    if (!state.rows.length) {
      rowsEl.innerHTML = '<div class="empty">No features added yet. Add a salon and its features on the <a href="/salons.html">Salons</a> screen.</div>';
    } else if (!visible.length) {
      rowsEl.innerHTML = '<div class="empty">Nothing matches. Check the spelling, or change the feature or filter.</div>';
    } else {
      rowsEl.innerHTML = visible.map(row).join('');
    }
  }

  // Clicking a card filters the list, like the chips.
  function renderStats() {
    const s = state.stats;
    const card = (key, label, value, cls) => `
      <button class="card stat ${cls} ${state.filter === key ? 'active' : ''}" data-filter="${key}">
        <div class="stat-label">${label}</div>
        <div class="stat-value">${value}</div>
      </button>`;
    statsEl.innerHTML =
      card('all', 'WhatsApp onboarding', s.onboarding, '') +
      card('stalled', 'Stalled', s.stalled, s.stalled ? 'bad' : '') +
      card('live', 'Live features', s.live, 'good');
  }

  function row(r) {
    const sub = [r.city, r.contactName && `Contact: ${r.contactName}`].filter(Boolean).join(' · ');
    const onboarding = r.stage && r.stage !== 'LIVE';

    let time = '<span class="faint">—</span>';
    if (onboarding && r.stalled) {
      time = `<span class="stalled-badge">Stalled</span><div class="stalled-time">${escapeHtml(r.timeInStage)}</div>`;
    } else if (onboarding) {
      time = `<span class="muted">${escapeHtml(r.timeInStage)}</span>`;
    }

    return `
      <div class="row clickable${r.stalled ? ' stalled' : ''}" data-href="/salon.html?id=${r.salonId}">
        <div>
          <a class="name" href="/salon.html?id=${r.salonId}">${escapeHtml(r.salonName)}</a>
          ${r.demo ? '<span class="tag">DEMO</span>' : ''}
          ${sub ? `<div class="sub">${escapeHtml(sub)}</div>` : ''}
        </div>
        <div><span class="cell-label">Feature</span>${escapeHtml(r.featureLabel)}</div>
        <div>
          <span class="pill ${toneClass(r.statusTone)}" title="${escapeHtml(r.statusLabel)}">${escapeHtml(r.statusLabel)}</span>
          ${onboarding ? `<div class="sub">${escapeHtml(waitingText(r.waitingOn))}</div>` : ''}
        </div>
        <div><span class="cell-label">In stage</span>${time}</div>
        <div><span class="cell-label">Added</span><span class="muted">${escapeHtml(formatDate(r.addedAt))}</span></div>
      </div>`;
  }

  searchEl.addEventListener('input', render);
  featureEl.addEventListener('change', render);
  sortEl.addEventListener('change', render);

  document.addEventListener('click', (e) => {
    const filter = e.target.closest('[data-filter]');
    if (filter) {
      state.filter = filter.dataset.filter;
      render();
      return;
    }
    const row = e.target.closest('.row[data-href]');
    if (row && !e.target.closest('a')) window.location.href = row.dataset.href;
  });

  load().catch((err) => toast(err.message, 'error'));
})();
