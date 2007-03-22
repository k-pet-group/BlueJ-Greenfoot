package greenfoot.actions;

import greenfoot.core.GClass;
import greenfoot.core.GProject;
import greenfoot.core.GreenfootMain;
import greenfoot.core.WorldHandler;
import greenfoot.gui.ExportDialog;
import greenfoot.gui.GreenfootFrame;
import greenfoot.util.GreenfootUtil;
import greenfoot.util.JarCreator;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.swing.AbstractAction;

import bluej.Config;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;
import bluej.utility.BlueJFileReader;
import bluej.utility.Debug;

/**
 * Action to export a project to a standalone program.
 * 
 * @author Poul Henriksen
 * @version $Id: ExportProjectAction.java 4872 2007-03-22 22:19:19Z polle $
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

    private ExportProjectAction()
    {
        super("Export");
        setEnabled(false);
    }

    public static void main(String[] args)
    {

        test();

    }

    private static void test()
    {
        GProject project = GreenfootMain.getInstance().getProject();
        String jarName = project.getName() + ".jar";
        String htmlName = project.getName() + ".html";
        String scenarioName = project.getName();
        String worldClass = "";
        worldClass = getWorldClasses().get(0);  
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
        //File projectDir = new File("/home/polle/workspace/greenfoot/scenarios/ants");        
        
        File exportDir = new File(projectDir, "export");

        File libDir = Config.getGreenfootLibDir();
        
        File greenfootDir = new File(libDir, "standalone");
        if (exportDir.exists()) {
            exportDir.delete();
        }
        exportDir.mkdir();
        JarCreator jarCreator = new JarCreator(exportDir, jarName);
        jarCreator.addDir(projectDir);

        jarCreator.addDir(greenfootDir);

        File standAloneProperties = new File(projectDir, "standalone.properties");

        Properties p = new Properties();
        p.put("project.name", scenarioName);
        p.put("main.class", worldClass);
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

    private static List<String> getWorldClasses()
    {
        
        List<String> worldClasses= new LinkedList<String>();
        try {
            GClass[] classes = GreenfootMain.getInstance().getPackage().getClasses();
            for (int i = 0; i < classes.length; i++) {
                GClass cls = classes[i];
                if(cls.isWorldSubclass()) {
                    Class realClass = cls.getJavaClass();
                   // if(realClass) {
                   //     worldClass = cls.getName();
                   // }
                    worldClasses.add(cls.getName());
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
        // TODO: Check whether everything is compiled and show dialog if not.
        
        
        GProject project = GreenfootMain.getInstance().getProject();
        String jarName = project.getName() + ".jar";
        String htmlName = project.getName() + ".html";
        String scenarioName = project.getName();
        //String worldClass = "";
      //  worldClass = getWorldClass();  
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
        //File projectDir = new File("/home/polle/workspace/greenfoot/scenarios/ants");        
        
        File defaultExportDir = new File(projectDir.getParentFile(), scenarioName + "-export");

        File libDir = Config.getGreenfootLibDir();
        
        File greenfootDir = new File(libDir, "standalone");
        if (defaultExportDir.exists()) {
            defaultExportDir.delete();
        }
        
        
        ExportDialog exportDialog = new ExportDialog(GreenfootMain.getInstance().getFrame(), getWorldClasses(), defaultExportDir);
        boolean okPressed = exportDialog.display();
        if(!okPressed) {
            return;
        }
        
        //TODO SPAWN NEW THREAD ? Or create new dialog that shows "working..."
        
        File exportDir = exportDialog.getExportLocation();
        String worldClass = exportDialog.getWorldClass();
        
        //TODO: Should it make dir here? Or should it be forced creationg in dialog?
        exportDir.mkdir();
        JarCreator jarCreator = new JarCreator(exportDir, jarName);
        jarCreator.addDir(projectDir);

        jarCreator.addDir(greenfootDir);

        File standAloneProperties = new File(projectDir, "standalone.properties");

        Properties p = new Properties();
        p.put("project.name", scenarioName);
        p.put("main.class", worldClass);
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
        File outputFile = new File(defaultExportDir, htmlName);
        jarCreator.generateHTMLSkeleton(outputFile, title, width, height);
    }

    private void zip()
    {
        String[] filenames = getFilesInProject();

        // Create a buffer for reading the files
        byte[] buf = new byte[1024];

        try {
            // Create the ZIP file
            String outFilename = "outfile.zip";
            ZipOutputStream out = new ZipOutputStream(new FileOutputStream(outFilename));

            // Compress the files
            for (int i = 0; i < filenames.length; i++) {
                FileInputStream in = new FileInputStream(filenames[i]);

                // Add ZIP entry to output stream.
                out.putNextEntry(new ZipEntry(filenames[i]));

                // Transfer bytes from the file to the ZIP file
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }

                // Complete the entry
                out.closeEntry();
                in.close();
            }

            // Complete the ZIP file
            out.close();
        }
        catch (IOException e) {}
    }

    private String[] getFilesInProject()
    {
        // Should include everything from the project except *.class files and
        // *.jar and greenfoot/**
        // These are the files to include in the ZIP file

        try {
            File projectDir = GreenfootMain.getInstance().getProject().getDir();

        }
        catch (ProjectNotOpenException e) {
            e.printStackTrace();
        }
        catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;

    }

    public void addFilesInDir(File dir, List fileList)
    {
        try {

            // get a listing of the directory content
            String[] dirList = dir.list();
            // loop through dirList, and zip the files
            for (int i = 0; i < dirList.length; i++) {
                File f = new File(dir, dirList[i]);
                if (ignore(f)) {
                    continue;
                }
                if (f.isDirectory()) {
                    // if the File object is a directory, call this
                    // function again to add its content recursively
                    addFilesInDir(f, fileList);
                    // loop again
                    continue;
                }

                fileList.add(f);
            }
        }
        catch (Exception e) {
            // handle exception
        }
    }

    private boolean ignore(File f)
    {
        /*
         * String fileName = f.getName(); if(f.isDirectory() &&
         * fileName=="greenfoot") { return true; } fileName.indexOf(".") String
         * suffix = if(f.isFile() &&
         * fileName.substring(-4).equalsIgnoreCase(".jar") )) { return true; }
         */
        return false;

    }
}