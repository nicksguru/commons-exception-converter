package guru.nicks.commons.exception.config;

import guru.nicks.commons.exception.ExceptionConverter;
import guru.nicks.commons.exception.converter.AccessDeniedExceptionConverter;
import guru.nicks.commons.exception.converter.BindExceptionConverter;
import guru.nicks.commons.exception.converter.BusinessExceptionConverter;
import guru.nicks.commons.exception.converter.ConnectExceptionConverter;
import guru.nicks.commons.exception.converter.ConversionFailedExceptionConverter;
import guru.nicks.commons.exception.converter.DateTimeParseExceptionConverter;
import guru.nicks.commons.exception.converter.HttpMediaTypeNotSupportedExceptionConverter;
import guru.nicks.commons.exception.converter.HttpMessageNotReadableExceptionConverter;
import guru.nicks.commons.exception.converter.HttpRequestMethodNotSupportedExceptionConverter;
import guru.nicks.commons.exception.converter.IllegalArgumentExceptionConverter;
import guru.nicks.commons.exception.converter.IllegalStateExceptionConverter;
import guru.nicks.commons.exception.converter.MethodArgumentMismatchExceptionConverter;
import guru.nicks.commons.exception.converter.MissingRequestHeaderExceptionConverter;
import guru.nicks.commons.exception.converter.MissingRequestValueExceptionConverter;
import guru.nicks.commons.exception.converter.MultipartExceptionConverter;
import guru.nicks.commons.exception.converter.NoFallbackAvailableExceptionConverter;
import guru.nicks.commons.exception.converter.NoResourceFoundExceptionConverter;
import guru.nicks.commons.exception.converter.PropertyReferenceExceptionConverter;
import guru.nicks.commons.exception.converter.SecurityExceptionConverter;
import guru.nicks.commons.exception.converter.UnsupportedOperationExceptionConverter;
import guru.nicks.commons.exception.converter.ValidationExceptionConverter;
import guru.nicks.commons.exception.mapper.ExceptionConverterRegistry;
import guru.nicks.commons.exception.visitor.ExceptionConverterFinderVisitor;
import guru.nicks.commons.exception.visitor.FieldErrorDiscovererVisitor;
import guru.nicks.commons.rest.v1.mapper.FieldErrorMapper;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * Autoconfiguration for exception converter beans. Creates beans for all exception converters if they are not already
 * defined in the Spring context. This allows for overriding of specific converters while providing sensible defaults.
 */
@AutoConfiguration
public class ExceptionConverterAutoConfiguration {

    // Exception converter beans

    /**
     * Creates {@link BindExceptionConverter} bean if not already present.
     *
     * @return converter bean
     */
    @Bean
    @ConditionalOnMissingBean
    public BindExceptionConverter bindExceptionConverter() {
        return new BindExceptionConverter();
    }

    /**
     * Creates {@link ConnectExceptionConverter} bean if not already present.
     *
     * @return converter bean
     */
    @Bean
    @ConditionalOnMissingBean
    public ConnectExceptionConverter connectExceptionConverter() {
        return new ConnectExceptionConverter();
    }

    /**
     * Creates {@link BusinessExceptionConverter} bean if not already present.
     *
     * @return converter bean
     */
    @Bean
    @ConditionalOnMissingBean
    public BusinessExceptionConverter businessExceptionConverter() {
        return new BusinessExceptionConverter();
    }

    /**
     * Creates {@link SecurityExceptionConverter} bean if not already present.
     *
     * @return converter bean
     */
    @Bean
    @ConditionalOnMissingBean
    public SecurityExceptionConverter securityExceptionConverter() {
        return new SecurityExceptionConverter();
    }

    /**
     * Creates {@link MultipartExceptionConverter} bean if not already present.
     *
     * @return converter bean
     */
    @Bean
    @ConditionalOnMissingBean
    public MultipartExceptionConverter multipartExceptionConverter() {
        return new MultipartExceptionConverter();
    }

    /**
     * Creates {@link ValidationExceptionConverter} bean if not already present.
     *
     * @return converter bean
     */
    @Bean
    @ConditionalOnMissingBean
    public ValidationExceptionConverter validationExceptionConverter() {
        return new ValidationExceptionConverter();
    }

    /**
     * Creates {@link AccessDeniedExceptionConverter} bean if not already present.
     *
     * @return converter bean
     */
    @Bean
    @ConditionalOnMissingBean
    public AccessDeniedExceptionConverter accessDeniedExceptionConverter() {
        return new AccessDeniedExceptionConverter();
    }

    /**
     * Creates {@link IllegalStateExceptionConverter} bean if not already present.
     *
     * @return converter bean
     */
    @Bean
    @ConditionalOnMissingBean
    public IllegalStateExceptionConverter illegalStateExceptionConverter() {
        return new IllegalStateExceptionConverter();
    }

    /**
     * Creates {@link DateTimeParseExceptionConverter} bean if not already present.
     *
     * @return converter bean
     */
    @Bean
    @ConditionalOnMissingBean
    public DateTimeParseExceptionConverter dateTimeParseExceptionConverter() {
        return new DateTimeParseExceptionConverter();
    }

    /**
     * Creates {@link IllegalArgumentExceptionConverter} bean if not already present.
     *
     * @return converter bean
     */
    @Bean
    @ConditionalOnMissingBean
    public IllegalArgumentExceptionConverter illegalArgumentExceptionConverter() {
        return new IllegalArgumentExceptionConverter();
    }

    /**
     * Creates {@link NoResourceFoundExceptionConverter} bean if not already present.
     *
     * @return converter bean
     */
    @Bean
    @ConditionalOnMissingBean
    public NoResourceFoundExceptionConverter noResourceFoundExceptionConverter() {
        return new NoResourceFoundExceptionConverter();
    }

    /**
     * Creates {@link ConversionFailedExceptionConverter} bean if not already present.
     *
     * @return converter bean
     */
    @Bean
    @ConditionalOnMissingBean
    public ConversionFailedExceptionConverter conversionFailedExceptionConverter() {
        return new ConversionFailedExceptionConverter();
    }

    /**
     * Creates {@link PropertyReferenceExceptionConverter} bean if not already present.
     *
     * @return converter bean
     */
    @Bean
    @ConditionalOnMissingBean
    public PropertyReferenceExceptionConverter propertyReferenceExceptionConverter() {
        return new PropertyReferenceExceptionConverter();
    }

    /**
     * Creates {@link MissingRequestValueExceptionConverter} bean if not already present.
     *
     * @return converter bean
     */
    @Bean
    @ConditionalOnMissingBean
    public MissingRequestValueExceptionConverter missingRequestValueExceptionConverter() {
        return new MissingRequestValueExceptionConverter();
    }

    /**
     * Creates {@link NoFallbackAvailableExceptionConverter} bean if not already present.
     *
     * @return converter bean
     */
    @Bean
    @ConditionalOnMissingBean
    public NoFallbackAvailableExceptionConverter noFallbackAvailableExceptionConverter() {
        return new NoFallbackAvailableExceptionConverter();
    }

    /**
     * Creates {@link MissingRequestHeaderExceptionConverter} bean if not already present.
     *
     * @return converter bean
     */
    @Bean
    @ConditionalOnMissingBean
    public MissingRequestHeaderExceptionConverter missingRequestHeaderExceptionConverter() {
        return new MissingRequestHeaderExceptionConverter();
    }

    /**
     * Creates {@link UnsupportedOperationExceptionConverter} bean if not already present.
     *
     * @return converter bean
     */
    @Bean
    @ConditionalOnMissingBean
    public UnsupportedOperationExceptionConverter unsupportedOperationExceptionConverter() {
        return new UnsupportedOperationExceptionConverter();
    }

    /**
     * Creates {@link HttpMessageNotReadableExceptionConverter} bean if not already present.
     *
     * @return converter bean
     */
    @Bean
    @ConditionalOnMissingBean
    public HttpMessageNotReadableExceptionConverter httpMessageNotReadableExceptionConverter() {
        return new HttpMessageNotReadableExceptionConverter();
    }

    /**
     * Creates {@link MethodArgumentMismatchExceptionConverter} bean if not already present.
     *
     * @return converter bean
     */
    @Bean
    @ConditionalOnMissingBean
    public MethodArgumentMismatchExceptionConverter methodArgumentMismatchExceptionConverter() {
        return new MethodArgumentMismatchExceptionConverter();
    }

    /**
     * Creates {@link HttpMediaTypeNotSupportedExceptionConverter} bean if not already present.
     *
     * @return converter bean
     */
    @Bean
    @ConditionalOnMissingBean
    public HttpMediaTypeNotSupportedExceptionConverter httpMediaTypeNotSupportedExceptionConverter() {
        return new HttpMediaTypeNotSupportedExceptionConverter();
    }

    /**
     * Creates {@link HttpRequestMethodNotSupportedExceptionConverter} bean if not already present.
     *
     * @return converter bean
     */
    @Bean
    @ConditionalOnMissingBean
    public HttpRequestMethodNotSupportedExceptionConverter httpRequestMethodNotSupportedExceptionConverter() {
        return new HttpRequestMethodNotSupportedExceptionConverter();
    }

    // Registry bean

    /**
     * Creates {@link ExceptionConverterRegistry} bean if not already present.
     *
     * @param exceptionConverters list of all exception converter beans
     * @return registry bean
     */
    @Bean
    @ConditionalOnMissingBean
    public ExceptionConverterRegistry exceptionConverterRegistry(
            List<ExceptionConverter<?, ?>> exceptionConverters) {
        return new ExceptionConverterRegistry(exceptionConverters);
    }

    // Mapper beans

    /**
     * Creates {@link FieldErrorMapper} bean if not already present.
     *
     * @return mapper bean
     */
    @Bean
    @ConditionalOnMissingBean
    public FieldErrorMapper fieldErrorMapper() {
        return new FieldErrorMapper();
    }

    // Visitor beans

    /**
     * Creates {@link FieldErrorDiscovererVisitor} bean if not already present.
     *
     * @param fieldErrorMapper field error mapper dependency
     * @return visitor bean
     */
    @Bean
    @ConditionalOnMissingBean
    public FieldErrorDiscovererVisitor fieldErrorDiscovererVisitor(FieldErrorMapper fieldErrorMapper) {
        return new FieldErrorDiscovererVisitor(fieldErrorMapper);
    }

    /**
     * Creates {@link ExceptionConverterFinderVisitor} bean if not already present.
     *
     * @param exceptionConverterRegistry exception converter registry dependency
     * @return visitor bean
     */
    @Bean
    @ConditionalOnMissingBean
    public ExceptionConverterFinderVisitor exceptionConverterFinderVisitor(
            ExceptionConverterRegistry exceptionConverterRegistry) {
        return new ExceptionConverterFinderVisitor(exceptionConverterRegistry);
    }

}
