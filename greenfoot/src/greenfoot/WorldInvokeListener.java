package greenfoot;

import greenfoot.localdebugger.LocalObject;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.RemoteException;

import javax.swing.JFrame;

import rmiextension.ObjectTracker;
import rmiextension.wrappers.RObject;
import bluej.debugmgr.CallDialog;
import bluej.debugmgr.CallDialogWatcher;
import bluej.debugmgr.CallHistory;
import bluej.debugmgr.ExpressionInformation;
import bluej.debugmgr.MethodDialog;
import bluej.debugmgr.inspector.ResultInspector;
import bluej.debugmgr.objectbench.InvokeListener;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;
import bluej.views.MethodView;

/**
 * A listener for method invocations
 * 
 * @author Davin McCall
 * @version $Id: WorldInvokeListener.java 3227 2004-12-08 04:04:58Z davmac $
 */
public class WorldInvokeListener
    implements InvokeListener, CallDialogWatcher
{
    private GreenfootObject obj;
    private RObject rObj;
    private MethodView mv;
    
    public WorldInvokeListener(GreenfootObject obj)
    {
        this.obj = obj;
    }
    
    /* (non-Javadoc)
     * @see bluej.debugmgr.objectbench.InvokeListener#executeMethod(bluej.views.MethodView)
     */
    public void executeMethod(MethodView mv)
    {
        this.mv = mv;
        
        try {
            rObj = ObjectTracker.instance().getRObject(obj);
            final String instanceName = rObj.getInstanceName();
            
            if (mv.getParameterCount() == 0) {
                final Method m = mv.getMethod();
                new Thread() {
                    public void run() {
                        try {
                            Object r = m.invoke(obj, null);
                            if (m.getReturnType() != void.class) {
                                ExpressionInformation ei = new ExpressionInformation(WorldInvokeListener.this.mv, instanceName);
                                // TODO more robust method to find greenfoot frame
                                ResultInspector ri = ResultInspector.getInstance(wrapResult(r, m.getReturnType()), instanceName, null, null, ei, (JFrame) JFrame.getFrames()[0]);
                                ri.show();
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
                CallHistory ch = Greenfoot.getInstance().getCallHistory();
                // TODO more robust method to find greenfoot frame
                MethodDialog md = new MethodDialog((JFrame) JFrame.getFrames()[0], null, ch, instanceName, mv, null);
                md.setWatcher(this);
                md.show();
                
                // The dialog will be Ok'd or cancelled, this
                // WorldInvokeListener instance is notified via the
                // callDialogEvent method.
            }
        }
        catch (RemoteException re) {}
    }
    
    public void callDialogEvent(CallDialog dlg, int event)
    {
        if (event == CallDialog.CANCEL) {
            dlg.setVisible(false);
        }
        else if (event == CallDialog.OK) {
            MethodDialog mdlg = (MethodDialog) dlg;
            mdlg.setEnabled(false);
            RObject rObj = ObjectTracker.instance().getRObject(obj);

            Class [] cparams = mv.getParameters();
            String [] params = new String[cparams.length];
            for (int i = 0; i < params.length; i++) {
                params[i] = cparams[i].getName();
            }
            
            try {
                String resultName = rObj.invokeMethod(mv.getName(), params, mdlg.getArgs());
                
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
                        
                        String instanceName = rObj.getInstanceName();
                        ExpressionInformation ei = new ExpressionInformation(mv, instanceName);

                        try {
                            RObject rresult = Greenfoot.getInstance().getPackage().getObject(resultName);
                            Object resultw = ObjectTracker.instance().getRealObject(rresult);
                            rresult.removeFromBench();
                            
                            Field rfield = resultw.getClass().getField("result");
                            rfield.setAccessible(true);
                            Object result = rfield.get(resultw);
                            
                            // TODO more robust method to find greenfoot frame
                            ResultInspector ri = ResultInspector.getInstance(wrapResult(result, m.getReturnType()), instanceName, null, null, ei, (JFrame) JFrame.getFrames()[0]);
                            ri.show();
                        }
                        catch (PackageNotFoundException pnfe) {}
                        catch (ProjectNotOpenException pnoe) {}
                        catch (NoSuchFieldException nsfe) {}
                        catch (IllegalAccessException iae) {}
                    }
                }
            }
            catch (RemoteException re) {
                // shouldn't happen.
                re.printStackTrace(System.out);
            }
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
