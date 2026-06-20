<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>分类管理 - 个人博客</title>
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
                    <a class="nav-link" href="${pageContext.request.contextPath}/admin/dashboard">
                        <i class="bi bi-speedometer2"></i> 仪表盘
                    </a>
                    <a class="nav-link" href="${pageContext.request.contextPath}/admin/users">
                        <i class="bi bi-people"></i> 用户管理
                    </a>
                    <a class="nav-link" href="${pageContext.request.contextPath}/admin/articles">
                        <i class="bi bi-file-text"></i> 文章管理
                    </a>
                    <a class="nav-link active" href="${pageContext.request.contextPath}/admin/categories">
                        <i class="bi bi-folder"></i> 分类管理
                    </a>
                </nav>
            </div>

            <!-- 主内容区 -->
            <div class="col-md-10 p-4">
                <div class="d-flex justify-content-between align-items-center mb-4">
                    <h4>分类管理</h4>
                    <a href="${pageContext.request.contextPath}/admin/category/add" class="btn btn-primary">
                        <i class="bi bi-plus"></i> 添加分类
                    </a>
                </div>

                <c:if test="${not empty requestScope.success}">
                    <div class="alert alert-success">${requestScope.success}</div>
                </c:if>
                <c:if test="${not empty requestScope.error}">
                    <div class="alert alert-danger">${requestScope.error}</div>
                </c:if>

                <div class="card">
                    <div class="card-body">
                        <table class="table table-hover">
                            <thead>
                                <tr>
                                    <th>ID</th>
                                    <th>分类名称</th>
                                    <th>描述</th>
                                    <th>排序</th>
                                    <th>文章数</th>
                                    <th>创建时间</th>
                                    <th>操作</th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:forEach items="${categories}" var="category">
                                    <tr>
                                        <td>${category.id}</td>
                                        <td>${category.name}</td>
                                        <td>${category.description}</td>
                                        <td>${category.sortOrder}</td>
                                        <td>
                                            <span class="badge bg-info">${category.articleCount}</span>
                                        </td>
                                        <td>${category.createTime}</td>
                                        <td>
                                            <a href="${pageContext.request.contextPath}/admin/category/edit?id=${category.id}" class="btn btn-sm btn-outline-primary">编辑</a>
                                            <button class="btn btn-sm btn-outline-danger" onclick="deleteCategory(${category.id}, ${category.articleCount})">删除</button>
                                        </td>
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
    <script>
        function deleteCategory(id, articleCount) {
            var message = '确定要删除该分类吗？';
            if (articleCount > 0) {
                message = '该分类下有 ' + articleCount + ' 篇文章，删除分类会同时删除这些文章。确定要删除吗？';
            }

            if (!confirm(message)) {
                return;
            }

            fetch('${pageContext.request.contextPath}/admin/category/delete', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: 'id=' + id
            })
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    showToast('分类已删除', 'success');
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
