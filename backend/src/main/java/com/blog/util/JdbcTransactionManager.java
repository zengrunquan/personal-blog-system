package com.blog.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

/** JDBC 事务边界；调用方只提供业务工作，不再重复处理连接提交和回滚。 */
public class JdbcTransactionManager implements TransactionManager {

    private static final Logger LOGGER = LogManager.getLogger(JdbcTransactionManager.class);
    private final DataSource dataSource;

    public JdbcTransactionManager() {
        this(null);
    }

    public JdbcTransactionManager(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public <T> T inTransaction(TransactionWork<T> work) {
        Objects.requireNonNull(work, "transaction work 不能为空");
        Connection connection = null;
        boolean originalAutoCommit = true;
        boolean autoCommitKnown = false;
        try {
            connection = (dataSource == null ? DBUtil.getDataSource() : dataSource).getConnection();
            originalAutoCommit = connection.getAutoCommit();
            autoCommitKnown = true;
            connection.setAutoCommit(false);
            T result = work.execute(connection);
            connection.commit();
            return result;
        } catch (Exception e) {
            rollbackQuietly(connection, e);
            if (e instanceof TransactionException) throw (TransactionException) e;
            throw new TransactionException(e);
        } finally {
            if (connection != null && autoCommitKnown) {
                try {
                    connection.setAutoCommit(originalAutoCommit);
                } catch (SQLException restoreError) {
                    LOGGER.error("[JdbcTransactionManager#inTransaction] 恢复 autoCommit 失败", restoreError);
                }
            }
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException closeError) {
                    LOGGER.error("[JdbcTransactionManager#inTransaction] 关闭数据库连接失败", closeError);
                }
            }
        }
    }

    private void rollbackQuietly(Connection connection, Exception original) {
        if (connection == null) return;
        try {
            connection.rollback();
        } catch (SQLException rollbackError) {
            LOGGER.error(
                    "[JdbcTransactionManager#inTransaction] 回滚失败，保留原始事务异常",
                    rollbackError
            );
            original.addSuppressed(rollbackError);
        }
    }
}
