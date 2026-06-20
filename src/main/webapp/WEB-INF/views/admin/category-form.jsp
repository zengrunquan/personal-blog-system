<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${empty category ? '添加' : '编辑'}分类 - 个人博客</title>
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
                <h4 class="mb-4">${empty category ? '添加' : '编辑'}分类</h4>

                <c:if test="${not empty requestScope.error}">
                    <div class="alert alert-danger">${requestScope.error}</div>
                </c:if>

                <div class="card">
                    <div class="card-body">
                        <form action="${pageContext.request.contextPath}/admin/category/${empty category ? 'add' : 'edit'}" method="post">
                            <c:if test="${not empty category}">
                                <input type="hidden" name="id" value="${category.id}">
                            </c:if>

                            <div class="mb-3">
                                <label for="name" class="form-label">分类名称 <span class="text-danger">*</span></label>
                                <input type="text" class="form-control" id="name" name="name" value="${category.name}" required maxlength="50">
                            </div>

                            <div class="mb-3">
                                <label for="description" class="form-label">分类描述</label>
                                <textarea class="form-control" id="description" name="description" rows="3" maxlength="200">${category.description}</textarea>
                            </div>

                            <div class="mb-3">
                                <label for="sortOrder" class="form-label">排序顺序</label>
                                <input type="number" class="form-control" id="sortOrder" name="sortOrder" value="${category.sortOrder}" min="0">
                                <div class="form-text">数值越小越靠前</div>
                            </div>

                            <div class="d-flex gap-2">
                                <button type="submit" class="btn btn-primary">保存</button>
                                <a href="${pageContext.request.contextPath}/admin/categories" class="btn btn-secondary">取消</a>
                            </div>
                        </form>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <script src="${pageContext.request.contextPath}/static/js/main.js"></script>
</body>
</html>
