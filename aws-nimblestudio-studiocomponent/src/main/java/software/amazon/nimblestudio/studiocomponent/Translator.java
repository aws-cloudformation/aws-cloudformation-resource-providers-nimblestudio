package software.amazon.nimblestudio.studiocomponent;

import java.util.ArrayList;
import java.util.List;
import static java.util.stream.Collectors.toList;

public class Translator {

    static StudioComponentConfiguration toModelStudioComponentConfiguration(
        final software.amazon.awssdk.services.nimble.model.StudioComponentConfiguration studioComponentConfiguration) {

        final StudioComponentConfiguration.StudioComponentConfigurationBuilder builder = new StudioComponentConfiguration.StudioComponentConfigurationBuilder();
        if (studioComponentConfiguration == null) {
            return builder.build();
        }

        if (studioComponentConfiguration.activeDirectoryConfiguration() != null) {
            builder.activeDirectoryConfiguration(toModelActiveDirectoryConfiguration(studioComponentConfiguration.activeDirectoryConfiguration()));
        }
        if (studioComponentConfiguration.computeFarmConfiguration() != null) {
            builder.computeFarmConfiguration(toModelComputeFarmConfiguration(studioComponentConfiguration.computeFarmConfiguration()));
        }
        if (studioComponentConfiguration.licenseServiceConfiguration() != null) {
            builder.licenseServiceConfiguration(toModelLicenseServiceConfiguration(studioComponentConfiguration.licenseServiceConfiguration()));
        }
        if (studioComponentConfiguration.sharedFileSystemConfiguration() != null) {
            builder.sharedFileSystemConfiguration(toModelSharedFileSystemConfiguration(studioComponentConfiguration.sharedFileSystemConfiguration()));
        }

        return builder.build();
    }

    static ActiveDirectoryConfiguration toModelActiveDirectoryConfiguration(
        final software.amazon.awssdk.services.nimble.model.ActiveDirectoryConfiguration activeDirectoryConfiguration) {
        return ActiveDirectoryConfiguration.builder()
            .computerAttributes(
                activeDirectoryConfiguration.computerAttributes().stream().map(
                    ca -> ActiveDirectoryComputerAttribute.builder()
                    .name(ca.name())
                    .value(ca.value())
                    .build()
                ).collect(toList()))
            .directoryId(activeDirectoryConfiguration.directoryId())
            .organizationalUnitDistinguishedName(activeDirectoryConfiguration.organizationalUnitDistinguishedName())
            .build();
    }

    static ComputeFarmConfiguration toModelComputeFarmConfiguration(
        final software.amazon.awssdk.services.nimble.model.ComputeFarmConfiguration computeFarmConfiguration) {
        return ComputeFarmConfiguration.builder()
            .activeDirectoryUser(computeFarmConfiguration.activeDirectoryUser())
            .endpoint(computeFarmConfiguration.endpoint())
            .build();
    }

    static List<StudioComponentInitializationScript> toModelStudioComponentInitializationScripts(
        final software.amazon.awssdk.services.nimble.model.StudioComponent studioComponent) {
        return studioComponent.initializationScripts() == null ? new ArrayList<>() :
            studioComponent.initializationScripts().stream()
                .map(is -> StudioComponentInitializationScript.builder()
                    .launchProfileProtocolVersion(is.launchProfileProtocolVersion())
                    .runContext(is.runContextAsString())
                    .script(is.script())
                    .platform(is.platformAsString())
                    .build()
                )
                .collect(toList());
    }

    static LicenseServiceConfiguration toModelLicenseServiceConfiguration(
        final software.amazon.awssdk.services.nimble.model.LicenseServiceConfiguration licenseServiceConfiguration) {
        return LicenseServiceConfiguration.builder()
            .endpoint(licenseServiceConfiguration.endpoint())
            .build();
    }

    static SharedFileSystemConfiguration toModelSharedFileSystemConfiguration(
        final software.amazon.awssdk.services.nimble.model.SharedFileSystemConfiguration sharedFileSystemConfiguration) {
        return SharedFileSystemConfiguration.builder()
            .endpoint(sharedFileSystemConfiguration.endpoint())
            .fileSystemId(sharedFileSystemConfiguration.fileSystemId())
            .linuxMountPoint(sharedFileSystemConfiguration.linuxMountPoint())
            .shareName(sharedFileSystemConfiguration.shareName())
            .windowsMountDrive(sharedFileSystemConfiguration.windowsMountDrive())
            .build();
    }
}
