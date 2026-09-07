package com.blog.media.service;

import java.sql.Connection;
import java.sql.SQLException;

public interface MediaReferenceService {

    void syncArticleReferences(
            Connection connection,
            int articleId,
            String sanitizedContent,
            String coverImage
    ) throws SQLException;
}
