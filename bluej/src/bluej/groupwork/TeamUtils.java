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
package bluej.groupwork;

import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import bluej.utility.DialogManager;
import bluej.utility.FileUtility;

public class TeamUtils
{
    /**
     * Handle a server response in an appropriate fashion, i.e. if the response
     * indicates an error, then display an error dialog.
     * 
     * Call on the AWT event handling thread.
     * 
     * @param basicServerResponse  The response to handle
     */
    public static void handleServerResponse(TeamworkCommandResult result, final Window window)
    {
        if (result != null) {
            if (result.wasAuthFailure()) {
                DialogManager.showError(window, "team-authentication-problem");
            }
            else if (result.isError() && ! result.wasAborted()) {
                String message = result.getErrorMessage();
                DialogManager.showErrorText(window, message);
            }
        }
    }
    
    /**
     * From a set of File objects, remove those files which should be treated as
     * binary files (and put them in a new set). 
     */
    public static Set<File> extractBinaryFilesFromSet(Set<File> files)
    {
        Set<File> binFiles = new HashSet<File>();
        Iterator<File> i = files.iterator();
        while (i.hasNext()) {
            File f = i.next();
            if (f.isDirectory()) {
                continue;
            }
            String fname = f.getName();
            if (! fname.endsWith(".txt") && ! fname.endsWith(".java")) {
                binFiles.add(f);
                i.remove();
            }
        }
        return binFiles;
    }

    /**
     * Backup a set of files, returning a map from the original file name to
     * the backup name. The backup files are created in the system's temp
     * folder/directory.
     */
    public static Map<File,File> backupFiles(Set<File> files) throws IOException
    {
        Map<File,File> rmap = new HashMap<File,File>();
        for (Iterator<File> i = files.iterator(); i.hasNext(); ) {
            File tempFile = File.createTempFile("bluejvcs", null);
            File srcFile = i.next();
            FileUtility.copyFile(srcFile, tempFile);
            rmap.put(srcFile, tempFile);
        }
        return rmap;
    }
    
    /**
     * Copy a set of files, then delete the source files. This is used to
     * restore a backup created by the backupFiles() method. The source
     * files (i.e. the backup files) are deleted afterwards.
     */
    public static void restoreBackups(Map<File,File> rmap) throws IOException
    {
        Iterator<Map.Entry<File,File>> i = rmap.entrySet().iterator();
        while (i.hasNext()) {
            Map.Entry<File,File> entry = i.next();
            File orig = entry.getKey();
            File backup = entry.getValue();
            FileUtility.copyFile(backup, orig);
            backup.delete();
        }
    }
}
