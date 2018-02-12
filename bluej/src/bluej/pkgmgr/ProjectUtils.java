/*
 This file is part of the BlueJ program. 
 Copyright (C) 2018  Michael Kolling and John Rosenberg 
 
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

import bluej.debugger.Debugger;
import bluej.utility.DialogManager;
import javafx.stage.Stage;

/**
 * Utilities for working with projects.
 * 
 * @author Davin McCall
 */
public class ProjectUtils
{
    /**
     * Check the debugger state is suitable for execution: that is, it is not already
     * executing anything or stuck at a breakpoint.
     * 
     * <P>Returns true if the debugger is currently idle, or false if it is already
     * executing, in which case an error dialog is also displayed.
     * 
     * @param project the project to check execution state for.
     * @param msgWindow the parent window for any error dialogs.
     * @return true if the debugger is currently idle.
     */
    public static boolean checkDebuggerState(Project project, Stage msgWindow)
    {
        Debugger debugger = project.getDebugger();
        if (debugger.getStatus() == Debugger.SUSPENDED)
        {
            DialogManager.showErrorFX(msgWindow, "stuck-at-breakpoint");
            return false;
        }
        else if (debugger.getStatus() == Debugger.RUNNING)
        {
            DialogManager.showErrorFX(msgWindow, "already-executing");
            return false;
        }
        
        return true;
    }
}
