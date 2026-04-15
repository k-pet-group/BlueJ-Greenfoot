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
package bluej.utility.filefilter;

import java.io.File;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for KotlinSourceFilter — accepts only .kt files.
 */
public class KotlinSourceFilterTest
{
    private final KotlinSourceFilter filter = new KotlinSourceFilter();

    @Test
    public void testAcceptsKtFile()
    {
        assertTrue("Should accept .kt file", filter.accept(new File("Utils.kt")));
    }

    @Test
    public void testAcceptsKtFileInPath()
    {
        assertTrue("Should accept .kt file with path", filter.accept(new File("/some/path/MyClass.kt")));
    }

    @Test
    public void testRejectsJavaFile()
    {
        assertFalse(filter.accept(new File("MyClass.java")));
    }

    @Test
    public void testRejectsStrideFile()
    {
        assertFalse(filter.accept(new File("MyClass.stride")));
    }

    @Test
    public void testRejectsClassFile()
    {
        assertFalse(filter.accept(new File("MyClass.class")));
    }

    @Test
    public void testRejectsKtsFile()
    {
        // .kts (Kotlin script) files should not be accepted
        assertFalse(filter.accept(new File("build.gradle.kts")));
    }

    @Test
    public void testRejectsFileWithKtInName()
    {
        // File named "Kotlin.java" should not be accepted
        assertFalse(filter.accept(new File("Kotlin.java")));
    }

    @Test
    public void testAcceptsMinimalKtFile()
    {
        assertTrue(filter.accept(new File("a.kt")));
    }
}
