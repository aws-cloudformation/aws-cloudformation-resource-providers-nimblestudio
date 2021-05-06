package software.amazon.nimblestudio.studio;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.nimble.model.AccessDeniedException;
import software.amazon.awssdk.services.nimble.model.ConflictException;
import software.amazon.awssdk.services.nimble.model.InternalServerErrorException;
import software.amazon.awssdk.services.nimble.model.ResourceNotFoundException;
import software.amazon.awssdk.services.nimble.model.ServiceQuotaExceededException;
import software.amazon.awssdk.services.nimble.model.Studio;
import software.amazon.awssdk.services.nimble.model.StudioEncryptionConfiguration;
import software.amazon.awssdk.services.nimble.model.ThrottlingException;
import software.amazon.awssdk.services.nimble.model.ValidationException;
import software.amazon.cloudformation.exceptions.BaseHandlerException;
import software.amazon.cloudformation.exceptions.CfnAccessDeniedException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnResourceConflictException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.exceptions.CfnServiceLimitExceededException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;

public class Translator {

    static ResourceModel toModel(Studio studio) {
        ResourceModel.ResourceModelBuilder builder = ResourceModel.builder()
                .adminRoleArn(studio.adminRoleArn())
                .userRoleArn(studio.userRoleArn())
                .displayName(studio.displayName())
                .homeRegion(studio.homeRegion())
                .ssoClientId(studio.ssoClientId())
                .studioId(studio.studioId())
                .studioName(studio.studioName())
                .studioUrl(studio.studioUrl())
                .tags(studio.tags());

        if (studio.studioEncryptionConfiguration() != null) {
            builder.studioEncryptionConfiguration(
                    Translator.toStudioEncryptionConfiguration(studio.studioEncryptionConfiguration())
            );
        }

        return  builder.build();
    }

    static StudioEncryptionConfiguration toModelStudioEncryptionConfiguration(
        final software.amazon.nimblestudio.studio.StudioEncryptionConfiguration configuration) {

        final StudioEncryptionConfiguration.Builder studioEncryptionConfigurationBuilder =
            StudioEncryptionConfiguration.builder();

        if (configuration.getKeyArn() != null) {
            studioEncryptionConfigurationBuilder.keyArn(configuration.getKeyArn());
        }

        // Required
        studioEncryptionConfigurationBuilder.keyType(configuration.getKeyType());

        return studioEncryptionConfigurationBuilder.build();
    }

    static software.amazon.nimblestudio.studio.StudioEncryptionConfiguration toStudioEncryptionConfiguration(
        final StudioEncryptionConfiguration configuration) {

        final software.amazon.nimblestudio.studio.StudioEncryptionConfiguration.StudioEncryptionConfigurationBuilder configBuilder =
            software.amazon.nimblestudio.studio.StudioEncryptionConfiguration.builder();

        if (configuration.keyArn() != null) {
            configBuilder.keyArn(configuration.keyArn());
        }

        // Required
        configBuilder.keyType(configuration.keyType().toString());

        return configBuilder.build();
    }

    static BaseHandlerException translateToCfnException(final AwsServiceException exception) {
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
