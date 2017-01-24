/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010,2016,2017  Poul Henriksen and Michael Kolling
 
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
package rmiextension;

import javax.swing.*;
import java.awt.EventQueue;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicReference;

import bluej.debugger.Debugger;
import bluej.debugger.DebuggerObject;
import bluej.debugger.DebuggerResult;
import bluej.debugmgr.ResultWatcher;
import bluej.debugmgr.objectbench.ObjectBench;
import bluej.debugmgr.objectbench.ObjectWrapper;
import bluej.extensions.BPackage;
import bluej.extensions.ExtensionBridge;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.PkgMgrFrame;

/**
 * 
 * This is an Invoker that can instantiate objects of classes that are NOT a part
 * of a project.
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 */
public class ConstructorInvoker
{
    private PkgMgrFrame pkgFrame;
    private String className;
    private DebuggerResult result;

    public ConstructorInvoker(BPackage bPackage, String className)
        throws ProjectNotOpenException, PackageNotFoundException
    {
        pkgFrame = ExtensionBridge.getPkgMgrFrame(bPackage);
        this.className = className;
    }
   
    
    /**
     * Invoke a constructor which takes String arguments only.
     * 
     * @param instanceNameOnObjectBench  Name of the created object as it
     *                                   should appear on the bench
     * @param args           Arguments to supply to the constructor; the constructor can
     *                       only take String parameters
     * @param resultNotify   A watcher to be notified when the constructor completes
     *                       (or when execution fails). Notification will occur on the AWT
     *                       event thread. 
     */
    public void invokeConstructor(final String instanceNameOnObjectBench, final String[] args,
            final ResultWatcher resultWatcher)
    {
        final AtomicReference<ObjectBench> objBench = new AtomicReference<>(pkgFrame.getObjectBench());
        final Package pkg = pkgFrame.getPackage(); 
        final Debugger debugger = pkgFrame.getProject().getDebugger();
                
        Thread t = new Thread() {
            public void run() {
                // It's possible the FX thread hasn't had chance to initialise Object Bench by the time we asked.
                // If so, keep asking until it is initialised:
                while (objBench.get() == null)
                {
                    try
                    {
                        Thread.sleep(100);
                        SwingUtilities.invokeAndWait(() -> {objBench.set(pkgFrame.getObjectBench()); });
                    }
                    catch (InterruptedException | InvocationTargetException e)
                    {
                    }
                    
                }
                
                String [] argTypes = new String[args.length];
                DebuggerObject [] argObjects = new DebuggerObject[args.length];
                for (int i = 0; i < args.length; i++) {
                    argTypes[i] = "java.lang.String";
                    argObjects[i] = debugger.getMirror(args[i]);
                }
                
                result = debugger.instantiateClass(className, argTypes, argObjects);
                final DebuggerObject debugObject = result.getResultObject();
                
                EventQueue.invokeLater(new Runnable() {
                    @Override
                    public void run()
                    {
                        if (debugObject != null) {
                            ObjectWrapper wrapper = ObjectWrapper.getWrapper(
                                    pkgFrame, objBench.get(),
                                    debugObject,
                                    debugObject.getGenType(),
                                    instanceNameOnObjectBench);       

                            objBench.get().addObject(wrapper);
                            pkg.getDebugger().addObject(pkg.getQualifiedName(), wrapper.getName(), debugObject);  
                        }
                        
                        if (resultWatcher != null) {
                            int status = result.getExitStatus();
                            if (status == Debugger.NORMAL_EXIT) {
                                resultWatcher.putResult(result.getResultObject(),
                                        instanceNameOnObjectBench, null);
                            }
                            else if (status == Debugger.EXCEPTION) {
                                resultWatcher.putException(result.getException(), null);
                            }
                            else if (status == Debugger.TERMINATED) {
                                resultWatcher.putVMTerminated(null);
                            }
                        }
                    }
                });
            }
        };
        
        t.start();
    }
    
    /**
     * Get the result from constructor invocation.
     */
    public DebuggerResult getResult()
    {
        return result;
    }
}
