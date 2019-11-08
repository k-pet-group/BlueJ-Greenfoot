/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2014,2015,2016,2018,2019  Michael Kolling and John Rosenberg
 
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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import bluej.pkgmgr.target.ClassTarget;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Window;

import bluej.Config;
import bluej.extensions2.SourceType;
import bluej.utility.Debug;
import bluej.utility.DialogManager;
import bluej.utility.FileUtility;
import bluej.utility.Utility;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Component to manage storing projects to jar file format.
 *
 * @author  Michael Kolling
 */
final class ExportManager
{
    private static final String specifyJar = Config.getString("pkgmgr.export.specifyJar");
    
    private static final String sourceSuffix = "." + SourceType.Java.toString().toLowerCase();
    private static final String contextSuffix = ".ctxt";
    private static final String packageFilePrefix = "bluej.pk";
    private static final String packageFileSuffix = ".bluej";
    private static final String packageFileBackup = "bluej.pkh";

    private final PkgMgrFrame frame;
    @OnThread(Tag.FXPlatform)
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
        Project proj = frame.getProject();
        ExportDialog.ProjectInfo projectInfo = new ExportDialog.ProjectInfo(proj);

        boolean hasStride = proj.getPackageNames().stream().map(proj::getPackage)
            .flatMap(p -> p.getClassTargets().stream())
            .anyMatch(ct -> ct.getSourceType() == SourceType.Stride);

        Window parent = frame.getWindow();
        if (dialog == null)
            dialog = new ExportDialog(parent, projectInfo);
        else
            dialog.updateDialog(projectInfo);
        Optional<ExportDialog.ExportInfo> result = dialog.showAndWait();

        if (!result.isPresent())
            return;
        ExportDialog.ExportInfo info = result.get();
        if (!info.mainClassName.equals(""))
        {
            for (Package p : proj.getProjectPackages())
            {
                for (ClassTarget c : p.getClassTargets())
                {
                    if (!c.isCompiled())
                    {
                        DialogManager.showErrorFX(parent,"jar-executable-uncompiled-project");
                        return;
                    }
                }
            }
        }

        File fileName = FileUtility.getSaveFileFX(parent, specifyJar, Arrays.asList(new ExtensionFilter("JAR file", "*.jar")), false);
        if (fileName == null)
            return;

        String sourceDir = proj.getProjectDir().getPath();

        createJar(proj, fileName.getAbsolutePath().toString(), sourceDir, info.mainClassName, info.selectedFiles,
            info.includeSource, info.includePkgFiles, hasStride);
    }

    /**
     * Export this project to a jar file.
     */
    @OnThread(Tag.FXPlatform)
    private void createJar(Project proj, String fileName, String sourceDir, String mainClass,
                           List<File> userLibs, boolean includeSource, boolean includePkgFiles, boolean includeStrideLang)
    {
        
        // Create a single jar file
        if(!fileName.endsWith(".jar"))
            fileName = fileName + ".jar";

        File jarFile = new File(fileName);

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
            JarOutput jarOutput = new JarOutput(jStream);

            writeDirToJar(new File(sourceDir), "", jarOutput, includeSource,
                            includePkgFiles,
                            jarFile.getCanonicalFile());
            if (includeStrideLang)
            {
                includeJarContent(new File(Config.getBlueJLibDir(), "lang-stride.jar"), jarOutput);
            }
            for (URL url : proj.getPlusLibsContent())
            {
                try
                {
                    includeJarContent(new File(new URI(url.toString())), jarOutput);
                }
                catch (URISyntaxException urie)
                {

                }
            }
            for (File f : userLibs)
                includeJarContent(f, jarOutput);
            
            frame.setStatus(Config.getString("pkgmgr.exported.jar"));
        }
        catch(IOException exc) {
            DialogManager.showErrorFX(frame.getWindow(), "error-writing-jar");
            Debug.reportError("problem writing jar file: " + exc);
        } finally {
            try {
                if(jStream != null)
                    jStream.close();
            } catch (IOException e) {}
        }
    }

    @OnThread(Tag.Any)
    private void includeJarContent(File srcJarFile, JarOutput jarOutput) throws IOException
    {
        try (ZipFile jar = new ZipFile(srcJarFile)) {
            for (ZipEntry entry : Utility.iterableStream(jar.stream())) {
                jarOutput.writeJarEntry(jar.getInputStream(entry), entry.getName());
            }
        }
    }

    /**
     * Write the contents of a directory to a jar stream. Recursively called
     * for subdirectories.
     * outputFile should be the canonical file representation of the Jar file
     * we are creating (to prevent including itself in the Jar file)
     */
    @OnThread(Tag.Any)
    private void writeDirToJar(File sourceDir, String pathPrefix,
                               JarOutput jarOutput, boolean includeSource, boolean includePkg, File outputFile)
        throws IOException
    {
        File[] dir = sourceDir.listFiles();
        for(int i = 0; i < dir.length; i++) {
            if(dir[i].isDirectory()) {
                if(!skipDir(dir[i], includePkg) ) {
                    writeDirToJar(dir[i], pathPrefix + dir[i].getName() + "/",
                                  jarOutput, includeSource, includePkg, outputFile);
                }
            }
            else {
                // check against a list of file we don't want to export and also
                // check that we don't try to export the jar file we are writing
                // (hangs the machine)
                if(!skipFile(dir[i].getName(), !includeSource, !includePkg) &&
                    !outputFile.equals(dir[i].getCanonicalFile())) {
                        jarOutput.writeJarEntry(dir[i], pathPrefix + dir[i].getName());
                }
            }
        }
    }

    /**
     * Copy all files specified in the given list to the new jar directory.
     */
    @OnThread(Tag.Any)
    private void copyLibsToJar(List<File> userLibs, File destDir)
        throws IOException
    {
        for(Iterator<File> it = userLibs.iterator(); it.hasNext(); ) {
            File lib = it.next();
            FileUtility.copyFile(lib, new File(destDir, lib.getName()));
        }
    }

    /** array of directory names not to be included in jar file **/
    @OnThread(Tag.Any)
    private static final String[] skipDirs = { "CVS", ".svn", ".git" };

    /**
     * Test whether a given directory should be skipped (not included) in
     * export.
     */
    @OnThread(Tag.Any)
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
    @OnThread(Tag.Any)
    private boolean skipFile(String fileName, boolean skipSource, boolean skipPkg)
    {
        if(fileName.equals(packageFileBackup))
            return true;
        
        if(fileName.endsWith(sourceSuffix) || fileName.endsWith(sourceSuffix + "~"))
            return skipSource;
        if(fileName.startsWith(packageFilePrefix) || fileName.endsWith(packageFileSuffix) ||
                fileName.endsWith(contextSuffix))
            return skipPkg;

        return false;
    }

    /**
     * A class for writing files to a JAR which does not allow duplicate files.
     * In the case of duplicates, the file inserted first is kept, with the later
     * duplicate(s) discarded.
     */
    @OnThread(Tag.Any)
    private static class JarOutput
    {
        private final JarOutputStream jStream;
        private final HashSet<String> existingNames = new HashSet<>();

        public JarOutput(JarOutputStream jStream)
        {
            this.jStream = jStream;
            // The manifest is already added, so we must put it in our records:
            existingNames.add("META-INF/MANIFEST.MF");
        }

        /**
         * Write a jar file entry to the jar output stream.
         * Note: entryName should always be a path with / seperators
         * (NOT the platform dependant File.seperator)
         */
        @OnThread(Tag.Any)
        public void writeJarEntry(File srcFile, String entryName)
            throws IOException
        {
            writeJarEntry(new FileInputStream(srcFile), entryName);
        }

        @OnThread(Tag.Any)
        public void writeJarEntry(InputStream src, String entryName)
            throws IOException
        {
            try
            {
                if (!existingNames.contains(entryName))
                {
                    existingNames.add(entryName);
                    jStream.putNextEntry(new ZipEntry(entryName));
                    FileUtility.copyStream(src, jStream);
                }
            } catch (ZipException exc)
            {
                Debug.message("warning: " + exc);
            } finally
            {
                if (src != null)
                    src.close();
            }
        }
    }
}
