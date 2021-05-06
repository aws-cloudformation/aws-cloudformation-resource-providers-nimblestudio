package software.amazon.nimblestudio.studiocomponent;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.services.nimble.NimbleClient;
import software.amazon.awssdk.services.nimble.model.GetStudioComponentRequest;
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
import software.amazon.awssdk.services.nimble.model.UpdateStudioComponentRequest;
import software.amazon.awssdk.services.nimble.model.UpdateStudioComponentResponse;
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

    private UpdateStudioComponentResponse generateUpdateStudioComponentResult() {
        return UpdateStudioComponentResponse.builder()
            .studioComponent(
                StudioComponent.builder()
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
                            .build()))
                    .name("studioComponent")
                    .scriptParameters(new ArrayList<>())
                    .ec2SecurityGroupIds(new ArrayList<>())
                    .state(StudioComponentState.UPDATE_IN_PROGRESS)
                    .statusCode(StudioComponentStatusCode.STUDIO_COMPONENT_UPDATE_IN_PROGRESS)
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

    private ResourceHandlerRequest<ResourceModel> generateUpdateHandlerRequest() {
        final ResourceModel model = ResourceModel.builder()
            .configuration(software.amazon.nimblestudio.studiocomponent.StudioComponentConfiguration.builder()
                .activeDirectoryConfiguration(software.amazon.nimblestudio.studiocomponent.ActiveDirectoryConfiguration
                    .builder()
                    .computerAttributes(new ArrayList<>())
                    .directoryId("did")
                    .organizationalUnitDistinguishedName("oname")
                    .build())
                .computeFarmConfiguration(software.amazon.nimblestudio.studiocomponent.ComputeFarmConfiguration
                    .builder()
                    .endpoint("endpoint")
                    .activeDirectoryUser("activeDirectoryUser")
                    .build())
                .licenseServiceConfiguration(software.amazon.nimblestudio.studiocomponent.LicenseServiceConfiguration
                    .builder()
                    .endpoint("endpoint")
                    .build())
                .sharedFileSystemConfiguration(software.amazon.nimblestudio.studiocomponent.SharedFileSystemConfiguration
                    .builder()
                    .windowsMountDrive("windowsMountDrive")
                    .shareName("shareName")
                    .linuxMountPoint("linuxMountPoint")
                    .fileSystemId("fileSystemId")
                    .endpoint("endpoint")
                    .build())
                .build())
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
                    .build()))
            .name("studioComponent")
            .scriptParameters(new ArrayList<>())
            .ec2SecurityGroupIds(new ArrayList<>())
            .studioComponentId("studioComponentid")
            .studioId("id")
            .subtype(StudioComponentSubtype.AMAZON_FSX_FOR_WINDOWS.toString())
            .type(StudioComponentType.SHARED_FILE_SYSTEM.toString())
            .tags(Utils.generateTags())
            .build();
        return ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(model)
            .clientRequestToken("clientToken")
            .build();
    }

    private ResourceHandlerRequest<ResourceModel> generateUpdateHandlerBlankRequest() {
        return ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(ResourceModel.builder()
                .studioId("id")
                .name("studioComponent")
                .tags(Utils.generateTags())
                .build())
            .clientRequestToken("clientToken")
            .build();
    }

    private  ResourceModel generateExpectedResponse() {
        return ResourceModel.builder()
            .configuration(software.amazon.nimblestudio.studiocomponent.StudioComponentConfiguration.builder()
                .activeDirectoryConfiguration(software.amazon.nimblestudio.studiocomponent.ActiveDirectoryConfiguration
                    .builder()
                    .computerAttributes(new ArrayList<>())
                    .build())
                .computeFarmConfiguration(software.amazon.nimblestudio.studiocomponent.ComputeFarmConfiguration
                    .builder()
                    .build())
                .licenseServiceConfiguration(software.amazon.nimblestudio.studiocomponent.LicenseServiceConfiguration
                        .builder()
                    .build())
                .sharedFileSystemConfiguration(software.amazon.nimblestudio.studiocomponent.SharedFileSystemConfiguration
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
    public void handleRequest_UpdateSuccess_Stabilization() {
        Mockito.when(proxyClient.injectCredentialsAndInvokeV2(any(GetStudioComponentRequest.class), any()))
            .thenReturn(Utils.generateReadStudioComponentReadyResult())
            .thenReturn(Utils.generateReadStudioComponentUpdatingResult())
            .thenReturn(Utils.generateReadStudioComponentUpdatedResult());

        Mockito.doReturn(generateUpdateStudioComponentResult()).when(proxyClient)
            .injectCredentialsAndInvokeV2(any(UpdateStudioComponentRequest.class), any());

        final ProgressEvent<ResourceModel, CallbackContext> response = handler
            .handleRequest(proxy, generateUpdateHandlerRequest(), new CallbackContext(), proxyClient, logger);

        Mockito.verify(proxyClient, Mockito.times(4))
            .injectCredentialsAndInvokeV2(Mockito.any(GetStudioComponentRequest.class), Mockito.any());
        Mockito.verify(proxyClient, Mockito.times(1))
            .injectCredentialsAndInvokeV2(Mockito.any(UpdateStudioComponentRequest.class), Mockito.any());

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
    public void handleRequest_UpdateSuccess_BlankRequest() {
        Mockito.doReturn(Utils.generateReadStudioComponentUpdatedResult()).when(proxyClient)
            .injectCredentialsAndInvokeV2(any(GetStudioComponentRequest.class), any());

        Mockito.doReturn(generateUpdateStudioComponentResult()).when(proxyClient)
            .injectCredentialsAndInvokeV2(any(UpdateStudioComponentRequest.class), any());

        final ResourceHandlerRequest<ResourceModel> request = generateUpdateHandlerBlankRequest();

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
    public void handleRequest_UpdateSuccess_BlankConfiguration() {
        Mockito.doReturn(Utils.generateReadStudioComponentUpdatedResult()).when(proxyClient)
            .injectCredentialsAndInvokeV2(any(GetStudioComponentRequest.class), any());

        Mockito.doReturn(generateUpdateStudioComponentResult()).when(proxyClient)
            .injectCredentialsAndInvokeV2(any(UpdateStudioComponentRequest.class), any());

        final ResourceHandlerRequest<ResourceModel> request = generateUpdateHandlerBlankRequest();
        request.getDesiredResourceState().setConfiguration(
            software.amazon.nimblestudio.studiocomponent.StudioComponentConfiguration.builder().build());

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
            handler.handleRequest(proxy, generateUpdateHandlerRequest(), new CallbackContext(), proxyClient, logger);
        });
    }

    @ParameterizedTest
    @MethodSource("testParamsForException")
    public void handleRequest_Failed_Exception(final Class<Throwable> thrownException,
                                               final Class<Throwable> expectedException) {
        Mockito.doReturn(Utils.generateReadStudioComponentReadyResult()).when(proxyClient)
            .injectCredentialsAndInvokeV2(any(GetStudioComponentRequest.class), any());

        Mockito.doThrow(thrownException).when(proxyClient).injectCredentialsAndInvokeV2(
            any(UpdateStudioComponentRequest.class), any());

        assertThrows(expectedException, () -> {
            handler.handleRequest(proxy, generateUpdateHandlerRequest(), new CallbackContext(), proxyClient, logger);
        });
    }

}
