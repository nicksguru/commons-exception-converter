package guru.nicks.exception.converter;

import guru.nicks.exception.ExceptionConverter;
import guru.nicks.exception.http.UnauthorizedException;

import org.springframework.stereotype.Component;

@Component
public class SecurityExceptionConverter implements ExceptionConverter<SecurityException, UnauthorizedException> {
}
