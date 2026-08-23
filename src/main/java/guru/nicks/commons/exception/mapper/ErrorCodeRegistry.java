package guru.nicks.commons.exception.mapper;

import guru.nicks.commons.designpattern.SubclassBeforeSuperclassMap;
import guru.nicks.commons.exception.BusinessException;
import guru.nicks.commons.exception.BusinessExceptionProvider;
import guru.nicks.commons.exception.RootHttpStatus;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpStatus;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static guru.nicks.commons.validation.dsl.ValiDsl.checkNotNull;

/**
 * Provides storage for mappings between {@code T}, {@link BusinessException}, and {@link HttpStatus}.
 */
@Slf4j
public abstract class ErrorCodeRegistry<T extends BusinessExceptionProvider> {

    /**
     * Mapping of exception classes to error codes. Needed to map their subclasses to the same error code.
     */
    @Getter
    private final SubclassBeforeSuperclassMap<BusinessException, T> exceptionClassToErrorCode =
            new SubclassBeforeSuperclassMap<>();

    /**
     * Mapping of exception classes annotated with {@link RootHttpStatus @RootHttpStatus} to HTTP status codes. Needed
     * to map their subclasses to the same HTTP status.
     */
    @Getter
    private final SubclassBeforeSuperclassMap<BusinessException, HttpStatus> exceptionClassToHttpStatus =
            new SubclassBeforeSuperclassMap<>();

    /**
     * Reverse mapping of HTTP statuses to error codes retrieved from exception classes annotated with
     * {@link RootHttpStatus @RootHttpStatus}. Needed to map HTTP statuses to error codes.
     */
    @Getter
    private final Map<HttpStatus, T> httpStatusToErrorCode = new EnumMap<>(HttpStatus.class);

    @Getter
    private final Function<T, Class<? extends BusinessException>> errorCodeToExceptionClass;

    /**
     * Registers all {@code T} enum values in auxiliary data structures.
     */
    protected ErrorCodeRegistry(Function<T, Class<? extends BusinessException>> errorCodeToExceptionClass) {
        this.errorCodeToExceptionClass = errorCodeToExceptionClass;
        registerAllErrorCodes();
    }

    /**
     * WARNING: this method is called from constructor, therefore it cannot refer to e.g. a final variable, it can only
     * refer to a static one! Otherwise, it will be null because {@code this} is incomplete at that point.
     *
     * @return all possible error codes - typically enum values
     */
    protected abstract Set<T> getAllErrorCodes();

    private void registerErrorCode(T errorCode) {
        checkNotNull(errorCode, "errorCode");
        registerExceptionClass(errorCode);
        possiblyRegisterRootHttpStatus(errorCode);
    }

    private void registerAllErrorCodes() {
        getAllErrorCodes().forEach(this::registerErrorCode);

        if (exceptionClassToErrorCode.isEmpty()) {
            log.warn("No error codes registered: is getAllErrorCodes() correct?");
        }
    }

    /**
     * Saves error code's exception class in {@link #getExceptionClassToErrorCode()}. Also instantiates the exception
     * class to make sure {@link Exception#Exception(Throwable)} constructor is functional.
     *
     * @param errorCode error code
     * @throws BeanInstantiationException exception class cannot be instantiated
     */
    private void registerExceptionClass(T errorCode) {
        Class<? extends BusinessException> exceptionClass = errorCodeToExceptionClass.apply(errorCode);
        checkNotNull(exceptionClass, errorCode + ".exceptionClass");
        T conflictingErrorCode = exceptionClassToErrorCode.get(exceptionClass);

        // throw exception on collision
        if (conflictingErrorCode != null) {
            throw new IllegalStateException(String.format(Locale.US,
                    "Multiple error codes [%s, %s] refer to the same exception class [%s]",
                    errorCode, conflictingErrorCode, exceptionClass.getName()));
        }

        exceptionClassToErrorCode.put(exceptionClass, errorCode);
    }

    /**
     * Checks if the linked exception is annotated with {@link RootHttpStatus @RootHttpStatus} WITHOUT looking at its
     * superclasses (one of them must be annotated; the point is to find conflicts where subclasses are annotated too).
     * If so, puts the error code to the {@link #exceptionClassToErrorCode} map.
     *
     * @param errorCode error code
     * @throws IllegalStateException if the HTTP status is already mapped to some error code
     */
    private void possiblyRegisterRootHttpStatus(T errorCode) {
        Class<? extends BusinessException> exceptionClass = errorCodeToExceptionClass.apply(errorCode);

        // WARNING: no annotation merging here
        RootHttpStatus rootHttpStatus = AnnotationUtils.getAnnotation(exceptionClass, RootHttpStatus.class);
        if (rootHttpStatus == null) {
            return;
        }

        HttpStatus httpStatus = rootHttpStatus.value();

        // check if HTTP status is already registered
        exceptionClassToHttpStatus.entrySet()
                .stream()
                .filter(entry -> (entry.getValue() == httpStatus) && (entry.getKey() != exceptionClass))
                .findFirst()
                .ifPresentOrElse(
                        entry -> {
                            throw new IllegalStateException(String.format(Locale.US,
                                    "Multiple exception classes declared as roots for HTTP status [%s]: [%s] and [%s]",
                                    httpStatus,
                                    entry.getKey().getName(),
                                    exceptionClass.getName()));
                        },
                        () -> {
                            exceptionClassToHttpStatus.put(exceptionClass, httpStatus);
                            httpStatusToErrorCode.put(httpStatus, errorCode);
                        });
    }

}
