package guru.nicks.commons.exception.converter;

import guru.nicks.commons.exception.BusinessException;
import guru.nicks.commons.exception.ExceptionConverter;

/**
 * Returns the original exception as-is because it is already an instance of {@link BusinessException}.
 */
public class BusinessExceptionConverter implements ExceptionConverter<BusinessException, BusinessException> {

    @Override
    public BusinessException convert(BusinessException cause) {
        return cause;
    }

}
