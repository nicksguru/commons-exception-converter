package guru.nicks.exception.converter;

import guru.nicks.exception.BusinessException;
import guru.nicks.exception.ExceptionConverter;

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
