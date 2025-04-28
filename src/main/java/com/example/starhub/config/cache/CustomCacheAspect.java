package com.example.starhub.config.cache;

import com.example.starhub.config.redis.RedisService;
import com.example.starhub.config.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Optional;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class CustomCacheAspect {

    private final RedisService redisService;

    @Around("@annotation(com.example.starhub.config.cache.CustomCacheable)")
    public Object handleCustomCache(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        CustomCacheable customCacheable = method.getAnnotation(CustomCacheable.class);

        String cacheKey = customCacheable.key();
        long ttl = customCacheable.ttl();

        log.debug("[CACHE] Checking cache for key: {}", cacheKey);

        // 1. 캐시 조회
        Optional<String> cachedValue = redisService.getValues(cacheKey);
        if (cachedValue.isPresent()) {
            log.debug("[CACHE] Hit for key: {}", cacheKey);
            Class<?> returnType = signature.getReturnType();
            return JsonUtil.fromJson(cachedValue.get(), returnType);
        }

        // 2. 캐시 미스 → 원래 메서드 실행
        Object result = joinPoint.proceed();

        // 3. 결과 캐시 저장
        String jsonResult = JsonUtil.toJson(result);
        redisService.setValues(cacheKey, jsonResult, Duration.ofSeconds(ttl));

        log.debug("[CACHE] Stored key: {} with TTL: {}s", cacheKey, ttl);

        return result;
    }
}
