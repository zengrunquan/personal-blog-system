package com.blog.util;

public interface TransactionManager {

    <T> T inTransaction(TransactionWork<T> work);
}
