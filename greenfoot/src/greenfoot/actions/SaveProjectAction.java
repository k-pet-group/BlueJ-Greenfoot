/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009  Poul Henriksen and Michael Kolling 
 
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
import greenfoot.gui.GreenfootFrame;

import java.awt.event.ActionEvent;
import java.rmi.RemoteException;

import javax.swing.AbstractAction;

import bluej.extensions.ProjectNotOpenException;
import bluej.utility.Debug;

/**
 * @author Poul Henriksen
 * @version $Id$
 */
public class SaveProjectAction extends AbstractAction
{
    private GreenfootFrame gfFrame;
    
    public SaveProjectAction(GreenfootFrame gfFrame)
    {
        super(Config.getString("project.save"));
        this.gfFrame = gfFrame;
        setEnabled(false);
    }
    
    public void actionPerformed(ActionEvent e)
    {
        try {
            gfFrame.getProject().save();
        }
        catch (ProjectNotOpenException e1) {
            Debug.reportError("Could not save scenario because it is not open.");
        }
        catch (RemoteException e1) {
            Debug.reportError("Could not save scenario because of a remote exception.");
            e1.printStackTrace();
        }
    }
}