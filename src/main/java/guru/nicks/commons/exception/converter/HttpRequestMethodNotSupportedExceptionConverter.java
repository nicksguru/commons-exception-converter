package guru.nicks.commons.exception.converter;

import guru.nicks.commons.exception.ExceptionConverter;
import guru.nicks.commons.exception.http.MethodNotAllowedException;

import org.springframework.web.HttpRequestMethodNotSupportedException;

public class HttpRequestMethodNotSupportedExceptionConverter
        implements ExceptionConverter<HttpRequestMethodNotSupportedException, MethodNotAllowedException> {
}
