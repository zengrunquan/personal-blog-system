<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>文章列表 - 个人博客系统</title>
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
                        <a class="nav-link active" href="${pageContext.request.contextPath}/article/list">
                            <i class="fas fa-newspaper me-1"></i>文章
                        </a>
                    </li>
                </ul>

                <!-- 搜索框 -->
                <form class="d-flex me-3" action="${pageContext.request.contextPath}/article/search" method="get">
                    <div class="input-group">
                        <input type="text" class="form-control" name="keyword" placeholder="搜索文章..."
                               value="${keyword}" style="min-width: 200px;">
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
                    <c:choose>
                        <c:when test="${not empty currentCategory}">
                            <li class="breadcrumb-item">
                                <a href="${pageContext.request.contextPath}/article/list">文章</a>
                            </li>
                            <li class="breadcrumb-item active">${currentCategory.name}</li>
                        </c:when>
                        <c:when test="${not empty keyword}">
                            <li class="breadcrumb-item">
                                <a href="${pageContext.request.contextPath}/article/list">文章</a>
                            </li>
                            <li class="breadcrumb-item active">搜索：${keyword}</li>
                        </c:when>
                        <c:otherwise>
                            <li class="breadcrumb-item active">文章列表</li>
                        </c:otherwise>
                    </c:choose>
                </ol>
            </nav>

            <!-- 消息提示 -->
            <c:if test="${not empty error}">
                <div class="alert alert-danger alert-dismissible fade show" role="alert">
                    <i class="fas fa-exclamation-circle me-2"></i>${error}
                    <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                </div>
            </c:if>

            <div class="row">
                <!-- 左侧文章列表 -->
                <div class="col-lg-8">
                    <!-- 标题 -->
                    <div class="d-flex justify-content-between align-items-center mb-4">
                        <h4 class="mb-0">
                            <c:choose>
                                <c:when test="${not empty currentCategory}">
                                    <i class="fas fa-folder me-2"></i>${currentCategory.name}
                                </c:when>
                                <c:when test="${not empty keyword}">
                                    <i class="fas fa-search me-2"></i>搜索结果
                                </c:when>
                                <c:otherwise>
                                    <i class="fas fa-newspaper me-2"></i>最新文章
                                </c:otherwise>
                            </c:choose>
                        </h4>
                        <span class="text-muted">共 ${pageUtil.totalCount} 篇文章</span>
                    </div>

                    <!-- 文章列表 -->
                    <c:choose>
                        <c:when test="${not empty articles}">
                            <c:forEach items="${articles}" var="article">
                                <div class="article-card">
                                    <div class="row">
                                        <c:if test="${not empty article.coverImage}">
                                            <div class="col-md-4">
                                                <a href="${pageContext.request.contextPath}/article/detail?id=${article.id}">
                                                    <img src="${article.coverImage}" alt="${article.title}" class="article-cover">
                                                </a>
                                            </div>
                                        </c:if>
                                        <div class="${not empty article.coverImage ? 'col-md-8' : 'col-md-12'}">
                                            <h5 class="article-title">
                                                <a href="${pageContext.request.contextPath}/article/detail?id=${article.id}">
                                                    ${article.title}
                                                </a>
                                            </h5>
                                            <div class="article-meta">
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
                                                    <fmt:formatDate value="${article.createTime}" pattern="yyyy-MM-dd"/>
                                                </span>
                                                <span>
                                                    <i class="fas fa-eye"></i>
                                                    ${article.viewCount}
                                                </span>
                                                <span>
                                                    <i class="fas fa-comment"></i>
                                                    ${article.commentCount}
                                                </span>
                                            </div>
                                            <p class="article-summary">
                                                ${fn:substring(article.summary, 0, 150)}${fn:length(article.summary) > 150 ? '...' : ''}
                                            </p>
                                            <a href="${pageContext.request.contextPath}/article/detail?id=${article.id}" class="btn btn-outline-primary btn-sm">
                                                阅读全文 <i class="fas fa-arrow-right ms-1"></i>
                                            </a>
                                        </div>
                                    </div>
                                </div>
                            </c:forEach>

                            <!-- 分页 -->
                            <c:if test="${pageUtil.totalPages > 1}">
                                <nav aria-label="文章分页">
                                    <ul class="pagination">
                                        <!-- 上一页 -->
                                        <li class="page-item ${!pageUtil.hasPrevious() ? 'disabled' : ''}">
                                            <a class="page-link" href="?page=${pageUtil.previousPage}${not empty keyword ? '&keyword='.concat(keyword) : ''}${not empty categoryId ? '&id='.concat(categoryId) : ''}">
                                                <i class="fas fa-chevron-left"></i>
                                            </a>
                                        </li>

                                        <!-- 页码 -->
                                        <c:forEach begin="1" end="${pageUtil.totalPages}" var="pageNum">
                                            <c:if test="${pageNum <= 3 || pageNum > pageUtil.totalPages - 2 || (pageNum >= pageUtil.currentPage - 1 && pageNum <= pageUtil.currentPage + 1)}">
                                                <li class="page-item ${pageNum == pageUtil.currentPage ? 'active' : ''}">
                                                    <a class="page-link" href="?page=${pageNum}${not empty keyword ? '&keyword='.concat(keyword) : ''}${not empty categoryId ? '&id='.concat(categoryId) : ''}">
                                                        ${pageNum}
                                                    </a>
                                                </li>
                                            </c:if>
                                            <c:if test="${pageNum == 4 && pageUtil.currentPage > 5}">
                                                <li class="page-item disabled">
                                                    <span class="page-link">...</span>
                                                </li>
                                            </c:if>
                                        </c:forEach>

                                        <!-- 下一页 -->
                                        <li class="page-item ${!pageUtil.hasNext() ? 'disabled' : ''}">
                                            <a class="page-link" href="?page=${pageUtil.nextPage}${not empty keyword ? '&keyword='.concat(keyword) : ''}${not empty categoryId ? '&id='.concat(categoryId) : ''}">
                                                <i class="fas fa-chevron-right"></i>
                                            </a>
                                        </li>
                                    </ul>
                                </nav>
                            </c:if>
                        </c:when>
                        <c:otherwise>
                            <div class="empty-state">
                                <i class="fas fa-inbox"></i>
                                <h5>暂无文章</h5>
                                <p class="text-muted">
                                    <c:choose>
                                        <c:when test="${not empty keyword}">
                                            没有找到包含 "${keyword}" 的文章
                                        </c:when>
                                        <c:otherwise>
                                            还没有发布的文章
                                        </c:otherwise>
                                    </c:choose>
                                </p>
                                <c:if test="${not empty sessionScope.loginUser}">
                                    <a href="${pageContext.request.contextPath}/article/add" class="btn btn-primary">
                                        <i class="fas fa-plus me-2"></i>发布第一篇文章
                                    </a>
                                </c:if>
                            </div>
                        </c:otherwise>
                    </c:choose>
                </div>

                <!-- 右侧边栏 -->
                <div class="col-lg-4">
                    <div class="sidebar">
                        <!-- 搜索框 -->
                        <div class="sidebar-widget">
                            <h5><i class="fas fa-search me-2"></i>文章搜索</h5>
                            <form action="${pageContext.request.contextPath}/article/search" method="get">
                                <div class="search-box">
                                    <input type="text" name="keyword" placeholder="输入关键词搜索..." value="${keyword}">
                                    <button type="submit"><i class="fas fa-search"></i></button>
                                </div>
                            </form>
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

                        <!-- 发布文章 -->
                        <c:if test="${not empty sessionScope.loginUser}">
                            <div class="sidebar-widget">
                                <a href="${pageContext.request.contextPath}/article/add" class="btn btn-primary w-100">
                                    <i class="fas fa-plus me-2"></i>发布新文章
                                </a>
                            </div>
                        </c:if>
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
</body>
</html>
