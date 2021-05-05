package software.amazon.nimblestudio.launchprofile;

import software.amazon.awssdk.services.nimble.NimbleClient;
import software.amazon.awssdk.services.nimble.model.CreateLaunchProfileRequest;
import software.amazon.awssdk.services.nimble.model.CreateLaunchProfileResponse;
import software.amazon.awssdk.services.nimble.model.GetLaunchProfileRequest;
import software.amazon.awssdk.services.nimble.model.GetLaunchProfileResponse;
import software.amazon.awssdk.services.nimble.model.LaunchProfileState;
import software.amazon.awssdk.services.nimble.model.NimbleException;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

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
                    "AWS-NimbleStudio-LaunchProfile::Create",
                    proxyClient,
                    progress.getResourceModel(),
                    progress.getCallbackContext()
                )
                .translateToServiceRequest(model -> {
                    final CreateLaunchProfileRequest.Builder createLaunchProfileRequestBuilder = CreateLaunchProfileRequest.builder()
                        .clientToken(request.getClientRequestToken())
                        .name(model.getName())
                        .ec2SubnetIds(model.getEc2SubnetIds())
                        .streamConfiguration(Translator.fromModelStreamConfiguration(model.getStreamConfiguration()))
                        .studioComponentIds(model.getStudioComponentIds())
                        .launchProfileProtocolVersions(model.getLaunchProfileProtocolVersions())
                        .studioId(model.getStudioId())
                        .tags(model.getTags());

                    if (!StringUtils.isEmpty(model.getDescription())) {
                        createLaunchProfileRequestBuilder.description(model.getDescription());
                    }

                    return createLaunchProfileRequestBuilder.build();
                })
                .makeServiceCall((awsRequest, client) -> {
                    try {
                        final CreateLaunchProfileResponse createLaunchProfileResponse = client
                                .injectCredentialsAndInvokeV2(awsRequest, client.client()::createLaunchProfile);

                        logger.log(String.format("%s [%s] creation requested successfully", ResourceModel.TYPE_NAME,
                                createLaunchProfileResponse.launchProfile().launchProfileId()));

                        return createLaunchProfileResponse;
                    } catch (final NimbleException e) {
                        logger.log(String.format("Exception during creation: %s", ResourceModel.TYPE_NAME));
                        throw Translator.translateToCfnException(e);
                    }
                })
                .stabilize((awsRequest, awsResponse, client, model, context) -> {
                    final String launchProfileId = awsResponse.launchProfile().launchProfileId();
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
                        logger.log(String.format("Exception during creation: %s for id: %s.", ResourceModel.TYPE_NAME,
                            launchProfileId));
                        throw Translator.translateToCfnException(e);
                    }

                    if (LaunchProfileState.CREATE_IN_PROGRESS.equals(getLaunchProfileResponse.launchProfile().state())) {
                        logger.log(String.format("%s [%s] is in state CREATE_IN_PROGRESS, creation in progress",
                                ResourceModel.TYPE_NAME, launchProfileId));
                        return false;
                    }
                    if (LaunchProfileState.READY.equals(getLaunchProfileResponse.launchProfile().state())) {
                        logger.log(String.format("%s [%s] is in state READY, creation succeeded",
                                ResourceModel.TYPE_NAME, launchProfileId));
                        return true;
                    }

                    logger.log(String.format("%s [%s] is in error state %s, creation failed", ResourceModel.TYPE_NAME,
                            launchProfileId, getLaunchProfileResponse.launchProfile().state()));
                    throw new CfnGeneralServiceException(String.format("%s - %s",
                            getLaunchProfileResponse.launchProfile().statusCodeAsString(),
                            getLaunchProfileResponse.launchProfile().statusMessage()));
                })
                .progress()
            )
            .then((r) -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }
}
