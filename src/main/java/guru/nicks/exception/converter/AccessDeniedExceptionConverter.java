package guru.nicks.exception.converter;

import guru.nicks.exception.ExceptionConverter;
import guru.nicks.exception.http.UnauthorizedException;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * Looking as {@link AccessDeniedException} subclasses, such as
 * {@code org.springframework.security.web.csrf.CsrfException}, the reason is more like HTTP 401 than 403.
 */
@Component
public class AccessDeniedExceptionConverter
        implements ExceptionConverter<AccessDeniedException, UnauthorizedException> {
}
