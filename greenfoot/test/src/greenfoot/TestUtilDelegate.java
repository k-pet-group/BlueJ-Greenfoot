/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010,2011,2012,2016,2018  Poul Henriksen and Michael Kolling 
 
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
package greenfoot;

import greenfoot.platforms.GreenfootUtilDelegate;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

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
    
    public Iterable<String> getResources(String path)
    {
        return new ArrayList<String>();
    }

    public String getGreenfootLogoPath()
    {
        return "resources/images/greenfoot-icon-16.png";
    }

    public String getNewProjectName(Component parent)
    {
        return null;
    }

    public File getScenarioFromFileBrowser(Component parent)
    {
        return null;
    }

    @Override
    public Iterable<String> getSoundFiles()
    {
        return null;
    }

    @Override
    public boolean isStorageSupported()
    {
        return false;
    }

    @Override
    public UserInfo getCurrentUserInfo()
    {
        return null;
    }

    @Override
    public boolean storeCurrentUserInfo(UserInfo info)
    {
        return false;
    }

    @Override
    public List<UserInfo> getTopUserInfo(int limit)
    {
        return null;
    }

    @Override
    public GreenfootImage getUserImage(String userName)
    {
        return null;
    }

    @Override
    public String getUserName()
    {
        return null;
    }

    @Override
    public List<UserInfo> getNearbyUserInfo(int maxAmount)
    {
        return null;
    }
}
