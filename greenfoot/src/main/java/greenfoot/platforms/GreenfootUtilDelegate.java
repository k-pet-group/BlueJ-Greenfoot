/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010,2011,2012,2018  Poul Henriksen and Michael Kolling
 
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
package greenfoot.platforms;

import greenfoot.GreenfootImage;
import greenfoot.UserInfo;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.awt.Component;
import java.net.URL;
import java.util.List;

/**
 * Interface to classes that contain specialized behaviour for the GreenfootUtil
 * class depending on where and how the greenfoot project is running.
 * 
 * @author Poul Henriksen
 */
@OnThread(Tag.Simulation)
public interface GreenfootUtilDelegate
{
    /**
     * Get some resource from the project, specified by a relative path.
     */
    @OnThread(Tag.Any)
    public URL getResource(String path);
    
    /**
     * Gets a list of sound files (as plain names, e.g. "foo.wav") that
     * accompany this scenario.  For the IDE version, this scans the filesystem,
     * and for the standalone version it looks at a list that's included
     * in the exported JAR.
     * <p>
     * The return value will not be null, but it may have no contents if there
     * was an error (e.g. problem reading the directory/JAR, or no list of sounds in the JAR)
     * and you should not rely on it being accurate (e.g. if files were just added/removed in the sounds directory,
     * or the JAR has been modified since export). 
     */
    @OnThread(Tag.Any)
    public Iterable<String> getSoundFiles();

    /**
     * Get the project-relative path of the Greenfoot logo.
     */
    @OnThread(Tag.Any)
    public String getGreenfootLogoPath();

    /**
     * Find out whether storage is supported in the current setting
     */
    public boolean isStorageSupported();

    /**
     * null if an error or not supported, blank values if no previous storage
     */
    public UserInfo getCurrentUserInfo();

    /**
     * returns whether it was successful
     */
    public boolean storeCurrentUserInfo(UserInfo data);

    /**
     * null if problem, empty list if simply no data
     * 
     * Returns highest data when sorted by integer index 0
     */
    public List<UserInfo> getTopUserInfo(int limit);

    /**
     * returns null if storage not supported or if there was an error.
     */
    public GreenfootImage getUserImage(String userName);

    /**
     * returns null if storage is not supported (or there was an error)
     */
    public String getUserName();

    /**
     * null if problem, empty list if simply no data.
     * 
     * Returns data near the current player when sorted by integer index 0
     */
    public List<UserInfo> getNearbyUserInfo(int maxAmount);
}
