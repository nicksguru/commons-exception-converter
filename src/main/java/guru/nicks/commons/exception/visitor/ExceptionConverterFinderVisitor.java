package guru.nicks.commons.exception.visitor;

import guru.nicks.commons.designpattern.visitor.ReflectionVisitor;
import guru.nicks.commons.designpattern.visitor.ReflectionVisitorMethod;
import guru.nicks.commons.exception.BusinessException;
import guru.nicks.commons.exception.ExceptionConverter;
import guru.nicks.commons.exception.mapper.ExceptionConverterRegistry;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Attempts to find and apply an appropriate {@link ExceptionConverter} to the given exception using
 * {@link ExceptionConverterRegistry} bean.
 */
@Component
@RequiredArgsConstructor
public class ExceptionConverterFinderVisitor extends ReflectionVisitor<BusinessException> {

    // DI
    private final ExceptionConverterRegistry exceptionConverterRegistry;

    @ReflectionVisitorMethod
    public Optional<BusinessException> visit(Throwable t) {
        return exceptionConverterRegistry
                .findConverter(t)
                .map(converter -> converter.convert(t));
    }

}
