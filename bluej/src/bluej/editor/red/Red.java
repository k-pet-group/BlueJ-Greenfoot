package bluej.editor.red;

import java.io.*;		// Object input, ouput streams

/**
 ** @version $Id: Red.java 36 1999-04-27 04:04:54Z mik $
 ** @author Michael Kolling
 ** @author Giuseppe Speranza
 **
 ** Entry point to the stand alone version of Red
 **/
public class Red
{
  /**
   ** FUNCTION: main()
   **/
  static public void main(String[] args)
  {
    RedEditorManager red;
    boolean opened = false;

    if ((args.length >0) && (args[0].equals("-v")))
    {
	System.out.println("Red version " + RedVersion.versionString());
    }

    else if ((args.length >0 ) && 
	     (args[0].equals("-h") || args[0].equals("-help")))
    {
	System.out.println("Red: text editor");
	System.out.println("usage: java Red [ options ] [ file ... ]");
	System.out.println("options: ");
	System.out.println("  -v             ; print version number");
	System.out.println("  -h, -help      ; show this help");
    }

    else    // open editor(s)
    {
	red = new RedEditorManager(true);

	int i;
	for (i=0; i<args.length; i++)
	    if (args[i].startsWith("-"))    // unknown option
	    {
		System.out.println("red: option unknown '"+args[i]+"'");
		System.out.println("use '-h' option for help");
	    }
	    else {
		red.openText (args[i], null, null);
		opened = true;
	    }

	if (!opened)
	    red.openText (null, null, null);
    }
  }
} // end class Red
