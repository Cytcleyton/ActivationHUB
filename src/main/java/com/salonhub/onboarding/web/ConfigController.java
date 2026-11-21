package com.salonhub.onboarding.web;

import com.salonhub.onboarding.config.MetaProperties;
import com.salonhub.onboarding.meta.MetaClient;
import com.salonhub.onboarding.web.Dtos.ConfigResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// What the browser needs to open Meta's signup pop-up.
@RestController
public class ConfigController {

    private final MetaProperties props;
    private final MetaClient meta;

    public ConfigController(MetaProperties props, MetaClient meta) {
        this.props = props;
        this.meta = meta;
    }

    @GetMapping("/api/config")
    public ConfigResponse config() {
        return new ConfigResponse(meta.mode(), props.getAppId(), props.getConfigId(), props.getGraphVersion(),
                props.isLiveSignupReady());
    }
}
