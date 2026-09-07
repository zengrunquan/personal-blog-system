package com.blog.util;

import org.junit.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JdbcTransactionManagerTest {

    @Test
    public void shouldCommitRestoreAutoCommitAndCloseConnectionAfterSuccess() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getAutoCommit()).thenReturn(true);
        JdbcTransactionManager manager = new JdbcTransactionManager(dataSource);

        String result = manager.inTransaction(conn -> "saved");

        assertEquals("saved", result);
        verify(connection).setAutoCommit(false);
        verify(connection).commit();
        verify(connection).setAutoCommit(true);
        verify(connection).close();
    }

    @Test
    public void shouldRollbackRestoreAutoCommitAndPreserveOriginalFailure() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        SQLException failure = new SQLException("article write failed");
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getAutoCommit()).thenReturn(true);
        JdbcTransactionManager manager = new JdbcTransactionManager(dataSource);

        TransactionException exception = assertThrows(
                TransactionException.class,
                () -> manager.inTransaction(conn -> {
                    throw failure;
                })
        );

        assertSame(failure, exception.getCause());
        verify(connection).rollback();
        verify(connection).setAutoCommit(true);
        verify(connection).close();
    }
}
