package com.example.starhub.config.cache;


import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CustomCacheable {
    String key();          // 캐시 Key
    long ttl() default 300; // TTL (초 단위)
}
