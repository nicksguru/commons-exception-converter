package guru.nicks.commons.cucumber;

import guru.nicks.commons.exception.BusinessException;
import guru.nicks.commons.exception.ExceptionConverter;
import guru.nicks.commons.exception.mapper.ExceptionConverterRegistry;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for testing {@link ExceptionConverterRegistry}.
 */
public class ExceptionConverterRegistrySteps {

    private ExceptionConverterRegistry registry;
    private List<ExceptionConverter<?, ?>> converters;

    private Optional<ExceptionConverter<Throwable, ? extends BusinessException>> foundConverter;
    private Throwable testException;
    private BusinessException convertedBusinessException;

    @Given("an exception converter registry with converters for different exception types")
    public void anExceptionConverterRegistryWithConvertersForDifferentExceptionTypes() throws Exception {
        converters = createTestConverters();
        registry = new ExceptionConverterRegistry(converters);

        // Call init method using reflection since it's private
        Method initMethod = registry.getClass().getDeclaredMethod("init");
        initMethod.setAccessible(true);
        initMethod.invoke(registry);
    }

    @Given("an exception converter registry with converters")
    public void anExceptionConverterRegistryWithConverters() {
        converters = createTestConverters();
        registry = new ExceptionConverterRegistry(converters);
    }

    @When("a converter is requested for exception type {string}")
    public void aConverterIsRequestedForExceptionType(String exceptionType) throws Exception {
        Class<?> exceptionClass = Class.forName(exceptionType);
        testException = (Throwable) exceptionClass.getDeclaredConstructor().newInstance();
        foundConverter = registry.findConverter(testException);
    }

    @When("the registry is initialized")
    public void theRegistryIsInitialized() throws Exception {
        Method initMethod = registry.getClass().getDeclaredMethod("init");
        initMethod.setAccessible(true);
        initMethod.invoke(registry);
    }

    @When("an exception of type {string} is converted")
    public void anExceptionOfTypeIsConverted(String exceptionType) throws Exception {
        Class<?> exceptionClass = Class.forName(exceptionType);
        testException = (Throwable) exceptionClass.getDeclaredConstructor().newInstance();
        foundConverter = registry.findConverter(testException);

        foundConverter.ifPresent(throwableExceptionConverter ->
                convertedBusinessException = throwableExceptionConverter.convert(testException));
    }

    @Then("no converter should be found")
    public void noConverterShouldBeFound() {
        assertThat(foundConverter)
                .as("foundConverter")
                .isEmpty();
    }

    @Then("the converter can convert the exception")
    public void theConverterCanConvertTheException() {
        assertThat(foundConverter)
                .as("foundConverter")
                .isPresent();

        // We can't directly check the source class of the converter because it's wrapped,
        // but we can verify it's the right type by checking if it can convert the exception
        BusinessException converted = foundConverter.get().convert(testException);
        assertThat(converted)
                .as("converted")
                .isNotNull();
    }

    @Then("the result should be a business exception")
    public void theResultShouldBeABusinessException() {
        assertThat(convertedBusinessException)
                .as("convertedBusinessException")
                .isNotNull()
                .isInstanceOf(BusinessException.class);
    }

    @Then("a converter should be found")
    public void aConverterShouldOrShouldNotBeFound() {
        assertThat(foundConverter)
                .as("foundConverter")
                .isPresent();
    }

    @Then("a converter should not be found")
    public void aConverterShouldNotBeFound() {
        assertThat(foundConverter)
                .as("foundConverter")
                .isEmpty();
    }

    /**
     * Creates a list of test converters. The order (subclasses before superclasses) doesn't matter because
     * {@link ExceptionConverterRegistry} sorts them by their source classes.
     *
     * @return list of test converters
     */
    private List<ExceptionConverter<?, ?>> createTestConverters() {
        List<ExceptionConverter<?, ?>> testConverters = new ArrayList<>();

        // Add converters in correct order: subclasses before superclasses
        testConverters.add(new TestExceptionConverter<>(ArrayIndexOutOfBoundsException.class));
        testConverters.add(new TestExceptionConverter<>(IllegalArgumentException.class));
        testConverters.add(new TestExceptionConverter<>(NullPointerException.class));
        testConverters.add(new TestExceptionConverter<>(UnsupportedOperationException.class));

        // WARNING: no catch-all converters, otherwise 'no converter found' test will never succeed
        //testConverters.add(new TestExceptionConverter<>(RuntimeException.class));
        //testConverters.add(new TestExceptionConverter<>(Exception.class));

        return testConverters;
    }

    /**
     * Test implementation of {@link ExceptionConverter}.
     *
     * @param <T> the exception type
     */
    @RequiredArgsConstructor
    private static class TestExceptionConverter<T extends Throwable>
            implements ExceptionConverter<T, TestBusinessException> {

        private final Class<T> sourceClass;

        @Nonnull
        @Override
        public TestBusinessException convert(@Nonnull T cause) {
            return new TestBusinessException(cause);
        }

        @Nonnull
        @Override
        public Class<T> getSourceClass() {
            return sourceClass;
        }

    }

    /**
     * Test implementation of {@link BusinessException}.
     */
    private static class TestBusinessException extends BusinessException {

        public TestBusinessException(Throwable cause) {
            super(cause);
        }

    }

}
