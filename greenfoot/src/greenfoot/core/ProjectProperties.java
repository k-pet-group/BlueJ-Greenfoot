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
public class ProjectProperties
{
    /** String printed in the top of the properties file. */
    private static final String FILE_HEADER = "Greenfoot properties";

    /**
     * Name of the greenfoot package file that holds information specific to a
     * package/project
     */
    public static final String GREENFOOT_PKG_NAME = "project.greenfoot";

    /** Holds the actual properties */
    private Properties properties;

    /** Reference to the file that holds the properties */
    private File propsFile;

    /**
     * Creates a new properties instance for the project in the given directory.
     * The directory has to exist.
     * 
     * @param projectDir
     */
    public ProjectProperties(File projectDir)
    {
        properties = new Properties();
        load(projectDir);
    }
    
    /**
     * Creates a new properties instance with the file loaded from the root of this class loader.
     * 
     * @param projectDir
     */
    public ProjectProperties()
    {
        properties = new Properties();
        load();
    }

    /**
     * Tries to load the project-file with the default class loader.
     */
    private void load()
    {
        URL probsFile = this.getClass().getResource("/" + GREENFOOT_PKG_NAME);
        InputStream is = null;
        try {
            is = probsFile.openStream();
            properties.load(is);
        }
        catch (IOException ioe) {
            // if it does not exist, we will create it later if something needs
            // to be written to it. This makes it work with scenarios created
            // with earlier versions of greenfoot that does not contain a
            // greenfoot project properties file.
        }
        finally {
            if (is != null) {
                try {
                    is.close();
                }
                catch (IOException e) {}
            }
        }
    }

    /**
     * Loads the properties from the greenfoot properties file in the given
     * directory.
     * 
     * @param projectDir The project dir.
     * @throws IllegalArgumentException If directory can't be read or written.
     */
    private void load(File projectDir)
    {
        propsFile = new File(projectDir, GREENFOOT_PKG_NAME);

        InputStream is = null;
        try {
            is = new FileInputStream(propsFile);
            properties.load(is);
        }
        catch (IOException ioe) {
            // if it does not exist, we will create it later if something needs
            // to be written to it. This makes it work with scenarios created
            // with earlier versions of greenfoot that does not contain a
            // greenfoot project properties file.
        }
        finally {
            if (is != null) {
                try {
                    is.close();
                }
                catch (IOException e) {}
            }
        }
    }
    
    
    /**
     * Stores these properties to the file.
     * 
     * @throws IOException If the properties can't be written to the properties
     *             file.
     */
    public synchronized void save()
    {
        OutputStream os = null;
        try {
            os = new FileOutputStream(propsFile);
            properties.store(os, FILE_HEADER);
        }
        catch (FileNotFoundException e) {}
        catch (IOException e) {}
        finally {
            if (os != null) {
                try {
                    os.close();
                }
                catch (IOException e) {}
            }
        }
    }


    /**
     * Sets a property as in Java's Properties class. Thread-safe. 
     */
    public synchronized void setString(String key, String value)
    {
        properties.setProperty(key, value);
    }


    /**
     * Gets a property as in Java's Properties class. Thread-safe.
     */
    public synchronized String getString(String key)
    {
        return properties.getProperty(key);
    }


    /**
     * Sets an int property as in Java's Properties class. Thread-safe.
     */
    public synchronized void setInt(String key, int value)
    {
        properties.setProperty(key, Integer.toString(value));
    }


    /**
     * Gets an int property as in Java's Properties class. Thread-safe.
     */
    public synchronized int getInt(String key) throws NumberFormatException
    {
        String number = properties.getProperty(key);
        return Integer.parseInt(number);
    }
    
    /**
     * Sets a boolean property as in Java's Properties class. Thread-safe. 
     */
    public synchronized void setBoolean(String key, boolean value)
    {
        properties.setProperty(key, Boolean.toString(value));        
    }
    
    /**
     * Gets a boolean property as in Java's Properties class. 
     * Allows the specification of a default value. Thread-safe.
     */
    public synchronized boolean getBoolean(String key, String defaultValue)
    {
        String bool = properties.getProperty(key, defaultValue);
        return Boolean.parseBoolean(bool);
    }
    
    /**
     * Remove a property; return its old value. Thread-safe.
     * @param key  The property name
     */
    public synchronized String removeProperty(String key)
    {
        return (String) properties.remove(key);
    }


    /**
     * Stores the API version. Thread-safe.
     */
    public void setApiVersion(String version)
    {
        properties.setProperty("version", version);
    }


    /**
     * Attempts to find the version number the greenfoot API that a greenfoot
     * project was created with. If it can not find a version number, it will
     * return Version.NO_VERSION. Thread-safe.
     * 
     * @return API version
     */
    public Version getAPIVersion()
    {
        String versionString = properties.getProperty("version");
        Version version = new Version(versionString);
        return version;
    }
    
    /**
     * Gets an image for the given class. The images are cached to avoid loading
     * images several times. This method is thread-safe.
     * 
     * @param className If it is a qualified name, the package is ignored.
     *            Returns null, if there is no entry for this class in the
     *            properties.
     * @return The image.
     */
    public GreenfootImage getImage(String className)
    {
        return GreenfootUtil.getGreenfootImage(className, getString("class." + className + ".image"));
    }
    
    /**
     * Remove the cached version of an image for a particular class. This should be
     * called when the image for the class is changed. Thread-safe.
     */
    public void removeCachedImage(String className)
    {
       GreenfootUtil.removeCachedImage(className);
    }

}
