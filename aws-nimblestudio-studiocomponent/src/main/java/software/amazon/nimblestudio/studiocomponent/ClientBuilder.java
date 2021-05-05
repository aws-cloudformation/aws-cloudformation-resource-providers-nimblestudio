package software.amazon.nimblestudio.studiocomponent;

import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.retry.conditions.RetryCondition;
import software.amazon.awssdk.services.nimble.NimbleClient;
import software.amazon.cloudformation.LambdaWrapper;

public class ClientBuilder {

    private static final int NUM_RETRY = 6;

    private ClientBuilder() {
    }

    private static final RetryPolicy RETRY_POLICY = RetryPolicy
        .builder()
        .numRetries(NUM_RETRY)
        .retryCondition(RetryCondition.defaultRetryCondition())
        .build();

    public static NimbleClient getClient() {

        return NimbleClient.builder().httpClient(LambdaWrapper.HTTP_CLIENT)
            .overrideConfiguration(ClientOverrideConfiguration
                .builder()
                .retryPolicy(RETRY_POLICY)
                .build())
            .build();
    }
}
