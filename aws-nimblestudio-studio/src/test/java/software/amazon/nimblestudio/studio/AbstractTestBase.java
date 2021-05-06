package software.amazon.nimblestudio.studio;

import java.time.Duration;

import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Credentials;
import software.amazon.cloudformation.proxy.LoggerProxy;

public class AbstractTestBase {

    protected static final Credentials MOCK_CREDENTIALS;
    protected static final LoggerProxy logger;

    static {
        MOCK_CREDENTIALS = new Credentials("accessKey", "secretKey", "token");
        logger = new LoggerProxy();
    }

    static AmazonWebServicesClientProxy getAmazonWebServicesClientProxy() {
        final int remainingTimeToExecuteInMillis = 600;
        return new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS,
            () -> Duration.ofSeconds(remainingTimeToExecuteInMillis).toMillis());
    }
}
