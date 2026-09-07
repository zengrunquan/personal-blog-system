package com.blog.util;

/** 事务失败统一包装，保留原始异常供服务层记录完整堆栈。 */
public class TransactionException extends RuntimeException {

    public TransactionException(Throwable cause) {
        super("数据库事务执行失败", cause);
    }
}
