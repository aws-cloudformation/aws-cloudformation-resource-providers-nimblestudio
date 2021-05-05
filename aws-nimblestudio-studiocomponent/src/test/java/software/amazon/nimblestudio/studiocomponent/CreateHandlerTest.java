package software.amazon.nimblestudio.studiocomponent;

import software.amazon.awssdk.services.nimble.NimbleClient;
import software.amazon.awssdk.services.nimble.model.CreateStudioComponentRequest;
import software.amazon.awssdk.services.nimble.model.CreateStudioComponentResponse;
import software.amazon.awssdk.services.nimble.model.ActiveDirectoryConfiguration;
import software.amazon.awssdk.services.nimble.model.ComputeFarmConfiguration;
import software.amazon.awssdk.services.nimble.model.GetStudioComponentRequest;
import software.amazon.awssdk.services.nimble.model.GetStudioComponentResponse;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

    public final CreateStudioComponentResponse generateCreateStudioComponentResult() {
        return CreateStudioComponentResponse.builder()
            .studioComponent(StudioComponent.builder()
                .configuration(StudioComponentConfiguration.builder()
                    .activeDirectoryConfiguration(ActiveDirectoryConfiguration.builder().build())
                    .computeFarmConfiguration(ComputeFarmConfiguration.builder().build())
                    .licenseServiceConfiguration(LicenseServiceConfiguration.builder().build())
                    .sharedFileSystemConfiguration(SharedFileSystemConfiguration.builder().build())
                    .build()
                )
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
                .state(StudioComponentState.CREATE_IN_PROGRESS)
                .statusCode(StudioComponentStatusCode.STUDIO_COMPONENT_CREATE_IN_PROGRESS)
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

    private ResourceHandlerRequest<ResourceModel> generateCreateHandlerRequest() {
        final ResourceModel model = ResourceModel.builder()
            .configuration(
                software.amazon.nimblestudio.studiocomponent.StudioComponentConfiguration.builder()
                    .activeDirectoryConfiguration(
                        software.amazon.nimblestudio.studiocomponent.ActiveDirectoryConfiguration.builder()
                            .computerAttributes(Arrays.asList(
                                ActiveDirectoryComputerAttribute.builder().name("n1").value("v1").build(),
                                ActiveDirectoryComputerAttribute.builder().name("n2").value("v2").build()
                            ))
                            .directoryId("did")
                            .organizationalUnitDistinguishedName("oname")
                            .build())
                    .computeFarmConfiguration(
                        software.amazon.nimblestudio.studiocomponent.ComputeFarmConfiguration.builder()
                            .endpoint("endpoint")
                            .activeDirectoryUser("activeDirectoryUser")
                            .build())
                    .licenseServiceConfiguration(
                        software.amazon.nimblestudio.studiocomponent.LicenseServiceConfiguration.builder()
                            .endpoint("endpoint")
                            .build())
                    .sharedFileSystemConfiguration(
                        software.amazon.nimblestudio.studiocomponent.SharedFileSystemConfiguration.builder()
                            .windowsMountDrive("windowsMountDrive")
                            .shareName("shareName")
                            .linuxMountPoint("linuxMountPoint")
                            .fileSystemId("fileSystemId")
                            .endpoint("endpoint")
                            .build())
                    .build()
            )
            .description("Fuzzy")
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
            .studioId("id")
            .studioComponentId("studioComponentid")
            .subtype(StudioComponentSubtype.AMAZON_FSX_FOR_WINDOWS.toString())
            .type(StudioComponentType.SHARED_FILE_SYSTEM.toString())
            .tags(Utils.generateTags())
            .build();
        return ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(model).clientRequestToken("clientToken").build();
    }

    public static ResourceHandlerRequest<ResourceModel> generateCreateHandlerBlankRequest() {
        return ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(ResourceModel.builder()
                .studioId("id")
                .name("studioComponent")
                .type(StudioComponentType.ACTIVE_DIRECTORY.toString())
                .tags(Utils.generateTags())
                .build())
            .clientRequestToken("clientToken")
            .build();
    }

    private ResourceModel generateExpectedResponse() {
        return ResourceModel.builder()
            .configuration(software.amazon.nimblestudio.studiocomponent.StudioComponentConfiguration.builder()
                .activeDirectoryConfiguration(
                    software.amazon.nimblestudio.studiocomponent.ActiveDirectoryConfiguration
                        .builder()
                        .computerAttributes(new ArrayList<>())
                        .build())
                .computeFarmConfiguration(
                    software.amazon.nimblestudio.studiocomponent.ComputeFarmConfiguration
                        .builder()
                        .build())
                .licenseServiceConfiguration(
                    software.amazon.nimblestudio.studiocomponent.LicenseServiceConfiguration
                        .builder()
                        .build())
                .sharedFileSystemConfiguration(
                    software.amazon.nimblestudio.studiocomponent.SharedFileSystemConfiguration
                        .builder()
                        .build())
                .build()
            )
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
        Mockito.doReturn(Utils.generateReadStudioComponentReadyResult()).when(proxyClient)
            .injectCredentialsAndInvokeV2(any(GetStudioComponentRequest.class), any());

        Mockito.doReturn(generateCreateStudioComponentResult()).when(proxyClient)
            .injectCredentialsAndInvokeV2(any(CreateStudioComponentRequest.class), any());

        final ResourceHandlerRequest<ResourceModel> request = generateCreateHandlerRequest();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler
            .handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        Mockito.verify(proxyClient, Mockito.times(2))
            .injectCredentialsAndInvokeV2(Mockito.any(GetStudioComponentRequest.class), Mockito.any());

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
    public void handleRequest_CreateSuccess_Stabilization() {
        Mockito.when(proxyClient.injectCredentialsAndInvokeV2(any(GetStudioComponentRequest.class), any()))
            .thenReturn(Utils.generateReadStudioComponentCreatingResult())
            .thenReturn(Utils.generateReadStudioComponentCreatingResult())
            .thenReturn(Utils.generateReadStudioComponentReadyResult());

        Mockito.doReturn(generateCreateStudioComponentResult()).when(proxyClient)
            .injectCredentialsAndInvokeV2(any(CreateStudioComponentRequest.class), any());

        final ResourceHandlerRequest<ResourceModel> request = generateCreateHandlerRequest();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler
            .handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        Mockito.verify(proxyClient, Mockito.times(4))
            .injectCredentialsAndInvokeV2(Mockito.any(GetStudioComponentRequest.class), Mockito.any());

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
    public void handleRequest_CreateSuccess_BlankRequest() {
        Mockito.doReturn(Utils.generateReadStudioComponentReadyResult()).when(proxyClient)
            .injectCredentialsAndInvokeV2(any(GetStudioComponentRequest.class), any());

        Mockito.doReturn(generateCreateStudioComponentResult()).when(proxyClient)
            .injectCredentialsAndInvokeV2(any(CreateStudioComponentRequest.class), any());

        final ResourceHandlerRequest<ResourceModel> request = generateCreateHandlerBlankRequest();

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
    public void handleRequest_StudioComponentDeleted() {
        Mockito.doReturn(Utils.generateReadStudioComponentDeletedResult()).when(proxyClient)
            .injectCredentialsAndInvokeV2(any(GetStudioComponentRequest.class), any());
        Mockito.doReturn(generateCreateStudioComponentResult()).when(proxyClient)
            .injectCredentialsAndInvokeV2(any(CreateStudioComponentRequest.class), any());

        final ResourceHandlerRequest<ResourceModel> request = generateCreateHandlerRequest();
        assertThrows(CfnGeneralServiceException.class, () -> {
            handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);
        }, "STUDIO_COMPONENT_DELETED - Delete Complete");
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
