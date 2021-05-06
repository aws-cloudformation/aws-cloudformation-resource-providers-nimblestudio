package software.amazon.nimblestudio.launchprofile;

import software.amazon.awssdk.services.nimble.NimbleClient;
import software.amazon.awssdk.services.nimble.model.CreateLaunchProfileRequest;
import software.amazon.awssdk.services.nimble.model.CreateLaunchProfileResponse;
import software.amazon.awssdk.services.nimble.model.LaunchProfile;
import software.amazon.awssdk.services.nimble.model.LaunchProfileState;
import software.amazon.awssdk.services.nimble.model.LaunchProfileStatusCode;
import software.amazon.awssdk.services.nimble.model.GetLaunchProfileRequest;
import software.amazon.awssdk.services.nimble.model.GetLaunchProfileResponse;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mockito;
import org.mockito.Mock;

import java.time.Instant;
import java.util.Collections;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest extends AbstractTestBase {

    @Mock
    private NimbleClient nimbleClient;

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<NimbleClient> proxyClient;

    private CreateHandler handler;

    @BeforeEach
    public void setup() {
        proxy = getAmazonWebServicesClientProxy();
        nimbleClient = mock(NimbleClient.class);
        handler = new CreateHandler();
        when(proxyClient.client()).thenReturn(nimbleClient);
    }

    static Stream<Arguments> testParamsForException() {
        return Utils.parametersForExceptionTests();
    }

    public final CreateLaunchProfileResponse generateCreateLaunchProfileResult() {
        return CreateLaunchProfileResponse.builder()
            .launchProfile(LaunchProfile.builder()
                .launchProfileId("launchProfileId")
                .createdAt(Instant.EPOCH)
                .createdBy("Bob")
                .description("For bob")
                .ec2SubnetIds(Collections.singletonList("subnet1"))
                .name("launchProfileName")
                .state(LaunchProfileState.CREATE_IN_PROGRESS)
                .statusCode(LaunchProfileStatusCode.LAUNCH_PROFILE_CREATED)
                .statusMessage("Ready!")
                .streamConfiguration(Utils.generateStreamConfiguration())
                .launchProfileProtocolVersions(Collections.singletonList("2021-03-31"))
                .studioComponentIds(Collections.singletonList("studioComponentId"))
                .updatedAt(Instant.EPOCH)
                .updatedBy("Bob")
                .tags(Utils.generateTags())
                .build()
            )
            .build();
    }

    public static ResourceHandlerRequest<ResourceModel> generateCreateHandlerRequest() {
        return ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(ResourceModel.builder()
                .name("launchProfileName")
                .studioId("studioId")
                .ec2SubnetIds(Collections.singletonList("subnet1"))
                .streamConfiguration(
                    Translator.toModelStreamConfiguration(Utils.generateStreamConfiguration())
                )
                .launchProfileProtocolVersions(Collections.singletonList("2021-03-31"))
                .studioComponentIds(Collections.singletonList("studioComponentId"))
                .tags(Utils.generateTags())
                .build())
            .clientRequestToken("clientToken")
            .build();
    }

    static GetLaunchProfileResponse generateGetLaunchProfileDeletedResponse() {
        return GetLaunchProfileResponse.builder()
            .launchProfile(LaunchProfile.builder()
                .launchProfileId("launchProfileId")
                .createdAt(Instant.EPOCH)
                .createdBy("Bob")
                .description("For bob")
                .ec2SubnetIds(Collections.singletonList("subnet1"))
                .name("launchProfileName")
                .state(LaunchProfileState.DELETED)
                .statusCode(LaunchProfileStatusCode.LAUNCH_PROFILE_DELETED)
                .statusMessage("Delete Complete")
                .streamConfiguration(Utils.generateStreamConfiguration())
                .launchProfileProtocolVersions(Collections.singletonList("2021-03-31"))
                .studioComponentIds(Collections.singletonList("studioComponentId"))
                .updatedAt(Instant.EPOCH)
                .updatedBy("Bob")
                .tags(Utils.generateTags())
                .build()
            ).build();
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        Mockito.doReturn(Utils.generateGetLaunchProfileResponse(LaunchProfileState.READY)).when(proxyClient)
            .injectCredentialsAndInvokeV2(any(GetLaunchProfileRequest.class), any());

        Mockito.doReturn(generateCreateLaunchProfileResult()).when(proxyClient)
            .injectCredentialsAndInvokeV2(any(CreateLaunchProfileRequest.class), any());

        final ResourceHandlerRequest<ResourceModel> request = generateCreateHandlerRequest();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler
            .handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        Mockito.verify(proxyClient, Mockito.times(2))
            .injectCredentialsAndInvokeV2(Mockito.any(GetLaunchProfileRequest.class), Mockito.any());

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(Utils.generateGetLaunchProfileResponseModel());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_CreateSuccess_Stabilization() {
        final GetLaunchProfileResponse getLaunchProfileCreatingResponse =
            Utils.generateGetLaunchProfileResponse(LaunchProfileState.CREATE_IN_PROGRESS);
        final GetLaunchProfileResponse getLaunchProfileReadyResponse =
            Utils.generateGetLaunchProfileResponse(LaunchProfileState.READY);
        Mockito.when(proxyClient.injectCredentialsAndInvokeV2(any(GetLaunchProfileRequest.class), any()))
            .thenReturn(getLaunchProfileCreatingResponse)
            .thenReturn(getLaunchProfileCreatingResponse)
            .thenReturn(getLaunchProfileReadyResponse);

        Mockito.doReturn(generateCreateLaunchProfileResult()).when(proxyClient)
            .injectCredentialsAndInvokeV2(any(CreateLaunchProfileRequest.class), any());

        final ProgressEvent<ResourceModel, CallbackContext> response = handler
            .handleRequest(proxy, generateCreateHandlerRequest(), new CallbackContext(), proxyClient, logger);

        Mockito.verify(proxyClient, Mockito.times(4))
            .injectCredentialsAndInvokeV2(Mockito.any(GetLaunchProfileRequest.class), Mockito.any());

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(Utils.generateGetLaunchProfileResponseModel());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_LaunchProfileDeleted() {
        Mockito.doReturn(generateGetLaunchProfileDeletedResponse()).when(proxyClient)
            .injectCredentialsAndInvokeV2(any(GetLaunchProfileRequest.class), any());
        Mockito.doReturn(generateCreateLaunchProfileResult()).when(proxyClient)
            .injectCredentialsAndInvokeV2(any(CreateLaunchProfileRequest.class), any());

        final ResourceHandlerRequest<ResourceModel> request = generateCreateHandlerRequest();
        assertThrows(CfnGeneralServiceException.class, () -> {
            handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);
        }, "LAUNCH_PROFILE_DELETED - Delete Complete");
    }

    @ParameterizedTest
    @MethodSource("testParamsForException")
    public void handleRequest_Failed_Exception(final Class<Throwable> thrownException,
                                               final Class<Throwable> expectedException) {
        Mockito.doThrow(thrownException).when(proxyClient).injectCredentialsAndInvokeV2(any(), any());
        assertThrows(expectedException, () -> {
            handler.handleRequest(proxy, generateCreateHandlerRequest(), new CallbackContext(), proxyClient, logger);
        });
    }
}
