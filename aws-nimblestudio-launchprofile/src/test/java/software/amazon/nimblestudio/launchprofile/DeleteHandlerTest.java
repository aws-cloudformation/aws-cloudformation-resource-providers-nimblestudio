package software.amazon.nimblestudio.launchprofile;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.services.nimble.NimbleClient;
import software.amazon.awssdk.services.nimble.model.DeleteLaunchProfileRequest;
import software.amazon.awssdk.services.nimble.model.DeleteLaunchProfileResponse;
import software.amazon.awssdk.services.nimble.model.GetLaunchProfileRequest;
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
public class DeleteHandlerTest extends AbstractTestBase {

    @Mock
    private NimbleClient nimbleClient;

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<NimbleClient> proxyClient;

    private DeleteHandler handler;

    @BeforeEach
    public void setup() {
        proxy = getAmazonWebServicesClientProxy();
        nimbleClient = mock(NimbleClient.class);
        handler = new DeleteHandler();
        when(proxyClient.client()).thenReturn(nimbleClient);
    }

    static Stream<Arguments> testParamsForException() {
        return Utils.parametersForExceptionTests();
    }

    public final DeleteLaunchProfileResponse generateDeleteLaunchProfileResult() {
        return DeleteLaunchProfileResponse.builder()
            .launchProfile(LaunchProfile.builder()
                .launchProfileId("launchProfileId")
                .createdAt(Instant.EPOCH)
                .createdBy("Bob")
                .description("For bob")
                .name("launchProfileName")
                .state(LaunchProfileState.DELETE_IN_PROGRESS)
                .statusCode(LaunchProfileStatusCode.LAUNCH_PROFILE_DELETE_IN_PROGRESS)
                .statusMessage("Ready!")
                .streamConfiguration(Utils.generateStreamConfiguration())
                .studioComponentIds(Collections.singletonList("studioComponentId"))
                .updatedAt(Instant.EPOCH)
                .updatedBy("Bob")
                .build()
            )
            .build();
    }

    public static ResourceHandlerRequest<ResourceModel> generateDeleteHandlerRequest() {
        return ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(ResourceModel.builder()
                .studioId("studioId")
                .launchProfileId("launchProfileId")
                .build())
            .clientRequestToken("clientToken")
            .build();
    }


    @Test
    public void handleRequest_DeleteSuccess_Stabilization() {
        Mockito.when(proxyClient.injectCredentialsAndInvokeV2(any(GetLaunchProfileRequest.class), any()))
                .thenReturn(Utils.generateGetLaunchProfileResponse(LaunchProfileState.READY))
                .thenReturn(Utils.generateGetLaunchProfileResponse(LaunchProfileState.DELETE_IN_PROGRESS))
                .thenReturn(Utils.generateGetLaunchProfileResponse(LaunchProfileState.DELETED));

        Mockito.doReturn(generateDeleteLaunchProfileResult()).when(proxyClient)
            .injectCredentialsAndInvokeV2(any(DeleteLaunchProfileRequest.class), any());

        final ProgressEvent<ResourceModel, CallbackContext> response = handler
            .handleRequest(proxy, generateDeleteHandlerRequest(), new CallbackContext(), proxyClient, logger);

        Mockito.verify(proxyClient, Mockito.times(3))
                .injectCredentialsAndInvokeV2(Mockito.any(GetLaunchProfileRequest.class), Mockito.any());
        Mockito.verify(proxyClient, Mockito.times(1))
                .injectCredentialsAndInvokeV2(Mockito.any(DeleteLaunchProfileRequest.class), Mockito.any());

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(null);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }


    @Test
    public void handleRequest_AlreadyDeleted_Exception() {
        Mockito.when(proxyClient.injectCredentialsAndInvokeV2(any(GetLaunchProfileRequest.class), any()))
            .thenReturn(Utils.generateGetLaunchProfileResponse(LaunchProfileState.DELETED));

        assertThrows(CfnNotFoundException.class, () -> {
            handler.handleRequest(proxy, generateDeleteHandlerRequest(), new CallbackContext(), proxyClient, logger);
        });
    }

    @ParameterizedTest
    @MethodSource("testParamsForException")
    public void handleRequest_Failed_Exception(final Class<Throwable> thrownException,
                                               final Class<Throwable> expectedException) {
        Mockito.when(proxyClient.injectCredentialsAndInvokeV2(any(GetLaunchProfileRequest.class), any()))
                .thenReturn(Utils.generateGetLaunchProfileResponse(LaunchProfileState.READY));
        Mockito.doThrow(thrownException).when(proxyClient).injectCredentialsAndInvokeV2(any(DeleteLaunchProfileRequest.class), any());

        assertThrows(expectedException, () -> {
            handler.handleRequest(proxy, generateDeleteHandlerRequest(), new CallbackContext(), proxyClient, logger);
        });
    }
}
