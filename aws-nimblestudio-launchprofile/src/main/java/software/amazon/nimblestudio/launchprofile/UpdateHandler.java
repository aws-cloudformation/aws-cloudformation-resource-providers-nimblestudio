package software.amazon.nimblestudio.launchprofile;

import software.amazon.awssdk.services.nimble.NimbleClient;
import software.amazon.awssdk.services.nimble.model.GetLaunchProfileRequest;
import software.amazon.awssdk.services.nimble.model.GetLaunchProfileResponse;
import software.amazon.awssdk.services.nimble.model.UpdateLaunchProfileRequest;
import software.amazon.awssdk.services.nimble.model.UpdateLaunchProfileResponse;
import software.amazon.awssdk.services.nimble.model.LaunchProfileState;
import software.amazon.awssdk.services.nimble.model.NimbleException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import software.amazon.awssdk.utils.StringUtils;

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
                    "AWS-NimbleStudio-LaunchProfile::Update",
                    proxyClient,
                    progress.getResourceModel(),
                    progress.getCallbackContext())
                .translateToServiceRequest(model -> {
                    final UpdateLaunchProfileRequest.Builder updateLaunchProfileBuilder = UpdateLaunchProfileRequest.builder()
                        .studioId(model.getStudioId())
                        .launchProfileId(model.getLaunchProfileId())
                        .clientToken(request.getClientRequestToken());

                    if (!StringUtils.isEmpty(model.getDescription())) {
                        updateLaunchProfileBuilder.description(model.getDescription());
                    }

                    if (!StringUtils.isEmpty(model.getName())) {
                        updateLaunchProfileBuilder.name(model.getName());
                    }

                    if (model.getStreamConfiguration() != null) {
                        updateLaunchProfileBuilder.streamConfiguration(
                            Translator.fromModelStreamConfiguration(model.getStreamConfiguration())
                        );
                    }

                    if (model.getLaunchProfileProtocolVersions() != null) {
                        updateLaunchProfileBuilder.launchProfileProtocolVersions(model.getLaunchProfileProtocolVersions());
                    }

                    if (model.getStudioComponentIds() != null) {
                        updateLaunchProfileBuilder.studioComponentIds(model.getStudioComponentIds());
                    }

                    return updateLaunchProfileBuilder.build();
                })
                .makeServiceCall((awsRequest, client) -> {
                    final String launchProfileId = awsRequest.launchProfileId();
                    try {
                        final GetLaunchProfileRequest getLaunchProfileRequest = GetLaunchProfileRequest.builder()
                            .launchProfileId(launchProfileId)
                            .studioId(awsRequest.studioId())
                            .build();
                        final GetLaunchProfileResponse getLaunchProfileResponse = client
                            .injectCredentialsAndInvokeV2(getLaunchProfileRequest, client.client()::getLaunchProfile);
                        final LaunchProfileState state = getLaunchProfileResponse.launchProfile().state();

                        if (LaunchProfileState.DELETED.equals(state) || LaunchProfileState.CREATE_FAILED.equals(state)) {
                            logger.log(String.format("%s [%s] is in state %s, update failed", ResourceModel.TYPE_NAME,
                                launchProfileId, state.toString()));
                            throw new CfnNotFoundException(ResourceModel.TYPE_NAME, launchProfileId);
                        }

                        final UpdateLaunchProfileResponse updateLaunchProfileResponse = client
                                .injectCredentialsAndInvokeV2(awsRequest, client.client()::updateLaunchProfile);
                        logger.log(String.format("%s [%s] update requested successfully", ResourceModel.TYPE_NAME,
                            launchProfileId));
                        return updateLaunchProfileResponse;
                    } catch (final NimbleException e) {
                        logger.log(String.format("%s [%s] exception during update", ResourceModel.TYPE_NAME,
                                launchProfileId));
                        throw Translator.translateToCfnException(e);
                    }
                })
                .stabilize((awsRequest, awsResponse, client, model, context) -> {
                    final String launchProfileId = awsRequest.launchProfileId();
                    final GetLaunchProfileRequest getLaunchProfileRequest = GetLaunchProfileRequest.builder()
                            .launchProfileId(launchProfileId)
                            .studioId(awsRequest.studioId())
                            .build();
                    GetLaunchProfileResponse getLaunchProfileResponse;

                    try {
                        getLaunchProfileResponse = client.injectCredentialsAndInvokeV2(
                                getLaunchProfileRequest, client.client()::getLaunchProfile);
                    } catch (final NimbleException e) {
                        logger.log(String.format("%s [%s] exception during update", ResourceModel.TYPE_NAME,
                                awsRequest.launchProfileId()));
                        throw Translator.translateToCfnException(e);
                    }

                    if (LaunchProfileState.READY.equals(getLaunchProfileResponse.launchProfile().state())) {
                        logger.log(String.format("%s [%s] is in state READY, update succeeded", ResourceModel.TYPE_NAME,
                                launchProfileId));
                        return true;
                    }

                    if (LaunchProfileState.UPDATE_IN_PROGRESS.equals(getLaunchProfileResponse.launchProfile().state())) {
                        logger.log(String.format("%s [%s] is in state UPDATE_IN_PROGRESS, update in progress",
                                ResourceModel.TYPE_NAME,
                                launchProfileId));
                        return false;
                    }

                    logger.log(String.format("%s [%s] is in error state %s, update failed", ResourceModel.TYPE_NAME,
                            launchProfileId, getLaunchProfileResponse.launchProfile().state()));
                    throw new CfnGeneralServiceException(String.format("Unexpected state %s: %s - %s",
                            getLaunchProfileResponse.launchProfile().stateAsString(),
                            getLaunchProfileResponse.launchProfile().statusCodeAsString(),
                            getLaunchProfileResponse.launchProfile().statusMessage()));
                })
                .progress()
            )
            .then((r) -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }
}
