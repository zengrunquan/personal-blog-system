package com.blog.api;

import com.blog.api.security.AuthenticatedSessionManager;
import com.blog.entity.User;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AuthenticatedSessionManagerTest {

    @Test
    public void loginShouldInvalidateAnonymousSessionAndCreateFreshAuthenticatedSession() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession anonymousSession = mock(HttpSession.class);
        HttpSession authenticatedSession = mock(HttpSession.class);
        User user = new User();
        when(request.getSession(false)).thenReturn(anonymousSession);
        when(request.getSession(true)).thenReturn(authenticatedSession);

        HttpSession result = AuthenticatedSessionManager.start(request, user);

        verify(anonymousSession).invalidate();
        verify(authenticatedSession).setAttribute("loginUser", user);
        assertSame(authenticatedSession, result);
    }
}
