package software.amazon.nimblestudio.studio;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.services.nimble.NimbleClient;
import software.amazon.awssdk.services.nimble.model.GetStudioRequest;
import software.amazon.awssdk.services.nimble.model.Studio;
import software.amazon.awssdk.services.nimble.model.StudioEncryptionConfiguration;
import software.amazon.awssdk.services.nimble.model.StudioEncryptionConfigurationKeyType;
import software.amazon.awssdk.services.nimble.model.UpdateStudioRequest;
import software.amazon.awssdk.services.nimble.model.UpdateStudioResponse;
import software.amazon.awssdk.services.nimble.model.StudioState;
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
import org.mockito.MockitoAnnotations;
import org.mockito.Mock;

import java.time.Instant;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UpdateHandlerTest extends AbstractTestBase {

    @Mock
    private NimbleClient nimbleClient;

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<NimbleClient> proxyClient;

    private UpdateHandler handler;
    private Instant timestamp = Instant.ofEpochSecond(1);

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

    private UpdateStudioResponse generateUpdateStudioResult() {
        return UpdateStudioResponse.builder()
            .studio(Studio.builder()
                .adminRoleArn("aGIAMARN")
                .createdAt(timestamp)
                .displayName("UpdateStudioDisplayName")
                .homeRegion("us-west-2")
                .ssoClientId("SsoClientId")
                .state(StudioState.READY).statusCode("STUDIO_READY")
                .statusMessage("Update Complete")
                .studioId("id").studioName("UpdateStudioName")
                .studioUrl("studiourl")
                .updatedAt(timestamp)
                .userRoleArn("uGIAMARN")
                .tags(Utils.generateTags())
                .studioEncryptionConfiguration(StudioEncryptionConfiguration.builder()
                    .keyArn("testKeyArn")
                    .keyType(StudioEncryptionConfigurationKeyType.AWS_OWNED_KEY.toString())
                    .build())
                .build())
            .build();
    }

    private ResourceHandlerRequest<ResourceModel> generateUpdateHandlerRequest() {
        return ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(
                ResourceModel.builder()
                    .adminRoleArn("aGIAMARN")
                    .displayName("UpdateStudioDisplayName")
                    .studioId("idUpdated")
                    .userRoleArn("uGIAMARN")
                    .tags(Utils.generateTags())
                    .build())
            .clientRequestToken("clientToken").build();
    }

    private ResourceHandlerRequest<ResourceModel> generateUpdateHandlerBlankRequest() {
        return ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(ResourceModel.builder()
                .adminRoleArn(" ")
                .studioId("idUpdated")
                .userRoleArn("uGIAMARN")
                .tags(Utils.generateTags())
                .build())
            .build();
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        Mockito.doReturn(Utils.generateReadStudioUpdatedResult()).when(proxyClient)
            .injectCredentialsAndInvokeV2(any(GetStudioRequest.class), any());
        Mockito.doReturn(generateUpdateStudioResult()).when(proxyClient)
            .injectCredentialsAndInvokeV2(any(UpdateStudioRequest.class), any());

        final ProgressEvent<ResourceModel, CallbackContext> response = handler
            .handleRequest(proxy, generateUpdateHandlerRequest(), new CallbackContext(), proxyClient, logger);

        final ResourceModel expectedResponseModel = ResourceModel.builder()
            .adminRoleArn("aGIAMARN")
            .displayName("UpdateStudioDisplayName")
            .homeRegion("us-west-2")
            .ssoClientId("SsoClientId")
            .studioId("id")
            .studioName("UpdateStudioName")
            .studioUrl("studiourl")
            .userRoleArn("uGIAMARN")
            .tags(Utils.generateTags())
            .studioEncryptionConfiguration(software.amazon.nimblestudio.studio.StudioEncryptionConfiguration.builder()
                .keyArn("testKeyArn")
                .keyType(StudioEncryptionConfigurationKeyType.AWS_OWNED_KEY.toString())
                .build())
            .build();

        Mockito.verify(proxyClient, Mockito.times(3))
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
    public void handleRequest_UpdateSuccess_Stabilization() {
        Mockito.when(proxyClient.injectCredentialsAndInvokeV2(any(GetStudioRequest.class), any()))
            .thenReturn(Utils.generateReadStudioUpdatingResult())
            .thenReturn(Utils.generateReadStudioUpdatingResult())
            .thenReturn(Utils.generateReadStudioUpdatedResult());

        Mockito.doReturn(generateUpdateStudioResult()).when(proxyClient)
            .injectCredentialsAndInvokeV2(any(UpdateStudioRequest.class), any());

        final ProgressEvent<ResourceModel, CallbackContext> response = handler
            .handleRequest(proxy, generateUpdateHandlerRequest(), new CallbackContext(), proxyClient, logger);

        final ResourceModel expectedResponseModel = ResourceModel.builder()
            .adminRoleArn("aGIAMARN")
            .displayName("UpdateStudioDisplayName")
            .homeRegion("us-west-2")
            .ssoClientId("SsoClientId")
            .studioId("id")
            .studioName("UpdateStudioName")
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

    @Test
    public void handleRequest_UpdateSuccess_BlankRequest() {
        Mockito.doReturn(Utils.generateReadStudioUpdatedResult()).when(proxyClient)
            .injectCredentialsAndInvokeV2(any(GetStudioRequest.class), any());
        Mockito.doReturn(generateUpdateStudioResult()).when(proxyClient)
            .injectCredentialsAndInvokeV2(any(UpdateStudioRequest.class), any());

        final ProgressEvent<ResourceModel, CallbackContext> response = handler
            .handleRequest(proxy, generateUpdateHandlerBlankRequest(), new CallbackContext(), proxyClient, logger);

        final ResourceModel expectedResponseModel = ResourceModel.builder()
            .adminRoleArn("aGIAMARN")
            .displayName("UpdateStudioDisplayName")
            .homeRegion("us-west-2")
            .ssoClientId("SsoClientId")
            .studioId("id")
            .studioName("UpdateStudioName")
            .studioUrl("studiourl")
            .userRoleArn("uGIAMARN")
            .tags(Utils.generateTags())
            .studioEncryptionConfiguration(software.amazon.nimblestudio.studio.StudioEncryptionConfiguration.builder()
                .keyArn("testKeyArn")
                .keyType(StudioEncryptionConfigurationKeyType.AWS_OWNED_KEY.toString())
                .build())
            .build();

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
    public void handleRequest_AlreadyDeleted_Exception() {
        Mockito.when(proxyClient.injectCredentialsAndInvokeV2(any(GetStudioRequest.class), any()))
            .thenReturn(Utils.generateReadStudioDeletedResult());

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
