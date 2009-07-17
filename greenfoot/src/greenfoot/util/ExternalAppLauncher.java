/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009  Poul Henriksen and Michael Kolling 
 
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
package greenfoot.util;

import bluej.Config;

import java.io.IOException;
import java.io.File;

import java.awt.Desktop;
import javax.swing.JFileChooser;

/**
 * A class containing static methods for the purposes of launching external programs.
 * This will probably primarily be used for the editing of media files, i.e.
 * image and sound editing.
 *
 * @author Michael Berry (mjrb4)
 * @version 18/05/09
 */
public class ExternalAppLauncher
{

    /**
     * Opens a file using the OS default program for that file type.
     * @param file the file to open.
     */
    public static void openFile(File file)
    {
        try {
            if(Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                desktop.open(file);
            }
            else {
                throw new RuntimeException("Desktop class is not supported on this platform.");
            }
        }
        catch(Exception ex) {
            JFileChooser jfc = new JFileChooser();
            jfc.setMultiSelectionEnabled(false);
            jfc.setDialogTitle("Open with...");
            int returnVal = jfc.showOpenDialog(null);
            if(returnVal==JFileChooser.APPROVE_OPTION) {
                File program = jfc.getSelectedFile();
                launchProgram(program.toString(), file.toString());
            }
        }
    }

    /**
     * Opens a file for editing using the OS default editor for that file type.
     * @param file the file to open for editing.
     */
    public static void editFile(File file)
    {
        try {
            if(Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                desktop.edit(file);
            }
            else {
                throw new RuntimeException("Desktop class is not supported on this platform.");
            }
        }
        catch(Exception ex) {
            JFileChooser jfc = new JFileChooser();
            jfc.setMultiSelectionEnabled(false);
            jfc.setDialogTitle("Open with...");
            int returnVal = jfc.showOpenDialog(null);
            if(returnVal==JFileChooser.APPROVE_OPTION) {
                File program = jfc.getSelectedFile();
                launchProgram(program.toString(), file.toString());
            }
        }
    }

    /**
     * Launch an external application without any parameters.
     * @param program the path of the application to launch.
     */
    public static void launchProgram(String program)
    {
        if(new File(program).canExecute()) {
            try {
                Runtime.getRuntime().exec(program);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        else {
            System.err.println(program + " is not executable.");
        }
    }

    /**
     * Launch an external application with a single parameter. This is usually
     * the file that the application should open.
     * @param path the path of the application to launch.
     * @param file the file to open in the application.
     */
    public static void launchProgram(String program, String file)
    {
        if(new File(program).canExecute()) {
            String[] cmd = new String[2];
            cmd[0] = program;
            cmd[1] = file;
            try {
                Runtime.getRuntime().exec(cmd);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        else {
            System.err.println(program + " is not executable.");
        }
    }

    /**
     * Launch an external application without any parameters, and block execution
     * of the current thread until it exits.
     * @param program the path of the application to launch.
     * @return the exit value of the program, or -1 if an exception occured. 0
     * indicates normal termination.
     */
    public static int waitForProgram(String program)
    {
        if(new File(program).canExecute()) {
            try {
                Process p = Runtime.getRuntime().exec(program);
                p.waitFor();
                return p.exitValue();
            } catch (Exception ex) {
                ex.printStackTrace();
                return -1;
            }
        }
        else {
            System.err.println(program + " is not executable.");
            return -1;
        }
    }

    /**
     * Launch an external application with a single parameter, and block execution
     * of the current thread until it exits.
     * @param path the path of the application to launch.
     * @param file the file to open in the application.
     * @return the exit value of the program, or -1 if an exception occured. 0
     * indicates normal termination.
     */
    public static int waitForProgram(String program, String file)
    {
        if(new File(program).canExecute()) {
            String[] cmd = new String[2];
            cmd[0] = program;
            cmd[1] = file;
            try {
                Process p = Runtime.getRuntime().exec(cmd);
                p.waitFor();
                return p.exitValue();
            } catch (Exception ex) {
                ex.printStackTrace();
                return -1;
            }
        }
        else {
            System.err.println(program + " is not executable.");
            return -1;
        }
    }

}
