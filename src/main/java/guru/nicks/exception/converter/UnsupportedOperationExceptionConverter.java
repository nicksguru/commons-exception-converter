package guru.nicks.exception.converter;

import guru.nicks.exception.ExceptionConverter;
import guru.nicks.exception.http.NotImplementedException;

import org.springframework.stereotype.Component;

/**
 * One of the commonly used subclasses of {@link UnsupportedOperationException} is
 * {@link org.apache.commons.lang3.NotImplementedException}.
 */
@Component
public class UnsupportedOperationExceptionConverter
        implements ExceptionConverter<UnsupportedOperationException, NotImplementedException> {
}
