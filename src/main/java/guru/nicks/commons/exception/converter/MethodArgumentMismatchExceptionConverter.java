package guru.nicks.commons.exception.converter;

import guru.nicks.commons.exception.ExceptionConverter;
import guru.nicks.commons.exception.http.BadRequestException;

import org.springframework.stereotype.Component;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Thrown when e.g. request contains a string that can't be converted to an enum member.
 */
@Component
public class MethodArgumentMismatchExceptionConverter
        implements ExceptionConverter<MethodArgumentTypeMismatchException, BadRequestException> {
}
