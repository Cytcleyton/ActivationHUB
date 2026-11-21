package com.salonhub.onboarding;

import com.salonhub.onboarding.meta.MetaApiException;
import com.salonhub.onboarding.meta.MetaClient;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// When Meta can't be asked (an expired token, say), the page still loads and shows why.
class StatusCheckFailureTest extends ApiTestBase {

    @MockitoSpyBean
    MetaClient meta;

    @Test
    void failedCheckIsShownNotThrown() throws Exception {
        long id = connectedSalon("Willow Hair Studio");
        doThrow(new MetaApiException("Error validating access token: Session has expired", 190))
                .when(meta).fetchStatus(any(), any());

        mvc.perform(post("/api/salons/" + id + "/refresh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.whatsapp.stage").value("NUMBER_PENDING"))
                .andExpect(jsonPath("$.whatsapp.lastCheckError")
                        .value("(#190) Error validating access token: Session has expired"));
    }
}
