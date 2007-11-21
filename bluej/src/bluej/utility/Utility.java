package bluej.utility;

import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Shape;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.border.Border;

import bluej.Config;

/**
 * Some generally useful utility methods available to all of bluej.
 *
 * @author  Michael Cahill
 * @author  Michael Kolling
 * @version $Id: Utility.java 5392 2007-11-21 19:23:48Z polle $
 */
public class Utility
{
    /**
     * Used to track which events have occurred for firstTimeThisRun()
     */
    private static Set occurredEvents = new HashSet();
    
    /**
     * Draw a thick rectangle - another of the things missing from the AWT
     */
    public static void drawThickRect(Graphics g, int x, int y, int width, int height, int thickness)
    {
        for(int i = 0; i < thickness; i++)
            g.drawRect(x + i, y + i, width - 2*i, height - 2*i);
    }

    /**
     * Draw stripes over a rectangle - yet another thing missing from the AWT
     */
    public static void stripeRect(Graphics g, int x, int y, int width, int height, int separation, int thickness)
    {
        for(int offset = 0; offset < width + height; offset += separation)
            for(int i = 0; i < thickness; i++, offset++) {
                int x1, y1, x2, y2;

                if(offset < height)
                    { x1 = x; y1 = y + offset; }
                else
                    { x1 = x + offset - height; y1 = y + height; }

                if(offset < width)
                    { x2 = x + offset; y2 = y; }
                else
                    { x2 = x + width; y2 = y + offset - width; }

                g.drawLine(x1, y1, x2, y2);
            }
    }

    /**
     * Draw a string at a given location on screen centered in a given rectangle.<br>
     * Left justifies the string if it is too long to fit all of the string inside the rectangle.
     */
    public static void drawCentredText(Graphics g, String str, int x, int y, int width, int height)
    {
        FontMetrics fm = g.getFontMetrics();

        Shape oldClip = g.getClip();
        g.clipRect(x, y, width, height);
        int xOffset = (width - fm.stringWidth(str)) / 2;        
        if(xOffset<0) {            
            xOffset=0;
        }
        int yOffset = (height + fm.getAscent()) / 2;
        g.drawString(str, x + xOffset, y + yOffset);
        g.setClip(oldClip);
    }

    /**
     * Draw a string at a given location on screen right-aligned in a given rectangle.
     */
    public static void drawRightText(Graphics g, String str, int x, int y, int width, int height)
    {
        FontMetrics fm = g.getFontMetrics();

        Shape oldClip = g.getClip();
        g.clipRect(x, y, width, height);
        g.drawString(str, x + width - fm.stringWidth(str),
                     y + (height + fm.getAscent()) / 2);
        g.setClip(oldClip);
    }

    /**
     * Splits "string" by "Delimiter"
     * @param str - the string to be split
     * @param delimiter - the field delimiter within str
     * @returns	an array of Strings
     */
    public static String[] split(String str, String delimiter)
    {
        List strings = new ArrayList();
        int	start = 0;
        int len = str.length();
        int dlen = delimiter.length();
        int offset = str.lastIndexOf(delimiter);		// First of all, find the
        // Last occurance of the Delimiter
        // Stop empty delimiters
        if (dlen < 1)
	    return null;
        else if(offset < 0)	 // one element
	    {
            String[] result = { str };
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
            offset = str.indexOf(delimiter,start);
            strings.add(str.substring(start,offset));

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
     * @param str - the string to be split
     * @returns	an array of Strings
     */
    public static String[] splitLines(String str)
    {
        return (str == null ? null : split(str, "\n"));
    }

    /**
     * Return a string in which all the quotable characters (tab, newline, ' and ", etc)
     * are quoted, Java-style. 
     */
    public static String quoteString(String src)
    {
        StringBuffer buf = new StringBuffer();

        for(int i = 0; i < src.length(); i++)
        {
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
                if(c == '\\' || c == '"' || c == '\'')
                    buf.append('\\');
                buf.append(src.charAt(i));
            }
        }

        return buf.toString();
    }

    /**
     * Translate a given, qualified class name into a URL where we believe
     * its documentation to be, and display that URL in a web browser.
     */
    public static void showClassDocumentation(String classname, String suffix)
    {
        classname = classname.replace('.', '/');
        String docURL = Config.getPropString("bluej.url.javaStdLib");
        if(docURL.endsWith(".html")) {
            int lastSlash = docURL.lastIndexOf('/');
            if(lastSlash != -1)
                docURL = docURL.substring(0, lastSlash+1);
        }
        //Debug.message(docURL + classname + ".html" + suffix);
        openWebBrowser(docURL + classname + ".html" + suffix);
    }

    /**
     * Let the given URL be shown in a browser window.
     * @param  url  the URL or file path to be shown.
     * @return true if the web browser could be started, false otherwise.
     */
    public static boolean openWebBrowser(String url) {

        if(Config.osname.startsWith("Windows")) {                 // Windows

            String cmd;
            // catering for stupid differences in Windows shells...
            if(Config.osname.startsWith("Windows 9") || Config.osname.equals("Windows Me"))    // win95/98/Me
                cmd = "command.com";
            else                                                        // other
                cmd = "cmd.exe";

            try {
                // more stupid Windows differences...
                if(Config.osname.startsWith("Windows 98") || Config.osname.equals("Windows Me")) {
                    Runtime.getRuntime().exec(
                         new String[] { cmd, "/c", "start", '"' + url + '"' });
                }
                else {
                    Runtime.getRuntime().exec(
                        new String[] { cmd, "/c", "start", "\"\"", '"' + url + '"' });
                }
            }
            catch(IOException e) {
                Debug.reportError("could not start web browser. exc: " + e);
                return false;
            }
        }
        else {                                                      // Mac, Unix and other
        
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
     * @param url the URL to be shown.
     * @return true if the web browser could be started, false otherwise.
     */
    public static boolean openWebBrowser(URL url) {

        if(Config.isMacOS()) {                           // Mac
            try {
                com.apple.eio.FileManager.openURL(url.toString());
            }
            catch(IOException e) {
                Debug.reportError("could not start web browser. exc: " + e);
                return false;
            }
        }
        else if(Config.osname.startsWith("Windows")) {                 // Windows

            return openWebBrowser(url.toString());
        }
        else {                                                      // Unix and other
        
            String cmd = mergeStrings(Config.getPropString("browserCmd1"), url.toString());

            try {
                Process p = Runtime.getRuntime().exec(cmd);

                // wait for exit code. 0 indicates success, otherwise
                // we try second command
                int exitCode = p.waitFor();

                cmd = Config.getPropString("browserCmd2");

                if(exitCode != 0 && cmd != null && cmd.length() > 0) {
                    cmd = mergeStrings(cmd, url.toString());
                    // Debug.message(cmd);
                    p = Runtime.getRuntime().exec(cmd);
                }
            }
            catch(InterruptedException e) {
                Debug.reportError("cannot start web browser: " + cmd);
                Debug.reportError("caught exc " + e);
                return false;
            }
            catch(IOException e) {
                Debug.reportError("could not start web browser.  exc: " + e);
                return false;
            }
        }
        return true;
    }

    /**
     * Let the given file be shown in a browser window.
     * @param file the file to be shown.
     * @return true if the web browser could be started, false otherwise.
     */
    public static boolean openWebBrowser(File file) {

        if(Config.osname.startsWith("Windows")) {                 // Windows

            return openWebBrowser(file.toString());
            
        }
        else {                                                      // Mac, Unix and other
        
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
     * Bring the current process to the front in the OS window stacking order.
     * Curently: only implemented for MacOS.
     */
    public static void bringToFront()
    {
        // This method implementation could be replaced by the following, once we use 1.5:
        
        //frame.setVisible(true); // necessary and idiomatically correct
        //frame.setAlwaysOnTop(true); // This brings it to the top immediately
        //frame.setAlwaysOnTop(false); // Oh no, I actually did not want

        if(Config.isMacOS()) {
            
            
            // The following code executes these calls:
            //    NSApplication app = NSApplication.sharedApplication();
            //    app.activateIgnoringOtherApps(true);
            // but does so by reflection so that this compiles on non-Apple machines.
            
            try {
                Class nsapp = null;
                try {
                    nsapp = Class.forName("com.apple.cocoa.application.NSApplication");
                }
                catch (ClassNotFoundException e) {}
                if (nsapp == null) {
                    // Using a custom class loader avoids having to set up the
                    // class path on the mac.
                    nsapp = Class.forName("com.apple.cocoa.application.NSApplication", true, new URLClassLoader(
                            new URL[]{new File("/System/Library/Java/").toURI().toURL()}));
                }
                java.lang.reflect.Method sharedApp = nsapp.getMethod("sharedApplication", null);
                Object obj = sharedApp.invoke(null, null);
                
                Class[] param = { boolean.class };
                java.lang.reflect.Method act = nsapp.getMethod("activateIgnoringOtherApps", param);
                Object[] args = { Boolean.TRUE };
                act.invoke(obj, args);
            }
            catch(Exception exc) {
                Debug.reportError("Bringing process to front failed (MacOS).");
            }
        }

//            alternative technique: using 'open command. works only for BlueJ.app, not for remote VM            
//            // first, find the path of BlueJ.app
//            String path = getClass().getResource("PkgMgrFrame.class").getPath();
//            int index = path.indexOf("BlueJ.app");
//            if(index != -1) {
//                path = path.substring(0, index+9);
//                // once we found it, call 'open' on it to bring it to front.
//                String[] openCmd = { "open", path };
//                Runtime.getRuntime().exec(openCmd);
//            }
            
        
        }

    /**
     * merge s2 into s1 at position of first '$'
     */
    public static String mergeStrings (String s1, String s2)
    {
        int pos = s1.indexOf('$');
        if(pos == -1)
            return s1;
        else
            return s1.substring(0,pos) + s2 + s1.substring(pos+1);
    }

    /**
     * merge strings in s2 into s1 at positions of '$'
     */
    public static String mergeStrings (String s1, String s2[]) {
	    for (int current = 0; current < s2.length; current++) {
		    s1 = mergeStrings(s1, s2[current]);
	    }

	    return s1;
    }

    /**
     * Converts tabs in a String into a specified number of spaces.  It assumes
     * that beginning of String is the starting point of tab offsets.
     *
     * @param original the String to convert
     * @param tabSize number of spaces to be inserted in place of tab
     * @return the String with spaces replacing tabs (if tabs present).
     */
    public static String convertTabsToSpaces(String originalString, int tabSize)
    {
        // if there are tab(s) in the String
        if(originalString.indexOf('\t') != -1) {
            StringBuffer buffer = new StringBuffer(originalString);
            for(int i = 0; i < buffer.length(); i++) {
                if(buffer.charAt(i) == '\t') {
                    buffer.deleteCharAt(i);
                    // calculate how many spaces to add
                    int numberOfSpaces = tabSize - (i % tabSize);
                    for(int j = 0; j < numberOfSpaces; j++)
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
     * @param context  Identifies the event (suggested: fully-qualified-class-name:event-id)
     * @return  true the first time the method was called with the given context; false
     *          every subsequent time.
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
     * @param context  Identifies the event (a property name)
     * @return  true the first time the method was called with the given context; false
     *          every subsequent time.
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
        Border oldBorder = button.getBorder();
        button.putClientProperty("JButton.buttonType", "textured");

        if (oldBorder == button.getBorder()) {
            // if the border didn't change the "textured" type probably doesn't
            // exist, which means we are running on MacOS < 10.5. This means we
            // should use the old pre-10.5 "toolbar" style instead.
            button.putClientProperty("JButton.buttonType", "toolbar");
        }
    }
    
}
