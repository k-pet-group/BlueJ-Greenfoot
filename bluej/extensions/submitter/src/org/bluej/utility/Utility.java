package org.bluej.utility;

import java.io.InputStream;
import java.io.OutputStream;

import java.lang.reflect.*;

import java.net.URL;
import java.net.URLEncoder;

import bluej.extensions.BlueJ;
/**
 * This is just a Utility class, functions here are quite general ones.
 */
 
public class Utility 
  {

  /**
   * CLoses a stream without trowing errors here and there
   */
  public static void inputStreamClose ( InputStream iStream )
    {
    try
      {
      if ( iStream != null ) iStream.close();
      }
    catch ( Exception exc )
      {
      // NOthing, really...  
      }
    }

  /**
   * CLoses a stream without trowing errors here and there
   */
  public static void outputStreamClose ( OutputStream oStream )
    {
    try
      {
      if ( oStream != null ) oStream.close();
      }
    catch ( Exception exc )
      {
      // NOthing, really...  
      }
    }


  /**
   * Convert a string into an int with a default and without complaining...
   */
  public static int convStringToInt ( String input, int defVal )
    {
    try 
      {
      Integer risul = Integer.decode(input);
      return risul.intValue();
      }
    catch ( Exception eee )
      {
      return defVal;
      }
    }


    /**
     * CONVENIENCE method, tries to center a window within a parent window.
     * @param child the window to be centered
     * @param parent the reference window

    public static void centreWindow(Window child, Window parent)
    {
        child.pack();

        Point p_topleft = parent.getLocationOnScreen();
        Dimension p_size = parent.getSize();
        Dimension d_size = child.getSize();

        Dimension screen = parent.getToolkit().getScreenSize(); // Avoid window going off the screen
        int x = p_topleft.x + (p_size.width - d_size.width) / 2;
        int y = p_topleft.y + (p_size.height - d_size.height) / 2;
        if (x + d_size.width > screen.width) x = screen.width - d_size.width;
        if (y + d_size.height > screen.height) y = screen.height - d_size.height;
        if (x < 0) x = 0;
        if (y < 0) y = 0;
        child.setLocation(x,y);
    }
     */

    /**
     * Let the given URL be shown in a browser window.
     * Adapted from the similar function in bluej.utility.Utility
     * @param url the URL to be shown.
     * @return true if the web browser could be started, false otherwise.
     */
    public static boolean openWebBrowser(BlueJ bluej, URL url) {
        String urlString = encodeURLSpaces(url.toString());
        String osname = System.getProperty("os.name", "");
        
        if(osname.startsWith("Mac")) {                           // Mac
            try {
                Class macClass;
                if(System.getProperty("java.vm.version").startsWith("1.3")) {
                    // MRJFileUtils.openURL(urlString);
                    macClass = Class.forName("MRJFileUtils");
                }
                else {
                    // com.apple.eio.FileManager.openURL(urlString);
                    macClass = Class.forName("com.apple.eio.FileManager");
                }
                Method m = macClass.getMethod("openURL", new Class [] { Class.forName("java.lang.String") });
                m.invoke(null, new String [] { urlString });
            }
            catch(Exception e) {
                return false;
            }
        }
        else if(osname.startsWith("Windows")) {                 // Windows

            String cmd;
            // catering for stupid differences in Windows shells...
            if(osname.startsWith("Windows 9") || osname.equals("Windows Me"))    // win95/98/Me
                cmd = "command.com";
            else                                                        // other
                cmd = "cmd.exe";

            try {
                // more stupid Windows differences...
                if(osname.startsWith("Windows 98")) {
                    Process p = Runtime.getRuntime().exec(
                         new String[] { cmd, "/c", "start", '"' + urlString + '"' });
                }
                else {
                    Process p = Runtime.getRuntime().exec(
                        new String[] { cmd, "/c", "start", "\"\"", '"' + urlString + '"' });
                }
            }
            catch(Exception e) {
                return false;
            }
        }
        else {                                                      // Unix and other
            String cmd = bluej.getBlueJPropertyString("browserCmd1", "");
            if(cmd == null || cmd.length() == 0) return false;
            cmd = mergeStrings(cmd, urlString);

            try {
                Process p = Runtime.getRuntime().exec(cmd);

                // wait for exit code. 0 indicates success, otherwise
                // we try second command
                if(p.waitFor() == 0) return true;

                cmd = bluej.getBlueJPropertyString("browserCmd2", "");
                if(cmd == null || cmd.length() == 0) return false;
                cmd = mergeStrings(cmd, urlString);

                p = Runtime.getRuntime().exec(cmd);
                // Don't wait for this one, as it's often blocking
            }
            catch(Exception e) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * merge s2 into s1 at position of first '$'
     * Copied from bluej.utility.Utility
     */
    private static String mergeStrings (String s1, String s2)
    {
        int pos = s1.indexOf('$');
        if(pos == -1)
            return s1;
        else
            return s1.substring(0,pos) + s2 + s1.substring(pos+1);
    }

    /**
     * Remove spaces in a URL - that is: replace each space with the
     * string "%20".
     * Copied from bluej.utility.Utility
     */
    private static String encodeURLSpaces(String url)
    {
        // if there are any spaces...
        if(url.indexOf(' ') != -1) {
            StringBuffer buffer = new StringBuffer(url);
            for(int i = 0; i < buffer.length(); i++) {
                if(buffer.charAt(i) == ' ') {
                    buffer.deleteCharAt(i);
                    buffer.insert(i, "%20");
                }
            }
            return buffer.toString();
        }
        else
            return url;
    }
    
  }
