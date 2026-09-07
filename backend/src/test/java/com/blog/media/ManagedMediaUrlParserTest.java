package com.blog.media;

import com.blog.media.model.ManagedMediaKey;
import com.blog.media.model.MediaType;
import com.blog.media.service.ManagedMediaUrlParser;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class ManagedMediaUrlParserTest {

    @Test
    public void shouldParsePublicAndPrivateMediaUrlsWithOptionalContextPath() {
        ManagedMediaUrlParser parser = new ManagedMediaUrlParser();

        assertEquals(
                new ManagedMediaKey(MediaType.AVATAR, "avatar-id.jpg"),
                parser.parse("/uploads/avatars/avatar-id.jpg")
        );
        assertEquals(
                new ManagedMediaKey(MediaType.AVATAR, "avatar-id.jpg"),
                parser.parse("/personal_blog_system_war_exploded/uploads/avatars/avatar-id.jpg")
        );
        assertEquals(
                new ManagedMediaKey(MediaType.AVATAR, "avatar-id.jpg"),
                parser.parse("/tenant/blog/uploads/avatars/avatar-id.jpg")
        );
        assertEquals(
                new ManagedMediaKey(MediaType.ARTICLE_IMAGE, "image-id.webp"),
                parser.parse("/uploads/images/image-id.webp")
        );
        assertEquals(
                new ManagedMediaKey(MediaType.ARTICLE_IMAGE, "image-id.webp"),
                parser.parse("/app/uploads/images/image-id.webp")
        );
        assertEquals(
                new ManagedMediaKey(MediaType.ATTACHMENT, "file-id.pdf"),
                parser.parse("/api/files/file-id.pdf/download")
        );
        assertEquals(
                new ManagedMediaKey(MediaType.ATTACHMENT, "file-id.pdf"),
                parser.parse("/app/api/files/file-id.pdf/download")
        );
    }

    @Test
    public void shouldRejectExternalUrlsQueriesFragmentsEncodedSeparatorsAndPhysicalNames() {
        ManagedMediaUrlParser parser = new ManagedMediaUrlParser();

        assertThrows(IllegalArgumentException.class,
                () -> parser.parse("https://example.com/uploads/images/image-id.png"));
        assertThrows(IllegalArgumentException.class,
                () -> parser.parse("//example.com/uploads/images/image-id.png"));
        assertThrows(IllegalArgumentException.class,
                () -> parser.parse("/uploads/images/image-id.png?download=1"));
        assertThrows(IllegalArgumentException.class,
                () -> parser.parse("/uploads/images/image-id.png#fragment"));
        assertThrows(IllegalArgumentException.class,
                () -> parser.parse("/uploads/images/%2e%2e/secret.png"));
        assertThrows(IllegalArgumentException.class,
                () -> parser.parse("/uploads/images/image-id.png%2fsecret"));
        assertThrows(IllegalArgumentException.class,
                () -> parser.parse("/uploads/images/image_image-id.png"));
        assertThrows(IllegalArgumentException.class,
                () -> parser.parse("/uploads/images/../image-id.png"));
    }

    @Test
    public void shouldMarkOnlyLocalManagedLookingPathsAsReviewCandidates() {
        assertTrue(ManagedMediaUrlParser.isPotentialManagedUrl(
                "/tenant/blog/uploads/images/bad%20name.png"));
        assertTrue(ManagedMediaUrlParser.isPotentialManagedUrl(
                "/tenant/blog/api/files/bad%20name.pdf/download"));
        assertFalse(ManagedMediaUrlParser.isPotentialManagedUrl(
                "https://example.com/uploads/images/image-id.png"));
        assertFalse(ManagedMediaUrlParser.isPotentialManagedUrl("#section"));
    }
}
