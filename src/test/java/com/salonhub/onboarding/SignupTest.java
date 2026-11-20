package com.salonhub.onboarding;

import com.salonhub.onboarding.domain.Salon;
import com.salonhub.onboarding.domain.SalonSource;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Connecting WhatsApp through Meta's signup, against the offline simulator.
class SignupTest extends ApiTestBase {

    // The simulator remembers registered numbers for the whole test run, so each salon gets its own IDs.
    private static String finish(long id) {
        return """
                {"event": "FINISH", "wabaId": "sim-waba-%d", "phoneNumberId": "sim-phone-%d", "businessId": "sim-business-%d"}"""
                .formatted(id, id, id);
    }

    private long salonWithWhatsApp() throws Exception {
        long id = createSalon("Willow Hair Studio");
        addFeature(id, "WHATSAPP_REMINDERS");
        return id;
    }

    private ResultActions postJson(String url, String body) throws Exception {
        return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions launched(long id) throws Exception {
        return mvc.perform(post("/api/salons/" + id + "/signup/launched"));
    }

    private ResultActions session(long id, String body) throws Exception {
        return postJson("/api/salons/" + id + "/signup/session", body);
    }

    private ResultActions code(long id, String code) throws Exception {
        return postJson("/api/salons/" + id + "/signup/code", "{\"code\": \"" + code + "\"}");
    }

    private void connect(long id) throws Exception {
        launched(id);
        session(id, finish(id));
        code(id, "offline-code-1").andExpect(status().isOk());
    }

    @Test
    void openingThePopUpMovesTheSalonOn() throws Exception {
        long id = salonWithWhatsApp();
        launched(id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.whatsapp.stage").value("SIGNUP_IN_PROGRESS"))
                .andExpect(jsonPath("$.events[0].type").value("STAGE_CHANGED"))
                .andExpect(jsonPath("$.events[1].type").value("SIGNUP_LAUNCHED"));
    }

    @Test
    void finishingSignupConnectsTheSalon() throws Exception {
        long id = salonWithWhatsApp();
        launched(id);
        session(id, finish(id)).andExpect(jsonPath("$.whatsapp.stage").value("CONNECTION_INCOMPLETE"));

        code(id, "offline-code-1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.whatsapp.connected").value(true))
                .andExpect(jsonPath("$.whatsapp.phoneConnected").value(false))
                .andExpect(jsonPath("$.whatsapp.stage").value("NUMBER_PENDING"))
                .andExpect(jsonPath("$.whatsapp.waitingOn").value("Onboarding team"))
                .andExpect(jsonPath("$.events[*].detail").value(hasItem("Connected to the salon's WhatsApp account")));
    }

    @Test
    void registeringTheNumberMovesOnToBusinessVerification() throws Exception {
        long id = salonWithWhatsApp();
        connect(id);

        mvc.perform(post("/api/salons/" + id + "/register-number"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.whatsapp.phoneConnected").value(true))
                .andExpect(jsonPath("$.whatsapp.stage").value("VERIFICATION_NOT_STARTED"))
                .andExpect(jsonPath("$.whatsapp.verificationLabel").value("Not started"))
                .andExpect(jsonPath("$.events[*].type").value(hasItem("NUMBER_REGISTERED")))
                .andExpect(jsonPath("$.events[*].detail").value(hasItem("Phone number: — → CONNECTED")));
    }

    @Test
    void numberCantBeRegisteredBeforeConnecting() throws Exception {
        long id = salonWithWhatsApp();
        mvc.perform(post("/api/salons/" + id + "/register-number"))
                .andExpect(status().isConflict());
    }

    @Test
    void givingUpRecordsTheScreen() throws Exception {
        long id = salonWithWhatsApp();
        launched(id);
        session(id, "{\"event\": \"CANCEL\", \"currentStep\": \"PHONE_NUMBER_VERIFICATION\"}")
                .andExpect(jsonPath("$.whatsapp.stage").value("SIGNUP_ABANDONED"))
                .andExpect(jsonPath("$.whatsapp.abandonedAt").value("phone number verification"))
                .andExpect(jsonPath("$.events[1].detail")
                        .value("Salon closed Meta signup at the phone number verification screen"));
    }

    @Test
    void errorInThePopUpIsShown() throws Exception {
        long id = salonWithWhatsApp();
        launched(id);
        session(id, "{\"event\": \"CANCEL\", \"errorMessage\": \"Name not allowed\", \"errorCode\": \"524126\"}")
                .andExpect(jsonPath("$.whatsapp.stage").value("SIGNUP_ERROR"))
                .andExpect(jsonPath("$.whatsapp.signupError").value("Name not allowed (code 524126)"));
    }

    @Test
    void cancelAfterFinishingIsIgnored() throws Exception {
        long id = salonWithWhatsApp();
        connect(id);
        session(id, "{\"event\": \"CANCEL\", \"currentStep\": \"PERMISSIONS\"}")
                .andExpect(jsonPath("$.whatsapp.connected").value(true))
                .andExpect(jsonPath("$.whatsapp.stage").value("NUMBER_PENDING"));
    }

    @Test
    void badCodeIsReportedAndLogged() throws Exception {
        long id = salonWithWhatsApp();
        launched(id);
        session(id, finish(id));
        code(id, "not-a-real-code")
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value(containsString("Invalid or expired code")));

        mvc.perform(get("/api/salons/" + id))
                .andExpect(jsonPath("$.whatsapp.stage").value("CONNECTION_INCOMPLETE"))
                .andExpect(jsonPath("$.events[0].type").value("SIGNUP_ERROR"));
    }

    @Test
    void alreadyConnectedSalonCantStartAgain() throws Exception {
        long id = salonWithWhatsApp();
        connect(id);
        launched(id).andExpect(status().isConflict());
    }

    @Test
    void salonNeedsWhatsAppRemindersFirst() throws Exception {
        long id = createSalon("Willow Hair Studio");
        launched(id)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Add WhatsApp reminders to the salon first."));
    }

    @Test
    void demoSalonsCantConnect() throws Exception {
        long id = salonWithWhatsApp();
        Salon salon = salons.findById(id).orElseThrow();
        salon.setSource(SalonSource.DEMO);
        salons.save(salon);

        launched(id).andExpect(status().isBadRequest());
    }
}
