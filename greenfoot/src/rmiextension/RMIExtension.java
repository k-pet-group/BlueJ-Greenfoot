/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010,2011,2013  Poul Henriksen and Michael Kolling 
 
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
package rmiextension;

import greenfoot.core.GreenfootLauncherBlueJVM;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import bluej.Config;
import bluej.Main;
import bluej.debugger.jdi.NetworkTest;
import bluej.extensions.BProject;
import bluej.extensions.BlueJ;
import bluej.extensions.Extension;
import bluej.extensions.event.ApplicationEvent;
import bluej.extensions.event.ApplicationListener;
import bluej.utility.Debug;

/**
 * This is the starting point of Greenfoot as a BlueJ Extension.
 * 
 * @author Poul Henriksen
 */
public class RMIExtension extends Extension implements ApplicationListener
{
    private BlueJ theBlueJ;

    /**
     * When this method is called, the extension may start its work.
     */
    public void startup(BlueJ bluej)
    {
        theBlueJ = bluej;
        ProjectManager.init(bluej);

        try {
            new BlueJRMIServer(theBlueJ);
        }
        catch (IOException e) {
            Debug.reportError("Could not launch RMI server", e);
            NetworkTest.doTest();
            ProjectManager.greenfootLaunchFailed(null);
        }

        theBlueJ.addApplicationListener(this);
    }

    /**
     * Opens a project in BlueJ if no other projects are open.
     * 
     * @param projectPath path of the project to open.
     */
    public void maybeOpenProject(File projectPath)
    {
        // Now we need to find out if a greenfoot project is automatically
        // opening. If not we must open the dummy project.
        boolean openOrphans = "true".equals(Config.getPropString("bluej.autoOpenLastProject"));
        if (!openOrphans || !Main.hadOrphanPackages()) {
            if (theBlueJ.getOpenProjects().length == 0) {
                openProject(projectPath);
            }
        }
    }

    /**
     * Opens a project in BlueJ
     * 
     * @param projectPath path of the project to open.
     */
    public void openProject(File projectPath)
    {
        BProject project = theBlueJ.openProject(projectPath);
        if (project == null) {
            Debug.reportError("Could not open scenario: " + projectPath);
        }
    }

    /**
     * Creates a new project in BlueJ
     * 
     * @param projectPath path of the project to open.
     */
    public void newProject(File projectPath)
    {
        ProjectManager.instance().addNewProject(projectPath);
        BProject project = theBlueJ.newProject(projectPath);
        if (project == null) {
            Debug.reportError("Could not open scenario: " + projectPath);
        }
        ProjectManager.instance().removeNewProject(projectPath);
    }

    /**
     * This method must decide if this Extension is compatible with the current
     * release of the BlueJ Extensions API
     */
    public boolean isCompatible()
    {
        return Config.isGreenfoot();
    }

    /**
     * Returns the version number of this extension
     */
    public String getVersion()
    {
        return ("2003.03");
    }

    /**
     * Returns the user-visible name of this extension
     */
    public String getName()
    {
        return ("greenfoot Extension");
    }

    @Override
    public String getDescription()
    {
        return ("greenfoot extension");
    }

    /**
     * Returns a URL where you can find info on this extension. The real problem
     * is making sure that the link will still be alive in three years...
     */
    @Override
    public URL getURL()
    {
        try {
            return new URL("http://www.greenfoot.org");
        }
        catch (MalformedURLException e) {
            return null;
        }
    }
    
    // ------------- ApplicationListener interface ------------
    
    public void blueJReady(ApplicationEvent event)
    {
        GreenfootLauncherBlueJVM.getInstance().launch(this);
    }
}
