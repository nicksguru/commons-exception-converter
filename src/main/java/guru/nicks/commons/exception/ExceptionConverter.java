package guru.nicks.commons.exception;

import guru.nicks.commons.utils.ReflectionUtils;

import org.springframework.core.convert.converter.Converter;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Each implementing class should be a Spring bean. All such beans are picked by the converter registry. The goal is to
 * map 3rd party exceptions to {@link BusinessException} and then to custom DTO.
 * <p>
 * {@link Exception#getMessage()} is never revealed to caller, for security reasons. Instead, each error code is
 * supposed to have a translation (the translation dictionary can be downloaded by client apps) and an English fallback
 * message.
 *
 * @param <S> source exception type (root for its subclasses)
 * @param <T> target exception type
 */
public interface ExceptionConverter<S extends Throwable, T extends BusinessException> extends Converter<S, T> {

    /**
     * @see #getTargetClass()
     */
    Class<?> STATIC_THIS = MethodHandles.lookup().lookupClass();

    /**
     * Cache for constructor handles to avoid repeated expensive lookups.
     */
    ConcurrentHashMap<Class<? extends BusinessException>, MethodHandle> CONSTRUCTOR_CACHE = new ConcurrentHashMap<>();

    /**
     * Uses {@link MethodHandles} for efficient constructor invocation with caching to avoid repeated expensive
     * lookups.
     *
     * @param cause original exception, becomes {@link Exception#getCause()}
     * @return converted exception
     */
    @Override
    default T convert(S cause) {
        var exceptionClass = getTargetClass();
        var constructorHandle = CONSTRUCTOR_CACHE.computeIfAbsent(exceptionClass, clazz -> {
            try {
                var lookup = MethodHandles.publicLookup();
                var constructorType = MethodType.methodType(void.class, Throwable.class);
                return lookup.findConstructor(clazz, constructorType);
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new IllegalStateException("Failed to find constructor for [" + clazz.getName() + "]: "
                        + e.getMessage(), e);
            }
        });

        try {
            @SuppressWarnings("unchecked")
            T result = (T) constructorHandle.invoke(cause);
            return result;
        } catch (Throwable e) {
            throw new IllegalStateException("Failed to instantiate " + exceptionClass.getName(), e);
        }
    }

    /**
     * Extracts {@code S} class out of generic class parameter.
     * <p>
     * Both {@code S} and {@code T} are {@link Throwable}, so this method depends on their order: {@code S} goes first
     * and therefore is found first. This order is not supposed to ever change because {@link Converter} is part of
     * Spring.
     *
     * @return exception class to be mapped
     * @throws IllegalStateException if {@code S} is not found
     */
    @SuppressWarnings("unchecked")
    default Class<S> getSourceClass() {
        return (Class<S>) ReflectionUtils
                .findMaterializedGenericType(getClass(), STATIC_THIS, Throwable.class)
                .filter(sourceClass -> !sourceClass.isInstance(BusinessException.class))
                .orElseThrow(() -> new IllegalStateException("Missing generic source class parameter in "
                        + getClass().getName()));
    }

    /**
     * Extracts {@code T} class out of generic parameter.
     *
     * @return exception class {@code S} is mapped to
     * @throws IllegalStateException if {@code T} is not found
     */
    @SuppressWarnings("unchecked")
    default Class<T> getTargetClass() {
        return (Class<T>) ReflectionUtils
                .findMaterializedGenericType(getClass(), STATIC_THIS, BusinessException.class)
                .orElseThrow(() -> new IllegalStateException("Missing generic target class parameter in "
                        + getClass().getName()));
    }

}
