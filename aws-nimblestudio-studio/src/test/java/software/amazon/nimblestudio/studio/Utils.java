package software.amazon.nimblestudio.studio;

import org.junit.jupiter.params.provider.Arguments;
import software.amazon.awssdk.services.nimble.model.GetStudioResponse;
import software.amazon.awssdk.services.nimble.model.Studio;
import software.amazon.awssdk.services.nimble.model.StudioEncryptionConfiguration;
import software.amazon.awssdk.services.nimble.model.StudioEncryptionConfigurationKeyType;
import software.amazon.awssdk.services.nimble.model.StudioState;

import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnAccessDeniedException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.exceptions.CfnServiceLimitExceededException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;
import software.amazon.cloudformation.exceptions.CfnResourceConflictException;
import software.amazon.awssdk.services.nimble.model.AccessDeniedException;
import software.amazon.awssdk.services.nimble.model.ConflictException;
import software.amazon.awssdk.services.nimble.model.InternalServerErrorException;
import software.amazon.awssdk.services.nimble.model.ResourceNotFoundException;
import software.amazon.awssdk.services.nimble.model.ServiceQuotaExceededException;
import software.amazon.awssdk.services.nimble.model.ThrottlingException;
import software.amazon.awssdk.services.nimble.model.ValidationException;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class Utils {

    private static final Instant timestamp = Instant.ofEpochSecond(1);

    public static Map<String, String> generateTags() {
        final Map<String, String> studioTags = new HashMap<>();
        studioTags.put("key1", "val1");
        studioTags.put("key2", "val2");
        return studioTags;
    }

    public static GetStudioResponse generateReadStudioCreatingResult() {
        return GetStudioResponse.builder()
            .studio(Studio.builder()
                .adminRoleArn("aGIAMARN")
                .createdAt(timestamp)
                .displayName("CreateStudioDisplayName")
                .homeRegion("us-west-2")
                .ssoClientId("SsoClientId")
                .state(StudioState.CREATE_IN_PROGRESS)
                .statusCode("STUDIO_READY")
                .statusMessage("Create Complete")
                .studioId("id")
                .studioName("CreateStudioName")
                .studioUrl("studiourl")
                .updatedAt(timestamp)
                .userRoleArn("uGIAMARN")
                .tags(generateTags())
                .studioEncryptionConfiguration(StudioEncryptionConfiguration.builder()
                    .keyArn("testKeyArn")
                    .keyType(StudioEncryptionConfigurationKeyType.AWS_OWNED_KEY.toString())
                    .build())
                .build())
            .build();
    }

    public static GetStudioResponse generateReadStudioReadyResult() {
        return GetStudioResponse.builder()
            .studio(Studio.builder()
                .adminRoleArn("aGIAMARN")
                .createdAt(timestamp)
                .displayName("CreateStudioDisplayName")
                .homeRegion("us-west-2")
                .ssoClientId("SsoClientId")
                .state(StudioState.READY)
                .statusCode("STUDIO_READY")
                .statusMessage("Create Complete")
                .studioId("id")
                .studioName("CreateStudioName")
                .studioUrl("studiourl")
                .updatedAt(timestamp)
                .userRoleArn("uGIAMARN")
                .tags(generateTags())
                .studioEncryptionConfiguration(StudioEncryptionConfiguration.builder()
                    .keyArn("testKeyArn")
                    .keyType(StudioEncryptionConfigurationKeyType.AWS_OWNED_KEY.toString())
                    .build())
                .build())
            .build();
    }

    public static GetStudioResponse generateReadStudioUpdatingResult() {
        return GetStudioResponse.builder()
            .studio(Studio.builder()
                .adminRoleArn("aGIAMARN")
                .createdAt(timestamp)
                .displayName("CreateStudioDisplayName")
                .homeRegion("us-west-2")
                .ssoClientId("SsoClientId")
                .state(StudioState.UPDATE_IN_PROGRESS)
                .statusCode("STUDIO_READY")
                .statusMessage("Create Complete")
                .studioId("id")
                .studioName("UpdateStudioName")
                .studioUrl("studiourl")
                .updatedAt(timestamp)
                .userRoleArn("uGIAMARN")
                .tags(generateTags())
                .studioEncryptionConfiguration(StudioEncryptionConfiguration.builder()
                    .keyArn("testKeyArn")
                    .keyType(StudioEncryptionConfigurationKeyType.AWS_OWNED_KEY.toString())
                    .build())
                .build())
            .build();
    }

    public static GetStudioResponse generateReadStudioUpdatedResult() {
        return GetStudioResponse.builder()
            .studio(Studio.builder()
                .adminRoleArn("aGIAMARN")
                .createdAt(timestamp)
                .displayName("UpdateStudioDisplayName")
                .homeRegion("us-west-2")
                .ssoClientId("SsoClientId")
                .state(StudioState.READY)
                .statusCode("STUDIO_READY")
                .statusMessage("Update Complete")
                .studioId("id")
                .studioName("UpdateStudioName")
                .studioUrl("studiourl")
                .updatedAt(timestamp)
                .userRoleArn("uGIAMARN")
                .tags(generateTags())
                .studioEncryptionConfiguration(StudioEncryptionConfiguration.builder()
                    .keyArn("testKeyArn")
                    .keyType(StudioEncryptionConfigurationKeyType.AWS_OWNED_KEY.toString())
                    .build())
                .build())
            .build();
    }

    public static GetStudioResponse generateReadStudioDeletingResult() {
        return GetStudioResponse.builder()
            .studio(Studio.builder()
                .adminRoleArn("aGIAMARN")
                .createdAt(timestamp)
                .displayName("CreateStudioDisplayName")
                .homeRegion("us-west-2")
                .ssoClientId("SsoClientId")
                .state(StudioState.DELETE_IN_PROGRESS)
                .statusCode("STUDIO_DELETED")
                .statusMessage("Delete Complete")
                .studioId("id")
                .studioName("CreateStudioName")
                .studioUrl("studiourl")
                .updatedAt(timestamp)
                .userRoleArn("uGIAMARN")
                .tags(generateTags())
                .studioEncryptionConfiguration(StudioEncryptionConfiguration.builder()
                    .keyArn("testKeyArn")
                    .keyType(StudioEncryptionConfigurationKeyType.AWS_OWNED_KEY.toString())
                    .build())
                .build())
            .build();
    }

    public static GetStudioResponse generateReadStudioDeletedResult() {
        return GetStudioResponse.builder()
            .studio(Studio.builder()
                .adminRoleArn("aGIAMARN")
                .createdAt(timestamp)
                .displayName("CreateStudioDisplayName")
                .homeRegion("us-west-2")
                .ssoClientId("SsoClientId")
                .state(StudioState.DELETED)
                .statusCode("STUDIO_DELETED")
                .statusMessage("Delete Complete")
                .studioId("id")
                .studioName("CreateStudioName")
                .studioUrl("studiourl")
                .updatedAt(timestamp)
                .userRoleArn("uGIAMARN")
                .tags(generateTags())
                .studioEncryptionConfiguration(StudioEncryptionConfiguration.builder()
                    .keyArn("testKeyArn")
                    .keyType(StudioEncryptionConfigurationKeyType.AWS_OWNED_KEY.toString())
                    .build())
                .build())
            .build();
    }

    public static List<Studio> getStudios() {
        return Arrays.asList(
            Studio.builder()
                .adminRoleArn("aGIAMARN1")
                .displayName("CreateStudioDisplayName1")
                .studioName("CreateStudioName1")
                .userRoleArn("uGIAMARN1")
                .createdAt(timestamp)
                .homeRegion("us-west-2")
                .ssoClientId("SsoClientId1")
                .state(StudioState.READY)
                .statusCode("STUDIO_READY")
                .statusMessage("Create Complete")
                .studioId("id1")
                .studioUrl("studiourl1")
                .updatedAt(timestamp)
                .tags(generateTags())
                .studioEncryptionConfiguration(StudioEncryptionConfiguration.builder()
                        .keyArn("testKeyArn")
                        .keyType(StudioEncryptionConfigurationKeyType.AWS_OWNED_KEY.toString())
                        .build())
                .build(),
            Studio
                .builder()
                .adminRoleArn("aGIAMARN2")
                .displayName("CreateStudioDisplayName2")
                .studioName("CreateStudioName2")
                .userRoleArn("uGIAMARN2")
                .createdAt(timestamp)
                .homeRegion("us-west-2")
                .ssoClientId("SsoClientId2")
                .state(StudioState.READY)
                .statusCode("STUDIO_READY")
                .statusMessage("Create Complete")
                .studioId("id2")
                .studioUrl("studiourl2")
                .updatedAt(timestamp)
                .tags(generateTags())
                .studioEncryptionConfiguration(StudioEncryptionConfiguration.builder()
                        .keyArn("testKeyArn")
                        .keyType(StudioEncryptionConfigurationKeyType.AWS_OWNED_KEY.toString())
                        .build())
                .build());
    }

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
}
