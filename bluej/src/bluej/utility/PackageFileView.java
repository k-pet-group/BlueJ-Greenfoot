/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kšlling and John Rosenberg 
 
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

import bluej.Config;
import bluej.pkgmgr.Package;

import javax.swing.*;
import javax.swing.filechooser.*;
import java.io.File;

/**
 * A FileView subclass that enables BlueJ packages to be displayed with a
 * distinct icon in a FileChooser.
 *
 * @author Michael Kolling
 * @see FileUtility
 * @version $Id: PackageFileView.java 6164 2009-02-19 18:11:32Z polle $
 */
public class PackageFileView extends FileView
{
    static final Icon packageIcon = Config.getImageAsIcon("image.filechooser.packageIcon");

    /**
     * The name of the file.  Do nothing special here. Let the system file
     * view handle this. (All methods that return null get then handled by
     * the system.)
     */
    public String getName(File f)
    {
        return null;
    }

    /**
     * A human readable description of the file.
     */
    public String getDescription(File f)
    {
        return null;
    }

    /**
     * A human readable description of the type of the file.
     */
    public String getTypeDescription(File f)
    {
        return null;
    }

    /**
     * Here we return proper BlueJ package icons for BlueJ packages.
     * Everything else gets handled by the system (by returning null).
     */
    public Icon getIcon(File f)
    {
        if(Package.isBlueJPackage(f))
            return packageIcon;
        else
            return null;
    }
}
