/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2010,2012,2014,2016,2017,2018,2019  Michael Kolling and John Rosenberg
 
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import bluej.Config;
import bluej.collect.DataCollector;
import bluej.groupwork.StatusHandle;
import bluej.groupwork.TeamUtils;
import bluej.groupwork.TeamworkCommand;
import bluej.groupwork.TeamworkCommandResult;
import bluej.groupwork.UpdateListener;
import bluej.groupwork.UpdateResults;
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
import bluej.utility.Debug;
import bluej.utility.DialogManager;
import bluej.utility.FXWorker;
import bluej.utility.JavaNames;
import javafx.stage.Window;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Action to update out-of-date files.
 *
 * <p>Before this action is enabled, setFilesToUpdate(), setFilesToForceUpdate()
 * and setStatusHandle() must each be called.
 *
 * @author Kasper Fisker
 */
@OnThread(Tag.FXPlatform)
public class UpdateAction extends TeamAction
{
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
        super(Config.getString("team.update"), false);
        setShortDescription(Config.getString("tooltip.update"));
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

    @Override
    protected void actionPerformed(Project project)
    {
        updateFrame.startProgress();
        PkgMgrFrame.displayMessage(project, Config.getString("team.update.statusMessage"));

        worker = new UpdateWorker(project, statusHandle, filesToUpdate, filesToForceUpdate);
        worker.start();
        updateFrame.disableLayoutCheck();
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

    private class UpdateWorker extends FXWorker implements UpdateListener
    {
        private Project project;
        private TeamworkCommand command;
        private TeamworkCommandResult result;
        private boolean aborted;

        public UpdateWorker(Project project, StatusHandle statusHandle,
                            Set<File> filesToUpdate, Set<File> filesToForceUpdate)
        {
            this.project = project;
            command = statusHandle.updateTo(this, filesToUpdate, filesToForceUpdate);
        }

        @OnThread(Tag.Worker)
        public Object construct()
        {
            removedPackages = new ArrayList<>();
            result = command.getResult();
            return result;
        }

        /* (non-Javadoc)
         * @see bluej.groupwork.UpdateListener#fileAdded(java.io.File)
         */
        @Override
        @OnThread(Tag.FXPlatform)
        public void fileModified(final File f)
        {
            String fileName = f.getName();
            boolean isPkgFile = BlueJPackageFile.isPackageFileName(fileName);
            if (! fileName.endsWith(".java") && ! fileName.endsWith(".class") && ! isPkgFile)
            {
                return;
            }

            // First find out the package name...
            String packageName = project.getPackageForFile(f);
            if (packageName == null) {
                return;
            }

            if (isPkgFile)
            {
                if (packageName.length() > 0)
                {
                    // If we now have a new package, we might need to add it
                    // as a target in an existing package
                    String parentPackageName = JavaNames.getPrefix(packageName);
                    Package parentPackage = project.getCachedPackage(parentPackageName);
                    if (parentPackage != null)
                    {
                        Target t = parentPackage.addPackage(JavaNames.getBase(packageName));
                        parentPackage.positionNewTarget(t);
                    }
                }
                
                Package filePackage = project.getCachedPackage(packageName);
                if (filePackage != null && includeLayout)
                {
                    // There's a pre-existing package, so we assume this is a modification:
                    try
                    {
                        filePackage.reReadGraphLayout();
                    }
                    catch (IOException ioe)
                    {
                        Debug.reportError("Error re-reading package file (team update)", ioe);
                    }
                }
            }
            else {
                int n = fileName.lastIndexOf(".");
                String name = fileName.substring(0, n);
                if (! JavaNames.isIdentifier(name))
                {
                    return;
                }

                Package pkg = project.getCachedPackage(packageName);
                if (pkg == null)
                {
                    return;
                }
                
                Target t = pkg.getTarget(name);
                if (t == null)
                {
                    // Addition of a new class:
                    ClassTarget ct = pkg.addClass(name);
                    pkg.positionNewTarget(ct);
                    DataCollector.addClass(pkg, ct);
                    return;
                }
                
                if (t instanceof ClassTarget)
                {
                    ((ClassTarget) t).reload();
                }
            }
        }

        /* (non-Javadoc)
         * @see bluej.groupwork.UpdateListener#fileRemoved(java.io.File)
         */
        @OnThread(Tag.FXPlatform)
        public void fileRemoved(final File f)
        {
            String fileName = f.getName();
            if (!fileName.endsWith(".java") &&
                    !fileName.endsWith(".class") &&
                    !BlueJPackageFile.isPackageFileName(fileName))
            {
                return;
            }

            // First find out the package name...
            String packageName = project.getPackageForFile(f);
            if (packageName == null)
            {
                return;
            }

            if (BlueJPackageFile.isPackageFileName(fileName))
            {
                // Delay removing the package until
                // after the update has finished, and only do it if there
                // are no files left in the package.
                removedPackages.add(packageName);
            }
            else
            {
                // Remove a class
                int n = fileName.lastIndexOf(".");
                String name = fileName.substring(0, n);
                Package pkg = project.getCachedPackage(packageName);
                if (pkg == null)
                {
                    return;
                }
                Target t = pkg.getTarget(name);
                if (!(t instanceof ClassTarget))
                {
                    return;
                }

                ClassTarget ct = (ClassTarget) t;
                if (ct.hasSourceCode() && !fileName.endsWith(".java"))
                {
                    ct.markModified();
                }
                else
                {
                    ct.remove();
                }
            }
        }

         /* (non-Javadoc)
         * @see bluej.groupwork.UpdateListener#handleConflicts(bluej.groupwork.UpdateServerResponse)
         */
        @OnThread(Tag.FXPlatform)
        public void handleConflicts(final UpdateResults updateServerResponse)
        {
            if (updateServerResponse.getConflicts().isEmpty()
                    && updateServerResponse.getBinaryConflicts().isEmpty())
            {
                return;
            }

            /** A list of files to replace with repository version */
            Set<File> filesToOverride = new HashSet<>();

            // Binary conflicts
            for (Iterator<File> i = updateServerResponse.getBinaryConflicts().iterator();
                 i.hasNext(); ) {
                File f = i.next();

                if (BlueJPackageFile.isPackageFileName(f.getName())) {
                    filesToOverride.add(f);
                }
                else {
                    // TODO make the displayed file path relative to project
                    int answer = DialogManager.askQuestionFX(null,
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

            for (Iterator<File> i = updateServerResponse.getConflicts().iterator(); i.hasNext(); )
            {
                File file = i.next();

                // Calculate the file base name
                String baseName = file.getName();

                // bluej package file may come up as a conflict, but it won't cause a problem,
                // so it can be ignored.
                if (!BlueJPackageFile.isPackageFileName(baseName))
                {
                    Target target = null;

                    if (baseName.endsWith(".java") || baseName.endsWith(".class"))
                    {
                        String pkg = project.getPackageForFile(file);
                        if (pkg != null)
                        {
                            String targetId = filenameToTargetIdentifier(baseName);
                            targetId = JavaNames.combineNames(pkg, targetId);
                            target = project.getTarget(targetId);
                        }
                    }
                    else if (baseName.equals("README.TXT"))
                    {
                        String pkg = project.getPackageForFile(file);
                        if (pkg != null)
                        {
                            String targetId = ReadmeTarget.README_ID;
                            targetId = JavaNames.combineNames(pkg, targetId);
                            target = project.getTarget(targetId);
                        }
                    }

                    String fileName = makeRelativePath(project.getProjectDir(), file);

                    if (target == null)
                    {
                        nonBlueJConflicts.add(fileName);
                    }
                    else
                    {
                        blueJconflicts.add(fileName);
                        targets.add(target);
                        // Force the package frame open, if it isn't already:
                        if (target.getPackage().getUI() == null)
                        {
                            PkgMgrFrame.createFrame(target.getPackage(), PkgMgrFrame.getMostRecent());
                        }
                    }
                }
            }

            if (!blueJconflicts.isEmpty() || !nonBlueJConflicts.isEmpty())
            {
                project.clearAllSelections();
                project.selectTargetsInGraphs(targets);
                
                // Show the conflicts dialog as a child of the first appropriate PkgMgr frame. We
                // can't make it a child of the update frame because that will close.
                Window stage = targets.isEmpty() ? null : targets.get(0).getPackage().getUI().getStage();

                ConflictsDialog conflictsDialog = new ConflictsDialog(project, stage,
                        blueJconflicts, nonBlueJConflicts);
                conflictsDialog.show();
            }
            else if (updateServerResponse.mergeCommitNeeded())
            {
                DialogManager.showMessageFX(null, "team-merge-commit-needed");
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
                TeamUtils.handleServerResponseFX(result, updateFrame.asWindow());
            }

            if (! aborted) {
                updateFrame.setVisible(false);
                updateFrame.close();
                setEnabled(true);
            }
        }

        /**
         * If packages were removed by the update, remove them from the
         * parent package graph.
         */
        @OnThread(Tag.FXPlatform)
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
     */
    private String filenameToTargetIdentifier(String filename)
    {
        int lastDot = filename.lastIndexOf('.');
        return filename.substring(0, lastDot);
    }

    /**
     * Make a relative path between a file and containing directory. 
     */
    @OnThread(Tag.Any)
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
