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














  }