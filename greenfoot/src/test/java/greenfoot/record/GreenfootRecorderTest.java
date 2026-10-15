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
package greenfoot.record;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.tools.ToolProvider;

import bluej.compiler.*;
import bluej.debugger.*;
import bluej.debugger.gentype.*;
import bluej.parser.kotlin.KotlinSourceEditor;
import bluej.utility.JavaReflective;
import greenfoot.Actor;
import greenfoot.TestUtilDelegate;
import greenfoot.World;
import greenfoot.core.Simulation;
import greenfoot.core.WorldHandler;
import greenfoot.util.GreenfootUtil;

import com.sun.jdi.ObjectReference;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.*;

import threadchecker.OnThread;
import threadchecker.Tag;

/** Compiler/direct-engine replay coverage; this does not launch the editor or a Greenfoot GUI. */
public class GreenfootRecorderTest
{
    /** Owns both Kotlin sources and generated classes, including on assertion failure. */
    @Rule
    public final TemporaryFolder temporary = new TemporaryFolder();

    /**
     * Replays captured interactions as native Kotlin after the World's existing initialization.
     *
     * @throws Exception if fixture I/O, class loading or reflective replay fails
     */
    @Test
    public void testKotlinRecordingReconstructsWorld() throws Exception
    {
        GreenfootUtil.initialise(new TestUtilDelegate());
        Simulation.initialize();
        WorldHandler.initialise();
        File output = temporary.newFolder("classes");
        KotlinCompiler compiler = replayCompiler(output);
        compile(compiler, temporary.newFile("When.kt"), """
            import greenfoot.Actor
            class When(var score: Int) : Actor() {
                fun boost(amount: Int) { score += amount }
                companion object {
                    @JvmStatic var total: Int = 0
                    fun bump(amount: Int) { total += amount }
                }
            }
            """);

        try (URLClassLoader loader = new URLClassLoader(new URL[] {output.toURI().toURL()}, getClass().getClassLoader())) {
            Class<?> actorType = loader.loadClass("When");
            Class<?> companionType = actorType.getField("Companion").getType();
            StubObject actor = new StubObject(actorType);
            JavaType[] integer = {JavaPrimitiveType.getInt()};
            GreenfootRecorder recorder = new GreenfootRecorder();
            recorder.createActor(actor, new String[] {"5"}, integer);
            recorder.addActorToWorld(actor, 2, 3);
            recorder.callStaticMethod("When", actorType.getMethod("setTotal", int.class), new String[] {"10"}, integer);
            recorder.callStaticMethod("When", companionType.getMethod("bump", int.class), new String[] {"3"}, integer);
            recorder.moveActor(actor, 7, 9);
            String[] arguments = {"8"};
            recorder.callActorOrWorldMethod(actor, actorType.getMethod("setScore", int.class), arguments, integer);
            arguments[0] = "100"; // Capture must retain the original expression, not the caller's array.
            recorder.callActorOrWorldMethod(actor, actorType.getMethod("getScore"), null, null);
            recorder.callActorOrWorldMethod(actor, actorType.getMethod("boost", int.class), new String[] {"3"}, integer);
            StubObject removed = new StubObject(actorType);
            recorder.createActor(removed, new String[] {"0"}, integer);
            recorder.addActorToWorld(removed, 1, 1);
            recorder.removeActor(removed);

            String source = """
                import greenfoot.World
                class ReplayWorld : World(20, 20, 1) {
                    var marker = 1
                    init { marker += 2; When.total = marker }
                }
                """;
            List<String> statements = recorder.getKotlinCode();
            assertTrue(statements.contains("val `when` = When(5)"));
            assertTrue(statements.contains("`when`.score = 8"));
            assertTrue(statements.contains("When.total = 10"));
            KotlinSourceEditor.Edit edit = KotlinSourceEditor.appendPreparation(source, "ReplayWorld", "prepare",
                statements, "Replay recorded setup.");
            assertNotNull("The inline World should admit a preparation edit", edit);
            assertTrue(edit.newMethod());
            source = source.substring(0, edit.start()) + edit.replacement() + source.substring(edit.end());
            compile(compiler, temporary.newFile("ReplayWorld.kt"), source);

            World world = (World) loader.loadClass("ReplayWorld").getConstructor().newInstance();
            assertEquals(3, world.getClass().getMethod("getMarker").invoke(world));
            assertEquals("Removed actor must not survive reconstruction", 1, world.getObjects(Actor.class).size());
            Actor replayed = world.getObjects(Actor.class).get(0);
            assertEquals(7, replayed.getX());
            assertEquals(9, replayed.getY());
            assertEquals("Property write and method call use the frozen argument", 11,
                actorType.getMethod("getScore").invoke(replayed));
            assertEquals("Companion effects must follow the existing init block", 13,
                actorType.getMethod("getTotal").invoke(null));
        }
    }

    /**
     * A recorded no-argument top-level function compiles and runs from the inserted preparation method.
     *
     * @throws Exception if fixture compilation, loading or replay fails
     */
    @Test
    @OnThread(Tag.FXPlatform)
    public void testTopLevelFunctionReplay() throws Exception
    {
        File output = temporary.newFolder("classes");
        KotlinCompiler compiler = replayCompiler(output);
        compile(compiler, temporary.newFile("Helpers.kt"), """
            var resetCalls = 0
            fun resetCounter() { resetCalls++ }
            """);
        try (URLClassLoader loader = new URLClassLoader(new URL[] {output.toURI().toURL()}, getClass().getClassLoader())) {
            Class<?> helpers = loader.loadClass("HelpersKt");
            GreenfootRecorder recorder = new GreenfootRecorder();
            recorder.callStaticMethod(helpers.getName(), helpers.getMethod("resetCounter"), null, null);
            List<String> statements = recorder.getKotlinCode();
            compile(compiler, temporary.newFile("ReplayWorld.kt"), preparationSource(statements));
            loader.loadClass("ReplayWorld").getConstructor().newInstance();
            assertEquals(1, helpers.getMethod("getResetCalls").invoke(null));
            assertEquals(List.of("resetCounter()"), statements);
            assertEquals("HelpersKt.resetCounter();\n",
                recorder.getCode().get(0).toJavaSource().toTemporaryJavaCodeString());
        }
    }

    /**
     * Default-package properties use native access syntax and retain exact accessor-signature matching.
     *
     * @throws Exception if fixture compilation, loading or replay fails
     */
    @Test
    @OnThread(Tag.FXPlatform)
    public void testTopLevelPropertyReplay() throws Exception
    {
        checkTopLevelProperties("", false);
    }

    /**
     * Named packages qualify the source declaration, including escaped package identifiers.
     *
     * @throws Exception if fixture compilation, loading or replay fails
     */
    @Test
    @OnThread(Tag.FXPlatform)
    public void testPackagedTopLevelReplay() throws Exception
    {
        checkTopLevelProperties("helpers", false);
        checkTopLevelProperties("example.`when`", false);
    }

    /**
     * Facade and property detection work when student Kotlin metadata has a different class identity.
     *
     * @throws Exception if fixture compilation, loading or replay fails
     */
    @Test
    @OnThread(Tag.FXPlatform)
    public void testTopLevelReplayWithForeignMetadata() throws Exception
    {
        checkTopLevelProperties("", true);
        checkTopLevelProperties("example.`when`", true);
    }

    @OnThread(Tag.FXPlatform)
    private void checkTopLevelProperties(String packageName, boolean foreignMetadata) throws Exception
    {
        File fixture = temporary.newFolder();
        File output = new File(fixture, "classes");
        Files.createDirectories(output.toPath());
        KotlinCompiler compiler = replayCompiler(output);
        compile(compiler, new File(fixture, "Helpers.kt"),
            (packageName.isEmpty() ? "" : "package " + packageName + "\n") + """
            var readCount = 0
            var counter = 0
                get() { readCount++; return field }
            @get:JvmName("readURL")
            @set:JvmName("writeURL")
            var URL = 0
            fun resetCounter() { counter = 1 }
            fun setCounter(value: String) { URL = value.length }
            """);
        List<URL> urls = new ArrayList<>();
        urls.add(output.toURI().toURL());
        if (foreignMetadata) {
            for (File library : kotlinStdlib()) {
                urls.add(library.toURI().toURL());
            }
        }
        ClassLoader parent = foreignMetadata ? ClassLoader.getPlatformClassLoader() : getClass().getClassLoader();
        try (URLClassLoader loader = new URLClassLoader(urls.toArray(URL[]::new), parent)) {
            if (foreignMetadata) {
                assertNotSame("Exercise the loader-neutral metadata boundary", Class.forName("kotlin.Metadata"),
                    loader.loadClass("kotlin.Metadata"));
            }
            String prefix = packageName.isEmpty() ? "" : packageName + ".";
            Class<?> helpers = loader.loadClass(prefix.replace("`", "") + "HelpersKt");
            GreenfootRecorder recorder = new GreenfootRecorder();
            recorder.callStaticMethod(helpers.getName(), helpers.getMethod("resetCounter"), null, null);
            recorder.callStaticMethod(helpers.getName(), helpers.getMethod("setCounter", int.class),
                new String[] {" 2 "}, null);
            recorder.callStaticMethod(helpers.getName(), helpers.getMethod("getCounter"), null, null);
            recorder.callStaticMethod(helpers.getName(), helpers.getMethod("setCounter", String.class),
                new String[] {"\"ok\""}, null);
            recorder.callStaticMethod(helpers.getName(), helpers.getMethod("writeURL", int.class),
                new String[] {"3"}, null);
            recorder.callStaticMethod(helpers.getName(), helpers.getMethod("readURL"), null, null);

            List<String> statements = recorder.getKotlinCode();
            compile(compiler, new File(fixture, "ReplayWorld.kt"), preparationSource(statements));
            loader.loadClass("ReplayWorld").getConstructor().newInstance();
            assertEquals("The recorded property read must execute its getter", 1,
                helpers.getMethod("getReadCount").invoke(null));
            assertEquals(2, helpers.getMethod("getCounter").invoke(null));
            assertEquals(3, helpers.getMethod("readURL").invoke(null));
            assertEquals(List.of(prefix + "resetCounter()", prefix + "counter =  2 ", prefix + "counter",
                prefix + "setCounter(\"ok\")", prefix + "URL = 3", prefix + "URL"), statements);
            assertEquals(helpers.getName() + ".setCounter(2);\n",
                recorder.getCode().get(1).toJavaSource().toTemporaryJavaCodeString());
            assertEquals(helpers.getName() + ".writeURL(3);\n",
                recorder.getCode().get(4).toJavaSource().toTemporaryJavaCodeString());
        }
    }

    /**
     * Real Kotlin classes ending in Kt and Java static methods keep their class receiver in replay.
     *
     * @throws Exception if fixture compilation, loading or replay fails
     */
    @Test
    @OnThread(Tag.FXPlatform)
    public void testStaticClassReplayRetainsReceivers() throws Exception
    {
        File output = temporary.newFolder("classes");
        File javaFile = temporary.newFile("JavaHelpersKt.java");
        Files.writeString(javaFile.toPath(), """
            public class JavaHelpersKt {
                public static int counter;
                public static void setCounter(int value) { counter = value; }
            }
            """, StandardCharsets.UTF_8);
        assertEquals("The Java control must compile", 0, ToolProvider.getSystemJavaCompiler().run(null, null, null,
            "--release", "21", "-d", output.getAbsolutePath(), javaFile.getAbsolutePath()));
        KotlinCompiler compiler = replayCompiler(output);
        compile(compiler, temporary.newFile("OrdinaryKt.kt"), """
            class OrdinaryKt {
                companion object {
                    @JvmStatic var counter = 0
                    @JvmStatic fun resetCounter() { counter = 1 }
                }
            }
            """);
        try (URLClassLoader loader = new URLClassLoader(new URL[] {output.toURI().toURL()}, getClass().getClassLoader())) {
            Class<?> kotlinClass = loader.loadClass("OrdinaryKt");
            Class<?> javaClass = loader.loadClass("JavaHelpersKt");
            GreenfootRecorder recorder = new GreenfootRecorder();
            recorder.callStaticMethod(kotlinClass.getName(), kotlinClass.getMethod("resetCounter"), null, null);
            recorder.callStaticMethod(kotlinClass.getName(), kotlinClass.getMethod("setCounter", int.class),
                new String[] {" 2 "}, null);
            recorder.callStaticMethod(javaClass.getName(), javaClass.getMethod("setCounter", int.class),
                new String[] {" 3 "}, null);
            List<String> statements = recorder.getKotlinCode();
            assertEquals(List.of("OrdinaryKt.resetCounter()", "OrdinaryKt.counter =  2 ",
                "JavaHelpersKt.setCounter( 3 )"), statements);
            compile(compiler, temporary.newFile("ReplayWorld.kt"), preparationSource(statements));
            loader.loadClass("ReplayWorld").getConstructor().newInstance();
            assertEquals(2, kotlinClass.getMethod("getCounter").invoke(null));
            assertEquals(3, javaClass.getField("counter").get(null));
        }
    }

    @OnThread(Tag.Any)
    private static String preparationSource(List<String> statements)
    {
        String source = "class ReplayWorld {}";
        KotlinSourceEditor.Edit edit = KotlinSourceEditor.appendPreparation(source, "ReplayWorld", "prepare",
            statements, "Replay recorded setup.");
        assertNotNull("The replay fixture must admit a preparation edit", edit);
        return source.substring(0, edit.start()) + edit.replacement() + source.substring(edit.end());
    }

    @OnThread(Tag.Any)
    private static List<File> kotlinStdlib()
    {
        List<File> libraries = Arrays.stream(System.getProperty("java.class.path").split(File.pathSeparator))
            .map(File::new).filter(file -> file.getName().startsWith("kotlin-stdlib")).toList();
        assertFalse("The staged Kotlin stdlib must be on the test classpath", libraries.isEmpty());
        return libraries;
    }

    @OnThread(Tag.Any)
    private static KotlinCompiler replayCompiler(File output) throws URISyntaxException
    {
        KotlinCompiler compiler = new KotlinCompiler();
        compiler.setDestDir(output);
        List<File> classpath = new ArrayList<>(kotlinStdlib());
        classpath.add(new File(Actor.class.getProtectionDomain().getCodeSource().getLocation().toURI()));
        classpath.add(output);
        compiler.setClasspath(classpath);
        return compiler;
    }

    private static void compile(KotlinCompiler compiler, File file, String source) throws IOException
    {
        Files.writeString(file.toPath(), source, StandardCharsets.UTF_8);
        List<String> diagnostics = new ArrayList<>();
        CompileObserver observer = new CompileObserver()
        {
            /**
             * Ignores lifecycle start notifications; this fixture only collects diagnostics.
             *
             * @param sources compilation inputs, unused
             * @param reason compilation trigger, unused
             * @param type compilation mode, unused
             * @param sequence compilation identifier, unused
             */
            @Override
            public void startCompile(CompileInputFile[] sources, CompileReason reason, CompileType type, int sequence)
            {
            }

            /**
             * Collects diagnostic text for the compilation assertion.
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
             * @param sources compilation inputs, unused
             * @param success compilation result, unused
             * @param type compilation mode, unused
             * @param sequence compilation identifier, unused
             */
            @Override
            public void endCompile(CompileInputFile[] sources, boolean success, CompileType type, int sequence)
            {
            }
        };
        boolean success = compiler.compile(new File[] {file}, observer, false, List.of(),
            StandardCharsets.UTF_8, CompileType.EXPLICIT_USER_COMPILE);
        assertTrue(file.getName() + ": " + diagnostics, success);
    }

    // Recorder identity/type only: unexpected debugger access must fail, not emulate a running VM.
    static class StubObject extends DebuggerObject
    {
        private final Class<?> type;

        StubObject(Class<?> type)
        {
            this.type = type;
        }

        /**
         * Supplies the replay type without consulting a debugger VM.
         *
         * @return the represented class's name
         */
        @Override
        public String getClassName()
        {
            return type.getName();
        }

        /**
         * Wraps the local fixture class for recorder type queries.
         *
         * @return the fixture's generic class type
         */
        @Override
        public GenTypeClass getGenType()
        {
            return new GenTypeClass(new JavaReflective(type));
        }

        /**
         * Keeps recorder object keys tied to stub identity.
         *
         * @return this stub's identity hash code
         */
        @Override
        public int hashCode()
        {
            return System.identityHashCode(this);
        }

        /**
         * Compares recorder object keys by identity.
         *
         * @param other object to compare with this stub
         * @return true only for this same stub instance
         */
        @Override
        public boolean equals(Object other)
        {
            return this == other;
        }

        /**
         * Rejects debugger class access that the recorder fixture does not support.
         *
         * @return never returns normally
         * @throws UnsupportedOperationException always
         */
        @Override
        public DebuggerClass getClassRef()
        {
            throw new UnsupportedOperationException();
        }

        /**
         * Rejects array queries outside the recorder fixture's type-only contract.
         *
         * @return never returns normally
         * @throws UnsupportedOperationException always
         */
        @Override
        public boolean isArray()
        {
            throw new UnsupportedOperationException();
        }

        /**
         * Rejects debugger null-state queries not needed by the recorder fixture.
         *
         * @return never returns normally
         * @throws UnsupportedOperationException always
         */
        @Override
        public boolean isNullObject()
        {
            throw new UnsupportedOperationException();
        }

        /**
         * Rejects field inspection rather than emulating a debugger VM.
         *
         * @return never returns normally
         * @throws UnsupportedOperationException always
         */
        @Override
        public List<DebuggerField> getFields()
        {
            throw new UnsupportedOperationException();
        }

        /**
         * Rejects array length queries because this fixture has no debugger array.
         *
         * @return never returns normally
         * @throws UnsupportedOperationException always
         */
        @Override
        public int getElementCount()
        {
            throw new UnsupportedOperationException();
        }

        /**
         * Rejects array element type queries because this fixture has no debugger array.
         *
         * @return never returns normally
         * @throws UnsupportedOperationException always
         */
        @Override
        public JavaType getElementType()
        {
            throw new UnsupportedOperationException();
        }

        /**
         * Rejects array element access rather than emulating debugger objects.
         *
         * @param index requested element index, unused
         * @return never returns normally
         * @throws UnsupportedOperationException always
         */
        @Override
        public DebuggerObject getElementObject(int index)
        {
            throw new UnsupportedOperationException();
        }

        /**
         * Rejects array value inspection rather than emulating debugger values.
         *
         * @param index requested element index, unused
         * @return never returns normally
         * @throws UnsupportedOperationException always
         */
        @Override
        public String getElementValueString(int index)
        {
            throw new UnsupportedOperationException();
        }

        /**
         * Rejects VM object access because the fixture represents only local identity and type.
         *
         * @return never returns normally
         * @throws UnsupportedOperationException always
         */
        @Override
        public ObjectReference getObjectReference()
        {
            throw new UnsupportedOperationException();
        }
    }
}
