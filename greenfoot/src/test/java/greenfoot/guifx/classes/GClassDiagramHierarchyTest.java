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
package greenfoot.guifx.classes;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import bluej.extensions2.SourceType;
import bluej.parser.kotlin.KotlinHierarchyResolver;
import bluej.parser.kotlin.KotlinHierarchyResolver.TypeLookup;
import bluej.parser.kotlin.KotlinInfoParser;
import bluej.parser.kotlin.KotlinInfoParser.ParsedSource;
import bluej.parser.symtab.ClassInfo;

import org.junit.Test;

import static org.junit.Assert.*;

/** Tests the source/reflection boundary used by hierarchy grouping, without constructing the FX sidebar. */
public class GClassDiagramHierarchyTest
{
    /** Source FQNs, not a stale reflected hierarchy, determine grouping before and after compilation. */
    @Test
    public void testKotlinSourceHierarchyWins()
    {
        ClassInfo source = KotlinInfoParser.parse(new StringReader(
            "import greenfoot.World as Scene\nclass Level : Scene(10, 10, 1)"), null);
        for (boolean compiled : new boolean[] {false, true}) {
            assertEquals("greenfoot.World", GClassDiagram.superclassName(source, SourceType.Kotlin, compiled,
                () -> { throw new AssertionError("Source hierarchy must win"); }));
        }
    }

    /** Secondary-constructor inheritance and same-package chains work using source identities only. */
    @Test
    public void testUncompiledChainAndLocalShadow()
    {
        Map<String, ParsedSource> sources = new HashMap<>();
        ParsedSource level = new ParsedSource("import greenfoot.*\n"
            + "open class Level : World { constructor() : super(10, 10, 1) }", "Level.kt");
        sources.put("Level", level);
        ParsedSource child = new ParsedSource("class Child : Level()", "Child.kt");
        TypeLookup lookup = name -> sources.containsKey(name) ? sources.get(name).getDeclaredType(name)
            : KotlinHierarchyResolver.libraryType(name, getClass().getClassLoader());
        assertEquals("greenfoot.World", GClassDiagram.superclassName(level.getInfo(null, lookup), SourceType.Kotlin,
            false, () -> { throw new AssertionError("No project bytecode exists"); }));
        assertEquals("Level", GClassDiagram.superclassName(child.getInfo(null, lookup), SourceType.Kotlin,
            false, () -> { throw new AssertionError("No project bytecode exists"); }));
        sources.put("World", new ParsedSource("interface World", "World.kt"));
        assertNull("A local interface now shadows greenfoot.World", GClassDiagram.superclassName(
            level.getInfo(null, lookup), SourceType.Kotlin, false,
            () -> { throw new AssertionError("Must not restore the library hierarchy by reflection"); }));
    }

    /** Unresolved and failed Kotlin source must not cause loading of old bytecode. */
    @Test
    public void testKotlinUnresolvedOrMissingMetadata()
    {
        ClassInfo unresolved = KotlinInfoParser.parse(new StringReader("class Level : Unknown()"), null);
        for (ClassInfo source : new ClassInfo[] {unresolved, null}) {
            for (boolean compiled : new boolean[] {false, true}) {
                assertNull(GClassDiagram.superclassName(source, SourceType.Kotlin, compiled,
                    () -> { throw new AssertionError("Must not reflect stale Kotlin output"); }));
            }
        }
    }

    /** Current compiled-only targets can still be grouped using reflection. */
    @Test
    public void testCompiledOnlyHierarchy()
    {
        assertEquals("greenfoot.Actor", GClassDiagram.superclassName(null, SourceType.NONE, true,
            () -> "greenfoot.Actor"));
        assertNull(GClassDiagram.superclassName(null, SourceType.NONE, false,
            () -> { throw new AssertionError("Output is not current"); }));
    }

    /** The existing Java/Stride fallback behaviour is retained. */
    @Test
    public void testJavaAndStrideFallbackUnchanged()
    {
        for (SourceType language : new SourceType[] {SourceType.Java, SourceType.Stride}) {
            assertEquals("greenfoot.World", GClassDiagram.superclassName(null, language, false,
                () -> "greenfoot.World"));
            assertNull(GClassDiagram.superclassName(new ClassInfo(), language, true,
                () -> { throw new AssertionError("Source metadata is definitive"); }));
        }
    }
}
