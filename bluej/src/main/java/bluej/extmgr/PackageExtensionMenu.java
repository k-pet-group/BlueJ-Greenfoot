/*
 This file is part of the BlueJ program. 
 Copyright (C) 2012,2013,2019  Michael Kolling and John Rosenberg
 
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
package bluej.extmgr;

import bluej.extensions2.BPackage;
import bluej.extensions2.ExtensionBridge;
import bluej.extensions2.MenuGenerator;
import bluej.pkgmgr.Package;
import javafx.scene.control.MenuItem;

/**
 * Implementation of the {@link ExtensionMenu} interface for the Package
 * menu.
 * 
 * @author Simon Gerlach
 */
public class PackageExtensionMenu implements ExtensionMenu
{
    private Package bluejPackage;

    /**
     * Constructor. Creates a new {@link PackageExtensionMenu}.
     * 
     * @param bluejPackage
     *            The current package opened in BlueJ.
     */
    public PackageExtensionMenu(Package bluejPackage)
    {
        this.bluejPackage = bluejPackage;
    }

    @Override
    public MenuItem getMenuItem(MenuGenerator menuGenerator)
    {
        BPackage bPackage = ExtensionBridge.newBPackage(bluejPackage);
        return menuGenerator.getPackageMenuItem(bPackage);
    }

    @Override
    public void postMenuItem(MenuGenerator menuGenerator, MenuItem onThisItem)
    {
        BPackage bPackage = ExtensionBridge.newBPackage(bluejPackage);
        menuGenerator.notifyPostPackageMenu(bPackage, onThisItem);
    }
}
