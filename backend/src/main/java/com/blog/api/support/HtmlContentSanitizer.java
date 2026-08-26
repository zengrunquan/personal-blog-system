package com.blog.api.support;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

public final class HtmlContentSanitizer {

    private static final PolicyFactory POLICY = new HtmlPolicyBuilder()
            .allowElements(
                    "h1", "h2", "h3", "h4", "p", "br", "strong", "b", "em", "i", "s",
                    "ul", "ol", "li", "blockquote", "pre", "code", "a", "img", "hr"
            )
            .allowAttributes("href", "title", "target", "rel").onElements("a")
            .allowAttributes("src", "alt", "title", "width", "height").onElements("img")
            .allowUrlProtocols("http", "https", "mailto")
            .requireRelNofollowOnLinks()
            .toFactory();

    private HtmlContentSanitizer() {
    }

    public static String sanitize(String content) {
        return content == null ? "" : POLICY.sanitize(content);
    }
}
