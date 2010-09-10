/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010  Michael Kolling and John Rosenberg 
 
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

import bluej.debugger.*;
import bluej.debugmgr.objectbench.*;
import bluej.pkgmgr.*;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.target.ClassTarget;
import com.sun.jdi.*;
import java.util.*;

/**
 * A wrapper for an object on the BlueJ object bench.
 * This wraps an object so you can add and remove it from the bench.
 *
 * @see        BConstructor
 * @see        BMethod
 * @see        BField
 *
 * @author Clive Miller, University of Kent at Canterbury, 2002
 * @author Damiano Bolla, University of Kent at Canterbury 2003,2004
 */
public class BObject
{
    private ObjectWrapper objectWrapper;
    
    /** An identifier for the class of this object */
    private Identifier wrapperId;


    /**
     * Constructor for BObject.
     *
     * @param  aWrapper  Description of the Parameter
     */
    BObject(ObjectWrapper aWrapper)
    {
        objectWrapper = aWrapper;

        Package bluejPkg = objectWrapper.getPackage();
        Project bluejProj = bluejPkg.getProject();

        // It really seems that the translation between Java naming and Class is needed.
        // Also tryng to get the Class instead of just the name is a mess...
        String className = transJavaToClass(objectWrapper.getClassName());

        wrapperId = new Identifier(bluejProj, bluejPkg, className);
    }


    /**
     * Returns the package this object belongs to.
     *
     * @return                            The package value
     * @throws  ProjectNotOpenException   if the project to which this object belongs has been closed by the user.
     * @throws  PackageNotFoundException  if the package to which this object belongs has been deleted by the user.
     */
    public BPackage getPackage()
             throws ProjectNotOpenException, PackageNotFoundException
    {
        Package bluejPkg = wrapperId.getBluejPackage();

        return bluejPkg.getBPackage();
    }


    /**
     * Removes this object from the object bench.
     * This will also remove it from the view of the object bench.
     * Once the object is removed from the bench it will not be available again.
     *
     * @throws  ProjectNotOpenException   if the project to which this object belongs has been closed by the user.
     * @throws  PackageNotFoundException  if the package to which this object belongs has been deleted by the user.
     */
    public void removeFromBench()
             throws ProjectNotOpenException, PackageNotFoundException
    {
        Package aPackage = wrapperId.getBluejPackage();
        PkgMgrFrame aFrame = wrapperId.getPackageFrame();

        ObjectBench aBench = aFrame.getObjectBench();
        aBench.removeObject(objectWrapper, aPackage.getId());

        objectWrapper = null;
    }


    /**
     * Adds this object on the object bench.
     * If you pass null as instanceName the object will have a predefined name.
     * If the object is not a valid one nothing will happen.
     *
     *
     * @param  instanceName               The name you want this object to have on the bench.
     * @throws  ProjectNotOpenException   if the project to which this object belongs has been closed by the user.
     * @throws  PackageNotFoundException  if the package to which this object belongs has been deleted by the user.
     */
    public void addToBench(String instanceName)
             throws ProjectNotOpenException, PackageNotFoundException
    {
        if (objectWrapper == null) {
            return;
        }

        // Not rational to add a null object, is it ?
        if (objectWrapper.getObject().isNullObject()) {
            return;
        }

        // If you want you may set the instance name here. Otherwise accept default
        if (instanceName != null) {
            objectWrapper.setName(instanceName);
        }

        // This should really always exists, no need to check
        Package aPackage = wrapperId.getBluejPackage();
        PkgMgrFrame aFrame = wrapperId.getPackageFrame();

        ObjectBench aBench = aFrame.getObjectBench();
        aBench.addObject(objectWrapper);

        // load the object into runtime scope
        aPackage.getDebugger().addObject(aPackage.getId(), objectWrapper.getName(), objectWrapper.getObject());
    }


    /**
     * Return the name of this object on the object bench.
     *
     * @return    The instance name if the object can be put into bench, null othervise
     */
    public String getInstanceName()
    {
        if (objectWrapper == null) {
            return null;
        }

        return objectWrapper.getName();
    }


    /**
     * Return the class of this object.
     * Similar to Reflection API. Note the naming inconsistency, which
     * avoids a clash with <code>java.lang.Object.getClass()</code>
     *
     * @return                           The bClass value
     * @throws  ProjectNotOpenException  if the project to which this object belongs has been closed by the user.
     * @throws  ClassNotFoundException   if the class has been deleted by the user.
     * @throws  PackageNotFoundException if the Package has been deleted by the user.
     */
    public BClass getBClass()
             throws ProjectNotOpenException, PackageNotFoundException, ClassNotFoundException
    {
        // BClasses are retrieved from the BlueJ classTarget
        ClassTarget classTarget = wrapperId.getClassTarget();
        
        if (classTarget == null) {
            // Not a project class; exists in a library or the Java runtime
            wrapperId.getJavaClass(); // will throw ClassNotFoundException if not loadable
            return BClass.getBClass(wrapperId);
        }
        
        // There is only one instance of BClass for each ClassTarget
        return classTarget.getBClass();
    }


    /**
     * Returns the underlying BlueJ package.
     * Should remain visible only to package members.
     *
     * @return                            The packageFrame value
     * @throws  ProjectNotOpenException   if the project to which this object belongs has been closed by the user.
     * @throws  PackageNotFoundException  if the package to which this object belongs has been deleted by the user.
     */
    PkgMgrFrame getPackageFrame()
             throws ProjectNotOpenException, PackageNotFoundException
    {
        return wrapperId.getPackageFrame();
    }


    /**
     * Returns the object wrapper to be used by the invoke on methods
     *
     * @return    The objectWrapper value
     */
    ObjectWrapper getObjectWrapper()
    {
        return objectWrapper;
    }


    /**
     * Used by BField to get hold of the real Object
     *
     * @return    The objectReference value
     */
    ObjectReference getObjectReference()
    {
        if (objectWrapper == null) {
            return null;
        }
        DebuggerObject obj = objectWrapper.getObject();

        if (obj == null) {
            return null;
        }
        return obj.getObjectReference();
    }


    /**
     * Returns a string representation of the Object
     *
     * @return    Description of the Return Value
     */
    public String toString()
    {
        String className = "";
        if (objectWrapper != null) {
            className = objectWrapper.getClassName();
        }

        return "BObject instanceName=" + getInstanceName() + " Class Name=" + className;
    }


    private static HashMap<String,String> primiMap;

    static {
        // This will be executed once when this class is loaded
        primiMap = new HashMap<String,String>();
        primiMap.put("boolean", "Z");
        primiMap.put("byte", "B");
        primiMap.put("short", "S");
        primiMap.put("char", "C");
        primiMap.put("int", "I");
        primiMap.put("long", "J");
        primiMap.put("float", "F");
        primiMap.put("double", "D");
    }


    /**
     * Needed to convert java style class names to classloaded class names.
     * From: java.lang.String[]
     * To:   [Ljava.lang.String;
     *
     * @param  javaStyle  Description of the Parameter
     * @return            Description of the Return Value
     */
    private String transJavaToClass(String javaStyle)
    {
        String className = javaStyle;

        int arrayCount = 0;
        while (className.endsWith("[]")) {
            // Counts how may arrays are in this class name
            arrayCount++;
            className = className.substring(0, className.length() - 2);
        }

        // No array around, nothing to do.
        if (arrayCount <= 0) {
            return className;
        }

        String replace = (String) primiMap.get(className);

        // If I can substitute the name I will do it
        if (replace != null) {
            className = replace;
        }
        else {
            className = "L" + className + ";";
        }

        while (arrayCount-- > 0) {
            className = "[" + className;
        }

        return className;
    }
}
