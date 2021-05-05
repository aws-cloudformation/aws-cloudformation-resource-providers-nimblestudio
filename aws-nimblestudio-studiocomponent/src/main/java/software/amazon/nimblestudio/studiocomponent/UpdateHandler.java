package software.amazon.nimblestudio.studiocomponent;

import software.amazon.awssdk.services.nimble.NimbleClient;
import software.amazon.awssdk.services.nimble.model.ActiveDirectoryConfiguration;
import software.amazon.awssdk.services.nimble.model.ComputeFarmConfiguration;
import software.amazon.awssdk.services.nimble.model.GetStudioComponentRequest;
import software.amazon.awssdk.services.nimble.model.GetStudioComponentResponse;
import software.amazon.awssdk.services.nimble.model.LicenseServiceConfiguration;
import software.amazon.awssdk.services.nimble.model.NimbleException;
import software.amazon.awssdk.services.nimble.model.SharedFileSystemConfiguration;
import software.amazon.awssdk.services.nimble.model.StudioComponentConfiguration;
import software.amazon.awssdk.services.nimble.model.StudioComponentInitializationScript;
import software.amazon.awssdk.services.nimble.model.StudioComponentState;
import software.amazon.awssdk.services.nimble.model.UpdateStudioComponentRequest;
import software.amazon.awssdk.services.nimble.model.UpdateStudioComponentResponse;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import software.amazon.awssdk.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class UpdateHandler extends BaseHandlerStd {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<NimbleClient> proxyClient,
        final Logger logger) {

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
            .then(progress -> proxy
                .initiate(
                    "AWS-NimbleStudio-StudioComponent::Update",
                    proxyClient,
                    progress.getResourceModel(),
                    progress.getCallbackContext())
                .translateToServiceRequest(model -> fromResourceModel(model, request.getClientRequestToken()))
                .makeServiceCall((updateStudioComponentRequest, client) -> {
                    final String studioComponentId = updateStudioComponentRequest.studioComponentId();

                    try {
                        final GetStudioComponentRequest getStudioComponentRequest = GetStudioComponentRequest.builder()
                            .studioComponentId(studioComponentId)
                            .studioId(updateStudioComponentRequest.studioId())
                            .build();
                        final GetStudioComponentResponse getStudioComponentResponse = client
                            .injectCredentialsAndInvokeV2(getStudioComponentRequest, client.client()::getStudioComponent);
                        final StudioComponentState state = getStudioComponentResponse.studioComponent().state();

                        if (StudioComponentState.DELETED.equals(state) || StudioComponentState.CREATE_FAILED.equals(state)) {
                            logger.log(String.format("%s [%s] is in state %s, update failed",
                                ResourceModel.TYPE_NAME, studioComponentId, state.toString()));
                            throw new CfnNotFoundException(ResourceModel.TYPE_NAME, studioComponentId);
                        }

                        final UpdateStudioComponentResponse updateStudioComponentResponse = client
                            .injectCredentialsAndInvokeV2(updateStudioComponentRequest,
                                client.client()::updateStudioComponent);

                        logger.log(String.format("%s [%s] update in progress",
                            ResourceModel.TYPE_NAME,
                            updateStudioComponentRequest.studioComponentId()));

                        return updateStudioComponentResponse;
                    } catch (final NimbleException e) {
                        logger.log(String.format("%s [%s] exception during update", ResourceModel.TYPE_NAME,
                            updateStudioComponentRequest.studioComponentId()));
                        throw ExceptionTranslator.translateToCfnException(e);
                    }
                })
                .stabilize((awsRequest, awsResponse, client, model, context) -> {
                    final String studioComponentId = awsRequest.studioComponentId();

                    GetStudioComponentRequest getStudioComponentRequest = GetStudioComponentRequest.builder()
                        .studioId(awsRequest.studioId())
                        .studioComponentId(studioComponentId)
                        .build();
                    GetStudioComponentResponse getStudioComponentResponse;

                    try {
                        getStudioComponentResponse = client.injectCredentialsAndInvokeV2(
                            getStudioComponentRequest, client.client()::getStudioComponent);
                    } catch (final NimbleException e) {
                        logger.log(String.format("%s [%s] exception during update", ResourceModel.TYPE_NAME,
                            studioComponentId));
                        throw ExceptionTranslator.translateToCfnException(e);
                    }

                    if (StudioComponentState.UPDATE_IN_PROGRESS.equals(getStudioComponentResponse.studioComponent().state())) {
                        logger.log(String.format("%s [%s] is in state UPDATE_IN_PROGRESS, update in progress",
                            ResourceModel.TYPE_NAME, studioComponentId));
                        return false;
                    }
                    if (StudioComponentState.READY.equals(getStudioComponentResponse.studioComponent().state())) {
                        logger.log(String.format("%s [%s] is in state READY, update succeeded",
                            ResourceModel.TYPE_NAME, studioComponentId));
                        return true;
                    }

                    logger.log(String.format("%s [%s] is in error state %s, update failed", ResourceModel.TYPE_NAME,
                        studioComponentId, getStudioComponentResponse.studioComponent().state()));
                    throw new CfnGeneralServiceException(String.format("%s - %s",
                        getStudioComponentResponse.studioComponent().statusCodeAsString(),
                        getStudioComponentResponse.studioComponent().statusMessage()));
                })
                .progress()
            )
            .then((r) -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }

    private UpdateStudioComponentRequest fromResourceModel(final ResourceModel model, final String requestClientToken) {
        // Only StudioId and StudioComponentId are required
        final UpdateStudioComponentRequest.Builder updateStudioComponentRequestBuilder = UpdateStudioComponentRequest.builder()
            .clientToken(requestClientToken)
            .studioComponentId(model.getStudioComponentId())
            .studioId(model.getStudioId());

        final StudioComponentConfiguration.Builder studioComponentConfigurationBuilder = StudioComponentConfiguration.builder();

        if (model.getConfiguration() != null && model.getConfiguration().getActiveDirectoryConfiguration() != null) {
            final ActiveDirectoryConfiguration.Builder adConfig = ActiveDirectoryConfiguration.builder();
            if (model.getConfiguration().getActiveDirectoryConfiguration().getComputerAttributes() != null) {
                final List<ActiveDirectoryComputerAttribute> modelComputerAttribute =
                    model.getConfiguration().getActiveDirectoryConfiguration().getComputerAttributes();

                adConfig.computerAttributes(
                    modelComputerAttribute.stream()
                        .map(ca -> software.amazon.awssdk.services.nimble.model.ActiveDirectoryComputerAttribute.builder()
                            .name(ca.getName())
                            .value(ca.getValue())
                            .build())
                        .collect(toList()));
            }
            studioComponentConfigurationBuilder.activeDirectoryConfiguration(
                adConfig
                    .directoryId(model.getConfiguration().getActiveDirectoryConfiguration().getDirectoryId())
                    .organizationalUnitDistinguishedName(model.getConfiguration().getActiveDirectoryConfiguration().getOrganizationalUnitDistinguishedName())
                    .build()
            );
        }

        if (model.getConfiguration() != null && model.getConfiguration().getComputeFarmConfiguration() != null) {
            studioComponentConfigurationBuilder.computeFarmConfiguration(
                ComputeFarmConfiguration.builder()
                    .activeDirectoryUser(model.getConfiguration().getComputeFarmConfiguration().getActiveDirectoryUser())
                    .endpoint(model.getConfiguration().getComputeFarmConfiguration().getEndpoint())
                    .build()
                );
        }

        if (model.getConfiguration() != null && model.getConfiguration().getLicenseServiceConfiguration() != null) {
            studioComponentConfigurationBuilder.licenseServiceConfiguration(
                LicenseServiceConfiguration.builder()
                    .endpoint(model.getConfiguration().getLicenseServiceConfiguration().getEndpoint())
                    .build());
        }

        if (model.getConfiguration() != null && model.getConfiguration().getSharedFileSystemConfiguration() != null) {
            studioComponentConfigurationBuilder.sharedFileSystemConfiguration(
                SharedFileSystemConfiguration.builder()
                    .endpoint(model.getConfiguration().getSharedFileSystemConfiguration().getEndpoint())
                    .fileSystemId(model.getConfiguration().getSharedFileSystemConfiguration().getFileSystemId())
                    .linuxMountPoint(model.getConfiguration().getSharedFileSystemConfiguration().getLinuxMountPoint())
                    .shareName(model.getConfiguration().getSharedFileSystemConfiguration().getShareName())
                    .windowsMountDrive(model.getConfiguration().getSharedFileSystemConfiguration().getWindowsMountDrive())
                    .build()
                );
        }

        if (!StringUtils.isEmpty(model.getDescription())) {
            updateStudioComponentRequestBuilder.description(model.getDescription());
        }

        if (model.getInitializationScripts() != null) {
            updateStudioComponentRequestBuilder.initializationScripts(
                model.getInitializationScripts().stream()
                    .map(is -> StudioComponentInitializationScript.builder()
                        .launchProfileProtocolVersion(is.getLaunchProfileProtocolVersion())
                        .platform(is.getPlatform())
                        .runContext(is.getRunContext())
                        .script(is.getScript())
                        .build())
                    .collect(toList())
                );
        }

        if (!StringUtils.isEmpty(model.getName())) {
            updateStudioComponentRequestBuilder.name(model.getName());
        }

        if (model.getScriptParameters() != null) {
            updateStudioComponentRequestBuilder.scriptParameters(
                model.getScriptParameters().stream()
                    .map(sp -> software.amazon.awssdk.services.nimble.model.ScriptParameterKeyValue.builder()
                        .key(sp.getKey())
                        .value(sp.getValue())
                        .build())
                    .collect(toList())
            );
        }

        if (model.getEc2SecurityGroupIds() != null) {
            updateStudioComponentRequestBuilder.ec2SecurityGroupIds(
                new ArrayList(model.getEc2SecurityGroupIds())
            );
        }

        if (!StringUtils.isEmpty(model.getType())) {
            updateStudioComponentRequestBuilder.type(model.getType());
        }

        if (!StringUtils.isEmpty(model.getSubtype())) {
            updateStudioComponentRequestBuilder.subtype(model.getSubtype());
        }

        updateStudioComponentRequestBuilder.configuration(studioComponentConfigurationBuilder.build());

        return updateStudioComponentRequestBuilder.build();
    }
}
