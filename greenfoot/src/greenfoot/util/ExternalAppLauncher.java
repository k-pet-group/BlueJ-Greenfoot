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

import java.io.IOException;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

import java.awt.Desktop;

import bluej.Config;
import bluej.utility.Debug;

/**
 * A class containing static methods for the purposes of launching external
 * programs. This will probably primarily be used for the editing of media
 * files, i.e. image and sound editing.
 * 
 * @author Michael Berry (mjrb4)
 * @version 18/05/09
 */
public class ExternalAppLauncher
{
    private static String imageEditor = Config.getPropString("greenfoot.editor.image", null);

    /**
     * Opens a file using the OS default program for that file type.
     * 
     * @param file the file to open.
     */
    public static void openFile(File file)
    {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                desktop.open(file);
            }
            else {
                throw new RuntimeException(
                        "Cannot open editor for the file, because the Desktop class is not supported on this platform.");
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Opens an image for editing using the OS default editor for that file
     * type. Only difference from editFile is that this method uses a specific
     * override for images as specified in greenfoot.defs.
     * 
     * @param file the file to open for editing.
     */
    public static void editImage(File file)
    {
        boolean success = false;
        if (imageEditor != null) {
            success = launchProgram(new File(imageEditor), file.toString());
            if(!success) {
                System.err.println("Could not launch the external program: " + imageEditor);
            } 
        }
        if(!success) {
            editFile(file);
        }
    }

    /**
     * Opens a file for editing using the OS default editor for that file type.
     * 
     * @param file the file to open for editing.
     */
    private static void editFile(File file)
    {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                if (desktop.isSupported(Desktop.Action.EDIT)) {
                    desktop.edit(file);
                } else {
                    // They're probably on Linux; let's take our best guess
                    // and use GIMP:
                    Runtime.getRuntime().exec(new String []{"gimp", file.getAbsolutePath()}, null, null);
                }
            }
            else {
                throw new RuntimeException(
                        "Cannot open editor for the file, because the Desktop class is not supported on this platform.");
            }
        }
        catch (Exception ex) {
            Debug.reportError("Error editing image", ex);
        }
    }

    /**
     * Launch an external application with a single parameter. This is usually
     * the file that the application should open.
     * 
     * @param path the path of the application to launch.
     * @param file the file to open in the application.
     */
    public static boolean launchProgram(File program, String file)
    {
        if (Config.isMacOS() && program.isDirectory()) {
            // If we are on a mac, and the program is a directory, we should use
            // the 'open' command.
            String[] cmd = new String[4];
            cmd[0] = "open";
            cmd[1] = "-a";
            cmd[2] = program.toString();
            cmd[3] = file;
            try {
                execWithOutput(cmd);
                return true;
            }
            catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        else if (program.canExecute()) {
            String[] cmd = new String[2];
            cmd[0] = program.toString();
            cmd[1] = file;
            try {
                execWithOutput(cmd);
                return true;
            }
            catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return false;    
        
    }

    /**
     * Use Runtime.getRuntime().exec to execute the given command and redirect
     * the output from the process to System.out and errors to System.err.
     * 
     */
    private static void execWithOutput(String[] cmd)
        throws IOException
    {
        Process p = Runtime.getRuntime().exec(cmd);
        StreamRedirector errRedirector = new StreamRedirector(p.getErrorStream(), System.err);
        errRedirector.start();
        StreamRedirector outRedirector = new StreamRedirector(p.getInputStream(), System.out);
        outRedirector.start();
    }

    /**
     * Class that redirects from an inputstream to an outputstream.
     * 
     * @author Poul Henriksen
     */
    private static class StreamRedirector extends Thread
    {
        private OutputStream target;
        private InputStream source;

        public StreamRedirector(InputStream source, OutputStream target)
        {
            this.source = source;
            this.target = target;
        }

        public void run()
        {
            int len = 0;
            while (len != -1) {
                // CharBuffer target = CharBuffer.allocate(20);
                byte[] bytes = new byte[50];
                try {
                    len = source.read(bytes);
                    if (len != -1) {
                        target.write(bytes, 0, len);
                        target.flush();
                    }
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
