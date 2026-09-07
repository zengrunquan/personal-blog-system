package com.blog.media.service;

import com.blog.media.model.ManagedMediaKey;
import com.blog.media.model.MediaReferenceType;
import com.blog.media.model.MediaType;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit.ParserCallback;
import javax.swing.text.html.parser.ParserDelegator;
import java.io.IOException;
import java.io.StringReader;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** 使用容错 HTML 解析器提取媒体引用，避免正则在嵌套或转义 HTML 上误判。 */
public final class MediaReferenceExtractor {

    private MediaReferenceExtractor() {
    }

    public static Map<ManagedMediaKey, Set<MediaReferenceType>> extract(
            String sanitizedHtml,
            String coverImage
    ) {
        Map<ManagedMediaKey, Set<MediaReferenceType>> references = new LinkedHashMap<>();
        try {
            new ParserDelegator().parse(
                    new StringReader(sanitizedHtml == null ? "" : sanitizedHtml),
                    new ParserCallback() {
                        @Override
                        public void handleStartTag(
                                HTML.Tag tag,
                                MutableAttributeSet attributes,
                                int position
                        ) {
                            if (HTML.Tag.A.equals(tag)) {
                                add(attributes, HTML.Attribute.HREF, MediaReferenceType.ARTICLE_CONTENT);
                            } else if (HTML.Tag.IMG.equals(tag)) {
                                add(attributes, HTML.Attribute.SRC, MediaReferenceType.ARTICLE_CONTENT);
                            }
                        }

                        @Override
                        public void handleSimpleTag(
                                HTML.Tag tag,
                                MutableAttributeSet attributes,
                                int position
                        ) {
                            if (HTML.Tag.IMG.equals(tag)) {
                                add(attributes, HTML.Attribute.SRC, MediaReferenceType.ARTICLE_CONTENT);
                            }
                        }

                        private void add(
                                MutableAttributeSet attributes,
                                HTML.Attribute attribute,
                                MediaReferenceType referenceType
                        ) {
                            Object value = attributes.getAttribute(attribute);
                            if (value == null) return;
                            ManagedMediaUrlParser.tryParse(value.toString()).ifPresent(key ->
                                    references.computeIfAbsent(key, ignored ->
                                            EnumSet.noneOf(MediaReferenceType.class)).add(referenceType)
                            );
                        }
                    },
                    true
            );
        } catch (IOException e) {
            // StringReader 不会产生真实 I/O；保留明确异常以便未来替换输入源时不静默丢引用。
            throw new IllegalArgumentException("解析文章媒体引用失败", e);
        }

        ManagedMediaUrlParser.tryParse(coverImage).ifPresent(key -> {
            if (key.getMediaType() != MediaType.ARTICLE_IMAGE) {
                return;
            }
            references.computeIfAbsent(key, ignored -> EnumSet.noneOf(MediaReferenceType.class))
                    .add(MediaReferenceType.ARTICLE_COVER);
        });
        return references;
    }

    /**
     * 统计历史数据中疑似站内媒体、但无法安全解析的 URL，供回填报告人工复核。
     * 普通外链不属于本地媒体，不应因为无法解析而阻止回填。
     */
    public static int countManualReviewCandidates(String sanitizedHtml, String coverImage) {
        Set<String> candidates = new LinkedHashSet<>();
        try {
            new ParserDelegator().parse(
                    new StringReader(sanitizedHtml == null ? "" : sanitizedHtml),
                    new ParserCallback() {
                        @Override
                        public void handleStartTag(
                                HTML.Tag tag,
                                MutableAttributeSet attributes,
                                int position
                        ) {
                            inspect(tag, attributes);
                        }

                        @Override
                        public void handleSimpleTag(
                                HTML.Tag tag,
                                MutableAttributeSet attributes,
                                int position
                        ) {
                            inspect(tag, attributes);
                        }

                        private void inspect(HTML.Tag tag, MutableAttributeSet attributes) {
                            HTML.Attribute attribute;
                            if (HTML.Tag.A.equals(tag)) {
                                attribute = HTML.Attribute.HREF;
                            } else if (HTML.Tag.IMG.equals(tag)) {
                                attribute = HTML.Attribute.SRC;
                            } else {
                                return;
                            }
                            Object value = attributes.getAttribute(attribute);
                            if (value != null) addIfInvalid(candidates, value.toString());
                        }
                    },
                    true
            );
        } catch (IOException e) {
            // 与 extract 保持一致：解析器异常必须显式暴露，不能让回填静默漏项。
            throw new IllegalArgumentException("统计历史文章媒体复核项失败", e);
        }

        if (ManagedMediaUrlParser.isPotentialManagedUrl(coverImage)) {
            Optional<ManagedMediaKey> parsedCover = ManagedMediaUrlParser.tryParse(coverImage);
            if (!parsedCover.isPresent()
                    || parsedCover.get().getMediaType() != MediaType.ARTICLE_IMAGE) {
                candidates.add(coverImage);
            }
        }
        return candidates.size();
    }

    private static void addIfInvalid(Set<String> candidates, String value) {
        if (ManagedMediaUrlParser.isPotentialManagedUrl(value)
                && !ManagedMediaUrlParser.tryParse(value).isPresent()) {
            candidates.add(value);
        }
    }
}
