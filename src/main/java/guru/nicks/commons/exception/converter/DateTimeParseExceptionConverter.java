package guru.nicks.commons.exception.converter;

import guru.nicks.commons.exception.ExceptionConverter;
import guru.nicks.commons.exception.http.BadRequestException;

import java.time.format.DateTimeParseException;

public class DateTimeParseExceptionConverter
        implements ExceptionConverter<DateTimeParseException, BadRequestException> {
}
