(() => {
  const { api, escapeHtml, formatDate, toast, busy, toneClass, confirmDialog } = App;
  const content = document.getElementById('content');
  const salonId = new URLSearchParams(window.location.search).get('id');

  function render(salon) {
    document.title = salon.name;
    const sub = [salon.city, salon.contactName && `Contact: ${salon.contactName}`].filter(Boolean).join(' · ');
    content.innerHTML = `
      <div class="salon-head">
        <h1>${escapeHtml(salon.name)} ${salon.demo ? '<span class="tag">DEMO</span>' : ''}</h1>
        <div class="meta-line muted">${sub ? `${escapeHtml(sub)} · ` : ''}Added ${escapeHtml(formatDate(salon.createdAt))}</div>
      </div>
      ${featuresCard(salon)}
      <section class="card card-pad danger-zone">
        <span class="muted">Delete removes the salon and its features from this app.</span>
        <button class="danger" data-action="delete-salon">Delete salon</button>
      </section>`;
  }

  function featuresCard(salon) {
    const rows = salon.features.map((f) => {
      const status = f.added
        ? `<span class="pill ${toneClass(f.statusTone)}">${escapeHtml(f.statusLabel)}</span>
           <div class="faint small">Added ${escapeHtml(formatDate(f.addedAt))}</div>`
        : '<span class="faint">Not added</span>';
      const action = f.added
        ? ''
        : `<button class="primary" data-action="add-feature" data-feature="${escapeHtml(f.type)}">Add</button>`;
      return `
        <li class="feature-row">
          <div>
            <div class="step-title">${escapeHtml(f.label)}
              ${f.implemented ? '' : '<span class="tag" title="Not built in this app: goes live as soon as it\'s added">DEMO ONLY</span>'}</div>
            <div class="step-body">${escapeHtml(f.description)}</div>
          </div>
          <div class="feature-status">${status}</div>
          <div class="step-actions">${action}</div>
        </li>`;
    }).join('');
    return `
      <section class="card card-pad">
        <div class="card-title"><h2>Features</h2></div>
        <ul class="features">${rows}</ul>
      </section>`;
  }

  async function load() {
    render(await api('GET', `/api/salons/${encodeURIComponent(salonId)}`));
  }

  // One listener for all buttons, since the page is re-rendered after each change.
  content.addEventListener('click', async (e) => {
    const button = e.target.closest('button[data-action]');
    if (!button) return;
    try {
      if (button.dataset.action === 'add-feature') {
        const salon = await busy(button, () =>
          api('POST', `/api/salons/${encodeURIComponent(salonId)}/features/${button.dataset.feature}`));
        render(salon);
      } else if (button.dataset.action === 'delete-salon') {
        if (await confirmDialog('Delete salon?', 'Removes the salon and its features from this app.', 'Delete', true)) {
          await busy(button, () => api('DELETE', `/api/salons/${encodeURIComponent(salonId)}`));
          window.location.href = '/salons.html';
        }
      }
    } catch (err) {
      toast(err.message, 'error');
    }
  });

  if (!salonId) {
    content.innerHTML = '<p class="muted">No salon selected. Go back to <a href="/salons.html">Salons</a>.</p>';
  } else {
    load().catch((err) => {
      content.innerHTML = `<p class="muted">${escapeHtml(err.message)} Go back to <a href="/salons.html">Salons</a>.</p>`;
    });
  }
})();
