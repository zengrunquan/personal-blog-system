<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>用户注册 - 个人博客系统</title>
    <!-- Bootstrap CSS -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <!-- Font Awesome -->
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css" rel="stylesheet">
    <!-- 自定义CSS -->
    <link href="${pageContext.request.contextPath}/static/css/style.css" rel="stylesheet">
    <style>
        body {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            min-height: 100vh;
            display: flex;
            align-items: center;
            padding: 2rem 0;
        }
        .register-card {
            max-width: 500px;
            margin: 0 auto;
            border-radius: 15px;
            box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
        }
        .register-header {
            background: linear-gradient(135deg, var(--color-success), #218838);
            color: white;
            text-align: center;
            padding: 2rem;
            border-radius: 15px 15px 0 0;
        }
        .register-header h2 {
            margin: 0;
            font-weight: 700;
        }
        .register-body {
            padding: 2rem;
            background: white;
            border-radius: 0 0 15px 15px;
        }
        .form-floating {
            margin-bottom: 1rem;
        }
        .form-floating .form-control {
            border-radius: 10px;
            padding: 1rem 1rem;
            height: auto;
        }
        .btn-register {
            padding: 12px;
            font-size: 1.1rem;
            border-radius: 10px;
            font-weight: 600;
        }
        .password-strength {
            height: 5px;
            margin-top: 0.5rem;
            border-radius: 3px;
            transition: all 0.3s ease;
        }
        .strength-weak { background-color: #dc3545; width: 33%; }
        .strength-medium { background-color: #ffc107; width: 66%; }
        .strength-strong { background-color: #28a745; width: 100%; }
    </style>
</head>
<body>
    <div class="container">
        <div class="card register-card">
            <div class="register-header">
                <h2><i class="fas fa-user-plus me-2"></i>用户注册</h2>
                <p class="mb-0 mt-2">创建您的博客账号</p>
            </div>
            <div class="register-body">
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

                <!-- 注册表单 -->
                <form id="registerForm" action="${pageContext.request.contextPath}/user/register" method="post">
                    <input type="hidden" name="csrfToken" value="${csrfToken}">

                    <div class="form-floating">
                        <input type="text" class="form-control" id="username" name="username"
                               placeholder="请输入用户名" value="${username}" required>
                        <label for="username"><i class="fas fa-user me-2"></i>用户名</label>
                        <div class="invalid-feedback" id="usernameFeedback"></div>
                        <div class="form-text">3-20个字符，只能包含字母、数字和下划线</div>
                    </div>

                    <div class="form-floating">
                        <input type="password" class="form-control" id="password" name="password"
                               placeholder="请输入密码" required>
                        <label for="password"><i class="fas fa-lock me-2"></i>密码</label>
                        <div class="invalid-feedback" id="passwordFeedback"></div>
                        <div class="password-strength" id="passwordStrength"></div>
                        <div class="form-text">6-20个字符</div>
                    </div>

                    <div class="form-floating">
                        <input type="password" class="form-control" id="confirmPassword"
                               placeholder="请确认密码" required>
                        <label for="confirmPassword"><i class="fas fa-lock me-2"></i>确认密码</label>
                        <div class="invalid-feedback" id="confirmFeedback"></div>
                    </div>

                    <div class="form-floating">
                        <input type="text" class="form-control" id="nickname" name="nickname"
                               placeholder="请输入昵称" value="${nickname}" required>
                        <label for="nickname"><i class="fas fa-id-card me-2"></i>昵称</label>
                        <div class="invalid-feedback"></div>
                    </div>

                    <div class="form-floating">
                        <input type="email" class="form-control" id="email" name="email"
                               placeholder="请输入邮箱" value="${email}">
                        <label for="email"><i class="fas fa-envelope me-2"></i>邮箱（选填）</label>
                        <div class="invalid-feedback" id="emailFeedback"></div>
                    </div>

                    <div class="form-check mb-3">
                        <input class="form-check-input" type="checkbox" id="agree" required>
                        <label class="form-check-label" for="agree">
                            我已阅读并同意 <a href="#" data-bs-toggle="modal" data-bs-target="#termsModal">服务条款</a>
                        </label>
                    </div>

                    <button type="submit" class="btn btn-success btn-register w-100">
                        <i class="fas fa-user-plus me-2"></i>立即注册
                    </button>
                </form>

                <div class="text-center mt-3">
                    <span class="text-muted">已有账号？</span>
                    <a href="${pageContext.request.contextPath}/login.jsp">
                        <i class="fas fa-sign-in-alt me-1"></i>立即登录
                    </a>
                </div>

                <div class="text-center mt-2">
                    <a href="${pageContext.request.contextPath}/" class="text-muted">
                        <i class="fas fa-arrow-left me-1"></i>返回首页
                    </a>
                </div>
            </div>
        </div>
    </div>

    <!-- 服务条款模态框 -->
    <div class="modal fade" id="termsModal" tabindex="-1">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title">服务条款</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body">
                    <h6>1. 服务说明</h6>
                    <p>本博客系统为用户提供文章发布、评论互动等服务。</p>
                    <h6>2. 用户责任</h6>
                    <p>用户应遵守相关法律法规，不得发布违法违规内容。</p>
                    <h6>3. 隐私保护</h6>
                    <p>我们将保护用户的个人信息安全。</p>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-primary" data-bs-dismiss="modal">我已了解</button>
                </div>
            </div>
        </div>
    </div>

    <!-- Bootstrap JS -->
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <!-- 自定义JS -->
    <script src="${pageContext.request.contextPath}/static/js/main.js"></script>

    <script>
        // 用户名验证
        const usernameInput = document.getElementById('username');
        const usernameFeedback = document.getElementById('usernameFeedback');
        let usernameTimer;

        usernameInput.addEventListener('input', function() {
            clearTimeout(usernameTimer);
            const username = this.value.trim();

            if (!username) {
                this.classList.remove('is-valid', 'is-invalid');
                return;
            }

            const result = validateUsername(username);
            if (!result.valid) {
                this.classList.remove('is-valid');
                this.classList.add('is-invalid');
                usernameFeedback.textContent = result.message;
                return;
            }

            // 异步检查用户名是否可用
            usernameTimer = setTimeout(async () => {
                try {
                    const response = await fetch('${pageContext.request.contextPath}/user/checkUsername?username=' + encodeURIComponent(username));
                    const data = await response.json();

                    if (data.available) {
                        this.classList.remove('is-invalid');
                        this.classList.add('is-valid');
                        usernameFeedback.textContent = '';
                    } else {
                        this.classList.remove('is-valid');
                        this.classList.add('is-invalid');
                        usernameFeedback.textContent = '用户名已存在';
                    }
                } catch (error) {
                    console.error('[DEBUG] 检查用户名失败:', error);
                }
            }, 500);
        });

        // 密码强度检测
        const passwordInput = document.getElementById('password');
        const passwordStrength = document.getElementById('passwordStrength');

        passwordInput.addEventListener('input', function() {
            const password = this.value;

            if (!password) {
                passwordStrength.className = 'password-strength';
                return;
            }

            let strength = 0;
            if (password.length >= 6) strength++;
            if (/[A-Z]/.test(password) && /[a-z]/.test(password)) strength++;
            if (/[0-9]/.test(password)) strength++;
            if (/[^A-Za-z0-9]/.test(password)) strength++;

            if (strength <= 1) {
                passwordStrength.className = 'password-strength strength-weak';
            } else if (strength <= 2) {
                passwordStrength.className = 'password-strength strength-medium';
            } else {
                passwordStrength.className = 'password-strength strength-strong';
            }
        });

        // 确认密码验证
        const confirmPasswordInput = document.getElementById('confirmPassword');
        const confirmFeedback = document.getElementById('confirmFeedback');

        confirmPasswordInput.addEventListener('input', function() {
            const password = passwordInput.value;
            const confirmPassword = this.value;

            if (!confirmPassword) {
                this.classList.remove('is-valid', 'is-invalid');
                return;
            }

            if (password === confirmPassword) {
                this.classList.remove('is-invalid');
                this.classList.add('is-valid');
                confirmFeedback.textContent = '';
            } else {
                this.classList.remove('is-valid');
                this.classList.add('is-invalid');
                confirmFeedback.textContent = '两次输入的密码不一致';
            }
        });

        // 邮箱验证
        const emailInput = document.getElementById('email');
        const emailFeedback = document.getElementById('emailFeedback');

        emailInput.addEventListener('input', function() {
            const email = this.value.trim();

            if (!email) {
                this.classList.remove('is-valid', 'is-invalid');
                return;
            }

            const result = validateEmail(email);
            if (result.valid) {
                this.classList.remove('is-invalid');
                this.classList.add('is-valid');
                emailFeedback.textContent = '';
            } else {
                this.classList.remove('is-valid');
                this.classList.add('is-invalid');
                emailFeedback.textContent = result.message;
            }
        });

        // 表单提交验证
        document.getElementById('registerForm').addEventListener('submit', function(e) {
            const username = usernameInput.value.trim();
            const password = passwordInput.value;
            const confirmPassword = confirmPasswordInput.value;
            const nickname = document.getElementById('nickname').value.trim();
            const agree = document.getElementById('agree').checked;

            // 验证用户名
            const usernameResult = validateUsername(username);
            if (!usernameResult.valid) {
                e.preventDefault();
                usernameInput.classList.add('is-invalid');
                usernameFeedback.textContent = usernameResult.message;
                return;
            }

            // 验证密码
            const passwordResult = validatePassword(password);
            if (!passwordResult.valid) {
                e.preventDefault();
                passwordInput.classList.add('is-invalid');
                document.getElementById('passwordFeedback').textContent = passwordResult.message;
                return;
            }

            // 验证确认密码
            if (password !== confirmPassword) {
                e.preventDefault();
                confirmPasswordInput.classList.add('is-invalid');
                confirmFeedback.textContent = '两次输入的密码不一致';
                return;
            }

            // 验证昵称
            if (!nickname) {
                e.preventDefault();
                document.getElementById('nickname').classList.add('is-invalid');
                return;
            }

            // 验证服务条款
            if (!agree) {
                e.preventDefault();
                alert('请阅读并同意服务条款');
                return;
            }
        });
    </script>
</body>
</html>
