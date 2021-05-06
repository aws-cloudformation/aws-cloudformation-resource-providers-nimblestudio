package software.amazon.nimblestudio.launchprofile;

import software.amazon.awssdk.services.nimble.NimbleClient;
import software.amazon.awssdk.services.nimble.model.GetLaunchProfileRequest;
import software.amazon.awssdk.services.nimble.model.GetLaunchProfileResponse;
import software.amazon.awssdk.services.nimble.model.UpdateLaunchProfileRequest;
import software.amazon.awssdk.services.nimble.model.UpdateLaunchProfileResponse;
import software.amazon.awssdk.services.nimble.model.LaunchProfile;
import software.amazon.awssdk.services.nimble.model.LaunchProfileState;
import software.amazon.awssdk.services.nimble.model.LaunchProfileStatusCode;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
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

public class UpdateHandlerTest extends AbstractTestBase {

    @Mock
    private NimbleClient nimbleClient;

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<NimbleClient> proxyClient;

    private UpdateHandler handler;

    @BeforeEach
    public void setup() {
        proxy = getAmazonWebServicesClientProxy();
        nimbleClient = mock(NimbleClient.class);
        handler = new UpdateHandler();
        when(proxyClient.client()).thenReturn(nimbleClient);
    }

    static Stream<Arguments> testParamsForException() {
        return Utils.parametersForExceptionTests();
    }

    public final UpdateLaunchProfileResponse generateUpdateLaunchProfileResult() {
        return UpdateLaunchProfileResponse.builder()
            .launchProfile(LaunchProfile.builder()
                .launchProfileId("launchProfileId")
                .createdAt(Instant.EPOCH)
                .createdBy("Bob")
                .description("For bob")
                .name("launchProfileName")
                .state(LaunchProfileState.UPDATE_IN_PROGRESS)
                .statusCode(LaunchProfileStatusCode.LAUNCH_PROFILE_UPDATE_IN_PROGRESS)
                .statusMessage("Ready!")
                .streamConfiguration(Utils.generateStreamConfiguration())
                .launchProfileProtocolVersions(Collections.singletonList("2021-03-31"))
                .studioComponentIds(Collections.singletonList("studioComponentId"))
                .updatedAt(Instant.EPOCH)
                .updatedBy("Bob")
                .tags(Utils.generateTags())
                .build())
            .build();

    }

    public static ResourceHandlerRequest<ResourceModel> generateUpdateHandlerRequest() {
        return ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(ResourceModel.builder()
                .name("launchProfileName")
                .studioId("studioId")
                .description("description")
                .launchProfileId("launchProfileId")
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

    @Test
    public void handleRequest_SimpleSuccess() {

        Mockito.doReturn(Utils.generateGetLaunchProfileResponse(LaunchProfileState.READY)).when(proxyClient)
            .injectCredentialsAndInvokeV2(any(GetLaunchProfileRequest.class), any());

        Mockito.doReturn(generateUpdateLaunchProfileResult()).when(proxyClient)
            .injectCredentialsAndInvokeV2(any(UpdateLaunchProfileRequest.class), any());

        final ProgressEvent<ResourceModel, CallbackContext> response = handler
            .handleRequest(proxy, generateUpdateHandlerRequest(), new CallbackContext(), proxyClient, logger);

        Mockito.verify(proxyClient, Mockito.times(3))
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
    public void handleRequest_UpdateSuccess_Stabilization() {

        final GetLaunchProfileResponse getLaunchProfileUpdatingResponse =
            Utils.generateGetLaunchProfileResponse(LaunchProfileState.UPDATE_IN_PROGRESS);
        final GetLaunchProfileResponse getLaunchProfileReadyResponse =
            Utils.generateGetLaunchProfileResponse(LaunchProfileState.READY);

        Mockito.when(proxyClient.injectCredentialsAndInvokeV2(any(GetLaunchProfileRequest.class), any()))
            .thenReturn(getLaunchProfileUpdatingResponse)
            .thenReturn(getLaunchProfileUpdatingResponse)
            .thenReturn(getLaunchProfileReadyResponse);

        Mockito.doReturn(generateUpdateLaunchProfileResult()).when(proxyClient)
            .injectCredentialsAndInvokeV2(any(UpdateLaunchProfileRequest.class), any());

        final ProgressEvent<ResourceModel, CallbackContext> response = handler
            .handleRequest(proxy, generateUpdateHandlerRequest(), new CallbackContext(), proxyClient, logger);

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
    public void handleRequest_AlreadyDeleted_Exception() {
        Mockito.when(proxyClient.injectCredentialsAndInvokeV2(any(GetLaunchProfileRequest.class), any()))
            .thenReturn(Utils.generateGetLaunchProfileResponse(LaunchProfileState.DELETED));

        assertThrows(CfnNotFoundException.class, () -> {
            handler.handleRequest(proxy, generateUpdateHandlerRequest(), new CallbackContext(), proxyClient, logger);
        });
    }

    @ParameterizedTest
    @MethodSource("testParamsForException")
    public void handleRequest_Failed_Exception(final Class<Throwable> thrownException,
                                               final Class<Throwable> expectedException) {
        Mockito.doThrow(thrownException).when(proxyClient).injectCredentialsAndInvokeV2(any(), any());
        assertThrows(expectedException, () -> {
            handler.handleRequest(proxy, generateUpdateHandlerRequest(), new CallbackContext(), proxyClient, logger);
        });
    }
}
