package bluej.extensions;

import bluej.pkgmgr.Project;
import bluej.pkgmgr.Package;

import java.io.File;
import java.util.List;
import java.util.ListIterator;

/**
 * A wrapper for a BlueJ project.
 *
 * @version $Id: BProject.java 1852 2003-04-15 14:56:38Z iau $
 */

/*
 * Author Clive Mille, Univeristy of Kent at Canterbury, 2002
 * Author Damiano Bolla, University of Kent at Canterbury, 2003
 */

public class BProject
{
    private final Object projectKey;
  
    /**
     * Constructor for a BProject.
     */
    BProject (File projectDir)
    {
      /* NOTE: As a reference I store ONLY the key of the project and EVERY time I want to 
       * get information I will retrieve the LIVE Project.
       * I do this to try to have a reasonably synchronized view between the BlueJ and this
       * Othervise I will be holding a Project object that is NO longer active !
       */
      projectKey = projectDir;
    }

    /**
     * The underlyng mechanism is the same as the previous one. This is for type checking
     */
    BProject (Project bluejProject)
    {
        if ( bluejProject == null ) 
            projectKey = null;
        else
            projectKey = bluejProject.getProjectDir();
    }


    /**
     * Test if this project still valid.
     * It may not be valid if it has been modified or closed
     * from the main BlueJ graphical user interface.
     * @return true if this project is valid and active, false otherwise.
     */
    public boolean isValid ()
    {
        Project thisProject = Project.getProject(projectKey);
        return thisProject != null;      
    }
        
    /**
     * Returns the name of this project. 
     * This is what is displayed in the title bar of the frame after 'BlueJ'.
     * Returns null if this project is invalid.
     */
    public String getName()
    {
        Project thisProject = Project.getProject(projectKey);
        if ( thisProject == null ) return null;
        
        return thisProject.getProjectName();
    }
    
    /**
     * Returns the directory in which this project is stored. 
     * Returns null if this project is invalid.
     */
    public File getDir()
    {
        Project thisProject = Project.getProject(projectKey);
        if ( thisProject == null ) return null;

        return thisProject.getProjectDir();
    }
    
    /**
     * Requests a "save" of all open files in this project. 
     */
    public void save()
    {
        Project thisProject = Project.getProject(projectKey);
        if ( thisProject == null ) return;

        thisProject.saveAll();
    }
    
    /**
     * Saves any open files, then closes all frames belonging to this project.
     */
    public void close()
    {
        Project thisProject = Project.getProject(projectKey);
        if ( thisProject == null ) return;

        thisProject.saveAll();
        Project.closeProject (thisProject);
    }
    
    
    /**
     * Get a package belonging to this project.
     * Returns null if this project is invalid.
     * 
     * @param the fully-qualified name of the package
     * @return the requested package, or null if it wasn't found
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
     * Returns all packages belonging to this project.
     * If for some reason none exist, or the project is invalid, an empty array is returned.
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
