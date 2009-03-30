/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009  Poul Henriksen and Michael Kolling 
 
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

import greenfoot.gui.FirstStartupDialog;
import greenfoot.gui.FirstStartupDialog.Result;
import greenfoot.util.FileChoosers;
import greenfoot.util.GreenfootUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import rmiextension.RMIExtension;
import bluej.Config;
import bluej.prefmgr.PrefMgr;
import bluej.utility.Debug;
import bluej.utility.FileUtility;
import bluej.utility.Utility;

/**
 * This singleton is responsible for starting up greenfoot from the BlueJ VM.
 * 
 * @author Poul Henriksen
 * @version $id:$
 */
public class GreenfootLauncherBlueJVM
{
    /** Singleton instance */
    private static GreenfootLauncherBlueJVM instance;

    /** Hook into BlueJ*/
    private RMIExtension extension;

    /** The project to start up if no other project is opened. */
    private final static String STARTUP_PROJECT = "greenfoot/startupProject";

    /** The tutorial project. TODO Test on mac and win */
    private final static String TUTORIAL_SCENARIO = "wombats";

    /** The HTML tutorial. Relative to the top level greenfoot dir. */
    private static final String TUTORIAL_FILE = "tutorial/tutorial.html";

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
     * @param extension The extension that allows ?s to do BlueJ stuff
     */
    public void launch(RMIExtension extension)
    {
        this.extension = extension;

        // First, we check if this is the first run of greenfoot ever for this user
        if (Utility.firstTimeEver("greenfoot.run")) {
            handleFirstTime();
            return;
        }
        else {
            openNormally();
        }
    }
    
    /**
     * Displays a dialog to the first time user of greenfoot.
     */
    private void handleFirstTime()
    {
        FirstStartupDialog dialog = new FirstStartupDialog();
        dialog.setLocationRelativeTo(null); // centers dialog
        dialog.setVisible(true);

        Result result = dialog.getResult();
        switch(result) {
            case TUTORIAL :
                openTutorial();
                break;
            case OPEN :
                openScenario();
                break;
            case CREATE :
                createScenario();
                break;
            case WITHOUT :
                openNormally();
                break;
        }
    }
    

    /**
     * Opens the tutorial and the tutorial scenario.
     * 
     */
    private void openTutorial()
    {
        // So that when they can easily open other scenarios once they finish
        // with the tutorial.
        setScenariosAsDefaultDir();
        File scenarioDir = null;
        try {
            String scenarioName = Config.getPropString("greenfoot.tutorial.scenario", null);
            if (scenarioName == null) {
                scenarioName = TUTORIAL_SCENARIO;
            }
            scenarioDir = getScenarioDir(scenarioName);
        }
        catch (FileNotFoundException e) {
            Debug.reportError("Error when attempting to open tutorial scenario on first startup", e);
        }
        catch (IOException e) {
            Debug.reportError("Error when attempting to open tutorial scenario on first startup", e);
        }

        if (scenarioDir != null) {
            extension.openProject(scenarioDir);
        }

        File greenfootDir = null;
        try {
            greenfootDir = GreenfootUtil.getGreenfootDir();
        }
        catch (IOException e) {
            Debug.reportError("Error when attempting to open tutorial on first startup", e);
            return;
        }

        String alternativeTutorial = Config.getPropString("greenfoot.tutorial", null);

        if (alternativeTutorial == null) {
            File tutorial = new File(greenfootDir, TUTORIAL_FILE);
            if (tutorial.canRead()) {
                Utility.openWebBrowser(tutorial);
            }
            else {
                Debug.reportError("Error when attempting to open tutorial on first startup", new IOException());
            }
        }
        else {
            try {
                URL tutorial = new URL(alternativeTutorial);
                Utility.openWebBrowser(tutorial);
            }
            catch (MalformedURLException e) {
                Debug.reportError("Error when trying to open tutorial in alternative location: " + alternativeTutorial,
                        e);
            }
        }
    }

    /**
     * Opens a new scenario seleceted by the user. 
     *
     */
    private void openScenario()
    {
        setScenariosAsDefaultDir();
        File scenario = FileChoosers.getScenario(null);
        if (scenario != null) {
            extension.openProject(scenario);
        }
        else {
            // User didn't choose a scenario, lets exit.
            System.exit(0);
        }
    }

    /**
     * Lets the user create a new scenario.
     *
     */    
    private void createScenario()
    {
        String newName = FileUtility.getFileName(null, "New Scenario", Config.getString("pkgmgr.newPkg.buttonLabel"), false, null, true);
        File newDir = new File(newName);
        extension.newProject(newDir);
    }

    /**
     * Starts up greenfoot by either letting BlueJ launch previously opened
     * scenarios or opening the empty startup project.
     */
    public void openNormally()
    {
        // If no project is open now, we might want to open the startup project
        File blueJLibDir = Config.getBlueJLibDir();
        File startupProject = new File(blueJLibDir, STARTUP_PROJECT);
        extension.maybeOpenProject(startupProject);
    }
    
    /**
     * Sets the directory containing the scenarios to be the directory that the
     * file browser will open up in.
     * <p>
     * If it can't find the scenarios dir, it will do nothing.
     * 
     */
    private void setScenariosAsDefaultDir()
    {
        // Attempt to set scenarios dir as default.
        try {
            File startupDir = getScenariosDir();
            PrefMgr.setProjectDirectory(startupDir.getAbsolutePath());
        }
        catch (IOException e) {
            // Not a problem, we use default dir
        }
    }
    
    /**
     * Returns the location of the scenarios.
     * 
     * @throws IOException If it can't read the greenfoot dir.
     */
    private File getScenariosDir()
        throws IOException
    {
        // The scenarios might be in a different location.
        // This is useful when running greenfoot from an IDE.
        String alternativeScenarios = Config.getPropString("greenfoot.scenarios", null);
        if (alternativeScenarios == null) {
            File greenfootDir = GreenfootUtil.getGreenfootDir();
            return new File(greenfootDir, "scenarios");
        }
        else {
            return new File(alternativeScenarios);
        }
    }

    /**
     * Try to find the specific scenario in the greenfoot scenario dir.
     * 
     * @throws FileNotFoundException If it could not find the scenario.
     * @throws IOException If it can't read the greenfoot dir.
     */
    private File getScenarioDir(String scenario)
        throws FileNotFoundException, IOException
    {
        File scenariosDir = getScenariosDir();
        File specificScenarioDir = new File(scenariosDir, scenario);
        if (specificScenarioDir.isDirectory()) {
            return specificScenarioDir;
        }
        else {
            throw new FileNotFoundException("Scenario not found: " + scenario + ". Tried to find it at: "
                    + specificScenarioDir);
        }
    }


}
