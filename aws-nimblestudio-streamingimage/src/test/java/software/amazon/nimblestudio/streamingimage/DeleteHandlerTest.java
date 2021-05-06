package software.amazon.nimblestudio.streamingimage;

import software.amazon.awssdk.services.nimble.NimbleClient;
import software.amazon.awssdk.services.nimble.model.DeleteStreamingImageRequest;
import software.amazon.awssdk.services.nimble.model.DeleteStreamingImageResponse;
import software.amazon.awssdk.services.nimble.model.GetStreamingImageRequest;
import software.amazon.awssdk.services.nimble.model.StreamingImageState;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
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
public class DeleteHandlerTest extends AbstractTestBase {
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

    private ResourceHandlerRequest<ResourceModel> generateRequest() {
        return ResourceHandlerRequest.<ResourceModel>builder()
            .clientRequestToken("clientToken")
            .desiredResourceState(ResourceModel.builder()
                .studioId("studioId")
                .streamingImageId("streamingImageId")
                .build())
            .build();
    }

    @Test
    public void handleRequest_DeleteSuccess_Stabilization() {
        // During stabilization, return a "DELETING" status a couple of times before sending a "DELETED" response
        Mockito.when(proxyClient.injectCredentialsAndInvokeV2(any(GetStreamingImageRequest.class), any()))
            .thenReturn(Utils.generateGetStreamingImageResponse(StreamingImageState.READY))
            .thenReturn(Utils.generateGetStreamingImageResponse(StreamingImageState.DELETE_IN_PROGRESS))
            .thenReturn(Utils.generateGetStreamingImageResponse(StreamingImageState.DELETED));
        Mockito.doReturn(DeleteStreamingImageResponse.builder().build())
            .when(proxyClient).injectCredentialsAndInvokeV2(any(DeleteStreamingImageRequest.class), any());

        // Make the DELETE request
        final ProgressEvent<ResourceModel, CallbackContext> response = new DeleteHandler()
            .handleRequest(proxy, generateRequest(), new CallbackContext(), proxyClient, logger);

        Mockito.verify(proxyClient, Mockito.times(3))
                .injectCredentialsAndInvokeV2(Mockito.any(GetStreamingImageRequest.class), Mockito.any());
        Mockito.verify(proxyClient, Mockito.times(1))
                .injectCredentialsAndInvokeV2(Mockito.any(DeleteStreamingImageRequest.class), Mockito.any());
        System.out.println(response);
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
    public void handleRequest_AlreadyDeleted_Exception() {
        Mockito.when(proxyClient.injectCredentialsAndInvokeV2(any(GetStreamingImageRequest.class), any()))
            .thenReturn(Utils.generateGetStreamingImageResponse(StreamingImageState.DELETED));

        assertThrows(CfnNotFoundException.class, () -> {
            new DeleteHandler().handleRequest(proxy, generateRequest(), new CallbackContext(), proxyClient, logger);
        });
    }

    static Stream<Arguments> testParamsForException() {
        return Utils.parametersForExceptionTests();
    }

    @ParameterizedTest
    @MethodSource("testParamsForException")
    public void handleRequest_Failed_Exception(final Class<Throwable> thrownException,
                                               final Class<Throwable> expectedException) {
        Mockito.doThrow(thrownException).when(proxyClient).injectCredentialsAndInvokeV2(any(), any());

        assertThrows(expectedException, () -> {
            new DeleteHandler().handleRequest(proxy, generateRequest(), new CallbackContext(), proxyClient, logger);
        });
    }
}
