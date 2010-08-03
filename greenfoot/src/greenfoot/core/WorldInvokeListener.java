/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010  Poul Henriksen and Michael Kolling 
 
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
package greenfoot.core;

import greenfoot.ObjectTracker;
import greenfoot.event.ActorInstantiationListener;
import greenfoot.gui.input.mouse.LocationTracker;
import greenfoot.localdebugger.LocalDebugger;
import greenfoot.localdebugger.LocalObject;

import java.awt.event.MouseEvent;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import rmiextension.wrappers.RObject;
import bluej.debugger.Debugger;
import bluej.debugger.DebuggerObject;
import bluej.debugger.ExceptionDescription;
import bluej.debugmgr.CallDialog;
import bluej.debugmgr.CallDialogWatcher;
import bluej.debugmgr.CallHistory;
import bluej.debugmgr.ConstructorDialog;
import bluej.debugmgr.ExpressionInformation;
import bluej.debugmgr.Invoker;
import bluej.debugmgr.InvokerCompiler;
import bluej.debugmgr.ResultWatcher;
import bluej.debugmgr.ValueCollection;
import bluej.debugmgr.inspector.InspectorManager;
import bluej.debugmgr.inspector.ResultInspector;
import bluej.debugmgr.objectbench.InvokeListener;
import bluej.debugmgr.objectbench.ObjectBenchInterface;
import bluej.extensions.ClassNotFoundException;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;
import bluej.testmgr.record.InvokerRecord;
import bluej.utility.Debug;
import bluej.views.CallableView;
import bluej.views.ConstructorView;
import bluej.views.MethodView;

/**
 * A listener for interactive method/constructor invocations.
 * 
 * <p>Invocations occur on either an object (instance methods) or a class
 * (static methods and constructors).
 * 
 * <p>A method call dialog is displayed, if necessary, to ask for parameters, and 
 * an inspector window for the result (method call). For constructed objects,
 * the greenfoot object instantiation listener is notified.
 * 
 * <p>When a method call has been executed the world is repainted.
 * 
 * @author Davin McCall
 */
public class WorldInvokeListener
    implements InvokeListener, CallDialogWatcher
{
    /** The object on which we are listening. Null if we are listening on a class. */
    private Object obj;
    private RObject rObj;
    private MethodView mv;
    private ConstructorView cv;
    private Class<?> cl;
    private InspectorManager inspectorManager;
    private ObjectBenchInterface objectBench;
    private GProject project;
    private JFrame frame;
    
    /** A map which tells us which construction location applies to each dialog */
    private Map<CallDialog,MouseEvent> dialogToLocationMap =
        new HashMap<CallDialog,MouseEvent>();
    
    /**
     * Create a WorldInvokeListener for listening to method invocations on an object.
     */
    public WorldInvokeListener(JFrame frame, Object obj, ObjectBenchInterface bench,
            InspectorManager inspectorManager, GProject project)
    {
        this.objectBench = bench;
        this.obj = obj;
        this.inspectorManager = inspectorManager;
        this.project = project;
        this.frame = frame;
    }
    
    /**
     * Create a WorldInvokeListener for listening to static method and constructor
     * invocations.
     */
    public WorldInvokeListener(JFrame frame, Class<?> cl, ObjectBenchInterface bench, InspectorManager inspectorManager, GProject project)
    {
        this.objectBench = bench;
        this.cl = cl;
        this.project = project;
        this.inspectorManager = inspectorManager;
        this.frame = frame;
    }
    
    /**
     * Get an invoker instance to deal with invocation of the given method or
     * constructor.
     */
    private Invoker getInvokerInstance(final CallableView callable)
    {
        if (obj != null) {
            try {
                rObj = ObjectTracker.getRObject(obj);
            }
            catch (ProjectNotOpenException e) {
                return null;
            }
            catch (PackageNotFoundException e) {
                return null;
            }
            catch (RemoteException e) {
                Debug.reportError("Error getting remote object", e);
                return null;
            }
            catch (ClassNotFoundException e) {
                Debug.reportError("Error getting remote object", e);
                return null;
            }
        }

        ValueCollection objectBenchVars = ObjectTracker.getObjects();
        Debugger debugger = new LocalDebugger(); // DAV probably can use a single static instance
        CallHistory ch = GreenfootMain.getInstance().getCallHistory();
        try {
            final String instanceName = rObj != null ? rObj.getInstanceName() : cl.getName();
            ResultWatcher watcher = new ResultWatcher() {
                public void beginCompile()
                {
                }
                
                public void beginExecution(InvokerRecord ir)
                {
                }
                
                public void putError(String message, InvokerRecord ir)
                {
                }
                
                public void putException(ExceptionDescription exception,
                        InvokerRecord ir)
                {
                }
                
                public void putResult(DebuggerObject result, String name,
                        InvokerRecord ir)
                {
                    if (result instanceof LocalObject) {
                        Object o = ((LocalObject) result).getObject();

                        if (callable instanceof MethodView) {
                            MethodView mv = (MethodView) callable;
                            if (! mv.isVoid()) {
                                // Display a result inspector for the method result
                                ExpressionInformation ei = new ExpressionInformation((MethodView) callable, instanceName);
                                ei.setArgumentValues(ir.getArgumentValues());
                                ResultInspector ri = inspectorManager.getResultInspectorInstance(result,
                                        instanceName, null, null, ei, GreenfootMain
                                        .getInstance().getFrame());
                                ri.setVisible(true);
                            }
                        }
                        else {
                            // DAV constructor for non-actor/world?
                            WorldHandler.getInstance().notifyCreatedActor(o, new String[0]);
                            ActorInstantiationListener invocListener = GreenfootMain.getInstance().getInvocationListener();
                            invocListener.localObjectCreated(o, LocationTracker.instance().getMouseButtonEvent());
                        }
                    }

                    update();
                    if (callable instanceof MethodView) {
                        MethodView m = (MethodView) callable;
                        WorldHandler.getInstance().notifyMethodCall(obj, instanceName, m.getName(), new String[0]);
                    }
                }

                public void putVMTerminated(InvokerRecord ir)
                {
                    // What, the VM was terminated? *this* VM? then how am I still executing...?
                }
            };
            InvokerCompiler compiler = project.getDefaultPackage().getCompiler();
            return new Invoker(frame, callable, watcher, project.getDir(), "", project.getDir().getPath(),
                    ch, objectBenchVars, objectBench, debugger, compiler, instanceName);
        }
        catch (RemoteException re) {
            Debug.reportError("Error getting invoker instance", re);
            return null;
        }
        catch (ProjectNotOpenException pnoe) {
            return null; // should never happen
        }
    }

    public void executeMethod(MethodView mv)
    {
        Invoker invoker = getInvokerInstance(mv);
        invoker.invokeInteractive();
    }

    /**
     * Interactively call a constructor. If necessary, a dialog is presented to ask for
     * parameters; then the object is constructed. The ActorInstantiationListener is
     * notified if an object is created successfully; for an actor, this will result in
     * the actor being inserted into the world.
     */
    public void callConstructor(ConstructorView cv)
    {
        this.cv = cv;
        
        if (cv.getParameterCount() == 0) {
            // No parameters to ask for, so there's no need to pop up a dialog
            // or compile a shell file
            Simulation.getInstance().runLater(new Runnable() {
                public void run() {
                    try {
                        final Constructor<?> c = cl.getDeclaredConstructor(new Class[0]);
                        c.setAccessible(true);
                        Object o = c.newInstance((Object[]) null);
                        WorldHandler.getInstance().notifyCreatedActor(o, new String[0]);
                        ActorInstantiationListener invocListener = GreenfootMain.getInstance().getInvocationListener();
                        invocListener.localObjectCreated(o, LocationTracker.instance().getMouseButtonEvent());
                    }
                    catch (NoSuchMethodException nsme) {
                        Debug.reportError("Invoking constructor", nsme);
                    }
                    catch (IllegalAccessException iae) {
                        Debug.reportError("Invoking constructor", iae);
                    }
                    catch (InstantiationException ie) {
                        Debug.reportError("Invoking constructor", ie);
                    }
                    catch (InvocationTargetException ite) {
                        // exception thrown in constructor
                        // TODO highlight the line in the editor
                        ite.getCause().printStackTrace();
                    }
                }
            });
        }
        else {
            // Parameters are required for this call, so we need to use a call dialog.
            CallHistory ch = GreenfootMain.getInstance().getCallHistory();
            ConstructorDialog md = new ConstructorDialog(GreenfootMain.getInstance().getFrame(),
                    objectBench, ch, "result", cv);

            dialogToLocationMap.put(md, LocationTracker.instance().getMouseButtonEvent());
            
            md.setWatcher(this);
            md.setVisible(true);
            
            // The dialog will be Ok'd or cancelled, this
            // WorldInvokeListener instance is notified via the
            // callDialogEvent method.
        }
    }
    
    /**
     * Callback method that will be called when Ok or Cancel has been pressed in
     * a method call dialog. This method will execute the method call on a
     * different thread.
     */
    public void callDialogEvent(CallDialog dlg, int event)
    {
        if (event == CallDialog.CANCEL) {
            dlg.setVisible(false);
            dlg.dispose(); // must dispose to prevent leaks
            dialogToLocationMap.remove(dlg);
        }
        else if (event == CallDialog.OK) {
            dlg.setWaitCursor(true);
            dlg.setEnabled(false);
            RObject rObj = null;
            try {
                if(obj != null) {
                    rObj = ObjectTracker.getRObject(obj);
                }
                
                GPackage pkg = project.getDefaultPackage();
                
                if(pkg == null) {
                    return;
                }      
                
                executeMethod(dlg, rObj, pkg);
            }
            catch (ProjectNotOpenException e1) {
                Debug.reportError("Error invoking interative method", e1);
            }
            catch (PackageNotFoundException e1) {
                Debug.reportError("Error invoking interative method", e1);
            }
            catch (RemoteException e1) {
                Debug.reportError("Error invoking interative method", e1);
            }
            catch (ClassNotFoundException e1) {
                Debug.reportError("Error invoking interative method", e1);
            }
        }
    }

    /**
     * Execute a method or constructor (async) that was invoked via the given
     * method dialog on the given object in the package pkg.
     * 
     * TODO: The methods should maybe be invoked via reflection instead of
     * through the BlueJ extensions. Using the BlueJ extension makes it
     * impossible to do a compile until the execution has finished. If Compile
     * All is pressed in Greenfoot while a method is being invoked, an error
     * will appear and the button will be disabled.
     * 
     * @param mdlg MethodDialog used for the invocation.
     * @param rObj Object to invoke method on
     * @param pkg Package used for invocation
     */
    private void executeMethod(final CallDialog mdlg, final RObject rObj, final GPackage pkg)
    {
        CallableView callv = mv == null ? (CallableView)cv : mv;
        Class<?> [] cparams = callv.getParameters();
        final String [] params = new String[cparams.length];
        for (int i = 0; i < params.length; i++) {
            params[i] = cparams[i].getName();
        }

        Runnable r = new Runnable() {
            public void run()
            {
                if (mv != null) {
                    // method call
                    try {
                        // Hide the method dialog so the execution can be seen.
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run()
                            {
                                mdlg.setVisible(false);
                            }
                        });
                        
                        String resultName;
                        if (rObj != null)
                            resultName = rObj.invokeMethod(mv.getName(), params, mdlg.getArgs());
                        else
                            resultName = pkg.invokeMethod(cl.getName(), mv.getName(), params, mdlg.getArgs());

                        // error is indicated by result beginning with "!"
                        if (resultName != null && resultName.charAt(0) == '!') {
                            final String errorMsg = resultName.substring(1);
                            
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run()
                                {
                                    mdlg.setErrorMessage(errorMsg);
                                    mdlg.setWaitCursor(false);
                                    mdlg.setEnabled(true);
                                    // relayout and display the dialog again.
                                    mdlg.pack();
                                    mdlg.setVisible(true);
                                }
                            });
                        }
                        else {
                            if (rObj != null)
                                WorldHandler.getInstance().notifyMethodCall(ObjectTracker.getRealObject(rObj), rObj.getInstanceName(), mv.getName(), mdlg.getArgs());
                            else
                                WorldHandler.getInstance().notifyStaticMethodCall(cl.getName(), mv.getName(), mdlg.getArgs());
                            
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run()
                                {
                                    mdlg.dispose(); // dispose to prevent leaks
                                }
                            });
                            Method m = mv.getMethod();
                            if (m.getReturnType() != void.class) {
                                // Non-void result, display it in a result
                                // inspector.

                                String instanceName;
                                if (rObj != null)
                                    instanceName = rObj.getInstanceName();
                                else
                                    instanceName = cl.getName();
                                ExpressionInformation ei = new ExpressionInformation(mv, instanceName);

                                try {
                                    RObject rresult = pkg.getObject(resultName);
                                    Object resultw = ObjectTracker.getRealObject(rresult);
                                    rresult.removeFromBench();

                                    ResultInspector ri = inspectorManager.getResultInspectorInstance(LocalObject
                                            .getLocalObject(resultw), instanceName, null, null, ei, GreenfootMain
                                            .getInstance().getFrame());
                                    ri.setVisible(true);
                                }
                                catch (PackageNotFoundException pnfe) {}
                                catch (ProjectNotOpenException pnoe) {}
                            }
                        }
                    }
                    catch (RemoteException re) {
                        // shouldn't happen.
                        re.printStackTrace(System.out);
                    }
                }
                else if (cv != null) {
                    // Constructor call

                    try {
                        String resultName = pkg.invokeConstructor(cl.getName(), params, mdlg.getArgs());
                        if (resultName != null && resultName.charAt(0) == '!') {
                            final String errorMsg = resultName.substring(1);

                            SwingUtilities.invokeLater(new Runnable() {
                                public void run()
                                {
                                    mdlg.setErrorMessage(errorMsg);
                                    mdlg.setWaitCursor(false);
                                    mdlg.setEnabled(true);
                                }
                            });
                        }
                        else {
                            // Construction went ok (or there was a runtime
                            // error).
                            mdlg.setVisible(false);
                            MouseEvent location = dialogToLocationMap.remove(mdlg);
                            if (resultName != null) {
                                RObject rresult = pkg.getObject(resultName);
                                Object resultw = ObjectTracker.getRealObject(rresult);
                                rresult.removeFromBench();
                                WorldHandler.getInstance().notifyCreatedActor(resultw, mdlg.getArgs());
                                ActorInstantiationListener invocListener = GreenfootMain.getInstance()
                                        .getInvocationListener();
                                invocListener.localObjectCreated(resultw, location);
                            }
                        }
                    }
                    catch (RemoteException re) {
                        re.printStackTrace();
                    }
                    catch (ProjectNotOpenException pnoe) {}
                    catch (PackageNotFoundException pnfe) {}
                }
                update();
            }
        };
        Simulation.getInstance().runLater(r);
    }

    private void update()
    {
        WorldHandler worldHandler = WorldHandler.getInstance();
        if(worldHandler != null) {
            worldHandler.repaint();
        }
    }
}
