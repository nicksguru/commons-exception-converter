package guru.nicks.exception.mapper;

import guru.nicks.exception.BusinessException;
import guru.nicks.exception.RootHttpStatus;
import guru.nicks.utils.HttpRequestUtils;

import jakarta.annotation.Nullable;
import org.springframework.http.HttpStatus;

import java.util.Map;
import java.util.Optional;

import static guru.nicks.validation.dsl.ValiDsl.checkNotNull;

/**
 * Provides mappings between {@code T}, {@link BusinessException}, and {@link HttpStatus}.
 */
public interface ErrorCodeMapper<T extends Enum<T>> {

    /**
     * @return error code registry
     */
    ErrorCodeRegistry<T> getErrorCodeRegistry();

    /**
     * Finds error code whose exception class is the same as (or the closest parent of) the argument.
     *
     * @param e business exception, can be {@code null}
     * @return error code with fallback to {@link #getDefaultErrorCode()}
     */
    default T toErrorCode(@Nullable BusinessException e) {
        // WARNING: don't call getOrDefault() - specific Map implementation may throw exceptions on null keys
        T errorCode = Optional.ofNullable(e)
                .map(BusinessException::getClass)
                .flatMap(getErrorCodeRegistry().getExceptionClassToErrorCode()::findEntryForClosestSuperclass)
                .map(Map.Entry::getValue)
                .orElseGet(this::getDefaultErrorCode);

        //  WARNING: don't store default error code in a static variable - it'll be null because this class is part of
        //  each enum member's initialization, so it's impossible to refer to enum members on the class level
        return checkNotNull(errorCode, "default error code");
    }

    /**
     * Maps the given {@link HttpStatus} to its corresponding {@code T}. The mapping comes from special exception
     * classes annotated with {@link RootHttpStatus @RootHttpStatus}.
     *
     * @param httpStatus HTTP status, can be {@code null}
     * @return error code with fallback to {@link #getDefaultErrorCode()}
     */
    default T toErrorCode(@Nullable HttpStatus httpStatus) {
        // WARNING: don't call getOrDefault() - specific Map implementation may throw exceptions on null keys
        T errorCode = Optional.ofNullable(httpStatus)
                .map(getErrorCodeRegistry().getHttpStatusToErrorCode()::get)
                .orElseGet(this::getDefaultErrorCode);

        return checkNotNull(errorCode, "missing default error code");
    }

    /**
     * Maps the given HTTP status code to its corresponding {@code T}. The mapping comes from special exception classes
     * annotated with {@link RootHttpStatus @RootHttpStatus}.
     *
     * @param httpStatusCode HTTP status code
     * @return error code with fallback to {@link #getDefaultErrorCode()}
     */
    default T toErrorCode(int httpStatusCode) {
        HttpStatus httpStatus = HttpRequestUtils.resolveHttpStatus(httpStatusCode).orElse(null);
        return toErrorCode(httpStatus);
    }

    /**
     * Maps {@link BusinessException} to {@link HttpStatus} declared in one of exception classes (the closest parent of
     * the argument) annotated with {@link RootHttpStatus @RootHttpStatus}.
     *
     * @param e business exception, can be {@code null}
     * @return HTTP status code with fallback to {@link #getDefaultHttpStatus()}
     */
    default HttpStatus toHttpStatus(@Nullable BusinessException e) {
        // WARNING: don't call getOrDefault() - specific Map implementation may throw exceptions on null keys
        HttpStatus httpStatus = Optional.ofNullable(e)
                .map(BusinessException::getClass)
                .flatMap(getErrorCodeRegistry().getExceptionClassToHttpStatus()::findEntryForClosestSuperclass)
                .map(Map.Entry::getValue)
                .orElseGet(this::getDefaultHttpStatus);

        return checkNotNull(httpStatus, "missing default HTTP status");
    }

    /**
     * Maps {@code T} to {@link HttpStatus} declared in one of exception classes (the closest parent of T's exception
     * class) annotated with {@link RootHttpStatus @RootHttpStatus}.
     *
     * @param errorCode error code, can be {@code null}
     * @return HTTP status code with fallback to {@link #getDefaultHttpStatus()}
     */
    default HttpStatus toHttpStatus(@Nullable T errorCode) {
        // WARNING: don't call getOrDefault() - specific Map implementation may throw exceptions on null keys
        HttpStatus httpStatus = Optional.ofNullable(errorCode)
                .map(getErrorCodeRegistry().getErrorCodeToExceptionClass())
                .flatMap(getErrorCodeRegistry().getExceptionClassToHttpStatus()::findEntryForClosestSuperclass)
                .map(Map.Entry::getValue)
                .orElseGet(this::getDefaultHttpStatus);

        return checkNotNull(httpStatus, "missing default HTTP status");
    }

    /**
     * Default HTTP status for such cases when it wasn't found during mapping.
     *
     * @see #toHttpStatus(BusinessException)
     */
    HttpStatus getDefaultHttpStatus();

    /**
     * Default error code for such cases when it wasn't found during mapping.
     *
     * @see #toErrorCode(BusinessException)
     * @see #toErrorCode(HttpStatus)
     * @see #toErrorCode(int)
     */
    T getDefaultErrorCode();

}
