package com.blog.media;

import com.blog.media.dao.MediaDao;
import com.blog.media.model.MediaType;
import com.blog.media.service.AttachmentDownloadNameService;
import com.blog.util.TransactionException;
import com.blog.util.TransactionManager;
import com.blog.util.TransactionWork;
import org.junit.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AttachmentDownloadNameServiceTest {

    @Test
    public void resolveDownloadNameShouldUsePersistedOriginalName() throws Exception {
        MediaDao mediaDao = mock(MediaDao.class);
        when(mediaDao.findOriginalName(
                isNull(Connection.class),
                eq(MediaType.ATTACHMENT),
                eq("12345678.pdf")
        )).thenReturn(Optional.of("课程笔记.pdf"));

        AttachmentDownloadNameService service = new AttachmentDownloadNameService(
                mediaDao,
                immediateTransactionManager()
        );

        assertEquals("课程笔记.pdf", service.resolveDownloadName("12345678.pdf"));
        verify(mediaDao).findOriginalName(
                isNull(Connection.class),
                eq(MediaType.ATTACHMENT),
                eq("12345678.pdf")
        );
    }

    @Test
    public void resolveDownloadNameShouldFallbackToUrlNameWhenMetadataMissing() throws Exception {
        MediaDao mediaDao = mock(MediaDao.class);
        when(mediaDao.findOriginalName(
                isNull(Connection.class),
                eq(MediaType.ATTACHMENT),
                eq("12345678.pdf")
        )).thenReturn(Optional.empty());

        AttachmentDownloadNameService service = new AttachmentDownloadNameService(
                mediaDao,
                immediateTransactionManager()
        );

        assertEquals("12345678.pdf", service.resolveDownloadName("12345678.pdf"));
    }

    @Test
    public void resolveDownloadNameShouldFallbackForBlankOrUnsafeMetadata() throws Exception {
        MediaDao mediaDao = mock(MediaDao.class);
        when(mediaDao.findOriginalName(
                isNull(Connection.class),
                eq(MediaType.ATTACHMENT),
                eq("12345678.pdf")
        )).thenReturn(Optional.of("C:\\fakepath\\   "));

        AttachmentDownloadNameService service = new AttachmentDownloadNameService(
                mediaDao,
                immediateTransactionManager()
        );

        assertEquals("12345678.pdf", service.resolveDownloadName("12345678.pdf"));
    }

    @Test
    public void resolveDownloadNameShouldPreserveUuidAndLegalFilePrefixInOriginalName() throws Exception {
        MediaDao mediaDao = mock(MediaDao.class);
        when(mediaDao.findOriginalName(
                isNull(Connection.class),
                eq(MediaType.ATTACHMENT),
                eq("12345678.pdf")
        )).thenReturn(Optional.of("file_学习资料.pdf"));

        AttachmentDownloadNameService service = new AttachmentDownloadNameService(
                mediaDao,
                immediateTransactionManager()
        );

        assertEquals("file_学习资料.pdf", service.resolveDownloadName("12345678.pdf"));
    }

    @Test
    public void resolveDownloadNameShouldPropagateDatabaseFailure() throws Exception {
        MediaDao mediaDao = mock(MediaDao.class);
        when(mediaDao.findOriginalName(
                isNull(Connection.class),
                eq(MediaType.ATTACHMENT),
                eq("12345678.pdf")
        )).thenThrow(new SQLException("media query failed"));

        AttachmentDownloadNameService service = new AttachmentDownloadNameService(
                mediaDao,
                immediateTransactionManager()
        );

        TransactionException error = assertThrows(
                TransactionException.class,
                () -> service.resolveDownloadName("12345678.pdf")
        );
        assertEquals("media query failed", error.getCause().getMessage());
    }

    private TransactionManager immediateTransactionManager() {
        return new TransactionManager() {
            @Override
            public <T> T inTransaction(TransactionWork<T> work) {
                try {
                    return work.execute(null);
                } catch (Exception error) {
                    throw new TransactionException(error);
                }
            }
        };
    }
}
