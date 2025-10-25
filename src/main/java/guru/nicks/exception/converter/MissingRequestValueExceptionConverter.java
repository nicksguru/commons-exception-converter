package guru.nicks.exception.converter;

import guru.nicks.exception.ExceptionConverter;
import guru.nicks.exception.http.BadRequestException;

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
