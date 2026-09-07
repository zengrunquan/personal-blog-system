package com.blog.media.service;

import com.blog.api.upload.UploadStorage;
import com.blog.dao.UserDao;
import com.blog.dao.impl.UserDaoImpl;
import com.blog.media.dao.MediaDao;
import com.blog.media.dao.MediaDaoImpl;
import com.blog.media.model.MediaAsset;
import com.blog.media.model.MediaStatus;
import com.blog.media.model.MediaType;
import com.blog.media.model.StoredMedia;
import com.blog.media.storage.LocalMediaFileStore;
import com.blog.media.storage.MediaFileStore;
import com.blog.util.JdbcTransactionManager;
import com.blog.util.TransactionManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.http.Part;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/** 上传应用服务；物理文件和媒体元数据失败时由同一入口负责补偿。 */
public class MediaUploadService {

    private static final Logger LOGGER = LogManager.getLogger(MediaUploadService.class);
    private static final Duration TEMP_RETENTION = Duration.ofHours(24);

    private final MediaFileStore fileStore;
    private final MediaDao mediaDao;
    private final UserDao userDao;
    private final TransactionManager transactionManager;
    private final Clock clock;

    public MediaUploadService() {
        this(
                new LocalMediaFileStore(),
                new MediaDaoImpl(),
                new UserDaoImpl(),
                new JdbcTransactionManager(),
                Clock.systemUTC()
        );
    }

    public MediaUploadService(
            MediaFileStore fileStore,
            MediaDao mediaDao,
            UserDao userDao,
            TransactionManager transactionManager,
            Clock clock
    ) {
        this.fileStore = Objects.requireNonNull(fileStore, "fileStore 不能为空");
        this.mediaDao = Objects.requireNonNull(mediaDao, "mediaDao 不能为空");
        this.userDao = Objects.requireNonNull(userDao, "userDao 不能为空");
        this.transactionManager = Objects.requireNonNull(
                transactionManager, "transactionManager 不能为空");
        this.clock = Objects.requireNonNull(clock, "clock 不能为空");
    }

    /**
     * 保存文章图片或附件的元数据。上传接口完成后文件还没有业务引用，因此先进入 TEMP。
     */
    public UploadStorage.UploadResult upload(
            Part part,
            MediaType type,
            Integer userId,
            String contextPath
    ) throws IOException {
        if (type == null || type == MediaType.AVATAR) {
            throw new IllegalArgumentException("普通上传只支持文章图片或附件");
        }
        validateUserId(userId);
        StoredMedia stored = fileStore.store(part, type);
        MediaAsset asset = MediaAsset.from(stored);
        asset.setUploadedBy(userId);
        asset.setStatus(MediaStatus.TEMP);
        asset.setDeleteAfter(clock.instant().plus(TEMP_RETENTION));

        try {
            persistAsset(asset);
        } catch (RuntimeException error) {
            throw compensateAndWrap(asset, "媒体元数据写入失败", error);
        }
        return toUploadResult(stored, contextPath);
    }

    /**
     * 替换头像时先完成数据库事务，Servlet 只有在该方法成功后才更新 Session。
     */
    public UploadStorage.UploadResult replaceAvatar(
            Part part,
            Integer userId,
            String contextPath
    ) throws IOException {
        validateUserId(userId);
        StoredMedia stored = fileStore.store(part, MediaType.AVATAR);
        MediaAsset asset = MediaAsset.from(stored);
        asset.setUploadedBy(userId);
        asset.setStatus(MediaStatus.ACTIVE);
        asset.setDeleteAfter(null);
        String avatarUrl = MediaType.AVATAR.buildUrl(contextPath, stored.getUrlFileName());

        try {
            transactionManager.inTransaction(connection -> {
                long mediaId = mediaDao.insert(connection, asset);
                if (mediaId <= 0 || asset.getId() == null) {
                    throw new java.sql.SQLException("头像媒体资产未生成有效 ID");
                }
                if (!userDao.updateAvatar(connection, userId, avatarUrl)) {
                    throw new java.sql.SQLException("用户头像更新没有影响任何行");
                }
                mediaDao.replaceAvatarReference(connection, userId, mediaId);
                return mediaId;
            });
        } catch (RuntimeException error) {
            throw compensateAndWrap(asset, "头像元数据写入失败", error);
        }
        return new UploadStorage.UploadResult(
                stored.getStorageName(),
                stored.getOriginalName(),
                avatarUrl,
                stored.getContentType(),
                stored.getSizeBytes()
        );
    }

    private void persistAsset(MediaAsset asset) {
        transactionManager.inTransaction(connection -> {
            long mediaId = mediaDao.insert(connection, asset);
            if (mediaId <= 0 || asset.getId() == null) {
                throw new java.sql.SQLException("媒体资产未生成有效 ID");
            }
            return mediaId;
        });
    }

    private UploadStorage.UploadResult toUploadResult(StoredMedia stored, String contextPath) {
        return new UploadStorage.UploadResult(
                stored.getStorageName(),
                stored.getOriginalName(),
                stored.getMediaType().buildUrl(contextPath, stored.getUrlFileName()),
                stored.getContentType(),
                stored.getSizeBytes()
        );
    }

    private IOException compensateAndWrap(MediaAsset asset, String message, RuntimeException error) {
        IOException failure = new IOException(message, error);
        try {
            boolean deleted = fileStore.deleteIfExists(asset);
            if (!deleted) {
                LOGGER.warn(
                        "[MediaUploadService#compensateAndWrap] 补偿删除文件时文件已不存在，storageName={}",
                        asset.getStorageName()
                );
            }
        } catch (Exception compensationError) {
            LOGGER.error(
                    "[MediaUploadService#compensateAndWrap] 补偿删除上传文件失败，mediaType={}，storageName={}",
                    asset.getMediaType(),
                    asset.getStorageName(),
                    compensationError
            );
            failure.addSuppressed(compensationError);
        }
        LOGGER.error(
                "[MediaUploadService#compensateAndWrap] 上传事务失败，mediaType={}，storageName={}",
                asset.getMediaType(),
                asset.getStorageName(),
                error
        );
        return failure;
    }

    private void validateUserId(Integer userId) {
        if (userId == null || userId <= 0) throw new IllegalArgumentException("用户 ID 无效");
    }
}
