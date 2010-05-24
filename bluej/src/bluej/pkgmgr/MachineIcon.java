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
package bluej.pkgmgr;

import java.awt.AWTEvent;
import java.awt.event.MouseEvent;

import javax.swing.*;

import bluej.Config;
import bluej.pkgmgr.actions.RestartVMAction;

public class MachineIcon extends JLabel
{
    private static final Icon workingIcon = Config.getFixedImageAsIcon("working.gif");
    private static final Icon notWorkingIcon = Config.getFixedImageAsIcon("working-idle.gif");
    private static final Icon workingIconDisabled = Config.getFixedImageAsIcon("working-disab.gif");
    private static final Icon stoppedIcon = Config.getFixedImageAsIcon("working-stopped.gif");

    private JPopupMenu popupMenu;

    public MachineIcon()
    {
        setIcon(notWorkingIcon);
        setDisabledIcon(workingIconDisabled);
        setToolTipText(Config.getString("tooltip.progress"));
        popupMenu = createMachinePopup();
        enableEvents(AWTEvent.KEY_EVENT_MASK);
    }

    /**
     * Indicate that the machine is idle.
     */
    public void setIdle()
    {
        setIcon(notWorkingIcon);
    }

    /**
     * Indicate that the machine is running.
     */
    public void setRunning()
    {
        setIcon(workingIcon);
    }

    /**
     * Indicate that the machine is stopped.
     */
    public void setStopped()
    {
        setIcon(stoppedIcon);
    }

    /**
     * Process a mouse click into this object. If it was a popup event, show the
     * object's menu.
     */
    protected void processMouseEvent(MouseEvent evt)
    {
        if (!isEnabled())
            return;

        super.processMouseEvent(evt);

        if (evt.isPopupTrigger()) {
            popupMenu.show(this, 10, 10);
        }
    }

    private JPopupMenu createMachinePopup()
    {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem item = new JMenuItem(RestartVMAction.getInstance());
        menu.add(item);
        return menu;
    }
}

