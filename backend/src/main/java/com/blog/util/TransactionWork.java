package com.blog.util;

import java.sql.Connection;

@FunctionalInterface
public interface TransactionWork<T> {

    T execute(Connection connection) throws Exception;
}
