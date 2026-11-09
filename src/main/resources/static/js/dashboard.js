(() => {
  const { api, escapeHtml, formatDate, toast, toneClass } = App;
  const rowsEl = document.getElementById('rows');
  const countEl = document.getElementById('count');
  const searchEl = document.getElementById('search');
  const featureEl = document.getElementById('feature');
  const sortEl = document.getElementById('sort');

  let allRows = [];

  async function load() {
    allRows = (await api('GET', '/api/overview')).rows;
    render();
  }

  // Filtering and sorting happen here in the browser; the API returns every row, newest first.
  function visibleRows() {
    const query = searchEl.value.trim().toLowerCase();
    const feature = featureEl.value;
    const rows = allRows.filter((r) =>
      (!query || r.salonName.toLowerCase().includes(query)) && (!feature || r.featureType === feature));
    if (sortEl.value === 'name') {
      rows.sort((a, b) => a.salonName.localeCompare(b.salonName) || a.featureLabel.localeCompare(b.featureLabel));
    }
    return rows;
  }

  function render() {
    const rows = visibleRows();
    countEl.textContent = allRows.length === 1 ? '1 feature added' : `${allRows.length} features added`;
    if (!allRows.length) {
      rowsEl.innerHTML = '<div class="empty">No features added yet. Add a salon and its features on the <a href="/salons.html">Salons</a> screen.</div>';
    } else if (!rows.length) {
      rowsEl.innerHTML = '<div class="empty">Nothing matches these filters.</div>';
    } else {
      rowsEl.innerHTML = rows.map(row).join('');
    }
  }

  function row(r) {
    return `
      <div class="row clickable" data-href="/salon.html?id=${r.salonId}">
        <a class="name" href="/salon.html?id=${r.salonId}">${escapeHtml(r.salonName)}</a>
        <div><span class="cell-label">Feature</span>${escapeHtml(r.featureLabel)}</div>
        <div><span class="cell-label">Status</span><span class="pill ${toneClass(r.statusTone)}">${escapeHtml(r.statusLabel)}</span></div>
        <div><span class="cell-label">Added</span><span class="muted">${escapeHtml(formatDate(r.addedAt))}</span></div>
      </div>`;
  }

  searchEl.addEventListener('input', render);
  featureEl.addEventListener('change', render);
  sortEl.addEventListener('change', render);

  rowsEl.addEventListener('click', (e) => {
    const row = e.target.closest('.row[data-href]');
    if (row && !e.target.closest('a')) window.location.href = row.dataset.href;
  });

  load().catch((err) => {
    countEl.textContent = 'Couldn’t load the overview';
    toast(err.message, 'error');
  });
})();
