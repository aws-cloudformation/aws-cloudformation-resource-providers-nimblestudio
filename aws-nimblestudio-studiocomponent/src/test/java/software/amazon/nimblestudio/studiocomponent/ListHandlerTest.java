package software.amazon.nimblestudio.studiocomponent;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.services.nimble.NimbleClient;
import software.amazon.awssdk.services.nimble.model.LaunchProfilePlatform;
import software.amazon.awssdk.services.nimble.model.ListStudioComponentsRequest;
import software.amazon.awssdk.services.nimble.model.ListStudioComponentsResponse;
import software.amazon.awssdk.services.nimble.model.StudioComponent;
import software.amazon.awssdk.services.nimble.model.StudioComponentInitializationScriptRunContext;
import software.amazon.awssdk.services.nimble.model.StudioComponentState;
import software.amazon.awssdk.services.nimble.model.StudioComponentSubtype;
import software.amazon.awssdk.services.nimble.model.StudioComponentType;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class ListHandlerTest extends AbstractTestBase {

    @Mock
    private NimbleClient nimbleClient;

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<NimbleClient> proxyClient;

    private ListHandler handler;

    @BeforeEach
    public void setup() {
        proxy = getAmazonWebServicesClientProxy();
        nimbleClient = mock(NimbleClient.class);
        handler = new ListHandler();
        when(proxyClient.client()).thenReturn(nimbleClient);
    }

    static Stream<Arguments> testParamsForException() {
        return Utils.parametersForExceptionTests();
    }

    private final static ResourceModel RESOURCE_MODEL_1 = ResourceModel.builder()
        .configuration(StudioComponentConfiguration.builder()
            .activeDirectoryConfiguration(
                ActiveDirectoryConfiguration.builder()
                    .computerAttributes(new ArrayList<>())
                    .build())
            .computeFarmConfiguration(ComputeFarmConfiguration.builder().build())
            .licenseServiceConfiguration(LicenseServiceConfiguration.builder().build())
            .sharedFileSystemConfiguration(SharedFileSystemConfiguration.builder().build())
            .build())
        .description("test1")
        .initializationScripts(Arrays.asList(
            StudioComponentInitializationScript.builder()
                .script("script1")
                .runContext(StudioComponentInitializationScriptRunContext.SYSTEM_INITIALIZATION.toString())
                .platform(LaunchProfilePlatform.WINDOWS.toString())
                .launchProfileProtocolVersion("2021-03-31")
                .build(),
            StudioComponentInitializationScript.builder()
                .script("script2")
                .runContext(StudioComponentInitializationScriptRunContext.USER_INITIALIZATION.toString())
                .platform(LaunchProfilePlatform.LINUX.toString())
                .launchProfileProtocolVersion("2021-03-31")
                .build()))
        .name("studioComponent1")
        .scriptParameters(new ArrayList<>())
        .ec2SecurityGroupIds(new ArrayList<>())
        .studioComponentId("studioComponentId1")
        .subtype(StudioComponentSubtype.AMAZON_FSX_FOR_WINDOWS.toString())
        .type(StudioComponentType.COMPUTE_FARM.toString())
        .tags(Utils.generateTags())
        .build();

    private final static ResourceModel RESOURCE_MODEL_2 = ResourceModel.builder()
        .configuration(StudioComponentConfiguration.builder()
                .activeDirectoryConfiguration(
                        ActiveDirectoryConfiguration.builder()
                                .computerAttributes(new ArrayList<>())
                                .build())
                .computeFarmConfiguration(ComputeFarmConfiguration.builder().build())
                .licenseServiceConfiguration(LicenseServiceConfiguration.builder().build())
                .sharedFileSystemConfiguration(SharedFileSystemConfiguration.builder().build())
                .build())
        .description("test2")
            .initializationScripts(Arrays.asList(
                    StudioComponentInitializationScript.builder()
                            .script("script1")
                            .runContext(StudioComponentInitializationScriptRunContext.SYSTEM_INITIALIZATION.toString())
                            .platform(LaunchProfilePlatform.WINDOWS.toString())
                            .launchProfileProtocolVersion("2021-03-31")
                            .build(),
                    StudioComponentInitializationScript.builder()
                            .script("script2")
                            .runContext(StudioComponentInitializationScriptRunContext.USER_INITIALIZATION.toString())
                            .platform(LaunchProfilePlatform.LINUX.toString())
                            .launchProfileProtocolVersion("2021-03-31")
                            .build()))
        .name("studioComponent2")
        .scriptParameters(new ArrayList<>())
        .ec2SecurityGroupIds(new ArrayList<>())
        .studioComponentId("studioComponentId2")
        .subtype(StudioComponentSubtype.AMAZON_FSX_FOR_WINDOWS.toString())
        .type(StudioComponentType.COMPUTE_FARM.toString())
        .tags(Utils.generateTags())
        .build();

    private ResourceHandlerRequest<ResourceModel> generateListHandlerRequest() {
        final ResourceModel model = ResourceModel.builder().build();
        return ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(model)
            .nextToken("09018023nj").build();
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        ListStudioComponentsResponse listStudioComponentsResponse = ListStudioComponentsResponse.builder()
            .studioComponents(Utils.getStudioComponents())
            .nextToken("1231j091j23")
            .build();
        Mockito.doReturn(listStudioComponentsResponse).when(proxyClient)
            .injectCredentialsAndInvokeV2(any(ListStudioComponentsRequest.class), any());

        final ListHandler handler = new ListHandler();

        final ResourceHandlerRequest<ResourceModel> request = generateListHandlerRequest();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler
            .handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getNextToken()).isEqualTo("1231j091j23");
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNotNull();
        assertThat(response.getResourceModels()).isEqualTo(Arrays.asList(
            RESOURCE_MODEL_1,
            RESOURCE_MODEL_2
        ));
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_IgnoreStates() {
        ListStudioComponentsResponse listStudioComponentsResponse = ListStudioComponentsResponse.builder()
            .studioComponents(Arrays.asList(
                StudioComponent.builder().state(StudioComponentState.DELETED).build(),
                StudioComponent.builder().state(StudioComponentState.CREATE_FAILED).build()))
            .nextToken("1231j091j23")
            .build();

        Mockito.doReturn(listStudioComponentsResponse).when(proxyClient)
            .injectCredentialsAndInvokeV2(any(ListStudioComponentsRequest.class), any());

        final ListHandler handler = new ListHandler();

        final ResourceHandlerRequest<ResourceModel> request = generateListHandlerRequest();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler
            .handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response.getResourceModels().size()).isEqualTo(0);
    }

    @ParameterizedTest
    @MethodSource("testParamsForException")
    public void handleRequest_Failed_Exception(final Class<Throwable> thrownException,
                                               final Class<Throwable> expectedException) {
        Mockito.doThrow(thrownException).when(proxyClient).injectCredentialsAndInvokeV2(any(), any());

        assertThrows(expectedException, () -> {
            handler.handleRequest(proxy, generateListHandlerRequest(), new CallbackContext(), proxyClient, logger);
        });
    }
}
