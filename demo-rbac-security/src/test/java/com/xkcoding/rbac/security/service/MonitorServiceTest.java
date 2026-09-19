package com.xkcoding.rbac.security.service;

import com.xkcoding.rbac.security.common.Consts;
import com.xkcoding.rbac.security.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;

/**
 * <p>
 * 测试 MonitorService，重点验证 kickout 会过滤空白/重复的用户名
 * </p>
 *
 * 这是一个纯 Mockito 单元测试，不依赖 Spring 上下文或真实 Redis。
 *
 * @author yangkai.shen
 */
@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class MonitorServiceTest {
    @Mock
    private RedisUtil redisUtil;

    @InjectMocks
    private MonitorService monitorService;

    @Before
    public void setUp() {
        // kickout() 内部会调用 SecurityUtil.getCurrentUsername()，其底层读取
        // SecurityContextHolder.getContext().getAuthentication()，默认为 null 会导致 NPE，
        // 这里填充一个最简单的 Authentication 以避免 NPE（无需真实登录用户信息）。
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("testuser", null));
    }

    @After
    public void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void kickoutFiltersBlankAndDuplicateNames() {
        List<String> names = Arrays.asList("alice", "", null, "bob", "alice", "   ", "bob");

        monitorService.kickout(names);

        ArgumentCaptor<Collection<String>> redisKeysCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(redisUtil).delete(redisKeysCaptor.capture());

        Collection<String> redisKeys = redisKeysCaptor.getValue();
        assertEquals(2, redisKeys.size());
        assertTrue(redisKeys.contains(Consts.REDIS_JWT_KEY_PREFIX + "alice"));
        assertTrue(redisKeys.contains(Consts.REDIS_JWT_KEY_PREFIX + "bob"));
    }
}
