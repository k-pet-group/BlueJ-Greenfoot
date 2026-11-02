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
package bluej.parser.kotlin.analysis;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import bluej.compiler.KotlinTestClasspath;
import bluej.parser.AssistContent.Access;
import bluej.parser.AssistContent.CompletionKind;
import bluej.parser.kotlin.analysis.KotlinCompletionCandidate.JavaMember;
import bluej.parser.kotlin.analysis.KotlinCompletionCandidate.Parameter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link KotlinCompletionEngine}, against Greenfoot-like Java sources in a temporary project.
 */
public class KotlinCompletionEngineTest
{
    private static final String CARET = "<caret>";

    private Path projectDir;
    private KotlinAnalysisService service;

    @Before
    public void setUp() throws IOException
    {
        projectDir = Files.createTempDirectory("kotlin-completion-test");
        service = new KotlinAnalysisService();
        write("greenfoot/Actor.java", """
            package greenfoot;
            public abstract class Actor {
                public void move(int distance) {}
                public void turn(int amount) {}
                public World getWorld() { return null; }
                public int getX() { return 0; }
                public int getRotation() { return 0; }
                public void setRotation(int rotation) {}
                public GreenfootImage getImage() { return null; }
                public void setImage(GreenfootImage image) {}
                public void setImage(String filename) {}
                protected boolean isTouching(Class<?> cls) { return false; }
                @Deprecated public void oldMove() {}
                public void act() {}
            }
            """);
        write("greenfoot/World.java", """
            package greenfoot;
            public abstract class World {
                public void addObject(Actor object, int x, int y) {}
                public <A> java.util.List<A> getObjects(Class<A> cls) { return null; }
            }
            """);
        write("greenfoot/Greenfoot.java", """
            package greenfoot;
            public class Greenfoot {
                public static int getRandomNumber(int limit) { return 0; }
                public static void delay(int time) {}
                public static String getKey() { return null; }
            }
            """);
        write("greenfoot/GreenfootImage.java", "package greenfoot; public class GreenfootImage {}");
        write("Rock.kt", """
            import greenfoot.*
            class Rock : Actor() {
                /** Rolls the rock. */
                fun roll(steps: Int = 1, fast: Boolean, action: () -> Unit): Int = steps
                private fun secret() {}
                val weight = 3
                companion object {
                    fun create(): Rock = Rock()
                    const val MAX = 5
                }
            }
            """);
        write("Direction.kt", "enum class Direction { NORTH, SOUTH }");
        write("Utils.kt", """
            import greenfoot.Actor
            fun helper(): Int = 1
            fun Actor.wiggle() {}
            """);
    }

    @After
    public void tearDown() throws IOException
    {
        service.dispose();
        try (Stream<Path> paths = Files.walk(projectDir)) {
            paths.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }
    }

    @Test
    public void testPlainOffersLocalsMembersProjectFunctionsAndTeachingSet() throws Exception
    {
        Set<String> names = names(complete(actor("val steps = 2\n" + CARET)));
        assertContains(names, "steps", "speed", "move", "turn", "isTouching", "act", "helper", "wiggle",
            "println", "listOf", "repeat", "Greenfoot.getRandomNumber", "Greenfoot.delay", "Greenfoot.getKey");
        assertContains(names, "world", "x", "rotation", "image", "setImage");
        assertExcludes(names, "getWorld", "getX", "setRotation", "oldMove", "uppercase", "let", "apply", "Rock",
            "Direction", "roll");
    }

    @Test
    public void testJavaSetterHiddenOnlyWhenItMatchesGetterProperty() throws Exception
    {
        List<KotlinCompletionCandidate> setImage = named(complete(actor(CARET)), "setImage");
        assertEquals("Only the setter not replaced by the image property should remain", 1, setImage.size());
        assertEquals("String", setImage.getFirst().parameters().getFirst().type());
    }

    @Test
    public void testGreenfootStaticsOnlyWhenGreenfootImported() throws Exception
    {
        String onlyActor = "import greenfoot.Actor\nclass Wombat : Actor() {\n override fun act() {\n" + CARET + "\n }\n}\n";
        assertExcludes(names(complete(onlyActor)), "Greenfoot.getRandomNumber");
        String withClass = "import greenfoot.Actor\nimport greenfoot.Greenfoot\n" + onlyActor.substring(onlyActor.indexOf("class"));
        assertContains(names(complete(withClass)), "Greenfoot.getRandomNumber");
    }

    @Test
    public void testPropertyHiddenByLocalIsOfferedWithThisPrefix() throws Exception
    {
        String text = "import greenfoot.*\nclass Wombat : Actor() {\n var speed = 1\n fun change(speed: Int) {\n" + CARET + "\n }\n}\n";
        KotlinCompletionResult result = complete(text);
        assertEquals(CompletionKind.FORMAL_PARAM, single(result, "speed").kind());
        assertEquals(CompletionKind.FIELD, single(result, "this.speed").kind());
    }

    @Test
    public void testTeachingFunctionOverloadsReducedToMostGeneral() throws Exception
    {
        KotlinCompletionResult result = complete(actor(CARET));
        List<KotlinCompletionCandidate> println = named(result, "println");
        assertEquals("println() and println(message) should remain", 2, println.size());
        assertTrue("The message overload should take Any?", println.stream()
            .anyMatch(c -> c.parameters().size() == 1 && c.parameters().getFirst().type().equals("Any?")));
        assertEquals("Extensions named like a teaching function (CharSequence.repeat) are not offered",
            1, named(result, "repeat").size());
    }

    @Test
    public void testMembersAfterDot() throws Exception
    {
        Set<String> names = names(complete(actor("getWorld()." + CARET)));
        assertContains(names, "addObject", "getObjects");
        assertExcludes(names, "move", "println");
        assertContains(names(complete(actor("world." + CARET))), "addObject");
    }

    @Test
    public void testLibraryExtensionsOfferedAfterDot() throws Exception
    {
        Set<String> names = names(complete(actor("\"abc\"." + CARET)));
        assertContains(names, "length", "uppercase", "let");
        assertExcludes(names, "println");
    }

    @Test
    public void testGenericExtensionsMatchReceiverType() throws Exception
    {
        KotlinCompletionResult result = complete(actor("listOf(1, 2)." + CARET));
        assertEquals("Only Iterable<Int>.sum applies to List<Int>", 1, named(result, "sum").size());
        assertEquals("The member contains hides the extension of the same shape", 1, named(result, "contains").size());
        List<KotlinCompletionCandidate> forEach = named(result, "forEach");
        assertEquals("The Java member forEach is hidden by Kotlin's extension", 1, forEach.size());
        assertTrue(forEach.getFirst().parameters().getFirst().functionType());
    }

    @Test
    public void testSmartCastSafeCallAndLambdaParameter() throws Exception
    {
        assertContains(names(complete(actor("val a: Actor = Rock()\nif (a is Rock) { a." + CARET + " }"))), "roll");
        assertContains(names(complete(actor("val w: World? = world\nw?." + CARET))), "addObject");
        assertContains(names(complete(actor("listOf(Rock()).forEach { it." + CARET + " }"))), "roll", "weight");
    }

    @Test
    public void testClassNameOffersStaticsCompanionAndEnumEntries() throws Exception
    {
        Set<String> greenfoot = names(complete(actor("Greenfoot." + CARET)));
        assertContains(greenfoot, "getRandomNumber", "delay");
        assertExcludes(greenfoot, "move", "toString");
        Set<String> rock = names(complete(actor("Rock." + CARET)));
        assertContains(rock, "create", "MAX");
        assertExcludes(rock, "roll", "toString", "equals");
        assertContains(names(complete(actor("Direction." + CARET))), "NORTH", "SOUTH", "values");
    }

    @Test
    public void testPrivateMembersOfOtherClassesHidden() throws Exception
    {
        Set<String> names = names(complete(actor("Rock()." + CARET)));
        assertContains(names, "roll", "weight");
        assertExcludes(names, "secret");
    }

    @Test
    public void testNoCompletionOutsideExpressions() throws Exception
    {
        assertNull("comment", complete(actor("// mo" + CARET)));
        assertNull("string", complete(actor("val s = \"mo" + CARET + "\"")));
        assertNull("type", complete(actor("val r: Ro" + CARET + "? = null")));
        assertNull("import", complete("import greenfoot.Gr" + CARET + "\nclass A"));
        assertNull("declaration name", complete("class A {\n fun mo" + CARET + "() {}\n}"));
        assertNull("unresolved receiver", complete(actor("unknownThing." + CARET)));
    }

    @Test
    public void testReplaceStartIsStartOfTypedIdentifier() throws Exception
    {
        String text = actor("mo" + CARET);
        assertEquals(text.indexOf(CARET) - 2, complete(text).replaceStart());
    }

    @Test
    public void testCandidateDetails() throws Exception
    {
        KotlinCompletionCandidate roll = single(complete(actor("Rock()." + CARET)), "roll");
        assertEquals("Int", roll.type());
        assertEquals("Rock", roll.declaringClass());
        assertEquals(Access.PUBLIC, roll.access());
        assertTrue("KDoc should be captured", roll.kdoc().contains("Rolls the rock."));
        assertEquals(List.of(new Parameter("steps", "Int", true, false, false),
            new Parameter("fast", "Boolean", false, false, false),
            new Parameter("action", "() -> Unit", false, false, true)), roll.parameters());

        KotlinCompletionResult plain = complete(actor(CARET));
        KotlinCompletionCandidate world = single(plain, "world");
        assertEquals(CompletionKind.FIELD, world.kind());
        assertEquals("Platform types should be shown without '!'", "World", world.type());
        assertEquals(new JavaMember("greenfoot.Actor", "getWorld", List.of()), world.javaMember());
        assertEquals(new JavaMember("greenfoot.Actor", "move", List.of("int")), single(plain, "move").javaMember());
        KotlinCompletionCandidate isTouching = single(plain, "isTouching");
        assertEquals(List.of("java.lang.Class"), isTouching.javaMember().parameterTypes());
        assertEquals(Access.PROTECTED, isTouching.access());
    }

    // A Wombat actor whose act() method contains the given body.
    private static String actor(String body)
    {
        return "import greenfoot.*\nclass Wombat : Actor() {\n var speed = 1\n override fun act() {\n" + body + "\n }\n}\n";
    }

    private KotlinCompletionResult complete(String textWithCaret) throws Exception
    {
        int caret = textWithCaret.indexOf(CARET);
        String text = textWithCaret.replace(CARET, "");
        List<File> classPath = new ArrayList<>(KotlinTestClasspath.getKotlinStdlib());
        classPath.add(projectDir.toFile());
        KotlinAnalysisRequest request = new KotlinAnalysisRequest(projectDir.toFile(), classPath,
            StandardCharsets.UTF_8, projectDir.resolve("Wombat.kt").toFile(), text, Map.of());
        return KotlinCompletionEngine.complete(service, request, caret).get(60, TimeUnit.SECONDS);
    }

    private static Set<String> names(KotlinCompletionResult result)
    {
        assertNotNull("Completion expected", result);
        return result.candidates().stream().map(c -> c.prefix() + c.name()).collect(Collectors.toSet());
    }

    private static List<KotlinCompletionCandidate> named(KotlinCompletionResult result, String prefixedName)
    {
        assertNotNull("Completion expected", result);
        return result.candidates().stream().filter(c -> (c.prefix() + c.name()).equals(prefixedName)).toList();
    }

    private static KotlinCompletionCandidate single(KotlinCompletionResult result, String prefixedName)
    {
        List<KotlinCompletionCandidate> found = named(result, prefixedName);
        assertEquals("Exactly one candidate named " + prefixedName, 1, found.size());
        return found.getFirst();
    }

    private static void assertContains(Set<String> names, String... expected)
    {
        for (String name : expected) {
            assertTrue("Expected " + name + " in " + names, names.contains(name));
        }
    }

    private static void assertExcludes(Set<String> names, String... unexpected)
    {
        for (String name : unexpected) {
            assertFalse("Did not expect " + name, names.contains(name));
        }
    }

    private void write(String relativePath, String text) throws IOException
    {
        Path file = projectDir.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, text);
    }
}
