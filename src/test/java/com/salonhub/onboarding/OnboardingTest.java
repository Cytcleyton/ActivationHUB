package com.salonhub.onboarding;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

// The salon page's WhatsApp onboarding section and activity log.
class OnboardingTest extends ApiTestBase {

    @Test
    void whatsAppSectionAppearsOnceTheFeatureIsAdded() throws Exception {
        long id = createSalon("Willow Hair Studio");
        mvc.perform(get("/api/salons/" + id)).andExpect(jsonPath("$.whatsapp").doesNotExist());

        addFeature(id, "WHATSAPP_REMINDERS")
                .andExpect(jsonPath("$.whatsapp.stage").value("NOT_STARTED"))
                .andExpect(jsonPath("$.whatsapp.waitingOn").value("Salon"))
                .andExpect(jsonPath("$.whatsapp.stalled").value(false))
                .andExpect(jsonPath("$.whatsapp.timeInStage").value("under a minute"))
                .andExpect(jsonPath("$.whatsapp.nextAction").value(startsWith("Remind the salon")));
    }

    @Test
    void stalledSalonShowsWhenItWasFlagged() throws Exception {
        long id = createSalon("Willow Hair Studio");
        addFeature(id, "WHATSAPP_REMINDERS");
        moveStageStartBack(id, Duration.ofDays(3).plusHours(1));

        mvc.perform(get("/api/salons/" + id))
                .andExpect(jsonPath("$.whatsapp.stalled").value(true))
                .andExpect(jsonPath("$.whatsapp.timeInStage").value("3d 1h"))
                .andExpect(jsonPath("$.whatsapp.stalledAfter").value("3d"));
    }

    @Test
    void activityLogIsNewestFirst() throws Exception {
        long id = createSalon("Willow Hair Studio");
        addFeature(id, "WHATSAPP_REMINDERS");

        mvc.perform(get("/api/salons/" + id))
                .andExpect(jsonPath("$.events[0].type").value("FEATURE_ADDED"))
                .andExpect(jsonPath("$.events[1].type").value("SALON_CREATED"));
    }
}
