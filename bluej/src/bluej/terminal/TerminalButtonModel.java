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
package bluej.terminal;

import javax.swing.JToggleButton;

import bluej.pkgmgr.PkgMgrFrame;

/**
 * ButtonModel for the "Show Terminal" checkBoxItem in the menu.
 * This model takes care that the right things happen when the checkbox
 * is shown or changed.
 *
 * @author Michael Kolling
 */
public class TerminalButtonModel extends JToggleButton.ToggleButtonModel
{
    private PkgMgrFrame pmf;
    
    public TerminalButtonModel(PkgMgrFrame pmf)
    {
        super();
        this.pmf = pmf;
    }

    public boolean isSelected()
    {
        if (pmf.isEmptyFrame()) {
            // if no project is open, we default to off
            return false;
        }
        else if (!pmf.getProject().hasTerminal()) {
            return false;
        }
        else {
            // otherwise, ask the Terminal if it is visible
            return pmf.getProject().getTerminal().isVisible();
        }
    }

    public void setSelected(boolean b)
    {
        if (!pmf.isEmptyFrame()) {
            super.setSelected(b);
            pmf.getProject().getTerminal().showHide(b);
        }
    }
}
