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
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Stream;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;

import bluej.Config;
import bluej.extensions2.SourceType;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.*;
import static org.junit.Assume.*;

import threadchecker.OnThread;
import threadchecker.Tag;

/** Real publishing archives with the installed student runtime, without an IDE loader or gallery. */
@OnThread(Tag.Any)
public class KotlinRuntimeTest
{
    private static final List<String> LICENCE_NAMES = List.of("LICENSE.txt", "NOTICE.txt", "README.txt");

    /** Temporary configuration for the archive builders. */
    @ClassRule
    public static final TemporaryFolder configuration = new TemporaryFolder();

    /** Fresh scenario and installation paths for each test. */
    @Rule
    public final TemporaryFolder temporary = new TemporaryFolder();

    /** Initializes labels without opening a live export dialog. */
    @BeforeClass
    public static void initialise()
    {
        ExportTestSupport.initialiseConfig(configuration.getRoot());
    }

    /**
     * Compiled Kotlin executes using only the produced JAR and platform classes; stdlib resources and legal text survive.
     *
     * @throws Exception if fixture compilation, packaging or isolated execution fails
     */
    @Test
    public void testPublishedKotlinExecutesWithEmbeddedStdlib() throws Exception
    {
        Path project = project();
        Path source = ExportTestSupport.write(project, "KotlinEntry.kt", """
            class KotlinEntry {
                fun message(): String = listOf("bundled", "stdlib", "λ").map { it.uppercase() }.joinToString("-")
            }
            """);
        ExportTestSupport.compileKotlin(project, source);
        Path stdlib = installedStdlib();
        KotlinRuntime runtime = prepare(stdlib);
        Path archive = runtimeArchive(project, runtime);
        Map<String, byte[]> contents = ExportTestSupport.readArchive(archive);
        assertTrue("The publishing JAR must embed the required student stdlib", contents.containsKey("kotlin/Unit.class"));
        assertFalse(contents.containsKey("KotlinEntry.kt"));
        assertTrue(contents.containsKey("META-INF/kotlin-stdlib.kotlin_module"));
        for (Map.Entry<String, byte[]> entry : ExportTestSupport.readArchive(stdlib).entrySet()) {
            if (!entry.getKey().equals("META-INF/MANIFEST.MF")) {
                assertArrayEquals("Retained stdlib entry: " + entry.getKey(), entry.getValue(), contents.get(entry.getKey()));
            }
        }
        for (String name : LICENCE_NAMES) {
            Path staged = Config.getGreenfootLibDir().toPath().resolve("export/kotlin/" + name);
            // Use the distribution resources copied by Gradle, not a source-tree-only legal-text lookup.
            URL resource = getClass().getResource("/lib/greenfoot/export/kotlin/" + name);
            assertNotNull("Legal text must be staged in the installed-style lib layout", resource);
            assertEquals(staged.toRealPath(), Path.of(resource.toURI()).toRealPath());
            assertArrayEquals(Files.readAllBytes(staged), contents.get("META-INF/kotlin/" + name));
        }
        assertTrue(text(contents, "META-INF/kotlin/NOTICE.txt").contains("Kotlin Compiler"));
        assertTrue(text(contents, "META-INF/kotlin/README.txt").contains("not the\nKotlin compiler"));
        assertNoCompilerOrNestedJars(contents.keySet());
        Manifest manifest = new Manifest(new ByteArrayInputStream(contents.get("META-INF/MANIFEST.MF")));
        assertEquals("The isolated loader must not follow any external runtime URLs", "",
                manifest.getMainAttributes().getValue(Attributes.Name.CLASS_PATH).trim());
        try (URLClassLoader loader = new URLClassLoader(new URL[] {archive.toUri().toURL()},
                ClassLoader.getPlatformClassLoader())) {
            Class<?> entry = loader.loadClass("KotlinEntry");
            assertEquals("BUNDLED-STDLIB-Λ", entry.getMethod("message").invoke(entry.getConstructor().newInstance()));
            assertSame(loader, loader.loadClass("kotlin.Unit").getClassLoader());
            assertEquals(archive.toUri().toURL(), loader.loadClass("kotlin.Unit").getProtectionDomain().getCodeSource().getLocation());
            assertNotNull(loader.getResource("META-INF/kotlin/LICENSE.txt"));
            for (String excluded : List.of("org.jetbrains.kotlin.cli.jvm.K2JVMCompiler",
                    "org.jetbrains.kotlin.com.intellij.psi.PsiElement", "bluej.compiler.KotlinCompiler",
                    "kotlin.reflect.jvm.internal.ReflectionFactoryImpl")) {
                try {
                    loader.loadClass(excluded);
                    fail("No IDE/compiler class should be available: " + excluded);
                }
                catch (ClassNotFoundException expected) {
                    // Platform parent only, with an empty manifest classpath: no IDE or network fallback.
                }
            }
        }
    }

    /** The decision uses all source types, never the package's preferred language or compiled-only targets. */
    @Test
    public void testKotlinSourceSelectionIncludesMixedScenarios()
    {
        assertFalse(Exporter.hasKotlinSources(Stream.empty()));
        assertFalse(Exporter.hasKotlinSources(Stream.of(SourceType.NONE)));
        assertFalse(Exporter.hasKotlinSources(Stream.of(SourceType.Java)));
        assertFalse(Exporter.hasKotlinSources(Stream.of(SourceType.Stride, SourceType.Java, SourceType.NONE)));
        assertTrue(Exporter.hasKotlinSources(Stream.of(SourceType.Kotlin)));
        assertTrue(Exporter.hasKotlinSources(Stream.of(SourceType.Java, SourceType.Kotlin)));
        assertTrue(Exporter.hasKotlinSources(Stream.of(SourceType.Stride, SourceType.Java, SourceType.Kotlin)));
        assertTrue(Exporter.hasKotlinSources(Stream.of(SourceType.Kotlin, SourceType.Stride)));
    }

    /**
     * Java/Stride publishing neither looks up nor adds Kotlin inputs, even when the installation has no stdlib.
     *
     * @throws Exception if archive creation or reading fails
     */
    @Test
    public void testJavaAndStrideAvoidRuntimeLookupAndBundling() throws Exception
    {
        for (List<SourceType> types : List.of(List.of(SourceType.Java), List.of(SourceType.Stride),
                List.of(SourceType.Java, SourceType.Stride, SourceType.NONE))) {
            KotlinRuntime runtime = KotlinRuntime.prepare(Exporter.hasKotlinSources(types.stream()),
                    () -> { throw new AssertionError("Java/Stride must not consult the Kotlin classpath"); },
                    () -> { throw new AssertionError("Java/Stride must not consult Kotlin licensing"); });
            Path project = project();
            ExportTestSupport.write(project, "JavaEntry.java", "class JavaEntry {}\n");
            ExportTestSupport.write(project, "StrideEntry.stride", "<class name=\"StrideEntry\"/>\n");
            ExportTestSupport.write(project, "asset.txt", "retained");
            Map<String, byte[]> contents = ExportTestSupport.readArchive(runtimeArchive(project, runtime));
            assertEquals(Set.of("META-INF/MANIFEST.MF", "standalone.properties", "soundindex.list", "asset.txt"),
                    contents.keySet());
        }
        // The production entry point must also avoid touching Boot when Kotlin isn't needed.
        KotlinRuntime.prepare(false);
    }

    /**
     * Existing external dependencies and scenario metadata remain intact; Kotlin adds no external dependency URL.
     *
     * @throws Exception if runtime preparation or packaging fails
     */
    @Test
    public void testExistingManifestReferencesRemainUnchanged() throws Exception
    {
        Path archive = temporary.newFolder().toPath().resolve("runtime.jar");
        JarCreator creator = new JarCreator(project().toFile(), archive.getParent().toFile(), "runtime.jar", "World", true);
        List<String> external = List.of("sharedjars/Greenfoot-core-3.1.jar", "sharedjars/jl1.0.1.jar", "sharedjars/lang-stride.jar");
        for (String path : external) {
            creator.addToClassPath(path);
        }
        creator.putManifestEntry("title", "Existing scenario");
        creator.putManifestEntry("width", "640");
        creator.putManifestEntry("height", "480");
        prepare(installedStdlib()).addTo(creator);
        creator.create();
        Map<String, byte[]> contents = ExportTestSupport.readArchive(archive);
        Manifest manifest = new Manifest(new ByteArrayInputStream(contents.get("META-INF/MANIFEST.MF")));
        Attributes attributes = manifest.getMainAttributes();
        assertEquals(String.join(" ", external), attributes.getValue(Attributes.Name.CLASS_PATH).trim());
        assertEquals("Existing scenario", attributes.getValue("title"));
        assertEquals("640", attributes.getValue("width"));
        assertEquals("480", attributes.getValue("height"));
        assertNoCompilerOrNestedJars(contents.keySet());
    }

    /**
     * Source GFARs and source ZIPs remain ordinary source archives after a Kotlin runtime has been prepared.
     *
     * @throws Exception if runtime preparation or packaging fails
     */
    @Test
    public void testSourceArchivesDoNotAutomaticallyGainRuntime() throws Exception
    {
        Path project = project();
        ExportTestSupport.write(project, "KotlinEntry.kt", "class KotlinEntry\n");
        ExportTestSupport.write(project, "project.greenfoot", "project.charset=UTF-8\n");
        prepare(installedStdlib());
        for (String filename : List.of("project.gfar", "gallery-source.zip")) {
            Path export = temporary.newFolder().toPath();
            new JarCreator(project.toFile(), export.toFile(), filename).create();
            Map<String, byte[]> contents = ExportTestSupport.readArchive(export.resolve(filename));
            String prefix = project.getFileName() + "/";
            assertEquals(Set.of(prefix + "KotlinEntry.kt", prefix + "project.greenfoot"), contents.keySet());
            assertEquals("class KotlinEntry\n", text(contents, prefix + "KotlinEntry.kt"));
        }
    }

    /**
     * Runtime classpath order wins over filesystem versions; encoded spaces, unrelated JARs and adapters are safe.
     *
     * @throws Exception if staging, preparation or packaging fails
     */
    @Test
    public void testResolvesFirstSelectedStdlibWithPathSpaces() throws Exception
    {
        Path install = temporary.newFolder("Greenfoot Install With Spaces").toPath();
        Path selected = runtimeFixture(install.resolve("kotlin-stdlib-2.1.20.jar"), "selected");
        Path newer = runtimeFixture(install.resolve("kotlin-stdlib-99.0.jar"), "newer");
        runtimeFixture(install.resolve("kotlin-stdlib-999.0.jar"), "unselected on disk");
        Path adapter = ExportTestSupport.write(install, "kotlin-stdlib-jdk8-2.1.20.jar", "not the main stdlib");
        Path compiler = ExportTestSupport.write(install, "kotlin-compiler-embeddable-2.1.20.jar", "not a student runtime");
        Path legalDir = stageLicences();
        URL[] classPath = {compiler.toUri().toURL(), adapter.toUri().toURL(), selected.toUri().toURL(), newer.toUri().toURL()};
        assertTrue("Fixture must exercise URL decoding, not a literal filesystem URL path", classPath[2].toString().contains("%20"));
        KotlinRuntime runtime = KotlinRuntime.prepare(true, () -> classPath, () -> legalDir.toFile());
        Map<String, byte[]> contents = ExportTestSupport.readArchive(runtimeArchive(project(), runtime));
        assertEquals("selected", text(contents, "selection.txt"));
        assertArrayEquals(Files.readAllBytes(legalDir.resolve("export/kotlin/LICENSE.txt")), contents.get("META-INF/kotlin/LICENSE.txt"));
    }

    /**
     * Missing classpath entries and remote locations never trigger a filesystem search or download.
     *
     * @throws Exception if fixture paths cannot be constructed
     */
    @Test
    public void testMissingRuntimePreflightFails() throws Exception
    {
        assertPreflightFails(new URL[0]);
        assertPreflightFails(null);
        assertPreflightFails(new URL[] {URI.create("https://example.invalid/kotlin-stdlib-2.1.20.jar").toURL()});
        Path dir = temporary.newFolder().toPath();
        assertPreflightFails(new URL[] {dir.resolve("kotlin-stdlib-2.1.20.jar").toUri().toURL()});
        Path directory = Files.createDirectory(dir.resolve("kotlin-stdlib-99.0.jar"));
        assertPreflightFails(new URL[] {directory.toUri().toURL()});
    }

    /**
     * Non-JAR, non-stdlib, truncated ZIP and entry-level corruption are rejected before packaging.
     *
     * @throws Exception if fixture creation fails
     */
    @Test
    public void testInvalidRuntimePreflightFails() throws Exception
    {
        Path dir = temporary.newFolder().toPath();
        Path nonJar = ExportTestSupport.write(dir, "kotlin-stdlib-1.jar", "not a jar");
        assertPreflightFails(new URL[] {nonJar.toUri().toURL()});
        Path empty = dir.resolve("kotlin-stdlib-2.jar");
        try (JarOutputStream ignored = new JarOutputStream(Files.newOutputStream(empty))) {
            // A readable ZIP is not necessarily the required runtime.
        }
        assertPreflightFails(new URL[] {empty.toUri().toURL()});
        Path truncated = dir.resolve("kotlin-stdlib-3.jar");
        Files.write(truncated, Arrays.copyOf(Files.readAllBytes(installedStdlib()), 100));
        assertPreflightFails(new URL[] {truncated.toUri().toURL()});
        Path corrupt = runtimeFixture(dir.resolve("kotlin-stdlib-4.jar"), "unique stored marker");
        byte[] bytes = Files.readAllBytes(corrupt);
        byte[] marker = "unique stored marker".getBytes(StandardCharsets.UTF_8);
        int offset = indexOf(bytes, marker);
        assertTrue("Find the stored entry data, leaving its recorded CRC and ZIP structure intact", offset >= 0);
        bytes[offset] ^= 1;
        Files.write(corrupt, bytes);
        assertPreflightFails(new URL[] {corrupt.toUri().toURL()});
        // Never silently substitute a later readable version for the selected broken installation entry.
        assertPreflightFails(new URL[] {nonJar.toUri().toURL(), installedStdlib().toUri().toURL()});
    }

    /**
     * A genuinely unreadable selected runtime is rejected (POSIX permission fixture).
     *
     * @throws Exception if staging or permission changes fail
     */
    @Test
    public void testUnreadableRuntimePreflightFails() throws Exception
    {
        Path runtime = Files.copy(installedStdlib(), temporary.newFolder().toPath().resolve("kotlin-stdlib-2.1.20.jar"));
        assumeTrue(runtime.getFileSystem().supportedFileAttributeViews().contains("posix"));
        Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(runtime);
        try {
            Files.setPosixFilePermissions(runtime, Set.of());
            assumeFalse("Privileged users may still read files without permission bits", Files.isReadable(runtime));
            assertPreflightFails(new URL[] {runtime.toUri().toURL()});
        }
        finally {
            Files.setPosixFilePermissions(runtime, permissions);
        }
    }

    /**
     * A Kotlin artifact must not be created without its installed licensing assets.
     *
     * @throws Exception if staging or permission changes fail
     */
    @Test
    public void testMissingOrEmptyLicensingPreflightFails() throws Exception
    {
        URL[] classPath = {installedStdlib().toUri().toURL()};
        for (String name : LICENCE_NAMES) {
            Path legalDir = stageLicences();
            Path licence = legalDir.resolve("export/kotlin/" + name);
            Files.delete(licence);
            expectIOException(() -> KotlinRuntime.prepare(true, () -> classPath, () -> legalDir.toFile()));
            Files.createFile(licence);
            expectIOException(() -> KotlinRuntime.prepare(true, () -> classPath, () -> legalDir.toFile()));
        }
    }

    private Path project() throws IOException
    {
        Path project = temporary.newFolder().toPath();
        Files.createDirectory(project.resolve("sounds"));
        return project;
    }

    private static Path installedStdlib()
    {
        Path stdlib = Config.getBlueJLibDir().toPath().resolve(ExportTestSupport.kotlinStdlib().getFirst().getName());
        assertTrue("The build must stage the runtime JAR in the installed lib layout", Files.isRegularFile(stdlib));
        return stdlib;
    }

    private static KotlinRuntime prepare(Path stdlib) throws IOException
    {
        URL[] classPath = {stdlib.toUri().toURL()};
        return KotlinRuntime.prepare(true, () -> classPath, Config::getGreenfootLibDir);
    }

    private Path runtimeArchive(Path project, KotlinRuntime runtime) throws IOException
    {
        Path archive = temporary.newFolder().toPath().resolve("published.jar");
        JarCreator creator = new JarCreator(project.toFile(), archive.getParent().toFile(), "published.jar", "KotlinEntry", false);
        creator.includeSource(false);
        runtime.addTo(creator);
        creator.create();
        assertTrue("The archive must actually exist", Files.isRegularFile(archive));
        return archive;
    }

    private Path stageLicences() throws IOException
    {
        Path greenfootLibDir = temporary.newFolder().toPath().resolve("Greenfoot Install/lib/greenfoot");
        Path licenceDir = Files.createDirectories(greenfootLibDir.resolve("export/kotlin"));
        for (String name : LICENCE_NAMES) {
            Files.copy(Config.getGreenfootLibDir().toPath().resolve("export/kotlin/" + name), licenceDir.resolve(name));
        }
        return greenfootLibDir;
    }

    private static Path runtimeFixture(Path file, String selection) throws IOException
    {
        Map<String, byte[]> installed = ExportTestSupport.readArchive(installedStdlib());
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(file))) {
            for (String name : List.of("kotlin/Unit.class", "kotlin/jvm/internal/Intrinsics.class", "selection.txt")) {
                byte[] data = name.equals("selection.txt") ? selection.getBytes(StandardCharsets.UTF_8) : installed.get(name);
                CRC32 crc = new CRC32();
                crc.update(data);
                ZipEntry entry = new ZipEntry(name);
                entry.setMethod(ZipEntry.STORED);
                entry.setSize(data.length);
                entry.setCrc(crc.getValue());
                jar.putNextEntry(entry);
                jar.write(data);
                jar.closeEntry();
            }
        }
        return file;
    }

    private static int indexOf(byte[] bytes, byte[] marker)
    {
        for (int offset = 0; offset <= bytes.length - marker.length; offset++) {
            if (Arrays.equals(bytes, offset, offset + marker.length, marker, 0, marker.length)) {
                return offset;
            }
        }
        return -1;
    }

    private static String text(Map<String, byte[]> contents, String path)
    {
        assertNotNull("Required archive entry: " + path, contents.get(path));
        return new String(contents.get(path), StandardCharsets.UTF_8);
    }

    private static void assertNoCompilerOrNestedJars(Set<String> entries)
    {
        for (String name : entries) {
            assertFalse("No nested Kotlin or other library JAR: " + name, name.endsWith(".jar"));
            assertFalse("No IDE/compiler internals: " + name, name.startsWith("org/jetbrains/kotlin/")
                    || name.startsWith("com/intellij/") || name.startsWith("bluej/")
                    || name.startsWith("kotlin/reflect/jvm/internal/"));
        }
    }

    private static void assertPreflightFails(URL[] classPath) throws IOException
    {
        expectIOException(() -> KotlinRuntime.prepare(true, () -> classPath,
                () -> { throw new AssertionError("Broken runtime must fail before licence lookup or artifact creation"); }));
    }

    private static void expectIOException(IOAction action) throws IOException
    {
        try {
            action.run();
            fail("Required runtime setup must fail before creating an artifact");
        }
        catch (IOException expected) {
            assertNotNull("Diagnostic log needs a useful failure reason", expected.getMessage());
        }
    }

    private interface IOAction
    {
        void run() throws IOException;
    }
}
