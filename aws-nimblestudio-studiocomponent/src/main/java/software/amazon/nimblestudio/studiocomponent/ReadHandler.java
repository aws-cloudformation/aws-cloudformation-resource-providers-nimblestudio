package software.amazon.nimblestudio.studiocomponent;

import com.amazonaws.util.StringUtils;
import software.amazon.awssdk.services.nimble.NimbleClient;
import software.amazon.awssdk.services.nimble.model.GetStudioComponentRequest;
import software.amazon.awssdk.services.nimble.model.GetStudioComponentResponse;
import software.amazon.awssdk.services.nimble.model.NimbleException;
import software.amazon.awssdk.services.nimble.model.StudioComponentState;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import static java.util.stream.Collectors.toList;

public class ReadHandler extends BaseHandlerStd {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<NimbleClient> proxyClient,
        final Logger logger) {

        return proxy.initiate(
                "AWS-NimbleStudio-StudioComponent::Read",
                proxyClient,
                request.getDesiredResourceState(),
                callbackContext)
            .translateToServiceRequest(model -> GetStudioComponentRequest.builder()
                .studioId(model.getStudioId())
                .studioComponentId(model.getStudioComponentId())
                .build())
            .makeServiceCall((awsRequest, client) -> {
                try {
                    final GetStudioComponentResponse getStudioComponentResponse =
                            client.injectCredentialsAndInvokeV2(awsRequest,
                        client.client()::getStudioComponent);
                    final StudioComponentState state = getStudioComponentResponse.studioComponent().state();

                    if (StudioComponentState.DELETED.equals(state) || StudioComponentState.CREATE_FAILED.equals(state)) {
                        logger.log(String.format("%s [%s] is in state %s, unable to get resource",
                            ResourceModel.TYPE_NAME,
                            getStudioComponentResponse.studioComponent().studioComponentId(),
                            state.toString()));
                        throw new CfnNotFoundException(ResourceModel.TYPE_NAME,
                            getStudioComponentResponse.studioComponent().studioComponentId());
                    }

                    logger.log(String.format("%s [%s] read requested successfully", ResourceModel.TYPE_NAME,
                        getStudioComponentResponse.studioComponent().studioComponentId()));

                    return getStudioComponentResponse;
                } catch (final NimbleException e) {
                    logger.log(String.format("%s [%s] exception during read", ResourceModel.TYPE_NAME,
                        awsRequest.studioComponentId()));
                    throw ExceptionTranslator.translateToCfnException(e);
                }
            })
            .done(awsResponse -> {
                final ResourceModel.ResourceModelBuilder modelBuilder = ResourceModel.builder()
                    .studioId(request.getDesiredResourceState().getStudioId())
                    .configuration(Translator.toModelStudioComponentConfiguration(
                        awsResponse.studioComponent().configuration()))
                    .description(awsResponse.studioComponent().description())
                    .scriptParameters(awsResponse.studioComponent().scriptParameters().stream()
                        .map(sp -> ScriptParameterKeyValue.builder()
                            .key(sp.key())
                            .value(sp.value())
                            .build())
                    .collect(toList()))
                    .ec2SecurityGroupIds(awsResponse.studioComponent().ec2SecurityGroupIds())
                    .studioComponentId(awsResponse.studioComponent().studioComponentId())
                    .subtype(awsResponse.studioComponent().subtype().toString())
                    .type(awsResponse.studioComponent().type().toString())
                    .tags(awsResponse.studioComponent().tags());

                if (!StringUtils.isNullOrEmpty(awsResponse.studioComponent().name())) {
                    modelBuilder.name(awsResponse.studioComponent().name());
                }

                if (awsResponse.studioComponent().hasInitializationScripts()) {
                    modelBuilder.initializationScripts(
                        Translator.toModelStudioComponentInitializationScripts(awsResponse.studioComponent()));
                }

                if (!StringUtils.isNullOrEmpty(awsResponse.studioComponent().runtimeRoleArn())) {
                    modelBuilder.runtimeRoleArn(awsResponse.studioComponent().runtimeRoleArn());
                }

                if (!StringUtils.isNullOrEmpty(awsResponse.studioComponent().secureInitializationRoleArn())) {
                    modelBuilder.secureInitializationRoleArn(awsResponse.studioComponent().secureInitializationRoleArn());
                }

                return ProgressEvent.defaultSuccessHandler(modelBuilder.build());
            });
    }
}
