/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2014  Michael Kolling and John Rosenberg 
 
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

import java.io.IOException;
import java.util.Properties;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Interface to a package file.
 * 
 * @author Poul Henriksen
 */
@OnThread(Tag.Any)
public interface PackageFile
{
    /**
     * Create this package.
     * 
     * @return true if it was created, false if it was not created (possibly
     *         because a package already existed here).
     * @throws IOException If the package file(s) could not be created.
     * 
     */
    public boolean create()
        throws IOException;

    /**
     * Load the properties from the file into the given properties.
     * 
     * @throws IOException
     */
    public void load(Properties p)
        throws IOException;

    /**
     * Save the given properties to the file.
     * 
     * @return False if it couldn't save it.
     * @throws IOException
     * 
     */
    public void save(Properties p)
        throws IOException;
}
