<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>管理后台 - 个人博客</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.0/font/bootstrap-icons.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/static/css/style.css" rel="stylesheet">
    <style>
        .sidebar {
            min-height: calc(100vh - 56px);
            background-color: #343a40;
        }
        .sidebar .nav-link {
            color: #adb5bd;
            padding: 12px 20px;
        }
        .sidebar .nav-link:hover,
        .sidebar .nav-link.active {
            color: #fff;
            background-color: #495057;
        }
        .sidebar .nav-link i {
            margin-right: 10px;
        }
        .stat-card {
            transition: transform 0.2s;
        }
        .stat-card:hover {
            transform: translateY(-5px);
        }
    </style>
</head>
<body>
    <!-- 导航栏 -->
    <nav class="navbar navbar-expand-lg navbar-dark bg-dark">
        <div class="container-fluid">
            <a class="navbar-brand" href="${pageContext.request.contextPath}/admin/dashboard">
                <i class="bi bi-gear"></i> 博客管理后台
            </a>
            <div class="navbar-nav ms-auto">
                <a class="nav-link" href="${pageContext.request.contextPath}/" target="_blank">
                    <i class="bi bi-house"></i> 访问前台
                </a>
                <a class="nav-link" href="${pageContext.request.contextPath}/user/logout">
                    <i class="bi bi-box-arrow-right"></i> 退出登录
                </a>
            </div>
        </div>
    </nav>

    <div class="container-fluid">
        <div class="row">
            <!-- 侧边栏 -->
            <div class="col-md-2 sidebar p-0">
                <nav class="nav flex-column">
                    <a class="nav-link active" href="${pageContext.request.contextPath}/admin/dashboard">
                        <i class="bi bi-speedometer2"></i> 仪表盘
                    </a>
                    <a class="nav-link" href="${pageContext.request.contextPath}/admin/users">
                        <i class="bi bi-people"></i> 用户管理
                    </a>
                    <a class="nav-link" href="${pageContext.request.contextPath}/admin/articles">
                        <i class="bi bi-file-text"></i> 文章管理
                    </a>
                    <a class="nav-link" href="${pageContext.request.contextPath}/admin/categories">
                        <i class="bi bi-folder"></i> 分类管理
                    </a>
                </nav>
            </div>

            <!-- 主内容区 -->
            <div class="col-md-10 p-4">
                <h4 class="mb-4">仪表盘</h4>

                <!-- 统计卡片 -->
                <div class="row mb-4">
                    <div class="col-md-3">
                        <div class="card stat-card bg-primary text-white">
                            <div class="card-body">
                                <div class="d-flex justify-content-between align-items-center">
                                    <div>
                                        <h6 class="card-title">用户总数</h6>
                                        <h2>${userCount}</h2>
                                    </div>
                                    <i class="bi bi-people display-4"></i>
                                </div>
                            </div>
                        </div>
                    </div>
                    <div class="col-md-3">
                        <div class="card stat-card bg-success text-white">
                            <div class="card-body">
                                <div class="d-flex justify-content-between align-items-center">
                                    <div>
                                        <h6 class="card-title">文章总数</h6>
                                        <h2>${articleCount}</h2>
                                    </div>
                                    <i class="bi bi-file-text display-4"></i>
                                </div>
                            </div>
                        </div>
                    </div>
                    <div class="col-md-3">
                        <div class="card stat-card bg-info text-white">
                            <div class="card-body">
                                <div class="d-flex justify-content-between align-items-center">
                                    <div>
                                        <h6 class="card-title">分类总数</h6>
                                        <h2>${categoryCount}</h2>
                                    </div>
                                    <i class="bi bi-folder display-4"></i>
                                </div>
                            </div>
                        </div>
                    </div>
                    <div class="col-md-3">
                        <div class="card stat-card bg-warning text-white">
                            <div class="card-body">
                                <div class="d-flex justify-content-between align-items-center">
                                    <div>
                                        <h6 class="card-title">评论总数</h6>
                                        <h2>${commentCount}</h2>
                                    </div>
                                    <i class="bi bi-chat display-4"></i>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- 最新文章 -->
                <div class="card">
                    <div class="card-header d-flex justify-content-between align-items-center">
                        <h5 class="mb-0">最新文章</h5>
                        <a href="${pageContext.request.contextPath}/admin/articles" class="btn btn-sm btn-outline-primary">查看全部</a>
                    </div>
                    <div class="card-body">
                        <table class="table table-hover">
                            <thead>
                                <tr>
                                    <th>标题</th>
                                    <th>作者</th>
                                    <th>分类</th>
                                    <th>浏览</th>
                                    <th>发布时间</th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:forEach items="${recentArticles}" var="article">
                                    <tr>
                                        <td>
                                            <a href="${pageContext.request.contextPath}/article/detail?id=${article.id}" target="_blank">
                                                ${article.title}
                                            </a>
                                        </td>
                                        <td>${article.authorNickname}</td>
                                        <td>${article.categoryName}</td>
                                        <td>${article.viewCount}</td>
                                        <td>${article.createTime}</td>
                                    </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <script src="${pageContext.request.contextPath}/static/js/main.js"></script>
</body>
</html>
