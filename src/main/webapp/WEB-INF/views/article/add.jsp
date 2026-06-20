<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>发布文章 - 个人博客系统</title>
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
            <div class="collapse navbar-collapse">
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
                <ul class="navbar-nav">
                    <li class="nav-item dropdown">
                        <a class="nav-link dropdown-toggle" href="#" role="button" data-bs-toggle="dropdown">
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
                            <li><a class="dropdown-item" href="${pageContext.request.contextPath}/user/profile"><i class="fas fa-user me-2"></i>个人信息</a></li>
                            <li><a class="dropdown-item" href="${pageContext.request.contextPath}/user/articles"><i class="fas fa-file-alt me-2"></i>我的文章</a></li>
                            <li><hr class="dropdown-divider"></li>
                            <li><a class="dropdown-item text-danger" href="${pageContext.request.contextPath}/user/logout"><i class="fas fa-sign-out-alt me-2"></i>退出登录</a></li>
                        </ul>
                    </li>
                </ul>
            </div>
        </div>
    </nav>

    <!-- 主内容 -->
    <div class="main-content">
        <div class="container">
            <div class="row justify-content-center">
                <div class="col-lg-10">
                    <div class="card">
                        <div class="card-header">
                            <h4 class="mb-0"><i class="fas fa-edit me-2"></i>发布新文章</h4>
                        </div>
                        <div class="card-body">
                            <!-- 消息提示 -->
                            <c:if test="${not empty error}">
                                <div class="alert alert-danger alert-dismissible fade show" role="alert">
                                    <i class="fas fa-exclamation-circle me-2"></i>${error}
                                    <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                                </div>
                            </c:if>
                            <c:if test="${not empty success}">
                                <div class="alert alert-success alert-dismissible fade show" role="alert">
                                    <i class="fas fa-check-circle me-2"></i>${success}
                                    <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                                </div>
                            </c:if>

                            <form id="articleForm" action="${pageContext.request.contextPath}/article/add" method="post" enctype="multipart/form-data">
                                <input type="hidden" name="csrfToken" value="${csrfToken}">

                                <div class="mb-3">
                                    <label for="title" class="form-label">文章标题 <span class="text-danger">*</span></label>
                                    <input type="text" class="form-control" id="title" name="title"
                                           placeholder="请输入文章标题" value="${article.title}" required maxlength="200">
                                    <div class="form-text">标题长度不超过200个字符</div>
                                </div>

                                <div class="row mb-3">
                                    <div class="col-md-6">
                                        <label for="categoryId" class="form-label">文章分类 <span class="text-danger">*</span></label>
                                        <select class="form-select" id="categoryId" name="categoryId" required>
                                            <option value="">请选择分类</option>
                                            <c:forEach items="${categories}" var="category">
                                                <option value="${category.id}" ${article.categoryId == category.id ? 'selected' : ''}>
                                                        ${category.name}
                                                </option>
                                            </c:forEach>
                                        </select>
                                    </div>
                                    <div class="col-md-6">
                                        <label for="status" class="form-label">发布状态</label>
                                        <select class="form-select" id="status" name="status">
                                            <option value="1" ${article.status == 1 ? 'selected' : ''}>立即发布</option>
                                            <option value="0" ${article.status == 0 ? 'selected' : ''}>保存为草稿</option>
                                        </select>
                                    </div>
                                </div>

                                <div class="mb-3">
                                    <label for="summary" class="form-label">文章摘要</label>
                                    <textarea class="form-control" id="summary" name="summary" rows="3"
                                              placeholder="请输入文章摘要（选填，不填将自动截取内容前200字）">${article.summary}</textarea>
                                    <div class="form-text">摘要会显示在文章列表中，建议填写</div>
                                </div>

                                <div class="mb-3">
                                    <label for="coverInput" class="form-label">封面图片</label>
                                    <input type="file" class="form-control" id="coverInput" name="coverImage" accept="image/*">
                                    <div class="form-text">支持jpg、png、gif格式，建议尺寸 800x400</div>
                                    <div class="mt-2">
                                        <img id="coverPreview" src="" alt="封面预览" style="max-width: 300px; display: none;" class="rounded">
                                    </div>
                                </div>

                                <div class="mb-3">
                                    <label for="content" class="form-label">文章内容 <span class="text-danger">*</span></label>
                                    <textarea class="form-control" id="content" name="content" rows="15"
                                              placeholder="请输入文章内容（支持HTML格式）" required>${article.content}</textarea>
                                    <div class="form-text">支持HTML格式，可以使用标签美化文章</div>
                                </div>

                                <div class="mb-3">
                                    <label for="fileUpload" class="form-label">附件上传</label>
                                    <input type="file" class="form-control" id="fileUpload" name="files" multiple>
                                    <div class="form-text">可以上传多个附件（单个文件最大10MB）</div>
                                </div>

                                <div class="d-flex justify-content-between">
                                    <a href="${pageContext.request.contextPath}/user/articles" class="btn btn-outline-secondary">
                                        <i class="fas fa-arrow-left me-2"></i>返回
                                    </a>
                                    <div>
                                        <button type="submit" class="btn btn-primary me-2">
                                            <i class="fas fa-paper-plane me-2"></i>发布文章
                                        </button>
                                    </div>
                                </div>
                            </form>
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
        // 封面图片预览
        document.getElementById('coverInput').addEventListener('change', function(e) {
            const file = e.target.files[0];
            if (file) {
                // 验证文件类型
                if (!file.type.startsWith('image/')) {
                    showToast('请选择图片文件', 'error');
                    this.value = '';
                    return;
                }

                // 验证文件大小（5MB）
                if (file.size > 5 * 1024 * 1024) {
                    showToast('图片大小不能超过5MB', 'error');
                    this.value = '';
                    return;
                }

                // 预览
                const reader = new FileReader();
                reader.onload = function(e) {
                    const preview = document.getElementById('coverPreview');
                    preview.src = e.target.result;
                    preview.style.display = 'block';
                };
                reader.readAsDataURL(file);
            }
        });

        // 表单验证
        document.getElementById('articleForm').addEventListener('submit', function(e) {
            const title = document.getElementById('title').value.trim();
            const categoryId = document.getElementById('categoryId').value;
            const content = document.getElementById('content').value.trim();

            if (!title) {
                e.preventDefault();
                showToast('请输入文章标题', 'warning');
                return;
            }

            if (!categoryId) {
                e.preventDefault();
                showToast('请选择文章分类', 'warning');
                return;
            }

            if (!content) {
                e.preventDefault();
                showToast('请输入文章内容', 'warning');
                return;
            }
        });
    </script>
</body>
</html>
