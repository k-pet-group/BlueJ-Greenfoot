package bluej.utility;

import java.awt.AWTException;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.Robot;
import java.awt.Shape;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;

import bluej.Config;

/**
 * Some generally useful utility methods available to all of bluej.
 * 
 * @author Michael Cahill
 * @author Michael Kolling
 * @version $Id: Utility.java 5685 2008-04-18 10:52:54Z polle $
 */
public class Utility
{
    /**
     * Used to track which events have occurred for firstTimeThisRun()
     */
    private static Set occurredEvents = new HashSet();

    /**
     * EventQueue that will intercept all mouse events until a mouse click is
     * recieved. After recieving the a mouse click it will disable itself.
     * 
     * @author Poul Henriksen
     */
    private static class InterceptEventQueue extends EventQueue
    {
        @Override
        public void dispatchEvent(java.awt.AWTEvent awtEvent)
        {
            System.out.println("AWT EVentQueue: " + awtEvent);
            if (awtEvent instanceof MouseEvent) {
                System.out.println("  interception: " + awtEvent);
                if (awtEvent.getID() == MouseEvent.MOUSE_CLICKED) {
                    // Stop intercepting event
                    System.out.println("Removing interceptQueue");
                    pop();
                }
                return;
            }
            // Dispatch as normal.
            super.dispatchEvent(awtEvent);
        }

        @Override
        public void pop()
        {
            try {
                super.pop();
            }
            catch (EmptyStackException e) {}
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
     * @param str -
     *            the string to be split
     * @param delimiter -
     *            the field delimiter within str
     * @returns an array of Strings
     */
    public static String[] split(String str, String delimiter)
    {
        List strings = new ArrayList();
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
     * @param str -
     *            the string to be split
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
     * @param url
     *            the URL or file path to be shown.
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
     * @param url
     *            the URL to be shown.
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
     * @param file
     *            the file to be shown.
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
     * Bring the current process to the front in the OS window stacking order.
     * Curently: only implemented for MacOS.
     */
    public static void bringToFront(final Window window)
    {
        // If already the active window, or not showing at all we return now.
        if (window.isActive() && window.isFocused() || !window.isShowing()) {
            System.out.println("Already in front: " + window + "  " + window.isActive() + "  " + window.isFocused());
            return;
        }
        boolean alwaysOnTopSupported = isAlwaysOnTopSupported(window);

        if (Config.isMacOS() && !Config.isJava16()) {
            // The following code executes these calls:
            // NSApplication app = NSApplication.sharedApplication();
            // app.activateIgnoringOtherApps(true);
            // but does so by reflection so that this compiles on non-Apple
            // machines.

            // Although there is a cross platform hack to do the same, we still
            // use the Mac specific code, because it is a less nasty hack.
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
                java.lang.reflect.Method sharedApp = nsapp.getMethod("sharedApplication", (Class[]) null);
                Object obj = sharedApp.invoke(null, (Object[]) null);

                Class[] param = {boolean.class};
                java.lang.reflect.Method act = nsapp.getMethod("activateIgnoringOtherApps", param);
                Object[] args = {Boolean.TRUE};
                act.invoke(obj, args);
            }
            catch (Exception exc) {
                Debug.reportError("Bringing process to front failed (MacOS).");
            }
        }
        else if (alwaysOnTopSupported) {
            // This should work cross platform, but is a very nasty hack, so we
            // only do it if alwaysOnTop is likely to be supported.
            SwingUtilities.invokeLater(new Thread() {
                public void run()
                {
                    if (!window.isVisible()) {
                        // necessary and idiomatically correct
                        window.setVisible(true);
                    }

                    // Bring the frame to the top (will not give it focus)
                    window.setAlwaysOnTop(true);

                    // Fake a click on the frame so that it gets the focus.
                    //
                    // This assumes that the frame is the top most frame, which
                    // is not necessarily the case if there are other
                    // alwaysOnTop windows - but it is a fair attempt.
                    Point windowLoc = window.getLocationOnScreen();
                    int x = (int) (windowLoc.getX() + window.getWidth() / 2);
                    int y = (int) (windowLoc.getY() + window.getHeight() / 2);
                    try {
                        simulateClick(x, y);
                    }
                    catch (Throwable e) {
                        Debug.reportError("Bringing process to front failed (cross platform).");
                    }
                    finally {
                        // Oh no, I actually did not want this
                        window.setAlwaysOnTop(false);
                    }
                }
            });
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
     * Performs a click at the given absolute coordinate.
     * 
     * @throws AWTException
     *             if something went wrong.
     */
    private static void simulateClick(int x, int y)
        throws AWTException
    {
        Robot r = new Robot();
        // Get the current location of the mouse pointer and
        // store it
        PointerInfo pointerInfo = MouseInfo.getPointerInfo();
        Point oldPoint = pointerInfo.getLocation();

        // Intercept mouse events with new event queue
        EventQueue eventQueue = java.awt.Toolkit.getDefaultToolkit().getSystemEventQueue();
        eventQueue.push(new InterceptEventQueue());

        // Move mouse cursor to the click location
        System.out.println("Clicking at: " + x + ", " + y);
        r.mouseMove(x, y);

        // Click
        r.mousePress(InputEvent.BUTTON1_MASK);
        r.mouseRelease(InputEvent.BUTTON1_MASK);

        // Move the mouse cursor back to the original location
        r.mouseMove((int) oldPoint.getX(), (int) oldPoint.getY());
    }

    /**
     * Try to determine if alwaysOnTop is supported for the window. If we can't
     * find out, we assume it is supported.
     * 
     */
    private static boolean isAlwaysOnTopSupported(final Window window)
    {
        // We assume that alwaysOnTop is supported since we can't find out on
        // Java 5
        boolean alwaysOnTopSupported = true;
        // If we are on Java 6 we can actually check if alwaysOnTop is
        // supported:
        if (Config.isJava16()) {
            // The following executes the Java 6 method
            // frame.isAlwaysOnTopSupported(). It does so by using reflection so
            // that it compiles on Java 5.
            Class<? extends Window> cls = window.getClass();

            try {
                Method m = cls.getMethod("isAlwaysOnTopSupported", (Class[]) null);
                Boolean result = (Boolean) m.invoke(window);
                alwaysOnTopSupported = result;
            }
            catch (Exception e) {
                Debug.reportError("Invoking alwaysOnTopSupported() failed in Utility.bringToFront().");
            }
        }
        return alwaysOnTopSupported;
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
     * @param original
     *            the String to convert
     * @param tabSize
     *            number of spaces to be inserted in place of tab
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
     * @param context
     *            Identifies the event (suggested:
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
     * @param context
     *            Identifies the event (a property name)
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
     * @param button
     *            The button that should be changed.
     */
    public static void changeToMacButton(AbstractButton button)
    {
        // available button styles, as of MacOS 10.5:
        // square, gradient, bevel, textured, roundRect, recessed, help
        // segmented styles:
        // segmented, segmentedRoundRect, segmentedCapsule, segmentedTextured
        // see: http://developer.apple.com/technotes/tn2007/tn2196.html

        Border oldBorder = button.getBorder();

        // the following works since MacOS 10.5

        button.putClientProperty("JButton.buttonType", "square");
        button.setMargin(new Insets(8, 8, 8, 8));
        // button.putClientProperty("JButton.segmentPosition", position);

        if (oldBorder == button.getBorder()) {
            // if the border didn't change the "textured" type probably doesn't
            // exist, which means we are running on MacOS < 10.5. This means we
            // should use the old pre-10.5 "toolbar" style instead.
            button.putClientProperty("JButton.buttonType", "toolbar");
        }
    }

}
