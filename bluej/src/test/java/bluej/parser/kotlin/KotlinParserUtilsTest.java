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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link KotlinParserUtils#kdocToJavadoc}.
 */
public class KotlinParserUtilsTest
{
    @Test
    public void testKDocMarkdownConvertedForJavadocDisplay()
    {
        String converted = KotlinParserUtils.kdocToJavadoc("""
            /**
             * Moves `x` by [steps], see [the docs](https://example.org).
             * **Note**: a < b & c.
             * @param steps how far
             */""");
        assertTrue(converted, converted.contains("Moves <code>x</code> by <code>steps</code>, see the docs."));
        assertTrue("HTML special characters are escaped: " + converted, converted.contains("<b>Note</b>: a &lt; b &amp; c."));
        assertTrue("Block tags are kept for Javadoc rendering: " + converted, converted.contains("@param steps how far"));
    }

    @Test
    public void testMissingKDoc()
    {
        assertNull(KotlinParserUtils.kdocToJavadoc(null));
    }

    @Test
    public void testDelimitersAndLeadingStarsRemoved()
    {
        assertEquals("One line.", KotlinParserUtils.kdocToJavadoc("/** One line. */").trim());
    }
}
