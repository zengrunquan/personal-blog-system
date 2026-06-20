-- ================================================
-- 个人博客系统 - 数据库初始化脚本
-- 请在MySQL中执行此脚本
-- ================================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS personal_blog DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE personal_blog;

-- ================================================
-- 1. 用户表
-- ================================================
DROP TABLE IF EXISTS `comment`;
DROP TABLE IF EXISTS `article`;
DROP TABLE IF EXISTS `category`;
DROP TABLE IF EXISTS `user`;

CREATE TABLE `user` (
    `id` INT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名（唯一）',
    `password` VARCHAR(64) NOT NULL COMMENT '密码（MD5加密）',
    `nickname` VARCHAR(50) NOT NULL COMMENT '昵称',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像路径',
    `role` TINYINT NOT NULL DEFAULT 0 COMMENT '角色：0-普通用户，1-管理员',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-正常',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ================================================
-- 2. 分类表
-- ================================================
CREATE TABLE `category` (
    `id` INT PRIMARY KEY AUTO_INCREMENT COMMENT '分类ID',
    `name` VARCHAR(50) NOT NULL UNIQUE COMMENT '分类名称（唯一）',
    `description` VARCHAR(200) DEFAULT NULL COMMENT '分类描述',
    `sort_order` INT DEFAULT 0 COMMENT '排序顺序',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章分类表';

-- ================================================
-- 3. 文章表
-- ================================================
CREATE TABLE `article` (
    `id` INT PRIMARY KEY AUTO_INCREMENT COMMENT '文章ID',
    `title` VARCHAR(200) NOT NULL COMMENT '文章标题',
    `content` TEXT NOT NULL COMMENT '文章内容',
    `summary` VARCHAR(500) DEFAULT NULL COMMENT '文章摘要',
    `cover_image` VARCHAR(255) DEFAULT NULL COMMENT '封面图片路径',
    `user_id` INT NOT NULL COMMENT '作者ID',
    `category_id` INT NOT NULL COMMENT '分类ID',
    `view_count` INT DEFAULT 0 COMMENT '浏览次数',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-草稿，1-已发布',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`category_id`) REFERENCES `category`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章表';

-- ================================================
-- 4. 评论表
-- ================================================
CREATE TABLE `comment` (
    `id` INT PRIMARY KEY AUTO_INCREMENT COMMENT '评论ID',
    `content` TEXT NOT NULL COMMENT '评论内容',
    `user_id` INT NOT NULL COMMENT '评论用户ID',
    `article_id` INT NOT NULL COMMENT '文章ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`article_id`) REFERENCES `article`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论表';

-- ================================================
-- 插入测试数据
-- ================================================

-- 管理员账号（密码：admin123，MD5加密）
INSERT INTO `user` (`username`, `password`, `nickname`, `email`, `role`) VALUES
('admin', '0192023a7bbd73250516f069df18b500', '管理员', 'admin@blog.com', 1);

-- 普通用户账号（密码：user123，MD5加密）
INSERT INTO `user` (`username`, `password`, `nickname`, `email`, `role`) VALUES
('zhangsan', '6ad14ba9986e3615423dfca256d04e3f', '张三', 'zhangsan@example.com', 0),
('lisi', '6ad14ba9986e3615423dfca256d04e3f', '李四', 'lisi@example.com', 0);

-- 文章分类
INSERT INTO `category` (`name`, `description`, `sort_order`) VALUES
('技术分享', '编程技术相关的文章', 1),
('生活随笔', '日常生活感悟', 2),
('学习笔记', '学习过程中的笔记和总结', 3),
('项目实战', '实际项目开发经验', 4);

-- 文章数据
INSERT INTO `article` (`title`, `content`, `summary`, `user_id`, `category_id`, `view_count`, `status`) VALUES
('JavaWeb开发入门指南', '本文将介绍JavaWeb开发的基础知识，包括Servlet、JSP、JDBC等核心技术的使用方法和最佳实践...', 'JavaWeb开发入门指南，适合初学者阅读', 1, 1, 128, 1),
('MySQL数据库优化技巧', '数据库优化是提升应用性能的关键。本文将分享一些MySQL数据库优化的实用技巧...', 'MySQL数据库优化的实用技巧分享', 1, 1, 256, 1),
('我的2026年上半年总结', '时光飞逝，转眼间2026年已经过去一半。在这半年里，我学到了很多东西...', '2026年上半年个人总结与感悟', 2, 2, 89, 1),
('Servlet学习笔记', 'Servlet是JavaWeb的核心技术之一。本文记录了我学习Servlet的过程和心得...', 'Servlet学习过程中的笔记和总结', 2, 3, 167, 1),
('个人博客系统开发记录', '这个个人博客系统是我在JavaWeb课程中的期末项目。下面我将记录开发过程中遇到的问题和解决方案...', '个人博客系统项目开发全过程记录', 3, 4, 203, 1);

-- 评论数据
INSERT INTO `comment` (`content`, `user_id`, `article_id`) VALUES
('写得很好，对我很有帮助！', 2, 1),
('请问有更详细的示例代码吗？', 3, 1),
('这些优化技巧很实用，收藏了！', 2, 2),
('总结得很全面，加油！', 1, 3),
('笔记整理得很清晰，谢谢分享！', 3, 4);

-- ================================================
-- 创建索引（提升查询性能）
-- ================================================
CREATE INDEX idx_article_user ON `article`(`user_id`);
CREATE INDEX idx_article_category ON `article`(`category_id`);
CREATE INDEX idx_article_status ON `article`(`status`);
CREATE INDEX idx_article_create_time ON `article`(`create_time`);
CREATE INDEX idx_comment_article ON `comment`(`article_id`);
CREATE INDEX idx_comment_user ON `comment`(`user_id`);

SELECT '✅ 数据库初始化完成！' AS message;
