package bluej.editor.moe;

import bluej.utility.Debug;
import bluej.utility.Utility;

import java.util.Hashtable;

import java.awt.Event;
import java.awt.event.*;
import javax.swing.Action;
import javax.swing.AbstractAction;
import javax.swing.KeyStroke;
import javax.swing.JOptionPane;
import javax.swing.text.Keymap;
import javax.swing.text.*;
import javax.swing.undo.*;

/**
 ** A set of actions supported by the Moe editor. This is a singleton: the
 ** actions are shared between all editor instances.
 **
 ** @author Michael Kolling
 **
 **/

public final class MoeActions
{
    // -------- CONSTANTS --------

    private static int SHIFT_CTRL_MASK;

    //  spaces for entering half tabs
    private static final String spaces = "    ";

    // -------- INSTANCE VARIABLES --------

    private Hashtable actions;		// table of all known actions
    private Keymap keymap;		// the editor's keymap
    private FunctionDialog functionDlg;	// the function bindings dialog

    // undo helpers
    public UndoAction undoAction;
    public RedoAction redoAction;
    public UndoManager undoManager;

    // =========================== STATIC METHODS ===========================

    private static MoeActions moeActions;

    /**
     * Get the actions object (a singleton).
     */
    public static MoeActions getActions(JTextComponent textComponent)
    {
	if(moeActions == null)
	    moeActions = new MoeActions(textComponent);
	return moeActions;
    }

    // ========================== INSTANCE METHODS ==========================

    /**
     * Constructor. 
     */

    private MoeActions(JTextComponent textComponent)
    {
	SHIFT_CTRL_MASK = Event.CTRL_MASK;
	SHIFT_CTRL_MASK |= Event.SHIFT_MASK;
	undoManager = new UndoManager();
	keymap = textComponent.getKeymap();
	createActionTable(textComponent);
    }

    /**
     * Return an action with a given name.
     */
    public Action getActionByName(String name)
    {
	return (Action)(actions.get(name));
    }

    /**
     * Get a keystroke for an action. Return null is there is none.
     */
    public KeyStroke[] getKeyStrokesForAction(Action action)
    {
	KeyStroke[] keys = keymap.getKeyStrokesForAction(action);
	if (keys != null && keys.length > 0) 
	    return keys;
	else
	    return null;
    }

    /**
     * Get a keystroke for an action by action name. Return null is there
     * is none.
     */
    public KeyStroke[] getKeyStrokesForName(String actionName)
    {
	Action action = getActionByName(actionName);
	KeyStroke[] keys = keymap.getKeyStrokesForAction(action);
	if (keys != null && keys.length > 0) 
	    return keys;
	else
	    return null;
    }

    // ============================ USER ACTIONS =============================

    abstract class MoeAbstractAction extends TextAction {

	public MoeAbstractAction(String name, KeyStroke keyStroke) {
	    super(name);
	    if(keyStroke != null)
		keymap.addActionForKeyStroke(keyStroke, this);
	}

	protected MoeEditor getEditor(ActionEvent e) {
	    JTextComponent textComponent = getTextComponent(e);
	    return (MoeEditor)textComponent.getTopLevelAncestor();
	}
    }

    // === File: ===
    // --------------------------------------------------------------------

    class SaveAction extends MoeAbstractAction {

	public SaveAction() {
	    super("save", 
		  KeyStroke.getKeyStroke(KeyEvent.VK_S, Event.CTRL_MASK));
	}

	public void actionPerformed(ActionEvent e) {
	    getEditor(e).userSave();
	}
    }

    // --------------------------------------------------------------------

    /**
     * Reload has been chosen. Ask "Really?" and call "doReload" if the answer
     * is yes.
     */
    class ReloadAction extends MoeAbstractAction {

	public ReloadAction() {
	    super("reload", null);
	}

	public void actionPerformed(ActionEvent e) {
	    getEditor(e).reload();
	}
    }

    // --------------------------------------------------------------------

    class PrintAction extends MoeAbstractAction {

	public PrintAction() {
	    super("print", 
		  KeyStroke.getKeyStroke(KeyEvent.VK_P, Event.CTRL_MASK));
	}

	public void actionPerformed(ActionEvent e) {
	    getEditor(e).print();
	}
    }

    // --------------------------------------------------------------------

    class CloseAction extends MoeAbstractAction {

	public CloseAction() {
	    super("close", 
		  KeyStroke.getKeyStroke(KeyEvent.VK_W, Event.CTRL_MASK));
	}

	public void actionPerformed(ActionEvent e) {
	    getEditor(e).close();
	}
    }

    // === Edit: ===
    // --------------------------------------------------------------------

    class UndoAction extends MoeAbstractAction {

	public UndoAction() 
	{
	    super("undo", 
		  KeyStroke.getKeyStroke(KeyEvent.VK_Z, Event.CTRL_MASK));
	    this.setEnabled(false);
	}
    
	public void actionPerformed(ActionEvent e) 
	{
	    try {
		undoManager.undo();
	    }
	    catch (CannotUndoException ex) {
		Debug.message("moe: cannot undo...");
	    }
	    update();
	    redoAction.update();
	}

	public void update()
	{
	    if (undoManager.canUndo()) {
		this.setEnabled(true);
		putValue(Action.NAME, undoManager.getUndoPresentationName());
	    }
	    else {
		this.setEnabled(false);
		putValue(Action.NAME, "Undo");
	    }
	}
    }

    // --------------------------------------------------------------------

    class RedoAction extends MoeAbstractAction {

	public RedoAction() 
	{
	    super("redo", 
		  KeyStroke.getKeyStroke(KeyEvent.VK_Y, Event.CTRL_MASK));
	    this.setEnabled(false);
	}

	public void actionPerformed(ActionEvent e) 
	{
	    try {
		undoManager.redo();
	    }
	    catch (CannotRedoException ex) {
		Debug.message("moe: cannot redo...");
	    }
	    update();
	    undoAction.update();
	}

	public void update()
	{
	    if (undoManager.canRedo()) {
		this.setEnabled(true);
		putValue(Action.NAME, undoManager.getRedoPresentationName());
	    }
	    else {
		this.setEnabled(false);
		putValue(Action.NAME, "Redo");
	    }
	}
    }

    // --------------------------------------------------------------------

    class CommentAction extends MoeAbstractAction {

	public CommentAction() {
	    super("comment", 
		  KeyStroke.getKeyStroke(KeyEvent.VK_SLASH, Event.CTRL_MASK));
	}

	public void actionPerformed(ActionEvent e) {
	    Utility.NYI(getEditor(e));
	}
    }

    // --------------------------------------------------------------------

    class UncommentAction extends MoeAbstractAction {

	public UncommentAction() {
	    super("uncomment", 
		  KeyStroke.getKeyStroke(KeyEvent.VK_SLASH, SHIFT_CTRL_MASK));
	}

	public void actionPerformed(ActionEvent e) {
	    Utility.NYI(getEditor(e));
	}
    }

    // --------------------------------------------------------------------

    class InsertMethodAction extends MoeAbstractAction {

	public InsertMethodAction() {
	    super("insert-method", 
		  KeyStroke.getKeyStroke(KeyEvent.VK_M, Event.CTRL_MASK));
	}

	public void actionPerformed(ActionEvent e) {
	    JTextComponent textPane = getTextComponent(e);
	    int pos = textPane.getCaretPosition();
	    textPane.replaceSelection(
		"    /**\n" +
		"     * An example of a method - replace this comment with your own\n" +
		"     * \n" +
		"     * @param  y   a sample parameter for a method \n" + 
		"     * @return     the sum of x and y \n" +
		"     **/\n" + 
		"    public int sampleMethod(int y)\n" +
		"    {\n" +
		"        // put your code here\n" + 
		"        return x + y;\n" +
		"    }");
	    textPane.setCaretPosition(pos);
	}
    }

    // --------------------------------------------------------------------

    class HalfTabAction extends MoeAbstractAction {

	public HalfTabAction() {
	    super("half-tab",
		  KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0));
	}

	public void actionPerformed(ActionEvent e) {
	    JTextComponent textPane = getTextComponent(e);
	    Element line = getCurrentLine(textPane);
	    int lineStart = line.getStartOffset();
	    int cursorPos = textPane.getCaretPosition();
	    int numSpaces = 4 - ((cursorPos - lineStart) % 4);
	    textPane.replaceSelection(spaces.substring(0, numSpaces));
	}
    }

    // === Tools: ===

    // --------------------------------------------------------------------

    class FindAction extends MoeAbstractAction {
 
	public FindAction() {
	    super("find", 
		  KeyStroke.getKeyStroke(KeyEvent.VK_F, Event.CTRL_MASK));
	}

	public void actionPerformed(ActionEvent e) {
	    getEditor(e).find();
	}
    }

    // --------------------------------------------------------------------

    class FindBackwardAction extends MoeAbstractAction {

	public FindBackwardAction() {
	    super("find-backward", 
		  KeyStroke.getKeyStroke(KeyEvent.VK_F, SHIFT_CTRL_MASK));
	}

	public void actionPerformed(ActionEvent e) {
	    getEditor(e).findBackward();
	}
    }

    // --------------------------------------------------------------------

    class FindNextAction extends MoeAbstractAction {

	public FindNextAction() {
	    super("find-next", 
		  KeyStroke.getKeyStroke(KeyEvent.VK_G, Event.CTRL_MASK));
	}

	public void actionPerformed(ActionEvent e) {
	    getEditor(e).findNext();
	}
    }

    // --------------------------------------------------------------------

    class FindNextReverseAction extends MoeAbstractAction {
 
	public FindNextReverseAction() {
	    super("find-next-reverse", 
		  KeyStroke.getKeyStroke(KeyEvent.VK_G, SHIFT_CTRL_MASK));
	}

	public void actionPerformed(ActionEvent e) {
	    getEditor(e).findNextReverse();
	}
    }

    // --------------------------------------------------------------------

    class ReplaceAction extends MoeAbstractAction {

	public ReplaceAction() {
	    super("replace", 
		  KeyStroke.getKeyStroke(KeyEvent.VK_R, Event.CTRL_MASK));
	}

	public void actionPerformed(ActionEvent e) {
	    Utility.NYI(getEditor(e));
	}
    }

    // --------------------------------------------------------------------

    class GotoLineAction extends MoeAbstractAction {

	public GotoLineAction() {
	    super("goto-line", 
		  KeyStroke.getKeyStroke(KeyEvent.VK_L, Event.CTRL_MASK));
	}

	public void actionPerformed(ActionEvent e) {
	    JTextComponent textPane = getTextComponent(e);
	    Element line = getLine(textPane, 2);
	    textPane.setCaretPosition(line.getStartOffset());
	}
    }

    // --------------------------------------------------------------------

    class CompileAction extends MoeAbstractAction {

	public CompileAction() {
	    super("compile", 
		  KeyStroke.getKeyStroke(KeyEvent.VK_K, Event.CTRL_MASK));
	}

	public void actionPerformed(ActionEvent e) {
	    getEditor(e).compile();
	}
    }

    // === Debug: ===
    // --------------------------------------------------------------------

    class SetBreakPointAction extends MoeAbstractAction {

	public SetBreakPointAction() {
	    super("set-breakpoint", 
		  KeyStroke.getKeyStroke(KeyEvent.VK_B, Event.CTRL_MASK));
	}

	public void actionPerformed(ActionEvent e) {
	    getEditor(e).setBreakpoint();
	}
    }

    // --------------------------------------------------------------------

    class ClearBreakPointAction extends MoeAbstractAction {
 
	public ClearBreakPointAction() {
	    super("clear-breakpoint", 
		  KeyStroke.getKeyStroke(KeyEvent.VK_B, SHIFT_CTRL_MASK));
	}

	public void actionPerformed(ActionEvent e) {
	    getEditor(e).clearBreakpoint();
	}
    }

    // --------------------------------------------------------------------

    class StepAction extends MoeAbstractAction {

	public StepAction() {
	    super("step", null);
	}

	public void actionPerformed(ActionEvent e) {
	    Utility.NYI(getEditor(e));
	}
    }

    // --------------------------------------------------------------------

    class StepIntoAction extends MoeAbstractAction {
 
	public StepIntoAction() {
	    super("step-into", null);
	}

	public void actionPerformed(ActionEvent e) {
	    Utility.NYI(getEditor(e));
	}
    }

    // --------------------------------------------------------------------

    class ContinueAction extends MoeAbstractAction {

	public ContinueAction() {
	    super("continue", null);
	}

	public void actionPerformed(ActionEvent e) {
	    Utility.NYI(getEditor(e));
	}
    }

    // --------------------------------------------------------------------

    class TerminateAction extends MoeAbstractAction {

	public TerminateAction() {
	    super("terminate", null);
	}

	public void actionPerformed(ActionEvent e) {
	    Utility.NYI(getEditor(e));
	}
    }

    // === Options: ===
    // --------------------------------------------------------------------

    class PreferencesAction extends MoeAbstractAction {
 
	public PreferencesAction() {
	    super("preferences", null);
	}

	public void actionPerformed(ActionEvent e) {
	    Utility.NYI(getEditor(e));
	}
    }

    // --------------------------------------------------------------------

    class KeyBindingsAction extends MoeAbstractAction {
 
	public KeyBindingsAction() {
	    super("key-bindings", null);
	}

	public void actionPerformed(ActionEvent e) {
	    FunctionDialog dlg = getFunctionDialog();
	    dlg.setVisible(true);
	}
    }

    // === Help: ===
    // --------------------------------------------------------------------

    class AboutAction extends MoeAbstractAction {

	public AboutAction() {
	    super("help-about", null);
	}

	public void actionPerformed(ActionEvent e) {
	    JOptionPane.showMessageDialog(getEditor(e),
		new String[] { 
		    "Moe",
		    "",
		    "Version " + MoeEditor.versionString,
		    "",
		    "Moe is the editor of the BlueJ programming environment.",
		    "Written by Michael K\u00F6lling (mik@csse.monash.edu.au)."
		    },
		"About Moe", JOptionPane.INFORMATION_MESSAGE);
	}
    }

    // --------------------------------------------------------------------

    class CopyrightAction extends MoeAbstractAction {
 
	public CopyrightAction() {
	    super("help-copyright", null);
	}

	public void actionPerformed(ActionEvent e) {
	    Utility.NYI(getEditor(e));
	}
    }

    // --------------------------------------------------------------------

    class DescribeKeyAction extends MoeAbstractAction {

	public DescribeKeyAction() {
	    super("help-describe-key", 
		  KeyStroke.getKeyStroke(KeyEvent.VK_D, Event.CTRL_MASK));
	}

	public void actionPerformed(ActionEvent e) {
	    Utility.NYI(getEditor(e));
	}
    }

    // --------------------------------------------------------------------

    class HelpMouseAction extends MoeAbstractAction {

	public HelpMouseAction() {
	    super("help-mouse", null);
	}

	public void actionPerformed(ActionEvent e) {
	    Utility.NYI(getEditor(e));
	}
    }

    // --------------------------------------------------------------------

    class ShowManualAction extends MoeAbstractAction {

	public ShowManualAction() {
	    super("help-show-manual", null);
	}

	public void actionPerformed(ActionEvent e) {
	    Utility.NYI(getEditor(e));
	}
    }

    // --------------------------------------------------------------------

    class ReportErrorAction extends MoeAbstractAction {

	public ReportErrorAction() {
	    super("report-errors", null);
	}

	public void actionPerformed(ActionEvent e) {
	    Utility.NYI(getEditor(e));
	}
    }

    // --------------------------------------------------------------------

    class EmptyAction extends MoeAbstractAction {

	public EmptyAction() {
	    super("nothing", null);
	}

	public void actionPerformed(ActionEvent e) {
	    Utility.NYI(getEditor(e));
	}
    }

    // --------------------------------------------------------------------

    //     class Action extends MoeAbstractAction {
    //  
    //       public Action() {
    //  	 super("");
    //       }
    // 
    //       public void actionPerformed(ActionEvent e) {
    // 	  Utility.NYI(editor);
    //       }
    //     }


    // ========================= SUPPORT ROUTINES ==========================

    // --------------------------------------------------------------------
    /**
     * 
     */

    // --------------------------------------------------------------------
    /**
     * Return the dialog to see/edit user functions and bindings.
     */
    private FunctionDialog getFunctionDialog()
    {
	if(functionDlg == null)
	    functionDlg = new FunctionDialog(actions);
	return functionDlg;
    }

    // --------------------------------------------------------------------
    /**
     * Return the current line.
     */

    private Element getCurrentLine(JTextComponent text)
    {
	//return text.getDocument().getDefaultRootElement().getElement(
	//		text.getCaretPosition());
	DefaultStyledDocument doc = (DefaultStyledDocument)text.getDocument();
	return doc.getParagraphElement(text.getCaretPosition());
    }

    // --------------------------------------------------------------------
    /**
     * Find and return a line by line number
     */

    private Element getLine(JTextComponent text, int lineNo)
    {
      return text.getDocument().getDefaultRootElement().getElement(lineNo-1);
    }

//      // --------------------------------------------------------------------
//      /**
//       * Find and return a line by text position
//       */

//      private Element getLineAt (JTextComponent textComponent, int pos)
//      {
//  	return document.getParagraphElement(pos);
//      }

//      // --------------------------------------------------------------------
//      /**
//       * Return the number of the current line.
//       */

//      private int getCurrentLineNo(JTextComponent textComponent)
//      {
//  	return document.getDefaultRootElement().getElementIndex(
//  					textPane.getCaretPosition()) + 1;
//      }

//      // ------------------------------------------------------------------------
//      /**
//       * Return the number of the line containing position 'pos'.
//       */

//      private int getLineNumberAt(JTextComponent textComponent, int pos)
//      {
//  	return document.getDefaultRootElement().getElementIndex(pos) + 1;
//      }

    // --------------------------------------------------------------------
    /**
     * Create the table of action supported by this editor
     */

    private void createActionTable(JTextComponent textComponent)
    {
	actions = new Hashtable();

	// first, create our own actions

	undoAction = new UndoAction();
	redoAction = new RedoAction();

	Action[] myActions = {

	    // class actions
	    new SaveAction(),
	    new ReloadAction(),
	    new PrintAction(),
	    new CloseAction(),

	    // edit actions
	    undoAction,
	    redoAction,
	    new CommentAction(),
	    new UncommentAction(),
	    new InsertMethodAction(),
	    new HalfTabAction(),

	    // tool actions
	    new FindAction(),
	    new FindBackwardAction(),
	    new FindNextAction(),
	    new FindNextReverseAction(),
	    new ReplaceAction(),
	    new GotoLineAction(),
	    new CompileAction(),

	    // debug actions
	    new SetBreakPointAction(),
	    new ClearBreakPointAction(),
	    new StepAction(),
	    new StepIntoAction(),
	    new ContinueAction(),
	    new TerminateAction(),

	    // option actions
	    new PreferencesAction(),
	    new KeyBindingsAction(),

	    // help actions
	    new AboutAction(),
	    new CopyrightAction(),
	    new DescribeKeyAction(),
	    new HelpMouseAction(),
	    new ShowManualAction(),
	    new ReportErrorAction(),

	    // internal actions
	    new EmptyAction()
	};

	// now, get the actions already defined in the editor and merge them
	// with our own actions

	Action[] allActions = TextAction.augmentList(
					     textComponent.getActions(),
					     myActions);

	// next, enter all those actions into our hash table

	Action action;
	for (int i=0; i < allActions.length; i++) {
	    action = allActions[i];
	    actions.put(action.getValue(Action.NAME), action);
	}
    }

} // end class MoeActions
