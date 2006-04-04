package rmiextension;

import bluej.extensions.BPackage;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;

/**
 * Represents a package in BlueJ. It is called a project because greenfoot does
 * not support packages.
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: Project.java 3974 2006-04-04 15:29:30Z polle $
 */
public class Project
{

    private BPackage pkg;

    public Project(BPackage pkg)
    {
        this.pkg = pkg;
    }

    public BPackage getPackage()
    {

        return pkg;
    }

    /**
     * @return
     */
    public String getDir()
    {

        try {
            return pkg.getProject().getDir().toString();
        }
        catch (ProjectNotOpenException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @return
     */
    public Object getName()
    {
        try {
            return pkg.getName();
        }
        catch (ProjectNotOpenException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (PackageNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

}