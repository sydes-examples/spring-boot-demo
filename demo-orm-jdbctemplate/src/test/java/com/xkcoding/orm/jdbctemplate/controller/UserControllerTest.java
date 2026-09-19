package com.xkcoding.orm.jdbctemplate.controller;

import cn.hutool.core.lang.Dict;
import com.xkcoding.orm.jdbctemplate.entity.User;
import com.xkcoding.orm.jdbctemplate.service.IUserService;
import org.junit.Test;
import org.springframework.dao.EmptyResultDataAccessException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * <p>
 * UserController 单元测试，不依赖真实数据库
 * </p>
 */
public class UserControllerTest {

    /**
     * 查询不存在的用户时，应返回 404，而不是让底层异常变成 500
     */
    @Test
    public void getUserReturns404WhenUserNotFound() {
        IUserService userService = mock(IUserService.class);
        when(userService.getUser(999L)).thenThrow(new EmptyResultDataAccessException(1));
        UserController controller = new UserController(userService);

        Dict result = controller.getUser(999L);

        assertEquals(404, (int) result.getInt("code"));
        assertNull(result.get("data"));
    }

    /**
     * 查询存在的用户时，行为保持不变
     */
    @Test
    public void getUserReturns200WhenUserFound() {
        IUserService userService = mock(IUserService.class);
        User user = new User();
        user.setId(1L);
        when(userService.getUser(1L)).thenReturn(user);
        UserController controller = new UserController(userService);

        Dict result = controller.getUser(1L);

        assertEquals(200, (int) result.getInt("code"));
        assertEquals(user, result.get("data"));
    }
}
