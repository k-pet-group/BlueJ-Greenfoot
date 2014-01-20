/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010,2014  Poul Henriksen and Michael Kolling 
 
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
package greenfoot.core;

import java.io.File;

import rmiextension.RMIExtension;
import bluej.Config;

/**
 * This singleton is responsible for starting up Greenfoot from the BlueJ VM.
 * 
 * @author Poul Henriksen
 */
public class GreenfootLauncherBlueJVM
{
    /** Singleton instance */
    private static GreenfootLauncherBlueJVM instance;

    /** Hook into BlueJ*/
    private RMIExtension extension;

    /** The project to start up if no other project is opened. */
    private final static String STARTUP_PROJECT = "greenfoot/startupProject";

    /**
     * Returns the instance of this singleton.
     */
    public static GreenfootLauncherBlueJVM getInstance()
    {
        if (instance == null) {
            instance = new GreenfootLauncherBlueJVM();
        }
        return instance;
    }

    /**
     * Launch greenfoot on the BlueJVM side.
     * 
     * @param extension The extension instance
     */
    public void launch(RMIExtension extension)
    {
        this.extension = extension;
        openNormally();
    }
    
    /**
     * Starts up Greenfoot by either letting BlueJ launch previously opened
     * scenarios or opening the empty startup project.
     */
    public void openNormally()
    {
        // If no project is open now, we might want to open the startup project
        File blueJLibDir = Config.getBlueJLibDir();
        File startupProject = new File(blueJLibDir, STARTUP_PROJECT);
        extension.maybeOpenProject(startupProject);
    }
}
