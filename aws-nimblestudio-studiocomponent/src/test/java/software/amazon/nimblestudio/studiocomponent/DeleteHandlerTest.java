package software.amazon.nimblestudio.studiocomponent;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.services.nimble.NimbleClient;
import software.amazon.awssdk.services.nimble.model.DeleteStudioComponentRequest;
import software.amazon.awssdk.services.nimble.model.DeleteStudioComponentResponse;
import software.amazon.awssdk.services.nimble.model.ActiveDirectoryConfiguration;
import software.amazon.awssdk.services.nimble.model.ComputeFarmConfiguration;
import software.amazon.awssdk.services.nimble.model.GetStudioComponentRequest;
import software.amazon.awssdk.services.nimble.model.LaunchProfilePlatform;
import software.amazon.awssdk.services.nimble.model.LicenseServiceConfiguration;
import software.amazon.awssdk.services.nimble.model.SharedFileSystemConfiguration;
import software.amazon.awssdk.services.nimble.model.StudioComponent;
import software.amazon.awssdk.services.nimble.model.StudioComponentConfiguration;
import software.amazon.awssdk.services.nimble.model.StudioComponentInitializationScript;
import software.amazon.awssdk.services.nimble.model.StudioComponentInitializationScriptRunContext;
import software.amazon.awssdk.services.nimble.model.StudioComponentState;
import software.amazon.awssdk.services.nimble.model.StudioComponentStatusCode;
import software.amazon.awssdk.services.nimble.model.StudioComponentSubtype;
import software.amazon.awssdk.services.nimble.model.StudioComponentType;
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
import java.util.ArrayList;
import java.util.Arrays;
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

    private DeleteStudioComponentResponse generateDeleteStudioComponentResult() {
        return DeleteStudioComponentResponse.builder()
            .studioComponent(StudioComponent.builder()
                .configuration(StudioComponentConfiguration.builder()
                    .activeDirectoryConfiguration(ActiveDirectoryConfiguration.builder().build())
                    .computeFarmConfiguration(ComputeFarmConfiguration.builder().build())
                    .licenseServiceConfiguration(LicenseServiceConfiguration.builder().build())
                    .sharedFileSystemConfiguration(SharedFileSystemConfiguration.builder().build())
                    .build())
                .createdAt(timestamp)
                .createdBy("Fuzzy")
                .description("test")
                .initializationScripts(Arrays.asList(
                    StudioComponentInitializationScript.builder()
                        .script("script1")
                        .runContext(StudioComponentInitializationScriptRunContext.SYSTEM_INITIALIZATION)
                        .platform(LaunchProfilePlatform.WINDOWS)
                        .build(),
                    StudioComponentInitializationScript.builder()
                        .script("script2")
                        .runContext(StudioComponentInitializationScriptRunContext.USER_INITIALIZATION)
                        .platform(LaunchProfilePlatform.LINUX)
                        .build()
                ))
                .name("studioComponent")
                .scriptParameters(new ArrayList<>())
                .ec2SecurityGroupIds(new ArrayList<>())
                .state(StudioComponentState.DELETE_IN_PROGRESS)
                .statusCode(StudioComponentStatusCode.STUDIO_COMPONENT_DELETE_IN_PROGRESS)
                .statusMessage("msg")
                .studioComponentId("studioComponentId")
                .subtype(StudioComponentSubtype.AMAZON_FSX_FOR_WINDOWS)
                .type(StudioComponentType.COMPUTE_FARM)
                .updatedAt(timestamp)
                .updatedBy("Pixel")
                .build())
            .build();
    }

    private static ResourceHandlerRequest<ResourceModel> generateDeleteHandlerRequest() {
        return ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(ResourceModel.builder()
                .studioId("id")
                .studioComponentId("studioComponentId")
                .build())
            .clientRequestToken("clientToken")
            .build();
    }

    @Test
    public void handleRequest_DeleteSuccess_Stabilization() {
        Mockito.when(proxyClient.injectCredentialsAndInvokeV2(any(GetStudioComponentRequest.class), any()))
            .thenReturn(Utils.generateReadStudioComponentReadyResult())
            .thenReturn(Utils.generateReadStudioComponentDeletingResult())
            .thenReturn(Utils.generateReadStudioComponentDeletedResult());

        Mockito.doReturn(generateDeleteStudioComponentResult()).when(proxyClient)
            .injectCredentialsAndInvokeV2(any(DeleteStudioComponentRequest.class), any());

        final ProgressEvent<ResourceModel, CallbackContext> response = handler
            .handleRequest(proxy, generateDeleteHandlerRequest(), new CallbackContext(), proxyClient, logger);

        Mockito.verify(proxyClient, Mockito.times(3))
            .injectCredentialsAndInvokeV2(Mockito.any(GetStudioComponentRequest.class), Mockito.any());

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
        Mockito.when(proxyClient.injectCredentialsAndInvokeV2(any(GetStudioComponentRequest.class), any()))
            .thenReturn(Utils.generateReadStudioComponentDeletedResult());

        assertThrows(CfnNotFoundException.class, () -> {
            handler.handleRequest(proxy, generateDeleteHandlerRequest(), new CallbackContext(), proxyClient, logger);
        });
    }

    @ParameterizedTest
    @MethodSource("testParamsForException")
    public void handleRequest_Failed_Exception(final Class<Throwable> thrownException,
                                               final Class<Throwable> expectedException) {
        Mockito.doReturn(Utils.generateReadStudioComponentReadyResult()).when(proxyClient)
            .injectCredentialsAndInvokeV2(any(GetStudioComponentRequest.class), any());

        Mockito.doThrow(thrownException).when(proxyClient).injectCredentialsAndInvokeV2(
            any(DeleteStudioComponentRequest.class), any());

        assertThrows(expectedException, () -> {
            handler.handleRequest(proxy, generateDeleteHandlerRequest(), new CallbackContext(), proxyClient, logger);
        });
    }

}
