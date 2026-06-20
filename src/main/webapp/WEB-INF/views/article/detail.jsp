<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${article.title} - 个人博客系统</title>
    <!-- Bootstrap CSS -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <!-- Font Awesome -->
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css" rel="stylesheet">
    <!-- 自定义CSS -->
    <link href="${pageContext.request.contextPath}/static/css/style.css" rel="stylesheet">
</head>
<body>
    <!-- 导航栏 -->
    <nav class="navbar navbar-expand-lg">
        <div class="container">
            <a class="navbar-brand" href="${pageContext.request.contextPath}/">
                <i class="fas fa-blog me-2"></i>个人博客
            </a>
            <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarNav">
                <span class="navbar-toggler-icon"></span>
            </button>
            <div class="collapse navbar-collapse" id="navbarNav">
                <ul class="navbar-nav me-auto">
                    <li class="nav-item">
                        <a class="nav-link" href="${pageContext.request.contextPath}/">
                            <i class="fas fa-home me-1"></i>首页
                        </a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link" href="${pageContext.request.contextPath}/article/list">
                            <i class="fas fa-newspaper me-1"></i>文章
                        </a>
                    </li>
                </ul>

                <!-- 搜索框 -->
                <form class="d-flex me-3" action="${pageContext.request.contextPath}/article/search" method="get">
                    <div class="input-group">
                        <input type="text" class="form-control" name="keyword" placeholder="搜索文章..."
                               style="min-width: 200px;">
                        <button class="btn btn-light" type="submit">
                            <i class="fas fa-search"></i>
                        </button>
                    </div>
                </form>

                <!-- 用户菜单 -->
                <ul class="navbar-nav">
                    <c:choose>
                        <c:when test="${not empty sessionScope.loginUser}">
                            <li class="nav-item dropdown">
                                <a class="nav-link dropdown-toggle" href="#" id="userDropdown"
                                   role="button" data-bs-toggle="dropdown">
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
                                    <li>
                                        <a class="dropdown-item" href="${pageContext.request.contextPath}/user/profile">
                                            <i class="fas fa-user me-2"></i>个人信息
                                        </a>
                                    </li>
                                    <li>
                                        <a class="dropdown-item" href="${pageContext.request.contextPath}/user/articles">
                                            <i class="fas fa-file-alt me-2"></i>我的文章
                                        </a>
                                    </li>
                                    <li>
                                        <a class="dropdown-item" href="${pageContext.request.contextPath}/article/add">
                                            <i class="fas fa-plus me-2"></i>发布文章
                                        </a>
                                    </li>
                                    <c:if test="${sessionScope.loginUser.role == 1}">
                                        <li><hr class="dropdown-divider"></li>
                                        <li>
                                            <a class="dropdown-item" href="${pageContext.request.contextPath}/admin/dashboard">
                                                <i class="fas fa-cog me-2"></i>后台管理
                                            </a>
                                        </li>
                                    </c:if>
                                    <li><hr class="dropdown-divider"></li>
                                    <li>
                                        <a class="dropdown-item text-danger" href="${pageContext.request.contextPath}/user/logout">
                                            <i class="fas fa-sign-out-alt me-2"></i>退出登录
                                        </a>
                                    </li>
                                </ul>
                            </li>
                        </c:when>
                        <c:otherwise>
                            <li class="nav-item">
                                <a class="nav-link" href="${pageContext.request.contextPath}/login.jsp">
                                    <i class="fas fa-sign-in-alt me-1"></i>登录
                                </a>
                            </li>
                            <li class="nav-item">
                                <a class="nav-link" href="${pageContext.request.contextPath}/register.jsp">
                                    <i class="fas fa-user-plus me-1"></i>注册
                                </a>
                            </li>
                        </c:otherwise>
                    </c:choose>
                </ul>
            </div>
        </div>
    </nav>

    <!-- 主内容 -->
    <div class="main-content">
        <div class="container">
            <!-- 面包屑导航 -->
            <nav aria-label="breadcrumb" class="mb-4">
                <ol class="breadcrumb">
                    <li class="breadcrumb-item">
                        <a href="${pageContext.request.contextPath}/">首页</a>
                    </li>
                    <li class="breadcrumb-item">
                        <a href="${pageContext.request.contextPath}/article/list">文章</a>
                    </li>
                    <li class="breadcrumb-item">
                        <a href="${pageContext.request.contextPath}/article/category?id=${article.categoryId}">
                            ${article.categoryName}
                        </a>
                    </li>
                    <li class="breadcrumb-item active">文章详情</li>
                </ol>
            </nav>

            <div class="row">
                <!-- 左侧文章内容 -->
                <div class="col-lg-8">
                    <!-- 文章内容 -->
                    <div class="article-content">
                        <!-- 文章标题 -->
                        <h1 class="mb-3">${article.title}</h1>

                        <!-- 文章信息 -->
                        <div class="article-meta mb-4">
                            <span>
                                <i class="fas fa-user"></i>
                                ${not empty article.authorNickname ? article.authorNickname : article.authorName}
                            </span>
                            <span>
                                <i class="fas fa-folder"></i>
                                <a href="${pageContext.request.contextPath}/article/category?id=${article.categoryId}">
                                    ${article.categoryName}
                                </a>
                            </span>
                            <span>
                                <i class="fas fa-clock"></i>
                                <fmt:formatDate value="${article.createTime}" pattern="yyyy-MM-dd HH:mm:ss"/>
                            </span>
                            <span>
                                <i class="fas fa-eye"></i>
                                ${article.viewCount} 次浏览
                            </span>
                            <span>
                                <i class="fas fa-comment"></i>
                                ${article.commentCount} 条评论
                            </span>
                        </div>

                        <!-- 封面图片 -->
                        <c:if test="${not empty article.coverImage}">
                            <img src="${article.coverImage}" alt="${article.title}" class="article-cover mb-4">
                        </c:if>

                        <!-- 文章正文 -->
                        <div class="article-body">
                            ${article.content}
                        </div>

                        <!-- 文章操作 -->
                        <div class="article-footer mt-4">
                            <div>
                                <c:if test="${not empty sessionScope.loginUser && (sessionScope.loginUser.id == article.userId || sessionScope.loginUser.role == 1)}">
                                    <a href="${pageContext.request.contextPath}/article/edit?id=${article.id}" class="btn btn-outline-primary btn-sm">
                                        <i class="fas fa-edit me-1"></i>编辑
                                    </a>
                                    <button class="btn btn-outline-danger btn-sm" onclick="deleteArticle(${article.id})">
                                        <i class="fas fa-trash me-1"></i>删除
                                    </button>
                                </c:if>
                            </div>
                            <div>
                                <span class="text-muted">
                                    最后更新于 <fmt:formatDate value="${article.updateTime}" pattern="yyyy-MM-dd HH:mm"/>
                                </span>
                            </div>
                        </div>
                    </div>

                    <!-- 评论区 -->
                    <div class="comment-section">
                        <h4 class="mb-4">
                            <i class="fas fa-comments me-2"></i>评论 (${article.commentCount})
                        </h4>

                        <!-- 评论表单 -->
                        <c:choose>
                            <c:when test="${not empty sessionScope.loginUser}">
                                <div class="comment-form mb-4">
                                    <form id="commentForm">
                                        <input type="hidden" name="articleId" value="${article.id}">
                                        <div class="mb-3">
                                            <textarea class="form-control" name="content" id="commentContent"
                                                      placeholder="写下你的评论..." rows="3" required></textarea>
                                        </div>
                                        <button type="submit" class="btn btn-primary">
                                            <i class="fas fa-paper-plane me-2"></i>发表评论
                                        </button>
                                    </form>
                                </div>
                            </c:when>
                            <c:otherwise>
                                <div class="alert alert-info mb-4">
                                    <i class="fas fa-info-circle me-2"></i>
                                    请先 <a href="${pageContext.request.contextPath}/login.jsp">登录</a> 后发表评论
                                </div>
                            </c:otherwise>
                        </c:choose>

                        <!-- 评论列表 -->
                        <div id="commentList">
                            <c:choose>
                                <c:when test="${not empty comments}">
                                    <c:forEach items="${comments}" var="comment">
                                        <div class="comment-item" id="comment-${comment.id}">
                                            <div class="comment-header">
                                                <c:choose>
                                                    <c:when test="${not empty comment.userAvatar}">
                                                        <img src="${comment.userAvatar}" alt="头像" class="comment-avatar">
                                                    </c:when>
                                                    <c:otherwise>
                                                        <div class="comment-avatar bg-primary text-white d-flex align-items-center justify-content-center">
                                                            <i class="fas fa-user"></i>
                                                        </div>
                                                    </c:otherwise>
                                                </c:choose>
                                                <div>
                                                    <span class="comment-user">
                                                        ${not empty comment.userNickname ? comment.userNickname : comment.username}
                                                    </span>
                                                    <span class="comment-time">
                                                        <fmt:formatDate value="${comment.createTime}" pattern="yyyy-MM-dd HH:mm"/>
                                                    </span>
                                                </div>
                                                <c:if test="${not empty sessionScope.loginUser && (sessionScope.loginUser.id == comment.userId || sessionScope.loginUser.role == 1)}">
                                                    <button class="btn btn-sm btn-outline-danger ms-auto" onclick="deleteComment(${comment.id})">
                                                        <i class="fas fa-trash"></i>
                                                    </button>
                                                </c:if>
                                            </div>
                                            <div class="comment-content">
                                                    ${comment.content}
                                            </div>
                                        </div>
                                    </c:forEach>
                                </c:when>
                                <c:otherwise>
                                    <div class="empty-state py-4">
                                        <i class="fas fa-comment-slash"></i>
                                        <p class="text-muted">暂无评论，快来发表第一条评论吧！</p>
                                    </div>
                                </c:otherwise>
                            </c:choose>
                        </div>
                    </div>
                </div>

                <!-- 右侧边栏 -->
                <div class="col-lg-4">
                    <div class="sidebar">
                        <!-- 作者信息 -->
                        <div class="sidebar-widget">
                            <h5><i class="fas fa-user me-2"></i>作者信息</h5>
                            <div class="text-center">
                                <c:choose>
                                    <c:when test="${not empty article.authorAvatar}">
                                        <img src="${article.authorAvatar}" alt="头像" class="avatar avatar-medium mb-3">
                                    </c:when>
                                    <c:otherwise>
                                        <div class="avatar avatar-medium bg-primary text-white d-flex align-items-center justify-content-center mx-auto mb-3">
                                            <i class="fas fa-user fa-2x"></i>
                                        </div>
                                    </c:otherwise>
                                </c:choose>
                                <h6>${not empty article.authorNickname ? article.authorNickname : article.authorName}</h6>
                            </div>
                        </div>

                        <!-- 文章分类 -->
                        <div class="sidebar-widget">
                            <h5><i class="fas fa-folder me-2"></i>文章分类</h5>
                            <ul class="category-list">
                                <c:forEach items="${categories}" var="category">
                                    <li>
                                        <a href="${pageContext.request.contextPath}/article/category?id=${category.id}">
                                            ${category.name}
                                            <span class="category-count">${category.articleCount}</span>
                                        </a>
                                    </li>
                                </c:forEach>
                            </ul>
                        </div>

                        <!-- 返回按钮 -->
                        <div class="sidebar-widget">
                            <a href="${pageContext.request.contextPath}/article/list" class="btn btn-outline-primary w-100">
                                <i class="fas fa-arrow-left me-2"></i>返回文章列表
                            </a>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <!-- Footer -->
    <footer class="footer">
        <div class="container">
            <div class="footer-bottom text-center">
                <p class="mb-0">&copy; 2026 个人博客系统 | JavaWeb课程项目</p>
            </div>
        </div>
    </footer>

    <!-- Bootstrap JS -->
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <!-- 自定义JS -->
    <script src="${pageContext.request.contextPath}/static/js/main.js"></script>

    <script>
        // 发表评论
        document.getElementById('commentForm')?.addEventListener('submit', async function(e) {
            e.preventDefault();

            const content = document.getElementById('commentContent').value.trim();
            const articleId = this.querySelector('[name="articleId"]').value;
            const submitBtn = this.querySelector('button[type="submit"]');

            if (!content) {
                showToast('请输入评论内容', 'warning');
                return;
            }

            setButtonLoading(submitBtn, '发表中...');

            try {
                const response = await fetch('${pageContext.request.contextPath}/comment/add', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded',
                    },
                    body: 'content=' + encodeURIComponent(content) + '&articleId=' + articleId
                });

                const data = await response.json();

                if (data.success) {
                    showToast('评论发表成功', 'success');
                    // 刷新页面显示新评论
                    setTimeout(() => {
                        location.reload();
                    }, 1000);
                } else {
                    showToast(data.message || '评论发表失败', 'error');
                }
            } catch (error) {
                console.error('[DEBUG] 评论发表失败:', error);
                showToast('网络错误，请稍后重试', 'error');
            } finally {
                restoreButton(submitBtn);
            }
        });

        // 删除评论
        async function deleteComment(commentId) {
            if (!confirm('确定要删除这条评论吗？')) {
                return;
            }

            try {
                const response = await fetch('${pageContext.request.contextPath}/comment/delete', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded',
                    },
                    body: 'commentId=' + commentId
                });

                const data = await response.json();

                if (data.success) {
                    showToast('评论删除成功', 'success');
                    // 移除评论元素
                    const commentElement = document.getElementById('comment-' + commentId);
                    if (commentElement) {
                        commentElement.style.transition = 'all 0.3s ease';
                        commentElement.style.opacity = '0';
                        commentElement.style.transform = 'translateX(-100%)';
                        setTimeout(() => commentElement.remove(), 300);
                    }
                } else {
                    showToast(data.message || '删除失败', 'error');
                }
            } catch (error) {
                console.error('[DEBUG] 删除评论失败:', error);
                showToast('网络错误，请稍后重试', 'error');
            }
        }

        // 删除文章
        async function deleteArticle(articleId) {
            if (!confirm('确定要删除这篇文章吗？删除后无法恢复！')) {
                return;
            }

            try {
                const response = await fetch('${pageContext.request.contextPath}/article/delete', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded',
                    },
                    body: 'id=' + articleId
                });

                const data = await response.json();

                if (data.success) {
                    showToast('文章删除成功', 'success');
                    setTimeout(() => {
                        window.location.href = '${pageContext.request.contextPath}/article/list';
                    }, 1000);
                } else {
                    showToast(data.message || '删除失败', 'error');
                }
            } catch (error) {
                console.error('[DEBUG] 删除文章失败:', error);
                showToast('网络错误，请稍后重试', 'error');
            }
        }
    </script>
</body>
</html>
