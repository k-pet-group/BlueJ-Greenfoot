package rmiextension;

import bluej.extensions.event.PackageEvent;

/**
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: ProjectEvent.java 3124 2004-11-18 16:08:48Z polle $
 */
public class ProjectEvent
{
    private Project project;

    protected ProjectEvent(PackageEvent e)
    {
        this.project = new Project(e.getPackage());
    }

    public Project getProject()
    {
        return project;
    }

}