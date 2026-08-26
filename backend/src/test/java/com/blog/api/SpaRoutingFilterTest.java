package com.blog.api;

import com.blog.web.SpaRoutingFilter;
import org.junit.Test;

import javax.servlet.FilterChain;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SpaRoutingFilterTest {

    @Test
    public void shouldRejectLegacyWritesBeforeTheyReachPageServlets() throws Exception {
        HttpServletRequest request = request("POST", "/app/article/delete");
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        StringWriter body = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(body));

        new SpaRoutingFilter().doFilter(request, response, chain);

        verify(response).setStatus(HttpServletResponse.SC_GONE);
        verify(chain, never()).doFilter(any(), any());
        assertTrue(body.toString().contains("LEGACY_ENDPOINT_DISABLED"));
    }

    @Test
    public void shouldRedirectLegacyPublicAttachmentsThroughProtectedDownloadApi() throws Exception {
        HttpServletRequest request = request(
                "GET",
                "/app/uploads/files/payload.html",
                "/uploads/files/payload.html"
        );
        HttpServletResponse response = mock(HttpServletResponse.class);

        new SpaRoutingFilter().doFilter(request, response, mock(FilterChain.class));

        verify(response).sendRedirect("/app/api/files/payload.html/download");
    }

    @Test
    public void shouldUseContainerDecodedPathForEncodedLegacyAttachments() throws Exception {
        HttpServletRequest request = request(
                "GET",
                "/app/uploads/files/payload%2ehtml",
                "/uploads/files/payload.html"
        );
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        new SpaRoutingFilter().doFilter(request, response, chain);

        verify(response).sendRedirect("/app/api/files/payload.html/download");
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    public void shouldUseCanonicalPathWhenLegacyAttachmentContainsMatrixParameters() throws Exception {
        HttpServletRequest request = request(
                "GET",
                "/app/uploads/files/payload.html;v=1",
                "/uploads/files/payload.html"
        );
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        new SpaRoutingFilter().doFilter(request, response, chain);

        verify(response).sendRedirect("/app/api/files/payload.html/download");
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    public void shouldNeverPassMalformedLegacyAttachmentsToStaticResourceHandling() throws Exception {
        HttpServletRequest request = request(
                "GET",
                "/app/uploads/files/%2e%2e/payload.html",
                "/uploads/files/../payload.html"
        );
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        new SpaRoutingFilter().doFilter(request, response, chain);

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND);
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    public void shouldForwardUnknownExtensionlessGetToVueRouter() throws Exception {
        HttpServletRequest request = request("GET", "/app/unknown");
        HttpServletResponse response = mock(HttpServletResponse.class);
        RequestDispatcher dispatcher = mock(RequestDispatcher.class);
        when(request.getRequestDispatcher("/index.html")).thenReturn(dispatcher);

        new SpaRoutingFilter().doFilter(request, response, mock(FilterChain.class));

        verify(dispatcher).forward(request, response);
    }

    @Test
    public void shouldRedirectRemovedCategoryFormsToVueCategories() throws Exception {
        HttpServletRequest request = request("GET", "/app/admin/category/edit");
        when(request.getParameter("id")).thenReturn("8");
        HttpServletResponse response = mock(HttpServletResponse.class);

        new SpaRoutingFilter().doFilter(request, response, mock(FilterChain.class));

        verify(response).setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
        verify(response).setHeader("Location", "/app/admin/categories?edit=8");
    }

    @Test
    public void shouldForwardCurrentAdminVueRoutesWithoutRedirectLoop() throws Exception {
        HttpServletRequest request = request("GET", "/app/admin/categories");
        HttpServletResponse response = mock(HttpServletResponse.class);
        RequestDispatcher dispatcher = mock(RequestDispatcher.class);
        when(request.getRequestDispatcher("/index.html")).thenReturn(dispatcher);

        new SpaRoutingFilter().doFilter(request, response, mock(FilterChain.class));

        verify(dispatcher).forward(request, response);
        verify(response, never()).setHeader(org.mockito.ArgumentMatchers.eq("Location"), any());
    }

    @Test
    public void shouldRedirectRemovedDownloadAndExportEndpointsToProtectedApis() throws Exception {
        HttpServletRequest download = request("GET", "/app/article/download");
        when(download.getParameter("file")).thenReturn("1710000000_42.pdf");
        HttpServletResponse downloadResponse = mock(HttpServletResponse.class);
        new SpaRoutingFilter().doFilter(download, downloadResponse, mock(FilterChain.class));
        verify(downloadResponse).setHeader(
                "Location",
                "/app/api/files/1710000000_42.pdf/download"
        );

        HttpServletRequest export = request("GET", "/app/article/export");
        HttpServletResponse exportResponse = mock(HttpServletResponse.class);
        new SpaRoutingFilter().doFilter(export, exportResponse, mock(FilterChain.class));
        verify(exportResponse).setHeader("Location", "/app/api/admin/articles/export");
    }

    @Test
    public void shouldRejectUnsafeLegacyDownloadFileName() throws Exception {
        HttpServletRequest request = request("GET", "/app/article/download");
        when(request.getParameter("file")).thenReturn("../db.properties");
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        new SpaRoutingFilter().doFilter(request, response, chain);

        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST, "非法附件文件名");
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    public void shouldRejectLegacyGetLogoutInsteadOfRestoringStateMutation() throws Exception {
        HttpServletRequest request = request("GET", "/app/user/logout");
        HttpServletResponse response = mock(HttpServletResponse.class);
        StringWriter body = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(body));
        FilterChain chain = mock(FilterChain.class);

        new SpaRoutingFilter().doFilter(request, response, chain);

        verify(response).setStatus(HttpServletResponse.SC_GONE);
        verify(chain, never()).doFilter(any(), any());
        assertTrue(body.toString().contains("LEGACY_GET_DISABLED"));
    }

    private HttpServletRequest request(String method, String uri) {
        return request(method, uri, uri.substring("/app".length()));
    }

    private HttpServletRequest request(String method, String uri, String servletPath) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn(method);
        when(request.getRequestURI()).thenReturn(uri);
        when(request.getContextPath()).thenReturn("/app");
        when(request.getServletPath()).thenReturn(servletPath);
        return request;
    }
}
