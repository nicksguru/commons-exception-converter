package guru.nicks.exception.converter;

import guru.nicks.exception.ExceptionConverter;
import guru.nicks.exception.http.UnsupportedMediaTypeException;

import org.springframework.stereotype.Component;
import org.springframework.web.HttpMediaTypeNotSupportedException;

@Component
public class HttpMediaTypeNotSupportedExceptionConverter
        implements ExceptionConverter<HttpMediaTypeNotSupportedException, UnsupportedMediaTypeException> {
}
