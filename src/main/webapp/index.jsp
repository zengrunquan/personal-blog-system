<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>个人博客系统</title>
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
                        <a class="nav-link active" href="${pageContext.request.contextPath}/">
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
                            <!-- 已登录 -->
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
                            <!-- 未登录 -->
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
            <!-- 消息提示 -->
            <c:if test="${not empty param.msg}">
                <div class="alert alert-info alert-dismissible fade show" role="alert">
                    <i class="fas fa-info-circle me-2"></i><c:out value="${param.msg}"/>
                    <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                </div>
            </c:if>

            <!-- 横幅 -->
            <div class="jumbotron bg-white rounded-3 shadow-sm p-5 mb-4 text-center">
                <h1 class="display-4 fw-bold text-primary">
                    <i class="fas fa-pen-fancy me-3"></i>欢迎来到我的博客
                </h1>
                <p class="lead text-muted">分享技术，记录生活，感悟人生</p>
                <hr class="my-4">
                <p class="text-muted">
                    这是一个使用 JavaWeb 技术开发的个人博客系统，采用 Servlet + JSP + JDBC 架构
                </p>
                <c:if test="${empty sessionScope.loginUser}">
                    <a class="btn btn-primary btn-lg me-3" href="${pageContext.request.contextPath}/register.jsp">
                        <i class="fas fa-user-plus me-2"></i>立即注册
                    </a>
                    <a class="btn btn-outline-primary btn-lg" href="${pageContext.request.contextPath}/login.jsp">
                        <i class="fas fa-sign-in-alt me-2"></i>登录
                    </a>
                </c:if>
            </div>

            <div class="row">
                <!-- 左侧文章列表 -->
                <div class="col-lg-8">
                    <h4 class="mb-4">
                        <i class="fas fa-fire text-danger me-2"></i>最新文章
                    </h4>

                    <!-- 文章列表由Ajax加载 -->
                    <div id="articleList">
                        <div class="text-center py-5">
                            <div class="loading"></div>
                            <p class="mt-3 text-muted">加载中...</p>
                        </div>
                    </div>
                </div>

                <!-- 右侧边栏 -->
                <div class="col-lg-4">
                    <!-- 关于博主 -->
                    <div class="sidebar-widget">
                        <h5><i class="fas fa-user me-2"></i>关于博主</h5>
                        <p class="text-muted">
                            一个热爱编程的开发者，喜欢分享技术心得和生活感悟。
                        </p>
                        <div class="text-center">
                            <p class="mb-1">
                                <i class="fas fa-envelope me-2"></i>admin@blog.com
                            </p>
                        </div>
                    </div>

                    <!-- 文章分类 -->
                    <div class="sidebar-widget">
                        <h5><i class="fas fa-folder me-2"></i>文章分类</h5>
                        <ul class="category-list" id="categoryList">
                            <!-- 由Ajax加载 -->
                        </ul>
                    </div>

                    <!-- 在线统计 -->
                    <div class="sidebar-widget">
                        <h5><i class="fas fa-chart-bar me-2"></i>站点统计</h5>
                        <div class="row text-center">
                            <div class="col-6">
                                <h4 class="text-primary mb-0" id="onlineCount">0</h4>
                                <small class="text-muted">在线人数</small>
                            </div>
                            <div class="col-6">
                                <h4 class="text-success mb-0" id="articleCount">0</h4>
                                <small class="text-muted">文章总数</small>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <!-- Footer -->
    <footer class="footer">
        <div class="container">
            <div class="row">
                <div class="col-md-6">
                    <h5><i class="fas fa-blog me-2"></i>个人博客系统</h5>
                    <p class="text-muted">基于 JavaWeb 技术开发的个人博客系统</p>
                </div>
                <div class="col-md-3">
                    <h6>快速链接</h6>
                    <ul class="list-unstyled">
                        <li><a href="${pageContext.request.contextPath}/">首页</a></li>
                        <li><a href="${pageContext.request.contextPath}/article/list">文章列表</a></li>
                    </ul>
                </div>
                <div class="col-md-3">
                    <h6>技术栈</h6>
                    <ul class="list-unstyled">
                        <li>Servlet + JSP</li>
                        <li>JDBC + MySQL</li>
                        <li>Bootstrap 5</li>
                    </ul>
                </div>
            </div>
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
        // 页面加载完成后获取数据
        document.addEventListener('DOMContentLoaded', function() {
            loadLatestArticles();
            loadCategories();
            updateOnlineCount();
        });

        // 加载最新文章
        async function loadLatestArticles() {
            try {
                // 这里应该调用Ajax接口获取文章列表
                // 简化处理：直接跳转到文章列表页面
                const response = await fetch('${pageContext.request.contextPath}/article/list?page=1&pageSize=5');
                // 由于是页面跳转，这里暂时显示静态内容
                document.getElementById('articleList').innerHTML = `
                    <div class="text-center py-5">
                        <i class="fas fa-newspaper fa-3x text-muted mb-3"></i>
                        <p class="text-muted">
                            <a href="${pageContext.request.contextPath}/article/list">点击查看最新文章</a>
                        </p>
                    </div>
                `;
            } catch (error) {
                console.error('[DEBUG] 加载文章失败:', error);
            }
        }

        // 加载分类列表
        async function loadCategories() {
            try {
                // 简化处理：显示静态分类
                document.getElementById('categoryList').innerHTML = `
                    <li>
                        <a href="${pageContext.request.contextPath}/article/category?id=1">
                            技术分享 <span class="category-count">0</span>
                        </a>
                    </li>
                    <li>
                        <a href="${pageContext.request.contextPath}/article/category?id=2">
                            生活随笔 <span class="category-count">0</span>
                        </a>
                    </li>
                    <li>
                        <a href="${pageContext.request.contextPath}/article/category?id=3">
                            学习笔记 <span class="category-count">0</span>
                        </a>
                    </li>
                    <li>
                        <a href="${pageContext.request.contextPath}/article/category?id=4">
                            项目实战 <span class="category-count">0</span>
                        </a>
                    </li>
                `;
            } catch (error) {
                console.error('[DEBUG] 加载分类失败:', error);
            }
        }

        // 更新在线人数
        function updateOnlineCount() {
            const onlineCount = ${applicationScope.onlineCount != null ? applicationScope.onlineCount : 0};
            document.getElementById('onlineCount').textContent = onlineCount;
        }
    </script>
</body>
</html>
