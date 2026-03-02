/*
 This file is part of the BlueJ program. 
 Copyright (C) 2025  Michael Kolling and John Rosenberg 

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
package bluej.parser.context;

import bluej.classmgr.BPClassLoader;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.io.File;

/**
 * Interface providing the minimal dependencies needed by CompilationUnitContextLoader.
 * This abstraction enables unit testing without requiring full Project instantiation.
 * 
 * <p>The interface defines the contract for providing:
 * <ul>
 *   <li>A ClassLoader for loading project classes</li>
 *   <li>The project directory root for resolving .ctxt files</li>
 * </ul>
 * 
 * <p>Production code uses Project which implements this interface.
 * Test code can provide lightweight implementations for specific scenarios.
 * 
 * @see CompilationUnitContextLoader
 */
@OnThread(value = Tag.Any, ignoreParent = true)
public interface ClassLoaderProvider {
    /**
     * Gets the ClassLoader to use for loading classes and resources.
     * 
     * @return the BPClassLoader for this provider
     */
    BPClassLoader getClassLoader();
    
    /**
     * Gets the project directory root.
     * 
     * @return the project directory
     */
    File getProjectDir();
}