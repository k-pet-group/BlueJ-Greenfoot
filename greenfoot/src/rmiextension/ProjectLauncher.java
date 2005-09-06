package rmiextension;

import greenfoot.core.GreenfootLauncher;

import java.util.logging.Logger;

/**
 * The ProjectLauncher monitors packOpenEvents and instantiates a specific
 * object in the Debug-VM.
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: ProjectLauncher.java 3551 2005-09-06 09:31:41Z polle $
 */
public class ProjectLauncher
    implements ProjectListener
{
    private transient final static Logger logger = Logger.getLogger("greenfoot");
    private static ProjectLauncher instance;

    //TODO if we want to truly decouple the rmiextension from greenfoot, this
    // should be specified dynamically in some way. For now there is now need to
    // decouple further.
    private String launchClass = GreenfootLauncher.class.getName();

    private ProjectLauncher()
    {}

    public static ProjectLauncher instance()
    {
        if (instance == null) {
            instance = new ProjectLauncher();
        }
        return instance;
    }

    /**
     * Launches the project in the Debug virtual machine
     */
    private void launchProject(ProjectEvent event)
    {
        ObjectBench.createObject(event.getProject(), launchClass, "launcher");
    }

    public void projectOpened(ProjectEvent event)
    {
        if (!ProjectManager.instance().isProjectOpen(event.getProject())) {
            launchProject(event);
        }
        logger.info("ProjectLauncher.packageOpened done ");
    }

    public void projectClosed(ProjectEvent e)
    {}

}