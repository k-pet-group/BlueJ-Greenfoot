package bluej.compiler;

import bluej.utility.Debug;
import bluej.utility.Utility;

import java.io.IOException;
import java.io.PrintStream;
import java.io.OutputStream;

/**
 ** @version $Id: ErrorStream.java 81 1999-05-12 06:30:41Z ajp $
 ** @author Michael Cahill
 ** ErrorStream - OutputStream that parses javac output
 **/

public class ErrorStream extends PrintStream
{
    public ErrorStream()
    {
	// we do not actually intend to use an actual OutputStream from
	// within this class yet our superclass requires us to pass a
	// non-null OutputStream
	// we pass it the system error stream
        super(System.err);
    }
	
  /**
   ** Note: this class "cheats" by assuming that all output will be written by
   ** a call to println. It happens that this is true for the current version 
   ** of javac but this could change in the future.
   **
   ** We assume a certain error message format here:
   **   filename:line-number:message
   **
   ** We find the components by searching for the colons. Careful: MS Windows
   ** systems might have a colon in the file name (if it is an absolute path
   ** with a drive name included). In that case we have to ignore the first
   ** colon.
   **/
  public void println(String msg)
    {
      Debug.message("Compiler message: " + msg);
		
      int first_colon = msg.indexOf(':', 0);
      if(first_colon == -1) {
	// cannot read format of error message
	Utility.showError(null, "Compiler error:\n" + msg);
	return;
      }
      String filename = msg.substring(0, first_colon);

      	// Windows might have a colon after drive name. If so, ignore it
      if(! filename.endsWith(".java")) {
	first_colon = msg.indexOf(':', first_colon + 1);
	if(first_colon == -1) {
	    // cannot read format of error message
	    Utility.showError(null, "Compiler error:\n" + msg);
	    return;
	}
	filename = msg.substring(0, first_colon);
      }
      int second_colon = msg.indexOf(':', first_colon + 1);
      if(second_colon == -1) {
	// cannot read format of error message
	Utility.showError(null, "Compiler error:\n" + msg);
	return;
      }

      int lineNo = 0;
      try {
	lineNo = Integer.parseInt(msg.substring(first_colon + 1, second_colon));
      } catch(NumberFormatException e) {
	// ignore it
      }

      String error = msg.substring(second_colon + 1);

      throw new CompilerMessageError(filename, lineNo, error);
    }
}
