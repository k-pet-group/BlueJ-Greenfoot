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
package bluej.parser.kotlin;

import java.util.List;

import org.junit.Test;

import static org.junit.Assert.*;

/** Tests the bounded Kotlin preparation edit without an editor or PSI mutation services. */
public class KotlinSourceEditorTest
{
    /** New preparation code follows existing initialization and preserves trailing comments. */
    @Test
    public void testNewMethodFollowsInitializationAndBodylessComment()
    {
        String source = """
            class Scene : World() {
              val first = load()
              init { initialise() }
              val last = loadMore()
              // Keep this comment.
            }
            """;
        KotlinSourceEditor.Edit edit = KotlinSourceEditor.appendPreparation(
            source, "Scene", "setup", List.of("placeActor()"), "Restore the scene.\nKeep the actors.");
        assertNotNull(edit);
        assertTrue(edit.newMethod());
        assertEquals(source.substring(0, source.lastIndexOf('}')) + """

              /**
               * Restore the scene.
               * Keep the actors.
               */
              private fun setup() {
                placeActor()
              }

              init {
                setup()
              }
            }
            """, apply(source, edit));

        String bodyless = "class Scene : World() // Keep this 🌍 comment";
        assertEquals(bodyless + """

            {
                private fun setup() {
                    placeActor()
                }

                init {
                    setup()
                }
            }""", apply(bodyless, KotlinSourceEditor.appendPreparation(
                bodyless, "Scene", "setup", List.of("placeActor()"), "")));
        KotlinSourceEditor.Edit empty = KotlinSourceEditor.appendPreparation(
            bodyless, "Scene", "setup", List.of(), "Unused documentation");
        assertNotNull(empty);
        assertEquals(empty.start(), empty.end());
        assertEquals("", empty.replacement());
        assertFalse(empty.newMethod());
    }

    /** Repeated appends reuse the preparation method and its existing initialization call. */
    @Test
    public void testExistingInitializationCallAndRepeatedAppend()
    {
        for (String initialization : new String[] {
            "",
            "init { this.prepare() }",
            "init { this@Scene.prepare() }",
            "val ready = prepare()",
            "constructor(n: Int) : this() { prepare() }"
        }) {
            String source = "class Scene() {\n    " + initialization + "\n" + """
                    private fun prepare(): Unit {
                        oldAction() // Keep the original statement and comment.
                        // Keep the trailing comment too.
                    }
                    val last = load()
                }
                """;
            KotlinSourceEditor.Edit first = KotlinSourceEditor.appendPreparation(
                source, "Scene", "prepare", List.of("firstAction()"), "Do not replace existing documentation");
            assertNotNull(first);
            assertFalse(first.newMethod());
            String once = apply(source, first);
            String expected = source.replace("    }\n", "        firstAction()\n    }\n");
            if (initialization.isEmpty()) {
                int classEnd = expected.lastIndexOf('}');
                expected = expected.substring(0, classEnd) + "\n    init {\n        prepare()\n    }\n"
                    + expected.substring(classEnd);
            }
            assertEquals(expected, once);
            String twice = apply(once, KotlinSourceEditor.appendPreparation(
                once, "Scene", "prepare", List.of("secondAction()"), ""));
            assertEquals(once.replace("firstAction()\n", "firstAction()\n        secondAction()\n"), twice);
            assertEquals(twice, apply(twice, KotlinSourceEditor.appendPreparation(
                twice, "Scene", "prepare", List.of(), "")));
        }
    }

    /** Unrelated calls do not suppress initialization, and raw argument text is inserted unchanged. */
    @Test
    public void testUnrelatedCallsDoNotSuppressInitAndRawArgumentsAreNotValidated()
    {
        String source = """
            class Other { init { prepare() }; fun prepare() {} }
            // 🌍 Offsets must count UTF-16 code units, not code points.
            class Scene {
                val text = "prepare() { }"
                val later = { prepare() }
                init { other.prepare() }
                fun act() { prepare() }
                class Nested { init { prepare() }; fun prepare() {} }
            }
            // Keep the file footer.
            """;
        String raw = "actor.configure(new Helper(), (Thing) value,\nnew int[]{1, 2})";
        KotlinSourceEditor.Edit edit = KotlinSourceEditor.appendPreparation(
            source, "Scene", "prepare", List.of(raw, "secondAction()"), "");
        assertNotNull(edit);
        assertTrue(edit.newMethod());
        int closingBrace = source.indexOf("\n}\n// Keep the file footer.") + 1;
        assertEquals(source.substring(0, closingBrace) + "\n    private fun prepare() {\n        " + raw
            + "\n        secondAction()\n    }\n\n    init {\n        prepare()\n    }\n" + source.substring(closingBrace), apply(source, edit));
    }

    /** Incompatible, ambiguous or incomplete class structures produce no preparation edit. */
    @Test
    public void testIncompatibleOrAmbiguousStructureFailsWithoutAnEdit()
    {
        for (String members : new String[] {
            "fun prepare() = Unit",
            "fun prepare(n: Int = 0) {}",
            "fun prepare() {}\nfun prepare(n: Int) {}",
            "fun prepare(): Int { return 1 }",
            "init { val prepare = {}; prepare() }\nfun prepare() {}",
            "init { run { prepare() } }\nfun prepare() {}",
            "init { run { this@Scene.prepare() } }\nfun prepare() {}"
        }) {
            assertNull(members, KotlinSourceEditor.appendPreparation(
                "class Scene { " + members + " }", "Scene", "prepare", List.of("placeActor()"), ""));
        }
        assertNull(KotlinSourceEditor.appendPreparation(
            "class Scene { fun prepare() {", "Scene", "prepare", List.of("placeActor()"), ""));
        assertNull(KotlinSourceEditor.appendPreparation(
            "class Other {}", "Scene", "prepare", List.of("placeActor()"), ""));
    }

    private String apply(String source, KotlinSourceEditor.Edit edit)
    {
        assertNotNull(edit);
        assertTrue(edit.start() >= 0 && edit.start() <= edit.end() && edit.end() <= source.length());
        return source.substring(0, edit.start()) + edit.replacement() + source.substring(edit.end());
    }
}
