package bluej.editor.red;           	// This file is part of the red editor

import bluej.utility.Debug;

import java.awt.*;		// MenuBar, MenuItem, Menu, Button, etc.
import java.awt.event.*;	// New Event model
import javax.swing.*;		// all the GUI components

/**
 ** @version $Id: Screen.java 36 1999-04-27 04:04:54Z mik $
 ** @author Michael Kolling
 **
 ** Screen is part of the editor implementation for the Blue programming
 ** system and the stand-alone version Red.
 **
 ** Screen is a kind of terminal emulation that manages I/O on a virtual
 ** screen and maps the operations onto a drawing area in an X Window.
 ** Operations are somewhat similar to simple text terminals.
 **/

public final class Screen extends JPanel
{
  // Constants
  private static final char[] Spaces = {' ',' ',' ',' ',' ',' ',' ',' '} ;
  private static final int TAB_length = 8;
  private static final char TAB = 9;
  private static final char EOL = 0;

  private static final char LINE = 0;	// for index in "lines_col"
  private static final int COL = 1;

  // Public variables
  // NOTE: all public variables should be considered read only!!
  public int lines, columns;	// number of text lines/columns on screen
  public int line, column;	// current cursor position on screen

  // Private variables 
  private RedEditor editor;
  private int left_offset;	// offset in points for first char in line
  private int right_offset;	// offset in points for last char in line
  private int cx, cy;		// current cursor position in pixels
  private int max_cx, max_cy;	// maximal cursor position in pixels
  private int screenwidth;	// screen width in pixels 
  private int screenheight;	// screen height in pixels

  private boolean initialised;	// True after everything is set up properly
  private boolean cursor_is_on;	// True if cursor is on
  private boolean cursor_visible;// True if cursor is displayed currently
  private boolean in_selection;	// True if displaying selected text
  private boolean tag_grey;	// True, if tag area is greyed

  private Graphics gc;
  private int fontwidth;
  private int fontascent;
  private int fontheight;

  // =========================== PUBLIC METHODS ===========================

  // --------------------------------------------------------------------
  /**
   ** Constructor. 
   **/

  public Screen(RedEditor editor, int tagWidth)
  {
    super(false);
    this.editor = editor;
    tag_grey = false;
    initialised = false;
    cursor_is_on = true;
    left_offset = tagWidth + 2;		// leave space for tag line 

    setBorder(BorderFactory.createLoweredBevelBorder());
    setOpaque(true);
  }

  // --------------------------------------------------------------------
  /**
   ** Initialise the screen. This does some calculations with the screen
   ** size, so it can only be done after the screen is sized properly.
   **/

  public void init()
  {
    if (!initialised) {
	setup_gc();
	right_offset = fontwidth;		// space needed for wrap mark
	initialised = true;
    }
    column = 0;
    line = 0;
    cx = left_offset;
    cy = 0;
    resize ();

    cursor_visible = false;		// True if on at this moment
    in_selection = false;
  }

  // --------------------------------------------------------------------
  /**
   ** Return the preferred size of the screen area - redefined
   **/

  public Dimension getPreferredSize()
  {
    return new Dimension(600, 450);  // ## NYI read size from properties!
  }


  // --------------------------------------------------------------------
  /**
   ** Return the minimum size of the screen area - redefined
   **/

  public Dimension getMinimumSize()
  {
    return new Dimension(200, 200);  // ## NYI read size from properties!
  }


  // --------------------------------------------------------------------
  /**
   ** The screen is being repainted (this is called by the AWT when the
   ** component needs repainting. Make sure the editor redraws the text.
   ** Use the graphics context passed in here (not the original). The new
   ** one is for an off screen buffer (double buffering) which is used for
   ** expose events. Restore the original gc afterwards.
   **/

  protected void paintComponent(Graphics g)
  {
    update (g);
  }

  // --------------------------------------------------------------------
  /**
   ** called to update the component on screen
   **/

  public void update(Graphics g)
  {
    if (!initialised)
      init();

    Graphics gcOriginal = gc;
//    gc = g;
    editor.expose();
    gc = gcOriginal;
  }

  // --------------------------------------------------------------------
  /**
   ** The screen (or part of it) was exposed. Called after an expose (of 
   ** course) and after resize. 
   **/

  public void expose()
  {
    if (initialised) {
      // remove cursor from screen (can't know whether it is still drawn...)
      gc.setColor(RedEditor.textBgColor);
      gc.fillRect(cx - 2, cy - 2, 3, fontheight+4);
      cursor_visible = false;

      // clear unused area UNDER last line
      gc.fillRect(0, lines*fontheight, screenwidth, 
		  screenheight - lines*fontheight);

      gc.setColor(RedEditor.textColor);
      gc.drawLine(left_offset-3, 0, left_offset-3, screenheight);
    }
  }

  // --------------------------------------------------------------------
  /**
   ** The screen has been resized. Recalculate lines, columns, etc.
   **/

  public void resize()
  {
    if (initialised) {
      Dimension d = getSize();
      screenwidth = d.width;
      screenheight = d.height;

      lines = Math.max(1, screenheight / fontheight);
      columns = Math.max(1, (screenwidth-left_offset-right_offset) / fontwidth);
      max_cx = left_offset + columns * fontwidth;
      max_cy = (lines-1) * fontheight;
    }
  }

  // --------------------------------------------------------------------
  /**
   ** move cursor to the beginnning of the next line. Scrolls screen
   ** up if cursor is in bottom line.
   **/

  public void next_line ()
  {
    if (cursor_visible) hide_cursor ();

    if (line < lines-1) {			// not at bottom...
	cx = left_offset;	column = 0;
	cy += fontheight;	line++;
    }
    else {
	gc.copyArea(0, fontheight, screenwidth, screenheight-fontheight, 0, 
		    -fontheight);
	cx = left_offset;	column = 0;
	clear_to_eol ();
    }
    if (cursor_is_on) show_cursor ();
  }

  // --------------------------------------------------------------------
  /**
   ** Position cursor on screen. x,y must be valid. Valid positions
   ** are 0..columns and 0..lines-1.
   **/

  public void cursor_to (int x, int y)
{
    hide_cursor ();

    column = x;
    line = y;
    cx = left_offset + column * fontwidth;
    cy = line * fontheight;

    show_cursor ();
  }

  // --------------------------------------------------------------------
  /**
   ** delete a character on screen.
   **/
  public void delete_char ()
  {
    hide_cursor();
    gc.copyArea(cx+fontwidth, cy, max_cx-cx-fontwidth, fontheight, 
		-fontwidth, 0);
    gc.setColor(RedEditor.textBgColor);
    gc.fillRect(max_cx-fontwidth, cy, fontwidth, fontheight); // clearRect
    show_cursor();
  }

  // --------------------------------------------------------------------
  /**
   ** Write a line at the current cursor position. The line can
   ** go over several lines. Returns the number of screen lines used and
   ** the last index within the text line that was written.
   **
   ** str: contains the text to be written
   ** index: current index in the current line (before writing)
   **
   ** returns result[0]: # lines written
   **			result[1]: index in current line (after writing)
   */

  public int[] write_string(LineFragment str, int index)
  {
    int[] lines_col = new int[2];

    int start_index;
    int screenlines = 1;
    int string_index = 0;
    int screencol = column;


    if (cursor_visible)
      hide_cursor ();

    while (string_index < str.length)	// go through the whole line...
    {
      if(screencol >= columns)		// at edge of screen?
      {
	set_wrap_mark ();
	if(cursor_at_bottom())
		break;	// end of screen?
	next_line ();
	clear_wrap_mark ();
	screencol = 0;
	screenlines++;
      }

      if(str.text[str.offset + string_index] == TAB)  // is next char TAB?
      {
	write_tab ();
	string_index++;
	screencol = column;
      }

      start_index = string_index;

      // find length of segment to write

      while((string_index < str.length)
	    && (str.text[str.offset + string_index] != TAB) 
	    && (screencol < columns))
      {
	string_index++;
	screencol++;
      }

      if(string_index > start_index)
	write_chars(str.text, str.offset+start_index, string_index-start_index);
    }

    if(cursor_is_on)
      show_cursor ();

    lines_col[LINE] = screenlines;
    lines_col[COL] = index + string_index;

    return lines_col;
  }

  // --------------------------------------------------------------------
  /**
   ** clears the screen. The cursor is positioned to 0,0.
   */

//   public void clear_screen ()
//   {
//     if (initialised) {
//      gc.setColor(RedEditor.textBgColor);
// 	gc.fillRect(0, 0, screenwidth, screenheight);		// clearRect
//      gc.setColor(RedEditor.textColor);
// 	gc.drawLine(left_offset-3, 0, left_offset-3, screenheight);
// 	cx = left_offset;	column = 0;
// 	cy = 0;			line = 0;
// 	cursor_visible = false;
// 	if (cursor_is_on) show_cursor ();
//     }
//   }

  // --------------------------------------------------------------------
  /**
   ** erases the characters from the cursor position to the end 
   ** of the line.
   */

  public void clear_to_eol ()
  {
    if (cursor_is_on) hide_cursor ();
    if (cx<max_cx) {
	if (in_selection)
	    gc.setColor(RedEditor.selectBgColor);
	else
	    gc.setColor(RedEditor.textBgColor);
	gc.fillRect(cx, cy, max_cx-cx, fontheight);
    }
    if (cursor_is_on) show_cursor ();
  }

  // --------------------------------------------------------------------
  /**
   ** Clear screen from the current line (including the whole
   ** current line) to end of screen. Leaves the cursor at the beginning 
   ** of the current line.
   */

  public void clear_to_eos ()
  {
    hide_cursor ();
    clear_to_eol ();
    gc.setColor(RedEditor.textBgColor);
    gc.fillRect(0, cy+fontheight, screenwidth, screenheight - (cy+fontheight));
    gc.setColor(RedEditor.textColor);
    gc.drawLine(left_offset-3, cy, left_offset-3, screenheight);

    column = 0;
    cx = left_offset;
    show_cursor ();
  }

  // --------------------------------------------------------------------
  /**
   ** Clear or display a tag to the left of a screen line.
   */
  public void set_tag (int tag)
  {
    Debug.message ("setting editor tag - NYI");
    if (!tag_grey)
      switch (tag) {

	case RedEditor.NoLineTag:
	    //utility->clear_area (window, 0, cy-1, 14, 14, False);
	    break;
	case RedEditor.ErrorLineTag:
	    //utility->copy_area ( utility->error_pix, window, gcNorm, 0, 0, 14, 14, 0, cy-1);
	    break;
	case RedEditor.BreakLineTag:
	    //utility->copy_area ( utility->stop_pix, window, gcNorm, 0, 0, 14, 14, 0, cy-1);
	    break;
	case RedEditor.ExecLineTag:
	    //utility->copy_area ( utility->exec_pix, window, gcNorm, 0, 0, 14, 14, 0, cy-1);
	    break;
	case RedEditor.ExecBreakLineTag:
	    //utility->copy_area ( utility->stop_pix, window, gcNorm, 0, 0, 14, 14, 0, cy-1);
	    //utility->set_gc_function ( gcNorm, GXand );
	    //utility->copy_area ( utility->exec_mask, window, gcNorm, 0, 0, 14, 14, 0, cy-1);
	    //utility->set_gc_function ( gcNorm, GXcopy);
	    //break;
      }
  }

  // --------------------------------------------------------------------
  /**
   ** Display an arrow at the end of the current screen line as
   ** a sign that this text line continues in the next screen line.
   */

  public void set_wrap_mark ()
  {
    // draw a rect to represent arrow
    gc.drawRect(max_cx, cy+2, 8, 9);
  }

  // --------------------------------------------------------------------
  /**
   ** clear the wrap mark in current line
   */

  public void clear_wrap_mark()
  {
    gc.setColor(RedEditor.textBgColor);
    gc.fillRect(max_cx, cy, 0, fontheight);
  }

  // --------------------------------------------------------------------
  /**
   ** Set the tag area to either grey (grey=true) or white (grey=false).
   */

  public void set_tag_grey (boolean grey)
  {
    tag_grey = grey;
    if (tag_grey)
      gc.setColor(RedEditor.frameBgColor);
    else
      gc.setColor(RedEditor.textBgColor);

    gc.fillRect(0, 0, left_offset-3, screenheight);
  }

  // --------------------------------------------------------------------
  /**
   ** show or hide the cursor and return the previous state.
   */

  public boolean cursor_on(boolean on)
  {
    boolean previous_state = cursor_is_on;

    hide_cursor ();
    cursor_is_on = on;
    if (cursor_is_on) show_cursor ();
    return previous_state;
  }

  // --------------------------------------------------------------------
  /**
   ** Set or unset mode for displaying the selection.
   */

  public void set_selection(boolean on)
  {
    in_selection = on;
  }

  // --------------------------------------------------------------------
  /**
   ** Toggle mode for displaying the selection.
   **/

  public void toggle_selection ()
  {
    in_selection = !in_selection;
  }

  // --------------------------------------------------------------------
  /**
   **   Set a style for display from now on.  Styles are called
   **	normal, bold, italics, bold-italics and colour-red.
   **	This routine is only used in the integrated version.  The 
   **	stand-alone version currently does not use styles.  (If this is 
   **	changed, something has to be done about the -font command line 
   **	option, since that currently only changes the main font.)
   **/

  public void set_style (int style) //###
  {
    switch (style) {
      case RedEditor.StyleNormal:
	gc.setColor(RedEditor.textColor);
	gc.setFont(RedEditor.editFont);
	break;
      case RedEditor.StyleBold:
	gc.setFont(RedEditor.boldFont);
	break;
      case RedEditor.StyleItalics:
	gc.setFont(RedEditor.italicFont);
	break;
      case RedEditor.StyleBoldItalics:
	gc.setFont(RedEditor.boldItalicFont);
	break;
      case RedEditor.StyleColourRed:
	gc.setColor(Color.red);
	break;
    }
  }

  // --------------------------------------------------------------------
  /**
   ** True if cursor in bottom screen line
   */

  public boolean cursor_at_bottom ()
  {
    return (boolean)(line==lines-1);
  }

  // --------------------------------------------------------------------
  /**
   ** True if the cursor is in the lower right corner of the 
   ** screen.
   */

  public boolean cursor_at_end ()
  {
    return (boolean)((line==lines-1) && (column==columns));
  }

  // --------------------------------------------------------------------
  /**
   ** Return the number of screen lines occupied by the printing
   ** of str,length starting at column startcol.
   */

  public int nr_of_lines (LineFragment str, int startcol)
  {
    int[] lines_col;

    lines_col = line_layout (str, startcol);
    return lines_col [LINE];
  }

  // --------------------------------------------------------------------
  /**
   ** Given in index in a string, this function returns the
   ** line and column offset of the position behind the string.	(If it is
   ** within one line, the line value is 0.)	'cursor'
   ** indicates whether this is used to determine a cursor or character
   ** position. The cursor may be behind the last column, characters 
   ** aren't. So if the position is behind the last column for a character
   ** it is set to the first column of the next line.
   */

  public int[] index_to_square (LineFragment str, boolean cursor)
  {
    int[] lines_col;

    lines_col = line_layout (str, 0);

    if (!cursor)
	if (lines_col [COL] == columns)
	    lines_col [COL] = 0;
	else
	    lines_col [LINE]--;
    else
	lines_col [LINE]--;

    return lines_col;
  }

  // --------------------------------------------------------------------
  /**
   ** Find an index in a buffer line that is on a given square
   ** on screen. Only the one line passed in is checked by doing the same
   ** calculations that would be done while printing.	'str' and 'length' 
   ** specify the line to test, starting at screen line 'line'.	If, while 
   ** testing, the position x,y is reached, the index in the line is 
   ** returned, otherwise -1 is returned.
   */

  public int[] square_to_index (LineFragment str, int line, int x, int y)
  {
    int screencol = 0;
    int index = str.offset;
    int str_end = str.offset + str.length;
    int[] result = new int[2];

    result[LINE] = line;
    while (index < str_end) {

	if ((line == y) && (screencol >= x)) {		// found!
	    result[LINE] = line;
	    result[COL] = index-str.offset;
	    return result;
	}

	if (screencol >= columns) {
	    screencol = 0;
	    line++;
	    if ((line == y) && (screencol >= x)) {		// found!
		result[LINE] = line;
		result[COL] = index-str.offset;
		return result;
	    }
	}

	if (str.text[index] == TAB)
		screencol = next_tab (screencol);
	else
		screencol++;
	index++;
    }
    result[LINE] = line;
    if (line == y)		// right line but index not reached, ret length
	result[COL] = str.length;
    else
	result[COL] = -1;		// haven't reached it yet
    return result;
  }

  // --------------------------------------------------------------------
  /**
   ** Transform point coordinates to character coordinates on
   ** screen. IN: x,y is the point to be transformed.	
   ** OUT: ln,col are set to the character square at point.
   **	   if col==-1 then the click was in the tag line
   */

  public int[] point_to_square (int x, int y)
  {
    int[] result = new int[2];
    int col, ln;

    ln = y / fontheight;

    if (ln >= lines) {
	result[LINE] = lines-1;
	result[COL] = columns;
	return result;
    }

    if (ln < 0) {
	result[LINE] = 0;
	result[COL] = 0;
	return result;
    }

    if (x < left_offset-2) {	// clicked in tag line
	result[LINE] = ln;
	result[COL] = -1;	// marks tag line
	return result;
    }

    col = (x+(fontwidth/2)-left_offset) / fontwidth;
    if (col > columns)
	col = columns;

    result[LINE] = ln;
    result[COL] = col;
    return result;
  }

  // ========================== PRIVATE METHODS ==========================

  // --------------------------------------------------------------------
  /**
   ** Show the cursor at the current cursor_position on screen.
   */

  private void show_cursor ()
  {
    if(initialised && cursor_is_on && !cursor_visible) {
	gc.setColor(RedEditor.cursorColor);
	gc.setXORMode(RedEditor.textBgColor);
	gc.fillRect(cx, cy-2, 2, fontheight + 2 );
	gc.setColor(RedEditor.textColor);
	gc.setPaintMode();
	cursor_visible = true;
    }
  }

  // --------------------------------------------------------------------
  /**
   ** Hide the cursor at the current cursor_position on screen.
   */

  private void hide_cursor ()
  {
    if(initialised && cursor_is_on && cursor_visible) {
	gc.setColor(RedEditor.cursorColor);
	gc.setXORMode(RedEditor.textBgColor);
	gc.fillRect(cx, cy-2, 2, fontheight + 2 );
	gc.setColor(RedEditor.textColor);
	gc.setPaintMode();
	cursor_visible = false;
    }
  }

  // --------------------------------------------------------------------
  /**
   ** write some chars at the current cursor position.
   ** It is assumed that cursor_to is called after this.
   */

  private void write_chars(char[] text, int offset, int length)
  {
    if (in_selection) {
	gc.setColor(RedEditor.selectBgColor);
	gc.fillRect(cx, cy, length * fontwidth, fontheight);
	gc.setColor(RedEditor.selectTextColor);
	gc.drawChars(text, offset, length, cx, cy+fontascent);
	gc.setColor(RedEditor.textColor);
    }
    else {
	gc.setColor(RedEditor.textBgColor);
	gc.fillRect(cx, cy, length * fontwidth, fontheight);
	gc.setColor(RedEditor.textColor);
	gc.drawChars(text, offset, length, cx, cy+fontascent);
    }
    cx += length * fontwidth;
    column += length;
  }

  // --------------------------------------------------------------------
  /**
   ** Write a TAB to screen.
   */

  private void write_tab ()
  {
    int nr_spaces;

    nr_spaces = next_tab(column) - column;
    if (column+nr_spaces < columns)
	write_chars(Spaces, 0, nr_spaces);
    else
	write_chars(Spaces, 0, columns-column);
  }

  // --------------------------------------------------------------------
  /**
   ** Return position of next TAB stop after col.
   */

  private int next_tab(int col)
  {
    int tab;

    tab = ((col / TAB_length + 1) * TAB_length);
    if (tab < columns)
	return tab;
    else
	return columns;
  }

  // --------------------------------------------------------------------
  /**
   ** Analyse a line to determine how it would be printed. Takes
   ** as arguments a LineFragment and a column on screen where to start 
   ** printing (startcol), and returns how many lines would be used to
   ** print the line (result[0]) and in which screen column the cursor ends
   ** up (result[1]).
   */

  private int[] line_layout (LineFragment str, int startcol)
  {
    int[] result = new int [2];
    int lines;
    int endcol;
    int index;

    index = str.offset;
    endcol = startcol;
    lines = 1;

    // count through str, looking for TABs and checking for end of screen

    while (index < str.length) {

	if (endcol >= columns) {		// at end of screen?
	    endcol = 0;
	    lines++;
	}

	if (str.text[index] == TAB)
	    endcol = next_tab (endcol);
	else
	    endcol++;
	index++;
    }

    result [LINE] = lines;
    result [COL] = endcol;

    return result;
  }

  // --------------------------------------------------------------------
  /**
   ** sets up the graphic components for this component
   */

  private void setup_gc()
  {
    FontMetrics fontmetrics = getFontMetrics(RedEditor.editFont);

    // Make sure that the specified fonts are valid 
    // ie; must be fixed width fonts and all be of the same width.

/*	int checkAll[] = fontmetrics.getWidths();
    int check = checkAll[0];
    boolean ok = true;
    for(int i=0; i<checkAll.length; i++) {
	    if(check != checkAll[i]) {
    ok = false;
	    }
    }
    if(!ok) {
	    System.err.println("Red: Error in font setting."+
		    " Font must be a fixed width font.");
    }
*/
    // get font attributes
    fontwidth = fontmetrics.charWidth('a');
    fontascent = fontmetrics.getAscent();
    fontheight = fontmetrics.getHeight();

    // get fg and bg colors and font
    setForeground(RedEditor.textColor);
    setBackground(RedEditor.textBgColor);
    setFont(RedEditor.editFont);

    gc = getGraphics();
    gc.setColor(RedEditor.textColor);
  }

} // end class Screen
