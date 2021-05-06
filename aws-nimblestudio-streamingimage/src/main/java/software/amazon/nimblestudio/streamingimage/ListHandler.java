package software.amazon.nimblestudio.streamingimage;

import software.amazon.awssdk.services.nimble.NimbleClient;
import software.amazon.awssdk.services.nimble.model.ListStreamingImagesRequest;
import software.amazon.awssdk.services.nimble.model.ListStreamingImagesResponse;
import software.amazon.awssdk.services.nimble.model.NimbleException;
import software.amazon.awssdk.services.nimble.model.StreamingImage;
import software.amazon.awssdk.services.nimble.model.StreamingImageState;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class ListHandler extends BaseHandlerStd {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<NimbleClient> proxyClient,
        final Logger logger) {

        final ListStreamingImagesRequest listStreamingImagesRequest =
            ListStreamingImagesRequest.builder()
                .nextToken(request.getNextToken())
                .studioId(request.getDesiredResourceState().getStudioId())
                .build();

        try {
            final ListStreamingImagesResponse listStreamingImagesResponse = proxyClient.injectCredentialsAndInvokeV2(
                    listStreamingImagesRequest, proxyClient.client()::listStreamingImages);

            final List<StreamingImage> streamingImages = listStreamingImagesResponse.hasStreamingImages() ?
                listStreamingImagesResponse.streamingImages() :
                new ArrayList<>();

            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .status(OperationStatus.SUCCESS)
                .resourceModels(streamingImages.stream()
                    .filter(streamingImage -> !StreamingImageState.DELETED.equals(streamingImage.state()))
                    .map(streamingImage -> {
                        ResourceModel.ResourceModelBuilder modelBuilder = ResourceModel.builder()
                            .studioId(request.getDesiredResourceState().getStudioId())
                            .streamingImageId(streamingImage.streamingImageId())
                            .ec2ImageId(streamingImage.ec2ImageId())
                            .name(streamingImage.name())
                            .description(streamingImage.description())
                            .owner(streamingImage.owner())
                            .eulaIds(streamingImage.eulaIds())
                            .platform(streamingImage.platform())
                            .tags(streamingImage.tags());

                        if(streamingImage.encryptionConfiguration() != null) {
                            modelBuilder.encryptionConfiguration(StreamingImageEncryptionConfiguration.builder()
                                .keyType(streamingImage.encryptionConfiguration().keyTypeAsString())
                                .keyArn(streamingImage.encryptionConfiguration().keyArn())
                                .build());
                        }

                        return modelBuilder.build();
                    })
                .collect(toList()))
                .nextToken(listStreamingImagesResponse.nextToken())
                .build();
        } catch (final NimbleException e) {
            logger.log(String.format("Exception during list: %s.", ResourceModel.TYPE_NAME));
            throw ExceptionTranslator.translateToCfnException(e);
        }
    }
}
