/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
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

import java.awt.event.ActionEvent;

import greenfoot.core.GProject;

import javax.swing.AbstractAction;
import javax.swing.ButtonModel;

/**
 * Subclasses {@link AbstractAction} allowing overriding
 * of the ActionPerformed, but also requiring subclasses
 * to implement their own getToggleModel.
 * 
 * Holds the current GProject to allow subclasses access
 * to project specific methods.
 * @author Philip Stevens
 */
public abstract class ToggleAction extends AbstractAction 
{

	/**
	 * Project to associate this action with.
	 */
	protected GProject project;

	/**
	 * Sets the project to the project provided and
	 * passes the title onto the AbstractAction constructor.
	 * @param title		Mainly used as the display of the window/menu item.
	 * @param project	Current project to associate this action with.
	 */
	public ToggleAction(String title, GProject project) 
	{
		super(title);
		setProject(project);
	}

	/**
	 * Can be overridden to perform an action.
	 */
	@Override
	public void actionPerformed(ActionEvent e) 
	{
		// do nothing
	}
	
	/**
	 * @param project	Set the debuggers project to be this parameter
	 */
	public void setProject(GProject project)
	{
		this.project = project;
	}
	
	/**
	 * Abstract method to be implemented by subclasses.
	 */
    public abstract ButtonModel getToggleModel();

}
