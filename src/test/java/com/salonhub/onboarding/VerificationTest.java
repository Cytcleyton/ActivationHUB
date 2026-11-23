package com.salonhub.onboarding;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// The salon owner's Verify on Meta link.
class VerificationTest extends ApiTestBase {

    private long readyToVerify() throws Exception {
        long id = connectedSalon("Willow Hair Studio");
        mvc.perform(post("/api/salons/" + id + "/register-number")).andExpect(status().isOk());
        return id;
    }

    @Test
    void linkSendsTheSalonToMetaForItsBusiness() throws Exception {
        long id = readyToVerify();

        mvc.perform(get("/api/salons/" + id + "/verify-on-meta"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", startsWith("https://business.facebook.com/")))
                .andExpect(header().string("Location", containsString("business_id=sim-business-" + id)));
    }

    @Test
    void openingTheLinkCountsAsStartingVerification() throws Exception {
        long id = readyToVerify();
        mvc.perform(get("/api/salons/" + id + "/verify-on-meta"));

        mvc.perform(get("/api/salons/" + id))
                .andExpect(jsonPath("$.whatsapp.stage").value("VERIFICATION_STARTED"))
                .andExpect(jsonPath("$.whatsapp.verificationLabel").value("Started, not submitted"))
                .andExpect(jsonPath("$.events[1].type").value("VERIFICATION_OPENED"));
    }

    @Test
    void salonMustConnectFirst() throws Exception {
        long id = createSalon("Willow Hair Studio");
        addFeature(id, "WHATSAPP_REMINDERS");

        mvc.perform(get("/api/salons/" + id + "/verify-on-meta"))
                .andExpect(status().isConflict());
    }
}
