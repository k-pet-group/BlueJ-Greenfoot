/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2010,2013  Poul Henriksen and Michael Kolling 
 
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

import greenfoot.util.StandalonePropStringManager;

import java.awt.EventQueue;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Properties;

import javax.swing.ImageIcon;
import javax.swing.JFrame;

import bluej.Config;

/**
 * The main class for Greenfoot scenarios when they are exported as standalone
 * applications.
 * 
 * <p>This must be a separate class from GreenfootScenarioViewer, and specifically
 * must not be a Swing/AWT derived class, because we need to set the application
 * name property (for Mac OS) before any Swing/AWT classes are propertly initialized.
 * 
 * @author Davin McCall
 */
public class GreenfootScenarioMain
{
    public static String scenarioName;
    public static String [] args;
    
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
     */
    public static void main(String[] args)
    {
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        if(args.length != 3 && args.length != 0) {
            System.err.println("Wrong number of arguments");
        }
        
        GreenfootScenarioMain.args = args; 
        initProperties(); // discover scenario name
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", scenarioName);
        
        final GreenfootScenarioViewer[] gsv = new GreenfootScenarioViewer[1];
        final JFrame[] frame = new JFrame[1];
        
        try {
            EventQueue.invokeAndWait(new Runnable() {
                public void run()
                {
                    frame[0] = new JFrame(scenarioName);
                    gsv[0] = new GreenfootScenarioViewer(frame[0]);
                    frame[0].setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                    frame[0].setResizable(false);
                    
                    URL resource = this.getClass().getClassLoader().getResource("greenfoot.png");
                    ImageIcon icon = new ImageIcon(resource);
                    frame[0].setIconImage(icon.getImage());
                }
            });
            
            // Apparently an applet's init() method is *not* called on the EDT.
            gsv[0].init();
            
            EventQueue.invokeAndWait(new Runnable() {
                public void run()
                {
                    frame[0].pack();
                    frame[0].setVisible(true);
                }
            });
            
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Initialize the project properties.
     */
    public static void initProperties()
    {
        if (scenarioName != null) {
            return; // already done
        }
        
        Properties p = new Properties();
        try {
            ClassLoader loader = GreenfootScenarioMain.class.getClassLoader();
            InputStream is = loader.getResourceAsStream("standalone.properties");
            
            if(is == null && args.length == 3) {
                // This might happen if we are running from ant
                // In that case we should have some command line arguments
                p.put("project.name", args[0]);
                p.put("main.class", args[1]);
                p.put("scenario.lock", "true");  
                File f = new File(args[2]);
                is = new FileInputStream(f);    
            } 
            
            p.load(is);
            scenarioName = p.getProperty("project.name");

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
}
