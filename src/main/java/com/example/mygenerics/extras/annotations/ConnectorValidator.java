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
@Constraint(validatedBy = ConfigConnectorValidator.class)
@Documented
public @interface ConnectorValidator {
    String message() default "You must specify a connectorId for the given dataSource.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
