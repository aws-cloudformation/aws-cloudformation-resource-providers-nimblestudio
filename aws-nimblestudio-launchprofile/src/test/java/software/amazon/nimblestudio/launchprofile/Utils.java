package software.amazon.nimblestudio.launchprofile;

import org.junit.jupiter.params.provider.Arguments;
import software.amazon.awssdk.services.nimble.model.AccessDeniedException;
import software.amazon.awssdk.services.nimble.model.ConflictException;
import software.amazon.awssdk.services.nimble.model.GetLaunchProfileResponse;
import software.amazon.awssdk.services.nimble.model.InternalServerErrorException;
import software.amazon.awssdk.services.nimble.model.LaunchProfile;
import software.amazon.awssdk.services.nimble.model.LaunchProfileState;
import software.amazon.awssdk.services.nimble.model.LaunchProfileStatusCode;
import software.amazon.awssdk.services.nimble.model.ListLaunchProfilesResponse;
import software.amazon.awssdk.services.nimble.model.ResourceNotFoundException;
import software.amazon.awssdk.services.nimble.model.ServiceQuotaExceededException;
import software.amazon.awssdk.services.nimble.model.StreamConfiguration;
import software.amazon.awssdk.services.nimble.model.StreamingClipboardMode;
import software.amazon.awssdk.services.nimble.model.StreamConfigurationSessionStorage;
import software.amazon.awssdk.services.nimble.model.StreamingSessionStorageRoot;
import software.amazon.awssdk.services.nimble.model.StreamingSessionStorageMode;

import software.amazon.awssdk.services.nimble.model.StreamingInstanceType;
import software.amazon.awssdk.services.nimble.model.ThrottlingException;
import software.amazon.awssdk.services.nimble.model.ValidationException;
import software.amazon.cloudformation.exceptions.CfnAccessDeniedException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnResourceConflictException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.exceptions.CfnServiceLimitExceededException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class Utils {

    public static Map<String, String> generateTags() {
        final Map<String, String> studioTags = new HashMap<>();
        studioTags.put("key1", "val1");
        studioTags.put("key2", "val2");
        return studioTags;
    }

    public static StreamConfiguration generateStreamConfiguration() {
        return StreamConfiguration.builder()
            .clipboardMode(StreamingClipboardMode.ENABLED)
            .streamingImageIds(Collections.singletonList("imageID"))
            .ec2InstanceTypes(Collections.singletonList(StreamingInstanceType.G4_DN_2_XLARGE))
            .maxSessionLengthInMinutes(1)
            .maxStoppedSessionLengthInMinutes(2)
            .sessionStorage(
                StreamConfigurationSessionStorage
                .builder()
                .root(
                    StreamingSessionStorageRoot.builder().linux("LinuxPath").windows("WindowsPath").build()
                )
                .mode(
                        Collections.singletonList(StreamingSessionStorageMode.UPLOAD)
                )
                .build()
            )
            .build();
    }

    static GetLaunchProfileResponse generateGetLaunchProfileResponse(final LaunchProfileState state) {
        return GetLaunchProfileResponse.builder()
            .launchProfile(generateLaunchProfile(state))
            .build();
    }

    static ResourceModel generateGetLaunchProfileResponseModel() {
        return ResourceModel.builder()
            .launchProfileId("launchProfileId")
            .description("For bob")
            .name("launchProfileName")
            .ec2SubnetIds(Collections.singletonList("subnet1"))
            .streamConfiguration(Translator.toModelStreamConfiguration(generateStreamConfiguration()))
            .studioId("studioId")
            .studioComponentIds(Collections.singletonList("studioComponentId"))
            .launchProfileProtocolVersions(Collections.singletonList("launchProfileProtocolVersion"))
            .tags(Utils.generateTags())
            .build();
    }

    static List<ResourceModel> generateListLaunchProfilesResponseModel(final String studioId) {
        return Collections.singletonList(
            ResourceModel.builder()
                .launchProfileId("launchProfileId")
                .description("For bob")
                .name("launchProfileName")
                .ec2SubnetIds(Collections.singletonList("subnet1"))
                .streamConfiguration(Translator.toModelStreamConfiguration(generateStreamConfiguration()))
                .studioComponentIds(Collections.singletonList("studioComponentId"))
                .launchProfileProtocolVersions(Collections.singletonList("launchProfileProtocolVersion"))
                .studioId(studioId)
                .tags(Utils.generateTags())
                .build());
    }

    static LaunchProfile generateLaunchProfile(final LaunchProfileState state) {
        return LaunchProfile.builder()
            .launchProfileId("launchProfileId")
            .createdAt(Instant.EPOCH)
            .createdBy("Bob")
            .description("For bob")
            .name("launchProfileName")
            .state(state)
            .statusCode(LaunchProfileStatusCode.LAUNCH_PROFILE_CREATED)
            .statusMessage("Ready!")
            .ec2SubnetIds(Collections.singletonList("subnet1"))
            .streamConfiguration(generateStreamConfiguration())
            .studioComponentIds(Collections.singletonList("studioComponentId"))
            .launchProfileProtocolVersions(Collections.singletonList("launchProfileProtocolVersion"))
            .updatedAt(Instant.EPOCH)
            .updatedBy("Bob")
            .tags(Utils.generateTags())
            .build();
    }

    static ListLaunchProfilesResponse generateListLaunchProfilesResponse(final LaunchProfileState state) {
        return ListLaunchProfilesResponse.builder()
            .launchProfiles(Collections.singletonList(generateLaunchProfile(state)))
            .build();
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
