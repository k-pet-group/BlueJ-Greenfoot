package bluej.editor.red;	// This file forms part of the red package

/**
 ** @version $Id: BufferPos.java 36 1999-04-27 04:04:54Z mik $
 ** @author Michael Klling
 ** @author Giuseppe Speranza
 ** This class is used to store a position within the buffer in the editor.
 ** All instance variables are public and read and written directly.
 **/

public final class BufferPos 
{
  public Line line;		// line of this position
  public int line_no;		// line number of this position
  public int column;         	// and the column (starting at 0)

  // ------------------------------------------------------------------------

  public String toString()
  {
    return "BufferPos(line_no=" + line_no + ",column=" + column + ")";
  }
	
  // ------------------------------------------------------------------------
  /**
   ** Create a new buffer position at indicated location.
   */

  public BufferPos (Line line, int line_no, int column)
  {
	  this.line = line;
	  this.line_no = line_no;
	  this.column = column;
  }

  // ------------------------------------------------------------------------
  /**
   ** Create a new buffer position as a copy of 'pos'.
   */

  public BufferPos (BufferPos pos)
  {
	  this(pos.line, pos.line_no, pos.column);
  }

  // ------------------------------------------------------------------------
  /**
   ** Set position to specified position.
   */

  public void set (Line ln, int n, int col)
  {
    line = ln;
    line_no = n;
    column = col;
  }

  // ------------------------------------------------------------------------
  /**
   ** Set position to same position as 'pos'.
   */

  public void set_to (BufferPos pos)
  {
    line = pos.line;
    line_no = pos.line_no;
    column = pos.column;
  }

  // ------------------------------------------------------------------------
  /**
   ** true if position has been set
   */

  public boolean is_invalid ()
  {
    return line==null;
  }

  // ------------------------------------------------------------------------
  /**
   ** Returns true, if this position is before position 'other' in
   ** the buffer.
   */

  public boolean is_before (BufferPos other)
  {
    return ((line_no < other.line_no) ||
	     ((line_no == other.line_no) && (column < other.column)));
  }

  // ------------------------------------------------------------------------
  /**
   ** Returns true, if this position is behind position 'other' in
   ** the buffer.
   */

  public boolean is_behind(BufferPos other)
  {
    return ((line_no > other.line_no) ||
	     ((line_no == other.line_no) && (column > other.column)));
  }

  // ------------------------------------------------------------------------
  /**
   ** equality test
   */

  public boolean equal(BufferPos other)
  { 
    return ((line==other.line) && (column==other.column)); 
  }

  // ------------------------------------------------------------------------
  /**
   ** true if pos is at end of line
   */

  public boolean at_eoln()
  { 
    return column==line.length; 
  }

} // end class BufferPos
