<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>用户登录 - 个人博客系统</title>
    <!-- Bootstrap CSS -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <!-- Font Awesome -->
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css" rel="stylesheet">
    <!-- 自定义CSS -->
    <link href="${pageContext.request.contextPath}/static/css/style.css" rel="stylesheet">
    <style>
        body {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            min-height: 100vh;
            display: flex;
            align-items: center;
        }
        .login-card {
            max-width: 450px;
            margin: 0 auto;
            border-radius: 15px;
            box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
        }
        .login-header {
            background: linear-gradient(135deg, var(--color-primary), var(--color-primary-hover));
            color: white;
            text-align: center;
            padding: 2rem;
            border-radius: 15px 15px 0 0;
        }
        .login-header h2 {
            margin: 0;
            font-weight: 700;
        }
        .login-body {
            padding: 2rem;
            background: white;
            border-radius: 0 0 15px 15px;
        }
        .form-floating {
            margin-bottom: 1rem;
        }
        .form-floating .form-control {
            border-radius: 10px;
            padding: 1rem 1rem;
            height: auto;
        }
        .btn-login {
            padding: 12px;
            font-size: 1.1rem;
            border-radius: 10px;
            font-weight: 600;
        }
        .divider {
            display: flex;
            align-items: center;
            margin: 1.5rem 0;
        }
        .divider::before,
        .divider::after {
            content: '';
            flex: 1;
            border-bottom: 1px solid #dee2e6;
        }
        .divider span {
            padding: 0 1rem;
            color: #6c757d;
            font-size: 0.875rem;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="card login-card">
            <div class="login-header">
                <h2><i class="fas fa-blog me-2"></i>个人博客系统</h2>
                <p class="mb-0 mt-2">欢迎回来，请登录您的账号</p>
            </div>
            <div class="login-body">
                <!-- 消息提示 -->
                <c:if test="${not empty error}">
                    <div class="alert alert-danger alert-dismissible fade show" role="alert">
                        <i class="fas fa-exclamation-circle me-2"></i>${error}
                        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                    </div>
                </c:if>
                <c:if test="${not empty success}">
                    <div class="alert alert-success alert-dismissible fade show" role="alert">
                        <i class="fas fa-check-circle me-2"></i>${success}
                        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                    </div>
                </c:if>
                <c:if test="${not empty param.msg}">
                    <div class="alert alert-warning alert-dismissible fade show" role="alert">
                        <i class="fas fa-exclamation-triangle me-2"></i><c:out value="${param.msg}"/>
                        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                    </div>
                </c:if>

                <!-- 登录表单 -->
                <form id="loginForm" action="${pageContext.request.contextPath}/user/login" method="post">
                    <div class="form-floating">
                        <input type="text" class="form-control" id="username" name="username"
                               placeholder="请输入用户名" value="${username}" required>
                        <label for="username"><i class="fas fa-user me-2"></i>用户名</label>
                        <div class="invalid-feedback"></div>
                    </div>

                    <div class="form-floating">
                        <input type="password" class="form-control" id="password" name="password"
                               placeholder="请输入密码" required>
                        <label for="password"><i class="fas fa-lock me-2"></i>密码</label>
                        <div class="invalid-feedback"></div>
                    </div>

                    <div class="d-flex justify-content-between align-items-center mb-3">
                        <div class="form-check">
                            <input class="form-check-input" type="checkbox" id="remember" name="remember">
                            <label class="form-check-label" for="remember">
                                记住我
                            </label>
                        </div>
                    </div>

                    <button type="submit" class="btn btn-primary btn-login w-100">
                        <i class="fas fa-sign-in-alt me-2"></i>登录
                    </button>
                </form>

                <div class="divider">
                    <span>还没有账号？</span>
                </div>

                <a href="${pageContext.request.contextPath}/register.jsp" class="btn btn-outline-primary btn-login w-100">
                    <i class="fas fa-user-plus me-2"></i>立即注册
                </a>

                <div class="text-center mt-3">
                    <a href="${pageContext.request.contextPath}/" class="text-muted">
                        <i class="fas fa-arrow-left me-1"></i>返回首页
                    </a>
                </div>
            </div>
        </div>
    </div>

    <!-- Bootstrap JS -->
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <!-- 自定义JS -->
    <script src="${pageContext.request.contextPath}/static/js/main.js"></script>

    <script>
        // 表单验证
        document.getElementById('loginForm').addEventListener('submit', function(e) {
            const username = document.getElementById('username').value.trim();
            const password = document.getElementById('password').value;

            // 验证用户名
            if (!username) {
                e.preventDefault();
                document.getElementById('username').classList.add('is-invalid');
                document.getElementById('username').nextElementSibling.nextElementSibling.textContent = '请输入用户名';
                return;
            }

            // 验证密码
            if (!password) {
                e.preventDefault();
                document.getElementById('password').classList.add('is-invalid');
                document.getElementById('password').nextElementSibling.nextElementSibling.textContent = '请输入密码';
                return;
            }
        });

        // 实时验证
        document.getElementById('username').addEventListener('input', function() {
            this.classList.remove('is-invalid');
        });

        document.getElementById('password').addEventListener('input', function() {
            this.classList.remove('is-invalid');
        });
    </script>
</body>
</html>
