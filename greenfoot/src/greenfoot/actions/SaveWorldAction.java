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

import greenfoot.core.GClass;
import greenfoot.gui.GreenfootFrame;
import greenfoot.platforms.ide.WorldHandlerDelegateIDE;
import greenfoot.record.GreenfootRecorder;

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;

import bluej.Config;

import bluej.utility.Debug;

public class SaveWorldAction extends AbstractAction
{
    private WorldHandlerDelegateIDE ide;

    public SaveWorldAction(GreenfootFrame greenfootFrame, WorldHandlerDelegateIDE ide)
    {
        super(Config.getString("save.world"));
        setEnabled(false);
        this.ide = ide;
    }

    public void actionPerformed(ActionEvent arg0)
    {
        final String methodName = GreenfootRecorder.METHOD_NAME;
        
        List<String> code = ide.getInitWorldCode();
                
        final String oneIndent = "    ";
        final String twoIndent = oneIndent + oneIndent;
        
        StringBuffer comment = new StringBuffer();
        comment.append("\n").append(oneIndent).append("/**\n");
        comment.append(oneIndent).append("* A method that performs your recorded actions.\n");
        comment.append(oneIndent).append("*/\n");
        
        StringBuffer method = new StringBuffer();
        for (String line : code) {
            method.append(twoIndent).append(line).append("\n");
        }
               
        try {
            GClass lastWorld = ide.getLastWorldGClass();
            lastWorld.insertAppendMethod(comment.toString(), methodName, method.toString());
            lastWorld.insertMethodCallInConstructor(methodName);
            // Now that we've inserted the code, we must reset the recorder,
            // so that if the user saves the world again before re-compiling,
            // it doesn't insert the same code twice.  If the user scrubs our method
            // and saves the world before re-compiling this will then go wrong
            // (by inserting code depending on objects no longer there) but that
            // seems less likely:
            ide.clearRecorderCode();
        }
        catch (Exception e) {
            Debug.reportError("Error trying to get editor for world class and insert method (with call)", e);
        }
    }

}
