package software.amazon.nimblestudio.studiocomponent;

import software.amazon.awssdk.services.nimble.NimbleClient;
import software.amazon.awssdk.services.nimble.model.ActiveDirectoryConfiguration;
import software.amazon.awssdk.services.nimble.model.ComputeFarmConfiguration;
import software.amazon.awssdk.services.nimble.model.CreateStudioComponentRequest;
import software.amazon.awssdk.services.nimble.model.CreateStudioComponentResponse;
import software.amazon.awssdk.services.nimble.model.GetStudioComponentRequest;
import software.amazon.awssdk.services.nimble.model.GetStudioComponentResponse;
import software.amazon.awssdk.services.nimble.model.LicenseServiceConfiguration;
import software.amazon.awssdk.services.nimble.model.NimbleException;
import software.amazon.awssdk.services.nimble.model.SharedFileSystemConfiguration;
import software.amazon.awssdk.services.nimble.model.StudioComponentState;
import software.amazon.awssdk.services.nimble.model.StudioComponentConfiguration;
import software.amazon.awssdk.services.nimble.model.StudioComponentInitializationScript;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.List;

import static java.util.stream.Collectors.toList;

public class CreateHandler extends BaseHandlerStd {

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
                    "AWS-NimbleStudio-StudioComponent::Create",
                    proxyClient,
                    progress.getResourceModel(),
                    progress.getCallbackContext()
                )
                .translateToServiceRequest(model -> fromResourceModel(model, request.getClientRequestToken()))
                .makeServiceCall((awsRequest, client) -> {
                    try {
                        final CreateStudioComponentResponse createStudioComponentResponse = client
                            .injectCredentialsAndInvokeV2(awsRequest, client.client()::createStudioComponent);

                        logger.log(String.format("%s [%s] create requested successfully", ResourceModel.TYPE_NAME,
                            createStudioComponentResponse.studioComponent().studioComponentId()));

                        return createStudioComponentResponse;
                    } catch (final NimbleException e) {
                        logger.log(String.format("Exception during CREATE: %s.", ResourceModel.TYPE_NAME));
                        throw ExceptionTranslator.translateToCfnException(e);
                    }
                })
                .stabilize((awsRequest, awsResponse, client, model, context) -> {
                    final String studioComponentId = awsResponse.studioComponent().studioComponentId();
                    model.setStudioComponentId(studioComponentId);

                    GetStudioComponentRequest getStudioComponentRequest = GetStudioComponentRequest.builder()
                            .studioId(awsRequest.studioId())
                            .studioComponentId(studioComponentId)
                            .build();
                    GetStudioComponentResponse getStudioComponentResponse;

                    try {
                        getStudioComponentResponse = client.injectCredentialsAndInvokeV2(
                                getStudioComponentRequest, client.client()::getStudioComponent);
                    } catch (final NimbleException e) {
                        logger.log(String.format("Exception during creation: %s for id: %s.", ResourceModel.TYPE_NAME,
                                studioComponentId));
                        throw ExceptionTranslator.translateToCfnException(e);
                    }

                    if (StudioComponentState.CREATE_IN_PROGRESS.equals(getStudioComponentResponse.studioComponent().state())) {
                        logger.log(String.format("%s [%s] is in state CREATE_IN_PROGRESS, creation in progress",
                                ResourceModel.TYPE_NAME, studioComponentId));
                        return false;
                    }
                    if (StudioComponentState.READY.equals(getStudioComponentResponse.studioComponent().state())) {
                        logger.log(String.format("%s [%s] is in state READY, creation succeeded",
                                ResourceModel.TYPE_NAME, studioComponentId));
                        return true;
                    }

                    logger.log(String.format("%s [%s] is in error state %s, creation failed", ResourceModel.TYPE_NAME,
                            studioComponentId, getStudioComponentResponse.studioComponent().state()));
                    throw new CfnGeneralServiceException(String.format("Unexpected state %s: %s - %s",
                            getStudioComponentResponse.studioComponent().stateAsString(),
                            getStudioComponentResponse.studioComponent().statusCodeAsString(),
                            getStudioComponentResponse.studioComponent().statusMessage()));
                })
                .progress()
            )
            .then((r) -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }


    private CreateStudioComponentRequest fromResourceModel(final ResourceModel model, final String requestClientToken) {
        // Only studioId, configuration, Name and Type are required
        final CreateStudioComponentRequest.Builder createStudioComponentRequestBuilder = CreateStudioComponentRequest.builder()
            .clientToken(requestClientToken)
            .studioId(model.getStudioId())
            .name(model.getName())
            .type(model.getType())
            .tags(model.getTags());

        final StudioComponentConfiguration.Builder studioComponentConfigurationBuilder = StudioComponentConfiguration.builder();

        boolean hasConfiguration = false;

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
            hasConfiguration = true;
        }

        if (model.getConfiguration() != null && model.getConfiguration().getComputeFarmConfiguration() != null) {
            studioComponentConfigurationBuilder.computeFarmConfiguration(
                ComputeFarmConfiguration.builder()
                    .activeDirectoryUser(model.getConfiguration().getComputeFarmConfiguration().getActiveDirectoryUser())
                    .endpoint(model.getConfiguration().getComputeFarmConfiguration().getEndpoint())
                    .build()
                );
            hasConfiguration = true;
        }

        if (model.getConfiguration() != null && model.getConfiguration().getLicenseServiceConfiguration() != null) {
            studioComponentConfigurationBuilder.licenseServiceConfiguration(
                LicenseServiceConfiguration.builder()
                    .endpoint(model.getConfiguration().getLicenseServiceConfiguration().getEndpoint())
                    .build()
                );
            hasConfiguration = true;
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
            hasConfiguration = true;
        }

        if (!StringUtils.isEmpty(model.getDescription())) {
            createStudioComponentRequestBuilder.description(model.getDescription());
        }

        if (model.getInitializationScripts() != null) {
            createStudioComponentRequestBuilder.initializationScripts(
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


        if (model.getScriptParameters() != null) {
            createStudioComponentRequestBuilder.scriptParameters(
                model.getScriptParameters().stream()
                    .map(sp -> software.amazon.awssdk.services.nimble.model.ScriptParameterKeyValue.builder()
                        .key(sp.getKey())
                        .value(sp.getValue())
                        .build())
                .collect(toList())
            );
        }

        if (model.getEc2SecurityGroupIds() != null) {
            createStudioComponentRequestBuilder.ec2SecurityGroupIds(model.getEc2SecurityGroupIds());
        }

        if (!StringUtils.isEmpty(model.getSubtype())) {
            createStudioComponentRequestBuilder.subtype(model.getSubtype());
        }

        if (hasConfiguration) {
            createStudioComponentRequestBuilder.configuration(studioComponentConfigurationBuilder.build());
        }

        return createStudioComponentRequestBuilder.build();
    }

}
