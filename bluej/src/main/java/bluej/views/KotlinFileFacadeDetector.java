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
package bluej.views;

import bluej.utility.Debug;

import kotlin.metadata.jvm.KotlinClassMetadata;

import threadchecker.OnThread;
import threadchecker.Tag;

/** Identifies Kotlin file facades without treating a class-name suffix as source ownership. */
@OnThread(Tag.FXPlatform)
public final class KotlinFileFacadeDetector
{
    private KotlinFileFacadeDetector()
    {
    }

    /**
     * Checks file-facade identity using metadata from either the IDE or a student classloader.
     *
     * @param type compiled class to inspect
     * @return true for a Kotlin file facade; false for other classes or unreadable metadata
     */
    public static boolean isFileFacade(Class<?> type)
    {
        try {
            return KotlinMetadataReader.read(type) instanceof KotlinClassMetadata.FileFacade;
        }
        catch (Exception e) {
            Debug.reportError("Failed to identify Kotlin file facade for " + type.getName(), e);
            return false;
        }
    }
}
