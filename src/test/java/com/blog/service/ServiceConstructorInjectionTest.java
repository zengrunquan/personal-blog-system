package com.blog.service;

import com.blog.dao.ArticleDao;
import com.blog.dao.CategoryDao;
import com.blog.dao.CommentDao;
import com.blog.dao.UserDao;
import com.blog.entity.Article;
import com.blog.entity.Category;
import com.blog.entity.Comment;
import com.blog.entity.User;
import com.blog.service.impl.ArticleServiceImpl;
import com.blog.service.impl.CategoryServiceImpl;
import com.blog.service.impl.CommentServiceImpl;
import com.blog.service.impl.UserServiceImpl;
import org.junit.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class ServiceConstructorInjectionTest {

    @Test
    public void articleServiceShouldUseInjectedDao() {
        Article expected = new Article();
        ArticleDao articleDao = createDao(ArticleDao.class, (proxy, method, args) -> {
            if ("findById".equals(method.getName())) {
                return expected;
            }
            return defaultValue(method.getReturnType());
        });

        Article actual = new ArticleServiceImpl(articleDao).findById(7);

        assertSame(expected, actual);
    }

    @Test
    public void categoryServiceShouldUseInjectedDao() {
        Category expected = new Category();
        CategoryDao categoryDao = createDao(CategoryDao.class, (proxy, method, args) -> {
            if ("findById".equals(method.getName())) {
                return expected;
            }
            return defaultValue(method.getReturnType());
        });

        Category actual = new CategoryServiceImpl(categoryDao).findById(7);

        assertSame(expected, actual);
    }

    @Test
    public void commentServiceShouldUseInjectedDao() {
        List<Comment> expected = Collections.singletonList(new Comment());
        CommentDao commentDao = createDao(CommentDao.class, (proxy, method, args) -> {
            if ("findByArticleId".equals(method.getName())) {
                return expected;
            }
            return defaultValue(method.getReturnType());
        });

        List<Comment> actual = new CommentServiceImpl(commentDao).findByArticleId(7);

        assertSame(expected, actual);
    }

    @Test
    public void userServiceShouldUseInjectedDao() {
        User expected = new User();
        UserDao userDao = createDao(UserDao.class, (proxy, method, args) -> {
            if ("findById".equals(method.getName())) {
                return expected;
            }
            return defaultValue(method.getReturnType());
        });

        User actual = new UserServiceImpl(userDao).findById(7);

        assertSame(expected, actual);
    }

    @Test
    public void noArgConstructorsShouldRemainAvailableForServlets() {
        assertNotNull(new ArticleServiceImpl());
        assertNotNull(new CategoryServiceImpl());
        assertNotNull(new CommentServiceImpl());
        assertNotNull(new UserServiceImpl());
    }

    @Test
    public void articleServiceShouldRejectNullDaoImmediately() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new ArticleServiceImpl(null)
        );

        assertTrue(exception.getMessage().contains("articleDao"));
    }

    @Test
    public void categoryServiceShouldRejectNullDaoImmediately() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new CategoryServiceImpl(null)
        );

        assertTrue(exception.getMessage().contains("categoryDao"));
    }

    @Test
    public void commentServiceShouldRejectNullDaoImmediately() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new CommentServiceImpl(null)
        );

        assertTrue(exception.getMessage().contains("commentDao"));
    }

    @Test
    public void userServiceShouldRejectNullDaoImmediately() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new UserServiceImpl(null)
        );

        assertTrue(exception.getMessage().contains("userDao"));
    }

    @Test
    public void readmeShouldDocumentServiceConstructorInjection() throws Exception {
        String readme = Files.readString(Paths.get("README.md"), StandardCharsets.UTF_8);

        assertTrue(readme.contains("Service 构造器依赖注入"));
    }

    @SuppressWarnings("unchecked")
    private <T> T createDao(Class<T> daoType, InvocationHandler invocationHandler) {
        return (T) Proxy.newProxyInstance(
                daoType.getClassLoader(),
                new Class<?>[]{daoType},
                invocationHandler
        );
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
