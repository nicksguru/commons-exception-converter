package guru.nicks.exception.impl;

import guru.nicks.exception.service.ErrorDictionaryService;
import guru.nicks.utils.ChecksumUtils;
import guru.nicks.utils.LocaleUtils;

import com.google.common.collect.ImmutableSortedMap;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static guru.nicks.validation.dsl.ValiDsl.checkNotNull;

@Slf4j
public abstract class ErrorDictionaryServiceImpl<T extends Enum<T>> implements ErrorDictionaryService<T> {

    // immutable
    @Getter // (onMethod_ = @Override) - COMMENTED OUT: JavaDoc plugin fails on this
    private final Map<T, Map<Locale, String>> dictionary;

    @Getter // (onMethod_ = @Override) - COMMENTED OUT: JavaDoc plugin fails on this
    private final String dictionaryVersion;

    @Getter // (onMethod_ = @Override) - COMMENTED OUT: JavaDoc plugin fails on this
    private final Locale defaultLocale;

    // immutable
    @Getter // (onMethod_ = @Override)
    private final List<Locale> supportedLocales;

    /**
     * Holds current request, if available (not a singleton-scoped bean). Can be {@code null}.
     * <p>
     * WARNING: when provided, this factory must be properly scoped (e.g., request-scoped proxy). The factory should not
     * hold direct references to request objects beyond their lifecycle.
     */
    private final ObjectFactory<HttpServletRequest> httpRequestFactory;

    /**
     * Constructor. Validates and processes the input dictionary by:
     * <ul>
     *   <li>filtering out null error codes and empty/null locale mappings</li>
     *   <li>filtering out invalid locales (having a blank {@link Locale#getLanguage()} resulting from passing
     *       unsupported language tags to {@link Locale#forLanguageTag(String)} - this is a documented behavior)</li>
     *   <li>creating an immutable, sorted dictionary for thread-safe access</li>
     *   <li>extracting and sorting all supported locales from the dictionary (as {@link #getSupportedLocales()})</li>
     *   <li>calculating a checksum version of the dictionary (as {@link #getDictionaryVersion()})</li>
     *   <li>reporting any missing error code translations (as compared to all {@link T} values)</li>
     * </ul>
     *
     * @param dictionary         A map of business error codes to their locale-specific translations. Must not be
     *                           {@code null}. Null keys and empty/null values are filtered out during processing.
     * @param defaultLocale      The default locale to use when no matching translation is found for requested locales.
     *                           Must not be {@code null}.
     * @param httpRequestFactory an optional factory for obtaining the current HTTP request, used to extract locale
     *                           preferences from request headers. May be null if HTTP request context is not available.
     *                           When provided, must be properly scoped (e.g., request-scoped proxy).
     */
    public ErrorDictionaryServiceImpl(Map<T, Map<Locale, String>> dictionary, Locale defaultLocale,
            @Nullable ObjectFactory<HttpServletRequest> httpRequestFactory) {
        this.defaultLocale = checkNotNull(defaultLocale, "defaultLocale");
        this.httpRequestFactory = httpRequestFactory;

        checkNotNull(dictionary, "dictionary");
        this.dictionary = sanitizeDictionary(dictionary);

        // at this point, the dictionary contains no null keys or values
        supportedLocales = this.dictionary.values()
                .stream()
                .flatMap(localeMap -> localeMap.keySet().stream())
                // Locale itself is not Comparable
                .sorted(Comparator
                        .comparing(Locale::getLanguage)
                        .thenComparing(Locale::getCountry))
                .distinct()
                .toList();

        dictionaryVersion = calculateErrorDictionaryChecksum(this.dictionary);
        reportIncompleteDictionary();
    }

    @Override
    public Optional<String> findTranslation(T errorCode, @Nullable Collection<Locale> locales) {
        checkNotNull(errorCode, "errorCode");

        Map<Locale, String> locale2message = dictionary.getOrDefault(errorCode, Collections.emptyMap());
        // no translation candidates
        if (locale2message.isEmpty()) {
            return Optional.empty();
        }

        Optional<String> translation = (locales == null)
                ? Optional.empty()
                : locales.stream()
                        .filter(Objects::nonNull)
                        .map(locale2message::get)
                        .filter(StringUtils::isNotBlank)
                        .findFirst();

        // try default locale (save memory - don't create a temporary collection with the default locale added)
        if (translation.isEmpty()) {
            translation = Optional
                    .ofNullable(locale2message.get(defaultLocale))
                    .filter(StringUtils::isNotBlank);
        }

        return translation;
    }

    @Override
    public Optional<String> findTranslationWithLocalePriority(T errorCode) {
        Collection<Locale> localePriority = resolveLocalePriority();
        return findTranslation(errorCode, localePriority);
    }

    @Override
    public List<Locale> resolveLocalePriority() {
        HttpServletRequest httpRequest = (httpRequestFactory != null)
                ? httpRequestFactory.getObject()
                : null;

        return LocaleUtils.resolveLocalePriority(SecurityContextHolder.getContext().getAuthentication(),
                httpRequest, supportedLocales);
    }

    /**
     * @see #stringifyLocales(Map)
     */
    private String calculateErrorDictionaryChecksum(Map<T, Map<Locale, String>> errorDictionary) {
        Map<T, Map<String, String>> mapWithSortableDeepKeys = errorDictionary.entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> stringifyLocales(entry.getValue())));

        // WARNING: the input map's keys are NOT sorted - the checksum engine sorts them.
        return ChecksumUtils.computeJsonChecksumBase64(mapWithSortableDeepKeys);
    }

    /**
     * Replaces each {@link Locale} with its {@link Locale#toLanguageTag()} because the former is not {@link Comparable}
     * - such keys can't be sorted, with is important for consistent checksum computation. Null locales are skipped.
     *
     * @param map source map
     * @return post-processed map
     */
    private Map<String, String> stringifyLocales(Map<Locale, String> map) {
        return map.entrySet()
                .stream()
                .filter(entry -> entry.getKey() != null)
                .collect(Collectors.toMap(entry -> entry.getKey().toLanguageTag(), Map.Entry::getValue));
    }

    /**
     * Validates the completeness of the error dictionary by comparing it against all defined {@code T} values and logs
     * appropriate messages based on the findings.
     * <p>
     * If the dictionary contains translations for all business error codes, logs an informational message with the
     * dictionary version and count. If there are missing translations, logs an error message identifying which error
     * codes lack translations.
     * <p>
     * This method must be called AFTER all the object fields have been initialized to ensure the dictionary is properly
     * configured and to alert developers of any missing translations that should be added.
     */
    private void reportIncompleteDictionary() {
        int totalErrorCodeCount = getErrorCodeClass().getEnumConstants().length;
        int errorDictionaryCount = dictionary.size();

        // the dictionary is complete
        if (errorDictionaryCount == totalErrorCodeCount) {
            if (log.isTraceEnabled()) {
                log.trace("Error dictionary version: '{}' ({} error codes of class [{}]): {}", dictionaryVersion,
                        errorDictionaryCount, dictionary, getErrorCodeClass().getName());
            } else {
                log.info("Error dictionary version: '{}' ({} error codes of class [{}])", dictionaryVersion,
                        errorDictionaryCount, getErrorCodeClass().getName());
            }

            return;
        }

        // EnumSet.copyOf fails on empty collections, hence this workaround
        EnumSet<T> source = dictionary.isEmpty()
                ? EnumSet.noneOf(getErrorCodeClass())
                : EnumSet.copyOf(dictionary.keySet());

        // discrepancy is not crucial but not nice either
        String errorCodesMissingFromDictionary = EnumSet.complementOf(source)
                .stream()
                .map(Enum::name)
                .sorted()
                .collect(Collectors.joining(", "));

        log.error("Incomplete error dictionary: only {}/{} error codes of class [{}] present, missing translations "
                        + "for: {}", errorDictionaryCount, totalErrorCodeCount,
                getErrorCodeClass().getName(), errorCodesMissingFromDictionary);
    }

    private Map<T, Map<Locale, String>> sanitizeDictionary(Map<T, Map<Locale, String>> dictionary) {
        Map<T, Map<Locale, String>> validatedDictionary = dictionary.entrySet()
                .stream()
                // skip null keys (error codes)
                .filter(entry -> entry.getKey() != null)
                // skip null null/empty values (locale maps)
                .filter(entry -> !MapUtils.isEmpty(entry.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey,
                        // Map<Locale, String>: filter out null keys (locales) and locales having an empty language tag
                        // (resulting from passing an unsupported value to Locale#forLanguageTag - this is a
                        // documented behavior). Without this, translating to any unsupported language would use such
                        // empty locales referring to whatever (another) unsupported language.
                        entry -> entry.getValue()
                                .entrySet()
                                .stream()
                                //
                                // Entry<Locale, String>: key = locale
                                .filter(entry1 -> entry1.getKey() != null)
                                .filter(entry1 -> StringUtils.isNotBlank(entry1.getKey().getLanguage()))
                                //
                                // Entry<Locale, String>: value = message
                                .filter(entry1 -> StringUtils.isNotBlank(entry1.getValue()))
                                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))));

        // sort map for convenient logging, also make it immutable
        return new ImmutableSortedMap.Builder<T, Map<Locale, String>>(
                Comparator.comparing(Enum::name))
                .putAll(validatedDictionary)
                .buildOrThrow();
    }

}
