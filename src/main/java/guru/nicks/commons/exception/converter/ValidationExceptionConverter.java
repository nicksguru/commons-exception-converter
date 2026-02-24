package guru.nicks.commons.exception.converter;

import guru.nicks.commons.exception.ExceptionConverter;
import guru.nicks.commons.exception.http.BadRequestException;

import jakarta.validation.ValidationException;
import org.springframework.stereotype.Component;

@Component
public class ValidationExceptionConverter implements ExceptionConverter<ValidationException, BadRequestException> {
}
