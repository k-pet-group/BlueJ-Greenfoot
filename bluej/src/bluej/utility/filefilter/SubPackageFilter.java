/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
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

import java.io.*;

import bluej.pkgmgr.Package;

/**
 * A FileFilter that only accepts BlueJ package directories.
 * An instance of this class can be used as a parameter for
 * the listFiles method of class File.
 *
 * @author  Axel Schmolitzky
 * @version $Id: SubPackageFilter.java 6347 2009-05-20 15:22:43Z polle $
 */
public class SubPackageFilter implements FileFilter
{
    /**
     * This method only accepts directories.
     */
    public boolean accept(File pathname)
    {
        return (pathname.isDirectory() &&
                 Package.isPackage(pathname));
    }
}
