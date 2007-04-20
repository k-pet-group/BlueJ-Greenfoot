package greenfoot.actions;

import greenfoot.core.GClass;
import greenfoot.core.GPackage;
import greenfoot.core.GProject;
import greenfoot.core.GreenfootMain;
import greenfoot.core.WorldHandler;
import greenfoot.gui.export.ExportCompileDialog;
import greenfoot.gui.export.ExportDialog;
import greenfoot.publish.JarCreator;
import greenfoot.util.GreenfootUtil;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.rmi.RemoteException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.swing.AbstractAction;

import bluej.Config;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;
import greenfoot.gui.export.ExportAppPane;
import greenfoot.gui.export.ExportPane;
import greenfoot.gui.export.ExportPublishPane;
import greenfoot.gui.export.ExportWebPagePane;

/**
 * Action to export a project to a standalone program.
 * 
 * @author Poul Henriksen, Michael Kolling
 * @version $Id: ExportProjectAction.java 4986 2007-04-20 13:24:49Z polle $
 */
public class ExportProjectAction extends AbstractAction
{
    private static ExportProjectAction instance = new ExportProjectAction();
    private GProject project;
    private File projectDir = null;
    
    /**
     * Singleton factory method for action.
     */
    public static ExportProjectAction getInstance()
    {
        return instance;
    }

    private ExportDialog exportDialog;

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
    }
        
    /**
     * Publish this scenario to the web server.
     */
    private void doWebPage(ExportWebPagePane pane)
    {
        File exportDir = new File(pane.getExportLocation());
        exportDir.mkdir();
        String worldClass = pane.getWorldClassName();
        boolean  includeControls = pane.includeExtraControls();
        String jarName = project.getName() + ".jar";
        createJar(exportDir, jarName, worldClass, includeControls, true);
    }
        
    /**
     * Publish this scenario to the web server.
     */
    private void doApplication(ExportAppPane pane)
    {
        File exportFile = new File(pane.getExportName());
        File exportDir = exportFile.getParentFile();
        String jarName = exportFile.getName();
        String worldClass = pane.getWorldClassName();
        boolean  includeControls = pane.includeExtraControls();
        createJar(exportDir, jarName, worldClass, includeControls, false);
    }
        
                
    /**
     * .
     */
    private void createJar(File exportDir, String jarName, String worldClass, 
                           boolean includeExtraControls, boolean writeWebPage)
    {        
        JarCreator jarCreator = null;
        jarCreator = new JarCreator(project, exportDir, jarName, worldClass, includeExtraControls);            

        // do not include source
        jarCreator.includeSource(false);
        
        //Extra entries for the manifest - used when publishing to stompt.org
        int width = WorldHandler.getInstance().getWorldCanvas().getWidth();
        int height = WorldHandler.getInstance().getWorldCanvas().getHeight() + 50;  
        jarCreator.putManifestEntry("short-description", "a one-line description (optional)");
        jarCreator.putManifestEntry("description", "a paragraph (even more optional)");
        jarCreator.putManifestEntry("url", "a url back to wherever the user would like to link to (like  their blog or home page) (also optional)");
        jarCreator.putManifestEntry("width", "" + width);
        jarCreator.putManifestEntry("height","" + height);
        jarCreator.putManifestEntry("args", "an argument string that is currently unused for applets, but  will be used for JNLP launching (not implemented completely yet!)");
        
        jarCreator.create();

        if(writeWebPage) {
            String htmlName = project.getName() + ".html";
            String title = project.getName();
            File outputFile = new File(exportDir, htmlName);
            jarCreator.generateHTMLSkeleton(outputFile, title, width, height);
        }
    }


    private boolean showCompileDialog(GProject project)
    {
        ExportCompileDialog d = new ExportCompileDialog(GreenfootMain.getInstance().getFrame(), project);
        GreenfootMain.getInstance().addCompileListener(d);
        boolean compiled = d.display();
        GreenfootMain.getInstance().removeCompileListener(d);
        return compiled;
    }
}
