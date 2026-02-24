package guru.nicks.commons.exception.converter;

import guru.nicks.commons.exception.ExceptionConverter;
import guru.nicks.commons.exception.http.BadRequestException;

import org.springframework.stereotype.Component;

import java.time.format.DateTimeParseException;

@Component
public class DateTimeParseExceptionConverter
        implements ExceptionConverter<DateTimeParseException, BadRequestException> {
}
