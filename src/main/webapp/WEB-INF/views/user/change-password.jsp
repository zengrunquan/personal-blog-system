<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>修改密码 - 个人博客</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/static/css/style.css" rel="stylesheet">
</head>
<body>
    <!-- 导航栏 -->
    <nav class="navbar navbar-expand-lg navbar-dark bg-primary">
        <div class="container">
            <a class="navbar-brand" href="${pageContext.request.contextPath}/">个人博客</a>
            <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarNav">
                <span class="navbar-toggler-icon"></span>
            </button>
            <div class="collapse navbar-collapse" id="navbarNav">
                <ul class="navbar-nav me-auto">
                    <li class="nav-item"><a class="nav-link" href="${pageContext.request.contextPath}/">首页</a></li>
                    <li class="nav-item"><a class="nav-link" href="${pageContext.request.contextPath}/article/list">文章列表</a></li>
                </ul>
                <ul class="navbar-nav">
                    <li class="nav-item dropdown">
                        <a class="nav-link dropdown-toggle" href="#" role="button" data-bs-toggle="dropdown">
                            ${sessionScope.loginUser.nickname}
                        </a>
                        <ul class="dropdown-menu dropdown-menu-end">
                            <li><a class="dropdown-item" href="${pageContext.request.contextPath}/user/profile">个人信息</a></li>
                            <li><a class="dropdown-item" href="${pageContext.request.contextPath}/user/articles">我的文章</a></li>
                            <li><hr class="dropdown-divider"></li>
                            <li><a class="dropdown-item" href="${pageContext.request.contextPath}/user/logout">退出登录</a></li>
                        </ul>
                    </li>
                </ul>
            </div>
        </div>
    </nav>

    <!-- 主要内容 -->
    <div class="container mt-4">
        <div class="row justify-content-center">
            <div class="col-md-6">
                <div class="card">
                    <div class="card-header">
                        <h5 class="mb-0">修改密码</h5>
                    </div>
                    <div class="card-body">
                        <c:if test="${not empty requestScope.success}">
                            <div class="alert alert-success">${requestScope.success}</div>
                        </c:if>
                        <c:if test="${not empty requestScope.error}">
                            <div class="alert alert-danger">${requestScope.error}</div>
                        </c:if>

                        <form action="${pageContext.request.contextPath}/user/password" method="post" id="passwordForm">
                            <div class="mb-3">
                                <label for="oldPassword" class="form-label">原密码 <span class="text-danger">*</span></label>
                                <input type="password" class="form-control" id="oldPassword" name="oldPassword" required>
                            </div>
                            <div class="mb-3">
                                <label for="newPassword" class="form-label">新密码 <span class="text-danger">*</span></label>
                                <input type="password" class="form-control" id="newPassword" name="newPassword" required minlength="6" maxlength="20">
                                <div class="form-text">密码长度6-20位</div>
                            </div>
                            <div class="mb-3">
                                <label for="confirmPassword" class="form-label">确认新密码 <span class="text-danger">*</span></label>
                                <input type="password" class="form-control" id="confirmPassword" name="confirmPassword" required>
                            </div>
                            <div class="d-flex gap-2">
                                <button type="submit" class="btn btn-warning">修改密码</button>
                                <a href="${pageContext.request.contextPath}/user/profile" class="btn btn-secondary">取消</a>
                            </div>
                        </form>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <!-- 页脚 -->
    <footer class="bg-dark text-white mt-5 py-4">
        <div class="container text-center">
            <p class="mb-0">&copy; 2026 个人博客系统. All rights reserved.</p>
        </div>
    </footer>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <script src="${pageContext.request.contextPath}/static/js/main.js"></script>
    <script>
        document.getElementById('passwordForm').addEventListener('submit', function(e) {
            var newPwd = document.getElementById('newPassword').value;
            var confirmPwd = document.getElementById('confirmPassword').value;

            if (newPwd !== confirmPwd) {
                e.preventDefault();
                showToast('两次输入的新密码不一致', 'danger');
                return false;
            }
        });
    </script>
</body>
</html>
