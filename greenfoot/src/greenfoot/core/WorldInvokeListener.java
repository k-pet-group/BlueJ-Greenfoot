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
package greenfoot.core;

import greenfoot.ObjectTracker;
import greenfoot.event.ActorInstantiationListener;
import greenfoot.gui.GreenfootMethodDialog;
import greenfoot.gui.input.mouse.LocationTracker;
import greenfoot.localdebugger.LocalObject;

import java.awt.EventQueue;
import java.awt.event.MouseEvent;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.SwingUtilities;

import rmiextension.wrappers.RObject;
import bluej.debugmgr.CallDialog;
import bluej.debugmgr.CallDialogWatcher;
import bluej.debugmgr.CallHistory;
import bluej.debugmgr.ExpressionInformation;
import bluej.debugmgr.MethodDialog;
import bluej.debugmgr.inspector.InspectorManager;
import bluej.debugmgr.inspector.ResultInspector;
import bluej.debugmgr.objectbench.InvokeListener;
import bluej.debugmgr.objectbench.ObjectBenchInterface;
import bluej.extensions.ClassNotFoundException;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;
import bluej.views.CallableView;
import bluej.views.ConstructorView;
import bluej.views.MethodView;

/**
 * A listener for interactive method/constructor invocations.
 * 
 * Invocations occur on either an object (instance methods) or a class
 * (static methods and constructors).
 * 
 * A method call dialog is displayed, if necessary, to ask for parameters, and 
 * an inspector window for the result (method call). For constructed objects,
 * the greenfoot object instantiation listener is notified.
 * 
 * When a method call has been executed the world is repainted.
 * 
 * @author Davin McCall
 * @version $Id$
 */
public class WorldInvokeListener
    implements InvokeListener, CallDialogWatcher
{
    //private Actor obj;
    private Object obj;
    private RObject rObj;
    private MethodView mv;
    private ConstructorView cv;
    private Class cl;
    private InspectorManager inspectorManager;
    private ObjectBenchInterface objectBench;
    private GProject project;
    
    /** A map which tells us which construction location applies to each dialog */
    private Map dialogToLocationMap = new HashMap();
    
    public WorldInvokeListener(Object obj, ObjectBenchInterface bench,
            InspectorManager inspectorManager, GProject project)
    {
        this.objectBench = bench;
        this.obj = obj;
        this.inspectorManager = inspectorManager;
        this.project = project;
    }
    
    public WorldInvokeListener(Class cl, ObjectBenchInterface bench, InspectorManager inspectorManager, GProject project)
    {
        this.objectBench = bench;
        this.cl = cl;
        this.project = project;
        this.inspectorManager = inspectorManager;
    }
    
    /* (non-Javadoc)
     * @see bluej.debugmgr.objectbench.InvokeListener#executeMethod(bluej.views.MethodView)
     */
    public void executeMethod(MethodView mv)
    {
        this.mv = mv;
        
        try {
            if (obj != null)
                rObj = ObjectTracker.getRObject(obj);
            final String instanceName = obj != null ? rObj.getInstanceName() : cl.getName();
            
            if (mv.getParameterCount() == 0) {
                final Method m = mv.getMethod();
                new Thread() {
                    public void run() {
                        try {
                            final Object r = m.invoke(obj, (Object[])null);
                            update();
                            if (m.getReturnType() != void.class) {
                                final ExpressionInformation ei = new ExpressionInformation(WorldInvokeListener.this.mv, instanceName);
                                EventQueue.invokeLater(new Runnable() {
                                    public void run()
                                    {
                                        ResultInspector ri = inspectorManager.getResultInspectorInstance(wrapResult(r, m.getReturnType()), instanceName, null, null, ei,  GreenfootMain.getInstance().getFrame());
                                        ri.setVisible(true);
                                    }
                                });
                            }
                        }
                        catch (InvocationTargetException ite) {
                            // TODO highlight the line in the editor
                            ite.getCause().printStackTrace();
                        }
                        catch (IllegalAccessException iae) {
                            // shouldn't happen
                            iae.printStackTrace();
                        }
                    }
                }.start();
            }
            else {
                CallHistory ch = GreenfootMain.getInstance().getCallHistory();
                GreenfootMethodDialog md = new GreenfootMethodDialog(GreenfootMain.getInstance().getFrame(),
                        objectBench , ch, instanceName, mv, null);

                md.setWatcher(this);
                md.setVisible(true);
                
                // The dialog will be Ok'd or cancelled, this
                // WorldInvokeListener instance is notified via the
                // callDialogEvent method.
            }
        }
        catch (RemoteException re) {}
        catch (ProjectNotOpenException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (PackageNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
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
            new Thread() {
                public void run() {
                    try {
                        final Constructor c = cl.getDeclaredConstructor(new Class[0]);
                        c.setAccessible(true);
                        Object o = c.newInstance((Object[]) null);
                        ActorInstantiationListener invocListener = GreenfootMain.getInstance().getInvocationListener();
                        invocListener.localObjectCreated(o, LocationTracker.instance().getMouseButtonEvent());
                    }
                    catch (NoSuchMethodException nsme) {}
                    catch (IllegalAccessException iae) {}
                    catch (InstantiationException ie) {}
                    catch (InvocationTargetException ite) {
                        // exception thrown in constructor
                        // TODO highlight the line in the editor
                        ite.getCause().printStackTrace();
                    }
                }
            }.start();
        }
        else {
            // Parameters are required for this call, so we need to use a call dialog.
            CallHistory ch = GreenfootMain.getInstance().getCallHistory();
            GreenfootMethodDialog md = new GreenfootMethodDialog(GreenfootMain.getInstance().getFrame(),
                    objectBench, ch, "result", cv, null);

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
            MethodDialog mdlg = (MethodDialog) dlg;
            mdlg.setWaitCursor(true);
            mdlg.setEnabled(false);
            RObject rObj = null;
            try {
                if(obj != null) {
                    rObj = ObjectTracker.getRObject(obj);
                }
            }
            catch (ProjectNotOpenException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            catch (PackageNotFoundException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            catch (RemoteException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            catch (ClassNotFoundException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }

            GPackage pkg = null;
            try {
                pkg = project.getDefaultPackage();
            }
            catch (ProjectNotOpenException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            catch (RemoteException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
            if(pkg == null) {
                return;
            }      
            
            executeMethod(mdlg, rObj, pkg);
        }
    }

    /**
     * Execute a method or constructor (async) that was invoked via the given
     * method dialog on the given object in the package pkg.
     * 
     * TODO: The methods should maybe be invoked via reflection instead of
     * through the BlueJ extensions. Using the BlueJ extension makes it
     * impossible to do a compile until the execution has finished. If Compile
     * All is pressed in Greenfoot while a method is being invoked, and error
     * will appear and the button will be disabled.
     * 
     * @param mdlg MethodDialog used for the invocation.
     * @param rObj Object to invoke method on
     * @param pkg Package used for invocation
     */
    private void executeMethod(final MethodDialog mdlg, final RObject rObj, final GPackage pkg)
    {

        CallableView callv = mv == null ? (CallableView)cv : mv;
        Class [] cparams = callv.getParameters();
        final String [] params = new String[cparams.length];
        for (int i = 0; i < params.length; i++) {
            params[i] = cparams[i].getName();
        }

        Thread t = new Thread() {
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
                            MouseEvent location = (MouseEvent) dialogToLocationMap.remove(mdlg);
                            if (resultName != null) {
                                RObject rresult = pkg.getObject(resultName);
                                Object resultw = ObjectTracker.getRealObject(rresult);
                                rresult.removeFromBench();
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
        t.start();
    }

    private void update()
    {
        WorldHandler worldHandler = WorldHandler.getInstance();
        if(worldHandler != null) {
            worldHandler.repaint();
        }
    }
        
    /**
     * Wrap a value, that is the result of a method call, in a form that the
     * ResultInspector can understand.<p>
     * 
     * Also ensure that if the result is a primitive type it is correctly
     * unwrapped.
     * 
     * @param r  The result value
     * @param c  The result type
     * @return   A DebuggerObject which wraps the result
     */
    private static LocalObject wrapResult(final Object r, Class c)
    {
        Object wrapped;
        if (c == boolean.class) {
            wrapped = new Object() {
                public boolean result = ((Boolean) r).booleanValue();
            };
        }
        else if (c == byte.class) {
            wrapped = new Object() {
                public byte result = ((Byte) r).byteValue();
            };
        }
        else if (c == char.class) {
            wrapped = new Object() {
                public char result = ((Character) r).charValue();
            };
        }
        else if (c == short.class) {
            wrapped = new Object() {
                public short result = ((Short) r).shortValue();
            };
        }
        else if (c == int.class) {
            wrapped = new Object() {
                public int result = ((Integer) r).intValue();
            };
        }
        else if (c == long.class) {
            wrapped = new Object() {
                public long result = ((Long) r).longValue();
            };
        }
        else if (c == float.class) {
            wrapped = new Object() {
                public float result = ((Float) r).floatValue();
            };
        }
        else if (c == double.class) {
            wrapped = new Object() {
                public double result = ((Double) r).doubleValue();
            };
        }
        else {
            wrapped = new Object() {
                public Object result = r;
            };
        }
        return LocalObject.getLocalObject(wrapped);
    }
}
