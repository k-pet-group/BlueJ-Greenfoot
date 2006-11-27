package bluej.debugmgr.objectbench;

import bluej.views.ConstructorView;
import bluej.views.MethodView;

/**
 * Listener interface for some object to be notified when a method is to be
 * interactively invoked.
 * 
 * @author Davin McCall
 * @version $Id: InvokeListener.java 4708 2006-11-27 00:47:57Z bquig $
 */
public interface InvokeListener
{
    void executeMethod(MethodView mv);
    
    void callConstructor(ConstructorView cv);
}
