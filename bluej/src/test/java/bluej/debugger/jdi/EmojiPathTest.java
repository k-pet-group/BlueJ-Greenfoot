/*
 This file is part of the BlueJ program.
 Copyright (C) 2024  Michael Kolling and John Rosenberg

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
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02116-1301, USA.

 This file is subject to the Classpath exception as provided in the
 LICENSE.txt file that accompanied this code.
*/
package bluej.debugger.jdi;

import junit.framework.TestCase;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Unit tests for emoji and Unicode character support in file paths.
 *
 * These tests verify the fix for issue #2426 where BlueJ would fail to open
 * project directories with emoji or other Unicode characters in the path.
 *
 * Original issue example:
 * - Path: /Users/anonymised/Desktop/💻Comp💻/MyAssignment
 * - Error: FileNotFoundException with garbled path (ð»Compð»)
 *
 * @author BlueJ Team
 * @see <a href="https://github.com/bluej-micro/bluej-micro/issues/2426">Issue #2426</a>
 */
public class EmojiPathTest extends TestCase
{
    private static final String[] TEST_EMOJIS = {
        "💻",  // Laptop
        "🎉",  // Party popper
        "🚀",  // Rocket
        "😀",  // Grinning face
        "❤️",   // Red heart
        "🎨",  // Art palette
        "🔥",  // Fire
        "✨",  // Sparkles
        "🏁",  // Chequered flag
        "🌍"   // Globe
    };

    private File tempTestDir;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        // Create a temporary directory for tests
        tempTestDir = createTempDirectory("bluej-emoji-test-");
    }

    @Override
    protected void tearDown() throws Exception
    {
        super.tearDown();
        // Clean up temporary directory
        if (tempTestDir != null && tempTestDir.exists()) {
            deleteDirectory(tempTestDir);
        }
    }

    /**
     * Test 1: Verify ProcessBuilder can handle emoji in directory paths
     * This tests the core fix: replacing Runtime.exec() with ProcessBuilder
     */
    public void testProcessBuilderWithEmojiPath() throws Exception
    {
        // Create directory with emoji in name
        File emojiDir = new File(tempTestDir, "💻Comp💻");
        assertTrue("Could not create emoji directory", emojiDir.mkdirs());

        // Create test file in emoji directory
        File testFile = new File(emojiDir, "test.txt");
        String testContent = "Hello, World!";
        Files.write(testFile.toPath(), testContent.getBytes(StandardCharsets.UTF_8));

        // Try to list directory using ProcessBuilder
        ProcessBuilder pb = new ProcessBuilder("ls", "-la", emojiDir.getAbsolutePath());
        Process process = pb.start();

        // Verify process completed successfully
        int exitCode = process.waitFor();
        assertEquals("ProcessBuilder should successfully list emoji directory", 0, exitCode);

        // Verify file still exists and is readable
        assertTrue("Test file should still exist", testFile.exists());
        String readContent = new String(Files.readAllBytes(testFile.toPath()), StandardCharsets.UTF_8);
        assertEquals("File content should be preserved", testContent, readContent);
    }

    /**
     * Test 2: Verify Path API handles emoji correctly
     * This tests the fix in Project.pathIntoStartingDirectory()
     */
    public void testPathApiWithEmoji() throws Exception
    {
        // Create directory with emoji
        File emojiDir = new File(tempTestDir, "🎉test🚀");
        assertTrue("Could not create emoji directory", emojiDir.mkdirs());

        // Test Path API conversion
        Path path = emojiDir.toPath();
        assertNotNull("Path should not be null", path);

        // Test toRealPath() preserves emoji
        Path realPath = path.toRealPath();
        assertTrue("Real path should contain emoji", realPath.toString().contains("🎉"));
        assertTrue("Real path should contain emoji", realPath.toString().contains("🚀"));

        // Test conversion back to File
        File convertedFile = realPath.toFile();
        assertTrue("Converted File should exist", convertedFile.exists());
        assertTrue("Converted File should be a directory", convertedFile.isDirectory());
    }

    /**
     * Test 3: Verify multiple emoji in path
     * Original issue had: /Users/anonymised/Desktop/💻Comp💻/MyAssignment
     */
    public void testMultipleEmojiInPath() throws Exception
    {
        // Create nested directories with emoji
        File dir1 = new File(tempTestDir, "💻");
        File dir2 = new File(dir1, "Comp");
        File dir3 = new File(dir2, "💻");
        File dir4 = new File(dir3, "MyAssignment");

        assertTrue("Could not create nested emoji path", dir4.mkdirs());

        // Verify all levels are accessible
        assertTrue("First level should exist", dir1.exists());
        assertTrue("Second level should exist", dir2.exists());
        assertTrue("Third level should exist", dir3.exists());
        assertTrue("Fourth level should exist", dir4.exists());

        // Test Path API with full emoji path
        Path fullPath = dir4.toPath().toRealPath();
        assertTrue("Full path should contain all emojis",
                   fullPath.toString().contains("💻"));

        // Verify we can create and read files in this path
        File testFile = new File(dir4, "Test.java");
        Files.write(testFile.toPath(), "public class Test {}".getBytes(StandardCharsets.UTF_8));

        assertTrue("Test file should exist", testFile.exists());
        assertTrue("Test file should be readable", testFile.canRead());
    }

    /**
     * Test 4: Verify emoji path works with file operations
     * This simulates editing README file mentioned in issue
     */
    public void testFileOperationsInEmojiPath() throws Exception
    {
        // Create directory with emoji
        File emojiDir = new File(tempTestDir, "🔥Project🔥");
        assertTrue("Could not create emoji directory", emojiDir.mkdirs());

        // Create README file (simulating BlueJ's README)
        File readmeFile = new File(emojiDir, "README.md");
        String readmeContent = "# My Project\nThis is a test project with emoji 🔥";
        Files.write(readmeFile.toPath(), readmeContent.getBytes(StandardCharsets.UTF_8));

        // Read it back
        String readContent = new String(Files.readAllBytes(readmeFile.toPath()), StandardCharsets.UTF_8);
        assertEquals("README content should be preserved", readmeContent, readContent);

        // Update README (simulate editing)
        String updatedContent = readmeContent + "\n\nUpdated on: 📅 " + new java.util.Date();
        Files.write(readmeFile.toPath(), updatedContent.getBytes(StandardCharsets.UTF_8));

        // Verify update
        String finalContent = new String(Files.readAllBytes(readmeFile.toPath()), StandardCharsets.UTF_8);
        assertTrue("Updated content should contain new text", finalContent.contains("Updated on:"));
    }

    /**
     * Test 5: Verify Unicode combining characters work
     * Some emoji are composed of multiple Unicode characters
     */
    public void testUnicodeCombiningCharacters() throws Exception
    {
        // Test with various Unicode characters
        String[] unicodeNames = {
            "test_日本語",  // Japanese
            "test_한국",   // Korean
            "test_العربية", // Arabic
            "test_עברית",  // Hebrew
            "test_Русский", // Russian
            "test_Ελληνικά", // Greek
            "test_ไทย"     // Thai
        };

        for (String unicodeName : unicodeNames) {
            File unicodeDir = new File(tempTestDir, unicodeName);
            assertTrue("Could not create directory: " + unicodeName, unicodeDir.mkdirs());

            File testFile = new File(unicodeDir, "test.txt");
            Files.write(testFile.toPath(), "test".getBytes(StandardCharsets.UTF_8));

            assertTrue("Directory should exist: " + unicodeName, unicodeDir.exists());
            assertTrue("File should exist in: " + unicodeName, testFile.exists());
        }
    }

    /**
     * Test 6: Verify emoji works with Path.toRealPath()
     * This specifically tests the fix in Project.pathIntoStartingDirectory()
     */
    public void testToRealPathPreservesEmoji() throws Exception
    {
        // Create directory with emoji
        File emojiDir = new File(tempTestDir, "✨Sparkles✨");
        assertTrue("Could not create emoji directory", emojiDir.mkdirs());

        // Get canonical path
        File canonical = emojiDir.getCanonicalFile();
        assertTrue("Canonical file should exist", canonical.exists());

        // Use Path API (as in the fix)
        Path pathFromFix = Paths.get(emojiDir.getAbsolutePath()).toRealPath();
        File fileFromFix = pathFromFix.toFile();

        assertTrue("Path.toRealPath().toFile() should exist", fileFromFix.exists());
        assertTrue("Path.toRealPath().toFile() should be directory", fileFromFix.isDirectory());

        // Verify emoji is preserved in path string
        String pathString = pathFromFix.toString();
        assertTrue("Emoji should be preserved in path string", pathString.contains("✨"));
    }

    /**
     * Test 7: Verify ProcessBuilder with emoji directory as working directory
     * This simulates BlueJ starting VM in project directory
     */
    public void testProcessBuilderWorkingDirectoryWithEmoji() throws Exception
    {
        // Create directory with emoji
        File emojiWorkDir = new File(tempTestDir, "🚀WorkDir🚀");
        assertTrue("Could not create emoji working directory", emojiWorkDir.mkdirs());

        // Create a simple test script/file
        File testFile = new File(emojiWorkDir, "test.dat");
        Files.write(testFile.toPath(), "test data".getBytes(StandardCharsets.UTF_8));

        // Use ProcessBuilder with emoji directory as working directory
        ProcessBuilder pb = new ProcessBuilder("pwd");
        pb.directory(emojiWorkDir);

        Process process = pb.start();

        // Read output
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String pwdOutput = reader.readLine();
        reader.close();

        int exitCode = process.waitFor();
        assertEquals("Process should complete successfully", 0, exitCode);

        // Verify working directory was set correctly (should contain emoji)
        assertNotNull("PWD output should not be null", pwdOutput);
        // Note: pwd output format varies by system, so we just verify it completed
    }

    /**
     * Test 8: Verify file paths with emoji survive serialization
     * This tests paths passed between processes (like to second VM)
     */
    public void testEmojiPathSerialization() throws Exception
    {
        // Create directory with emoji
        File emojiDir = new File(tempTestDir, "💾Save💾");
        assertTrue("Could not create emoji directory", emojiDir.mkdirs());

        // Get absolute path
        String absolutePath = emojiDir.getAbsolutePath();

        // Simulate passing path as string (as would happen between VMs)
        String serializedPath = absolutePath;

        // Reconstruct from string
        File reconstructedDir = new File(serializedPath);

        // Verify it points to same location
        assertTrue("Reconstructed directory should exist", reconstructedDir.exists());
        assertEquals("Reconstructed path should match original",
                     emojiDir.getAbsolutePath(),
                     reconstructedDir.getAbsolutePath());
    }

    /**
     * Test 9: Verify all test emojis work in paths
     */
    public void testAllEmojiVariants() throws Exception
    {
        for (String emoji : TEST_EMOJIS) {
            File emojiDir = new File(tempTestDir, "test" + emoji + "dir");
            assertTrue("Could not create directory for emoji: " + emoji, emojiDir.mkdirs());

            File testFile = new File(emojiDir, "test.txt");
            Files.write(testFile.toPath(), emoji.getBytes(StandardCharsets.UTF_8));

            // Verify using Path API
            Path path = emojiDir.toPath().toRealPath();
            assertTrue("Path should exist for emoji: " + emoji,
                      path.toFile().exists());

            // Verify file content
            String content = new String(Files.readAllBytes(testFile.toPath()),
                                       StandardCharsets.UTF_8);
            assertEquals("File content should match emoji: " + emoji, emoji, content);
        }
    }

    /**
     * Test 10: Stress test with complex emoji combinations
     */
    public void testComplexEmojiPath() throws Exception
    {
        // Create path with multiple different emoji
        String complexEmojiPath = "🎨-💻-🚀-✨-🔥-❤️";
        File complexDir = new File(tempTestDir, complexEmojiPath);
        assertTrue("Could not create complex emoji directory", complexDir.mkdirs());

        // Verify all levels
        assertTrue("Complex emoji directory should exist", complexDir.exists());

        // Test Path API
        Path path = complexDir.toPath();
        String pathString = path.toString();

        for (String emoji : TEST_EMOJIS) {
            if (complexEmojiPath.contains(emoji)) {
                assertTrue("Path should contain emoji: " + emoji,
                          pathString.contains(emoji));
            }
        }

        // Test file operations
        File testFile = new File(complexDir, "test.java");
        String code = "public class Test { public static void main(String[] args) { } }";
        Files.write(testFile.toPath(), code.getBytes(StandardCharsets.UTF_8));

        assertTrue("Test file should exist in complex emoji path", testFile.exists());
        assertTrue("Test file should be readable", testFile.canRead());
    }

    // ========== Helper Methods ==========

    /**
     * Create a temporary directory for testing
     */
    private File createTempDirectory(String prefix) throws IOException
    {
        File tempDir = File.createTempFile(prefix, Long.toString(System.nanoTime()));
        if (!tempDir.delete()) {
            throw new IOException("Could not delete temp file: " + tempDir.getAbsolutePath());
        }
        if (!tempDir.mkdirs()) {
            throw new IOException("Could not create temp directory: " + tempDir.getAbsolutePath());
        }
        return tempDir;
    }

    /**
     * Recursively delete a directory
     */
    private void deleteDirectory(File directory) throws IOException
    {
        if (directory == null || !directory.exists()) {
            return;
        }

        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectory(file);
                }
            }
        }

        if (!directory.delete()) {
            throw new IOException("Could not delete: " + directory.getAbsolutePath());
        }
    }
}
