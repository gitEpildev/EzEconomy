package com.skyblockexp.ezeconomy.lock;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.SetArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class RedisLockManager implements LockManager, AutoCloseable {
    private final RedisClient client;
    private final StatefulRedisConnection<String, String> connection;
    private final RedisCommands<String, String> commands;
    private final String prefix = "ezeconomy:lock:";

    public RedisLockManager(String host, int port, String password, int database) {
        RedisURI.Builder b = RedisURI.builder().withHost(host).withPort(port).withDatabase(database);
        if (password != null && !password.isEmpty()) b.withPassword(password.toCharArray());
        RedisURI uri = b.build();
        this.client = RedisClient.create(uri);
        this.connection = client.connect();
        this.commands = connection.sync();
    }

    @Override
    public String acquire(UUID uuid, long ttlMs, long retryMs, int maxAttempts) throws InterruptedException {
        String key = prefix + uuid.toString();
        String token = UUID.randomUUID().toString();
        SetArgs args = SetArgs.Builder.nx().px(ttlMs);
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            try {
                String res = commands.set(key, token, args);
                if ("OK".equalsIgnoreCase(res)) return token;
            } catch (Exception ex) {
                throw new RuntimeException("Redis error while acquiring lock: " + ex.getMessage(), ex);
            }
            TimeUnit.MILLISECONDS.sleep(retryMs);
        }
        return null;
    }

    @Override
    public boolean release(UUID uuid, String token) {
        String key = prefix + uuid.toString();
        String script = "if redis.call('get',KEYS[1]) == ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end";
        try {
            Object res = commands.eval(script, ScriptOutputType.INTEGER, new String[]{key}, token);
            if (res instanceof Number) {
                return ((Number) res).longValue() == 1L;
            }
            return false;
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    public void close() {
        try {
            if (connection != null) connection.close();
        } finally {
            if (client != null) client.shutdown();
        }
    }
}
