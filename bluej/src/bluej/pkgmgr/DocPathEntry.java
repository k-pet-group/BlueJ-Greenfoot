/*
 This file is part of the BlueJ program. 
 Copyright (C) 2010  Michael Kolling and John Rosenberg 
 
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
package bluej.pkgmgr;

import java.io.File;

/**
 * Represents an entry on the Javadoc search path. This can include both a jar/zip file/directory,
 * and a path prefix which specifies where the source root within the jar/zip/directory really is.
 * 
 * @author Davin McCall
 */
public class DocPathEntry
{
    private File file;
    private String pathPrefix;
    
    /**
     * Create a new DocPathEntry, for the given file (either a jar, zip, or a directory), and
     * with sources in the given prefix.
     */
    public DocPathEntry(File file, String pathPrefix)
    {
        this.file = file;
        this.pathPrefix = pathPrefix;
    }
    
    public File getFile()
    {
        return file;
    }
    
    public String getPathPrefix()
    {
        return pathPrefix;
    }
}
