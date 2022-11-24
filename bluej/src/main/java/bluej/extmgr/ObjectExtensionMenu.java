/*
 This file is part of the BlueJ program. 
 Copyright (C) 2012,2013,2016,2019  Michael Kolling and John Rosenberg
 
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
import bluej.debugmgr.objectbench.ObjectWrapper;
import bluej.extensions2.BObject;
import bluej.extensions2.ExtensionBridge;
import bluej.extensions2.MenuGenerator;
import javafx.scene.control.MenuItem;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Implementation of the {@link ExtensionMenu} interface for the Object
 * menu.
 * 
 * @author Simon Gerlach
 */
public class ObjectExtensionMenu implements ExtensionMenu
{
    private ObjectWrapper objectWrapper;

    /**
     * Constructor. Creates a new {@link ObjectExtensionMenu}.
     * 
     * @param objectWrapper
     *            The object which was selected by the user.
     */
    @OnThread(Tag.Any)
    public ObjectExtensionMenu(ObjectWrapper objectWrapper)
    {
        this.objectWrapper = objectWrapper;
    }

    @Override
    public MenuItem getMenuItem(MenuGenerator menuGenerator)
    {
        BObject bObject = ExtensionBridge.newBObject(objectWrapper);
        return menuGenerator.getObjectMenuItem(bObject);
    }

    @Override
    public void postMenuItem(MenuGenerator menuGenerator, MenuItem onThisItem)
    {
        BObject bObject = ExtensionBridge.newBObject(objectWrapper);
        menuGenerator.notifyPostObjectMenu(bObject, onThisItem);
    }
}
