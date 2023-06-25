package io.credable.reconapi.util.annotations;


import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ListContainsValidator.class)
@Documented
public @interface ListContains {
    String message() default "List must only contain {value}";

    String[] value() default {};

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

