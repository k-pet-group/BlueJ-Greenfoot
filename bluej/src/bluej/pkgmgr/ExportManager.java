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

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.jar.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;

import bluej.Config;
import bluej.utility.*;

/**
 * Component to manage storing projects to jar file format.
 *
 * @author  Michael Kolling
 */
final class ExportManager
{
    private static final String specifyJar = Config.getString("pkgmgr.export.specifyJar");
    private static final String createJarText = Config.getString("pkgmgr.export.createJarText");
    
    private static final String sourceSuffix = ".java";
    private static final String contextSuffix = ".ctxt";
    private static final String packageFilePrefix = "bluej.pk";
    private static final String packageFileBackup = "bluej.pkh";

    private PkgMgrFrame frame;
    private ExportDialog dialog;

    public ExportManager(PkgMgrFrame frame)
    {
        this.frame = frame;
    }

    /**
     * Envoke the "create jar" user function. This starts by displaying the
     * export dialog, then it reads the options and performs the export to jar.
     */
    public void export()
    {
        if (dialog == null)
            dialog = new ExportDialog(frame);
        else
            dialog.updateDialog(frame);
        boolean okay = dialog.display();

        if(!okay)
            return;

        String fileName = FileUtility.getFileName(frame, specifyJar, createJarText, 
                                                 null, false);
        if(fileName == null)
            return;

        String sourceDir = frame.getProject().getProjectDir().getPath();

        createJar(fileName, sourceDir, dialog.getMainClass(), dialog.getSelectedLibs(),
                  dialog.includeSource(), dialog.includePkgFiles());
    }

    /**
     * Export this project to a jar file.
     */
    private void createJar(String fileName, String sourceDir, String mainClass,
                           List<File> userLibs, boolean includeSource, boolean includePkgFiles)
    {
        // Construct classpath with used library jars       
        String classpath = "";
        
        // add jar files from +libs to classpath               
        List<URL> plusLibs = frame.getProject().getPlusLibsContent();
        List<File> plusLibAsFiles = new ArrayList<File>();
        for(Iterator<URL> it = plusLibs.iterator(); it.hasNext();) {
            URL url = it.next();
            try {
                File file = new File(new URI(url.toString()));
                plusLibAsFiles.add(file);
                classpath += " " + file.getName();
            }
            catch(URISyntaxException urie) {
                // nothing at the moment
            }
            
        }
        
        // add jar files from userlibs to classpath
        for(Iterator<File> it = userLibs.iterator(); it.hasNext(); ) {
            classpath += " " + it.next().getName();
        }
        
        File jarFile = null;
        File parent = null;
        
        if(classpath.length() == 0) {
            // if we don't have library jars, just create a single jar file
            if(!fileName.endsWith(".jar"))
                fileName = fileName + ".jar";

            jarFile = new File(fileName);
            
            if(jarFile.exists()) {
                if (DialogManager.askQuestion(frame, "error-file-exists") != 0)
                    return;
            }
        }
        else {
            // if we have library jars, create a directory with the new jar file
            // and all library jar files in it
            if(fileName.endsWith(".jar"))
                fileName = fileName.substring(0, fileName.length() - 4);
            parent = new File(fileName);

            if(parent.exists()) {
                if (DialogManager.askQuestion(frame, "error-file-exists") != 0)
                    return;
            }
            parent.mkdir();
            jarFile = new File(parent, parent.getName() + ".jar");
        }
        
        OutputStream oStream = null;
        JarOutputStream jStream = null;

        try {
            // create manifest
            Manifest manifest = new Manifest();
            Attributes attr = manifest.getMainAttributes();
            attr.put(Attributes.Name.MANIFEST_VERSION, "1.0");
            attr.put(Attributes.Name.MAIN_CLASS, mainClass);
            attr.put(Attributes.Name.CLASS_PATH, classpath);

            // create jar file
            oStream = new FileOutputStream(jarFile);
            jStream = new JarOutputStream(oStream, manifest);

            writeDirToJar(new File(sourceDir), "", jStream, includeSource,
                            includePkgFiles,
                            jarFile.getCanonicalFile());
            if(parent != null) {
                copyLibsToJar(plusLibAsFiles, parent);
                copyLibsToJar(userLibs, parent);
            }
            
            frame.setStatus(Config.getString("pkgmgr.exported.jar"));
        }
        catch(IOException exc) {
            DialogManager.showError(frame, "error-writing-jar");
            Debug.reportError("problen writing jar file: " + exc);
        } finally {
            try {
                if(jStream != null)
                    jStream.close();
            } catch (IOException e) {}
        }
    }

    /**
     * Write the contents of a directory to a jar stream. Recursively called
     * for subdirectories.
     * outputFile should be the canonical file representation of the Jar file
     * we are creating (to prevent including itself in the Jar file)
     */
    private void writeDirToJar(File sourceDir, String pathPrefix,
                               JarOutputStream jStream, boolean includeSource, boolean includePkg, File outputFile)
        throws IOException
    {
        File[] dir = sourceDir.listFiles();
        for(int i = 0; i < dir.length; i++) {
            if(dir[i].isDirectory()) {
                if(!skipDir(dir[i], includePkg)) {
                    writeDirToJar(dir[i], pathPrefix + dir[i].getName() + "/",
                                  jStream, includeSource, includePkg, outputFile);
                }
            }
            else {
                // check against a list of file we don't want to export and also
                // check that we don't try to export the jar file we are writing
                // (hangs the machine)
                if(!skipFile(dir[i].getName(), !includeSource, !includePkg) &&
                    !outputFile.equals(dir[i].getCanonicalFile())) {
                        writeJarEntry(dir[i], jStream, pathPrefix + dir[i].getName());
                }
            }
        }
    }

    /**
     * Copy all files specified in the given list to the new jar directory.
     */
    private void copyLibsToJar(List<File> userLibs, File destDir)
        throws IOException
    {
        for(Iterator<File> it = userLibs.iterator(); it.hasNext(); ) {
            File lib = it.next();
            FileUtility.copyFile(lib, new File(destDir, lib.getName()));
        }
    }

    /** array of directory names not to be included in jar file **/
    private static final String[] skipDirs = { "CVS" };

    /**
     * Test whether a given directory should be skipped (not included) in
     * export.
     */
    private boolean skipDir(File dir, boolean includePkg)
    {
        if (dir.getName().equals(Project.projectLibDirName))
            return ! includePkg;
        
        for(int i = 0; i < skipDirs.length; i++) {
            if(dir.getName().equals(skipDirs[i]))
                return true;
        }
        return false;
    }

    /**
     * Checks whether a file should be skipped during a copy operation.
     * BlueJ specific files (bluej.pkg and *.ctxt) and - optionally - Java
     * source files are skipped.
     */
    private boolean skipFile(String fileName, boolean skipSource, boolean skipPkg)
    {
        if(fileName.equals(packageFileBackup))
            return true;
        
        if(fileName.endsWith(sourceSuffix))
            return skipSource;

        if(fileName.startsWith(packageFilePrefix) || fileName.endsWith(contextSuffix))
            return skipPkg;

        return false;
    }

    /**
     * Write a jar file entry to the jar output stream.
     * Note: entryName should always be a path with / seperators
     *       (NOT the platform dependant File.seperator)
     */
    private void writeJarEntry(File file, JarOutputStream jStream,
                                  String entryName)
        throws IOException
    {
        InputStream in = null;
        try {
            in = new FileInputStream(file);
            jStream.putNextEntry(new ZipEntry(entryName));
            FileUtility.copyStream(in, jStream);
        }
        catch(ZipException exc) {
            Debug.message("warning: " + exc);
        }
        finally {
            if(in != null)
                in.close();
        }
    }
}
