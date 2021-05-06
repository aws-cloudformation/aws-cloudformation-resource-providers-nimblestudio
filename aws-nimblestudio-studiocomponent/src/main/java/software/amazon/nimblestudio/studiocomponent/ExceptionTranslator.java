package software.amazon.nimblestudio.studiocomponent;

import software.amazon.cloudformation.exceptions.BaseHandlerException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnAccessDeniedException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.exceptions.CfnServiceLimitExceededException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;
import software.amazon.cloudformation.exceptions.CfnResourceConflictException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.awssdk.services.nimble.model.AccessDeniedException;
import software.amazon.awssdk.services.nimble.model.ConflictException;
import software.amazon.awssdk.services.nimble.model.InternalServerErrorException;
import software.amazon.awssdk.services.nimble.model.ResourceNotFoundException;
import software.amazon.awssdk.services.nimble.model.ServiceQuotaExceededException;
import software.amazon.awssdk.services.nimble.model.ThrottlingException;
import software.amazon.awssdk.services.nimble.model.ValidationException;
import software.amazon.awssdk.awscore.exception.AwsServiceException;

public final class ExceptionTranslator {
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
