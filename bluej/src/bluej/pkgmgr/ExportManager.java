package bluej.pkgmgr;

import java.util.jar.*;
import java.util.zip.*;
import java.io.*;

import bluej.Config;
import bluej.classmgr.ProjectClassLoader;
import bluej.utility.Debug;
import bluej.utility.DialogManager;
import bluej.utility.FileUtility;

/**
 * Component to manage exporting projects to standard Java format.
 * The format can be either a directory tree or a jar file.
 *
 * @author  Michael Kolling
 * @version $Id: ExportManager.java 2895 2004-08-18 08:42:23Z mik $
 */
final class ExportManager
{
    private static final String specifyJar = Config.getString("pkgmgr.export.specifyJar");
    private static final String createJarText = Config.getString("pkgmgr.export.createJarText");
    
    private static final String sourceSuffix = ".java";
    private static final String contextSuffix = ".ctxt";
    private static final String packageFilePrefix = "bluej.pk";

    private PkgMgrFrame frame;

    public ExportManager(PkgMgrFrame frame)
    {
        this.frame = frame;
    }

    /**
     * Envoke the "export" user function. This starts by displaying the
     * export dialog, then it calls the appropriate export implementation
     * function (jar or directory).
     */
    public void export()
    {
        ExportDialog dialog = new ExportDialog(frame);
        boolean okay = dialog.display();

        if(!okay)
            return;

        String newName = FileUtility.getFileName(frame, specifyJar, createJarText, 
                                                 false, null, false);
        if(newName == null)
            return;

        String sourceDir = frame.getProject().getProjectDir().getPath();

        exportJar(sourceDir, newName, dialog.getMainClass(),
                dialog.includeSource());
    }

    /**
     * Export this project to a jar file.
     */
    private void exportJar(String sourceDir, String fileName,
                           String mainClass, boolean includeSource)
    {
        if(!fileName.endsWith(".jar"))
           fileName = fileName + ".jar";

        File jarFile = new File(fileName);
        if(jarFile.exists()) {
            if (DialogManager.askQuestion(frame, "error-jar-exists") != 0)
                return;
        }

        OutputStream oStream = null;
        JarOutputStream jStream = null;

        // add jar files from +libs to classpath
        File[] libs = frame.getProject().getLocalClassLoader().getProjectLibs();
        String classpath = "";
        for(int i=0; i < libs.length; i++) {
            classpath += " " + libs[i].getName();
        }
        
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
                            jarFile.getCanonicalFile());

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
                               JarOutputStream jStream, boolean includeSource, File outputFile)
        throws IOException
    {
        File[] dir = sourceDir.listFiles();
        for(int i = 0; i < dir.length; i++) {
            if(dir[i].isDirectory()) {
                if(!skipDir(dir[i])) {
                    if(isLibDirectory(dir[i])) // move jars from lib directory to top level dir
                        writeDirToJar(dir[i], pathPrefix,
                                jStream, includeSource, outputFile);
                    else
                        writeDirToJar(dir[i], pathPrefix + dir[i].getName() + "/",
                                jStream, includeSource, outputFile);
                }
            }
            else {
                // check against a list of file we don't want to export and also
                // check that we don't try to export the jar file we are writing
                // (hangs the machine)
                if(!skipFile(dir[i].getName(), !includeSource) &&
                    !outputFile.equals(dir[i].getCanonicalFile())) {
                        writeJarEntry(dir[i], jStream, pathPrefix + dir[i].getName());
                }
            }
        }
    }

    /** array of directory names not to be included in jar file **/
    private static final String[] skipDirs = { "CVS" };

    /**
     * Test whether a given directory should be skipped (not included) in
     * export.
     */
    private boolean skipDir(File dir)
    {
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
    private boolean skipFile(String fileName, boolean skipSource)
    {
        if(fileName.startsWith(packageFilePrefix) || fileName.endsWith(contextSuffix))
            return true;

        if(skipSource && fileName.endsWith(sourceSuffix))
            return true;

        return false;
    }

    /**
     * Test whether a given directory is the project +libs directory
     */
    private boolean isLibDirectory(File dir)
    {
        return ProjectClassLoader.projectLibDirName.equals(dir.getName());
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
