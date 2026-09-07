package com.blog.media.maintenance;

/** 回填所需的最小用户媒体字段，避免维护查询加载页面无关数据。 */
public final class UserMediaSource {

    private final int id;
    private final String avatar;

    public UserMediaSource(int id, String avatar) {
        this.id = id;
        this.avatar = avatar;
    }

    public int getId() {
        return id;
    }

    public String getAvatar() {
        return avatar;
    }
}
