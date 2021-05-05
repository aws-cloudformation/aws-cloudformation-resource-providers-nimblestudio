package software.amazon.nimblestudio.studiocomponent;

import software.amazon.awssdk.services.nimble.NimbleClient;
import software.amazon.awssdk.services.nimble.model.ListStudioComponentsRequest;
import software.amazon.awssdk.services.nimble.model.ListStudioComponentsResponse;
import software.amazon.awssdk.services.nimble.model.NimbleException;
import software.amazon.awssdk.services.nimble.model.StudioComponentState;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import software.amazon.awssdk.services.nimble.model.StudioComponent;

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

        final ListStudioComponentsRequest listStudiosRequest = ListStudioComponentsRequest.builder()
                .nextToken(request.getNextToken())
                .studioId(request.getDesiredResourceState().getStudioId())
                .build();

        try {
            final ListStudioComponentsResponse listStudioComponentsResponse =
                    proxyClient.injectCredentialsAndInvokeV2(listStudiosRequest,
                    proxyClient.client()::listStudioComponents);

            final List<StudioComponent> studioComponents =
                    listStudioComponentsResponse.hasStudioComponents() ?
                        listStudioComponentsResponse.studioComponents() :
                        new ArrayList<>();

            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModels(studioComponents.stream()
                    .filter(studioComponent ->
                        !StudioComponentState.DELETED.equals(studioComponent.state()) &&
                        !StudioComponentState.CREATE_FAILED.equals(studioComponent.state()))
                    .map(studioComponent -> ResourceModel.builder()
                        .configuration(Translator.toModelStudioComponentConfiguration(studioComponent.configuration()))
                        .description(studioComponent.description())
                        .initializationScripts(Translator.toModelStudioComponentInitializationScripts(studioComponent))
                        .name(studioComponent.name())
                        .scriptParameters(new ArrayList(studioComponent.scriptParameters()))
                        .ec2SecurityGroupIds(studioComponent.ec2SecurityGroupIds())
                        .studioComponentId(studioComponent.studioComponentId())
                        .studioId(request.getDesiredResourceState().getStudioId())
                        .subtype(studioComponent.subtype().toString())
                        .type(studioComponent.type().toString())
                        .tags(studioComponent.tags())
                        .build())
                    .collect(toList()))
                .nextToken(listStudioComponentsResponse.nextToken())
                .status(OperationStatus.SUCCESS)
                .build();
        } catch (final NimbleException e) {
            logger.log(String.format("Exception during list: %s.", ResourceModel.TYPE_NAME));
            throw ExceptionTranslator.translateToCfnException(e);
        }
    }
}
