# 个人博客系统

JavaWeb课程期末项目 - 基于Servlet+JSP+JDBC的个人博客系统

## 项目简介

这是一个功能完整的个人博客系统，采用B/S架构，使用JavaWeb核心技术开发。支持用户注册登录、文章管理、分类管理、评论互动等功能，并实现了管理员后台管理。

## 技术栈

### 后端技术
- **Servlet 4.0** - 控制层
- **JSP + EL + JSTL** - 视图层
- **JDBC + PreparedStatement** - 数据访问层
- **Druid连接池** - 数据库连接管理
- **Log4j2** - 日志管理
- **Filter** - 编码过滤、登录拦截、权限控制
- **Listener** - 在线用户统计
- **Session/Cookie** - 会话管理

### 前端技术
- **HTML5 + CSS3** - 页面结构和样式
- **JavaScript** - 前端交互（原生ES6+）
- **Bootstrap 5** - 响应式UI框架
- **Font Awesome 6** - 图标库
- **Fetch API** - 异步数据交互

### 数据库
- **MySQL 8.0** - 数据存储

## 功能特性

### 用户功能
- 用户注册（用户名唯一性实时检查）
- 用户登录/注销（"记住我"功能）
- 个人信息管理
- 头像上传（支持jpg/png/gif，最大5MB）
- 修改密码
- 文章发布/编辑/删除
- 文章搜索和分类筛选
- 评论互动

### 管理员功能
- 用户管理（查看/禁用/删除）
- 文章管理（查看/删除/批量删除）
- 分类管理（增删改查，支持排序）
- 数据导出（CSV格式）
- 数据统计面板

### 技术特性
- MVC三层架构（Controller → Service → DAO）
- 权限控制（普通用户/管理员，Filter实现）
- 文件上传（头像/封面/附件，自动创建目录）
- 分页查询（自定义PageUtil工具类）
- 多表关联查询
- 事务管理（分类删除级联清理评论）
- XSS防护（输入转义 + 输出转义）
- CSRF防护（Token验证）
- 密码MD5加密
- 响应式设计（适配桌面/平板/手机）

## 项目结构

```
personal-blog-system/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/blog/
│       │       ├── controller/    # 控制层（Servlet）
│       │       ├── service/       # 服务层（业务逻辑）
│       │       │   └── impl/      # 服务实现类
│       │       ├── dao/           # 数据访问层（JDBC）
│       │       │   └── impl/      # DAO实现类
│       │       ├── entity/        # 实体类
│       │       ├── filter/        # 过滤器
│       │       ├── listener/      # 监听器
│       │       └── util/          # 工具类
│       ├── resources/
│       │   ├── db.properties      # 数据库配置
│       │   └── log4j2.xml         # 日志配置
│       └── webapp/
│           ├── WEB-INF/
│           │   ├── web.xml        # Web配置
│           │   └── views/         # JSP页面（受保护）
│           │       ├── user/      # 用户相关页面
│           │       ├── article/   # 文章相关页面
│           │       └── admin/     # 管理后台页面
│           ├── static/
│           │   ├── css/style.css  # 全局样式
│           │   └── js/main.js     # 全局JS工具函数
│           ├── uploads/           # 上传文件目录（运行时自动创建）
│           │   ├── avatars/       # 用户头像
│           │   ├── covers/        # 文章封面
│           │   └── files/         # 其他附件
│           ├── index.jsp          # 首页
│           ├── login.jsp          # 登录页
│           └── register.jsp       # 注册页
├── init-database.sql              # 数据库建表与测试数据
├── pom.xml                        # Maven配置
└── README.md                      # 项目说明
```

## 运行环境

- JDK 11+
- Maven 3.6+
- MySQL 8.0+
- Tomcat 9.0+

## 快速开始

### 1. 克隆项目
```bash
git clone https://gitee.com/Rquan_Zeng/personal-blog-system.git
```

### 2. 创建数据库
```bash
mysql -u root -p < init-database.sql
```

### 3. 修改数据库配置
编辑 `src/main/resources/db.properties`，修改数据库连接信息：
```properties
druid.url=jdbc:mysql://localhost:3306/personal_blog?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
druid.username=your_username
druid.password=your_password
```

### 4. 运行项目

**方式一：IDEA中使用Tomcat（推荐）**
1. 在IDEA中配置Tomcat（Run → Edit Configurations → + → Tomcat Server → Local）
2. 设置Deployment为 `personal-blog-system:war exploded`
3. 设置Application context为 `/personal_blog_system_war_exploded`
4. 点击运行

**方式二：使用Maven Tomcat插件**
```bash
mvn tomcat7:run
```
访问：http://localhost:8080

**方式三：打包部署到Tomcat**
```bash
mvn clean package
```
将 `target/personal-blog.war` 复制到Tomcat的 `webapps/` 目录

### 5. 测试账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | admin | admin123 |
| 普通用户 | zhangsan | user123 |
| 普通用户 | lisi | user123 |

## 页面路由

| URL | 页面 | 说明 |
|-----|------|------|
| `/` | 首页 | 最新文章、分类、站点统计 |
| `/login.jsp` | 登录页 | 用户登录 |
| `/register.jsp` | 注册页 | 用户注册 |
| `/article/list` | 文章列表 | 分页展示、分类筛选、搜索 |
| `/article/detail?id=x` | 文章详情 | 文章内容、评论 |
| `/user/profile` | 个人信息 | 头像上传、资料查看 |
| `/user/edit` | 编辑资料 | 修改昵称、邮箱 |
| `/user/password` | 修改密码 | 修改登录密码 |
| `/user/articles` | 我的文章 | 个人文章管理 |
| `/admin/dashboard` | 管理后台 | 数据统计面板 |
| `/admin/users` | 用户管理 | 管理员操作 |
| `/admin/articles` | 文章管理 | 管理员操作 |
| `/admin/categories` | 分类管理 | 管理员操作 |

## 已修复的问题

项目开发过程中发现并修复了以下问题：

1. **头像显示过大** - `EncodingFilter` 对所有请求设置了 `Content-Type: text/html`，导致CSS文件因MIME类型不匹配无法加载
2. **文章编辑失效** - 编辑表单缺少CSRF Token隐藏字段
3. **批量删除报错** - 前后端参数格式不匹配（逗号分隔 vs 多参数）
4. **XSS漏洞** - 搜索关键词和URL参数未转义
5. **管理后台分页失效** - EL表达式变量名与后端不一致
6. **分类排序无效** - 后端未读取排序参数
7. **删除分类级联异常** - 未先删除文章评论导致外键约束错误
8. **登录/注册页样式异常** - CSS变量名引用错误
9. **Log4j警告** - 缺少日志实现依赖
10. **过滤器重复执行** - `@WebFilter` 注解与 `web.xml` 声明共存导致
11. **SQL文件密码错误** - `database.sql` 中密码哈希值与注释不匹配，已替换为修正版 `init-database.sql`
12. **NPE空指针风险** - `MD5Util.verify()`、`ArticleDaoImpl.update()` 等多处缺少null检查
13. **资源泄漏风险** - 事务方法中 `finally` 块的资源关闭逻辑可能被跳过
14. **SQL通配符转义** - 搜索功能未转义 `%` 和 `_` 通配符
15. **分页参数校验** - `page <= 0` 时会产生负偏移量导致SQL报错
16. **Controller层NPE** - `AdminServlet`、`UserServlet` 等多处未检查Session中的用户对象
17. **文章ID验证缺失** - `ArticleServiceImpl.update()` 未验证文章ID是否为空
18. **批量删除空数组** - `batchDelete()` 传入空数组时未做校验

## 注意事项

1. 本项目为课程作业，请勿直接抄袭
2. 上传的文件存储在 `webapp/uploads/` 下，重新部署WAR包时会被清空
3. 请勿将数据库密码等敏感信息提交到Git仓库

## 许可证

本项目仅供学习使用
