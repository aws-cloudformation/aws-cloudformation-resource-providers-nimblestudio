package software.amazon.nimblestudio.streamingimage;

import software.amazon.awssdk.services.nimble.NimbleClient;
import software.amazon.awssdk.services.nimble.model.DeleteStreamingImageRequest;
import software.amazon.awssdk.services.nimble.model.DeleteStreamingImageResponse;
import software.amazon.awssdk.services.nimble.model.GetStreamingImageRequest;
import software.amazon.awssdk.services.nimble.model.GetStreamingImageResponse;
import software.amazon.awssdk.services.nimble.model.StreamingImageState;
import software.amazon.awssdk.services.nimble.model.NimbleException;
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
                    "AWS-NimbleStudio-StreamingImage::Delete",
                    proxyClient,
                    progress.getResourceModel(),
                    progress.getCallbackContext())
                .translateToServiceRequest((model) -> DeleteStreamingImageRequest.builder()
                    .clientToken(request.getClientRequestToken())
                    .studioId(model.getStudioId())
                    .streamingImageId(model.getStreamingImageId())
                    .build())
                .makeServiceCall(((deleteStreamingImageRequest, client) -> {
                    final String streamingImageId = deleteStreamingImageRequest.streamingImageId();

                    try {
                        final GetStreamingImageRequest getStreamingImageRequest = GetStreamingImageRequest.builder()
                                .streamingImageId(streamingImageId)
                                .studioId(deleteStreamingImageRequest.studioId())
                                .build();
                        final GetStreamingImageResponse getStreamingImageResponse = client
                                .injectCredentialsAndInvokeV2(getStreamingImageRequest, client.client()::getStreamingImage);
                        if (StreamingImageState.DELETED.equals(getStreamingImageResponse.streamingImage().state())) {
                            logger.log(String.format("%s [%s] is already in state DELETED, deletion failed",
                                    ResourceModel.TYPE_NAME, streamingImageId));
                            throw new CfnNotFoundException(ResourceModel.TYPE_NAME, streamingImageId);
                        }

                        final DeleteStreamingImageResponse deleteStreamingImageResponse = client
                            .injectCredentialsAndInvokeV2(deleteStreamingImageRequest,
                                    client.client()::deleteStreamingImage);

                        logger.log(String.format("%s [%s] deletion in progress", ResourceModel.TYPE_NAME,
                            deleteStreamingImageRequest.streamingImageId()));

                        return deleteStreamingImageResponse;
                    } catch (final NimbleException e) {
                        logger.log(String.format("%s [%s] exception during deletion", ResourceModel.TYPE_NAME,
                                deleteStreamingImageRequest.streamingImageId()));
                        throw ExceptionTranslator.translateToCfnException(e);
                    }
                }))
                .stabilize((awsRequest, awsResponse, client, model, context) -> {
                    final String streamingImageId = awsRequest.streamingImageId();
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
                        logger.log(String.format("%s [%s] Exception during deletion", ResourceModel.TYPE_NAME,
                            streamingImageId));
                        throw ExceptionTranslator.translateToCfnException(e);
                    }

                    if (StreamingImageState.DELETE_IN_PROGRESS.equals(getStreamingImageResponse.streamingImage().state())) {
                        logger.log(String.format("%s [%s] is in state DELETE_IN_PROGRESS, deletion in progress",
                            ResourceModel.TYPE_NAME, streamingImageId));
                        return false;
                    }
                    if (StreamingImageState.DELETED.equals(getStreamingImageResponse.streamingImage().state())) {
                        logger.log(String.format("%s [%s] is in state DELETED, deletion succeeded",
                            ResourceModel.TYPE_NAME, streamingImageId));
                        return true;
                    }

                    logger.log(String.format("%s [%s] is in error state %s, deletion failed", ResourceModel.TYPE_NAME,
                        streamingImageId, getStreamingImageResponse.streamingImage().state()));
                    throw new CfnGeneralServiceException(String.format("Unexpected state %s: %s - %s",
                        getStreamingImageResponse.streamingImage().stateAsString(),
                        getStreamingImageResponse.streamingImage().statusCodeAsString(),
                        getStreamingImageResponse.streamingImage().statusMessage()));
                })
                .done(awsResponse -> ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .status(OperationStatus.SUCCESS)
                    .build()));
    }
}
