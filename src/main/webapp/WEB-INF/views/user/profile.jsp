<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>个人信息 - 个人博客</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <!-- Font Awesome -->
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css" rel="stylesheet">
    <!-- 自定义CSS -->
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
                    <li class="nav-item">
                        <a class="nav-link" href="${pageContext.request.contextPath}/">首页</a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link" href="${pageContext.request.contextPath}/article/list">文章列表</a>
                    </li>
                </ul>
                <ul class="navbar-nav">
                    <c:choose>
                        <c:when test="${not empty sessionScope.loginUser}">
                            <li class="nav-item dropdown">
                                <a class="nav-link dropdown-toggle" href="#" role="button" data-bs-toggle="dropdown">
                                    <c:choose>
                                        <c:when test="${not empty sessionScope.loginUser.avatar}">
                                            <img src="${sessionScope.loginUser.avatar}" alt="头像" class="nav-avatar">
                                        </c:when>
                                        <c:otherwise>
                                            <i class="fas fa-user-circle me-1"></i>
                                        </c:otherwise>
                                    </c:choose>
                                    ${sessionScope.loginUser.nickname}
                                </a>
                                <ul class="dropdown-menu dropdown-menu-end">
                                    <li><a class="dropdown-item" href="${pageContext.request.contextPath}/user/profile">个人信息</a></li>
                                    <li><a class="dropdown-item" href="${pageContext.request.contextPath}/user/articles">我的文章</a></li>
                                    <li><hr class="dropdown-divider"></li>
                                    <li><a class="dropdown-item" href="${pageContext.request.contextPath}/user/logout">退出登录</a></li>
                                </ul>
                            </li>
                        </c:when>
                        <c:otherwise>
                            <li class="nav-item">
                                <a class="nav-link" href="${pageContext.request.contextPath}/login.jsp">登录</a>
                            </li>
                            <li class="nav-item">
                                <a class="nav-link" href="${pageContext.request.contextPath}/register.jsp">注册</a>
                            </li>
                        </c:otherwise>
                    </c:choose>
                </ul>
            </div>
        </div>
    </nav>

    <!-- 主要内容 -->
    <div class="container mt-4">
        <div class="row">
            <div class="col-md-4">
                <!-- 头像卡片 -->
                <div class="card mb-4">
                    <div class="card-body text-center">
                        <!-- 头像容器：统一使用img标签，便于上传后更新 -->
                        <div class="avatar-container mb-3" style="width: 150px; height: 150px; margin: 0 auto;">
                            <c:choose>
                                <c:when test="${not empty sessionScope.loginUser.avatar}">
                                    <img src="${sessionScope.loginUser.avatar}" class="rounded-circle" width="150" height="150" id="avatarImg"
                                         style="object-fit: cover; border: 3px solid #e2e8f0;">
                                </c:when>
                                <c:otherwise>
                                    <div class="bg-secondary rounded-circle d-inline-flex align-items-center justify-content-center" style="width: 150px; height: 150px;" id="avatarPlaceholder">
                                        <span class="text-white display-4">${sessionScope.loginUser.nickname.charAt(0)}</span>
                                    </div>
                                    <img src="" class="rounded-circle d-none" width="150" height="150" id="avatarImg"
                                         style="object-fit: cover; border: 3px solid #e2e8f0;">
                                </c:otherwise>
                            </c:choose>
                        </div>
                        <h4>${sessionScope.loginUser.nickname}</h4>
                        <p class="text-muted">@${sessionScope.loginUser.username}</p>

                        <!-- 头像上传表单 -->
                        <form id="avatarForm" enctype="multipart/form-data" class="mt-3">
                            <div class="mb-3">
                                <input type="file" class="form-control form-control-sm" id="avatarFile" name="avatar" accept="image/jpeg,image/png,image/gif">
                            </div>
                            <button type="submit" class="btn btn-outline-primary btn-sm">更换头像</button>
                        </form>
                    </div>
                </div>
            </div>

            <div class="col-md-8">
                <!-- 个人信息卡片 -->
                <div class="card mb-4">
                    <div class="card-header d-flex justify-content-between align-items-center">
                        <h5 class="mb-0">个人信息</h5>
                        <a href="${pageContext.request.contextPath}/user/edit" class="btn btn-primary btn-sm">编辑资料</a>
                    </div>
                    <div class="card-body">
                        <c:if test="${not empty requestScope.success}">
                            <div class="alert alert-success">${requestScope.success}</div>
                        </c:if>
                        <c:if test="${not empty requestScope.error}">
                            <div class="alert alert-danger">${requestScope.error}</div>
                        </c:if>

                        <table class="table">
                            <tr>
                                <th width="120">用户名</th>
                                <td>${sessionScope.loginUser.username}</td>
                            </tr>
                            <tr>
                                <th>昵称</th>
                                <td>${sessionScope.loginUser.nickname}</td>
                            </tr>
                            <tr>
                                <th>邮箱</th>
                                <td>${sessionScope.loginUser.email}</td>
                            </tr>
                            <tr>
                                <th>角色</th>
                                <td>
                                    <c:choose>
                                        <c:when test="${sessionScope.loginUser.role == 1}">
                                            <span class="badge bg-danger">管理员</span>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="badge bg-primary">普通用户</span>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                            </tr>
                            <tr>
                                <th>注册时间</th>
                                <td>${sessionScope.loginUser.createTime}</td>
                            </tr>
                        </table>
                    </div>
                </div>

                <!-- 快捷操作 -->
                <div class="card">
                    <div class="card-header">
                        <h5 class="mb-0">快捷操作</h5>
                    </div>
                    <div class="card-body">
                        <div class="d-flex gap-2">
                            <a href="${pageContext.request.contextPath}/user/password" class="btn btn-outline-warning">修改密码</a>
                            <a href="${pageContext.request.contextPath}/user/articles" class="btn btn-outline-info">我的文章</a>
                            <a href="${pageContext.request.contextPath}/article/add" class="btn btn-outline-success">写文章</a>
                        </div>
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
        // 头像上传
        document.getElementById('avatarForm').addEventListener('submit', function(e) {
            e.preventDefault();

            const fileInput = document.getElementById('avatarFile');
            if (!fileInput.files.length) {
                showToast('请选择要上传的头像', 'warning');
                return;
            }

            // 验证文件类型
            const file = fileInput.files[0];
            const allowedTypes = ['image/jpeg', 'image/png', 'image/gif'];
            if (!allowedTypes.includes(file.type)) {
                showToast('只支持jpg、png、gif格式的图片', 'warning');
                return;
            }

            // 验证文件大小（5MB）
            if (file.size > 5 * 1024 * 1024) {
                showToast('图片大小不能超过5MB', 'warning');
                return;
            }

            const submitBtn = this.querySelector('button[type="submit"]');
            setButtonLoading(submitBtn, '上传中...');

            const formData = new FormData();
            formData.append('avatar', file);

            fetch('${pageContext.request.contextPath}/user/avatar', {
                method: 'POST',
                body: formData
            })
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    showToast('头像上传成功', 'success');

                    // 添加缓存破坏参数，确保浏览器加载新图片
                    const newAvatarUrl = data.avatar + '?t=' + new Date().getTime();

                    // 更新资料页头像显示
                    const avatarImg = document.getElementById('avatarImg');
                    const avatarPlaceholder = document.getElementById('avatarPlaceholder');

                    avatarImg.src = newAvatarUrl;
                    avatarImg.classList.remove('d-none');

                    // 隐藏占位符
                    if (avatarPlaceholder) {
                        avatarPlaceholder.classList.add('d-none');
                    }

                    // 同步更新导航栏头像（实时联动）
                    const navAvatar = document.querySelector('.nav-avatar');
                    if (navAvatar) {
                        navAvatar.src = newAvatarUrl;
                    }

                    fileInput.value = '';
                } else {
                    showToast(data.message || '上传失败', 'danger');
                }
            })
            .catch(error => {
                console.error('[DEBUG] 头像上传失败:', error);
                showToast('上传失败: ' + error.message, 'danger');
            })
            .finally(() => {
                restoreButton(submitBtn);
            });
        });

        // 文件选择预览
        document.getElementById('avatarFile').addEventListener('change', function() {
            const file = this.files[0];
            if (file) {
                const allowedTypes = ['image/jpeg', 'image/png', 'image/gif'];
                if (!allowedTypes.includes(file.type)) {
                    showToast('只支持jpg、png、gif格式的图片', 'warning');
                    this.value = '';
                    return;
                }

                if (file.size > 5 * 1024 * 1024) {
                    showToast('图片大小不能超过5MB', 'warning');
                    this.value = '';
                    return;
                }

                // 预览选中的图片（同时更新资料页和导航栏头像）
                const reader = new FileReader();
                reader.onload = function(e) {
                    const avatarImg = document.getElementById('avatarImg');
                    const avatarPlaceholder = document.getElementById('avatarPlaceholder');

                    avatarImg.src = e.target.result;
                    avatarImg.classList.remove('d-none');

                    if (avatarPlaceholder) {
                        avatarPlaceholder.classList.add('d-none');
                    }

                    // 同步更新导航栏头像预览
                    const navAvatar = document.querySelector('.nav-avatar');
                    if (navAvatar) {
                        navAvatar.src = e.target.result;
                    }
                };
                reader.readAsDataURL(file);
            }
        });
    </script>
</body>
</html>
