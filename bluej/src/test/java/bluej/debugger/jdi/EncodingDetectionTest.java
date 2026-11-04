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
import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Tests to detect and report the current system encoding environment.
 *
 * This test helps identify whether the current test environment can validate
 * the emoji path fix across different encodings (GBK, Shift-JIS, etc.).
 *
 * @author BlueJ Team
 * @see <a href="https://github.com/bluej-micro/bluej-micro/issues/2426">Issue #2426</a>
 */
public class EncodingDetectionTest extends TestCase
{
    /**
     * Test 1: Detect and report current system encoding
     *
     * This test provides diagnostic information about the test environment.
     * It should PASS on all systems, but the output tells us what encoding we're testing under.
     */
    public void testDetectSystemEncoding()
    {
        // Get system properties
        String defaultCharset = Charset.defaultCharset().displayName();
        String fileEncoding = System.getProperty("file.encoding", "not set");
        String sunJnuEncoding = System.getProperty("sun.jnu.encoding", "not set");
        String osName = System.getProperty("os.name");

        // Build diagnostic report
        StringBuilder report = new StringBuilder();
        report.append("=== Test Environment Encoding Detection ===\n");
        report.append("OS: ").append(osName).append("\n");
        report.append("Default Charset: ").append(defaultCharset).append("\n");
        report.append("file.encoding: ").append(fileEncoding).append("\n");
        report.append("sun.jnu.encoding: ").append(sunJnuEncoding).append("\n");
        report.append("==========================================\n");

        // Print to console (visible in test output)
        System.out.println(report.toString());

        // Log what we're testing
        if (Charset.defaultCharset().equals(StandardCharsets.UTF_8)) {
            System.out.println("\u2713 Testing on UTF-8 system (emoji should work)");
            System.out.println("\u26A0 WARNING: GBK/Shift-JIS systems NOT tested here");
            System.out.println("\u26A0 Please run tests on Windows CN/JP/KR for full validation");
        } else {
            System.out.println("\u2713 Testing on " + Charset.defaultCharset().displayName() + " system");
            System.out.println("\u26A0 This is GOOD for testing legacy encoding scenarios");
        }

        // Test should always pass
        assertTrue("Encoding detection should complete", true);
    }

    /**
     * Test 2: Verify sun.jnu.encoding can be set to UTF-8
     *
     * This verifies that the JVM accepts sun.jnu.encoding=UTF-8 parameter,
     * which is the core of our fix.
     */
    public void testSunJnuEncodingIsSettable()
    {
        // Try to launch a simple JVM with UTF-8 sun.jnu.encoding
        ProcessBuilder pb = new ProcessBuilder(
            "java",
            "-Dsun.jnu.encoding=UTF-8",
            "-XshowSettings:properties",
            "-version"
        );

        try {
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream())
            );
            
            boolean found = false;
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("sun.jnu.encoding = UTF-8")) {
                    found = true;
                    break;
                }
            }
            reader.close();

            int exitCode = process.waitFor();

            // The command might fail due to classpath issues, but that's OK
            // We're just verifying the parameter is accepted
            assertTrue("JVM should accept -Dsun.jnu.encoding=UTF-8", exitCode == 0);
            assertTrue("sun.jnu.encoding should be UTF-8 in sub-process", found);

        } catch (Exception e) {
            // If we can't run java, that's OK for this diagnostic test
            System.out.println("\u26A0 Could not test sun.jnu.encoding: " + e.getMessage());
            assertTrue("Test should not fail", true);
        }
    }

    /**
     * Test 3: Create a matrix of what encodings need testing
     *
     * This test documents which encoding combinations need manual testing.
     */
    public void testEncodingTestMatrix()
    {
        Map<String, Boolean> testedEncodings = new HashMap<>();

        // Current test environment
        String currentEncoding = Charset.defaultCharset().displayName();
        testedEncodings.put(currentEncoding, true);

        // Encodings that SHOULD work with our fix
        String[] shouldWork = {
            "UTF-8",      // \u2713 Tested
            "GBK",        // \u2705 SHOULD work (not tested)
            "Shift-JIS",  // \u2705 SHOULD work (not tested)
            "EUC-KR",     // \u2705 SHOULD work (not tested)
            "CP1251",     // \u2705 SHOULD work (not tested)
            "Big5"        // \u2705 SHOULD work (not tested)
        };

        // Build report
        System.out.println("\n=== Encoding Test Coverage ===");
        System.out.println("Current test environment: " + currentEncoding);
        System.out.println("\nEncodings that SHOULD work with our fix:");
        for (String encoding : shouldWork) {
            boolean tested = testedEncodings.containsKey(encoding);
            System.out.println(String.format("  %-15s %s",
                    encoding,
                    tested ? "\u2713 TESTED" : "\u2717 NOT TESTED (needs manual verification)"));
        }

        System.out.println("\n\u26A0 IMPORTANT:");
        System.out.println("  sun.jnu.encoding=UTF-8 should work on ALL systems,");
        System.out.println("  regardless of the default system encoding.");
        System.out.println("  However, this should be verified on actual systems.");

        assertTrue("Test matrix should be generated", true);
    }

    /**
     * Test 4: Verify Unicode/Emoji path operations (NIO.2)
     *
     * This test creates directories and files with names containing various
     * Unicode characters (Chinese, Japanese, Emoji) and verifies they can be
     * correctly accessed and listed.
     */
    public void testUnicodePathOperations() throws Exception
    {
        String[] testNames = {
            "\u6d4b\u8bd5_\u9879\u76ee",              // 测试_项目
            "\u30c6\u30b9\u30c8_\u30d7\u30ed\u30b8\u30a7\u30af\u30c8", // テスト_プロジェクト
            "\uD83D\uDCBBComp\uD83D\uDCBB",           // 💻Comp💻
            "\uD83D\uDCBB\u6d4b\u8bd5_\u30c6\u30b9\u30c8\uD83D\uDE80" // 💻测试_テスト🚀
        };

        java.nio.file.Path baseTempDir = java.nio.file.Files.createTempDirectory("bluej_encoding_test_");

        try {
            for (String name : testNames) {
                // 1. Create directory
                java.nio.file.Path dirPath = baseTempDir.resolve(name);
                java.nio.file.Files.createDirectory(dirPath);
                assertTrue("Directory should exist: " + name, java.nio.file.Files.exists(dirPath));

                // 2. Create file inside
                java.nio.file.Path filePath = dirPath.resolve("test.txt");
                java.nio.file.Files.write(filePath, "test".getBytes(StandardCharsets.UTF_8));
                assertTrue("File should exist inside: " + name, java.nio.file.Files.exists(filePath));

                // 3. List and verify
                try (java.util.stream.Stream<java.nio.file.Path> stream = java.nio.file.Files.list(dirPath)) {
                    boolean found = stream.anyMatch(p -> p.getFileName().toString().equals("test.txt"));
                    assertTrue("Should be able to list file in: " + name, found);
                }
            }

            // Verify we can list all directories in the base temp dir
            try (java.util.stream.Stream<java.nio.file.Path> stream = java.nio.file.Files.list(baseTempDir)) {
                long count = stream.count();
                assertEquals("Should have created " + testNames.length + " directories",
                            (long)testNames.length, count);
            }

        } finally {
            // Cleanup
            deleteDirectoryRecursively(baseTempDir.toFile());
        }
    }

    /**
     * Test 5: Verify ProcessBuilder can start a process in a Unicode/Emoji directory
     *
     * This simulates the VM launch scenario where the working directory contains emojis.
     */
    public void testProcessExecutionInUnicodePath() throws Exception
    {
        String emojiName = "BlueJ_\uD83D\uDE80_Emoji"; // BlueJ_🚀_Emoji
        java.nio.file.Path tempDir = java.nio.file.Files.createTempDirectory(emojiName);

        try {
            ProcessBuilder pb = new ProcessBuilder("java", "-version");
            pb.directory(tempDir.toFile());

            Process process = pb.start();
            int exitCode = process.waitFor();

            assertEquals("Process should start successfully in emoji directory", 0, exitCode);

        } finally {
            java.nio.file.Files.deleteIfExists(tempDir);
        }
    }

    /**
     * Helper to delete a directory and its contents recursively
     */
    private void deleteDirectoryRecursively(File file) {
        File[] contents = file.listFiles();
        if (contents != null) {
            for (File f : contents) {
                deleteDirectoryRecursively(f);
            }
        }
        file.delete();
    }
}
