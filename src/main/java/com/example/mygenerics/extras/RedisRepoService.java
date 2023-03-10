package com.example.mygenerics.extras;

import java.util.List;
import java.util.concurrent.TimeUnit;

public interface RedisRepoService {
    Long rightPushListToRedis(String key, String... value);

    Long leftPushListToRedis(String key, String value);

    void saveStringToRedis(String key, String value);
    void appendStringToRedis(String key, String value);

    Long size(String key);

    List<String> getListFromRedis(String key);
    List<String> getListFromRedisGivenRange(String key, long start, long end);

    String getStringFromRedis(String key);

    Boolean hasKey(String key);

    Boolean removeFromRedis(String key);
    Long removeMultipleFromRedis(List<String> keys);
    Boolean expireKey(String key, long time, TimeUnit timeUnit);

    Long removeFromRedisList(String key, long count, String value);
    Long indexOf(String key, String value);
    void set(String key,long index, String value);

}
