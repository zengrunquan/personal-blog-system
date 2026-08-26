package com.blog.entity;

import java.io.Serializable;
import java.util.Date;

/**
 * 文章分类实体类
 *
 * @author blog-system
 */
public class Category implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 分类ID */
    private Integer id;

    /** 分类名称 */
    private String name;

    /** 分类描述 */
    private String description;

    /** 排序顺序 */
    private Integer sortOrder;

    /** 创建时间 */
    private Date createTime;

    /** 该分类下的文章数量（关联查询） */
    private Integer articleCount;

    // 构造方法

    public Category() {
    }

    public Category(String name, String description) {
        this.name = name;
        this.description = description;
        this.sortOrder = 0;
    }

    // Getter和Setter方法

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Integer getArticleCount() {
        return articleCount;
    }

    public void setArticleCount(Integer articleCount) {
        this.articleCount = articleCount;
    }

    @Override
    public String toString() {
        return "Category{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", sortOrder=" + sortOrder +
                '}';
    }
}
