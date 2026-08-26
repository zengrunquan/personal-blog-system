package com.blog.api;

import com.blog.api.upload.UploadStorage;
import org.junit.Test;

import javax.servlet.ServletContext;
import java.nio.file.Path;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UploadStorageTest {

    @Test
    public void attachmentsShouldLiveUnderProtectedWebInfDirectory() {
        ServletContext context = mock(ServletContext.class);
        when(context.getRealPath("/WEB-INF/private-uploads/files"))
                .thenReturn("C:/app/WEB-INF/private-uploads/files");

        Path directory = UploadStorage.attachmentDirectory(context);
        String normalized = directory.toString().replace('\\', '/');

        assertTrue(normalized.endsWith("/WEB-INF/private-uploads/files"));
        assertFalse(normalized.contains("/uploads/files"));
    }
}
