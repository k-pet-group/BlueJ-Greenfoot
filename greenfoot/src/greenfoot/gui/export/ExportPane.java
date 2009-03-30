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
/*
 * ExportPane is a superclass for all changing panes that can appear 
 * in the Export dialogue.
 *
 * @author Michael Kolling
 * @version $Id: ExportPane.java 6216 2009-03-30 13:41:07Z polle $
 */

package greenfoot.gui.export;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import bluej.Config;

public abstract class ExportPane extends JPanel
{
    private static final String lockText = Config.getString("export.lock.label");
    private static final String lockDescription = Config.getString("export.lock.description");

    protected JCheckBox lockScenario;

    /**
     * Create a an export pane for export to web pages.
     */
    public ExportPane()
    {
        lockScenario = new JCheckBox(lockText, false);
        lockScenario.setSelected(true);
        lockScenario.setAlignmentX(LEFT_ALIGNMENT);
        lockScenario.setToolTipText(lockDescription);
        lockScenario.setOpaque(false);
    }

    /**
     * This method will be called when this pane is activated (about to be
     * shown/visible)
     * @param continueButton 
     */
    public abstract void activated(JButton continueButton);
    
    /**
     * This method will be called when the user is about to export the scenario
     * with information from this pane. Will be called from the swing event
     * thread and will not publish until this method returns.
     * 
     * @return Whether to continue publishing. Continues if true, cancels if false.
     */
    public abstract boolean prePublish();  
    
    /**
     * This method will be called when the scenario has been published with the
     * information from this pane.
     * 
     * @param success Whether the publish was successfull
     */
    public abstract void postPublish(boolean success);

    /**
     * Return true if the user wants to lock the scenario.
     */
    public boolean lockScenario()
    {
        return lockScenario.isSelected();
    }
}