package com.blog.api.servlet;

import com.blog.api.response.PageResult;
import com.blog.api.support.DtoMapper;
import com.blog.api.upload.UploadStorage;
import com.blog.entity.User;
import com.blog.service.ArticleService;
import com.blog.service.UserService;
import com.blog.service.impl.ArticleServiceImpl;
import com.blog.service.impl.UserServiceImpl;

import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.IOException;
import java.util.Map;

@WebServlet(urlPatterns = {"/api/me", "/api/me/*"})
@MultipartConfig(maxFileSize = 5L * 1024 * 1024, maxRequestSize = 6L * 1024 * 1024)
public class MeApiServlet extends BaseApiServlet {

    private final UserService userService = new UserServiceImpl();
    private final ArticleService articleService = new ArticleServiceImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        execute(request, response, () -> {
            User loginUser = requireUser(request);
            if ("/articles".equals(pathInfo(request))) {
                int page = intQuery(request, "page", 1, 1, Integer.MAX_VALUE);
                int pageSize = intQuery(request, "pageSize", 10, 1, 50);
                writeSuccess(response, new PageResult<>(
                        DtoMapper.toArticleDtos(articleService.findByUserId(loginUser.getId(), page, pageSize)),
                        page,
                        pageSize,
                        articleService.getCountByUserId(loginUser.getId())
                ));
                return;
            }
            if (!"/".equals(pathInfo(request))) throw notFound("用户中心接口不存在");
            User current = userService.findById(loginUser.getId());
            if (current == null) throw notFound("用户不存在");
            writeSuccess(response, DtoMapper.toUserDto(current));
        });
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        execute(request, response, () -> {
            User loginUser = requireUser(request);
            if ("/password".equals(pathInfo(request))) {
                PasswordRequest body = readJson(request, PasswordRequest.class);
                String result = userService.changePassword(
                        loginUser.getId(), body.oldPassword, body.newPassword
                );
                if (result != null) throw validation("oldPassword", result);
                writeSuccess(response, Map.of("changed", true), "密码修改成功");
                return;
            }
            if (!"/".equals(pathInfo(request))) throw notFound("用户中心接口不存在");
            ProfileRequest body = readJson(request, ProfileRequest.class);
            if (body.nickname == null || body.nickname.isBlank()) throw validation("nickname", "昵称不能为空");
            if (body.email == null || !body.email.contains("@")) throw validation("email", "请输入有效邮箱");
            String bio = body.bio == null ? null : body.bio.trim();
            if (bio != null && bio.length() > 200) throw validation("bio", "个人寄语不能超过200个字符");
            User current = userService.findById(loginUser.getId());
            if (current == null) throw notFound("用户不存在");
            current.setNickname(body.nickname.trim());
            current.setEmail(body.email.trim());
            // 空寄语不占用数据库空间，展示层会统一回退到系统默认文案。
            current.setBio(bio == null || bio.isEmpty() ? null : bio);
            if (!userService.updateProfile(current)) throw badRequest("UPDATE_FAILED", "资料更新失败");
            request.getSession().setAttribute("loginUser", current);
            writeSuccess(response, DtoMapper.toUserDto(current), "资料已更新");
        });
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        execute(request, response, () -> {
            User user = requireUser(request);
            if (!"/avatar".equals(pathInfo(request))) throw notFound("用户中心接口不存在");
            Part part = request.getPart("file");
            if (part == null) throw validation("file", "请选择头像图片");
            try {
                UploadStorage.UploadResult uploaded = UploadStorage.saveImage(
                        part, "avatars", request, getServletContext()
                );
                if (!userService.updateAvatar(user.getId(), uploaded.url)) {
                    throw badRequest("UPDATE_FAILED", "头像更新失败");
                }
                user.setAvatar(uploaded.url);
                request.getSession().setAttribute("loginUser", user);
                writeCreated(response, uploaded, "头像已更新");
            } catch (IllegalArgumentException e) {
                throw validation("file", e.getMessage());
            }
        });
    }

    private static final class ProfileRequest {
        private String nickname;
        private String email;
        private String bio;
    }

    private static final class PasswordRequest {
        private String oldPassword;
        private String newPassword;
    }
}
