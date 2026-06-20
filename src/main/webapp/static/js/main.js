/**
 * 个人博客系统 - 主JavaScript文件
 * 包含通用的工具函数和交互逻辑
 *
 * 设计原则：
 * - 所有交互都有即时反馈
 * - 表单验证在blur时触发，不是keystroke
 * - 按钮在异步操作时显示加载状态
 * - Toast消息3-5秒自动消失
 *
 * @author blog-system
 */

// ================================================
// 工具函数
// ================================================

/**
 * 显示Toast消息
 * 符合UX规范：aria-live="polite"确保屏幕阅读器能读到
 * @param {string} message - 消息内容
 * @param {string} type - 消息类型：success, error, warning, info
 * @param {number} duration - 显示时长（毫秒），默认3秒
 */
function showToast(message, type = 'success', duration = 3000) {
    // 创建Toast容器（如果不存在）
    let container = document.querySelector('.toast-container');
    if (!container) {
        container = document.createElement('div');
        container.className = 'toast-container';
        container.setAttribute('aria-live', 'polite');
        container.setAttribute('aria-atomic', 'true');
        document.body.appendChild(container);
    }

    // 创建Toast元素
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.setAttribute('role', 'status');

    // 图标 - 使用SVG确保一致性
    let icon = '';
    switch (type) {
        case 'success':
            icon = '<svg class="toast-icon" width="20" height="20" viewBox="0 0 20 20" fill="none"><circle cx="10" cy="10" r="10" fill="#059669"/><path d="M6 10l3 3 5-5" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>';
            break;
        case 'error':
            icon = '<svg class="toast-icon" width="20" height="20" viewBox="0 0 20 20" fill="none"><circle cx="10" cy="10" r="10" fill="#dc2626"/><path d="M7 7l6 6M13 7l-6 6" stroke="white" stroke-width="2" stroke-linecap="round"/></svg>';
            break;
        case 'warning':
            icon = '<svg class="toast-icon" width="20" height="20" viewBox="0 0 20 20" fill="none"><circle cx="10" cy="10" r="10" fill="#d97706"/><path d="M10 6v5M10 13v1" stroke="white" stroke-width="2" stroke-linecap="round"/></svg>';
            break;
        default:
            icon = '<svg class="toast-icon" width="20" height="20" viewBox="0 0 20 20" fill="none"><circle cx="10" cy="10" r="10" fill="#0891b2"/><path d="M10 6v1M10 9v5" stroke="white" stroke-width="2" stroke-linecap="round"/></svg>';
    }

    toast.innerHTML = `${icon}<span class="toast-message">${escapeHtml(message)}</span>`;

    // 添加关闭按钮
    const closeBtn = document.createElement('button');
    closeBtn.className = 'toast-close';
    closeBtn.setAttribute('aria-label', '关闭');
    closeBtn.innerHTML = '<svg width="14" height="14" viewBox="0 0 14 14" fill="none"><path d="M3 3l8 8M11 3l-8 8" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>';
    closeBtn.onclick = () => removeToast(toast, container);
    toast.appendChild(closeBtn);

    container.appendChild(toast);

    // 自动消失 - 3-5秒符合UX规范
    const timer = setTimeout(() => {
        removeToast(toast, container);
    }, duration);

    // 鼠标悬停时暂停自动消失
    toast.addEventListener('mouseenter', () => {
        clearTimeout(timer);
    });

    toast.addEventListener('mouseleave', () => {
        setTimeout(() => removeToast(toast, container), 1000);
    });
}

/**
 * 移除Toast
 */
function removeToast(toast, container) {
    toast.classList.add('toast-exit');
    setTimeout(() => {
        toast.remove();
        if (container && container.children.length === 0) {
            container.remove();
        }
    }, 200);
}

/**
 * HTML转义 - 防止XSS
 */
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

/**
 * 确认对话框 - 用于删除等危险操作
 * @param {string} message - 提示信息
 * @returns {Promise<boolean>} - 用户选择结果
 */
function confirmDialog(message) {
    return new Promise((resolve) => {
        // 使用原生confirm，未来可替换为自定义模态框
        const result = confirm(message);
        resolve(result);
    });
}

/**
 * 发送Ajax请求
 * @param {string} url - 请求URL
 * @param {object} options - 请求选项
 * @returns {Promise<object>} - 响应数据
 */
async function ajax(url, options = {}) {
    const defaultOptions = {
        method: 'GET',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
        },
    };

    const mergedOptions = { ...defaultOptions, ...options };

    try {
        const response = await fetch(url, mergedOptions);

        // 检查响应状态
        if (!response.ok) {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }

        const data = await response.json();
        return data;
    } catch (error) {
        console.error('[DEBUG] Ajax请求失败:', error);
        showToast('网络请求失败，请稍后重试', 'error');
        throw error;
    }
}

/**
 * 发送POST请求
 * @param {string} url - 请求URL
 * @param {object} data - 请求数据
 * @returns {Promise<object>} - 响应数据
 */
async function post(url, data = {}) {
    const formData = new URLSearchParams(data).toString();
    return ajax(url, {
        method: 'POST',
        body: formData,
    });
}

/**
 * 表单序列化
 * @param {HTMLFormElement} form - 表单元素
 * @returns {object} - 表单数据对象
 */
function serializeForm(form) {
    const formData = new FormData(form);
    const data = {};
    for (let [key, value] of formData.entries()) {
        data[key] = value;
    }
    return data;
}

// ================================================
// 按钮加载状态管理
// 符合UX规范：禁用按钮并显示加载指示器
// ================================================

/**
 * 设置按钮为加载状态
 * @param {HTMLButtonElement} btn - 按钮元素
 * @param {string} loadingText - 加载时的文本
 */
function setButtonLoading(btn, loadingText = '处理中...') {
    btn.disabled = true;
    btn.dataset.originalText = btn.innerHTML;
    btn.innerHTML = `
        <svg class="btn-spinner" width="16" height="16" viewBox="0 0 16 16" fill="none">
            <circle cx="8" cy="8" r="7" stroke="currentColor" stroke-width="2" opacity="0.25"/>
            <path d="M8 1a7 7 0 017 7" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
        </svg>
        <span>${loadingText}</span>
    `;
}

/**
 * 恢复按钮原始状态
 * @param {HTMLButtonElement} btn - 按钮元素
 */
function restoreButton(btn) {
    btn.disabled = false;
    if (btn.dataset.originalText) {
        btn.innerHTML = btn.dataset.originalText;
        delete btn.dataset.originalText;
    }
}

// ================================================
// 表单验证
// 符合UX规范：在blur时验证，不是keystroke
// ================================================

/**
 * 验证用户名格式
 * @param {string} username - 用户名
 * @returns {object} - 验证结果 {valid, message}
 */
function validateUsername(username) {
    if (!username || username.trim() === '') {
        return { valid: false, message: '用户名不能为空' };
    }
    if (username.length < 3 || username.length > 20) {
        return { valid: false, message: '用户名长度必须在3-20个字符之间' };
    }
    if (!/^[a-zA-Z0-9_]+$/.test(username)) {
        return { valid: false, message: '用户名只能包含字母、数字和下划线' };
    }
    return { valid: true, message: '' };
}

/**
 * 验证密码格式
 * @param {string} password - 密码
 * @returns {object} - 验证结果 {valid, message}
 */
function validatePassword(password) {
    if (!password || password.trim() === '') {
        return { valid: false, message: '密码不能为空' };
    }
    if (password.length < 6 || password.length > 20) {
        return { valid: false, message: '密码长度必须在6-20个字符之间' };
    }
    return { valid: true, message: '' };
}

/**
 * 验证邮箱格式
 * @param {string} email - 邮箱
 * @returns {object} - 验证结果 {valid, message}
 */
function validateEmail(email) {
    if (!email || email.trim() === '') {
        return { valid: true, message: '' }; // 邮箱可以为空
    }
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
        return { valid: false, message: '邮箱格式不正确' };
    }
    return { valid: true, message: '' };
}

/**
 * 设置表单字段验证
 * 在blur时验证，不是keystroke（符合UX规范）
 * @param {HTMLInputElement} input - 输入元素
 * @param {Function} validateFn - 验证函数
 * @param {object} options - 选项
 */
function setupFieldValidation(input, validateFn, options = {}) {
    const { showOnInput = false } = options;
    let feedbackElement = input.parentNode.querySelector('.invalid-feedback');

    // 如果没有反馈元素，创建一个
    if (!feedbackElement) {
        feedbackElement = document.createElement('div');
        feedbackElement.className = 'invalid-feedback';
        input.parentNode.appendChild(feedbackElement);
    }

    // blur时验证（主要验证时机）
    input.addEventListener('blur', function() {
        if (this.value.trim() !== '' || this.dataset.required) {
            const result = validateFn(this.value);
            updateFieldState(this, feedbackElement, result);
        }
    });

    // input时清除错误状态（如果之前有错误）
    input.addEventListener('input', function() {
        if (this.classList.contains('is-invalid')) {
            const result = validateFn(this.value);
            if (result.valid) {
                updateFieldState(this, feedbackElement, result);
            }
        }
    });
}

/**
 * 更新字段状态
 */
function updateFieldState(input, feedbackElement, result) {
    if (result.valid) {
        input.classList.remove('is-invalid');
        input.classList.add('is-valid');
        feedbackElement.textContent = '';
        feedbackElement.style.display = 'none';
        input.removeAttribute('aria-invalid');
        input.removeAttribute('aria-describedby');
    } else {
        input.classList.remove('is-valid');
        input.classList.add('is-invalid');
        feedbackElement.textContent = result.message;
        feedbackElement.style.display = 'block';
        input.setAttribute('aria-invalid', 'true');
        input.setAttribute('aria-describedby', feedbackElement.id || '');
    }
}

/**
 * 验证整个表单
 * @param {HTMLFormElement} form - 表单元素
 * @returns {boolean} - 是否通过验证
 */
function validateForm(form) {
    let isValid = true;
    const inputs = form.querySelectorAll('input[required], textarea[required], select[required]');

    inputs.forEach(input => {
        let validateFn;
        switch (input.type) {
            case 'text':
                if (input.name === 'username') validateFn = validateUsername;
                break;
            case 'password':
                validateFn = validatePassword;
                break;
            case 'email':
                validateFn = validateEmail;
                break;
        }

        if (validateFn) {
            const result = validateFn(input.value);
            const feedbackElement = input.parentNode.querySelector('.invalid-feedback');
            if (feedbackElement) {
                updateFieldState(input, feedbackElement, result);
            }
            if (!result.valid) isValid = false;
        } else if (input.value.trim() === '') {
            isValid = false;
            input.classList.add('is-invalid');
        }
    });

    // 聚焦到第一个无效字段（符合UX规范）
    if (!isValid) {
        const firstInvalid = form.querySelector('.is-invalid');
        if (firstInvalid) {
            firstInvalid.focus();
        }
    }

    return isValid;
}

// ================================================
// 表格排序功能
// ================================================

/**
 * 初始化表格排序
 * @param {string} tableId - 表格ID
 */
function initTableSort(tableId) {
    const table = document.getElementById(tableId);
    if (!table) return;

    const headers = table.querySelectorAll('th[data-sortable]');
    headers.forEach(header => {
        header.style.cursor = 'pointer';
        header.setAttribute('role', 'columnheader');
        header.setAttribute('aria-sort', 'none');

        // 添加排序图标
        const icon = document.createElement('span');
        icon.className = 'sort-icon';
        icon.innerHTML = '↕';
        icon.setAttribute('aria-hidden', 'true');
        header.appendChild(icon);

        // 键盘支持
        header.setAttribute('tabindex', '0');
        header.addEventListener('keydown', (e) => {
            if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                header.click();
            }
        });

        header.addEventListener('click', function() {
            const column = this.dataset.column;
            const type = this.dataset.type || 'string';
            const currentOrder = this.dataset.order || 'asc';
            const newOrder = currentOrder === 'asc' ? 'desc' : 'asc';

            // 更新所有表头的排序状态
            headers.forEach(h => {
                h.dataset.order = '';
                h.setAttribute('aria-sort', 'none');
                h.querySelector('.sort-icon').textContent = '↕';
            });

            // 更新当前表头
            this.dataset.order = newOrder;
            this.setAttribute('aria-sort', newOrder === 'asc' ? 'ascending' : 'descending');
            this.querySelector('.sort-icon').textContent = newOrder === 'asc' ? '↑' : '↓';

            // 排序表格
            sortTable(table, column, type, newOrder);
        });
    });
}

/**
 * 排序表格
 * @param {HTMLTableElement} table - 表格元素
 * @param {string} column - 列名
 * @param {string} type - 数据类型
 * @param {string} order - 排序顺序
 */
function sortTable(table, column, type, order) {
    const tbody = table.querySelector('tbody');
    const rows = Array.from(tbody.querySelectorAll('tr'));
    const columnIndex = Array.from(table.querySelectorAll('th')).findIndex(th => th.dataset.column === column);

    rows.sort((a, b) => {
        let valueA = a.cells[columnIndex].textContent.trim();
        let valueB = b.cells[columnIndex].textContent.trim();

        // 根据类型转换值
        if (type === 'number') {
            valueA = parseFloat(valueA) || 0;
            valueB = parseFloat(valueB) || 0;
        } else if (type === 'date') {
            valueA = new Date(valueA).getTime() || 0;
            valueB = new Date(valueB).getTime() || 0;
        }

        // 比较
        if (valueA < valueB) return order === 'asc' ? -1 : 1;
        if (valueA > valueB) return order === 'asc' ? 1 : -1;
        return 0;
    });

    // 重新插入排序后的行
    rows.forEach(row => tbody.appendChild(row));
}

// ================================================
// 文件上传预览
// ================================================

/**
 * 设置图片上传预览
 * @param {string} inputId - 文件输入框ID
 * @param {string} previewId - 预览图片ID
 */
function setupImagePreview(inputId, previewId) {
    const input = document.getElementById(inputId);
    const preview = document.getElementById(previewId);

    if (!input || !preview) return;

    input.addEventListener('change', function() {
        const file = this.files[0];
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
                preview.src = e.target.result;
                preview.style.display = 'block';
            };
            reader.readAsDataURL(file);
        }
    });
}

// ================================================
// 页面加载完成后执行
// ================================================

document.addEventListener('DOMContentLoaded', function() {
    console.log('[DEBUG] 页面加载完成');

    // 初始化Bootstrap工具提示
    const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
    tooltipTriggerList.map(function(tooltipTriggerEl) {
        return new bootstrap.Tooltip(tooltipTriggerEl);
    });

    // 自动隐藏Alert消息（3-5秒符合UX规范）
    const alerts = document.querySelectorAll('.alert-dismissible');
    alerts.forEach(alert => {
        setTimeout(() => {
            alert.style.transition = 'opacity 0.3s ease, transform 0.3s ease';
            alert.style.opacity = '0';
            alert.style.transform = 'translateY(-10px)';
            setTimeout(() => alert.remove(), 300);
        }, 4000);
    });

    // 初始化表格排序
    initTableSort('dataTable');

    // 初始化图片上传预览
    setupImagePreview('avatarInput', 'avatarPreview');
    setupImagePreview('coverInput', 'coverPreview');

    // 为所有表单添加提交时的加载状态
    document.querySelectorAll('form').forEach(form => {
        form.addEventListener('submit', function(e) {
            const submitBtn = this.querySelector('button[type="submit"]');
            if (submitBtn && !submitBtn.disabled) {
                setButtonLoading(submitBtn);
            }
        });
    });
});

// ================================================
// 添加动态样式
// ================================================

const dynamicStyles = document.createElement('style');
dynamicStyles.textContent = `
    /* Toast样式 */
    .toast-container {
        position: fixed;
        top: calc(60px + 1rem);
        right: 1rem;
        z-index: 9999;
        display: flex;
        flex-direction: column;
        gap: 0.75rem;
        pointer-events: none;
    }

    .toast {
        background: white;
        border-radius: 0.5rem;
        box-shadow: 0 10px 15px -3px rgb(0 0 0 / 0.1), 0 4px 6px -4px rgb(0 0 0 / 0.1);
        padding: 1rem 1.25rem;
        display: flex;
        align-items: center;
        gap: 0.75rem;
        min-width: 300px;
        max-width: 400px;
        pointer-events: auto;
        animation: toastSlideIn 0.3s cubic-bezier(0.4, 0, 0.2, 1);
        border-left: 4px solid transparent;
    }

    .toast.success { border-left-color: #059669; }
    .toast.error { border-left-color: #dc2626; }
    .toast.warning { border-left-color: #d97706; }
    .toast.info { border-left-color: #0891b2; }

    .toast-icon {
        flex-shrink: 0;
    }

    .toast-message {
        flex: 1;
        font-size: 0.875rem;
        line-height: 1.5;
        color: #1e293b;
    }

    .toast-close {
        flex-shrink: 0;
        background: none;
        border: none;
        padding: 0.25rem;
        cursor: pointer;
        color: #94a3b8;
        border-radius: 0.25rem;
        transition: all 0.15s ease;
    }

    .toast-close:hover {
        color: #475569;
        background-color: #f1f5f9;
    }

    .toast-exit {
        animation: toastSlideOut 0.2s ease forwards;
    }

    @keyframes toastSlideIn {
        from {
            transform: translateX(100%);
            opacity: 0;
        }
        to {
            transform: translateX(0);
            opacity: 1;
        }
    }

    @keyframes toastSlideOut {
        from {
            transform: translateX(0);
            opacity: 1;
        }
        to {
            transform: translateX(100%);
            opacity: 0;
        }
    }

    /* 按钮加载状态 */
    .btn-spinner {
        animation: spin 0.8s linear infinite;
    }

    @keyframes spin {
        to { transform: rotate(360deg); }
    }

    /* 表单验证状态 */
    .is-valid {
        border-color: #059669 !important;
    }

    .is-invalid {
        border-color: #dc2626 !important;
    }

    .invalid-feedback {
        display: none;
        color: #dc2626;
        font-size: 0.75rem;
        margin-top: 0.25rem;
    }

    /* 焦点状态 - 无障碍 */
    *:focus-visible {
        outline: 2px solid #2563eb;
        outline-offset: 2px;
    }

    /* 减少动画偏好 */
    @media (prefers-reduced-motion: reduce) {
        .toast,
        .toast-exit {
            animation: none !important;
        }
    }
`;
document.head.appendChild(dynamicStyles);
