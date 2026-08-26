package com.blog.api;

import com.blog.api.security.ApiSecurityFilter;
import com.blog.api.security.CsrfTokenManager;
import com.blog.entity.User;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ApiSecurityFilterTest {

    private ApiSecurityFilter filter;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain chain;
    private HttpSession session;
    private StringWriter body;

    @Before
    public void setUp() throws Exception {
        filter = new ApiSecurityFilter();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        chain = mock(FilterChain.class);
        session = mock(HttpSession.class);
        body = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(body));
        when(request.getContextPath()).thenReturn("/personal_blog_system_war_exploded");
    }

    @Test
    public void guestShouldReceiveJson401ForMeEndpoint() throws Exception {
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn(
                "/personal_blog_system_war_exploded/api/me"
        );
        when(request.getSession(false)).thenReturn(null);

        filter.doFilter(request, response, chain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(chain, never()).doFilter(request, response);
        assertTrue(body.toString().contains("AUTH_REQUIRED"));
    }

    @Test
    public void normalUserShouldReceiveJson403ForAdminEndpoint() throws Exception {
        User user = new User();
        user.setRole(0);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn(
                "/personal_blog_system_war_exploded/api/admin/dashboard"
        );
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("loginUser")).thenReturn(user);

        filter.doFilter(request, response, chain);

        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        assertTrue(body.toString().contains("ADMIN_REQUIRED"));
    }

    @Test
    public void writeRequestWithoutCsrfShouldBeRejected() throws Exception {
        User user = new User();
        user.setRole(0);
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn(
                "/personal_blog_system_war_exploded/api/articles"
        );
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("loginUser")).thenReturn(user);
        when(session.getAttribute(CsrfTokenManager.SESSION_KEY)).thenReturn("known");
        when(request.getHeader(CsrfTokenManager.HEADER_NAME)).thenReturn(null);

        filter.doFilter(request, response, chain);

        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        assertTrue(body.toString().contains("CSRF_INVALID"));
    }

    @Test
    public void publicReadShouldPassWithoutSession() throws Exception {
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn(
                "/personal_blog_system_war_exploded/api/articles"
        );

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }
}
