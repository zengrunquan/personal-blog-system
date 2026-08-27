package com.blog.api.upload;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class UploadStorageDefaultPathTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void explodedWarDirectoryShouldResolveToProjectRootDocsUploads() throws Exception {
        Path projectRoot = temporaryFolder.newFolder("personal-blog-system").toPath();
        Files.createDirectories(projectRoot.resolve("backend"));
        Files.writeString(projectRoot.resolve("backend/pom.xml"), "<project />");
        Files.createDirectories(projectRoot.resolve("frontend"));
        Files.writeString(projectRoot.resolve("frontend/package.json"), "{}");
        Path explodedClasses = projectRoot.resolve(
                "backend/target/personal_blog_system_war_exploded/WEB-INF/classes"
        );
        Files.createDirectories(explodedClasses);

        Path actual = UploadStorage.resolveDefaultStorageDirectory(explodedClasses);

        assertEquals(
                projectRoot.resolve("docs/uploads").toAbsolutePath().normalize(),
                actual
        );
    }

    @Test
    public void directoryOutsideProjectShouldProduceActionableConfigurationError() throws Exception {
        Path unrelatedDirectory = temporaryFolder.newFolder("tomcat-work").toPath();

        IOException error = assertThrows(
                IOException.class,
                () -> UploadStorage.resolveDefaultStorageDirectory(unrelatedDirectory)
        );

        assertTrue(error.getMessage().contains(UploadStorage.STORAGE_DIRECTORY_PROPERTY));
        assertTrue(error.getMessage().contains(UploadStorage.STORAGE_DIRECTORY_ENV));
    }
}
