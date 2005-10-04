package greenfoot.event;

import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import rmiextension.wrappers.event.RCompileEvent;
import rmiextension.wrappers.event.RCompileListenerImpl;

/**
 * Class that forwards compile events to all the compile listeners registred
 * with greenfoot. This is for performance reasons. Many objects are intereseted
 * in compile events, and if all these should use remote listenerers it might be
 * to heavy. Consider using non remote compile events as well.
 * 
 * @author Poul Henriksen
 * @version $Id: CompileListenerForwarder.java,v 1.4 2004/11/18 09:43:52 polle
 *          Exp $
 */
public class CompileListenerForwarder extends RCompileListenerImpl
{
    private transient final static Logger logger = Logger.getLogger("greenfoot");

    private List compileListeners;

    public CompileListenerForwarder(List compileListeners)
        throws RemoteException
    {
        this.compileListeners = compileListeners;
    }

    public void compileStarted(RCompileEvent event)
        throws RemoteException
    {
        logger.info("compileStarted");
        for (Iterator iter = compileListeners.iterator(); iter.hasNext();) {
            CompileListener element = (CompileListener) iter.next();
            element.compileStarted(event);
        }
    }

    public void compileError(RCompileEvent event)
        throws RemoteException
    {
        logger.info("compileError");
        for (Iterator iter = compileListeners.iterator(); iter.hasNext();) {
            CompileListener element = (CompileListener) iter.next();
            element.compileError(event);
        }
    }

    public void compileWarning(RCompileEvent event)
        throws RemoteException
    {
        logger.info("compileWarning");
        for (Iterator iter = compileListeners.iterator(); iter.hasNext();) {
            CompileListener element = (CompileListener) iter.next();
            element.compileWarning(event);
        }
    }

    public void compileSucceeded(RCompileEvent event)
        throws RemoteException
    {
        logger.info("compileSucceeded");
        CompileListener[] listenersCopy = new CompileListener[compileListeners.size()];
        listenersCopy = (CompileListener[]) compileListeners.toArray(listenersCopy);
        for (int i = 0; i < listenersCopy.length; i++) {
            CompileListener listener = listenersCopy[i];
            listener.compileSucceeded(event);
        }
    }

    public void compileFailed(RCompileEvent event)
        throws RemoteException
    {
        logger.info("compileFailed");
        for (Iterator iter = compileListeners.iterator(); iter.hasNext();) {
            CompileListener element = (CompileListener) iter.next();
            element.compileFailed(event);
        }
    }

}