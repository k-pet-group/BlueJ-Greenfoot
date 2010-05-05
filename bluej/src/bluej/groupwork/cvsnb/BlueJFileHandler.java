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
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.netbeans.lib.cvsclient.file.DefaultFileHandler;

/**
 * A file handler which captures file rename operations requested by the server.
 * Renames are used to make backups of conflicting files; if we capture them
 * we can give the user the option of keeping one or the other (the local
 * version, or the repository version).
 * 
 * @author Davin McCall
 */
public class BlueJFileHandler extends DefaultFileHandler 
{
    /** Map a file name to it's backed-up local version */
    private Map<File,File> conflicts = new HashMap<File,File>();
    
    private boolean ignoreNextConflict = false;
    
    /**
     * Inform the file handler that the next conflict is a non-binary
     * conflict (it doesn't need to be tracked).
     */
    public void nextConflictNonBinary()
    {
        ignoreNextConflict = true;
    }
    
    /**
     * Get the conflicts map. This is a map (File to File) which maps the
     * original file name to the backup file name for each file for which
     * a backup was created.
     */
    public Map<File,File> getConflicts()
    {
        return conflicts;
    }
    
    public void renameLocalFile(String pathname, String newName)
        throws IOException
    {
        File path = new File(pathname);
        File parent = path.getParentFile();
        File backup = new File(parent, newName);
        
        // The backup shouldn't exist; the cvs library explicitly deletes it if
        // it does, before calling this method. But we'll check for safety.
        if (! backup.exists()) {
            if (! ignoreNextConflict) {
                conflicts.put(path, backup);
            }
            super.renameLocalFile(pathname, newName);
        }
        
        ignoreNextConflict = false;
    }
}
