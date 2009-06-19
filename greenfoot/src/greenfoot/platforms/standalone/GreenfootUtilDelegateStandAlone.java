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
package greenfoot.platforms.standalone;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.net.URL;


import greenfoot.platforms.GreenfootUtilDelegate;

public class GreenfootUtilDelegateStandAlone implements GreenfootUtilDelegate
{

    public void createSkeleton(String className, String superClassName, File file, String templateFileName)
        throws IOException
    {
        // Not needed in stand alone
    }

    public URL getResource(String path)
    {
        // Resources from the standalone should always be in a jar, which means
        // they should contain the character "!". If we do get a URL back, and
        // it doesn't contain a ! it is probably because it didn't exists, but
        // the webserver produced an error page at the given URL instead of
        // returning a fail. Therefore, we need to explicitly test for the
        // existence of a ! in the returned URL.
        URL res = this.getClass().getClassLoader().getResource(path);
        if (res != null && res.toString().contains("!")) {  
            return res;
        }
        else {
            return null;
        }
    }

    public String getNewProjectName(Component parent)
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
