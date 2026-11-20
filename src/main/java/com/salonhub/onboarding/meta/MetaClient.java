package com.salonhub.onboarding.meta;

// Everything the app needs from Meta. For now the only implementation is the offline simulator;
// the real Graph API client will sit behind the same interface.
public interface MetaClient {

    // Swaps the short-lived code from Embedded Signup for the salon's business token.
    String exchangeCodeForToken(String code);

    // Subscribes our app to the salon's WhatsApp account, a required onboarding step.
    void subscribeApp(String wabaId, String token);

    // Registers the salon's number for sending messages (Meta moves it from PENDING to CONNECTED).
    void registerPhoneNumber(String phoneNumberId, String pin, String token);

    MetaStatus fetchStatus(MetaAssets assets, String token);
}
