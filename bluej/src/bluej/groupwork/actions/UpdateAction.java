package bluej.groupwork.actions;

import bluej.Config;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.SwingUtilities;

import org.netbeans.lib.cvsclient.command.CommandAbortedException;
import org.netbeans.lib.cvsclient.command.CommandException;
import org.netbeans.lib.cvsclient.connection.AuthenticationException;

import bluej.groupwork.InvalidCvsRootException;
import bluej.groupwork.Repository;
import bluej.groupwork.UpdateListener;
import bluej.groupwork.UpdateResult;
import bluej.groupwork.UpdateServerResponse;
import bluej.groupwork.ui.ConflictsDialog;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.pkgmgr.Project;
import bluej.pkgmgr.target.ClassTarget;
import bluej.pkgmgr.target.PackageTarget;
import bluej.pkgmgr.target.Target;
import bluej.utility.JavaNames;


/**
 * Action to update out-of-date files.
 * 
 * @author fisker
 * @version $Id: UpdateAction.java 4780 2006-12-22 04:14:21Z bquig $
 */
public class UpdateAction extends TeamAction implements UpdateListener
{
    private Project project;
    private boolean includeLayout;
    
    public UpdateAction()
    {
        super("team.update");
        putValue(SHORT_DESCRIPTION, Config.getString("tooltip.update"));
    }

    /* (non-Javadoc)
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(PkgMgrFrame pmf)
    {
        project = pmf.getProject();
        includeLayout = project.getTeamSettingsController().includeLayout();
        
        if (project != null) {
            project.saveAllEditors();
            doUpdate(project);
        }
    }

    private void doUpdate(final Project project)
    {
        final Repository repository = project.getRepository();
        if (repository == null) {
            return;
        }
        
        Thread thread = new Thread() {
            public void run()
            {
                boolean success = false;
                try {
                    // Get a list of our current pkg files. We exclude them from
                    // the update, to prevent conflicts.

                    List pkgFiles = getPkgFilesInProject(project);
                    File[] pkgArray = (File []) pkgFiles.toArray(new File[pkgFiles.size()]);
                    
                    final UpdateServerResponse response = repository.updateAll(pkgArray, UpdateAction.this);
                                        
                    Runnable projectUpdate = new Runnable() {
                        public void run()
                        {
                            handleConflicts(response);
                        }
                    };
                    
                    SwingUtilities.invokeLater(projectUpdate);
                    
                    // update layout files if necessary
                    if (includeLayout && ! response.isError()) {
                        // Save the current graph layout, so that we pick up
                        // actual changes
                        project.saveAllGraphLayout();
                        repository.updateAndOverride(pkgArray, UpdateAction.this);
                    }
                    
                    success = ! response.isError();
                    
                } catch (CommandAbortedException e) {
                    e.printStackTrace();
                } catch (CommandException e) {
                    e.printStackTrace();
                } catch (AuthenticationException e) {
                    handleAuthenticationException(e);
                } catch (InvalidCvsRootException e) {
                    handleInvalidCvsRootException(e);
                }
                finally {
                    stopProgressBar();
                    if (success) {
                        setStatus(Config.getString("team.update.statusDone"));
                    }
                    else {
                        clearStatus();
                    }
                }
            }
            
            protected void handleConflicts(UpdateServerResponse updateServerResponse)
            {
                if (updateServerResponse == null) {
                    return;
                }
                
                if (updateServerResponse.isError()) {
                    //return;
                }
                
                if (updateServerResponse.getConflicts().size() <= 0) {
                    return;
                }
                
                List blueJconflicts = new LinkedList();
                List nonBlueJConflicts = new LinkedList();
                List targetIdentifiers = new LinkedList();
                
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
                    // so it can be ignored
                    if (! baseName.equals("bluej.pkg")) {
                        Target target = project.getTarget(filenameToTargetIdentifier(
                                updateResult.getFilename()));
                        
                        if (target == null) {
                            nonBlueJConflicts.add(updateResult.getFilename());
                        } else {
                            blueJconflicts.add(updateResult.getFilename());
                            targetIdentifiers.add(filenameToTargetIdentifier(
                                    updateResult.getFilename()));
                        }
                    }
                }
                
                if (! blueJconflicts.isEmpty() || ! nonBlueJConflicts.isEmpty()) {
                    project.clearAllSelections();
                    project.selectTargetsInGraphs(targetIdentifiers);
                    
                    ConflictsDialog conflictsDialog = new ConflictsDialog(project,
                            blueJconflicts, nonBlueJConflicts);
                    conflictsDialog.setVisible(true);
                }
            }
            
            private String filenameToTargetIdentifier(String filename)
            {
                int lastDot = filename.lastIndexOf('.');
                
                return filename.substring(0, lastDot);
            }
        };
        
        thread.start();
        startProgressBar();
        setStatus(Config.getString("team.update.statusMessage"));
    }
    
    /* (non-Javadoc)
     * @see bluej.groupwork.UpdateListener#fileAdded(java.io.File)
     */
    public void fileAdded(final File f)
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
                   // Remove the package
                   String parentPackage = JavaNames.getPrefix(packageName);
                   String pkgName = JavaNames.getBase(packageName);
                   Package pkg = project.getCachedPackage(parentPackage);
                   if (pkg == null) {
                       return;
                   }
                   Target target = pkg.getTarget(pkgName);
                   if (target instanceof PackageTarget) {
                       pkg.removeTarget(target);
                   }
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
                        pkg.reReadGraphLayout();
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
    
    
    /**
     * Get a list of the bluej.pkg files in a project
     */
    private static List getPkgFilesInProject(Project project)
    {
        List pkgFiles = new ArrayList();
        LinkedList dirStack = new LinkedList();
        dirStack.add(project.getProjectDir());
        
        while (! dirStack.isEmpty()) {
            File dir = (File) dirStack.remove(0);
            File [] files = dir.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    dirStack.add(files[i]);
                }
                else if (files[i].getName().equals(Package.pkgfileName)) {
                    pkgFiles.add(files[i]);
                }
            }
        }
        
        return pkgFiles;
    }
}
