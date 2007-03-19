package greenfoot.platforms;

import java.awt.Component;
import java.io.File;
import java.io.IOException;

/**
 * Interface to classes that contain specialized behaviour for the GreefootUtil
 * class depending on where and how the greenfoot project is running.
 * 
 * @author Poul Henriksen
 * 
 */
public interface GreenfootUtilDelegate
{
    public void createSkeleton(String className, String superClassName, File file, String templateFileName) throws IOException;

    public File getScenarioFromFileBrowser(Component parent);
    
    public String getNewNameFromFileBrowser(Component parent);

    public ClassLoader getCurrentClassLoader();  

    public String getGreenfootLogoPath();
}
