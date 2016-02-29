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
import bluej.parser.InitConfig;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import junit.framework.TestCase;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.StoredConfig;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

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

    private File fileTestA;

    private Repository repository;
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
            initRepository();
        } catch (Throwable ex) {
            Logger.getLogger(GitTester.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void initRepository() throws IOException, GitAPIException
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
    public void testCheckoutRepo()
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

                repository = gitProvider.getRepository(fileTestA, settings);

                // Select parent directory for the new project
                TeamworkCommand checkoutCmd = repository.checkout(fileTestA);
                response = checkoutCmd.getResult();

                failed = response.isError();

            } catch (IOException ex) {
                Logger.getLogger(GitTester.class.getName()).log(Level.SEVERE, null, ex);
                Assert.fail("could not create temporary file.");
            } catch (Throwable ex) {
                Logger.getLogger(GitTester.class.getName()).log(Level.SEVERE, null, ex);
                Assert.fail("could not create Git repo.");
            }
            assertEquals(failed, false);

            //In order to have the Status command working, we need to manually push.
            //this will properly create the master branch. This seems to be the default
            //behaviour of Git: http://www.codeaffine.com/2015/05/06/jgit-initialize-repository/
            File tempFile;
            PrintWriter tempFileWriter;
            tempFile = File.createTempFile("addedFile", "java", fileTestA);
            tempFile.deleteOnExit();
            tempFileWriter = new PrintWriter(tempFile);
            tempFileWriter.println("random content.");
            tempFileWriter.close();
            LinkedHashSet<File> newFiles = new LinkedHashSet<>();
            newFiles.add(tempFile);
            TeamworkCommand commitAllCmd = repository.commitAll(newFiles, new LinkedHashSet<>(), new LinkedHashSet<>(), newFiles, "This commit was made by the GitTester. It should add a file to the repository.");
            commitAllCmd.getResult();
            try (Git repo = Git.open(fileTestA)) {
                repo.push().call();
            }

        } catch (FileNotFoundException ex) {
            Logger.getLogger(GitTester.class.getName()).log(Level.SEVERE, null, ex);
            Assert.fail("Something went wrong...");
        } catch (IOException | GitAPIException ex) {
            Logger.getLogger(GitTester.class.getName()).log(Level.SEVERE, null, ex);
            Assert.fail("Something went wrong...");
        }

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
            PrintWriter tempFileWriter = null;
            try {
                testCheckoutRepo();
                tempFile = File.createTempFile("addedFile", "java", fileTestA);
                tempFile.deleteOnExit();
                tempFileWriter = new PrintWriter(tempFile);
                tempFileWriter.println("random content.");
            } catch (FileNotFoundException ex) {
                Logger.getLogger(GitTester.class.getName()).log(Level.SEVERE, null, ex);
                Assert.fail("could not create temporary file");
            } finally {
                if (tempFileWriter != null) {
                    tempFileWriter.close();
                    LinkedHashSet<File> newFiles = new LinkedHashSet<>();
                    newFiles.add(tempFile);

                    TeamworkCommand commitAllCmd = repository.commitAll(newFiles, new LinkedHashSet<>(), new LinkedHashSet<>(), newFiles, "This commit was made by the GitTester. It should add a file to the repository.");
                    response = commitAllCmd.getResult();

                }

            }

            assertEquals(response.isError(), false);
            TestCase.assertNotNull(tempFile);
            assertEquals(tempFile.exists(), true);
            TestStatusListener listener;
            listener = new TestStatusListener();
            TeamworkCommand repoStatus = repository.getStatus(listener, new CodeFileFilter(tsc.getIgnoreFiles(), false, repository.getMetadataFilter()), false);
            response = repoStatus.getResult();

            assertEquals(listener.getResources().size(), 1);
            assertEquals(listener.getResources().get(0).getRemoteStatus(), TeamStatusInfo.STATUS_NEEDSCHECKOUT);
            assertEquals(listener.getResources().get(0).getFile().getAbsolutePath(), tempFile.getAbsolutePath());

        } catch (IOException ex) {
            Logger.getLogger(GitTester.class.getName()).log(Level.SEVERE, null, ex);
            Assert.fail("Something went wrong.");
        }

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

        }

        List<TeamStatusInfo> getResources()
        {
            return resources;
        }
    }
}
