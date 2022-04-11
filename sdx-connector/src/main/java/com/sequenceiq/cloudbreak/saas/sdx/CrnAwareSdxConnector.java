package com.sequenceiq.cloudbreak.saas.sdx;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptResults;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.saas.sdx.polling.PollingResult;

@Service
public class CrnAwareSdxConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(CrnAwareSdxConnector.class);

    @Inject
    private Map<CrnResourceDescriptor, SdxService<?>> platformDependentServiceMap;

    public void delete(String sdxCrn, Boolean force) {
        platformDependentServiceMap.get(CrnResourceDescriptor.getByCrnString(sdxCrn)).deleteSdx(sdxCrn, force);
    }

    public AttemptResult<Object> getAttemptResultForDeletion(Set<String> sdxCrns, String environmentName, String environmentCrn) {
        Function<CrnResourceDescriptor, AttemptResult<Object>> attemptResultFunction = crnDescriptor ->
                getAttemptResultForPolling(platformDependentServiceMap.get(crnDescriptor).getPollingResultForDeletion(environmentName, environmentCrn),
                        "SDX deletion is failed for these: %s");
        switch (calculatePollingTarget(sdxCrns)) {
            case PAAS:
                return attemptResultFunction.apply(CrnResourceDescriptor.DATALAKE);
            case SAAS:
                return attemptResultFunction.apply(CrnResourceDescriptor.SDX_SAAS_INSTANCE);
            default:
                return AttemptResults.breakFor("Polling for SDX deletion should be happen only for SaaS or PaaS only at the same time.");
        }
    }

    public Set<String> listSdxCrns(String environmentName, String environmentCrn, TargetPlatform target) {
        switch (target) {
            case SAAS:
                return platformDependentServiceMap.get(CrnResourceDescriptor.SDX_SAAS_INSTANCE).listSdxCrns(environmentName, environmentCrn);
            case PAAS:
                return platformDependentServiceMap.get(CrnResourceDescriptor.DATALAKE).listSdxCrns(environmentName, environmentCrn);
            default:
                return Set.of();
        }
    }

    private TargetPlatform calculatePollingTarget(Set<String> sdxCrns) {
        if (sdxCrns.stream().allMatch(crn -> Crn.ResourceType.INSTANCE.equals(Crn.safeFromString(crn).getResourceType()))) {
            return TargetPlatform.SAAS;
        } else if (sdxCrns.stream().allMatch(crn -> Crn.ResourceType.DATALAKE.equals(Crn.safeFromString(crn).getResourceType()))) {
            return TargetPlatform.PAAS;
        }
        throw new IllegalStateException("Polling for SDX should be happen only for SaaS or PaaS only at the same time.");
    }

    private AttemptResult<Object> getAttemptResultForPolling(Map<String, PollingResult> pollingResult, String failedPollingErrorMessageTemplate) {
        if (!pollingResult.isEmpty()) {
            Set<String> failedSdxCrns = pollingResult.entrySet().stream()
                    .filter(entry -> PollingResult.FAILED.equals(entry.getValue()))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());
            if (!failedSdxCrns.isEmpty()) {
                String errorMessage = String.format(failedPollingErrorMessageTemplate, Joiner.on(",").join(failedSdxCrns));
                LOGGER.info(errorMessage);
                return AttemptResults.breakFor(new IllegalStateException(errorMessage));
            }
            return AttemptResults.justContinue();
        }
        return AttemptResults.finishWith(null);
    }
}
