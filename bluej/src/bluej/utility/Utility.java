/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
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

import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Shape;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractButton;
import javax.swing.border.Border;

import bluej.Config;

/**
 * Some generally useful utility methods available to all of bluej.
 * 
 * @author Michael Cahill
 * @author Michael Kolling
 * @version $Id: Utility.java 6489 2009-08-05 12:36:56Z polle $
 */
public class Utility
{
    /**
     * Used to track which events have occurred for firstTimeThisRun()
     */
    private static Set<String> occurredEvents = new HashSet<String>();

    private static URLClassLoader classLoader;
    static {
        try {
            classLoader = new URLClassLoader(new URL[]{new File("/System/Library/Java/").toURI().toURL()});
        }
        catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Draw a thick rectangle - another of the things missing from the AWT
     */
    public static void drawThickRect(Graphics g, int x, int y, int width, int height, int thickness)
    {
        for (int i = 0; i < thickness; i++)
            g.drawRect(x + i, y + i, width - 2 * i, height - 2 * i);
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
        int yOffset = (height + fm.getAscent()) / 2;
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
        if (Config.isMacOS()) {
            // Mac
            try {
                com.apple.eio.FileManager.openURL(url.toString());
            }
            catch (IOException e) {
                Debug.reportError("could not start web browser. exc: " + e);
                return false;
            }
        }
        else if (Config.isWinOS()) {
            // Windows
            return openWebBrowser(url.toString());
        }
        else {
            // Unix and other
            if (JavaUtils.getJavaUtils().openWebBrowser(url)) {
                return true;
            }

            String cmd = mergeStrings(Config.getPropString("browserCmd1"), url.toString());
            String cmd2 = mergeStrings(Config.getPropString("browserCmd2"), url.toString());

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
                    Debug.reportError("could not start web browser.  exc: " + e);
                    return false;
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
     */
    public static void bringToFront(final Window window)
    {
        // If not showing at all we return now.
        if (!window.isShowing()) {
            // System.out.println("Not bringing window to front: " + window + "
            // isFocusOwner: " + window.isFocusOwner() + " isShowing: " +
            // window.isShowing());
            return;
        }

        String pid = getProcessId();

        if (Config.isWinOS()) {
            // Use WSH (Windows Script Host) to execute a javascript that brings
            // a window to front.
            File libdir = calculateBluejLibDir();
            String[] command = {"cscript","\"" + libdir.getAbsolutePath() + "\\windowtofront.js\"",pid };
            
            StringBuffer commandAsStr = new StringBuffer();
            for (int i = 0; i < command.length; i++) {
                commandAsStr.append(command[i] + " ");
            }
         //   System.out.println("toFront executing command: " + commandAsStr);

            try {
                Process p = Runtime.getRuntime().exec(command);

                // Grab error output
                try {
                    BufferedReader br = new BufferedReader(new InputStreamReader(p.getErrorStream()));
                    StringBuffer extra = new StringBuffer();

                    char[] buf = new char[1024];
                    for (int i = 0; i < 5; i++) {
                        Thread.sleep(200);

                        // discontinue if no data available or stream closed
                        if (!br.ready())
                            break;
                        int len = br.read(buf);
                        if (len == -1)
                            break;

                        extra.append(buf, 0, len);
                    }
                    if (extra.length() != 0) {
                        Debug.message("When trying to launch cscript:" + extra);
                        Debug.message(" This error was recieved: " + commandAsStr);
                    }
                }
                catch (InterruptedException ie) {}

            }
            catch (IOException e) {}

        }
        if (Config.isLinux()) {
            // http://ubuntuforums.org/archive/index.php/t-197207.html
        }
        else if (Config.isMacOS() && !Config.isJava16()) {
            // The following code executes these calls:
            // NSApplication app = NSApplication.sharedApplication();
            // app.activateIgnoringOtherApps(true);
            // but does so by reflection so that this compiles on non-Apple
            // machines.

            try {
                Class<?> nsapp = null;
                try {
                    nsapp = Class.forName("com.apple.cocoa.application.NSApplication");
                }
                catch (ClassNotFoundException e) {}
                if (nsapp == null) {
                    // Using a custom class loader avoids having to set up the
                    // class path on the mac.
                    nsapp = Class.forName("com.apple.cocoa.application.NSApplication", true, classLoader);
                }
                java.lang.reflect.Method sharedApp = nsapp.getMethod("sharedApplication", (Class[]) null);
                Object obj = sharedApp.invoke(null, (Object[]) null);

                Class<?>[] param = {boolean.class};
                java.lang.reflect.Method act = nsapp.getMethod("activateIgnoringOtherApps", param);
                Object[] args = {Boolean.TRUE};
                act.invoke(obj, args);
            }
            catch (Exception exc) {
                Debug.reportError("Bringing process to front failed (MacOS): " + exc);
            }
        }
        else if (Config.isMacOS()) {
            // Use applescript to bring it to front.
            String command[] = {"osascript", "-e", "tell application \"System Events\"", "-e",
                    "set frontmost of first process whose unix id is " + pid + " to true", "-e", "end tell"};
            
            StringBuffer commandAsStr = new StringBuffer();
            for (int i = 0; i < command.length; i++) {
                commandAsStr.append(command[i] + " ");
            }
            
            //System.out.print("toFront executing command: " + commandAsStr);

            StringBuffer extra = new StringBuffer();
            try {
                Process p = Runtime.getRuntime().exec(command);
                
                BufferedReader br = null;
                // grab anything else
                try {
                    br = new BufferedReader(new InputStreamReader(p.getErrorStream()));

                    char[] buf = new char[1024];
                    for (int i = 0; i < 5; i++) {
                        Thread.sleep(200);

                        // discontinue if no data available or stream closed
                        if (!br.ready())
                            break;
                        int len = br.read(buf);
                        if (len == -1)
                            break;

                        extra.append(buf, 0, len);
                    }
                    if (extra.length() != 0) {
                        Debug.message("When trying to launch osascript:" + commandAsStr);
                        Debug.message(" This error output was recieved: " + extra);
                    }                    
                }
                catch (InterruptedException ie) {
                    Debug.message("When trying to launch osascript:" + commandAsStr);
                    Debug.message(" This error output was recieved: " + extra);
                    Debug.reportError(" And got InterruptedException: ", ie);
                }
                finally {
                    if(br != null) {
                        br.close();
                    }
                }
            }
            catch (IOException e) {
                Debug.message("When trying to launch osascript:" + commandAsStr);
                Debug.message(" This error output was recieved: " + extra);
                Debug.reportError(" And got IOException: ", e);
            }
        }        

        // alternative technique: using 'open command. works only for BlueJ.app,
        // not for remote VM
        // // first, find the path of BlueJ.app
        // String path = getClass().getResource("PkgMgrFrame.class").getPath();
        // int index = path.indexOf("BlueJ.app");
        // if(index != -1) {
        // path = path.substring(0, index+9);
        // // once we found it, call 'open' on it to bring it to front.
        // String[] openCmd = { "open", path };
        // Runtime.getRuntime().exec(openCmd);
        // }

    }

    /**
     * Get the process ID of this process.
     */
	public static String getProcessId() {
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
            // if the border didn't change the "textured" type probably doesn't
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
     * Determines whether the given key is a dead key.
     */
    public static boolean isDeadKey(KeyEvent event)
    {
        switch(event.getKeyCode()) {
            case KeyEvent.VK_DEAD_GRAVE:
            case KeyEvent.VK_DEAD_ACUTE:
            case KeyEvent.VK_DEAD_CIRCUMFLEX:
            case KeyEvent.VK_DEAD_TILDE:
            case KeyEvent.VK_DEAD_MACRON:
            case KeyEvent.VK_DEAD_BREVE:
            case KeyEvent.VK_DEAD_ABOVEDOT:
            case KeyEvent.VK_DEAD_DIAERESIS:
            case KeyEvent.VK_DEAD_ABOVERING:
            case KeyEvent.VK_DEAD_DOUBLEACUTE:
            case KeyEvent.VK_DEAD_CARON:
            case KeyEvent.VK_DEAD_CEDILLA:
            case KeyEvent.VK_DEAD_OGONEK:
            case KeyEvent.VK_DEAD_IOTA:
            case KeyEvent.VK_DEAD_VOICED_SOUND:
            case KeyEvent.VK_DEAD_SEMIVOICED_SOUND:
                return true;                
        }
        return false;
    }
}
