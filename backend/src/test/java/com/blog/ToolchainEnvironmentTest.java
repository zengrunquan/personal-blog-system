package com.blog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.management.ManagementFactory;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.junit.Test;

/**
 * 验证项目测试 JVM 的工具链约定，避免测试结果依赖开发者本机的隐式配置。
 */
public class ToolchainEnvironmentTest {
    private static final String REQUIRE_NO_DYNAMIC_AGENT_LOADING =
            "blog.test.requireNoDynamicAgentLoading";
    private static final DateTimeFormatter LOG_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Test
    public void testJava21AndUtf8Output() {
        String timestamp = LocalDateTime.now().format(LOG_TIME_FORMATTER);
        System.out.println("[" + timestamp + "][DEBUG] [ToolchainEnvironmentTest] UTF-8 中文 stdout 标记");
        System.err.println("[" + timestamp + "][DEBUG] [ToolchainEnvironmentTest] UTF-8 中文 stderr 标记");

        assertEquals("21", System.getProperty("java.specification.version"));
        assertEquals(StandardCharsets.UTF_8, Charset.defaultCharset());
    }

    @Test
    public void testFinalClassMockWorksWithPreloadedMockitoAgent() {
        if (Boolean.getBoolean(REQUIRE_NO_DYNAMIC_AGENT_LOADING)) {
            assertTrue(
                    "专项验收必须实际向测试 JVM 传入禁止动态 agent 加载参数",
                    ManagementFactory.getRuntimeMXBean().getInputArguments()
                            .contains("-XX:-EnableDynamicAgentLoading"));
        }

        FinalToolchainProbe probe = mock(FinalToolchainProbe.class);
        when(probe.value()).thenReturn("agent-ready");

        assertEquals("agent-ready", probe.value());
    }

    public static final class FinalToolchainProbe {
        public String value() {
            return "original";
        }
    }
}
