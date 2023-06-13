package io.credable.reconapi.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collection;
import java.util.List;

public class DataMapper {
    public static <S,T> T doMap(S sourceData, Class<T> destClass)
    {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return objectMapper.convertValue(sourceData,destClass);

    }

    public static <S,T> List<T> mapCollection(Collection<S> sourceData, Class<T> destClass)
    {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return objectMapper.convertValue(sourceData,  objectMapper.getTypeFactory().constructCollectionType(Collection.class, destClass));

    }



}
