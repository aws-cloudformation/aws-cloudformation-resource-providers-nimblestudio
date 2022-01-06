package software.amazon.nimblestudio.studio;

import software.amazon.awssdk.services.nimble.NimbleClient;
import software.amazon.awssdk.services.nimble.model.CreateStudioRequest;
import software.amazon.awssdk.services.nimble.model.CreateStudioResponse;
import software.amazon.awssdk.services.nimble.model.GetStudioRequest;
import software.amazon.awssdk.services.nimble.model.GetStudioResponse;
import software.amazon.awssdk.services.nimble.model.NimbleException;
import software.amazon.awssdk.services.nimble.model.StudioState;
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
                        "AWS-NimbleStudio-Studio::Create",
                        proxyClient,
                        progress.getResourceModel(),
                        progress.getCallbackContext()
                )
                .translateToServiceRequest(model -> {
                    final CreateStudioRequest.Builder createStudioRequestBuilder = CreateStudioRequest.builder()
                            .clientToken(request.getClientRequestToken())
                            .studioName(model.getStudioName())
                            .displayName(model.getDisplayName())
                            .adminRoleArn(model.getAdminRoleArn())
                            .userRoleArn(model.getUserRoleArn())
                            .tags(model.getTags());

                    if (model.getStudioEncryptionConfiguration() != null) {
                        createStudioRequestBuilder.studioEncryptionConfiguration(
                                Translator.toModelStudioEncryptionConfiguration(model.getStudioEncryptionConfiguration())
                        );
                    }

                    return createStudioRequestBuilder.build();
                })
                .makeServiceCall((awsRequest, client) -> {
                    try {
                        final CreateStudioResponse createStudioResponse = client
                                .injectCredentialsAndInvokeV2(awsRequest, client.client()::createStudio);

                        logger.log(String.format("%s [%s] creation requested successfully", ResourceModel.TYPE_NAME,
                                createStudioResponse.studio().studioName()));

                        return createStudioResponse;
                    } catch (final NimbleException e) {
                        logger.log(String.format("Exception during creation: %s.", ResourceModel.TYPE_NAME));
                        throw Translator.translateToCfnException(e);
                    }
                })
                .stabilize((awsRequest, awsResponse, client, model, context) -> {
                    final String studioId = awsResponse.studio().studioId();
                    model.setStudioId(studioId);
                    GetStudioResponse getStudioResponse;

                    try {
                        getStudioResponse = client.injectCredentialsAndInvokeV2(
                            GetStudioRequest.builder().studioId(studioId).build(), client.client()::getStudio);
                    } catch (final NimbleException e) {
                        logger.log(String.format("%s [%s] exception during creation", ResourceModel.TYPE_NAME,
                            studioId));
                        throw Translator.translateToCfnException(e);
                    }

                    if (StudioState.CREATE_IN_PROGRESS.equals(getStudioResponse.studio().state())) {
                        logger.log(String.format("%s [%s] is in state CREATE_IN_PROGRESS, creation in progress", ResourceModel.TYPE_NAME,
                                studioId));
                        return false;
                    }
                    if (StudioState.READY.equals(getStudioResponse.studio().state())) {
                        logger.log(String.format("%s [%s] is in state READY, creation succeeded", ResourceModel.TYPE_NAME,
                                studioId));
                        return true;
                    }

                    logger.log(String.format("%s [%s] is in error state %s, creation failed", ResourceModel.TYPE_NAME,
                            studioId, getStudioResponse.studio().state()));
                    throw new CfnGeneralServiceException(String.format("Unexpected state %s: %s - %s",
                            getStudioResponse.studio().stateAsString(),
                            getStudioResponse.studio().statusCodeAsString(),
                            getStudioResponse.studio().statusMessage()));
                })
                .progress())
            .then((r) -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }
}
