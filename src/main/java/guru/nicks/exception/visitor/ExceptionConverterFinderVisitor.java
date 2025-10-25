package guru.nicks.exception.visitor;

import guru.nicks.designpattern.visitor.ReflectionVisitor;
import guru.nicks.designpattern.visitor.ReflectionVisitorMethod;
import guru.nicks.exception.BusinessException;
import guru.nicks.exception.ExceptionConverter;
import guru.nicks.exception.mapper.ExceptionConverterRegistry;

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
