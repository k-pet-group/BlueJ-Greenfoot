package bluej.editor.red;            // This file forms part of the red package

import java.awt.*;	// For Frame
import bluej.utility.Debug;

/**
 ** @version $Id: Messages.java 36 1999-04-27 04:04:54Z mik $
 ** @author Michael Kolling
 ** @author Justin Tan
 **
 ** The class messages handles the display of all message dialog boxes.  These
 ** include error and help dialogs.  Help dialogs are non-modal dialogs
 ** consisting of a message and an OK button.  Error dialogs are modal dialogs
 ** consisting of a message, an OK and a Help button.  Every error message has
 ** an associated help message.
 ** 
 ** Messages are displayed by calling the show_help or show_error routine with
 ** the appropriate message number.  This class defines all the actual message
 ** texts and performs the transformation from number to text.  Every error
 ** message and its associated help message have the same number.  So all the
 ** help messages up to NR_ERROR are help messages for error dialogs.
 **/

public final class Messages
{
  private static int NR_OF_ERRORS = 0;

  // IDs for help messages and error messages shown in pop-up dialogs
  public static final int ErrSaveBindings = NR_OF_ERRORS++;
  public static final int ErrReadPrefs = NR_OF_ERRORS++;
  public static final int ErrReadKeys = NR_OF_ERRORS++;

  private static int NR_OF_HELP = NR_OF_ERRORS;	// one help message for each error

  public static final int HlpNYI = NR_OF_HELP++;
  public static final int HlpFileSelect = NR_OF_HELP++;
  public static final int HlpFind = NR_OF_HELP++;
  public static final int HlpPref = NR_OF_HELP++;
  public static final int HlpUserFunc = NR_OF_HELP++;
  public static final int HlpMenuUpdate = NR_OF_HELP++;
  public static final int HlpAboutRed  = NR_OF_HELP++;
  public static final int HlpCopyright = NR_OF_HELP++;
  public static final int HlpMouse = NR_OF_HELP++;
  public static final int HlpErrors = NR_OF_HELP++;
  public static final int HlpCantRedefine = NR_OF_HELP++;
  public static final int HlpChangingPref = NR_OF_HELP++;

  // IDs for question messages shown in pop-up dialogs

  private static int NR_OF_QUESTIONS = 0;

  public static final int QuSaveChanges = NR_OF_QUESTIONS++;
  public static final int QuRevert = NR_OF_QUESTIONS++;
  public static final int QuKeyDefaults = NR_OF_QUESTIONS++;
  public static final int QuDeleteFile = NR_OF_QUESTIONS++;
  public static final int QuDeleteDirectory = NR_OF_QUESTIONS++;
  public static final int QuDeleteOther = NR_OF_QUESTIONS++;

  // question dialog to get user response
  public static QuestionDialog questiondialog;

  // private variables
  private String error[];		// array with the error messages
  private String help[];		// array with the help messages
  private Question questions[];

/**
 * CONSTRUCTOR: Messages()
 * Initialise the messages
 */
public Messages()
{
  error = new String[NR_OF_ERRORS];
  help = new String[NR_OF_HELP];
  questions = new Question[NR_OF_QUESTIONS];

  // red errors

  error[ErrSaveBindings] = "Could not save key bindings!";
  help[ErrSaveBindings]  = 
    "Red tried to create a file named \".red-key-bindings\" in your home\n"+
    "directory.  This attempt failed.  The reason could be that a file\n"+
    "with that name already exists and can not be replaced (because it\n"+
    "is protected by its access rights) or that you do not have write\n"+
    "permission in your own home directory.  Check this and try again.\n\n"+
    "For now, your key bindings are in use, but they will be lost when \n"+
    "you exit Red.  If you can fix the problem before exiting Red, you\n"+
    "can try to save again by making another change to the key bindings.";

  error[ErrReadPrefs] = 
    "The Red preferences file in your home directory\n"+
    "is of a newer version of Red.  Ignoring it.\n\n"+
    "You will get default preferences.";
  help[ErrReadPrefs] = 
    "You can find the preferences file in your home directory under the name\n"+
    ".red-prefs.  The existing file was created by a version of Red newer\n"+
    "than the one you are currently using.  You can overwrite it with\n"+
    "preferences for this version by opening the preferences dialog (which\n"+
    "you can find in the \"Options\" menu), setting the preferences and\n"+
    "clicking \"OK\".";

  error[ErrReadKeys] = 
    "The Red key bindings file in your home directory is not a valid\n"+
    "key bindings file.  Ignoring it.  (You should delete that file.)\n\n"+
    "You will get default key bindings.";
  help[ErrReadKeys] =
    "You can find the key bindings file in your home directory under the name\n"+
    ".red-key_bindings.  This file is expected to have a certain format.  In your\n"+
    "home directory, a file with that name exists, but it does not have the \n"+
    "expected format.  It is possible that this is no key bindings file at all,\n"+
    "or that it comes from an old Red version and can not be converted.  In any\n"+
    "case, it can not be used.  Delete that file, or just set new key bindings in\n"+
    "Red - that will overwrite that file with a new, valid version.";

// END OF ERRORS

  help[HlpNYI] = 
    "This function is not yet implemented\n"+
    "Sorry...\n\n";

  help[HlpFileSelect] = 
    "This is the file selection box.  It is used to specify file names to\n"+
    "open a file or to save a file.\n\n"+
    "OPENING A FILE:\n\n"+
    "  To open a file, either type the file name into the ...\n\n"+
    "SAVING A FILE:\n\n"+
    "  To save a file, ...";

  help[HlpFind] = "This is the Find dialog.  This dialog is used to ...\n";

  help[HlpPref] =
    "The Preferences Dialog\n\n"+
    "The Preferences box lets you change some aspects of the behaviour of RED.\n"+
    "All settings apply to all edit windows.\n\n"+
    "Preferences are saved and remain the same in future sessions.\n\n"+
    "Show toolbar:  When on, the toolbar (list of buttons at the\n"+
    "               top of the edit window) is shown.\n\n"+
    "Beep with warnings:  When on, Red beeps every time it \n"+
    "               displays a warning.\n\n"+
    "Make backup when saving:  When on, Red will make a backup\n"+
    "               of the original version of a file before overwriting it.\n"+
    "               The backup file name consists of the original file\n"+
    "               name with a \".~\"-suffix.\n\n"+
    "Always end file with Newline character:  Some application expect all\n"+
    "               their input files to end with a \"NewLine\" character.\n"+
    "               (You get a \"NewLine\" character by typing \"Enter\" or\n"+
    "               \"Return\".)  If this option is on, Red will add a NewLine\n"+
    "               at the end of the file whan saving if it does not have one.\n\n"+
    "Automatically convert DOS files while loading:  When on, and a file is\n"+
    "               loaded that is found to be in DOS format (lines are ended \n"+
    "               with CR/LF rather than the Unix standard LF only), the CR\n"+
    "               is automatically stripped from the file.\n\n"+
    "Quote string: When you include a file into the current buffer (using the\n"+
    "               \"insert-file\" function from the File menu), you have the\n"+
    "               option of \"quoting\" that file.  If a file is quoted, the\n"+
    "               quote string defined here is used as a prefix to every line.";


  help[HlpUserFunc] = 
    "The User Function Dialog\n\n"+
    "The user function dialog is used to get information and set key bindings\n"+
    "for user functions.  Everything you can do with RED is done by calling a\n"+
    "user function.  The information you can get using this dialog is:\n"+
    "   - what functions are available\n"+
    "   - a short description of each function\n"+
    "   - keys bound to that function (calling that function if you press them)\n\n"+
    "Elements Of The Dialog\n\n"+
    "Categories:\n"+
    "   User functions are grouped into different categories.  This is a pop-up\n"+
    "   menu that lets you chose a category to display.  The functions of that\n"+
    "   category will be displayed in the function list.\n\n"+
    "Function List\n"+
    "   The function list displays the functions of the current category.  Click\n"+
    "   on a function to view information for that function.\n\n"+
    "Information Region\n"+
    "   The information region is the box at the bottom of the dialog window.\n"+
    "   It is used to display a brief description of the selected function.\n\n"+
    "Key Bindings\n"+
    "   The \"Key Bindings\" list displays the keys or key combinations that\n"+
    "   call (\"are bound to\") the selected function.\n\n"+
    "Add Key Button\n"+
    "   Use this button to add a key binding (that is: to make a key call the\n"+
    "   selected function).\n\n"+
    "Delete Key Button\n"+
    "   Use this function to delete a key binding.  Select the function first,\n"+
    "   then select the entry in the \"Key Binding\" list to be deleted, then \n"+
    "   click the button.\n\n"+
    "Close Button\n"+
    "   This button ends this dialog.  It is the default button (i.e. pressing\n"+
    "   \"Enter\" acts like a click into this button).\n\n"+
    "Defaults Button\n"+
    "   This button resets all key bindings to the standard bindings described\n"+
    "   in the Red User Manual.\n\n"+
    "Help Button\n"+
    "   Shows this text that you are currently reading.";

  help[HlpMenuUpdate] = 
    "Key bindings were changed for functions that appears\n"+
    "in a menu.  The key shortcuts shown in the menus will\n"+
    "be updated to show the correct bindings the next time\n"+
    "you restart Red.";

  help[HlpAboutRed] = "R  E  D\n\n"+
		"Version " + RedVersion.versionString() + "\n"+
		" \n"+
		"Red is a text editor originally developed for the\n"+
		"Blue programming environment.\n"+
		" \n"+
		"Written by:\n"+
		"    Michael Klling\n"+
		" \n"+
		"Ported to Java by:\n"+
		"    Guiseppe Speranza\n"+
		"    Michael Klling\n"+
		" \n"+
		"Feel free to send questions/comments to:\n"+
		"    michael.kolling@csse.monash.edu.au\n"+
		" \n"+
		"If you would like to get notified by email when new\n"+
		"versions of Red are released, send mail to the address\n"+
		"above with the subject \"Red notification request\"";

  help[HlpCopyright] =	
  "Copyright Notice\n"+
  "Copyright (c) 1998 Michael Klling. "+
  "All rights reserved.\n"+
  " \n"+
  "Permission to use, copy, modify, and distribute this software and its\n"+
  "documentation, without fee, and without written agreement is hereby\n"+
  "granted, with the following two restrictions:\n"+
  " \n"+
  "   a) this program, its source, or parts of its source may not be sold\n"+
  "       for profit or included in another program or package that is sold\n"+
  "       for profit\n"+
  "   b) this copyright notice must appear in full in all copies of this\n"+
  "       software\n"+
  " \n"+
  "IN NO EVENT SHALL THE AUTHOR OR MONASH UNIVERSITY \n"+
  "BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT, SPECIAL, \n"+
  "INCIDENTAL, OR CONSEQUENTIAL DAMAGES ARISING OUT OF THE \n"+
  "USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF THE \n"+
  "AUTHOR OR MONASH UNIVERSITY HAS BEEN ADVISED OF \n"+
  "THE POSSIBILITY OF SUCH DAMAGE.\n"+
  " \n"+
  "THE AUTHOR AND MONASH UNIVERSITY OF SPECIFICALLY \n"+
  "DISCLAIM ANY WARRANTIES, INCLUDING, BUT NOT LIMITED TO, \n"+
  "THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR \n"+
  "A PARTICULAR PURPOSE.  THE SOFTWARE PROVIDED HEREUNDER IS \n"+
  "ON AN \"AS IS\" BASIS, AND THE AUTHOR AND MONASH UNIVERSITY \n"+
  "HAVE NO OBLIGATION TO PROVIDE MAINTENANCE, \n"+
  "SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.";

  help[HlpMouse] =
    "Using the mouse in Red\n\n"+
    "Left button:\n"+
    "    click: set cursor\n"+
    "    drag:  select region of text\n"+
    "    double-click: select word\n"+
    "    triple-click: select line\n"+
    "    every additional click:\n"+
    "               (without shift): add another line to selection\n"+
    "               (with shift): add another word to selection\n\n"+
    "Middle button:\n"+
    "    click: Insert paste buffer at click position\n\n"+
    "Right button:\n"+
    "    click: Select text from cursor to click position or extend\n"+
    "            selection if it existed before";

  help[HlpErrors] = 
    "To report errors, please mail a description of the error\n"+
    "and the version number of Red to gsp@cs.monash.edu.au\n\n"+
    "(This is version " + RedVersion.versionString()+")\n\n";

  help[HlpCantRedefine] = 
    "You can not redefine the bindings of printable keys.\n"+
    "You can only redefine the binding for function keys, or\n"+
    "key combinations that include the Alt or Control key.";

  help[HlpChangingPref] = 
    "Your key bindings file is in a format of an older version\n"+
    "of Red.  This file will now be converted to the new format.";

// END OF HELP

  questions[QuSaveChanges] = new Question (
    "The text has been changed.\nSave changes?", 
    "Save", "Don't save", "Cancel");

  questions[QuRevert] = new Question (
    "Reload discards all changes since the last save.\nAre you sure?", 
    "Reload", null, "Cancel");

  questions[QuKeyDefaults] = new Question (
    "Setting the key bindings back to defaults\n" +
    "permanently deletes your personal key bindings.\n\n" +
    "Do you want to do that?",
    "Set Defaults", null, "Cancel");
}

/**
 * FUNCTION: show_help(Frame, int)
 * Display an help message in a dialog box.
 */

public void show_help(Frame frame, int msg)
{
  String message = new String(help[msg]);
  questiondialog = new QuestionDialog(frame, "Help", message, "OK");
  questiondialog.display();
}

/**
 * FUNCTION: show_error(Frame, int)
 * Display an error message in a dialog box.
 */

public void show_error(Frame frame, int msg)
{
  String message = new String(error[msg]);
  questiondialog = new QuestionDialog(frame, "Error", error[msg], "OK");
  questiondialog.display();
}

/**
 * FUNCTION: show_error_s(Frame, int, String)
 * Display an help message with a user defined string in a 
 * dialog box.
 */

public void show_error_s(Frame parent, int msg, String s)
{
  String message;

  message = make_string_message (error[msg], s);
  questiondialog = new QuestionDialog(parent, "Error", message, "OK");
  questiondialog.display();
}

/**
 * FUNCTION: show_question(Frame, int)
 * Display a question in a dialog box.
 */

public void show_question(Frame parent, int QId)
{
  questiondialog = new QuestionDialog(parent, "Question",
		    questions[QId].text, questions[QId].yes_label, 
		    questions[QId].no_label, questions[QId].cancel_label);
  questiondialog.display();
  Debug.message("questionDialog:getCommand()="+questiondialog.getCommand());
}

/**
 * FUNCTION: show_question_s(Frame, int, String)
 * Display a question in a dialog box.
 */

public void show_question_s(Frame parent, int QId, String s)
{
  String message;

  message = make_string_message (questions[QId].text, s);
  questiondialog = new QuestionDialog(parent, "Question",
		    message, questions[QId].yes_label, 
		    questions[QId].no_label, questions[QId].cancel_label);
  questiondialog.display();
}


/**
 * FUNCTION: make_string_message(String, String)
 * Create a string containing a message which is 
 * constructed from the message pattern 'pattern' and the string s.  
 * (A message pattern must contain a dollar sign ($), which is replaced
 * by 's'.)
 * The message returned must be deleted by the caller.
 */

private String make_string_message(String pattern, String  s)
{
  String message = new String();
  int ins_pos;

  ins_pos = pattern.indexOf('$');

  if(ins_pos>=0 && ins_pos<pattern.length()) {
    message = pattern.substring(0,ins_pos);
    message = message + s;
    ins_pos++;
    int end = pattern.length();
    message = message + pattern.substring(ins_pos, end);
  }
  return message;
}

}  // end class Messages
