package com.blog.service;

import com.blog.dao.UserDao;
import com.blog.entity.User;
import com.blog.service.impl.UserServiceImpl;
import com.blog.util.PasswordUtil;
import org.junit.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class UserServicePasswordMigrationTest {

    @Test
    public void registerShouldStoreBcryptPassword() throws Exception {
        AtomicReference<User> insertedUser = new AtomicReference<>();
        UserDao userDao = createUserDao((proxy, method, args) -> {
            if ("existsByUsername".equals(method.getName())) {
                return false;
            }
            if ("insert".equals(method.getName())) {
                insertedUser.set((User) args[0]);
                return true;
            }
            return defaultValue(method.getReturnType());
        });
        UserServiceImpl service = createService(userDao);

        String result = service.register(
                "new_user",
                "StrongPass123!",
                "新用户",
                "new@example.com"
        );

        assertNull(result);
        assertNotNull(insertedUser.get());
        assertTrue(PasswordUtil.verify("StrongPass123!", insertedUser.get().getPassword()));
        assertFalse(PasswordUtil.needsRehash(insertedUser.get().getPassword()));
    }

    @Test
    public void changePasswordShouldStoreBcryptPassword() throws Exception {
        User storedUser = createStoredUser("0192023a7bbd73250516f069df18b500");
        AtomicReference<String> updatedHash = new AtomicReference<>();
        UserDao userDao = createUserDao((proxy, method, args) -> {
            if ("findById".equals(method.getName())) {
                return storedUser;
            }
            if ("updatePassword".equals(method.getName())) {
                updatedHash.set((String) args[1]);
                return true;
            }
            return defaultValue(method.getReturnType());
        });
        UserServiceImpl service = createService(userDao);

        String result = service.changePassword(7, "admin123", "NewPass123!");

        assertNull(result);
        assertTrue(PasswordUtil.verify("NewPass123!", updatedHash.get()));
        assertFalse(PasswordUtil.needsRehash(updatedHash.get()));
    }

    @Test
    public void loginShouldUpgradeLegacyMd5PasswordToBcrypt() throws Exception {
        User storedUser = createStoredUser("0192023a7bbd73250516f069df18b500");
        AtomicInteger updateCount = new AtomicInteger();
        AtomicReference<String> updatedHash = new AtomicReference<>();
        UserServiceImpl service = createService(storedUser, true, updateCount, updatedHash);

        User loggedInUser = service.login("legacy-user", "admin123");

        assertNotNull(loggedInUser);
        assertNull("登录成功后不应向上层返回密码哈希", loggedInUser.getPassword());
        assertTrue("旧 MD5 登录成功后必须更新密码", updateCount.get() == 1);
        assertTrue(PasswordUtil.verify("admin123", updatedHash.get()));
        assertFalse(PasswordUtil.needsRehash(updatedHash.get()));
    }

    @Test
    public void loginShouldNotRewriteExistingBcryptPassword() throws Exception {
        User storedUser = createStoredUser(PasswordUtil.hash("admin123"));
        AtomicInteger updateCount = new AtomicInteger();
        UserServiceImpl service = createService(
                storedUser,
                true,
                updateCount,
                new AtomicReference<>()
        );

        User loggedInUser = service.login("legacy-user", "admin123");

        assertNotNull(loggedInUser);
        assertTrue("已有 BCrypt 哈希不应重复写回", updateCount.get() == 0);
    }

    @Test
    public void loginShouldContinueWhenLegacyHashUpgradeFails() throws Exception {
        User storedUser = createStoredUser("0192023a7bbd73250516f069df18b500");
        AtomicInteger updateCount = new AtomicInteger();
        UserServiceImpl service = createService(
                storedUser,
                false,
                updateCount,
                new AtomicReference<>()
        );

        User loggedInUser = service.login("legacy-user", "admin123");

        assertNotNull("旧密码已验证成功时，升级失败不应锁定用户", loggedInUser);
        assertTrue(updateCount.get() == 1);
    }

    private User createStoredUser(String passwordHash) {
        User user = new User();
        user.setId(7);
        user.setUsername("legacy-user");
        user.setPassword(passwordHash);
        user.setStatus(1);
        return user;
    }

    private UserServiceImpl createService(
            User storedUser,
            boolean updateResult,
            AtomicInteger updateCount,
            AtomicReference<String> updatedHash
    ) throws Exception {
        UserDao userDao = createUserDao((proxy, method, args) -> {
            if ("findByUsername".equals(method.getName())) {
                return storedUser;
            }
            if ("updatePassword".equals(method.getName())) {
                updateCount.incrementAndGet();
                updatedHash.set((String) args[1]);
                return updateResult;
            }
            return defaultValue(method.getReturnType());
        });
        return createService(userDao);
    }

    private UserDao createUserDao(InvocationHandler invocationHandler) {
        return (UserDao) Proxy.newProxyInstance(
                UserDao.class.getClassLoader(),
                new Class<?>[]{UserDao.class},
                invocationHandler
        );
    }

    private UserServiceImpl createService(UserDao userDao) {
        return new UserServiceImpl(userDao);
    }

    private Object defaultValue(Class<?> returnType) {
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == int.class) {
            return 0;
        }
        return null;
    }
}
