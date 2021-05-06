package software.amazon.nimblestudio.studiocomponent;

import org.junit.jupiter.params.provider.Arguments;
import software.amazon.awssdk.services.nimble.model.*;

import software.amazon.awssdk.services.nimble.model.ActiveDirectoryConfiguration;
import software.amazon.awssdk.services.nimble.model.ComputeFarmConfiguration;
import software.amazon.awssdk.services.nimble.model.LicenseServiceConfiguration;
import software.amazon.awssdk.services.nimble.model.SharedFileSystemConfiguration;
import software.amazon.awssdk.services.nimble.model.StudioComponentInitializationScript;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnAccessDeniedException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.exceptions.CfnServiceLimitExceededException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;
import software.amazon.cloudformation.exceptions.CfnResourceConflictException;

import java.time.Instant;
import java.util.ArrayList;
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

    public static GetStudioComponentResponse generateReadStudioComponentCreatingResult() {
        return GetStudioComponentResponse.builder()
            .studioComponent(StudioComponent.builder()
                .configuration(software.amazon.awssdk.services.nimble.model.StudioComponentConfiguration.builder()
                    .activeDirectoryConfiguration(ActiveDirectoryConfiguration.builder().build())
                    .computeFarmConfiguration(ComputeFarmConfiguration.builder().build())
                    .licenseServiceConfiguration(LicenseServiceConfiguration.builder().build())
                    .sharedFileSystemConfiguration(SharedFileSystemConfiguration.builder().build())
                    .build())
                .createdAt(timestamp)
                .createdBy("Fuzzy")
                .description("test")
                .initializationScripts(Arrays.asList(
                    StudioComponentInitializationScript.builder()
                        .script("script1")
                        .runContext(StudioComponentInitializationScriptRunContext.SYSTEM_INITIALIZATION)
                        .platform(LaunchProfilePlatform.WINDOWS)
                        .launchProfileProtocolVersion("2021-03-31")
                        .build(),
                    StudioComponentInitializationScript.builder()
                        .script("script2")
                        .runContext(StudioComponentInitializationScriptRunContext.USER_INITIALIZATION)
                        .platform(LaunchProfilePlatform.LINUX)
                        .launchProfileProtocolVersion("2021-03-31")
                        .build()))
                .name("studioComponent")
                .scriptParameters(new ArrayList<>())
                .ec2SecurityGroupIds(new ArrayList<>())
                .state(StudioComponentState.CREATE_IN_PROGRESS)
                .statusCode(StudioComponentStatusCode.STUDIO_COMPONENT_CREATE_IN_PROGRESS)
                .statusMessage("msg")
                .studioComponentId("studioComponentId")
                .subtype(StudioComponentSubtype.AMAZON_FSX_FOR_WINDOWS)
                .type(StudioComponentType.COMPUTE_FARM)
                .tags(Utils.generateTags())
                .updatedAt(timestamp)
                .updatedBy("Pixel")
                .build())
            .build();
    }

    public static GetStudioComponentResponse generateReadStudioComponentReadyResult() {
        return GetStudioComponentResponse.builder()
            .studioComponent(StudioComponent.builder()
                .configuration(software.amazon.awssdk.services.nimble.model.StudioComponentConfiguration.builder()
                    .activeDirectoryConfiguration(ActiveDirectoryConfiguration.builder()
                        .computerAttributes(new ArrayList<>())
                        .build())
                    .computeFarmConfiguration(ComputeFarmConfiguration.builder().build())
                    .licenseServiceConfiguration(LicenseServiceConfiguration.builder().build())
                    .sharedFileSystemConfiguration(SharedFileSystemConfiguration.builder().build())
                    .build())
                .createdAt(timestamp)
                .createdBy("Fuzzy")
                .description("test")
                .initializationScripts(Arrays.asList(
                    StudioComponentInitializationScript.builder()
                        .script("script1")
                        .runContext(StudioComponentInitializationScriptRunContext.SYSTEM_INITIALIZATION)
                        .platform(LaunchProfilePlatform.WINDOWS)
                        .launchProfileProtocolVersion("2021-03-31")
                        .build(),
                    StudioComponentInitializationScript.builder()
                        .script("script2")
                        .runContext(StudioComponentInitializationScriptRunContext.USER_INITIALIZATION)
                        .platform(LaunchProfilePlatform.LINUX)
                        .launchProfileProtocolVersion("2021-03-31")
                        .build()))
                .name("studioComponent")
                .scriptParameters(new ArrayList<>())
                .ec2SecurityGroupIds(new ArrayList<>())
                .state(StudioComponentState.READY)
                .statusCode(StudioComponentStatusCode.STUDIO_COMPONENT_CREATED)
                .statusMessage("msg")
                .studioComponentId("studioComponentId")
                .subtype(StudioComponentSubtype.AMAZON_FSX_FOR_WINDOWS)
                .type(StudioComponentType.COMPUTE_FARM)
                .tags(Utils.generateTags())
                .updatedAt(timestamp)
                .updatedBy("Pixel")
                .build())
            .build();
    }

    public static GetStudioComponentResponse generateReadStudioComponentUpdatingResult() {
        return GetStudioComponentResponse.builder()
            .studioComponent(StudioComponent.builder()
                .configuration(software.amazon.awssdk.services.nimble.model.StudioComponentConfiguration.builder()
                    .activeDirectoryConfiguration(
                        ActiveDirectoryConfiguration.builder()
                            .computerAttributes(new ArrayList<>())
                            .build())
                    .computeFarmConfiguration(ComputeFarmConfiguration.builder().build())
                    .licenseServiceConfiguration(LicenseServiceConfiguration.builder().build())
                    .sharedFileSystemConfiguration(SharedFileSystemConfiguration.builder().build())
                    .build())
                .createdAt(timestamp)
                .createdBy("Fuzzy")
                .description("test")
                .initializationScripts(Arrays.asList(
                    StudioComponentInitializationScript.builder()
                        .script("script1")
                        .runContext(StudioComponentInitializationScriptRunContext.SYSTEM_INITIALIZATION)
                        .platform(LaunchProfilePlatform.WINDOWS)
                        .launchProfileProtocolVersion("2021-03-31")
                        .build(),
                    StudioComponentInitializationScript.builder()
                        .script("script2")
                        .runContext(StudioComponentInitializationScriptRunContext.USER_INITIALIZATION)
                        .platform(LaunchProfilePlatform.LINUX)
                        .launchProfileProtocolVersion("2021-03-31")
                        .build()))
                .name("studioComponent")
                .scriptParameters(new ArrayList<>())
                .ec2SecurityGroupIds(new ArrayList<>())
                .state(StudioComponentState.UPDATE_IN_PROGRESS)
                .statusCode(StudioComponentStatusCode.STUDIO_COMPONENT_UPDATE_IN_PROGRESS)
                .statusMessage("msg")
                .studioComponentId("studioComponentId")
                .subtype(StudioComponentSubtype.AMAZON_FSX_FOR_WINDOWS)
                .type(StudioComponentType.COMPUTE_FARM)
                .tags(Utils.generateTags())
                .updatedAt(timestamp)
                .updatedBy("Pixel")
                .build())
            .build();
    }

    public static GetStudioComponentResponse generateReadStudioComponentUpdatedResult() {
        return GetStudioComponentResponse.builder()
            .studioComponent(StudioComponent.builder()
                .configuration(software.amazon.awssdk.services.nimble.model.StudioComponentConfiguration.builder()
                    .activeDirectoryConfiguration(ActiveDirectoryConfiguration.builder()
                        .computerAttributes(new ArrayList<>())
                        .build())
                    .computeFarmConfiguration(ComputeFarmConfiguration.builder().build())
                    .licenseServiceConfiguration(LicenseServiceConfiguration.builder().build())
                    .sharedFileSystemConfiguration(SharedFileSystemConfiguration.builder().build())
                    .build())
                .createdAt(timestamp)
                .createdBy("Fuzzy")
                .description("test")
                .initializationScripts(Arrays.asList(
                    StudioComponentInitializationScript.builder()
                        .script("script1")
                        .runContext(StudioComponentInitializationScriptRunContext.SYSTEM_INITIALIZATION)
                        .platform(LaunchProfilePlatform.WINDOWS)
                        .launchProfileProtocolVersion("2021-03-31")
                        .build(),
                    StudioComponentInitializationScript.builder()
                        .script("script2")
                        .runContext(StudioComponentInitializationScriptRunContext.USER_INITIALIZATION)
                        .platform(LaunchProfilePlatform.LINUX)
                        .launchProfileProtocolVersion("2021-03-31")
                        .build()))
                .name("studioComponent")
                .scriptParameters(new ArrayList<>())
                .ec2SecurityGroupIds(new ArrayList<>())
                .state(StudioComponentState.READY)
                .statusCode(StudioComponentStatusCode.STUDIO_COMPONENT_CREATED)
                .statusMessage("msg")
                .studioComponentId("studioComponentId")
                .subtype(StudioComponentSubtype.AMAZON_FSX_FOR_WINDOWS)
                .type(StudioComponentType.COMPUTE_FARM)
                .tags(Utils.generateTags())
                .updatedAt(timestamp)
                .updatedBy("Pixel")
                .build())
            .build();
    }

    public static GetStudioComponentResponse generateReadStudioComponentDeletingResult() {
        return GetStudioComponentResponse.builder()
            .studioComponent(StudioComponent.builder()
                .configuration(software.amazon.awssdk.services.nimble.model.StudioComponentConfiguration.builder()
                    .activeDirectoryConfiguration(ActiveDirectoryConfiguration.builder()
                            .computerAttributes(new ArrayList<>())
                            .build())
                    .computeFarmConfiguration(ComputeFarmConfiguration.builder().build())
                    .licenseServiceConfiguration(LicenseServiceConfiguration.builder().build())
                    .sharedFileSystemConfiguration(SharedFileSystemConfiguration.builder().build())
                    .build())
                .createdAt(timestamp)
                .createdBy("Fuzzy")
                .description("test")
                .initializationScripts(Arrays.asList(
                    StudioComponentInitializationScript.builder()
                        .script("script1")
                        .runContext(StudioComponentInitializationScriptRunContext.SYSTEM_INITIALIZATION)
                        .platform(LaunchProfilePlatform.WINDOWS)
                        .launchProfileProtocolVersion("2021-03-31")
                        .build(),
                    StudioComponentInitializationScript.builder()
                        .script("script2")
                        .runContext(StudioComponentInitializationScriptRunContext.USER_INITIALIZATION)
                        .platform(LaunchProfilePlatform.LINUX)
                        .launchProfileProtocolVersion("2021-03-31")
                        .build()))
                .name("studioComponent")
                .scriptParameters(new ArrayList<>())
                .ec2SecurityGroupIds(new ArrayList<>())
                .state(StudioComponentState.DELETE_IN_PROGRESS)
                .statusCode(StudioComponentStatusCode.STUDIO_COMPONENT_DELETE_IN_PROGRESS)
                .statusMessage("msg")
                .studioComponentId("studioComponentId")
                .subtype(StudioComponentSubtype.AMAZON_FSX_FOR_WINDOWS)
                .type(StudioComponentType.COMPUTE_FARM)
                .tags(Utils.generateTags())
                .updatedAt(timestamp)
                .updatedBy("Pixel")
                .build())
            .build();
    }

    public static GetStudioComponentResponse generateReadStudioComponentDeletedResult() {
        return GetStudioComponentResponse.builder()
            .studioComponent(StudioComponent.builder()
                .configuration(software.amazon.awssdk.services.nimble.model.StudioComponentConfiguration.builder()
                    .activeDirectoryConfiguration(ActiveDirectoryConfiguration.builder()
                        .computerAttributes(new ArrayList<>())
                        .build())
                    .computeFarmConfiguration(ComputeFarmConfiguration.builder().build())
                    .licenseServiceConfiguration(LicenseServiceConfiguration.builder().build())
                    .sharedFileSystemConfiguration(SharedFileSystemConfiguration.builder().build())
                    .build())
                .createdAt(timestamp)
                .createdBy("Fuzzy")
                .description("test")
                .initializationScripts(Arrays.asList(
                    StudioComponentInitializationScript.builder()
                        .script("script1")
                        .runContext(StudioComponentInitializationScriptRunContext.SYSTEM_INITIALIZATION)
                        .platform(LaunchProfilePlatform.WINDOWS)
                        .launchProfileProtocolVersion("2021-03-31")
                        .build(),
                    StudioComponentInitializationScript.builder()
                        .script("script2")
                        .runContext(StudioComponentInitializationScriptRunContext.USER_INITIALIZATION)
                        .platform(LaunchProfilePlatform.LINUX)
                        .launchProfileProtocolVersion("2021-03-31")
                        .build()))
                .name("studioComponent")
                .scriptParameters(new ArrayList<>())
                .ec2SecurityGroupIds(new ArrayList<>())
                .state(StudioComponentState.DELETED)
                .statusCode(StudioComponentStatusCode.STUDIO_COMPONENT_DELETED)
                .statusMessage("Delete Complete")
                .studioComponentId("studioComponentId")
                .subtype(StudioComponentSubtype.AMAZON_FSX_FOR_WINDOWS)
                .type(StudioComponentType.COMPUTE_FARM)
                .tags(Utils.generateTags())
                .updatedAt(timestamp)
                .updatedBy("Pixel")
                .build())
            .build();
    }

    public static List<StudioComponent> getStudioComponents() {
        final StudioComponent studioComponent1 = StudioComponent.builder()
            .configuration(software.amazon.awssdk.services.nimble.model.StudioComponentConfiguration.builder()
                .activeDirectoryConfiguration(
                    ActiveDirectoryConfiguration.builder()
                        .computerAttributes(new ArrayList<>())
                        .build())
                .computeFarmConfiguration(ComputeFarmConfiguration.builder().build())
                .licenseServiceConfiguration(LicenseServiceConfiguration.builder().build())
                .sharedFileSystemConfiguration(SharedFileSystemConfiguration.builder().build())
                .build())
            .createdAt(timestamp)
            .createdBy("Fuzzy1")
            .description("test1")
            .initializationScripts(Arrays.asList(
                StudioComponentInitializationScript.builder()
                    .script("script1")
                    .runContext(StudioComponentInitializationScriptRunContext.SYSTEM_INITIALIZATION)
                    .platform(LaunchProfilePlatform.WINDOWS)
                    .launchProfileProtocolVersion("2021-03-31")
                    .build(),
                StudioComponentInitializationScript.builder()
                    .script("script2")
                    .runContext(StudioComponentInitializationScriptRunContext.USER_INITIALIZATION)
                    .platform(LaunchProfilePlatform.LINUX)
                    .launchProfileProtocolVersion("2021-03-31")
                    .build()))
            .name("studioComponent1")
            .scriptParameters(new ArrayList<>())
            .ec2SecurityGroupIds(new ArrayList<>())
            .state(StudioComponentState.READY)
            .statusCode(StudioComponentStatusCode.STUDIO_COMPONENT_CREATED)
            .statusMessage("msg1")
            .studioComponentId("studioComponentId1")
            .subtype(StudioComponentSubtype.AMAZON_FSX_FOR_WINDOWS)
            .type(StudioComponentType.COMPUTE_FARM)
            .tags(Utils.generateTags())
            .updatedAt(timestamp)
            .updatedBy("Pixel1")
            .build();

        final StudioComponent studioComponent2 = StudioComponent.builder()
            .configuration(software.amazon.awssdk.services.nimble.model.StudioComponentConfiguration.builder()
                .activeDirectoryConfiguration(
                    ActiveDirectoryConfiguration.builder()
                        .computerAttributes(new ArrayList<>())
                        .build())
                .computeFarmConfiguration(ComputeFarmConfiguration.builder().build())
                .licenseServiceConfiguration(LicenseServiceConfiguration.builder().build())
                .sharedFileSystemConfiguration(SharedFileSystemConfiguration.builder().build())
                .build())
            .createdAt(timestamp)
            .createdBy("Fuzzy2")
            .description("test2")
            .initializationScripts(Arrays.asList(
                StudioComponentInitializationScript.builder()
                    .script("script1")
                    .runContext(StudioComponentInitializationScriptRunContext.SYSTEM_INITIALIZATION)
                    .platform(LaunchProfilePlatform.WINDOWS)
                    .launchProfileProtocolVersion("2021-03-31")
                    .build(),
                StudioComponentInitializationScript.builder()
                    .script("script2")
                    .runContext(StudioComponentInitializationScriptRunContext.USER_INITIALIZATION)
                    .platform(LaunchProfilePlatform.LINUX)
                    .launchProfileProtocolVersion("2021-03-31")
                    .build()
            ))
            .name("studioComponent2")
            .scriptParameters(new ArrayList<>())
            .ec2SecurityGroupIds(new ArrayList<>())
            .state(StudioComponentState.READY)
            .statusCode(StudioComponentStatusCode.STUDIO_COMPONENT_CREATED)
            .statusMessage("msg2")
            .studioComponentId("studioComponentId2")
            .subtype(StudioComponentSubtype.AMAZON_FSX_FOR_WINDOWS)
            .type(StudioComponentType.COMPUTE_FARM)
            .tags(Utils.generateTags())
            .updatedAt(timestamp)
            .updatedBy("Pixel2")
            .build();

        return Arrays.asList(studioComponent1, studioComponent2);
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
