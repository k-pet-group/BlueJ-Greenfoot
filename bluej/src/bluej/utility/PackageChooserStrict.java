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

import bluej.pkgmgr.Package;

import java.io.File;

/**
 * A file chooser for opening packages (with strict behaviour with
 * regards clicking on BlueJ packages).
 *
 * <p>Behaves the same as a PackageChooser but with the added restriction
 * that only BlueJ package directories, and archives, are an acceptable
 * selection. Double clicking on a BlueJ package will open it rather
 * than traverse into it.
 *
 * @author Michael Kolling
 * @author Axel Schmolitzky
 * @author Markus Ostman
 */
public class PackageChooserStrict extends PackageChooser
{
    /**
     * Create a new strict PackageChooser.
     *
     * @param startDirectory the directory to start the package selection in.
     */
    public PackageChooserStrict(File startDirectory)
    {
        super(startDirectory, false, true);
    }

    /**
     *  Selection approved by button-click. Check whether the selected
     *  directory is a BlueJ package. If so, let it be opened.
     */
    @Override
    public void approveSelection()   // redefined
    {
        File selectedFile = getSelectedFile();
        if (selectedFile.isFile()) {
            // it must be an archive (jar or zip)
            approved();
        }
        else if (Package.isPackage(getSelectedFile())) {
            approved();
        }
        else {
            super.setCurrentDirectory(getSelectedFile());
        }
    }
}
