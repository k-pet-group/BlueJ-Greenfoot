/*
 This file is part of the BlueJ program.
 Copyright (C) 2026  Michael Kolling and John Rosenberg

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 2
 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

 This file is subject to the Classpath exception as provided in the
 LICENSE.txt file that accompanied this code.
 */
package bluej.di.test;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

/**
 * Tests for {@link TestProjectBuilder}.
 * 
 * <p>Note: Tests that create real Projects require the BlueJ environment
 * to be properly initialized and may need to run on the FXPlatform thread.
 * These tests focus on parameter validation and file system behavior.
 */
public class TestProjectBuilderTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    // =========================================================================
    // Parameter validation tests - fromFixture
    // =========================================================================

    @Test(expected = IllegalArgumentException.class)
    public void fromFixture_File_nullThrowsException() {
        TestProjectBuilder.fromFixture((File) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void fromFixture_Path_nullThrowsException() {
        TestProjectBuilder.fromFixture((Path) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void fromFixture_nonexistentDirectoryThrowsException() {
        File nonexistent = new File("/nonexistent/directory/that/does/not/exist");
        TestProjectBuilder.fromFixture(nonexistent);
    }

    @Test(expected = IllegalArgumentException.class)
    public void fromFixture_fileNotDirectoryThrowsException() throws Exception {
        File file = tempFolder.newFile("test.txt");
        TestProjectBuilder.fromFixture(file);
    }

    // =========================================================================
    // Parameter validation tests - fromFixture(String) (named fixture)
    // =========================================================================

    @Test(expected = IllegalArgumentException.class)
    public void fromFixture_String_nullThrowsException() {
        TestProjectBuilder.fromFixture((String) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void fromFixture_String_emptyThrowsException() {
        TestProjectBuilder.fromFixture("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void fromFixture_String_whitespaceThrowsException() {
        TestProjectBuilder.fromFixture("   ");
    }

    @Test(expected = IllegalArgumentException.class)
    public void fromFixture_String_nonexistentFixtureThrowsException() {
        TestProjectBuilder.fromFixture("nonexistent-fixture-that-does-not-exist");
    }

    // =========================================================================
    // Parameter validation tests - inTempDirectory
    // =========================================================================

    @Test(expected = IllegalArgumentException.class)
    public void inTempDirectory_nullNameThrowsException() {
        TestProjectBuilder.inTempDirectory(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void inTempDirectory_emptyNameThrowsException() {
        TestProjectBuilder.inTempDirectory("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void inTempDirectory_whitespaceNameThrowsException() {
        TestProjectBuilder.inTempDirectory("   ");
    }

    // =========================================================================
    // Parameter validation tests - inDirectory
    // =========================================================================

    @Test(expected = IllegalArgumentException.class)
    public void inDirectory_File_nullThrowsException() {
        TestProjectBuilder.inDirectory((File) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void inDirectory_Path_nullThrowsException() {
        TestProjectBuilder.inDirectory((Path) null);
    }

    // =========================================================================
    // File system behavior tests (don't require full BlueJ initialization)
    // =========================================================================

    @Test
    public void inDirectory_createsDirectoryIfNotExists() throws Exception {
        File newDir = new File(tempFolder.getRoot(), "new-project");
        assertFalse("Directory should not exist initially", newDir.exists());
        
        try {
            // This will fail because Project.openProject won't work without BlueJ,
            // but the directory should be created first
            TestProjectBuilder.inDirectory(newDir);
            fail("Expected RuntimeException because BlueJ is not initialized");
        } catch (RuntimeException e) {
            // Expected - Project.openProject requires BlueJ environment
            assertTrue("Directory should have been created", newDir.exists());
            assertTrue("Should be a directory", newDir.isDirectory());
        }
    }

    @Test
    public void inDirectory_createsPackageBluejFile() throws Exception {
        File newDir = tempFolder.newFolder("test-project");
        File packageFile = new File(newDir, "package.bluej");
        assertFalse("package.bluej should not exist initially", packageFile.exists());
        
        try {
            TestProjectBuilder.inDirectory(newDir);
            fail("Expected RuntimeException because BlueJ is not initialized");
        } catch (RuntimeException e) {
            // Expected - but package.bluej should be created
            assertTrue("package.bluej should have been created", packageFile.exists());
        }
    }

    @Test
    public void inDirectory_existingProjectDirectoryWithPackageBluej() throws Exception {
        File projectDir = tempFolder.newFolder("existing-project");
        File packageFile = new File(projectDir, "package.bluej");
        assertTrue("Failed to create package.bluej", packageFile.createNewFile());
        
        try {
            TestProjectBuilder.inDirectory(projectDir);
            fail("Expected RuntimeException because BlueJ is not initialized");
        } catch (RuntimeException e) {
            // Expected - Project.openProject requires BlueJ environment
            // but it should try to open, not recreate
            assertTrue("package.bluej should still exist", packageFile.exists());
        }
    }

    @Test
    public void inDirectory_existingProjectDirectoryWithLegacyBluejPkg() throws Exception {
        File projectDir = tempFolder.newFolder("legacy-project");
        File legacyFile = new File(projectDir, "bluej.pkg");
        assertTrue("Failed to create bluej.pkg", legacyFile.createNewFile());
        
        File modernFile = new File(projectDir, "package.bluej");
        assertFalse("package.bluej should not exist", modernFile.exists());
        
        try {
            TestProjectBuilder.inDirectory(projectDir);
            fail("Expected RuntimeException because BlueJ is not initialized");
        } catch (RuntimeException e) {
            // Expected - should try to open legacy project
            assertTrue("bluej.pkg should still exist", legacyFile.exists());
            // Modern package.bluej should NOT be created if legacy exists
            assertFalse("package.bluej should not have been created", modernFile.exists());
        }
    }

    // =========================================================================
    // Fixture copy tests - verify fixture content is properly copied
    // =========================================================================

    @Test
    public void copyFixtureToDirectory_simpleFixture_copiesAllExpectedFiles() throws Exception {
        Path targetDir = tempFolder.newFolder("fixture-copy-test").toPath();
        
        TestProjectBuilder.copyFixtureToDirectory("simple", targetDir);
        
        // Verify all expected files exist
        assertTrue("package.bluej should exist",
            Files.exists(targetDir.resolve("package.bluej")));
        assertTrue("README.TXT should exist",
            Files.exists(targetDir.resolve("README.TXT")));
        assertTrue("TestFile.java should exist",
            Files.exists(targetDir.resolve("TestFile.java")));
    }

    @Test
    public void copyFixtureToDirectory_simpleFixture_packageBluejHasCorrectContent() throws Exception {
        Path targetDir = tempFolder.newFolder("fixture-content-test").toPath();
        
        TestProjectBuilder.copyFixtureToDirectory("simple", targetDir);
        
        // Read the copied file
        Path packageBluej = targetDir.resolve("package.bluej");
        String content = new String(Files.readAllBytes(packageBluej), StandardCharsets.UTF_8);
        
        // Verify expected content
        assertTrue("Should contain BlueJ package file header",
            content.contains("#BlueJ package file"));
        assertTrue("Should contain charset setting",
            content.contains("project.charset=UTF-8"));
    }

    @Test
    public void copyFixtureToDirectory_simpleFixture_testFileJavaHasCorrectContent() throws Exception {
        Path targetDir = tempFolder.newFolder("fixture-java-test").toPath();
        
        TestProjectBuilder.copyFixtureToDirectory("simple", targetDir);
        
        // Read the copied file
        Path testFileJava = targetDir.resolve("TestFile.java");
        String content = new String(Files.readAllBytes(testFileJava), StandardCharsets.UTF_8);
        
        // Verify key content elements
        assertTrue("Should contain class declaration",
            content.contains("public class TestFile"));
        assertTrue("Should contain constructor",
            content.contains("public TestFile()"));
        assertTrue("Should contain sampleMethod",
            content.contains("public int sampleMethod(int y)"));
        assertTrue("Should contain instance variable",
            content.contains("private int x"));
    }

    @Test
    public void copyFixtureToDirectory_simpleFixture_readmeTxtHasCorrectContent() throws Exception {
        Path targetDir = tempFolder.newFolder("fixture-readme-test").toPath();
        
        TestProjectBuilder.copyFixtureToDirectory("simple", targetDir);
        
        // Read the copied file
        Path readmeTxt = targetDir.resolve("README.TXT");
        String content = new String(Files.readAllBytes(readmeTxt), StandardCharsets.UTF_8);
        
        // Verify key content elements
        assertTrue("Should contain project README header",
            content.contains("This is the project README file"));
        assertTrue("Should contain PROJECT TITLE",
            content.contains("PROJECT TITLE:"));
        assertTrue("Should contain AUTHORS",
            content.contains("AUTHORS:"));
    }

    @Test
    public void copyFixtureToDirectory_simpleFixture_copiedFilesAreIndependent() throws Exception {
        Path targetDir = tempFolder.newFolder("fixture-independent-test").toPath();
        
        TestProjectBuilder.copyFixtureToDirectory("simple", targetDir);
        
        // Modify the copied file
        Path testFileJava = targetDir.resolve("TestFile.java");
        String originalContent = new String(Files.readAllBytes(testFileJava), StandardCharsets.UTF_8);
        String modifiedContent = originalContent.replace("private int x", "private int modifiedVariable");
        Files.write(testFileJava, modifiedContent.getBytes(StandardCharsets.UTF_8));
        
        // Copy again to a new directory
        Path targetDir2 = tempFolder.newFolder("fixture-independent-test-2").toPath();
        TestProjectBuilder.copyFixtureToDirectory("simple", targetDir2);
        
        // Verify the second copy has original content (not modified)
        Path testFileJava2 = targetDir2.resolve("TestFile.java");
        String content2 = new String(Files.readAllBytes(testFileJava2), StandardCharsets.UTF_8);
        
        assertTrue("Second copy should have original content",
            content2.contains("private int x"));
        assertFalse("Second copy should not have modified content",
            content2.contains("modifiedVariable"));
    }

    @Test
    public void copyFixtureToDirectory_simpleFixture_fileCountMatches() throws Exception {
        Path targetDir = tempFolder.newFolder("fixture-count-test").toPath();
        
        TestProjectBuilder.copyFixtureToDirectory("simple", targetDir);
        
        // Count files in target directory
        long fileCount = Files.list(targetDir).count();
        
        // Simple fixture should have exactly 3 files
        assertEquals("Simple fixture should have exactly 3 files", 3, fileCount);
    }

    @Test(expected = IOException.class)
    public void copyFixtureToDirectory_nonexistentFixture_throwsIOException() throws Exception {
        Path targetDir = tempFolder.newFolder("nonexistent-fixture-test").toPath();
        
        TestProjectBuilder.copyFixtureToDirectory("does-not-exist", targetDir);
    }

    @Test
    public void copyFixtureToDirectory_targetDirectoryIsPopulated() throws Exception {
        Path targetDir = tempFolder.newFolder("populated-test").toPath();
        
        // Verify empty initially
        assertEquals("Target should be empty initially", 0, Files.list(targetDir).count());
        
        TestProjectBuilder.copyFixtureToDirectory("simple", targetDir);
        
        // Verify populated after copy
        assertTrue("Target should have files after copy", Files.list(targetDir).count() > 0);
    }
}
