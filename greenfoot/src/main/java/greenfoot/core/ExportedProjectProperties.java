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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;


/**
 * A set of read-only project properties, read from a file.  Used in exported Greenfoot
 * scenarios which just need to read the data without modifying it.  The file is located
 * using the class-loader, which makes sense in an exported JAR.
 */
public class ExportedProjectProperties implements ReadOnlyProjectProperties
{
    /**
     * Name of the greenfoot package file that holds information specific to a
     * package/project
     */
    public static final String GREENFOOT_PKG_NAME = "project.greenfoot";

    /** Holds the actual properties */
    private Properties properties;

    /**
     * Creates a new properties instance with the file loaded from the root of this class loader.
     */
    public ExportedProjectProperties()
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
     * Gets a property as in Java's Properties class. Thread-safe.
     */
    public synchronized String getString(String key, String defaultValue)
    {
        return properties.getProperty(key, defaultValue);
    }


}
