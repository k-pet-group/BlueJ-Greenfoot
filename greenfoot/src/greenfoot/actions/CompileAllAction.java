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
import greenfoot.core.GProject;
import greenfoot.core.Simulation;

import java.awt.event.ActionEvent;
import java.rmi.RemoteException;

import javax.swing.AbstractAction;

import bluej.extensions.CompilationNotStartedException;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;

/**
 * Action that compiles all classes that needs compilation.
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: CompileAllAction.java 6170 2009-02-20 13:29:34Z polle $
 */
public class CompileAllAction extends AbstractAction
{
	private GProject project;
    
    public CompileAllAction(GProject project)
    {
        super(Config.getString("compile.all"));
        setProject(project);
    }
    
    public void setProject(GProject project)
    {
    	this.project = project;
    	setEnabled(project != null);
    }

    /**
     * Compiles all classes.
     *  
     */
    public void actionPerformed(ActionEvent e)
    {
        try {
            int numOfClasses = project.getDefaultPackage().getClasses().length;
            // we only want to compile if there are classes in the project
            if(numOfClasses < 1) {
                return;
            }
            Simulation.getInstance().setPaused(true);
        	project.getDefaultPackage().compileAll(false);
        }
        catch (ProjectNotOpenException pnoe) {}
        catch (PackageNotFoundException pnfe) {}
        catch (RemoteException re) {
        	re.printStackTrace();
        }
        catch (CompilationNotStartedException cnse) {
        	cnse.printStackTrace();
        }
        
        // Disable the action until the compilation is finished, when it
        // will be re-enabled.
        setEnabled(false);
    }
}