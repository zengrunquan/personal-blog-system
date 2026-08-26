package com.blog.api;

import com.blog.api.dto.UserDto;
import com.blog.api.response.ApiResponse;
import com.blog.api.response.PageResult;
import com.blog.api.servlet.BaseApiServlet;
import com.blog.api.support.DtoMapper;
import com.blog.entity.User;
import com.google.gson.Gson;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import org.junit.Test;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ApiContractTest {

    private final Gson gson = new Gson();

    @Test
    public void successResponseShouldFollowStableEnvelope() {
        ApiResponse<String> response = ApiResponse.success("ok", "完成");

        String json = gson.toJson(response);

        assertTrue(json.contains("\"success\":true"));
        assertTrue(json.contains("\"data\":\"ok\""));
        assertTrue(json.contains("\"message\":\"完成\""));
    }

    @Test
    public void errorResponseShouldExposeCodeAndFieldErrors() {
        ApiResponse<Void> response = ApiResponse.error(
                "VALIDATION_ERROR",
                "参数校验失败",
                Collections.singletonMap("title", "标题不能为空")
        );

        String json = gson.toJson(response);

        assertTrue(json.contains("\"success\":false"));
        assertTrue(json.contains("\"code\":\"VALIDATION_ERROR\""));
        assertTrue(json.contains("\"title\":\"标题不能为空\""));
    }

    @Test
    public void userDtoShouldNeverContainPasswordHash() {
        User user = new User();
        user.setId(7);
        user.setUsername("writer");
        user.setPassword("$2a$10$sensitive-hash");
        user.setNickname("写作者");
        user.setRole(0);

        UserDto dto = DtoMapper.toUserDto(user);
        String json = gson.toJson(dto);

        assertEquals(Integer.valueOf(7), dto.getId());
        assertFalse(json.toLowerCase().contains("password"));
        assertFalse(json.contains("sensitive-hash"));
    }

    @Test
    public void userDtoShouldExposeProfileBiography() throws Exception {
        User user = new User();
        user.setId(7);
        user.setUsername("writer");
        user.setBio("写代码，也认真记录生活。");

        UserDto dto = DtoMapper.toUserDto(user);

        assertEquals("写代码，也认真记录生活。", dto.getBio());
    }

    @Test
    public void pageResultShouldCalculateTotalPages() {
        PageResult<String> page = new PageResult<>(Collections.singletonList("first"), 2, 10, 21);

        assertEquals(3, page.getTotalPages());
        assertEquals(2, page.getPage());
    }

    @Test
    public void anonymousSessionShouldKeepExplicitNullUserField() throws IOException {
        JsonObject data = new JsonObject();
        data.addProperty("authenticated", false);
        data.add("user", JsonNull.INSTANCE);
        HttpServletResponse response = mock(HttpServletResponse.class);
        StringWriter output = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(output));

        new TestApiServlet().write(response, data);
        String json = output.toString();

        assertTrue(json, json.contains("\"user\":null"));
    }

    private static final class TestApiServlet extends BaseApiServlet {
        private void write(HttpServletResponse response, Object data) throws IOException {
            writeSuccess(response, data);
        }
    }

}
