/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2010  Poul Henriksen and Michael Kolling 
 
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

import greenfoot.core.ClassStateManager;
import greenfoot.core.GClass;
import greenfoot.core.ClassStateManager.CompiledStateListener;
import greenfoot.platforms.ide.WorldHandlerDelegateIDE;
import greenfoot.record.GreenfootRecorder;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;

import bluej.Config;
import bluej.utility.Debug;

public class SaveWorldAction extends AbstractAction implements CompiledStateListener
{
    private WorldHandlerDelegateIDE ide;
    private boolean recordingValid;

    public SaveWorldAction(WorldHandlerDelegateIDE ide, ClassStateManager classStateManager)
    {
        super(Config.getString("save.world"));
        setEnabled(false);
        this.ide = ide;
        if (classStateManager != null)
            classStateManager.addCompiledStateListener(this);
    }

    public void actionPerformed(ActionEvent arg0)
    {
        final String methodName = GreenfootRecorder.METHOD_NAME;
        
        List<String> code = ide.getInitWorldCode();
                
        final String oneIndent = "    ";
        final String twoIndent = oneIndent + oneIndent;
        
        StringBuffer comment = new StringBuffer();
        comment.append("\n").append(oneIndent).append("/**\n");
        comment.append(oneIndent).append("* ").append(Config.getString("record.method.comment1")).append("\n");
        comment.append(oneIndent).append("* ").append(Config.getString("record.method.comment2")).append("\n");
        comment.append(oneIndent).append("*/\n");
        
        StringBuffer method = new StringBuffer();
        for (String line : code) {
            method.append(twoIndent).append(line).append("\n");
        }
               
        try {
            GClass lastWorld = ide.getLastWorldGClass();
            lastWorld.insertMethodCallInConstructor(methodName, false);
            lastWorld.insertAppendMethod(comment.toString(), "private", methodName, method.toString(), true, false);
            lastWorld.showMessage(Config.getString("record.saved.message"));
            // Now that we've inserted the code, we must reset the recorder,
            // so that if the user saves the world again before re-compiling,
            // it doesn't insert the same code twice.  If the user scrubs our method
            // and saves the world before re-compiling this will then go wrong
            // (by inserting code depending on objects no longer there) but that
            // seems less likely:
            ide.clearRecorderCode();
            
            lastWorld.compile(false, true);
        }
        catch (Exception e) {
            Debug.reportError("Error trying to get editor for world class and insert method (with call)", e);
        }
    }

    public boolean isEnabled()
    {
        GClass lastWorld = ide.getLastWorldGClass();
        return recordingValid && super.isEnabled() && lastWorld != null && lastWorld.isCompiled();
    }

    public void setRecordingValid(boolean valid)
    {
        boolean oldEnabled = isEnabled();
        recordingValid = valid;
        if (oldEnabled != isEnabled()) {
            //This action will actually change the status of the menu
            updateEnabledStatus(oldEnabled);
        }        
    }
    
    private void updateEnabledStatus(final boolean oldEnabled)
    {
        EventQueue.invokeLater(new Runnable() {
            public void run ()
            {
                firePropertyChange("enabled", Boolean.valueOf(oldEnabled), Boolean.valueOf(isEnabled()));
            }
        });
    }

    public void compiledStateChanged(final GClass gclass, boolean compiled)
    {
        // We must use a thread here to avoid an inter-VM deadlock;
        // we are called by RPackageImpl remotely while it holds the lock,
        // but getLastWorldGClass() calls back to the other VM and needs that lock:        
        new Thread(new Runnable() {
            public void run()
            {
                GClass lastClass = ide.getLastWorldGClass();
                if (lastClass != null && gclass.getQualifiedName().equals(lastClass.getQualifiedName())) {            
                    updateEnabledStatus(!isEnabled());
                }
            }
        }).start();
    } 
}
