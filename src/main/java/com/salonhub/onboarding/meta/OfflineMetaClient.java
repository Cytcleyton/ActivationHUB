package com.salonhub.onboarding.meta;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// Stands in for Meta so the whole flow can be tried without a Meta app. It behaves like Meta where
// it matters: only a code from the simulated pop-up works, and a number is connected once registered.
public class OfflineMetaClient implements MetaClient {

    public static final String CODE_PREFIX = "offline-code-";

    private final Set<String> registeredNumbers = ConcurrentHashMap.newKeySet();

    @Override
    public String mode() {
        return "offline";
    }

    @Override
    public String exchangeCodeForToken(String code, List<String> redirectUris) {
        if (code == null || !code.startsWith(CODE_PREFIX)) {
            throw new MetaApiException("Invalid or expired code (offline simulator)", 100);
        }
        return "offline-token-" + UUID.randomUUID();
    }

    @Override
    public MetaAssets discoverAssets(String token) {
        // The simulated pop-up always sends the IDs itself.
        return new MetaAssets(null, null, null);
    }

    @Override
    public void subscribeApp(String wabaId, String token) {
        // Nothing to do offline.
    }

    @Override
    public void registerPhoneNumber(String phoneNumberId, String pin, String token) {
        if (pin == null || !pin.matches("\\d{6}")) {
            throw new MetaApiException("The PIN must be 6 digits (offline simulator)", 100);
        }
        registeredNumbers.add(phoneNumberId);
    }

    @Override
    public MetaStatus fetchStatus(MetaAssets assets, String token) {
        // Registrations are only kept in memory, so after a restart the phone status is left unknown
        // and the app falls back on its own record of the registration.
        String phone = registeredNumbers.contains(assets.phoneNumberId()) ? "CONNECTED" : null;
        // A business that has just signed up hasn't started verification yet.
        return new MetaStatus(phone, "not_verified");
    }
}
