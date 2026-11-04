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

  return { api, escapeHtml, formatDate, toast, busy };
})();
