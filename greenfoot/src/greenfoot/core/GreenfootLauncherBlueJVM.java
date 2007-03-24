package greenfoot.core;

import greenfoot.gui.FirstStartupDialog;
import greenfoot.gui.FirstStartupDialog.Result;
import greenfoot.util.GreenfootUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.SwingUtilities;

import rmiextension.RMIExtension;
import bluej.Boot;
import bluej.Config;
import bluej.prefmgr.PrefMgr;
import bluej.utility.Debug;
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
     * <p>
     * Will be executed on the eventthread later
     * 
     */
    private void handleFirstTime()
    {
        Thread t = new Thread() {
            public void run()
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
        };
        SwingUtilities.invokeLater(t);
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
            greenfootDir = getGreenfootDir();
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
        File scenario = GreenfootUtil.getScenarioFromFileBrowser(null);
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
        String newName = GreenfootUtil.getNewProjectName(null);
        File newDir = new File(newName);
        extension.newProject(newDir);
    }
    /**
     * Starts up greenfoot by either letting BlueJ launch previous opened
     * scenarios or opening the empty startup project.
     * <p>
     * Will be executed in its own thread.
     */
    public void openNormally()
    {
        Thread t = new Thread() {
            public void run()  {
                Debug.message("Greenfoot version: " + Boot.GREENFOOT_VERSION);
                extension.waitUntilBlueJStarted();
                // If no project is open now, we might want to open the startup project
                File blueJLibDir = Config.getBlueJLibDir();
                File startupProject = new File(blueJLibDir, STARTUP_PROJECT);
                extension.maybeOpenProject(startupProject);
            }            
        };
        t.start();
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
            File greenfootDir = getGreenfootDir();
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

    /**
     * Tries to locate the top level greenfoot dir. This method takes the
     * different platforms into account. Specifically the Mac has a different
     * structure.
     * 
     * @throws IOException If it can't read the greenfoot dir.
     * 
     */
    private File getGreenfootDir()
        throws IOException
    {
        File libDir = Config.getBlueJLibDir();
        // The parent dir of the lib dir is the top level dir of greenfoot
        File greenfootDir = libDir.getParentFile();
        // But on the mac it is further back in the hierarchy.
        if (Config.isMacOS()) {
            greenfootDir = greenfootDir.getParentFile().getParentFile().getParentFile();
        }
        if (!(greenfootDir.isDirectory() && greenfootDir.canRead())) {
            throw new IOException("Could not read from greenfoot directory: " + greenfootDir);
        }
        return greenfootDir;
    }
}
