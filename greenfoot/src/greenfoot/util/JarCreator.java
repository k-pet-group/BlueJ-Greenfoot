package greenfoot.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;

import bluej.Config;
import bluej.pkgmgr.Project;
import bluej.utility.Debug;
import bluej.utility.FileUtility;

/**
 * Utility class to create jar files.
 * 
 * @author Poul Henriksen <polle@polle.org>
 * 
 */
public class JarCreator
{

    //private static final String specifyJar = Config.getString("pkgmgr.export.specifyJar");
   // private static final String createJarText = Config.getString("pkgmgr.export.createJarText");

    private static final String sourceSuffix = ".java";
    private static final String contextSuffix = ".ctxt";
    private static final String packageFilePrefix = "bluej.pk";
    private static final String packageFileBackup = "bluej.pkh";

    /** Should source files be included in the jar? */
    private boolean includeSource;

    /** Whether the main class is an applet */
    private boolean createApplet;

    /** The main class attribute for the JAR */
    private String mainClass;
    private File exportDir;
    private String jarName;
    private List<File> extraJars = new LinkedList<File>();
    private File jarFile;
    private boolean includeMetaFiles;
    private List<File> dirs = new LinkedList<File>();

    /** array of directory names not to be included in jar file * */
    private List<String> skipDirs = new LinkedList<String>();

    /** array of file names not to be included in jar file * */
    private List<String> skipFiles = new LinkedList<String>();

    /**
     * Prepares a new jar creator. Once everything is set up, call create()
     * 
     * @param exportDir The directory in which to store the jar-file and any
     *            other resources required. Must exist and be writable
     * @param jarName The name of the jar file.
     */
    public JarCreator(File exportDir, String jarName)
    {
        skipDirs.add("CVS");
        if (!exportDir.canWrite()) {
            throw new IllegalArgumentException("exportDir not writable: " + exportDir);
        }
        this.exportDir = exportDir;
        this.jarName = jarName;
    }

    /**
     * Creates the jar file with the current settings.
     * 
     */
    public void create()
    {

        // create the jar file
        if (!jarName.endsWith(".jar"))
            jarName = jarName + ".jar";
        jarFile = new File(exportDir, jarName);

        OutputStream oStream = null;
        JarOutputStream jStream = null;

        try {
            Manifest manifest = createManifest();

            // create jar file
            oStream = new FileOutputStream(jarFile);
            jStream = new JarOutputStream(oStream, manifest);

            // Write contents of project-dir
            for(File dir : dirs) {
                writeDirToJar(dir, "", jStream, jarFile.getCanonicalFile());
            }
            
            copyLibsToJar(extraJars, exportDir);
            
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
        }

    }

    private Manifest createManifest()
    {
        // Construct classpath with used library jars
        String classpath = "";

        // add extra jar to classpath
        for (Iterator<File> it = extraJars.iterator(); it.hasNext();) {
            classpath += " " + it.next().getName();
        }
        
        // create manifest
        Manifest manifest = new Manifest();
        Attributes attr = manifest.getMainAttributes();
        attr.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        attr.put(Attributes.Name.MAIN_CLASS, mainClass);
        attr.put(Attributes.Name.CLASS_PATH, classpath);
        return manifest;
    }

    /**
     * Whether to include sources or not.
     */
    public void includeSource(boolean b)
    {
        includeSource = b;
    }
    
    /**
     * Whether to include project files like project.greenfoot etc.
     */
    public void includeMetaFiles(boolean b)
    {
        includeMetaFiles = b;
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
     * 
     * @param b
     */
    public void setApplet(boolean b)
    {
        this.createApplet = b;
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

    public void addDir(File dir)
    {
        dirs.add(dir);
    }
    /**
     * All dirs that end with the specified string will be skipped.
     * TODO: platform dependent file separators?
     * @param dir
     */
    public void addSkipDir(String dir) {
        skipDirs.add(dir);
    }


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
    private void writeDirToJar(File sourceDir, String pathPrefix, JarOutputStream jStream, File outputFile)
        throws IOException
    {
        File[] dir = sourceDir.listFiles();
        for (int i = 0; i < dir.length; i++) {
            if (dir[i].isDirectory()) {
                if (!skipDir(dir[i])) {
                    writeDirToJar(dir[i], pathPrefix + dir[i].getName() + "/", jStream, outputFile);
                }
            }
            else {
                // check against a list of file we don't want to export and also
                // check that we don't try to export the jar file we are writing
                // (hangs the machine)
                if (!skipFile(dir[i].getName(), !includeSource)
                        && !outputFile.equals(dir[i].getCanonicalFile())) {
                    writeJarEntry(dir[i], jStream, pathPrefix + dir[i].getName());
                }
            }
        }
    }

    /**
     * Copy all files specified in the given list to the new jar directory.
     */
    private void copyLibsToJar(List userLibs, File destDir)
    {
        for (Iterator it = userLibs.iterator(); it.hasNext();) {
            File lib = (File) it.next();
            FileUtility.copyFile(lib, new File(destDir, lib.getName()));
        }
    }

    /**
     * Test whether a given directory should be skipped (not included) in
     * export.
     */
    private boolean skipDir(File dir) throws IOException
    {
        if (dir.getName().equals(Project.projectLibDirName))
            return !includeMetaFiles;

        Iterator<String> it = skipDirs.iterator();
        while(it.hasNext()) {
            String skipDir = it.next();
            if (dir.getCanonicalFile().getPath().endsWith(skipDir))
                return true;
        }
        return false;
    }

    /**
     * Checks whether a file should be skipped during a copy operation. BlueJ
     * specific files (bluej.pkg and *.ctxt) and - optionally - Java source
     * files are skipped.
     */
    private boolean skipFile(String fileName, boolean skipSource)
    {
        if (fileName.equals(packageFileBackup))
            return true;

        for(String skipFile : skipFiles) {
            if (fileName.endsWith(skipFile))
                return true;            
        }
        
        if (fileName.endsWith(sourceSuffix))
            return !includeSource;

        if (fileName.startsWith(packageFilePrefix) || fileName.endsWith(contextSuffix))
            return !includeMetaFiles;

        return false;
    }

    /**
     * Write a jar file entry to the jar output stream. Note: entryName should
     * always be a path with / seperators (NOT the platform dependant
     * File.seperator)
     */
    private void writeJarEntry(File file, JarOutputStream jStream, String entryName)
        throws IOException
    {
        InputStream in = null;
        try {
            in = new FileInputStream(file);
            jStream.putNextEntry(new ZipEntry(entryName));
            FileUtility.copyStream(in, jStream);
        }
        catch (ZipException exc) {
            Debug.message("warning: " + exc);
        }
        finally {
            if (in != null)
                in.close();
        }
    }

}
