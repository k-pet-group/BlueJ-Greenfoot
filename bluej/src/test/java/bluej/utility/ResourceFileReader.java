/*
 This file is part of the BlueJ program.
 Copyright (C) 2009,2010,2011,2012,2014,2016,2022,2024  Michael Kolling and John Rosenberg

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
package bluej.utility;

import java.io.File;
import java.net.URL;

public class ResourceFileReader {
    
    /**
     * Get resource as File object.
     */
    public static File getResourceAsFile(Class<?> clazz, String name)
    {
        URL url = clazz.getResource(name);
        return url != null && !url.getFile().isEmpty() ? new File(url.getFile()) : null;
    }

    /**
     * Get resource file wrapped in SourceInput for InfoParser.
     * Returns FileSource with default resolver for test convenience.
     * This allows tests using getResourceFile() to work with new SourceInput API.
     * 
     * @param clazz Class to load resource from
     * @param name Resource path
     * @return SourceInput.FileSource ready for parsing, or null if resource not found
     */
    public static bluej.parser.SourceInput getResourceFile(Class<?> clazz, String name)
    {
        File file = getResourceAsFile(clazz, name);
        if (file == null) {
            return null;
        }
        
        // Determine source type from file extension or path (test resources in /kotlin/ dir are Kotlin)
        bluej.extensions2.SourceType sourceType = (name.endsWith(".kt") || name.contains("/kotlin/"))
            ? bluej.extensions2.SourceType.Kotlin
            : bluej.extensions2.SourceType.Java;
        
        // Create FileSource with default ClassLoaderResolver for tests
        return new bluej.parser.SourceInput.FileSource(
            file,
            sourceType,
            java.nio.charset.StandardCharsets.UTF_8,
            null,
            new bluej.parser.entity.ClassLoaderResolver(clazz.getClassLoader())
        );
    }
}