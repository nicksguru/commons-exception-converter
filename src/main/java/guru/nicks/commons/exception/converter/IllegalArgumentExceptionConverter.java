package guru.nicks.commons.exception.converter;

import guru.nicks.commons.exception.ExceptionConverter;
import guru.nicks.commons.exception.http.BadRequestException;

import org.springframework.stereotype.Component;

@Component
public class IllegalArgumentExceptionConverter
        implements ExceptionConverter<IllegalArgumentException, BadRequestException> {
}
