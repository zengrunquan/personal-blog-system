<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>我的文章 - 个人博客</title>
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
                            <li><a class="dropdown-item active" href="${pageContext.request.contextPath}/user/articles">我的文章</a></li>
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
        <div class="d-flex justify-content-between align-items-center mb-4">
            <h4>我的文章 <span class="badge bg-secondary">${totalCount}</span></h4>
            <a href="${pageContext.request.contextPath}/article/add" class="btn btn-primary">
                <i class="bi bi-plus"></i> 写文章
            </a>
        </div>

        <c:if test="${empty articles}">
            <div class="alert alert-info">您还没有发布过文章</div>
        </c:if>

        <c:forEach items="${articles}" var="article">
            <div class="card mb-3">
                <div class="card-body">
                    <div class="d-flex justify-content-between">
                        <h5>
                            <a href="${pageContext.request.contextPath}/article/detail?id=${article.id}" class="text-decoration-none">
                                ${article.title}
                            </a>
                        </h5>
                        <div>
                            <a href="${pageContext.request.contextPath}/article/edit?id=${article.id}" class="btn btn-outline-primary btn-sm">编辑</a>
                            <button class="btn btn-outline-danger btn-sm" onclick="deleteArticle(${article.id})">删除</button>
                        </div>
                    </div>
                    <c:if test="${not empty article.summary}">
                        <p class="text-muted mt-2">${article.summary}</p>
                    </c:if>
                    <div class="text-muted small">
                        <span>分类：${article.categoryName}</span>
                        <span class="ms-3">浏览：${article.viewCount}</span>
                        <span class="ms-3">发布时间：${article.createTime}</span>
                    </div>
                </div>
            </div>
        </c:forEach>

        <!-- 分页 -->
        <c:if test="${totalPages > 1}">
            <nav>
                <ul class="pagination justify-content-center">
                    <li class="page-item ${currentPage == 1 ? 'disabled' : ''}">
                        <a class="page-link" href="?page=${currentPage - 1}">上一页</a>
                    </li>
                    <c:forEach begin="1" end="${totalPages}" var="i">
                        <li class="page-item ${currentPage == i ? 'active' : ''}">
                            <a class="page-link" href="?page=${i}">${i}</a>
                        </li>
                    </c:forEach>
                    <li class="page-item ${currentPage == totalPages ? 'disabled' : ''}">
                        <a class="page-link" href="?page=${currentPage + 1}">下一页</a>
                    </li>
                </ul>
            </nav>
        </c:if>
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
        function deleteArticle(id) {
            if (!confirm('确定要删除这篇文章吗？此操作不可恢复。')) {
                return;
            }

            fetch('${pageContext.request.contextPath}/article/delete', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: 'id=' + id
            })
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    showToast('文章已删除', 'success');
                    setTimeout(function() {
                        location.reload();
                    }, 1000);
                } else {
                    showToast(data.message || '删除失败', 'danger');
                }
            })
            .catch(error => {
                showToast('删除失败: ' + error.message, 'danger');
            });
        }
    </script>
</body>
</html>
