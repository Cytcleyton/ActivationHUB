(() => {
  const { api, escapeHtml, formatDate, toast, toneClass } = App;
  const rowsEl = document.getElementById('rows');
  const countEl = document.getElementById('count');

  async function load() {
    const { salons } = await api('GET', '/api/salons');
    countEl.textContent = salons.length === 1 ? '1 salon' : `${salons.length} salons`;
    rowsEl.innerHTML = salons.length
      ? salons.map(row).join('')
      : '<div class="empty">No salons yet. Use <strong>Add salon</strong> to create one.</div>';
  }

  function row(salon) {
    const chips = salon.features.length
      ? salon.features.map((f) => `
          <span class="feature-chip">
            <span>${escapeHtml(f.label)}</span>
            <span class="pill ${toneClass(f.statusTone)}" title="${escapeHtml(f.statusLabel)}">${escapeHtml(f.statusLabel)}</span>
          </span>`).join('')
      : '<span class="faint">No features yet</span>';
    return `
      <div class="row clickable" data-href="/salon.html?id=${salon.id}">
        <div><a class="name" href="/salon.html?id=${salon.id}">${escapeHtml(salon.name)}</a>${salon.demo ? ' <span class="tag">DEMO</span>' : ''}</div>
        <div><span class="cell-label">Features</span><div class="feature-chips">${chips}</div></div>
        <div><span class="cell-label">Added</span><span class="muted">${escapeHtml(formatDate(salon.createdAt))}</span></div>
      </div>`;
  }

  // The whole row opens the salon, not just its name.
  rowsEl.addEventListener('click', (e) => {
    const row = e.target.closest('.row[data-href]');
    if (row && !e.target.closest('a')) window.location.href = row.dataset.href;
  });

  load().catch((err) => {
    countEl.textContent = 'Couldn’t load salons';
    toast(err.message, 'error');
  });
})();
