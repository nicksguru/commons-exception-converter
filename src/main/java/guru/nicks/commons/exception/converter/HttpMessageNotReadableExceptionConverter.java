package guru.nicks.commons.exception.converter;

import guru.nicks.commons.exception.ExceptionConverter;
import guru.nicks.commons.exception.http.BadRequestException;

import org.springframework.http.converter.HttpMessageNotReadableException;

public class HttpMessageNotReadableExceptionConverter
        implements ExceptionConverter<HttpMessageNotReadableException, BadRequestException> {
}
