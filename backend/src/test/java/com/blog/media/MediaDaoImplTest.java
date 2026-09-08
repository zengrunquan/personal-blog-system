package com.blog.media;

import com.blog.media.dao.MediaDaoImpl;
import com.blog.media.model.ManagedMediaKey;
import com.blog.media.model.MediaAsset;
import com.blog.media.model.MediaReferenceType;
import com.blog.media.model.MediaStatus;
import com.blog.media.model.MediaType;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MediaDaoImplTest {

    @Test
    public void insertShouldWriteAssetMetadataAndReturnGeneratedId() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet generatedKeys = mock(ResultSet.class);
        when(connection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
                .thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(1);
        when(statement.getGeneratedKeys()).thenReturn(generatedKeys);
        when(generatedKeys.next()).thenReturn(true);
        when(generatedKeys.getLong(1)).thenReturn(42L);

        MediaAsset asset = asset(MediaStatus.TEMP);
        long id = new MediaDaoImpl().insert(connection, asset);

        assertEquals(42L, id);
        verify(statement).setString(1, "ARTICLE_IMAGE");
        verify(statement).setString(2, "image_abc.png");
        verify(statement).setString(3, "abc.png");
        verify(statement).setString(4, "正文.png");
        verify(statement).setString(5, "image/png");
        verify(statement).setLong(6, 123L);
        verify(statement).setString(7, "sha256");
        verify(statement).setInt(8, 7);
        verify(statement).setString(9, "TEMP");
        verify(statement).setTimestamp(eq(10), eq(Timestamp.from(Instant.parse("2026-09-01T00:00:00Z"))));
    }

    @Test
    public void findByKeysShouldUseForUpdateAndBindEachTypeKey() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        List<MediaAsset> assets = new MediaDaoImpl().findByKeysForUpdate(
                connection,
                Collections.singleton(new ManagedMediaKey(MediaType.ATTACHMENT, "file-1.pdf"))
        );

        assertTrue(assets.isEmpty());
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sql.capture());
        assertTrue(sql.getValue().contains("FOR UPDATE"));
        verify(statement).setString(1, "ATTACHMENT");
        verify(statement).setString(2, "file-1.pdf");
    }

    @Test
    public void findOriginalNameShouldBindAttachmentAndNotLockRow() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getString("original_name")).thenReturn("课程笔记.pdf");

        assertEquals("课程笔记.pdf", new MediaDaoImpl().findOriginalName(
                connection,
                MediaType.ATTACHMENT,
                "12345678.pdf"
        ).orElse(null));

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sql.capture());
        assertTrue(sql.getValue().contains("SELECT original_name FROM media_asset"));
        assertTrue(sql.getValue().contains("media_type = ? AND url_file_name = ?"));
        assertFalse(sql.getValue().toUpperCase().contains("FOR UPDATE"));
        verify(statement).setString(1, MediaType.ATTACHMENT.name());
        verify(statement).setString(2, "12345678.pdf");
        verify(resultSet).close();
        verify(statement).close();
    }

    @Test
    public void findOriginalNameShouldReturnEmptyWhenAssetDoesNotExist() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        assertTrue(new MediaDaoImpl().findOriginalName(
                connection,
                MediaType.ATTACHMENT,
                "missing.pdf"
        ).isEmpty());

        verify(resultSet).close();
        verify(statement).close();
    }

    @Test
    public void findOriginalNameShouldPropagateSqlFailure() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenThrow(new SQLException("media query failed"));

        assertThrows(SQLException.class, () -> new MediaDaoImpl().findOriginalName(
                connection,
                MediaType.ATTACHMENT,
                "12345678.pdf"
        ));

        verify(statement).close();
    }

    @Test
    public void claimNextDueAssetShouldExcludeReferencedRows() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        assertTrue(new MediaDaoImpl().claimNextDueAsset(
                connection,
                "claim-token",
                Instant.parse("2026-09-01T00:00:00Z"),
                Instant.parse("2026-08-31T23:00:00Z")
        ).isEmpty());

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sql.capture());
        assertTrue(sql.getValue().contains("NOT EXISTS"));
        assertTrue(sql.getValue().contains("media_reference"));
    }

    @Test
    public void markDeletedShouldRequireMatchingClaimToken() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(1);

        new MediaDaoImpl().markDeleted(connection, 19L, "claim-token");

        verify(statement).setLong(1, 19L);
        verify(statement).setString(2, "claim-token");
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sql.capture());
        assertTrue(sql.getValue().contains("claim_token"));
        assertTrue(sql.getValue().contains("status = 'DELETING'"));
    }

    @Test
    public void replaceAvatarReferenceShouldRejectNonAvatarAsset() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement lock = mock(PreparedStatement.class);
        ResultSet resultSet = assetResultSet(MediaType.ARTICLE_IMAGE, MediaStatus.ACTIVE);
        when(connection.prepareStatement(anyString())).thenReturn(lock);
        when(lock.executeQuery()).thenReturn(resultSet);

        assertThrows(SQLException.class,
                () -> new MediaDaoImpl().replaceAvatarReference(connection, 7, 42L));
    }

    @Test
    public void replaceArticleReferencesShouldRejectAttachmentAsCover() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement lock = mock(PreparedStatement.class);
        ResultSet resultSet = assetResultSet(MediaType.ATTACHMENT, MediaStatus.ACTIVE);
        when(connection.prepareStatement(anyString())).thenReturn(lock);
        when(lock.executeQuery()).thenReturn(resultSet);

        assertThrows(SQLException.class,
                () -> new MediaDaoImpl().replaceArticleReferences(
                        connection,
                        9,
                        Map.of(42L, Set.of(MediaReferenceType.ARTICLE_COVER))
                ));
    }

    @Test
    public void replaceArticleReferencesShouldAllowAvatarInArticleContent() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement lock = mock(PreparedStatement.class);
        PreparedStatement delete = mock(PreparedStatement.class);
        PreparedStatement insert = mock(PreparedStatement.class);
        ResultSet resultSet = assetResultSet(MediaType.AVATAR, MediaStatus.ACTIVE);
        when(connection.prepareStatement(anyString())).thenReturn(lock, delete, insert);
        when(lock.executeQuery()).thenReturn(resultSet);

        new MediaDaoImpl().replaceArticleReferences(
                connection,
                9,
                Map.of(42L, Set.of(MediaReferenceType.ARTICLE_CONTENT))
        );

        verify(insert).setString(2, MediaReferenceType.ARTICLE_CONTENT.name());
    }

    @Test
    public void insertShouldPropagateSqlFailureToOuterTransaction() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        when(connection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
                .thenReturn(statement);
        when(statement.executeUpdate()).thenThrow(new SQLException("duplicate storage name"));

        assertThrows(SQLException.class, () -> new MediaDaoImpl().insert(connection, asset(MediaStatus.TEMP)));
    }

    @Test
    public void reconcileBackfillAssetShouldUseIdAndExpectedStatusGuard() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(1);

        MediaAsset asset = asset(MediaStatus.ACTIVE);
        asset.setId(42L);
        new MediaDaoImpl().reconcileBackfillAsset(
                connection,
                asset,
                MediaStatus.MISSING_BINARY,
                false
        );

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sql.capture());
        assertTrue(sql.getValue().contains("WHERE id = ? AND status = ?"));
        verify(statement).setLong(6, 42L);
        verify(statement).setString(7, MediaStatus.MISSING_BINARY.name());
    }

    private MediaAsset asset(MediaStatus status) {
        MediaAsset asset = new MediaAsset();
        asset.setMediaType(MediaType.ARTICLE_IMAGE);
        asset.setStorageName("image_abc.png");
        asset.setUrlFileName("abc.png");
        asset.setOriginalName("正文.png");
        asset.setContentType("image/png");
        asset.setSizeBytes(123L);
        asset.setSha256("sha256");
        asset.setUploadedBy(7);
        asset.setStatus(status);
        asset.setDeleteAfter(Instant.parse("2026-09-01T00:00:00Z"));
        return asset;
    }

    private ResultSet assetResultSet(MediaType type, MediaStatus status) throws SQLException {
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getLong("id")).thenReturn(42L);
        when(resultSet.getString("media_type")).thenReturn(type.name());
        when(resultSet.getString("storage_name")).thenReturn(type.getStoragePrefix() + "asset.png");
        when(resultSet.getString("url_file_name")).thenReturn("asset.png");
        when(resultSet.getString("original_name")).thenReturn("asset.png");
        when(resultSet.getString("content_type")).thenReturn("application/octet-stream");
        when(resultSet.getString("sha256")).thenReturn("sha256");
        when(resultSet.getString("status")).thenReturn(status.name());
        when(resultSet.getInt("delete_attempts")).thenReturn(0);
        when(resultSet.wasNull()).thenReturn(false);
        return resultSet;
    }
}
