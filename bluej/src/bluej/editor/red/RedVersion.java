package bluej.editor.red;	// This file forms part of the red package
  
/**
 ** @version $Id: RedVersion.java 36 1999-04-27 04:04:54Z mik $
 ** @author Michael Kolling
 ** @author Giuseppe Speranza
 **/

public final class RedVersion
{
    private static final int Red_Version = 0;
    private static final int Red_Revision = 8;
    private static final int Red_UpdateLevel = 3;

    /**
     ** Return the version number.
     **/
    public static int number()
    {
	return Red_Version * 1000 + Red_Revision*100 + Red_UpdateLevel;
    }

    /**
     ** FUNCTION: versionString()
     ** Return version number as string.
     **/
    public static String versionString()
    {
	return new String(Red_Version + "." + Red_Revision + "." 
		   	  + Red_UpdateLevel + " (j)");
    }
} // end class RedVersion
