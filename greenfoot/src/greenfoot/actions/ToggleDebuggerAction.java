/*
 This file is part of the BlueJ program. 
 Copyright (C) 2010  Michael Kolling and John Rosenberg 
 
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

import greenfoot.core.GProject;

import javax.swing.ButtonModel;
import javax.swing.JToggleButton;

/**
 * Creates the debugger window when called, also setting
 * the checkbox of its menu item to its current state
 * (visible or not).
 * 
 * @author Philip Stevens
 */
public class ToggleDebuggerAction extends ToggleAction 
{
    public ToggleDebuggerAction(String title, GProject project) 
    {
        super(title, project);
    }

    /**
     * @param project	Set the debuggers project to be this parameter
     */
    @Override
    public void setProject(GProject project)
    {
        this.project = project;
    }

    /**
     * Creates a new {@link ExecControlButtonModel} to
     * determine the state of the debugger window,
     * in terms of visibility.
     */
    @Override
    public ButtonModel getToggleModel() 
    {
        return new ExecControlButtonModel();
    }

    /**
     * Subclass that updates the check box field and also
     * opens the debugger or closes it depending on its
     * current state.
     * 
     * @author Philip Stevens
     */
    public class ExecControlButtonModel extends JToggleButton.ToggleButtonModel
    {
        /**
         * Returns whether or not the debugger window is currently visible.
         */
        @Override
        public boolean isSelected()
        {
            if (project != null) {
                return project.isExecControlVisible();
            }
            return false;
        }

        /**
         * Updates the visibility of the debugger window, and
         * sets the checkbox to the parameter.
         * @param b	State to set the checkbox to.
         */
        @Override
        public void setSelected(boolean b)
        {
            if (project != null && b != isSelected()) {
                super.setSelected(b);
                project.toggleExecControls();
            }
        }
    }
}
