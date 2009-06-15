/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
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
package bluej.pkgmgr;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Reference to the Greenfoot project file(s). A Greenfoot project file is
 * basically just a BlueJ package with some extra information added.
 * 
 * @author Poul Henriksen
 */
public class GreenfootProjectFile
    implements PackageFile
{
    private static final String pkgfileName = "project.greenfoot";
    private File dir;
    private File pkgFile;

    /**
     * @see PackageFileFactory
     */
    GreenfootProjectFile(File dir)
    {
        this.dir = dir;
        this.pkgFile = new File(dir, pkgfileName);
    }

    public String toString()
    {
        return dir.toString() + File.separator + pkgfileName;
    }

    public void load(Properties p)
        throws IOException
    {
        FileInputStream input = null;
        try {
            if (pkgFile.canRead()) {
                input = new FileInputStream(pkgFile);
            }
            else {
                throw new IOException("Can't read from project file: " + pkgFile);
            }
            p.load(input);
        }
        finally {
            if(input != null) {
                input.close();
            }
        }
    }

    /**
     * Save the given properties to the file.
     * 
     * @throws IOException if something goes wrong while trying to write the
     *             file.
     */
    public void save(Properties props)
        throws IOException
    {
        if (!pkgFile.canWrite()) {
            throw new IOException("Greenfoot project file not writable: " + this);
        }

        // This is Greenfoot, this file will contain Greenfoot specific
        // properties as well that we don't want to overwrite, so we load the
        // file first. The greenfoot properties will be loaded through the class
        // greenfoot.core.ProjectProperties. 

        // TODO: It would probably be better to always forward to the BlueJ
        // version of the properties instead of writing to the same file from
        // two VMs. That would fix the issue with things not being written from
        // the Greenfoot VM if exited in the BlueJ vm.
        try {
            Properties greenfootProps = new Properties();
            FileInputStream input = new FileInputStream(pkgFile);
            greenfootProps.load(input);
            
            // Make sure that we do not overwrite any values in props.
            greenfootProps.putAll(props);
            props.putAll(greenfootProps);
        }
        catch (IOException e) {
            // If we can't load it for some reason, we just continue.
        }

        FileOutputStream output = null;
        try {
            output = new FileOutputStream(pkgFile);
            String header = "Greenfoot project file";
            props.store(output, header);
        }
        catch (IOException e) {
            throw new IOException("Error when storing properties to Greenfoot project file: " + this);
        }
        finally {
            if (output != null) {
                output.close();
            }
        }
    }
    
    /**
     * Whether a Greenfoot package file exists in this directory.
     */
    public static boolean exists(File dir)
    {
        if (dir == null)
            return false;

        // don't try to test Windows root directories (you'll get in
        // trouble with disks that are not in drives...).
        if (dir.getPath().endsWith(":\\"))
            return false;

        if (!dir.isDirectory())
            return false;

        File packageFile = new File(dir, pkgfileName);
        return packageFile.exists();
    }
    
    /**
     * Whether this file is the name has the name of a Greenfoot project file.
     */
    public static boolean isProjectFileName(String fileName)
    {
        return fileName.endsWith(pkgfileName);
    }
    
    /**
     * Creates the Greenfoot project file if it does not already exist. 
     * 
     * @return true if it created a package file, false if it didn't create any package files.
     * @param dir The directory to create package file in.
     * @throws IOException If the package file could not be created.
     * 
     */
    public boolean create()
        throws IOException
    {
        File pkgFile = new File(dir, pkgfileName);

        if (pkgFile.exists()) {
            return false;
        }
        else {
            pkgFile.createNewFile();
            return true;
        }     
    }

}
