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
import bluej.groupwork.*;
import bluej.groupwork.ui.ConflictsDialog;
import bluej.groupwork.ui.UpdateFilesFrame;
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
 * @author fisker
 * @version $Id: UpdateAction.java 5058 2007-05-25 04:40:45Z davmac $
 */
public class UpdateAction extends AbstractAction
{
    private Project project;
    private boolean includeLayout;
    private UpdateFilesFrame updateFrame;
    private UpdateWorker worker;
    
    /** A list of packages whose bluej.pkg file has been removed */
    private List removedPackages;
    
    public UpdateAction(UpdateFilesFrame updateFrame)
    {
        super(Config.getString("team.update"));
        putValue(SHORT_DESCRIPTION, Config.getString("tooltip.update"));
        this.updateFrame = updateFrame;
    }

    /* (non-Javadoc)
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent event)
    {
        project = updateFrame.getProject();
        includeLayout = project.getTeamSettingsController().includeLayout();
        
        if (project != null) {
            project.saveAllEditors();
            // doUpdate(project);
            updateFrame.startProgress();
            PkgMgrFrame.displayMessage(project, Config.getString("team.update.statusMessage"));
            
            worker = new UpdateWorker(project);
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
        private Repository repository;
        private TeamworkCommand command;
        private TeamworkCommandResult result;
        
        public UpdateWorker(Project project)
        {
            repository = project.getRepository();
            if (repository != null) {
                command = repository.updateAll(this);
            }
        }
        
        public Object construct()
        {
            removedPackages = new ArrayList();
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
                            ! fileName.equals("bluej.pkg")) {
                        return;
                    }
                    
                    // First find out the package name...
                    String packageName = project.getPackageForFile(f);
                    if (packageName == null) {
                        return;
                    }
                    
                    if (fileName.equals("bluej.pkg")) {
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
                           ! fileName.equals("bluej.pkg")) {
                       return;
                   }
                   
                   // First find out the package name...
                   String packageName = project.getPackageForFile(f);
                   if (packageName == null) {
                       return;
                   }
                   
                   if (fileName.equals("bluej.pkg")) {
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
                            ! fileName.equals("bluej.pkg")) {
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
                    
                    if (fileName.equals("bluej.pkg")) {
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
                        if (pkg == null) {
                            return;
                        }
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
         * @see bluej.groupwork.UpdateListener#handleConflicts(bluej.groupwork.UpdateServerResponse)
         */
        public void handleConflicts(final UpdateResults updateServerResponse)
        {
            if (updateServerResponse == null) {
                return;
            }

            if (updateServerResponse.getConflicts().size() <= 0) {
                return;
            }

            try {
                EventQueue.invokeAndWait(new Runnable() {
                    public void run()
                    {
                        /** A list of files to replace with repository version */
                        Set filesToOverride = new HashSet();

                        // Binary conflicts
                        for (Iterator i = updateServerResponse.getBinaryConflicts().iterator();
                                i.hasNext(); ) {
                            File f = (File) i.next();

                            // TODO proper check for name - case insensitive file systems
                            if (f.getName().equals("bluej.pkg")) {
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

                        List blueJconflicts = new LinkedList();
                        List nonBlueJConflicts = new LinkedList();
                        List targets = new LinkedList();

                        for (Iterator i = updateServerResponse.getConflicts().iterator();
                                i.hasNext();) {
                            UpdateResult updateResult = (UpdateResult) i.next();

                            // Calculate the file base name
                            String fileName = updateResult.getFilename();
                            String baseName;
                            int n = fileName.lastIndexOf('/');
                            if (n != -1) {
                                baseName = fileName.substring(n + 1);
                            }
                            else {
                                baseName = fileName;
                            }

                            // bluej.pkg may come up as a conflict, but it won't cause a problem,
                            // so it can be ignored.
                            if (! baseName.equals("bluej.pkg")) {
                                Target target = null;

                                if (baseName.endsWith(".java") || baseName.endsWith(".class")) {
                                    File file = new File(project.getProjectDir(), fileName);
                                    String pkg = project.getPackageForFile(file);
                                    if (pkg != null) {
                                        String targetId = filenameToTargetIdentifier(baseName);
                                        targetId = JavaNames.combineNames(pkg, targetId);
                                        target = project.getTarget(targetId);
                                    }
                                }
                                else if (baseName.equals("README.TXT")) {
                                    File file = new File(project.getProjectDir(), fileName);
                                    String pkg = project.getPackageForFile(file);
                                    if (pkg != null) {
                                        String targetId = ReadmeTarget.README_ID;
                                        targetId = JavaNames.combineNames(pkg, targetId);
                                        target = project.getTarget(targetId);
                                    }
                                }

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
            updateFrame = null;
        }
        
        public void finished()
        {
            handleRemovedPkgs();
            if (updateFrame != null) {
                updateFrame.stopProgress();
            }

            if (! result.isError()) {
                PkgMgrFrame.displayMessage(project, Config.getString("team.update.statusDone"));
            }
            else {
                PkgMgrFrame.displayMessage(project, "");
                TeamUtils.handleServerResponse(result, updateFrame);
            }
            
            if (updateFrame != null) {
                updateFrame.setVisible(false);
                updateFrame.dispose();
            }
            
            setEnabled(true);
        }
        
        /**
         * If packages were removed by the update, remove them from the
         * parent package graph.
         */
        private void handleRemovedPkgs()
        {
            for (Iterator i = removedPackages.iterator(); i.hasNext(); ) {
                String packageName = i.next().toString();
                String parentPackage = JavaNames.getPrefix(packageName);
                String baseName = JavaNames.getBase(packageName);
                
                File packageDir = JavaNames.convertQualifiedNameToFile(packageName);
                if (! packageDir.exists()) {
                    // Get the parent package so we can remove the child.
                    Package pkg = project.getPackage(parentPackage);
                    if (pkg == null) {
                        return;
                    }
                    Target target = pkg.getTarget(baseName);
                    if (target instanceof PackageTarget) {
                        pkg.removeTarget(target);
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
}
