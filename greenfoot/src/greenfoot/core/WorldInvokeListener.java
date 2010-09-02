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

import greenfoot.Actor;
import greenfoot.ObjectTracker;
import greenfoot.World;
import greenfoot.gui.input.mouse.LocationTracker;
import greenfoot.localdebugger.LocalDebugger;
import greenfoot.localdebugger.LocalObject;

import java.awt.event.MouseEvent;
import java.rmi.RemoteException;

import javax.swing.JFrame;

import rmiextension.wrappers.RObject;
import bluej.debugger.Debugger;
import bluej.debugger.DebuggerObject;
import bluej.debugger.ExceptionDescription;
import bluej.debugger.gentype.JavaType;
import bluej.debugmgr.CallHistory;
import bluej.debugmgr.ExpressionInformation;
import bluej.debugmgr.Invoker;
import bluej.debugmgr.InvokerCompiler;
import bluej.debugmgr.ResultWatcher;
import bluej.debugmgr.ValueCollection;
import bluej.debugmgr.inspector.InspectorManager;
import bluej.debugmgr.inspector.ResultInspector;
import bluej.debugmgr.objectbench.InvokeListener;
import bluej.debugmgr.objectbench.ObjectBenchInterface;
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
    implements InvokeListener
{
    /** The object on which we are listening. Null if we are listening on a class. */
    private Object obj;
    private RObject rObj;
    private Class<?> cl;
    private InspectorManager inspectorManager;
    private ObjectBenchInterface objectBench;
    private GProject project;
    private JFrame frame;
    
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
            catch (RemoteException e) {
                Debug.reportError("Error getting remote object", e);
                return null;
            }
        }

        ValueCollection objectBenchVars = ObjectTracker.getObjects();
        Debugger debugger = new LocalDebugger();
        CallHistory ch = GreenfootMain.getInstance().getCallHistory();
        final MouseEvent event = LocationTracker.instance().getMouseButtonEvent();
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
                	JavaType[] paramTypes = callable.getParamTypes(false);
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
                            WorldHandler worldHandler = WorldHandler.getInstance();
                            if(o instanceof Actor) {
                                worldHandler.notifyCreatedActor(o, ir.getArgumentValues(), paramTypes);
                                worldHandler.addObjectAtEvent((Actor) o, event);
                                worldHandler.repaint();
                            }
                            else if(o instanceof greenfoot.World) {
                                worldHandler.setWorld((World) o);
                            }
                            else {
                                inspectorManager.getInspectorInstance(result, "result", null, null,
                                        GreenfootMain.getInstance().getFrame());
                            }
                        }
                    }

                    update();
                    if (callable instanceof MethodView) {
                        MethodView m = (MethodView) callable;
                        WorldHandler.getInstance().notifyMethodCall(obj, instanceName, m.getName(), ir.getArgumentValues(), paramTypes);
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
    }

    /**
     * Interactively call a method. If necessary, a dialog is presented to ask for
     * parameters; then the object is constructed.
     */
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
        Invoker invoker = getInvokerInstance(cv);
        invoker.invokeInteractive();
    }

    private void update()
    {
        WorldHandler worldHandler = WorldHandler.getInstance();
        if(worldHandler != null) {
            worldHandler.repaint();
        }
    }
}
