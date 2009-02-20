/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009  Poul Henriksen and Michael Kšlling 
 
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

import bluej.debugger.Debugger;
import bluej.debugger.DebuggerObject;
import bluej.debugmgr.objectbench.ObjectBench;
import bluej.debugmgr.objectbench.ObjectWrapper;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.views.View;

/**
 * 
 * This is an Invoker that can instantiate objects of classes that are NOT a part
 * of a project.
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 */
public class ConstructorInvoker {
    private PkgMgrFrame pkgFrame;
    private BProject prj;

    private View view;

    public ConstructorInvoker(BPackage bPackage, String className)
        throws ProjectNotOpenException, PackageNotFoundException {
        pkgFrame = (PkgMgrFrame) bPackage.getFrame();
        prj = bPackage.getProject();
        Class launcherClass = null;
        try {
            launcherClass = getClass(className);
        } catch (java.lang.ClassNotFoundException e) {
            e.printStackTrace();
        }
        view = View.getView(launcherClass);
    }
   
    
    /**
     * Invoke a constructor which takes String arguments only.
     * 
     * @param instanceNameOnObjectBench  Name of the created object as it
     *                                   should appear on the bench
     * @param args  Arguments to supply to the constructor
     * @return  The newly created object
     * 
     * @throws InvocationArgumentException
     * @throws InvocationErrorException
     */
    public ObjectWrapper invokeConstructor(String instanceNameOnObjectBench, String[] args)
        throws InvocationArgumentException, InvocationErrorException
    {
        ObjectBench objBench = pkgFrame.getObjectBench();
        Package pkg = pkgFrame.getPackage();    
        
        Debugger debugger = pkgFrame.getProject().getDebugger();
        String [] argTypes = new String[args.length];
        DebuggerObject [] argObjects = new DebuggerObject[args.length];
        for (int i = 0; i < args.length; i++) {
            argTypes[i] = "java.lang.String";
            argObjects[i] = debugger.getMirror(args[i]);
        }
        
        DebuggerObject debugObject = debugger.instantiateClass(view.getQualifiedName(),
                argTypes, argObjects).getResultObject();
        
        ObjectWrapper wrapper = ObjectWrapper.getWrapper(
                pkgFrame, objBench,
                debugObject,
                debugObject.getGenType(),
                instanceNameOnObjectBench);       
        
        objBench.addObject(wrapper);        
        pkg.getDebugger().addObject(pkg.getQualifiedName(), wrapper.getName(), debugObject);  
        
        return wrapper;         
    }

    private Class getClass(String fullClassname)
        throws java.lang.ClassNotFoundException {
        Class cls = null;
        try {
            cls = prj.getClassLoader().loadClass(fullClassname);
        }
        catch (ProjectNotOpenException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return cls;
    }
}
