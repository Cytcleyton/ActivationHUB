package com.salonhub.onboarding;

import com.salonhub.onboarding.domain.SalonSource;
import com.salonhub.onboarding.meta.MetaClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Checking connected salons with Meta again, here against the offline simulator.
class StatusRefreshTest extends ApiTestBase {

    @Autowired
    MetaClient meta;

    // As if the number had been registered outside this app, e.g. by Meta support.
    private void registerAtMeta(long id) {
        meta.registerPhoneNumber("sim-phone-" + id, "246810", "any-token");
    }

    @Test
    void refreshPicksUpChangesAtMeta() throws Exception {
        long id = connectedSalon("Willow Hair Studio");
        registerAtMeta(id);

        mvc.perform(post("/api/salons/" + id + "/refresh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.whatsapp.stage").value("VERIFICATION_NOT_STARTED"))
                .andExpect(jsonPath("$.whatsapp.lastCheckedAt").value(notNullValue()))
                .andExpect(jsonPath("$.whatsapp.lastCheckError").value(nullValue()))
                .andExpect(jsonPath("$.events[*].detail").value(hasItem("Phone number: — → CONNECTED")));
    }

    @Test
    void refreshWithNothingNewLogsNothing() throws Exception {
        long id = connectedSalon("Willow Hair Studio");

        mvc.perform(post("/api/salons/" + id + "/refresh"));
        mvc.perform(post("/api/salons/" + id + "/refresh"))
                .andExpect(jsonPath("$.whatsapp.stage").value("NUMBER_PENDING"))
                .andExpect(jsonPath("$.events[?(@.type == 'STATUS_CHANGED')]", hasSize(2)));
    }

    @Test
    void overviewRefreshChecksEveryConnectedSalon() throws Exception {
        long id = connectedSalon("Willow Hair Studio");
        registerAtMeta(id);

        mvc.perform(post("/api/overview/refresh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows[0].salonName").value("Willow Hair Studio"))
                .andExpect(jsonPath("$.rows[0].statusLabel").value("Business verification not started"));
    }

    @Test
    void demoSalonsArentCheckedWithMeta() throws Exception {
        long id = createSalon("Bloom Beauty");
        addFeature(id, "WHATSAPP_REMINDERS");
        var salon = salons.findById(id).orElseThrow();
        salon.setSource(SalonSource.DEMO);
        salons.save(salon);

        mvc.perform(post("/api/salons/" + id + "/refresh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.whatsapp.lastCheckedAt").value(nullValue()));
    }
}
