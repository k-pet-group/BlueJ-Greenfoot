/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2011,2013,2014,2015,2016,2019  Michael Kolling and John Rosenberg
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package bluej.extensions2;

import bluej.collect.DataCollector;
import bluej.editor.stride.FXTabbedEditor;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.pkgmgr.Project;
import javafx.application.Platform;

import java.io.File;
import java.net.URLClassLoader;
import java.util.List;
import java.util.ListIterator;

/**
 * A wrapper for a BlueJ project.
 *
 * @author Clive Mille, Univeristy of Kent at Canterbury, 2002
 * @author Damiano Bolla, University of Kent at Canterbury, 2003,2004,2005
 */
public class BProject
{
    private Identifier projectId;

    /**
     * @param  i_projectId an {@link Identifier} object referencing the underlying project to be wrapped by this BProject.
     */
    BProject (Identifier i_projectId)
    {
        projectId = i_projectId;
    }

    /**
     * Returns the name of this project.
     * This is what is displayed in the title bar of the frame after 'BlueJ'.
     * @throws ProjectNotOpenException if the project has been closed by the user.
     */
    public String getName() throws ProjectNotOpenException
    {
        Project thisProject = projectId.getBluejProject();

        return thisProject.getProjectName();
    }

    /**
     * Returns the directory in which this project is stored.
     * @return A {@link File} object representing the directory where this project is stored.
     * @throws ProjectNotOpenException if the project has been closed by the user.
     */
    public File getDir() throws ProjectNotOpenException
    {
        Project thisProject = projectId.getBluejProject();

        return thisProject.getProjectDir();
    }

    /**
     * Requests a "save" of all open files in this project.
     * @throws ProjectNotOpenException if the project has been closed by the user.
     */
    public void save() throws ProjectNotOpenException
    {
        Project thisProject = projectId.getBluejProject();

        thisProject.saveAll();
    }

    /**
     * Saves any open files, then closes all frames belonging to this project.
     * @throws ProjectNotOpenException if the project has been closed by the user.
     */
    public void close() throws ProjectNotOpenException
    {
        Project thisProject = projectId.getBluejProject();

        thisProject.saveAll();
        PkgMgrFrame.closeProject (thisProject);
    }

    /**
     * Restarts the VM used to run user code for this project.
     * As a side-effect, removes all objects from the object bench.
     * @throws ProjectNotOpenException if the project has been closed by the user.
     */
    public void restartVM() throws ProjectNotOpenException
    {
        projectId.getBluejProject().restartVM();
        DataCollector.debuggerTerminate(projectId.getBluejProject());
    }

    /**
     * Creates and returns a new package with the given fully qualified name.
     * The necessary directories and files will be created.
     *
     * @return A {@link BPackage} object wrapping the created package.
     * @throws ProjectNotOpenException if the project has been closed by the user.
     * @throws PackageAlreadyExistsException if the named package already exists in the project.
     */
    public BPackage newPackage(String fullyQualifiedName )
        throws ProjectNotOpenException, PackageAlreadyExistsException
    {
        Project bluejProject = projectId.getBluejProject();

        int result = bluejProject.newPackage(fullyQualifiedName);

        if ( result == Project.NEW_PACKAGE_BAD_NAME ) {
            throw new IllegalArgumentException("newPackage: Bad package name '"+fullyQualifiedName+"'");
        }

        if ( result == Project.NEW_PACKAGE_EXIST ) {
            throw new PackageAlreadyExistsException("newPackage: Package '"+fullyQualifiedName+"' already exists");
        }

        if ( result == Project.NEW_PACKAGE_NO_PARENT ) {
            throw new IllegalStateException("newPackage: Package '"+fullyQualifiedName+"' has no parent package");
        }

        if ( result != Project.NEW_PACKAGE_DONE ) {
            throw new IllegalStateException("newPackage: Unknown result code="+result);
        }

        Package pkg = bluejProject.getPackage(fullyQualifiedName);

        if ( pkg == null ) {
            throw new Error("newPackage: getPackage '"+fullyQualifiedName+"' returned null");
        }

        Package reloadPkg = pkg;
        for(int index=0; index<10 && reloadPkg != null; index++) {
            // This is needed since the GUI is not sync with the state
            // It would be better is core BlueJ did fix this..
            reloadPkg.reload();
            reloadPkg = reloadPkg.getParent();
        }

        return pkg.getBPackage();
    }

    /**
     * Gets a package belonging to this project.
     *
     * @param name the fully-qualified name of the package.
     * @return A {@link BPackage} object wrapping the requested package, <code>null</code> if it wasn't found.
    *
     * @throws ProjectNotOpenException if the project has been closed by the user.
     */
    public BPackage getPackage (String name) throws ProjectNotOpenException
    {
        Project bluejProject = projectId.getBluejProject();
        Package pkg = bluejProject.getPackage (name);
        if (pkg == null) {
            return null;
        }
        return pkg.getBPackage();
    }

    /**
     * Returns all packages belonging to this project.
     * If none exist an empty array is returned.
     *
     * @return An array of {@link BPackage} objects wrapping this project's packages.
     * @throws ProjectNotOpenException if the project has been closed by the user.
     */
    public BPackage[] getPackages() throws ProjectNotOpenException
    {
        Project thisProject = projectId.getBluejProject();

        List<String> names = thisProject.getPackageNames();
        BPackage[] packages = new BPackage[names.size()];
        for (ListIterator<String> li = names.listIterator(); li.hasNext();) {
            int i=li.nextIndex();
            String name = li.next();
            packages [i] = getPackage (name);
        }
        return packages;
    }


    /**
     * Returns a URLClassLoader that should be used to load project classes.
     * Every time a project is compiled, even when the compilation is started from the GUI,
     * a new URLClassLoader is created and if the extension currently have a copy of the old one it should discard it
     * and use getClassLoader() to acquire the new one.
     * @return A {@link ClassLoader} object that should be used to load project classes.
     * @throws ProjectNotOpenException if the project has been closed by the user.
     */
    public URLClassLoader getClassLoader() throws ProjectNotOpenException
    {
        Project thisProject = projectId.getBluejProject();

        return thisProject.getClassLoader();
    }

    /**
     * Returns a string representation of this package object
     */
    public String toString ()
    {
        try {
            Project thisProject = projectId.getBluejProject();
            return "BProject: "+thisProject.getProjectName();
        }
        catch(ExtensionException exc) {
            return "BProject: INVALID";
        }
    }

    void clearObjectBench() throws ProjectNotOpenException
    {
        Project thisProject = projectId.getBluejProject();
        thisProject.clearObjectBenches();
    }

    //Package-visible:
    Project getProject() throws ProjectNotOpenException
    {
        return projectId.getBluejProject();
    }

    /**
     * Opens a tab editor with a web browser showing the given URL.
     * If any open web browser tab is already showing that URL,
     * it is shown and focused instead of opening another browser with the same page.
     *
     * @param url The URL to open in the web browser
     * @throws ProjectNotOpenException if the project has been closed by the user
     */
    public void openWebViewTab(String url) throws ProjectNotOpenException
    {
        Project bjProject = projectId.getBluejProject();
        FXTabbedEditor fXTabbedEditor = bjProject.getDefaultFXTabbedEditor();
        fXTabbedEditor.openWebViewTab(url);
    }
}
