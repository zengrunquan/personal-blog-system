-- 媒体元数据与业务引用独立于上传二进制，支持事务补偿和延迟回收。
CREATE TABLE `media_asset` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `media_type` VARCHAR(32) NOT NULL,
    `storage_name` VARCHAR(255) NOT NULL,
    `url_file_name` VARCHAR(255) NOT NULL,
    `original_name` VARCHAR(255) NOT NULL,
    `content_type` VARCHAR(127) DEFAULT NULL,
    `size_bytes` BIGINT DEFAULT NULL,
    `sha256` CHAR(64) DEFAULT NULL,
    `uploaded_by` INT DEFAULT NULL,
    `status` VARCHAR(32) NOT NULL,
    `delete_after` DATETIME DEFAULT NULL,
    `claim_token` CHAR(36) DEFAULT NULL,
    `claimed_at` DATETIME DEFAULT NULL,
    `delete_attempts` INT NOT NULL DEFAULT 0,
    `last_error` VARCHAR(1000) DEFAULT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uq_media_storage_name` (`storage_name`),
    UNIQUE KEY `uq_media_url_key` (`media_type`, `url_file_name`),
    KEY `idx_media_cleanup` (`status`, `delete_after`),
    CONSTRAINT `fk_media_uploader`
        FOREIGN KEY (`uploaded_by`) REFERENCES `user` (`id`) ON DELETE SET NULL,
    CONSTRAINT `chk_media_type`
        CHECK (`media_type` IN ('AVATAR', 'ARTICLE_IMAGE', 'ATTACHMENT')),
    CONSTRAINT `chk_media_status`
        CHECK (`status` IN (
            'TEMP', 'ACTIVE', 'DELETE_PENDING', 'DELETING',
            'DELETE_FAILED', 'DELETED', 'LEGACY_PROTECTED', 'MISSING_BINARY'
        ))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `media_reference` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `media_id` BIGINT NOT NULL,
    `reference_type` VARCHAR(32) NOT NULL,
    `user_id` INT DEFAULT NULL,
    `article_id` INT DEFAULT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uq_avatar_reference` (`reference_type`, `user_id`),
    UNIQUE KEY `uq_article_media_reference`
        (`media_id`, `reference_type`, `article_id`),
    KEY `idx_media_reference_media` (`media_id`),
    KEY `idx_media_reference_article` (`article_id`),
    CONSTRAINT `fk_media_reference_asset`
        FOREIGN KEY (`media_id`) REFERENCES `media_asset` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_media_reference_user`
        FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_media_reference_article`
        FOREIGN KEY (`article_id`) REFERENCES `article` (`id`) ON DELETE CASCADE,
    CONSTRAINT `chk_media_reference_type`
        CHECK (
            (`reference_type` = 'USER_AVATAR'
                AND `user_id` IS NOT NULL AND `article_id` IS NULL)
            OR
            (`reference_type` IN ('ARTICLE_CONTENT', 'ARTICLE_COVER')
                AND `article_id` IS NOT NULL AND `user_id` IS NULL)
        )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
