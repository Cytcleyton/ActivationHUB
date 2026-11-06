package com.salonhub.onboarding.domain;

import java.util.Arrays;
import java.util.Optional;

public enum FeatureType {

    WHATSAPP_REMINDERS("WhatsApp reminders",
            "Booking confirmations and reminders sent from the salon's own WhatsApp number.", true),
    BRANDED_APP("Salon branded app",
            "The salon's own booking app in the App Store and Google Play.", false),
    ADS_MANAGER("Facebook Ads Manager",
            "Run Facebook and Instagram ads that link straight to online booking.", false);

    private final String label;
    private final String description;
    private final boolean implemented;

    FeatureType(String label, String description, boolean implemented) {
        this.label = label;
        this.description = description;
        this.implemented = implemented;
    }

    public String label() { return label; }
    public String description() { return description; }

    // Demo-only features aren't built in this app: they go live as soon as they're added.
    public boolean implemented() { return implemented; }

    public static Optional<FeatureType> parse(String value) {
        return Arrays.stream(values()).filter(t -> t.name().equalsIgnoreCase(value)).findFirst();
    }
}
