/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010,2011,2012,2013,2015,2018,2022  Poul Henriksen and Michael Kolling
 
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

import bluej.Boot;
import bluej.Config;
import bluej.pkgmgr.Project;
import bluej.utility.Utility;

import greenfoot.event.PublishEvent;
import greenfoot.event.PublishListener;
import greenfoot.export.mygame.MyGameClient;
import greenfoot.export.mygame.ExportInfo;
import greenfoot.guifx.export.ExportDialog;
import greenfoot.guifx.export.ProxyAuthDialog;
import greenfoot.util.GreenfootUtil;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Dimension2D;

import javax.imageio.ImageIO;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Class Exporter manages the various possible export functions, such as writing 
 * jar files or publishing to the scenario web server.
 *
 * The exporter is a singleton
 *
 * @author Michael Kolling
 */
public class Exporter implements PublishListener
{
    /**
     * An enum for the different export functions
     */
    public enum ExportFunction
    {
        PUBLISH, PROJECT, APP;

        /**
         * Returns the export function which corresponds to the passed name.
         * In case the name doesn't match a function, returns
         * ExportFunction.PUBLISH as a default function.
         *
         * @param name The function name
         * @return The corresponding function to the name passed,
         *         otherwise return ExportFunction.PUBLISH
         */
        public static ExportFunction getFunction(String name)
        {
            try
            {
                return ExportFunction.valueOf(name);
            }
            catch (IllegalArgumentException ex)
            {
                return ExportFunction.PUBLISH;
            }
        }
    }

    private static final String GREENFOOT_CORE_JAR = getGreenfootCoreJar();
    private static final String GALLERY_SHARED_JARS = "sharedjars/";
    
    private static String getGreenfootCoreJar()
    {
        // The core jar filename doesn't need to include the API internal version increment.
        String coreJar = "Greenfoot-core-";
        int lastDot = Boot.GREENFOOT_API_VERSION.lastIndexOf('.');
        coreJar += Boot.GREENFOOT_API_VERSION.substring(0, lastDot) + ".jar";
        return coreJar;
    }
    
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
    private MyGameClient webPublisher;

    private Project project;
    private ExportInfo scenarioInfo;
    private String worldName;
    private double worldWidth;
    private double worldHeight;
    
    private ExportDialog dialog;

    /**
     * Creates a new instance of Exporter.
     */
    private Exporter() { }

    /**
     * Publish/Export this scenario based on the passed function.
     *
     * @param project       The current project.
     * @param dialog        A share/export dialog reference to show progress/messages to user.
     * @param scenarioSaver The listener that will enable us to save the scenario when exporting.
     * @param scenarioInfo  The scenario info needed for different export functions.
     * @param function      The share function type which will be perform.
     * @param worldName     The world's name.
     * @param worldWidth    The world's width.
     * @param worldHeight   The world's height.
     */
    @OnThread(Tag.Worker)
    public void doExport(Project project, ExportDialog dialog, ScenarioSaver scenarioSaver,
                         ExportInfo scenarioInfo, ExportFunction function, String worldName,
                         double worldWidth, double worldHeight)
    {
        this.project = project;
        this.dialog = dialog;
        this.scenarioInfo = scenarioInfo;
        this.worldName = worldName;
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;

        if (function.equals(ExportFunction.PUBLISH))
        {
            publishToWebServer();
        }
        if (function.equals(ExportFunction.APP))
        {
            makeApplication();
        }
        if (function.equals(ExportFunction.PROJECT))
        {
            makeProject();
        }
    }
    
    /**
     * Publish this scenario to the web server.
     */
    @OnThread(Tag.Worker)
    private void publishToWebServer()
    {
        dialog.setProgress(true, Config.getString("export.progress.bundling"));
        
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
        
        String hostAddress = Config.getPropString("greenfoot.gameserver.address", "https://www.greenfoot.org/");
        if (! hostAddress.endsWith("/")) {
            hostAddress += "/";
        }

        boolean lockScenario = scenarioInfo.isLocked();
        
        JarCreator jarCreator = new JarCreator(project, exportDir, jarName, worldName,
                lockScenario, true);
        
        // do not include source
        jarCreator.includeSource(false);
       
        // Add the Greenfoot standalone classes as a separate external jar
        jarCreator.addToClassPath(hostAddress + GALLERY_SHARED_JARS + GREENFOOT_CORE_JAR);   
       
        // Add 3rd party libraries used by Greenfoot.      
        Set<File> thirdPartyLibs = GreenfootUtil.get3rdPartyLibs();
        for (File lib : thirdPartyLibs) {
            jarCreator.addToClassPath(hostAddress + GALLERY_SHARED_JARS + lib.getName());  
        }
        
        // Extra entries for the manifest
        jarCreator.putManifestEntry("title", scenarioInfo.getTitle());
        
        jarCreator.putManifestEntry("short-description", scenarioInfo.getShortDescription());
        jarCreator.putManifestEntry("description", scenarioInfo.getLongDescription());
        jarCreator.putManifestEntry("url", scenarioInfo.getUrl());

        jarCreator.putManifestEntry("greenfoot-version", Boot.GREENFOOT_VERSION);
        jarCreator.putManifestEntry("java-version", System.getProperty("java.version"));
        jarCreator.putManifestEntry("java-vm-name", System.getProperty("java.vm.name"));
        jarCreator.putManifestEntry("java-vm-version", System.getProperty("java.vm.version"));
        jarCreator.putManifestEntry("java-vm-vendor", System.getProperty("java.vm.vendor"));
        jarCreator.putManifestEntry("os-name", System.getProperty("os.name"));
        jarCreator.putManifestEntry("os-version", System.getProperty("os.version"));
        jarCreator.putManifestEntry("os-arch", System.getProperty("os.arch"));
        jarCreator.putManifestEntry("java-home", System.getProperty("java.home"));        
        
        Dimension2D size = getSize(!lockScenario);
        jarCreator.putManifestEntry("width", "" + size.getWidth());
        jarCreator.putManifestEntry("height", "" + size.getHeight());

        jarCreator.create();
            
        // Build zip with source code if needed
        if(scenarioInfo.isIncludeSource())
        {
            //Create temporary zip file for the source code        
            try
            {
                tmpZipFile = File.createTempFile("greenfootSource", ".zip", null);
                //make sure it is deleted on exit (should be deleted right after the publish finish - but just in case...)
                tmpZipFile.deleteOnExit();     
            }
            catch (IOException e)
            {
                e.printStackTrace();
                return;
            }
            String zipName = tmpZipFile.getName();  
            JarCreator zipCreator = new JarCreator(project, exportDir, zipName);            
            zipCreator.create();
        }
                  
        // Create image file     
        if (!scenarioInfo.isKeepSavedScreenshot())
        {
            String formatName = "png";
            try {
                tmpImgFile = File.createTempFile("greenfoot", "." + formatName, null);
                BufferedImage bufferedImage = SwingFXUtils.fromFXImage(scenarioInfo.getImage(), null);
                ImageIO.write(bufferedImage, formatName, tmpImgFile);
                // make sure it is deleted on exit (should be deleted right after
                // the publish finish - but just in case...)
                tmpImgFile.deleteOnExit();              
            }
            catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }
        
        String login = scenarioInfo.getUserName();
        String password = scenarioInfo.getPassword();
        String scenarioName = scenarioInfo.getTitle();
        if (scenarioName != null && scenarioName.length() < 1)
        {
            scenarioName = "NO_NAME";
        }       
        
        if(webPublisher == null) {
            webPublisher = new MyGameClient(this);
        }

        dialog.setProgress(true, Config.getString("export.progress.publishing"));
        try
        {
            ExportInfo exportedInfo = scenarioInfo;
            
            int uploadSize = (int) tmpJarFile.length();
            if (tmpImgFile != null)
            {
                uploadSize += (int) tmpImgFile.length();
            }
            if (tmpZipFile != null)
            {
                uploadSize += (int) tmpZipFile.length();
            }
            setUploadSize(uploadSize);
            
            webPublisher.submit(hostAddress, login, password, tmpJarFile.getAbsolutePath(),
                    tmpZipFile, tmpImgFile, (int)size.getWidth(), (int)size.getHeight(), exportedInfo);
        }
        catch (UnknownHostException e)
        {
            dialog.setProgress(false,
                    Config.getString("export.publish.unknownHost") + " (" + e.getMessage() + ")");
        }
        catch (IOException e)
        {
            dialog.setProgress(false, Config.getString("export.publish.fail") + " " + e.getMessage());
        }
    }

    private static File[] getJarsInPlusLib(Project project)
    {
        File plusLibsDir = new File(project.getProjectDir(), Project.projectLibDirName);
        return plusLibsDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
    }
        
    /**
     * Create an application (jar-file)
     */
    @OnThread(Tag.Worker)
    private void makeApplication()
    {
        dialog.setProgress(true, Config.getString("export.progress.writingJar"));
        File exportFile = new File(scenarioInfo.getExportFileName());
        File exportDir = exportFile.getParentFile();
        String jarName = exportFile.getName();

        boolean lockScenario = scenarioInfo.isLocked();
        boolean hideControls = scenarioInfo.isHideControls();

        JarCreator jarCreator = new JarCreator(project, exportDir, jarName, worldName,
                lockScenario, hideControls, false, false);
        // do not include source
        jarCreator.includeSource(false);  
        
        // Add the Greenfoot standalone classes
        File greenfootLibDir = Config.getGreenfootLibDir();        
        File greenfootDir = new File(greenfootLibDir, "standalone");        
        jarCreator.addFile(greenfootDir);     
        
        // Add 3rd party libraries used by Greenfoot.      
        Set<File> thirdPartyLibs = GreenfootUtil.get3rdPartyLibs();
        for (File lib : thirdPartyLibs) {
            jarCreator.addJarToJar(lib);
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
            File license = new File(Utility.getGreenfootDir(), "GREENFOOT_LICENSES.txt");
            if(license.exists()) {
                jarCreator.addFile(license);
            }
        }
        catch (IOException e) {
            // Ignore exceptions with license file since it is not a crucial thing to include.
        }
        
        jarCreator.create();
        dialog.setProgress(false, Config.getString("export.progress.complete"));
    }
    
    /**
     * Create an standalone project (gfar-file)
     */
    @OnThread(Tag.Worker)
    private void makeProject()
    {
        dialog.setProgress(true, Config.getString("export.progress.writingGfar"));
        
        File exportFile = new File(scenarioInfo.getExportFileName());
        File exportDir = exportFile.getParentFile();
        String gfarName = exportFile.getName();
       
        // Build gfar with source code        
        JarCreator gfarCrator = new JarCreator(project, exportDir, gfarName);
        gfarCrator.create();

        dialog.setProgress(false, Config.getString("export.progress.complete"));
    }

    /**
     * Get the size needed to display the application and control panel.
     * @return The width and the height wrapped in Dimension2D object.
     */
    private Dimension2D getSize(boolean includeControls)
    {
        final int EMPTY_BORDER_SIZE = 5;
        
        //The control panel size is hard coded for now, since it has different sizes on different platforms. 
        //It is bigger on windows than most other platforms, so this is the size that is used.
        //Will be even more problematic once we get i18n!
        Dimension2D controlsBorder = new Dimension2D((EMPTY_BORDER_SIZE) * 2, (EMPTY_BORDER_SIZE) * 2);
        double controlsWidth = controlsBorder.getWidth() + (includeControls ? 560 : 410);
        double controlHeight = controlsBorder.getHeight() + 48;

        Dimension2D worldBorder = new Dimension2D((EMPTY_BORDER_SIZE + 1) * 2, (EMPTY_BORDER_SIZE + 1) * 2);

        // +2 to add some extra padding
        double width = Math.max(worldWidth + worldBorder.getWidth() + 2, controlsWidth);
        double height = controlHeight + worldHeight + worldBorder.getHeight() + 2;
        return new Dimension2D(width, height);
    }
        
    /**
     * Something went wrong when publishing.
     */
    @Override
    public void errorRecieved(final PublishEvent event)
    {
        deleteTmpFiles();
        Platform.runLater(() -> dialog.publishFinished(false,  Config.getString("export.publish.fail") + " " + event.getMessage()));
    }

    /**
     * Publish succeeded.
     */    
    @Override
    public void uploadComplete(PublishEvent event)
    {
        deleteTmpFiles();
        Platform.runLater(() -> dialog.publishFinished(true, Config.getString("export.publish.complete")));
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
    public void setUploadSize(final int size)
    {
        Platform.runLater(() -> dialog.setUploadSize(size));
    }
    
    /**
     * Upload progress made.
     */
    @Override
    public void progressMade(final PublishEvent event)
    {
        Platform.runLater(() -> dialog.progressMade(event.getBytes()));
    }
    
    @Override
    public String[] needProxyAuth()
    {
        CompletableFuture<String[]> detailsFuture = new CompletableFuture<>();
        Platform.runLater(() ->
        {
            Optional<ProxyAuthDialog.ProxyAuthInfo> infoOptional =
                    new ProxyAuthDialog(dialog.asWindow()).showAndWait();
            if (infoOptional.isPresent())
            {
                ProxyAuthDialog.ProxyAuthInfo info = infoOptional.get();
                detailsFuture.complete(new String[] {info.getUsername(), info.getPassword()});
            }
            else
            {
                detailsFuture.complete(null);
            }
        });

        try
        {
            return detailsFuture.get();
        }
        catch (InterruptedException e)
        {
            return null;
        }
        catch (ExecutionException e)
        {
            throw new RuntimeException(e.getCause());
        }
    }
}
