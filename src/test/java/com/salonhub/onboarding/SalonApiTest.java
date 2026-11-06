package com.salonhub.onboarding;

import com.salonhub.onboarding.repo.SalonFeatureRepository;
import com.salonhub.onboarding.repo.SalonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:salontest")
@AutoConfigureMockMvc
class SalonApiTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    SalonRepository salons;

    @Autowired
    SalonFeatureRepository features;

    @BeforeEach
    void cleanDatabase() {
        features.deleteAll();
        salons.deleteAll();
    }

    @Test
    void addedSalonAppearsInTheList() throws Exception {
        createSalon("  Willow Hair Studio  ")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Willow Hair Studio"));

        mvc.perform(get("/api/salons"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.salons", hasSize(1)))
                .andExpect(jsonPath("$.salons[0].name").value("Willow Hair Studio"))
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
        createSalon("   ")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Enter the salon's name."));

        mvc.perform(get("/api/salons")).andExpect(jsonPath("$.salons", hasSize(0)));
    }

    @Test
    void tooLongNameIsRejected() throws Exception {
        createSalon("x".repeat(121)).andExpect(status().isBadRequest());
    }

    @Test
    void homeRedirectsToSalons() throws Exception {
        mvc.perform(get("/")).andExpect(redirectedUrl("/salons.html"));
    }

    private ResultActions createSalon(String name) throws Exception {
        return mvc.perform(post("/api/salons")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"" + name + "\"}"));
    }
}
