/*
 This file is part of the BlueJ program.
 Copyright (C) 1999-2010,2012,2014,2016,2019  Michael Kolling and John Rosenberg

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

import bluej.compiler.CompileReason;
import bluej.compiler.CompileType;
import bluej.compiler.JobQueue;
import bluej.debugmgr.objectbench.ObjectBench;
import bluej.debugmgr.objectbench.ObjectWrapper;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.pkgmgr.Project;
import bluej.pkgmgr.target.ClassTarget;
import bluej.pkgmgr.target.PackageTarget;
import bluej.pkgmgr.target.Target;
import javafx.stage.Stage;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
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
     * @param  aPackageId an {@link Identifier} object referencing the underlying package to be wrapped by this BPackage.
     */
    @OnThread(Tag.Any)
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
     *
     * @return A {@link BProject} object wrapping the package's project.
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
     * Creates a new Java class with the given name.
     * The class name must not be a fully qualified name, and the .java file must already exist.
     * @param className the fully qualified name of the class to create.
     * @return A {@link BClass} object wrapping the created class.
     * @throws ProjectNotOpenException if the project this package is part of has been closed by the user.
     * @throws PackageNotFoundException if the package has been deleted by the user.
     * @throws MissingJavaFileException if the .java file for the new class does not exist.
     */
    public BClass newClass (String className)
        throws ProjectNotOpenException, PackageNotFoundException, MissingJavaFileException
    {
        return newClass(className, SourceType.Java);
    }

    /**
     * Creates a new class with the given name for the given type.
     * The class name must not be a fully qualified name, and the .java or .stride file must already exist.
     * @param className the fully qualified name of the class to create.
     * @param sourceType the {@link SourceType} objecet describing the type of the class, either {@link SourceType#Java} or {@link SourceType#Stride}.
     * @return A {@link BClass} object wrapping the created class.
     * @throws ProjectNotOpenException if the project this package is part of has been closed by the user.
     * @throws PackageNotFoundException if the package has been deleted by the user.
     * @throws MissingJavaFileException if the .java file for the new class does not exist.
     */
    public BClass newClass (String className, SourceType sourceType)
    throws ProjectNotOpenException, PackageNotFoundException, MissingJavaFileException
    {
        Package bluejPkg = packageId.getBluejPackage();
        PkgMgrFrame bluejFrame = packageId.getPackageFrame();

        File classJavaFile = new File (bluejPkg.getPath(), className + "." + sourceType.getExtension());
        if ( ! classJavaFile.canWrite() )
            throw new MissingJavaFileException(classJavaFile.toString());

        bluejFrame.createNewClass(className,null,sourceType,true,-1,-1);
        return getBClass ( className );
    }

    /**
     * Returns the package window.
     * This can be used (e.g.) as the "parent" frame for positioning modal dialogues.
     * @return A {@link Stage} object representing the window of the package wrapped by this BPackage.
     * @throws ProjectNotOpenException if the project this package is part of has been closed by the user.
     * @throws PackageNotFoundException if the package has been deleted by the user.
     */
    public Stage getWindow()
    throws ProjectNotOpenException, PackageNotFoundException
    {
        return packageId.getPackageFrame().getWindow();
    }

    /**
     * Returns the class with the given name in this package.
     * Note the naming inconsistency, which avoids a clash with
     * <code>java.lang.Object.getClass()</code>
     *
     * @param name the simple name of the required class.
     * @return A {@link BClass} object wrapping the class targeted with <code>name</code>, <code>null</code> if the class name does not exist.
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
     * @return An array of {@link BClass} objects wrapping the classes contained in this package.
     * @throws ProjectNotOpenException if the project this package is part of has been closed by the user.
     * @throws PackageNotFoundException if the package has been deleted by the user.
     */
    public BClass[] getClasses()
    throws ProjectNotOpenException, PackageNotFoundException
    {
        packageId.getBluejProject();
        Package bluejPkg = packageId.getBluejPackage();

        ArrayList<ClassTarget> classTargets = bluejPkg.getClassTargets();

        BClass[] classes = new BClass[classTargets.size()];
        for (int index=0; index<classTargets.size(); index++) {
            ClassTarget target = classTargets.get(index);
            classes [index] = target.getBClass();
        }

        return classes;
    }

    /**
     * Returns a wrapper for the object with the given name on BlueJ's object bench.
     * @param instanceName the name of the object as shown on the object bench.
     * @return A {@link BObject} object wrapping the object targered by <code>instanceName</code>, <code>null</code> if no such object exists.
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
     * @return An array of {@link BObject} objects wrapping the objects contained in the object bench.
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
     * Compiles all <b>modified</b> files of this package.
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
            throw new CompilationNotStartedException("BlueJ is currently executing Java code");

        // Start compilation
        bluejPkg.compile(CompileReason.EXTENSION, CompileType.EXTENSION);

        // if requested wait for the compilation to finish.
        if ( waitCompileEnd ) JobQueue.getJobQueue().waitForEmptyQueue();
    }


    /**
     * Compiles all files of this package.
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
            throw new CompilationNotStartedException("BlueJ is currently executing Java code");

        // Request for ALL files to be compiled
        bluejPkg.rebuild();

        // if requested wait for the compilation to finish.
        if ( waitCompileEnd ) JobQueue.getJobQueue().waitForEmptyQueue();
    }

    /**
     * Returns the currently selected classes in this Package.
     * If no class is selected an empty array is returned.
     * @return An array of {@link BClass} objects wrapping the classes selected in this package.
     * @throws ProjectNotOpenException if the project this package is part of has been closed by the user.
     * @throws PackageNotFoundException if the package has been deleted by the user.
     */
    public BClass[] getCurrentClasses ()
        throws ProjectNotOpenException, PackageNotFoundException
    {
        Package bluejPkg = packageId.getBluejPackage();
        List<Target> targets = bluejPkg.getSelectedTargets();
        ArrayList<BClass> aList  = new ArrayList<BClass>();

        for (Target t : targets)
        {
            if ( !(t instanceof ClassTarget )) continue;

            ClassTarget target = (ClassTarget)t;
            aList.add(target.getBClass());
        }

        return aList.toArray(new BClass[aList.size()]);
    }

    /**
     * Returns the currently selected objects in the object bench.
     * If no object is selected an empty array is returned.
     * @return An array of {@link BObject} objects wrapping the objects selected in the object bench.
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
     * @return A {@link File} object representing the directory where this package is stored.
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
     * Returns a string representation of the package object
     */
    @Override
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

    /**
     * Schedules a compilation of the package.
     *
     * @param immediate if <code>true</code>, compile now.  Otherwise, wait for the default time
     *                  (currently 1 second) then perform a compilation.  Any other
     *                  compilation requests from extensions or internally (e.g. due to code
     *                  editing) will reset the timer to 1 second again, so the compilation
     *                  will always occur 1 second after the call to the most recent <code>scheduleCompilation</code>
     *                  call.  e.g. if this method is called every 900ms, compilation will never occur.
     * @throws ProjectNotOpenException if the project has been closed by the user
     * @throws PackageNotFoundException if the package has been deleted
     */
    public void scheduleCompilation(boolean immediate) throws ProjectNotOpenException, PackageNotFoundException
    {
        Package bjPkg = packageId.getBluejPackage();
        Project bjProject = bjPkg.getProject();
        bjProject.scheduleCompilation(immediate, CompileReason.EXTENSION, CompileType.EXTENSION, bjPkg);
    }

    //package-visible
    PkgMgrFrame getPkgMgrFrame() throws ProjectNotOpenException, PackageNotFoundException
    {
        return packageId.getPackageFrame();
    }
}
