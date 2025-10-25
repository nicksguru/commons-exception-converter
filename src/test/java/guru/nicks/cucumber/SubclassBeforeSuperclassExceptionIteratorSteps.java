package guru.nicks.cucumber;

import guru.nicks.cucumber.world.TextWorld;
import guru.nicks.exception.SubclassBeforeSuperclassExceptionIterator;
import guru.nicks.exception.http.NotFoundException;
import guru.nicks.exception.http.UnauthorizedException;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Step definitions for {@link SubclassBeforeSuperclassExceptionIterator} testing.
 */
@RequiredArgsConstructor
public class SubclassBeforeSuperclassExceptionIteratorSteps {

    // DI
    private final TextWorld textWorld;

    @Getter
    @Setter
    private Throwable exceptionChain;

    @Getter
    @Setter
    private SubclassBeforeSuperclassExceptionIterator iterator;

    @Getter
    @Setter
    private Function<Throwable, Optional<String>> statelessVisitor;

    @Getter
    @Setter
    private BiFunction<Throwable, VisitorState, Optional<String>> statefulVisitor;

    @Getter
    @Setter
    private VisitorState visitorState;

    @Getter
    @Setter
    private Optional<String> visitorResult;

    @Getter
    @Setter
    private Stream<Throwable> iteratorStream;

    @Getter
    @Setter
    private List<String> iterationOrder;

    @Given("an exception chain with types {string}")
    public void anExceptionChainWithTypes(String exceptionTypes) {
        var typeNames = Arrays.stream(exceptionTypes.split(","))
                .map(String::strip)
                .toList();

        exceptionChain = createExceptionChain(typeNames);
    }

    @Given("an exception chain with duplicate exception types")
    public void anExceptionChainWithDuplicateExceptionTypes() {
        // create chain: RuntimeException -> RuntimeException -> Exception
        var innerException = new Exception("Root cause");
        var middleException = new RuntimeException("Middle", innerException);
        exceptionChain = new RuntimeException("Outer", middleException);
    }

    @Given("a stateless visitor that returns result for {string}")
    public void aStatelessVisitorThatReturnsResultFor(String targetType) {
        statelessVisitor = throwable -> {
            if (throwable.getClass().getSimpleName().equals(targetType)) {
                return Optional.of("found");
            }

            return Optional.empty();
        };
    }

    @Given("a stateful visitor that returns result for {string}")
    public void aStatefulVisitorThatReturnsResultFor(String targetType) {
        statefulVisitor = (throwable, state) -> {
            state.incrementVisitCount();

            if (throwable.getClass().getSimpleName().equals(targetType)) {
                return Optional.of("found");
            }

            return Optional.empty();
        };
    }

    @Given("visitor state is initialized")
    public void visitorStateIsInitialized() {
        visitorState = new VisitorState();
    }

    @When("a SubclassBeforeSuperclassExceptionIterator is created with null exception")
    public void aSubclassBeforeSuperclassExceptionIteratorIsCreatedWithNullException() {
        textWorld.setLastException(catchThrowable(() ->
                new SubclassBeforeSuperclassExceptionIterator(null)));
    }

    @When("a SubclassBeforeSuperclassExceptionIterator is created with the exception chain")
    public void aSubclassBeforeSuperclassExceptionIteratorIsCreatedWithTheExceptionChain() {
        textWorld.setLastException(catchThrowable(() ->
                iterator = new SubclassBeforeSuperclassExceptionIterator(exceptionChain)));
    }

    @When("acceptUntilResult is called with the stateless visitor")
    public void acceptUntilResultIsCalledWithTheStatelessVisitor() {
        visitorResult = iterator.acceptUntilResult(statelessVisitor);
    }

    @When("acceptUntilResult is called with the stateful visitor and state")
    public void acceptUntilResultIsCalledWithTheStatefulVisitorAndState() {
        visitorResult = iterator.acceptUntilResult(statefulVisitor, visitorState);
    }

    @Then("the iterator should have next elements")
    public void theIteratorShouldHaveNextElements() {
        assertThat(iterator.hasNext())
                .as("iterator should have next elements")
                .isTrue();
    }

    @Then("the iteration order should be {string}")
    public void theIterationOrderShouldBe(String expectedOrder) {
        var actualOrder = new ArrayList<String>();

        while (iterator.hasNext()) {
            var exception = iterator.next();
            actualOrder.add(exception.getClass().getSimpleName());
        }

        var expectedOrderList = Arrays.stream(expectedOrder.split(","))
                .map(String::strip)
                .toList();

        assertThat(actualOrder)
                .as("iteration order")
                .isEqualTo(expectedOrderList);
    }

    @Then("the iterator should support stream operations")
    public void theIteratorShouldSupportStreamOperations() {
        iteratorStream = iterator.stream();

        assertThat(iteratorStream)
                .as("iterator stream")
                .isNotNull();
    }

    @Then("the stream should contain the same elements as iteration")
    public void theStreamShouldContainTheSameElementsAsIteration() {
        var streamElements = iteratorStream
                .map(throwable -> throwable.getClass().getSimpleName())
                .toList();

        // create new iterator for comparison
        var comparisonIterator = new SubclassBeforeSuperclassExceptionIterator(exceptionChain);
        var iteratorElements = new ArrayList<String>();

        while (comparisonIterator.hasNext()) {
            var exception = comparisonIterator.next();
            iteratorElements.add(exception.getClass().getSimpleName());
        }

        assertThat(streamElements)
                .as("stream elements")
                .isEqualTo(iteratorElements);
    }

    @Then("the visitor result should be {string}")
    public void theVisitorResultShouldBe(String expectedResult) {
        if ("empty".equals(expectedResult)) {
            assertThat(visitorResult)
                    .as("visitor result")
                    .isEmpty();
        } else {
            assertThat(visitorResult)
                    .as("visitor result")
                    .isPresent()
                    .get()
                    .isEqualTo(expectedResult);
        }
    }

    @Then("the visitor state should be updated")
    public void theVisitorStateShouldBeUpdated() {
        assertThat(visitorState.getVisitCount())
                .as("visitor state visit count")
                .isGreaterThan(0);
    }

    @Then("calling iterator method should return the same instance")
    public void callingIteratorMethodShouldReturnTheSameInstance() {
        Iterator<Throwable> returnedIterator = iterator.iterator();

        assertThat(returnedIterator)
                .as("returned iterator")
                .isSameAs(iterator);
    }

    @Then("the iterator should process unique exception types only")
    public void theIteratorShouldProcessUniqueExceptionTypesOnly() {
        var processedTypes = new ArrayList<Class<?>>();

        while (iterator.hasNext()) {
            var exception = iterator.next();
            processedTypes.add(exception.getClass());
        }

        var uniqueTypes = processedTypes.stream()
                .distinct()
                .toList();

        assertThat(processedTypes)
                .as("processed exception types")
                .hasSameSizeAs(uniqueTypes);
    }

    @Then("the iteration should complete successfully")
    public void theIterationShouldCompleteSuccessfully() {
        assertThat(iterator.hasNext())
                .as("iterator has no more elements")
                .isFalse();
    }

    /**
     * Creates an exception chain based on the provided type names.
     *
     * @param typeNames list of exception type names
     * @return root exception of the chain
     */
    private Throwable createExceptionChain(List<String> typeNames) {
        if (typeNames.isEmpty()) {
            return new Exception("Empty chain");
        }

        Throwable current = null;

        for (int i = typeNames.size() - 1; i >= 0; i--) {
            var typeName = typeNames.get(i);
            current = createExceptionByName(typeName, current);
        }

        return current;
    }

    /**
     * Creates an exception instance by type name.
     *
     * @param typeName exception type name
     * @param cause    exception cause
     * @return exception instance
     */
    private Throwable createExceptionByName(String typeName, Throwable cause) {
        return switch (typeName) {
            case "Exception" -> new Exception("Exception message", cause);
            case "RuntimeException" -> new RuntimeException("RuntimeException message", cause);
            case "NotFoundException" -> new NotFoundException("NotFoundException message", cause);
            case "UnauthorizedException" -> new UnauthorizedException("UnauthorizedException message", cause);
            default -> throw new IllegalArgumentException("Unknown exception type: '" + typeName + "'");
        };
    }

    /**
     * Visitor state for testing stateful visitors.
     */
    @Getter
    @Setter
    public static class VisitorState {
        private int visitCount = 0;

        public void incrementVisitCount() {
            visitCount++;
        }
    }

}
