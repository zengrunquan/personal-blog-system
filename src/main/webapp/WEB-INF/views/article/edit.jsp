<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>编辑文章 - 个人博客</title>
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
            <div class="col-md-10">
                <div class="card">
                    <div class="card-header">
                        <h5 class="mb-0">编辑文章</h5>
                    </div>
                    <div class="card-body">
                        <c:if test="${not empty requestScope.error}">
                            <div class="alert alert-danger">${requestScope.error}</div>
                        </c:if>

                        <form action="${pageContext.request.contextPath}/article/edit" method="post" id="articleForm">
                            <input type="hidden" name="id" value="${article.id}">
                            <input type="hidden" name="csrfToken" value="${csrfToken}">

                            <div class="mb-3">
                                <label for="title" class="form-label">文章标题 <span class="text-danger">*</span></label>
                                <input type="text" class="form-control" id="title" name="title" value="${article.title}" required maxlength="200">
                            </div>

                            <div class="mb-3">
                                <label for="categoryId" class="form-label">文章分类 <span class="text-danger">*</span></label>
                                <select class="form-select" id="categoryId" name="categoryId" required>
                                    <option value="">请选择分类</option>
                                    <c:forEach items="${categories}" var="category">
                                        <option value="${category.id}" ${category.id == article.categoryId ? 'selected' : ''}>
                                            ${category.name}
                                        </option>
                                    </c:forEach>
                                </select>
                            </div>

                            <div class="mb-3">
                                <label for="summary" class="form-label">文章摘要</label>
                                <textarea class="form-control" id="summary" name="summary" rows="3" maxlength="500">${article.summary}</textarea>
                                <div class="form-text">不填则自动截取正文前200字</div>
                            </div>

                            <div class="mb-3">
                                <label for="content" class="form-label">文章内容 <span class="text-danger">*</span></label>
                                <textarea class="form-control" id="content" name="content" rows="15" required>${article.content}</textarea>
                            </div>

                            <div class="mb-3">
                                <label class="form-label">状态</label>
                                <div>
                                    <div class="form-check form-check-inline">
                                        <input class="form-check-input" type="radio" name="status" id="statusDraft" value="0" ${article.status == 0 ? 'checked' : ''}>
                                        <label class="form-check-label" for="statusDraft">草稿</label>
                                    </div>
                                    <div class="form-check form-check-inline">
                                        <input class="form-check-input" type="radio" name="status" id="statusPublish" value="1" ${article.status == 1 ? 'checked' : ''}>
                                        <label class="form-check-label" for="statusPublish">发布</label>
                                    </div>
                                </div>
                            </div>

                            <div class="d-flex gap-2">
                                <button type="submit" class="btn btn-primary">保存修改</button>
                                <a href="${pageContext.request.contextPath}/user/articles" class="btn btn-secondary">取消</a>
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
</body>
</html>
