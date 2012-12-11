/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2010,2012  Michael Kolling and John Rosenberg 
 
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
package bluej.groupwork.actions;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

import javax.swing.AbstractAction;
import javax.swing.SwingUtilities;

import bluej.Config;
import bluej.collect.DataCollector;
import bluej.groupwork.*;
import bluej.groupwork.ui.ConflictsDialog;
import bluej.groupwork.ui.UpdateFilesFrame;
import bluej.pkgmgr.BlueJPackageFile;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.pkgmgr.Project;
import bluej.pkgmgr.target.ClassTarget;
import bluej.pkgmgr.target.PackageTarget;
import bluej.pkgmgr.target.ReadmeTarget;
import bluej.pkgmgr.target.Target;
import bluej.utility.DialogManager;
import bluej.utility.JavaNames;
import bluej.utility.SwingWorker;


/**
 * Action to update out-of-date files.
 * 
 * <p>Before this action is enabled, setFilesToUpdate(), setFilesToForceUpdate()
 * and setStatusHandle() must each be called.
 * 
 * @author Kasper Fisker
 */
public class UpdateAction extends AbstractAction
{
    private Project project;
    private boolean includeLayout = true;
    private UpdateFilesFrame updateFrame;
    private UpdateWorker worker;
    
    private Set<File> filesToUpdate;
    private Set<File> filesToForceUpdate;
    private StatusHandle statusHandle;
    
    /** A list of packages whose bluej.pkg file has been removed */
    private List<String> removedPackages;
    
    public UpdateAction(UpdateFilesFrame updateFrame)
    {
        super(Config.getString("team.update"));
        putValue(SHORT_DESCRIPTION, Config.getString("tooltip.update"));
        this.updateFrame = updateFrame;
    }

    /**
     * Set the files to be updated (changes merged if necessary).
     * @files a Set of File
     */
    public void setFilesToUpdate(Set<File> files)
    {
        filesToUpdate = files;
    }
    
    /**
     * Set the files to be updated with a clean copy of the repository
     * @files a Set of File
     */
    public void setFilesToForceUpdate(Set<File> files)
    {
        filesToForceUpdate = files;
    }
    
    /**
     * Set the status handle (which comes from a preceeding status operation).
     */
    public void setStatusHandle(StatusHandle statusHandle)
    {
        this.statusHandle = statusHandle;
    }
    
    /* (non-Javadoc)
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent event)
    {
        project = updateFrame.getProject();
        
        if (project != null) {
            updateFrame.startProgress();
            PkgMgrFrame.displayMessage(project, Config.getString("team.update.statusMessage"));
            
            worker = new UpdateWorker(project, statusHandle,
                    filesToUpdate, filesToForceUpdate);
            worker.start();
        }
    }

    /**
     * Cancel the update, if it is presently running.
     */
    public void cancel()
    {
        if (worker != null) {
            worker.abort();
        }
        setEnabled(true);
    }
    
    private class UpdateWorker extends SwingWorker implements UpdateListener
    {
        private TeamworkCommand command;
        private TeamworkCommandResult result;
        private boolean aborted;
        
        public UpdateWorker(Project project, StatusHandle statusHandle,
                Set<File> filesToUpdate, Set<File> filesToForceUpdate)
        {
            command = statusHandle.updateTo(this, filesToUpdate, filesToForceUpdate);
        }
        
        public Object construct()
        {
            removedPackages = new ArrayList<String>();
            result = command.getResult();
            return result;
        }

        /* (non-Javadoc)
         * @see bluej.groupwork.UpdateListener#fileAdded(java.io.File)
         */
        public void fileAdded(final File f)
        {
            SwingUtilities.invokeLater(new Runnable() {
                public void run()
                {
                    project.prepareCreateDir(f.getParentFile());
                    
                    String fileName = f.getName();
                    if (! fileName.endsWith(".java") &&
                            ! fileName.endsWith(".class") &&
                            ! BlueJPackageFile.isPackageFileName(fileName)) {
                        return;
                    }
                    
                    // First find out the package name...
                    String packageName = project.getPackageForFile(f);
                    if (packageName == null) {
                        return;
                    }
                    
                    if (BlueJPackageFile.isPackageFileName(fileName)) {
                        if (packageName.length() > 0) {
                            // If we now have a new package, we might need to add it
                            // as a target in an existing package
                            String parentPackageName = JavaNames.getPrefix(packageName);
                            Package parentPackage = project.getCachedPackage(parentPackageName);
                            if (parentPackage != null) {
                                Target t = parentPackage.addPackage(JavaNames.getBase(packageName));
                                parentPackage.positionNewTarget(t);
                            }
                        }
                    }
                    else {
                        int n = fileName.lastIndexOf(".");
                        String name = fileName.substring(0, n);
                        if (! JavaNames.isIdentifier(name)) {
                            return;
                        }
                        
                        Package pkg = project.getCachedPackage(packageName);
                        if (pkg == null) {
                            return;
                        }
                        Target t = pkg.getTarget(name);
                        if (t != null && ! (t instanceof ClassTarget)) {
                            return;
                        }
                        ClassTarget ct = (ClassTarget) t;
                        if (ct == null) {
                            ct = pkg.addClass(name);
                            pkg.positionNewTarget(ct);
                            DataCollector.addClass(pkg, f);
                        }
                        ct.reload();
                    }
                }
            });
        }
        
        /* (non-Javadoc)
         * @see bluej.groupwork.UpdateListener#fileRemoved(java.io.File)
         */
        public void fileRemoved(final File f)
        {
            SwingUtilities.invokeLater(new Runnable() {
               public void run()
                {
                   String fileName = f.getName();
                   if (! fileName.endsWith(".java") &&
                           ! fileName.endsWith(".class") &&
                           ! BlueJPackageFile.isPackageFileName(fileName)) {
                       return;
                   }
                   
                   // First find out the package name...
                   String packageName = project.getPackageForFile(f);
                   if (packageName == null) {
                       return;
                   }
                   
                   if (BlueJPackageFile.isPackageFileName(fileName)) {
                       // Delay removing the package until
                       // after the update has finished, and only do it if there
                       // are no files left in the package.
                       removedPackages.add(packageName);
                   }
                   else {
                       // Remove a class
                       int n = fileName.lastIndexOf(".");
                       String name = fileName.substring(0, n);
                       Package pkg = project.getCachedPackage(packageName);
                       if (pkg == null) {
                           return;
                       }
                       Target t = pkg.getTarget(name);
                       if (! (t instanceof ClassTarget)) {
                           return;
                       }
                       
                       ClassTarget ct = (ClassTarget) t;
                       if (ct.hasSourceCode() && ! fileName.endsWith(".java")) {
                           ct.setInvalidState();
                       }
                       else {
                           ct.remove();
                       }
                   }
                } 
            });
        }
        
        /* (non-Javadoc)
         * @see bluej.groupwork.UpdateListener#fileUpdated(java.io.File)
         */
        public void fileUpdated(final File f)
        {
            SwingUtilities.invokeLater(new Runnable() {
                public void run()
                {
                    String fileName = f.getName();
                    if (! fileName.endsWith(".java") &&
                            ! fileName.endsWith(".class") &&
                            ! BlueJPackageFile.isPackageFileName(fileName)) {
                        return;
                    }
                    
                    // First find out the package name...
                    String packageName = project.getPackageForFile(f);
                    if (packageName == null) {
                        return;
                    }
                    Package pkg = project.getCachedPackage(packageName);
                    if (pkg == null) {
                        return;
                    }
                    
                    if (BlueJPackageFile.isPackageFileName(fileName)) {
                        try {
                            if (includeLayout) {
                                pkg.reReadGraphLayout();
                            }
                        }
                        catch (IOException ioe) {
                            ioe.printStackTrace();
                        }
                    }
                    else {
                        int n = fileName.lastIndexOf(".");
                        String name = fileName.substring(0, n);
                        Target t = pkg.getTarget(name);
                        if (! (t instanceof ClassTarget)) {
                            return;
                        }
                        
                        ClassTarget ct = (ClassTarget) t;
                        ct.reload();
                    }
                }
            });
        }
        
        /* (non-Javadoc)
         * @see bluej.groupwork.UpdateListener#dirRemoved(java.io.File)
         */
        public void dirRemoved(final File f)
        {
            SwingUtilities.invokeLater(new Runnable() {
                public void run()
                {
                    String path = makeRelativePath(project.getProjectDir(), f);
                    String pkgName = path.replace(File.separatorChar, '.');
                    removedPackages.add(pkgName);
                }
            });
        }
        
        /* (non-Javadoc)
         * @see bluej.groupwork.UpdateListener#handleConflicts(bluej.groupwork.UpdateServerResponse)
         */
        public void handleConflicts(final UpdateResults updateServerResponse)
        {
            if (updateServerResponse == null) {
                return;
            }

            if (updateServerResponse.getConflicts().size() <= 0
                    && updateServerResponse.getBinaryConflicts().size() <= 0) {
                return;
            }

            try {
                EventQueue.invokeAndWait(new Runnable() {
                    public void run()
                    {
                        /** A list of files to replace with repository version */
                        Set<File> filesToOverride = new HashSet<File>();

                        // Binary conflicts
                        for (Iterator<File> i = updateServerResponse.getBinaryConflicts().iterator();
                                i.hasNext(); ) {
                            File f = i.next();

                            if (BlueJPackageFile.isPackageFileName(f.getName())) {
                                filesToOverride.add(f);
                            }
                            else {
                                // TODO make the displayed file path relative to project
                                int answer = DialogManager.askQuestion(PkgMgrFrame.getMostRecent(),
                                        "team-binary-conflict", new String[] {f.getName()});
                                if (answer == 0) {
                                    // keep local version
                                }
                                else {
                                    // use repository version
                                    filesToOverride.add(f);
                                }
                            }
                        }

                        updateServerResponse.overrideFiles(filesToOverride);

                        List<String> blueJconflicts = new LinkedList<String>();
                        List<String> nonBlueJConflicts = new LinkedList<String>();
                        List<Target> targets = new LinkedList<Target>();

                        for (Iterator<File> i = updateServerResponse.getConflicts().iterator();
                                i.hasNext();) {
                            File file = i.next();

                            // Calculate the file base name
                            String baseName = file.getName();

                            // bluej package file may come up as a conflict, but it won't cause a problem,
                            // so it can be ignored.
                            if (! BlueJPackageFile.isPackageFileName(baseName)) {
                                Target target = null;

                                if (baseName.endsWith(".java") || baseName.endsWith(".class")) {
                                    String pkg = project.getPackageForFile(file);
                                    if (pkg != null) {
                                        String targetId = filenameToTargetIdentifier(baseName);
                                        targetId = JavaNames.combineNames(pkg, targetId);
                                        target = project.getTarget(targetId);
                                    }
                                }
                                else if (baseName.equals("README.TXT")) {
                                    String pkg = project.getPackageForFile(file);
                                    if (pkg != null) {
                                        String targetId = ReadmeTarget.README_ID;
                                        targetId = JavaNames.combineNames(pkg, targetId);
                                        target = project.getTarget(targetId);
                                    }
                                }

                                String fileName = makeRelativePath(project.getProjectDir(), file);
                                
                                if (target == null) {
                                    nonBlueJConflicts.add(fileName);
                                } else {
                                    blueJconflicts.add(fileName);
                                    targets.add(target);
                                }
                            }
                        }

                        if (! blueJconflicts.isEmpty() || ! nonBlueJConflicts.isEmpty()) {
                            project.clearAllSelections();
                            project.selectTargetsInGraphs(targets);

                            ConflictsDialog conflictsDialog = new ConflictsDialog(project,
                                    blueJconflicts, nonBlueJConflicts);
                            conflictsDialog.setVisible(true);
                        }
                    }
                });
            }
            catch (InvocationTargetException ite) {
                throw new Error(ite);
            }
            catch (InterruptedException ie) {
                // Probably indicates an application exit; just ignore it.
            }
        }
        
        public void abort()
        {
            command.cancel();
            aborted = true;
        }
        
        public void finished()
        {
            handleRemovedPkgs();
            updateFrame.stopProgress();

            if (! result.isError() && ! aborted) {
                Set<File> files = new HashSet<File>();
                files.addAll(filesToUpdate);
                files.addAll(filesToForceUpdate);
                DataCollector.teamUpdateProject(project, statusHandle.getRepository(), files);
                PkgMgrFrame.displayMessage(project, Config.getString("team.update.statusDone"));
            }
            else {
                PkgMgrFrame.displayMessage(project, "");
                TeamUtils.handleServerResponse(result, updateFrame);
            }
            
            if (! aborted) {
                updateFrame.setVisible(false);
                updateFrame.dispose();
                setEnabled(true);
            }
        }
        
        /**
         * If packages were removed by the update, remove them from the
         * parent package graph.
         */
        private void handleRemovedPkgs()
        {
            for (Iterator<String> i = removedPackages.iterator(); i.hasNext(); ) {
                String packageName = i.next();
                String parentPackage = JavaNames.getPrefix(packageName);
                String baseName = JavaNames.getBase(packageName);
                
                File packageDir = JavaNames.convertQualifiedNameToFile(packageName, project.getProjectDir());
                if (! packageDir.exists()) {
                    // Close the package window, if open
                    Package pkg = project.getCachedPackage(packageName);
                    if (pkg != null) {
                        pkg.closeAllEditors();
                        PkgMgrFrame frame = PkgMgrFrame.findFrame(pkg);
                        if (frame != null) {
                            frame.doClose(true, false);
                        }
                        project.removePackage(packageName);
                    }
                    
                    // Get the parent package so we can remove the child.
                    pkg = project.getCachedPackage(parentPackage);
                    if (pkg != null) {
                        Target target = pkg.getTarget(baseName);
                        if (target instanceof PackageTarget) {
                            pkg.removeTarget(target);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Strip the dot-suffix from a file name.
     * @param filename
     * @return
     */
    private String filenameToTargetIdentifier(String filename)
    {
        int lastDot = filename.lastIndexOf('.');
        return filename.substring(0, lastDot);
    }
    
    /**
     * Make a relative path between a file and containing directory. 
     */
    private static String makeRelativePath(File parent, File file)
    {
        String parentStr = parent.getAbsolutePath();
        String filePath = file.getAbsolutePath();
        
        if (filePath.startsWith(parentStr)) {
            // remove parent, plus path separator character
            filePath = filePath.substring(parentStr.length() + 1);
        }
        
        return filePath;
    }
}
