package io.credable.reconapi.util.annotations;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;

public class ConnectorEnumValidator implements ConstraintValidator<Connector, String> {
    String[] allowedValues;

    @Override
    public void initialize(Connector constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
        allowedValues = constraintAnnotation.value();
    }

    @Override
    public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {
        return Arrays.asList(allowedValues).contains(s);

    }
}
