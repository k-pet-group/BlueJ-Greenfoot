package bluej.extensions;

import bluej.pkgmgr.Project;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.PkgMgrFrame;

import java.io.File;
import java.util.List;
import java.util.ListIterator;

/**
 * The BlueJ proxy Project object. This represents an open project, and functions relating
 * to that project.
 *
 * @author Clive Miller
 * @version $Id: BProject.java 1631 2003-02-25 11:33:16Z damiano $
 *
 * @see bluej.extensions.BlueJ#getOpenProjects()
 * @see bluej.extensions.BlueJ#openProject(java.io.File)
 * @see bluej.extensions.BPackage#getProject()
 */
public class BProject
{
    private final Project project;

    BProject (Project project)
    {
        this.project = project;
    }
        
    /**
     * Gets the name of this project. This is generally what is displayed in the title bar of the frame
     * after 'BlueJ'.
     * @return the project name
     */
    public String getName()
    {
        return project.getProjectName();
    }
    
    /**
     * Gets the current top-level directory of this project. This would not change during the
     * lifetime of the extension.
     * @return directory of this project.
     */
    public File getProjectDir()
    {
        return project.getProjectDir();
    }
    
    /**
     * Requests BlueJ to save all open files of this project. Since files are saved everytime
     * they are compiled, it is not envisaged that there should be any problem to do this.
     * However, if the project has been closed, the request will be denied and <CODE>false</CODE>
     * is returned.
     * @return <CODE>true</CODE> if the project is open and no exceptions were thrown.
     */
    public boolean saveProject()
    {
        try {
            project.saveAll();
            return true;
        } catch (NullPointerException ex) {
            return false;
        }
    }
    
    /**
     * Saves any open files, then closes all frames relating to this project.
     */
    public void closeProject()
    {
        project.saveAll();
        Project.closeProject (project);
    }
    
    
    /**
     * Get a package
     * @param the fully-qualified name of the package
     * @return the requested package, or <code>null</code> if it wasn't found
     */
    public BPackage getPackage (String name)
    {
        Package pkg = project.getPackage (name);
        return (pkg==null) ? null : new BPackage (pkg);
    }
    
    /**
     * Gets all the packages
     * @return all the packages in this project
     */
    public BPackage[] getPackages()
    {
        List names = project.getPackageNames();
        BPackage[] packages = new BPackage [names.size()];
        for (ListIterator li=names.listIterator(); li.hasNext();) {
            int i=li.nextIndex();
            String name = (String)li.next();
            packages [i] = getPackage (name);
        }
        return packages;
    }
}