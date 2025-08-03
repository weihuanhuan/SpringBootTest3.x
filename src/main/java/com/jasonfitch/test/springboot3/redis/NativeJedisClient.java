package com.jasonfitch.test.springboot3.redis;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;

@Component
public class NativeJedisClient {

    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;

    @Value("${spring.data.redis.database}")
    private int database;

    @Value("${spring.data.redis.username}")
    private String username;

    @Value("${spring.data.redis.password}")
    private String password;

    public Jedis initNewJedis() {
        System.out.println(this);

        Jedis jedis = new Jedis(host, port);

        if (!isBlank(password)) {
            if (isBlank(username)) {
                jedis.auth(password);
            } else {
                jedis.auth(username, password);
            }
        }

        if (database >= 0) {
            jedis.select(database);
        }
        return jedis;
    }

    private boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getDatabase() {
        return database;
    }

    public void setDatabase(int database) {
        this.database = database;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return "NativeJedisClient{" +
                "host='" + host + '\'' +
                ", port=" + port +
                ", database=" + database +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                '}';
    }

}
