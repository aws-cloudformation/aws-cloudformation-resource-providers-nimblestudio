package software.amazon.nimblestudio.launchprofile;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.nimble.model.*;
import software.amazon.cloudformation.exceptions.BaseHandlerException;
import software.amazon.cloudformation.exceptions.CfnAccessDeniedException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnResourceConflictException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.exceptions.CfnServiceLimitExceededException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;

import java.util.stream.Collectors;

public class Translator {

    static StreamConfiguration toModelStreamConfiguration(
        final software.amazon.awssdk.services.nimble.model.StreamConfiguration streamConfiguration) {
        final StreamConfiguration.StreamConfigurationBuilder streamConfigurationBuilder = StreamConfiguration.builder();

        if (streamConfiguration.maxSessionLengthInMinutes() != null) {
            streamConfigurationBuilder.maxSessionLengthInMinutes(Double.valueOf(streamConfiguration.maxSessionLengthInMinutes()));
        }

        if (streamConfiguration.maxStoppedSessionLengthInMinutes() != null) {
            streamConfigurationBuilder.maxStoppedSessionLengthInMinutes(Double.valueOf(streamConfiguration.maxStoppedSessionLengthInMinutes()));
        }

        if (streamConfiguration.sessionStorage() != null) {
            streamConfigurationBuilder.sessionStorage(toModelStreamConfigurationSessionStorage(streamConfiguration.sessionStorage()));
        }

        return streamConfigurationBuilder
            .clipboardMode(streamConfiguration.clipboardModeAsString())
            .streamingImageIds(streamConfiguration.streamingImageIds())
            .ec2InstanceTypes(streamConfiguration.ec2InstanceTypesAsStrings())
            .build();
    }

    static StreamConfigurationSessionStorage toModelStreamConfigurationSessionStorage(
        final software.amazon.awssdk.services.nimble.model.StreamConfigurationSessionStorage streamConfigurationSessionStorage) {
        final StreamConfigurationSessionStorage.StreamConfigurationSessionStorageBuilder streamConfigurationSessionStorageBuilder = StreamConfigurationSessionStorage.builder();

        return streamConfigurationSessionStorageBuilder
            .root(toModelStreamingSessionStorageRoot(streamConfigurationSessionStorage.root()))
            .mode(streamConfigurationSessionStorage.modeAsStrings())
            .build();
    }

    static StreamingSessionStorageRoot toModelStreamingSessionStorageRoot(
        final software.amazon.awssdk.services.nimble.model.StreamingSessionStorageRoot streamingSessionStorageRoot
    ){

        final StreamingSessionStorageRoot.StreamingSessionStorageRootBuilder streamingSessionStorageRootBuilder = StreamingSessionStorageRoot.builder();

        if (streamingSessionStorageRoot.linux()!= null) {
            streamingSessionStorageRootBuilder.linux(streamingSessionStorageRoot.linux());
        }

        if (streamingSessionStorageRoot.windows()!= null){
            streamingSessionStorageRootBuilder.windows(streamingSessionStorageRoot.windows());
        }

        return streamingSessionStorageRootBuilder.build();
    }

    static StreamConfigurationCreate fromModelStreamConfiguration(final StreamConfiguration streamConfiguration) {
        if (streamConfiguration == null) {
            return StreamConfigurationCreate.builder().build();
        }

        final StreamConfigurationCreate.Builder streamConfigurationCreateBuilder = StreamConfigurationCreate.builder()
            .clipboardMode(streamConfiguration.getClipboardMode())
            .streamingImageIds(streamConfiguration.getStreamingImageIds())
            .ec2InstanceTypes(streamConfiguration.getEc2InstanceTypes().stream()
                .map(StreamingInstanceType::fromValue)
                .collect(Collectors.toList()));

        if (streamConfiguration.getMaxSessionLengthInMinutes() != null) {
            streamConfigurationCreateBuilder.maxSessionLengthInMinutes(
                    streamConfiguration.getMaxSessionLengthInMinutes().intValue());
        }

        if (streamConfiguration.getMaxStoppedSessionLengthInMinutes() != null) {
            streamConfigurationCreateBuilder.maxStoppedSessionLengthInMinutes(
                    streamConfiguration.getMaxStoppedSessionLengthInMinutes().intValue());
        }

        if (streamConfiguration.getSessionStorage() != null) {
            software.amazon.awssdk.services.nimble.model.StreamConfigurationSessionStorage.Builder storageBuilder = software.amazon.awssdk.services.nimble.model.StreamConfigurationSessionStorage.builder();
            software.amazon.awssdk.services.nimble.model.StreamingSessionStorageRoot.Builder storageRootBuilder = software.amazon.awssdk.services.nimble.model.StreamingSessionStorageRoot.builder();
            StreamingSessionStorageRoot storageRoot = streamConfiguration.getSessionStorage().getRoot();

            if (storageRoot.getLinux() != null){
                storageRootBuilder.linux(storageRoot.getLinux());
            }

            if (storageRoot.getWindows() != null){
                storageRootBuilder.windows(storageRoot.getWindows());
            }

            streamConfigurationCreateBuilder.sessionStorage(
                storageBuilder
                .mode(
                    streamConfiguration.getSessionStorage().getMode().stream()
                    .map(StreamingSessionStorageMode::fromValue)
                    .collect(Collectors.toList()))
                .root(storageRootBuilder.build())
                .build()
            );
        }

        return streamConfigurationCreateBuilder.build();
    }

    public static BaseHandlerException translateToCfnException(final AwsServiceException exception) {
        if (exception instanceof AccessDeniedException) {
            return new CfnAccessDeniedException(ResourceModel.TYPE_NAME, exception);
        } else if (exception instanceof ValidationException) {
            return new CfnInvalidRequestException(exception);
        } else if (exception instanceof InternalServerErrorException) {
            return new CfnServiceInternalErrorException(exception);
        } else if (exception instanceof ServiceQuotaExceededException) {
            return new CfnServiceLimitExceededException(ResourceModel.TYPE_NAME, exception.getMessage(), exception);
        } else if (exception instanceof ResourceNotFoundException) {
            return new CfnNotFoundException(exception);
        } else if (exception instanceof ThrottlingException) {
            return new CfnThrottlingException(exception);
        } else if (exception instanceof ConflictException) {
            return new CfnResourceConflictException(exception);
        } else {
            return new CfnGeneralServiceException(exception.getMessage(), exception);
        }
    }
}
