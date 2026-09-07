package com.blog.media;

import com.blog.api.upload.UploadStorage;
import com.blog.dao.UserDao;
import com.blog.media.dao.MediaDao;
import com.blog.media.model.MediaAsset;
import com.blog.media.model.MediaStatus;
import com.blog.media.model.MediaType;
import com.blog.media.model.StoredMedia;
import com.blog.media.service.MediaUploadService;
import com.blog.util.TransactionException;
import com.blog.util.TransactionManager;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import javax.servlet.http.Part;
import java.io.IOException;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MediaUploadServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-01T00:00:00Z");

    @Test
    public void articleUploadShouldPersistTempAssetWithRetention() throws Exception {
        MediaFileStoreStub files = new MediaFileStoreStub();
        MediaDao mediaDao = mock(MediaDao.class);
        UserDao userDao = mock(UserDao.class);
        Part part = mock(Part.class);
        StoredMedia stored = stored(MediaType.ARTICLE_IMAGE, "image-a.png", "image_image-a.png");
        files.stored = stored;
        when(mediaDao.insert(isNull(Connection.class), any(MediaAsset.class))).thenAnswer(invocation -> {
            MediaAsset asset = invocation.getArgument(1);
            asset.setId(11L);
            return 11L;
        });

        MediaUploadService service = new MediaUploadService(
                files,
                mediaDao,
                userDao,
                immediateTransactionManager(),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );

        UploadStorage.UploadResult result = service.upload(
                part, MediaType.ARTICLE_IMAGE, 7, "/blog"
        );

        assertEquals("image_image-a.png", result.storedName);
        assertEquals("/blog/uploads/images/image-a.png", result.url);
        ArgumentCaptor<MediaAsset> asset = ArgumentCaptor.forClass(MediaAsset.class);
        verify(mediaDao).insert(isNull(Connection.class), asset.capture());
        assertEquals(MediaStatus.TEMP, asset.getValue().getStatus());
        assertEquals(NOW.plusSeconds(24 * 60 * 60), asset.getValue().getDeleteAfter());
        assertEquals(Integer.valueOf(7), asset.getValue().getUploadedBy());
    }

    @Test
    public void metadataFailureShouldCompensatePhysicalFile() throws Exception {
        MediaFileStoreStub files = new MediaFileStoreStub();
        MediaDao mediaDao = mock(MediaDao.class);
        Part part = mock(Part.class);
        StoredMedia stored = stored(MediaType.ATTACHMENT, "report.pdf", "file_report.pdf");
        files.stored = stored;
        doThrow(new SQLException("media insert failed"))
                .when(mediaDao).insert(isNull(Connection.class), any(MediaAsset.class));

        MediaUploadService service = new MediaUploadService(
                files,
                mediaDao,
                mock(UserDao.class),
                immediateTransactionManager(),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );

        try {
            service.upload(part, MediaType.ATTACHMENT, 7, "");
        } catch (IOException error) {
            assertTrue(error.getMessage().contains("媒体元数据写入失败"));
            assertSame(stored, files.deleted);
            return;
        }
        throw new AssertionError("数据库失败时必须抛出 IOException");
    }

    @Test
    public void avatarReplacementShouldWriteUserAndReferenceInOneTransaction() throws Exception {
        MediaFileStoreStub files = new MediaFileStoreStub();
        MediaDao mediaDao = mock(MediaDao.class);
        UserDao userDao = mock(UserDao.class);
        Part part = mock(Part.class);
        StoredMedia stored = stored(MediaType.AVATAR, "avatar-a.png", "avatar_avatar-a.png");
        files.stored = stored;
        when(mediaDao.insert(isNull(Connection.class), any(MediaAsset.class))).thenAnswer(invocation -> {
            MediaAsset asset = invocation.getArgument(1);
            asset.setId(21L);
            return 21L;
        });
        when(userDao.updateAvatar(isNull(Connection.class), eq(3), eq("/uploads/avatars/avatar-a.png")))
                .thenReturn(true);

        MediaUploadService service = new MediaUploadService(
                files,
                mediaDao,
                userDao,
                immediateTransactionManager(),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );

        UploadStorage.UploadResult result = service.replaceAvatar(part, 3, "");

        assertEquals("/uploads/avatars/avatar-a.png", result.url);
        ArgumentCaptor<MediaAsset> asset = ArgumentCaptor.forClass(MediaAsset.class);
        verify(mediaDao).insert(isNull(Connection.class), asset.capture());
        assertEquals(MediaStatus.ACTIVE, asset.getValue().getStatus());
        assertNull(asset.getValue().getDeleteAfter());
        InOrder order = inOrder(mediaDao, userDao);
        order.verify(mediaDao).insert(isNull(Connection.class), any(MediaAsset.class));
        order.verify(userDao).updateAvatar(
                isNull(Connection.class), eq(3), eq("/uploads/avatars/avatar-a.png")
        );
        order.verify(mediaDao).replaceAvatarReference(isNull(Connection.class), eq(3), eq(21L));
    }

    @Test
    public void avatarDatabaseFailureShouldCompensateNewFile() throws Exception {
        MediaFileStoreStub files = new MediaFileStoreStub();
        MediaDao mediaDao = mock(MediaDao.class);
        Part part = mock(Part.class);
        StoredMedia stored = stored(MediaType.AVATAR, "avatar-b.png", "avatar_avatar-b.png");
        files.stored = stored;
        doThrow(new SQLException("avatar metadata failed"))
                .when(mediaDao).insert(isNull(Connection.class), any(MediaAsset.class));

        MediaUploadService service = new MediaUploadService(
                files,
                mediaDao,
                mock(UserDao.class),
                immediateTransactionManager(),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );

        try {
            service.replaceAvatar(part, 3, "");
        } catch (IOException error) {
            assertSame(stored, files.deleted);
            return;
        }
        throw new AssertionError("头像事务失败时必须抛出 IOException");
    }

    private StoredMedia stored(MediaType type, String urlName, String storageName) {
        return new StoredMedia(
                type,
                storageName,
                urlName,
                "original-name",
                "application/octet-stream",
                12L,
                "hash",
                Paths.get("target", storageName)
        );
    }

    private TransactionManager immediateTransactionManager() {
        return new TransactionManager() {
            @Override
            public <T> T inTransaction(com.blog.util.TransactionWork<T> work) {
                try {
                    return work.execute(null);
                } catch (Exception error) {
                    throw new TransactionException(error);
                }
            }
        };
    }

    private static final class MediaFileStoreStub implements com.blog.media.storage.MediaFileStore {
        private StoredMedia stored;
        private StoredMedia deleted;

        @Override
        public StoredMedia store(Part part, MediaType type) {
            return stored;
        }

        @Override
        public java.nio.file.Path resolve(MediaAsset asset) {
            return asset == null ? null : Paths.get("target", asset.getStorageName());
        }

        @Override
        public boolean deleteIfExists(MediaAsset asset) {
            deleted = stored;
            return true;
        }
    }
}
