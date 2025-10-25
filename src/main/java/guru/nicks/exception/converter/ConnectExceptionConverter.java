package guru.nicks.exception.converter;

import guru.nicks.exception.ExceptionConverter;
import guru.nicks.exception.http.ServiceTimeoutException;

import org.springframework.stereotype.Component;

import java.net.ConnectException;

/**
 * {@link ConnectException} is thrown if the given host and port do not accept connections.
 */
@Component
public class ConnectExceptionConverter implements ExceptionConverter<ConnectException, ServiceTimeoutException> {
}
