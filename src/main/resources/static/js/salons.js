(() => {
  const { api, escapeHtml, formatDate, toast } = App;
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
    return `
      <div class="row">
        <span class="name">${escapeHtml(salon.name)}</span>
        <span class="muted">${escapeHtml(formatDate(salon.createdAt))}</span>
      </div>`;
  }

  load().catch((err) => {
    countEl.textContent = 'Couldn’t load salons';
    toast(err.message, 'error');
  });
})();
