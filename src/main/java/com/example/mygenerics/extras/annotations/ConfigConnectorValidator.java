package io.credable.reconapi.util.annotations;

import io.credable.reconapi.enums.Datasource;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import java.util.Objects;

public class ConfigConnectorValidator implements ConstraintValidator<ConnectorValidator, Object> {
    @Override
    public void initialize(ConnectorValidator constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext constraintValidatorContext) {
        BeanWrapper wrapper = new BeanWrapperImpl(value);
        String message = "";
        Datasource leftDatasource = (Datasource) wrapper.getPropertyValue("leftDatasource");
        Datasource rightDatasource = (Datasource) wrapper.getPropertyValue("rightDatasource");
        String leftConnectorId = String.valueOf(wrapper.getPropertyValue("leftConnectorId"));
        String rightConnectorId = String.valueOf(wrapper.getPropertyValue("rightConnectorId"));
        boolean result = true;
        String field = "field";
        if (leftDatasource != Datasource.UPLOAD && leftConnectorId.equals("null")) {
            result = false;
            message = "You must specify a 'leftConnectorId' if leftDatasource is not 'UPLOAD'";
            field = "leftConnectorId";

        }
        if (rightDatasource != Datasource.UPLOAD && Objects.equals(rightConnectorId, "null")) {
            result = false;
            message = "You must specify a 'rightConnectorId' if rightDatasource is not 'UPLOAD'";
            field = "rightConnectorId";
        }

        if (!result) {
            constraintValidatorContext.disableDefaultConstraintViolation();
            constraintValidatorContext.buildConstraintViolationWithTemplate(message)
                    .addPropertyNode(field)
                    .addConstraintViolation();
        }
        return result;

    }
}
