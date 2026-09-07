package com.blog.media.storage;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;

public class StorageRootResolverTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @After
    public void clearUploadDirectoryProperty() {
        System.clearProperty(StorageRootResolver.STORAGE_DIRECTORY_PROPERTY);
    }

    @Test
    public void systemPropertyShouldTakePrecedenceOverEnvironmentConfiguration() throws Exception {
        Path configured = temporaryFolder.newFolder("configured-upload-root").toPath();
        System.setProperty(StorageRootResolver.STORAGE_DIRECTORY_PROPERTY, configured.toString());

        assertEquals(
                configured.toAbsolutePath().normalize(),
                StorageRootResolver.resolve()
        );
    }

    @Test
    public void projectAndBackendStartingDirectoriesShouldResolveToSameRoot() throws Exception {
        Path projectRoot = temporaryFolder.newFolder("project").toPath();
        Files.createDirectories(projectRoot.resolve("backend"));
        Files.createDirectories(projectRoot.resolve("frontend"));
        Files.createFile(projectRoot.resolve("backend/pom.xml"));
        Files.createFile(projectRoot.resolve("frontend/package.json"));

        assertEquals(
                StorageRootResolver.resolveDefaultStorageDirectory(projectRoot),
                StorageRootResolver.resolveDefaultStorageDirectory(projectRoot.resolve("backend"))
        );
    }

    @Test
    public void missingProjectRootShouldFailClosedInsteadOfUsingWorkingDirectory() throws Exception {
        Path unrelatedDirectory = temporaryFolder.newFolder("unrelated").toPath();

        IOException error = assertThrows(
                IOException.class,
                () -> StorageRootResolver.resolveDefaultStorageDirectory(unrelatedDirectory)
        );

        assertFalse(Files.exists(unrelatedDirectory.resolve("docs/uploads")));
        assertFalse(error.getMessage().isEmpty());
    }

    @Test
    public void existingRootValidationShouldNotCreateMissingDirectories() throws Exception {
        Path missingRoot = temporaryFolder.getRoot().toPath().resolve("missing/docs/uploads");

        assertThrows(
                IOException.class,
                () -> StorageRootResolver.requireExistingReadableRoot(missingRoot)
        );

        assertFalse(Files.exists(missingRoot));
        assertFalse(Files.exists(missingRoot.resolve("image")));
        assertFalse(Files.exists(missingRoot.resolve("file")));
    }
}
