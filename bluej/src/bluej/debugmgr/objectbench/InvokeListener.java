package bluej.debugmgr.objectbench;

import bluej.views.ConstructorView;
import bluej.views.MethodView;

/**
 * Listener interface for some object to be notified when a method is to be
 * interactively invoked.
 * 
 * @author Davin McCall
 * @version $Id: InvokeListener.java 3519 2005-08-13 18:01:44Z polle $
 */
public interface InvokeListener
{
    void executeMethod(MethodView mv);
    
    void callConstructor(ConstructorView cv);
}
