package bluej.editor.red;		// This file forms part of the red package

/**
 ** @version $Id: LineFragment.java 36 1999-04-27 04:04:54Z mik $
 ** @author Michael Kolling
 ** @author Justin Tan
 **
 **  This class represents a piece of text. The text is defined by a char array
 **  and an offset. The text logically contained in this segment is the text
 **  in the array from the offset to the end of the array.
 **
 **  All instance variables are public and read and written directly.
 **/

public final class LineFragment 
{
  public char[] text;		// the array holding the characters
  public int offset;		// an offset in the array
  public int length;		// length of the fragment

  /**
   **	Create a line fragment from a line, index and length. 
   **
   **	The fragment created is the part of the line from the index to the end
   **	of the line.
   */

  public LineFragment (char [] line, int index, int length)
  {
    text = line;
    offset = index;
    this.length = length;
  }

} // end class LineFragment
