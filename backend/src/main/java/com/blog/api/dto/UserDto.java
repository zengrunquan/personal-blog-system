package com.blog.api.dto;

import java.util.Date;

public final class UserDto {

    private final Integer id;
    private final String username;
    private final String nickname;
    private final String email;
    private final String avatar;
    private final String bio;
    private final Integer role;
    private final Integer status;
    private final Date createTime;

    public UserDto(
            Integer id,
            String username,
            String nickname,
            String email,
            String avatar,
            String bio,
            Integer role,
            Integer status,
            Date createTime
    ) {
        this.id = id;
        this.username = username;
        this.nickname = nickname;
        this.email = email;
        this.avatar = avatar;
        this.bio = bio;
        this.role = role;
        this.status = status;
        this.createTime = createTime;
    }

    public Integer getId() { return id; }
    public String getUsername() { return username; }
    public String getNickname() { return nickname; }
    public String getEmail() { return email; }
    public String getAvatar() { return avatar; }
    public String getBio() { return bio; }
    public Integer getRole() { return role; }
    public Integer getStatus() { return status; }
    public Date getCreateTime() { return createTime; }
    public boolean isAdmin() { return role != null && role == 1; }
}
