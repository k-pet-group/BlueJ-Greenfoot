/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2016  Michael Kolling and John Rosenberg
 
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

import bluej.Config;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Factory for creating package files.
 * 
 * @author Poul Henriksen
 */
public class PackageFileFactory
{

    /**
     * Get a packagefile for the given directory. This will be either a
     * Greenfoot or BlueJ package file, depending on whether we are using this
     * from Greenfoot or BlueJ.
     * 
     * @param dir
     * @return
     */
    @OnThread(Tag.Any)
    public static PackageFile getPackageFile(File dir)
    {
        if (Config.isGreenfoot()) {
            return new GreenfootProjectFile(dir);
        }
        else {
            return new BlueJPackageFile(dir);
        }
    }
}
