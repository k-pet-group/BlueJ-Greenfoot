// Copyright (c) 2000 BlueJ Group, Monash University
//
// This software is made available under the terms of the "MIT License"
// A copy of this license is included with this source distribution
// in "license.txt" and is also available at:
// http://www.opensource.org/licenses/mit-license.html 
// Any queries should be directed to Michael Kolling mik@monash.edu.au

package bluej.editor.moe;

import java.io.*;		// Object input, ouput streams

/**
 ** @author Michael Kolling
 **
 ** Main class of the stand alone version of Moe
 **/

public class Moe
{

  static public void main(String[] args)
  {
    MoeEditorManager moe;
    boolean opened = false;

    if ((args.length >0) && (args[0].equals("-v")))
    {
	System.out.println("moe version " + "0.0");
    }

    else if ((args.length >0 ) && 
	     (args[0].equals("-h") || args[0].equals("-help")))
    {
	System.out.println("Moe: Michael's Other Editor");
	System.out.println("usage: java Moe [ options ] [ file ... ]");
	System.out.println("options: ");
	System.out.println("  -v             ; print version number");
	System.out.println("  -h, -help      ; show this help");
    }

    else    // open editor(s)
    {
	moe = new MoeEditorManager(true);

	int i;
	for (i=0; i<args.length; i++)
	    if (args[i].startsWith("-"))    // unknown option
	    {
		System.out.println("moe: option unknown '"+args[i]+"'");
		System.out.println("use '-h' option for help");
	    }
	    else {
		moe.openText (args[i], null, null);
		opened = true;
	    }

	if (!opened)
	    moe.openText (null, null, null);	// open empty edit window
    }
  }
} // end class Moe
