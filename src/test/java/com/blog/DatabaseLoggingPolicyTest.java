package com.blog;

import com.blog.util.DBUtil;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DatabaseLoggingPolicyTest {

    private static final List<Path> DATABASE_SOURCE_PATHS = Arrays.asList(
            Paths.get("src/main/java/com/blog/dao/impl/ArticleDaoImpl.java"),
            Paths.get("src/main/java/com/blog/dao/impl/CategoryDaoImpl.java"),
            Paths.get("src/main/java/com/blog/dao/impl/CommentDaoImpl.java"),
            Paths.get("src/main/java/com/blog/dao/impl/UserDaoImpl.java"),
            Paths.get("src/main/java/com/blog/util/DBUtil.java")
    );
    private static final Pattern LOGGER_ERROR_CALL = Pattern.compile(
            "LOGGER\\.error\\((.*?)\\);",
            Pattern.DOTALL
    );

    @Test
    public void databaseClassesShouldUseContextualLog4jErrors() throws Exception {
        for (Path sourcePath : DATABASE_SOURCE_PATHS) {
            String source = Files.readString(sourcePath);
            String className = sourcePath.getFileName().toString().replace(".java", "");

            assertFalse(className + " 不得继续直接打印异常堆栈",
                    source.contains("printStackTrace()"));
            assertTrue(className + " 必须导入 LogManager",
                    source.contains("import org.apache.logging.log4j.LogManager;"));
            assertTrue(className + " 必须导入 Logger",
                    source.contains("import org.apache.logging.log4j.Logger;"));
            assertTrue(className + " 必须声明类级 Logger",
                    source.contains("private static final Logger LOGGER = LogManager.getLogger("
                            + className + ".class);"));

            Matcher matcher = LOGGER_ERROR_CALL.matcher(source);
            int errorLogCount = 0;
            while (matcher.find()) {
                String invocation = matcher.group(1);
                errorLogCount++;
                assertTrue(className + " 的错误日志必须包含类名和方法名",
                        invocation.contains("\"[" + className + "#"));
                assertTrue(className + " 的错误日志必须把异常作为最后一个参数",
                        invocation.matches("(?s).*,\\s*(e|ex)\\s*$"));
            }
            assertTrue(className + " 至少需要一条错误日志", errorLogCount > 0);
        }
    }

    @Test
    public void dbUtilCloseShouldLogEachResourceFailureWithThrowable() {
        CapturingAppender appender = new CapturingAppender();
        Logger logger = (Logger) LogManager.getLogger(DBUtil.class);
        Level originalLevel = logger.getLevel();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.ERROR);

        try {
            Connection connection = closeFailureProxy(Connection.class, "connection-close");
            PreparedStatement statement = closeFailureProxy(
                    PreparedStatement.class,
                    "statement-close"
            );
            ResultSet resultSet = closeFailureProxy(ResultSet.class, "result-set-close");

            DBUtil.close(connection, statement, resultSet);

            assertEquals("三个资源关闭失败必须分别记录", 3, appender.events.size());
            assertResourceFailure(appender.events.get(0), "ResultSet", "result-set-close");
            assertResourceFailure(appender.events.get(1), "PreparedStatement", "statement-close");
            assertResourceFailure(appender.events.get(2), "Connection", "connection-close");
        } finally {
            logger.removeAppender(appender);
            logger.setLevel(originalLevel);
            appender.stop();
        }
    }

    @Test
    public void readmeShouldDocumentUnifiedDatabaseErrorLogging() throws Exception {
        String readme = Files.readString(Paths.get("README.md"));

        assertTrue("README 必须说明统一数据库错误日志",
                readme.contains("统一数据库错误日志"));
        assertTrue("README 必须说明 DAO 和 DBUtil 的日志范围",
                readme.contains("DAO") && readme.contains("DBUtil"));
        assertTrue("README 必须说明日志包含操作上下文和异常堆栈",
                readme.contains("操作上下文") && readme.contains("异常堆栈"));
    }

    private void assertResourceFailure(
            LogEvent event,
            String resourceType,
            String exceptionMessage
    ) {
        assertTrue(event.getMessage().getFormattedMessage().contains("[DBUtil#close]"));
        assertTrue(event.getMessage().getFormattedMessage().contains(resourceType));
        assertNotNull("日志事件必须保留异常对象", event.getThrown());
        assertTrue(event.getThrown() instanceof SQLException);
        assertEquals(exceptionMessage, event.getThrown().getMessage());
    }

    @SuppressWarnings("unchecked")
    private <T> T closeFailureProxy(Class<T> resourceType, String exceptionMessage) {
        return (T) Proxy.newProxyInstance(
                resourceType.getClassLoader(),
                new Class<?>[]{resourceType},
                (proxy, method, args) -> {
                    if ("close".equals(method.getName())) {
                        throw new SQLException(exceptionMessage);
                    }
                    return primitiveDefault(method.getReturnType());
                }
        );
    }

    private Object primitiveDefault(Class<?> returnType) {
        if (!returnType.isPrimitive() || returnType == void.class) {
            return null;
        }
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == char.class) {
            return '\0';
        }
        return 0;
    }

    private static final class CapturingAppender extends AbstractAppender {

        private final List<LogEvent> events = new ArrayList<>();

        private CapturingAppender() {
            super(
                    "DatabaseLoggingPolicyTestAppender",
                    null,
                    PatternLayout.createDefaultLayout(),
                    false,
                    Property.EMPTY_ARRAY
            );
        }

        @Override
        public void append(LogEvent event) {
            events.add(event.toImmutable());
        }
    }
}
