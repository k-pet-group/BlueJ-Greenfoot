package rmiextension.wrappers.event;

import java.rmi.RemoteException;

import bluej.extensions.event.CompileEvent;
import bluej.extensions.event.CompileListener;

/**
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: RCompileListenerWrapper.java,v 1.3 2004/11/18 09:43:50 polle
 *          Exp $
 */
public class RCompileListenerWrapper
    implements CompileListener
{

    private RCompileListener remoteListener;

    public RCompileListenerWrapper(RCompileListener remoteListener)
    {
        this.remoteListener = remoteListener;
    }

    /*
     * (non-Javadoc)
     * 
     * @see bluej.extensions.event.CompileListener#compileStarted(bluej.extensions.event.CompileEvent)
     */
    public void compileStarted(CompileEvent event)
    {
        try {
            RCompileEvent rEvent = new RCompileEventImpl(event);
            remoteListener.compileStarted(rEvent);
        }
        catch (RemoteException e1) {
            e1.printStackTrace();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see bluej.extensions.event.CompileListener#compileError(bluej.extensions.event.CompileEvent)
     */
    public void compileError(CompileEvent event)
    {
        try {
            RCompileEvent rEvent = new RCompileEventImpl(event);
            remoteListener.compileError(rEvent);
        }
        catch (RemoteException e1) {
            e1.printStackTrace();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see bluej.extensions.event.CompileListener#compileWarning(bluej.extensions.event.CompileEvent)
     */
    public void compileWarning(CompileEvent event)
    {
        try {
            RCompileEvent rEvent = new RCompileEventImpl(event);
            remoteListener.compileWarning(rEvent);
        }
        catch (RemoteException e1) {
            e1.printStackTrace();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see bluej.extensions.event.CompileListener#compileSucceeded(bluej.extensions.event.CompileEvent)
     */
    public void compileSucceeded(CompileEvent event)
    {
        try {
            RCompileEvent rEvent = new RCompileEventImpl(event);
            remoteListener.compileSucceeded(rEvent);
        }
        catch (RemoteException e1) {
            e1.printStackTrace();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see bluej.extensions.event.CompileListener#compileFailed(bluej.extensions.event.CompileEvent)
     */
    public void compileFailed(CompileEvent event)
    {
        try {
            RCompileEvent rEvent = new RCompileEventImpl(event);
            remoteListener.compileFailed(rEvent);
        }
        catch (RemoteException e1) {
            e1.printStackTrace();
        }
    }
}