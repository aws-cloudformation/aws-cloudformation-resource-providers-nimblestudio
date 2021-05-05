package software.amazon.nimblestudio.streamingimage;

import software.amazon.awssdk.services.nimble.NimbleClient;
import software.amazon.awssdk.services.nimble.model.GetStreamingImageRequest;
import software.amazon.awssdk.services.nimble.model.GetStreamingImageResponse;
import software.amazon.awssdk.services.nimble.model.NimbleException;
import software.amazon.awssdk.services.nimble.model.StreamingImageState;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class ReadHandler extends BaseHandlerStd {

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<NimbleClient> proxyClient,
        final Logger logger) {

        return proxy.initiate(
                "AWS-NimbleStudio-StreamingImage::Read",
                proxyClient,
                request.getDesiredResourceState(),
                callbackContext)
            .translateToServiceRequest((model) -> GetStreamingImageRequest.builder()
                .studioId(model.getStudioId())
                .streamingImageId(model.getStreamingImageId())
                .build()
            )
            .makeServiceCall((getStreamingImageRequest, client) -> {
                try {
                    final NimbleClient studioClient = client.client();
                    final GetStreamingImageResponse getResponse = client
                        .injectCredentialsAndInvokeV2(getStreamingImageRequest, studioClient::getStreamingImage);

                    if (StreamingImageState.DELETED.equals(getResponse.streamingImage().state())) {
                        // If a resource was deleted, read request needs to throw a NotFoundException
                        logger.log(String.format("%s [%s] is in state DELETED, unable to get resource", ResourceModel.TYPE_NAME,
                            getResponse.streamingImage().streamingImageId()));
                        throw new CfnNotFoundException(ResourceModel.TYPE_NAME, getResponse.streamingImage().streamingImageId());
                    }

                    logger.log(String.format("%s [%s] read successful", ResourceModel.TYPE_NAME,
                        getResponse.streamingImage().streamingImageId()));

                    return getResponse;
                } catch (final NimbleException e) {
                    logger.log(String.format("%s [%s] exception during read", ResourceModel.TYPE_NAME,
                        getStreamingImageRequest.streamingImageId()));
                    throw ExceptionTranslator.translateToCfnException(e);
                }
            })
            .done((awsResponse) -> {
                ResourceModel.ResourceModelBuilder modelBuilder = ResourceModel.builder()
                    .studioId(request.getDesiredResourceState().getStudioId())
                    .streamingImageId(awsResponse.streamingImage().streamingImageId())
                    .ec2ImageId(awsResponse.streamingImage().ec2ImageId())
                    .name(awsResponse.streamingImage().name())
                    .description(awsResponse.streamingImage().description())
                    .owner(awsResponse.streamingImage().owner())
                    .eulaIds(awsResponse.streamingImage().eulaIds())
                    .platform(awsResponse.streamingImage().platform())
                    .tags(awsResponse.streamingImage().tags());

                if(awsResponse.streamingImage().encryptionConfiguration() != null) {
                    modelBuilder.encryptionConfiguration(StreamingImageEncryptionConfiguration.builder()
                        .keyType(awsResponse.streamingImage().encryptionConfiguration().keyTypeAsString())
                        .keyArn(awsResponse.streamingImage().encryptionConfiguration().keyArn())
                        .build());
                }

                return ProgressEvent.defaultSuccessHandler(modelBuilder.build());
            });
    }
}
