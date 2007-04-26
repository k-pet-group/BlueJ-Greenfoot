package greenfoot.export;

import greenfoot.World;
import greenfoot.core.LocationTracker;
import greenfoot.core.ProjectProperties;
import greenfoot.core.Simulation;
import greenfoot.core.WorldHandler;
import greenfoot.event.SimulationEvent;
import greenfoot.event.SimulationListener;
import greenfoot.event.WorldEvent;
import greenfoot.gui.CenterLayout;
import greenfoot.gui.ControlPanel;
import greenfoot.gui.DragGlassPane;
import greenfoot.gui.WorldCanvas;
import greenfoot.platforms.standalone.ActorDelegateStandAlone;
import greenfoot.platforms.standalone.GreenfootUtilDelegateStandAlone;
import greenfoot.platforms.standalone.WorldHandlerDelegateStandAlone;
import greenfoot.util.GreenfootUtil;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FocusTraversalPolicy;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.JApplet;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.RootPaneContainer;

/**
 * This class can view and run a greenfoot scenario. It is not possible to
 * interact with the objects in any way.
 * 
 * @author Poul Henriksen
 * 
 */
public class GreenfootScenarioViewer extends JApplet
{

    private static final int EMPTY_BORDER_SIZE = 5;

    private static String scenarioName;

    private ProjectProperties properties;
    private Simulation sim;
    private WorldCanvas canvas;
    private ControlPanel controls;
    private RootPaneContainer rootPaneContainer;

    private Constructor worldConstructor;

    /**
     * Start the scenario.
     * <p>
     * 
     * BlueJ and the scenario MUST be on the classpath.
     * 
     * @param args One argument can be passed to this method. The first one
     *            should be the World to be instantiated. If no arguments are
     *            supplied it will read from the properties file. And if that
     *            can't be found either it will use AntWorld.
     * 
     */
    public static void main(String[] args)
    {
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        JFrame frame = new JFrame(scenarioName);
        GreenfootScenarioViewer gs = new GreenfootScenarioViewer(frame);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setTitle(scenarioName);
        frame.pack();
        frame.setVisible(true);
    }

    public GreenfootScenarioViewer()
    {

    }

    public GreenfootScenarioViewer(RootPaneContainer rootPane)
    {
        rootPaneContainer = rootPane;
        init();
    }

    /**
     * Returns the size of the borders around the controls.
     * 
     */
    public static Dimension getControlsBorderSize() {
        return new Dimension((EMPTY_BORDER_SIZE ) * 2, (EMPTY_BORDER_SIZE ) * 2);
    } 
    /**
     * Returns the size of the borders around the world panel.
     * 
     */
    public static Dimension getWorldBorderSize() {
        return new Dimension((EMPTY_BORDER_SIZE + 1) * 2, EMPTY_BORDER_SIZE + 1 * 2);
    }
    
    private void buildGUI()
    {
        if (rootPaneContainer == null) {
            // it will be null when running as applet, so set it to the applet.
            rootPaneContainer = this;
        }

        rootPaneContainer.setGlassPane(DragGlassPane.getInstance());
        JPanel centerPanel = new JPanel(new CenterLayout());
        centerPanel.add(canvas);
        canvas.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        
        centerPanel.setBorder( BorderFactory.createEmptyBorder(EMPTY_BORDER_SIZE,EMPTY_BORDER_SIZE,EMPTY_BORDER_SIZE,EMPTY_BORDER_SIZE)); 
        controls.setBorder(BorderFactory.createCompoundBorder( BorderFactory.createEmptyBorder(0,EMPTY_BORDER_SIZE,EMPTY_BORDER_SIZE,EMPTY_BORDER_SIZE), BorderFactory.createEtchedBorder()));
        rootPaneContainer.getContentPane().add(centerPanel, BorderLayout.CENTER);
        rootPaneContainer.getContentPane().add(controls, BorderLayout.SOUTH);

    }

    /**
     * Called by the browser or applet viewer to inform this JApplet that it has
     * been loaded into the system. It is always called before the first time
     * that the start method is called.
     */
    public void init()
    {
        // this is a workaround for a security conflict with some browsers
        // including some versions of Netscape & Internet Explorer which do
        // not allow access to the AWT system event queue which JApplets do
        // on startup to check access. May not be necessary with your browser.
        JRootPane rootPane = this.getRootPane();
        rootPane.putClientProperty("defeatSystemEventQueueCheck", Boolean.TRUE);

        String worldClassName = null; 
        boolean includeExtraControls = false;
        Properties p = new Properties();
        try {
            ClassLoader loader = GreenfootScenarioViewer.class.getClassLoader();
            InputStream is = loader.getResourceAsStream("standalone.properties");

            p.load(is);
            worldClassName = p.getProperty("main.class");
            scenarioName = p.getProperty("project.name");
            includeExtraControls = Boolean.parseBoolean(p.getProperty("controls.extra"));
            is.close();
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        try {

            GreenfootUtil.initialise(new GreenfootUtilDelegateStandAlone());
            properties = new ProjectProperties();

            ActorDelegateStandAlone.setupAsActorDelegate();
            ActorDelegateStandAlone.initProperties(properties);

            canvas = new WorldCanvas(null);

            WorldHandler.initialise(canvas, new WorldHandlerDelegateStandAlone(this));
            WorldHandler worldHandler = WorldHandler.getInstance();
            Simulation.initialize(worldHandler);
            LocationTracker.initialize();
            sim = Simulation.getInstance();
            controls = new ControlPanel(sim, includeExtraControls);

            sim.addSimulationListener(new SimulationListener() {
                public void simulationChanged(SimulationEvent e)
                {
                    // If the simulation starts, try to transfer keyboard
                    // focus to the world canvas to allow control of Actors
                    // via the keyboard
                    if (e.getType() == SimulationEvent.STARTED) {
                        canvas.requestFocusInWindow();
                    }
                }
            });
            
            try {
                int initialSpeed = properties.getInt("simulation.speed");
                sim.setSpeed(initialSpeed);
            } catch (NumberFormatException nfe) {
                // If there is no speed info in the properties we don't care...
            }
            Class<?> worldClass = Class.forName(worldClassName);
            worldConstructor = worldClass.getConstructor(new Class[]{});
            instantiateNewWorld();
        }
        catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (NoSuchMethodException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    

        buildGUI();

    }


    /**
     * Called by the browser or applet viewer to inform this JApplet that it
     * should start its execution. It is called after the init method and each
     * time the JApplet is revisited in a Web page.
     */
    public void start()
    {
    // provide any code requred to run each time
    // web page is visited
    }

    /**
     * Called by the browser or applet viewer to inform this JApplet that it
     * should stop its execution. It is called when the Web page that contains
     * this JApplet has been replaced by another page, and also just before the
     * JApplet is to be destroyed.
     */
    public void stop()
    {
    // provide any code that needs to be run when page
    // is replaced by another page or before JApplet is destroyed
    }

    /**
     * Called by the browser or applet viewer to inform this JApplet that it is
     * being reclaimed and that it should destroy any resources that it has
     * allocated. The stop method will always be called before destroy.
     */
    public void destroy()
    {
    // provide code to be run when JApplet is about to be destroyed.
    }

    /**
     * Returns information about this applet. An applet should override this
     * method to return a String containing information about the author,
     * version, and copyright of the JApplet.
     * 
     * @return a String representation of information about this JApplet
     */
    public String getAppletInfo()
    {
        // provide information about the applet
        return "Applet generated with Greenfoot (www.greenfoot.org). Scenario name: " + scenarioName; //"Title:   \nAuthor:   \nA simple applet example description. ";
    }

    /**
     * Returns parameter information about this JApplet. Returns information
     * about the parameters than are understood by this JApplet. An applet
     * should override this method to return an array of Strings describing
     * these parameters. Each element of the array should be a set of three
     * Strings containing the name, the type, and a description.
     * 
     * @return a String[] representation of parameter information about this
     *         JApplet
     */
    public String[][] getParameterInfo()
    {
        // provide parameter information about the applet
        String paramInfo[][] = {};
        /*
         * {"firstParameter", "1-10", "description of first parameter"},
         * {"status", "boolean", "description of second parameter"}, {"images",
         * "url", "description of third parameter"} };
         */
        return paramInfo;
    }

    
    /**
     * Creates a new instance of the world. And initialises with that world.
     * @return The new world or null if failed to create new world. 
     */
    public World instantiateNewWorld() 
    {
        World world = null;
        try {
            world = (World) worldConstructor.newInstance(new Object[]{});            
            WorldHandler.getInstance().setWorld(world);
        }
        catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (InstantiationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            e.getCause().printStackTrace();
        }
        return world;
    }

}
