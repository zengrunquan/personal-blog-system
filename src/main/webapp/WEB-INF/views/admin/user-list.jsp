<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>用户管理 - 个人博客</title>
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
                    <a class="nav-link active" href="${pageContext.request.contextPath}/admin/users">
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
                <h4 class="mb-4">用户管理</h4>

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
                                    <th>用户名</th>
                                    <th>昵称</th>
                                    <th>邮箱</th>
                                    <th>角色</th>
                                    <th>状态</th>
                                    <th>注册时间</th>
                                    <th>操作</th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:forEach items="${users}" var="user">
                                    <tr>
                                        <td>${user.id}</td>
                                        <td>${user.username}</td>
                                        <td>${user.nickname}</td>
                                        <td>${user.email}</td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${user.role == 1}">
                                                    <span class="badge bg-danger">管理员</span>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="badge bg-primary">普通用户</span>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${user.status == 1}">
                                                    <span class="badge bg-success">正常</span>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="badge bg-secondary">禁用</span>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td>${user.createTime}</td>
                                        <td>
                                            <c:if test="${user.id != sessionScope.loginUser.id}">
                                                <c:choose>
                                                    <c:when test="${user.status == 1}">
                                                        <button class="btn btn-sm btn-outline-warning" onclick="toggleStatus(${user.id}, 0)">禁用</button>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <button class="btn btn-sm btn-outline-success" onclick="toggleStatus(${user.id}, 1)">启用</button>
                                                    </c:otherwise>
                                                </c:choose>
                                            </c:if>
                                        </td>
                                    </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </div>

                <!-- 分页 -->
                <c:if test="${pageUtil.totalPages > 1}">
                    <nav class="mt-4">
                        <ul class="pagination justify-content-center">
                            <li class="page-item ${!pageUtil.hasPrevious() ? 'disabled' : ''}">
                                <a class="page-link" href="?page=${pageUtil.previousPage}">上一页</a>
                            </li>
                            <c:forEach begin="1" end="${pageUtil.totalPages}" var="i">
                                <li class="page-item ${pageUtil.currentPage == i ? 'active' : ''}">
                                    <a class="page-link" href="?page=${i}">${i}</a>
                                </li>
                            </c:forEach>
                            <li class="page-item ${!pageUtil.hasNext() ? 'disabled' : ''}">
                                <a class="page-link" href="?page=${pageUtil.nextPage}">下一页</a>
                            </li>
                        </ul>
                    </nav>
                </c:if>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <script src="${pageContext.request.contextPath}/static/js/main.js"></script>
    <script>
        function toggleStatus(userId, status) {
            var action = status === 0 ? '禁用' : '启用';
            if (!confirm('确定要' + action + '该用户吗？')) {
                return;
            }

            fetch('${pageContext.request.contextPath}/admin/user/status', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: 'id=' + userId + '&status=' + status
            })
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    showToast('用户状态已更新', 'success');
                    setTimeout(function() {
                        location.reload();
                    }, 1000);
                } else {
                    showToast(data.message || '操作失败', 'danger');
                }
            })
            .catch(error => {
                showToast('操作失败: ' + error.message, 'danger');
            });
        }
    </script>
</body>
</html>
