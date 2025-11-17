package guru.nicks.commons.exception.converter;

import guru.nicks.commons.exception.ExceptionConverter;
import guru.nicks.commons.exception.http.ServiceTimeoutException;

import org.springframework.stereotype.Component;

import java.net.ConnectException;

/**
 * {@link ConnectException} is thrown if the given host and port do not accept connections.
 */
@Component
public class ConnectExceptionConverter implements ExceptionConverter<ConnectException, ServiceTimeoutException> {
}
