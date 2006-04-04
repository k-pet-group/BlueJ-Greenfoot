package rmiextension;

import greenfoot.core.Greenfoot;
import greenfoot.core.GreenfootLauncher;
import greenfoot.core.ProjectProperties;

import java.io.File;
import java.rmi.RemoteException;
import java.util.logging.Logger;

import bluej.extensions.BlueJ;

/**
 * The ProjectLauncher monitors project events from BlueJ and instantiates a
 * specific object in the Debug-VM.
 * 
 * @author Poul Henriksen
 * @version $Id: ProjectLauncher.java 3974 2006-04-04 15:29:30Z polle $
 */
public class ProjectLauncher
    implements ProjectListener
{
    private transient final static Logger logger = Logger.getLogger("greenfoot");
    private static ProjectLauncher instance;
    private static BlueJ bluej;
    
    /** The class that will be instantiated in the greenfoot VM */
    private String launchClass = GreenfootLauncher.class.getName();

    private ProjectLauncher()
    {}

    /**
     * Returns the singleton.
     * 
     * @return The singleton instance of the Project Launcher
     * @throws IllegalStateException If the Project Launcher has not been initialised.
     */
    public static ProjectLauncher instance() throws IllegalStateException
    {
        if(bluej == null) {
            throw new IllegalStateException("ProjectLauncher has not been initialised.");
        }
        if (instance == null) {
            instance = new ProjectLauncher();
        }
        return instance;
    }

    
    /**
     * Initialise. Must be called before the instance is accessed.
     * @param bluej
     */
    public static void init(BlueJ bluej)
    {
        ProjectLauncher.bluej = bluej;
    } 
    
    /**
     * Launches the project in the Debug virtual machine
     */
    private void launchProject(Project project)
    {     
        ObjectBench.createObject(project, launchClass, "launcher");
    }

    /**
     * This method will be invoked when BlueJ has opened the project. The method
     * will launch the project in the greenfoot-VM if it is a proper greenfoot
     * project.
     */
    public void projectOpened(ProjectEvent event)
    {
        if (!ProjectManager.instance().isProjectOpen(event.getProject())) {
            File projectDir = new File(event.getProject().getDir());
            boolean doOpen = false;
            try {
                doOpen = Greenfoot.updateApi(projectDir, bluej.getSystemLibDir());
            }
            catch (RemoteException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            if(doOpen) {
                launchProject(event.getProject());
            }
        }
        logger.info("ProjectLauncher.packageOpened done ");
    }
    
    public void projectClosed(ProjectEvent e)
    {}
}