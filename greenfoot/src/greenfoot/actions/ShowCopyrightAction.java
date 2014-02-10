/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010,2011,2014  Poul Henriksen and Michael Kolling 
 
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
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

/**
 * Action to display a copyright notice.
 *
 * @author mik
 */
public class ShowCopyrightAction extends AbstractAction
{
    private static ShowCopyrightAction instance;
    
     /**
     * Singleton factory method for action.
     */
    public static ShowCopyrightAction getInstance(JFrame parent)
    {
        if(instance == null) {
            instance = new ShowCopyrightAction(parent);
        }
        return instance;
    }


    private JFrame parent;

    /** 
     *  Creates a new instance of ShowCopyrightAction 
     */
    private ShowCopyrightAction(JFrame parent) 
    {
        super(Config.getString("greenfoot.copyright"));
        this.parent = parent;
    }
    
    /**
     * The action was fired...
     */
    public void actionPerformed(ActionEvent e)
    {
        JOptionPane.showMessageDialog(parent, new String[]{
                Config.getString("menu.help.copyright.line0"), " ",
                Config.getString("menu.help.copyright.line1"), Config.getString("menu.help.copyright.line2"),
                Config.getString("menu.help.copyright.line3"), Config.getString("menu.help.copyright.line4"),
                }, 
                Config.getString("menu.help.copyright.title"), JOptionPane.INFORMATION_MESSAGE);
    }
}
