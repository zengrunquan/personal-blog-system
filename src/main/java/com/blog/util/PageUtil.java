package com.blog.util;

/**
 * 分页工具类
 * 封装分页相关的参数和计算逻辑
 *
 * @author blog-system
 */
public class PageUtil {

    /** 当前页码（从1开始） */
    private int currentPage;

    /** 每页显示数量 */
    private int pageSize;

    /** 总记录数 */
    private int totalCount;

    /** 总页数 */
    private int totalPages;

    /** 起始索引 */
    private int startIndex;

    /**
     * 构造方法
     *
     * @param currentPage 当前页码
     * @param pageSize    每页数量
     * @param totalCount  总记录数
     */
    public PageUtil(int currentPage, int pageSize, int totalCount) {
        this.currentPage = Math.max(1, currentPage);
        this.pageSize = Math.max(1, pageSize);
        this.totalCount = Math.max(0, totalCount);

        // 计算总页数
        this.totalPages = (this.totalCount + this.pageSize - 1) / this.pageSize;

        // 确保当前页不超过总页数
        if (this.totalPages > 0) {
            this.currentPage = Math.min(this.currentPage, this.totalPages);
        }

        // 计算起始索引
        this.startIndex = (this.currentPage - 1) * this.pageSize;
    }

    /**
     * 是否有上一页
     */
    public boolean hasPrevious() {
        return currentPage > 1;
    }

    /**
     * 是否有下一页
     */
    public boolean hasNext() {
        return currentPage < totalPages;
    }

    /**
     * 获取上一页页码
     */
    public int getPreviousPage() {
        return Math.max(1, currentPage - 1);
    }

    /**
     * 获取下一页页码
     */
    public int getNextPage() {
        return Math.min(totalPages, currentPage + 1);
    }

    // Getter和Setter方法

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public int getStartIndex() {
        return startIndex;
    }

    public void setStartIndex(int startIndex) {
        this.startIndex = startIndex;
    }

    @Override
    public String toString() {
        return "PageUtil{" +
                "currentPage=" + currentPage +
                ", pageSize=" + pageSize +
                ", totalCount=" + totalCount +
                ", totalPages=" + totalPages +
                ", startIndex=" + startIndex +
                '}';
    }
}
