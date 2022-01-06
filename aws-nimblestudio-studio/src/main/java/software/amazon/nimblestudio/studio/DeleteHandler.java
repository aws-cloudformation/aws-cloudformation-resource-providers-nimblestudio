package software.amazon.nimblestudio.studio;

import software.amazon.awssdk.services.nimble.NimbleClient;
import software.amazon.awssdk.services.nimble.model.DeleteStudioRequest;
import software.amazon.awssdk.services.nimble.model.DeleteStudioResponse;
import software.amazon.awssdk.services.nimble.model.GetStudioRequest;
import software.amazon.awssdk.services.nimble.model.GetStudioResponse;
import software.amazon.awssdk.services.nimble.model.Studio;
import software.amazon.awssdk.services.nimble.model.StudioState;
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

    private Logger logger;

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<NimbleClient> proxyClient,
        final Logger logger) {

        this.logger = logger;

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
            .then(progress -> proxy
                .initiate(
                    "AWS-NimbleStudio-Studio::Delete",
                    proxyClient,
                    progress.getResourceModel(),
                    progress.getCallbackContext())
                .translateToServiceRequest(model -> DeleteStudioRequest.builder()
                    .clientToken(request.getClientRequestToken())
                    .studioId(model.getStudioId())
                    .build())
                .makeServiceCall((awsRequest, client) -> {
                    final String studioId = awsRequest.studioId();
                    final Studio studio = GetStudio(studioId, client);
                    final StudioState studioState = studio.state();

                    // If the studio is already DELETING, don't send another DELETE request (which will fail)
                    if(StudioState.DELETE_IN_PROGRESS.equals(studioState)) {
                        return null;
                    }
                    if (StudioState.DELETED.equals(studioState) || StudioState.CREATE_FAILED.equals(studioState)) {
                        logger.log(String.format("%s [%s] is already in state %s, deletion failed",
                            ResourceModel.TYPE_NAME, studioId, studioState.toString()));
                        throw new CfnNotFoundException(ResourceModel.TYPE_NAME, studioId);
                    }

                    try {
                        final NimbleClient studioClient = client.client();
                        final DeleteStudioResponse deleteStudioResponse = client
                            .injectCredentialsAndInvokeV2(awsRequest, studioClient::deleteStudio);
                        logger.log(String.format("%s [%s] DELETE requested successfully", ResourceModel.TYPE_NAME, studioId));
                        return deleteStudioResponse;
                    } catch (final NimbleException e) {
                        logger.log(String.format("%s [%s] exception during deletion", ResourceModel.TYPE_NAME,
                            studioId));
                        throw Translator.translateToCfnException(e);
                    }
                })
                .stabilize((awsRequest, awsResponse, client, model, context) -> {
                    final String studioId = awsRequest.studioId();
                    final Studio studio = GetStudio(studioId, client);
                    final StudioState studioState = studio.state();

                    if (StudioState.DELETED.equals(studioState) || StudioState.CREATE_FAILED.equals(studioState)) {
                        logger.log(String.format("%s [%s] is in state %s, deletion succeeded",
                            ResourceModel.TYPE_NAME, studioId, studioState));
                        return true;
                    }
                    if (StudioState.DELETE_IN_PROGRESS.equals(studioState)) {
                        logger.log(String.format("%s [%s] is in state %s, deletion in progress",
                            ResourceModel.TYPE_NAME, studioId, studioState));
                        return false;
                    }

                    logger.log(String.format("%s [%s] is in unexpected state %s, deletion failed",
	                    ResourceModel.TYPE_NAME, studioId, studioState));

                    throw new CfnGeneralServiceException(String.format("Unexpected state %s: %s - %s",
                            studio.stateAsString(),
                            studio.statusCodeAsString(), 
                            studio.statusMessage()));
                })
                .done(awsResponse ->
                    ProgressEvent.<ResourceModel, CallbackContext>builder().status(OperationStatus.SUCCESS).build()));
    }

    private Studio GetStudio(final String studioId, final ProxyClient<NimbleClient> proxyClient) {
        try {
            GetStudioResponse getStudioResponse = proxyClient.injectCredentialsAndInvokeV2(
                    GetStudioRequest.builder().studioId(studioId).build(),
                    proxyClient.client()::getStudio);
            return getStudioResponse.studio();
        } catch (final NimbleException e) {
            logger.log(String.format("%s [%s] Exception during deletion", ResourceModel.TYPE_NAME, studioId));
            throw Translator.translateToCfnException(e);
        }
    }
}
