package guru.nicks.commons.exception.converter;

import guru.nicks.commons.exception.ExceptionConverter;
import guru.nicks.commons.exception.http.NotImplementedException;

/**
 * One of the commonly used subclasses of {@link UnsupportedOperationException} is
 * {@link org.apache.commons.lang3.NotImplementedException}.
 */
public class UnsupportedOperationExceptionConverter
        implements ExceptionConverter<UnsupportedOperationException, NotImplementedException> {
}
