package guru.nicks.exception.converter;

import guru.nicks.exception.ExceptionConverter;
import guru.nicks.exception.http.BadRequestException;

import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.stereotype.Component;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;

/**
 * Converts exception denoting a non-existing property name passed (for example to sort by). Adds {@link FieldError}
 * with the missing property name, so it's rendered to caller.
 */
@Component
public class PropertyReferenceExceptionConverter
        implements ExceptionConverter<PropertyReferenceException, BadRequestException> {

    @Override
    public BadRequestException convert(PropertyReferenceException cause) {
        FieldError error = new FieldError("request", cause.getPropertyName(),
                null, true,
                new String[]{"NoSuchProperty"}, null, "No such property");

        var result = new BeanPropertyBindingResult(null, "request");
        result.addError(error);
        return new BadRequestException(new BindException(result));
    }

}
