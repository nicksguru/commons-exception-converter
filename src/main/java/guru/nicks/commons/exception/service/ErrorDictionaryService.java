package guru.nicks.commons.exception.service;

import guru.nicks.commons.auth.domain.OpenIdConnectData;
import guru.nicks.commons.utils.text.LocaleUtils;

import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Maps error codes to localized strings. Client apps are supposed to download the whole dictionary and cache it with
 * ETag because, for example, mobile apps can't be force-updated, yet they should be able to recognize new error codes
 * and display locale-dependent messages for them.
 *
 * @param <T> error code type
 */
public interface ErrorDictionaryService<T extends Enum<T>> {

    /**
     * Finds a non-blank translation of the error code to one of the given locales.
     *
     * @param errorCode error code, must not be {@code null}
     * @param locales   locales to find the translation for, in the given order (if the concrete collection class
     *                  maintains order); {@link #getDefaultLocale()} is tried always, even if it's missing from this
     *                  collection or if the collection is {@code null}
     * @return optional translation
     */
    Optional<String> findTranslation(T errorCode, @Nullable Collection<Locale> locales);

    /**
     * Does the same as {@link #findTranslation(Enum, Collection)}, but the list of locales is inferred from current
     * user's {@link OpenIdConnectData#getLanguageCode()} (if any) and {@code Accept-Language} request header (if any).
     *
     * @param errorCode error code, must not be {@code null}
     * @return optional translation
     */
    Optional<String> findTranslationWithLocalePriority(T errorCode);

    /**
     * Delegates to {@link LocaleUtils#resolveLocalePriority(Authentication, HttpServletRequest, Collection)}.
     *
     * @return locales, never {@code null} (but possibly empty)
     */
    List<Locale> resolveLocalePriority();

    /**
     * Returns all existing translations.
     *
     * @return results
     */
    Map<T, Map<Locale, String>> getDictionary();

    /**
     * @return error codes that are not present in the dictionary, if any
     */
    EnumSet<T> getMissingErrorCodes();

    /**
     * @return locales mentioned in {@link #getDictionary()}, sorted by language and then by country
     */
    List<Locale> getSupportedLocales();

    /**
     * @return default locale for fallback translations
     */
    Locale getDefaultLocale();

    /**
     * Returns current dictionary version (abstract string, has no semver inside).
     *
     * @return dictionary version
     */
    String getDictionaryVersion();

    /**
     * @return error code class
     */
    Class<T> getErrorCodeClass();

}
