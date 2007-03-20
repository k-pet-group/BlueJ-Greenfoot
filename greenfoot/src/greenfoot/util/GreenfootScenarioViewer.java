package greenfoot.util;

import greenfoot.World;
import greenfoot.core.ProjectProperties;
import greenfoot.core.Simulation;
import greenfoot.core.WorldHandler;
import greenfoot.gui.ControlPanel;
import greenfoot.gui.DragGlassPane;
import greenfoot.gui.WorldCanvas;
import greenfoot.platforms.standalone.ActorDelegateStandAlone;
import greenfoot.platforms.standalone.GreenfootUtilDelegateStandAlone;
import greenfoot.platforms.standalone.WorldHandlerDelegateStandAlone;

import java.awt.BorderLayout;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;

import javax.swing.JFrame;

/**
 * This class can view and run a greenfoot scenario. It is not possible to
 * interact with the objects in any way.
 * 
 * @author Poul Henriksen
 * 
 */
public class GreenfootScenarioViewer
{

    private static String frameTitleName;
    
    private ProjectProperties properties;
    private World world;
    private Simulation sim;
    private WorldCanvas canvas;
    private ControlPanel controls;

    /**
     * Start the scenario. <p>
     * 
     * BlueJ and the scenario MUST be on the classpath.
     * 
     * @param args One argument can be passed to this method. The
     * first one should be the World to be instantiated. If no arguments
     * are supplied it will read from the properties file. And if that can't be found either it will use AntWorld.
     * 
     */
    public static void main(String[] args)
    {
        String worldClassName = null;
        Properties p = new Properties();
        try {
            ClassLoader loader = GreenfootScenarioViewer.class.getClassLoader();
            InputStream is = loader.getResourceAsStream("standalone.properties");
            
            p.load(is);
            worldClassName = p.getProperty("main.class");
            frameTitleName = p.getProperty("project.name");
            is.close();
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        if(args.length == 1) {
            worldClassName = args[0];
        }
        
        if(worldClassName == null) {
       //     worldClassName = "Breakout";
        }
        

        
        GreenfootScenarioViewer gs = new GreenfootScenarioViewer();
        gs.init(worldClassName);
        gs.buildGUI();        
    }
    
    private void buildGUI()
    {
        JFrame frame = new JFrame(frameTitleName);

        frame.setGlassPane(DragGlassPane.getInstance());
        frame.getContentPane().add(canvas, BorderLayout.CENTER);

       
        frame.getContentPane().add(controls, BorderLayout.SOUTH);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);

    }

    private void init(String worldClassName)
    {
        try {           

            GreenfootUtil.initialise(new GreenfootUtilDelegateStandAlone());
            properties = new ProjectProperties();
            
            ActorDelegateStandAlone.setupAsActorDelegate();    
            ActorDelegateStandAlone.initProperties(properties);
           
            canvas = new WorldCanvas(null);

            WorldHandler.initialise(canvas, new WorldHandlerDelegateStandAlone());
            WorldHandler worldHandler = WorldHandler.getInstance();
            Simulation.initialize(worldHandler);
            sim = Simulation.getInstance();
            controls = new ControlPanel(sim);            

            int initialSpeed = properties.getInt("simulation.speed");
            sim.setSpeed(initialSpeed);
            
            Class worldClass = Class.forName(worldClassName);
            Constructor worldConstructor = worldClass.getConstructor(new Class[]{});
            world = (World) worldConstructor.newInstance(new Object[]{});

            ActorDelegateStandAlone.initWorld(world);
            
            worldHandler.setWorld(world);
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
    }

}
