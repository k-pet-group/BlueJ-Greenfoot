/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010,2011,2012,2013,2014,2015,2016,2018  Poul Henriksen and Michael Kolling
 
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

import bluej.Config;
import bluej.utility.Debug;
import bluej.utility.javafx.UnfocusableScrollPane;
import greenfoot.World;
import greenfoot.core.ExportedProjectProperties;
import greenfoot.core.Simulation;
import greenfoot.core.WorldHandler;
import greenfoot.event.SimulationEvent;
import greenfoot.event.SimulationListener;
import greenfoot.guifx.AskPaneFX;
import greenfoot.guifx.ControlPanel;
import greenfoot.guifx.ControlPanel.ControlPanelListener;
import greenfoot.guifx.GreenfootStage.State;
import greenfoot.guifx.WorldDisplay;
import greenfoot.platforms.standalone.ActorDelegateStandAlone;
import greenfoot.platforms.standalone.GreenfootUtilDelegateStandAlone;
import greenfoot.platforms.standalone.WorldHandlerDelegateStandAlone;
import greenfoot.sound.SoundFactory;
import greenfoot.util.AskHandler;
import greenfoot.util.GreenfootUtil;
import javafx.application.Platform;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This class can view and run a Greenfoot scenario. It is not possible to
 * interact with the objects in any way.
 * 
 * @author Poul Henriksen
 */
@OnThread(Tag.FXPlatform)
public class GreenfootScenarioViewer extends BorderPane implements ControlPanelListener, SimulationListener
{
    private static String scenarioName;

    private boolean showControls;
    private ExportedProjectProperties properties;
    private Simulation sim;
    private AskPaneFX askPanel;
    private ControlPanel controls;

    private Constructor<?> worldConstructor;

    private AskHandler askHandler;
    private final WorldDisplay worldDisplay = new WorldDisplay();


    private void buildGUI()
    {
        ScrollPane worldViewScroll = new UnfocusableScrollPane(worldDisplay);
               
        setCenter(worldViewScroll);
        if (!Config.getPropBoolean("scenario.hideControls",false)){
            //show controls.
            setBottom(controls);
        } 
    }

    /**
     * Called by the browser or applet viewer to inform this JApplet that it has
     * been loaded into the system. It is always called before the first time
     * that the start method is called.
     */
    public GreenfootScenarioViewer()
    {
        GreenfootScenarioMain.initProperties();
        
        final String worldClassName = Config.getPropString("main.class"); 
        final boolean lockScenario = Config.getPropBoolean("scenario.lock");

        try {
            GreenfootUtil.initialise(new GreenfootUtilDelegateStandAlone());
            properties = new ExportedProjectProperties();

            ActorDelegateStandAlone.setupAsActorDelegate();
            ActorDelegateStandAlone.initProperties(properties);

            // We must construct the simulation before the world, as a call to
            // Greenfoot.setSpeed() requires a call to the simulation instance.
            Simulation.initialize();
            
            guiSetup(lockScenario, worldClassName);

            WorldHandler worldHandler = WorldHandler.getInstance();
            Class<?> worldClass = Class.forName(worldClassName);
            worldConstructor = worldClass.getConstructor(new Class[]{});
            World world = instantiateNewWorld();
            if (! worldHandler.checkWorldSet()) {
                worldHandler.setWorld(world, false);
            }
            
            buildGUI();
            
            controls.updateState(State.PAUSED, false);
        }        
        catch (SecurityException | IllegalArgumentException | ClassNotFoundException | NoSuchMethodException e) {
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
        WorldHandler.initialise(new WorldHandlerDelegateStandAlone(this, lockScenario));
        WorldHandler worldHandler = WorldHandler.getInstance();
        sim = Simulation.getInstance();
        sim.attachWorldHandler(worldHandler);
        controls = new ControlPanel(this, null);

        // Make sure the SoundCollection is initialized and listens for events
        sim.addSimulationListener(SoundFactory.getInstance().getSoundCollection());
        
        try {
            int initialSpeed = properties.getInt("simulation.speed");
            sim.setSpeed(initialSpeed);
        } catch (NumberFormatException nfe) {
            // If there is no speed info in the properties we don't care...
        }
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
        catch (IllegalArgumentException | InstantiationException | IllegalAccessException e) {
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
    
    @OnThread(Tag.Any)
    public String ask(final String prompt)
    {
        final AtomicReference<Callable<String>> c = new AtomicReference<Callable<String>>();
        try
        {
            EventQueue.invokeAndWait(new Runnable() {public void run() {
                //c.set(askHandler.ask(prompt, canvas.getPreferredSize().width));
            }});
        }
        catch (InvocationTargetException | InterruptedException e)
        {
            Debug.reportError(e);
        }
        
        try
        {
            return c.get().call();
        }
        catch (Exception e)
        {
            Debug.reportError(e);
            return null;
        }
    }

    @Override
    public void act()
    {
        Simulation.getInstance().runOnce();
    }

    @Override
    public void doRunPause()
    {
        // TODO support toggling of pause status
        Simulation.getInstance().setPaused(false);
    }

    @Override
    public void doReset()
    {
        Simulation.getInstance().setEnabled(false);
        WorldHandler.getInstance().discardWorld();
        WorldHandler.getInstance().instantiateNewWorld(null);
    }

    @Override
    public void setSpeedFromSlider(int speed)
    {
        Simulation.getInstance().setSpeed(speed);
    }

    /**
     * Sets the latest world image on the screen.
     * 
     * @param worldImage A Swing BufferedImage which is copied before returning.
     */
    public void setWorldImage(BufferedImage worldImage)
    {
        if (worldDisplay.setImage(bufferedImageToFX(worldImage)))
        {
            worldDisplay.getScene().getWindow().sizeToScene();
        }
    }

    /**
     * Directly copies a BufferedImage, which is assumed to have ARGB format, into a JavaFX image.
     * @param worldImage The BufferedImage to copy from.  Must be in ARGB format.
     * @return The JavaFX image with a copy of the BufferedImage
     */
    private static Image bufferedImageToFX(BufferedImage worldImage)
    {
        WritableImage fxImage = new WritableImage(worldImage.getWidth(), worldImage.getHeight());
        int [] raw = ((DataBufferInt) worldImage.getData().getDataBuffer()).getData();
        int offset = 0;
        for (int y = 0; y < worldImage.getHeight(); y++)
        {
            for (int x = 0; x < worldImage.getWidth(); x++)
            {
                fxImage.getPixelWriter().setArgb(x, y, raw[offset++]);
            }
        }
        return fxImage;
    }

    @Override
    @OnThread(Tag.Simulation)
    public void simulationChanged(SimulationEvent e)
    {
        int eventType = e.getType();
        if (eventType == SimulationEvent.STOPPED)
        {
            Platform.runLater(() -> {
                controls.updateState(State.PAUSED, false);
            });
        }
        else if (eventType == SimulationEvent.STARTED)
        {
            Platform.runLater(() -> {
                controls.updateState(State.RUNNING, false);
            });
        }
        else if (eventType == SimulationEvent.DISABLED)
        {
            Platform.runLater(() -> {
                controls.updateState(State.UNCOMPILED, false);
            });
        }
    }
}
