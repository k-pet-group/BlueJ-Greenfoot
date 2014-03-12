/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010,2011,2012,2014  Poul Henriksen and Michael Kolling 
 
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
import greenfoot.record.InteractionListener;

import java.awt.event.MouseEvent;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
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
    private InteractionListener interactionListener;
    
    /**
     * Create a WorldInvokeListener for listening to method invocations on an object.
     */
    public WorldInvokeListener(JFrame frame, Object obj, ObjectBenchInterface bench,
            InspectorManager inspectorManager, InteractionListener interactionListener, GProject project)
    {
        this.objectBench = bench;
        this.obj = obj;
        this.inspectorManager = inspectorManager;
        this.interactionListener = interactionListener;
        this.project = project;
        this.frame = frame;
    }
    
    /**
     * Create a WorldInvokeListener for listening to static method and constructor
     * invocations.
     */
    public WorldInvokeListener(JFrame frame, Class<?> cl, ObjectBenchInterface bench,
            InspectorManager inspectorManager, InteractionListener interactionListener, GProject project)
    {
        this.objectBench = bench;
        this.cl = cl;
        this.project = project;
        this.inspectorManager = inspectorManager;
        this.interactionListener = interactionListener;
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
                @Override
                public void beginCompile()
                {
                }
                
                @Override
                public void beginExecution(InvokerRecord ir)
                {
                    interactionListener.beginCallExecution(callable);
                    WorldHandler.getInstance().clearWorldSet();
                }
                
                @Override
                public void putError(String message, InvokerRecord ir)
                {
                }
                
                @Override
                public void putException(ExceptionDescription exception,
                        InvokerRecord ir)
                {
                }
                
                @Override
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
                                interactionListener.createdActor(o, ir.getArgumentValues(), paramTypes);
                                worldHandler.addObjectAtEvent((Actor) o, event);
                                worldHandler.repaint();
                            }
                            else if(o instanceof greenfoot.World) {
                                interactionListener.worldConstructed(o);
                                if (! worldHandler.checkWorldSet()) {
                                    ImageCache.getInstance().clearImageCache();
                                    worldHandler.setWorld((World) o);
                                }
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
                        interactionListener.methodCall(obj, instanceName, m.getMethod(), ir.getArgumentValues(), paramTypes);
                    }
                }

                @Override
                public void putVMTerminated(InvokerRecord ir)
                {
                    // What, the VM was terminated? *this* VM? then how am I still executing...?
                }
            };
            InvokerCompiler compiler = project.getDefaultPackage().getCompiler();
            
            // Find project character set
            String csName = project.getProjectProperties().getString("project.charset");
            if (csName == null) {
                csName = "UTF-8";
            }
            Charset cs;
            try {
                cs = Charset.forName(csName);
            }
            catch (IllegalCharsetNameException icsne) {
                cs = Charset.forName("UTF-8");
            }
            
            return new Invoker(frame, callable, watcher, project.getDir(), "", project.getDir().getPath(),
                    ch, objectBenchVars, objectBench, debugger, compiler, instanceName, cs);
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
    @Override
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
    @Override
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
