package guru.nicks.exception.converter;

import guru.nicks.exception.ExceptionConverter;
import guru.nicks.exception.http.MethodNotAllowedException;

import org.springframework.stereotype.Component;
import org.springframework.web.HttpRequestMethodNotSupportedException;

@Component
public class HttpRequestMethodNotSupportedExceptionConverter
        implements ExceptionConverter<HttpRequestMethodNotSupportedException, MethodNotAllowedException> {
}
