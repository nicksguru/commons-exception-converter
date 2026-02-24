package guru.nicks.commons.exception.converter;

import guru.nicks.commons.exception.ExceptionConverter;
import guru.nicks.commons.exception.http.UnsupportedMediaTypeException;

import org.springframework.web.HttpMediaTypeNotSupportedException;

public class HttpMediaTypeNotSupportedExceptionConverter
        implements ExceptionConverter<HttpMediaTypeNotSupportedException, UnsupportedMediaTypeException> {
}
