package com.blog.api;

import com.blog.api.upload.UploadPolicy;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UploadPolicyTest {

    @Test
    public void shouldAcceptAllowedImageMimeWithinFiveMegabytes() {
        UploadPolicy.validateImage("image/webp", 5L * 1024 * 1024);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectSvgEvenThoughItIsAnImageMime() {
        UploadPolicy.validateImage("image/svg+xml", 1024);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectImageLargerThanFiveMegabytes() {
        UploadPolicy.validateImage("image/jpeg", 5L * 1024 * 1024 + 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectAttachmentLargerThanTenMegabytes() {
        UploadPolicy.validateAttachment(10L * 1024 * 1024 + 1);
    }

    @Test
    public void shouldStripPathFromSubmittedFileName() {
        assertEquals("notes.pdf", UploadPolicy.safeOriginalName("C:\\fakepath\\notes.pdf"));
        assertEquals("notes.pdf", UploadPolicy.safeOriginalName("../../notes.pdf"));
    }
}
