package guru.nicks.commons.exception;

import guru.nicks.commons.designpattern.SubclassBeforeSuperclassMap;
import guru.nicks.commons.designpattern.visitor.ReflectionVisitor;
import guru.nicks.commons.designpattern.visitor.StatefulReflectionVisitor;
import guru.nicks.commons.exception.http.NotFoundException;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.data.util.Streamable;

import java.util.Iterator;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import static guru.nicks.commons.validation.dsl.ValiDsl.checkNotNull;

/**
 * Iterates over a chain of {@link Throwable#getCause()} in such a way that subclasses go before superclasses, like in
 * {@link SubclassBeforeSuperclassMap}. This is NOT always the same order as in
 * {@code throwable -> cause -> ... -> cause}; it's meant for reacting to more specific exceptions as opposed to more
 * generic ones, no matter where they're in the exception chain.
 * <p>
 * For example, if {@link NotFoundException} is the {@link Exception#getCause()} of a raw {@link Exception}, user will
 * be notified about something missing (HTTP status 404) and not about just 'something went wrong' (HTTP status 500
 * resulting from the raw {@link Exception}).
 * <p>
 * Besides such traditional methods as {@link #hasNext()}, {@link #stream()} is supported. There's no thread safety.
 */
public class SubclassBeforeSuperclassExceptionIterator implements Iterator<Throwable>, Streamable<Throwable> {

    private final Iterator<Throwable> delegate;

    /**
     * Constructor.
     *
     * @param t start of chain (see class comment for iteration order description)
     */
    public SubclassBeforeSuperclassExceptionIterator(Throwable t) {
        checkNotNull(t, "exception chain");

        // sort exception classes
        SubclassBeforeSuperclassMap<Throwable, Throwable> iterationOrder = ExceptionUtils
                .getThrowableList(t)
                .stream()
                .collect(Collectors.toMap(Throwable::getClass, throwable -> throwable,
                        (throwable1, throwable2) -> throwable2,
                        SubclassBeforeSuperclassMap::new));
        delegate = iterationOrder.values().iterator();
    }

    @Override
    public boolean hasNext() {
        return delegate.hasNext();
    }

    @Override
    public Throwable next() {
        return delegate.next();
    }

    @Override
    public Iterator<Throwable> iterator() {
        return this;
    }

    /**
     * Applies visitor to exception chain (for item order, see class comment) until either non-empty {@link Optional} is
     * returned or the chain ends.
     *
     * @param visitor stateless visitor, such as {@link ReflectionVisitor}
     * @param <O>     visitor output type
     * @return non-empty {@link Optional} if visitor has returned something
     */
    public <O> Optional<O> acceptUntilResult(Function<? super Throwable, Optional<O>> visitor) {
        return stream()
                .map(visitor)
                .flatMap(Optional::stream)
                .findFirst();
    }

    /**
     * Applies visitor to exception chain (for item order, see class comment) until either non-empty {@link Optional} is
     * returned or the chain ends.
     * <p>
     * WARNING: exceptions having class equal to this (abstract) class are ignored. First, no explicit objects of this
     * (abstract) class may exist.
     *
     * @param visitor      stateful visitor, such as {@link StatefulReflectionVisitor}
     * @param visitorState visitor state (this method never creates it because it's potentially recursive - may be
     *                     called from within visitors)
     * @param <S>          visitor state type
     * @param <O>          visitor output type
     * @return non-empty {@link Optional} if visitor has returned something
     */
    public <S, O> Optional<O> acceptUntilResult(BiFunction<? super Throwable, ? super S, Optional<O>> visitor,
            S visitorState) {
        return stream()
                .map(t -> visitor.apply(t, visitorState))
                .flatMap(Optional::stream)
                .findFirst();
    }

}
