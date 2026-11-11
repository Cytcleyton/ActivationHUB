package com.salonhub.onboarding;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SalonApiTest extends ApiTestBase {

    @Test
    void addedSalonAppearsInTheList() throws Exception {
        postSalon("  Willow Hair Studio  ")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Willow Hair Studio"));

        mvc.perform(get("/api/salons"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.salons", hasSize(1)))
                .andExpect(jsonPath("$.salons[0].name").value("Willow Hair Studio"))
                .andExpect(jsonPath("$.salons[0].demo").value(false))
                .andExpect(jsonPath("$.salons[0].createdAt").exists())
                .andExpect(jsonPath("$.salons[0].features", hasSize(0)));
    }

    @Test
    void listIsNewestFirst() throws Exception {
        createSalon("First");
        createSalon("Second");

        mvc.perform(get("/api/salons"))
                .andExpect(jsonPath("$.salons[0].name").value("Second"))
                .andExpect(jsonPath("$.salons[1].name").value("First"));
    }

    @Test
    void emptyNameIsRejected() throws Exception {
        postSalon("   ")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Enter the salon's name."));

        mvc.perform(get("/api/salons")).andExpect(jsonPath("$.salons", hasSize(0)));
    }

    @Test
    void tooLongNameIsRejected() throws Exception {
        postSalon("x".repeat(121)).andExpect(status().isBadRequest());
    }
}
