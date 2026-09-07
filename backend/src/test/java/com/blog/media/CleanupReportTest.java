package com.blog.media;

import com.blog.media.service.CleanupReport;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;

public class CleanupReportTest {

    @Test
    public void reportShouldExposeStableAuditFields() {
        CleanupReport report = new CleanupReport(1, 2, 3, 4, 5, 6, 7L);

        Map<String, Object> values = report.asMap();

        assertEquals(1, values.get("staleClaimsRecovered"));
        assertEquals(2, values.get("assetsMarkedPending"));
        assertEquals(3, values.get("assetsClaimed"));
        assertEquals(4, values.get("filesDeleted"));
        assertEquals(5, values.get("filesAlreadyMissing"));
        assertEquals(6, values.get("deleteFailures"));
        assertEquals(7L, values.get("elapsedMillis"));
    }
}
