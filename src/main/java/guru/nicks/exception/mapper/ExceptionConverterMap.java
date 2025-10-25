package guru.nicks.exception.mapper;

import guru.nicks.designpattern.SubclassBeforeSuperclassMap;
import guru.nicks.exception.ExceptionConverter;

/**
 * Keys are exception classes (hierarchy roots), values are their mappers. Key order defines priority: subclasses go
 * before their parent classes, otherwise their mappers will not come into play.
 *
 * @see ExceptionConverterRegistry
 */
public class ExceptionConverterMap extends SubclassBeforeSuperclassMap<Throwable, ExceptionConverter<?, ?>> {

    /**
     * Extracts {@code T} class out of each {@code ExceptionConverter<T>} and arranges them in correct order: subclasses
     * go before their parent classes.
     *
     * @throws IllegalStateException failed to retrieve exception class (no or wrong generic type) or converter already
     *                               exists for exception
     */
    public static ExceptionConverterMap of(Iterable<ExceptionConverter<?, ?>> exceptionConverters) {
        var map = new ExceptionConverterMap();

        for (var exceptionConverter : exceptionConverters) {
            Class<? extends Throwable> sourceClass = exceptionConverter.getSourceClass();
            ExceptionConverter<?, ?> existingConverter = map.get(sourceClass);

            if (existingConverter != null) {
                throw new IllegalStateException("Collision detected for exception converter ["
                        + exceptionConverter.getClass().getName()
                        + "]: its source class [" + sourceClass.getName()
                        + "] is already converted by [" + existingConverter.getClass().getName() + "]");
            }

            map.put(sourceClass, exceptionConverter);
        }

        return map;
    }

}
