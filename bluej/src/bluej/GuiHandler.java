/*
 This file is part of the BlueJ program. 
 Copyright (C) 2018,2019  Michael Kolling and John Rosenberg
 
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
package bluej;

import java.io.File;

import javafx.stage.Stage;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * This is an interface for handling project-related and general startup-related GUI tasks.
 * <p>
 * This interface allows projects to be managed by BlueJ or Greenfoot independently.
 *  
 * @author Davin McCall
 */
@OnThread(Tag.FXPlatform)
public interface GuiHandler
{
    /**
     * Try to open a project and display a GUI for it.
     * 
     * @param path  The path to the project
     * @param displayError  true if an error dialog should be displayed on failure
     * @return      true if successful; false if not
     */
    boolean tryOpen(File path, boolean displayError);
    
    /**
     * Handle an 'about' request issued from the OS interface (i.e. Mac application menu).
     */
    void handleAbout();
    
    /**
     * Handle a 'preferences' request issued from the OS interface.
     */
    void handlePreferences();
    
    /**
     * Handle a 'quit' request issued from the OS interface.
     */
    void handleQuit();
    
    /**
     * Initial opening of projects is complete.
     * 
     * @param projectOpen  true if a project was opened; false if no projects are open
     * @return An open main window, or null if not available due to an unexpected error. 
     */
    Stage initialOpenComplete(boolean projectOpen);
    
    /**
     * Perform any cleanup/save prior to application exit. The exit cannot be cancelled at this point.
     */
    void doExitCleanup();
}
