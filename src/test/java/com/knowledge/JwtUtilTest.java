package com.knowledge;

import com.knowledge.common.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class JwtUtilTest {

    @Autowired
    private JwtUtil jwtUtil;

    @Test
    void shouldCreateAndParseAccessToken() {
        String token = jwtUtil.createAccessToken(1L);
        System.out.println("access_token = " + token);

        Claims claims = jwtUtil.parse(token);
        assertEquals(1L, Long.parseLong(claims.getSubject()));
        assertEquals("access", claims.get("type"));
    }

    @Test
    void refreshTokenTypeShouldBeRefresh() {
        String token = jwtUtil.createRefreshToken(1L);
        assertEquals("refresh", jwtUtil.parse(token).get("type"));
    }
}
