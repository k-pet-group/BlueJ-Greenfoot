package greenfoot.platforms.standalone;

import java.awt.Component;
import java.io.File;
import java.io.IOException;

import greenfoot.platforms.GreenfootUtilDelegate;

public class GreenfootUtilDelegateStandAlone implements GreenfootUtilDelegate
{

    public void createSkeleton(String className, String superClassName, File file, String templateFileName)
        throws IOException
    {
        // Not needed in stand alone
    }

    public ClassLoader getCurrentClassLoader()
    {
        return this.getClass().getClassLoader();
    }

    public String getNewNameFromFileBrowser(Component parent)
    {
        // Not needed in stand alone
        return null;
    }

    public File getScenarioFromFileBrowser(Component parent)
    {
        // Not needed in stand alone
        return null;
    }
    
    /**
     * Returns the path to a small version of the greenfoot logo.
     */
    public String getGreenfootLogoPath()
    {    
        return this.getClass().getClassLoader().getResource("greenfoot.png").toString();
    }

}
