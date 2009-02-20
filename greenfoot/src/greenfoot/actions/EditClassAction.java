/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009  Poul Henriksen and Michael Kšlling 
 
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
package greenfoot.actions;

import bluej.Config;
import greenfoot.core.GClass;
import greenfoot.gui.classbrowser.ClassBrowser;
import greenfoot.gui.classbrowser.ClassView;

import java.awt.event.ActionEvent;
import java.rmi.RemoteException;

import javax.swing.AbstractAction;

import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;

/**
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: EditClassAction.java 6170 2009-02-20 13:29:34Z polle $
 */
public class EditClassAction extends AbstractAction
{
	private ClassBrowser classBrowser;
	
    public EditClassAction(ClassBrowser classBrowser)
    {
        super(Config.getString("edit.class"));
        this.classBrowser = classBrowser;
    }
    
    /**
     * Edits the currently selected class. If no class is selected it does
     * nothing.
     * 
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e)
    {
    	ClassView selectedView = (ClassView) classBrowser.getSelectionManager().getSelected();
    	GClass selectedClass = selectedView.getGClass();
    	
        try {
            if (selectedClass != null) {
                selectedClass.edit();
            }

        }
        catch (RemoteException e1) {
            e1.printStackTrace();
        }
        catch (ProjectNotOpenException e1) {
            e1.printStackTrace();
        }
        catch (PackageNotFoundException e1) {
            e1.printStackTrace();
        }
    }

}