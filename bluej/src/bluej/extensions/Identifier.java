/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
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
package bluej.extensions;


/* The problem I am trying to solve is to have a uniform and simple way to deal with
 * objects validity. An extension may be holding BProject  but this BProject may not be valid
 * since the gui has closed it. Or may be holding aBPackage and this not being valid
 * The same apply to a BClass or a BOBject.
 *
 * To solve it I need to store the ID of the above objects and check if it is still valid
 * before doing anything.
 * Again, the problem is that for a BClass I need not only to check if the Class is valid
 * but also the Project and the Package !
 * So the ID if a BClass is really all of the above...
 *
 * Then, the solution is to put all that is needed in here and have this class only deal with
 * checking the mess of it...
 *
 * NOTE on class Names: Most of the time we would like the qualified form of the class name
 * however there are cases when we need the short form, it seems reasonable to store the
 * long form and derive the short one.
 *
 * Author: Damiano Bolla 2003,2004
 */
import java.awt.EventQueue;
import java.io.File;

import bluej.pkgmgr.Package;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.pkgmgr.Project;
import bluej.pkgmgr.target.ClassTarget;
import bluej.pkgmgr.target.Target;
import bluej.views.View;

/**
 *  Description of the Class
 */
class Identifier
{
    private File projectId;
    private String packageId;
    private String qualifiedClassName;


    /**
     *Constructor for the Identifier object
     *
     * @param  bleujProject  Description of the Parameter
     */
    Identifier(Project bluejProject)
    {
        this(bluejProject, null, null);
    }


    /**
     *Constructor for the Identifier object
     *
     * @param  bluejProject  Description of the Parameter
     * @param  bluejPackage  Description of the Parameter
     */
    Identifier(Project bluejProject, Package bluejPackage)
    {
        this(bluejProject, bluejPackage, null);
    }


    /**
     * Constructor for the Identifier object
     *
     * @param  bluejProject         Description of the Parameter
     * @param  bluejPackage         Description of the Parameter
     * @param  aQualifiedClassName  Description of the Parameter
     */
    Identifier(Project bluejProject, Package bluejPackage, String aQualifiedClassName)
    {
        projectId = bluejProject.getProjectDir();
        if (bluejPackage != null) packageId = bluejPackage.getQualifiedName();
        qualifiedClassName = aQualifiedClassName;
    }


    /**
     * Returns the blueJProject and also checks its existence
     *
     * @return                              The bluejProject value
     * @exception  ProjectNotOpenException  Description of the Exception
     */
    Project getBluejProject() throws ProjectNotOpenException
    {
        Project aProject = Project.getProject(projectId);

        if (aProject == null)
            throw new ProjectNotOpenException("Project " + projectId + " is closed");

        return aProject;
    }


    /**
     * Returns the inner bluej package given the current identifier.
     *
     * @return                               The bluejPackage value
     * @exception  ProjectNotOpenException   Description of the Exception
     * @exception  PackageNotFoundException  Description of the Exception
     */
    Package getBluejPackage() throws ProjectNotOpenException, PackageNotFoundException
    {
        Project bluejProject = getBluejProject();

        Package bluejPkg = bluejProject.getCachedPackage(packageId);
        if (bluejPkg == null)
            throw new PackageNotFoundException("Package '" + packageId + "' is deleted");

        return bluejPkg;
    }

    /**
     * Returns the name of the class. No checks are made for validity of 
     * the name, to avoid having to compile the class in order to get 
     * its name.
     * This means that the name may not be valid, if the class has 
     * been renamed or deleted.
     * @return    The qualified name of the class represented by this identifier, or null if it doesn't represent a class
     */
    String getClassName()
    {
        return qualifiedClassName;
    }

    /**
     * Returns the Frame associated with this Package.
     * The nice thing about this one is that it WILL open a frame if it was not already open.
     * This gets rid of one possible exception regarding a packageFrame not open...
     *
     * @return                               The packageFrame value
     * @exception  ProjectNotOpenException   Description of the Exception
     * @exception  PackageNotFoundException  Description of the Exception
     */
    PkgMgrFrame getPackageFrame()
             throws ProjectNotOpenException, PackageNotFoundException
    {
        Package thisPkg = getBluejPackage();

        // Get a frame for the package.
        final PkgMgrFrame pmf = PkgMgrFrame.createFrame(thisPkg);

        EventQueue.invokeLater(new Runnable() {
            public void run()
            {
                // Check if it's already visible first, as calling
                // setVisible(true) causes frame to grab focus on
                // Windows
                if(!pmf.isVisible()) {
                    pmf.setVisible(true);
                }
            }
        });
        return pmf;
    }

    /**
     * Returns the Java class that is associated with this name in this package
     *
     * @return                              The javaClass value
     * @exception  ProjectNotOpenException  Description of the Exception
     * @exception  ClassNotFoundException   Description of the Exception
     */
    Class getJavaClass() throws ProjectNotOpenException, ClassNotFoundException
    {
        Project bluejPrj = getBluejProject();

        Class aClass = bluejPrj.loadClass(qualifiedClassName);
        if (aClass == null)
            throw new ClassNotFoundException("Class " + qualifiedClassName + " Not Found");

        return aClass;
    }


    /**
     * Returns the class target of this java class by checking its existence
     *
     * @return                               The classTarget value
     * @exception  ProjectNotOpenException   Description of the Exception
     * @exception  PackageNotFoundException  Description of the Exception
     */
    ClassTarget getClassTarget()
             throws ProjectNotOpenException, PackageNotFoundException
    {
        Package bluejPkg = getBluejPackage();

        String className = qualifiedClassName;
        int dotpos = qualifiedClassName.lastIndexOf(".");
        if (dotpos > 0)
            className = qualifiedClassName.substring(dotpos + 1);
        Target aTarget = bluejPkg.getTarget(className);

        if (aTarget == null)
            throw new NullPointerException("Class " + qualifiedClassName + " has NO ClassTarget");

        if (!(aTarget instanceof ClassTarget))
            throw new NullPointerException("Class " + qualifiedClassName + " is not of type ClassTarget");

        return (ClassTarget) aTarget;
    }


    /**
     * Returns the view associated with this Class
     *
     * @return                              The bluejView value
     * @exception  ProjectNotOpenException  Description of the Exception
     * @exception  ClassNotFoundException   Description of the Exception
     */
    View getBluejView()
             throws ProjectNotOpenException, ClassNotFoundException
    {
        Class aClass = getJavaClass();

        // View.getView does not fail, if the class does not exist it will be created.
        return View.getView(aClass);
    }
}
