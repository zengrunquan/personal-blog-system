package com.blog.media;

import com.blog.media.dao.MediaDao;
import com.blog.media.model.ManagedMediaKey;
import com.blog.media.model.MediaAsset;
import com.blog.media.model.MediaReferenceType;
import com.blog.media.model.MediaStatus;
import com.blog.media.model.MediaType;
import com.blog.media.service.MediaReferenceServiceImpl;
import org.junit.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class MediaReferenceServiceImplTest {

    @Test
    public void shouldSynchronizeAvatarUsedInArticleContent() throws Exception {
        MediaDao mediaDao = mock(MediaDao.class);
        Connection connection = mock(Connection.class);
        MediaAsset avatar = new MediaAsset();
        avatar.setId(42L);
        avatar.setMediaType(MediaType.AVATAR);
        avatar.setUrlFileName("avatar-a.png");
        avatar.setStatus(MediaStatus.ACTIVE);
        when(mediaDao.findByKeysForUpdate(
                eq(connection),
                eq(Collections.singleton(new ManagedMediaKey(MediaType.AVATAR, "avatar-a.png")))
        )).thenReturn(Collections.singletonList(avatar));

        new MediaReferenceServiceImpl(mediaDao).syncArticleReferences(
                connection,
                9,
                "<img src=\"/uploads/avatars/avatar-a.png\">",
                null
        );

        verify(mediaDao).replaceArticleReferences(
                eq(connection),
                eq(9),
                eq(Collections.singletonMap(
                        42L,
                        Collections.singleton(MediaReferenceType.ARTICLE_CONTENT)
                ))
        );
    }

    @Test
    public void shouldRejectAvatarAsArticleCoverBeforeChangingReferences() {
        MediaDao mediaDao = mock(MediaDao.class);
        Connection connection = mock(Connection.class);

        assertThrows(SQLException.class, () -> new MediaReferenceServiceImpl(mediaDao)
                .syncArticleReferences(
                        connection,
                        9,
                        "<p>正文</p>",
                        "/uploads/avatars/avatar-a.png"
                ));

        verifyNoInteractions(mediaDao);
    }
}
