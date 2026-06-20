<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>文章管理 - 个人博客</title>
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
                    <a class="nav-link active" href="${pageContext.request.contextPath}/admin/articles">
                        <i class="bi bi-file-text"></i> 文章管理
                    </a>
                    <a class="nav-link" href="${pageContext.request.contextPath}/admin/categories">
                        <i class="bi bi-folder"></i> 分类管理
                    </a>
                </nav>
            </div>

            <!-- 主内容区 -->
            <div class="col-md-10 p-4">
                <h4 class="mb-4">文章管理</h4>

                <c:if test="${not empty requestScope.success}">
                    <div class="alert alert-success">${requestScope.success}</div>
                </c:if>

                <div class="card">
                    <div class="card-body">
                        <table class="table table-hover">
                            <thead>
                                <tr>
                                    <th><input type="checkbox" id="selectAll"></th>
                                    <th>ID</th>
                                    <th>标题</th>
                                    <th>作者</th>
                                    <th>分类</th>
                                    <th>状态</th>
                                    <th>浏览</th>
                                    <th>发布时间</th>
                                    <th>操作</th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:forEach items="${articles}" var="article">
                                    <tr>
                                        <td><input type="checkbox" class="article-checkbox" value="${article.id}"></td>
                                        <td>${article.id}</td>
                                        <td>
                                            <a href="${pageContext.request.contextPath}/article/detail?id=${article.id}" target="_blank">
                                                ${article.title}
                                            </a>
                                        </td>
                                        <td>${article.authorNickname}</td>
                                        <td>${article.categoryName}</td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${article.status == 1}">
                                                    <span class="badge bg-success">已发布</span>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="badge bg-secondary">草稿</span>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td>${article.viewCount}</td>
                                        <td>${article.createTime}</td>
                                        <td>
                                            <a href="${pageContext.request.contextPath}/article/edit?id=${article.id}" class="btn btn-sm btn-outline-primary">编辑</a>
                                            <button class="btn btn-sm btn-outline-danger" onclick="deleteArticle(${article.id})">删除</button>
                                        </td>
                                    </tr>
                                </c:forEach>
                            </tbody>
                        </table>

                        <!-- 批量操作 -->
                        <div class="mt-3">
                            <button class="btn btn-danger btn-sm" onclick="batchDelete()">批量删除</button>
                            <a href="${pageContext.request.contextPath}/article/export" class="btn btn-success btn-sm">
                                <i class="bi bi-download"></i> 导出CSV
                            </a>
                        </div>
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
        // 全选/取消全选
        document.getElementById('selectAll').addEventListener('change', function() {
            var checkboxes = document.querySelectorAll('.article-checkbox');
            checkboxes.forEach(function(checkbox) {
                checkbox.checked = this.checked;
            }.bind(this));
        });

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

        function batchDelete() {
            var checked = document.querySelectorAll('.article-checkbox:checked');
            if (checked.length === 0) {
                showToast('请选择要删除的文章', 'warning');
                return;
            }

            if (!confirm('确定要删除选中的 ' + checked.length + ' 篇文章吗？此操作不可恢复。')) {
                return;
            }

            var ids = Array.from(checked).map(function(cb) {
                return cb.value;
            });

            fetch('${pageContext.request.contextPath}/article/batchDelete', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: 'ids=' + ids.join(',')
            })
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    showToast('批量删除成功', 'success');
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
