package com.salonhub.onboarding.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.salonhub.onboarding.domain.StallPolicy;
import com.salonhub.onboarding.meta.GraphApiMetaClient;
import com.salonhub.onboarding.meta.MetaClient;
import com.salonhub.onboarding.meta.OfflineMetaClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;

@Configuration
@EnableConfigurationProperties({StallProperties.class, AppProperties.class, MetaProperties.class})
public class AppConfig {

    private static final Logger log = LoggerFactory.getLogger(AppConfig.class);

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public StallPolicy stallPolicy(StallProperties stallProperties) {
        return new StallPolicy(stallProperties.getThresholds());
    }

    @Bean
    public MetaClient metaClient(MetaProperties props, ObjectMapper objectMapper) {
        if (props.isLive()) {
            log.info("Meta mode: LIVE (Graph API {}, app {})", props.getGraphVersion(), props.getAppId());
            HttpClient http = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(props.getTimeoutSeconds()))
                    .build();
            return new GraphApiMetaClient(props, http, objectMapper);
        }
        log.info("Meta mode: OFFLINE (simulator, no calls to Meta)");
        return new OfflineMetaClient();
    }
}
