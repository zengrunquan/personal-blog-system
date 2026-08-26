package com.blog.api.support;

import com.blog.api.dto.ArticleDto;
import com.blog.api.dto.CategoryDto;
import com.blog.api.dto.CommentDto;
import com.blog.api.dto.UserDto;
import com.blog.entity.Article;
import com.blog.entity.Category;
import com.blog.entity.Comment;
import com.blog.entity.User;

import java.util.List;
import java.util.stream.Collectors;

public final class DtoMapper {

    private DtoMapper() {
    }

    public static UserDto toUserDto(User user) {
        if (user == null) return null;
        // Entity 包含密码哈希，显式映射 DTO 可以从结构上阻断敏感字段泄漏。
        return new UserDto(
                user.getId(), user.getUsername(), user.getNickname(), user.getEmail(),
                user.getAvatar(), user.getBio(), user.getRole(), user.getStatus(), user.getCreateTime()
        );
    }

    public static ArticleDto toArticleDto(Article article) {
        if (article == null) return null;
        ArticleDto dto = new ArticleDto();
        dto.id = article.getId();
        dto.title = article.getTitle();
        dto.content = article.getContent();
        dto.summary = article.getSummary();
        dto.coverImage = article.getCoverImage();
        dto.userId = article.getUserId();
        dto.categoryId = article.getCategoryId();
        dto.viewCount = article.getViewCount();
        dto.status = article.getStatus();
        dto.createTime = article.getCreateTime();
        dto.updateTime = article.getUpdateTime();
        dto.authorName = article.getAuthorName();
        dto.authorNickname = article.getAuthorNickname();
        dto.authorAvatar = article.getAuthorAvatar();
        dto.categoryName = article.getCategoryName();
        dto.commentCount = article.getCommentCount();
        return dto;
    }

    public static CategoryDto toCategoryDto(Category category) {
        if (category == null) return null;
        CategoryDto dto = new CategoryDto();
        dto.id = category.getId();
        dto.name = category.getName();
        dto.description = category.getDescription();
        dto.sortOrder = category.getSortOrder();
        dto.createTime = category.getCreateTime();
        dto.articleCount = category.getArticleCount();
        return dto;
    }

    public static CommentDto toCommentDto(Comment comment) {
        if (comment == null) return null;
        CommentDto dto = new CommentDto();
        dto.id = comment.getId();
        dto.content = comment.getContent();
        dto.userId = comment.getUserId();
        dto.articleId = comment.getArticleId();
        dto.createTime = comment.getCreateTime();
        dto.userNickname = comment.getUserNickname();
        dto.userAvatar = comment.getUserAvatar();
        dto.username = comment.getUsername();
        return dto;
    }

    public static List<ArticleDto> toArticleDtos(List<Article> articles) {
        return articles.stream().map(DtoMapper::toArticleDto).collect(Collectors.toList());
    }

    public static List<CategoryDto> toCategoryDtos(List<Category> categories) {
        return categories.stream().map(DtoMapper::toCategoryDto).collect(Collectors.toList());
    }

    public static List<CommentDto> toCommentDtos(List<Comment> comments) {
        return comments.stream().map(DtoMapper::toCommentDto).collect(Collectors.toList());
    }

    public static List<UserDto> toUserDtos(List<User> users) {
        return users.stream().map(DtoMapper::toUserDto).collect(Collectors.toList());
    }
}
