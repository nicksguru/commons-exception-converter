package guru.nicks.commons.exception.visitor;

import guru.nicks.commons.designpattern.visitor.ReflectionVisitor;
import guru.nicks.commons.designpattern.visitor.ReflectionVisitorMethod;
import guru.nicks.commons.rest.v1.dto.FieldErrorDto;
import guru.nicks.commons.rest.v1.mapper.FieldErrorMapper;
import guru.nicks.commons.utils.TransformUtils;

import com.google.common.collect.Iterables;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.Optional;

/**
 * Extracts {@link FieldError} out of known exception classes (see all methods called 'visit'). Returns either an empty
 * Optional if exception class is unknown (from the field errors extraction perspective) - or an empty list inside a
 * non-empty Optional if a known exception class contained no errors.
 * <p>
 * Field names are masked with {@link FieldErrorMapper#maskFieldName(String)}.
 */
@Component
@RequiredArgsConstructor
public class FieldErrorDiscovererVisitor extends ReflectionVisitor<List<FieldErrorDto>> {

    // DI
    private final FieldErrorMapper fieldErrorMapper;

    /**
     * Reports error raised by (usually custom) {@link ConstraintValidator}.
     *
     * @param e exception
     * @return field-level error
     */
    @ReflectionVisitorMethod
    public Optional<List<FieldErrorDto>> visit(ConstraintViolationException e) {
        // only field values are known in this case, not field names
        return Optional.of(TransformUtils.toList(e.getConstraintViolations(), violation -> {
            // 'ObjectClassOrMethodName.nested.field' -> 'field' (bean / method argument validation)
            String fieldName = Optional.ofNullable(violation.getPropertyPath())
                    .filter(path -> path.iterator().hasNext())
                    // throws NoSuchElementException if path is empty, hence check above
                    .map(Iterables::getLast)
                    .map(Path.Node::getName)
                    .filter(StringUtils::isNotBlank)
                    .orElse("<unknown>");

            return new FieldErrorDto(fieldName, "Constraint",
                    // raw messages are e.g. 'must be a well-formed email address' (lowercase 1st letter)
                    StringUtils.capitalize(violation.getMessage()), null);

        }));
    }

    /**
     * Reports a missing (i.e. {@code null} in terms of validation) GET request parameter.
     *
     * @param e exception
     * @return field-level error
     */
    @ReflectionVisitorMethod
    public Optional<List<FieldErrorDto>> visit(MissingServletRequestParameterException e) {
        return Optional.of(List.of(
                new FieldErrorDto(FieldErrorMapper.maskFieldName(e.getParameterName()), "NotNull",
                        "Missing mandatory parameter", null)));
    }

    /**
     * Reports error which occurs when request field type contradicts to that declared in DTO.
     *
     * @param e exception
     * @return field-level error
     */
    @ReflectionVisitorMethod
    public Optional<List<FieldErrorDto>> visit(MethodArgumentTypeMismatchException e) {
        return Optional.of(List.of(
                new FieldErrorDto(
                        FieldErrorMapper.maskFieldName(e.getName()),
                        // e.g. for enums: typeMismatch -> TypeMismatch
                        StringUtils.capitalize(e.getErrorCode()),
                        Strings.CI.contains(e.getMessage(), "enum") ? "Enumeration" : "",
                        null)));
    }

    @ReflectionVisitorMethod
    public Optional<List<FieldErrorDto>> visit(MethodArgumentNotValidException e) {
        return Optional.of(TransformUtils.toList(
                e.getBindingResult().getFieldErrors(), fieldErrorMapper::toDto));
    }

    @ReflectionVisitorMethod
    public Optional<List<FieldErrorDto>> visit(BindException e) {
        return Optional.of(TransformUtils.toList(
                e.getFieldErrors(), fieldErrorMapper::toDto));
    }

    @ReflectionVisitorMethod
    public Optional<List<FieldErrorDto>> visit(ValidationException e) {
        Throwable cause = e.getCause();

        if (!(cause instanceof BindException)) {
            return Optional.empty();
        }

        return Optional.of(TransformUtils.toList(
                ((BindException) cause).getFieldErrors(), fieldErrorMapper::toDto));
    }

}
