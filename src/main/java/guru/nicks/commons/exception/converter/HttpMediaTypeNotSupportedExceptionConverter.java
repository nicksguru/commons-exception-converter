package guru.nicks.commons.exception.converter;

import guru.nicks.commons.exception.ExceptionConverter;
import guru.nicks.commons.exception.http.UnsupportedMediaTypeException;

import org.springframework.stereotype.Component;
import org.springframework.web.HttpMediaTypeNotSupportedException;

@Component
public class HttpMediaTypeNotSupportedExceptionConverter
        implements ExceptionConverter<HttpMediaTypeNotSupportedException, UnsupportedMediaTypeException> {
}
