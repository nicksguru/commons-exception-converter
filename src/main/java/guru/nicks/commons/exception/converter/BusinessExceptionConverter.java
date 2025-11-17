package guru.nicks.commons.exception.converter;

import guru.nicks.commons.exception.BusinessException;
import guru.nicks.commons.exception.ExceptionConverter;

import org.springframework.stereotype.Component;

/**
 * Returns the original exception as-is because it is already an instance of {@link BusinessException}.
 */
@Component
public class BusinessExceptionConverter implements ExceptionConverter<BusinessException, BusinessException> {

    @Override
    public BusinessException convert(BusinessException cause) {
        return cause;
    }

}
