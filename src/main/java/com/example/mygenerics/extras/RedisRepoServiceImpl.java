package com.example.mygenerics.extras;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.isNull;

@Service
public class RedisRepoServiceImpl implements RedisRepoService {


    private StringRedisTemplate redisTemplate;

    public StringRedisTemplate getRedisTemplate() {
        return redisTemplate;
    }

    @Autowired
    public void setRedisTemplate(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    @Retryable(maxAttemptsExpression = "${app.retry.maxAttempts}", value = RetryableException.class,
               backoff = @Backoff(random = true, delayExpression = "${app.retry.delay}", maxDelayExpression = "${app" +
                       ".retry.maxDelay}", multiplierExpression = "${app.retry.multiplier}"))
    public Long rightPushListToRedis(String key, String... values) {
        return redisTemplate.opsForList().rightPushAll(key, values);

    }

    @Override
    @Retryable(maxAttemptsExpression = "${app.retry.maxAttempts}", value = RetryableException.class,
               backoff = @Backoff(random = true, delayExpression = "${app.retry.delay}", maxDelayExpression = "${app" +
                       ".retry.maxDelay}", multiplierExpression = "${app.retry.multiplier}"))
    public Long leftPushListToRedis(String key, String value) {
        return redisTemplate.opsForList().leftPush(key, value);
    }

    @Override
    @Retryable(maxAttemptsExpression = "${app.retry.maxAttempts}", value = RetryableException.class,
               backoff = @Backoff(random = true, delayExpression = "${app.retry.delay}", maxDelayExpression = "${app" +
                       ".retry.maxDelay}", multiplierExpression = "${app.retry.multiplier}"))
    public void saveStringToRedis(String key, String value) {
        redisTemplate.opsForValue().set(key, value);

    }

    @Override
    @Retryable(maxAttemptsExpression = "${app.retry.maxAttempts}", value = RetryableException.class,
               backoff = @Backoff(random = true, delayExpression = "${app.retry.delay}", maxDelayExpression = "${app" +
                       ".retry.maxDelay}", multiplierExpression = "${app.retry.multiplier}"))
    public void appendStringToRedis(String key, String value) {
        redisTemplate.opsForValue().append(key, value);

    }

    @Override
    @Retryable(maxAttemptsExpression = "${app.retry.maxAttempts}", value = RetryableException.class,
               backoff = @Backoff(random = true, delayExpression = "${app.retry.delay}", maxDelayExpression = "${app" +
                       ".retry.maxDelay}", multiplierExpression = "${app.retry.multiplier}"))
    public Long size(String key) {
        return redisTemplate.opsForList().size(key);
    }

    @Override
    @Retryable(maxAttemptsExpression = "${app.retry.maxAttempts}", value = RetryableException.class,
               backoff = @Backoff(random = true, delayExpression = "${app.retry.delay}", maxDelayExpression = "${app" +
                       ".retry.maxDelay}", multiplierExpression = "${app.retry.multiplier}"))
    public List<String> getListFromRedis(String key) {
        return redisTemplate.opsForList().range(key, 0, -1);
    }

    @Override
    @Retryable(maxAttemptsExpression = "${app.retry.maxAttempts}", value = RetryableException.class,
               backoff = @Backoff(random = true, delayExpression = "${app.retry.delay}", maxDelayExpression = "${app" +
                       ".retry.maxDelay}", multiplierExpression = "${app.retry.multiplier}"))
    public List<String> getListFromRedisGivenRange(String key, long start, long end) {
        return redisTemplate.opsForList().range(key, start, end);
    }

    @Override
    @Retryable(maxAttemptsExpression = "${app.retry.maxAttempts}", value = RetryableException.class,
               backoff = @Backoff(random = true, delayExpression = "${app.retry.delay}", maxDelayExpression = "${app" +
                       ".retry.maxDelay}", multiplierExpression = "${app.retry.multiplier}"))
    public String getStringFromRedis(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    @Override
    @Retryable(maxAttemptsExpression = "${app.retry.maxAttempts}", value = RetryableException.class,
               backoff = @Backoff(random = true, delayExpression = "${app.retry.delay}", maxDelayExpression = "${app" +
                       ".retry.maxDelay}", multiplierExpression = "${app.retry.multiplier}"))
    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    @Override
    @Retryable(maxAttemptsExpression = "${app.retry.maxAttempts}", value = RetryableException.class,
               backoff = @Backoff(random = true, delayExpression = "${app.retry.delay}", maxDelayExpression = "${app" +
                       ".retry.maxDelay}", multiplierExpression = "${app.retry.multiplier}"))
    public Boolean removeFromRedis(String key) {
        return redisTemplate.delete(key);
    }

    @Override
    @Retryable(maxAttemptsExpression = "${app.retry.maxAttempts}", value = RetryableException.class,
            backoff = @Backoff(random = true, delayExpression = "${app.retry.delay}", maxDelayExpression = "${app" +
                    ".retry.maxDelay}", multiplierExpression = "${app.retry.multiplier}"))
    public Long removeMultipleFromRedis(List<String> keys) {
        return redisTemplate.delete(keys);
    }

    @Override
    @Retryable(maxAttemptsExpression = "${app.retry.maxAttempts}", value = RetryableException.class,
               backoff = @Backoff(random = true, delayExpression = "${app.retry.delay}", maxDelayExpression = "${app" +
                       ".retry.maxDelay}", multiplierExpression = "${app.retry.multiplier}"))
    public Boolean expireKey(String key, long time, TimeUnit timeUnit) {
        return redisTemplate.expire(key, time, timeUnit);
    }

    @Override
    @Retryable(maxAttemptsExpression = "${app.retry.maxAttempts}", value = RetryableException.class,
               backoff = @Backoff(random = true, delayExpression = "${app.retry.delay}", maxDelayExpression = "${app" +
                       ".retry.maxDelay}", multiplierExpression = "${app.retry.multiplier}"))
    public Long removeFromRedisList(String key, long count, String value) {
        return redisTemplate.opsForList().remove(key, count, value);
    }

    @Override
    @Retryable(maxAttemptsExpression = "${app.retry.maxAttempts}", value = RetryableException.class,
               backoff = @Backoff(random = true, delayExpression = "${app.retry.delay}", maxDelayExpression = "${app" +
                       ".retry.maxDelay}", multiplierExpression = "${app.retry.multiplier}"))
    public Long indexOf(String key, String value) {
        List<String> strings = redisTemplate.opsForList().range(key, 0, -1);
        if(!isNull(strings) &&!strings.isEmpty()) {
            return (long)strings.indexOf(value);
        }
        return -1L;
    }

    @Override
    @Retryable(maxAttemptsExpression = "${app.retry.maxAttempts}", value = RetryableException.class,
               backoff = @Backoff(random = true, delayExpression = "${app.retry.delay}", maxDelayExpression = "${app" +
                       ".retry.maxDelay}", multiplierExpression = "${app.retry.multiplier}"))
    public void set(String key, long index, String value) {
        redisTemplate.opsForList().set(key, index, value);
    }
}
