package software.amazon.nimblestudio.launchprofile;

import software.amazon.awssdk.services.nimble.NimbleClient;
import software.amazon.awssdk.services.nimble.model.DeleteLaunchProfileRequest;
import software.amazon.awssdk.services.nimble.model.DeleteLaunchProfileResponse;
import software.amazon.awssdk.services.nimble.model.GetLaunchProfileRequest;
import software.amazon.awssdk.services.nimble.model.GetLaunchProfileResponse;
import software.amazon.awssdk.services.nimble.model.LaunchProfileState;
import software.amazon.awssdk.services.nimble.model.NimbleException;
import software.amazon.awssdk.services.nimble.model.ValidationException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class DeleteHandler extends BaseHandlerStd {

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
                    "AWS-NimbleStudio-LaunchProfile::Delete",
                    proxyClient,
                    progress.getResourceModel(),
                    progress.getCallbackContext())
                .translateToServiceRequest(model -> DeleteLaunchProfileRequest.builder()
                    .clientToken(request.getClientRequestToken())
                    .studioId(model.getStudioId())
                    .launchProfileId(model.getLaunchProfileId())
                    .build())
                .makeServiceCall((awsRequest, client) -> {
                    final String launchProfileId = awsRequest.launchProfileId();

                    try {
                        final GetLaunchProfileRequest getLaunchProfileRequest = GetLaunchProfileRequest.builder()
                                .studioId(awsRequest.studioId())
                                .launchProfileId(launchProfileId)
                                .build();
                        final GetLaunchProfileResponse getLaunchProfileResponse = client.injectCredentialsAndInvokeV2(
                                getLaunchProfileRequest, client.client()::getLaunchProfile);
                        final LaunchProfileState state = getLaunchProfileResponse.launchProfile().state();

                        if(LaunchProfileState.DELETED.equals(state) || LaunchProfileState.CREATE_FAILED.equals(state)) {
                            logger.log(String.format("%s [%s] is already in %s state",
                                    ResourceModel.TYPE_NAME, launchProfileId, state.toString()));
                            throw new CfnNotFoundException(ResourceModel.TYPE_NAME, launchProfileId);
                        }
                        if(LaunchProfileState.DELETE_IN_PROGRESS.equals(state)) {
                            logger.log(String.format("%s [%s] is already in DELETE_IN_PROGRESS state",
                                    ResourceModel.TYPE_NAME,launchProfileId));
                            return null;
                        }

                        final DeleteLaunchProfileResponse deleteLaunchProfileResponse = client
                                .injectCredentialsAndInvokeV2(awsRequest, client.client()::deleteLaunchProfile);

                        logger.log(String.format("%s [%s] deletion requested successfully", ResourceModel.TYPE_NAME,
                                launchProfileId));

                        return deleteLaunchProfileResponse;
                    } catch (final NimbleException e) {
                        logger.log(String.format("%s [%s] exception during deletion", ResourceModel.TYPE_NAME,
                                launchProfileId));
                        throw Translator.translateToCfnException(e);
                    }
                })
                .stabilize((awsRequest, awsResponse, client, model, context) -> {
                    final String launchProfileId = awsRequest.launchProfileId();
                    model.setLaunchProfileId(launchProfileId);

                    GetLaunchProfileRequest getLaunchProfileRequest = GetLaunchProfileRequest.builder()
                        .studioId(awsRequest.studioId())
                        .launchProfileId(launchProfileId)
                        .build();
                    GetLaunchProfileResponse getLaunchProfileResponse;

                    try {
                        getLaunchProfileResponse = client.injectCredentialsAndInvokeV2(
                            getLaunchProfileRequest, client.client()::getLaunchProfile);
                    } catch (final NimbleException e) {
                        logger.log(String.format("%s [%s] exception during deletion", ResourceModel.TYPE_NAME,
                            launchProfileId));
                        throw Translator.translateToCfnException(e);
                    }

                    if (LaunchProfileState.DELETE_IN_PROGRESS.equals(getLaunchProfileResponse.launchProfile().state())) {
                        logger.log(String.format("%s [%s] is in state DELETE_IN_PROGRESS, deletion in progress",
                            ResourceModel.TYPE_NAME, launchProfileId));
                        return false;
                    }
                    if (LaunchProfileState.DELETED.equals(getLaunchProfileResponse.launchProfile().state())) {
                        logger.log(String.format("%s [%s] is in state DELETED, deletion succeeded",
                            ResourceModel.TYPE_NAME, launchProfileId));
                        return true;
                    }

                    logger.log(String.format("%s [%s] is in error state %s, deletion failed", ResourceModel.TYPE_NAME,
                        launchProfileId, getLaunchProfileResponse.launchProfile().state()));
                    throw new CfnGeneralServiceException(String.format("Unexpected state %s: %s - %s",
                        getLaunchProfileResponse.launchProfile().stateAsString(),
                        getLaunchProfileResponse.launchProfile().statusCodeAsString(),
                        getLaunchProfileResponse.launchProfile().statusMessage()));
                })
                .done(awsResponse -> ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .status(OperationStatus.SUCCESS)
                    .build()));
    }
}
