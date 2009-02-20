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
package rmiextension;

import bluej.extensions.BObject;
import bluej.extensions.ConstructorInvoker;
import bluej.extensions.InvocationArgumentException;
import bluej.extensions.InvocationErrorException;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;

/**
 * Creates and removes objects on the object bench.
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: ObjectBench.java 6170 2009-02-20 13:29:34Z polle $
 */
public class ObjectBench
{
    /**
     * Creates a new object, and puts it on the objectbench
     * 
     * @param prj
     * @param className
     * @param instanceName
     */
    public static void createObject(Project prj, String className, String instanceName)
    {
        try {
            ConstructorInvoker invoker = new ConstructorInvoker(prj.getPackage(), className); 
            invoker.invokeConstructor(instanceName, new String[] {});
        }
        catch (InvocationArgumentException e) {
            e.printStackTrace();
        }
        catch (InvocationErrorException e) {
            e.printStackTrace();
        }
        catch (ProjectNotOpenException e) {
            e.printStackTrace();
        }
        catch (PackageNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a new object, and puts it on the objectbench
     * 
     * @throws InvocationErrorException 
     * @throws InvocationArgumentException 
     */
    public static void createObject(Project prj, String className, String instanceName, String[] constructorParams) throws InvocationArgumentException, InvocationErrorException
    {
        try {
            ConstructorInvoker launcher = new ConstructorInvoker(prj.getPackage(), className);
            launcher.invokeConstructor(instanceName, constructorParams);
        }
        catch (ProjectNotOpenException e) {
            e.printStackTrace();
        }
        catch (PackageNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Removes the object from the object bench
     * 
     * @param prj
     *            The project
     * @param string
     *            The name of the instance
     */
    public static void removeObject(Project prj, String instanceName)
    {
        try {
            BObject existing = prj.getPackage().getObject(instanceName);
            if (existing != null) {
                existing.removeFromBench();
            }
        }
        catch (ProjectNotOpenException e) {
            e.printStackTrace();
        }
        catch (PackageNotFoundException e) {
            e.printStackTrace();
        }
    }

}