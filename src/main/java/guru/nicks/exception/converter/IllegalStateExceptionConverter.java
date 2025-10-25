package guru.nicks.exception.converter;

import guru.nicks.exception.ExceptionConverter;
import guru.nicks.exception.http.ConflictException;

import org.springframework.stereotype.Component;

@Component
public class IllegalStateExceptionConverter implements ExceptionConverter<IllegalStateException, ConflictException> {
}
