package guru.nicks.exception.converter;

import guru.nicks.exception.ExceptionConverter;
import guru.nicks.exception.http.PayloadTooLargeException;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartException;

/**
 * {@link MultipartException} is thrown if request size or uploaded file size exceeds the limit. See
 * {@code spring.servlet: multipart.max-request-size}, {@code spring.servlet.multipart.max-file-size}.
 */
@Component
public class MultipartExceptionConverter implements ExceptionConverter<MultipartException, PayloadTooLargeException> {
}
