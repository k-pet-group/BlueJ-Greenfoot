/*
 This file is part of the BlueJ program.
 Copyright (C) 2025,2026  Michael Kolling and John Rosenberg

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
import java.io.FileFilter;

import bluej.extensions2.SourceType;

/**
 * A FileFilter that only accepts Kotlin source files (.kt).
 *
 * @author BlueJ Team
 */
public class KotlinSourceFilter implements FileFilter
{
    @Override
    public boolean accept(File pathname)
    {
        return pathname.getName().endsWith("." + SourceType.Kotlin.getExtension());
    }
}
