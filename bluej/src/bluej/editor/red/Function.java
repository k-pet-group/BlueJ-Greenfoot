package bluej.editor.red;	// This file forms part of the red package

import java.io.*;               // Object input, ouput streams

/**
 ** @version $Id: Function.java 36 1999-04-27 04:04:54Z mik $
 ** @author Michael Klling
 ** @author Giuseppe Speranza
 **/

public final class Function
{
	// public variables
	// READ ONLY!
	public String functionName;
	public int code;
	public String helpString;

	/**
	 ** Constructor
	 **/

	public Function (String name, int index, String help)
	{
		functionName = new String(name);
		code = index;
		helpString = new String(help);
	}
} // end class Function
