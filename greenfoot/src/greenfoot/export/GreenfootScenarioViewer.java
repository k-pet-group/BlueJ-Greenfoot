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
import bluej.utility.javafx.JavaFXUtil;
import bluej.utility.javafx.UnfocusableScrollPane;
import greenfoot.World;
import greenfoot.core.ExportedProjectProperties;
import greenfoot.core.Simulation;
import greenfoot.core.WorldHandler;
import greenfoot.event.SimulationListener;
import greenfoot.guifx.ControlPanel;
import greenfoot.guifx.ControlPanel.ControlPanelListener;
import greenfoot.guifx.GreenfootStage.State;
import greenfoot.guifx.WorldDisplay;
import greenfoot.platforms.standalone.ActorDelegateStandAlone;
import greenfoot.platforms.standalone.GreenfootUtilDelegateStandAlone;
import greenfoot.platforms.standalone.WorldHandlerDelegateStandAlone;
import greenfoot.sound.SoundFactory;
import greenfoot.util.GreenfootUtil;
import greenfoot.util.StandalonePropStringManager;
import javafx.application.Platform;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

/**
 * This class can view and run a Greenfoot scenario. It is not possible to
 * interact with the objects in any way.
 * 
 * @author Poul Henriksen
 */
@OnThread(Tag.FXPlatform)
public class GreenfootScenarioViewer extends BorderPane implements ControlPanelListener, SimulationListener
{
    private ExportedProjectProperties properties;
    private Simulation sim;
    private ControlPanel controls;

    @OnThread(Tag.Any)
    private Constructor<?> worldConstructor;
    
    private final WorldDisplay worldDisplay = new WorldDisplay();
    private boolean updatingSliderFromSimulation = false;

    /**
     * Initialize the project properties.
     */
    public static void initProperties()
    {
        Properties p = new Properties();
        try {
            ClassLoader loader = GreenfootScenarioViewer.class.getClassLoader();
            InputStream is = loader.getResourceAsStream("standalone.properties");
            if (is != null) {
                p.load(is);
            }

            // set bluej Config to use the standalone prop values
            Config.initializeStandalone(new StandalonePropStringManager(p));
            if (is != null) {
                is.close();
            }
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void buildGUI(boolean hideControls)
    {
        ScrollPane worldViewScroll = new UnfocusableScrollPane(new StackPane(worldDisplay));
        JavaFXUtil.expandScrollPaneContent(worldViewScroll);
               
        setCenter(worldViewScroll);
        if (!hideControls){
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
        initProperties();
        
        final String worldClassName = Config.getPropString("main.class"); 
        final boolean lockScenario = Config.getPropBoolean("scenario.lock");
        final boolean hideControls = Config.getPropBoolean("scenario.hideControls", false);

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
            
            buildGUI(hideControls);
            
            controls.updateState(State.PAUSED, false);

            JavaFXUtil.onceNotNull(sceneProperty(), scene -> scene.addEventFilter(KeyEvent.ANY, e -> {
                if (e.getEventType() == KeyEvent.KEY_PRESSED)
                {
                    worldHandler.getKeyboardManager().keyPressed(e.getCode(), e.getText());
                }
                else if (e.getEventType() == KeyEvent.KEY_RELEASED)
                {
                    worldHandler.getKeyboardManager().keyReleased(e.getCode(), e.getText());
                }
                else if (e.getEventType() == KeyEvent.KEY_TYPED)
                {
                    worldHandler.getKeyboardManager().keyTyped(e.getCode(), e.getText());
                }
            }));
            worldDisplay.addEventFilter(MouseEvent.ANY, e -> {
                MouseButton button = e.getButton();
                if (Config.isMacOS() && button == MouseButton.PRIMARY && e.isControlDown())
                {
                    button = MouseButton.SECONDARY;
                }
                
                if (e.getEventType() == MouseEvent.MOUSE_CLICKED)
                {
                    worldHandler.getMouseManager().mouseClicked((int)e.getX(), (int)e.getY(), button, e.getClickCount());
                }
                else if (e.getEventType() == MouseEvent.MOUSE_MOVED)
                {
                    worldHandler.getMouseManager().mouseMoved((int)e.getX(), (int)e.getY());
                }
                else if (e.getEventType() == MouseEvent.MOUSE_DRAGGED)
                {
                    worldHandler.getMouseManager().mouseDragged((int)e.getX(), (int)e.getY(), button);
                }
                else if (e.getEventType() == MouseEvent.MOUSE_PRESSED)
                {
                    worldHandler.getMouseManager().mousePressed((int)e.getX(), (int)e.getY(), button);
                }
                else if (e.getEventType() == MouseEvent.MOUSE_RELEASED)
                {
                    worldHandler.getMouseManager().mouseReleased((int)e.getX(), (int)e.getY(), button);
                }
                else if (e.getEventType() == MouseEvent.MOUSE_EXITED)
                {
                    worldHandler.getMouseManager().mouseExited();
                }
            });
            
            // If we are hiding controls, auto-run:
            if (hideControls)
            {
                Simulation.getInstance().setPaused(false);
            }
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
        if (lockScenario)
        {
            controls.lockControls();
        }

        // Make sure the SoundCollection is initialized and listens for events
        sim.addSimulationListener(SoundFactory.getInstance().getSoundCollection());
        sim.addSimulationListener(this);
        
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
    @OnThread(Tag.Any)
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
    
    @OnThread(Tag.Simulation)
    public String ask(final String prompt)
    {
        final CompletableFuture<String> answer = new CompletableFuture<>();
        Platform.runLater(() -> worldDisplay.ensureAsking(prompt, answer::complete));
        
        try
        {
            return answer.get();
        }
        catch (Exception e)
        {
            e.printStackTrace();
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
        Simulation.getInstance().togglePaused();
    }

    @Override
    public void userReset()
    {
        Simulation.getInstance().setEnabled(false);
        WorldHandler.getInstance().discardWorld();
        WorldHandler.getInstance().instantiateNewWorld(null);
    }

    @Override
    public void setSpeedFromSlider(int speed)
    {
        if (!updatingSliderFromSimulation)
        {
            Simulation.getInstance().setSpeed(speed);
        }
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
    public @OnThread(Tag.Simulation) void simulationChangedSync(SyncEvent eventType)
    {
        if (eventType == SyncEvent.STARTED)
        {
            Platform.runLater(() -> {
                controls.updateState(State.RUNNING, false);
            });
        }
    }

    @Override
    @OnThread(Tag.Any)
    public void simulationChangedAsync(AsyncEvent eventType)
    {
        if (eventType == AsyncEvent.STOPPED)
        {
            Platform.runLater(() -> {
                controls.updateState(State.PAUSED, false);
            });
        }
        else if (eventType == AsyncEvent.DISABLED)
        {
            Platform.runLater(() -> {
                controls.updateState(State.NO_WORLD, false);
            });
        }
        else if (eventType == AsyncEvent.CHANGED_SPEED)
        {
            Platform.runLater(() -> {
                updatingSliderFromSimulation = true;
                controls.setSpeed(Simulation.getInstance().getSpeed());
                updatingSliderFromSimulation = false;
            });
        }
    }
}
