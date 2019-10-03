/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2014,2016,2018,2019  Michael Kolling and John Rosenberg
 
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

import java.io.File;
import java.io.IOException;
import java.util.*;

import javafx.stage.Window;

import threadchecker.OnThread;
import threadchecker.Tag;
import bluej.utility.DialogManager;
import bluej.utility.FileUtility;

/**
 * Utilities for teamwork functionality.
 */
public class TeamUtils
{
    /**
     * Handle a server response in an appropriate fashion, i.e. if the response
     * indicates an error, then display an error dialog.
     * 
     * Call on the AWT event handling thread.
     *
     */
    @OnThread(Tag.FXPlatform)
    public static void handleServerResponseFX(TeamworkCommandResult result, final Window window)
    {
        if (result != null)
        {
            if (result.wasAuthFailure())
            {
                DialogManager.showErrorFX(window, "team-authentication-problem");
            }
            else if (result.isError() && ! result.wasAborted())
            {
                DialogManager.showErrorTextFX(window, result.getErrorMessage());
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
}
