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
import java.util.Arrays;
import java.util.List;

/**
 * Locates the Kotlin standard library for tests that compile or analyse Kotlin code.
 */
public class KotlinTestClasspath
{
    private KotlinTestClasspath()
    {
    }

    /**
     * Find the kotlin-stdlib JAR(s) on the test runtime classpath.
     *
     * @return the stdlib JARs, possibly empty
     */
    public static List<File> getKotlinStdlib()
    {
        String cp = System.getProperty("java.class.path");
        if (cp == null) {
            return List.of();
        }
        return Arrays.stream(cp.split(File.pathSeparator))
                .map(File::new)
                .filter(entry -> entry.getName().startsWith("kotlin-stdlib"))
                .toList();
    }
}
