/*
 This file is part of the Greenfoot program.
 Copyright (C) 2026  Poul Henriksen and Michael Kolling

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
package greenfoot.export;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import bluej.Boot;
import bluej.Config;
import bluej.compiler.CompileInputFile;
import bluej.compiler.CompileObserver;
import bluej.compiler.CompileReason;
import bluej.compiler.CompileType;
import bluej.compiler.Diagnostic;
import bluej.compiler.KotlinCompiler;

import static org.junit.Assert.*;

import threadchecker.OnThread;
import threadchecker.Tag;

/** Filesystem fixtures and real compiler/archive access shared by export tests. */
@OnThread(Tag.Any)
final class ExportTestSupport
{
    private ExportTestSupport()
    {
    }

    static void initialiseConfig(File userHome)
    {
        Properties properties = new Properties();
        properties.setProperty("bluej.debug", "true");
        properties.setProperty("bluej.userHome", userHome.getAbsolutePath());
        Config.initialise(Boot.getBluejLibDir(), properties, true);
    }

    static Path write(Path directory, String name, String content) throws IOException
    {
        Path file = directory.resolve(name);
        Files.createDirectories(file.getParent());
        return Files.writeString(file, content, StandardCharsets.UTF_8);
    }

    static Map<String, byte[]> readArchive(Path archive) throws IOException
    {
        Map<String, byte[]> contents = new TreeMap<>();
        try (ZipFile zip = new ZipFile(archive.toFile())) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.isDirectory()) {
                    try (InputStream input = zip.getInputStream(entry)) {
                        assertNull("Duplicate archive entry: " + entry.getName(),
                                contents.put(entry.getName(), input.readAllBytes()));
                    }
                }
            }
        }
        return contents;
    }

    static void extractArchive(Path archive, Path destination) throws IOException
    {
        for (Map.Entry<String, byte[]> entry : readArchive(archive).entrySet()) {
            Path file = destination.resolve(entry.getKey()).normalize();
            assertTrue("Entry must stay inside the extraction directory", file.startsWith(destination));
            Files.createDirectories(file.getParent());
            Files.write(file, entry.getValue());
        }
    }

    // Match the compiler tests: only the Gradle-supplied student stdlib, never the IDE/compiler classpath.
    static List<File> kotlinStdlib()
    {
        List<File> libraries = Arrays.stream(System.getProperty("java.class.path").split(File.pathSeparator))
                .map(File::new)
                .filter(file -> file.getName().startsWith("kotlin-stdlib") && file.isFile())
                .toList();
        assertFalse("The test runtime must supply Kotlin's stdlib", libraries.isEmpty());
        return libraries;
    }

    static void compileKotlin(Path output, Path... sources) throws IOException
    {
        Files.createDirectories(output);
        KotlinCompiler compiler = new KotlinCompiler();
        compiler.setDestDir(output.toFile());
        compiler.setClasspath(kotlinStdlib());
        List<String> diagnostics = new ArrayList<>();
        CompileObserver observer = new CompileObserver()
        {
            /**
             * Ignores lifecycle start notifications; export fixtures only collect diagnostics.
             *
             * @param inputs compilation inputs, unused
             * @param reason compilation trigger, unused
             * @param type compilation mode, unused
             * @param sequence compilation identifier, unused
             */
            @Override
            public void startCompile(CompileInputFile[] inputs, CompileReason reason, CompileType type, int sequence)
            {
            }

            /**
             * Collects diagnostic text for the fixture compilation assertion.
             *
             * @param diagnostic message emitted by the compiler
             * @param type compilation mode, unused
             */
            @Override
            public void compilerMessage(Diagnostic diagnostic, CompileType type)
            {
                diagnostics.add(diagnostic.getMessage().englishMessage());
            }

            /**
             * Ignores completion notifications; the fixture checks the compiler's return value.
             *
             * @param inputs compilation inputs, unused
             * @param success compilation result, unused
             * @param type compilation mode, unused
             * @param sequence compilation identifier, unused
             */
            @Override
            public void endCompile(CompileInputFile[] inputs, boolean success, CompileType type, int sequence)
            {
            }
        };
        boolean success = compiler.compile(Arrays.stream(sources).map(Path::toFile).toArray(File[]::new),
                observer, false, List.of(), StandardCharsets.UTF_8, CompileType.EXPLICIT_USER_COMPILE);
        assertTrue("Kotlin compilation failed: " + diagnostics, success);
    }
}
