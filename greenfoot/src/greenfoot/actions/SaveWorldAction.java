/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2010-2015,2016  Poul Henriksen and Michael Kolling
 
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

import javax.swing.AbstractAction;

import bluej.Config;
import bluej.extensions.CompilationNotStartedException;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;
import bluej.stride.framedjava.elements.CallElement;
import bluej.stride.framedjava.elements.NormalMethodElement;
import bluej.utility.Debug;
import java.rmi.RemoteException;
import java.util.Optional;

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
        NormalMethodElement method = recorder.getPrepareMethod();
        CallElement methodCall = recorder.getPrepareMethodCall();

        GClass lastWorld = getLastWorldGClass();
        lastWorld.insertMethodCallInConstructor(methodCall.toXML().toXML(), false);
        lastWorld.insertAppendMethod(method.toXML().toXML(), true, false, true);
        // Now that we've inserted the code, we must reset the recorder,
        // so that if the user saves the world again before re-compiling,
        // it doesn't insert the same code twice.  If the user scrubs our method
        // and saves the world before re-compiling this will then go wrong
        // (by inserting code depending on objects no longer there) but that
        // seems less likely:
        recorder.clearCode(false);
    }

    /**
     * Check whether the action should currently be enabled.
     *
     * @param compiledState The compiled state of the last world class, if we know it.
     *                      Otherwise, we use the isCompiled method.  We don't just always
     *                      use isCompiled because it might not be up to date yet.
     */
    private synchronized boolean shouldBeEnabled(Optional<Boolean> compiledState)
    {
        GClass lastWorld = getLastWorldGClass();

        boolean compiled = compiledState.orElse(lastWorld == null ? false : lastWorld.isCompiled());

        return recordingValid && lastWorld != null && compiled;
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
                    updateEnabledStatus(Optional.empty());
                }
            });
        }        
    }
    
    private void updateEnabledStatus(Optional<Boolean> compiledState)
    {
        setEnabled(shouldBeEnabled(compiledState));
    }

    @Override
    public void compiledStateChanged(final GClass gclass, boolean compiled)
    {
        EventQueue.invokeLater(new Runnable() {
            public void run()
            {
                GClass lastClass = getLastWorldGClass();
                if (lastClass != null && gclass.getQualifiedName().equals(lastClass.getQualifiedName())) {
                    // Because we are in a race with the compiled state update, we can't
                    // rely on gclass.isCompiled being up to date yet, so we pass in our copy of
                    // the compiled state:
                    updateEnabledStatus(Optional.of(compiled));
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
