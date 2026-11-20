// Opens Meta's Embedded Signup for a salon and passes what it reports back to our server.
// For now this is an offline simulator: a look-alike dialog where you choose what the salon does,
// which sends the same messages Meta's real pop-up would.
const MetaSignup = (() => {
  const { api, toast, escapeHtml } = App;

  // Meta's names for the screens of its pop-up, in order.
  const SCREENS = [
    ['BUSINESS_ACCOUNT_SELECTION', 'Business portfolio'],
    ['WABA_PHONE_PROFILE_PICKER', 'WhatsApp account selection'],
    ['WHATSAPP_BUSINESS_PROFILE_SETUP', 'WhatsApp account creation'],
    ['PHONE_NUMBER_SETUP', 'Phone number entry'],
    ['PHONE_NUMBER_VERIFICATION', 'Phone number verification'],
    ['PERMISSIONS', 'Permissions review']
  ];

  // onDone runs once the server has handled whatever the pop-up reported.
  async function launch(salonId, onDone) {
    await api('POST', `/api/salons/${salonId}/signup/launched`);
    openSimulator(salonId, onDone);
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

  return { launch };
})();
