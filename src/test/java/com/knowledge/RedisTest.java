package com.knowledge;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class RedisTest {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void shouldSetAndGet() {
        // 写入
        stringRedisTemplate.opsForValue().set("test:key", "hello-redis");
        // 读出并断言
        String value = stringRedisTemplate.opsForValue().get("test:key");
        assertEquals("hello-redis", value);
        // 清理测试数据
        stringRedisTemplate.delete("test:key");
    }
}
