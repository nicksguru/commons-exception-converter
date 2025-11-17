package guru.nicks.commons.exception.converter;

import guru.nicks.commons.exception.ExceptionConverter;
import guru.nicks.commons.exception.http.NotFoundException;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Component
public class NoResourceFoundExceptionConverter
        implements ExceptionConverter<NoResourceFoundException, NotFoundException> {
}
