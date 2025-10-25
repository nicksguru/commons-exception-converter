package guru.nicks.exception.converter;

import guru.nicks.exception.ExceptionConverter;
import guru.nicks.exception.http.BadRequestException;

import org.springframework.stereotype.Component;
import org.springframework.web.bind.MissingRequestHeaderException;

/**
 * Original exception holds such message as:
 * {@code Required request header 'X-API-Key' for method parameter type String is not present} - too much technical
 * stuff (such as Java class of the header value).
 */
@Component
public class MissingRequestHeaderExceptionConverter
        implements ExceptionConverter<MissingRequestHeaderException, BadRequestException> {

    @Override
    public BadRequestException convert(MissingRequestHeaderException cause) {
        return new BadRequestException("Missing request header", cause);
    }

}
