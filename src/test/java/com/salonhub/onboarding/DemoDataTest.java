package com.salonhub.onboarding;

import com.salonhub.onboarding.domain.Salon;
import com.salonhub.onboarding.repo.SalonRepository;
import com.salonhub.onboarding.service.StartupData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:demotest",
        "app.seed-demo-data=true"
})
@AutoConfigureMockMvc
class DemoDataTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    SalonRepository salons;

    @Autowired
    StartupData startupData;

    @Test
    void overviewOpensWithEightDemoSalonsFiveStalled() throws Exception {
        mvc.perform(get("/api/overview"))
                .andExpect(jsonPath("$.stats.onboarding").value(8))
                .andExpect(jsonPath("$.stats.stalled").value(5))
                .andExpect(jsonPath("$.stats.live").value(5))
                .andExpect(jsonPath("$.rows[?(@.stalled == true)].salonName", containsInAnyOrder(
                        "Glow Hair Studio", "The Barber Room", "Bloom Beauty", "Lush Lashes", "Fade Factory")))
                .andExpect(jsonPath("$.rows[0].demo").value(true));
    }

    @Test
    void demoSalonPageShowsItsStage() throws Exception {
        Salon bloom = salons.findAll().stream()
                .filter(s -> s.getName().equals("Bloom Beauty")).findFirst().orElseThrow();

        mvc.perform(get("/api/salons/" + bloom.getId()))
                .andExpect(jsonPath("$.demo").value(true))
                .andExpect(jsonPath("$.city").value("Leeds"))
                .andExpect(jsonPath("$.features[0].statusLabel").value("Abandoned Meta signup"))
                .andExpect(jsonPath("$.features[0].statusTone").value("problem"));
    }

    @Test
    void demoSalonsAreOnlySeededOnce() throws Exception {
        long before = salons.count();
        startupData.run(null);
        assertThat(salons.count()).isEqualTo(before);
    }
}
