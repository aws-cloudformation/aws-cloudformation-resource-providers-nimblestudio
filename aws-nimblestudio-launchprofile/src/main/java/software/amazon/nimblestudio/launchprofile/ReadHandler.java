package software.amazon.nimblestudio.launchprofile;

import software.amazon.awssdk.services.nimble.NimbleClient;
import software.amazon.awssdk.services.nimble.model.GetLaunchProfileRequest;
import software.amazon.awssdk.services.nimble.model.GetLaunchProfileResponse;
import software.amazon.awssdk.services.nimble.model.LaunchProfile;
import software.amazon.awssdk.services.nimble.model.LaunchProfileState;
import software.amazon.awssdk.services.nimble.model.NimbleException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class ReadHandler extends BaseHandlerStd {

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
                    "AWS-NimbleStudio-LaunchProfile::Read",
                    proxyClient,
                    progress.getResourceModel(),
                    progress.getCallbackContext())
                .translateToServiceRequest(model -> GetLaunchProfileRequest.builder()
                    .launchProfileId(model.getLaunchProfileId())
                    .studioId(model.getStudioId())
                    .build())
                .makeServiceCall((awsRequest, client) -> {
                    try {
                        final GetLaunchProfileResponse getResponse = client
                                .injectCredentialsAndInvokeV2(awsRequest, client.client()::getLaunchProfile);
                        final LaunchProfileState state = getResponse.launchProfile().state();

                        if (LaunchProfileState.DELETED.equals(state) || LaunchProfileState.CREATE_FAILED.equals(state)) {
                            // If a resource was deleted, read request needs to throw a NotFoundException
                            logger.log(String.format("%s [%s] is in state %s, unable to get resource",
                                ResourceModel.TYPE_NAME, getResponse.launchProfile().launchProfileId(), state));
                            throw new CfnNotFoundException(ResourceModel.TYPE_NAME, getResponse.launchProfile().launchProfileId());
                        }

                        logger.log(String.format("%s [%s] read successful",
                                ResourceModel.TYPE_NAME, getResponse.launchProfile().launchProfileId()));

                        return getResponse;
                    } catch (final NimbleException e) {
                        logger.log(String.format("%s [%s] exception during read",
                                ResourceModel.TYPE_NAME,
                                awsRequest.launchProfileId()));
                        throw Translator.translateToCfnException(e);
                    }
                })
                .done((awsResponse) -> ProgressEvent.defaultSuccessHandler(
                    ResourceModel.builder()
                        .studioId(request.getDesiredResourceState().getStudioId())
                        .launchProfileId(request.getDesiredResourceState().getLaunchProfileId())
                        .description(awsResponse.launchProfile().description())
                        .launchProfileId(awsResponse.launchProfile().launchProfileId())
                        .name(awsResponse.launchProfile().name())
                        .ec2SubnetIds(awsResponse.launchProfile().ec2SubnetIds())
                        .streamConfiguration(
                            Translator.toModelStreamConfiguration(awsResponse.launchProfile().streamConfiguration()))
                        .launchProfileProtocolVersions(awsResponse.launchProfile().launchProfileProtocolVersions())
                        .studioComponentIds(awsResponse.launchProfile().studioComponentIds())
                        .tags(awsResponse.launchProfile().tags())
                        .build())));
    }
}
