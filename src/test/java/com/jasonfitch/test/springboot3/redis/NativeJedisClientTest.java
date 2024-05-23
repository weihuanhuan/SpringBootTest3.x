package com.jasonfitch.test.springboot3.redis;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import redis.clients.jedis.Jedis;

@SpringBootTest
public class NativeJedisClientTest {

    @Autowired
    private NativeJedisClient nativeJedisClient;

    @Test
    public void testPing() {
        try (Jedis jedis = nativeJedisClient.initNewJedis()) {
            String ping = jedis.ping();
            int db = jedis.getDB();

            System.out.println("jedis=" + jedis);
            System.out.println("ping=" + ping);
            System.out.println("db=" + db);

            String messagePing = String.format("Ping failed with Jedis [%s]!", jedis);
            Assertions.assertEquals("PONG", ping, messagePing);
            Assertions.assertEquals(nativeJedisClient.getDatabase(), db);
        }
    }

}
