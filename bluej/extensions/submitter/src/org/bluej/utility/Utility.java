package org.bluej.utility;

import java.io.InputStream;
import java.io.OutputStream;

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











  }