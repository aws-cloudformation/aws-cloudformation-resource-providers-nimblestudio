package software.amazon.nimblestudio.streamingimage;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import software.amazon.awssdk.services.nimble.NimbleClient;
import software.amazon.awssdk.services.nimble.model.ListStreamingImagesRequest;
import software.amazon.awssdk.services.nimble.model.ListStreamingImagesResponse;
import software.amazon.awssdk.services.nimble.model.StreamingImageState;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ListHandlerTest extends AbstractTestBase {
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

    @Test
    public void handleRequest_SimpleSuccess() {
        // Mock request
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(ResourceModel.builder()
                .studioId("studioId")
                .build())
            .build();

        // Mock the response
        Mockito.doReturn(ListStreamingImagesResponse.builder()
                .streamingImages(Arrays.asList(
                    Utils.generateStreamingImage("streamingImage1", StreamingImageState.READY),
                    Utils.generateStreamingImage("streamingImage2", StreamingImageState.CREATE_IN_PROGRESS)
                )).build())
            .when(proxyClient).injectCredentialsAndInvokeV2(any(ListStreamingImagesRequest.class), any());

        // Make the LIST request
        final ProgressEvent<ResourceModel, CallbackContext> response = new ListHandler()
            .handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).hasSize(2);
        assertThat(response.getResourceModels().get(0)).isEqualTo(
            Utils.generateResourceModel("streamingImage1"));
        assertThat(response.getResourceModels().get(1)).isEqualTo(
            Utils.generateResourceModel("streamingImage2"));
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
            .desiredResourceState(ResourceModel.builder()
                .studioId("studioId")
                .build())
            .build();

        assertThrows(expectedException, () -> {
            new ListHandler().handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);
        });
    }
}
