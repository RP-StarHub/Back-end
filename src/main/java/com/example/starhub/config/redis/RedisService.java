package com.example.starhub.config.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Redis와 상호작용하기 위한 서비스 클래스.
 * RedisTemplate을 이용해 Redis에 데이터를 저장하거나 조회, 삭제하는 다양한 메서드를 제공합니다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Redis에 키-값 쌍을 저장하는 메서드.
     *
     * @param key 저장할 키
     * @param data 저장할 값
     */
    public void setValues(String key, String data) {
        ValueOperations<String, Object> values = redisTemplate.opsForValue();
        values.set(key, data);
    }

    /**
     * Redis에 키-값 쌍을 지정된 유효 기간과 함께 저장하는 메서드.
     *
     * @param key 저장할 키
     * @param data 저장할 값
     * @param duration 키-값 쌍의 유효 기간
     */
    public void setValues(String key, String data, Duration duration) {
        ValueOperations<String, Object> values = redisTemplate.opsForValue();
        values.set(key, data, duration);
    }

    /**
     * Redis에서 특정 키에 해당하는 값을 조회하는 메서드.
     *
     * @param key 조회할 키
     * @return 해당 키의 값이 존재하면 값을 반환, 없으면 "false"를 반환
     */
    public Optional<String> getValues(String key) {
        ValueOperations<String, Object> values = redisTemplate.opsForValue();
        Object value = values.get(key);
        return Optional.ofNullable(value).map(String::valueOf);
    }

    /**
     * Redis에서 특정 키-값 쌍을 삭제하는 메서드.
     *
     * @param key 삭제할 키
     */
    public void deleteValues(String key) {
        redisTemplate.delete(key);
    }

    /**
     * Redis에서 특정 키의 유효 기간을 설정하는 메서드.
     *
     * @param key 유효 기간을 설정할 키
     * @param timeout 유효 기간(밀리초 단위)
     */
    public void expireValues(String key, int timeout) {
        redisTemplate.expire(key, timeout, TimeUnit.MILLISECONDS);
    }

    /**
     * Redis에 해시(Hash) 데이터를 저장하는 메서드.
     *
     * @param key 저장할 해시 키
     * @param data 저장할 해시 데이터 (Map 형태)
     */
    public void setHashOps(String key, Map<String, String> data) {
        HashOperations<String, Object, Object> values = redisTemplate.opsForHash();
        values.putAll(key, data);
    }

    /**
     * Redis에서 특정 해시 키와 해시 필드 키에 해당하는 값을 조회하는 메서드.
     *
     * @param key 해시 키
     * @param hashKey 해시 필드 키
     * @return 해당 필드의 값이 존재하면 값을 반환, 없으면 빈 문자열 반환
     */
    public String getHashOps(String key, String hashKey) {
        HashOperations<String, Object, Object> values = redisTemplate.opsForHash();
        return Boolean.TRUE.equals(values.hasKey(key, hashKey)) ?
                (String) redisTemplate.opsForHash().get(key, hashKey) : "";
    }

    /**
     * Redis에서 특정 해시 키와 해시 필드 키에 해당하는 데이터를 삭제하는 메서드.
     *
     * @param key 해시 키
     * @param hashKey 삭제할 해시 필드 키
     */
    public void deleteHashOps(String key, String hashKey) {
        HashOperations<String, Object, Object> values = redisTemplate.opsForHash();
        values.delete(key, hashKey);
    }

    /**
     * 특정 값이 존재하는지 확인하는 메서드.
     *
     * @param key 확인할 키 값
     * @return 값이 존재하면 true, 존재하지 않으면 false
     */
    public boolean checkExistsValue(String key) {
        return redisTemplate.hasKey(key);
    }
}
