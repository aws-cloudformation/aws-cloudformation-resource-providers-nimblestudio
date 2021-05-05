package software.amazon.nimblestudio.studio;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.services.nimble.NimbleClient;
import software.amazon.awssdk.services.nimble.model.DeleteStudioRequest;
import software.amazon.awssdk.services.nimble.model.DeleteStudioResponse;
import software.amazon.awssdk.services.nimble.model.GetStudioRequest;
import software.amazon.awssdk.services.nimble.model.Studio;
import software.amazon.awssdk.services.nimble.model.StudioEncryptionConfiguration;
import software.amazon.awssdk.services.nimble.model.StudioEncryptionConfigurationKeyType;
import software.amazon.awssdk.services.nimble.model.StudioState;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import org.junit.Before;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.junit.Rule;

import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Mock;

import java.time.Instant;
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
    private final Instant timestamp = Instant.ofEpochSecond(1);

    @Rule
    private final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @BeforeEach
    public void setup() {
        environmentVariables.set("AWS_REGION", "us-west-2");
        proxy = getAmazonWebServicesClientProxy();
        nimbleClient = mock(NimbleClient.class);
        handler = new DeleteHandler();
        when(proxyClient.client()).thenReturn(nimbleClient);
    }

    static Stream<Arguments> testParamsForException() {
        return Utils.parametersForExceptionTests();
    }

    private DeleteStudioResponse generateDeleteStudioResult() {

        return DeleteStudioResponse
            .builder().studio(Studio.builder()
                .adminRoleArn("aGIAMARN")
                .createdAt(timestamp)
                .displayName("CreateStudioDisplayName")
                .homeRegion("us-west-2")
                .ssoClientId("SsoClientId")
                .state(StudioState.DELETED)
                .statusCode("STUDIO_DELETED")
                .statusMessage("Delete Complete")
                .studioId("id")
                .studioName("CreateStudioName")
                .studioUrl("studiourl")
                .updatedAt(timestamp)
                .userRoleArn("uGIAMARN")
                .studioEncryptionConfiguration(StudioEncryptionConfiguration.builder()
                    .keyArn("testKeyArn")
                    .keyType(StudioEncryptionConfigurationKeyType.AWS_OWNED_KEY.toString())
                    .build()
                ).build()
            ).build();
    }

    private static ResourceHandlerRequest<ResourceModel> generateDeleteHandlerRequest() {
        return ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(ResourceModel.builder().studioId("id").build())
            .clientRequestToken("clientToken").build();
    }

    @Test
    public void handleRequest_DeleteSuccess_Stabilization() {
        Mockito.when(proxyClient.injectCredentialsAndInvokeV2(any(GetStudioRequest.class), any()))
            .thenReturn(Utils.generateReadStudioReadyResult())
            .thenReturn(Utils.generateReadStudioDeletingResult())
            .thenReturn(Utils.generateReadStudioDeletedResult());

        Mockito.doReturn(generateDeleteStudioResult()).when(proxyClient)
            .injectCredentialsAndInvokeV2(any(DeleteStudioRequest.class), any());

        final ProgressEvent<ResourceModel, CallbackContext> response = handler
            .handleRequest(proxy, generateDeleteHandlerRequest(), new CallbackContext(), proxyClient, logger);

        Mockito.verify(proxyClient, Mockito.times(3))
            .injectCredentialsAndInvokeV2(Mockito.any(GetStudioRequest.class), Mockito.any());
        Mockito.verify(proxyClient, Mockito.times(1))
            .injectCredentialsAndInvokeV2(Mockito.any(DeleteStudioRequest.class), Mockito.any());

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_DeleteFailed_Stabilization() {
        Mockito.when(proxyClient.injectCredentialsAndInvokeV2(any(GetStudioRequest.class), any()))
            .thenReturn(Utils.generateReadStudioDeletingResult())
            .thenReturn(Utils.generateReadStudioDeletingResult())
            .thenReturn(Utils.generateReadStudioReadyResult());

        Mockito.doReturn(generateDeleteStudioResult()).when(proxyClient)
            .injectCredentialsAndInvokeV2(any(DeleteStudioRequest.class), any());

        final ResourceHandlerRequest<ResourceModel> request = generateDeleteHandlerRequest();

        assertThrows(CfnGeneralServiceException.class, () -> {
            handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);
        });
    }

    @Test
    public void handleRequest_AlreadyDeleted_Exception() {
        Mockito.when(proxyClient.injectCredentialsAndInvokeV2(any(GetStudioRequest.class), any()))
            .thenReturn(Utils.generateReadStudioDeletedResult());

        assertThrows(CfnNotFoundException.class, () -> {
            handler.handleRequest(proxy, generateDeleteHandlerRequest(), new CallbackContext(), proxyClient, logger);
        });
    }

    @ParameterizedTest
    @MethodSource("testParamsForException")
    public void handleRequest_Failed_Exception(final Class<Throwable> thrownException,
                                               final Class<Throwable> expectedException) {
        Mockito.doThrow(thrownException).when(proxyClient).injectCredentialsAndInvokeV2(any(), any());

        assertThrows(expectedException, () -> {
            handler.handleRequest(proxy, generateDeleteHandlerRequest(), new CallbackContext(), proxyClient, logger);
        });
    }

}
