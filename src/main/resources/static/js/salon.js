(() => {
  const { api, escapeHtml, formatDate, toast, busy, toneClass, confirmDialog, waitingText } = App;
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
      ${salon.whatsapp ? whatsappSection(salon.whatsapp) : ''}
      <section class="card card-pad">
        <div class="card-title"><h2>Activity</h2></div>
        ${activity(salon.events)}
      </section>
      <section class="card card-pad danger-zone">
        <span class="muted">Delete removes the salon and its history from this app.</span>
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

  function whatsappSection(w) {
    const live = w.stage === 'LIVE';
    const inStage = live ? '' : (w.stalled
      ? `<span class="stalled-badge">Stalled · ${escapeHtml(w.timeInStage)} in this stage</span>`
      : `<span class="muted">${escapeHtml(w.timeInStage)} in this stage</span>`);
    return `
      <div class="section-head">
        <div>
          <h2>WhatsApp reminders onboarding</h2>
          <div class="meta-line">
            <span class="pill ${toneClass(w.stageTone)}">${escapeHtml(w.stageLabel)}</span>
            ${live ? '' : `<span class="muted">${escapeHtml(waitingText(w.waitingOn))}</span>`}
            ${inStage}
          </div>
        </div>
      </div>
      ${callout(w)}
      <section class="card card-pad">
        <div class="card-title"><h2>Onboarding steps</h2></div>
        <ol class="steps">${steps(w)}</ol>
      </section>`;
  }

  // What the team should do next; red when the salon has stalled.
  function callout(w) {
    let tone = w.stageTone === 'waiting' ? 'waiting' : (w.stageTone === 'problem' ? 'problem' : '');
    let title = 'Next step';
    if (w.stalled) {
      tone = 'problem';
      title = 'Needs follow-up';
    }
    const stalledAfter = w.stalled && w.stalledAfter ? ` <span class="small">(flagged after ${escapeHtml(w.stalledAfter)} in this stage)</span>` : '';
    return `
      <div class="callout ${tone}">
        <div>
          <div class="callout-title">${escapeHtml(title)}${stalledAfter}</div>
          <div>${escapeHtml(w.nextAction)}</div>
        </div>
      </div>`;
  }

  function steps(w) {
    let signupState = w.connected ? 'done' : 'current';
    let signupBody = 'The salon logs in with Facebook in Meta\'s pop-up, picks or creates a business portfolio and '
      + 'WhatsApp account, and verifies its phone number.';
    if (w.signupState === 'ABANDONED') {
      signupState = 'problem';
      signupBody = w.abandonedAt
        ? `Closed Meta's pop-up at the <strong>${escapeHtml(w.abandonedAt)}</strong> screen.`
        : "Closed Meta's pop-up before finishing.";
    } else if (w.signupState === 'ERROR') {
      signupState = 'problem';
      signupBody = `Reported an error in Meta's pop-up: ${escapeHtml(w.signupError || '')}`;
    } else if (w.signupState === 'FINISHED' && !w.connected) {
      signupState = 'problem';
      signupBody = 'Finished the pop-up, but the connection didn\'t complete. See the activity log.';
    } else if (w.connected) {
      signupBody = 'Connected to the salon\'s WhatsApp account.';
    } else if (w.signupState === 'IN_PROGRESS') {
      signupBody = 'Meta\'s pop-up was opened; waiting for the salon to finish.';
    }

    const numberState = w.connected ? (w.phoneConnected ? 'done' : 'current') : '';
    const numberBody = !w.connected
      ? 'Registers the salon\'s number for sending messages once it\'s connected.'
      : (w.phoneConnected
        ? '<span class="pill done">Connected</span> Ready to send messages.'
        : '<span class="pill waiting">Pending registration</span> Meta allows 14 days after signup.');

    let verificationState = '';
    let verificationBody = 'The salon submits documents in Meta Business Suite; we can only watch the status.';
    if (w.phoneConnected) {
      verificationState = w.stage === 'LIVE' ? 'done' : (w.verificationTone === 'problem' ? 'problem' : 'current');
      verificationBody = `<span class="pill ${toneClass(w.verificationTone)}">${escapeHtml(w.verificationLabel)}</span> ${verificationBody}`;
    }

    const live = w.stage === 'LIVE';
    return [
      step(1, signupState, 'Salon connects WhatsApp through Meta', signupBody),
      step(2, numberState, 'Register the phone number', numberBody),
      step(3, verificationState, 'Salon verifies their business with Meta', verificationBody),
      step(4, live ? 'done' : '', 'Live', live
        ? 'Verified and connected. Automated messages can go out from the salon\'s number.'
        : 'Verified business with a connected number.')
    ].join('');
  }

  function step(n, state, title, body) {
    return `
      <li class="step ${state}">
        <span class="step-num">${state === 'done' ? '✓' : (state === 'problem' ? '!' : n)}</span>
        <div><div class="step-title">${title}</div><div class="step-body">${body}</div></div>
      </li>`;
  }

  function activity(events) {
    if (!events.length) return '<p class="muted">Nothing yet.</p>';
    return `<ul class="activity">${events.map((e) => `
      <li>
        <span class="when">${escapeHtml(formatDate(e.at))}</span>
        <span class="what"><span class="type">${escapeHtml(e.type.replaceAll('_', ' '))}</span>${escapeHtml(e.detail)}</span>
      </li>`).join('')}</ul>`;
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
        if (await confirmDialog('Delete salon?', 'Removes the salon and its history from this app.', 'Delete', true)) {
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
