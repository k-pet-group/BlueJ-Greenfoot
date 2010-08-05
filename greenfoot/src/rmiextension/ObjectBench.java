/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2010  Poul Henriksen and Michael Kolling 
 
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

import bluej.debugmgr.ResultWatcher;
import bluej.extensions.BPackage;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;

/**
 * Creates and removes objects on the object bench.
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 */
public class ObjectBench
{
    /**
     * Creates a new object, and puts it on the object bench. The given ResultWatcher
     * will be notified of the result (on the AWT event thread).
     */
    public static void createObject(BPackage pkg, String className,
            String instanceName, String[] constructorParams,
            ResultWatcher watcher)
    {
        try {
            ConstructorInvoker launcher = new ConstructorInvoker(pkg, className);
            launcher.invokeConstructor(instanceName, constructorParams, watcher);
        }
        catch (ProjectNotOpenException e) {
            throw new RuntimeException("Project not open (any longer)");
        }
        catch (PackageNotFoundException e) {
            throw new RuntimeException("Package not found");
        }
    }
}
