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

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for the JobQueue's compiler dispatch logic — verifies that source files
 * are correctly partitioned by language so .kt files go to KotlinCompiler and
 * .java files (and everything else) go to CompilerAPICompiler. The mixed-language
 * case is the regression target: previously a single batch with both languages
 * was routed to one compiler, dropping the other.
 */
public class JobQueueDispatchTest
{
    @Test
    public void allKotlin_goesToKotlinGroup()
    {
        CompileInputFile a = input("Hello.kt");
        CompileInputFile b = input("World.kt");

        JobQueue.LanguagePartition p = JobQueue.partitionByLanguage(
                new CompileInputFile[]{a, b}, true);

        assertEquals(2, p.kotlinSources().size());
        assertEquals(0, p.javaSources().size());
    }

    @Test
    public void allJava_goesToJavaGroup()
    {
        CompileInputFile a = input("Hello.java");
        CompileInputFile b = input("World.java");

        JobQueue.LanguagePartition p = JobQueue.partitionByLanguage(
                new CompileInputFile[]{a, b}, true);

        assertEquals(0, p.kotlinSources().size());
        assertEquals(2, p.javaSources().size());
    }

    @Test
    public void mixed_splitsByExtension()
    {
        CompileInputFile java1 = input("Hello.java");
        CompileInputFile kt1 = input("World.kt");
        CompileInputFile java2 = input("Other.java");
        CompileInputFile kt2 = input("Util.kt");

        JobQueue.LanguagePartition p = JobQueue.partitionByLanguage(
                new CompileInputFile[]{java1, kt1, java2, kt2}, true);

        assertEquals(2, p.kotlinSources().size());
        assertEquals(2, p.javaSources().size());
        assertTrue(p.kotlinSources().contains(kt1));
        assertTrue(p.kotlinSources().contains(kt2));
        assertTrue(p.javaSources().contains(java1));
        assertTrue(p.javaSources().contains(java2));
    }

    @Test
    public void emptyInput_returnsEmptyPartition()
    {
        JobQueue.LanguagePartition p = JobQueue.partitionByLanguage(
                new CompileInputFile[]{}, true);

        assertEquals(0, p.kotlinSources().size());
        assertEquals(0, p.javaSources().size());
    }

    @Test
    public void nullInput_returnsEmptyPartition()
    {
        JobQueue.LanguagePartition p = JobQueue.partitionByLanguage(null, true);

        assertEquals(0, p.kotlinSources().size());
        assertEquals(0, p.javaSources().size());
    }

    @Test
    public void unknownExtension_routedToJava()
    {
        // Stride / shell / unexpected extensions fall back to the Java compiler,
        // matching the pre-Kotlin behaviour where CompilerAPICompiler handled them all.
        CompileInputFile odd = input("Generated.shell");

        JobQueue.LanguagePartition p = JobQueue.partitionByLanguage(
                new CompileInputFile[]{odd}, true);

        assertEquals(0, p.kotlinSources().size());
        assertEquals(1, p.javaSources().size());
        assertTrue(p.javaSources().contains(odd));
    }

    @Test
    public void mixedCaseKtExtension_treatedAsKotlin()
    {
        CompileInputFile upper = input("Hello.KT");
        CompileInputFile mixed = input("World.Kt");

        JobQueue.LanguagePartition p = JobQueue.partitionByLanguage(
                new CompileInputFile[]{upper, mixed}, true);

        assertEquals(2, p.kotlinSources().size());
        assertEquals(0, p.javaSources().size());
    }

    @Test
    public void ktFileWithPath_treatedAsKotlin()
    {
        CompileInputFile withPath = new CompileInputFile(
                new File("/path/to/project/Hello.kt"),
                new File("/path/to/project/Hello.kt"));

        JobQueue.LanguagePartition p = JobQueue.partitionByLanguage(
                new CompileInputFile[]{withPath}, true);

        assertEquals(1, p.kotlinSources().size());
        assertEquals(0, p.javaSources().size());
    }

    @Test
    public void kotlinCompilerUnavailable_routesAllToJava()
    {
        // In Greenfoot (no Kotlin compiler), .kt files should not be split off;
        // they fall through to the Java compiler, which keeps the legacy single-job behaviour.
        CompileInputFile java1 = input("Hello.java");
        CompileInputFile kt1 = input("World.kt");

        JobQueue.LanguagePartition p = JobQueue.partitionByLanguage(
                new CompileInputFile[]{java1, kt1}, false);

        assertEquals(0, p.kotlinSources().size());
        assertEquals(2, p.javaSources().size());
    }

    @Test
    public void nullEntriesInArray_areSkipped()
    {
        CompileInputFile java1 = input("Hello.java");

        JobQueue.LanguagePartition p = JobQueue.partitionByLanguage(
                new CompileInputFile[]{null, java1, null}, true);

        assertEquals(0, p.kotlinSources().size());
        assertEquals(1, p.javaSources().size());
    }

    private static CompileInputFile input(String name)
    {
        File f = new File(name);
        return new CompileInputFile(f, f);
    }
}
