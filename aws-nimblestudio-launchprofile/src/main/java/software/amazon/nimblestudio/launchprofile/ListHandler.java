package software.amazon.nimblestudio.launchprofile;

import software.amazon.awssdk.services.nimble.NimbleClient;
import software.amazon.awssdk.services.nimble.model.LaunchProfile;
import software.amazon.awssdk.services.nimble.model.LaunchProfileState;
import software.amazon.awssdk.services.nimble.model.ListLaunchProfilesRequest;
import software.amazon.awssdk.services.nimble.model.ListLaunchProfilesResponse;
import software.amazon.awssdk.services.nimble.model.NimbleException;
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
        final AmazonWebServicesClientProxy proxy, final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext, final ProxyClient<NimbleClient> proxyClient, final Logger logger) {

        final ListLaunchProfilesRequest listLaunchProfilesRequest = ListLaunchProfilesRequest.builder()
                .nextToken(request.getNextToken())
                .studioId(request.getDesiredResourceState().getStudioId())
                .build();

        try {
            final ListLaunchProfilesResponse listLaunchProfilesResponse = proxyClient
                .injectCredentialsAndInvokeV2(listLaunchProfilesRequest, proxyClient.client()::listLaunchProfiles);

            final List<LaunchProfile> launchProfiles = listLaunchProfilesResponse.hasLaunchProfiles() ?
                    listLaunchProfilesResponse.launchProfiles() :
                    new ArrayList<>();

            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModels(launchProfiles.stream()
                    .filter(launchProfile ->
                        !LaunchProfileState.DELETED.equals(launchProfile.state()) &&
                        !LaunchProfileState.CREATE_FAILED.equals(launchProfile.state()))
                    .map(launchProfile -> ResourceModel.builder()
                        .description(launchProfile.description())
                        .ec2SubnetIds(launchProfile.ec2SubnetIds())
                        .launchProfileId(launchProfile.launchProfileId())
                        .name(launchProfile.name())
                        .streamConfiguration(Translator.toModelStreamConfiguration(launchProfile.streamConfiguration()))
                        .studioComponentIds(launchProfile.studioComponentIds())
                        .launchProfileProtocolVersions(launchProfile.launchProfileProtocolVersions())
                        .studioId(request.getDesiredResourceState().getStudioId())
                        .tags(launchProfile.tags())
                        .build())
                    .collect(toList()))
                .nextToken(listLaunchProfilesResponse.nextToken())
                .status(OperationStatus.SUCCESS)
                .build();
        } catch (final NimbleException e) {
            logger.log(String.format("Exception during list: %s.", ResourceModel.TYPE_NAME));
            throw Translator.translateToCfnException(e);
        }
    }
}
