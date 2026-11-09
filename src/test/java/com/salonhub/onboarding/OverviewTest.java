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

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:salontest")
@AutoConfigureMockMvc
class OverviewTest {

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
    void homeShowsTheOverview() throws Exception {
        mvc.perform(get("/")).andExpect(forwardedUrl("/index.html"));
    }

    private long createSalon(String name) throws Exception {
        String body = mvc.perform(post("/api/salons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"" + name + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("id").asLong();
    }

    private void addFeature(long salonId, String type) throws Exception {
        mvc.perform(post("/api/salons/" + salonId + "/features/" + type)).andExpect(status().isOk());
    }
}
