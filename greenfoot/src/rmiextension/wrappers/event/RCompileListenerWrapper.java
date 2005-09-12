package rmiextension.wrappers.event;

import java.io.File;
import java.rmi.RemoteException;

import rmiextension.wrappers.RProject;

import bluej.extensions.BClass;
import bluej.extensions.BPackage;
import bluej.extensions.BProject;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;
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
    private BProject project;

    public RCompileListenerWrapper(RCompileListener remoteListener, BProject project)
    {
        this.remoteListener = remoteListener;
        this.project = project;
    }

    /**
     * Returns true if the files are in the project associated with this listener.
     */
    private boolean isInThisProject(File[] files)
    {
        if(files.length <= 0) {
            //Can't think of a reason why this should happen, but according to extensions API it can.
            return true;
        }
        File file = files[0];
        try {
            BPackage[] packages = project.getPackages();
            for (int i = 0; i < packages.length; i++) {
                BPackage pkg = packages[i];
                BClass[] classes = pkg.getClasses();
                for (int j = 0; j < classes.length; j++) {
                    BClass cls = classes[j];
                    if(cls.getJavaFile().equals(file)) {
                        return true;
                    }
                }
            }
        }
        catch (ProjectNotOpenException e) {
            e.printStackTrace();
        }
        catch (PackageNotFoundException e) {
            e.printStackTrace();
        }
        return false;
        
    }

    /*
     * (non-Javadoc)
     * 
     * @see bluej.extensions.event.CompileListener#compileStarted(bluej.extensions.event.CompileEvent)
     */
    public void compileStarted(CompileEvent event)
    {
        if (isInThisProject(event.getFiles())) {
            try {
                RCompileEvent rEvent = new RCompileEventImpl(event);
                remoteListener.compileStarted(rEvent);
            }
            catch (RemoteException e1) {
                e1.printStackTrace();
            }
        }
    }

    
    /*
     * (non-Javadoc)
     * 
     * @see bluej.extensions.event.CompileListener#compileError(bluej.extensions.event.CompileEvent)
     */
    public void compileError(CompileEvent event)
    {
        if (isInThisProject(event.getFiles())) {
            try {
                RCompileEvent rEvent = new RCompileEventImpl(event);
                remoteListener.compileError(rEvent);
            }
            catch (RemoteException e1) {
                e1.printStackTrace();
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see bluej.extensions.event.CompileListener#compileWarning(bluej.extensions.event.CompileEvent)
     */
    public void compileWarning(CompileEvent event)
    {
        if (isInThisProject(event.getFiles())) {
            try {
                RCompileEvent rEvent = new RCompileEventImpl(event);
                remoteListener.compileWarning(rEvent);
            }
            catch (RemoteException e1) {
                e1.printStackTrace();
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see bluej.extensions.event.CompileListener#compileSucceeded(bluej.extensions.event.CompileEvent)
     */
    public void compileSucceeded(CompileEvent event)
    {
        if (isInThisProject(event.getFiles())) {
            try {
                RCompileEvent rEvent = new RCompileEventImpl(event);
                remoteListener.compileSucceeded(rEvent);
            }
            catch (RemoteException e1) {
                e1.printStackTrace();
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see bluej.extensions.event.CompileListener#compileFailed(bluej.extensions.event.CompileEvent)
     */
    public void compileFailed(CompileEvent event)
    {
        if (isInThisProject(event.getFiles())) {
            try {
                RCompileEvent rEvent = new RCompileEventImpl(event);
                remoteListener.compileFailed(rEvent);
            }
            catch (RemoteException e1) {
                e1.printStackTrace();
            }
        }
    }
}