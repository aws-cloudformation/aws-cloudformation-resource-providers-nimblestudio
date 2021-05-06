package software.amazon.nimblestudio.studio;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.services.nimble.NimbleClient;
import software.amazon.awssdk.services.nimble.model.ListStudiosRequest;
import software.amazon.awssdk.services.nimble.model.ListStudiosResponse;
import software.amazon.awssdk.services.nimble.model.StudioEncryptionConfigurationKeyType;
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
import java.util.Arrays;
import java.util.List;
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

    private ListStudiosResponse generateListStudioResult() {
        return ListStudiosResponse.builder()
            .studios(Utils.getStudios())
            .nextToken("1231j091j23")
            .build();
    }

    private ResourceHandlerRequest<ResourceModel> generateListHandlerRequest() {
        return ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(ResourceModel.builder().build())
            .nextToken("09018023nj").build();
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        Mockito.doReturn(generateListStudioResult()).when(proxyClient)
            .injectCredentialsAndInvokeV2(any(ListStudiosRequest.class), any());

        final ProgressEvent<ResourceModel, CallbackContext> response = handler
            .handleRequest(proxy, generateListHandlerRequest(), new CallbackContext(), proxyClient, logger);

        List<ResourceModel> expectedModels = Arrays.asList(
            ResourceModel.builder()
                .adminRoleArn("aGIAMARN1")
                .displayName("CreateStudioDisplayName1")
                .studioName("CreateStudioName1")
                .userRoleArn("uGIAMARN1")
                .homeRegion("us-west-2")
                .ssoClientId("SsoClientId1")
                .studioId("id1")
                .studioUrl("studiourl1")
                .tags(Utils.generateTags())
                .studioEncryptionConfiguration(software.amazon.nimblestudio.studio.StudioEncryptionConfiguration.builder()
                    .keyArn("testKeyArn")
                    .keyType(StudioEncryptionConfigurationKeyType.AWS_OWNED_KEY.toString())
                    .build())
                .build(),
            ResourceModel.builder()
                .adminRoleArn("aGIAMARN2")
                .displayName("CreateStudioDisplayName2")
                .studioName("CreateStudioName2")
                .userRoleArn("uGIAMARN2")
                .homeRegion("us-west-2")
                .ssoClientId("SsoClientId2")
                .studioId("id2")
                .studioUrl("studiourl2")
                .tags(Utils.generateTags())
                .studioEncryptionConfiguration(software.amazon.nimblestudio.studio.StudioEncryptionConfiguration.builder()
                    .keyArn("testKeyArn")
                    .keyType(StudioEncryptionConfigurationKeyType.AWS_OWNED_KEY.toString())
                    .build())
                .build()
        );
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getNextToken()).isEqualTo("1231j091j23");
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNotNull();
        assertThat(response.getResourceModels()).isEqualTo(expectedModels);
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
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
