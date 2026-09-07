package com.blog.media;

import com.blog.media.model.ManagedMediaKey;
import com.blog.media.model.MediaReferenceType;
import com.blog.media.model.MediaType;
import com.blog.media.service.MediaReferenceExtractor;
import org.junit.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MediaReferenceExtractorTest {

    @Test
    public void shouldClassifyAndDeduplicateImageAttachmentAndCoverReferences() {
        String sanitizedHtml = "<p><img src=\"/uploads/images/image-a.png\"></p>"
                + "<p><img src=\"/app/uploads/images/image-a.png\"></p>"
                + "<p><a href=\"/app/api/files/file-a.pdf/download\">附件</a></p>";

        Map<ManagedMediaKey, Set<MediaReferenceType>> references =
                MediaReferenceExtractor.extract(sanitizedHtml, "/uploads/images/cover.webp");

        assertEquals(3, references.size());
        assertEquals(
                Set.of(MediaReferenceType.ARTICLE_CONTENT),
                references.get(new ManagedMediaKey(MediaType.ARTICLE_IMAGE, "image-a.png"))
        );
        assertEquals(
                Set.of(MediaReferenceType.ARTICLE_CONTENT),
                references.get(new ManagedMediaKey(MediaType.ATTACHMENT, "file-a.pdf"))
        );
        assertEquals(
                Set.of(MediaReferenceType.ARTICLE_COVER),
                references.get(new ManagedMediaKey(MediaType.ARTICLE_IMAGE, "cover.webp"))
        );
    }

    @Test
    public void shouldIgnoreExternalLinksAnchorsAndInvalidManagedUrls() {
        String sanitizedHtml = "<p><a href=\"https://example.com/a.pdf\">外链</a>"
                + "<a href=\"#section\">锚点</a>"
                + "<img src=\"https://example.com/image.png\">"
                + "<img src=\"/uploads/images/image-a.png?cache=1\"></p>";

        Map<ManagedMediaKey, Set<MediaReferenceType>> references =
                MediaReferenceExtractor.extract(sanitizedHtml, "https://example.com/cover.png");

        assertEquals(0, references.size());
        assertTrue(references.isEmpty());
    }

    @Test
    public void shouldReportInvalidLocalManagedUrlsForHistoricalReview() {
        String html = "<img src=\"/tenant/blog/uploads/images/bad%20name.png\">"
                + "<a href=\"https://example.com/ok.pdf\">外链</a>";

        assertEquals(
                2,
                MediaReferenceExtractor.countManualReviewCandidates(
                        html,
                        "/tenant/blog/uploads/avatars/avatar-id.jpg"
                )
        );
    }
}
