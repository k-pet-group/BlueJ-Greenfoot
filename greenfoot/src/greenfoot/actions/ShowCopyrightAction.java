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
        if(instance == null)
            instance = new ShowCopyrightAction(parent);
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
                "Greenfoot \u00a9 2005-2008 Michael K\u00F6lling, Poul Henriksen.", " ",
                "Greenfoot is available 'as is' free of charge for use and non-commercial",
                "redistribution. Disassembly of the system is prohibited. Greenfoot may",
                "not be sold for profit or included in other packages which are sold for",
                "profit without written authorisation.",
                " ",
                "The Greenfoot library used in exported Greenfoot scenarios",
                "('GreenfootScenarioViewer') is exempt from the not-for-profit clause.",
                "Thus, this license imposes no commercial restrictions on scenarios",
                "created and exported with Greenfoot. Licenses for individual scenarios",
                "may be defined by scenario authors as they see fit, including",
                "commercial and for-profit licenses. Disassembly of the Greenfoot",
                "library is prohibited."
                }, 
                "Copyright, License and Redistribution", JOptionPane.INFORMATION_MESSAGE);
    }
}
