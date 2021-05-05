package software.amazon.nimblestudio.studio;

import software.amazon.awssdk.services.nimble.NimbleClient;
import software.amazon.awssdk.services.nimble.model.GetStudioRequest;
import software.amazon.awssdk.services.nimble.model.GetStudioResponse;
import software.amazon.awssdk.services.nimble.model.NimbleException;
import software.amazon.awssdk.services.nimble.model.StudioState;
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

        return proxy.initiate(
                "AWS-NimbleStudio-Studio::Read",
                proxyClient,
                request.getDesiredResourceState(),
                callbackContext)
            .translateToServiceRequest(model -> GetStudioRequest.builder().studioId(model.getStudioId()).build())
            .makeServiceCall((awsRequest, client) -> {
                try {
                    final NimbleClient studioClient = proxyClient.client();
                    final GetStudioResponse getStudioResponse = proxyClient
                            .injectCredentialsAndInvokeV2(awsRequest, studioClient::getStudio);
                    logger.log(String.format("%s [%s] read requested successfully", ResourceModel.TYPE_NAME,
                            getStudioResponse.studio().studioId()));

                    // If a resource was deleted, read request needs to throw a NotFoundException
                    if (StudioState.DELETED.equals(getStudioResponse.studio().state()) ||
                        StudioState.CREATE_FAILED.equals(getStudioResponse.studio().state())) {
                        logger.log(String.format("%s [%s] is in state %s, unable to get resource",
                            ResourceModel.TYPE_NAME,
                            getStudioResponse.studio().studioId(),
                            getStudioResponse.studio().stateAsString()));
                        throw new CfnNotFoundException(ResourceModel.TYPE_NAME, getStudioResponse.studio().studioName());
                    }

                    return getStudioResponse;
                } catch (final NimbleException e) {
                    logger.log(String.format("Exception during read: %s for id: %s.", ResourceModel.TYPE_NAME,
                            awsRequest.studioId()));
                    throw Translator.translateToCfnException(e);
                }
            })
            .done(awsResponse -> ProgressEvent.defaultSuccessHandler(Translator.toModel(awsResponse.studio())));
    }
}
