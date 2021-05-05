package software.amazon.nimblestudio.streamingimage;

import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.services.nimble.NimbleClient;
import software.amazon.awssdk.services.nimble.model.GetStreamingImageRequest;
import software.amazon.awssdk.services.nimble.model.StreamingImageState;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReadHandlerTest extends AbstractTestBase {

    @Mock
    private NimbleClient nimbleClient;

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<NimbleClient> proxyClient;

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
        when(proxyClient.client()).thenReturn(nimbleClient);
    }

    private ResourceHandlerRequest<ResourceModel> generateRequest() {
        return ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(ResourceModel.builder()
                .studioId("studioId")
                .streamingImageId("streamingImageId")
                .build())
            .build();
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        // Mock the response
        Mockito.doReturn(Utils.generateGetStreamingImageResponse(StreamingImageState.READY))
            .when(proxyClient).injectCredentialsAndInvokeV2(any(GetStreamingImageRequest.class), any());

        // Make the READ request
        final ProgressEvent<ResourceModel, CallbackContext> handlerResponse = new ReadHandler()
            .handleRequest(proxy, generateRequest(), new CallbackContext(), proxyClient, logger);

        assertThat(handlerResponse).isNotNull();
        assertThat(handlerResponse.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(handlerResponse.getCallbackContext()).isNull();
        assertThat(handlerResponse.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(handlerResponse.getResourceModel()).isEqualTo(
            Utils.generateResourceModel("streamingImageId"));
        assertThat(handlerResponse.getResourceModels()).isNull();
        assertThat(handlerResponse.getMessage()).isNull();
        assertThat(handlerResponse.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_AlreadyDeleted_Exception() {
        Mockito.when(proxyClient.injectCredentialsAndInvokeV2(any(GetStreamingImageRequest.class), any()))
            .thenReturn(Utils.generateGetStreamingImageResponse(StreamingImageState.DELETED));

        assertThrows(CfnNotFoundException.class, () -> {
            new ReadHandler().handleRequest(proxy, generateRequest(), new CallbackContext(), proxyClient, logger);
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
            new ReadHandler().handleRequest(proxy, generateRequest(), new CallbackContext(), proxyClient, logger);
        });
    }
}
