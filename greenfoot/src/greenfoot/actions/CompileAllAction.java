/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010,2011  Poul Henriksen and Michael Kolling 
 
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

import javax.swing.AbstractAction;

/**
 * Action that compiles all classes that needs compilation.
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
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
        int numOfClasses = project.getDefaultPackage().getClasses(false).length;
        // we only want to compile if there are classes in the project
        if(numOfClasses < 1) {
            return;
        }
        Simulation.getInstance().setPaused(true);
        project.getDefaultPackage().compileAll();
        
        // Disable the action until the compilation is finished, when it
        // will be re-enabled.
        setEnabled(false);
    }
}