/*
 This file is part of the BlueJ program. 
 Copyright (C) 2012,2014  Michael Kolling and John Rosenberg 
 
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
package bluej.collect;

import java.io.File;

import bluej.collect.CollectUtility.ProjectDetails;

//package-visible
class FileKey
{
    private File projDir;
    private String file;
    
    public FileKey(ProjectDetails proj, String path)
    {
        this.projDir = proj.projectDir;
        this.file = path;
    }
    
    //Eclipse-generated hashCode and equals methods:
    
    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((file == null) ? 0 : file.hashCode());
        result = prime * result
                + ((projDir == null) ? 0 : projDir.hashCode());
        return result;
    }
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        FileKey other = (FileKey) obj;
        if (file == null) {
            if (other.file != null)
                return false;
        }
        else if (!file.equals(other.file))
            return false;
        if (projDir == null) {
            if (other.projDir != null)
                return false;
        }
        else if (!projDir.equals(other.projDir))
            return false;
        return true;
    }
}