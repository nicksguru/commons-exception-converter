package guru.nicks.commons.exception.impl;

import guru.nicks.commons.exception.service.ErrorDictionaryService;
import guru.nicks.commons.utils.ChecksumUtils;
import guru.nicks.commons.utils.LocaleUtils;

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
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static guru.nicks.commons.validation.dsl.ValiDsl.checkNotNull;

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
     *       ill-formed language tags to {@link Locale#forLanguageTag(String)} - this is a documented behavior)</li>
     *   <li>creating an immutable, sorted dictionary for thread-safe access</li>
     *   <li>extracting and sorting all supported locales from the dictionary (as {@link #getSupportedLocales()})</li>
     *   <li>calculating a checksum-based version of the dictionary (as {@link #getDictionaryVersion()})</li>
     *   <li>reporting any missing error code translations (as compared to all {@link T} values)</li>
     * </ul>
     *
     * @param dictionary         A map of business error codes to their locale-specific translations. Must not be
     *                           {@code null}. Null keys and empty/null values are filtered out.
     * @param defaultLocale      The default locale to use when no matching translation is found for requested locales.
     *                           Must not be {@code null}.
     * @param httpRequestFactory An optional factory for obtaining the current HTTP request. Used to extract locale
     *                           preferences from request headers. May be {@code null} if HTTP request context is not
     *                           available. When provided, must be a request-scoped proxy.
     */
    public ErrorDictionaryServiceImpl(Map<T, Map<Locale, String>> dictionary, Locale defaultLocale,
            @Nullable ObjectFactory<HttpServletRequest> httpRequestFactory) {
        this.defaultLocale = checkNotNull(defaultLocale, "defaultLocale");
        this.httpRequestFactory = httpRequestFactory;

        checkNotNull(dictionary, "dictionary");
        this.dictionary = sanitizeDictionary(dictionary);
        int originalSize = dictionary.size();

        // Warn if significant data loss occurred during sanitization
        if (originalSize > 0) {
            if (this.dictionary.isEmpty()) {
                log.error("All {} dictionary entries were filtered out during sanitization. "
                        + "Check for null keys, empty values, or invalid locales.", originalSize);
            } else if (this.dictionary.size() < originalSize * 0.5) {
                log.warn("Significant data loss during sanitization: {} of {} entries removed. "
                        + "Check for data quality issues.", originalSize - this.dictionary.size(), originalSize);
            }
        }

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

    @Override
    public EnumSet<T> getMissingErrorCodes() {
        // EnumSet.copyOf fails on empty collections, hence this workaround
        EnumSet<T> source = dictionary.isEmpty()
                ? EnumSet.noneOf(getErrorCodeClass())
                : EnumSet.copyOf(dictionary.keySet());

        // discrepancy is not crucial but not nice either
        return EnumSet.complementOf(source);
    }

    /**
     * Computes a {@link ChecksumUtils#computeJsonChecksumBase64(Object) checksum} ensuring the keys are sorted first
     * (both {@code T} and {@link Locale} - see {@link #sortLocales(Map)}). The manual sorting is superfluous for the
     * above algorithm, but it may change some day, and the key order is crucial.
     */
    protected String calculateErrorDictionaryChecksum(Map<T, Map<Locale, String>> errorDictionary) {
        SortedMap<T, Map<String, String>> mapWithSortedKeys = errorDictionary.entrySet()
                .stream()
                .collect(Collectors.toMap(
                        // T is Enum which is Comparable
                        Map.Entry::getKey,
                        entry -> sortLocales(entry.getValue()),
                        (existingValue, newValue) -> existingValue,
                        TreeMap::new));

        // the checksum engine sorts the keys, but it's better to not rely on that and sort them manually
        return ChecksumUtils.computeJsonChecksumBase64(mapWithSortedKeys);
    }

    /**
     * Replaces each {@link Locale} with its {@link Locale#toLanguageTag()} because the former is not {@link Comparable}
     * - such keys can't be sorted, with is important for consistent checksum computation. Null locales are skipped.
     * <p>
     * NOTE: if the same key ({@link Locale#toLanguageTag() language tag} - theoretically, different original locales
     * may have the same tag) maps to multiple messages during the replacement, the method preserves the latest one.
     * This doesn't affect translation accuracy; the result of this method is only used for checksum computation.
     *
     * @param map source map
     * @return post-processed map
     */
    protected SortedMap<String, String> sortLocales(Map<Locale, String> map) {
        return map.entrySet()
                .stream()
                .filter(entry -> entry.getKey() != null)
                .collect(Collectors.toMap(
                        entry -> entry.getKey().toLanguageTag(),
                        Map.Entry::getValue,
                        // see method-level comment for the reasoning of such merge
                        (existingValue, newValue) -> newValue,
                        TreeMap::new));
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
    protected void reportIncompleteDictionary() {
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

        String commaSeparatedMissingErrorCodes = getMissingErrorCodes().stream()
                .map(Enum::name)
                .sorted()
                .collect(Collectors.joining(", "));

        log.error("Incomplete error dictionary: {}/{} error codes of class [{}] present, missing translations for: {}",
                errorDictionaryCount, totalErrorCodeCount,
                getErrorCodeClass().getName(), commaSeparatedMissingErrorCodes);
    }

    /**
     * Called from constructor to sanitize the error dictionary:
     * <ul>
     *   <li>remove entries with {@code null} error codes (keys)</li>
     *   <li>remove entries with {@code null} or empty locale mapping (values)</li>
     *   <li>sanitize each locale map with {@link #sanitizeLocaleMap(Map)}</li>
     *   <li>sort the resulting dictionary by error code name for consistent logging</li>
     *   <li>return an immutable sorted map to ensure thread-safety</li>
     * </ul>
     *
     * @param dictionary The raw error dictionary to sanitize. Must not be {@code null}. May contain {@code null} keys,
     *                   {@code null} values, or empty locale mappings which will be filtered out.
     * @return An immutable, sorted map containing only valid error code to locale mapping entries. The map is sorted by
     *         error code name and guaranteed to contain no {@code null} keys or empty/{@code null} values.
     */
    protected Map<T, Map<Locale, String>> sanitizeDictionary(Map<T, Map<Locale, String>> dictionary) {
        // sort map for convenient logging, also make it immutable
        var builder = new ImmutableSortedMap.Builder<T, Map<Locale, String>>(Comparator.comparing(Enum::name));

        dictionary.entrySet()
                .stream()
                // skip null keys (error codes)
                .filter(entry -> entry.getKey() != null)
                // skip null null/empty values (locale maps)
                .filter(entry -> !MapUtils.isEmpty(entry.getValue()))
                // sanitize each locale map
                .forEach(entry -> {
                    Map<Locale, String> sanitizedLocaleMap = sanitizeLocaleMap(entry.getValue());

                    if (!sanitizedLocaleMap.isEmpty()) {
                        builder.put(entry.getKey(), sanitizedLocaleMap);
                    }
                });

        return builder.build();
    }

    /**
     * Filters out {@code null} keys (locales) and locales having an empty language tag (resulting from passing an
     * ill-formed value to {@link Locale#forLanguageTag(String)} - a documented behavior). Without this, translating to
     * any unsupported language would use such empty locales referring to whatever (another) unsupported language.
     * <p>
     * Also, filters out blank translations because they have no business value.
     *
     * @param localeMap The locale map to sanitize. May be {@code null}. May contain {@code null} keys/values.
     * @return a sanitized (possibly empty) map; always empty if the input map was {@code null}
     */
    protected Map<Locale, String> sanitizeLocaleMap(@Nullable Map<Locale, String> localeMap) {
        if (MapUtils.isEmpty(localeMap)) {
            return Collections.emptyMap();
        }

        return localeMap.entrySet()
                .stream()
                //
                // key = locale
                .filter(entry -> entry.getKey() != null)
                .filter(entry -> StringUtils.isNotBlank(entry.getKey().getLanguage()))
                //
                // value = message
                .filter(entry -> StringUtils.isNotBlank(entry.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

}
