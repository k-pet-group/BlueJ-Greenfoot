package rmiextension.wrappers.event;

import java.io.File;
import java.rmi.RemoteException;

import bluej.extensions.event.CompileEvent;

/**
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: RCompileEventImpl.java 3124 2004-11-18 16:08:48Z polle $
 */
public class RCompileEventImpl extends java.rmi.server.UnicastRemoteObject
    implements RCompileEvent
{
    private CompileEvent compileEvent;

    /**
     * @param event
     */
    public RCompileEventImpl(CompileEvent event)
        throws RemoteException
    {
        super();
        this.compileEvent = event;
    }

    /**
     * @return
     */
    public int getErrorLineNumber()
    {
        return compileEvent.getErrorLineNumber();
    }

    /**
     * @return
     */
    public String getErrorMessage()
    {
        return compileEvent.getErrorMessage();
    }

    /**
     * @return
     */
    public int getEvent()
    {
        return compileEvent.getEvent();
    }

    /**
     * @return
     */
    public File[] getFiles()
    {
        return compileEvent.getFiles();
    }

    /**
     * @param aLineNumber
     */
    public void setErrorLineNumber(int aLineNumber)
    {
        compileEvent.setErrorLineNumber(aLineNumber);
    }

    /**
     * @param anErrorMessage
     */
    public void setErrorMessage(String anErrorMessage)
    {
        compileEvent.setErrorMessage(anErrorMessage);
    }
}