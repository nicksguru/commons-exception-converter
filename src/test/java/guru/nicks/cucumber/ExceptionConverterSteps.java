package guru.nicks.cucumber;

import guru.nicks.exception.BusinessException;
import guru.nicks.exception.ExceptionConverter;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import jakarta.annotation.Nonnull;
import lombok.Getter;
import org.apache.commons.beanutils.ConstructorUtils;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.BeanUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for testing {@link ExceptionConverter}.
 */
public class ExceptionConverterSteps {

    private AutoCloseable closeableMocks;
    private ExceptionConverter<? extends Throwable, ? extends BusinessException> converter;
    private Throwable sourceException;
    private BusinessException convertedException;
    private Class<?> retrievedSourceClass;
    private Class<?> retrievedTargetClass;

    @Before
    public void beforeEachScenario() {
        closeableMocks = MockitoAnnotations.openMocks(this);
    }

    @After
    public void afterEachScenario() throws Exception {
        closeableMocks.close();
    }

    @Given("an exception converter from {string} to TestBusinessException")
    public void anExceptionConverterFor(String sourceType) throws ClassNotFoundException {
        Class<? extends Throwable> sourceClass = (Class<? extends Throwable>)
                getClass().getClassLoader().loadClass(sourceType);
        converter = createConverterForTypes(sourceClass, TestBusinessException.class);
    }

    @Given("a custom exception converter with specific conversion logic")
    public void aCustomExceptionConverterWithSpecificConversionLogic() {
        converter = new CustomExceptionConverter();
    }

    @When("the converter is used to convert an exception")
    public void theConverterIsUsedToConvertAnException() {
        // create an instance of the source exception type
        sourceException = BeanUtils.instantiateClass(converter.getSourceClass());
        convertedException = ((ExceptionConverter<Throwable, BusinessException>)
                converter).convert(sourceException);
    }

    @When("the converter is used to convert an exception with message {string}")
    public void theConverterIsUsedToConvertAnExceptionWithMessage(String message) throws Exception {
        // create an instance of the source exception type with the given message
        sourceException = ConstructorUtils.invokeConstructor(converter.getSourceClass(), message);
        convertedException = ((ExceptionConverter<Throwable, BusinessException>) converter).convert(sourceException);
    }

    @When("the source class is retrieved from the converter")
    public void theSourceClassIsRetrievedFromTheConverter() {
        retrievedSourceClass = converter.getSourceClass();
    }

    @When("the target class is retrieved from the converter")
    public void theTargetClassIsRetrievedFromTheConverter() {
        retrievedTargetClass = converter.getTargetClass();
    }

    @Then("the converted exception should be of the target type")
    public void theConvertedExceptionShouldBeOfTheTargetType() {
        assertThat(convertedException)
                .as("convertedException")
                .isNotNull()
                .isInstanceOf(TestBusinessException.class);
    }

    @Then("the converted exception should have the original exception as cause")
    public void theConvertedExceptionShouldHaveTheOriginalExceptionAsCause() {
        assertThat(convertedException.getCause())
                .as("convertedException.cause")
                .isEqualTo(sourceException);
    }

    @Then("the source class should be {string}")
    public void theSourceClassShouldBe(String expectedClassName) throws Exception {
        Class<?> expectedClass = Class.forName(expectedClassName);
        assertThat(retrievedSourceClass)
                .as("retrievedSourceClass")
                .isEqualTo(expectedClass);
    }

    @Then("the target class should be TestBusinessException")
    public void theTargetClassShouldBeTestBusinessException() {
        assertThat(retrievedTargetClass)
                .as("retrievedTargetClass")
                .isEqualTo(TestBusinessException.class);
    }

    @Then("the converted exception should contain the original message")
    public void theConvertedExceptionShouldContainTheOriginalMessage() {
        assertThat(convertedException)
                .as("convertedException")
                .isInstanceOf(CustomBusinessException.class);

        CustomBusinessException customException = (CustomBusinessException) convertedException;
        assertThat(customException.getCustomMessage())
                .as("customException.customMessage")
                .isEqualTo(sourceException.getMessage());
    }

    /**
     * Creates an exception converter for the given source and target types.
     *
     * @param <S> the source exception class type
     * @param <T> the target business exception class type
     * @return the exception converter
     */
    private <S extends Throwable, T extends BusinessException> ExceptionConverter<S, T> createConverterForTypes(
            Class<S> sourceClass, Class<T> targetClass) {
        return new ExceptionConverter<>() {

            @Nonnull
            @Override
            public Class<S> getSourceClass() {
                return sourceClass;
            }

            @Nonnull
            @Override
            public Class<T> getTargetClass() {
                return targetClass;
            }
        };
    }

    /**
     * Test implementation of {@link BusinessException}.
     */
    public static class TestBusinessException extends BusinessException {

        public TestBusinessException(Throwable cause) {
            super(cause);
        }

    }

    /**
     * Custom business exception with additional fields.
     */
    public static class CustomBusinessException extends BusinessException {

        @Getter
        private final String customMessage;

        public CustomBusinessException(String customMessage) {
            super();
            this.customMessage = customMessage;
        }

    }

    /**
     * Custom exception converter with specific conversion logic.
     */
    public static class CustomExceptionConverter
            implements ExceptionConverter<IllegalArgumentException, CustomBusinessException> {

        @Nonnull
        @Override
        public CustomBusinessException convert(@Nonnull IllegalArgumentException cause) {
            return new CustomBusinessException(cause.getMessage());
        }

    }

}
