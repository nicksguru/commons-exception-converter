package guru.nicks.commons.exception.converter;

import guru.nicks.commons.exception.ExceptionConverter;
import guru.nicks.commons.exception.http.ServiceTimeoutException;

import java.net.ConnectException;

/**
 * {@link ConnectException} is thrown if the given host and port do not accept connections.
 */
public class ConnectExceptionConverter implements ExceptionConverter<ConnectException, ServiceTimeoutException> {
}
