package com.blog.api;

import com.blog.web.FrontendRoutePolicy;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FrontendRoutePolicyTest {

    @Test
    public void shouldForwardExtensionlessFrontendRoutesAndExcludeBackendResources() {
        assertTrue(FrontendRoutePolicy.isSpaRoute("/"));
        assertTrue(FrontendRoutePolicy.isSpaRoute("/articles/18"));
        assertTrue(FrontendRoutePolicy.isSpaRoute("/editor/new"));
        assertTrue(FrontendRoutePolicy.isSpaRoute("/admin/categories"));
        assertTrue(FrontendRoutePolicy.isSpaRoute("/unknown"));
        assertTrue(FrontendRoutePolicy.isSpaRoute("/apiary"));
        assertTrue(FrontendRoutePolicy.isSpaRoute("/articles/not-a-number"));
        assertFalse(FrontendRoutePolicy.isSpaRoute("/api/articles"));
        assertFalse(FrontendRoutePolicy.isSpaRoute("/uploads/images/a.webp"));
        assertFalse(FrontendRoutePolicy.isSpaRoute("/assets/index.js"));
        assertFalse(FrontendRoutePolicy.isSpaRoute("/api/files/a.pdf/download"));
        assertFalse(FrontendRoutePolicy.isSpaRoute("/article/download"));
        assertFalse(FrontendRoutePolicy.isSpaRoute("/article/export"));
    }

    @Test
    public void shouldRecognizeLegacyEntryPoints() {
        assertTrue(FrontendRoutePolicy.isLegacyRoute("/login.jsp"));
        assertTrue(FrontendRoutePolicy.isLegacyRoute("/article/detail"));
        assertTrue(FrontendRoutePolicy.isLegacyRoute("/user/articles"));
        assertTrue(FrontendRoutePolicy.isLegacyRoute("/admin/dashboard"));
        assertTrue(FrontendRoutePolicy.isLegacyRoute("/admin/category/add"));
        assertTrue(FrontendRoutePolicy.isLegacyRoute("/admin/category/edit"));
        assertTrue(FrontendRoutePolicy.isLegacyRoute("/article/download"));
        assertTrue(FrontendRoutePolicy.isLegacyRoute("/article/export"));
        assertFalse(FrontendRoutePolicy.isLegacyRoute("/admin/users"));
        assertFalse(FrontendRoutePolicy.isLegacyRoute("/admin/articles"));
        assertFalse(FrontendRoutePolicy.isLegacyRoute("/admin/categories"));
        assertFalse(FrontendRoutePolicy.isLegacyRoute("/api/admin/dashboard"));
    }

    @Test
    public void shouldRejectLegacyGetThatUsedToMutateSession() {
        assertTrue(FrontendRoutePolicy.isLegacyUnsafeGet("/user/logout"));
        assertFalse(FrontendRoutePolicy.isLegacyUnsafeGet("/user/profile"));
    }

    @Test
    public void shouldBlockEveryLegacyWriteNamespace() {
        assertTrue(FrontendRoutePolicy.isLegacyWriteRoute("/article/delete"));
        assertTrue(FrontendRoutePolicy.isLegacyWriteRoute("/comment/add"));
        assertTrue(FrontendRoutePolicy.isLegacyWriteRoute("/user/editProfile"));
        assertTrue(FrontendRoutePolicy.isLegacyWriteRoute("/admin/categories"));
        assertTrue(FrontendRoutePolicy.isLegacyWriteRoute("/login.jsp"));
        assertFalse(FrontendRoutePolicy.isLegacyWriteRoute("/api/articles/1"));
        assertFalse(FrontendRoutePolicy.isLegacyWriteRoute("/uploads/images/a.webp"));
    }

    @Test
    public void shouldRecognizeTheEntireLegacyAttachmentNamespace() {
        assertTrue(FrontendRoutePolicy.isLegacyPublicAttachmentNamespace("/uploads/files"));
        assertTrue(FrontendRoutePolicy.isLegacyPublicAttachmentNamespace("/uploads/files/payload.html"));
        assertTrue(FrontendRoutePolicy.isLegacyPublicAttachmentNamespace("/uploads/files;v=1/payload.html"));
        assertFalse(FrontendRoutePolicy.isLegacyPublicAttachmentNamespace("/uploads/files-backup/payload.html"));
    }
}
