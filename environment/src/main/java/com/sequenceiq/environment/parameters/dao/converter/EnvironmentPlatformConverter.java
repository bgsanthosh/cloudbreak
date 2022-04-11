package com.sequenceiq.environment.parameters.dao.converter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.environment.environment.domain.EnvironmentPlatform;

public class EnvironmentPlatformConverter extends DefaultEnumConverter<EnvironmentPlatform> {

    @Override
    public EnvironmentPlatform getDefault() {
        return EnvironmentPlatform.PAAS;
    }
}
