package com.sequenceiq.cloudbreak.cloud.aws.common;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

@Service
public class AwsAuthenticator implements Authenticator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsAuthenticator.class);

    @Inject
    private CommonAwsClient awsClient;

    @Override
    public Platform platform() {
        return AwsConstants.AWS_PLATFORM;
    }

    @Override
    public Variant variant() {
        return AwsConstants.AWS_DEFAULT_VARIANT;
    }

    @Override
    public AuthenticatedContext authenticate(CloudContext cloudContext, CloudCredential cloudCredential) {
        LOGGER.debug("Authenticating to aws ...");
        awsClient.checkAwsEnvironmentVariables(cloudCredential);
        return awsClient.createAuthenticatedContext(cloudContext, cloudCredential);
    }
}
