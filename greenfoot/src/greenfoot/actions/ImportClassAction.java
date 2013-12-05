/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2011,2013  Poul Henriksen and Michael Kolling 
 
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
import greenfoot.gui.ImportClassWindow;
import greenfoot.record.InteractionListener;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;

import bluej.Config;

/**
 * The action that shows the import-class dialog
 * (which allows you to import a supplied common class),
 * and imports whatever you select into the project
 * 
 * @author neil
 */
public class ImportClassAction extends AbstractAction
{   
    private GreenfootFrame gfFrame;
    private InteractionListener interactionListener;
    private ImportClassWindow dlg;

    public ImportClassAction(GreenfootFrame gfFrame, InteractionListener interactionListener)
    {
        super(Config.getString("import.action"));
        setEnabled(false);
        this.gfFrame = gfFrame;
        this.interactionListener = interactionListener;
    }   
    
    @Override
    public void actionPerformed(ActionEvent e)
    {
        if (dlg == null)
        {
            dlg = new ImportClassWindow(gfFrame, interactionListener);
        }
        
        dlg.setVisible(true);
    }
}
