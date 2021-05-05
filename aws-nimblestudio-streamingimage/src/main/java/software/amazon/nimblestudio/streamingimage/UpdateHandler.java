package software.amazon.nimblestudio.streamingimage;

import software.amazon.awssdk.services.nimble.NimbleClient;
import software.amazon.awssdk.services.nimble.model.GetStreamingImageRequest;
import software.amazon.awssdk.services.nimble.model.GetStreamingImageResponse;
import software.amazon.awssdk.services.nimble.model.StreamingImageState;
import software.amazon.awssdk.services.nimble.model.UpdateStreamingImageRequest;
import software.amazon.awssdk.services.nimble.model.UpdateStreamingImageResponse;
import software.amazon.awssdk.services.nimble.model.NimbleException;
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
                    "AWS-NimbleStudio-StreamingImage::Update",
                    proxyClient,
                    progress.getResourceModel(),
                    progress.getCallbackContext())
                .translateToServiceRequest((model) -> {
                    final UpdateStreamingImageRequest.Builder updateStreamingImageBuilder =
                        UpdateStreamingImageRequest.builder()
                            .clientToken(request.getClientRequestToken())
                            .studioId(model.getStudioId())
                            .streamingImageId(model.getStreamingImageId());

                    if (!StringUtils.isEmpty(model.getName())) {
                        updateStreamingImageBuilder.name(model.getName());
                    }

                    if (!StringUtils.isEmpty(model.getDescription())) {
                        updateStreamingImageBuilder.description(model.getDescription());
                    }

                    return updateStreamingImageBuilder.build();
                })
                .makeServiceCall((updateStreamingImageRequest, client) -> {
                    final String streamingImageId = updateStreamingImageRequest.streamingImageId();

                    try {
                        final GetStreamingImageRequest getStreamingImageRequest = GetStreamingImageRequest.builder()
                                .streamingImageId(streamingImageId)
                                .studioId(updateStreamingImageRequest.studioId())
                                .build();
                        final GetStreamingImageResponse getStreamingImageResponse = client
                                .injectCredentialsAndInvokeV2(getStreamingImageRequest, client.client()::getStreamingImage);
                        if (StreamingImageState.DELETED.equals(getStreamingImageResponse.streamingImage().state())) {
                            logger.log(String.format("%s [%s] is in state DELETED, update failed", ResourceModel.TYPE_NAME,
                                    streamingImageId));
                            throw new CfnNotFoundException(ResourceModel.TYPE_NAME, streamingImageId);
                        }

                        final UpdateStreamingImageResponse updateStreamingImageResponse = client
                            .injectCredentialsAndInvokeV2(updateStreamingImageRequest,
                                    client.client()::updateStreamingImage);

                        logger.log(String.format("%s [%s] update requested successfully", ResourceModel.TYPE_NAME,
                            streamingImageId));

                        return updateStreamingImageResponse;
                    } catch (final NimbleException e) {
                        logger.log(String.format("%s [%s] Exception during update", ResourceModel.TYPE_NAME,
                            streamingImageId));
                        throw ExceptionTranslator.translateToCfnException(e);
                    }
                })
                /*
                 * Even though CREATE and DELETE requests are async and require stabilization, UPDATE requests are
                 * synchronous and do not require stabilization.
                 */
                .progress()
            )
            .then((r) -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }
}
