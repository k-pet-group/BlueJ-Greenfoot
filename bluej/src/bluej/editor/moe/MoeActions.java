package bluej.editor.moe;

import bluej.utility.Debug;
import bluej.utility.Utility;
import bluej.utility.DialogManager;
import bluej.prefmgr.PrefMgrDialog;

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

import org.gjt.sp.jedit.syntax.*;

/**
** A set of actions supported by the Moe editor. This is a singleton: the
** actions are shared between all editor instances.
**
** Actions are stores both in a hashtable and in an array. The hashtable is
** used for fast lookup by name, whereas the array is needed to support
** complete, ordered access.
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

    private Action[] actionTable;	// table of all known actions
    private Hashtable actions;		// the same actions in a hashtable
    private String[] categories;
    private int[] categoryIndex;

    private Keymap keymap;		// the editor's keymap
    private FunctionDialog functionDlg;	// the function bindings dialog
    private KeyCatcher keyCatcher;

    // undo helpers
    public UndoAction undoAction;
    public RedoAction redoAction;
    public UndoManager undoManager;

    // frequently needed actions
    public Action compileAction;

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
        SHIFT_CTRL_MASK = Event.CTRL_MASK + Event.SHIFT_MASK;
        undoManager = new UndoManager();
        keymap = textComponent.getKeymap();
        createActionTable(textComponent);
        keyCatcher = new KeyCatcher();
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
    //      public KeyStroke[] getKeyStrokesForName(String actionName)
    //      {
    //  	Action action = getActionByName(actionName);
    //  	KeyStroke[] keys = keymap.getKeyStrokesForAction(action);
    //  	if (keys != null && keys.length > 0)
    //  	    return keys;
    //  	else
    //  	    return null;
    //      }
    // ============================ USER ACTIONS =============================

    abstract class MoeAbstractAction extends TextAction {

        public MoeAbstractAction(String name, KeyStroke keyStroke) {
            super(name);
            if(keyStroke != null)
                keymap.addActionForKeyStroke(keyStroke, this);
        }

        /* side effect: clears message in editor! */
        protected MoeEditor getEditor(ActionEvent e) {
            JTextComponent textComponent = getTextComponent(e);
            MoeEditor ed = (MoeEditor)textComponent.getTopLevelAncestor();
            ed.clearMessage();
            return ed;
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
                  KeyStroke.getKeyStroke(KeyEvent.VK_I, Event.CTRL_MASK));
        }

        public void actionPerformed(ActionEvent e) {

            getEditor(e);
            JTextComponent textPane = getTextComponent(e);
            Caret caret = textPane.getCaret();
            int selectionStart = caret.getMark();
            int selectionEnd = caret.getDot();
            if(selectionStart > selectionEnd) {
                int tmp = selectionStart;
                selectionStart = selectionEnd;
                selectionEnd = tmp;
            }
            if(selectionStart != selectionEnd)
                selectionEnd = selectionEnd - 1;    // skip last position

            MoeSyntaxDocument doc = (MoeSyntaxDocument)textPane.getDocument();
            Element text = doc.getDefaultRootElement();

            int firstLineIndex = text.getElementIndex(selectionStart);
            int lastLineIndex = text.getElementIndex(selectionEnd);
            for(int i = firstLineIndex; i <= lastLineIndex; i++) {
                Element line = text.getElement(i);
                int lineStart = line.getStartOffset();
                try {
                    doc.insertString(lineStart, "// ", null);
                }
                catch(Exception exc) {}
            }
            
             textPane.setCaretPosition(
                          text.getElement(firstLineIndex).getStartOffset());
             textPane.moveCaretPosition(
                          text.getElement(lastLineIndex).getEndOffset());
        }
    }

    // --------------------------------------------------------------------

    class UncommentAction extends MoeAbstractAction {

        public UncommentAction() {
            super("uncomment",
                  KeyStroke.getKeyStroke(KeyEvent.VK_O, Event.CTRL_MASK));
        }

        public void actionPerformed(ActionEvent e) {

            getEditor(e);
            JTextComponent textPane = getTextComponent(e);
            Caret caret = textPane.getCaret();
            int selectionStart = caret.getMark();
            int selectionEnd = caret.getDot() - 1;  // skip last position
            if(selectionStart > selectionEnd) {
                int tmp = selectionStart;
                selectionStart = selectionEnd;
                selectionEnd = tmp;
            }

            MoeSyntaxDocument doc = (MoeSyntaxDocument)textPane.getDocument();
            Element text = doc.getDefaultRootElement();

            int firstLineIndex = text.getElementIndex(selectionStart);
            int lastLineIndex = text.getElementIndex(selectionEnd);
            for(int i = firstLineIndex; i <= lastLineIndex; i++) {
                Element line = text.getElement(i);
                int lineStart = line.getStartOffset();
                int lineEnd = line.getEndOffset();
                try {
                    String lineText = doc.getText(lineStart, lineEnd-lineStart);
                    if(lineText.trim().startsWith("//")) {
                        int cnt=0;
                        while(lineText.charAt(cnt) != '/')   // whitespace chars
                            cnt++;
                        if(lineText.charAt(cnt+2) == ' ')
                            doc.remove(lineStart, cnt+3);
                        else
                            doc.remove(lineStart, cnt+2);
                    }
                }
                catch (Exception exc) {}
            }
            
            textPane.setCaretPosition(
                         text.getElement(firstLineIndex).getStartOffset());
            textPane.moveCaretPosition(
                         text.getElement(lastLineIndex).getEndOffset());
        }
    }

    // --------------------------------------------------------------------

    class InsertMethodAction extends MoeAbstractAction {

        public InsertMethodAction() {
            super("insert-method",
                  KeyStroke.getKeyStroke(KeyEvent.VK_M, Event.CTRL_MASK));
        }

        public void actionPerformed(ActionEvent e) {
            getEditor(e);
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
               "        return y;\n" +
               "    }");
            textPane.setCaretPosition(pos);
        }
    }

    // --------------------------------------------------------------------

    class IndentAction extends MoeAbstractAction {

        public IndentAction() {
            super("indent",
                  KeyStroke.getKeyStroke(KeyEvent.VK_TAB, Event.SHIFT_MASK));
        }

        public void actionPerformed(ActionEvent e) {
            JTextComponent textPane = getTextComponent(e);
            doIndent(textPane);
        }
    }

    // --------------------------------------------------------------------

    class BreakAndIndentAction extends MoeAbstractAction {

        public BreakAndIndentAction() {
            super("insert-break-and-indent",
                 KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, Event.SHIFT_MASK));
        }

        public void actionPerformed(ActionEvent e) {
            Action action = (Action)(actions.get("insert-break"));
            action.actionPerformed(e);

            JTextComponent textPane = getTextComponent(e);
            doIndent(textPane);
        }
    }

    // --------------------------------------------------------------------

    // **** not needed any more - TABs now set to 4
    // 	public HalfTabAction() {
    //  	    super("insert-half-tab",
    //  		  KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0));
    // 	}

    // 	public void actionPerformed(ActionEvent e) {
    // 	    getEditor(e);
    // 	    JTextComponent textPane = getTextComponent(e);
    // 	    Element line = getCurrentLine(textPane);
    // 	    int lineStart = line.getStartOffset();
    // 	    int cursorPos = textPane.getCaretPosition();
    // 	    int numSpaces = 4 - ((cursorPos - lineStart) % 4);
    // 	    textPane.replaceSelection(spaces.substring(0, numSpaces));
    // 	}
    //     }

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
            DialogManager.NYI(getEditor(e));
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

    class ToggleBreakPointAction extends MoeAbstractAction {

        public ToggleBreakPointAction() {
            super("toggle-breakpoint",
                  KeyStroke.getKeyStroke(KeyEvent.VK_B, Event.CTRL_MASK));
        }

        public void actionPerformed(ActionEvent e) {
            getEditor(e).toggleBreakpoint();
        }
    }

    // === Options: ===
    // --------------------------------------------------------------------

    class KeyBindingsAction extends MoeAbstractAction {

        public KeyBindingsAction() {
            super("key-bindings", null);
        }

        public void actionPerformed(ActionEvent e) {
            FunctionDialog dlg = new FunctionDialog(getEditor(e), actionTable,
                                                    categories, categoryIndex);

            dlg.setVisible(true);
        }
    }

    // --------------------------------------------------------------------

    class PreferencesAction extends MoeAbstractAction {

        public PreferencesAction() {
            super("preferences", null);
        }

        public void actionPerformed(ActionEvent e) {
            PrefMgrDialog.showDialog(getEditor(e));
        }
    }

    // === Help: ===
    // --------------------------------------------------------------------

    class AboutAction extends MoeAbstractAction {

        public AboutAction() {
            super("about-editor", null);
        }

        public void actionPerformed(ActionEvent e) {
            JOptionPane.showMessageDialog(getEditor(e),
                                          new String[] {
                "Moe",
                    "Version " + MoeEditor.versionString,
                    " ",
                    "Moe is the editor of the BlueJ programming environment.",
                    "Written by Michael K\u00F6lling (mik@monash.edu.au)."
                    },
                "About Moe", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    // --------------------------------------------------------------------

    class DescribeKeyAction extends MoeAbstractAction {

        public DescribeKeyAction() {
            super("describe-key",
                  KeyStroke.getKeyStroke(KeyEvent.VK_D, Event.CTRL_MASK));
        }

        public void actionPerformed(ActionEvent e) {
            JTextComponent textComponent = getTextComponent(e);
            textComponent.addKeyListener(keyCatcher);
            MoeEditor ed = getEditor(e);
            keyCatcher.setEditor(ed);
            ed.writeMessage("Describe key: ");
        }
    }

    // --------------------------------------------------------------------

    class HelpMouseAction extends MoeAbstractAction {

        public HelpMouseAction() {
            super("help-mouse", null);
        }

        public void actionPerformed(ActionEvent e) {
            JOptionPane.showMessageDialog(getEditor(e),
                                          new String[] {
                "Moe Mouse Buttons:",
                    " ",
                    "left button:",
                    "   click: place cursor",
                    "   double-click: select word",
                    "   triple-click: select line",
                    "   drag: make selection",
                    " ",
                    "right button:",
                    "   (currently unused)",
                    },
                "Moe Mouse Buttons", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    // --------------------------------------------------------------------

    class ShowManualAction extends MoeAbstractAction {

        public ShowManualAction() {
            super("show-manual", null);
        }

        public void actionPerformed(ActionEvent e) {
            DialogManager.NYI(getEditor(e));
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
    // 	  DialogManager.NYI(editor);
    //       }
    //     }


    // ========================= SUPPORT ROUTINES ==========================

    // --------------------------------------------------------------------
    /**
     *
     */

    // --------------------------------------------------------------------
    /**
     * Return the current line.
     */

    private Element getCurrentLine(JTextComponent text)
    {
        MoeSyntaxDocument document = (MoeSyntaxDocument)text.getDocument();
        return document.getParagraphElement(text.getCaretPosition());
    }

    // --------------------------------------------------------------------
    /**
     * Find and return a line by line number
     */

    private Element getLine(JTextComponent text, int lineNo)
    {
        return text.getDocument().getDefaultRootElement().getElement(lineNo);
    }

    // -------------------------------------------------------------------
    /**
     * Find and return a line by text position
     */

    private Element getLineAt(JTextComponent text, int pos)
    {
        MoeSyntaxDocument document = (MoeSyntaxDocument)text.getDocument();
        return document.getParagraphElement(pos);
    }

    // -------------------------------------------------------------------
    /**
     * Return the number of the current line.
     */

    private int getCurrentLineIndex(JTextComponent text)
    {
        MoeSyntaxDocument document = (MoeSyntaxDocument)text.getDocument();
	return document.getDefaultRootElement().getElementIndex(
					text.getCaretPosition());
    }

//      // -------------------------------------------------------------------
//      /**
//       * Return the number of the line containing position 'pos'.
//       */

//      private int getLineNumberAt(JTextComponent textComponent, int pos)
//      {
//  	return document.getDefaultRootElement().getElementIndex(pos) + 1;
//      }


    // ===================== ACTION IMPLEMENTATION ======================

    private void doIndent(JTextComponent textPane)
    {
        int lineIndex = getCurrentLineIndex(textPane);
        if(lineIndex == 0) 
            return;
        
        try {
            // get indentation string from previous line

            MoeSyntaxDocument doc = (MoeSyntaxDocument)textPane.getDocument();
            Element line = getLine(textPane, lineIndex - 1);
            int lineStart = line.getStartOffset();
            int lineEnd = line.getEndOffset();
            String lineText = doc.getText(lineStart, lineEnd-lineStart);
            int indentPos = findFirstNonWhiteChar(lineText);
            String indent = lineText.substring(0, indentPos);

            // find and replace indentation of current line

            line = getLine(textPane, lineIndex);
            lineStart = line.getStartOffset();
            lineEnd = line.getEndOffset();
            lineText = doc.getText(lineStart, lineEnd-lineStart);
            indentPos = findFirstNonWhiteChar(lineText);
            doc.remove(lineStart, indentPos);
            doc.insertString(lineStart, indent, null);

            //textPane.setCaretPosition(lineStart + indent.length());
        }
        catch (Exception exc) {}
    }

    private int findFirstNonWhiteChar(String s)
    {
        int cnt=0;
        char ch = s.charAt(0);

        while(ch == ' ' || ch == '\t') {   // SPACE or TAB
            cnt++;
            ch = s.charAt(cnt);
        }
        return cnt;
    }


    // --------------------------------------------------------------------
    /**
     * Create the table of action supported by this editor
     */

    private void createActionTable(JTextComponent textComponent)
    {
        undoAction = new UndoAction();
        redoAction = new RedoAction();
        compileAction = new CompileAction();

        // get all actions into arrays

        Action[] textActions = textComponent.getActions();
        Action[] myActions = {
            new SaveAction(),
            new ReloadAction(),
            new PrintAction(),
            new CloseAction(),

            undoAction,
            redoAction,
            new CommentAction(),
            new UncommentAction(),
            new InsertMethodAction(),
            new IndentAction(),
            new BreakAndIndentAction(),

            new FindAction(),
            new FindBackwardAction(),
            new FindNextAction(),
            new FindNextReverseAction(),
            new ReplaceAction(),
            compileAction,
            new ToggleBreakPointAction(),

            new KeyBindingsAction(),
            new PreferencesAction(),

            new AboutAction(),
            new DescribeKeyAction(),
            new HelpMouseAction(),
            new ShowManualAction(),
        };

        // insert all actions into a hashtable

        actions = new Hashtable();

        Action action;
        for (int i=0; i < textActions.length; i++) {
            action = textActions[i];
            actions.put(action.getValue(Action.NAME), action);
        }
        for (int i=0; i < myActions.length; i++) {
            action = myActions[i];
            actions.put(action.getValue(Action.NAME), action);
        }

        // sort all actions into a big, ordered table

        actionTable = new Action[] {

            // edit functions
            
            (Action)(actions.get("delete-previous")),           // 0
            (Action)(actions.get("delete-next")),
            (Action)(actions.get("copy-to-clipboard")),
            (Action)(actions.get("cut-to-clipboard")),
            (Action)(actions.get("paste-from-clipboard")),
            (Action)(actions.get("insert-tab")),
            (Action)(actions.get("insert-break")),
            (Action)(actions.get("insert-break-and-indent")),
            (Action)(actions.get("indent")),
            (Action)(actions.get("insert-method")),
            (Action)(actions.get("comment")),
            (Action)(actions.get("uncomment")),

            (Action)(actions.get("select-word")),               // 12
            (Action)(actions.get("select-line")),
            (Action)(actions.get("select-paragraph")),
            (Action)(actions.get("select-all")),
            (Action)(actions.get("unselect")),
            (Action)(actions.get("selection-backward")),
            (Action)(actions.get("selection-forward")),
            (Action)(actions.get("selection-up")),
            (Action)(actions.get("selection-down")),            // 20
            (Action)(actions.get("selection-begin-word")),
            (Action)(actions.get("selection-end-word")),
            (Action)(actions.get("selection-previous-word")),
            (Action)(actions.get("selection-next-word")),
            (Action)(actions.get("selection-begin-line")),
            (Action)(actions.get("selection-end-line")),
            (Action)(actions.get("selection-begin-paragraph")),
            (Action)(actions.get("selection-end-paragraph")),
            (Action)(actions.get("selection-page-up")),
            (Action)(actions.get("selection-page-down")),       // 30
            (Action)(actions.get("selection-begin")),
            (Action)(actions.get("selection-end")),

            // move and scroll functions

            (Action)(actions.get("caret-backward")),            // 33
            (Action)(actions.get("caret-forward")),
            (Action)(actions.get("caret-up")),
            (Action)(actions.get("caret-down")),
            (Action)(actions.get("caret-begin-word")),
            (Action)(actions.get("caret-end-word")),
            (Action)(actions.get("caret-previous-word")),
            (Action)(actions.get("caret-next-word")),            // 40
            (Action)(actions.get("caret-begin-line")),
            (Action)(actions.get("caret-end-line")),
            (Action)(actions.get("caret-begin-paragraph")),
            (Action)(actions.get("caret-end-paragraph")),
            (Action)(actions.get("page-up")),
            (Action)(actions.get("page-down")),
            (Action)(actions.get("caret-begin")),
            (Action)(actions.get("caret-end")),

            // class functions
            (Action)(actions.get("save")),                      // 49
            (Action)(actions.get("reload")),
            (Action)(actions.get("close")),
            (Action)(actions.get("print")),

            // customisation functions
            (Action)(actions.get("key-bindings")),              // 53
            (Action)(actions.get("preferences")),

            // help functions
            (Action)(actions.get("describe-key")),              // 55
            (Action)(actions.get("help-mouse")),
            (Action)(actions.get("show-manual")),
            (Action)(actions.get("about-editor")),

            // misc functions
            undoAction,                                         // 59
            redoAction,
            (Action)(actions.get("find")),
            (Action)(actions.get("find-backward")),
            (Action)(actions.get("find-next")),
            (Action)(actions.get("find-next-reverse")),
            (Action)(actions.get("replace")),
            (Action)(actions.get("compile")),
            (Action)(actions.get("toggle-breakpoint")),
        };                                                      // 68

        categories = new String[] { "Edit Functions",
                                    "Move & Scroll",
                                    "Class Functions",
                                    "Customisation",
                                    "Help",
                                    "Misc." };
        categoryIndex = new int[] { 0, 33, 49, 53, 55, 59, 68 };
    }

    class KeyCatcher extends KeyAdapter
    {
        Action action;
        boolean haveKey = false;
        String keyName;
        MoeEditor editor;

        public void keyPressed(KeyEvent e)
        {
            KeyStroke key = KeyStroke.getKeyStrokeForEvent(e);
            keyName = KeyEvent.getKeyModifiersText(key.getModifiers());
            if(keyName.length() == 0)
                keyName = "" + (char)key.getKeyCode();
            else
                keyName = keyName + "+" + (char)key.getKeyCode();
            action = keymap.getAction(key);
            haveKey = true;
            e.consume();
        }

        public void keyTyped(KeyEvent e)
        {
            if(haveKey) {
                if (action == null)
                    editor.writeMessage(keyName + " is not bound to a function");
                else {
                    String name = (String) action.getValue(Action.NAME);
                    editor.writeMessage(keyName + " calls the function \"" + name
                                    + "\"");
                }                    
                e.getComponent().removeKeyListener(keyCatcher);
                haveKey = false;
            }
        }

        public void setEditor(MoeEditor ed)
        {
            editor = ed;
        }
    }

} // end class MoeActions
