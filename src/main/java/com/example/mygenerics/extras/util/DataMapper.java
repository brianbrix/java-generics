package io.credable.reconapi.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;

public class DataMapper {
    public static <S, T> T doMap(S sourceData, Class<T> destClass) {
        ObjectMapper objectMapper = new ObjectMapper();
        configureObjectMapper(objectMapper);

//        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return objectMapper.convertValue(sourceData, destClass);

    }

    public static <S, T> List<T> mapCollection(Collection<S> sourceData, Class<T> destClass) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return objectMapper.convertValue(sourceData, objectMapper.getTypeFactory().constructCollectionType(Collection.class, destClass));

    }

    private static void configureObjectMapper(ObjectMapper objectMapper) {
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;

        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(formatter));
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(formatter));

        objectMapper.registerModule(javaTimeModule);
    }


}
