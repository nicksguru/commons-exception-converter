package guru.nicks.commons.exception.converter;

import guru.nicks.commons.exception.ExceptionConverter;
import guru.nicks.commons.exception.http.BadRequestException;

import org.springframework.core.convert.ConversionFailedException;
import org.springframework.stereotype.Component;

/**
 * Thrown when an invalid field type is passed in an HTTP request, e.g. string instead of integer.
 */
@Component
public class ConversionFailedExceptionConverter
        implements ExceptionConverter<ConversionFailedException, BadRequestException> {
}
