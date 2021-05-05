package software.amazon.nimblestudio.studio;

import software.amazon.awssdk.services.nimble.NimbleClient;
import software.amazon.awssdk.services.nimble.model.CreateStudioRequest;
import software.amazon.awssdk.services.nimble.model.CreateStudioResponse;
import software.amazon.awssdk.services.nimble.model.GetStudioRequest;
import software.amazon.awssdk.services.nimble.model.GetStudioResponse;
import software.amazon.awssdk.services.nimble.model.Studio;
import software.amazon.awssdk.services.nimble.model.StudioEncryptionConfiguration;
import software.amazon.awssdk.services.nimble.model.StudioEncryptionConfigurationKeyType;
import software.amazon.awssdk.services.nimble.model.StudioState;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.Rule;

import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Mock;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
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
        handler = new CreateHandler();
        when(proxyClient.client()).thenReturn(nimbleClient);
    }

    static Stream<Arguments> testParamsForException() {
        return Utils.parametersForExceptionTests();
    }

    public final CreateStudioResponse generateCreateStudioResult() {
        return CreateStudioResponse.builder()
            .studio(Studio.builder()
                .adminRoleArn("aGIAMARN")
                .createdAt(timestamp)
                .displayName("CreateStudioDisplayName")
                .homeRegion("us-west-2")
                .ssoClientId("SsoClientId")
                .state(StudioState.READY)
                .statusCode("STUDIO_READY")
                .statusMessage("Create Complete")
                .studioId("id")
                .studioName("CreateStudioName")
                .studioUrl("studiourl")
                .updatedAt(timestamp)
                .userRoleArn("uGIAMARN")
                .tags(Utils.generateTags())
                .studioEncryptionConfiguration(StudioEncryptionConfiguration.builder()
                    .keyArn("testKeyArn")
                    .keyType(StudioEncryptionConfigurationKeyType.AWS_OWNED_KEY.toString())
                    .build()
                ).build()
            ).build();
    }

    public static ResourceHandlerRequest<ResourceModel> generateCreateHandlerRequest() {
        return ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(ResourceModel.builder()
                .adminRoleArn("aGIAMARN")
                .displayName("CreateStudioDisplayName")
                .studioName("CreateStudioName")
                .userRoleArn("uGIAMARN")
                .tags(Utils.generateTags())
                .build())
            .clientRequestToken("clientToken")
            .build();
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        Mockito.doReturn(Utils.generateReadStudioReadyResult()).when(proxyClient)
            .injectCredentialsAndInvokeV2(any(GetStudioRequest.class), any());
        Mockito.doReturn(generateCreateStudioResult()).when(proxyClient)
            .injectCredentialsAndInvokeV2(any(CreateStudioRequest.class), any());

        final ProgressEvent<ResourceModel, CallbackContext> response = handler
            .handleRequest(proxy, generateCreateHandlerRequest(), new CallbackContext(), proxyClient, logger);

        final ResourceModel expectedResponseModel = ResourceModel.builder()
            .adminRoleArn("aGIAMARN")
            .displayName("CreateStudioDisplayName")
            .homeRegion("us-west-2")
            .ssoClientId("SsoClientId")
            .studioId("id")
            .studioName("CreateStudioName")
            .studioUrl("studiourl")
            .userRoleArn("uGIAMARN")
            .tags(Utils.generateTags())
            .studioEncryptionConfiguration(software.amazon.nimblestudio.studio.StudioEncryptionConfiguration.builder()
                .keyArn("testKeyArn")
                .keyType(StudioEncryptionConfigurationKeyType.AWS_OWNED_KEY.toString())
                .build())
            .build();

        Mockito.verify(proxyClient, Mockito.times(2))
            .injectCredentialsAndInvokeV2(Mockito.any(GetStudioRequest.class), Mockito.any());

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(expectedResponseModel);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_StudioDeleted() {
        Mockito.doReturn(Utils.generateReadStudioDeletedResult()).when(proxyClient)
            .injectCredentialsAndInvokeV2(any(GetStudioRequest.class), any());
        Mockito.doReturn(generateCreateStudioResult()).when(proxyClient)
            .injectCredentialsAndInvokeV2(any(CreateStudioRequest.class), any());

        assertThrows(CfnGeneralServiceException.class, () -> {
            handler.handleRequest(proxy, generateCreateHandlerRequest(), new CallbackContext(), proxyClient, logger);
        }, "STUDIO_DELETED - Delete Complete");
    }

    @Test
    public void handleRequest_CreateSuccess_Stabilization() {
        Mockito.when(proxyClient.injectCredentialsAndInvokeV2(any(GetStudioRequest.class), any()))
            .thenReturn(Utils.generateReadStudioCreatingResult())
            .thenReturn(Utils.generateReadStudioCreatingResult())
            .thenReturn(Utils.generateReadStudioReadyResult());

        Mockito.doReturn(generateCreateStudioResult()).when(proxyClient)
            .injectCredentialsAndInvokeV2(any(CreateStudioRequest.class), any());

        final ProgressEvent<ResourceModel, CallbackContext> response = handler
            .handleRequest(proxy, generateCreateHandlerRequest(), new CallbackContext(), proxyClient, logger);

        final ResourceModel expectedResponseModel = ResourceModel.builder()
            .adminRoleArn("aGIAMARN")
            .displayName("CreateStudioDisplayName")
            .homeRegion("us-west-2")
            .ssoClientId("SsoClientId")
            .studioId("id")
            .studioName("CreateStudioName")
            .studioUrl("studiourl")
            .userRoleArn("uGIAMARN")
            .tags(Utils.generateTags())
            .studioEncryptionConfiguration(software.amazon.nimblestudio.studio.StudioEncryptionConfiguration.builder()
                .keyArn("testKeyArn")
                .keyType(StudioEncryptionConfigurationKeyType.AWS_OWNED_KEY.toString())
                .build())
            .build();

        Mockito.verify(proxyClient, Mockito.times(4))
            .injectCredentialsAndInvokeV2(Mockito.any(GetStudioRequest.class), Mockito.any());

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(expectedResponseModel);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
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
