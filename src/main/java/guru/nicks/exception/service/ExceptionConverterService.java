package guru.nicks.exception.service;

import guru.nicks.exception.BusinessException;
import guru.nicks.rest.v1.dto.BusinessExceptionDto;

import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

/**
 * Converts various exceptions to {@link BusinessExceptionDto}.
 */
public interface ExceptionConverterService {

    /**
     * Creates DTO using HTTP request URI stored in {@link RequestContextHolder}.
     *
     * @param cause cause of error
     * @return DTO
     * @see #createDto(Throwable, String)
     */
    default BusinessExceptionDto createDto(Throwable cause) {
        return createDto(cause, getCurrentHttpRequestUri());
    }

    /**
     * Creates DTO.
     *
     * @param cause      cause of error
     * @param requestUri HTTP request URI
     * @return DTO
     */
    BusinessExceptionDto createDto(Throwable cause, @Nullable String requestUri);

    /**
     * Creates DTO using HTTP request URI stored in {@link RequestContextHolder}.
     *
     * @param e business exception
     * @return DTO
     * @see #createDto(BusinessException, String)
     */
    default BusinessExceptionDto createDto(BusinessException e) {
        return createDto(e, getCurrentHttpRequestUri());
    }

    /**
     * Creates DTO.
     *
     * @param cause      business exception
     * @param requestUri optional HTTP request URI
     * @return DTo
     */
    BusinessExceptionDto createDto(@Nullable BusinessException cause, @Nullable String requestUri);

    @Nullable
    private String getCurrentHttpRequestUri() {
        // getRequestAttributes() returns null if we're not in a web request (e.g. in a JMS request)
        return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
                .filter(ServletRequestAttributes.class::isInstance)
                .map(ServletRequestAttributes.class::cast)
                .map(ServletRequestAttributes::getRequest)
                .map(HttpServletRequest::getRequestURI)
                .orElse(null);
    }

}
