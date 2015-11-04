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
package bluej.utility;

import java.io.File;

import javax.swing.Icon;
import javax.swing.filechooser.FileView;

import bluej.Config;
import bluej.pkgmgr.Package;

/**
 * A FileView subclass that enables BlueJ packages to be displayed with a
 * distinct icon in a FileChooser.
 *
 * @author Michael Kolling
 * @see FileUtility
 * @version $Id: PackageFileView.java 12533 2014-10-10 12:08:52Z nccb $
 */
public class PackageFileView extends FileView
{
    private static Icon bluejProjectIcon;
    private static Icon greenfootProjectIcon;

    /**
     * The name of the file.  Do nothing special here. Let the system file
     * view handle this. (All methods that return null get then handled by
     * the system.)
     */
    @Override
    public String getName(File f)
    {
        return null;
    }

    /**
     * A human readable description of the file.
     */
    @Override
    public String getDescription(File f)
    {
        return null;
    }

    /**
     * A human readable description of the type of the file.
     */
    @Override
    public String getTypeDescription(File f)
    {
        return null;
    }

    /**
     * Here we return proper BlueJ package icons for BlueJ packages.
     * Everything else gets handled by the system (by returning null).
     */
    @Override
    public Icon getIcon(File f)
    {
        if (Config.isMacOS() && f.getAbsolutePath().equals("/net")) {
            // On MacOS this path is a special mapping; looking for a particular
            // file inside it can cause a significant delay.
            return null;
        }
        
        if(Package.isPackage(f))
            if (Config.isGreenfoot()) {
                if (greenfootProjectIcon == null)
                {
                    greenfootProjectIcon = Config.getFixedImageAsIcon("greenfoot-project.png");
                }
                return greenfootProjectIcon;
            }
            else {
                if (bluejProjectIcon == null)
                {
                    bluejProjectIcon = Config.getFixedImageAsIcon("bluej-project.png");
                }
                return bluejProjectIcon;
            }
        else {
            return null;
        }
    }
}
