package com.jasonfitch.test.springboot3.redis;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisConnectionUtils;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import redis.clients.jedis.Jedis;

import java.util.Objects;

@SpringBootTest
public class RedisTemplateSelectTest {

    private static final String TEST_KEY = "test-key";
    private static final String TEST_VALUE = "test-value";

    @Autowired
    private SimpleRedisClient simpleRedisClient;

    @Test
    public void testSelectWithRedisCallback() {
        StringRedisTemplate template = simpleRedisClient.getTemplate();

        Void unused = template.execute((RedisCallback<Void>) redisConnection -> {
            // select implemented by native connection
            // org.springframework.data.redis.connection.RedisConnection is an interface implemented by JedisConnection
            redisConnection.select(2);
            assertSelectDB(redisConnection, 2);

            // RedisCommands aggregate all supported redis commands
            // select inherited from org.springframework.data.redis.connection.RedisConnectionCommands.select
            // org.springframework.data.redis.connection.RedisCommands is an interface implemented by JedisConnection
            redisConnection.commands().select(3);
            assertSelectDB(redisConnection, 3);
            return null;
        });
    }

    @Test
    public void testSelectWithSessionCallback() {
        StringRedisTemplate template = simpleRedisClient.getTemplate();

        String value = template.execute(new SessionCallback<>() {
            @Override
            public <K, V> String execute(RedisOperations<K, V> operations) throws DataAccessException {
                // org.springframework.data.redis.core.RedisOperations is an interface implemented by RedisTemplate
                operations.execute((RedisCallback<Void>) connection -> {
                    connection.select(4);
                    assertSelectDB(connection, 4);
                    return null;
                });

                // org.springframework.data.redis.core.ValueOperations is a field member used by RedisTemplate
                ValueOperations<K, V> opsForValue = operations.opsForValue();

                //TODO how to implement type safely instead of casting type for input/output value?
                // @see: org.springframework.data.redis.support.atomic.CompareAndSet.execute
                opsForValue.set((K) TEST_KEY, (V) TEST_VALUE);
                String value = (String) opsForValue.get(TEST_KEY);

                RedisConnectionFactory connectionFactory = template.getRequiredConnectionFactory();
                RedisConnection redisConnection = RedisConnectionUtils.doGetConnection(connectionFactory, false, false, false);
                assertSelectDB(redisConnection, 4);
                return value;
            }
        });
    }

    private void assertSelectDB(RedisConnection connection, int expectedDB) {
        // org.springframework.data.redis.connection.RedisConnection.getNativeConnection
        // for cluster, native connection is JedisCluster
        // for standalone, native connection is Jedis
        Object nativeConnection = connection.getNativeConnection();

        Jedis jedis = (Jedis) nativeConnection;
        Thread thread = Thread.currentThread();
        int currentDB = jedis.getDB();

        System.out.println("jedis=" + jedis);
        System.out.println("toIdentityString=" + Objects.toIdentityString(jedis));
        System.out.println("thread=" + thread);
        System.out.println("currentDB=" + currentDB);
        Assertions.assertEquals(expectedDB, currentDB);
    }

}
