package bluej.pkgmgr;

import java.util.*;
import java.util.jar.*;
import java.util.zip.*;
import java.io.*;

import bluej.Config;
import bluej.utility.Debug;
import bluej.utility.DialogManager;
import bluej.utility.FileUtility;

/**
 * Component to manage exporting projects to standard Java format.
 * The format can be either a directory tree or a jar file.
 *
 * @author  Michael Kolling
 * @version $Id: ExportManager.java 555 2000-06-19 00:35:11Z mik $
 */
final class ExportManager
{
    private PkgMgrFrame frame;

    public ExportManager(PkgMgrFrame frame)
    {
        this.frame = frame;
    }

    /**
     * 
     * 
     */
    public void export()
    {
        ExportDialog dialog = new ExportDialog(frame);
        boolean okay = dialog.display();

        if(!okay)
            return;

        String newName;
        if(dialog.saveAsJar())
            newName = FileUtility.getFileName(frame, 
                                              "Specify name for jar file", 
                                              "Create");
        else
            newName = FileUtility.getFileName(frame, 
                                              "Specify name for directory", 
                                              "Create");
        if(newName == null)
            return;

        String sourceDir = frame.getProject().getProjectDir().getPath();

        if(dialog.saveAsJar())
            exportJar(sourceDir, newName, dialog.getMainClass(), 
                      dialog.includeSource());
        else
            exportDir(sourceDir, newName, dialog.getMainClass(), 
                      dialog.includeSource());
    }

    private void exportJar(String sourceDir, String fileName, 
                           String mainClass, boolean includeSource)
    {
        if(!fileName.endsWith(".jar"))
           fileName = fileName + ".jar";

        File jarFile = new File(fileName);
        if(jarFile.exists()) {
            DialogManager.showError(frame, "directory-exists");
            return;
        }

        OutputStream oStream = null;
        JarOutputStream jStream = null;

        try {
            // create manifest
            Manifest manifest = new Manifest();
            Attributes attr = manifest.getMainAttributes();
            attr.put(Attributes.Name.MANIFEST_VERSION, "1.0");
            attr.put(Attributes.Name.MAIN_CLASS, mainClass);

            // create jar file
            oStream = new FileOutputStream(jarFile);
            jStream = new JarOutputStream(oStream, manifest);

            File srcFile = new File(sourceDir);
            String[] dir = srcFile.list();
            for(int i=0; i<dir.length; i++) {
                String srcName = sourceDir + File.separator + dir[i];
                File file = new File(srcName);
                if(file.isDirectory()) {
                    ; // ignore for now
                }
                else {
                    if(!FileUtility.skipFile(dir[i], true, !includeSource)) {
                        writeJarEntry(file, jStream, dir[i]);
                    }
                }
            }
        }
        catch(IOException exc) {
            Debug.reportError("problem: " + exc);
        } finally {
            try {
                if(jStream != null)
                    jStream.close();
            } catch (IOException e) {}
        }
    }

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
        //catch(IOException exc) {
        finally {
            if(in != null)
                in.close();
        }
    }

    private void exportDir(String sourceDir, String destDir, String mainClass, 
                           boolean includeSource)
    {
        int result = FileUtility.copyDirectory(sourceDir, destDir, 
                                               true, !includeSource);
        switch(result) {
        case FileUtility.NO_ERROR:
            break;
        case FileUtility.DEST_EXISTS:
            DialogManager.showError(frame, "directory-exists");
            return;
        case FileUtility.SRC_NOT_DIRECTORY:
        case FileUtility.COPY_ERROR:
            Debug.message("copy error");
            return;
        }
    }

}
