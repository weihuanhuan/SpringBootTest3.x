package com.jasonfitch.test.springboot3.redis;

import org.springframework.stereotype.Component;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * A simple Redis client.
 * <p>
 * reference: <a href="https://docs.spring.io/spring-boot/docs/3.2.0/reference/htmlsingle/index.html#data.nosql.redis">9.2.1. Redis</a>
 */
@Component
public class SimpleRedisClient {

    private final StringRedisTemplate template;

    public SimpleRedisClient(StringRedisTemplate template) {
        this.template = template;
    }

    public Boolean hasKey(String key) {
        return template.hasKey(key);
    }

    public StringRedisTemplate getTemplate() {
        return template;
    }

}
