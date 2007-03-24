package greenfoot.actions;

import greenfoot.core.GClass;
import greenfoot.core.GProject;
import greenfoot.core.GreenfootMain;
import greenfoot.core.WorldHandler;
import greenfoot.gui.ExportCompileDialog;
import greenfoot.gui.ExportDialog;
import greenfoot.util.GreenfootUtil;
import greenfoot.util.JarCreator;

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

/**
 * Action to export a project to a standalone program.
 * 
 * @author Poul Henriksen
 * @version $Id: ExportProjectAction.java 4880 2007-03-24 13:05:55Z polle $
 */
public class ExportProjectAction extends AbstractAction
{
    private static ExportProjectAction instance = new ExportProjectAction();
    
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
        super("Export");
        setEnabled(false);
    }

    private static List<String> getWorldClasses()
    {
        List<String> worldClasses= new LinkedList<String>();
        try {
            GClass[] classes = GreenfootMain.getInstance().getPackage().getClasses();
            for (int i = 0; i < classes.length; i++) {
                GClass cls = classes[i];
                if(cls.isWorldSubclass()) {
                    Class realClass = cls.getJavaClass();   
                    if (GreenfootUtil.canBeInstantiated(realClass)) {                  
                        worldClasses.add(cls.getName());
                    }                    
                }
            }
        }
        catch (ProjectNotOpenException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        catch (PackageNotFoundException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        catch (RemoteException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        return worldClasses;
    }

    

    public void actionPerformed(ActionEvent ae)
    {
        
        
        GProject project = GreenfootMain.getInstance().getProject();
        
        if(!project.isCompiled())  {
            boolean isCompiled = showCompileDialog(project);
            if(!isCompiled) {               
                //Cancel export
                return;
            }
        }
        
        String jarName = project.getName() + ".jar";
        String htmlName = project.getName() + ".html";
        String scenarioName = project.getName();

        String title = project.getName() + " Applet";
        int width = WorldHandler.getInstance().getWorldCanvas().getWidth();
        int height = WorldHandler.getInstance().getWorldCanvas().getHeight() + 50;  
        
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
        
        File defaultExportDir = new File(projectDir.getParentFile(), scenarioName + "-export");

        File libDir = Config.getGreenfootLibDir();
        
        File greenfootDir = new File(libDir, "standalone");
        if (defaultExportDir.exists()) {
            defaultExportDir.delete();
        }
        
        if(exportDialog == null) {
            exportDialog = new ExportDialog(GreenfootMain.getInstance().getFrame(), getWorldClasses(), defaultExportDir);
        }
        boolean okPressed = exportDialog.display();
        if(!okPressed) {
            return;
        }
                
        File exportDir = exportDialog.getExportLocation();
        String worldClass = exportDialog.getWorldClass();
        boolean  includeExtraControls = exportDialog.includeExtraControls();
        
        exportDir.mkdir();
        JarCreator jarCreator = new JarCreator(exportDir, jarName);
        jarCreator.addDir(projectDir);

        jarCreator.addDir(greenfootDir);

        File standAloneProperties = new File(projectDir, "standalone.properties");

        Properties p = new Properties();
        p.put("project.name", scenarioName);
        p.put("main.class", worldClass);
        p.put("controls.extra", "" + includeExtraControls);
        OutputStream os = null;
        try {
            standAloneProperties.createNewFile();
            os = new FileOutputStream(standAloneProperties);
            p.store(os, "Properties for running Greenfoot projects alone.");
        }
        catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        finally {
            try {
                os.close();
            }
            catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        jarCreator.addSkipDir(projectDir.getPath() + "/greenfoot");
        jarCreator.addSkipFile(".cvsignore");

        jarCreator.includeMetaFiles(false);
        jarCreator.includeSource(false);
        
        String mainClass = "greenfoot.util.GreenfootScenarioViewer";
        jarCreator.setMainClass(mainClass);

        jarCreator.create();
        standAloneProperties.delete();
        File outputFile = new File(exportDir, htmlName);
        jarCreator.generateHTMLSkeleton(outputFile, title, width, height);
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