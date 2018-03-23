/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010,2011,2013,2014,2015,2016  Poul Henriksen and Michael Kolling 
 
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
package greenfoot.core;

import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;
import bluej.extensions.SourceType;
import bluej.utility.Debug;
import greenfoot.World;
import greenfoot.util.GreenfootUtil;
import rmiextension.wrappers.RClass;
import rmiextension.wrappers.RPackage;

import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Represents a package in Greenfoot.
 * 
 * <p>A GPackage is essentially a reference to a remote package (RPackage), together
 * with a pool of GClass objects representing the classes in the package. 
 * 
 * @author Poul Henriksen
 */
public class GPackage
{
    private RPackage pkg;
    private GProject project; 
        
    /**
     * Contructor for an unspecified package, but for which a project is known.
     * Used to allow a class to not be part of a package, but still being able
     * to get the project the class is part of.
     */
    GPackage(GProject project) 
    {
        if(project == null) {
            throw new NullPointerException("Project must not be null.");
        }
        this.project = project;
    }
    
    /**
     * Construct a new GPackage; this should generally only be called by
     * GProject.
     * 
     * @param pkg  The reference to the remote package
     * @param project  The project
     */
    public GPackage(RPackage pkg, GProject project)
    {
        if(pkg == null) {
            throw new NullPointerException("Pkg must not be null.");
        }
        if(project == null) {
            throw new NullPointerException("Project must not be null.");
        }
        this.pkg = pkg;
        this.project = project;
    }

    public GProject getProject()
    {
        return project;
    }
}
