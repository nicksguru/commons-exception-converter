package guru.nicks.commons.cucumber;

import guru.nicks.commons.cucumber.world.TextWorld;
import guru.nicks.commons.exception.visitor.FieldErrorDiscovererVisitor;
import guru.nicks.commons.rest.v1.dto.FieldErrorDto;
import guru.nicks.commons.rest.v1.mapper.FieldErrorMapper;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.DataTableType;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.apache.commons.lang3.StringUtils;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RequiredArgsConstructor
public class FieldErrorDiscovererVisitorSteps {

    // DI
    private final TextWorld textWorld;

    @Mock
    private FieldErrorMapper fieldErrorMapper;
    @Spy
    private FieldErrorDiscovererVisitor visitor = new FieldErrorDiscovererVisitor(fieldErrorMapper);

    private AutoCloseable closeableMocks;
    private Exception testException;
    private Optional<List<FieldErrorDto>> result;

    @Before
    public void beforeEachScenario() {
        closeableMocks = MockitoAnnotations.openMocks(this);
        visitor = new FieldErrorDiscovererVisitor(fieldErrorMapper);
    }

    @After
    public void afterEachScenario() throws Exception {
        closeableMocks.close();
    }

    @DataTableType
    public FieldErrorData createFieldErrorData(Map<String, String> entry) {
        return FieldErrorData.builder()
                .fieldName(StringUtils.isNotBlank(entry.get("fieldName")) ? entry.get("fieldName") : null)
                .errorCode(StringUtils.isNotBlank(entry.get("errorCode")) ? entry.get("errorCode") : null)
                .errorMessage(StringUtils.isNotBlank(entry.get("errorMessage")) ? entry.get("errorMessage") : null)
                .build();
    }

    @Given("a ConstraintViolationException with violation property path {string} and message {string}")
    public void aConstraintViolationExceptionWithViolationPropertyPathAndMessage(String propertyPath, String message) {
        var constraintViolation = mock(ConstraintViolation.class);
        var path = mock(Path.class);
        var pathNode = mock(Path.Node.class);

        when(pathNode.getName())
                .thenReturn(extractLastPathSegment(propertyPath));

        // create a list with multiple path nodes to simulate the full path
        var pathNodes = createPathNodes(propertyPath);
        when(path.iterator())
                .thenReturn(pathNodes.iterator());
        when(constraintViolation.getPropertyPath())
                .thenReturn(path);
        when(constraintViolation.getMessage())
                .thenReturn(message);

        var violations = Set.of((ConstraintViolation<?>) constraintViolation);
        testException = new ConstraintViolationException(violations);
    }

    @Given("a MissingServletRequestParameterException with parameter name {string}")
    public void aMissingServletRequestParameterExceptionWithParameterName(String parameterName) {
        testException = new MissingServletRequestParameterException(parameterName, "String");
    }

    @Given("a MethodArgumentTypeMismatchException with name {string}, error code {string}, and message {string}")
    public void aMethodArgumentTypeMismatchExceptionWithNameErrorCodeAndMessage(String name, String errorCode,
            String message) {
        var exception = mock(MethodArgumentTypeMismatchException.class);

        when(exception.getName())
                .thenReturn(name);

        when(exception.getErrorCode())
                .thenReturn(errorCode);

        when(exception.getMessage())
                .thenReturn(message);

        testException = exception;
    }

    @Given("a MethodArgumentNotValidException with field errors")
    public void aMethodArgumentNotValidExceptionWithFieldErrors(DataTable dataTable) {
        var fieldErrors = dataTable.asList(FieldErrorData.class);
        var bindingResult = mock(org.springframework.validation.BindingResult.class);

        var springFieldErrors = fieldErrors.stream()
                .map(this::createSpringFieldError)
                .toList();

        when(bindingResult.getFieldErrors())
                .thenReturn(springFieldErrors);

        var exception = mock(MethodArgumentNotValidException.class);
        when(exception.getBindingResult())
                .thenReturn(bindingResult);

        // mock the field error mapper
        for (int i = 0; i < fieldErrors.size(); i++) {
            var fieldErrorData = fieldErrors.get(i);
            var springFieldError = springFieldErrors.get(i);

            var dto = FieldErrorDto.builder()
                    .fieldName(fieldErrorData.getFieldName())
                    .errorCode(fieldErrorData.getErrorCode())
                    .errorMessage(fieldErrorData.getErrorMessage())
                    .build();
            when(fieldErrorMapper.toDto(springFieldError))
                    .thenReturn(dto);
        }

        testException = exception;
    }

    @Given("a BindException with field errors")
    public void aBindExceptionWithFieldErrors(DataTable dataTable) {
        var fieldErrors = dataTable.asList(FieldErrorData.class);
        var bindException = mock(BindException.class);

        var springFieldErrors = fieldErrors.stream()
                .map(this::createSpringFieldError)
                .toList();

        when(bindException.getFieldErrors())
                .thenReturn(springFieldErrors);

        // mock the field error mapper
        for (int i = 0; i < fieldErrors.size(); i++) {
            var fieldErrorData = fieldErrors.get(i);
            var springFieldError = springFieldErrors.get(i);

            var dto = FieldErrorDto.builder()
                    .fieldName(fieldErrorData.getFieldName())
                    .errorCode(fieldErrorData.getErrorCode())
                    .errorMessage(fieldErrorData.getErrorMessage())
                    .build();
            when(fieldErrorMapper.toDto(springFieldError))
                    .thenReturn(dto);
        }

        testException = bindException;
    }

    @Given("a ValidationException with cause type {string}")
    public void aValidationExceptionWithCauseType(String causeType) {
        Exception cause = switch (causeType) {
            case "BindException" -> {
                var bindException = mock(BindException.class);
                when(bindException.getFieldErrors())
                        .thenReturn(List.of());
                yield bindException;
            }

            case "RuntimeException" -> new RuntimeException("Test cause");
            default -> throw new IllegalArgumentException("Unknown cause type: " + causeType);
        };

        testException = new jakarta.validation.ValidationException("Validation failed", cause);
    }

    @Given("an unknown exception type")
    public void anUnknownExceptionType() {
        testException = new UnsupportedOperationException("Unknown exception");
    }

    @Given("a null exception")
    public void aNullException() {
        testException = null;
    }

    @When("the visitor processes the exception")
    public void theVisitorProcessesTheException() {
        result = visitor.apply(testException);
    }

    @Then("field errors should be discovered")
    public void fieldErrorsShouldBeDiscovered() {
        assertThat(result)
                .as("result")
                .isPresent();
    }

    @Then("field errors should be empty")
    public void fieldErrorsShouldBeEmpty() {
        assertThat(result)
                .as("result")
                .isEmpty();
    }

    @And("the field error should have field name {string}")
    public void theFieldErrorShouldHaveFieldName(String expectedFieldName) {
        assertThat(result)
                .as("result")
                .isPresent();

        assertThat(result.get())
                .as("result.get()")
                .isNotEmpty();

        var fieldError = result.get().getFirst();
        assertThat(fieldError.fieldName())
                .as("fieldError.getFieldName()")
                .isEqualTo(expectedFieldName);
    }

    @And("the field error should have error code {string}")
    public void theFieldErrorShouldHaveErrorCode(String expectedErrorCode) {
        assertThat(result)
                .as("result")
                .isPresent();

        assertThat(result.get())
                .as("result.get()")
                .isNotEmpty();

        var fieldError = result.get().getFirst();
        assertThat(fieldError.errorCode())
                .as("fieldError.getErrorCode()")
                .isEqualTo(expectedErrorCode);
    }

    @And("the field error should have error message {string}")
    public void theFieldErrorShouldHaveErrorMessage(String expectedErrorMessage) {
        assertThat(result)
                .as("result")
                .isPresent();

        assertThat(result.get())
                .as("result.get()")
                .isNotEmpty();

        var fieldError = result.get().getFirst();
        assertThat(fieldError.errorMessage())
                .as("fieldError.getErrorMessage()")
                .isEqualTo(expectedErrorMessage);
    }

    @And("the field errors should contain {int} items")
    public void theFieldErrorsShouldContainItems(int expectedCount) {
        assertThat(result)
                .as("result")
                .isPresent();

        assertThat(result.get())
                .as("result.get()")
                .hasSize(expectedCount);
    }

    private String extractLastPathSegment(String propertyPath) {
        if (propertyPath.contains(".")) {
            return propertyPath.substring(propertyPath.lastIndexOf(".") + 1);
        }

        if (propertyPath.contains("[")) {
            var bracketIndex = propertyPath.indexOf("[");
            return propertyPath.substring(0, bracketIndex);
        }

        return propertyPath;
    }

    private List<Path.Node> createPathNodes(String propertyPath) {
        // the visitor uses Iterables.getLast() to get the last path node
        // so we need to create a list where the last element has the correct name
        var pathNode = mock(Path.Node.class);
        when(pathNode.getName())
                .thenReturn(extractLastPathSegment(propertyPath));
        return List.of(pathNode);
    }

    private FieldError createSpringFieldError(FieldErrorData fieldErrorData) {
        var fieldError = mock(FieldError.class);

        when(fieldError.getField())
                .thenReturn(fieldErrorData.getFieldName());
        when(fieldError.getCode())
                .thenReturn(fieldErrorData.getErrorCode());
        when(fieldError.getDefaultMessage())
                .thenReturn(fieldErrorData.getErrorMessage());

        return fieldError;
    }

    @DataTableType
    public ViolationData createViolationData(Map<String, String> entry) {
        return ViolationData.builder()
                .propertyPath(StringUtils.isNotBlank(entry.get("propertyPath"))
                        ? entry.get("propertyPath")
                        : null)
                .message(StringUtils.isNotBlank(entry.get("message"))
                        ? entry.get("message")
                        : null)
                .build();
    }

    @Given("multiple ConstraintViolationExceptions with violations:")
    public void multipleConstraintViolationExceptionsWithViolations(DataTable dataTable) {
        var violations = dataTable.asList(ViolationData.class);
        Set<ConstraintViolation<?>> constraintViolations = new HashSet<>();

        for (var violationData : violations) {
            var constraintViolation = mock(ConstraintViolation.class);
            var path = mock(Path.class);
            var pathNode = mock(Path.Node.class);

            when(pathNode.getName())
                    .thenReturn(extractLastPathSegment(violationData.getPropertyPath()));
            when(path.iterator())
                    .thenReturn(List.of(pathNode).iterator());
            when(constraintViolation.getPropertyPath())
                    .thenReturn(path);
            when(constraintViolation.getMessage())
                    .thenReturn(violationData.getMessage());

            constraintViolations.add(constraintViolation);
        }

        testException = new ConstraintViolationException(constraintViolations);
    }

    @Given("a ConstraintViolationException with no violations")
    public void aConstraintViolationExceptionWithNoViolations() {
        testException = new ConstraintViolationException(Set.of());
    }

    @Given("a ValidationException with BindException cause containing field errors:")
    public void aValidationExceptionWithBindExceptionCauseContainingFieldErrors(DataTable dataTable) {
        var fieldErrors = dataTable.asList(FieldErrorData.class);
        var bindException = mock(BindException.class);

        var springFieldErrors = fieldErrors.stream()
                .map(this::createSpringFieldError)
                .toList();

        when(bindException.getFieldErrors())
                .thenReturn(springFieldErrors);

        // mock the field error mapper
        for (int i = 0; i < fieldErrors.size(); i++) {
            var fieldErrorData = fieldErrors.get(i);
            var springFieldError = springFieldErrors.get(i);

            var dto = FieldErrorDto.builder()
                    .fieldName(fieldErrorData.getFieldName())
                    .errorCode(fieldErrorData.getErrorCode())
                    .errorMessage(fieldErrorData.getErrorMessage())
                    .build();
            when(fieldErrorMapper.toDto(springFieldError))
                    .thenReturn(dto);
        }

        testException = new jakarta.validation.ValidationException("Validation failed", bindException);
    }

    @Given("a ValidationException with null cause")
    public void aValidationExceptionWithNullCause() {
        testException = new jakarta.validation.ValidationException("Validation failed", null);
    }

    @Given("an exception of type {string}")
    public void anExceptionOfType(String exceptionType) {
        testException = switch (exceptionType) {
            case "ConstraintViolationException" -> {
                var constraintViolation = mock(ConstraintViolation.class);
                var path = mock(Path.class);
                var pathNode = mock(Path.Node.class);

                when(pathNode.getName())
                        .thenReturn("testField");
                when(path.iterator())
                        .thenReturn(List.of(pathNode).iterator());
                when(constraintViolation.getPropertyPath())
                        .thenReturn(path);
                when(constraintViolation.getMessage())
                        .thenReturn("test message");

                var violations = Set.of((ConstraintViolation<?>) constraintViolation);
                yield new ConstraintViolationException(violations);
            }

            case "MissingServletRequestParameterException" ->
                    new MissingServletRequestParameterException("param", "String");

            case "MethodArgumentTypeMismatchException" -> {
                var exception = mock(MethodArgumentTypeMismatchException.class);
                when(exception.getName())
                        .thenReturn("param");
                when(exception.getErrorCode())
                        .thenReturn("typeMismatch");
                when(exception.getMessage())
                        .thenReturn("test message");
                yield exception;
            }

            case "MethodArgumentNotValidException" -> {
                var bindingResult = mock(org.springframework.validation.BindingResult.class);
                when(bindingResult.getFieldErrors())
                        .thenReturn(List.of());

                var exception = mock(MethodArgumentNotValidException.class);
                when(exception.getBindingResult())
                        .thenReturn(bindingResult);

                yield exception;
            }

            case "BindException" -> {
                var exception = mock(BindException.class);
                when(exception.getFieldErrors())
                        .thenReturn(List.of());
                yield exception;
            }

            case "ValidationException" -> new jakarta.validation.ValidationException("test");
            case "RuntimeException" -> new RuntimeException("test");
            case "IllegalArgumentException" -> new IllegalArgumentException("test");
            case "NullPointerException" -> new NullPointerException("test");
            default -> throw new IllegalArgumentException("Unknown exception type: " + exceptionType);
        };
    }

    @And("the field errors should contain field names {string}, {string}, {string}")
    public void theFieldErrorsShouldContainFieldNames(String fieldName1, String fieldName2, String fieldName3) {
        assertThat(result)
                .as("result")
                .isPresent();

        var fieldNames = result.get().stream()
                .map(FieldErrorDto::fieldName)
                .toList();

        assertThat(fieldNames)
                .as("fieldNames")
                .containsExactlyInAnyOrder(fieldName1, fieldName2, fieldName3);
    }

    @Value
    @Builder
    public static class FieldErrorData {

        String fieldName;
        String errorCode;
        String errorMessage;

    }

    @Value
    @Builder
    public static class ViolationData {

        String propertyPath;
        String message;

    }

}
