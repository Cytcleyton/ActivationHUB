// Opens Meta's Embedded Signup for a salon and passes what it reports back to our server.
//
// Live: Meta's JavaScript SDK opens the real pop-up, which reports back two things separately:
//   1. a window message with FINISH (and the salon's IDs), CANCEL (and the screen) or an error;
//   2. a code in the FB.login callback, which our server swaps for a token within 30 seconds.
// Offline: a look-alike dialog where you choose what the salon does, sending the same messages.
const MetaSignup = (() => {
  const { api, toast, escapeHtml } = App;

  let config = null;
  let sdkLoading = null;
  let active = null;              // the signup in progress: { salonId, gotSession, dialogRedirectUris }
  let queue = Promise.resolve();  // keeps the message and the code in the order they arrived

  function enqueue(task) {
    queue = queue.then(task).catch((err) => toast(err.message, 'error'));
    return queue;
  }

  // Call when the page loads: browsers block pop-ups that don't open straight from a click,
  // so the click itself can't wait for settings or the SDK.
  async function prepare() {
    config = await api('GET', '/api/config');
    if (config.liveSignupReady) {
      await loadSdk();
    }
    return config;
  }

  function loadSdk() {
    if (sdkLoading) return sdkLoading;
    sdkLoading = new Promise((resolve, reject) => {
      window.fbAsyncInit = () => {
        FB.init({ appId: config.appId, autoLogAppEvents: true, xfbml: true, version: config.graphVersion });
        resolve();
      };
      const script = document.createElement('script');
      script.async = true;
      script.defer = true;
      script.crossOrigin = 'anonymous';
      script.src = 'https://connect.facebook.net/en_US/sdk.js';
      script.onerror = () => reject(new Error("Couldn't load Meta's SDK. Is an ad blocker on?"));
      document.head.appendChild(script);
    });
    return sdkLoading;
  }

  // The pop-up's message comes from facebook.com.
  window.addEventListener('message', (event) => {
    if (!event.origin.endsWith('facebook.com') || !active) return;
    let data;
    try { data = JSON.parse(event.data); } catch { return; }
    if (!data || data.type !== 'WA_EMBEDDED_SIGNUP') return;
    const salonId = active.salonId;
    active.gotSession = true;
    const d = data.data || {};
    enqueue(() => api('POST', `/api/salons/${salonId}/signup/session`, {
      event: data.event, currentStep: d.current_step, wabaId: d.waba_id, phoneNumberId: d.phone_number_id,
      businessId: d.business_id, errorMessage: d.error_message, errorCode: d.error_code
    }));
  });

  // Meta's names for the screens of its pop-up, in order.
  const SCREENS = [
    ['BUSINESS_ACCOUNT_SELECTION', 'Business portfolio'],
    ['WABA_PHONE_PROFILE_PICKER', 'WhatsApp account selection'],
    ['WHATSAPP_BUSINESS_PROFILE_SETUP', 'WhatsApp account creation'],
    ['PHONE_NUMBER_SETUP', 'Phone number entry'],
    ['PHONE_NUMBER_VERIFICATION', 'Phone number verification'],
    ['PERMISSIONS', 'Permissions review']
  ];

  // Call straight from a click. onDone runs once the server has handled whatever the pop-up reported.
  function launch(salonId, onDone) {
    if (!config) {
      return Promise.reject(new Error('Still loading, try again in a moment.'));
    }
    if (config.mode !== 'live') {
      return api('POST', `/api/salons/${salonId}/signup/launched`).then(() => openSimulator(salonId, onDone));
    }
    if (!config.liveSignupReady) {
      return Promise.reject(new Error('Live mode needs META_APP_ID and META_CONFIG_ID in .env.'));
    }
    if (typeof FB === 'undefined') {
      return Promise.reject(new Error("Meta's SDK hasn't loaded. Check for an ad blocker, then reload the page."));
    }

    const run = { salonId, gotSession: false, dialogRedirectUris: [] };
    active = run;
    enqueue(() => api('POST', `/api/salons/${salonId}/signup/launched`));

    // The SDK picks the dialog's redirect address itself, and our server must send the same one
    // with the code. Note it from the dialog's URL as the SDK opens the window.
    const originalOpen = window.open;
    window.open = function (url, ...rest) {
      try {
        if (typeof url === 'string' && url.includes('facebook.com')) {
          const params = new URL(url, window.location.href).searchParams;
          ['redirect_uri', 'fallback_redirect_uri'].forEach((name) => {
            const value = params.get(name);
            if (value && !run.dialogRedirectUris.includes(value)) run.dialogRedirectUris.push(value);
          });
        }
      } catch { /* not a URL we can read */ }
      return originalOpen.call(window, url, ...rest);
    };

    // FB.login opens the window synchronously and doesn't accept an async callback.
    FB.login(function (response) {
      window.open = originalOpen;
      const finished = () => { if (active === run) active = null; onDone(); };
      if (response.authResponse && response.authResponse.code) {
        const code = response.authResponse.code;
        enqueue(() => api('POST', `/api/salons/${salonId}/signup/code`,
          { code, redirectUri: window.location.href, dialogRedirectUris: run.dialogRedirectUris }));
        enqueue(finished);
      } else {
        // Closed without finishing. Meta usually says where with a CANCEL message; give it a moment,
        // and if nothing came, record the close ourselves.
        setTimeout(() => {
          if (!run.gotSession) {
            enqueue(() => api('POST', `/api/salons/${salonId}/signup/session`, { event: 'CLOSED' }));
          }
          enqueue(finished);
        }, 1500);
      }
    }, {
      config_id: config.configId,
      response_type: 'code',
      override_default_response_type: true,
      extras: { setup: {} }
    });
    return Promise.resolve();
  }

  function openSimulator(salonId, onDone) {
    const dialog = document.createElement('dialog');
    dialog.className = 'sim-popup';
    dialog.innerHTML = `
      <div class="sim-header">Meta · Connect WhatsApp <span>Offline simulator</span></div>
      <div class="dialog-body">
        <p class="muted">This stands in for Meta's pop-up. Choose what the salon does:</p>
        <div class="sim-option">
          <strong>Finish the signup</strong>
          <div class="faint small">Returns a new WhatsApp account, phone number and business, plus a code.</div>
          <div class="row-inline"><button class="primary" data-act="finish">Finish signup</button></div>
        </div>
        <div class="sim-option">
          <strong>Give up part-way</strong>
          <div class="row-inline">
            <select data-screen aria-label="Screen they give up on">
              ${SCREENS.map(([value, label]) => `<option value="${value}" ${value === 'PHONE_NUMBER_VERIFICATION' ? 'selected' : ''}>${escapeHtml(label)}</option>`).join('')}
            </select>
            <button data-act="cancel">Close here</button>
          </div>
        </div>
        <div class="sim-option">
          <strong>Hit an error</strong>
          <div class="row-inline">
            <input data-error value="Your verified name violates WhatsApp guidelines." aria-label="Error message">
            <button data-act="error">Show error</button>
          </div>
        </div>
      </div>
      <div class="dialog-actions"><button data-act="close">Close window</button></div>`;
    document.body.appendChild(dialog);
    dialog.showModal();

    const session = (body) => api('POST', `/api/salons/${salonId}/signup/session`, body);
    const finish = async (task) => {
      dialog.close();
      dialog.remove();
      try {
        await task();
      } catch (err) {
        toast(err.message, 'error');
      }
      onDone();
    };

    // Esc would close the dialog without telling the server anything.
    dialog.addEventListener('cancel', (e) => e.preventDefault());
    dialog.addEventListener('click', (e) => {
      const act = e.target.closest('[data-act]')?.dataset.act;
      if (act === 'finish') {
        const n = Date.now().toString().slice(-6);
        finish(async () => {
          // Same order as Meta: the account IDs first, then the code.
          await session({ event: 'FINISH', wabaId: `sim-waba-${n}`, phoneNumberId: `sim-phone-${n}`,
                          businessId: `sim-business-${n}` });
          await api('POST', `/api/salons/${salonId}/signup/code`, { code: `offline-code-${n}` });
        });
      } else if (act === 'cancel') {
        finish(() => session({ event: 'CANCEL', currentStep: dialog.querySelector('[data-screen]').value }));
      } else if (act === 'error') {
        finish(() => session({ event: 'CANCEL', errorMessage: dialog.querySelector('[data-error]').value,
                               errorCode: '524126' }));
      } else if (act === 'close') {
        finish(() => session({ event: 'CLOSED' }));
      }
    });
  }

  return { prepare, launch };
})();
