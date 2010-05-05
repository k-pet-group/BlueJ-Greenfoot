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
package bluej.groupwork.cvsnb;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.netbeans.lib.cvsclient.admin.Entry;
import org.netbeans.lib.cvsclient.admin.StandardAdminHandler;
import org.netbeans.lib.cvsclient.command.GlobalOptions;

import bluej.utility.Debug;
import bluej.utility.FileUtility;

/**
 * A CVS admin handler for BlueJ. The main difference between this and the
 * standard admin handler is that the BlueJ handler knows that deleted packages
 * have their metadata (CVS/Entries file) stored in an alternate location
 * (project_root/CVS/deleted).<p>
 * 
 * This admin handler also has a "mild-mannered mode" which pretends that all
 * directories exist. This is useful for certain commands when we want to find
 * out about what exists in the repository, even if it doesn't exist locally.
 * 
 * @author Davin McCall
 */
public class BlueJAdminHandler extends StandardAdminHandler
{
    File projectDir;
    
    /** "mild-mannered" mode */
    private boolean mode = false;
    
    public BlueJAdminHandler(File projectDir)
    {
        this.projectDir = projectDir;
    }
    
    /**
     * Switch the admin handler between mild-mannered mode and
     * standard mode. In mild-mannered mode, directories are reported as
     * existing even if they are not present locally. This can be used to
     * convince the CVS library to give us information about what might
     * be contained within such directories.
     * 
     * @param mode  true for mild-mannered mode
     */
    public void setMildManneredMode(boolean mode)
    {
        this.mode = mode;
    }

    /**
     * Prepare for the deletion of a directory. This is called just before
     * the directory is deleted. For CVS, we move the metadata to the
     * "CVS/deleted" subdirectory of the project so as not to lose it.
     */
    public void prepareDeleteDir(File dir)
    {
        File cvsDir = new File(dir, "CVS");
        if (cvsDir.isDirectory()) {
            String relPath = getRelativePath(dir.getAbsolutePath());
            
            File newMetaDir = new File(projectDir, "CVS");
            newMetaDir = new File(newMetaDir, "deleted");
            newMetaDir = new File(newMetaDir, relPath);
            
            newMetaDir.mkdirs();
            
            if (! cvsDir.renameTo(new File(newMetaDir, "CVS"))) {
                Debug.message("Rename of meta-data directory failed: " + cvsDir);
            }
        }
    }
    
    /**
     * Prepare for creation of new directory. This is called just after the
     * directory has actually been created.
     * 
     * If we have metadata for the named directory stored under the
     * "CVS/deleted" hierarchy, we move it back to the "normal" place.
     */
    public void prepareCreateDir(File dir)
    {
        String relPath = getRelativePath(dir.getAbsolutePath());
        
        File newMetaDir = new File(projectDir, "CVS");
        newMetaDir = new File(newMetaDir, "deleted");
        newMetaDir = new File(newMetaDir, relPath);

        if (newMetaDir.exists()) {
            File cvsDir = new File(newMetaDir, "CVS");
            if (! cvsDir.renameTo(new File(dir, "CVS"))) {
                Debug.message("Rename of meta-data directory failed: " + cvsDir);
            }
            
            while (newMetaDir.delete()) {
                newMetaDir = newMetaDir.getParentFile();
            }
        }
    }
        
    /**
     * Get the relative path (from the project directory) to an absolute directory
     * within. Returns null if the directory is not within the project.
     */
    private String getRelativePath(String directory)
    {
        String projDirString = projectDir.toString();
        if (! directory.startsWith(projDirString)) {
            return null;
        }
        
        directory = directory.substring(projDirString.length());
        if (directory.startsWith(File.pathSeparator)) {
            directory = directory.substring(1);
        }
        
        return directory;
    }
    
    /**
     * Get the directory which actually contains the "CVS" subfolder with metadata
     * for files in the given directory.
     */
    public String getMetaDataPath(String directory)
    {
        String relativeDir = getRelativePath(directory);
        if (relativeDir == null) {
            // On initial checkout, the "project" directory is not actually any
            // specific directory.
            return directory;
        }
        
        File deletedFile = new File(projectDir, "CVS");
        deletedFile = new File(deletedFile, "deleted");
        deletedFile = new File(deletedFile, relativeDir);
        File deletedFileMd = new File(deletedFile, "CVS");
        
        if (deletedFileMd.exists()) {
            return deletedFile.getPath();
        }
        else {
            return directory;
        }
    }
    
    /**
     * Get the translated file location for a file. I.e. If the file resides in a deleted
     * directory, which has been moved to "CVS/deleted", adjust the path accordingly.
     */
    private String getMetaDataPathForFile(String absFilePath)
    {
        String relativeDir = getRelativePath(absFilePath);
        if (relativeDir == null) {
            // On initial checkout, the "project" directory is not actually any
            // specific directory.
            return absFilePath;
        }
        
        File deletedFile = new File(projectDir, "CVS");
        deletedFile = new File(deletedFile, "deleted");
        deletedFile = new File(deletedFile, relativeDir);
        File deletedFileMd = new File(deletedFile.getParentFile(), "CVS");
        
        if (deletedFileMd.exists()) {
            return deletedFile.getPath();
        }
        else {
            return absFilePath;
        }
    }
    
    public void updateAdminData(String localDirectory, String repositoryPath,
            Entry entry, GlobalOptions globalOptions)
        throws IOException
    {
        localDirectory = getMetaDataPath(localDirectory);
        super.updateAdminData(localDirectory, repositoryPath, entry, globalOptions);
    }
    
    public boolean exists(File file)
    {
        File nFile = new File(getMetaDataPath(file.getAbsolutePath()));
        boolean result = super.exists(nFile);
        if (!result) {
            result = super.exists(file);
        }
        return result;
    }

    public Entry getEntry(File file) throws IOException
    {
        file = new File(getMetaDataPathForFile(file.getAbsolutePath()));
        try {
            return super.getEntry(file);
        }
        catch (IOException ioe) {
            if (mode) {
                return null;
            }
            else {
                throw ioe;
            }
        }
    }
    
    public Entry[] getEntriesAsArray(File directory) throws IOException
    {
        directory = new File(getMetaDataPath(directory.getAbsolutePath()));
        return super.getEntriesAsArray(directory);
    }

    @SuppressWarnings("unchecked")
    public Iterator<Entry> getEntries(File directory) throws IOException
    {
        directory = new File(getMetaDataPath(directory.getAbsolutePath()));
        try {
            return super.getEntries(directory);
        }
        catch (IOException ioe) {
            if (mode) {
                return Collections.EMPTY_LIST.iterator();
            }
            else {
                throw ioe;
            }
        }
    }
    
    public void setEntry(File file, Entry entry)
        throws IOException
    {
        file = new File(getMetaDataPathForFile(file.getAbsolutePath()));
        super.setEntry(file, entry);
    }

    public void removeEntry(File file) throws IOException
    {
        file = new File(getMetaDataPathForFile(file.getAbsolutePath()));
        super.removeEntry(file);
        
        // If there are no entries left, we can remove the metadata altogether.
        if (! super.getEntries(file.getParentFile()).hasNext()) {
            File pDir = file.getParentFile();
            File cvsDir = new File(pDir, "CVS");
            FileUtility.deleteDir(cvsDir);
            
            while (pDir.delete()) {
                pDir = pDir.getParentFile();
            }
        }
    }

    public String getRepositoryForDirectory(String directory,
            String repository) throws IOException
    {
        directory = getMetaDataPath(directory);
        try {
            return super.getRepositoryForDirectory(directory, repository);
        }
        catch (IOException ioe) {
            if (mode) {
                File f = new File(directory);
                String parentFile = f.getParent();
                if (parentFile == null) {
                    throw ioe;
                }
                String parentRepository = getRepositoryForDirectory(f.getParent(), repository);
                return parentRepository + "/" + f.getName();
            }
            else {
                throw ioe;
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    public Set<File> getAllFiles(File directory) throws IOException
    {
        directory = new File(getMetaDataPath(directory.getAbsolutePath()));
        
        if (mode && ! directory.exists()) {
            return Collections.emptySet();
        }

        Set<File> s = super.getAllFiles(directory);
        Set<File> newSet = new TreeSet<File>();
        
        // Now we need to convert the path back to the correct path.
        Iterator<File> i = s.iterator();
        while (i.hasNext()) {
            File f = (File) i.next();
            f = new File(directory, f.getName());
            newSet.add(f);
        }
        
        return newSet;
    }
    
    public String getStickyTagForDirectory(File directory)
    {
        directory = new File(getMetaDataPath(directory.getAbsolutePath()));
        return super.getStickyTagForDirectory(directory);
    }
}
