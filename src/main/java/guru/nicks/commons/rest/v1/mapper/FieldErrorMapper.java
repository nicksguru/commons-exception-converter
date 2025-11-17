package guru.nicks.commons.rest.v1.mapper;

import guru.nicks.commons.mapper.DefaultMapStructConfig;
import guru.nicks.commons.rest.v1.dto.BusinessExceptionDto;
import guru.nicks.commons.utils.ReflectionUtils;

import org.apache.commons.collections4.CollectionUtils;
import org.mapstruct.Mapper;
import org.springframework.validation.FieldError;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Mapper(config = DefaultMapStructConfig.class)
public class FieldErrorMapper {

    /**
     * If {@code true}, retrieves and renders error arguments, e.g. for {@code @Size(min=3)} it's 2147483647 and 3 - in
     * this order.
     */
    private static final boolean RETRIEVE_ERROR_ARGUMENTS = false;

    /**
     * Returns field name without the {@code outer.field.} prefix. Needed to mark REST DTO field names that fail
     * validation, otherwise clients will see messages like 'Missing products[0].productId' instead of just 'Missing
     * productId'.
     *
     * @param fieldName field name, possibly {@code null}
     * @return masked (or not) field name
     */
    public static String maskFieldName(String fieldName) {
        if (fieldName == null) {
            return null;
        }

        int lastDotIndex = fieldName.lastIndexOf(".");

        // no '.' or it's at the end of the string
        if ((lastDotIndex == -1) || (lastDotIndex == fieldName.length() - 1)) {
            return fieldName;
        }

        return fieldName.substring(lastDotIndex + 1);
    }

    public BusinessExceptionDto.FieldErrorDto toDto(FieldError fieldError) {
        var dtoBuilder = BusinessExceptionDto.FieldErrorDto.builder()
                .fieldName(maskFieldName(fieldError.getField()))
                .errorCode(fieldError.getCode());

        // add error arguments
        if (RETRIEVE_ERROR_ARGUMENTS) {
            Optional.of(fieldError)
                    .map(this::collectErrorArguments)
                    .filter(CollectionUtils::isNotEmpty)
                    .ifPresent(dtoBuilder::arguments);
        }

        // Capitalize (make 1st letter in capital) because raw messages are e.g. 'must not be blank'.
        // COMMENTED OUT to avoid revealing such messages as "Failed to convert property value of type
        // 'java.lang.String' to required type 'java.lang.Boolean'"
        //dtoBuilder.message(StringUtils.capitalize(fieldError.getDefaultMessage())

        return dtoBuilder.build();
    }

    /**
     * Retrieves error arguments, e.g. for {@code @Size(min=3)} it's 2147483647 and 3 (in this order).
     *
     * @param fieldError field error after validation
     * @return list of arguments, possibly empty
     */
    private List<Object> collectErrorArguments(FieldError fieldError) {
        return Optional.ofNullable(fieldError.getArguments())
                .stream()
                .flatMap(Arrays::stream)
                // in particular, filter out MessageSourceResolvable instances
                .filter(ReflectionUtils::isScalar)
                .toList();
    }

}
