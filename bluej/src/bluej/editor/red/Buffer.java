package bluej.editor.red;	// This file forms part of the red package

import bluej.utility.Debug;
import java.io.*;	// File inout/output

/**
 ** @version $Id: Buffer.java 36 1999-04-27 04:04:54Z mik $
 ** @author Michael Klling
 ** @author Giuseppe Speranza
 ** 
 ** The buffer stores the text that is currently held in the editor.
 ** It is organised as a sequence of lines and two special positions:
 ** "point" and "mark".
 ** "Point" represents the "current position" (on screen represented by the cursor)
 ** and "mark" is a position that is remembered for later use (e.g. a position can be
 ** marked and point can later jump to it). It can also represent a selection:
 ** If the seletion is on, "mark" represents the beginning of the selection, "point"
 ** represents the end and all line within the selection are marked as selected.
 ** A variety of functions are provided to edit the contents of the buffer,
 ** retrieve its content and move point and mark.
 **/

public final class Buffer 
{
  // Used to determine selection status
  public static final int NoSelection = 0;   
  public static final int SelectionStart = 1;
  public static final int InSelection = 2; 
  public static final int SelectionEnd = 3;
  public static final int ContainsSelection = 4;

  // private variables
  private static final char NewLine = 10;	// the file newline character
  private static final int TAB_length = 8;
  private static final char TAB = 9;
  private static final char CR = 13;

  private Line endmark; 			// dummy line at end of buffer

  // NOTE: all public variables should be treated as read only!!
  public BufferPos step_line;   // point position of the Step Symbol
  public BufferPos pt;          // point position
  public BufferPos pt2;         // used when selection is on: the other end   
                          	// Note: when sel is on, pt2 is always at the
                          	// start, pt at the end of the selection  
  public BufferPos mark;   	// mark position
  public boolean selection_on;	// True if selection is currently on
  public int lines;		// total number of lines
        
/**
 ** Create a new, empty buffer and set Point to the first character
 ** position.  An empty buffer consists of two lines: an (empty) text line
 ** and an end-of-buffer mark (called endmark).  These are cicular, doubly
 ** linked. The filename is stored, but the file not yet loaded.
 */

public Buffer()
{
  pt = new BufferPos (new Line (20, null, 0), 1, 0);
  endmark = new Line (0, null, 0);
  pt.line.prev = pt.line.next = endmark;
  endmark.prev = endmark.next = pt.line;

  pt2 = new BufferPos (pt);
  mark = new BufferPos (null, -1, -1);	// invalid
  lines = 1;
  selection_on = false;
}

/**
 ** Insert a character at point position.  Remember, that the 
 ** column can be greater than the line length.  If so, insert at the end
 ** of the line.
 */

public void insert_char (char ch)
{
  pt.line.insert (ch, pt.column);
  pt.column++;
}

/**
 ** Insert a string into the current line of the buffer.  The
 ** string may not contain newlines.  The string is inserted at the 
 ** current point position.  Point is left behind the string.
 */

public void insert_string (String str)
{
    insert_string(str, RedEditor.StyleNormal);
}

public void insert_string (String str, int st)
{
  if (st != RedEditor.StyleNormal)
    pt.line.set_style (st, pt.column, pt.column + str.length());
    
  pt.line.insert_string (str, pt.column);
  pt.column = pt.column + str.length();
}

/**
 ** Insert a new line at point.  If point is in the middle of
 ** an existing line, that line is cut of and the tail is taken over as 
 ** the contents of the new one.
 */

public void insert_newline()
{
  Line ln;
  int newlength;

  newlength = pt.line.length - pt.column;
  
  ln = new Line (newlength+10, pt.line.text, pt.column);
  ln.length = newlength;

  pt.line.length = pt.column;

  pt.line.next.prev = ln;	// link new line into list
  ln.next = pt.line.next;
  pt.line.next = ln;
  ln.prev = pt.line;

  lines++;
  point_to_next_line();
  pt.column = 0;
}

/**
 ** Delete a character at point position.  This is also used to
 ** delete a line break: delete at the end of a line joins two lines.  The
 ** function result indicates what happened.  These are the return values
 ** and their meanings:
 ** NewLine    a line break has been deleted (lines are joined)
 ** '\0'  	point was at end of buffer. Nothing deleted.
 ** other	the character which has been deleted
 */

public char delete_char()
{
  char ch;

  if (pt.column == pt.line.length) {	// delete newline

    if (pt.line.next == endmark)	// at end of buffer
      return '\0';

    pt.line.append (pt.line.next.text);
    internal_delete_line (pt.line.next);
    return NewLine;
  }
  else {				// delete within line
    ch = pt.line.text[pt.column];
    pt.line.remove (pt.column);
    return ch;
  }
}

/**
 ** Cut the pt2 to pt. That is: remove the selected text from the buffer.
 ** No saving of the selection to the paste buffer takes place here. (That
 ** has to be done on some higher level...) If the selection is empty,
 ** nothing happens.  Leaves point at the spot where the selection has
 ** been.  Selection is off after this.  The selection does not have to
 ** be on to cut, but pt and pt2 must be set correctly.
 */

public void cut()
{
  Line ln;
  Line tmp;

  if ((mark.is_behind (pt2)) && (mark.is_before(pt)))	// mark gets cut!
    mark.set_to (pt2);

  if (pt2.line == pt.line) {
    pt2.line.delete_part (pt2.column, pt.column);
    pt.set_to (pt2);
  }
  else {
    pt2.line.delete_tail (pt2.column);		// cut off first line
    ln = pt2.line.next;
    while (ln != pt.line) {		// cut out middle lines
      tmp = ln;
      ln = ln.next;
      lines--;
    }
    pt2.line.next = pt.line;
    pt.line.prev = pt2.line;
    pt.line.delete_head (pt.column);		// cut beginning of last line
    pt.set_to (pt2);
    delete_char ();				// join first and last line
  }
  pt.line.select = NoSelection;			// in case it was on...
  selection_on = false;
}

/**
 ** Indent the cursor to the start of the next word in the next 
 ** nonblank line above.  If there in no word in the nonblank line above
 ** after the cursor position, return False.
 ** If 'new_line' is true then this is the indent done with the 
 ** newline-and-indent - those are hadled slightly different: if the above
 ** line is not indented, then we won't indent here either.
 */
boolean indent(boolean new_line) 
{

  Line ln;
  int ind, ptcol, col;

  ln = pt.line.prev;					// find previous line 
  while ((ln != endmark) && (ln.length == 0))	// (skipping blank lines)
    ln = ln.prev;

  if (ln == endmark)				// no previous line
    return false;

  if (new_line && (ln.text[0] != ' ') && (ln.text[0] != TAB))
    return true;

  ptcol = index_to_screencol (pt.line, pt.column);	// find screen column of pt

  ind = screencol_to_index (ln, ptcol);			// find index in prev line
  if (ind == -1)
    return false;					// line too short

  // find start of next word

  while ((ind < ln.length) && (ln.text[ind] != ' ') && (ln.text[ind] != TAB))
    ind++;
  while ((ind < ln.length) && ((ln.text[ind] == ' ') || (ln.text[ind] == TAB)))
    ind++;
  if (ind == ln.length)					// eoln reached
    if (((ln.text[0] != ' ') && (ln.text[0] != TAB))
         || ((ln.text[ind-1] != ' ') && (ln.text[ind-1] != TAB)))
      return false;

  col = index_to_screencol (ln, ind);			// find target screen col

  // Now col is the column we want to set pt to.  col>ptcol
  while ((pt.column > 0) && (pt.line.text[pt.column-1] == ' ')) {
    pt.column--;						  // delete spaces
    pt.line.remove (pt.column);
    ptcol--;
  }

  while ((ptcol / TAB_length + 1) * TAB_length <= col) {
    pt.line.insert (TAB, pt.column++);
    ptcol = (ptcol / TAB_length + 1) * TAB_length;
  }
  while (ptcol < col) {
    pt.line.insert (' ', pt.column++);
    ptcol++;
  }
  return true;
}

/**
 ** Insert spaces and/or TABs to next half-tab position.
 ** Returns the number of spaces inserted.  If the number (n) is:
 ** - positive:	n spaces have been inserted
 ** - negative: 	n spaces have been deleted, and a TAB inserted
 ** - = 0:	a TAB has been inserted
 */

public int insert_half_tab()
{
  int spaces;			// number of spaces inserted
  int removed_spaces;		// number of spaces removed
  int col;			// screen column of point

  col = index_to_screencol (pt.line, pt.column);	// find point screen col
  spaces = 4 - (col % 4);
  pt.line.insert_string ("    ".substring (0,spaces), pt.column);
  removed_spaces = spaces_to_tab (pt.column + spaces);
  pt.column = screencol_to_index (pt.line, col+spaces);
  return (spaces - removed_spaces);
}

/**
 ** Increment the indentation of the current line.  This is done by
 ** inserting a space at the position of the first non-blank character and,
 ** if appripriate, converting some spaces into tabs.
 */

public void inc_indent()
{
  int ind = 0;
  int col;		// screen column of cursor

  col = index_to_screencol (pt.line, pt.column);	// find point screen col

  while ((ind < pt.line.length) && 			// find non-blank char
	 ((pt.line.text[ind] == ' ') || (pt.line.text[ind] == TAB)))
    ind++;
  if (ind == pt.line.length)
    return;

  pt.line.insert (' ', ind++);
  spaces_to_tab (ind);
  pt.column = screencol_to_index (pt.line, col+1);	// find next column
}

/**
 ** Decrement indentation of current line.
 */

public void dec_indent() 
{
  int ind = 0;
  int col;		// screen column of cursor

  col = index_to_screencol (pt.line, pt.column);	// find point screen col

  while ((ind < pt.line.length) &&  			// find non-blank char
	 ((pt.line.text[ind] == ' ') || (pt.line.text[ind] == TAB)))
    ind++;
  if (ind == 0)						// already at left edge
    return;
  pt.column = ind;
  untabify ();						// untab if necessary

  pt.line.remove (--ind);
  pt.column = screencol_to_index (pt.line, col-1);	// find index of column
}

/**
 ** insert strings around current line
 */

public void comment (String start, String end) 
{
  pt.line.insert_string (start, 0);
  pt.column = pt.column+start.length();
  pt.line.append (end.toCharArray());
}

/**
 ** remove strings around current line.  First: check whether the
 ** start and end of the line really are the comment symbols.  If not,
 ** just return.  ('start' and 'end' are the comment strings.)
 */

public void uncomment(String start, String end) 
{
  int start_len = start.length();
  int end_len = end.length();
  String str = new String (pt.line.text);

  if ( ! str.startsWith (start) )
    return;
  if ( ! str.endsWith(end) )
    return;

  pt.line.delete_tail (pt.line.length - end_len);
  pt.line.delete_head (start_len);
  pt.column = pt.column-start_len;
  if (pt.column > pt.line.length)
    point_to_eol ();
}

/**
 ** If the character before point is a TAB, turn it into spaces.  If
 ** not, just return.
 */

public void untabify()
{
  int col, prev_col;
  char[] tabSpaces = "        ".toCharArray();

  if (pt.column == 0)
    return;
  if (pt.line.text[pt.column-1] != TAB)
    return;
  else {
    col = index_to_screencol (pt.line, pt.column);	// screen col after tab
    pt.column--;
    prev_col = index_to_screencol (pt.line, pt.column);	// screen col before tab
    pt.line.remove (pt.column);				// delete tab
    pt.line.insert_string ("        ".substring(0, col-prev_col), pt.column);
    pt.column = pt.column+col-prev_col;
  }
}

/**
 ** Move the point one character forward.  If the end of a line 
 ** is reached, move to beginning of next line.  
 */

public boolean point_forward ()
{
  return forward (pt);
}

/**
 ** Move the point one character backward.  If the beginning of
 ** a line is reached, move to end of next line.
 */

public boolean point_backward ()
{
  return backward (pt);
}

/**
 ** Move point one line up. 'col' is the screen column to aim at.
 ** We have to find out what logical column that is (because of tabs).
 ** Returns the screen column it was set to or -1 if it couldn't be moved
 ** up.
 */

public boolean point_up (int col)
{
  if (pt.line.prev == endmark)
    return false;

  point_to_prev_line ();
  point_to_screencol (col);
  return true;
}

/**
 ** Move point one line down. 'col' is the screen column to aim at.
 ** We have to find out what logical column that is (because of tabs).
 ** Returns true if it worked, False if it was in last line already.
 */

public boolean point_down (int col)
{
  if (pt.line.next == endmark)
    return false;

  point_to_next_line ();
  point_to_screencol (col);
  return true;
}

/**
 ** Set point to the specified location.
 */

public void point_to (BufferPos pos)
{
  pt.set_to (pos);
}

/**
 ** Set point to line number 'line', column 'col'.  If line number is
 ** greater than number of lines in buffer, set to last line. If it is
 ** less than one, go to first line.  Same with 'col'.
 */

public void goto_pos(int line, int col)
{
  point_to_bob ();
  while (--line > 0) {				// set line
    if (pt.line.next == endmark) break;
    point_to_next_line ();
  }

  if (col>0)					// set column
    if (col > pt.line.length)
      pt.column = pt.line.length;
    else
      pt.column = col;
}

/**
 ** Set point to beginning of buffer.
 */

public void point_to_bob()
{
  pt.set (endmark.next,1,0);
}

/**
 ** Set point to end of buffer.
 */

public void point_to_eob()
{
  pt.set (endmark.prev, lines, endmark.prev.length);
}

/**
 ** Set point to beginning of line.
 */

public void point_to_bol()
{
  pt.column = 0;
}

/**
 ** Set point to end of line.
 */

public void point_to_eol()
{
  pt.column = pt.line.length;
}

/**
 ** Set point one word forward.
 */

public void point_forward_word()
{
  boolean ok = true;

  while (ok && (!is_word_char (current_char ())))
    ok = forward (pt);

  while (ok && (is_word_char (current_char ())))
    ok = forward (pt);
}

/**
 ** Set point one word backward.
 */

public void point_backward_word()
{
  boolean ok;

  ok = backward (pt);

  while (ok && (!is_word_char (current_char ())))
    ok = backward (pt);

  while (ok && (is_word_char (current_char ())))
    ok = backward (pt);

  if (ok)
    forward (pt);
}

/**
 ** Set pos to beginning of buffer
 */

public void set_to_bob (BufferPos pos)
{
  pos.set (endmark.next, 1, 0);
}

/**
 ** Set pos to beginning of next line. The next line MUST be a valid 
 ** buffer line.
 */

public void next_line (BufferPos pos)
{
  pos.line = pos.line.next;
  pos.line_no++;
  pos.column = 0;
}

/**
 ** set line one line up. That line MUST be a valid buffer line.
 */

public void prev_line (BufferPos pos)
{
  pos.line = pos.line.prev;
  pos.line_no--;
  pos.column = 0;
}

/**
 ** find the pattern "pattern" in the buffer and set point to its end
 ** and pt2 to it's beginning.
 ** Return True. If the pattern is not found, return False and leave
 ** point unchanged.
 */

public boolean find (String patternStr, boolean case_sens,
		      boolean whole_word, boolean backwd, boolean wrap)
{
  BufferPos spos = new BufferPos (pt);	// search position
  BufferPos chpos;			// check position -- currently checked letter
  int i;
  boolean done = false;
  char[] pattern = patternStr.toCharArray();

  int length = pattern.length;

  if (backwd)			// step back
    if (!backward (spos))
      spos.set (endmark.prev, lines, endmark.prev.length);

  do {
      do {		// search the text for pattern
	  i = 0;
	  chpos = new BufferPos (spos);
	  while (char_equal(chpos, pattern[i], case_sens) && (i<length)
		 && forward (chpos))
	      i++;

	  if (i<length) {	// not found
	    if (backwd) {
	      if (!backward (spos))
	        if (wrap)
		  spos.set (endmark.prev, lines, endmark.prev.length);
		else
		  done = true;
	    }
	    else {	// (forwd)
	      if (!forward (spos))
	        if (wrap)
		  spos.set (endmark.next, 1, 0);
		else
		  done = true;
	    }
	    done = done || spos.equal(pt);
	  }
      } while ((i<length) && (!done));

    // if searching for whole word, check that neighbouring characters are no
    // word characters

      if ((i==length) && (whole_word)) {	// looking for word?
	backward (spos);
	if (is_word_char(char_at(spos)) || is_word_char(char_at(chpos))) {
	  done = false;
	  if (!backwd) {
	    forward (spos);
	    forward (spos);
	  }
	}
	else {			// word found!
	  forward (spos);	// set back to position where found
	  done = true;
	}
      }
      else			// not looking for word
	done = true;
  } while (!done);		// repeat if looking for whole word only

  if (i==length) {				// found
    pt.set_to (chpos);
    pt2.set_to (spos);
    return true;
  }
  else 
    return false;
}

/**
 ** True if line is after last line
 */

public boolean is_end (Line line)
{
  return line == endmark;
}

/**
 ** True if line is first line
 */

public boolean is_top (Line line)
{
  return line.prev == endmark;
}

/**
 ** True if line is last line
 */

public boolean is_last (Line line)
{
  return line.next == endmark;
}

/**
 ** Get point location.
 */

public BufferPos get_point ()
{
  return new BufferPos (pt);	// copy of pt
}

/**
 ** Get point 2 location.
 */

public BufferPos get_point2 ()
{
  return new BufferPos (pt2);	// copy of pt2
}

/**
 ** Return in the parameters the text of the current line
 ** from point to the end of the line.
 */

public LineFragment get_text_to_eol ()
{
  return new LineFragment (pt.line.text, pt.column, pt.line.length-pt.column);
}

/**
 ** Get text in 'line' starting from col.
 */

public LineFragment get_text_in_line (Line line, int col)
{  
  return new LineFragment (line.text, col, line.length - col);
}

/**
 ** Copy the current selection to a string
 ** The selection does not have to be turned on, only pt2
 ** and pt have to be set properly.
 ** PRE:  pt2 <= pt
 */

public StringBuffer selection_to_string ()
{
  StringBuffer str = new StringBuffer ();
  int len, pos;
  Line ln;

  if (pt2.line == pt.line) {			// selection in one line

    str.append (pt2.line.text, pt2.column, pt.column-pt2.column);
  }
  else {					// selection over several lines

    // copy first line

    str.append (pt2.line.text, pt2.column, pt2.line.length-pt2.column);
    str.append (NewLine);

    // copy middle lines

    ln = pt2.line.next;
    while (ln != pt.line) {
      str.append (ln.text);
      str.append (NewLine);
      ln = ln.next;
    }

    // copy last line

    str.append (pt.line.text, 0, pt.column);
  }
  return str;
}

//  set_tag: Set a tag (the little flag that is displayed in the editor window
//	to the left of the line) for the current line.

public void set_tag (int tag)
{
  pt.line.tag = tag;
}

// get tag of line
public int get_tag (Line line)
{
    return line.tag;
}

//  set_step_tag: Set the step tag.  The position is remembered - only one 
//	step tag can be set in a buffer.  It is an error to set the step tag
//	if it is set currently.  It can be removed with 'clear_step_tag'.

public void set_step_tag ()
{
  if (pt.line.tag == RedEditor.BreakLineTag)
    pt.line.tag = RedEditor.ExecBreakLineTag;
  else
    pt.line.tag = RedEditor.ExecLineTag;
  step_line.set (pt.line, pt.line_no, 0);
}

//  clear_step_tag: Clear the step tag. Return the line number of line where
//	it was (-1, if there was none).

public BufferPos clear_step_tag ()
{
  BufferPos res = step_line;

  if (step_line.is_invalid ()) 
    return res;

  if (step_line.line.tag == RedEditor.ExecBreakLineTag)
    step_line.line.tag = RedEditor.BreakLineTag;
  else
    step_line.line.tag = RedEditor.NoLineTag;
  step_line.set (null, -1, -1);
  return res;
}

//  clear_all_tags: Clear all line tags in the buffer

public void clear_all_tags ()
{
  Line line;

  line = endmark.next;		    	// first line
  while (line != endmark) {
    line.tag = RedEditor.NoLineTag;
    line = line.next;
  }
}

/*
//  set_style: mark a section of the current line as "styled"

void BUFFER::set_style (Style style, int start, int end)
{
  pt.line->set_style (style, start, end);
}
*/

/**
 ** FUNCTION: set_mark()
 ** Set the mark at the current point position.
 */

public void set_mark()
{
  mark.set_to (pt);
}

/**
 ** FUNCTION: boolean swap_point_mark()
 ** Exchange point and mark.
 */

public boolean swap_point_mark()
{
  BufferPos tmp;

  if (mark.is_invalid ())
    return false;

  tmp = mark;
  mark = pt;
  pt = tmp;
  return true;
}

/**
 ** FUNCTION: set_pt2()
 ** Set pt2 at the current point position.
 */

public void set_pt2()
{
  pt2.set_to (pt);
}

/**
 ** FUNCTION: boolean set_pt2_to_mark()
 ** Set pt2 to mark.  If mark is undefined, return false.
 */

public boolean set_pt2_to_mark()
{
  if (mark.is_invalid ())		// can't select if mark is not set
     return false;

  pt2.set_to (mark);
  return true;
}

/**
 ** FUNCTION: select_point_point()
 ** Select the text between point and pt2.  
 */

public void select_point_point()
{
  Line ln;
  BufferPos tmp;

  if (pt2.is_behind (pt)) {		// if point is first...
    tmp = pt2;				//  ...swap
    pt2 = pt;
    pt = tmp;
  }

  if (pt2.line == pt.line)
    pt2.line.select = ContainsSelection;	// line contain whole selection
  else {
    pt2.line.select = SelectionStart;		// selection starts here, ...
    ln = pt2.line.next;
    while (ln != pt.line) {
      ln.select = InSelection;			// ... contains these lines,...
      ln = ln.next;
    }
    pt.line.select = SelectionEnd;		// ... and ends here.
  }

  selection_on = true;
}

/**
 ** FUNCTION: unselect()
 ** If the selection is active at the moment, unselect it (that is:
 ** mark all the selected lines as unselected again).
 */

public void unselect()
{
  Line ln;

  ln = pt2.line;		// first line of selection
  while (ln != pt.line) {
    ln.select = NoSelection;
    ln = ln.next;
  }
  ln.select = NoSelection;

  selection_on = false;
}

/**
 ** FUNCTION: clear_buffer()
 ** Erase the whole buffer contents, leaving an empty buffer.
 */

public void clear_buffer()
{
  pt.line = endmark.next;
  pt.line.next = endmark;
  endmark.prev = pt.line;

  pt.line.length = 0;
  pt.line_no = 1;
  pt.column = 0;
  lines = 1;

  selection_on = false;
}

/**
 ** Insert the file into the buffer (at current point position). 'prefix'
 ** is added as a prefix to every line.
 */

public void insert_file (FileInputStream file, String prefix, 
			 boolean convert_dos)
{
  // start the first line

  if (prefix != null) {
    pt.line.insert_string (prefix, pt.column);
    pt.column = pt.column + prefix.length();
  }
  insert_newline ();
  point_backward ();
  load (file, false, prefix, convert_dos);
  point_to_eol ();
  delete_char ();
}

/**
 ** Load the contents of the associated file (defined on creation of the
 ** buffer) into the buffer.  If buffer was not empty, discard old 
 ** contents.
 */

public void load (FileInputStream file, boolean new_buffer, String prefix, 
		    boolean convert_dos)
{
  int input;
  char ch;
  Line ln;
  char[] prefixChars = null;
  
  // Debug.message("Started a load");
  
  if (prefix != null)
    prefixChars = prefix.toCharArray();

  if (new_buffer)
    clear_buffer ();

  try {
    while ((input = file.read()) != -1) {
     ch = (char)input;
     if (ch == NewLine) {
       if (convert_dos) {			// remove CR at end of line
	 if ((pt.line.length > 0) && (pt.line.text[pt.line.length-1] == CR))
	   pt.line.delete_tail (pt.line.length-1);
       }

       ln = new Line (40, null, 0);

       pt.line.next.prev = ln;			// link new line into list
       ln.next = pt.line.next;
       pt.line.next = ln;
       ln.prev = pt.line;

       pt.line = ln;
       pt.line_no++;
       lines++;
       if (prefix != null)				// insert line prefix
         pt.line.append (prefixChars);
     }
     else
       pt.line.append_char (ch);
    }
  } catch(Exception e) {
    e.printStackTrace();	
  }

  if (new_buffer)
    pt.set (endmark.next, 1, 0);	// set point to beginning of buffer
  
  // Debug.message("Finished load");
}

/**
 ** Save buffer to the file 'filename'. If 'append_newline' is true, make 
 ** sure that the very last character is a newline.
 */

public boolean save (String filename, boolean append_newline)
{
  int i;
  Line ln;
  boolean newline_at_end = false;
  FileOutputStream out;

  try {
    // open file "filename"
    out = new FileOutputStream(filename);

    ln = endmark.next;
    while (ln != endmark) {
      if (ln.length > 0) {
        for (i=0; i<ln.length; i++)
          out.write (ln.text[i]);
        newline_at_end = false;
      }
      ln = ln.next;
      if (ln != endmark) {
        out.write (NewLine);
        newline_at_end = true;
      }
    }

    if (append_newline && (!newline_at_end))
      out.write (NewLine);
  
    out.close();
    return true;
  }
  catch(IOException e) {
    return false;
  }
}

// internal routines (private)

/**
 ** Return character under point.
 */

private char current_char ()
{
  if (pt.column >= pt.line.length)
    return NewLine;
  else
    return pt.line.text[pt.column];
}

/**
 ** Return character at position p.
 */

private char char_at (BufferPos p)
{
  if (p.column >= p.line.length)
    return NewLine;
  else
    return p.line.text[p.column];
}

/**
 ** deletes internal line
 */

private void internal_delete_line(Line ln)
{
  ln.prev.next = ln.next;		// unlink line from list
  ln.next.prev = ln.prev;
  lines--;

  if (mark.line == ln)
    mark.set (null, 0, 0);		// set to invalid
}

/**
 ** Set point to next buffer line. Attention: column is 
 ** NOT updated!
 */

private void point_to_next_line()
{
  pt.line = pt.line.next;
  pt.line_no++;
}

/**
 ** Set point to next buffer line. Attention: column is 
 ** NOT updated!
 */

private void point_to_prev_line()
{
  pt.line = pt.line.prev;
  pt.line_no--;
}

/**
 ** Set point as close as possible to screencol and return
 ** the screen column actually used.
 */

private int point_to_screencol (int col)
{
  int screencol=0;

  pt.column=0;
  while ((screencol<col) && (current_char() != NewLine)) {
    if (current_char() == TAB)
      screencol = (screencol / TAB_length + 1) * TAB_length;
    else
      screencol++;
    pt.column++;
  }
  // Debug.message("point_to_screen:Start " + pt.toString());
  return screencol;
}

/**
 ** Move one position forward. Returns False if we were already at the end 
 ** of the buffer.
 */

private boolean forward (BufferPos p)
{
  if (p.column < p.line.length) {		// no problems, step forward
    p.column++;
    return true;
  }
  else  if (p.line.next == endmark)		// at end of line
    return false;
  else {
    p.line = p.line.next;
    p.column = 0;
    p.line_no++;
    return true;
  }
}

/**
 ** Move one position backward. Returns false if we were already at the 
 ** beginning of the buffer.
 */

private boolean backward (BufferPos p)
{
  if (p.column > 0) {			// no problems, step backward
    p.column--;
    return true;
  }
  else if (p.line.prev == endmark)	// at beginning of line
    return false;
  else {
    p.line = p.line.prev;
    p.line_no--;
    p.column = p.line.length;
    return true;
  }
}

/**
 ** If there are spaces in the current line before index that
 ** could be converted into a Tab, do so.  Return the number of spaces
 ** that were actually removed.
 */

private int spaces_to_tab(int index)
{
  int col;
  int cnt = 0;

  if (index == 0)
    return 0;

  col = index_to_screencol (pt.line, index);	// find screen col for index

  if (col % TAB_length != 0)
    return 0;
  if (pt.line.text[index-1] != ' ')
    return 0;

  while ((index > 0) && (pt.line.text[index-1] == ' ') && (cnt<TAB_length)) {
    pt.line.remove (--index);
    cnt++;
  }
  pt.line.insert (TAB, index);
  return cnt;
}

/**
 ** Find the screen column for a given index in a line
 */

private int index_to_screencol (Line ln, int index)
{
  int i, col = 0;
  
  for (i=0; i<index; i++)
    if (ln.text[i] == TAB)
      col = (col / TAB_length + 1) * TAB_length;
    else
      col++;

  return col;
}

/**
 ** Find the index in a line that is on or after a given
 ** screen column.  If the line does not reach that column, return -1.
 */

private int screencol_to_index (Line ln, int column)
{
  int i = 0, col = 0;

  while ((i < ln.length) && (col < column))
  {
      if (ln.text[i] == TAB)
	col = (col / TAB_length + 1) * TAB_length;
      else
	col++;
      i++;
  }

  if (col > column)	// if there was a TAB, stay before TAB
    i--;

  if (col < column)
    return -1;
  else
    return i;
}

/**
 ** True if ch is in word chars (letters, digits, underscore).
 */

private boolean is_word_char (char ch)
{ 
  return (Character.isLetterOrDigit (ch) || ch == '_');
}

/**
 ** Equality test for chars. If case_sens, it is the same as ==,
 ** if not case_sens, tests case insensitive.
 */

private boolean char_equal (BufferPos p, char ch, boolean case_sens)
{
  if (Character.isLetter(ch) && (!case_sens))
    return (Character.toLowerCase (char_at(p)) == (Character.toLowerCase (ch)));
  else
    return (char_at (p) == ch);
}

} // end class Buffer
