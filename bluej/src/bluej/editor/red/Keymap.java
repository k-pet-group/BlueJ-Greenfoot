package bluej.editor.red;                	// This file forms part of the red package

import java.io.*;               // Object input, ouput streams
import java.awt.event.*;	// Key events

/**
 ** @version $Id: Keymap.java 36 1999-04-27 04:04:54Z mik $
 ** @author Michael Klling
 ** @author Giuseppe Speranza
 **
 ** A Keymap is an array of pointers to functions.  Each keypress will be
 ** mapped to an index in this array.  We have separate keymaps for each 
 ** valid modifier combination, so inside the keymap we don't care about 
 ** modifiers any more.
 ** 
 ** Key symbols (keysyms) are unknown numbers. They are here mapped onto an
 ** array of 101 entries (since we recognise 101 different keys).  Index
 ** 100 is special: it is bound to "SelfInsert" in all keymaps and cannot 
 ** be changed.
 ** 
 **	So the mapping takes place in two steps: first the keysym is mapped 
 **	onto an array index, then it is mapped onto a function associated 
 **	with that index.
 **
 **/

public final class Keymap extends UserFuncID
{
  // public constant variables
  public static final int NR_OF_KEYS = 101;
  public static final int FIRST_PRINTABLE = 37;
  public static final int LAST_PRINTABLE = 99;
  public static final int FIXED_PRINTABLE = 100;

  // private variables
  private String prefix;          	// This map's prefix for key strings
  private int binding[];		// binding information for every key
  private int search_idx;

/**
 * CONSTRUCTOR: Keymap(String)
 * create the keymap and initialise to invalid entries. 
 */

public Keymap (String name)
{
  prefix = name;	// the prefix for key strings in this keymap
  search_idx = -1;
  binding = new int[NR_OF_KEYS];
  binding[FIXED_PRINTABLE] = UFSelfInsert;
}

/**
 * FUNCTION: init()
 * Clear all key bindings (set all to NOT_BOUND, except the fixed
 * printable).
 */

public void init ()
{
  int i;

  for (i=0; i<NR_OF_KEYS; i++) {	// set all bindings to nothing
    binding[i] = NOT_BOUND;
  }
  binding[FIXED_PRINTABLE] = UFSelfInsert;
}

/**
 * FUNCTION: bind(int, int)
 * insert a binding from key to func. The function is defined by its
 * index in the user function table.
 */

public boolean bind (int key, int func)
{
  int index;

  index = sym_to_index(key);
  if (index == -1)
    return false;

  if (index == -1)
    return false;

  binding[index] = func;
  return true;
}

/**
 * insert a binding for all printable keys
 */

public void bind_printables (int func)
{
  int i;

  for (i=FIRST_PRINTABLE; i<=LAST_PRINTABLE; i++)
    binding[i] = func;
}

/**
 * map a key symbol to an associated function. If the key is unknown
 * UNKNOWN_KEY is returned, if it is undefined NULL is returned.
 */

public int map (int key)
{
  int index;

  index = sym_to_index (key);
  if (index == -1)
    return UNKNOWN_KEY;
  else
    return binding[index];
}

/**
 * FUNCTION: init_search()
 * Init a search for keys that map to a particular function
 */

public void init_search ()
{
  search_idx = -1;
}

/**
 * FUNCTION: search_key(int)
 * search_key: Search for a key for a function.  Return VK_UNDEFINED
 * if no key is found.
 */
int search_key (int func)
{
  search_idx++;

  while ((search_idx < NR_OF_KEYS) && (binding[search_idx] != func))
    search_idx++;

  if (search_idx == NR_OF_KEYS)
    return KeyEvent.VK_UNDEFINED;
  else
    return index_to_sym (search_idx);
}

/**
 * FUNCTION: key_string(int)
 * Convert a key index into its string representation.
 */
String key_string (int key)
{
  String str;
  String sym;

  sym = KeyEvent.getKeyText(key);
  str = prefix;
  str = str + sym;
  return str;
}

/**
 * FUNCTION: save_to_file(FileOutputStream)
 * Save this keymap to "file".  The file is already open.
 */

public void save_to_file (FileOutputStream file)
{
  int i;
  char ch;

  for (i=0; i<NR_OF_KEYS; i++) {
    ch = (char)binding[i];
    try {
      file.write (ch);
    }
    catch(Exception e) {
      System.err.println("ERROR: Could not write keymap to file");
    }
  }
}

/**
 * FUNCTION: read_from_file(FileInputStream, int)
 * Read the definition of this keymap from the open file "file"
 * If "convert" is true, the file is from the previous Red
 * version and a conversion has to be made.
 */

public void read_from_file (FileInputStream file, int convert_level)
{
  int i;
  char ch;
  int KEYS;

  if (convert_level > 0)
    KEYS = NR_OF_KEYS-1;
  else
    KEYS = NR_OF_KEYS;

  for (i=0; i<KEYS; i++) {
    try {
      ch = (char)file.read();
      binding[i] = ch;
      if (convert_level > 1) { 
        if (binding[i] > UFInsertFile)
          binding[i] = (binding[i] + 4);
      }
    }
    catch(Exception e) {
      System.err.println("ERROR: Could not read keymap from file");
    }
  }

  // temporarily, to convert from older format (before 1.1.6)
  binding[FIXED_PRINTABLE] = UFSelfInsert;  
}


/**
 * FUNCTION: int sym_to_index(int)
 * first (internal) mapping.  Map the key symbol to an index
 * in the bindings array.
 */
private int sym_to_index (int key)
{
  // for characters and numericals, key is index

  if (key >= KeyEvent.VK_0  &&  key <= KeyEvent.VK_9)	// index 48..57
    return (key);

  if (key >= KeyEvent.VK_A  &&  key <= KeyEvent.VK_Z)	// index 65..90
    return (key);

// now some individual keys

  switch (key) {

    // non-printables

    case KeyEvent.VK_BACK_SPACE:	return 0;
    case KeyEvent.VK_CANCEL:		return 1;
    case KeyEvent.VK_CLEAR:		return 2;
    case KeyEvent.VK_CONVERT:	   	return 3;
    case KeyEvent.VK_DELETE:	   	return 4;
    case KeyEvent.VK_DOWN:	   	return 5;
    case KeyEvent.VK_END:	   	return 6;
    case KeyEvent.VK_ENTER:	   	return 7;
    case KeyEvent.VK_ESCAPE:	   	return 8;
    case KeyEvent.VK_F:	   		return 9;
    case KeyEvent.VK_F1:	   	return 10;
    case KeyEvent.VK_F2:	   	return 11;
    case KeyEvent.VK_F3:	   	return 12;
    case KeyEvent.VK_F4:	   	return 13;
    case KeyEvent.VK_F5:	   	return 14;
    case KeyEvent.VK_F6:	   	return 15;
    case KeyEvent.VK_F7:	   	return 16;
    case KeyEvent.VK_F8:	   	return 17;
    case KeyEvent.VK_F9:	   	return 18;
    case KeyEvent.VK_F10:	   	return 19;
    case KeyEvent.VK_F11:	   	return 20;
    case KeyEvent.VK_F12:	   	return 21;
    case KeyEvent.VK_FINAL:	   	return 22;
    case KeyEvent.VK_HELP:	   	return 23;
    case KeyEvent.VK_HOME:	   	return 24;
    case KeyEvent.VK_INSERT:	   	return 25;
    case KeyEvent.VK_KANA:	   	return 26;
    case KeyEvent.VK_KANJI:	   	return 27;
    case KeyEvent.VK_LEFT:	   	return 28;
    case KeyEvent.VK_MODECHANGE:	return 29;
    case KeyEvent.VK_PAGE_DOWN:	   	return 30;
    case KeyEvent.VK_PAGE_UP:	   	return 31;
    case KeyEvent.VK_PAUSE:	   	return 32;
    case KeyEvent.VK_PRINTSCREEN:	return 33;
    case KeyEvent.VK_SEPARATER:	   	return 34;
    case KeyEvent.VK_TAB:	   	return 35;
    case KeyEvent.VK_UP:	   	return 36;

    // printables

    case KeyEvent.VK_ADD:		return 37;
    case KeyEvent.VK_BACK_QUOTE:	return 38;
    case KeyEvent.VK_BACK_SLASH:	return 39;
    case KeyEvent.VK_CLOSE_BRACKET:	return 40;
    case KeyEvent.VK_COMMA:	   	return 41;
    case KeyEvent.VK_DECIMAL:	   	return 42;
    case KeyEvent.VK_DIVIDE:	   	return 43;
    case KeyEvent.VK_EQUALS:	   	return 44;
    case KeyEvent.VK_MULTIPLY:	   	return 45;
    case KeyEvent.VK_NUMPAD0:	   	return 46;
    case KeyEvent.VK_NUMPAD1:	   	return 47;
					// 48..57 taken by numbers (above)
    case KeyEvent.VK_NUMPAD2:	   	return 58;
    case KeyEvent.VK_NUMPAD3:	   	return 59;
    case KeyEvent.VK_NUMPAD4:	   	return 60;
    case KeyEvent.VK_NUMPAD5:	   	return 61;
    case KeyEvent.VK_NUMPAD6:	   	return 62;
    case KeyEvent.VK_NUMPAD7:	   	return 63;
    case KeyEvent.VK_NUMPAD8:	   	return 64;
					// 65..90 taken by characters (above)
    case KeyEvent.VK_NUMPAD9:	   	return 91;
    case KeyEvent.VK_OPEN_BRACKET:	return 92;
    case KeyEvent.VK_PERIOD:	   	return 93;
    case KeyEvent.VK_QUOTE:	   	return 94;
    case KeyEvent.VK_RIGHT:	   	return 95;
    case KeyEvent.VK_SEMICOLON:	   	return 96;
    case KeyEvent.VK_SLASH:	   	return 97;
    case KeyEvent.VK_SPACE:	   	return 98;
    case KeyEvent.VK_SUBTRACT:	   	return 99;

//    case <special characters/umlaut>:	all return (FIXED_PRINTABLE);

    default:		 return -1;
  }
}

/**
 * Internal mapping.  Map the key index to an symbol.
 */

private int index_to_sym (int idx)
{

  // for numerals ans letters the index is the same as the symbol

  if (idx >= KeyEvent.VK_0  &&  idx <= KeyEvent.VK_9)	// index 48..57
    return (idx);

  if (idx >= KeyEvent.VK_A  &&  idx <= KeyEvent.VK_Z)	// index 65..90
    return (idx);

  // now some individual idxs

  switch (idx) {

    // non-printables

    case 0:	return KeyEvent.VK_BACK_SPACE;
    case 1:	return KeyEvent.VK_CANCEL;
    case 2:	return KeyEvent.VK_CLEAR;
    case 3:   	return KeyEvent.VK_CONVERT;
    case 4:   	return KeyEvent.VK_DELETE;
    case 5:   	return KeyEvent.VK_DOWN;
    case 6:   	return KeyEvent.VK_END;
    case 7:   	return KeyEvent.VK_ENTER;
    case 8:   	return KeyEvent.VK_ESCAPE;
    case 9:	return KeyEvent.VK_F;
    case 10:   	return KeyEvent.VK_F1;
    case 11:   	return KeyEvent.VK_F2;
    case 12:   	return KeyEvent.VK_F3;
    case 13:   	return KeyEvent.VK_F4;
    case 14:   	return KeyEvent.VK_F5;
    case 15:   	return KeyEvent.VK_F6;
    case 16:   	return KeyEvent.VK_F7;
    case 17:   	return KeyEvent.VK_F8;
    case 18:   	return KeyEvent.VK_F9;
    case 19:   	return KeyEvent.VK_F10;
    case 20:   	return KeyEvent.VK_F11;
    case 21:   	return KeyEvent.VK_F12;
    case 22:   	return KeyEvent.VK_FINAL;
    case 23:   	return KeyEvent.VK_HELP;
    case 24:   	return KeyEvent.VK_HOME;
    case 25:   	return KeyEvent.VK_INSERT;
    case 26:   	return KeyEvent.VK_KANA;
    case 27:	return KeyEvent.VK_KANJI;
    case 28:	return KeyEvent.VK_LEFT;
    case 29:	return KeyEvent.VK_MODECHANGE;
    case 30:	return KeyEvent.VK_PAGE_DOWN;
    case 31:	return KeyEvent.VK_PAGE_UP;
    case 32:	return KeyEvent.VK_PAUSE;
    case 33:	return KeyEvent.VK_PRINTSCREEN;
    case 34:	return KeyEvent.VK_SEPARATER;
    case 35:	return KeyEvent.VK_TAB;
    case 36:	return KeyEvent.VK_UP;

    // printables

    case 37:	return KeyEvent.VK_ADD;
    case 38:	return KeyEvent.VK_BACK_QUOTE;
    case 39:	return KeyEvent.VK_BACK_SLASH;
    case 40:	return KeyEvent.VK_CLOSE_BRACKET;
    case 41:	return KeyEvent.VK_COMMA;
    case 42:	return KeyEvent.VK_DECIMAL;
    case 43:	return KeyEvent.VK_DIVIDE;
    case 44:	return KeyEvent.VK_EQUALS;
    case 45:	return KeyEvent.VK_MULTIPLY;
    case 46:	return KeyEvent.VK_NUMPAD0;
    case 47:	return KeyEvent.VK_NUMPAD1;
					// 48..57 taken by numbers (above)
    case 58:	return KeyEvent.VK_NUMPAD2;
    case 59:	return KeyEvent.VK_NUMPAD3;
    case 60:	return KeyEvent.VK_NUMPAD4;
    case 61:	return KeyEvent.VK_NUMPAD5;
    case 62:	return KeyEvent.VK_NUMPAD6;
    case 63:	return KeyEvent.VK_NUMPAD7;
    case 64:	return KeyEvent.VK_NUMPAD8;
					// 65..90 taken by characters (above)
    case 91:	return KeyEvent.VK_NUMPAD9;
    case 92:	return KeyEvent.VK_OPEN_BRACKET;
    case 93:	return KeyEvent.VK_PERIOD;
    case 94:	return KeyEvent.VK_QUOTE;
    case 95:	return KeyEvent.VK_RIGHT;
    case 96:	return KeyEvent.VK_SEMICOLON;
    case 97:	return KeyEvent.VK_SLASH;
    case 98:	return KeyEvent.VK_SPACE;
    case 99:	return KeyEvent.VK_SUBTRACT;

//    case FIXED_PRINTABLE:	return <special characters/umlaut>
  }

  return KeyEvent.VK_ESCAPE;		// just to avoid a warning...
}

} // end class Keymap
