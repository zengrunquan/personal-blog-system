package com.blog.filter;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertTrue;

public class SecurityFilterMappingTest {

    private static final Path WEB_XML_PATH = Paths.get("src/main/webapp/WEB-INF/web.xml");

    @Test
    public void authFilterShouldProtectArticleUploadEndpoints() throws Exception {
        Set<String> patterns = findUrlPatterns("AuthFilter");

        assertTrue("AuthFilter 必须保护封面上传接口", patterns.contains("/article/uploadCover"));
        assertTrue("AuthFilter 必须保护附件上传接口", patterns.contains("/article/uploadFile"));
        assertTrue("AuthFilter 必须保护批量上传接口", patterns.contains("/article/batchUpload"));
    }

    @Test
    public void adminFilterShouldProtectArticleExportEndpoint() throws Exception {
        Set<String> patterns = findUrlPatterns("AdminFilter");

        assertTrue("AdminFilter 必须保护文章导出接口", patterns.contains("/article/export"));
    }

    private Set<String> findUrlPatterns(String targetFilterName) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        Document document = factory.newDocumentBuilder().parse(WEB_XML_PATH.toFile());
        NodeList mappings = document.getElementsByTagName("filter-mapping");
        Set<String> patterns = new HashSet<>();

        for (int i = 0; i < mappings.getLength(); i++) {
            Element mapping = (Element) mappings.item(i);
            String filterName = textOf(mapping, "filter-name");
            if (targetFilterName.equals(filterName)) {
                patterns.add(textOf(mapping, "url-pattern"));
            }
        }

        return patterns;
    }

    private String textOf(Element element, String tagName) {
        NodeList nodes = element.getElementsByTagName(tagName);
        if (nodes.getLength() == 0) {
            return "";
        }
        return nodes.item(0).getTextContent().trim();
    }
}
