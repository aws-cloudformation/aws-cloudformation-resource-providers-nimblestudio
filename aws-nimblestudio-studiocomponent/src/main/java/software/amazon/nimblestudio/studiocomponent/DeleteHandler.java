package software.amazon.nimblestudio.studiocomponent;

import software.amazon.awssdk.services.nimble.NimbleClient;
import software.amazon.awssdk.services.nimble.model.DeleteStudioComponentResponse;
import software.amazon.awssdk.services.nimble.model.DeleteStudioComponentRequest;
import software.amazon.awssdk.services.nimble.model.GetStudioComponentRequest;
import software.amazon.awssdk.services.nimble.model.GetStudioComponentResponse;
import software.amazon.awssdk.services.nimble.model.StudioComponentState;
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
                    "AWS-NimbleStudio-StudioComponent::Delete",
                    proxyClient,
                    progress.getResourceModel(),
                    progress.getCallbackContext())
                .translateToServiceRequest(model -> DeleteStudioComponentRequest.builder()
                    .clientToken(request.getClientRequestToken())
                    .studioId(model.getStudioId())
                    .studioComponentId(model.getStudioComponentId())
                    .build())
                .makeServiceCall((deleteStudioComponentRequest, client) -> {
                    final String studioComponentId = deleteStudioComponentRequest.studioComponentId();

                    try {
                        final GetStudioComponentRequest getStudioComponentRequest = GetStudioComponentRequest.builder()
                                .studioComponentId(studioComponentId)
                                .studioId(deleteStudioComponentRequest.studioId())
                                .build();
                        final GetStudioComponentResponse getStudioComponentResponse = client
                                .injectCredentialsAndInvokeV2(getStudioComponentRequest, client.client()::getStudioComponent);
                        final StudioComponentState state = getStudioComponentResponse.studioComponent().state();
                        if (StudioComponentState.DELETE_IN_PROGRESS.equals(state)) {
                            logger.log(String.format("%s [%s] is already in state DELETE_IN_PROGRESS",
                                    ResourceModel.TYPE_NAME, studioComponentId));
                            return null;
                        }
                        if (StudioComponentState.DELETED.equals(state) ||
                            StudioComponentState.CREATE_FAILED.equals(state)) {
                            logger.log(String.format("%s [%s] is already in state %s, deletion failed",
                                ResourceModel.TYPE_NAME, studioComponentId, state.toString()));
                            throw new CfnNotFoundException(ResourceModel.TYPE_NAME, studioComponentId);
                        }

                        final DeleteStudioComponentResponse deleteStudioComponentResponse = client
                                .injectCredentialsAndInvokeV2(deleteStudioComponentRequest,
                                        client.client()::deleteStudioComponent);

                        logger.log(String.format("%s [%s] deletion in progress", ResourceModel.TYPE_NAME,
                            deleteStudioComponentRequest.studioComponentId()));

                        return deleteStudioComponentResponse;
                    } catch (final NimbleException e) {
                        logger.log(String.format("%s [%s] Exception during deletion", ResourceModel.TYPE_NAME,
                            deleteStudioComponentRequest.studioComponentId()));
                        throw ExceptionTranslator.translateToCfnException(e);
                    }
                })
                .stabilize((awsRequest, awsResponse, client, model, context) -> {
                    final String studioComponentId = awsRequest.studioComponentId();

                    GetStudioComponentRequest getStudioComponentRequest = GetStudioComponentRequest.builder()
                        .studioId(awsRequest.studioId())
                        .studioComponentId(studioComponentId)
                        .build();
                    GetStudioComponentResponse getStudioComponentResponse;
                    StudioComponentState state;

                    try {
                        getStudioComponentResponse = client.injectCredentialsAndInvokeV2(
                            getStudioComponentRequest, client.client()::getStudioComponent);
                        state = getStudioComponentResponse.studioComponent().state();
                    } catch (final NimbleException e) {
                        logger.log(String.format("%s [%s] Exception during deletion", ResourceModel.TYPE_NAME,
                            studioComponentId));
                        throw ExceptionTranslator.translateToCfnException(e);
                    }

                    if (StudioComponentState.DELETE_IN_PROGRESS.equals(state)) {
                        logger.log(String.format("%s [%s] is in state DELETE_IN_PROGRESS, deletion in progress",
                            ResourceModel.TYPE_NAME, studioComponentId));
                        return false;
                    }
                    if (StudioComponentState.DELETED.equals(state) || StudioComponentState.CREATE_FAILED.equals(state)) {
                        logger.log(String.format("%s [%s] is in state %s, deletion succeeded",
                            ResourceModel.TYPE_NAME, studioComponentId, state.toString()));
                        return true;
                    }

                    logger.log(String.format("%s [%s] is in error state %s, deletion failed", ResourceModel.TYPE_NAME,
                        studioComponentId, getStudioComponentResponse.studioComponent().state()));
                    throw new CfnGeneralServiceException(String.format("Unexpected state %s: %s - %s",
                        getStudioComponentResponse.studioComponent().stateAsString(),
                        getStudioComponentResponse.studioComponent().statusCodeAsString(),
                        getStudioComponentResponse.studioComponent().statusMessage()));
                })
                .done(awsResponse -> ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .status(OperationStatus.SUCCESS)
                    .build()));
    }
}
