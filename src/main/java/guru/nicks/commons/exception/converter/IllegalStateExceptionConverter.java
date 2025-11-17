package guru.nicks.commons.exception.converter;

import guru.nicks.commons.exception.ExceptionConverter;
import guru.nicks.commons.exception.http.ConflictException;

import org.springframework.stereotype.Component;

@Component
public class IllegalStateExceptionConverter implements ExceptionConverter<IllegalStateException, ConflictException> {
}
