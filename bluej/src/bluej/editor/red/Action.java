package bluej.editor.red;	// This file forms part of the red package

/**
 ** @version $Id: Action.java 36 1999-04-27 04:04:54Z mik $
 ** @author Michael Kolling
 ** @author Giuseppe Speranza
 ** @author Justin Tan
 ** This class implements the Editor actions
 **/
public final class Action
{
  // public constants

  // Action Types
  public static final int InsAction = 1;
  public static final int DelAction = 2;
  public static final int UndefAction = 3;

  // variables
  private int type;     	// shows whether this action was insertion or deletion
  private int sline, scol;	// start position of insertion or position of deletion
  private int eline, ecol;    	// end position of insertion (unused for deletion)
  private StringBuffer text;	// the text deleted (if more than one character)
  private char ch;		// the character deleted (if only one character)
                        	//  (both of these are unused for an insertion)
  private boolean linked;	// if true, the action is linked to the previous action
                        	//  (both will be undone in one step)
/**
 ** CONSTRUCTOR: Action()
 ** initialise variables
 */

public Action()
{
  type = UndefAction;
  text = null;
}

/**
 ** FUNCTION: set_insertion(int,int,int,int,boolean)
 ** Record an insertion in this action record.  "start" is the position
 ** where the inserted text starts, "end" is the end.
 */

public void set_insertion (int startl, int startc, int endl, int endc, 
			    boolean link)
{
  type = InsAction;
  sline = startl;
  scol = startc;
  eline = endl;
  ecol = endc;
  linked = link;
  text = null;
}

/**
 ** FUNCTION: set_deletion(int,int,String,char,boolean)
 ** Record a deletion in this action record.  "pos" is the position
 ** where the text was deleted.  If the deletion was only one character,
 ** "seg" is null and chr holds the character.  Otherwise "seg" holds
 ** a pointer to the segment holding the deleted text.
 */

public void set_deletion (int line, int col, StringBuffer seg, char chr, boolean link)
{
  type = DelAction;
  sline = line;
  scol = col;
  text = seg;
  ch = chr;
  linked = link;
}

/**
 ** FUNCTION: set_undef()
 ** Set this action to undefined.
 */

public void set_undef ()
{
  type = UndefAction;
  text = null;
}

/**
 ** FUNCTION: get_type()
 ** Return the type of this action (insertion or deletion).
 */

public int get_type ()
{
  return type;
}

/**
 ** Return the start line of this action.
 */

public int get_start_line ()
{
  return sline;
}

/**
 ** Return the end line of this action.
 */

public int get_end_line ()
{
  return eline;
}

/**
 ** Return the start column of this action.
 */

public int get_start_col ()
{
  return scol;
}

/**
 ** Return the end column of this action.
 */

public int get_end_col ()
{
  return ecol;
}

/**
 ** Return the text of this action (null if only one character).
 */

public StringBuffer get_text ()
{
  return text;
}

/**
 ** Return the text of this action (only valid if the text is a single
 ** character, i.e. text==null)
 */

public char get_character ()
{
  return ch;
}

/**
 ** FUNCTION: is_linked()
 ** Return true, if this action is linked to the previous action (which
 ** means that when this action is undone, the previous action should
 ** be undone also).
 */

public boolean is_linked ()
{ 
  return linked; 
}

} // end class Action
