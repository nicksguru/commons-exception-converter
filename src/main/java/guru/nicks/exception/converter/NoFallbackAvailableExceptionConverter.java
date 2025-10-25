package guru.nicks.exception.converter;

import guru.nicks.exception.ExceptionConverter;
import guru.nicks.exception.http.ServiceTimeoutException;

import org.springframework.cloud.client.circuitbreaker.NoFallbackAvailableException;
import org.springframework.stereotype.Component;

/**
 * {@link NoFallbackAvailableException} is thrown by the Resilience4j Circuit Breaker if the call whose circuit is open
 * ('broken') has no fallback specified.
 */
@Component
public class NoFallbackAvailableExceptionConverter
        implements ExceptionConverter<NoFallbackAvailableException, ServiceTimeoutException> {
}
