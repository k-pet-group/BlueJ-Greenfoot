/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009, 2010  Poul Henriksen and Michael Kolling 
 
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
package greenfoot.core;

import greenfoot.GreenfootImage;
import greenfoot.util.GreenfootUtil;
import greenfoot.util.Version;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Properties;


/**
 * Represents the persistent properties associated with a greenfoot project. It
 * represents both the file that holds the properties and the actual properties.
 * 
 * @author Poul Henriksen
 */
public interface ProjectProperties extends ReadOnlyProjectProperties
{
    /**
     * Sets a property as in Java's Properties class. Thread-safe. 
     */
    public void setString(String key, String value);

    /**
     * Sets an int property as in Java's Properties class. Thread-safe.
     */
    public default void setInt(String key, int value)
    {
        setString(key, Integer.toString(value));
    }
    
    /**
     * Sets a boolean property as in Java's Properties class. Thread-safe. 
     */
    public default void setBoolean(String key, boolean value)
    {
        setString(key, Boolean.toString(value));        
    }
    
    /**
     * Remove a property; return its old value. Thread-safe.
     * @param key  The property name
     */
    public String removeProperty(String key);
    
    public void save();
}
