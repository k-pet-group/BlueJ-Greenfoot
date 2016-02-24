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


import bluej.Config;
import bluej.groupwork.Repository;
import bluej.groupwork.TeamSettings;
import bluej.groupwork.TeamSettingsController;
import bluej.groupwork.TeamworkCommand;
import bluej.groupwork.TeamworkCommandResult;
import bluej.groupwork.TeamworkProvider;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import static junit.framework.TestCase.assertEquals;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test cases for Git support.
 *
 * @author heday
 */
public class GitTester
{

    private static final File BLUEJ_DIR = new File("/home/heday/data/NetBeansProjects/bluej/lib"); //BlueJ's lib folder
    private static final String PROTOCOL = "ssh"; //git communication protocol
    private static final String SERVER = "localhost"; //server address. Usually localhost.
    private static String REMOTE_REPO_ADDRESS; //remote repository address in the filesystem.
    private static final String USER_NAME="tester"; //username to access the repository (ssh's login)
    private static final String PASSWORD="atsui"; //ssh's password.
    
    private Properties commandLineProps;

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
    public void initialize()
    {
        try {
            commandLineProps = new Properties();
            Config.initialise(BLUEJ_DIR, commandLineProps, false);
            gitProvider = loadProvider("bluej.groupwork.git.GitProvider");
            initRepository();
        } catch (Throwable ex) {
            Logger.getLogger(GitTester.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void initRepository() throws IOException
    {
        File repoFolder = Files.createTempDirectory("test_git_repository").toFile();
        repoFolder.deleteOnExit(); 
        
        //set POSIX attributes.        
        Set<PosixFilePermission> perms = new HashSet<>();
        //add owners permission
        perms.add(PosixFilePermission.OWNER_READ);
        perms.add(PosixFilePermission.OWNER_WRITE);
        perms.add(PosixFilePermission.OWNER_EXECUTE);
        //add group permissions
        perms.add(PosixFilePermission.GROUP_READ);
        perms.add(PosixFilePermission.GROUP_WRITE);
        perms.add(PosixFilePermission.GROUP_EXECUTE);
        //add others permissions
        perms.add(PosixFilePermission.OTHERS_READ);
        perms.add(PosixFilePermission.OTHERS_WRITE);
        perms.add(PosixFilePermission.OTHERS_EXECUTE);
        
        Files.setPosixFilePermissions(repoFolder.toPath(), perms);
        
        REMOTE_REPO_ADDRESS = repoFolder.getAbsolutePath();
        org.eclipse.jgit.lib.Repository r = new FileRepositoryBuilder().setGitDir(repoFolder).setBare().build();
        r.create(true);
        r.close();
        
        
    }

    /**
     * Tests the checkout operation.
     *
     */
    @Test
    public void testCheckoutRepo()
    {

        initialize();
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
        } catch (Throwable ex) {
            Logger.getLogger(GitTester.class.getName()).log(Level.SEVERE, null, ex);
        }
        assertEquals(failed, false);
    }

    /**
     * Test the add function.
     * 
     */
    @Test
    public void testAddFile()
    {
        try {
            File tempFile;
            PrintWriter testFile = null;
            tempFile = File.createTempFile("addedFile", "java", fileTestA);
            tempFile.deleteOnExit();
            try {
                testCheckoutRepo();
                testFile = new PrintWriter(tempFile);
                testFile.println("random content.");
            } catch (FileNotFoundException ex) {
                Logger.getLogger(GitTester.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                if (testFile != null) {
                    testFile.close();
                    LinkedHashSet<File> newFiles = new LinkedHashSet<>();
                    newFiles.add(tempFile);

                    TeamworkCommand commitAllCmd = repository.commitAll(newFiles, new LinkedHashSet<>(), new LinkedHashSet<>(), newFiles, "This commit was made by the GitTester. It should add a file to the repository.");
                    response = commitAllCmd.getResult();
                    
                }

            }
            assertEquals(response.isError(), false);
            assertEquals(tempFile.exists(), true);

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
}
