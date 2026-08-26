package com.blog.api;

import com.blog.api.security.CsrfTokenManager;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CsrfTokenManagerTest {

    @Test
    public void getOrCreateShouldReuseTokenStoredInSession() {
        HttpSession session = mock(HttpSession.class);
        when(session.getAttribute(CsrfTokenManager.SESSION_KEY)).thenReturn("existing-token");

        assertTrue("existing-token".equals(CsrfTokenManager.getOrCreate(session)));
    }

    @Test
    public void getOrCreateShouldPersistNewToken() {
        HttpSession session = mock(HttpSession.class);

        String token = CsrfTokenManager.getOrCreate(session);

        assertNotNull(token);
        verify(session).setAttribute(CsrfTokenManager.SESSION_KEY, token);
    }

    @Test
    public void validateShouldUseHeaderAndConstantTimeComparison() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(CsrfTokenManager.SESSION_KEY)).thenReturn("known-token");
        when(request.getHeader(CsrfTokenManager.HEADER_NAME)).thenReturn("known-token");

        assertTrue(CsrfTokenManager.isValid(request));

        when(request.getHeader(CsrfTokenManager.HEADER_NAME)).thenReturn("wrong-token");
        assertFalse(CsrfTokenManager.isValid(request));
    }
}
