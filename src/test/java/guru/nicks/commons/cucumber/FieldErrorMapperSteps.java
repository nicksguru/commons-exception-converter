package guru.nicks.commons.cucumber;

import guru.nicks.commons.rest.v1.dto.FieldErrorDto;
import guru.nicks.commons.rest.v1.mapper.FieldErrorMapper;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.validation.FieldError;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Step definitions for testing {@link FieldErrorMapper}.
 */
@RequiredArgsConstructor
public class FieldErrorMapperSteps {

    // DI
    private final FieldErrorMapper fieldErrorMapper;

    @Mock
    private FieldError fieldError;
    private AutoCloseable closeableMocks;

    private String fieldName;
    private String maskedFieldName;
    private FieldErrorDto fieldErrorDto;

    @Before
    public void beforeEachScenario() {
        closeableMocks = MockitoAnnotations.openMocks(this);
    }

    @After
    public void afterEachScenario() throws Exception {
        closeableMocks.close();
    }

    @Given("a field name {string}")
    public void aFieldName(String name) {
        fieldName = name;
    }

    @Given("a field name that is null")
    public void aFieldNameThatIsNull() {
        fieldName = null;
    }

    @Given("a field error for field {string} with code {string} and message {string}")
    public void aFieldErrorForFieldWithCodeAndMessage(String field, String code, String message) {
        when(fieldError.getField())
                .thenReturn(field);
        when(fieldError.getCode())
                .thenReturn(code);
        when(fieldError.getDefaultMessage())
                .thenReturn(message);
    }

    @Given("a field error for field {string} with code null and message {string}")
    public void aFieldErrorForFieldWithNullCodeAndMessage(String field, String message) {
        when(fieldError.getField())
                .thenReturn(field);
        when(fieldError.getCode())
                .thenReturn(null);
        when(fieldError.getDefaultMessage())
                .thenReturn(message);
    }

    @When("the field name is masked")
    public void theFieldNameIsMasked() {
        maskedFieldName = FieldErrorMapper.maskFieldName(fieldName);
    }

    @When("the field error is converted to DTO")
    public void theFieldErrorIsConvertedToDTO() {
        fieldErrorDto = fieldErrorMapper.toDto(fieldError);
    }

    @Then("the masked field name should be {string}")
    public void theMaskedFieldNameShouldBe(String expected) {
        assertThat(maskedFieldName)
                .as("maskedFieldName")
                .isEqualTo(expected);
    }

    @Then("the masked field name should be null")
    public void theMaskedFieldNameShouldBeNull() {
        assertThat(maskedFieldName)
                .as("maskedFieldName")
                .isNull();
    }

    @Then("the field error DTO should have field name {string}")
    public void theFieldErrorDtoShouldHaveFieldName(String expected) {
        assertThat(fieldErrorDto.fieldName())
                .as("fieldErrorDto.fieldName")
                .isEqualTo(expected);
    }

    @Then("the field error DTO should have error code {string}")
    public void theFieldErrorDtoShouldHaveErrorCode(String expected) {
        assertThat(fieldErrorDto.errorCode())
                .as("fieldErrorDto.errorCode")
                .isEqualTo(expected);
    }

    @Then("the field error DTO should have null error code")
    public void theFieldErrorDtoShouldHaveNullErrorCode() {
        assertThat(fieldErrorDto.errorCode())
                .as("fieldErrorDto.errorCode")
                .isNull();
    }

}
