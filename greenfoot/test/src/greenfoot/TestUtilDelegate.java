/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009  Poul Henriksen and Michael Kolling 
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
/*
 * Created on Nov 16, 2008
 */
package greenfoot;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import greenfoot.platforms.GreenfootUtilDelegate;

public class TestUtilDelegate implements GreenfootUtilDelegate
{
    public void createSkeleton(String className, String superClassName, File file, String templateFileName)
        throws IOException
    {
        return;
    }

    public URL getResource(String path)
    {
        return getClass().getClassLoader().getResource(path);
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
