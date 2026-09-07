package com.blog.media.service;

import com.blog.media.dao.MediaDao;
import com.blog.media.dao.MediaDaoImpl;
import com.blog.media.model.ManagedMediaKey;
import com.blog.media.model.MediaAsset;
import com.blog.media.model.MediaReferenceType;
import com.blog.media.model.MediaType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** 将已清洗文章中的站内媒体 URL 同步为数据库引用，拒绝静默丢失资产。 */
public class MediaReferenceServiceImpl implements MediaReferenceService {

    private static final Logger LOGGER = LogManager.getLogger(MediaReferenceServiceImpl.class);
    private final MediaDao mediaDao;

    public MediaReferenceServiceImpl() {
        this(new MediaDaoImpl());
    }

    public MediaReferenceServiceImpl(MediaDao mediaDao) {
        this.mediaDao = java.util.Objects.requireNonNull(mediaDao, "mediaDao 不能为空");
    }

    @Override
    public void syncArticleReferences(
            Connection connection,
            int articleId,
            String sanitizedContent,
            String coverImage
    ) throws SQLException {
        validateCoverImage(coverImage, articleId);
        Map<ManagedMediaKey, Set<MediaReferenceType>> extracted =
                MediaReferenceExtractor.extract(sanitizedContent, coverImage);
        if (extracted.isEmpty()) {
            mediaDao.replaceArticleReferences(connection, articleId, java.util.Collections.emptyMap());
            return;
        }

        List<MediaAsset> assets = mediaDao.findByKeysForUpdate(connection, extracted.keySet());
        Map<ManagedMediaKey, MediaAsset> assetsByKey = new HashMap<>();
        for (MediaAsset asset : assets) {
            assetsByKey.put(new ManagedMediaKey(asset.getMediaType(), asset.getUrlFileName()), asset);
        }

        Map<Long, Set<MediaReferenceType>> referencesById = new HashMap<>();
        for (Map.Entry<ManagedMediaKey, Set<MediaReferenceType>> entry : extracted.entrySet()) {
            ManagedMediaKey key = entry.getKey();
            MediaAsset asset = assetsByKey.get(key);
            if (asset == null || asset.getId() == null) {
                throw new SQLException("文章引用的媒体资产不存在，articleId=" + articleId
                        + "，media=" + key);
            }
            Set<MediaReferenceType> allowedTypes = EnumSet.noneOf(MediaReferenceType.class);
            for (MediaReferenceType type : entry.getValue()) {
                if (type == MediaReferenceType.ARTICLE_COVER
                        && key.getMediaType() != MediaType.ARTICLE_IMAGE) {
                    throw new SQLException("文章封面必须是文章图片，articleId=" + articleId);
                }
                allowedTypes.add(type);
            }
            referencesById.computeIfAbsent(asset.getId(), ignored -> new HashSet<>())
                    .addAll(allowedTypes);
        }
        mediaDao.replaceArticleReferences(connection, articleId, referencesById);
        LOGGER.debug("[MediaReferenceServiceImpl#syncArticleReferences] 同步文章媒体引用完成，articleId={}，referenceCount={}",
                articleId, referencesById.size());
    }

    private void validateCoverImage(String coverImage, int articleId) throws SQLException {
        Optional<ManagedMediaKey> coverKey = ManagedMediaUrlParser.tryParse(coverImage);
        if (coverKey.isPresent()) {
            if (coverKey.get().getMediaType() != MediaType.ARTICLE_IMAGE) {
                throw new SQLException("文章封面必须是文章图片，articleId=" + articleId);
            }
            return;
        }
        if (ManagedMediaUrlParser.isPotentialManagedUrl(coverImage)) {
            throw new SQLException("文章封面媒体 URL 无法解析，articleId=" + articleId);
        }
    }
}
