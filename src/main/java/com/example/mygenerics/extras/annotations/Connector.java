package io.credable.reconapi.util.annotations;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ConnectorEnumValidator.class)
@Documented
public @interface Connector {
    String message() default "Must be one of {value}";

    String[] value();

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
