/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2014,2016  Michael Kolling and John Rosenberg
 
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

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Reference to the BlueJ package file(s). This includes references to the old
 * file called bluej.pkg as well as the current file named package.bluej.
 * 
 * There are (or will be) three versions of BlueJ that handles these package
 * files differently:
 * 
 * <ul>
 * <li><i>Old BlueJ:</i> support only the .pkg extension. This is all versions
 * before BlueJ 2.3.0.</li>
 * 
 * 
 * <li><i>Transition BlueJ:</i> support both .pkg and .bluej extension. If .pkg
 * exists, it will load from this file. It will always attempt to save to both
 * .bluej and .pkg. The first transition version is BlueJ 2.3.0.</li>
 * 
 * <li><i>New BlueJ:</i> support for .bluej, and limited for .pkg. If .pkg
 * exists, it will load from this file. If .pkg doesn't exist it is NOT created.
 * It will always attempt to save to .bluej. If .pkg exists it will also save to
 * that. No versions of this exists yet. This has been implemented in version 
 * 2.6.0</li>
 * <ul>
 * 
 * One implication of this is that a project that has been created with a New
 * version of BlueJ can not be opened with an Old version of BlueJ (it can be
 * opened by a Transition version though). The alternative would be to keep the
 * .pkg around forever, which is not what we want. And if the transition period
 * is long enough, it should not create to many problems.
 * 
 * @author Poul Henriksen
 */
@OnThread(Tag.Any)
public class BlueJPackageFile
    implements PackageFile
{
    private static final String pkgfileName = "package.bluej";
    private static final String oldPkgfileName = "bluej.pkg";

    private File dir;
    private File pkgFile;
    private File oldPkgFile;

    /**
     * @see PackageFileFactory
     */
    @OnThread(Tag.Any)
    BlueJPackageFile(File dir)
    {
        this.dir = dir;
        this.pkgFile = new File(dir, pkgfileName);
        this.oldPkgFile = new File(dir, oldPkgfileName);
    }

    public String toString()
    {
        return "BlueJ package file in: " + dir.toString();
    }

    /**
     * Whether a BlueJ package file exists in this directory.
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
        if (packageFile.exists()) {
            return true;
        }

        File oldPackageFile = new File(dir, oldPkgfileName);
        return oldPackageFile.exists();
    }

    /**
     * Will first try to load from the old package file (.pkg) if that fails, it
     * will try to load from the new one (package.bluej)
     */
    public void load(Properties p)
        throws IOException
    {
        FileInputStream input = null;
        try {
            // First, try to load from the new  package file since, if it exists,
            // will be the most up-to-date.
            if (pkgFile.canRead()) {
                input = new FileInputStream(pkgFile);
            }
            else if (oldPkgFile.canRead()) {
                input = new FileInputStream(oldPkgFile);
            }
            else {
                throw new IOException("Can't read from package file(s) in: " + this);
            }
            p.load(input);
        }
        finally {
            if (input != null) {
                input.close();
            }
        }
    }

    /**
     * Save the given properties to the file.
     * <p>
     * 
     * Store properties to both package files. This method will always attempt
     * to store the properties to both package files (.bluej and .pkg).
     * <p>
     * 
     * It should fail if the oldPkgFile exists but can't be written, because
     * this is the first one to be loaded if both exists and it would then
     * result in inconsistent properties. If it manages to store to the
     * oldPkgFile it doesn't matter if it fails to store to the new one, since
     * whenever the old one is present, that will be loaded first in all
     * versions of BlueJ.
     * 
     * @throws IOException if something goes wrong while trying to write the
     *             properties.
     */
    public void save(Properties props)
        throws IOException
    {
        // TODO: In some future version of BlueJ the createNewFile invocation
        // below should be removed to get rid of the old .pkg file.
        //
        // And, when the invocation is removed the Javadoc for the method should
        // include the following lines:
        //
        // * Store properties to both package files. It always try to store to
        // * the pkgFile, and if the oldPkgFile exists it will also try to store
        // * to that.

        if (oldPkgFile.exists()) {
            if (!oldPkgFile.canWrite()) {
                throw new IOException("BlueJ package file not writable: " + oldPkgFile);
            }
            saveToFile(props, oldPkgFile);
        }

        pkgFile.createNewFile();
        if (!pkgFile.canWrite()) {
                throw new IOException("BlueJ package file not writable: " + pkgFile);
        }
        else {
            saveToFile(props, pkgFile);
        }
    }

    private void saveToFile(Properties props, File file)
        throws IOException
    {
        FileOutputStream output = null;
        try {
            output = new FileOutputStream(file);
            String header = "BlueJ package file";
            props.store(output, header);
        }
        catch (IOException e) {
            throw new IOException("Error when storing properties to BlueJ package file: " + file);
        }
        finally {
            if (output != null) {
                output.close();
            }
        }
    }

    /**
     * Check if the given name matches the name of a BlueJ package file (either
     * bluej.pkg or package.bluej).
     */
    public static boolean isPackageFileName(String name)
    {
        return name.equals(pkgfileName) || name.equals(oldPkgfileName);
    }
    
    /**
     * Check if the given name matches the name of the old BlueJ package file
     * (bluej.pkg).
     */
    public static boolean isOldPackageFileName(String name)
    {
        return name.equals(oldPkgfileName);
    }

    /**
     * Creates the two package files if they don't already exist. If only
     * package.bluej exists it will not create bluej.pkg.
     * 
     * @return true if it created a package file, false if it didn't create any package files.
     * @param dir The directory to create package files in.
     * @throws IOException If the package file(s) could not be created.
     * 
     */
    public boolean create()
        throws IOException
    {
        File pkgFile = new File(dir, pkgfileName);
        File oldPkgFile = new File(dir, oldPkgfileName);

        boolean created = false;
        if (pkgFile.exists() && !oldPkgFile.exists()) {
            return false;
        }

        if (!pkgFile.exists()) {
            pkgFile.createNewFile();
            created = true;
        }
        return created;
    }
}
