package greenfoot.util;

import greenfoot.GreenfootImage;
import greenfoot.ActorVisitor;
import greenfoot.World;
import greenfoot.core.ClassImageManager;
import greenfoot.core.ProjectProperties;
import greenfoot.core.Simulation;
import greenfoot.core.WorldHandler;
import greenfoot.gui.ControlPanel;
import greenfoot.gui.WorldCanvas;

import java.awt.BorderLayout;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import javax.swing.JFrame;

import bluej.runtime.ExecServer;

/**
 * This class can view and run a greenfoot scenario. It is not possible to
 * interact with the objects in any way.
 * 
 * @author Poul Henriksen
 * 
 */
public class GreenfootScenarioViewer implements ClassImageManager
{

    private Simulation sim;
    private WorldCanvas canvas;
    private ProjectProperties properties;
    

    public static void main(String[] args)
    {
        String worldClassName = "AntWorld"; 
        String worldInitMethod = "big";  
        if(args.length == 2) {
            worldClassName = args[0];
            worldInitMethod = args[1];
        }
        
        GreenfootScenarioViewer gs = new GreenfootScenarioViewer();
        ActorVisitor.setClassImageManager(gs);
        gs.init(worldClassName, worldInitMethod);
        gs.buildGUI();        
    }

    private void buildGUI()
    {
        JFrame frame = new JFrame();
        frame.getContentPane().add(canvas, BorderLayout.CENTER);

        ControlPanel controls = new ControlPanel(sim);
        controls.worldCreated(null);
        frame.getContentPane().add(controls, BorderLayout.SOUTH);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);

    }

    private void init(String worldClassName, String worldInitMethod)
    {
        GreenfootLogger.init();
        
        try {            
            String packageName = GreenfootUtil.extractPackageName(worldClassName);
            worldClassName = GreenfootUtil.extractClassName(worldClassName);
            properties = new ProjectProperties(new File(packageName));
            
            Class worldClass = Class.forName(worldClassName);
            ExecServer.setClassLoader(worldClass.getClassLoader());
            Constructor worldConstructor = worldClass.getConstructor(new Class[]{});
            World world = (World) worldConstructor.newInstance(new Object[]{});

            canvas = new WorldCanvas(world);

            WorldHandler.initialise(null, canvas, world);
            WorldHandler worldHandler = WorldHandler.instance();

            Simulation.initialize(worldHandler);
            sim = Simulation.getInstance();

            Method initMethod = worldClass.getMethod(worldInitMethod, new Class[]{});
            initMethod.invoke(world, new Object[]{});

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
        }
    }
    
    
    // --------- ClassImageManager interface ---------
    
    public GreenfootImage getClassImage(String className)
    {   
        return properties.getImage(className);
    }
   
}
