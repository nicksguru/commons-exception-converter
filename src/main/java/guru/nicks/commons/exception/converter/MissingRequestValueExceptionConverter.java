package guru.nicks.commons.exception.converter;

import guru.nicks.commons.exception.ExceptionConverter;
import guru.nicks.commons.exception.http.BadRequestException;

import org.springframework.web.bind.MissingRequestValueException;

public class MissingRequestValueExceptionConverter
        implements ExceptionConverter<MissingRequestValueException, BadRequestException> {

    @Override
    public BadRequestException convert(MissingRequestValueException cause) {
        return new BadRequestException("Missing request value", cause);
    }

}
