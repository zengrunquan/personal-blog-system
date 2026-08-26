package com.blog.api;

import com.blog.api.support.HtmlContentSanitizer;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HtmlContentSanitizerTest {

    @Test
    public void shouldKeepEditorFormattingAndRemoveExecutableMarkup() {
        String source = "<h2>章节</h2><blockquote>引用</blockquote>"
                + "<pre><code>const safe = true;</code></pre>"
                + "<a href=\"javascript:alert(1)\">危险</a>"
                + "<img src=\"/uploads/images/a.png\" onerror=\"alert(1)\">"
                + "<script>alert('xss')</script>";

        String cleaned = HtmlContentSanitizer.sanitize(source);

        assertTrue(cleaned.contains("<h2>章节</h2>"));
        assertTrue(cleaned.contains("<blockquote>引用</blockquote>"));
        assertTrue(cleaned.contains("<code>"));
        assertTrue(cleaned.contains("const safe"));
        assertTrue(cleaned.contains("/uploads/images/a.png"));
        assertFalse(cleaned.contains("javascript:"));
        assertFalse(cleaned.contains("onerror"));
        assertFalse(cleaned.contains("<script"));
    }

    @Test
    public void shouldHandleNullAsEmptyContent() {
        assertTrue(HtmlContentSanitizer.sanitize(null).isEmpty());
    }
}
