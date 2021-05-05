package software.amazon.nimblestudio.studio;

import software.amazon.awssdk.services.nimble.NimbleClient;
import software.amazon.awssdk.services.nimble.model.ListStudiosRequest;
import software.amazon.awssdk.services.nimble.model.ListStudiosResponse;
import software.amazon.awssdk.services.nimble.model.NimbleException;
import software.amazon.awssdk.services.nimble.model.Studio;
import software.amazon.awssdk.services.nimble.model.StudioState;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
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

        final NimbleClient studioClient = proxyClient.client();
        final ListStudiosRequest listStudiosRequest = ListStudiosRequest.builder()
                .nextToken(request.getNextToken())
                .build();

        ListStudiosResponse listStudiosResponse ;
        try {
            listStudiosResponse = proxyClient.injectCredentialsAndInvokeV2(listStudiosRequest, studioClient::listStudios);
        } catch (final NimbleException e) {
            logger.log(String.format("Exception during list: %s.", ResourceModel.TYPE_NAME));
            throw Translator.translateToCfnException(e);
        }

        final List<Studio> studios = listStudiosResponse.hasStudios() ?
                listStudiosResponse.studios() :
                new ArrayList<>();

        final List<ResourceModel> models = studios.stream()
                .filter(studio ->
                    !StudioState.DELETED.equals(studio.state()) &&
                    !StudioState.CREATE_FAILED.equals(studio.state()))
                .map(Translator::toModel)
                .collect(toList());

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModels(models)
                .nextToken(listStudiosResponse.nextToken())
                .status(OperationStatus.SUCCESS).build();
    }
}
