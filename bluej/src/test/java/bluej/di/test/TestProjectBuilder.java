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

import bluej.pkgmgr.Project;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.UUID;

/**
 * Builder for creating Project instances in tests.
 * 
 * <p>This class provides factory methods for creating Project instances
 * in different configurations suitable for various test scenarios:
 * 
 * <h3>Mock Project (Unit Tests)</h3>
 * <p>For unit tests that need a Project instance but don't need real file system
 * operations, use your preferred mocking framework:
 * <pre>
 * // Using Mockito
 * Project mockProject = mock(Project.class);
 * when(mockProject.getProjectDir()).thenReturn(new File("/mock/path"));
 * </pre>
 * 
 * <h3>Fixture Project (Integration Tests)</h3>
 * <p>Load from an existing test fixture directory:
 * <pre>
 * Project project = TestProjectBuilder.fromFixture(
 *     new File("src/test/resources/test-projects/simple-project"));
 * </pre>
 * 
 * <h3>Temporary Directory Project (Integration Tests)</h3>
 * <p>Create a new project in a temporary directory:
 * <pre>
 * Project project = TestProjectBuilder.inTempDirectory();
 * // Or with a specific name:
 * Project project = TestProjectBuilder.inTempDirectory("my-test-project");
 * </pre>
 * 
 * <h3>Auto-Cleanup</h3>
 * <p>For temporary directory projects, the directory is NOT automatically
 * cleaned up. Use JUnit's {@code @TempDir} or cleanup in {@code @AfterEach}:
 * <pre>
 * &#64;TempDir Path tempDir;
 * 
 * &#64;Test
 * void testWithProject() {
 *     Project project = TestProjectBuilder.inDirectory(tempDir.toFile());
 *     // tempDir is automatically cleaned up by JUnit
 * }
 * </pre>
 * 
 * @see TestProjectScopeBuilder
 * @see Project
 */
public final class TestProjectBuilder {
    
    private TestProjectBuilder() { /* utility class */ }
    
    /**
     * Create a project by opening an existing fixture directory.
     * 
     * <p>The directory must be a valid BlueJ project (containing package.bluej
     * or bluej.pkg files). This is useful for integration tests that need
     * pre-configured project structures.
     * 
     * @param fixtureDir the path to the existing project fixture
     * @return the opened Project instance
     * @throws IllegalArgumentException if fixtureDir is null or doesn't exist
     * @throws RuntimeException if the project cannot be opened
     */
    public static Project fromFixture(File fixtureDir) {
        if (fixtureDir == null) {
            throw new IllegalArgumentException("Fixture directory cannot be null");
        }
        if (!fixtureDir.exists()) {
            throw new IllegalArgumentException(
                "Fixture directory does not exist: " + fixtureDir.getAbsolutePath());
        }
        if (!fixtureDir.isDirectory()) {
            throw new IllegalArgumentException(
                "Fixture path is not a directory: " + fixtureDir.getAbsolutePath());
        }
        
        Project project = Project.openProject(fixtureDir.getAbsolutePath());
        if (project == null) {
            throw new RuntimeException(
                "Failed to open project from fixture: " + fixtureDir.getAbsolutePath() +
                ". Ensure it's a valid BlueJ project directory.");
        }
        return project;
    }
    
    /**
     * Create a project by opening an existing fixture directory.
     * 
     * @param fixturePath the path to the existing project fixture
     * @return the opened Project instance
     * @throws IllegalArgumentException if fixturePath is null or doesn't exist
     * @throws RuntimeException if the project cannot be opened
     * @see #fromFixture(File)
     */
    public static Project fromFixture(Path fixturePath) {
        if (fixturePath == null) {
            throw new IllegalArgumentException("Fixture path cannot be null");
        }
        return fromFixture(fixturePath.toFile());
    }
    
    /**
     * Create a project by copying a named fixture from classpath resources to a temp directory.
     *
     * <p>The fixture is looked up from the classpath at {@code bluej/fixtures/{fixtureName}}.
     * The contents are copied to a temporary directory, and the project is opened from there.
     * This allows tests to modify the project without affecting the original fixture.
     *
     * <p>The temporary directory path is printed to stdout for debugging.
     *
     * <p>Example:
     * <pre>
     * // Uses classpath resource bluej/fixtures/simple
     * Project project = TestProjectBuilder.fromFixture("simple");
     * </pre>
     *
     * @param fixturePath the name of the fixture (e.g., "simple")
     * @return the opened Project instance in a temporary directory
     * @throws IllegalArgumentException if fixturePath is null or empty, or fixture not found
     * @throws RuntimeException if copying or opening the project fails
     */
    public static Project fromFixture(String fixturePath) {
        if (fixturePath == null || fixturePath.trim().isEmpty()) {
            throw new IllegalArgumentException("Fixture path cannot be null or empty");
        }
        
        String resourcePath = "bluej/fixtures/" + fixturePath;
        
        // Get the fixture URL from classpath
        URL fixtureUrl = TestProjectBuilder.class.getClassLoader().getResource(resourcePath);
        if (fixtureUrl == null) {
            throw new IllegalArgumentException(
                "Fixture not found on classpath: " + resourcePath);
        }
        
        try {
            // Create temp directory
            String fixtureName = fixturePath.replace('/', '-').replace('\\', '-');
            String randomSuffix = UUID.randomUUID().toString().substring(0, 8);
            Path tempDir = Files.createTempDirectory("test-fixture-" + fixtureName + "-" + randomSuffix);
            
            // Print the temp directory for test debugging
            System.out.println("[TestProjectBuilder] Project directory: " + tempDir.toAbsolutePath());
            
            // Copy fixture contents to temp directory
            copyResourceDirectory(resourcePath, tempDir);
            
            // Open the project
            Project project = Project.openProject(tempDir.toFile().getAbsolutePath());
            if (project == null) {
                throw new RuntimeException(
                    "Failed to open project from fixture copy: " + tempDir.toAbsolutePath());
            }
            return project;
            
        } catch (IOException e) {
            throw new RuntimeException(
                "Failed to copy fixture to temp directory: " + fixturePath, e);
        }
    }
    
    /**
     * Copies a resource directory from the classpath to a target directory.
     *
     * @param resourcePath the classpath resource path (e.g., "bluej/fixtures/simple")
     * @param targetDir the target directory to copy to
     * @throws IOException if copying fails
     */
    private static void copyResourceDirectory(String resourcePath, Path targetDir) throws IOException {
        URL resourceUrl = TestProjectBuilder.class.getClassLoader().getResource(resourcePath);
        if (resourceUrl == null) {
            throw new IOException("Resource not found: " + resourcePath);
        }
        
        // Handle file:// URLs (development/IDE)
        if ("file".equals(resourceUrl.getProtocol())) {
            try {
                Path sourcePath = Paths.get(resourceUrl.toURI());
                copyDirectory(sourcePath, targetDir);
                return;
            } catch (URISyntaxException e) {
                throw new IOException("Invalid resource URI: " + resourceUrl, e);
            }
        }
        
        // Handle jar:// URLs (when running from jar)
        // For test resources, they're typically unpacked, so file:// should work
        throw new IOException(
            "Unsupported resource URL protocol: " + resourceUrl.getProtocol() +
            ". Fixture resources should be on the file system.");
    }
    
    /**
     * Copies a named fixture from classpath to a target directory.
     *
     * <p>This is package-private to enable testing of the copy functionality
     * without requiring the full BlueJ environment needed by Project.openProject().
     *
     * @param fixtureName the name of the fixture (e.g., "simple")
     * @param targetDir the target directory to copy to
     * @return the target directory (same as input, for convenience)
     * @throws IOException if the fixture doesn't exist or copying fails
     */
    static Path copyFixtureToDirectory(String fixtureName, Path targetDir) throws IOException {
        String resourcePath = "bluej/fixtures/" + fixtureName;
        copyResourceDirectory(resourcePath, targetDir);
        return targetDir;
    }
    
    /**
     * Recursively copies a directory tree.
     */
    private static void copyDirectory(Path source, Path target) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                    throws IOException {
                Path targetDir = target.resolve(source.relativize(dir));
                if (!Files.exists(targetDir)) {
                    Files.createDirectories(targetDir);
                }
                return FileVisitResult.CONTINUE;
            }
            
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                Path targetFile = target.resolve(source.relativize(file));
                Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }
    
    /**
     * Create a new project in a system temporary directory with a random name.
     * 
     * <p>Creates a new directory in the system temp folder with a UUID-based
     * name and initializes it as a BlueJ project. The directory is NOT
     * automatically cleaned up - use JUnit's {@code @TempDir} annotation
     * or manual cleanup for better lifecycle management.
     * 
     * @return a new Project instance in a temporary directory
     * @throws RuntimeException if the project cannot be created
     */
    public static Project inTempDirectory() {
        String randomName = "test-project-" + UUID.randomUUID().toString().substring(0, 8);
        return inTempDirectory(randomName);
    }
    
    /**
     * Create a new project in a system temporary directory with the specified name.
     * 
     * <p>Creates a new directory in the system temp folder and initializes it
     * as a BlueJ project. The directory is NOT automatically cleaned up.
     * 
     * @param projectName the name for the project directory
     * @return a new Project instance in a temporary directory
     * @throws IllegalArgumentException if projectName is null or empty
     * @throws RuntimeException if the project cannot be created
     */
    public static Project inTempDirectory(String projectName) {
        if (projectName == null || projectName.trim().isEmpty()) {
            throw new IllegalArgumentException("Project name cannot be null or empty");
        }
        
        try {
            Path tempDir = Files.createTempDirectory(projectName);
            return inDirectory(tempDir.toFile());
        } catch (IOException e) {
            throw new RuntimeException(
                "Failed to create temporary directory for project: " + projectName, e);
        }
    }
    
    /**
     * Create a new project in the specified directory.
     * 
     * <p>If the directory doesn't exist, it will be created. If it exists and
     * is empty, a new BlueJ project will be initialized. If it exists and
     * contains a valid BlueJ project, it will be opened.
     * 
     * <p>This method is useful with JUnit 5's {@code @TempDir}:
     * <pre>
     * &#64;TempDir Path tempDir;
     * 
     * &#64;Test
     * void test() {
     *     Project project = TestProjectBuilder.inDirectory(tempDir.toFile());
     * }
     * </pre>
     * 
     * @param directory the directory to create the project in
     * @return a new or opened Project instance
     * @throws IllegalArgumentException if directory is null
     * @throws RuntimeException if the project cannot be created or opened
     */
    public static Project inDirectory(File directory) {
        if (directory == null) {
            throw new IllegalArgumentException("Directory cannot be null");
        }
        
        // Create directory if it doesn't exist
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                throw new RuntimeException(
                    "Failed to create directory: " + directory.getAbsolutePath());
            }
        }
        
        // Check if this is already a BlueJ project
        File packageFile = new File(directory, "package.bluej");
        File legacyPackageFile = new File(directory, "bluej.pkg");
        
        if (packageFile.exists() || legacyPackageFile.exists()) {
            // Open existing project
            Project project = Project.openProject(directory.getAbsolutePath());
            if (project == null) {
                throw new RuntimeException(
                    "Failed to open existing project: " + directory.getAbsolutePath());
            }
            return project;
        }
        
        // Initialize new BlueJ project by creating minimal package.bluej
        try {
            if (!packageFile.createNewFile()) {
                throw new RuntimeException(
                    "Failed to create package.bluej in: " + directory.getAbsolutePath());
            }
            
            Project project = Project.openProject(directory.getAbsolutePath());
            if (project == null) {
                throw new RuntimeException(
                    "Failed to open newly created project: " + directory.getAbsolutePath());
            }
            return project;
        } catch (IOException e) {
            throw new RuntimeException(
                "Failed to initialize project in: " + directory.getAbsolutePath(), e);
        }
    }
    
    /**
     * Create a new project in the specified directory.
     * 
     * @param directory the directory path to create the project in
     * @return a new or opened Project instance
     * @throws IllegalArgumentException if directory is null
     * @throws RuntimeException if the project cannot be created or opened
     * @see #inDirectory(File)
     */
    public static Project inDirectory(Path directory) {
        if (directory == null) {
            throw new IllegalArgumentException("Directory cannot be null");
        }
        return inDirectory(directory.toFile());
    }
}
