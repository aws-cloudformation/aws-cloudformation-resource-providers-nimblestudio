package software.amazon.nimblestudio.streamingimage;

import software.amazon.awssdk.services.nimble.NimbleClient;
import software.amazon.awssdk.services.nimble.model.GetStreamingImageRequest;
import software.amazon.awssdk.services.nimble.model.GetStreamingImageResponse;
import software.amazon.awssdk.services.nimble.model.CreateStreamingImageRequest;
import software.amazon.awssdk.services.nimble.model.CreateStreamingImageResponse;
import software.amazon.awssdk.services.nimble.model.GetStudioRequest;
import software.amazon.awssdk.services.nimble.model.GetStudioResponse;
import software.amazon.awssdk.services.nimble.model.NimbleException;
import software.amazon.awssdk.services.nimble.model.StreamingImageState;
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
                    "AWS-NimbleStudio-StreamingImage::Create",
                    proxyClient,
                    progress.getResourceModel(),
                    progress.getCallbackContext()
                )
                .translateToServiceRequest((model) -> CreateStreamingImageRequest.builder()
                    .clientToken(request.getClientRequestToken())
                    .studioId(model.getStudioId())
                    .ec2ImageId(model.getEc2ImageId())
                    .name(model.getName())
                    .description(model.getDescription())
                    .tags(model.getTags())
                    .build())
                .makeServiceCall((createStreamingImageRequest, client) -> {
                    try {
                        final NimbleClient studioClient = client.client();
                        final CreateStreamingImageResponse createStreamingImageResponse = client
                            .injectCredentialsAndInvokeV2(createStreamingImageRequest,
                                studioClient::createStreamingImage);

                        logger.log(String.format("%s [%s] creation in progress", ResourceModel.TYPE_NAME,
                            createStreamingImageResponse.streamingImage().streamingImageId()));

                        return createStreamingImageResponse;
                    } catch (final NimbleException e) {
                        logger.log(String.format(
                            "Exception during creation: %s.", ResourceModel.TYPE_NAME));
                        throw ExceptionTranslator.translateToCfnException(e);
                    }
                })
                .stabilize((awsRequest, awsResponse, client, model, context) -> {
                    final String streamingImageId = awsResponse.streamingImage().streamingImageId();
                    model.setStreamingImageId(streamingImageId);

                    GetStreamingImageRequest getStreamingImageRequest = GetStreamingImageRequest.builder()
                        .studioId(awsRequest.studioId())
                        .streamingImageId(streamingImageId)
                        .build();
                    GetStreamingImageResponse getStreamingImageResponse;

                    try {
                        getStreamingImageResponse = client.injectCredentialsAndInvokeV2(
                                getStreamingImageRequest, client.client()::getStreamingImage);
                    } catch (final NimbleException e) {
                        logger.log(String.format("%s [%s] exception during creation", ResourceModel.TYPE_NAME,
                            streamingImageId));
                        throw ExceptionTranslator.translateToCfnException(e);
                    }

                    if (StreamingImageState.CREATE_IN_PROGRESS.equals(getStreamingImageResponse.streamingImage().state())) {
                        logger.log(String.format("%s [%s] is in state %s, creation in progress", ResourceModel.TYPE_NAME, streamingImageId,
                            getStreamingImageResponse.streamingImage().stateAsString()));
                        return false;
                    }
                    if (StreamingImageState.READY.equals(getStreamingImageResponse.streamingImage().state())) {
                        logger.log(String.format("%s [%s] is in state READY, creation succeeded",
                            ResourceModel.TYPE_NAME, streamingImageId));
                        return true;
                    }

                    logger.log(String.format("%s [%s] is in error state %s, creation failed", ResourceModel.TYPE_NAME,
                        streamingImageId, getStreamingImageResponse.streamingImage().state()));
                    throw new CfnGeneralServiceException(String.format("%s - %s",
                        getStreamingImageResponse.streamingImage().statusCodeAsString(),
                        getStreamingImageResponse.streamingImage().statusMessage()));
                })
                .progress()
            )
            .then((progress) -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }
}
