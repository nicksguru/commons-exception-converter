package guru.nicks.commons.cucumber;

import guru.nicks.commons.cucumber.world.TextWorld;
import guru.nicks.commons.exception.impl.ErrorDictionaryServiceImpl;
import guru.nicks.commons.exception.service.ErrorDictionaryService;
import guru.nicks.commons.utils.TextUtils;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.DataTableType;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.apache.commons.lang3.StringUtils;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.http.HttpHeaders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.when;

@RequiredArgsConstructor
public class ErrorDictionaryServiceSteps {

    // DI
    private final TextWorld textWorld;

    @Mock
    private ObjectFactory<HttpServletRequest> httpRequestFactory;
    @Mock
    private HttpServletRequest httpRequest;
    private AutoCloseable closeableMocks;

    private Map<TestErrorCode, Map<Locale, String>> errorDictionary;
    private Locale defaultLocale;
    private ErrorDictionaryService<TestErrorCode> errorDictionaryService;
    private ErrorDictionaryService<TestErrorCode> anotherErrorDictionaryService;

    private Optional<String> foundTranslation;
    private List<Locale> resolvedLocalePriority;
    private boolean useHttpRequestFactory;

    private ListAppender<ILoggingEvent> logAppender;

    @Before
    public void beforeEachScenario() {
        closeableMocks = MockitoAnnotations.openMocks(this);

        errorDictionary = new HashMap<>();
        useHttpRequestFactory = false;

        // setup log appender
        Logger logger = (Logger) LoggerFactory.getLogger(ErrorDictionaryServiceImpl.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);
    }

    @After
    public void afterEachScenario() throws Exception {
        if (closeableMocks != null) {
            closeableMocks.close();
        }

        // cleanup log appender
        if (logAppender != null) {
            Logger logger = (Logger) LoggerFactory.getLogger(ErrorDictionaryServiceImpl.class);
            logger.detachAppender(logAppender);
            logAppender.stop();
        }
    }

    @DataTableType
    public TranslationRow createTranslationRow(Map<String, String> entry) {
        String errorCodeStr = entry.get("errorCode");
        String localeStr = entry.get("locale");
        String message = entry.get("message");

        return TranslationRow.builder()
                .errorCode(StringUtils.isNotBlank(errorCodeStr)
                        ? TestErrorCode.valueOf(errorCodeStr)
                        : null)
                .locale(StringUtils.isNotBlank(localeStr)
                        ? Locale.forLanguageTag(localeStr)
                        : null)
                .message(StringUtils.isNotBlank(message)
                        ? message
                        : null)
                .build();
    }

    @Given("an error dictionary with the following translations:")
    public void anErrorDictionaryWithTheFollowingTranslations(List<TranslationRow> translations) {
        errorDictionary = new HashMap<>();

        for (TranslationRow row : translations) {
            errorDictionary
                    .computeIfAbsent(row.getErrorCode(), k -> new HashMap<>())
                    .put(row.getLocale(), row.getMessage());
        }
    }

    @Given("an error dictionary with error code {string} having an empty locale map")
    public void anErrorDictionaryWithErrorCodeHavingAnEmptyLocaleMap(String errorCode) {
        errorDictionary = new HashMap<>();
        errorDictionary.put(TestErrorCode.valueOf(errorCode), new HashMap<>());
    }

    @Given("the default locale is {string}")
    public void theDefaultLocaleIs(String locale) {
        defaultLocale = Locale.forLanguageTag(locale);
    }

    @Given("the error dictionary service is initialized")
    public void theErrorDictionaryServiceIsInitialized() {
        textWorld.setLastException(catchThrowable(() ->
                errorDictionaryService = new TestErrorDictionaryService(
                        errorDictionary,
                        defaultLocale,
                        useHttpRequestFactory ? httpRequestFactory : null)));
    }

    @Given("the error dictionary service is initialized with HTTP request factory")
    public void theErrorDictionaryServiceIsInitializedWithHttpRequestFactory() {
        useHttpRequestFactory = true;
        theErrorDictionaryServiceIsInitialized();
    }

    @Given("the error dictionary service is initialized without HTTP request factory")
    public void theErrorDictionaryServiceIsInitializedWithoutHttpRequestFactory() {
        useHttpRequestFactory = false;
        theErrorDictionaryServiceIsInitialized();
    }

    @Given("the HTTP request has Accept-Language header {string}")
    public void theHttpRequestHasAcceptLanguageHeader(String acceptLanguage) {
        when(httpRequestFactory.getObject())
                .thenReturn(httpRequest);

        when(httpRequest.getHeader(HttpHeaders.ACCEPT_LANGUAGE))
                .thenReturn(acceptLanguage);
    }

    @When("another error dictionary service is initialized with the same translations")
    public void anotherErrorDictionaryServiceIsInitializedWithTheSameTranslations() {
        anotherErrorDictionaryService = new TestErrorDictionaryService(
                errorDictionary,
                defaultLocale,
                useHttpRequestFactory ? httpRequestFactory : null);
    }

    @Then("the error dictionary service should be initialized without throwing an exception")
    public void theErrorDictionaryServiceShouldBeInitializedWithoutThrowingAnException() {
        assertThat(textWorld.getLastException())
                .as("No exception should be thrown during initialization")
                .isNull();
    }

    @Then("the error dictionary service should be initialized with an exception")
    public void theErrorDictionaryServiceShouldBeInitializedWithAnException() {
        assertThat(textWorld.getLastException())
                .as("An exception should be thrown during initialization")
                .isNotNull();
    }

    @Then("the error dictionary should contain {int} error codes")
    public void theErrorDictionaryShouldContainErrorCodes(int expectedCount) {
        assertThat(errorDictionaryService)
                .as("Error dictionary service should be initialized")
                .isNotNull();

        assertThat(errorDictionaryService.getDictionary())
                .as("Error code enum constant count should match expected value")
                .hasSize(expectedCount);
    }

    @Then("the error dictionary service should resolve locale priority as {string}")
    public void theErrorDictionaryServiceShouldResolveLocalePriorityAs(String expectedPriority) {
        List<Locale> expectedLocales = Arrays.stream(expectedPriority.split(","))
                .map(String::strip)
                .map(Locale::forLanguageTag)
                .toList();

        assertThat(resolvedLocalePriority)
                .as("Resolved locale priority should match expected value")
                .containsExactlyElementsOf(expectedLocales);
    }

    @Then("the error dictionary service should find translation for error code {string} in locale {string}")
    public void theErrorDictionaryServiceShouldFindTranslationForErrorCodeInLocale(String errorCode, String locale) {
        TestErrorCode testErrorCode = TestErrorCode.valueOf(errorCode);
        Locale testLocale = Locale.forLanguageTag(locale);

        foundTranslation = errorDictionaryService.findTranslation(testErrorCode, List.of(testLocale));

        assertThat(foundTranslation)
                .as("Found translation should not be empty")
                .isPresent();
    }

    @Then("the error dictionary service should not find translation for error code {string} in locale {string}")
    public void theErrorDictionaryServiceShouldNotFindTranslationForErrorCodeInLocale(String errorCode, String locale) {
        TestErrorCode testErrorCode = TestErrorCode.valueOf(errorCode);
        Locale testLocale = Locale.forLanguageTag(locale);

        foundTranslation = errorDictionaryService.findTranslation(testErrorCode, List.of(testLocale));

        assertThat(foundTranslation)
                .as("Found translation should be empty")
                .isNotPresent();
    }

    @Then("the error dictionary service should log warning about missing translation for error code {string} in locale {string}")
    public void theErrorDictionaryServiceShouldLogWarningAboutMissingTranslationForErrorCodeInLocale(String errorCode,
            String locale) {
        String expectedMessage = "Missing translation for error code " + errorCode + " in locale " + locale;

        assertThat(logAppender.list)
                .as("Log should contain warning message")
                .anySatisfy(event -> {
                    assertThat(event.getLevel())
                            .as("Log level should be WARN")
                            .isEqualTo(Level.WARN);
                    assertThat(event.getFormattedMessage())
                            .as("Log message should contain expected text")
                            .contains(expectedMessage);
                });
    }

    @And("the supported locales should be {string}")
    public void theSupportedLocalesShouldBe(String commaSeparatedLanguageTags) {
        List<Locale> expectedLocales = TextUtils.collectUniqueCommaSeparated(commaSeparatedLanguageTags)
                .stream()
                .map(Locale::forLanguageTag)
                // sort locales in the same way as the service (Locale itself is not Comparable)
                .sorted(Comparator
                        .comparing(Locale::getLanguage)
                        .thenComparing(Locale::getCountry))
                .distinct()
                .toList();

        assertThat(errorDictionaryService.getSupportedLocales())
                .as("Supported locales should match expected value")
                .containsExactlyElementsOf(expectedLocales);
    }

    @And("the dictionary version should not be blank")
    public void theDictionaryVersionShouldNotBeBlank() {
        assertThat(errorDictionaryService.getDictionaryVersion())
                .as("Dictionary version should not be blank")
                .isNotBlank();
    }

    @When("finding translation for error code {string} with locales {string}")
    public void findingTranslationForErrorCodeWithLocales(String errorCode, String commaSeparatedLanguageTags) {
        List<Locale> locales = TextUtils.collectUniqueCommaSeparated(commaSeparatedLanguageTags)
                .stream()
                .map(Locale::forLanguageTag)
                .toList();

        foundTranslation = errorDictionaryService.findTranslation(TestErrorCode.valueOf(errorCode), locales);
    }

    @Then("the translation should be {string}")
    public void theTranslationShouldBe(String message) {
        assertThat(foundTranslation)
                .as("Translation should not be empty")
                .isPresent();

        assertThat(foundTranslation)
                .as("Translation should match expected value")
                .contains(message);
    }

    @Then("the translation should be empty")
    public void theTranslationShouldBeEmpty() {
        assertThat(foundTranslation)
                .as("Translation should be empty")
                .isEmpty();
    }

    @When("finding translation for error code {string} with null locales")
    public void findingTranslationForErrorCodeWithNullLocales(String errorCode) {
        foundTranslation = errorDictionaryService.findTranslation(TestErrorCode.valueOf(errorCode), null);
    }

    @When("finding translation for error code {string} with locales containing nulls and {string}")
    public void findingTranslationForErrorCodeWithLocalesContainingNullsAnd(String errorCode,
            String commaSeparatedLanguageTags) {
        List<Locale> locales = TextUtils.collectUniqueCommaSeparated(commaSeparatedLanguageTags)
                .stream()
                .map(Locale::forLanguageTag)
                .collect(Collectors.toCollection(ArrayList::new));

        locales.add(null);
        foundTranslation = errorDictionaryService.findTranslation(TestErrorCode.valueOf(errorCode), locales);
    }

    @When("finding translation with locale priority for error code {string}")
    public void findingTranslationWithLocalePriorityForErrorCode(String errorCode) {
        foundTranslation = errorDictionaryService.findTranslationWithLocalePriority(TestErrorCode.valueOf(errorCode));
    }

    @When("resolving locale priority")
    public void resolvingLocalePriority() {
        resolvedLocalePriority = errorDictionaryService.resolveLocalePriority();
    }

    @Then("the locale priority should contain {string}")
    public void theLocalePriorityShouldContain(String commasSeparatedLanguageTags) {
        List<Locale> expectedLocales = TextUtils.collectUniqueCommaSeparated(commasSeparatedLanguageTags)
                .stream()
                .map(Locale::forLanguageTag)
                // sort locales for checking priority order (Locale itself is not Comparable)
                .sorted(Comparator
                        .comparing(Locale::getLanguage)
                        .thenComparing(Locale::getCountry))
                .toList();

        assertThat(resolvedLocalePriority)
                .as("Resolved locale priority should match expected value")
                .containsExactlyElementsOf(expectedLocales);
    }

    @Then("an error should be logged about incomplete dictionary")
    public void anErrorShouldBeLoggedAboutIncompleteDictionary() {
        String expectedMessage = "Incomplete error dictionary";

        assertThat(logAppender.list)
                .as("Log should contain error message")
                .anySatisfy(event -> {
                    assertThat(event.getLevel())
                            .as("Log level should be ERROR")
                            .isEqualTo(Level.ERROR);
                    assertThat(event.getFormattedMessage())
                            .as("Log message should contain expected text")
                            .contains(expectedMessage);
                });
    }

    @Then("an info should be logged about complete dictionary")
    public void anInfoShouldBeLoggedAboutCompleteDictionary() {
        String expectedMessage = "Error dictionary version:";

        assertThat(logAppender.list)
                .as("Log should contain info message")
                .anySatisfy(event -> {
                    assertThat(event.getLevel())
                            .as("Log level should be INFO")
                            .isEqualTo(Level.INFO);
                    assertThat(event.getFormattedMessage())
                            .as("Log message should contain expected text")
                            .contains(expectedMessage);
                });
    }

    @And("the error code enum has {int} constants")
    public void theErrorCodeEnumHasConstants(int number) {
        assertThat(TestErrorCode.values())
                .as("TestErrorCode enum should have expected number of constants")
                .hasSize(number);
    }

    @Then("both dictionary versions should be equal")
    public void bothDictionaryVersionsShouldBeEqual() {
        assertThat(errorDictionaryService.getDictionaryVersion())
                .as("Dictionary versions should be equal")
                .isEqualTo(anotherErrorDictionaryService.getDictionaryVersion());
    }

    @And("another error dictionary service is initialized with different translations")
    public void anotherErrorDictionaryServiceIsInitializedWithDifferentTranslations() {
        anotherErrorDictionaryService = new TestErrorDictionaryService(
                Map.of(
                        TestErrorCode.CODE_ONE, Map.of(Locale.US, "Translation for CODE_ONE (US)"),
                        TestErrorCode.CODE_TWO, Map.of(Locale.US, "Translation for CODE_TWO (US)")
                ),
                Locale.US, null);
    }

    @Then("the dictionary versions should be different")
    public void theDictionaryVersionsShouldBeDifferent() {
        assertThat(errorDictionaryService.getDictionaryVersion())
                .as("Dictionary versions should be different")
                .isNotEqualTo(anotherErrorDictionaryService.getDictionaryVersion());
    }

    @Then("the locale priority should be empty")
    public void theLocalePriorityShouldBeEmpty() {
        assertThat(resolvedLocalePriority)
                .as("Locale priority should be empty")
                .isEmpty();
    }


    @Then("the missing error codes should be empty")
    public void theMissingErrorCodesShouldBeEmpty() {
        assertThat(errorDictionaryService)
                .as("Error dictionary service should be initialized")
                .isNotNull();

        EnumSet<TestErrorCode> missingCodes = errorDictionaryService.getMissingErrorCodes();

        assertThat(missingCodes)
                .as("Missing error codes should be empty when dictionary is complete")
                .isEmpty();
    }

    @Then("the missing error codes should contain {string}")
    public void theMissingErrorCodesShouldContain(String expectedMissingCodes) {
        assertThat(errorDictionaryService)
                .as("Error dictionary service should be initialized")
                .isNotNull();

        EnumSet<TestErrorCode> missingCodes = errorDictionaryService.getMissingErrorCodes();

        List<String> expectedCodeNames = Arrays.stream(expectedMissingCodes.split(",\\s*"))
                .map(String::strip)
                .filter(StringUtils::isNotBlank)
                .sorted()
                .toList();

        List<String> actualCodeNames = missingCodes.stream()
                .map(Enum::name)
                .sorted()
                .toList();

        assertThat(actualCodeNames)
                .as("Missing error codes should match expected values")
                .containsExactlyElementsOf(expectedCodeNames);
    }

    @Given("an empty error dictionary")
    public void anEmptyErrorDictionary() {
        errorDictionary = new HashMap<>();
    }

    public enum TestErrorCode {

        CODE_ONE,
        CODE_TWO,
        CODE_THREE

    }

    @Value
    @Builder
    public static class TranslationRow {

        TestErrorCode errorCode;
        Locale locale;
        String message;

    }

    private static class TestErrorDictionaryService extends ErrorDictionaryServiceImpl<TestErrorCode> {

        public TestErrorDictionaryService(
                Map<TestErrorCode, Map<Locale, String>> errorDictionary,
                Locale defaultLocale,
                ObjectFactory<HttpServletRequest> httpRequestFactory) {
            super(errorDictionary, defaultLocale, httpRequestFactory);
        }

        @Override
        public Class<TestErrorCode> getErrorCodeClass() {
            return TestErrorCode.class;
        }

    }
}
