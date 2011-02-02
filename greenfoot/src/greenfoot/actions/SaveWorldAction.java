/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2010,2011  Poul Henriksen and Michael Kolling 
 
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
import greenfoot.core.ClassStateManager.CompiledStateListener;
import greenfoot.core.GClass;
import greenfoot.record.GreenfootRecorder;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;

import bluej.Config;
import bluej.utility.Debug;

/**
 * Action to "save the world" - i.e. write out code which restores the world and the
 * actors in it.
 */
public class SaveWorldAction extends AbstractAction implements CompiledStateListener
{
    private GreenfootRecorder recorder;
    private boolean recordingValid;
    private GClass lastWorldGClass;

    /**
     * Construct a new action to save the world state.
     */
    public SaveWorldAction(GreenfootRecorder recorder, ClassStateManager classStateManager)
    {
        super(Config.getString("save.world"));
        setEnabled(false);
        this.recorder = recorder;
        if (classStateManager != null) {
            classStateManager.addCompiledStateListener(this);
        }
    }

    @Override
    public void actionPerformed(ActionEvent arg0)
    {
        final String methodName = GreenfootRecorder.METHOD_NAME;
        
        List<String> code = recorder.getCode();
                
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
            GClass lastWorld = getLastWorldGClass();
            lastWorld.insertMethodCallInConstructor(methodName, false);
            lastWorld.insertAppendMethod(comment.toString(), "private", methodName, method.toString(), true, false);
            lastWorld.showMessage(Config.getString("record.saved.message"));
            // Now that we've inserted the code, we must reset the recorder,
            // so that if the user saves the world again before re-compiling,
            // it doesn't insert the same code twice.  If the user scrubs our method
            // and saves the world before re-compiling this will then go wrong
            // (by inserting code depending on objects no longer there) but that
            // seems less likely:
            recorder.clearCode(false);
            
            lastWorld.compile(false, true);
        }
        catch (Exception e) {
            Debug.reportError("Error trying to get editor for world class and insert method (with call)", e);
        }
    }

    /**
     * Check whether the action should currently be enabled.
     */
    private synchronized boolean shouldBeEnabled()
    {
        GClass lastWorld = getLastWorldGClass();
        return recordingValid && lastWorld != null && lastWorld.isCompiled();
    }

    /**
     * Set the recording state as valid or not. If invalid, the action becomes disabled.
     * This can be called from any thread.
     */
    public synchronized void setRecordingValid(boolean valid)
    {
        boolean oldValid = recordingValid;
        recordingValid = valid;
        if (oldValid != recordingValid) {
            //This action will actually change the status of the menu
            EventQueue.invokeLater(new Runnable() {
                @Override
                public void run()
                {
                    updateEnabledStatus();
                }
            });
        }        
    }
    
    private void updateEnabledStatus()
    {
        setEnabled(shouldBeEnabled());
    }

    @Override
    public void compiledStateChanged(final GClass gclass, boolean compiled)
    {
        EventQueue.invokeLater(new Runnable() {
            public void run()
            {
                GClass lastClass = getLastWorldGClass();
                if (lastClass != null && gclass.getQualifiedName().equals(lastClass.getQualifiedName())) {            
                    updateEnabledStatus();
                }
            }
        });
    }
    
    private synchronized GClass getLastWorldGClass()
    {
        return lastWorldGClass;
    }
    
    /**
     * Set the most recently interactively instantiated world class.
     * This is currently called from any & every thread.
     */
    public synchronized void setLastWorldGClass(GClass lastWorld)
    {
        lastWorldGClass = lastWorld;
    }
}
