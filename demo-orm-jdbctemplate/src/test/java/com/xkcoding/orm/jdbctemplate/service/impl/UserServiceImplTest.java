package com.xkcoding.orm.jdbctemplate.service.impl;

import com.xkcoding.orm.jdbctemplate.dao.UserDao;
import com.xkcoding.orm.jdbctemplate.entity.User;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * <p>
 * UserServiceImpl 单元测试，不依赖真实数据库
 * </p>
 *
 * @author yangkai.shen
 * @date Created in 2018-10-15 13:53
 */
public class UserServiceImplTest {

    /**
     * 用户名为空时，保存应该被拒绝，且不应该调用 DAO
     */
    @Test(expected = IllegalArgumentException.class)
    public void saveRejectsBlankUsername() {
        UserDao userDao = mock(UserDao.class);
        UserServiceImpl userService = new UserServiceImpl(userDao);
        User user = new User();
        user.setName("");
        user.setPassword("password");

        try {
            userService.save(user);
        } finally {
            verify(userDao, never()).insert(any(User.class));
        }
    }

    /**
     * 用户名合法时，保存应该正常执行并调用 DAO
     */
    @Test
    public void saveAcceptsNonBlankUsername() {
        UserDao userDao = mock(UserDao.class);
        when(userDao.insert(any(User.class))).thenReturn(1);
        UserServiceImpl userService = new UserServiceImpl(userDao);
        User user = new User();
        user.setName("xkcoding");
        user.setPassword("password");

        assertTrue(userService.save(user));
        verify(userDao).insert(any(User.class));
    }
}
