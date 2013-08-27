/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010,2011,2012,2013  Poul Henriksen and Michael Kolling 
 
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
package greenfoot.export;

import greenfoot.World;
import greenfoot.core.ProjectProperties;
import greenfoot.core.Simulation;
import greenfoot.core.WorldHandler;
import greenfoot.event.SimulationEvent;
import greenfoot.event.SimulationListener;
import greenfoot.gui.ControlPanel;
import greenfoot.gui.WorldCanvas;
import greenfoot.gui.input.mouse.LocationTracker;
import greenfoot.platforms.standalone.ActorDelegateStandAlone;
import greenfoot.platforms.standalone.GreenfootUtilDelegateStandAlone;
import greenfoot.platforms.standalone.SimulationDelegateStandAlone;
import greenfoot.platforms.standalone.WorldHandlerDelegateStandAlone;
import greenfoot.sound.SoundFactory;
import greenfoot.util.GreenfootUtil;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.swing.JApplet;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.BorderFactory;
import javax.swing.RootPaneContainer;

import bluej.Config;
import bluej.utility.CenterLayout;

/**
 * This class can view and run a Greenfoot scenario. It is not possible to
 * interact with the objects in any way.
 * 
 * @author Poul Henriksen
 */
public class GreenfootScenarioViewer extends JApplet
{
    private static final int EMPTY_BORDER_SIZE = 5;

    private static String scenarioName;

    private boolean isApplet;
    private ProjectProperties properties;
    private Simulation sim;
    private WorldCanvas canvas;
    private ControlPanel controls;
    private RootPaneContainer rootPaneContainer;

    private Constructor<?> worldConstructor;

    /**
     * The default constructor, used when the scenario runs as an applet.
     */
    public GreenfootScenarioViewer()
    {
        isApplet = true;
    }

    /**
     * Constructor for when the scenario runs as an application.
     */
    public GreenfootScenarioViewer(RootPaneContainer rootPane)
    {
        super();
        rootPaneContainer = rootPane;
        isApplet = false;
    }

    /**
     * Returns the size of the borders around the controls.
     */
    public static Dimension getControlsBorderSize()
    {
        return new Dimension((EMPTY_BORDER_SIZE ) * 2, (EMPTY_BORDER_SIZE ) * 2);
    } 
    
    /**
     * Returns the size of the borders around the world panel.
     */
    public static Dimension getWorldBorderSize()
    {
        return new Dimension((EMPTY_BORDER_SIZE + 1) * 2, EMPTY_BORDER_SIZE + 1 * 2);
    }
    
    private void buildGUI()
    {
        if (rootPaneContainer == null) {
            // it will be null when running as applet, so set it to the applet.
            rootPaneContainer = this;
        }
        
        JPanel centerPanel = new JPanel(new CenterLayout());
        centerPanel.add( canvas );
        centerPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        
        JScrollPane outer = new JScrollPane( centerPanel );
        outer.setBorder(BorderFactory.createEmptyBorder(EMPTY_BORDER_SIZE,EMPTY_BORDER_SIZE,EMPTY_BORDER_SIZE,EMPTY_BORDER_SIZE));
        controls.setBorder(BorderFactory.createCompoundBorder( BorderFactory.createEmptyBorder(0,EMPTY_BORDER_SIZE,EMPTY_BORDER_SIZE,EMPTY_BORDER_SIZE), BorderFactory.createEtchedBorder()));
        
        rootPaneContainer.getContentPane().add(outer, BorderLayout.CENTER);
        rootPaneContainer.getContentPane().add(controls, BorderLayout.SOUTH);
    }
    
    @Override
    public String getParameter(String name)
    {
        if (isApplet)
            return super.getParameter(name);
        else
            return null;
    }

    /**
     * Called by the browser or applet viewer to inform this JApplet that it has
     * been loaded into the system. It is always called before the first time
     * that the start method is called.
     */
    public void init()
    {
        GreenfootScenarioMain.initProperties();
        
        boolean storageStandalone = getParameter("storage.standalone") != null;
        String storageHost = getParameter("storage.server");
        String storagePort = getParameter("storage.serverPort");
        String storagePasscode = getParameter("storage.passcode");
        String storageScenarioId = getParameter("storage.scenarioId");
        String storageUserId = getParameter("storage.userId");
        String storageUserName = getParameter("storage.userName");
        
        // this is a workaround for a security conflict with some browsers
        // including some versions of Netscape & Internet Explorer which do
        // not allow access to the AWT system event queue which JApplets do
        // on startup to check access. May not be necessary with your browser.
        JRootPane rootPane = this.getRootPane();
        rootPane.putClientProperty("defeatSystemEventQueueCheck", Boolean.TRUE);
        
        final String worldClassName = Config.getPropString("main.class"); 
        final boolean lockScenario = Config.getPropBoolean("scenario.lock");

        try {
            GreenfootUtil.initialise(new GreenfootUtilDelegateStandAlone(storageStandalone, storageHost, storagePort, storagePasscode, storageScenarioId, storageUserId, storageUserName));
            properties = new ProjectProperties();

            ActorDelegateStandAlone.setupAsActorDelegate();
            ActorDelegateStandAlone.initProperties(properties);

            // We must construct the simulation before the world, as a call to
            // Greenfoot.setSpeed() requires a call to the simulation instance.
            Simulation.initialize(new SimulationDelegateStandAlone());
            
            EventQueue.invokeAndWait(new Runnable() {
                @Override
                public void run()
                {
                    guiSetup(lockScenario, worldClassName);
                }
            });

            WorldHandler worldHandler = WorldHandler.getInstance();
            Class<?> worldClass = Class.forName(worldClassName);
            worldConstructor = worldClass.getConstructor(new Class[]{});
            World world = instantiateNewWorld();
            if (! worldHandler.checkWorldSet()) {
                worldHandler.setWorld(world);
            }
            
            EventQueue.invokeAndWait(new Runnable() {
                @Override
                public void run()
                {
                    buildGUI();
                }
            });
        }        
        catch (SecurityException e) {
            e.printStackTrace();
        }
        catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        catch (InvocationTargetException ite) {
            ite.printStackTrace();
        }
        catch (InterruptedException ie) {
            ie.printStackTrace();
        }
        catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Perform gui setup; this needs to be done on the Event Dispatch Thread.
     * @param lockScenario   whether the scenario is "locked" (speed slider and actor dragging disabled)
     * @param worldClassName  the name of the world class to instantiate
     */
    private void guiSetup(boolean lockScenario, String worldClassName)
    {
        canvas = new WorldCanvas(null);
        canvas.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e)
            {
                canvas.requestFocusInWindow();
                // Have to use requestFocus, since it is the only way to
                // make it work in some browsers (Ubuntu's Firefox 1.5
                // and 2.0)
                canvas.requestFocus();
            }
        });        

        WorldHandler.initialise(canvas, new WorldHandlerDelegateStandAlone(this, lockScenario));
        WorldHandler worldHandler = WorldHandler.getInstance();
        sim = Simulation.getInstance();
        sim.attachWorldHandler(worldHandler);
        LocationTracker.initialize();
        controls = new ControlPanel(sim, ! lockScenario);

        // Make sure the SoundCollection is initialized and listens for events
        sim.addSimulationListener(SoundFactory.getInstance().getSoundCollection());

        sim.addSimulationListener(new SimulationListener() {
            public void simulationChanged(SimulationEvent e)
            {
                // If the simulation starts, try to transfer keyboard
                // focus to the world canvas to allow control of Actors
                // via the keyboard
                if (e.getType() == SimulationEvent.STARTED) {
                    canvas.requestFocusInWindow();
                    // Have to use requestFocus, since it is the only way to
                    // make it work in some browsers: (Ubuntu's Firefox 1.5
                    // and 2.0)
                    canvas.requestFocus();
                }
            }
        });

        try {
            int initialSpeed = properties.getInt("simulation.speed");
            sim.setSpeed(initialSpeed);
        } catch (NumberFormatException nfe) {
            // If there is no speed info in the properties we don't care...
        }
    }
    
    /**
     * Called by the browser or applet viewer to inform this JApplet that it
     * should start its execution. It is called after the init method and each
     * time the JApplet is revisited in a Web page.
     */
    public void start()
    {
    }

    /**
     * Called by the browser or applet viewer to inform this JApplet that it
     * should stop its execution. It is called when the Web page that contains
     * this JApplet has been replaced by another page, and also just before the
     * JApplet is to be destroyed.
     */
    public void stop()
    {
        sim.setPaused(true);
    }

    /**
     * Called by the browser or applet viewer to inform this JApplet that it is
     * being reclaimed and that it should destroy any resources that it has
     * allocated. The stop method will always be called before destroy.
     */
    public void destroy()
    {
        sim.abort();
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
        return Config.getString("scenario.viewer.appletInfo") + " " + scenarioName; //"Title:   \nAuthor:   \nA simple applet example description. ";
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
     */
    public World instantiateNewWorld() 
    {
        try {
            World world = (World) worldConstructor.newInstance(new Object[]{});
            return world;
        }
        catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        catch (InstantiationException e) {
            e.printStackTrace();
        }
        catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        catch (InvocationTargetException e) {
            e.getCause().printStackTrace();
        }
        return null;
    }
    
    /**
     * Get access to the world. Being a public method in the applet class allows
     * this method to be called via JavaScript.
     */
    public World getWorld()
    {
        return WorldHandler.getInstance().getWorld();
    }
    
    /**
     * Get access to the world lock, for the given world.
     */
    public ReentrantReadWriteLock getWorldLock(World world)
    {
        return WorldHandler.getInstance().getWorldLock();
    }
}
