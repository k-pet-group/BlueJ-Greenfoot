package rmiextension;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import bluej.extensions.BPackage;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;
import bluej.extensions.event.PackageEvent;
import bluej.extensions.event.PackageListener;

/**
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: ProjectManager.java 3842 2006-03-20 14:56:04Z polle $
 */
public class ProjectManager
    implements PackageListener
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
        try {
            if (event.getPackage().getName().equals("") || event.getPackage().getProject().getName().equals("startupProject")) {
               
                ProjectEvent projectEvent = new ProjectEvent(event);
                
                logger.info("Creating bluejRMIClient");
                ObjectBench.createObject(projectEvent.getProject(), BlueJRMIClient.class.getName(), "blueJRMIClient",
                        new Object[]{projectEvent.getProject().getDir(), projectEvent.getProject().getName()});
                logger.info("bluejRMIClient created");
                
                for (Iterator iter = projectListeners.iterator(); iter.hasNext();) {
                    ProjectListener element = (ProjectListener) iter.next();
                    element.projectOpened(projectEvent);
                }
            } 
        }
        catch (PackageNotFoundException pnfe) {}
        catch (ProjectNotOpenException pnoe) {}
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
        }
        openedPackages.remove(event.getPackage());
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

   

  

}