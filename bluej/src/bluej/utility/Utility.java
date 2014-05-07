/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2011,2012,2013,2014  Michael Kolling and John Rosenberg 
 
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
package bluej.utility;

import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Shape;
import java.awt.Window;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import javax.swing.AbstractButton;
import javax.swing.UIDefaults;
import javax.swing.border.Border;
import javax.swing.text.TabExpander;

import com.apple.eawt.Application;

import bluej.Config;

/**
 * Some generally useful utility methods available to all of bluej.
 * 
 * @author Michael Cahill
 * @author Michael Kolling
 */
public class Utility
{
    /**
     * Used to track which events have occurred for firstTimeThisRun()
     */
    private static Set<String> occurredEvents = new HashSet<String>();

    /**
     * Draw a thick rectangle - another of the things missing from the AWT
     */
    public static void drawThickRect(Graphics g, int x, int y, int width, int height, int thickness)
    {
        for (int i = 0; i < thickness; i++)
            g.drawRect(x + i, y + i, width - 2 * i, height - 2 * i);
    }
    
    /**
     * Draw a thick rounded rectangle - another of the things missing from the AWT
     */
    public static void drawThickRoundRect(Graphics g, int x, int y, int width, int height, int arc, int thickness)
    {
        for (int i = 0; i < thickness; i++)
            g.drawRoundRect(x + i, y + i, width - 2 * i, height - 2 * i, arc, arc);
    }

    /**
     * Draw stripes over a rectangle - yet another thing missing from the AWT
     */
    public static void stripeRect(Graphics g, int x, int y, int width, int height, int separation, int thickness)
    {
        for (int offset = 0; offset < width + height; offset += separation)
            for (int i = 0; i < thickness; i++, offset++) {
                int x1, y1, x2, y2;

                if (offset < height) {
                    x1 = x;
                    y1 = y + offset;
                }
                else {
                    x1 = x + offset - height;
                    y1 = y + height;
                }

                if (offset < width) {
                    x2 = x + offset;
                    y2 = y;
                }
                else {
                    x2 = x + width;
                    y2 = y + offset - width;
                }

                g.drawLine(x1, y1, x2, y2);
            }
    }

    /**
     * Draw a string at a given location on screen centered in a given
     * rectangle.<br>
     * Left justifies the string if it is too long to fit all of the string
     * inside the rectangle.
     */
    public static void drawCentredText(Graphics g, String str, int x, int y, int width, int height)
    {
        FontMetrics fm = g.getFontMetrics();

        Shape oldClip = g.getClip();
        g.clipRect(x, y, width, height);
        int xOffset = (width - fm.stringWidth(str)) / 2;
        if (xOffset < 0) {
            xOffset = 0;
        }
        // This is the space left around the text, divided by 2 (equal gap above and below)
        // to get the top of the text, plus the ascent to get the baseline 
        int yOffset = fm.getAscent() + ((height - fm.getAscent() - fm.getDescent()) / 2);
        g.drawString(str, x + xOffset, y + yOffset);
        g.setClip(oldClip);
    }

    /**
     * Draw a string at a given location on screen right-aligned in a given
     * rectangle.
     */
    public static void drawRightText(Graphics g, String str, int x, int y, int width, int height)
    {
        FontMetrics fm = g.getFontMetrics();

        Shape oldClip = g.getClip();
        g.clipRect(x, y, width, height);
        g.drawString(str, x + width - fm.stringWidth(str), y + (height + fm.getAscent()) / 2);
        g.setClip(oldClip);
    }

    /**
     * Splits "string" by "Delimiter"
     * 
     * @param str - the string to be split
     * @param delimiter - the field delimiter within str
     * @returns an array of Strings
     */
    public static String[] split(String str, String delimiter)
    {
        List<String> strings = new ArrayList<String>();
        int start = 0;
        int len = str.length();
        int dlen = delimiter.length();
        int offset = str.lastIndexOf(delimiter); // First of all, find the
        // Last occurance of the Delimiter
        // Stop empty delimiters
        if (dlen < 1)
            return null;
        else if (offset < 0) // one element
        {
            String[] result = {str};
            return result;
        }

        //
        // Append the delimiter onto the end if it doesn't already exit
        //
        if (len > offset + dlen) {
            str += delimiter;
            len += dlen;
        }

        do {
            // Get the new Offset
            offset = str.indexOf(delimiter, start);
            strings.add(str.substring(start, offset));

            // Get the new Start position
            start = offset + dlen;
        } while ((start < len) && (offset != -1));

        // Convert the list into an Array of Strings
        String result[] = new String[strings.size()];
        strings.toArray(result);
        return result;
    }

    /**
     * Splits "string" into lines (stripping end-of-line characters)
     * 
     * @param str - the string to be split
     * @returns an array of Strings
     */
    public static String[] splitLines(String str)
    {
        return (str == null ? null : split(str, "\n"));
    }

    /**
     * Return a string in which all the quotable characters (tab, newline, ' and ",
     * etc) are quoted, Java-style.
     */
    public static String quoteString(String src)
    {
        StringBuffer buf = new StringBuffer();

        for (int i = 0; i < src.length(); i++) {
            char c = src.charAt(i);
            if (c == '\n')
                buf.append("\\n");
            else if (c == '\r')
                buf.append("\\r");
            else if (c == '\t')
                buf.append("\\g");
            else if (c < 32 || c > 128) {
                // Character is outside normal ASCII range, output it as unicode
                // escape sequence.
                String n = Integer.toHexString(c);
                n = "0000".substring(n.length()) + n;
                buf.append("\\u");
                buf.append(n);
            }
            else {
                if (c == '\\' || c == '"' || c == '\'')
                    buf.append('\\');
                buf.append(src.charAt(i));
            }
        }

        return buf.toString();
    }

    /**
     * Translate a given, qualified class name into a URL where we believe its
     * documentation to be, and display that URL in a web browser.
     */
    public static void showClassDocumentation(String classname, String suffix)
    {
        classname = classname.replace('.', '/');
        String docURL = Config.getPropString("bluej.url.javaStdLib");
        if (docURL.endsWith(".html")) {
            int lastSlash = docURL.lastIndexOf('/');
            if (lastSlash != -1)
                docURL = docURL.substring(0, lastSlash + 1);
        }
        // Debug.message(docURL + classname + ".html" + suffix);
        openWebBrowser(docURL + classname + ".html" + suffix);
    }

    /**
     * Let the given URL be shown in a browser window.
     * 
     * @param url the URL or file path to be shown.
     * @return true if the web browser could be started, false otherwise.
     */
    public static boolean openWebBrowser(String url)
    {
        if (Config.isWinOS()) { // Windows

            String cmd;
            // catering for stupid differences in Windows shells...
            if (Config.osname.startsWith("Windows 9") || Config.osname.equals("Windows Me")) // win95/98/Me
                cmd = "command.com";
            else
                // other
                cmd = "cmd.exe";

            try {
                // more stupid Windows differences...
                if (Config.osname.startsWith("Windows 98") || Config.osname.equals("Windows Me")) {
                    Runtime.getRuntime().exec(new String[]{cmd, "/c", "start", '"' + url + '"'});
                }
                else {
                    Runtime.getRuntime().exec(new String[]{cmd, "/c", "start", "\"\"", '"' + url + '"'});
                }
            }
            catch (IOException e) {
                Debug.reportError("could not start web browser. exc: " + e);
                return false;
            }
        }
        else { // Mac, Unix and other

            // The string should be either a URL or a file path
            try {
                return openWebBrowser(new URL(url));
            }
            catch (MalformedURLException mfue) {
                return openWebBrowser(new File(url));
            }

        }
        return true;
    }

    /**
     * Let the given URL be shown in a browser window.
     * 
     * @param url the URL to be shown.
     * @return true if the web browser could be started, false otherwise.
     */
    public static boolean openWebBrowser(URL url)
    {
        if (Config.isWinOS()) {
            // Windows
            return openWebBrowser(url.toString());
        }
        else {
            Exception exception = null;
            
            if (Desktop.isDesktopSupported()) {
                try {
                    Desktop.getDesktop().browse(url.toURI());
                }
                catch (IOException ioe) { exception = ioe; }
                catch (URISyntaxException use) { exception = use; }

                if (exception == null) {
                    return true; // success
                }
            }
            
            if (Config.isMacOS()) {
                Debug.reportError("could not start web browser. exc: " + exception);
                return false;
            }
            
            // Unix and other

            String cmd = mergeStrings(Config.getPropString("browserCmd1"), url.toString());
            String cmd2 = mergeStrings(Config.getPropString("browserCmd2"), url.toString());
            String cmd3 = mergeStrings("xdg-open $", url.toString());

            Process p = null;
            try {
                p = Runtime.getRuntime().exec(cmd);
            }
            catch (IOException e) {
                try {
                    p = Runtime.getRuntime().exec(cmd2);
                    cmd2 = null;
                }
                catch (IOException e2) {
                    try{
                        p = Runtime.getRuntime().exec(cmd3);
                        cmd3 = null;
                    }
                    catch (IOException e3) {
                        Debug.reportError("could not start web browser.  exc: " + e);
                        return false;
                    }
                }
            }

            final String command2 = cmd2;
            final Process process = p;
            new Thread() {
                public void run()
                {
                    runUnixWebBrowser(process, command2);
                }
            }.start();
        }
        return true;
    }

    /**
     * Wait for the given process to finish, try running the second command if
     * it returns false.
     * 
     * @param p
     * @param url
     * @param cmd2
     */
    private static void runUnixWebBrowser(Process p, String cmd2)
    {
        try {
            // wait for exit code. 0 indicates success, otherwise
            // we try second command
            int exitCode = p.waitFor();

            if (exitCode != 0 && cmd2 != null && cmd2.length() > 0) {
                p = Runtime.getRuntime().exec(cmd2);
            }
        }
        catch (InterruptedException ie) {
            Debug.reportError("cannot start web browser:");
            Debug.reportError("caught exc " + ie);
        }
        catch (IOException ioe) {
            Debug.reportError("cannot start web browser:");
            Debug.reportError("caught exc " + ioe);
        }
    }

    /**
     * Let the given file be shown in a browser window.
     * 
     * @param file the file to be shown.
     * @return true if the web browser could be started, false otherwise.
     */
    public static boolean openWebBrowser(File file)
    {
        if (Config.isWinOS()) { // Windows
            return openWebBrowser(file.toString());
        }
        else { // Mac, Unix and other
            try {
                return openWebBrowser(file.toURI().toURL());
            }
            catch (MalformedURLException mfue) {
                // This shouldn't happen.
                return false;
            }
        }
    }
    
    /**
     * Method copied from Boot since we don't always have access to Boot here (if this method is called from the user VM for instance).
     * 
     * Calculate the bluejLibDir value by doing some reasoning on a resource 
     * we know we have: the .class file for the Utility class.
     *
     * @return    the path of the BlueJ lib directory
     */
    private static File calculateBluejLibDir()
    {
        File bluejDir = null;
        String bootFullName = Utility.class.getResource("Utility.class").toString();

        try {
            if (! bootFullName.startsWith("jar:")) {
                // Boot.class is not in a jar-file. Find a lib directory somewhere
                // above us to use
                File startingDir = (new File(new URI(bootFullName)).getParentFile());
                while((startingDir != null) &&
                        !(new File(startingDir.getParentFile(), "lib").isDirectory())) {
                    startingDir = startingDir.getParentFile();
                }
                
                if (startingDir == null) {
                    bluejDir = null;
                }
                else {
                    bluejDir = new File(startingDir.getParentFile(), "lib");
                }
            }
            else {
                // The class is in a jar file, '!' separates the jar file name
                // from the class name. Cut off the class name and the "jar:" prefix.
                int classIndex = bootFullName.indexOf("!");
                String bootName = bootFullName.substring(4, classIndex);
                
                File finalFile = new File(new URI(bootName));
                bluejDir = finalFile.getParentFile();
            }   
        } 
        catch (URISyntaxException use) { }
        
        return bluejDir;
    }
    
    
    /**
     * Bring the current process to the front in the OS window stacking order.
     * The given window will be brought to the front.
     * 
     * <p>This method can be called from the debug VM.
     * 
     * @param window   the window to be brought to the front. If null, the process
     *                 is brought to the front.
     */
    public static void bringToFront(final Window window)
    {
        // If not showing at all we return now.
        if (window != null) {
            if (!window.isShowing() || !window.getFocusableWindowState()) {
                return;
            }
            window.toFront();
        }
        
        if (Config.isMacOS()) {
            Application.getApplication().requestForeground(false);
            return;
        }

        String pid = getProcessId();
        boolean isWindows = Config.isWinOS();

        if (isWindows) {
            // Use WSH (Windows Script Host) to execute a javascript that brings
            // a window to front.
            File libdir = calculateBluejLibDir();
            String[] command = new String[] {"cscript","\"" + libdir.getAbsolutePath() + "\\windowtofront.js\"",pid };
            
            final StringBuffer commandAsStr = new StringBuffer();
            for (int i = 0; i < command.length; i++) {
                commandAsStr.append(command[i] + " ");
            }

            try {
                Process p = Runtime.getRuntime().exec(command);
                new ExternalProcessLogger(command[0], commandAsStr.toString(), p).start();
                if (isWindows) {
                    // An apparent JDK bug causes us to lose the ability to receive
                    // input if the script is executed while a popup window is showing.
                    // In an attempt to avoid that we'll wait for the script to execute
                    // now:
                    new ProcessWaiter(p).waitForProcess(500);
                }
            }
            catch (IOException e) {
                Debug.reportError("While trying to launch \"" + command[0] + "\", got this IOException:", e);
            }
            catch (InterruptedException ie) {}
        }
    }

    private static class ExternalProcessLogger extends Thread
    {
        String commandAsStr;
        String processName; 
        Process p;
        
        public ExternalProcessLogger(String processName, String command, Process process)
        {
            this.processName = processName;
            commandAsStr = command;
            p = process;
        }
        
        @Override
        public void run()
        {
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            StringBuffer extra = new StringBuffer();

            try {
                char[] buf = new char[1024];
                Thread.sleep(1000);

                // discontinue if no data available or stream closed
                if (br.ready()) {
                    int len = br.read(buf);
                    if (len != -1) {
                        extra.append(buf, 0, len);
                    }
                }
                
                if (extra.length() != 0) {
                    Debug.message("When trying to launch " + processName + ":" + commandAsStr);
                    Debug.message(" This error was recieved: " + extra);
                }
            }
            catch (InterruptedException ie) {}
            catch (IOException ioe) {}
            finally {
                try {
                    br.close();
                }
                catch (IOException ioe) {}
            }
        }
    }
    
    /**
     * A utility class to wait for an external process to complete.
     * This allows waiting with a timeout, unlike the Process.waitFor()
     * method. Simply create a ProcessWaiter, and then call {@code wait()}
     * or {@code wait(long)} on the ProcessWaiter.
     */
    private static class ProcessWaiter
    {
        boolean complete = false;
        
        public ProcessWaiter(final Process p)
        {
            new Thread() {
                public void run() {
                    try {
                        p.waitFor();
                    }
                    catch (InterruptedException ie) {}
                    synchronized (ProcessWaiter.this) {
                        complete = true;
                        ProcessWaiter.this.notify();
                    }
                };
            }.start();
        }
        
        /**
         * Wait for the process to complete, with the given timeout.
         * If the timeout is 0, wait indefinitely.
         */
        public synchronized void waitForProcess(long timeout)
            throws InterruptedException
        {
            while (! complete) {
                wait(timeout);
            }
        }
    }
    
    
    /**
     * Get the process ID of this process.
     */
    public static String getProcessId()
    {
        String pid = ManagementFactory.getRuntimeMXBean().getName();
        // Strip the host name from the pid.
        int atIndex = pid.indexOf("@");
        if (atIndex != -1) {
            pid = pid.substring(0, atIndex);
        }
        return pid;
    }

   
    /**
     * merge s2 into s1 at position of first '$'
     */
    public static String mergeStrings(String s1, String s2)
    {
        int pos = s1.indexOf('$');
        if (pos == -1)
            return s1;
        else
            return s1.substring(0, pos) + s2 + s1.substring(pos + 1);
    }

    /**
     * merge strings in s2 into s1 at positions of '$'
     */
    public static String mergeStrings(String s1, String s2[])
    {
        for (int current = 0; current < s2.length; current++) {
            s1 = mergeStrings(s1, s2[current]);
        }

        return s1;
    }

    /**
     * Converts tabs in a String into a specified number of spaces. It assumes
     * that beginning of String is the starting point of tab offsets.
     * 
     * @param original the String to convert
     * @param tabSize number of spaces to be inserted in place of tab
     * @return the String with spaces replacing tabs (if tabs present).
     */
    public static String convertTabsToSpaces(String originalString, int tabSize)
    {
        // if there are tab(s) in the String
        if (originalString.indexOf('\t') != -1) {
            StringBuffer buffer = new StringBuffer(originalString);
            for (int i = 0; i < buffer.length(); i++) {
                if (buffer.charAt(i) == '\t') {
                    buffer.deleteCharAt(i);
                    // calculate how many spaces to add
                    int numberOfSpaces = tabSize - (i % tabSize);
                    for (int j = 0; j < numberOfSpaces; j++)
                        buffer.insert(i, ' ');
                }
            }
            return buffer.toString();
        }
        else
            return originalString;
    }
    
    /**
     * Calculates how many spaces each tab in the given string turns into.
     * 
     * If there is a tab at character index N, the array entry N in the
     * returned array will indicate how many spaces the tab converts into.
     * The value of all other entries is undefined.
     */
    public static int[] calculateTabSpaces(String line, int tabSize)
    {
        // Bigger array than necessary, but we're only doing one line at a time:
        int[] tabSpaces = new int[line.length()];
        int curPos = 0;
        for (int i = 0; i < line.length(); i++) {
            if (line.charAt(i) == '\t') {
                // calculate how many spaces to add
                int numberOfSpaces = tabSize - (curPos % tabSize);
                tabSpaces[i] = numberOfSpaces;
                curPos += numberOfSpaces;
            }
            else {
                curPos += 1;
            }
        }
        return tabSpaces;
    }
    
    /**
     * Makes a TabExpander object that will turn tabs into the appropriate
     * white-space, based on the original String.  This means that the tabs
     * will get aligned to the correct tab-stops rather than just being
     * converted into a set number of spaces.  Thus, the TabExpander will match
     * the behaviour of the editor.
     */
    public static TabExpander makeTabExpander(String line, int tabSize, final FontMetrics fontMetrics)
    {
        final int[] tabSpaces = Utility.calculateTabSpaces(line, tabSize);
        
        return new TabExpander() {
            @Override
            public float nextTabStop(float x, int tabOffset) {
                return x + tabSpaces[tabOffset] * fontMetrics.charWidth(' ');
            }
        };
    }
    
    /**
     * Given a String and an index into it, along with the pre-calculated tabSpaces array,
     * advances the index by the given number of character widths.
     * 
     * If the String contains to tabs, this effectively adds advanceBy to index.
     * 
     * If the String does contain tabs, their width is taken into account
     * as the index is advanced through the array.
     * 
     */
    public static int advanceChars(String line, int[] tabSpaces, int index, int advanceBy)
    {
        while (advanceBy > 0 && index < line.length())
        {
            int width = (line.charAt(index) == '\t') ? tabSpaces[index] : 1;
            advanceBy -= width;
            index += 1;
        }
        return index;
    }

    /**
     * Check if this is the first time a particular event (identified by the
     * context string) has occurred during this run of BlueJ.
     * 
     * @param context Identifies the event (suggested:
     *            fully-qualified-class-name:event-id)
     * @return true the first time the method was called with the given context;
     *         false every subsequent time.
     */
    public static boolean firstTimeThisRun(String context)
    {
        if (occurredEvents.contains(context))
            return false;

        occurredEvents.add(context);
        return true;
    }

    /**
     * Check if this is the first time a particular event (identified by the
     * context string) has occurred "ever" (in this BlueJ installation).
     * 
     * @param context Identifies the event (a property name)
     * @return true the first time the method was called with the given context;
     *         false every subsequent time.
     */
    public static boolean firstTimeEver(String context)
    {
        boolean occurred = Config.getPropBoolean(context);
        if (occurred) {
            return false;
        }

        Config.putPropBoolean(context, true);
        return true;
    }

    /**
     * This method creates a MacOS button. It will create a "textured" button on
     * MacOS 10.5 and newer and a "toolbar" button on older MasOS.
     * 
     * @param button The button that should be changed.
     */
    public static void changeToMacButton(AbstractButton button)
    {
        // available button styles, as of MacOS 10.5:
        // square, gradient, bevel, textured, roundRect, recessed, help
        // segmented styles:
        // segmented, segmentedRoundRect, segmentedCapsule, segmentedTextured
        // see: http://developer.apple.com/technotes/tn2007/tn2196.html

        if (!Config.isMacOS()) {
            return;
        }

        Border oldBorder = button.getBorder();

        // the following works since MacOS 10.5
        button.putClientProperty("JButton.buttonType", "square");

        if (oldBorder == button.getBorder()) {
            // if the border didn't change the "square" type probably doesn't
            // exist, which means we are running on MacOS < 10.5. This means we
            // should use the old pre-10.5 "toolbar" style instead.
            button.putClientProperty("JButton.buttonType", "toolbar");
        }
        else {
            // if we get to this point, the square button type is available, and
            // we can continue configuring for that one.
            button.setMargin(new Insets(3, 1, 3, 1));
        }
    }
    
    /**
     * Attempt to determine the prefix folder of a zip or jar archive.
     * That is, if all files in the archive are stored under a first-level
     * folder, return the name of that folder; otherwise return null.
     * 
     * @param arName   The archive file
     * @return         The prefix folder of the archive, or null.
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static String getArchivePrefixFolder(File arName)
    throws FileNotFoundException, IOException
    {
        JarInputStream jarInStream = null;
        FileInputStream is = null;
        String prefixFolder = null;
        try {
            is = new FileInputStream(arName);
            jarInStream = new JarInputStream(is);
            
            // Extract entries in the jar file
            JarEntry je = jarInStream.getNextJarEntry();
            while (je != null) {
                String entryName = je.getName();
                int slashIndex = entryName.indexOf('/');
                if (slashIndex == -1) {
                    prefixFolder = null;
                    break;
                }
                
                String prefix = entryName.substring(0, slashIndex);
                if (prefixFolder == null)
                    prefixFolder = prefix;
                else if (! prefixFolder.equals(prefix)) {
                    prefixFolder = null;
                    break;
                }
                
                je = jarInStream.getNextJarEntry();
            }
        }
        catch (FileNotFoundException fnfe) {
            throw fnfe;  // rethrow after processing finally block
        }
        catch (IOException ioe) {
            throw ioe; // rethrow after processing finally block
        }
        finally {
            if (jarInStream != null)
                jarInStream.close();
            if (is != null)
                is.close();
        }
        
        return prefixFolder;
    }

    /**
     * Attempt to intelligently extract an archive (zip, jar).
     *  
     * @param archive  the archive file
     * @param parent  parent component for dialogs
     * 
     * @return  the single folder containing the extracted archive contents,
     *          or null if the archive couldn't be extracted (in which case
     *          an error dialog is displayed).
     */
    public static File maybeExtractArchive(File archive, Component parent)
    {
        JarInputStream jarInStream = null;
        File oPath = archive.getParentFile();
    
        try { 
            // first need to determine the output path. If the jar file
            // contains a root-level (eg bluej.pkg) entry, extract into a directory
            // whose name is the basename of the archive file. Otherwise, if
            // all entries have a common ancestor, extract to that directory
            // (after checking it doesn't exist).
            String prefixFolder = getArchivePrefixFolder(archive);
            
            if (prefixFolder == null) {
                // Try to extract to directory which has same name as the jar
                // file, with the .jar or .bjar extension stripped.
                String archiveName = archive.getName();
                int dotIndex = archiveName.lastIndexOf('.');
                String strippedName = null;
                if(dotIndex != -1) {
                    strippedName = archiveName.substring(0, dotIndex);
                } else {
                    strippedName = archiveName;
                }
                oPath = new File(oPath, strippedName);
                if (oPath.exists()) {
                    DialogManager.showErrorWithText(parent, "jar-output-dir-exists", oPath.toString());
                    return null;
                }
                else if (! oPath.mkdir()) {
                    DialogManager.showErrorWithText(parent, "jar-output-no-write", archive.toString());
                    return null;
                }
            }
            else {
                File prefixFolderFile = new File(oPath, prefixFolder);
                if (prefixFolderFile.exists()) {
                    DialogManager.showErrorWithText(parent, "jar-output-dir-exists", prefixFolderFile.toString());
                    return null;
                }
                if (! prefixFolderFile.mkdir()) {
                    DialogManager.showErrorWithText(parent, "jar-output-no-write", archive.toString());
                    return null;
                }
            }
            
            // Need to extract the project somewhere, then open it
            FileInputStream is = new FileInputStream(archive);
            jarInStream = new JarInputStream(is);
            
            // Extract entries in the jar file
            JarEntry je = jarInStream.getNextJarEntry();
            while (je != null) {
                File outFile = new File(oPath, je.getName());
                
                // An entry could represent a file or directory
                if (je.getName().endsWith("/"))
                    outFile.mkdirs();
                else {
                    outFile.getParentFile().mkdirs();
                    OutputStream os = new FileOutputStream(outFile);
                    
                    // try to read 8k at a time
                    byte [] buffer = new byte[8192];
                    int rlength = jarInStream.read(buffer);
                    while (rlength != -1) {
                        os.write(buffer, 0, rlength);
                        rlength = jarInStream.read(buffer);
                    }
                    
                    jarInStream.closeEntry();
                    os.close();
                }
                je = jarInStream.getNextJarEntry();
            }
            
            // Now, the jar file may contain a bluej project, or it may
            // be a regular jar file in which case we should convert it
            // to a bluej project first.
            
            if (prefixFolder != null)
                oPath = new File(oPath, prefixFolder);
        }
        catch (Exception e) {
            e.printStackTrace();
            DialogManager.showError(parent, "jar-extraction-error");
            return null;
        }
        finally {
            try {
                if (jarInStream != null)
                    jarInStream.close();
            }
            catch (IOException ioe) {}
        }
        return oPath;
    }
    
    /**
     * Convert an array of files into a classpath string that can be used to start a VM.
     * If files is null or files is empty then an empty string is returned.
     * 
     * @param files an array of files.
     * @return a non null string, possibly empty.
     */
    public static final String toClasspathString(File[] files)
    {
        if ((files == null) || (files.length < 1)) {
            return "";
        }

        boolean addSeparator = false; // Do not add a separator at the beginning
        StringBuffer buf = new StringBuffer();

        for (int index = 0; index < files.length; index++) {
            File file = files[index];

            // It may happen that one entry is null, strange, but just skip it.
            if (file == null) {
                continue;
            }

            if (addSeparator) {
                buf.append(File.pathSeparatorChar);
            }

            buf.append(file.toString());

            // From now on, you have to add a separator.
            addSeparator = true;
        }

        return buf.toString();
    }
    
    /**
     * Transform an array of URL into an array of File. Any non-file URLs are skipped.
     * 
     * @param urls  an array of URL to be converted
     * @return  a non null (but possibly empty) array of File
     */
    public static final File[] urlsToFiles(URL[] urls)
    {
        if ((urls == null) || (urls.length < 1)) {
            return new File[0];
        }

        List<File> rlist = new ArrayList<File>();

        for (int index = 0; index < urls.length; index++) {
            URL url = urls[index];

            // A class path is always without the qualifier file in front of it.
            // However some characters (such as space) are encoded.
            
            if ("file".equals(url.getProtocol())) {
                URI uri = URI.create(url.toString());
                rlist.add(new File(uri));
            }
        }

        return rlist.toArray(new File[rlist.size()]);
    }

    /**
     * Break a quoted command-line string into separate arguments.
     */
    public static List<String> dequoteCommandLine(String str)
    {
        List<String> strings = new ArrayList<String>();
        
        int i = 0;
        while (i < str.length()) {
            // Skip white space
            while (i < str.length() && Character.isWhitespace(str.charAt(i))) {
                i++;
            }
            
            StringBuffer arg = new StringBuffer();
            char c;
            
            while (i < str.length()) {
                c = str.charAt(i++);
                if (c == '\\') {
                    if (i < str.length()) {
                        arg.append(str.charAt(i++));
                    }
                }
                else if (c == '\"') {
                    // Process quoted string
                    while (i < str.length()) {
                        c = str.charAt(i++);
                        if (c == '\"') {
                            break;
                        }
                        if (c == '\\') {
                            if (i < str.length()) {
                                arg.append(str.charAt(i++));
                            }
                        }
                        else {
                            arg.append(c);
                        }
                    }
                }
                else if (Character.isWhitespace(c)) {
                    break;
                }
                else {
                    arg.append(c);
                }
            }
            strings.add(arg.toString());
        }
        
        return strings;        
    }
    
    /**
     * Set background colour of a JEditorPane.
     * based on fix from: https://community.oracle.com/thread/1356459
     * @param JEditorPane     the pane to apply the background colour to
     * @param color           the colour to be applied to the panel.
     */
    public static void setJEditorPaneBackground(javax.swing.JEditorPane jEditorPane, Color color)
    {
        Color bgColor = new Color(250, 246, 229);
        UIDefaults defaults = new UIDefaults();
        defaults.put("EditorPane[Enabled].backgroundPainter", bgColor);
        jEditorPane.putClientProperty("Nimbus.Overrides", defaults);
        jEditorPane.putClientProperty("Nimbus.Overrides.InheritDefaults", true);
        jEditorPane.setBackground(bgColor);
    }
}
