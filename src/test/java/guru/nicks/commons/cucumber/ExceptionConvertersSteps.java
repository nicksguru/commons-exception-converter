package guru.nicks.commons.cucumber;

import guru.nicks.commons.cucumber.world.TextWorld;
import guru.nicks.commons.exception.BusinessException;
import guru.nicks.commons.exception.ExceptionConverter;
import guru.nicks.commons.exception.converter.AccessDeniedExceptionConverter;
import guru.nicks.commons.exception.converter.ConnectExceptionConverter;
import guru.nicks.commons.exception.converter.MissingRequestValueExceptionConverter;
import guru.nicks.commons.exception.converter.MultipartExceptionConverter;
import guru.nicks.commons.exception.converter.SecurityExceptionConverter;
import guru.nicks.commons.exception.converter.UnsupportedOperationExceptionConverter;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;
import org.apache.commons.beanutils.ConstructorUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MissingRequestValueException;
import org.springframework.web.multipart.MultipartException;

import java.net.ConnectException;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for testing exception converter implementations in the
 * {@link guru.nicks.commons.exception.converter} package.
 */
@RequiredArgsConstructor
public class ExceptionConvertersSteps {

    // DI
    private final TextWorld textWorld;

    // map of exception type names to their actual classes
    private final Map<String, Class<? extends Throwable>> exceptionTypeMap = new HashMap<>();
    // map of exception type names to their appropriate converters
    private final Map<String, ExceptionConverter<?, ?>> converterMap = new HashMap<>();

    private Throwable sourceException;
    private BusinessException convertedException;

    @Before
    public void beforeEachScenario() {
        // exception type map
        exceptionTypeMap.put("java.net.ConnectException", ConnectException.class);
        exceptionTypeMap.put("java.lang.SecurityException", SecurityException.class);
        exceptionTypeMap.put("java.lang.IllegalArgumentException", IllegalArgumentException.class);
        exceptionTypeMap.put("org.apache.commons.lang3.NotImplementedException", NotImplementedException.class);
        exceptionTypeMap.put("org.springframework.security.access.AccessDeniedException", AccessDeniedException.class);
        exceptionTypeMap.put("org.springframework.web.multipart.MultipartException", MultipartException.class);
        exceptionTypeMap.put("org.springframework.web.bind.MissingRequestValueException",
                MissingRequestValueException.class);

        // exception converter map
        converterMap.put("java.net.ConnectException", new ConnectExceptionConverter());
        converterMap.put("java.lang.SecurityException", new SecurityExceptionConverter());
        converterMap.put("org.apache.commons.lang3.NotImplementedException",
                new UnsupportedOperationExceptionConverter());
        converterMap.put("org.springframework.security.access.AccessDeniedException",
                new AccessDeniedExceptionConverter());
        converterMap.put("org.springframework.web.multipart.MultipartException", new MultipartExceptionConverter());
        converterMap.put("org.springframework.web.bind.MissingRequestValueException",
                new MissingRequestValueExceptionConverter());
    }

    @Given("an exception of type {string} with message {string}")
    public void anExceptionOfTypeWithMessage(String exceptionType, String message) {
        try {
            Class<? extends Throwable> exceptionClass = exceptionTypeMap.get(exceptionType);

            if (exceptionClass != null) {
                sourceException = ConstructorUtils.invokeConstructor(exceptionClass, message);
            }
            // fallback to loading the class dynamically
            else {
                Class<?> dynamicClass = getClass()
                        .getClassLoader()
                        .loadClass(exceptionType);
                sourceException = (Throwable) ConstructorUtils.invokeConstructor(dynamicClass, message);
            }
        } catch (Exception e) {
            textWorld.setLastException(e);
        }
    }

    @When("the appropriate converter converts the exception")
    public void theAppropriateConverterConvertsTheException() {
        String exceptionClassName = sourceException.getClass().getName();
        var appropriateConverter = (ExceptionConverter<Throwable, BusinessException>)
                converterMap.get(exceptionClassName);

        if (appropriateConverter != null) {
            convertedException = appropriateConverter.convert(sourceException);
        } else {
            textWorld.setLastException(new IllegalArgumentException("No converter found for " + exceptionClassName));
        }
    }

    @Then("the result should be a {string}")
    public void theResultShouldBeA(String resultTypeName) {
        Class<?> expectedClass = null;

        // map the result type name to the actual class
        try {
            expectedClass = Class.forName(resultTypeName);
        } catch (ClassNotFoundException e) {
            textWorld.setLastException(e);
        }

        assertThat(convertedException)
                .as("convertedException")
                .isNotNull()
                .isInstanceOf(expectedClass);
    }

    @Then("the original exception should be the cause")
    public void theOriginalExceptionShouldBeTheCause() {
        assertThat(convertedException.getCause())
                .as("convertedException.cause")
                .isEqualTo(sourceException);
    }

}
