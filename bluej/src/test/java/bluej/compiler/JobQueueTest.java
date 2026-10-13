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
package bluej.compiler;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import bluej.Boot;
import bluej.Config;
import bluej.classmgr.BPClassLoader;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.*;

/** Behavioral regressions using the real compiler queue, K2, javac and student classloader. */
public class JobQueueTest
{
    /** Source and output directories owned by each test. */
    @Rule
    public final TemporaryFolder temporary = new TemporaryFolder();

    /**
     * Initializes BlueJ without starting its UI.
     *
     * @throws IOException if the temporary home cannot be created
     */
    @BeforeClass
    public static void initialise() throws IOException
    {
        initialiseConfig(false);
    }

    /**
     * Compiles a fresh mutual-reference pair with one complete lifecycle.
     *
     * @throws Exception on test failure
     */
    @Test(timeout = 60000)
    public void testMutualReferencesHaveOneLifecycle() throws Exception
    {
        Path dir = temporary.newFolder().toPath();
        CompileInputFile[] inputs = mutualSources(dir);
        Observer observer = compile(dir, CompileType.EXPLICIT_USER_COMPILE, inputs);
        observer.assertResult(inputs, CompileType.EXPLICIT_USER_COMPILE, true);
        assertTrue(observer.allOutputsAtEnd);
        try (BPClassLoader loader = loader(dir)) {
            assertEquals(7, invoke(loader, "KotlinActor"));
            assertThrows(ClassNotFoundException.class,
                    () -> loader.loadClass("org.jetbrains.kotlin.cli.jvm.K2JVMCompiler"));
        }
    }

    /**
     * Queued requests own their paths/options, but not source contents.
     *
     * @throws Exception on test failure
     */
    @Test(timeout = 60000)
    public void testQueuedRequestsAreIsolated() throws Exception
    {
        Path gateDir = temporary.newFolder().toPath();
        CompileInputFile gateInput = source(gateDir, "Gate.java", "public class Gate {}");
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Observer gate = new Observer()
        {
            /**
             * Records the start and holds the queue while later requests are enqueued.
             *
             * @param sources inputs for the gated compilation
             * @param reason compilation trigger
             * @param type compilation mode
             * @param sequence compilation identifier
             * @throws IllegalStateException if the gate times out or its wait is interrupted
             */
            @Override
            public void startCompile(CompileInputFile[] sources, CompileReason reason, CompileType type, int sequence)
            {
                super.startCompile(sources, reason, type, sequence);
                entered.countDown();
                try {
                    if (!release.await(30, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Queue gate timed out");
                    }
                }
                catch (InterruptedException e) {
                    throw new IllegalStateException(e);
                }
            }
        };
        Path a = temporary.newFolder().toPath();
        Path b = temporary.newFolder().toPath();
        CompileInputFile[] aInputs = helperSources(a, "A", 10);
        CompileInputFile[] originalA = aInputs.clone();
        CompileInputFile[] bInputs = helperSources(b, "B", 20);
        Observer aObserver = new Observer();
        Observer bObserver = new Observer();
        String options = Config.getPropString(Compiler.COMPILER_OPTIONS, "");
        try (BPClassLoader gateLoader = loader(gateDir); BPClassLoader aLoader = loader(a);
                BPClassLoader bLoader = loader(b)) {
            enqueue(gateDir, gateLoader, gate, CompileType.EXPLICIT_USER_COMPILE, gateInput);
            try {
                assertTrue("Compiler should reach the gate", entered.await(10, TimeUnit.SECONDS));
                Config.putPropString(Compiler.COMPILER_OPTIONS, "-Xlint:unchecked");
                enqueue(a, aLoader, aObserver, CompileType.EXPLICIT_USER_COMPILE, aInputs);
                enqueue(b, bLoader, bObserver, CompileType.EXPLICIT_USER_COMPILE, bInputs);
                aInputs[0] = bInputs[0];
                Config.putPropString(Compiler.COMPILER_OPTIONS, "--not-a-compiler-option");
                source(a, "HelperA.java", "public class HelperA { public int value() { return 11; } }");
            }
            finally {
                release.countDown();
                JobQueue.getJobQueue().waitForEmptyQueue();
                Config.putPropString(Compiler.COMPILER_OPTIONS, options);
            }
            aObserver.assertResult(originalA, CompileType.EXPLICIT_USER_COMPILE, true);
            bObserver.assertResult(bInputs, CompileType.EXPLICIT_USER_COMPILE, true);
            assertTrue(aObserver.starts.getFirst() < bObserver.starts.getFirst());
            assertEquals(11, invoke(aLoader, "ClientA"));
            assertEquals(20, invoke(bLoader, "ClientB"));
        }
    }

    /**
     * A Kotlin failure skips javac rather than publishing partial success.
     *
     * @throws Exception on test failure
     */
    @Test(timeout = 60000)
    public void testKotlinFailureSkipsJava() throws Exception
    {
        Path dir = temporary.newFolder().toPath();
        CompileInputFile[] inputs = {
            source(dir, "Bad.kt", "class Bad { fun value(): Int = \"wrong\" }"),
            source(dir, "Fine.java", "public class Fine {}")
        };
        for (CompileType type : List.of(CompileType.EXPLICIT_USER_COMPILE, CompileType.ERROR_CHECK_ONLY)) {
            Observer observer = compile(dir, type, inputs);
            observer.assertResult(inputs, type, false);
            assertEquals(List.of("kotlin"), observer.errorOrigins());
            assertFalse(Files.exists(dir.resolve("Fine.class")));
        }
    }

    /**
     * A javac failure fails the whole request without rolling back Kotlin writes.
     *
     * @throws Exception on test failure
     */
    @Test(timeout = 60000)
    public void testJavaFailureKeepsDirectWritesButReportsFailure() throws Exception
    {
        Path dir = temporary.newFolder().toPath();
        CompileInputFile[] inputs = {
            source(dir, "Client.kt", "class Client { fun run(): Int = Broken().value() }"),
            source(dir, "Broken.java", "public class Broken { public int value() { return \"wrong\"; } }")
        };
        Observer observer = compile(dir, CompileType.EXPLICIT_USER_COMPILE, inputs);
        observer.assertResult(inputs, CompileType.EXPLICIT_USER_COMPILE, false);
        assertEquals(List.of("javac"), observer.errorOrigins());
        assertTrue("Retained compilation still writes Kotlin output directly", Files.exists(dir.resolve("Client.class")));
        assertFalse(Files.exists(dir.resolve("Broken.class")));
    }

    /**
     * Check-only shares temporary output and keeps javac's real source root.
     *
     * @throws Exception on test failure
     */
    @Test(timeout = 60000)
    public void testBlueJCheckOnlySharesAndCleansTemporaryOutput() throws Exception
    {
        runCheckOnlyProbe(false);
    }

    /**
     * Single-language checks and internal Java shells retain their existing routing.
     *
     * @throws Exception on test failure
     */
    @Test(timeout = 60000)
    public void testSingleLanguageAndInternalShellParity() throws Exception
    {
        Path dir = temporary.newFolder().toPath();
        CompileInputFile java = source(dir, "PureJava.java", "public class PureJava { public int value() { return 4; } }");
        CompileInputFile kotlin = source(dir, "PureKotlin.kt", "class PureKotlin { fun value(): Int = 3 }");
        for (CompileInputFile input : List.of(java, kotlin)) {
            compile(dir, CompileType.ERROR_CHECK_ONLY, input)
                    .assertResult(new CompileInputFile[]{input}, CompileType.ERROR_CHECK_ONLY, true);
            String className = input.getJavaCompileInputFile().getName().replaceFirst("\\.[^.]+$", ".class");
            assertFalse(Files.exists(dir.resolve(className)));
            compile(dir, CompileType.EXPLICIT_USER_COMPILE, input)
                    .assertResult(new CompileInputFile[]{input}, CompileType.EXPLICIT_USER_COMPILE, true);
            assertTrue(Files.exists(dir.resolve(className)));
        }
        CompileInputFile shell = source(dir, "__SHELL0.java", """
            public class __SHELL0 {
                public int run() {
                    java.util.List<String> list = new java.util.ArrayList();
                    return new PureJava().value() + new PureKotlin().value();
                }
            }
            """);
        Observer observer = compile(dir, CompileType.INTERNAL_COMPILE, shell);
        observer.assertResult(new CompileInputFile[]{shell}, CompileType.INTERNAL_COMPILE, true);
        assertTrue("Internal unchecked notes remain suppressed", observer.diagnostics.isEmpty());
        try (BPClassLoader loader = loader(dir)) {
            assertEquals(7, invoke(loader, "__SHELL0"));
        }
    }

    /**
     * An observer exception must not generate a second completion or stop the queue.
     *
     * @throws Exception on test failure
     */
    @Test(timeout = 60000)
    public void testEndCallbackExceptionIsNotRetried() throws Exception
    {
        Path dir = temporary.newFolder().toPath();
        CompileInputFile input = source(dir, "Fine.java", "public class Fine {}");
        Observer observer = new Observer()
        {
            /**
             * Records completion and deliberately fails once to check that the queue does not retry.
             *
             * @param sources completed compilation inputs
             * @param success compilation result
             * @param type compilation mode
             * @param sequence compilation identifier
             * @throws IllegalStateException on the first completion notification
             */
            @Override
            public void endCompile(CompileInputFile[] sources, boolean success, CompileType type, int sequence)
            {
                super.endCompile(sources, success, type, sequence);
                if (results.size() == 1) {
                    throw new IllegalStateException("Expected observer failure");
                }
            }
        };
        try (BPClassLoader loader = loader(dir)) {
            enqueue(dir, loader, observer, CompileType.EXPLICIT_USER_COMPILE, input);
            JobQueue.getJobQueue().waitForEmptyQueue();
        }
        observer.assertResult(new CompileInputFile[]{input}, CompileType.EXPLICIT_USER_COMPILE, true);
        compile(dir, CompileType.EXPLICIT_USER_COMPILE, input)
                .assertResult(new CompileInputFile[]{input}, CompileType.EXPLICIT_USER_COMPILE, true);
    }

    /**
     * Uses a separate JVM because application mode is fixed at Config initialization.
     *
     * @throws Exception on test failure
     */
    @Test(timeout = 60000)
    public void testGreenfootCheckOnlyRetainsClasses() throws Exception
    {
        runCheckOnlyProbe(true);
    }

    private void runCheckOnlyProbe(boolean greenfoot) throws Exception
    {
        Path log = temporary.newFile("check-only.log").toPath();
        // Isolate temporary-output assertions from other compiler tests running in parallel JVMs.
        Path compilerTemp = temporary.newFolder().toPath();
        Process process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-Djava.awt.headless=true", "-Djava.io.tmpdir=" + compilerTemp, "-cp", System.getProperty("java.class.path"),
                CheckOnlyProbe.class.getName(), temporary.newFolder().getAbsolutePath(), Boolean.toString(greenfoot))
                .redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try {
            assertTrue("Check-only queue test timed out", process.waitFor(50, TimeUnit.SECONDS));
            assertEquals(Files.readString(log), 0, process.exitValue());
        }
        finally {
            process.destroyForcibly();
        }
    }

    /** Isolated check-only probe; not a JUnit class because Config mode is process-wide. */
    public static class CheckOnlyProbe
    {
        /**
         * Checks output retention, then exits the process owning the permanent compiler thread.
         *
         * @param args the temporary project directory and whether to use Greenfoot mode
         */
        public static void main(String[] args)
        {
            try {
                boolean greenfoot = Boolean.parseBoolean(args[1]);
                initialiseConfig(greenfoot);
                Path dir = Path.of(args[0]);
                if (greenfoot) {
                    CompileInputFile[] inputs = mutualSources(dir);
                    Observer observer = compile(dir, CompileType.ERROR_CHECK_ONLY, inputs);
                    observer.assertResult(inputs, CompileType.ERROR_CHECK_ONLY, true);
                    assertTrue(observer.allOutputsAtEnd);
                    try (BPClassLoader loader = loader(dir)) {
                        assertEquals(7, invoke(loader, "KotlinActor"));
                    }
                }
                else {
                    checkBlueJOutput(dir);
                }
            }
            catch (Throwable t) {
                t.printStackTrace();
                System.exit(1);
            }
            System.exit(0);
        }
    }

    private static void checkBlueJOutput(Path dir) throws Exception
    {
        CompileInputFile kotlin = source(dir, "demo/KThing.kt", "package demo; class KThing { fun value(): Int = 3 }");
        source(dir, "support/Extra.java", "package support; public class Extra { public static int value() { return 4; } }");
        Set<Path> before = temporaryOutputs();
        for (boolean valid : List.of(true, false)) {
            CompileInputFile java = source(dir, "demo/JThing.java", """
                package demo;
                public class JThing { public int run() { return new KThing().value() + support.Extra.value()%s; } }
                """.formatted(valid ? "" : " + \"wrong\""));
            CompileInputFile[] inputs = {java, kotlin};
            compile(dir, CompileType.ERROR_CHECK_ONLY, inputs).assertResult(inputs, CompileType.ERROR_CHECK_ONLY, valid);
            try (Stream<Path> files = Files.walk(dir)) {
                assertFalse("Check-only must not retain any output", files.anyMatch(p -> p.toString().endsWith(".class")));
            }
            assertEquals("Request temporary output must be removed even on failure", before, temporaryOutputs());
        }
    }

    private static void initialiseConfig(boolean greenfoot) throws IOException
    {
        Properties properties = new Properties();
        properties.setProperty("bluej.debug", "true");
        properties.setProperty("bluej.userHome", Files.createTempDirectory("queue-test-home").toString());
        Config.initialise(Boot.getBluejLibDir(), properties, greenfoot);
        assertEquals(greenfoot, Config.isGreenfoot());
    }

    private static CompileInputFile[] mutualSources(Path dir) throws IOException
    {
        return new CompileInputFile[]{
            source(dir, "JavaHelper.java", "public class JavaHelper { public int use(KotlinActor actor) { return actor.base(); } }"),
            source(dir, "KotlinActor.kt", "class KotlinActor { fun base(): Int = 7; fun run(): Int = JavaHelper().use(this) }")
        };
    }

    private static CompileInputFile[] helperSources(Path dir, String suffix, int value) throws IOException
    {
        return new CompileInputFile[]{
            source(dir, "Client" + suffix + ".kt", "class Client" + suffix + " { fun run(): Int = Helper" + suffix + "().value() }"),
            source(dir, "Helper" + suffix + ".java", "public class Helper" + suffix + " { public int value() { return " + value + "; } }")
        };
    }

    private static CompileInputFile source(Path dir, String name, String text) throws IOException
    {
        Path file = dir.resolve(name);
        Files.createDirectories(file.getParent());
        Files.writeString(file, text);
        return new CompileInputFile(file.toFile(), file.toFile());
    }

    private static Observer compile(Path dir, CompileType type, CompileInputFile... inputs) throws Exception
    {
        Observer observer = new Observer();
        try (BPClassLoader loader = loader(dir)) {
            enqueue(dir, loader, observer, type, inputs);
            JobQueue.getJobQueue().waitForEmptyQueue();
        }
        return observer;
    }

    private static void enqueue(Path dir, BPClassLoader loader, Observer observer, CompileType type, CompileInputFile... inputs)
    {
        JobQueue.getJobQueue().addJob(inputs, observer, loader, dir.toFile(), type == CompileType.INTERNAL_COMPILE,
                StandardCharsets.UTF_8, CompileReason.USER, type);
    }

    private static BPClassLoader loader(Path dir) throws Exception
    {
        List<URL> urls = new ArrayList<>();
        urls.add(dir.toUri().toURL());
        for (String entry : System.getProperty("java.class.path").split(File.pathSeparator)) {
            if (new File(entry).getName().startsWith("kotlin-stdlib")) {
                urls.add(new File(entry).toURI().toURL());
            }
        }
        return new BPClassLoader(urls.toArray(URL[]::new), ClassLoader.getPlatformClassLoader());
    }

    private static int invoke(BPClassLoader loader, String name) throws Exception
    {
        Class<?> cls = loader.loadClass(name);
        return ((Number) cls.getMethod("run").invoke(cls.getConstructor().newInstance())).intValue();
    }

    private static Set<Path> temporaryOutputs() throws IOException
    {
        try (Stream<Path> dirs = Files.list(Path.of(System.getProperty("java.io.tmpdir")))) {
            return dirs.filter(p -> p.getFileName().toString().startsWith("bluej-compile")).collect(Collectors.toSet());
        }
    }

    private static class Observer implements CompileObserver
    {
        final List<Integer> starts = new ArrayList<>();
        final List<Integer> ends = new ArrayList<>();
        final List<List<CompileInputFile>> sourceSets = new ArrayList<>();
        final List<CompileType> types = new ArrayList<>();
        final List<Boolean> results = new ArrayList<>();
        final List<Diagnostic> diagnostics = new ArrayList<>();
        boolean allOutputsAtEnd;

        /**
         * Captures the start identifier, source snapshot and mode for lifecycle assertions.
         *
         * @param sources compilation inputs to snapshot
         * @param reason compilation trigger, unused
         * @param type compilation mode to record
         * @param sequence compilation identifier to record
         */
        @Override
        public void startCompile(CompileInputFile[] sources, CompileReason reason, CompileType type, int sequence)
        {
            starts.add(sequence);
            sourceSets.add(List.copyOf(Arrays.asList(sources)));
            types.add(type);
        }

        /**
         * Retains compiler diagnostics for result and origin assertions.
         *
         * @param diagnostic compiler message to record
         * @param type compilation mode, unused
         */
        @Override
        public void compilerMessage(Diagnostic diagnostic, CompileType type)
        {
            diagnostics.add(diagnostic);
        }

        /**
         * Captures completion and whether every selected source has output at notification time.
         *
         * @param sources completed compilation inputs to snapshot and check
         * @param success compilation result to record
         * @param type compilation mode to record
         * @param sequence compilation identifier to record
         */
        @Override
        public void endCompile(CompileInputFile[] sources, boolean success, CompileType type, int sequence)
        {
            ends.add(sequence);
            sourceSets.add(List.copyOf(Arrays.asList(sources)));
            types.add(type);
            results.add(success);
            allOutputsAtEnd = Arrays.stream(sources).allMatch(s -> {
                File file = s.getJavaCompileInputFile();
                return new File(file.getParentFile(), file.getName().replaceFirst("\\.[^.]+$", ".class")).exists();
            });
        }

        List<String> errorOrigins()
        {
            return diagnostics.stream().filter(d -> d.getType() == Diagnostic.ERROR).map(Diagnostic::getOrigin).distinct().toList();
        }

        void assertResult(CompileInputFile[] inputs, CompileType type, boolean success)
        {
            assertEquals(diagnostics.stream().map(d -> d.getMessage().englishMessage()).toList().toString(), List.of(success), results);
            assertEquals(1, starts.size());
            assertEquals(starts, ends);
            assertEquals(List.of(Arrays.asList(inputs), Arrays.asList(inputs)), sourceSets);
            assertEquals(List.of(type, type), types);
        }
    }
}
