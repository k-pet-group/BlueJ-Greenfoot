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
package bluej.groupwork.cvsnb;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.netbeans.lib.cvsclient.admin.AdminHandler;
import org.netbeans.lib.cvsclient.admin.Entry;
import org.netbeans.lib.cvsclient.command.GlobalOptions;

/**
 * An admin handler which pretends that nothing exists.
 * 
 * @author Davin McCall
 * @version $Id: EmptyAdminHandler.java 8116 2010-08-20 03:29:52Z davmac $
 */
public class EmptyAdminHandler
    implements AdminHandler
{
    public EmptyAdminHandler()
    {
        // do nothing.
    }
    
    public void updateAdminData(String localDirectory, String repositoryPath, Entry entry, GlobalOptions globalOptions)
        throws IOException
    {
    }

    public Entry getEntry(File file)
        throws IOException
    {
        return null;
    }

    @SuppressWarnings("unchecked")
    public Iterator getEntries(File directory)
        throws IOException
    {
        return Collections.EMPTY_LIST.iterator();
    }

    public void setEntry(File file, Entry entry)
        throws IOException
    {
    }

    public String getRepositoryForDirectory(String directory, String repository)
        throws IOException
    {
        return null;
    }

    public void removeEntry(File file)
        throws IOException
    {
    }

    public Set<File> getAllFiles(File directory)
        throws IOException
    {
        return new HashSet<File>();
    }

    public String getStickyTagForDirectory(File directory)
    {
        return null;
    }

    public boolean exists(File file)
    {
        return false;
    }
}
