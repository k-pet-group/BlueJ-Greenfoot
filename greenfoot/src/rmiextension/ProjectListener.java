package rmiextension;

/**
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: ProjectListener.java 3124 2004-11-18 16:08:48Z polle $
 */
public interface ProjectListener
{
    public void projectOpened(ProjectEvent e);

    public void projectClosed(ProjectEvent e);

}