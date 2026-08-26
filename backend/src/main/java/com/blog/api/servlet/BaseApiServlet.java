package com.blog.api.servlet;

import com.blog.api.exception.ApiException;
import com.blog.api.response.ApiResponse;
import com.blog.entity.User;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Collections;

public abstract class BaseApiServlet extends HttpServlet {

    protected final Logger logger = LogManager.getLogger(getClass());
    protected final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX")
            .create();

    @FunctionalInterface
    protected interface ApiAction {
        void run() throws Exception;
    }

    protected void execute(HttpServletRequest request, HttpServletResponse response, ApiAction action)
            throws IOException {
        try {
            action.run();
        } catch (ApiException e) {
            writeError(response, e.getStatus(), e.getCode(), e.getMessage(), e.getFieldErrors());
        } catch (Exception e) {
            logger.error("[{}#execute] API 请求处理失败，method={}，uri={}",
                    getClass().getSimpleName(), request.getMethod(), request.getRequestURI(), e);
            writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "INTERNAL_ERROR", "服务暂时不可用，请稍后重试", null);
        }
    }

    protected <T> T readJson(HttpServletRequest request, Class<T> type) {
        try {
            T body = gson.fromJson(request.getReader(), type);
            if (body == null) {
                throw badRequest("INVALID_JSON", "请求内容不能为空");
            }
            return body;
        } catch (JsonParseException | IOException e) {
            throw badRequest("INVALID_JSON", "请求内容不是有效的 JSON");
        }
    }

    protected void writeSuccess(HttpServletResponse response, Object data) throws IOException {
        writeSuccess(response, data, null);
    }

    protected void writeSuccess(HttpServletResponse response, Object data, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(successJson(data, message));
        response.getWriter().flush();
    }

    protected void writeCreated(HttpServletResponse response, Object data, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_CREATED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(successJson(data, message));
        response.getWriter().flush();
    }

    private String successJson(Object data, String message) {
        JsonObject envelope = new JsonObject();
        envelope.addProperty("success", true);
        JsonElement dataElement = data instanceof JsonElement
                ? (JsonElement) data
                : data == null ? JsonNull.INSTANCE : gson.toJsonTree(data);
        envelope.add("data", dataElement);
        if (message != null) envelope.addProperty("message", message);
        // JsonObject 自身负责转义，同时保留业务契约中显式声明的 null 字段。
        return envelope.toString();
    }

    protected void writeError(
            HttpServletResponse response,
            int status,
            String code,
            String message,
            java.util.Map<String, String> fieldErrors
    ) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(gson.toJson(ApiResponse.error(code, message, fieldErrors)));
        response.getWriter().flush();
    }

    protected ApiException badRequest(String code, String message) {
        return new ApiException(HttpServletResponse.SC_BAD_REQUEST, code, message);
    }

    protected ApiException validation(String field, String message) {
        return new ApiException(
                HttpServletResponse.SC_BAD_REQUEST,
                "VALIDATION_ERROR",
                "参数校验失败",
                Collections.singletonMap(field, message)
        );
    }

    protected ApiException notFound(String message) {
        return new ApiException(HttpServletResponse.SC_NOT_FOUND, "NOT_FOUND", message);
    }

    protected ApiException forbidden(String message) {
        return new ApiException(HttpServletResponse.SC_FORBIDDEN, "FORBIDDEN", message);
    }

    protected User requireUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Object user = session == null ? null : session.getAttribute("loginUser");
        if (!(user instanceof User)) {
            throw new ApiException(HttpServletResponse.SC_UNAUTHORIZED,
                    "AUTH_REQUIRED", "登录状态已失效，请重新登录");
        }
        return (User) user;
    }

    protected int intQuery(HttpServletRequest request, String name, int defaultValue, int min, int max) {
        String value = request.getParameter(name);
        if (value == null || value.isBlank()) return defaultValue;
        try {
            return Math.min(Math.max(Integer.parseInt(value), min), max);
        } catch (NumberFormatException e) {
            throw validation(name, name + " 必须是整数");
        }
    }

    protected int positiveId(String value, String field) {
        try {
            int id = Integer.parseInt(value);
            if (id <= 0) throw new NumberFormatException();
            return id;
        } catch (NumberFormatException e) {
            throw validation(field, field + " 必须是正整数");
        }
    }

    protected String pathInfo(HttpServletRequest request) {
        String path = request.getPathInfo();
        return path == null || path.isBlank() ? "/" : path;
    }
}
