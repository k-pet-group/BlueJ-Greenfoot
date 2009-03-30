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

import greenfoot.gui.GreenfootFrame;
import greenfoot.gui.classbrowser.ClassView;

import java.awt.event.ActionEvent;

import javax.swing.JFrame;

import bluej.Config;

/**
 * An action to remove the currently selected class.
 * 
 * @author davmac
 * @version $Id: RemoveSelectedClassAction.java 6216 2009-03-30 13:41:07Z polle $
 */
public class RemoveSelectedClassAction extends ClassAction
{
    private JFrame frame;

    /**
     * Construct a remove action to remove the currently selected class.
     * The constructed action should be set as selection listener for the
     * class browser.
     */
    public RemoveSelectedClassAction(GreenfootFrame gfFrame)
    {
        super(Config.getString("remove.selected"), gfFrame);
        setEnabled(false);
        this.frame = gfFrame;
    }
    
    public void actionPerformed(ActionEvent e)
    {
    	ClassView cls = getSelectedClassView();
    	
    	boolean confirmed = RemoveClassAction.confirmRemoveClass(cls, frame);
        if (confirmed) {
            cls.remove();
        }
    }

}
