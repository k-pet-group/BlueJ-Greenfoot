package rmiextension;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import bluej.extensions.BObject;
import bluej.extensions.BPackage;
import bluej.extensions.InvocationArgumentException;
import bluej.extensions.InvocationErrorException;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;
import bluej.extensions.event.CompileEvent;
import bluej.extensions.event.CompileListener;
import bluej.extensions.event.PackageEvent;
import bluej.extensions.event.PackageListener;

/**
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: ProjectManager.java 3124 2004-11-18 16:08:48Z polle $
 */
public class ProjectManager
    implements PackageListener, CompileListener
{
    private transient final static Logger logger = Logger.getLogger("greenfoot");

    private static ProjectManager instance;
    private List projectListeners = new ArrayList();
    private List openedPackages = new ArrayList();

    private ProjectManager()
    {}

    public static ProjectManager instance()
    {
        if (instance == null) {
            instance = new ProjectManager();
        }
        return instance;
    }

    public void addProjectListener(ProjectListener l)
    {
        projectListeners.add(l);
    }

    /*
     * (non-Javadoc)
     * 
     * @see bluej.extensions.event.PackageListener#packageOpened(bluej.extensions.event.PackageEvent)
     */
    public void packageOpened(PackageEvent event)
    {
        ProjectEvent projectEvent = new ProjectEvent(event);
        try {
            createObjectTracker(projectEvent.getProject());
        }
        catch (ProjectNotOpenException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (PackageNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (InvocationArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (InvocationErrorException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        logger.info("Creating bluejRMIClient");
        ObjectBench.createObject(projectEvent.getProject(), BlueJRMIClient.class.getName(), "blueJRMIClient",
                new Object[]{projectEvent.getProject().getDir(), projectEvent.getProject().getName()});
        logger.info("bluejRMIClient created");

        for (Iterator iter = projectListeners.iterator(); iter.hasNext();) {
            ProjectListener element = (ProjectListener) iter.next();
            element.projectOpened(projectEvent);
        }
        openedPackages.add(event.getPackage());

    }

    /*
     * (non-Javadoc)
     * 
     * @see bluej.extensions.event.PackageListener#packageClosing(bluej.extensions.event.PackageEvent)
     */
    public void packageClosing(PackageEvent event)
    {
        for (Iterator iter = projectListeners.iterator(); iter.hasNext();) {
            ProjectListener element = (ProjectListener) iter.next();
            ProjectEvent projectEvent = new ProjectEvent(event);
            element.projectClosed(projectEvent);
            openedPackages.remove(event.getPackage());
        }
    }

    public BPackage getPackage(String project, String name)
    {
        for (Iterator iter = openedPackages.iterator(); iter.hasNext();) {
            BPackage element = (BPackage) iter.next();
            try {
                if (element.getName().equals(name) && element.getProject().getName().equals(project)) {
                    return element;
                }
            }
            catch (ProjectNotOpenException e) {
                e.printStackTrace();
            }
            catch (PackageNotFoundException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public boolean isProjectOpen(Project prj)
    {
        boolean projectIsOpen = false;
        File prjFile = null;
        try {
            prjFile = prj.getPackage().getProject().getDir();
        }
        catch (ProjectNotOpenException e1) {
            e1.printStackTrace();
        }
        for (int i = 0; i < openedPackages.size(); i++) {
            BPackage openPkg = (BPackage) openedPackages.get(i);

            File openPrj = null;
            try {
                //  TODO package could be null if it is removed inbetween. should
                // synchronize the access to the list.
                //can throw ProjectNotOpenException
                openPrj = openPkg.getProject().getDir();
            }
            catch (ProjectNotOpenException e2) {
                //e2.printStackTrace();
            }

            //can give npe here - not anymore :-)
            if (openPrj != null && prjFile != null && openPrj.equals(prjFile)) {
                projectIsOpen = true;
            }
        }
        return projectIsOpen;
    }

    private synchronized void createObjectTracker(Project prj)
        throws ProjectNotOpenException, PackageNotFoundException, InvocationArgumentException, InvocationErrorException
    {
        ObjectBench.removeObject(prj, ObjectTracker.INSTANCE_NAME);
        logger.info("ProjectManager creating ObjectTracker");
        ObjectBench.createObject(prj, ObjectTracker.class.getName(), ObjectTracker.INSTANCE_NAME);
    }

    /**
     * TODO: make this nicer...
     */
    public void compileSucceeded(CompileEvent event)
    {
        //run through all the packages and search for an obejctracker.
        //if a package dont have an objecttracker it should get one!
        for (Iterator iter = openedPackages.iterator(); iter.hasNext();) {
            final BPackage element = (BPackage) iter.next();
            final Project prj = new Project(element);
            BObject objTracker;
            try {
                objTracker = element.getObject(ObjectTracker.INSTANCE_NAME);
                logger.info("objTracker: " + objTracker);
                if (objTracker == null) {
                    logger.info("Createing thread to create ObjectTracker");

                    Thread t = new Thread() {
                        public void run()
                        {
                            try {
                                createObjectTracker(prj);
                            }
                            catch (ProjectNotOpenException e) {
                                // TODO Handle this properly. Trck the closing
                                // of projects
                                //e.printStackTrace();
                            }
                            catch (PackageNotFoundException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                            catch (InvocationArgumentException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                            catch (InvocationErrorException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        }
                    };
                    t.start();
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

        }
        //  createObjectTracker();
    }

    /*
     * (non-Javadoc)
     * 
     * @see bluej.extensions.event.CompileListener#compileStarted(bluej.extensions.event.CompileEvent)
     */
    public void compileStarted(CompileEvent event)
    {
    // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see bluej.extensions.event.CompileListener#compileError(bluej.extensions.event.CompileEvent)
     */
    public void compileError(CompileEvent event)
    {
    // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see bluej.extensions.event.CompileListener#compileWarning(bluej.extensions.event.CompileEvent)
     */
    public void compileWarning(CompileEvent event)
    {
    // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see bluej.extensions.event.CompileListener#compileFailed(bluej.extensions.event.CompileEvent)
     */
    public void compileFailed(CompileEvent event)
    {
    // TODO Auto-generated method stub

    }

}