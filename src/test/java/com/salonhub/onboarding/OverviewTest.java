package com.salonhub.onboarding;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OverviewTest extends ApiTestBase {

    @Test
    void salonWithoutFeaturesHasNoRows() throws Exception {
        createSalon("Willow Hair Studio");

        mvc.perform(get("/api/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows", hasSize(0)));
    }

    @Test
    void salonWithTwoFeaturesHasTwoRowsNewestFirst() throws Exception {
        long id = createSalon("Willow Hair Studio");
        addFeature(id, "BRANDED_APP");
        addFeature(id, "WHATSAPP_REMINDERS");

        mvc.perform(get("/api/overview"))
                .andExpect(jsonPath("$.rows", hasSize(2)))
                .andExpect(jsonPath("$.rows[0].salonId").value(id))
                .andExpect(jsonPath("$.rows[0].salonName").value("Willow Hair Studio"))
                .andExpect(jsonPath("$.rows[0].featureType").value("WHATSAPP_REMINDERS"))
                .andExpect(jsonPath("$.rows[0].statusLabel").value("Waiting for salon to connect"))
                .andExpect(jsonPath("$.rows[0].addedAt").exists())
                .andExpect(jsonPath("$.rows[1].featureType").value("BRANDED_APP"))
                .andExpect(jsonPath("$.rows[1].statusLabel").value("Live"));
    }

    @Test
    void rowsFromDifferentSalonsAreInterleavedByWhenTheyWereAdded() throws Exception {
        long willow = createSalon("Willow Hair Studio");
        long glow = createSalon("Glow Hair Studio");
        addFeature(willow, "BRANDED_APP");
        addFeature(glow, "ADS_MANAGER");
        addFeature(willow, "WHATSAPP_REMINDERS");

        mvc.perform(get("/api/overview"))
                .andExpect(jsonPath("$.rows", hasSize(3)))
                .andExpect(jsonPath("$.rows[0].salonName").value("Willow Hair Studio"))
                .andExpect(jsonPath("$.rows[1].salonName").value("Glow Hair Studio"))
                .andExpect(jsonPath("$.rows[2].salonName").value("Willow Hair Studio"));
    }

    @Test
    void whatsAppRowShowsStageAndWhoItsWaitingOn() throws Exception {
        long id = createSalon("Willow Hair Studio");
        addFeature(id, "WHATSAPP_REMINDERS");
        addFeature(id, "BRANDED_APP");

        mvc.perform(get("/api/overview"))
                .andExpect(jsonPath("$.rows[1].stage").value("NOT_STARTED"))
                .andExpect(jsonPath("$.rows[1].waitingOn").value("Salon"))
                .andExpect(jsonPath("$.rows[1].stalled").value(false))
                .andExpect(jsonPath("$.rows[1].timeInStage").value("under a minute"))
                .andExpect(jsonPath("$.rows[0].stage").doesNotExist())
                .andExpect(jsonPath("$.rows[0].waitingOn").doesNotExist());
    }

    @Test
    void salonGoesStalledAfterThreeDaysWithoutConnecting() throws Exception {
        long id = createSalon("Willow Hair Studio");
        addFeature(id, "WHATSAPP_REMINDERS");
        moveStageStartBack(id, Duration.ofDays(3).plusHours(1));

        mvc.perform(get("/api/overview"))
                .andExpect(jsonPath("$.rows[0].stalled").value(true))
                .andExpect(jsonPath("$.rows[0].timeInStage").value("3d 1h"))
                .andExpect(jsonPath("$.stats.stalled").value(1));
    }

    @Test
    void statsCountOnboardingStalledAndLive() throws Exception {
        long id = createSalon("Willow Hair Studio");
        addFeature(id, "WHATSAPP_REMINDERS");
        addFeature(id, "BRANDED_APP");

        mvc.perform(get("/api/overview"))
                .andExpect(jsonPath("$.stats.onboarding").value(1))
                .andExpect(jsonPath("$.stats.stalled").value(0))
                .andExpect(jsonPath("$.stats.live").value(1));
    }

    @Test
    void homeShowsTheOverview() throws Exception {
        mvc.perform(get("/")).andExpect(forwardedUrl("/index.html"));
    }
}
