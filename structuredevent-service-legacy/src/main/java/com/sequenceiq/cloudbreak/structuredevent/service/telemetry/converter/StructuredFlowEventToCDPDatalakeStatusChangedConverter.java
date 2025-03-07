package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;

@Component
public class StructuredFlowEventToCDPDatalakeStatusChangedConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(StructuredFlowEventToCDPDatalakeStatusChangedConverter.class);

    @Inject
    private StructuredEventToCDPOperationDetailsConverter operationDetailsConverter;

    @Inject
    private StructuredEventToCDPStatusDetailsConverter statusDetailsConverter;

    @Inject
    private StructuredEventToCDPClusterDetailsConverter clusterDetailsConverter;

    @Inject
    private StructuredEventToCDPDatalakeFeaturesConverter featuresConverter;

    public UsageProto.CDPDatalakeStatusChanged convert(StructuredFlowEvent structuredFlowEvent, UsageProto.CDPClusterStatus.Value status) {
        UsageProto.CDPDatalakeStatusChanged.Builder cdpDatalakeStatusChanged = UsageProto.CDPDatalakeStatusChanged.newBuilder();

        cdpDatalakeStatusChanged.setOperationDetails(operationDetailsConverter.convert(structuredFlowEvent));

        cdpDatalakeStatusChanged.setNewStatus(status);

        cdpDatalakeStatusChanged.setStatusDetails(statusDetailsConverter.convert(structuredFlowEvent));

        cdpDatalakeStatusChanged.setFeatures(featuresConverter.convert(structuredFlowEvent));

        if (structuredFlowEvent != null && structuredFlowEvent.getOperation() != null) {
            cdpDatalakeStatusChanged.setEnvironmentCrn(structuredFlowEvent.getOperation().getEnvironmentCrn());
        }

        cdpDatalakeStatusChanged.setClusterDetails(clusterDetailsConverter.convert(structuredFlowEvent));

        UsageProto.CDPDatalakeStatusChanged ret = cdpDatalakeStatusChanged.build();
        LOGGER.debug("Converted CDPDatalakeStatusChanged telemetry event: {}", ret);
        return ret;
    }
}
