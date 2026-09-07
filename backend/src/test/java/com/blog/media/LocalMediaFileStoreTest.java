package com.blog.media;

import com.blog.media.model.MediaAsset;
import com.blog.media.model.MediaType;
import com.blog.media.model.StoredMedia;
import com.blog.media.storage.LocalMediaFileStore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.servlet.http.Part;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LocalMediaFileStoreTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void shouldWriteToTypeDirectoryAndMoveAwayTemporaryFile() throws Exception {
        Path root = temporaryFolder.newFolder("uploads").toPath();
        byte[] content = "image-content".getBytes(StandardCharsets.UTF_8);
        LocalMediaFileStore store = new LocalMediaFileStore(root);

        StoredMedia stored = store.store(writablePart("正文.png", "image/png", content),
                MediaType.ARTICLE_IMAGE);

        assertTrue(stored.getStorageName().startsWith("image_"));
        assertTrue(Files.isRegularFile(root.resolve("image").resolve(stored.getStorageName())));
        assertArrayEquals(content, Files.readAllBytes(stored.getPath()));
        try (Stream<Path> files = Files.list(root.resolve("image"))) {
            assertTrue(files.noneMatch(path -> path.getFileName().toString().endsWith(".uploading")));
        }
    }

    @Test
    public void shouldRemoveTemporaryFileWhenPartWriteFails() throws Exception {
        Path root = temporaryFolder.newFolder("uploads").toPath();
        Part part = mock(Part.class);
        when(part.getSubmittedFileName()).thenReturn("broken.png");
        when(part.getContentType()).thenReturn("image/png");
        when(part.getSize()).thenReturn(7L);
        doThrow(new IOException("simulated write failure")).when(part).write(anyString());
        LocalMediaFileStore store = new LocalMediaFileStore(root);

        assertThrows(IOException.class, () -> store.store(part, MediaType.ARTICLE_IMAGE));

        Path imageDirectory = root.resolve("image");
        assertTrue(Files.isDirectory(imageDirectory));
        try (Stream<Path> files = Files.list(imageDirectory)) {
            assertFalse(files.findAny().isPresent());
        }
    }

    @Test
    public void shouldResolveOnlyTypeDerivedSafeStoragePathAndDeleteIdempotently() throws Exception {
        Path root = temporaryFolder.newFolder("uploads").toPath();
        LocalMediaFileStore store = new LocalMediaFileStore(root);
        byte[] content = "attachment".getBytes(StandardCharsets.UTF_8);
        StoredMedia stored = store.store(writablePart("file.pdf", "application/pdf", content),
                MediaType.ATTACHMENT);
        MediaAsset asset = MediaAsset.from(stored);

        assertTrue(store.resolve(asset).equals(stored.getPath()));
        assertTrue(store.deleteIfExists(asset));
        assertFalse(store.deleteIfExists(asset));

        MediaAsset traversal = new MediaAsset();
        traversal.setMediaType(MediaType.ATTACHMENT);
        traversal.setStorageName("../outside.pdf");
        assertThrows(IllegalArgumentException.class, () -> store.resolve(traversal));
    }

    private Part writablePart(String originalName, String contentType, byte[] content) throws Exception {
        Part part = mock(Part.class);
        when(part.getSubmittedFileName()).thenReturn(originalName);
        when(part.getContentType()).thenReturn(contentType);
        when(part.getSize()).thenReturn((long) content.length);
        doAnswer(invocation -> {
            Files.write(Path.of(invocation.getArgument(0, String.class)), content);
            return null;
        }).when(part).write(anyString());
        return part;
    }
}
