package io.credable.reconapi.util.annotations;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.credable.reconapi.dto.ColumnConfigRequest;
import io.credable.reconapi.dto.DatabaseConfigRequest;
import io.credable.reconapi.dto.SftpConfigRequest;
import io.credable.reconapi.enums.ConnectorType;
import io.credable.reconapi.enums.Datasource;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import java.util.Objects;

public class ConnectorBodyValidator implements ConstraintValidator<ValidBody, Object> {
    private static boolean extracted(Object body, Class type) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
        try {
            objectMapper.readValue(objectMapper.writeValueAsString(body), type);
            return true;
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void initialize(ValidBody constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext constraintValidatorContext) {
        if (value instanceof ColumnConfigRequest) {
            BeanWrapper wrapper = new BeanWrapperImpl(value);
            Datasource leftDatasource = (Datasource) wrapper.getPropertyValue("leftDatasource");
            Datasource rightDatasource = (Datasource) wrapper.getPropertyValue("rightDatasource");
            String leftConnectorId = String.valueOf(wrapper.getPropertyValue("leftConnectorId"));
            String rightConnectorId = String.valueOf(wrapper.getPropertyValue("rightConnectorId"));


        }
        BeanWrapper wrapper = new BeanWrapperImpl(value);
        String connectorType = (String) wrapper.getPropertyValue("connectorType");
        Object body = wrapper.getPropertyValue("body");


        boolean result;
        String message = "";
        if (Objects.equals(connectorType, ConnectorType.DATABASE.getType()) && !extracted(body, DatabaseConfigRequest.class)) {
            result = false;
            message = "Invalid body given for 'database' connector type";
        } else if (Objects.equals(connectorType, ConnectorType.FTP.getType()) && !extracted(body, SftpConfigRequest.class)) {
            result = false;
            message = "Invalid body given for 'ftp' connector type";
        } else
            result = true;

        if (!result) {
            constraintValidatorContext.disableDefaultConstraintViolation();
            constraintValidatorContext.buildConstraintViolationWithTemplate(message)
                    .addPropertyNode("body")
                    .addConstraintViolation();
        }
        return result;

    }

}
