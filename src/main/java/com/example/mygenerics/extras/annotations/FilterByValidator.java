package io.credable.reconapi.util.annotations;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;

public class FilterByValidator implements ConstraintValidator<FilterBy, String> {
    String[] allowedValues;

    @Override
    public void initialize(FilterBy constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
        allowedValues = constraintAnnotation.value();
    }

    @Override
    public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {
        return Arrays.asList(allowedValues).contains(s);
    }
}
