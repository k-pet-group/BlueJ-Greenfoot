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
import java.io.IOException;
import java.nio.file.Files;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import static junit.framework.TestCase.assertEquals;
import org.junit.Test;

/**
 * Test cases for Git support.
 *
 * @author heday
 */
public class GitTester
{

    private File bluejLibDir;
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
            bluejLibDir = new File("/home/heday/data/NetBeansProjects/bluej/lib");
            Config.initialise(bluejLibDir, commandLineProps, false);
            gitProvider = loadProvider("bluej.groupwork.git.GitProvider");

        } catch (Throwable ex) {
            Logger.getLogger(GitTester.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Tests the checkout operation.
     * 
     */
    @Test
    public void checkoutRepo()
    {
        
        initialize();
        settings = new TeamSettings(gitProvider, "ssh", "localhost", "/home/tester/repo2/", "", "tester", "atsui");
        settings.setYourEmail("my@email.com");
        settings.setYourName("My name");
        
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

    private static TeamworkProvider loadProvider(String name) throws Throwable
    {
        Class<?> c = Class.forName(name);
        Object instance = c.newInstance();
        return (TeamworkProvider) instance;
    }
}
