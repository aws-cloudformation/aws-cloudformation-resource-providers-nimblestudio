package software.amazon.nimblestudio.studio;

import software.amazon.awssdk.services.nimble.NimbleClient;
import software.amazon.awssdk.services.nimble.model.GetStudioRequest;
import software.amazon.awssdk.services.nimble.model.GetStudioResponse;
import software.amazon.awssdk.services.nimble.model.Studio;
import software.amazon.awssdk.services.nimble.model.UpdateStudioRequest;
import software.amazon.awssdk.services.nimble.model.UpdateStudioResponse;
import software.amazon.awssdk.services.nimble.model.StudioState;
import software.amazon.awssdk.services.nimble.model.NimbleException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import software.amazon.awssdk.utils.StringUtils;

public class UpdateHandler extends BaseHandlerStd {

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
                    "AWS-NimbleStudio-Studio::Update",
                    proxyClient,
                    progress.getResourceModel(),
                    progress.getCallbackContext())
                .translateToServiceRequest(model -> {
                    final UpdateStudioRequest.Builder updateStudioBuilder = UpdateStudioRequest.builder()
                        .clientToken(request.getClientRequestToken())
                        .studioId(model.getStudioId());

                    if (!StringUtils.isEmpty(model.getDisplayName())) {
                        updateStudioBuilder.displayName(model.getDisplayName());
                    }

                    if (!StringUtils.isEmpty(model.getAdminRoleArn())) {
                        updateStudioBuilder.adminRoleArn(model.getAdminRoleArn());
                    }

                    if (!StringUtils.isEmpty(model.getUserRoleArn())) {
                        updateStudioBuilder.userRoleArn(model.getUserRoleArn());
                    }

                    return updateStudioBuilder.build();
                })
                .makeServiceCall((awsRequest, client) -> {
                    final NimbleClient studioClient = client.client();
                    final String studioId = awsRequest.studioId();

                    final StudioState studioState = getStudio(client, studioId).state();
                    if (StudioState.DELETED.equals(studioState) || StudioState.CREATE_FAILED.equals(studioState)) {
                        logger.log(String.format("%s [%s] is in state %s, update failed", ResourceModel.TYPE_NAME,
                            studioId, studioState));
                        throw new CfnNotFoundException(ResourceModel.TYPE_NAME, studioId);
                    }

                    try {
                        final UpdateStudioResponse updateStudioResponse = client
                            .injectCredentialsAndInvokeV2(awsRequest, studioClient::updateStudio);

                        logger.log(String.format("%s [%s] update requested successfully", ResourceModel.TYPE_NAME, studioId));

                        return updateStudioResponse;
                    } catch (final NimbleException e) {
                        logger.log(String.format("%s [%s] exception during update", ResourceModel.TYPE_NAME, studioId));
                        throw Translator.translateToCfnException(e);
                    }

                })
                .stabilize((awsRequest, awsResponse, client, updateModel, context) -> {
                    final String studioId = request.getDesiredResourceState().getStudioId();
                    final Studio studio = getStudio(proxyClient, studioId);

                    if (StudioState.READY.equals(studio.state())) {
                        logger.log(String.format("%s [%s] is in state READY, update succeeded", ResourceModel.TYPE_NAME, studioId));
                        return true;
                    }
                    if (StudioState.UPDATE_IN_PROGRESS.equals(studio.state())) {
                        logger.log(String.format("%s [%s] is in state UPDATING, update pending", ResourceModel.TYPE_NAME,
                            studioId));
                        return false;
                    }

                    logger.log(String.format("%s [%s] is in state %s, update failed", ResourceModel.TYPE_NAME, studioId,
                        studio.state()));
                    throw new CfnGeneralServiceException(String.format("%s - %s", studio.statusCodeAsString(),
                        studio.statusMessage()));

                })
                .progress()
        )
        .then((r) -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }

    private Studio getStudio(final ProxyClient<NimbleClient> proxyClient, final String studioId) {
        try {
            final GetStudioRequest getStudioRequest = GetStudioRequest.builder().studioId(studioId).build();
            final GetStudioResponse getStudioResponse = proxyClient
                .injectCredentialsAndInvokeV2(getStudioRequest, proxyClient.client()::getStudio);
            return getStudioResponse.studio();
        } catch (final NimbleException e) {
            logger.log(String.format("Exception during UPDATE: %s for id: %s.", ResourceModel.TYPE_NAME, studioId));
            throw Translator.translateToCfnException(e);
        }
    }
}
