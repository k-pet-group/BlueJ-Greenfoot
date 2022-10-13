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

import javax.swing.*;
import java.io.File;

/**
 * A modified JFileChooser. Modifications are made for
 * displaying BlueJ packages with a specific icon and to clear the selection
 * field after traversing into a directory.
 *
 * @author Michael Kolling
 */
class BlueJFileChooser extends JFileChooser
{
    /**
     * Create a new BlueJFileChooser.
     *
     * @param   startDirectory  directory to start the package selection in.
     */
    public BlueJFileChooser(String startDirectory)
    {
        super(startDirectory);
        setFileView(new PackageFileView());
    }

    /**
     * A directory was double-clicked. If it is a BlueJ package maybe
     * we want to treat it differently
     */
    @Override
    public void setCurrentDirectory(File dir)    // redefined
    {
        //Here we could treat bluej package differently
        //At the moment nothing is done.
        //if (Package.isBlueJPackage(dir)) { ...
        
        //commented out post 1.1.6 to fix null pointer issue with J2SDK 1.4
        //setSelectedFile(null);              //clear the textfield
        super.setCurrentDirectory(dir);
    }
}
