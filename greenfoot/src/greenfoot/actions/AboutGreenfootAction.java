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

import greenfoot.gui.AboutGreenfootDialog;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JFrame;

import bluej.Boot;
import bluej.Config;

/**
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id$
 */
public class AboutGreenfootAction extends AbstractAction
{
    private static AboutGreenfootAction instance;
    
     /**
     * Singleton factory method for action.
     */
    public static AboutGreenfootAction getInstance(JFrame parent)
    {
        if(instance == null)
            instance = new AboutGreenfootAction(parent);
        return instance;
    }
    
    
    private AboutGreenfootDialog aboutGreenfoot;
    private JFrame parent;

    private AboutGreenfootAction(JFrame parent)
    {
        super(Config.getString("greenfoot.about"));
        this.parent = parent;
    }

    public void actionPerformed(ActionEvent e)
    {
        if (aboutGreenfoot == null) {
            aboutGreenfoot = new AboutGreenfootDialog(parent, Boot.GREENFOOT_VERSION);
        }
        aboutGreenfoot.setVisible(true);
    }
}