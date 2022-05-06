package com.sequenceiq.cloudbreak.controller.v4;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.AccountIdNotNeeded;
import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.annotation.DisableCheckPermissions;
import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.UtilV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.requests.RenewCertificateV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.requests.RepoConfigValidationV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.CloudStorageSupportedV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.DeploymentPreferencesV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.RepoConfigValidationV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.ResourceEventResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.SecurityRulesV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.StackMatrixV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.SupportedExternalDatabaseServiceEntryV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.UsedImagesListV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.ccm.key.CcmResourceUtil;
import com.sequenceiq.cloudbreak.ccm.termination.CcmResourceTerminationListener;
import com.sequenceiq.cloudbreak.ccm.termination.CcmV2AgentTerminationListener;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.status.ExtendedHostStatuses;
import com.sequenceiq.cloudbreak.clusterproxy.ConfigRegistrationResponse;
import com.sequenceiq.cloudbreak.common.type.HealthCheckResult;
import com.sequenceiq.cloudbreak.common.type.HealthCheckType;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.converter.SupportedExternalDatabaseServiceEntryToSupportedExternalDatabaseServiceEntryResponseConverter;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterServiceRunner;
import com.sequenceiq.cloudbreak.core.flow2.cluster.provision.service.ClusterProxyService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.notification.NotificationSender;
import com.sequenceiq.cloudbreak.service.StackMatrixService;
import com.sequenceiq.cloudbreak.service.account.PreferencesService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.cluster.RepositoryConfigValidationService;
import com.sequenceiq.cloudbreak.service.filesystem.FileSystemSupportMatrixService;
import com.sequenceiq.cloudbreak.service.image.PlatformStringTransformer;
import com.sequenceiq.cloudbreak.service.image.UsedImagesProvider;
import com.sequenceiq.cloudbreak.service.image.userdata.CcmUserDataService;
import com.sequenceiq.cloudbreak.service.securityrule.SecurityRuleService;
import com.sequenceiq.cloudbreak.service.stack.RuntimeVersionService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.flow.StackOperationService;
import com.sequenceiq.cloudbreak.service.upgrade.ccm.UpgradeCcmOrchestratorService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.validation.externaldatabase.SupportedDatabaseProvider;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.common.api.util.versionchecker.ClientVersionUtil;
import com.sequenceiq.common.api.util.versionchecker.VersionCheckResult;

@Controller
public class UtilV4Controller extends NotificationController implements UtilV4Endpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(UtilV4Controller.class);

    @Inject
    private StackMatrixService stackMatrixService;

    @Inject
    private FileSystemSupportMatrixService fileSystemSupportMatrixService;

    @Inject
    private RepositoryConfigValidationService validationService;

    @Inject
    private PreferencesService preferencesService;

    @Inject
    private SecurityRuleService securityRuleService;

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private NotificationSender notificationSender;

    @Inject
    private StackOperationService stackOperationService;

    @Inject
    private SupportedExternalDatabaseServiceEntryToSupportedExternalDatabaseServiceEntryResponseConverter supportedExternalDatabaseServiceEntryResponseConverter;

    @Inject
    private UsedImagesProvider usedImagesProvider;

    @Inject
    private PlatformStringTransformer platformStringTransformer;

    @Inject
    private StackService stackService;

    @Inject
    private CcmUserDataService ccmUserDataService;

    @Inject
    private CcmResourceTerminationListener ccmResourceTerminationListener;

    @Inject
    private CcmV2AgentTerminationListener ccmV2AgentTerminationListener;

    @Inject
    private UpgradeCcmOrchestratorService upgradeCcmOrchestratorService;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private RuntimeVersionService runtimeVersionService;

    @Inject
    private ClusterProxyService clusterProxyService;

    @Inject
    private ClusterServiceRunner clusterServiceRunner;

    @Value("${info.app.version:}")
    private String cbVersion;

    @Override
    @DisableCheckPermissions
    public VersionCheckResult checkClientVersion(String version) {
        return ClientVersionUtil.checkClientVersion(cbVersion, version);
    }

    @Override
    @DisableCheckPermissions
    public StackMatrixV4Response getStackMatrix(String imageCatalogName, String platform, boolean govCloud) throws Exception {
        return stackMatrixService.getStackMatrix(restRequestThreadLocalService.getRequestedWorkspaceId(),
                platformStringTransformer.getPlatformStringForImageCatalog(platform, govCloud), imageCatalogName);
    }

    @Override
    @DisableCheckPermissions
    public CloudStorageSupportedV4Responses getCloudStorageMatrix(String stackVersion) {
        return new CloudStorageSupportedV4Responses(fileSystemSupportMatrixService.getCloudStorageMatrix(stackVersion));
    }

    @Override
    @DisableCheckPermissions
    public RepoConfigValidationV4Response repositoryConfigValidationRequest(RepoConfigValidationV4Request repoConfigValidationV4Request) {
        return validationService.validate(repoConfigValidationV4Request);
    }

    @Override
    @DisableCheckPermissions
    public SecurityRulesV4Response getDefaultSecurityRules() {
        return securityRuleService.getDefaultSecurityRules();
    }

    @Override
    @DisableCheckPermissions
    public DeploymentPreferencesV4Response deployment() {
        DeploymentPreferencesV4Response response = new DeploymentPreferencesV4Response();
        response.setFeatureSwitchV4s(preferencesService.getFeatureSwitches());
        Set<SupportedExternalDatabaseServiceEntryV4Response> supportedExternalDatabases =
                SupportedDatabaseProvider.supportedExternalDatabases().stream()
                        .map(s -> supportedExternalDatabaseServiceEntryResponseConverter.convert(s))
                        .collect(Collectors.toSet());
        response.setSupportedExternalDatabases(supportedExternalDatabases);
        response.setPlatformSelectionDisabled(preferencesService.isPlatformSelectionDisabled());
        response.setPlatformEnablement(preferencesService.platformEnablement());
        response.setGovPlatformEnablement(preferencesService.govPlatformEnablement());
        return response;
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public ResourceEventResponse postNotificationTest() {
        CloudbreakUser cloudbreakUser = restRequestThreadLocalService.getCloudbreakUser();
        notificationSender.sendTestNotification(cloudbreakUser.getUserId());
        ResourceEventResponse response = new ResourceEventResponse();
        response.setEvent(ResourceEvent.CREDENTIAL_CREATED);
        return response;
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public Response renewCertificate(RenewCertificateV4Request renewCertificateV4Request) {
        stackOperationService.renewCertificate(renewCertificateV4Request.getStackName());
        return Response.ok().build();
    }

    @Override
    @InternalOnly
    @AccountIdNotNeeded
    public UsedImagesListV4Response usedImages(Integer thresholdInDays) {
        return usedImagesProvider.getUsedImages(thresholdInDays);
    }

    @Override
    @InternalOnly
    @AccountIdNotNeeded
    public void pushPillars(Long stackId) {
        try {
            // flowchain:

            // 1. flow
            Stack stack = stackService.getById(stackId);
            Tunnel oldTunnel = stack.getTunnel();
            stack.setTunnel(Tunnel.CCMV2_JUMPGATE);
            stackService.save(stack);

            // TODO: backup the parameters for rollback... might need to parse userdata
//            CcmConnectivityParameters ccmConnectivityParameters = ccmUserDataService.parseAndSaveCcmParameters(stack);
//            stack.setCcmParameters(ccmConnectivityParameters);
//            stackService.save(stack);

            InMemoryStateStore.putStack(stackId, PollGroup.POLLABLE); // here comes the salt update flow...
            clusterServiceRunner.redeployStates(stackId);
            clusterServiceRunner.redeployGatewayPillar(stackId);
            // check connectivity from freeipa node(s)
            upgradeCcmOrchestratorService.reconfigureNginx(stackId);
            registerCcmInternal(stackId);


            // call healthcheck
            healthCheck(stackId);
            // TODO: check what's in healtDetails if OK, rollback if bad

            if (oldTunnel == Tunnel.CCM) {
                // destroy mina
                upgradeCcmOrchestratorService.disableMina(stackId);
                // unregister mina
                String keyId = CcmResourceUtil.getKeyId(stack.getResourceCrn());
                String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
                ccmResourceTerminationListener.deregisterCcmSshTunnelingKey(userCrn, getAccountId(stack), keyId, stack.getMinaSshdServiceId());
            } else if (oldTunnel == Tunnel.CCMV2) {
                // destroy inverting proxy agent (a.k.a. jumpgate-agent)
                upgradeCcmOrchestratorService.disableInvertingProxyAgent(stackId);
                // unregister inverting proxy agent
                ccmV2AgentTerminationListener.deregisterInvertingProxyAgent(stack.getCcmV2AgentCrn());
            }

            // update userdata -- standalone flow 2. flow
        } catch (Exception ex) {
            LOGGER.debug("Error happened", ex);
        } finally {
            InMemoryStateStore.deleteStack(stackId);
        }
    }

    private void registerCcmInternal(Long stackId) {
        Optional<ConfigRegistrationResponse> configRegistrationResponse = clusterProxyService.reRegisterCluster(stackId);
        configRegistrationResponse.ifPresentOrElse(c -> LOGGER.debug(c.toString()), () -> LOGGER.debug("No ccm register response for {}", stackId));
    }

    @Override
    @InternalOnly
    @AccountIdNotNeeded
    public void healthCheck(Long stackId) {
        try {
            Stack stack = stackService.getById(stackId);

            ClusterApi connector = clusterApiConnectors.getConnector(stack);
            ExtendedHostStatuses extendedHostStatuses = connector.clusterStatusService().getExtendedHostStatuses(
                    runtimeVersionService.getRuntimeVersion(stack.getCluster().getId()));
            Set<String> unhealthyHosts = extendedHostStatuses.getHostsHealth().entrySet().stream()
                    .filter(e -> e.getValue().stream().anyMatch(hc -> hc.getType() == HealthCheckType.HOST && hc.getResult() == HealthCheckResult.UNHEALTHY))
                    .map(e -> e.getKey().value())
                    .collect(Collectors.toSet());
            if (!unhealthyHosts.isEmpty()) {
                LOGGER.info("There are unhealthy hosts after registering to Cluster Proxy: {}", unhealthyHosts);
            }
        } catch (Exception ex) {
            LOGGER.debug("Error happened", ex);
        }
    }

    private String getAccountId(Stack stack) {
        return Crn.safeFromString(stack.getResourceCrn()).getAccountId();
    }

}
