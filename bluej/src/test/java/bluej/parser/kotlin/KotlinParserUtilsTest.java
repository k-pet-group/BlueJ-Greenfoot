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

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for KotlinParserUtils.
 */
public class KotlinParserUtilsTest
{
    @Test
    public void testHardKeywordsThatAreValidJavaIdentifiersDetected()
    {
        for (String kw : new String[]{"fun", "val", "var", "object", "in", "is", "as", "when", "typealias", "typeof"}) {
            assertTrue(kw, KotlinParserUtils.containsHardKeyword(kw));
        }
    }

    @Test
    public void testKeywordInAnySegmentDetected()
    {
        assertTrue(KotlinParserUtils.containsHardKeyword("fun.animals"));
        assertTrue(KotlinParserUtils.containsHardKeyword("zoo.fun.animals"));
        assertTrue(KotlinParserUtils.containsHardKeyword("zoo.object"));
    }

    @Test
    public void testOrdinaryAndSoftKeywordNamesAccepted()
    {
        // Soft keywords and modifiers are usable as identifiers in Kotlin
        for (String name : new String[]{"animals", "zoo.animals", "data", "open", "value", "inner", "fun2", "Fun"}) {
            assertFalse(name, KotlinParserUtils.containsHardKeyword(name));
        }
    }
}
