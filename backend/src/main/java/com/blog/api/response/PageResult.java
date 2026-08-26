package com.blog.api.response;

import java.util.List;

public final class PageResult<T> {

    private final List<T> items;
    private final int page;
    private final int pageSize;
    private final int total;
    private final int totalPages;

    public PageResult(List<T> items, int page, int pageSize, int total) {
        this.items = items;
        this.page = Math.max(page, 1);
        this.pageSize = Math.max(pageSize, 1);
        this.total = Math.max(total, 0);
        this.totalPages = this.total == 0 ? 0 : (int) Math.ceil((double) this.total / this.pageSize);
    }

    public List<T> getItems() {
        return items;
    }

    public int getPage() {
        return page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public int getTotal() {
        return total;
    }

    public int getTotalPages() {
        return totalPages;
    }
}
