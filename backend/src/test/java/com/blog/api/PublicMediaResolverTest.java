package com.blog.api;

import com.blog.api.upload.PublicMediaResolver;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class PublicMediaResolverTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void shouldResolvePublicAvatarAndArticleImageFromImageDirectory() {
        Path root = temporaryFolder.getRoot().toPath();

        assertEquals(
                root.resolve("image/avatar_123.jpg").toAbsolutePath().normalize(),
                PublicMediaResolver.resolve(root, "/avatars/123.jpg")
        );
        assertEquals(
                root.resolve("image/image_456.webp").toAbsolutePath().normalize(),
                PublicMediaResolver.resolve(root, "/images/456.webp")
        );
    }

    @Test
    public void shouldRejectPrivateAttachmentThroughPublicMediaRoute() {
        Path root = temporaryFolder.getRoot().toPath();

        assertThrows(
                IllegalArgumentException.class,
                () -> PublicMediaResolver.resolve(root, "/images/file_456.pdf")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> PublicMediaResolver.resolve(root, "/files/file_456.pdf")
        );
    }

    @Test
    public void shouldRejectCategoryMismatchAndPathTraversal() {
        Path root = temporaryFolder.getRoot().toPath();

        assertThrows(
                IllegalArgumentException.class,
                () -> PublicMediaResolver.resolve(root, "/avatars/image_456.webp")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> PublicMediaResolver.resolve(root, "/images/../file_456.pdf")
        );
    }
}
