package com.blog.media;

import com.blog.media.dao.MediaDaoImpl;
import org.junit.Test;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.util.Properties;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

/** 显式启用后使用连接私有临时表，避免真实 SQL 验证修改业务资产或删除上传文件。 */
public class MediaDaoCompatibilityTest {
    @Test
    public void realServerShouldClaimOnlyDueUnreferencedAssetAndRollback() throws Exception {
        assumeTrue(Boolean.getBoolean("blog.test.media.database"));
        Properties config = new Properties();
        try (InputStream input = getClass().getResourceAsStream("/db.properties")) {
            config.load(input);
        }
        try (Connection connection = DriverManager.getConnection(config.getProperty("druid.url"),
                config.getProperty("druid.username"), config.getProperty("druid.password"));
             Statement sql = connection.createStatement()) {
            System.out.println("[DEBUG] " + Instant.now() + " [MediaDaoCompatibilityTest] server="
                    + connection.getMetaData().getDatabaseProductVersion());
            // 临时表遮蔽同名业务表，连接关闭后由数据库自动回收。
            sql.execute("CREATE TEMPORARY TABLE test_media_asset LIKE media_asset");
            sql.execute("CREATE TEMPORARY TABLE test_media_reference LIKE media_reference");
            sql.execute("ALTER TABLE test_media_asset RENAME TO media_asset");
            sql.execute("ALTER TABLE test_media_reference RENAME TO media_reference");
            sql.executeUpdate("INSERT INTO media_asset (id, media_type, storage_name, url_file_name, "
                    + "original_name, status, delete_after, delete_attempts) VALUES "
                    + "(1,'ARTICLE_IMAGE','test1.png','test1.png','test1.png','TEMP','2020-01-01',0),"
                    + "(2,'ARTICLE_IMAGE','test2.png','test2.png','test2.png','TEMP','2020-01-01',0),"
                    + "(3,'ARTICLE_IMAGE','test3.png','test3.png','test3.png','TEMP','2099-01-01',0)");
            sql.executeUpdate("INSERT INTO media_reference(media_id,reference_type,article_id) "
                    + "VALUES (1,'ARTICLE_CONTENT',1)");
            connection.setAutoCommit(false);
            MediaDaoImpl dao = new MediaDaoImpl();
            Instant now = Instant.parse("2026-09-07T00:00:00Z");
            assertEquals(Long.valueOf(2), dao.claimNextDueAsset(connection,"test-claim",now,now)
                    .orElseThrow(() -> new AssertionError("未认领到期资产")).getId());
            assertFalse(dao.claimNextDueAsset(connection,"another-claim",now,now).isPresent());
            connection.rollback();
            try (ResultSet rows = sql.executeQuery("SELECT status,claim_token,delete_attempts FROM media_asset WHERE id=2")) {
                assertTrue(rows.next());
                assertEquals("TEMP", rows.getString(1));
                assertNull(rows.getString(2));
                assertEquals(0, rows.getInt(3));
            }
        }
    }
}
