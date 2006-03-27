package greenfoot.util;

import greenfoot.GreenfootImage;
import greenfoot.GreenfootObjectVisitor;
import greenfoot.GreenfootWorld;
import greenfoot.core.ClassImageManager;
import greenfoot.core.Simulation;
import greenfoot.core.WorldHandler;
import greenfoot.gui.ControlPanel;
import greenfoot.gui.WorldCanvas;

import java.awt.BorderLayout;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

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
    
    private Map classImages = new HashMap();
    private Map pkgPropsMap = new HashMap();

    public static void main(String[] args)
    {
        String worldClassName = "AntWorld"; 
        String worldInitMethod = "big";  
        if(args.length == 2) {
            worldClassName = args[0];
            worldInitMethod = args[1];
        }
        
        GreenfootScenarioViewer gs = new GreenfootScenarioViewer();
        GreenfootObjectVisitor.setClassImageManager(gs);
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
            Class worldClass = Class.forName(worldClassName);
            ExecServer.currentLoader = worldClass.getClassLoader();
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
    
    private Properties getPackageProperties(String pkgName)
    {
        Properties pkgProps = (Properties) pkgPropsMap.get(pkgName);
        if (pkgProps == null) {
            pkgProps = new Properties();
            String propsFileResource = pkgName.replace('.', '/');
            // + "/greenfoot.pkg";
            if (propsFileResource.length() == 0) {
                propsFileResource = "greenfoot.pkg";
            }
            else {
                propsFileResource += "/greenfoot.pkg";
            }
            InputStream propStream = ClassLoader.getSystemResourceAsStream(propsFileResource);
            try {
                pkgProps.load(propStream);
            }
            catch (IOException ioe) {}
            pkgPropsMap.put(pkgName, pkgProps);
        }
        return pkgProps;
    }
    
    // --------- ClassImageManager interface ---------
    
    public GreenfootImage getClassImage(String className)
    {
        GreenfootImage image = (GreenfootImage) classImages.get(className);
        if (image == null) {
            int lastDot = className.lastIndexOf('.');
            String packageName;
            if (lastDot != -1) {
                packageName = className.substring(0, lastDot);
                className = className.substring(lastDot + 1);
            }
            else {
                packageName = "";
            }
            
            Properties pkgProps = getPackageProperties(packageName);
            String imageName = pkgProps.getProperty("class." + className + ".image");
            if (imageName != null) {
                image = new GreenfootImage("images/" + imageName);
                if (image != null) {
                    classImages.put(className, image);
                }
            }
        }
        return image;
    }

}
