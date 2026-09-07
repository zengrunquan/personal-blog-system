package com.blog.service;

import com.blog.dao.ArticleDao;
import com.blog.entity.Article;
import com.blog.media.service.MediaReferenceService;
import com.blog.service.impl.ArticleServiceImpl;
import com.blog.util.TransactionException;
import com.blog.util.TransactionManager;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ArticleMediaLifecycleTest {

    @Test
    public void publishShouldSyncReferencesFromSanitizedContentInsideTransaction() throws Exception {
        ArticleDao articleDao = mock(ArticleDao.class);
        MediaReferenceService mediaReferences = mock(MediaReferenceService.class);
        Article article = validArticle();
        when(articleDao.insert(isNull(Connection.class), same(article))).thenAnswer(invocation -> {
            article.setId(7);
            return true;
        });
        TransactionManager transactionManager = immediateTransactionManager();

        String result = new ArticleServiceImpl(articleDao, mediaReferences, transactionManager)
                .publish(article);

        assertNull(result);
        ArgumentCaptor<String> content = ArgumentCaptor.forClass(String.class);
        verify(mediaReferences).syncArticleReferences(
                isNull(Connection.class), eq(7), content.capture(), isNull(String.class)
        );
        assertEquals("<p>正文</p><img src=\"/uploads/images/image-a.png\" />", content.getValue());
    }

    @Test
    public void publishShouldReturnFailureWhenReferenceSyncFails() throws Exception {
        ArticleDao articleDao = mock(ArticleDao.class);
        MediaReferenceService mediaReferences = mock(MediaReferenceService.class);
        Article article = validArticle();
        when(articleDao.insert(isNull(Connection.class), same(article))).thenAnswer(invocation -> {
            article.setId(8);
            return true;
        });
        doThrow(new SQLException("reference write failed")).when(mediaReferences).syncArticleReferences(
                isNull(Connection.class), eq(8), any(String.class), isNull(String.class)
        );

        String result = new ArticleServiceImpl(
                articleDao, mediaReferences, immediateTransactionManager()
        ).publish(article);

        assertEquals("发布文章失败", result);
    }

    @Test
    public void updateShouldSynchronizeReferencesAfterSanitizingContent() throws Exception {
        ArticleDao articleDao = mock(ArticleDao.class);
        MediaReferenceService mediaReferences = mock(MediaReferenceService.class);
        Article article = validArticle();
        article.setId(9);
        when(articleDao.update(isNull(Connection.class), same(article))).thenReturn(true);

        String result = new ArticleServiceImpl(
                articleDao, mediaReferences, immediateTransactionManager()
        ).update(article);

        assertNull(result);
        verify(mediaReferences).syncArticleReferences(
                isNull(Connection.class), eq(9), eq("<p>正文</p><img src=\"/uploads/images/image-a.png\" />"),
                isNull(String.class)
        );
    }

    private Article validArticle() {
        Article article = new Article();
        article.setTitle("标题");
        article.setContent("<p>正文</p><img src=\"/uploads/images/image-a.png\" onerror=\"alert(1)\">");
        article.setCategoryId(1);
        article.setUserId(2);
        article.setStatus(1);
        return article;
    }

    private TransactionManager immediateTransactionManager() {
        return new TransactionManager() {
            @Override
            public <T> T inTransaction(com.blog.util.TransactionWork<T> work) {
                try {
                    return work.execute(null);
                } catch (Exception e) {
                    throw new TransactionException(e);
                }
            }
        };
    }
}
