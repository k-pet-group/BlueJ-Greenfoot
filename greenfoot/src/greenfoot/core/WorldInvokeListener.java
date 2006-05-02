package greenfoot.core;

import greenfoot.ObjectTracker;
import greenfoot.event.ActorInstantiationListener;
import greenfoot.localdebugger.LocalObject;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.RemoteException;

import rmiextension.wrappers.RObject;
import bluej.debugmgr.CallDialog;
import bluej.debugmgr.CallDialogWatcher;
import bluej.debugmgr.CallHistory;
import bluej.debugmgr.ExpressionInformation;
import bluej.debugmgr.MethodDialog;
import bluej.debugmgr.inspector.InspectorManager;
import bluej.debugmgr.inspector.ResultInspector;
import bluej.debugmgr.objectbench.InvokeListener;
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
    private WorldHandler worldHandler;
    private InspectorManager inspectorManager;
    
    public WorldInvokeListener(Object obj, WorldHandler worldHandler, InspectorManager inspectorManager)
    {
        this.worldHandler = worldHandler;
        this.obj = obj;
        this.inspectorManager = inspectorManager;
    }
    
    public WorldInvokeListener(Class cl)
    {
        this.cl = cl;
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
                            Object r = m.invoke(obj, (Object[])null);
                            update();
                            if (m.getReturnType() != void.class) {
                                ExpressionInformation ei = new ExpressionInformation(WorldInvokeListener.this.mv, instanceName);
                                ResultInspector ri = inspectorManager.getResultInspectorInstance(wrapResult(r, m.getReturnType()), instanceName, null, null, ei,  GreenfootMain.getInstance().getFrame());
                                ri.setVisible(true);
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
                MethodDialog md = new MethodDialog(GreenfootMain.getInstance().getFrame(), null, ch, instanceName, mv, null);
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
                        invocListener.localObjectCreated(o);
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
            CallHistory ch = GreenfootMain.getInstance().getCallHistory();
            MethodDialog md = new MethodDialog(GreenfootMain.getInstance().getFrame(), null, ch, "result", cv, null);
            md.setWatcher(this);
            md.setVisible(true);
            
            // The dialog will be Ok'd or cancelled, this
            // WorldInvokeListener instance is notified via the
            // callDialogEvent method.
        }
    }
    
    public void callDialogEvent(CallDialog dlg, int event)
    {
        if (event == CallDialog.CANCEL) {
            dlg.setVisible(false);
        }
        else if (event == CallDialog.OK) {
            MethodDialog mdlg = (MethodDialog) dlg;
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

            CallableView callv = mv == null ? (CallableView)cv : mv;
            Class [] cparams = callv.getParameters();
            String [] params = new String[cparams.length];
            for (int i = 0; i < params.length; i++) {
                params[i] = cparams[i].getName();
            }

            GPackage pkg = null;
            try {
                pkg = GreenfootMain.getInstance().getProject().getDefaultPackage();
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
            
            if (mv != null) {
                // method call
                try {
                    String resultName;
                    if (rObj != null)
                        resultName = rObj.invokeMethod(mv.getName(), params, mdlg.getArgs());
                    else
                        resultName = pkg.invokeMethod(cl.getName(), mv.getName(), params, mdlg.getArgs());
                    
                    // error is indicated by result beginning with "!"
                    if (resultName != null && resultName.charAt(0) == '!') {
                        String errorMsg = resultName.substring(1);
                        mdlg.setErrorMessage(errorMsg);
                        mdlg.setEnabled(true);
                    }
                    else {
                        mdlg.dispose();
                        Method m = mv.getMethod();
                        if (m.getReturnType() != void.class) {
                            // Non-void result, display it in a result inspector.
                            
                            String instanceName;
                            if (rObj != null)
                                instanceName = rObj.getInstanceName();
                            else
                                instanceName = cl.getName();
                            ExpressionInformation ei = new ExpressionInformation(mv, instanceName);
                            
                            try {
                                RObject rresult = pkg.getObject(resultName);
                                Object resultw =  ObjectTracker.getRealObject(rresult);
                                rresult.removeFromBench();
                                
                                ResultInspector ri = inspectorManager.getResultInspectorInstance(new LocalObject(resultw), instanceName, null, null, ei, GreenfootMain.getInstance().getFrame());
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
                        String errorMsg = resultName.substring(1);
                        mdlg.setErrorMessage(errorMsg);
                        mdlg.setEnabled(true);
                    }
                    else {
                        // Construction went ok (or there was a runtime error).
                        mdlg.dispose();
                        if (resultName != null) {
                            RObject rresult = pkg.getObject(resultName);
                            Object resultw =  ObjectTracker.getRealObject(rresult);
                            rresult.removeFromBench();
                            ActorInstantiationListener invocListener = GreenfootMain.getInstance().getInvocationListener();
                            invocListener.localObjectCreated(resultw);
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
    }

    private void update()
    {
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
                boolean result = ((Boolean) r).booleanValue();
            };
        }
        else if (c == byte.class) {
            wrapped = new Object() {
                byte result = ((Byte) r).byteValue();
            };
        }
        else if (c == char.class) {
            wrapped = new Object() {
                char result = ((Character) r).charValue();
            };
        }
        else if (c == short.class) {
            wrapped = new Object() {
                short result = ((Short) r).shortValue();
            };
        }
        else if (c == int.class) {
            wrapped = new Object() {
                int result = ((Integer) r).intValue();
            };
        }
        else if (c == long.class) {
            wrapped = new Object() {
                long result = ((Long) r).longValue();
            };
        }
        else if (c == float.class) {
            wrapped = new Object() {
                float result = ((Float) r).floatValue();
            };
        }
        else if (c == double.class) {
            wrapped = new Object() {
                double result = ((Double) r).doubleValue();
            };
        }
        else {
            wrapped = new Object() {
                Object result = r;
            };
        }
        return new LocalObject(wrapped);
    }
}
