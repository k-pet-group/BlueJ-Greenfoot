package bluej.editor.red;		// This file forms part of the red package

import bluej.utility.Debug;

/**
 ** @version $Id: Line.java 36 1999-04-27 04:04:54Z mik $
 ** @author Michael Klling
 ** @author Giuseppe Speranza
 **
 ** Line is one line of text in the buffer. The text is stored as a block
 ** of characters and a length field. There is NO null byte at the end.
 ** The line has links to join in a doubly linked list and a mark field.
 **/

public final class Line 
{
  public Line next;			// Link to next line
  public Line prev;			// Link to previous line
  public char[] text;			// Line buffer
  public int tag;               // Line Tag
  public int length;			// Logical length of line
  private int bufsize;			// Buffer size(physical length)
  public int select;			// Indicates whether line is selected
  public int style;             // Insert line style
  public int style_start;       // Insert line style start
  public int style_end;         // Insert line style end

	public String toString()
	{
		return "Line(length=" + length + ")[text=" + text + "]";
	}

  // ------------------------------------------------------------------------
  /**
   **	Create new line with the initial text being a substring of str.
   **
   **	The initial text of the line is the substring of 'str' from
   **   'start' to the end of 'str'.
   **   'buffersize'==0 indicates the endmark (must never be used for normal 
   **	lines).
   */

  public Line (int buffersize, char[] str, int start)
  {
    bufsize = buffersize;
    length = 0;
    select = Buffer.NoSelection;
    if (buffersize > 0) 
    {
    	int newlength = 0;
    	
    	if (str != null)
    	    newlength = str.length - start;
    		
	// Make sure that the string length can take the whole string
	if (bufsize < newlength)
	    bufsize = newlength;

	text = new char [bufsize];
	if (str != null) 
	{
	    length = newlength;

	    if (length > 0)
		System.arraycopy (str, start, text, 0, length);
	}
    }
    else
	text = null;			// marks endmark
  }

  // ------------------------------------------------------------------------
  /**
   *	insert a character into the line (or at end of line). The
   *	character is inserted or appended.
   */

  public void insert (char ch, int index)
  {
    check_buffer (1);                     // ensure buffer is large enough

    if (index<length)                     // if not at end...
      System.arraycopy (text, index, text, index+1, length-index);
    text[index] = ch;
    length++;
  }

  // ------------------------------------------------------------------------
  /**
   *	Insert a string into the line (or at end of line). The
   *	string is inserted or appended.
   */

  public void insert_string (String str, int index)
  {
    StringBuffer buf;
    int len = str.length();

    check_buffer (len);                   // ensure buffer is large enough

    buf = new StringBuffer (0);
    buf.append (text, 0, index);
    buf.append (str);
    buf.append (text, index, length-index);
    length += len;
    if(length > 0)
        buf.getChars (0, length, text, 0);
  }

  // ------------------------------------------------------------------------
  /**
   *	remove one character in the line at index. 
   */

  public void remove (int index)
  {
    System.arraycopy (text, index+1, text, index, length-index-1);
    length--;
  }

  // ------------------------------------------------------------------------
  /**
   *	append the characters in 'chars' to this line.
   */

  public void append (char [] chars)
  {
    check_buffer (chars.length);	// ensure buffer is large enough
    System.arraycopy (chars, 0, text, length, chars.length);
    length += chars.length;
  }

  // ------------------------------------------------------------------------
  /*
  **	Append a line at the end of this one.
  **	This will make sure that the extaneous buffer at the end of a line
  **	is not copied into it too.
  */
//   public void append (Line ln)
//   {
//     check_buffer(ln.length);
//     System.arraycopy (ln.text, 0, text, length, ln.length);
//     length += ln.length;
//   }
	
  // ------------------------------------------------------------------------
  /**
   *	append a character to this line.
   */

  public void append_char (char ch)
  {
    check_buffer (1);			// ensure buffer is large enough
    text[length] = ch;
    length++;
  }

  // ------------------------------------------------------------------------
  /**
   *	Delete text from index to the end of the line. Index must be
   *	within valid range.
   */

  public void delete_tail (int index)
  {
    length = index;
  }

  // ------------------------------------------------------------------------
  /**
   *	Delete text from the beginning of the line to index. Index
   *	must be within valid range.
   */

  public void delete_head (int index)
  { 
    length -= index;
    System.arraycopy (text, index, text, 0, length);
  }

  // ------------------------------------------------------------------------
  /**
   *	Delete part of the line between idx1 and idx2. Both indices
   *	must be within the valid range.
   */

  public void delete_part (int idx1, int idx2)
  {
    System.arraycopy (text, idx2, text, idx1, length-idx2);
    length = length - (idx2-idx1);
  }

  // ------------------------------------------------------------------------
  /**
   *	mark a section of the current line as "styled"
   */

  public void set_style (int st, int start, int end)
  {
    style = st;
    style_start = start;
    style_end = end;
  }

  // ------------------------------------------------------------------------
  /**
   *	Make sure buffer is big enough to insert 'new_chars'
   *	characters.  If it is not, allocate a new (bigger) buffer.
   */

  private void check_buffer (int new_chars)
  {
    char[] oldtext;
    
    if (length+new_chars > bufsize)		// need larger buffer
    {
      if (new_chars>40)
	bufsize+=new_chars;
      else
	bufsize+=40;				// add at least 40 characters
      oldtext = text;
      text = new char [bufsize];
      if (length > 0)
	System.arraycopy (oldtext, 0, text, 0, length);
    }
  }

} // end class Line
