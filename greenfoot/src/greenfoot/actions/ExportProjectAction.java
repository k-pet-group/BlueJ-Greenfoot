package greenfoot.actions;

import greenfoot.core.GProject;
import greenfoot.core.GreenfootMain;
import greenfoot.core.WorldHandler;
import greenfoot.event.PublishEvent;
import greenfoot.event.PublishListener;
import greenfoot.export.JarCreator;
import greenfoot.export.WebPublisher;
import greenfoot.gui.export.ExportAppPane;
import greenfoot.gui.export.ExportCompileDialog;
import greenfoot.gui.export.ExportDialog;
import greenfoot.gui.export.ExportPane;
import greenfoot.gui.export.ExportPublishPane;
import greenfoot.gui.export.ExportWebPagePane;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.List;

import javax.swing.AbstractAction;

import bluej.extensions.ProjectNotOpenException;

/**
 * Action to export a project to a standalone program.
 * 
 * @author Poul Henriksen, Michael Kolling
 * @version $Id: ExportProjectAction.java 4988 2007-04-20 14:12:08Z polle $
 */
public class ExportProjectAction extends AbstractAction implements PublishListener
{
    private static ExportProjectAction instance = new ExportProjectAction();
    private GProject project;
    private ExportDialog exportDialog;
    private File tmpJarFile;
    private WebPublisher webPublisher;
    
    /**
     * Singleton factory method for action.
     */
    public static ExportProjectAction getInstance()
    {
        return instance;
    }

    private ExportProjectAction()
    {
        super("Export...");
        setEnabled(false);
    }

    public void actionPerformed(ActionEvent ae)
    {
        project = GreenfootMain.getInstance().getProject();
        
        if(!project.isCompiled())  {
            boolean isCompiled = showCompileDialog(project);
            if(!isCompiled) {               
                //Cancel export
                return;
            }
        }
        
        String scenarioName = project.getName();
        
        File projectDir = null;
        try {
            projectDir = project.getDir();
        }
        catch (ProjectNotOpenException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        catch (RemoteException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }       
        
        List<String> worldClasses = GreenfootMain.getInstance().getPackage().getWorldClasses();
        if(exportDialog == null) {
            exportDialog = new ExportDialog(GreenfootMain.getInstance().getFrame(), scenarioName, worldClasses, projectDir.getParentFile());
        }
        boolean okPressed = exportDialog.display();
        if(!okPressed) {
            return;
        }
        
        String function = exportDialog.getSelectedFunction();
        ExportPane pane = exportDialog.getSelectedPane();
        
        if(function.equals(ExportPublishPane.FUNCTION)) {
            doPublish((ExportPublishPane)pane);
        }
        if(function.equals(ExportWebPagePane.FUNCTION)) {
            doWebPage((ExportWebPagePane)pane);
        }
        if(function.equals(ExportAppPane.FUNCTION)) {
            doApplication((ExportAppPane)pane);
        }
    }

    /**
     * Publish this scenario to the web server.
     */
    private void doPublish(ExportPublishPane pane)
    {
        System.out.println("publishing...");
        
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
        int width = WorldHandler.getInstance().getWorldCanvas().getWidth();
        int height = WorldHandler.getInstance().getWorldCanvas().getHeight() + 50; 
        jarCreator.putManifestEntry("width", "" + width);
        jarCreator.putManifestEntry("height","" + height);
        
        jarCreator.create();
        
        //TODO: get these from the pane?
        String login = "polle";
        String password = "polle123";
        String scenarioName = project.getName();        
        
        if(webPublisher == null) {
            webPublisher = new WebPublisher();
            webPublisher.addPublishListener(this);
        }
        webPublisher.submit(login, password, scenarioName, tmpJarFile.getAbsolutePath()); //TODO change so that it takes a File instead of String for the filename.
        
     //   System.out.println(login + ",  " + password + ",  " + scenarioName + ",  " + tmpJarFile.getAbsolutePath());
      /*  publisher.submit("polle","polle123","forrest-fire on"+new java.util.Date(),
                "/home/polle/workspace/greenfoot/scenarios/forrest-fire-export/forrest-fire.jar");*/
    }
        
    /**
     * Create a web page and jar-file.
     */
    private void doWebPage(ExportWebPagePane pane)
    {
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
    }
        
    /**
     * Create an application (jar-file)
     */
    private void doApplication(ExportAppPane pane)
    {
        File exportFile = new File(pane.getExportName());
        File exportDir = exportFile.getParentFile();
        String jarName = exportFile.getName();
        String worldClass = pane.getWorldClassName();
        boolean  includeControls = pane.includeExtraControls();
        
        JarCreator jarCreator = new JarCreator(project, exportDir, jarName, worldClass, includeControls); 
        // do not include source
        jarCreator.includeSource(false);  
        jarCreator.create();
    }
        
                
    private boolean showCompileDialog(GProject project)
    {
        ExportCompileDialog d = new ExportCompileDialog(GreenfootMain.getInstance().getFrame(), project);
        GreenfootMain.getInstance().addCompileListener(d);
        boolean compiled = d.display();
        GreenfootMain.getInstance().removeCompileListener(d);
        return compiled;
    }

    /**
     * Something went wrong when publishing.
     */
    public void errorRecieved(PublishEvent event)
    {
        tmpJarFile.delete();
        // TODO Handle error
        
    }

    /**
     * Publsh succeeded.
     */    
    public void statusRecieved(PublishEvent event)
    {
        tmpJarFile.delete();
        // TODO Display success - close dialog?
        
    }
}
