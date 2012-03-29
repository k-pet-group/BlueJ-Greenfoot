/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2010,2012  Michael Kolling and John Rosenberg 
 
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

import bluej.compiler.*;
import bluej.debugmgr.objectbench.*;
import bluej.extensions.BDependency.Type;
import bluej.pkgmgr.*;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.dependency.Dependency;
import bluej.pkgmgr.target.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;


/**
 * A wrapper for a single package of a BlueJ project.
 * This represents an open package, and functions relating to that package.
 *
 * @author Clive Miller, University of Kent at Canterbury, 2002
 * @author Damiano Bolla, University of Kent at Canterbury, 2003
 */
public class BPackage
{
    private Identifier packageId;

    /**
     * Constructor for a BPackage.
     */
    BPackage (Identifier aPackageId)
    {
        packageId=aPackageId;
    }

    /**
     * Removes this package from BlueJ, including the underlying files.
     *
     * @throws ProjectNotOpenException if the project this package is part of has been closed by the user.
     * @throws PackageNotFoundException if the package has been deleted by the user.
     */
    public void remove() 
        throws ProjectNotOpenException, PackageNotFoundException
    {
        Package bluejPkg  = packageId.getBluejPackage();
        Package parentPkg = bluejPkg.getParent();

        PackageTarget pkgTarget=(PackageTarget)parentPkg.getTarget(bluejPkg.getBaseName());
        pkgTarget.removeImmediate();
    }

    /**
     * Returns the package's project.
     * @throws ProjectNotOpenException if the project has been closed by the user.
     */
    public BProject getProject() throws ProjectNotOpenException
    {
        Project bluejProject = packageId.getBluejProject();

        return bluejProject.getBProject();
    }

    /**
     * Returns the name of the package. 
     * Returns an empty string if no package name has been set.
     * @throws ProjectNotOpenException if the project this package is part of has been closed by the user.
     * @throws PackageNotFoundException if the package has been deleted by the user.
     */
    public String getName() 
    throws ProjectNotOpenException, PackageNotFoundException
    {
        Package bluejPkg = packageId.getBluejPackage();

        return bluejPkg.getQualifiedName();
    }


    /**
     * Reloads the entire package.
     * This is used (e.g.) when a new <code>.java</code> file has been added to the package.
     * @throws ProjectNotOpenException if the project this package is part of has been closed by the user.
     * @throws PackageNotFoundException if the package has been deleted by the user.
     */
    public void reload() 
    throws ProjectNotOpenException, PackageNotFoundException
    {
        Package bluejPkg = packageId.getBluejPackage();

        bluejPkg.reload();
    }

    /**
     * Creates a new Class with the given name.
     * The class name must not be a fully qualified name, and the .java file must already exist.
     * @throws ProjectNotOpenException if the project this package is part of has been closed by the user.
     * @throws PackageNotFoundException if the package has been deleted by the user.
     * @throws MissingJavaFileException if the .java file for the new class does not exist.
     */
    public BClass newClass ( String className )
    throws ProjectNotOpenException, PackageNotFoundException, MissingJavaFileException
    {
        Package bluejPkg = packageId.getBluejPackage();
        PkgMgrFrame bluejFrame = packageId.getPackageFrame();

        File classJavaFile = new File (bluejPkg.getPath(),className+".java");
        if ( ! classJavaFile.canWrite() ) 
            throw new MissingJavaFileException (classJavaFile.toString());

        bluejFrame.createNewClass(className,null,true);
        return getBClass ( className );
    }
    
    /**
     * Returns the package frame.
     * This can be used (e.g.) as the "parent" frame for positioning modal dialogues.
     * @throws ProjectNotOpenException if the project this package is part of has been closed by the user.
     * @throws PackageNotFoundException if the package has been deleted by the user.
     */
    public Frame getFrame() 
    throws ProjectNotOpenException, PackageNotFoundException
    {
        return packageId.getPackageFrame();
    }
    
    /**
     * Returns the class with the given name in this package.
     * Returns null if the class name does not exist. Note the naming
     * inconsistency, which avoids a clash with
     * <code>java.lang.Object.getClass()</code>
     * 
     * @param name the simple name of the required class.
     * @throws ProjectNotOpenException if the project this package is part of has been closed by the user.
     * @throws PackageNotFoundException if the package has been deleted by the user.
     */
    public BClass getBClass (String name)   
    throws ProjectNotOpenException, PackageNotFoundException
    {
        packageId.getBluejProject();
        Package bluejPkg = packageId.getBluejPackage();

        Target aTarget = bluejPkg.getTarget (name);

        // We may consider reporting this as a not found
        if ( aTarget == null ) return null;

        // And this in a different way
        if ( !(aTarget instanceof ClassTarget)) return null;

        ClassTarget classTarget = (ClassTarget)aTarget;

        return classTarget.getBClass();
    }
    
    /**
     * Returns an array containing all the classes in this package.
     * If there are no classes an empty array will be returned.
     * @throws ProjectNotOpenException if the project this package is part of has been closed by the user.
     * @throws PackageNotFoundException if the package has been deleted by the user.
     */
    public BClass[] getClasses() 
    throws ProjectNotOpenException, PackageNotFoundException
    {
        packageId.getBluejProject();
        Package bluejPkg = packageId.getBluejPackage();

        ArrayList<ClassTarget> classTargets = bluejPkg.getClassTargets();

        BClass[] classes = new BClass [classTargets.size()];
        for (int index=0; index<classTargets.size(); index++) {
            ClassTarget target = classTargets.get(index);
            classes [index] = target.getBClass();
        }

        return classes;
    }
    
    /**
     * Returns a wrapper for the object with the given name on BlueJ's object bench.
     * @param instanceName the name of the object as shown on the object bench
     * @return the object, or null if no such object exists.
     * @throws ProjectNotOpenException if the project this package is part of has been closed by the user.
     * @throws PackageNotFoundException if the package has been deleted by the user.
     */
    public BObject getObject (String instanceName) 
        throws ProjectNotOpenException, PackageNotFoundException
    {
        // The usual check to avoid silly stack trace
        if(instanceName == null) {
            return null;
        }

        packageId.getBluejPackage();
        PkgMgrFrame pmf = packageId.getPackageFrame();
        
        List<ObjectWrapper> objects = pmf.getObjectBench().getObjects();
        for(Iterator<ObjectWrapper> i=objects.iterator(); i.hasNext(); ) {
            ObjectWrapper wrapper = i.next();
            if (instanceName.equals(wrapper.getName())) {
                return wrapper.getBObject();
            }
        }
        return null;
    }    

    /**
     * Returns an array of all the Objects on the object bench.
     * The array will be empty if no objects are on the bench.
     * @throws ProjectNotOpenException if the project this package is part of has been closed by the user.
     * @throws PackageNotFoundException if the package has been deleted by the user.
     */
    public BObject[] getObjects() 
        throws ProjectNotOpenException, PackageNotFoundException
    {
        packageId.getBluejPackage();
        PkgMgrFrame pmf = packageId.getPackageFrame();
   
        List<ObjectWrapper> objectWrappers = pmf.getObjectBench().getObjects();
        BObject[] objects = new BObject [objectWrappers.size()];
        int index = 0;
        for(Iterator<ObjectWrapper> i=objectWrappers.iterator(); i.hasNext(); ) {
            ObjectWrapper wrapper = i.next();
            objects[index] = wrapper.getBObject();
            index++;
        }
        return objects;
    }
    

    /**
     * Compile all modified files of this package.
     * A single CompileEvent with all modified files listed will be generated.
     * @param  waitCompileEnd <code>true</code> waits for the compilation to be finished.
     * @throws ProjectNotOpenException if the project this package is part of has been closed by the user.
     * @throws PackageNotFoundException if the package has been deleted by the user.
     * @throws CompilationNotStartedException if BlueJ is currently executing Java code.
     */
    public void compile ( boolean waitCompileEnd ) 
    throws ProjectNotOpenException, PackageNotFoundException, CompilationNotStartedException
    {
        Package bluejPkg = packageId.getBluejPackage();

        if ( ! bluejPkg.isDebuggerIdle() )
            throw new CompilationNotStartedException ("BlueJ is currently executing Java code");

        // Start compilation
        bluejPkg.compile();

        // if requested wait for the compilation to finish.
        if ( waitCompileEnd ) JobQueue.getJobQueue().waitForEmptyQueue();
    }
    

    /**
     * Compile all files of this package.
     * A single CompileEvent with all compiled files listed will be generated.
     * @param  waitCompileEnd <code>true</code> waits for the compilation to be finished.
     * @throws ProjectNotOpenException if the project this package is part of has been closed by the user.
     * @throws PackageNotFoundException if the package has been deleted by the user.
     * @throws CompilationNotStartedException if BlueJ is currently executing Java code.
     */
    public void compileAll ( boolean waitCompileEnd ) 
    throws ProjectNotOpenException, PackageNotFoundException, CompilationNotStartedException
    {
        Package bluejPkg = packageId.getBluejPackage();

        if ( ! bluejPkg.isDebuggerIdle() )
            throw new CompilationNotStartedException ("BlueJ is currently executing Java code");

        // Request for ALL files to be compiled
        bluejPkg.rebuild(); 

        // if requested wait for the compilation to finish.
        if ( waitCompileEnd ) JobQueue.getJobQueue().waitForEmptyQueue();
    }

    /** 
     * Returns the currently selected classes in this Package.
     * If no class is selected an empty array is returned.
     * @throws ProjectNotOpenException if the project this package is part of has been closed by the user.
     * @throws PackageNotFoundException if the package has been deleted by the user.
     */
    public BClass [] getCurrentClasses ()
        throws ProjectNotOpenException, PackageNotFoundException
    {
        Package bluejPkg = packageId.getBluejPackage();    
        Target [] targets = bluejPkg.getSelectedTargets();
        ArrayList<BClass> aList  = new ArrayList<BClass>();
        
        for(int index=0; index<targets.length; index++) 
        {
            if ( !(targets[index] instanceof ClassTarget )) continue; 

            ClassTarget target = (ClassTarget)targets[index];
            aList.add(target.getBClass());
        }

        return aList.toArray(new BClass[aList.size()]);
    }

    /** 
     * Returns the currently selected objects in the Object Bench.
     * If no object is selected an empty array is returned.
     * @throws ProjectNotOpenException if the project this package is part of has been closed by the user.
     * @throws PackageNotFoundException if the package has been deleted by the user.
     */
    public BObject[] getCurrentObjects ()
        throws ProjectNotOpenException, PackageNotFoundException
    {
        PkgMgrFrame bluejFrame = packageId.getPackageFrame();
        ObjectBench aBench = bluejFrame.getObjectBench();
        if ( aBench == null ) return new BObject[0];

        ArrayList<BObject> aList  = new ArrayList<BObject>();
        // In the future we will really return more than one element
        ObjectWrapper aWrapper = aBench.getSelectedObject();
        if (aWrapper != null) {
            aList.add(aWrapper.getBObject());
        }

        return aList.toArray(new BObject[aList.size()]);
    }

    /**
     * Returns the directory where this package is stored.
     * @throws ProjectNotOpenException if the project this package is part of has been closed by the user.
     * @throws PackageNotFoundException if the package has been deleted by the user.
     */
    public File getDir ()
        throws ProjectNotOpenException, PackageNotFoundException
    {
        Package aPkg = packageId.getBluejPackage();

        return aPkg.getPath();
    }

    /**
     * Returns the dependency with the given <code>origin</code>,
     * <code>target</code> and <code>type</code>.
     * 
     * @param from
     *            The origin of the dependency.
     * @param to
     *            The target of the dependency.
     * @param type
     *            The type of the dependency (there may be more than one
     *            dependencies with the same origin and target but different
     *            types).
     * @return The dependency with the given <code>origin</code> and
     *         <code>target</code> or <code>null</code> if there is no such
     *         dependency.
     * @throws ProjectNotOpenException
     *             if the project this package is part of has been closed by the
     *             user.
     * @throws PackageNotFoundException
     *             if the package has been deleted by the user.
     */
    public BDependency getDependency(BClassTarget from, BClassTarget to, Type type)
            throws ProjectNotOpenException, PackageNotFoundException
    {
        Package bluejPackage = packageId.getBluejPackage();
        Dependency dependency = bluejPackage.getDependency(from.getClassTarget(), to.getClassTarget(), type);
        
        return (dependency != null) ? dependency.getBDependency() : null;
    }

    /**
     * Returns a string representation of the package object
     */
    public String toString () 
    {
        try 
        {
            Package bluejPkg = packageId.getBluejPackage();
            return "BPackage: "+bluejPkg.getQualifiedName();
        }
        catch ( ExtensionException exc )
        {
            return "BPackage: INVALID";  
        }
    }
}
