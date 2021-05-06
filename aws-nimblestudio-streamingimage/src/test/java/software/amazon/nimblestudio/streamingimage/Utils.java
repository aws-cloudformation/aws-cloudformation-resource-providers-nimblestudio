package software.amazon.nimblestudio.streamingimage;

import org.junit.jupiter.params.provider.Arguments;
import software.amazon.awssdk.services.nimble.model.AccessDeniedException;
import software.amazon.awssdk.services.nimble.model.ConflictException;
import software.amazon.awssdk.services.nimble.model.GetStreamingImageResponse;
import software.amazon.awssdk.services.nimble.model.InternalServerErrorException;
import software.amazon.awssdk.services.nimble.model.ResourceNotFoundException;
import software.amazon.awssdk.services.nimble.model.ServiceQuotaExceededException;
import software.amazon.awssdk.services.nimble.model.StreamingImage;
import software.amazon.awssdk.services.nimble.model.StreamingImageState;
import software.amazon.awssdk.services.nimble.model.ThrottlingException;
import software.amazon.awssdk.services.nimble.model.ValidationException;
import software.amazon.cloudformation.exceptions.CfnAccessDeniedException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnResourceConflictException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.exceptions.CfnServiceLimitExceededException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class Utils {
    static Stream<Arguments> parametersForExceptionTests() {
        return Stream.of(
            Arguments.of(AccessDeniedException.class, CfnAccessDeniedException.class),
            Arguments.of(ConflictException.class, CfnResourceConflictException.class),
            Arguments.of(InternalServerErrorException.class, CfnServiceInternalErrorException.class),
            Arguments.of(ResourceNotFoundException.class, CfnNotFoundException.class),
            Arguments.of(ServiceQuotaExceededException.class, CfnServiceLimitExceededException.class),
            Arguments.of(ThrottlingException.class, CfnThrottlingException.class),
            Arguments.of(ValidationException.class, CfnInvalidRequestException.class)
        );
    }

    static Map<String, String> getTestTags() {
        Map<String, String> tags = new HashMap<>();
        tags.put("key1", "value1");
        tags.put("key2", "value2");
        return tags;
    }

    static StreamingImage generateStreamingImage(String streamingImageId, StreamingImageState state) {
        return StreamingImage.builder()
            .streamingImageId(streamingImageId)
            .ec2ImageId("ec2ImageId")
            .name("imageName")
            .description("my image")
            .eulaIds(Arrays.asList("eula1", "eula2"))
            .encryptionConfiguration(software.amazon.awssdk.services.nimble.model.StreamingImageEncryptionConfiguration.builder()
                .keyArn("arn:::::")
                .keyType("CUSTOMER_OWNED_CMK")
                .build())
            .owner("crowest")
            .state(state.toString())
            .statusCode("statusCode")
            .statusMessage("success")
            .platform("platform")
            .tags(getTestTags())
            .build();
    }

    static GetStreamingImageResponse generateGetStreamingImageResponse(StreamingImageState state) {
        return GetStreamingImageResponse.builder()
            .streamingImage(generateStreamingImage("streamingImageId", state))
            .build();
    }

    static ResourceModel generateResourceModel(String streamingImageId) {
        return ResourceModel.builder()
            .studioId("studioId")
            .streamingImageId(streamingImageId)
            .ec2ImageId("ec2ImageId")
            .name("imageName")
            .description("my image")
            .eulaIds(Arrays.asList("eula1", "eula2"))
            .encryptionConfiguration(StreamingImageEncryptionConfiguration.builder()
                .keyArn("arn:::::")
                .keyType("CUSTOMER_OWNED_CMK")
                .build())
            .owner("crowest")
            .platform("platform")
            .tags(Utils.getTestTags())
            .build();
    }
}
