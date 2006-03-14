package greenfoot.util;

import java.awt.BorderLayout;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.swing.JFrame;

import greenfoot.GreenfootWorld;
import greenfoot.core.Simulation;
import greenfoot.core.WorldHandler;
import greenfoot.gui.ControlPanel;
import greenfoot.gui.WorldCanvas;

/**
 * This class can view and run a greenfoot scenario. It is not possible to
 * interact with the objects in any way.
 * 
 * @author Poul Henriksen
 * 
 */
public class GreenfootScenarioViewer
{

    private Simulation sim;
    private WorldCanvas canvas;

    public static void main(String[] args)
    {
        String worldClassName = "AntWorld"; 
        String worldInitMethod = "scenarioStupidBig";  
        if(args.length == 2) {
            worldClassName = args[0];
            worldInitMethod = args[1];
        }
        
        GreenfootScenarioViewer gs = new GreenfootScenarioViewer();
        gs.init(worldClassName, worldInitMethod);
        gs.buildGUI();
        gs.startSim();
    }

    private void startSim()
    {
        sim.run();
    }

    private void buildGUI()
    {
        JFrame frame = new JFrame();
        frame.getContentPane().add(canvas, BorderLayout.CENTER);

        ControlPanel controls = new ControlPanel(sim);
        controls.worldCreated(null);
        frame.getContentPane().add(controls, BorderLayout.SOUTH);
        frame.pack();
        frame.setVisible(true);

    }

    private void init(String worldClassName, String worldInitMethod)
    {
        GreenfootLogger.init();
        try {
            Class worldClass = Class.forName(worldClassName);
            Constructor worldConstructor = worldClass.getConstructor(new Class[]{});
            GreenfootWorld world = (GreenfootWorld) worldConstructor.newInstance(new Object[]{});

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
}
