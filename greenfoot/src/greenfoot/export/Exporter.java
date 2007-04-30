/*
 * Class Exporter manages the various possibel export functions, such as writing 
 * jar files or publishing to the scenario web server.
 *
 * The exporter is a singleton
 *
 * @author Michael Kolling
 * @version $Id: Exporter.java 5032 2007-04-30 12:32:22Z polle $
 */

package greenfoot.export;

import greenfoot.core.GProject;
import greenfoot.core.GreenfootMain;
import greenfoot.core.WorldHandler;
import greenfoot.event.PublishEvent;
import greenfoot.event.PublishListener;
import greenfoot.gui.WorldCanvas;
import greenfoot.gui.export.ExportAppPane;
import greenfoot.gui.export.ExportDialog;
import greenfoot.gui.export.ExportPublishPane;
import greenfoot.gui.export.ExportWebPagePane;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.List;

public class Exporter 
        implements PublishListener
{
    private static Exporter instance;
    
    public static synchronized Exporter getInstance()
    {
        if(instance == null) {
            instance = new Exporter();
        }
        return instance;
    }
    
    private File tmpJarFile;
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
        dlg.setProgress(true, "Bundling scenario...");
        
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
        
        boolean  includeControls = pane.includeExtraControls();
        
        JarCreator jarCreator = new JarCreator(project, exportDir, jarName, worldClass, includeControls);            
        
        // do not include source
        jarCreator.includeSource(false);
        
        
        // Extra entries for the manifest - used when publishing to stompt.org 
        // TODO: get these from the pane?
        jarCreator.putManifestEntry("short-description", pane.getShortDescription());
        jarCreator.putManifestEntry("description", pane.getDescription());
        jarCreator.putManifestEntry("url", pane.getURL());
        jarCreator.putManifestEntry("args", "currently unused");

        Dimension size = getSize(includeControls);
        jarCreator.putManifestEntry("width", "" + size.width);
        jarCreator.putManifestEntry("height","" + size.height);

        // Make sure the current properties are saved before they are exported.
        project.getProjectProperties().save();
        
        jarCreator.create();
        

        //TODO: get these from the pane?
        String login = pane.getUserName();
        String password = pane.getPassword();
        String scenarioName = project.getName();        
        String host = "mygame.java.sun.com";
        if(webPublisher == null) {
            webPublisher = new WebPublisher();
            webPublisher.addPublishListener(this);
        }
        
        dlg.setProgress(true, "Publishing...");
        try {
            webPublisher.submit(host, login, password, scenarioName, tmpJarFile.getAbsolutePath());//TODO change so that it takes a File instead of String for the filename.
        }
        catch (UnknownHostException e) {
            dlg.setProgress(false, "Publish failed: Unknown host (" + e.getMessage() + ")");
            return;
        }
        catch (IOException e) {
            dlg.setProgress(false, "Publish failed: " + e.getMessage());
            return;
        }
    }

    /**
     * Create a web page and jar-file.
     */
    public void makeWebPage(GProject project, ExportWebPagePane pane, ExportDialog dlg)
    {
        this.dlg = dlg;
        dlg.setProgress(true, "Writing web page...");
        File exportDir = new File(pane.getExportLocation());
        exportDir.mkdir();

        String worldClass = WorldHandler.getInstance().getLastWorldClass().getName();
        
        boolean  includeControls = pane.includeExtraControls();
        String jarName = project.getName() + ".jar";
        JarCreator jarCreator = new JarCreator(project, exportDir, jarName, worldClass, includeControls);            
        
        // do not include source
        jarCreator.includeSource(false);

        Dimension size = getSize(includeControls);

        // Make sure the current properties are saved before they are exported.
        project.getProjectProperties().save();
        
        jarCreator.create();
    
        String htmlName = project.getName() + ".html";
        String title = project.getName();
        File outputFile = new File(exportDir, htmlName);
        jarCreator.generateHTMLSkeleton(outputFile, title, size.width, size.height);
        dlg.setProgress(false, "Export complete."); 
    }
        
    /**
     * Create an application (jar-file)
     */
    public void makeApplication(GProject project, ExportAppPane pane, ExportDialog dlg)
    {
        dlg.setProgress(true, "Writing jar file...");
        File exportFile = new File(pane.getExportName());
        File exportDir = exportFile.getParentFile();
        String jarName = exportFile.getName();

        String worldClass = WorldHandler.getInstance().getLastWorldClass().getName();
        
        boolean  includeControls = pane.includeExtraControls();
        
        JarCreator jarCreator = new JarCreator(project, exportDir, jarName, worldClass, includeControls); 
        // do not include source
        jarCreator.includeSource(false);  

        // Make sure the current properties are saved before they are exported.
        project.getProjectProperties().save();
        
        jarCreator.create();
        dlg.setProgress(false, "Export complete."); 
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
    public void errorRecieved(PublishEvent event)
    {
        tmpJarFile.delete();
        dlg.setProgress(false, "Publish failed: " + event.getMessage());
    }

    /**
     * Publsh succeeded.
     */    
    public void statusRecieved(PublishEvent event)
    {
        tmpJarFile.delete();
        dlg.setProgress(false, "Publish complete.");
    }
    
}
