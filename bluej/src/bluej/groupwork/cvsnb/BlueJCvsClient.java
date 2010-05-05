/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
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
import java.util.Map;

import org.netbeans.lib.cvsclient.Client;
import org.netbeans.lib.cvsclient.admin.AdminHandler;
import org.netbeans.lib.cvsclient.connection.Connection;

/**
 * Provide some additional mechanism over the standard CVS library "Client" class.
 * Specifically we need to track binary conflicts.
 * 
 * @author Davin McCall
 */
public class BlueJCvsClient extends Client
{
    BlueJFileHandler fileHandler;
    
    public BlueJCvsClient(Connection connection, AdminHandler adminHandler)
    {
        super(connection, adminHandler);
        fileHandler = new BlueJFileHandler();
        setUncompressedFileHandler(fileHandler);
    }
    
    /**
     * Get the map of conflicting files. The return maps (File to File) the
     * original file name (repository version) to its backup (local version).
     */
    public Map<File,File> getConflictFiles()
    {
        return fileHandler.getConflicts();
    }
    
    /**
     * Inform the BlueJCvsClient that the next conflict detected is a non-binary
     * conflict.
     */
    public void nextConflictNonBinary()
    {
        fileHandler.nextConflictNonBinary();
    }
}
