/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2016  Michael Kolling and John Rosenberg 
 
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
package bluej.teamwork;

import javax.swing.SwingUtilities;

import bluej.groupwork.CodeFileFilter;
import bluej.groupwork.Repository;
import bluej.groupwork.StatusHandle;
import bluej.groupwork.StatusListener;
import bluej.groupwork.TeamSettings;
import bluej.groupwork.TeamSettingsController;
import bluej.groupwork.TeamStatusInfo;
import bluej.groupwork.TeamworkCommand;
import bluej.groupwork.TeamworkCommandResult;
import bluej.groupwork.TeamworkProvider;
import bluej.groupwork.UpdateListener;
import bluej.groupwork.UpdateResults;
import bluej.groupwork.git.GitStatusHandle;
import bluej.parser.InitConfig;
import bluej.pkgmgr.BlueJPackageFile;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.pkgmgr.Project;
import bluej.pkgmgr.target.Target;
import bluej.utility.DialogManager;
import java.awt.EventQueue;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;

import junit.framework.TestCase;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import static junit.framework.TestCase.assertEquals;

/**
 * Test cases for Git support.
 *
 * @author heday
 */
public class GitTester
{

    private static final String PROTOCOL = "file"; //git communication protocol
    private static final String SERVER = "";
    private static String REMOTE_REPO_ADDRESS; //remote repository address in the filesystem.
    private static final String USER_NAME = "";
    private static final String PASSWORD = "";

    private File fileTestA, fileTestB;

    private Repository repositoryA, repositoryB;
    private TeamSettingsController tsc;

    private TeamworkCommandResult response;

    private TeamworkProvider gitProvider;
    private TeamSettings settings;

    /**
     * General settings common to Git operations
     *
     */
    @Before
    public void initialize()
    {
        try {
            InitConfig.init();
            gitProvider = loadProvider("bluej.groupwork.git.GitProvider");
            initBareRepository();
        } catch (Throwable ex) {
            Logger.getLogger(GitTester.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void initBareRepository() throws IOException, GitAPIException
    {
        File repoFolder = Files.createTempDirectory("test_git_repository").toFile();
        repoFolder.deleteOnExit();

        REMOTE_REPO_ADDRESS = repoFolder.getAbsolutePath();
        try {
            Git repo = Git.init().setBare(true).setDirectory(repoFolder).call();
            repo.close();
            File tmpRepo = Files.createTempDirectory("TmpRepo").toFile();
            tmpRepo.deleteOnExit();

        } catch (IllegalStateException | GitAPIException | IOException e) {
            System.out.println("e" + e.getMessage());
            Assert.fail("Could not create local bare repository");
        }
    }

    /**
     * Tests the checkout operation.
     *
     */
    @Test
    public void testCheckoutRepoA()
    {

        try {

            settings = new TeamSettings(gitProvider, PROTOCOL, SERVER, REMOTE_REPO_ADDRESS, "", USER_NAME, PASSWORD);
            settings.setYourEmail("my@email.com"); // random email
            settings.setYourName("My name"); //random name

            boolean failed = true;
            try {
                fileTestA = Files.createTempDirectory("TestA").toFile();
                fileTestA.deleteOnExit();
                tsc = new TeamSettingsController(fileTestA.getAbsoluteFile());

                repositoryA = gitProvider.getRepository(fileTestA, settings);

                // Select parent directory for the new project
                TeamworkCommand checkoutCmd = repositoryA.checkout(fileTestA);
                response = checkoutCmd.getResult();

                failed = response.isError();

            } catch (IOException ex) {
                Logger.getLogger(GitTester.class.getName()).log(Level.SEVERE, null, ex);
                Assert.fail("could not create temporary file.");
            } catch (Throwable ex) {
                Logger.getLogger(GitTester.class.getName()).log(Level.SEVERE, null, ex);
                Assert.fail("could not create Git repo.");
            }
            assertEquals(false, failed);

            //In order to have the Status command working, we need to manually push.
            //this will properly create the master branch. This seems to be the default
            //behaviour of Git: http://www.codeaffine.com/2015/05/06/jgit-initialize-repository/
            File tempFile;
            tempFile = File.createTempFile("addedFile", "java", fileTestA);
            tempFile.deleteOnExit();
            createFileWithContent(tempFile, "random content.");
            LinkedHashSet<File> newFiles = new LinkedHashSet<>();
            newFiles.add(tempFile);
            TeamworkCommand commitAllCmd = repositoryA.commitAll(newFiles,
                    new LinkedHashSet<>(), new LinkedHashSet<>(), newFiles,
                    "This commit was made by the GitTester as the first commit of the repostiory. "
                    + "It should add a file to the repository. This will finish preparing "
                    + "the repository for use.");
            commitAllCmd.getResult();
            try (Git repo = Git.open(fileTestA)) {
                repo.push().call();
            }

        } catch (FileNotFoundException ex) {
            Logger.getLogger(GitTester.class.getName()).log(Level.SEVERE, null, ex);
            Assert.fail("Something went wrong creating the repository...");
        } catch (IOException | GitAPIException ex) {
            Logger.getLogger(GitTester.class.getName()).log(Level.SEVERE, null, ex);
            Assert.fail("Something went wrong creating the repository...");
        }

    }

    /**
     * Tests the checkout operation.
     *
     */
    @Test
    public void testCheckoutRepoB()
    {

        settings = new TeamSettings(gitProvider, PROTOCOL, SERVER, REMOTE_REPO_ADDRESS, "", USER_NAME, PASSWORD);
        settings.setYourEmail("my@email.com"); // random email
        settings.setYourName("My name"); //random name

        boolean failed = true;
        try {
            fileTestB = Files.createTempDirectory("TestB").toFile();
            fileTestB.deleteOnExit();
            tsc = new TeamSettingsController(fileTestB.getAbsoluteFile());

            repositoryB = gitProvider.getRepository(fileTestB, settings);

            // Select parent directory for the new project
            TeamworkCommand checkoutCmd = repositoryB.checkout(fileTestB);
            response = checkoutCmd.getResult();

            failed = response.isError();

        } catch (IOException ex) {
            Logger.getLogger(GitTester.class.getName()).log(Level.SEVERE, null, ex);
            Assert.fail("could not create temporary file.");
        } catch (Throwable ex) {
            Logger.getLogger(GitTester.class.getName()).log(Level.SEVERE, null, ex);
            Assert.fail("could not create Git repo.");
        }
        assertEquals(false, failed);

    }

    /**
     * Test the add function.
     *
     */
    @Test
    public void testAddFile()
    {

        try {
            File tempFile = null;
            TestStatusListener listener;
            TeamStatusInfo statusItem = null;
            try {
                testCheckoutRepoA();
                tempFile = File.createTempFile("addedFile", "java", fileTestA);
                tempFile.deleteOnExit();
                createFileWithContent(tempFile, "random content.");
            } catch (FileNotFoundException ex) {
                Logger.getLogger(GitTester.class.getName()).log(Level.SEVERE, null, ex);
                Assert.fail("could not create temporary file");
            } finally {
                listener = getRepoStatus(repositoryA, false);

                assertEquals(2, listener.getResources().size());
                statusItem = getStatusItemFromListener(listener, tempFile);
                
                assertEquals(TeamStatusInfo.STATUS_NEEDSADD, statusItem.getStatus());
                assertEquals(tempFile.getAbsolutePath(), statusItem.getFile().getAbsolutePath());
                response = addFileToRepo(repositoryA, "This commit was made by the GitTester. It should add a file to the repository.", tempFile);
            }

            assertEquals(false, response.isError());
            TestCase.assertNotNull(tempFile);
            assertEquals(true, tempFile.exists());

            listener = getRepoStatus(repositoryA, false);
            statusItem = getStatusItemFromListener(listener, tempFile);
            
            assertEquals(2, listener.getResources().size());
            assertEquals(TeamStatusInfo.STATUS_UPTODATE, statusItem.getStatus());
            assertEquals(TeamStatusInfo.STATUS_NEEDSADD, statusItem.getRemoteStatus());
        } catch (IOException ex) {
            Logger.getLogger(GitTester.class.getName()).log(Level.SEVERE, null, ex);
            Assert.fail("Something went wrong.");
        }

    }

    /**
     * Test adding a folder with a file inside.
     */
    @Test
    public void testAddFolderWithFile()
    {
        File tempFolder, tempFile;
        try {
            testCheckoutRepoA();
            tempFolder = Files.createTempDirectory(fileTestA.toPath(), "addedDirectory").toFile();
            tempFolder.deleteOnExit();
            tempFolder.mkdir();

            tempFile = File.createTempFile("CreatedFile", "inSubdirectory", tempFolder);
            tempFile.deleteOnExit();
            createFileWithContent(tempFile, "random content.");

            TestStatusListener listener = getRepoStatus(repositoryA, true);
            assertEquals(3, listener.getResources().size());
            
            TeamStatusInfo statusItem = getStatusItemFromListener(listener, tempFile);
            assertEquals(TeamStatusInfo.STATUS_NEEDSADD, statusItem.getStatus());
            assertEquals(tempFile.getAbsolutePath(), statusItem.getFile().getAbsolutePath());
            
            statusItem = getStatusItemFromListener(listener, tempFolder);
            assertEquals(TeamStatusInfo.STATUS_NEEDSADD, statusItem.getStatus());
            assertEquals(tempFolder.getAbsolutePath(), statusItem.getFile().getAbsolutePath());
            response = addFileToRepo(repositoryA, "This commit was made by the GitTester. It should add a file and a directory to the repository.", new File[]{tempFolder, tempFile});

            assertEquals(false, response.isError());
            assertEquals(true, tempFolder.exists());

            listener = getRepoStatus(repositoryA, true);
            assertEquals(2, listener.getResources().size());
            
            statusItem = getStatusItemFromListener(listener, tempFile);
            assertEquals(TeamStatusInfo.STATUS_UPTODATE, statusItem.getStatus());
            assertEquals(TeamStatusInfo.STATUS_NEEDSADD, statusItem.getRemoteStatus());

        } catch (IOException ex) {
            Logger.getLogger(GitTester.class.getName()).log(Level.SEVERE, null, ex);
            Assert.fail("Something went wrong.");
        }
    }

    /**
     * Test adding a folder with a file inside (like previous test), but now we
     * also remove this file and folder.
     */
    @Test
    public void testAddFolderWithFileAndRemoveFileAndFolder()
    {
        testCheckoutRepoA();
        File tempFolder, tempFile;
        try {
            tempFolder = Files.createTempDirectory(fileTestA.toPath(), "addedDirectory").toFile();
            tempFolder.deleteOnExit();
            tempFolder.mkdir();

            tempFile = File.createTempFile("CreatedFile", "inSubdirectory", tempFolder);
            tempFile.deleteOnExit();
            createFileWithContent(tempFile, "random content.");
            response = addFileToRepo(repositoryA, "This commit was made by the GitTester. It should add a file and a directory to the repository.", new File[]{tempFile});
            //so far, we added a directory and a file inside that directory.

            //now we need to remove them.
            tempFile.delete();
            tempFolder.delete();

            assertEquals(tempFile.exists(), false);
            assertEquals(tempFolder.exists(), false);

            TestStatusListener listener = getRepoStatus(repositoryA, true);
            assertEquals(2, listener.getResources().size());
            
            TeamStatusInfo statusItem = getStatusItemFromListener(listener, tempFile);
            assertEquals(TeamStatusInfo.STATUS_DELETED, statusItem.getStatus());
            assertEquals(TeamStatusInfo.STATUS_UPTODATE, statusItem.getRemoteStatus());
            assertEquals(tempFile.getAbsolutePath(), statusItem.getFile().getAbsolutePath());

            response = RemoveFileFromRepo(repositoryA, "This commit should remove a file and a directory from the repository", new File[]{tempFile});
            assertEquals(false, response.isError());
            listener = getRepoStatus(repositoryA, true);
            assertEquals(1, listener.getResources().size());

        } catch (IOException ex) {
            Logger.getLogger(GitTester.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    /**
     * Checkout a repository in two locations (repositoryA and repositoryB),
     * edit this file on both in order to create a conflict. This test should
     * detect such conflict and decide for one version of the file.
     */
    @Test
    public void createConflict()
    {
        TestStatusListener listener;
        try {
            testCheckoutRepoA();
            testCheckoutRepoB();
            File tempFileRepoA, tempFileRepoB = null;
            String fileContent = "random content for A.";
            tempFileRepoA = File.createTempFile("addedFileA", "java", fileTestA);
            tempFileRepoA.deleteOnExit();
            createFileWithContent(tempFileRepoA, fileContent);

            listener = getRepoStatus(repositoryA, true);

            assertEquals(2, listener.getResources().size());
            
            TeamStatusInfo statusItem = getStatusItemFromListener(listener, tempFileRepoA);
            assertEquals(TeamStatusInfo.STATUS_NEEDSADD, statusItem.getStatus());
            assertEquals(tempFileRepoA.getAbsolutePath(), statusItem.getFile().getAbsolutePath());

            // add, commit and push tempFileRepoA.
            response = addFileToRepo(repositoryA, "File added to repository A. This should not create a conflict.", tempFileRepoA);
            assertEquals(false, response.isError());
            TestCase.assertNotNull(tempFileRepoA);
            assertEquals(true, tempFileRepoA.exists());

            listener = getRepoStatus(repositoryA, true);

            statusItem = getStatusItemFromListener(listener, tempFileRepoA);
            assertEquals(2, listener.getResources().size());
            assertEquals(TeamStatusInfo.STATUS_UPTODATE, statusItem.getStatus());
            assertEquals(TeamStatusInfo.STATUS_NEEDSADD, statusItem.getRemoteStatus());

            //push.
            response = repositoryA.pushChanges().getResult();
            assertEquals(false, response.isError());

            listener = getRepoStatus(repositoryA, true);
            assertEquals(2, listener.getResources().size());

            //done. 
            //now, update repositoryB so it can get the same file.
            listener = getRepoStatus(repositoryB, true);
            assertEquals(2, listener.getResources().size());
            statusItem = getStatusItemFromListener(listener, tempFileRepoA);
            assertEquals(TeamStatusInfo.STATUS_NEEDSCHECKOUT, statusItem.getRemoteStatus());
            assertEquals(TeamStatusInfo.STATUS_UPTODATE, statusItem.getStatus());
            assertEquals(tempFileRepoA.getName(), statusItem.getFile().getName());

            GitStatusHandle statusHandler = listener.getStatusHandle();
            HashSet<File> filesHashSet = new HashSet<>();
            for (TeamStatusInfo item : listener.resources) {
                filesHashSet.add(item.getFile());
            }

            //update (aka pull)
            TeamworkCommand updateTo = statusHandler.updateTo(new TestUpdateListener(null, statusHandler, filesHashSet, new HashSet<>()), filesHashSet, new HashSet<>());
            response = updateTo.getResult();

            assertEquals(false, response.isError());
            for (File tmp : fileTestB.listFiles()) {
                if (tmp.getName().equals(tempFileRepoA.getName())) {
                    tempFileRepoB = tmp;
                    break;
                }
            }
            TestCase.assertNotNull(tempFileRepoB);
            assertEquals(true, tempFileRepoB.exists());

            //In repositoryA, edit the file, commit, push.
            createFileWithContent(tempFileRepoA, fileContent + "edit on repoA");

            response = addFileToRepo(repositoryA, SERVER, tempFileRepoA);
            assertEquals(false, response.isError());
            TestCase.assertNotNull(tempFileRepoA);
            assertEquals(true, tempFileRepoA.exists());
            listener = getRepoStatus(repositoryA, true);

            assertEquals(2, listener.getResources().size());
            statusItem = getStatusItemFromListener(listener, tempFileRepoA);
            assertEquals(TeamStatusInfo.STATUS_NEEDSCOMMIT, statusItem.getRemoteStatus());
            assertEquals(tempFileRepoA.getAbsolutePath(), statusItem.getFile().getAbsolutePath());

            //push.
            response = repositoryA.pushChanges().getResult();
            assertEquals(false, response.isError());

            //In repoB, edit the same file
            createFileWithContent(tempFileRepoB, fileContent + "edit on repository B. This should raise a conflict.");
            response = addFileToRepo(repositoryB, SERVER, tempFileRepoB);
            assertEquals(false, response.isError());
            TestCase.assertNotNull(tempFileRepoB);
            assertEquals(true, tempFileRepoB.exists());
            listener = getRepoStatus(repositoryB, true);
            assertEquals(2, listener.getResources().size());
            statusItem = getStatusItemFromListener(listener, tempFileRepoB);
            assertEquals(TeamStatusInfo.STATUS_NEEDSMERGE, statusItem.getRemoteStatus());
            assertEquals(tempFileRepoB.getAbsolutePath(), statusItem.getFile().getAbsolutePath());

            //push. this sho
            response = repositoryB.pushChanges().getResult();
            assertEquals(false, response.isError());
            listener = getRepoStatus(repositoryB, true);
            assertEquals(2, listener.getResources().size());

            //this file should be marked as a conflicting one.
            statusHandler = listener.getStatusHandle();
            filesHashSet = new HashSet<>();
            for (TeamStatusInfo item : listener.resources) {
                filesHashSet.add(item.getFile());
            }

            updateTo = statusHandler.updateTo(new TestUpdateListener(null, statusHandler, filesHashSet, new HashSet<>()), filesHashSet, new HashSet<>());
            response = updateTo.getResult();
            assertEquals(false, response.isError());
            
            //TODO: add status checks.

        } catch (IOException ex) {
            Logger.getLogger(GitTester.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void createFileWithContent(File file, String content) throws FileNotFoundException
    {
        PrintWriter tempFileWriter;
        tempFileWriter = new PrintWriter(file);
        tempFileWriter.println(content);
        tempFileWriter.close();
    }

    private TeamworkCommandResult addFileToRepo(Repository repo, String message, File... files)
    {
        LinkedHashSet<File> newFiles = new LinkedHashSet<>();
        TeamworkCommandResult result;
        newFiles.addAll(Arrays.asList(files));
        TeamworkCommand commitAllCmd = repo.commitAll(newFiles, new LinkedHashSet<>(), new LinkedHashSet<>(), newFiles, message);
        result = commitAllCmd.getResult();
        return result;
    }

    private TestStatusListener getRepoStatus(Repository repo, boolean remote)
    {
        TestStatusListener listener;
        listener = new TestStatusListener();
        TeamworkCommand repoStatus = repo.getStatus(listener, new CodeFileFilter(tsc.getIgnoreFiles(), false, repo.getMetadataFilter()), remote);
        repoStatus.getResult();
        return listener;
    }

    private TeamworkCommandResult RemoveFileFromRepo(Repository repo, String message, File... files)
    {
        LinkedHashSet<File> deletedFiles = new LinkedHashSet<>();
        TeamworkCommandResult result;
        deletedFiles.addAll(Arrays.asList(files));
        TeamworkCommand commitAllCmd = repo.commitAll(new LinkedHashSet<>(), new LinkedHashSet<>(), deletedFiles, deletedFiles, message);
        result = commitAllCmd.getResult();
        return result;
    }

    private TeamStatusInfo getStatusItemFromListener(TestStatusListener listener, File tempFile)
    {
        TeamStatusInfo result = null;
        for (TeamStatusInfo item : listener.getResources()) {
            if (item.getFile().getName().equals(tempFile.getName())) {
                result = item;
                break;
            }
        }
        return result;
    }

    private static TeamworkProvider loadProvider(String name) throws Throwable
    {
        Class<?> c = Class.forName(name);
        Object instance = c.newInstance();
        return (TeamworkProvider) instance;
    }

    /**
     * This private class is meant to be used only to collect information about
     * the repository files status.
     */
    private class TestStatusListener implements StatusListener
    {

        List<TeamStatusInfo> resources;
        GitStatusHandle statusHandle;

        public TestStatusListener()
        {
            resources = new ArrayList<>();
        }

        @Override
        public void gotStatus(TeamStatusInfo info)
        {
            resources.add(info);
        }

        @Override
        public void statusComplete(StatusHandle statusHandle)
        {
            this.statusHandle = (GitStatusHandle) statusHandle;
        }

        List<TeamStatusInfo> getResources()
        {
            return resources;
        }

        public GitStatusHandle getStatusHandle()
        {
            return this.statusHandle;
        }
    }

    /**
     * This private class is meant to be used to test conflict resolution.
     */
    private class TestUpdateListener implements UpdateListener
    {

        private TeamworkCommand command;
        private TeamworkCommandResult result;
        private boolean aborted;
        private Set<File> filesToUpdate, filesToForceUpdate;
        GitStatusHandle statusHandle;

        /**
         * A list of packages whose bluej.pkg file has been removed
         */
        private List<String> removedPackages;

        public TestUpdateListener(Project project, GitStatusHandle statusHandle,
                Set<File> filesToUpdate, Set<File> filesToForceUpdate)
        {
            command = statusHandle.updateTo(this, filesToUpdate, filesToForceUpdate);
        }

        /**
         * Set the files to be updated (changes merged if necessary).
         *
         * @files a Set of File
         */
        public void setFilesToUpdate(Set<File> files)
        {
            filesToUpdate = files;
        }

        /**
         * Set the files to be updated with a clean copy of the repository
         *
         * @files a Set of File
         */
        public void setFilesToForceUpdate(Set<File> files)
        {
            filesToForceUpdate = files;
        }

        /**
         * Set the status handle (which comes from a preceeding status
         * operation).
         */
        public void setStatusHandle(GitStatusHandle statusHandle)
        {
            this.statusHandle = statusHandle;
        }

        @Override
        public void fileAdded(File f)
        {
            //do nothing
        }

        @Override
        public void fileRemoved(File f)
        {
            //do nothing.
        }

        @Override
        public void fileUpdated(File f)
        {
            //do nothing.
        }

        @Override
        public void dirRemoved(File f)
        {
            //do nothing.
        }

        @Override
        public void handleConflicts(UpdateResults updateServerResponse)
        {
            if (updateServerResponse == null) {
                return;
            }

            if (updateServerResponse.getConflicts().size() <= 0
                    && updateServerResponse.getBinaryConflicts().size() <= 0) {
                return;
            }

                Platform.runLater(() -> {
                    /**
                     * A list of files to replace with repository version
                     */
                    Set<File> filesToOverride = new HashSet<File>();

                    // Binary conflicts
                    for (Iterator<File> i = updateServerResponse.getBinaryConflicts().iterator();
                            i.hasNext();) {
                        File f = i.next();

                        if (BlueJPackageFile.isPackageFileName(f.getName())) {
                            filesToOverride.add(f);
                        } else {
                            // TODO make the displayed file path relative to project
                            int answer = DialogManager.askQuestionFX(PkgMgrFrame.getMostRecent().getFXWindow(),
                                    "team-binary-conflict", new String[]{f.getName()});
                            if (answer == 0) {
                                // keep local version
                            } else {
                                // use repository version
                                filesToOverride.add(f);
                            }
                        }
                    }
                    SwingUtilities.invokeLater(() -> {
                        //TODO: implement overrideFiles!!!!!!!!!!!!!!!!!
                        //updateServerResponse.overrideFiles(filesToOverride);
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
                            if (!BlueJPackageFile.isPackageFileName(baseName)) {
                                Target target = null;

//                                if (baseName.endsWith(".java") || baseName.endsWith(".class")) {
//                                    String pkg = project.getPackageForFile(file);
//                                    if (pkg != null) {
//                                        String targetId = filenameToTargetIdentifier(baseName);
//                                        targetId = JavaNames.combineNames(pkg, targetId);
//                                        target = project.getTarget(targetId);
//                                    }
//                                }
//                                else if (baseName.equals("README.TXT")) {
//                                    String pkg = project.getPackageForFile(file);
//                                    if (pkg != null) {
//                                        String targetId = ReadmeTarget.README_ID;
//                                        targetId = JavaNames.combineNames(pkg, targetId);
//                                        target = project.getTarget(targetId);
//                                    }
//                                }
//                                String fileName = makeRelativePath(project.getProjectDir(), file);
//                                
//                                if (target == null) {
//                                    nonBlueJConflicts.add(fileName);
//                                } else {
//                                    blueJconflicts.add(fileName);
//                                    targets.add(target);
//                                }
                            }
                        }

                        if (!blueJconflicts.isEmpty() || !nonBlueJConflicts.isEmpty()) {
//                            project.clearAllSelections();
//                            project.selectTargetsInGraphs(targets);

//                            ConflictsDialog conflictsDialog = new ConflictsDialog(project,
//                                    blueJconflicts, nonBlueJConflicts);
//                            conflictsDialog.setVisible(true);
                        }
                    });
                });
        }

    }
}
