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
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Properties;

import bluej.Boot;
import bluej.Config;
import bluej.compiler.CompileInputFile;
import bluej.compiler.CompileType;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/** Request-level retry policy, without starting a Project's UI/debugger lifecycle. */
public class PackageCompileRetryTest
{
    /**
     * Initializes labels read by Package without launching the IDE.
     *
     * @throws IOException if the temporary user home cannot be created
     */
    @BeforeClass
    public static void initialise() throws IOException
    {
        Properties properties = new Properties();
        properties.setProperty("bluej.userHome", Files.createTempDirectory("retry-test-home").toString());
        Config.initialise(Boot.getBluejLibDir(), properties, false);
    }

    /** A failed mixed request must not retry a supposedly independent language subset. */
    @Test
    public void testFailedMixedRequestDoesNotRetry()
    {
        CompileInputFile java = input("Helper.java");
        CompileInputFile kotlin = input("Actor.kt");
        assertFalse(Package.allowsPartialCompileRetry(new CompileInputFile[]{java, kotlin},
                false, CompileType.EXPLICIT_USER_COMPILE));
        assertFalse(Package.allowsPartialCompileRetry(new CompileInputFile[]{kotlin, java},
                false, CompileType.EXPLICIT_USER_COMPILE));
    }

    /** Single-language failure recovery remains exclusive to explicit user compiles. */
    @Test
    public void testSingleLanguageRecoveryKeepsCompileTypePolicy()
    {
        for (String name : List.of("Helper.java", "Actor.kt")) {
            for (CompileType type : CompileType.values()) {
                assertEquals(name + " / " + type, type == CompileType.EXPLICIT_USER_COMPILE,
                        Package.allowsPartialCompileRetry(new CompileInputFile[]{input(name)}, false, type));
            }
        }
        for (CompileType type : CompileType.values()) {
            assertFalse(Package.allowsPartialCompileRetry(
                    new CompileInputFile[]{input("Helper.java"), input("Actor.kt")}, false, type));
        }
    }

    /** Successful requests, including empty sources that emit no classes, must never retry. */
    @Test
    public void testSuccessfulRequestsNeverRetry()
    {
        List<CompileInputFile[]> selections = List.of(new CompileInputFile[0],
                new CompileInputFile[]{input("Empty.java")}, new CompileInputFile[]{input("Empty.kt")},
                new CompileInputFile[]{input("Helper.java"), input("Actor.kt")});
        for (CompileInputFile[] sources : selections) {
            for (CompileType type : CompileType.values()) {
                assertFalse(Package.allowsPartialCompileRetry(sources, true, type));
            }
        }
    }

    /** Generated Java from Stride is classified by compiler input, not editable source type. */
    @Test
    public void testGeneratedStrideUsesJavaBackendPolicy()
    {
        CompileInputFile stride = new CompileInputFile(new File("Generated.java"), new File("Generated.stride"));
        assertTrue(Package.allowsPartialCompileRetry(new CompileInputFile[]{input("Helper.java"), stride},
                false, CompileType.EXPLICIT_USER_COMPILE));
        assertFalse(Package.allowsPartialCompileRetry(new CompileInputFile[]{input("Actor.kt"), stride},
                false, CompileType.EXPLICIT_USER_COMPILE));
        assertFalse(Package.allowsPartialCompileRetry(new CompileInputFile[]{stride, input("Actor.kt")},
                false, CompileType.EXPLICIT_USER_COMPILE));
    }

    private static CompileInputFile input(String name)
    {
        File file = new File(name);
        return new CompileInputFile(file, file);
    }
}
