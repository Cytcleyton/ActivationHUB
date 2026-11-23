// The salon owner's view (portal.html?id=N): connecting WhatsApp, in the salon's own words.
(() => {
  const { api, escapeHtml, toast, busy, every } = App;
  const content = document.getElementById('content');
  const salonId = new URLSearchParams(window.location.search).get('id');
  let salon = null;

  async function load() {
    salon = await api('GET', `/api/salons/${encodeURIComponent(salonId)}`);
    render();
  }

  // Connected salons: ask Meta for the latest, so the steps reflect what the salon did there.
  async function refresh() {
    if (!salon || salon.demo || !salon.whatsapp || !salon.whatsapp.connected) return;
    salon = await api('POST', `/api/salons/${encodeURIComponent(salonId)}/refresh`);
    render();
  }

  function render() {
    document.title = `${salon.name} · WhatsApp`;
    document.getElementById('salon-name').textContent = salon.name;
    document.getElementById('salon-mark').textContent = salon.name.trim().charAt(0).toUpperCase() || '·';

    const w = salon.whatsapp;
    if (!w) {
      content.innerHTML = `
        <h1>WhatsApp messaging</h1>
        <section class="card card-pad portal-card">
          <p class="no-margin">WhatsApp reminders aren't switched on for your salon yet.
            Ask your account manager if you'd like to send booking reminders on WhatsApp.</p>
        </section>
        ${otherFeatures()}`;
      return;
    }

    const live = w.stage === 'LIVE';
    content.innerHTML = `
      <h1>WhatsApp messaging</h1>
      <p class="muted portal-intro">Send booking confirmations and reminders to your clients on WhatsApp.
        ${live ? '' : 'A few steps to get set up:'}</p>
      ${salon.demo ? `<div class="callout waiting"><div><div class="callout-title">Demo salon</div>
        This salon is demo data, so it can't connect to Meta. Add a new salon in the onboarding team's
        view to try the real flow.</div></div>` : ''}
      <section class="card card-pad portal-card">
        <ol class="steps">${steps(w)}</ol>
      </section>
      ${otherFeatures()}
      <section class="help-box">
        <div>
          <div class="callout-title">Need a hand?</div>
          <div class="muted">Your onboarding contact${salon.contactName ? `, <strong>${escapeHtml(salon.contactName)}</strong>,` : ''}
            can talk you through any of these steps.</div>
        </div>
        <button data-action="refresh">Refresh</button>
      </section>`;
  }

  function otherFeatures() {
    const others = salon.features.filter((f) => f.added && f.type !== 'WHATSAPP_REMINDERS');
    if (!others.length) return '';
    return `
      <h2 class="portal-subhead">Your other features</h2>
      <section class="card portal-card">
        ${others.map((f) => `
          <div class="picker-row">
            <span class="salon-mark small" aria-hidden="true">✓</span>
            <span><strong>${escapeHtml(f.label)}</strong>
              <span class="faint small">· ${escapeHtml(f.description)}</span></span>
            <span class="spacer"></span>
            <span class="pill done">Active</span>
          </div>`).join('')}
      </section>`;
  }

  function steps(w) {
    const canAct = !salon.demo;

    let state = w.connected ? 'done' : 'current';
    let body = 'Log in with Facebook, create or choose your business, and verify the phone number '
      + 'your clients will see. About 5 minutes.';
    let label = 'Connect WhatsApp';
    if (w.connected) {
      body = 'Connected.';
    } else if (w.signupState === 'IN_PROGRESS') {
      body = 'Finish connecting in the Meta window. If you closed it, open it again here.';
      label = 'Open Meta again';
    } else if (w.signupState === 'ABANDONED') {
      state = 'problem';
      body = (w.abandonedAt
        ? `You didn't finish connecting: you stopped at the <strong>${escapeHtml(w.abandonedAt)}</strong> step. `
        : "You didn't finish connecting. ") + 'Pick up where you left off.';
      label = 'Continue connecting';
    } else if (w.signupState === 'ERROR') {
      state = 'problem';
      body = `Meta showed an error: ${escapeHtml(w.signupError || 'unknown error')}. Fix it and try again.`;
      label = 'Try again';
    } else if (w.signupState === 'FINISHED') {
      state = 'problem';
      body = "We couldn't finish connecting your account. Your onboarding team can see this and will be in touch.";
    }
    const connectButton = canAct && !w.connected && w.signupState !== 'FINISHED'
      ? `<button class="meta" data-action="connect">${label}</button>` : '';

    const numberState = w.connected ? (w.phoneConnected ? 'done' : 'current') : '';
    const numberBody = !w.connected
      ? "Your onboarding team does this once you're connected."
      : (w.phoneConnected
        ? 'Your number is ready to send messages.'
        : 'Your onboarding team is setting up your number. Nothing for you to do.');

    const [verificationState, verificationBody] = verification(w);
    const verifyLabel = { VERIFICATION_NOT_STARTED: 'Verify on Meta', VERIFICATION_STARTED: 'Continue on Meta',
                          VERIFICATION_NEEDS_INFO: 'Open Meta' }[w.stage];
    // A plain link: the server records that the salon opened it, then sends the browser on to Meta.
    const verifyButton = canAct && verifyLabel
      ? `<a class="btn primary" href="/api/salons/${encodeURIComponent(salon.id)}/verify-on-meta" target="_blank"
           rel="noopener">${verifyLabel} ↗</a>` : '';

    const live = w.stage === 'LIVE';
    return [
      step(1, state, 'Connect your WhatsApp', body, connectButton),
      step(2, numberState, 'We set up your number', numberBody),
      step(3, verificationState, 'Verify your business with Meta', verificationBody, verifyButton),
      step(4, live ? 'done' : '', 'Start sending reminders', live
        ? 'All set. Booking reminders now go out on WhatsApp.'
        : 'Once the steps above are done, reminders go out automatically.')
    ].join('');
  }

  function verification(w) {
    if (!w.phoneConnected) {
      return ['', 'Meta confirms your business is real. Have your business registration document ready.'];
    }
    switch (w.stage) {
      case 'LIVE': return ['done', 'Meta has verified your business.'];
      case 'VERIFICATION_STARTED': return ['current', "You've started verifying with Meta but haven't submitted "
        + 'your documents yet. Pick up where you left off in Meta Business Suite.'];
      case 'VERIFICATION_IN_REVIEW': return ['current', "Meta is reviewing your documents. This usually takes a few "
        + "days; we'll keep an eye on it."];
      case 'VERIFICATION_NEEDS_INFO': return ['problem', 'Meta needs more information. Open Meta Business Suite to '
        + 'see what they asked for.'];
      case 'VERIFICATION_FAILED': return ['problem', "Meta couldn't verify your business. Your onboarding contact "
        + 'will go through the reason with you.'];
      case 'VERIFICATION_NOT_STARTED': return ['current', 'Upload your business documents in Meta Business Suite so '
        + 'Meta can confirm your business is real. Have your business registration document ready.'];
      default: return ['current', 'Your onboarding contact is checking your verification with Meta.'];
    }
  }

  function step(n, state, title, body, actions = '') {
    return `
      <li class="step ${state}">
        <span class="step-num">${state === 'done' ? '✓' : (state === 'problem' ? '!' : n)}</span>
        <div><div class="step-title">${escapeHtml(title)}</div><div class="step-body">${body}</div></div>
        <div class="step-actions">${actions}</div>
      </li>`;
  }

  content.addEventListener('click', (e) => {
    const button = e.target.closest('button[data-action]');
    if (!button) return;
    const reload = () => load().catch((err) => toast(err.message, 'error'));
    if (button.dataset.action === 'connect') {
      MetaSignup.launch(salon.id, reload).catch((err) => toast(err.message, 'error'));
    } else if (button.dataset.action === 'refresh') {
      busy(button, () => load().then(refresh)).catch((err) => toast(err.message, 'error'));
    }
  });

  if (!salonId) {
    document.getElementById('salon-name').textContent = 'Salon app';
    content.innerHTML = `<h1>No salon selected</h1>
      <p class="muted">Open this from a salon's page in the onboarding team's view, with <strong>Salon's view</strong>.</p>`;
    return;
  }

  // Coming back to this tab: show the latest, unless the simulator is open.
  window.addEventListener('focus', () => {
    if (salon && !document.querySelector('dialog[open]')) load().catch(() => {});
  });

  load().then(refresh).catch((err) => {
    content.innerHTML = `<p class="muted">${escapeHtml(err.message)}</p>`;
  });
  every(60000, () => refresh().catch(() => {}));
  MetaSignup.prepare().catch((err) => toast(err.message, 'error'));
})();
