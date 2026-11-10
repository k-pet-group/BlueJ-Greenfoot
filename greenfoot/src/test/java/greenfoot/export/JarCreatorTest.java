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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

import javax.tools.ToolProvider;

import bluej.extensions2.SourceType;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.*;

import threadchecker.OnThread;
import threadchecker.Tag;

/** Exercises real runtime JARs and source archives without launching a Project's UI/debugger. */
@OnThread(Tag.Any)
public class JarCreatorTest
{
    /** Temporary configuration shared by this class's archive builders. */
    @ClassRule
    public static final TemporaryFolder configuration = new TemporaryFolder();

    /** Each test owns fresh scenario, archive and extraction paths. */
    @Rule
    public final TemporaryFolder temporary = new TemporaryFolder();

    /** Initializes the labels used by the runtime archive constructor without opening the IDE. */
    @BeforeClass
    public static void initialise()
    {
        ExportTestSupport.initialiseConfig(configuration.getRoot());
    }

    /**
     * Source-free runtime artifacts retain compiled classes and assets, but no supported project sources.
     *
     * @throws Exception if fixture compilation or archive access fails
     */
    @Test
    public void testRuntimeExcludesAllProjectSources() throws Exception
    {
        Path project = mixedProject();
        Map<String, byte[]> original = readFiles(project);
        Path archive = runtimeArchive(project, false);
        Map<String, byte[]> contents = ExportTestSupport.readArchive(archive);

        assertRetained(original, contents, "JavaActor.class", "KotlinActor.class", "StrideActor.class",
                "images/actor.bin", "sounds/tone.wav", "notes.kt.txt", "notes.java.txt", "notes.stride.txt",
                "asset.", "assets.kt/picture.bin");
        List<String> leakedSources = contents.keySet().stream().filter(JarCreatorTest::isSource).toList();
        assertTrue("Project sources leaked into runtime JAR: " + leakedSources, leakedSources.isEmpty());
        assertFalse("Runtime archives still exclude project libraries", contents.containsKey("+libs/example.jar"));
        assertFalse(contents.containsKey("JavaActor.ctxt"));
        assertFalse(contents.containsKey("bluej.pkg"));
        assertFalse(contents.containsKey("doc/index.html"));
        assertNoKotlinRuntime(contents.keySet());
    }

    /**
     * Explicit source inclusion still works independently of the runtime/source archive format.
     *
     * @throws Exception if fixture compilation or archive access fails
     */
    @Test
    public void testRuntimeCanExplicitlyIncludeSource() throws Exception
    {
        Path project = mixedProject();
        Map<String, byte[]> original = readFiles(project);
        Map<String, byte[]> contents = ExportTestSupport.readArchive(runtimeArchive(project, true));
        for (String source : original.keySet().stream().filter(JarCreatorTest::isSource).toList()) {
            assertArrayEquals(source, original.get(source), contents.get(source));
        }
    }

    /**
     * Java-only runtime archives preserve their manifest, properties, classes and resources.
     *
     * @throws Exception if fixture compilation or archive access fails
     */
    @Test
    public void testJavaRuntimeBaseline() throws Exception
    {
        Path project = temporary.newFolder("Java Scenario").toPath();
        Path source = ExportTestSupport.write(project, "JavaActor.java", "public class JavaActor {}\n");
        compileJava(project, source);
        ExportTestSupport.write(project, "sounds/tone.wav", "sound fixture");
        ExportTestSupport.write(project, "images/actor.bin", "image fixture");
        Map<String, byte[]> original = readFiles(project);
        Path archive = temporary.newFolder("java-export").toPath().resolve("java.jar");
        JarCreator creator = new JarCreator(project.toFile(), archive.getParent().toFile(),
                archive.getFileName().toString(), "JavaActor", true);
        creator.includeSource(false);
        creator.addToClassPath("sharedjars/Greenfoot-core.jar");
        creator.putManifestEntry("title", "Java baseline");
        creator.create();

        Map<String, byte[]> contents = ExportTestSupport.readArchive(archive);
        assertEquals(Set.of("META-INF/MANIFEST.MF", "JavaActor.class", "sounds/tone.wav", "images/actor.bin",
                "standalone.properties", "soundindex.list"), contents.keySet());
        assertRetained(original, contents, "JavaActor.class", "sounds/tone.wav", "images/actor.bin");
        Manifest manifest = new Manifest(new ByteArrayInputStream(contents.get("META-INF/MANIFEST.MF")));
        Attributes attributes = manifest.getMainAttributes();
        assertEquals("1.0", attributes.getValue(Attributes.Name.MANIFEST_VERSION));
        assertEquals(GreenfootScenarioViewer.class.getName(), attributes.getValue(Attributes.Name.MAIN_CLASS));
        assertEquals("sharedjars/Greenfoot-core.jar", attributes.getValue(Attributes.Name.CLASS_PATH).trim());
        assertEquals("Java baseline", attributes.getValue("title"));
        Properties properties = new Properties();
        properties.load(new ByteArrayInputStream(contents.get("standalone.properties")));
        assertEquals(project.getFileName().toString(), properties.getProperty("project.name"));
        assertEquals("JavaActor", properties.getProperty("main.class"));
        assertEquals("true", properties.getProperty("scenario.lock"));
        assertEquals("tone.wav\n", new String(contents.get("soundindex.list"), StandardCharsets.UTF_8));
    }

    /**
     * Project GFARs retain the original prefix and files, and their extracted Kotlin source recompiles.
     *
     * @throws Exception if archive round-trip or fresh compilation fails
     */
    @Test
    public void testProjectGfarRoundTrip() throws Exception
    {
        assertSourceRoundTrip("project.gfar");
    }

    /**
     * Optional gallery source ZIPs use the same project archive format, not the runtime JAR layout.
     *
     * @throws Exception if archive round-trip or fresh compilation fails
     */
    @Test
    public void testGallerySourceZipRoundTrip() throws Exception
    {
        assertSourceRoundTrip("gallery-source.zip");
    }

    private Path mixedProject() throws IOException
    {
        Path project = temporary.newFolder("Mixed Scenario").toPath();
        Path java = ExportTestSupport.write(project, "JavaActor.java", "public class JavaActor {}\n");
        Path strideJava = ExportTestSupport.write(project, "StrideActor.java", "public class StrideActor {}\n");
        ExportTestSupport.write(project, "StrideActor.stride", """
            <?xml version="1.0" encoding="UTF-8"?>
            <class name="StrideActor" enable="true" strideversion="1">
                <javadoc/><imports/><implements/><fields/><constructors/><methods/>
            </class>
            """);
        Path kotlin = ExportTestSupport.write(project, "KotlinActor.kt", """
            class KotlinActor {
                fun message(): String = listOf("fresh", "source", "\u03bb").joinToString("-")
            }
            """);
        compileJava(project, java, strideJava);
        ExportTestSupport.compileKotlin(project, kotlin);
        ExportTestSupport.write(project, "nested/NestedJava.java", "package nested; public class NestedJava {}\n");
        ExportTestSupport.write(project, "nested/NestedKotlin.kt", "package nested\nclass NestedKotlin\n");
        ExportTestSupport.write(project, "nested/NestedStride.stride", "<class name=\"NestedStride\"/>\n");
        ExportTestSupport.write(project, "project.greenfoot", """
            #Greenfoot project file
            project.charset=UTF-8
            simulation.speed=42
            target1.name=KotlinActor
            target1.type=ClassTarget
            package.numTargets=1
            """);
        ExportTestSupport.write(project, "README.TXT", "Sources and assets: \u03bb\n");
        ExportTestSupport.write(project, "JavaActor.ctxt", "#BlueJ context information\n");
        for (String resource : List.of("notes.kt.txt", "notes.java.txt", "notes.stride.txt", "asset.",
                "assets.kt/picture.bin", "images/actor.bin", "sounds/tone.wav")) {
            Path file = ExportTestSupport.write(project, resource, "");
            Files.write(file, new byte[] {0, 1, 2, (byte) 255, 42});
        }
        Path library = project.resolve("+libs/example.jar");
        Files.createDirectories(library.getParent());
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(library))) {
            jar.putNextEntry(new ZipEntry("example/resource.txt"));
            jar.write("project library".getBytes(StandardCharsets.UTF_8));
            jar.closeEntry();
        }
        for (String excluded : List.of("bluej.pkg", "bluej.pkh", "doc/index.html", "CVS/Entries",
                ".svn/entries", ".cvsignore", ".DS_Store", "greenfoot/internal.txt")) {
            ExportTestSupport.write(project, excluded, "excluded fixture\n");
        }
        return project;
    }

    private Path runtimeArchive(Path project, boolean includeSource) throws IOException
    {
        Path archive = temporary.newFolder().toPath().resolve("runtime.jar");
        JarCreator creator = new JarCreator(project.toFile(), archive.getParent().toFile(),
                archive.getFileName().toString(), "JavaActor", false);
        creator.includeSource(includeSource);
        creator.create();
        assertTrue("The archive must actually exist", Files.isRegularFile(archive));
        return archive;
    }

    private void assertSourceRoundTrip(String archiveName) throws Exception
    {
        Path project = mixedProject();
        // Keep export inside the project to check that it is not recursively archived.
        Path export = Files.createDirectory(project.resolve("export"));
        ExportTestSupport.write(export, "previous.zip", "previous export");
        Map<String, byte[]> original = readFiles(project);
        Path archive = export.resolve(archiveName);
        new JarCreator(project.toFile(), export.toFile(), archiveName).create();
        Map<String, byte[]> contents = ExportTestSupport.readArchive(archive);
        String prefix = project.getFileName() + "/";
        Set<String> excluded = Set.of("bluej.pkg", "bluej.pkh", "doc/index.html", "CVS/Entries", ".svn/entries",
                ".cvsignore", ".DS_Store", "greenfoot/internal.txt", "export/previous.zip");
        Set<String> expectedEntries = new TreeSet<>();
        for (Map.Entry<String, byte[]> entry : original.entrySet()) {
            if (!excluded.contains(entry.getKey())) {
                String archivePath = prefix + entry.getKey();
                expectedEntries.add(archivePath);
                assertArrayEquals(archivePath, entry.getValue(), contents.get(archivePath));
            }
        }
        assertEquals("Source archives keep the project-directory prefix and add no runtime or manifest",
                expectedEntries, contents.keySet());
        assertNoKotlinRuntime(contents.keySet());

        Path extraction = temporary.newFolder("extracted").toPath();
        ExportTestSupport.extractArchive(archive, extraction);
        Path extractedProject = extraction.resolve(project.getFileName());
        for (String entry : expectedEntries) {
            assertArrayEquals(entry, contents.get(entry), Files.readAllBytes(extraction.resolve(entry)));
        }
        // Source archives preserve existing output; remove it before testing the recipient-side compile.
        try (Stream<Path> files = Files.walk(extractedProject)) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                if (file.toString().endsWith(".class") || file.toString().endsWith(".kotlin_module")) {
                    Files.delete(file);
                }
            }
        }
        Path output = temporary.newFolder("recompiled").toPath();
        ExportTestSupport.compileKotlin(output, extractedProject.resolve("KotlinActor.kt"));
        List<URL> urls = new ArrayList<>();
        urls.add(output.toUri().toURL());
        for (File library : ExportTestSupport.kotlinStdlib()) {
            urls.add(library.toURI().toURL());
        }
        try (URLClassLoader loader = new URLClassLoader(urls.toArray(URL[]::new), ClassLoader.getPlatformClassLoader())) {
            Class<?> actor = loader.loadClass("KotlinActor");
            assertEquals("fresh-source-\u03bb", actor.getMethod("message").invoke(actor.getConstructor().newInstance()));
        }
    }

    private static void compileJava(Path output, Path... sources)
    {
        List<String> arguments = new ArrayList<>(List.of("--release", "21", "-d", output.toString()));
        for (Path source : sources) {
            arguments.add(source.toString());
        }
        ByteArrayOutputStream diagnostics = new ByteArrayOutputStream();
        assertNotNull("Tests require a JDK", ToolProvider.getSystemJavaCompiler());
        int result = ToolProvider.getSystemJavaCompiler().run(null, diagnostics, diagnostics, arguments.toArray(String[]::new));
        assertEquals(diagnostics.toString(StandardCharsets.UTF_8), 0, result);
    }

    private static Map<String, byte[]> readFiles(Path directory) throws IOException
    {
        Map<String, byte[]> files = new TreeMap<>();
        try (Stream<Path> paths = Files.walk(directory)) {
            for (Path path : paths.filter(Files::isRegularFile).toList()) {
                files.put(directory.relativize(path).toString().replace(File.separatorChar, '/'), Files.readAllBytes(path));
            }
        }
        return files;
    }

    private static boolean isSource(String name)
    {
        for (SourceType type : SourceType.values()) {
            if (type != SourceType.NONE && name.endsWith("." + type.getExtension())) {
                return true;
            }
        }
        return false;
    }

    private static void assertRetained(Map<String, byte[]> original, Map<String, byte[]> contents, String... names)
    {
        for (String name : names) {
            assertNotNull("Fixture must contain " + name, original.get(name));
            assertArrayEquals(name, original.get(name), contents.get(name));
        }
    }

    private static void assertNoKotlinRuntime(Set<String> entries)
    {
        assertFalse("Source archives and plain JarCreator calls must not automatically inject stdlib",
                entries.stream().anyMatch(name -> name.contains("kotlin-stdlib") || name.endsWith("kotlin/Unit.class")));
    }
}
