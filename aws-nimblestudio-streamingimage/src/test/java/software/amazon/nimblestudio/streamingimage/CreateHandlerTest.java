package software.amazon.nimblestudio.streamingimage;

import software.amazon.awssdk.services.nimble.NimbleClient;
import software.amazon.awssdk.services.nimble.model.CreateStreamingImageRequest;
import software.amazon.awssdk.services.nimble.model.CreateStreamingImageResponse;
import software.amazon.awssdk.services.nimble.model.GetStreamingImageRequest;
import software.amazon.awssdk.services.nimble.model.StreamingImage;
import software.amazon.awssdk.services.nimble.model.StreamingImageState;
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

    @BeforeEach
    public void setup() {
        proxy = getAmazonWebServicesClientProxy();
        nimbleClient = mock(NimbleClient.class);
        when(proxyClient.client()).thenReturn(nimbleClient);
    }

    static CreateStreamingImageResponse generateCreateStreamingImageResponse() {
        return CreateStreamingImageResponse.builder()
            .streamingImage(StreamingImage.builder()
                .streamingImageId("streamingImageId")
                .build())
            .build();
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        // Mock request
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .clientRequestToken("clientToken")
            .desiredResourceState(ResourceModel.builder()
                .studioId("studioId")
                .streamingImageId("streamingImageId")
                .ec2ImageId("ec2ImageId")
                .name("imageName")
                .description("my image")
                .tags(Utils.getTestTags())
                .build()
            ).build();

        // Mock responses, first for CREATE request then for GET request
        Mockito.doReturn(generateCreateStreamingImageResponse())
            .when(proxyClient).injectCredentialsAndInvokeV2(any(CreateStreamingImageRequest.class), any());
        Mockito.doReturn(Utils.generateGetStreamingImageResponse(StreamingImageState.READY))
            .when(proxyClient).injectCredentialsAndInvokeV2(any(GetStreamingImageRequest.class), any());

        // Make the CREATE request
        final ProgressEvent<ResourceModel, CallbackContext> response = new CreateHandler()
            .handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        // GET should get called once while stabilizing and once for the final READ
        Mockito.verify(proxyClient, Mockito.times(2))
            .injectCredentialsAndInvokeV2(any(GetStreamingImageRequest.class), any());

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(
            Utils.generateResourceModel("streamingImageId"));
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_CreateSuccess_Stabilization() {
        // Mock request
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .clientRequestToken("clientToken")
            .desiredResourceState(ResourceModel.builder()
                .studioId("studioId")
                .ec2ImageId("ec2ImageId")
                .name("imageName")
                .description("my image")
                .build())
            .build();

        // During stabilization, we will return the "CREATING" status a couple of times before sending a "READY"
        // response
        Mockito.when(proxyClient.injectCredentialsAndInvokeV2(any(GetStreamingImageRequest.class), any()))
            .thenReturn(Utils.generateGetStreamingImageResponse(StreamingImageState.CREATE_IN_PROGRESS))
            .thenReturn(Utils.generateGetStreamingImageResponse(StreamingImageState.CREATE_IN_PROGRESS))
            .thenReturn(Utils.generateGetStreamingImageResponse(StreamingImageState.READY))
            .thenReturn(Utils.generateGetStreamingImageResponse(StreamingImageState.READY));

        Mockito.doReturn(generateCreateStreamingImageResponse())
            .when(proxyClient).injectCredentialsAndInvokeV2(any(CreateStreamingImageRequest.class), any());

        // Make the CREATE request
        final ProgressEvent<ResourceModel, CallbackContext> response = new CreateHandler()
            .handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        Mockito.verify(proxyClient, Mockito.times(4))
            .injectCredentialsAndInvokeV2(any(GetStreamingImageRequest.class), any());

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(
            Utils.generateResourceModel("streamingImageId"));
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    static Stream<Arguments> testParamsForException() {
        return Utils.parametersForExceptionTests();
    }

    @ParameterizedTest
    @MethodSource("testParamsForException")
    public void handleRequest_Failed_Exception(final Class<Throwable> thrownException,
                                               final Class<Throwable> expectedException) {
        Mockito.doThrow(thrownException).when(proxyClient).injectCredentialsAndInvokeV2(any(), any());

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .clientRequestToken("clientToken")
            .desiredResourceState(ResourceModel.builder()
                .ec2ImageId("ec2ImageId")
                .name("imageName")
                .description("my image")
                .build())
            .build();

        assertThrows(expectedException, () -> {
            new CreateHandler().handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);
        });
    }
}
