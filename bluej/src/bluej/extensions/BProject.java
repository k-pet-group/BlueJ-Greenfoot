package bluej.extensions;

import bluej.pkgmgr.Project;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.PkgMgrFrame;

import java.io.File;
import java.util.List;
import java.util.ListIterator;

/**
 * This represent a Project open by BlueJ
 * NOTE: As a reference I store ONLY the key of the project and EVERY time I want to 
 * get information I will retrieve the LIVE Project.
 * I do this to try to have a reasonably syncronized view between the BleuJ and this
 * Othervise I will be holding a Project object that is NO longer active !
 *
 * @version $Id: BProject.java 1640 2003-03-04 20:26:52Z damiano $
 */

public class BProject
{
    private final Object projectKey;
  
    /**
     * Not for public use:
     * Creates a new project using as project ID the project dir
     * I need this to make better control on what parameters are passwd to this constructor
     */
    BProject (File projectDir)
    {
        projectKey = projectDir;
    }

    /**
     * Not for public use:
     * Creates a new project using as project ID the original bluejProject
     * I need this to make better control on what parameters are passwd to this constructor
     * NOTA: The underlyng mechanism is the same as the previous one. This is for type checking
     */
    BProject (Project bluejProject)
    {
        projectKey = bluejProject.getProjectDir();
    }


    /**
     * @return true if this project is valid and active, false othervise.
     */
    public boolean isValid ()
    {
        Project thisProject = Project.getProject(projectKey);
        return thisProject != null;      
    }
        
    /**
     * Gets the name of this project. 
     * This is what is displayed in the title bar of the frame after 'BlueJ'.
     * It can return null if this project has been invalidated.
     * 
     * @return the project name
     */
    public String getName()
    {
        Project thisProject = Project.getProject(projectKey);
        if ( thisProject == null ) return null;
        
        return thisProject.getProjectName();
    }
    
    /**
     * Gets the current top-level directory of this project. 
     * This would not change during the lifetime of the extension.
     * 
     * @return directory of this project. Can return null if project is invalid.
     */
    public File getProjectDir()
    {
        Project thisProject = Project.getProject(projectKey);
        if ( thisProject == null ) return null;

        return thisProject.getProjectDir();
    }
    
    /**
     * Requests BlueJ to save all open files of this project. Since files are saved everytime
     * they are compiled, it is not envisaged that there should be any problem to do this.
     * TODO: Should decide to trow error on strange situation.
     */
    public void saveProject()
    {
        Project thisProject = Project.getProject(projectKey);
        if ( thisProject == null ) return;

        try {
            thisProject.saveAll();
        } catch (NullPointerException ex) {
        }
    }
    
    /**
     * Saves any open files, then closes all frames relating to this project.
     */
    public void closeProject()
    {
        Project thisProject = Project.getProject(projectKey);
        if ( thisProject == null ) return;

        thisProject.saveAll();
        Project.closeProject (thisProject);
    }
    
    
    /**
     * Get a package
     * @param the fully-qualified name of the package
     * @return the requested package, or <code>null</code> if it wasn't found
     */
    public BPackage getPackage (String name)
    {
        Project thisProject = Project.getProject(projectKey);
        if ( thisProject == null ) return null;

        Package pkg = thisProject.getPackage (name);
        if ( pkg == null ) return null;
        
        return new BPackage (pkg);
    }
    
    /**
     * Gets all the packages
     * @return all the packages in this project
     */
    public BPackage[] getPackages()
    {
        Project thisProject = Project.getProject(projectKey);
        if ( thisProject == null ) return new BPackage[0];

        List names = thisProject.getPackageNames();
        BPackage[] packages = new BPackage [names.size()];
        for (ListIterator li=names.listIterator(); li.hasNext();) {
            int i=li.nextIndex();
            String name = (String)li.next();
            packages [i] = getPackage (name);
        }
        return packages;
    }
}