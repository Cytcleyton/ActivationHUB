package com.salonhub.onboarding.meta;

import java.util.List;

// Everything the app needs from Meta. GraphApiMetaClient calls Meta for real; OfflineMetaClient
// simulates it. META_MODE picks one.
public interface MetaClient {

    // "live" or "offline", for the browser and the logs.
    String mode();

    // Swaps the short-lived code from Embedded Signup for the salon's business token.
    // redirectUris are addresses the pop-up may have used; Meta only accepts the same one back.
    String exchangeCodeForToken(String code, List<String> redirectUris);

    // Works out the salon's WhatsApp account and number from the token, for when the pop-up's
    // own message with the IDs never arrived.
    MetaAssets discoverAssets(String token);

    // Subscribes our app to the salon's WhatsApp account, a required onboarding step.
    void subscribeApp(String wabaId, String token);

    // Registers the salon's number for sending messages (Meta moves it from PENDING to CONNECTED).
    void registerPhoneNumber(String phoneNumberId, String pin, String token);

    MetaStatus fetchStatus(MetaAssets assets, String token);
}
