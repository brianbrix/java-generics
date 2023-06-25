package io.credable.reconapi.util.annotations;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.TYPE;

@Target({TYPE, ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ConnectorBodyValidator.class)
@Documented
public @interface ValidBody {
    String message() default "Invalid body for given connectorType";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
