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
import greenfoot.core.GClass;
import greenfoot.core.Simulation;
import greenfoot.gui.GreenfootFrame;

import java.awt.event.ActionEvent;
import java.rmi.RemoteException;

import bluej.extensions.CompilationNotStartedException;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;

/**
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: CompileClassAction.java 8313 2010-09-14 13:02:19Z nccb $
 */
public class CompileClassAction extends ClassAction
{
    public CompileClassAction(GreenfootFrame gfFrame)
    {
        super(Config.getString("compile.class"), gfFrame);
    }

    /**
     * Compiles the currently selected class. If no class is selected it does
     * nothing.
     * 
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e)
    {
    	GClass selectedClass = getSelectedClassView().getGClass();
    	
        Simulation.getInstance().setPaused(true);
        try {
            if (selectedClass != null) {
                selectedClass.compile(false, false);
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
        catch (CompilationNotStartedException e1) {
            e1.printStackTrace();
        }
    }
}