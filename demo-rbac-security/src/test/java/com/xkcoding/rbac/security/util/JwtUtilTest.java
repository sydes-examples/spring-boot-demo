package com.xkcoding.rbac.security.util;

import com.xkcoding.rbac.security.common.Consts;
import com.xkcoding.rbac.security.common.Status;
import com.xkcoding.rbac.security.config.JwtConfig;
import com.xkcoding.rbac.security.exception.SecurityException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

/**
 * <p>
 * 测试 JwtUtil#parseJWT 的 Redis 交叉校验规则，重点验证 token 刷新后的
 * "上一个 token 宽限期" 行为（calibration case JAVA-H-01）。
 * </p>
 *
 * 这是一个纯 Mockito 单元测试，不依赖 Spring 上下文或真实 Redis，
 * 沿用 {@code MonitorServiceTest} 中已经验证过的写法。
 *
 * @author yangkai.shen
 */
@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class JwtUtilTest {
    private static final String SIGNING_KEY = "test-jwt-signing-key";

    private static final String USERNAME = "testuser";

    private static final String PREVIOUS_KEY_SUFFIX = ":previous";

    @Mock
    private JwtConfig jwtConfig;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private JwtUtil jwtUtil;

    private String redisKey;

    @Before
    public void setUp() {
        when(jwtConfig.getKey()).thenReturn(SIGNING_KEY);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        redisKey = Consts.REDIS_JWT_KEY_PREFIX + USERNAME;
    }

    /**
     * 生成一个签名有效的 JWT，nonce 用于保证不同 token 之间字符串不同
     * （避免同一毫秒内生成的 token 因内容完全相同而无法区分新旧）。
     */
    private String buildJwt(String nonce) {
        return Jwts.builder().setId(nonce).setSubject(USERNAME).setIssuedAt(new Date()).signWith(SignatureAlgorithm.HS256, SIGNING_KEY).compact();
    }

    @Test
    public void parseJWT_currentTokenMatchesRedis_succeeds() {
        String jwt = buildJwt("current");
        when(stringRedisTemplate.getExpire(redisKey, TimeUnit.MILLISECONDS)).thenReturn(60000L);
        when(valueOperations.get(redisKey)).thenReturn(jwt);

        Claims claims = jwtUtil.parseJWT(jwt);

        assertNotNull(claims);
        assertEquals(USERNAME, claims.getSubject());
    }

    @Test
    public void parseJWT_previousTokenMatchesWithinGraceWindow_succeeds() {
        String oldJwt = buildJwt("old");
        String newJwt = buildJwt("new");
        when(stringRedisTemplate.getExpire(redisKey, TimeUnit.MILLISECONDS)).thenReturn(60000L);
        // Redis 中已经是刷新后的新 token
        when(valueOperations.get(redisKey)).thenReturn(newJwt);
        // 旧 token 仍处于宽限期内
        when(valueOperations.get(redisKey + PREVIOUS_KEY_SUFFIX)).thenReturn(oldJwt);

        // 携带刷新前的旧 token 请求，宽限期内应当仍然校验通过
        Claims claims = jwtUtil.parseJWT(oldJwt);

        assertNotNull(claims);
        assertEquals(USERNAME, claims.getSubject());
    }

    @Test
    public void parseJWT_tokenMatchesNeitherCurrentNorPrevious_throwsTokenOutOfCtrl() {
        String currentJwt = buildJwt("current");
        String previousJwt = buildJwt("previous");
        String unknownJwt = buildJwt("unknown");
        when(stringRedisTemplate.getExpire(redisKey, TimeUnit.MILLISECONDS)).thenReturn(60000L);
        when(valueOperations.get(redisKey)).thenReturn(currentJwt);
        when(valueOperations.get(redisKey + PREVIOUS_KEY_SUFFIX)).thenReturn(previousJwt);

        try {
            jwtUtil.parseJWT(unknownJwt);
            fail("Expected SecurityException with TOKEN_OUT_OF_CTRL status");
        } catch (SecurityException e) {
            assertEquals(Status.TOKEN_OUT_OF_CTRL.getCode(), e.getCode());
        }
    }
}
