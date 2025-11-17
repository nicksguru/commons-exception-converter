package guru.nicks.commons.exception.converter;

import guru.nicks.commons.exception.ExceptionConverter;
import guru.nicks.commons.exception.http.BadRequestException;

import org.springframework.stereotype.Component;
import org.springframework.web.bind.MissingRequestValueException;

@Component
public class MissingRequestValueExceptionConverter
        implements ExceptionConverter<MissingRequestValueException, BadRequestException> {

    @Override
    public BadRequestException convert(MissingRequestValueException cause) {
        return new BadRequestException("Missing request value", cause);
    }

}
