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
package bluej.pkgmgr;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;

import bluej.extensions2.SourceType;
import bluej.parser.InitConfig;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;

/** Exercises the class-copy boundary used by Greenfoot, without opening its dialogs. */
public class ProjectUtilsDuplicateTest
{
    /** Source and destination files removed after each test. */
    @Rule
    public final TemporaryFolder temporary = new TemporaryFolder();

    /** Initializes configuration for source-copy operations without opening the IDE. */
    @BeforeClass
    public static void initialise()
    {
        InitConfig.init();
    }

    /**
     * Class duplication renames Kotlin identifiers without changing comments or resource text.
     *
     * @throws Exception if source fixtures cannot be written, copied or read
     */
    @Test
    public void testKotlinIdentifiersWithoutRewritingResourceText() throws Exception
    {
        String template = """
            import greenfoot.Actor
            // Crab 🌍 remains the image name.
            open class %1$s: Actor() {
                var friend:%1$s? = null
                fun hatch()=%1$s()
                fun self()=this@%1$s
                val kind=%1$s::class
                val filename="Crab.png"
                init { setImage(filename) }
            }
            """.replace("\n", "\r\n");
        assertEquals(template.formatted("CopyOfCrab"),
            duplicate(SourceType.Kotlin, "Crab", "CopyOfCrab", template.formatted("Crab")));
    }

    /**
     * Kotlin copies rename escaped and soft-keyword class names, including constructor references.
     *
     * @throws Exception if source fixtures cannot be written, copied or read
     */
    @Test
    public void testKotlinEscapedNamesAndSecondaryConstructors() throws Exception
    {
        String template = """
            open class %1$s<T>() {
                constructor(other:%1$s<T>):this() {}
                fun hatch()=%1$s<T>()
                val label="${%1$s::class.simpleName}"
            }
            """;
        assertEquals(template.formatted("CopyOfWhen"),
            duplicate(SourceType.Kotlin, "when", "CopyOfWhen", template.formatted("`when`")));
        assertEquals(template.formatted("CopyOfOpen"),
            duplicate(SourceType.Kotlin, "open", "CopyOfOpen", template.formatted("open")));
    }

    /**
     * Java and Stride copies retain their existing class and constructor renaming behavior.
     *
     * @throws Exception if source fixtures cannot be written, copied or read
     */
    @Test
    public void testLegacyJavaAndStrideCopies() throws Exception
    {
        String java = "public class Crab { public Crab() {} Crab hatch() { return new Crab(); } }\n";
        assertEquals(java.replace("Crab", "CopyOfCrab").replace("\n", System.lineSeparator()),
            duplicate(SourceType.Java, "Crab", "CopyOfCrab", java));
        String stride = "<class name=\"Crab\"><constructor name=\"Crab\"/></class>\n";
        assertEquals(stride.replace("Crab", "CopyOfCrab").replace("\n", System.lineSeparator()),
            duplicate(SourceType.Stride, "Crab", "CopyOfCrab", stride));
    }

    private String duplicate(SourceType type, String name, String newName, String source) throws Exception
    {
        File original = temporary.newFile(name + "." + type.getExtension());
        File copy = new File(temporary.getRoot(), newName + "." + type.getExtension());
        Files.writeString(original.toPath(), source, Charset.defaultCharset());
        ProjectUtils.duplicate(name, newName, original, copy, type);
        assertEquals(source, Files.readString(original.toPath(), Charset.defaultCharset()));
        return Files.readString(copy.toPath(), Charset.defaultCharset());
    }
}
