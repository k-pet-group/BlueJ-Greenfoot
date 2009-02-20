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

import bluej.extensions.BPackage;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;

/**
 * Represents a package in BlueJ. It is called a project because greenfoot does
 * not support packages.
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: Project.java 6170 2009-02-20 13:29:34Z polle $
 */
public class Project
{

    private BPackage pkg;

    public Project(BPackage pkg)
    {
        this.pkg = pkg;
    }

    public BPackage getPackage()
    {

        return pkg;
    }

    /**
     * @return
     */
    public String getDir()
    {

        try {
            return pkg.getProject().getDir().toString();
        }
        catch (ProjectNotOpenException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Get the name of the package.
     * @return  The package name
     */
    public String getName()
    {
        try {
            return pkg.getName();
        }
        catch (ProjectNotOpenException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (PackageNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

}