package com.blog.api.security;

import com.blog.entity.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Objects;

public final class AuthenticatedSessionManager {

    private AuthenticatedSessionManager() {
    }

    public static HttpSession start(HttpServletRequest request, User user) {
        Objects.requireNonNull(request, "request 不能为空");
        Objects.requireNonNull(user, "user 不能为空");
        HttpSession anonymousSession = request.getSession(false);
        if (anonymousSession != null) {
            // 登录前令牌已暴露给匿名会话，认证成功后必须整体轮换，阻断 Session fixation。
            anonymousSession.invalidate();
        }
        HttpSession authenticatedSession = request.getSession(true);
        authenticatedSession.setAttribute("loginUser", user);
        return authenticatedSession;
    }
}
