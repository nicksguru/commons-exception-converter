package guru.nicks.commons.exception.converter;

import guru.nicks.commons.exception.ExceptionConverter;
import guru.nicks.commons.exception.http.UnauthorizedException;

import org.springframework.stereotype.Component;

@Component
public class SecurityExceptionConverter implements ExceptionConverter<SecurityException, UnauthorizedException> {
}
