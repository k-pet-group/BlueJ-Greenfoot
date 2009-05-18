/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009  Poul Henriksen and Michael Kolling 
 
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
/*
 * Class Exporter manages the various possible export functions, such as writing 
 * jar files or publishing to the scenario web server.
 *
 * The exporter is a singleton
 *
 * @author Michael Kolling
 * @version $Id: Exporter.java 6339 2009-05-18 11:53:07Z polle $
 */

package greenfoot.export;

import greenfoot.core.GProject;
import greenfoot.core.WorldHandler;
import greenfoot.event.PublishEvent;
import greenfoot.event.PublishListener;
import greenfoot.export.mygame.ScenarioInfo;
import greenfoot.gui.WorldCanvas;
import greenfoot.gui.export.ExportAppPane;
import greenfoot.gui.export.ExportDialog;
import greenfoot.gui.export.ExportPublishPane;
import greenfoot.gui.export.ExportWebPagePane;
import greenfoot.util.GreenfootUtil;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.UnknownHostException;
import java.rmi.RemoteException;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

import bluej.Boot;
import bluej.Config;
import bluej.extensions.ProjectNotOpenException;
import bluej.pkgmgr.Project;
import bluej.utility.FileUtility;

public class Exporter 
        implements PublishListener
{
    private static final String GREENFOOT_CORE_JAR = "Greenfoot-core-" + Boot.GREENFOOT_API_VERSION + ".jar";
    private static final String GALLERY_SHARED_JARS = "http://www.greenfootgallery.org/sharedjars/";
    
    private static Exporter instance;
    
    public static synchronized Exporter getInstance()
    {
        if(instance == null) {
            instance = new Exporter();
        }
        return instance;
    }
    
    private File tmpJarFile;
    private File tmpImgFile;
    private File tmpZipFile;
    private WebPublisher webPublisher;
    private ExportDialog dlg;
    
    /**
     * Creates a new instance of Exporter.
     */
    public Exporter() {
    }
    
   /**
     * Publish this scenario to the web server.
     */
    public void publishToWebServer(GProject project, ExportPublishPane pane, ExportDialog dlg)
    {
        this.dlg = dlg;
        dlg.setProgress(true, Config.getString("export.progress.bundling"));
        
        //Create temporary jar        
        try {
            tmpJarFile = File.createTempFile("greenfoot", ".jar", null);
            //make sure it is deleted on exit (should be deleted right after the publish finish - but just in case...)
            tmpJarFile.deleteOnExit();     
        }
        catch (IOException e) {
            e.printStackTrace();
            return;
        }
        File exportDir = tmpJarFile.getParentFile();
        String jarName = tmpJarFile.getName();           
        
        
        String worldClass = WorldHandler.getInstance().getLastWorldClass().getName();
        
        boolean  lockScenario = pane.lockScenario();
        
        JarCreator jarCreator = new JarCreator(project, exportDir, jarName, worldClass, lockScenario);            
        
        // do not include source
        jarCreator.includeSource(false);
       
        // Add the Greenfoot standalone classes as a separate external jar
        jarCreator.addToClassPath(GALLERY_SHARED_JARS + GREENFOOT_CORE_JAR);   
       
        // Add 3rd party libraries used by Greenfoot.      
        String[] thirdPartyLibs = Boot.GREENFOOT_EXPORT_JARS;
        for (String lib : thirdPartyLibs) {
            jarCreator.addToClassPath(GALLERY_SHARED_JARS + lib);  
        }
        
        // Extra entries for the manifest
        jarCreator.putManifestEntry("title", pane.getTitle());
        
        jarCreator.putManifestEntry("short-description", pane.getShortDescription());
        jarCreator.putManifestEntry("description", pane.getDescription());
        jarCreator.putManifestEntry("url", pane.getURL());

        jarCreator.putManifestEntry("greenfoot-version", Boot.GREENFOOT_VERSION);
        jarCreator.putManifestEntry("java-version", System.getProperty("java.version"));
        jarCreator.putManifestEntry("java-vm-name", System.getProperty("java.vm.name"));
        jarCreator.putManifestEntry("java-vm-version", System.getProperty("java.vm.version"));
        jarCreator.putManifestEntry("java-vm-vendor", System.getProperty("java.vm.vendor"));
        jarCreator.putManifestEntry("os-name", System.getProperty("os.name"));
        jarCreator.putManifestEntry("os-version", System.getProperty("os.version"));
        jarCreator.putManifestEntry("os-arch", System.getProperty("os.arch"));
        jarCreator.putManifestEntry("java-home", System.getProperty("java.home"));        
        
        Dimension size = getSize(!lockScenario);
        jarCreator.putManifestEntry("width", "" + size.width);
        jarCreator.putManifestEntry("height","" + size.height);

        // Make sure the current properties are saved before they are exported.
        project.getProjectProperties().save();
        
        jarCreator.create();
            
        // Build zip with source code if needed
        if(pane.includeSourceCode()) { 
            //Create temporary zip file for the source code        
            try {
                tmpZipFile = File.createTempFile("greenfootSource", ".zip", null);
                //make sure it is deleted on exit (should be deleted right after the publish finish - but just in case...)
                tmpZipFile.deleteOnExit();     
            }
            catch (IOException e) {
                e.printStackTrace();
                return;
            }
            String zipName = tmpZipFile.getName();  
            JarCreator zipCreator = new JarCreator(project, exportDir, zipName);            
            zipCreator.create();
        }
            
        
        // Create image file      
        String formatName = "png";
        try {
            tmpImgFile = File.createTempFile("greenfoot", "." + formatName, null);
            BufferedImage img = pane.getImage();
            ImageIO.write(img, formatName, tmpImgFile);
            // make sure it is deleted on exit (should be deleted right after
            // the publish finish - but just in case...)
            tmpImgFile.deleteOnExit();              
        }
        catch (IOException e) {
            e.printStackTrace();
            return;
        }
        
        String login = pane.getUserName();
        String password = pane.getPassword();     
        String scenarioName = pane.getTitle();
        if(scenarioName != null && scenarioName.length() < 1) {
            scenarioName = "NO_NAME";
        }       
        String hostAddress = Config.getPropString("greenfoot.gameserver.address", "http://www.greenfootgallery.org/");
        if (! hostAddress.endsWith("/")) {
            hostAddress += "/";
        }
        
        if(webPublisher == null) {
            webPublisher = new WebPublisher();
            webPublisher.addPublishListener(this);
        }
        
        dlg.setProgress(true, Config.getString("export.progress.publishing"));
        try {
            ScenarioInfo info = new ScenarioInfo();
            info.setTitle(scenarioName);
            info.setShortDescription(pane.getShortDescription());
            info.setLongDescription(pane.getDescription());
            info.setTags(pane.getTags());
            info.setUrl(pane.getURL());
            
            int uploadSize = (int) tmpJarFile.length();
            if (tmpImgFile != null) {
                uploadSize += (int) tmpImgFile.length();
            }
            if (tmpZipFile != null) {
                uploadSize += (int) tmpZipFile.length();
            }
            gotUploadSize(uploadSize);
            
            webPublisher.submit(hostAddress, login, password,
                    tmpJarFile.getAbsolutePath(), tmpZipFile, tmpImgFile, size.width, size.height,
                    info);
        }
        catch (UnknownHostException e) {
            dlg.setProgress(false, Config.getString("export.publish.unknownHost") + " (" + e.getMessage() + ")");
            return;
        }
        catch (IOException e) {
            dlg.setProgress(false, Config.getString("export.publish.fail") + " " + e.getMessage());
            return;
        }
    }

    /**
     * Create a web page and jar-file.
     */
    public void makeWebPage(GProject project, ExportWebPagePane pane, ExportDialog dlg)
    {
        this.dlg = dlg;
        dlg.setProgress(true, Config.getString("export.progress.writingHTML"));
        File exportDir = new File(pane.getExportLocation());
        exportDir.mkdir();

        String worldClass = WorldHandler.getInstance().getLastWorldClass().getName();
        
        boolean  includeControls = pane.lockScenario();
        String jarName = project.getName() + ".jar";
        JarCreator jarCreator = new JarCreator(project, exportDir, jarName, worldClass, includeControls);            
        
        // do not include source
        jarCreator.includeSource(false);        

        // Add the Greenfoot standalone classes
        File greenfootLibDir = Config.getGreenfootLibDir();        
        File greenfootDir = new File(greenfootLibDir, "standalone");        
        jarCreator.addFile(greenfootDir);   
        
        // Add 3rd party libraries used by Greenfoot.
        File bluejLibDir = Config.getBlueJLibDir();        
        String[] thirdPartyLibs = Boot.GREENFOOT_EXPORT_JARS;
        for (int i = 0; i < thirdPartyLibs.length; i++) {
            String lib = thirdPartyLibs[i];
            jarCreator.addJar(new File(bluejLibDir,lib));
        }
        
        // Add jars in +libs dir in project directory
        File[] jarFiles = getJarsInPlusLib(project);
        if (jarFiles != null) {
            for (File file : jarFiles) {
                jarCreator.addJar(file);
            }
        }                    
        
        Dimension size = getSize(includeControls);

        // Make sure the current properties are saved before they are exported.
        project.getProjectProperties().save();
        
        jarCreator.create();
    
        String htmlName = project.getName() + ".html";
        String title = project.getName();
        File outputFile = new File(exportDir, htmlName);
        jarCreator.generateHTMLSkeleton(outputFile, title, size.width, size.height);
        dlg.setProgress(false, Config.getString("export.progress.complete")); 
    }

    private File[] getJarsInPlusLib(GProject project)
    {
        File[] jarFiles = null;
        try {
            File plusLibsDir = new File(project.getDir(), Project.projectLibDirName);
            jarFiles = plusLibsDir.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name)
                {
                    return name.toLowerCase().endsWith(".jar");
                }
            });
        }
        catch (ProjectNotOpenException e) {}
        catch (RemoteException e) {}
        return jarFiles;
    }
        
    /**
     * Create an application (jar-file)
     */
    public void makeApplication(GProject project, ExportAppPane pane, ExportDialog dlg)
    {
        dlg.setProgress(true, Config.getString("export.progress.writingJar"));
        File exportFile = new File(pane.getExportName());
        File exportDir = exportFile.getParentFile();
        String jarName = exportFile.getName();

        String worldClass = WorldHandler.getInstance().getLastWorldClass().getName();
        
        boolean  includeControls = pane.lockScenario();
        
        JarCreator jarCreator = new JarCreator(project, exportDir, jarName, worldClass, includeControls); 
        // do not include source
        jarCreator.includeSource(false);  
        
        // Add the Greenfoot standalone classes
        File greenfootLibDir = Config.getGreenfootLibDir();        
        File greenfootDir = new File(greenfootLibDir, "standalone");        
        jarCreator.addFile(greenfootDir);     
        
        // Add 3rd party libraries used by Greenfoot.
        File bluejLibDir = Config.getBlueJLibDir();        
        String[] thirdPartyLibs = Boot.GREENFOOT_EXPORT_JARS;
        for (int i = 0; i < thirdPartyLibs.length; i++) {
            String lib = thirdPartyLibs[i];
            jarCreator.addJarToJar(new File(bluejLibDir,lib));
        }

        // Add jars in +libs dir in project directory
        File[] jarFiles = getJarsInPlusLib(project);
        if (jarFiles != null) {
            for (File file : jarFiles) {
                jarCreator.addJarToJar(file);
            }
        }         
        
        // Add text file with license information
        try {
            File license = new File(GreenfootUtil.getGreenfootDir(), "GREENFOOT_LICENSES.txt");
            if(license.exists()) {
                jarCreator.addFile(license);
            }
        } catch (IOException e) {
            // Ignore exceptions with license file since it is not a crucial thing to include.
        }
        
        // Make sure the current properties are saved before they are exported.
        project.getProjectProperties().save();
        
        jarCreator.create();
        dlg.setProgress(false, Config.getString("export.progress.complete")); 
    }

    /**
     * Get the size needed to display the application and control panel.
     * @return
     */
    private Dimension getSize(boolean includeControls)
    {     
        //The control panel size is hard coded for now, since it has different sizes on different platforms. 
        //It is bigger on windows than most other platforms, so this is the size that is used.
        //Will be even more problematic once we get i18n!
        Dimension controlPanelSize = null;  
        Dimension border = GreenfootScenarioViewer.getControlsBorderSize();        
        if(includeControls) {
            controlPanelSize = new Dimension(560 + border.width , 46 + border.height);
        }
        else {   
            controlPanelSize = new Dimension(410 + border.width, 46 + border.height);
        }
        
        WorldCanvas canvas = WorldHandler.getInstance().getWorldCanvas();
        border = GreenfootScenarioViewer.getControlsBorderSize();        
        Dimension size = new Dimension(canvas.getWidth() + border.width, (int) controlPanelSize.getHeight() + canvas.getHeight() + border.height);
        if(size.getWidth() < controlPanelSize.getWidth()) {
            size.width = controlPanelSize.width;
        }
        return size;
    }
        
    /**
     * Something went wrong when publishing.
     */
    public void errorRecieved(final PublishEvent event)
    {
        deleteTmpFiles();
        SwingUtilities.invokeLater(new Runnable() {
            public void run()
            {
                dlg.publishFinished(false,  Config.getString("export.publish.fail") + " " + event.getMessage());
            }
        });
    }

    /**
     * Publish succeeded.
     */    
    public void statusRecieved(PublishEvent event)
    {
        deleteTmpFiles();
        SwingUtilities.invokeLater(new Runnable() {
            public void run()
            {
                dlg.publishFinished(true, Config.getString("export.publish.complete"));
            }
        });
    }

    private void deleteTmpFiles()
    {
        if (tmpJarFile != null) {
            tmpJarFile.delete();
            tmpJarFile = null;
        }
        if (tmpImgFile != null) {
            tmpImgFile.delete();
            tmpImgFile = null;
        }
        if (tmpZipFile != null) {
            tmpZipFile.delete();
            tmpZipFile = null;
        }
    }
    
    /**
     * We now know the total upload size.
     */
    public void gotUploadSize(final int size)
    {
        SwingUtilities.invokeLater(new Runnable() {
            public void run()
            {
                dlg.gotUploadSize(size);
            }
        });
    }
    
    /**
     * Upload progress made.
     */
    public void progressMade(final PublishEvent event)
    {
        SwingUtilities.invokeLater(new Runnable() {
            public void run()
            {
                dlg.progressMade(event.getBytes());
            }
        });
    }
}
