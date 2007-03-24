package greenfoot.platforms.ide;

import greenfoot.platforms.GreenfootUtilDelegate;
import greenfoot.util.GreenfootUtil;

import java.awt.Component;
import java.io.File;
import javax.swing.filechooser.FileFilter;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

import bluej.Config;
import bluej.runtime.ExecServer;
import bluej.utility.BlueJFileReader;
import bluej.utility.FileUtility;

public class GreenfootUtilDelegateIDE implements GreenfootUtilDelegate
{
    /**
     * 
     * Creates the skeleton for a new class
     * 
     */
    public void createSkeleton(String className, String superClassName, File file, String templateFileName) throws IOException   {
        Dictionary<String, String> translations = new Hashtable<String, String>();
        translations.put("CLASSNAME", className);
        if(superClassName != null) {
            translations.put("SUPERCLASSNAME", superClassName);
        }
        File libDir = Config.getGreenfootLibDir();
        File template = new File(libDir, "templates/" +  templateFileName);  
        BlueJFileReader.translateFile(template, file, translations);
    }
    
    
    public File getScenarioFromFileBrowser(Component parent) {
        return FileUtility.getPackageName(parent);
    }
    
    
    public String getNewProjectName(Component parent)
    {
        return FileUtility.getFileName(parent, Config.getString("pkgmgr.newPkg.title"), Config.getString("pkgmgr.newPkg.buttonLabel"), false, null, true);
    }


    public ClassLoader getCurrentClassLoader() 
    {
        return ExecServer.getCurrentClassLoader();
    }
    
    /**
     * Returns the path to a small version of the greenfoot logo.
     */
    public  String getGreenfootLogoPath()
    {        
        File libDir = Config.getGreenfootLibDir();
        return libDir.getAbsolutePath() + "/imagelib/other/greenfoot.png";        
    }


}
