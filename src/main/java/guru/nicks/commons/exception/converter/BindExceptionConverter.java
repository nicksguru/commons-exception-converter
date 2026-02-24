package guru.nicks.commons.exception.converter;

import guru.nicks.commons.exception.ExceptionConverter;
import guru.nicks.commons.exception.http.BadRequestException;

import org.springframework.stereotype.Component;
import org.springframework.validation.BindException;

@Component
public class BindExceptionConverter implements ExceptionConverter<BindException, BadRequestException> {
}
