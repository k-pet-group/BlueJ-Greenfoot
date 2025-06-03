/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2011,2014,2016,2017,2019,2020,2023  Michael Kolling and John Rosenberg
 
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
package bluej.pkgmgr.target.role;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A role object for Kotlin file facades (files with top-level functions).
 * 
 * @author Vitaly Bragilevsky
 */
public class KotlinFileFacadeRole extends ClassRole
{
    public static final String KOTLIN_FILE_FACADE_ROLE_NAME = "KotlinFileFacadeTarget";
    
    /**
     * Create the class role.
     */
    public KotlinFileFacadeRole()
    {
    }

    @OnThread(Tag.Any)
    public String getRoleName()
    {
        return KOTLIN_FILE_FACADE_ROLE_NAME;
    }
    
    /**
     * Get the stereotype label for this class role.
     * For Kotlin file facades, this is "functions".
     */
    @Override
    @OnThread(Tag.Any)
    public String getStereotypeLabel()
    {
        return "functions";
    }
    
    @Override
    @OnThread(Tag.Any)
    public boolean canConvertToStride()
    {
        return false;
    }
}