/*
 * Created on Nov 16, 2008
 */
package greenfoot;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import greenfoot.platforms.GreenfootUtilDelegate;

public class TestUtilDelegate implements GreenfootUtilDelegate
{
    public void createSkeleton(String className, String superClassName, File file, String templateFileName)
        throws IOException
    {
        return;
    }

    public ClassLoader getCurrentClassLoader()
    {
        return getClass().getClassLoader();
    }

    public String getGreenfootLogoPath()
    {
        String classes = getClass().getClassLoader().getResource(".").toString();
        File startingDir = null;
        try {
            startingDir = (new File(new URI(classes)).getParentFile());
        }
        catch (URISyntaxException e) {
            e.printStackTrace();
        }
        while((startingDir != null) &&
                !(new File(startingDir, "images").isDirectory())) {
            startingDir = startingDir.getParentFile();
        }
        File imageFile = new File(startingDir, "images/greenfoot.png");
        return imageFile.toString();
    }

    public String getNewProjectName(Component parent)
    {
        // TODO Auto-generated method stub
        return null;
    }

    public File getScenarioFromFileBrowser(Component parent)
    {
        // TODO Auto-generated method stub
        return null;
    }}
