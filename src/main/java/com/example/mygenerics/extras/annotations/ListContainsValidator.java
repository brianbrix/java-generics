package io.credable.reconapi.util.annotations;


import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class ListContainsValidator implements ConstraintValidator<ListContains, List<String>> {
    private String[] allowedValues;

    @Override
    public void initialize(ListContains constraintAnnotation) {
        this.allowedValues = constraintAnnotation.value();
    }

    @Override
    public boolean isValid(List<String> value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        return new HashSet<>(Arrays.stream(allowedValues).toList()).containsAll(value);


    }
}
