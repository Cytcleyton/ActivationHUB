package com.salonhub.onboarding;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.salonhub.onboarding.domain.Salon;
import com.salonhub.onboarding.repo.SalonFeatureRepository;
import com.salonhub.onboarding.repo.SalonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Duration;
import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Shared set-up for the API tests: an in-memory database, emptied before each test, and no demo data.
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:salontest",
        "app.seed-demo-data=false"
})
@AutoConfigureMockMvc
abstract class ApiTestBase {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @Autowired
    SalonRepository salons;

    @Autowired
    SalonFeatureRepository features;

    @BeforeEach
    void cleanDatabase() {
        features.deleteAll();
        salons.deleteAll();
    }

    ResultActions postSalon(String name) throws Exception {
        return mvc.perform(post("/api/salons")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"" + name + "\"}"));
    }

    long createSalon(String name) throws Exception {
        String body = postSalon(name).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("id").asLong();
    }

    ResultActions addFeature(long salonId, String type) throws Exception {
        return mvc.perform(post("/api/salons/" + salonId + "/features/" + type));
    }

    // Pretends the salon entered its current stage some time ago.
    void moveStageStartBack(long salonId, Duration by) {
        Salon salon = salons.findById(salonId).orElseThrow();
        salon.setStageSince(Instant.now().minus(by));
        salons.save(salon);
    }
}
