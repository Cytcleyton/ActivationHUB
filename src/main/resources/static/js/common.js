const App = (() => {

  async function api(method, url, body) {
    const options = { method, headers: { Accept: 'application/json' } };
    if (body !== undefined) {
      options.headers['Content-Type'] = 'application/json';
      options.body = JSON.stringify(body);
    }
    const res = await fetch(url, options);
    const text = await res.text();
    let data = null;
    try { data = text ? JSON.parse(text) : null; } catch { data = null; }
    if (!res.ok) {
      throw new Error((data && data.message) || `Request failed (${res.status})`);
    }
    return data;
  }

  function escapeHtml(value) {
    return String(value ?? '')
      .replaceAll('&', '&amp;')
      .replaceAll('<', '&lt;')
      .replaceAll('>', '&gt;')
      .replaceAll('"', '&quot;')
      .replaceAll("'", '&#39;');
  }

  const dateFmt = new Intl.DateTimeFormat(undefined, {
    day: 'numeric', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit'
  });

  function formatDate(iso) {
    return iso ? dateFmt.format(new Date(iso)) : '—';
  }

  function toast(message, kind = '') {
    let box = document.querySelector('.toasts');
    if (!box) {
      box = document.createElement('div');
      box.className = 'toasts';
      box.setAttribute('role', 'status');
      document.body.appendChild(box);
    }
    const el = document.createElement('div');
    el.className = `toast ${kind}`;
    el.textContent = message;
    box.appendChild(el);
    setTimeout(() => el.remove(), kind === 'error' ? 9000 : 5000);
  }

  function waitingText(waitingOn) {
    return `Waiting on ${waitingOn === 'Meta' ? 'Meta' : String(waitingOn).toLowerCase()}`;
  }

  function toneClass(tone) {
    return ['done', 'waiting', 'problem', 'neutral'].includes(tone) ? tone : 'neutral';
  }

  // Resolves to true if the user confirms, false if they cancel or press Esc.
  function confirmDialog(title, message, confirmLabel = 'Confirm', danger = false) {
    return new Promise((resolve) => {
      const dialog = document.createElement('dialog');
      dialog.innerHTML = `
        <form method="dialog">
          <div class="dialog-body"><h2>${escapeHtml(title)}</h2><p class="muted">${escapeHtml(message)}</p></div>
          <div class="dialog-actions">
            <button value="no">Cancel</button>
            <button value="yes" class="${danger ? 'danger' : 'primary'}">${escapeHtml(confirmLabel)}</button>
          </div>
        </form>`;
      document.body.appendChild(dialog);
      dialog.addEventListener('close', () => {
        resolve(dialog.returnValue === 'yes');
        dialog.remove();
      });
      dialog.showModal();
    });
  }

  async function busy(button, action) {
    const original = button.innerHTML;
    button.disabled = true;
    button.innerHTML = `<span class="spinner"></span> ${button.textContent.trim()}`;
    try {
      return await action();
    } finally {
      button.disabled = false;
      button.innerHTML = original;
    }
  }

  return { api, escapeHtml, formatDate, toast, busy, toneClass, confirmDialog, waitingText };
})();
