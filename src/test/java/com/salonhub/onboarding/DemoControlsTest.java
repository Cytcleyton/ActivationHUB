package com.salonhub.onboarding;

import com.salonhub.onboarding.domain.Salon;
import com.salonhub.onboarding.domain.SalonSource;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DemoControlsTest extends ApiTestBase {

    private ResultActions decide(long id, String status) throws Exception {
        return mvc.perform(post("/api/salons/" + id + "/demo/verification")
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\": \"" + status + "\"}"));
    }

    private ResultActions skip(long id, int days) throws Exception {
        return mvc.perform(post("/api/salons/" + id + "/demo/skip-ahead")
                .contentType(MediaType.APPLICATION_JSON).content("{\"days\": " + days + "}"));
    }

    private long registeredSalon() throws Exception {
        long id = connectedSalon("Willow Hair Studio");
        mvc.perform(post("/api/salons/" + id + "/register-number")).andExpect(status().isOk());
        return id;
    }

    private void makeDemo(long id) {
        Salon salon = salons.findById(id).orElseThrow();
        salon.setSource(SalonSource.DEMO);
        salons.save(salon);
    }

    @Test
    void simulatedDecisionTakesARealSalonLive() throws Exception {
        long id = registeredSalon();

        decide(id, "verified")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.whatsapp.stage").value("LIVE"))
                .andExpect(jsonPath("$.whatsapp.simulatedVerification").value("verified"))
                .andExpect(jsonPath("$.events[*].detail").value(hasItem("Business verification: not_verified → verified")));
    }

    @Test
    void simulatedDecisionSurvivesTheNextCheckWithMeta() throws Exception {
        long id = registeredSalon();
        decide(id, "pending");

        mvc.perform(post("/api/salons/" + id + "/refresh"))
                .andExpect(jsonPath("$.whatsapp.stage").value("VERIFICATION_IN_REVIEW"));
    }

    @Test
    void choosingMetasRealStatusStopsSimulating() throws Exception {
        long id = registeredSalon();
        decide(id, "rejected");

        decide(id, "meta")
                .andExpect(jsonPath("$.whatsapp.stage").value("VERIFICATION_NOT_STARTED"))
                .andExpect(jsonPath("$.whatsapp.simulatedVerification").value(nullValue()));
    }

    @Test
    void demoSalonsStatusIsSimplySet() throws Exception {
        long id = registeredSalon();
        makeDemo(id);

        decide(id, "pending_need_more_info")
                .andExpect(jsonPath("$.whatsapp.stage").value("VERIFICATION_NEEDS_INFO"))
                .andExpect(jsonPath("$.whatsapp.simulatedVerification").value(nullValue()));
    }

    @Test
    void unknownDecisionIsRejected() throws Exception {
        long id = registeredSalon();
        decide(id, "approved").andExpect(status().isBadRequest());
    }

    @Test
    void realSalonMustConnectBeforeADecision() throws Exception {
        long id = createSalon("Willow Hair Studio");
        addFeature(id, "WHATSAPP_REMINDERS");
        decide(id, "verified").andExpect(status().isConflict());
    }

    @Test
    void skippingAheadMovesTheHistoryAndShowsTheStall() throws Exception {
        long id = createSalon("Fade Factory");
        addFeature(id, "WHATSAPP_REMINDERS");
        Instant createdBefore = salons.findById(id).orElseThrow().getCreatedAt();

        // "Waiting for salon to connect" is flagged after 3 days.
        skip(id, 4)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.whatsapp.stalled").value(true));

        Instant createdAfter = salons.findById(id).orElseThrow().getCreatedAt();
        assertThat(Duration.between(createdAfter, createdBefore)).isEqualTo(Duration.ofDays(4));
    }

    @Test
    void skipIsLimited() throws Exception {
        long id = createSalon("Fade Factory");
        skip(id, 0).andExpect(status().isBadRequest());
        skip(id, 31).andExpect(status().isBadRequest());
    }

    @Test
    void resetLetsTheSignupBeRehearsedAgain() throws Exception {
        long id = registeredSalon();
        decide(id, "verified");

        mvc.perform(post("/api/salons/" + id + "/demo/reset"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.whatsapp.stage").value("NOT_STARTED"))
                .andExpect(jsonPath("$.whatsapp.connected").value(false))
                .andExpect(jsonPath("$.whatsapp.simulatedVerification").value(nullValue()));

        mvc.perform(post("/api/salons/" + id + "/signup/launched")).andExpect(status().isOk());
    }

    @Test
    void demoSalonsCantBeReset() throws Exception {
        long id = registeredSalon();
        makeDemo(id);
        mvc.perform(post("/api/salons/" + id + "/demo/reset")).andExpect(status().isBadRequest());
    }
}
