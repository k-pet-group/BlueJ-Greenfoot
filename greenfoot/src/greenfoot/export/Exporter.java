/*
 * Class Exporter manages the various possibel export functions, such as writing 
 * jar files or publishing to the scenario web server.
 *
 * The exporter is a singleton
 *
 * @author Michael Kolling
 * @version $Id: Exporter.java 4997 2007-04-24 09:09:48Z mik $
 */

package greenfoot.export;

import greenfoot.core.GProject;
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

        String worldClass = pane.getWorldClassName();
        boolean  includeControls = pane.includeExtraControls();
        
        JarCreator jarCreator = new JarCreator(project, exportDir, jarName, worldClass, includeControls);            
        
        // do not include source
        jarCreator.includeSource(false);
        
        
        // Extra entries for the manifest - used when publishing to stompt.org 
        // TODO: get these from the pane?
        jarCreator.putManifestEntry("short-description", "a one-line description (optional)");
        jarCreator.putManifestEntry("description", "a paragraph (even more optional)");
        jarCreator.putManifestEntry("url", "a url back to wherever the user would like to link to (like  their blog or home page) (also optional)");
        jarCreator.putManifestEntry("args", "an argument string that is currently unused for applets, but  will be used for JNLP launching (not implemented completely yet!)");

        Dimension size = getSize(includeControls);
        jarCreator.putManifestEntry("width", "" + size.width);
        jarCreator.putManifestEntry("height","" + size.height);
        
        jarCreator.create();
        

        //TODO: get these from the pane?
        String login = "polle";
        String password = "polle123";
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
            // TODO Handle this!
            e.printStackTrace();
        }
        dlg.setProgress(false, "Publish complete."); 
    }

    /**
     * Create a web page and jar-file.
     */
    public void makeWebPage(GProject project, ExportWebPagePane pane, ExportDialog dlg)
    {
        dlg.setProgress(true, "Writing web page...");
        File exportDir = new File(pane.getExportLocation());
        exportDir.mkdir();
        String worldClass = pane.getWorldClassName();
        boolean  includeControls = pane.includeExtraControls();
        String jarName = project.getName() + ".jar";
        JarCreator jarCreator = new JarCreator(project, exportDir, jarName, worldClass, includeControls);            
        
        // do not include source
        jarCreator.includeSource(false);
        
        int width = WorldHandler.getInstance().getWorldCanvas().getWidth();
        int height = WorldHandler.getInstance().getWorldCanvas().getHeight() + 50;  
        
        jarCreator.create();
    
        String htmlName = project.getName() + ".html";
        String title = project.getName();
        File outputFile = new File(exportDir, htmlName);
        jarCreator.generateHTMLSkeleton(outputFile, title, width, height);
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
        String worldClass = pane.getWorldClassName();
        boolean  includeControls = pane.includeExtraControls();
        
        JarCreator jarCreator = new JarCreator(project, exportDir, jarName, worldClass, includeControls); 
        // do not include source
        jarCreator.includeSource(false);  
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
        if(includeControls) {
            controlPanelSize = new Dimension(560, 47);
        }
        else {   
            controlPanelSize = new Dimension(410, 46);
        }
        
        WorldCanvas canvas = WorldHandler.getInstance().getWorldCanvas();
        Dimension size = new Dimension(canvas.getWidth(), (int) controlPanelSize.getHeight() + canvas.getHeight());
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
        // TODO Handle error
        System.out.println("Error: " + event);
    }

    /**
     * Publsh succeeded.
     */    
    public void statusRecieved(PublishEvent event)
    {
        tmpJarFile.delete();
        // TODO Display success - close dialog?

        System.out.println("Success: " + event);
        
    }
    
}
