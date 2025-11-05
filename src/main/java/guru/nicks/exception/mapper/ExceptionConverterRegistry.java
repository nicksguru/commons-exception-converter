package guru.nicks.exception.mapper;

import guru.nicks.exception.BusinessException;
import guru.nicks.exception.ExceptionConverter;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.ConstructorUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExceptionConverterRegistry {

    // DI
    private final List<ExceptionConverter<?, ?>> exceptionConverters;

    private ExceptionConverterWrapperMap exceptionConverterWrappers;

    @PostConstruct
    private void init() {
        ExceptionConverterMap converters = ExceptionConverterMap.of(exceptionConverters);
        exceptionConverterWrappers = ExceptionConverterWrapperMap.of(converters);
        verifyConverters(converters, exceptionConverterWrappers);
        logConverters(converters);
    }

    /**
     * Finds (and caches in 'exception-converter-registry' cache if {@code @Cacheable} provider is present in runtime)
     * exception class converter.
     *
     * @param t exception to find converter for
     * @return optional converter
     */
    @Cacheable(cacheNames = "exception-converter-registry", key = "#t.class.name")
    public Optional<ExceptionConverter<Throwable, ? extends BusinessException>> findConverter(Throwable t) {
        // first try direct lookup
        var converter = exceptionConverterWrappers.get(t.getClass());

        // if not found, find converter (map value) whose target exception class (map key) is a superclass or t
        if (converter == null) {
            converter = exceptionConverterWrappers.entrySet()
                    .stream()
                    .filter(mapEntry -> mapEntry.getKey().isAssignableFrom(t.getClass()))
                    .findFirst()
                    .map(Map.Entry::getValue)
                    .orElse(null);
        }

        // can't log the concrete converter class because it's wrapped in a generic catch-all converter
        log.debug("Looked up (on cache miss) converter for exception [{}]", t.getClass().getName());
        return Optional.ofNullable(converter);
    }

    private void logConverters(ExceptionConverterMap converters) {
        // save time - don't build temporary data structure
        if (!log.isInfoEnabled()) {
            return;
        }

        // print like a JSON map
        log.info("Exception converters: {}", converters.entrySet()
                .stream()
                .map(entry -> "\"" + entry.getKey().getName() + "\":\"" + entry.getValue().getClass().getName() + "\"")
                .collect(Collectors.joining(", ", "{", "}")));
    }

    /**
     * If ExceptionB inherits from ExceptionA, converter for ExceptionB should go before that for ExceptionA - because
     * the latter is applicable to ExceptionB too.
     *
     * @throws IllegalStateException thrown if the above issue has not been solved
     */
    private void verifyConverters(ExceptionConverterMap converters, ExceptionConverterWrapperMap wrappers) {
        long entriesToSkip = 1;

        for (var leftMapEntry : converters.entrySet()) {
            Class<? extends Throwable> leftExceptionClass = leftMapEntry.getKey();
            ExceptionConverter<?, ?> leftExceptionConverter = leftMapEntry.getValue();

            // For each key to the right of the current one (i.e. starting with entriesToSkip index), ensure that none
            // of its subclasses are there. This is not a NavigableMap, so there's no tailMap method.
            converters.entrySet()
                    .stream()
                    .skip(entriesToSkip)
                    .forEach(rightMapEntry -> {
                        Class<? extends Throwable> rightExceptionClass = rightMapEntry.getKey();

                        if (leftExceptionClass.isAssignableFrom(rightExceptionClass)) {
                            ExceptionConverter<?, ?> rightExceptionConverter = rightMapEntry.getValue();

                            throw new IllegalStateException("Wrong map key order: exception converter ["
                                    + rightExceptionConverter.getClass().getName()
                                    + "] which converts [" + rightExceptionClass.getName()
                                    + "] is masked by early occurrence of ["
                                    + leftExceptionConverter.getClass().getName()
                                    + "] which converts its superclass [" + leftExceptionClass.getName() + "]");
                        }
                    });

            entriesToSkip++;
        }

        // invoke exception converters
        wrappers.forEach((clazz, converter) -> {
            Throwable exception;
            boolean exceptionArgumentIsCorrect = false;

            // it's not always possible to instantiate the needed exception class because of different constructors
            try {
                exception = ConstructorUtils.invokeConstructor(clazz, "test message");
                exceptionArgumentIsCorrect = true;
            } catch (NoSuchMethodException | InstantiationException
                     | IllegalAccessException | InvocationTargetException e) {
                exception = new Exception();
            }

            try {
                converter.convert(exception);
            }
            // No error is expected if exception class is exactly what's expected by this converter. Otherwise,
            // converter should throw ClassCastException because it expects a certain argument class.
            catch (Exception e) {
                if (exceptionArgumentIsCorrect || !(ExceptionUtils.getRootCause(e) instanceof ClassCastException)) {
                    throw new IllegalStateException("Unexpected error from " + converter.getClass().getName(), e);
                }
            }
        });
    }

}
