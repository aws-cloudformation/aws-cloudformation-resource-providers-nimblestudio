package software.amazon.nimblestudio.studiocomponent;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.services.nimble.NimbleClient;
import software.amazon.awssdk.services.nimble.model.GetStudioComponentRequest;
import software.amazon.awssdk.services.nimble.model.GetStudioComponentResponse;
import software.amazon.awssdk.services.nimble.model.ActiveDirectoryConfiguration;
import software.amazon.awssdk.services.nimble.model.ComputeFarmConfiguration;
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
public class ReadHandlerTest extends AbstractTestBase {

    @Mock
    private NimbleClient nimbleClient;

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<NimbleClient> proxyClient;

    private ReadHandler handler;
    private Instant timestamp = Instant.now();

    @BeforeEach
    public void setup() {
        proxy = getAmazonWebServicesClientProxy();
        nimbleClient = mock(NimbleClient.class);
        handler = new ReadHandler();
        when(proxyClient.client()).thenReturn(nimbleClient);
    }

    static Stream<Arguments> testParamsForException() {
        return Utils.parametersForExceptionTests();
    }

    private GetStudioComponentResponse generateReadStudioComponentResult() {
        return GetStudioComponentResponse.builder()
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
                        .launchProfileProtocolVersion("2021-03-31")
                        .build(),
                    StudioComponentInitializationScript.builder()
                        .script("script2")
                        .runContext(StudioComponentInitializationScriptRunContext.USER_INITIALIZATION)
                        .platform(LaunchProfilePlatform.LINUX)
                        .launchProfileProtocolVersion("2021-03-31")
                        .build()
                ))
                .name("studioComponent")
                .scriptParameters(new ArrayList<>())
                .ec2SecurityGroupIds(new ArrayList<>())
                .state(StudioComponentState.READY)
                .statusCode(StudioComponentStatusCode.STUDIO_COMPONENT_CREATED)
                .statusMessage("msg")
                .studioComponentId("studioComponentId")
                .subtype(StudioComponentSubtype.AMAZON_FSX_FOR_WINDOWS)
                .type(StudioComponentType.COMPUTE_FARM)
                .tags(Utils.generateTags())
                .updatedAt(timestamp)
                .updatedBy("Pixel")
                .build())
            .build();
    }

    private ResourceHandlerRequest<ResourceModel> generateReadHandlerRequest() {
        return ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(ResourceModel.builder()
                .studioId("id")
                .build())
            .build();
    }

    private ResourceModel generateExpectedResponse() {
        return ResourceModel.builder()
            .configuration(
                software.amazon.nimblestudio.studiocomponent.StudioComponentConfiguration.builder()
                    .activeDirectoryConfiguration(
                        software.amazon.nimblestudio.studiocomponent.ActiveDirectoryConfiguration.builder()
                            .computerAttributes(new ArrayList<>())
                            .build())
                    .computeFarmConfiguration(
                        software.amazon.nimblestudio.studiocomponent.ComputeFarmConfiguration.builder().build())
                    .licenseServiceConfiguration(
                        software.amazon.nimblestudio.studiocomponent.LicenseServiceConfiguration.builder().build())
                    .sharedFileSystemConfiguration(
                        software.amazon.nimblestudio.studiocomponent.SharedFileSystemConfiguration.builder().build())
                    .build())
            .description("test")
            .initializationScripts(Arrays.asList(
                software.amazon.nimblestudio.studiocomponent.StudioComponentInitializationScript.builder()
                    .script("script1")
                    .runContext(StudioComponentInitializationScriptRunContext.SYSTEM_INITIALIZATION.toString())
                    .platform(LaunchProfilePlatform.WINDOWS.toString())
                    .launchProfileProtocolVersion("2021-03-31")
                    .build(),
                software.amazon.nimblestudio.studiocomponent.StudioComponentInitializationScript.builder()
                    .script("script2")
                    .runContext(StudioComponentInitializationScriptRunContext.USER_INITIALIZATION.toString())
                    .platform(LaunchProfilePlatform.LINUX.toString())
                    .launchProfileProtocolVersion("2021-03-31")
                    .build()
            ))
            .name("studioComponent")
            .scriptParameters(new ArrayList<>())
            .ec2SecurityGroupIds(new ArrayList<>())
            .studioComponentId("studioComponentId")
            .subtype(StudioComponentSubtype.AMAZON_FSX_FOR_WINDOWS.toString())
            .type(StudioComponentType.COMPUTE_FARM.toString())
            .tags(Utils.generateTags())
            .studioId("id")
            .build();
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        Mockito.doReturn(generateReadStudioComponentResult()).when(proxyClient)
            .injectCredentialsAndInvokeV2(any(GetStudioComponentRequest.class), any());

        final ResourceHandlerRequest<ResourceModel> request = generateReadHandlerRequest();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler
            .handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(generateExpectedResponse());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_AlreadyDeleted_Exception() {
        Mockito.when(proxyClient.injectCredentialsAndInvokeV2(any(GetStudioComponentRequest.class), any()))
            .thenReturn(Utils.generateReadStudioComponentDeletedResult());

        assertThrows(CfnNotFoundException.class, () -> {
            handler.handleRequest(proxy, generateReadHandlerRequest(), new CallbackContext(), proxyClient, logger);
        });
    }

    @ParameterizedTest
    @MethodSource("testParamsForException")
    public void handleRequest_Failed_Exception(final Class<Throwable> thrownException,
                                               final Class<Throwable> expectedException) {
        Mockito.doThrow(thrownException).when(proxyClient).injectCredentialsAndInvokeV2(any(), any());

        assertThrows(expectedException, () -> {
            handler.handleRequest(proxy, generateReadHandlerRequest(), new CallbackContext(), proxyClient, logger);
        });
    }

}
