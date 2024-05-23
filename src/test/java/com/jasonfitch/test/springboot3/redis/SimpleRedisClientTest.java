package com.jasonfitch.test.springboot3.redis;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootTest
public class SimpleRedisClientTest {

    @Autowired
    private SimpleRedisClient simpleRedisClient;

    @Test
    public void testHasKey() {
        StringRedisTemplate template = simpleRedisClient.getTemplate();
        Boolean hasKey = simpleRedisClient.hasKey("dummy-non-exist-key");

        System.out.println("template=" + template);
        System.out.println("hasKey=" + hasKey);

        Assertions.assertFalse(hasKey);
    }

}
