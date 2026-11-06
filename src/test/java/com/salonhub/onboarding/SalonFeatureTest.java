package com.salonhub.onboarding;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.salonhub.onboarding.repo.SalonFeatureRepository;
import com.salonhub.onboarding.repo.SalonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:salontest")
@AutoConfigureMockMvc
class SalonFeatureTest {

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

    @Test
    void newSalonListsAllFeaturesAsNotAdded() throws Exception {
        long id = createSalon("Willow Hair Studio");

        mvc.perform(get("/api/salons/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Willow Hair Studio"))
                .andExpect(jsonPath("$.features", hasSize(3)))
                .andExpect(jsonPath("$.features[0].type").value("WHATSAPP_REMINDERS"))
                .andExpect(jsonPath("$.features[0].implemented").value(true))
                .andExpect(jsonPath("$.features[0].added").value(false))
                .andExpect(jsonPath("$.features[1].implemented").value(false));
    }

    @Test
    void demoOnlyFeatureGoesLiveStraightAway() throws Exception {
        long id = createSalon("Willow Hair Studio");

        mvc.perform(post("/api/salons/" + id + "/features/BRANDED_APP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.features[1].type").value("BRANDED_APP"))
                .andExpect(jsonPath("$.features[1].added").value(true))
                .andExpect(jsonPath("$.features[1].addedAt").exists())
                .andExpect(jsonPath("$.features[1].statusLabel").value("Live"))
                .andExpect(jsonPath("$.features[1].statusTone").value("done"));
    }

    @Test
    void whatsAppRemindersWaitsForTheSalonToConnect() throws Exception {
        long id = createSalon("Willow Hair Studio");

        mvc.perform(post("/api/salons/" + id + "/features/WHATSAPP_REMINDERS"))
                .andExpect(jsonPath("$.features[0].added").value(true))
                .andExpect(jsonPath("$.features[0].statusLabel").value("Waiting for salon to connect"))
                .andExpect(jsonPath("$.features[0].statusTone").value("waiting"));
    }

    @Test
    void addingAFeatureTwiceDoesNothing() throws Exception {
        long id = createSalon("Willow Hair Studio");

        mvc.perform(post("/api/salons/" + id + "/features/ADS_MANAGER")).andExpect(status().isOk());
        mvc.perform(post("/api/salons/" + id + "/features/ADS_MANAGER")).andExpect(status().isOk());

        assertThat(features.findBySalonIdOrderByAddedAtAscIdAsc(id)).hasSize(1);
    }

    @Test
    void addedFeaturesShowAsChipsOnTheList() throws Exception {
        long id = createSalon("Willow Hair Studio");
        mvc.perform(post("/api/salons/" + id + "/features/BRANDED_APP"));
        mvc.perform(post("/api/salons/" + id + "/features/WHATSAPP_REMINDERS"));

        mvc.perform(get("/api/salons"))
                .andExpect(jsonPath("$.salons[0].features", hasSize(2)))
                .andExpect(jsonPath("$.salons[0].features[0].label").value("Salon branded app"))
                .andExpect(jsonPath("$.salons[0].features[0].statusLabel").value("Live"))
                .andExpect(jsonPath("$.salons[0].features[1].label").value("WhatsApp reminders"));
    }

    @Test
    void unknownFeatureIsRejected() throws Exception {
        long id = createSalon("Willow Hair Studio");

        mvc.perform(post("/api/salons/" + id + "/features/TELEPORTER"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Unknown feature: TELEPORTER"));
    }

    @Test
    void unknownSalonIsNotFound() throws Exception {
        mvc.perform(get("/api/salons/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Salon not found."));
        mvc.perform(post("/api/salons/999999/features/BRANDED_APP")).andExpect(status().isNotFound());
        mvc.perform(delete("/api/salons/999999")).andExpect(status().isNotFound());
    }

    @Test
    void deletingASalonAlsoDeletesItsFeatures() throws Exception {
        long id = createSalon("Willow Hair Studio");
        mvc.perform(post("/api/salons/" + id + "/features/BRANDED_APP"));

        mvc.perform(delete("/api/salons/" + id)).andExpect(status().isNoContent());

        mvc.perform(get("/api/salons/" + id)).andExpect(status().isNotFound());
        mvc.perform(get("/api/salons")).andExpect(jsonPath("$.salons", hasSize(0)));
        assertThat(features.count()).isZero();
    }

    private long createSalon(String name) throws Exception {
        String body = mvc.perform(post("/api/salons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"" + name + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("id").asLong();
    }
}
