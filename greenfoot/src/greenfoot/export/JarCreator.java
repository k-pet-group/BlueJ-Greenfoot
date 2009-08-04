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
package greenfoot.export;

import greenfoot.core.GProject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.rmi.RemoteException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import bluej.Config;
import bluej.extensions.ProjectNotOpenException;
import bluej.pkgmgr.Project;
import bluej.utility.BlueJFileReader;
import bluej.utility.Debug;
import bluej.utility.FileUtility;

/**
 * Utility class to create jar or zip files from a Greenfoot project.
 * 
 * @author Poul Henriksen <polle@polle.org>
 * 
 */
public class JarCreator
{
    private static final String SOURCE_SUFFIX = ".java";    

    /** Should source files be included in the jar? */
    private boolean includeSource;

    /** The main class attribute for the JAR. */
    private String mainClass;
    
    /** Directory where the jar is exported to. */
    private File exportDir;
    
    /** Directory to be exported. */
    private File projectDir;
    
    /** Name of the jar file that will be created. */
    private String jarName;
    
    /** List of extra jars that should be put in the same dir as the created jar (the exportDir)*/
    private List<File> extraJars = new LinkedList<File>();
    
    /** List of extra jars whose contents should be put into the created jar */
    private List<File> extraJarsInJar = new LinkedList<File>();

    /** List of paths to external jars that should be included in the manifest's classpath. */
    private List<String> extraExternalJars = new LinkedList<String>();
    
    private List<File> dirs = new LinkedList<File>();
    private List<PrefixedFile> prefixDirs = new LinkedList<PrefixedFile>();

    /** array of directory names not to be included in jar file * */
    private List<String> skipDirs = new LinkedList<String>();

    /** array of file names not to be included in jar file * */
    private List<String> skipFiles = new LinkedList<String>();
    
    /** The maninfest */ 
    private Manifest manifest = new Manifest();
    
    /** Properties that contains information read by the GreenfootScnearioViewer */
    private Properties properties;
 
    private boolean isZip = false;

    /**
     * Prepares a new jar creator. Once everything is set up, call create()
     * 
     * @param exportDir The directory in which to store the jar-file and any
     *            other resources required. Must exist and be writable
     * @param jarName The name of the jar file.
     */
    public JarCreator(File exportDir, String jarName)
    {

        File jarFile = new File(exportDir, jarName);
        if (!jarFile.canWrite() && jarFile.exists()) {
            throw new IllegalArgumentException("Cannot write file: " + jarFile);
        }
        this.exportDir = exportDir;
        this.jarName = jarName;
        properties = new Properties();
    }

    /**
     * Export the class files for a project.
     * 
     * Convenience constructor that includes settings that are common for all
     * projects and export types. This will exclude BlueJ metafiles.
     * 
     * @param project The project to be exported.
     * @param exportDir The directory to export to.
     * @param jarName Name of the jar file that should be created.
     * @param worldClass Name of the main class.
     * @param lockScenario Should the exported scenario include 'act'
     *            and speedslider.
     */
    public JarCreator(GProject project, File exportDir, String jarName, String worldClass, boolean lockScenario) 
    {   
        this(exportDir, jarName);
        
        // get the project directory        
        try {
            projectDir = project.getDir();
        }
        catch (ProjectNotOpenException e) {
            e.printStackTrace();
        }
        catch (RemoteException e) {
            e.printStackTrace();
        }
        
        String scenarioName = project.getName();
        
        addFile(projectDir);

        // skip CVS stuff
        addSkipDir("CVS");
        addSkipFile(".cvsignore");
        
        // skip Subversion files
        addSkipDir(".svn");
        
        // skip Mac files
        addSkipFile(".DS_Store");
        
        // skip doc dir
        addSkipDir(projectDir.getPath() + System.getProperty("file.separator") + "doc");
        
        // skip the export dir (in case it is in the projectDir)
        addSkipDir(exportDir.getAbsolutePath());
        
        // skip the greenfoot subdir that are in the projects
        addSkipDir(projectDir.getPath() + System.getProperty("file.separator") + "greenfoot");
        
        // skip BlueJ files
        addSkipFile(".ctxt");
        addSkipFile("bluej.pkg");
        addSkipFile("bluej.pkh");   
        
        // Exlude +libs. These should be added with the addJar() method.
        addSkipDir(Project.projectLibDirName);
        
        // Set the main class
        String mainClass = "greenfoot.export.GreenfootScenarioViewer";
        setMainClass(mainClass);
        
        // Add the properties read by the GreenfootScenarioViewer
        properties.put("project.name", scenarioName);
        properties.put("main.class", worldClass);
        properties.put("scenario.lock", "" + lockScenario);
        properties.put("scenario.viewer.appletInfo", Config.getString("scenario.viewer.appletInfo"));
        properties.put("run.once", Config.getString("run.once"));
        properties.put("run.simulation", Config.getString("run.simulation"));
        properties.put("pause.simulation", Config.getString("pause.simulation"));
        properties.put("reset.world", Config.getString("reset.world"));
        properties.put("controls.speed.label", Config.getString("controls.speed.label"));
        properties.put("controls.runonce.longDescription", Config.getString("controls.runonce.longDescription"));
        properties.put("controls.runonce.shortDescription", Config.getString("controls.runonce.shortDescription"));
        properties.put("controls.run.longDescription", Config.getString("controls.run.longDescription"));
        properties.put("controls.run.shortDescription", Config.getString("controls.run.shortDescription"));
        properties.put("controls.pause.longDescription", Config.getString("controls.pause.longDescription"));
        properties.put("controls.pause.shortDescription", Config.getString("controls.pause.shortDescription"));
        properties.put("controls.run.button", Config.getString("controls.run.button"));
        properties.put("controls.pause.button", Config.getString("controls.pause.button"));
        properties.put("controls.reset.longDescription", Config.getString("controls.reset.longDescription"));
        properties.put("controls.reset.shortDescription", Config.getString("controls.reset.shortDescription"));
        properties.put("controls.speedSlider.tooltip", Config.getString("controls.speedSlider.tooltip"));
              
    }
    
    /**
     * 
     * Export source code. Includes all the project files. Creates a dir in the
     * zip with the same name as the project dir.
     *
     * Convenience constructor that includes settings that are common for all
     * projects and export types.
     * 
     * @param project The project to be exported.
     * @param exportDir The directory to export to.
     * @param jarName Name of the jar file that should be created.
     * @param worldClass Name of the main class.
     * @param includeExtraControls Should the exported scenario include 'act'
     *            and speedslider.
     */
    public JarCreator(GProject project, File exportDir, String zipName) 
    {   
        this(exportDir, zipName);
        
        isZip = true;
        
        // get the project directory        
        try {
            projectDir = project.getDir();
        }
        catch (ProjectNotOpenException e) {
            e.printStackTrace();
        }
        catch (RemoteException e) {
            e.printStackTrace();
        }
        
        
        addFile(projectDir);        
        
        // skip CVS stuff
        addSkipDir("CVS");
        addSkipFile(".cvsignore");
        
        // skip Subversion files
        addSkipDir(".svn");
        
        // skip Mac files
        addSkipFile(".DS_Store");
        
        // skip doc dir
        addSkipDir(projectDir.getPath() + System.getProperty("file.separator") + "doc");
        
        // skip the export dir (in case it is in the projectDir)
        addSkipDir(exportDir.getAbsolutePath());
        
        // skip the greenfoot subdir that are in the projects
        addSkipDir(projectDir.getPath() + System.getProperty("file.separator") + "greenfoot");
        
        // skip BlueJ files
        addSkipFile("bluej.pkg");
        addSkipFile("bluej.pkh");   
        
        includeSource(true);
    }
    
    /**
     * Creates the jar file with the current settings.
     * 
     */
    public void create()
    {        
        File jarFile = new File(exportDir, jarName);
        File propertiesFile = null;
        OutputStream oStream = null;
        ZipOutputStream jStream = null;

        try {
            oStream = new BufferedOutputStream(new FileOutputStream(jarFile));
            String pathPrefix = ""; // Put everything in top level of jar
            if (! isZip) {
                // It is a jar file so we write the manifest and the properties.
                writeManifest();
                propertiesFile = new File(projectDir, "standalone.properties");
                writePropertiesFile(propertiesFile);
                jStream = new JarOutputStream(oStream, manifest);
            }
            else {
                // It is a zip, so we want a dir with the project name inside the zip
                pathPrefix = projectDir.getName() + "/";
                jStream = new ZipOutputStream(oStream);
            }
            // Write contents of directories added
            for(File dir : dirs) {
                writeFileToJar(dir, pathPrefix, jStream, jarFile.getCanonicalFile(), true);
            }
            for(PrefixedFile dir : prefixDirs) {
                writeFileToJar(dir.getFile(), pathPrefix + dir.getPrefix(), jStream, jarFile.getCanonicalFile(), true);
            }
            for(File jar : extraJarsInJar) {
                writeJarToJar(jar, jStream);
            }
            copyLibsToDir(extraJars, exportDir);            
        }
        catch (IOException exc) {
            Debug.reportError("problen writing jar file: " + exc);
        }
        finally {
            try {
                if (jStream != null)
                    jStream.close();
            }
            catch (IOException e) {}
            if(propertiesFile != null) {
                propertiesFile.delete();
            }
        }
    }

    
    /**
     * Writes the properties to the given file.
     */
    private void writePropertiesFile(File file)
    {
        OutputStream os = null;
        try {
            file.createNewFile();
            os = new BufferedOutputStream(new FileOutputStream(file));
            properties.store(os, "Properties for running Greenfoot scenarios alone.");
        }
        catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        finally {
            try {
                os.close();
            }
            catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    /**
     * Puts an entry into to the manifest file.
     * 
     * @param key The key 
     * @param value The value
     */
    public void putManifestEntry(String key, String value) {
        Attributes attr = manifest.getMainAttributes();
        value = fixNewlines(value);
        attr.put(new Attributes.Name(key), value);
    }
    
    private String fixNewlines(String value)
    {
        StringBuffer buffer = new StringBuffer(value.length());
        
        //(?m) MULTILINE is required for $ and ^ to work. 
        //(?s) DOTALL makes . match newlines as well.
        String newLineRegExp = "(?m)(?s)$.^";
        //\\z matches end of input, so this will match all trailing newlines.
        String trailingNewLineReqExp = "$.\\z";
		String[] lines = value.split(newLineRegExp + "|" + trailingNewLineReqExp );
        for (int i = 0; i < lines.length; i++) {
            String string = lines[i];
            if(i!=0) {
                buffer.append("<br>");
            }
            buffer.append(string);
        }
        return buffer.toString();
    }

    /**
     * Writes entries to the manifest file.
     *
     */
    private void writeManifest()
    {
        // Construct classpath with used library jars
        String classpath = "";

        // add extra jars to classpath
        for (Iterator<File> it = extraJars.iterator(); it.hasNext();) {
            classpath += " " + it.next().getName();
        }
        
        // add extra external jars to classpath
        for (Iterator<String> it = extraExternalJars.iterator(); it.hasNext();) {
            classpath += " " + it.next();
        }
        
        Attributes attr = manifest.getMainAttributes();
        attr.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        attr.put(Attributes.Name.MAIN_CLASS, mainClass);
        attr.put(Attributes.Name.CLASS_PATH, classpath);
    }

    /**
     * Whether to include sources or not.
     */
    public void includeSource(boolean b)
    {
        includeSource = b;
    }
    
    /**
     * Sets the main class for this JAR. The class that contains the main method
     * or Applet class.
     * 
     * @param mainClass
     */
    public void setMainClass(String mainClass)
    {
        this.mainClass = mainClass;
    }

    /**
     * Adds a jar file to be distributed together with this jar-file. It will be
     * copied into the same location as the jar-file created (the exportDir).
     * <br>
     * 
     * This will usually be the jars +libs dir and userlib jars
     * 
     * @param jar A jar file.
     */
    public void addJar(File jar)
    {
        extraJars.add(jar);
    }
    
    /** 
     * Add a jar to the list of extra jars whose contents should be put into the created jar 
     * <br>
     * 
     * This will usually be the jars +libs dir and userlib jars
     * @param jar A jar file.
     */
    public void addJarToJar(File jar)
    {
        extraJarsInJar.add(jar);
    }

    /**
     * Adds a location of an external jar file. This will be added to the
     * classpath of the manifest.
     * 
     * @param  path Usually a URL or a relative path.
     */
    public void addToClassPath(String path)
    {
        extraExternalJars.add(path);
    }
    
    /**
     * Directory or file to include in export.
     * 
     */
    public void addFile(File file)
    {
        dirs.add(file);
    }

    
    /**
     * Directory or file to include in export, with the given prefix added when putting
     * it into the jar.
     * 
     */
    public void addFile(String prefix, File file)
    {
        prefixDirs.add(new PrefixedFile(prefix, file));
    }
    
    /**
     * All dirs that end with the specified string will be skipped.
     * Be aware of platform dependent file separators.
     */
    public void addSkipDir(String dir) {
        skipDirs.add(dir);
    }


    /**
     * All files that end with the specified string will be skipped.
     * Be aware of platform dependent file separators.
     */
    public void addSkipFile(String file)
    {
        skipFiles.add(file);
    }

    /**
     * Write the contents of a directory to a jar stream. Recursively called for
     * subdirectories. outputFile should be the canonical file representation of
     * the Jar file we are creating (to prevent including itself in the Jar
     * file)
     */
    private void writeDirToJar(File sourceDir, String pathPrefix, ZipOutputStream stream, File outputFile)
        throws IOException
    {
        if (!skipDir(sourceDir)) {
            File[] dir = sourceDir.listFiles();
            for (int i = 0; i < dir.length; i++) {
                writeFileToJar(dir[i], pathPrefix, stream, outputFile, false);
            }
        }
    }
    
    /**
     * Writes a file or directory to a jar. Recursively called for
     * subdirectories. outputFile should be the canonical file representation of
     * the Jar file we are creating (to prevent including itself in the Jar
     * file)
     * @param onlyDirContents If sourceFile is a dir, this parameter indicates that the contents of the dir should be added, not the dir itself.
     */
    private void writeFileToJar(File sourceFile, String pathPrefix, ZipOutputStream stream, File outputFile, boolean onlyDirContents)
        throws IOException
    {
        if(sourceFile.isDirectory()) {
            if(!onlyDirContents) {
                pathPrefix += sourceFile.getName()  + "/";
            }
            writeDirToJar(sourceFile, pathPrefix, stream, outputFile);
        }
        else {
            // check against a list of files we don't want to export and also
            // check that we don't try to export the jar file we are writing
            // (hangs the machine)
            if (!skipFile(sourceFile.getName(), !includeSource)
                    && !outputFile.equals(sourceFile.getCanonicalFile())) {
                writeJarEntry(sourceFile, stream, pathPrefix + sourceFile.getName());
            }
        }
    }
    
    /**
     * Write the contents of a jar into another jar stream. 
     */
    private void writeJarToJar(File inputJar, ZipOutputStream outputStream)
        throws IOException
    {
        
        JarInputStream inputStream = new JarInputStream(
                new BufferedInputStream(new FileInputStream(inputJar)));
        
        ZipEntry inputEntry = inputStream.getNextJarEntry();
        while(inputEntry != null) {
            //TODO: What if we have duplicate files????
            outputStream.putNextEntry(inputEntry);
            FileUtility.copyStream(inputStream, outputStream);
            inputStream.closeEntry();
            inputEntry = inputStream.getNextJarEntry();
        }        
        inputStream.close();
    }

    /**
     * Copy all files specified in the given list to the new jar directory.
     */
    private void copyLibsToDir(List<File> userLibs, File destDir)
    {
        for (Iterator<File> it = userLibs.iterator(); it.hasNext();) {
            File lib = (File) it.next();
            File destFile = new File(destDir, lib.getName());
            try {
                FileUtility.copyFile(lib, destFile);
            }
            catch (IOException e) {
                Debug.reportError("Error when copying file: " + lib + " to: " + destFile, e);               
            }
        }
    }

    /**
     * Test whether a given directory should be skipped (not included) in
     * export.
     */
    private boolean skipDir(File dir) throws IOException
    {

        Iterator<String> it = skipDirs.iterator();
        while(it.hasNext()) {
            String skipDir = it.next();
            if (dir.getCanonicalFile().getPath().endsWith(skipDir))
                return true;
        }
        return false;
    }

    /**
     * Checks whether a file should be skipped during a copy operation. 
     */
    private boolean skipFile(String fileName, boolean skipSource)
    {

        for(String skipFile : skipFiles) {
            if (fileName.endsWith(skipFile))
                return true;            
        }
        
        if (fileName.endsWith(SOURCE_SUFFIX))
            return !includeSource;


        return false;
    }

    /**
     * Write a jar file entry to the jar output stream. Note: entryName should
     * always be a path with / seperators (NOT the platform dependant
     * File.seperator)
     */
    private void writeJarEntry(File file, ZipOutputStream stream, String entryName)
        throws IOException
    {
        InputStream in = null;
        try {
            in = new BufferedInputStream(new FileInputStream(file));
            stream.putNextEntry(new ZipEntry(entryName));
            FileUtility.copyStream(in, stream);
        }
        catch (ZipException exc) {
            Debug.message("warning: " + exc);
        }
        finally {
            if (in != null)
                in.close();
        }
    }
    
    public void generateHTMLSkeleton(File outputFile, String title, int width, int height)
    {
        Hashtable<String,String> translations = new Hashtable<String,String>();

        translations.put("TITLE", title);
       // translations.put("COMMENT", htmlComment);
        translations.put("CLASSFILE", mainClass + ".class");
        // whilst it would be nice to be able to have no codebase, it is in the
        // HTML template file and hence even if we define no CODEBASE here, it
        // will appear in the resulting HTML anyway (as CODEBASE=$CODEBASE)
        translations.put("CODEBASE", "");
        translations.put("APPLETWIDTH", "" + width);
        translations.put("APPLETHEIGHT", "" + height);

        // add libraries from <project>/+libs/ to archives
        /*
        This does not work on Safari (and maybe other browser as well)
        String archives = jarName;
        try {
            for (int i = 0; i < libs.length; i++) {
                if (archives.length() == 0)
                    archives = libs[i].toURI().toURL().toString();
                else
                    archives += "," + libs[i].toURL();
            }
        }
        catch (MalformedURLException e) {}*/

        translations.put("ARCHIVE", jarName);


        String baseName = "greenfoot/templates/html.tmpl";
        File template = Config.getLanguageFile(baseName);
        
        try {
            BlueJFileReader.translateFile(template, outputFile, translations, Charset.forName("UTF-8"));
        }
        catch (IOException e) {
            Debug.reportError("Exception during file translation from " + template + " to " + outputFile);
            e.printStackTrace();
        }
    }
    
    static class PrefixedFile 
    {
        private File file;
        public File getFile()
        {
            return file;
        }

        public String getPrefix()
        {
            return prefix;
        }

        private String prefix;

        public PrefixedFile(String prefix, File file) 
        {
            this.prefix = prefix;
            this.file = file;
        }    
    }
    
}
