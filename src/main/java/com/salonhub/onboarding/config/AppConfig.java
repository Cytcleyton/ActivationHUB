package com.salonhub.onboarding.config;

import com.salonhub.onboarding.domain.StallPolicy;
import com.salonhub.onboarding.meta.MetaClient;
import com.salonhub.onboarding.meta.OfflineMetaClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties({StallProperties.class, AppProperties.class, MetaProperties.class})
public class AppConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public StallPolicy stallPolicy(StallProperties stallProperties) {
        return new StallPolicy(stallProperties.getThresholds());
    }

    @Bean
    public MetaClient metaClient() {
        return new OfflineMetaClient();
    }
}
