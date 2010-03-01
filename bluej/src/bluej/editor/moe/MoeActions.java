/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2010  Michael Kolling and John Rosenberg 

 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 

 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 

 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 

 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package bluej.editor.moe;

import java.awt.Container;
import java.awt.Event;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import javax.swing.Action;
import javax.swing.InputMap;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.Keymap;
import javax.swing.text.TextAction;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import bluej.Config;
import bluej.parser.nodes.CommentNode;
import bluej.parser.nodes.ContainerNode;
import bluej.parser.nodes.ExpressionNode;
import bluej.parser.nodes.MethodNode;
import bluej.parser.nodes.ParsedNode;
import bluej.parser.nodes.TypeInnerNode;
import bluej.parser.nodes.NodeTree.NodeAndPosition;
import bluej.prefmgr.PrefMgr;
import bluej.prefmgr.PrefMgrDialog;
import bluej.utility.Debug;
import bluej.utility.DialogManager;

/**
 * A set of actions supported by the Moe editor. This is a singleton: the
 * actions are shared between all editor instances.
 * 
 * Actions are stored both in a hashtable and in an array. The hashtable is used
 * for fast lookup by name, whereas the array is needed to support complete,
 * ordered access.
 * 
 * @author Michael Kolling
 * @author Bruce Quig
 */

public final class MoeActions
{
    // -------- CONSTANTS --------

    private static final String KEYS_FILE = "editor.keys";

    private static int SHORTCUT_MASK;
    private static int ALT_SHORTCUT_MASK;
    private static int SHIFT_SHORTCUT_MASK;
    private static int SHIFT_ALT_SHORTCUT_MASK;
    private static int DOUBLE_SHORTCUT_MASK; // two masks (ie. CTRL + META)

    private static final int tabSize = Config.getPropInteger("bluej.editor.tabsize", 4);
    private static final String spaces = "                                        ";
    private static final char TAB_CHAR = '\t';

    // -------- INSTANCE VARIABLES --------

    private Action[] actionTable; // table of all known actions
    private Hashtable<Object, Action> actions; // the same actions in a hashtable
    private String[] categories;
    public Action[] getActionTable() {
        return actionTable;
    }

    public void setActionTable(Action[] actionTable) {
        this.actionTable = actionTable;
    }

    public String[] getCategories() {
        return categories;
    }

    public void setCategories(String[] categories) {
        this.categories = categories;
    }

    public int[] getCategoryIndex() {
        return categoryIndex;
    }

    public void setCategoryIndex(int[] categoryIndex) {
        this.categoryIndex = categoryIndex;
    }

    private int[] categoryIndex;

    private Keymap keymap; // the editor's keymap
    private KeyCatcher keyCatcher;

    private boolean lastActionWasCut; // true if last action was a cut action
    // undo helpers
    public UndoAction undoAction;
    public RedoAction redoAction;

    public FindNextAction findNextAction;
    public FindNextBackwardAction findNextBackwardAction;

    // frequently needed actions
    public Action compileAction;
    public Action contentAssistAction;

    // for bug workaround:
    private InputMap componentInputMap;

    // =========================== STATIC METHODS ===========================

    private static MoeActions moeActions;

    /**
     * Get the actions object (a singleton) and, at the same time, install the
     * action keymap as the main keymap for the given textComponent..
     */
    public static MoeActions getActions(JTextComponent textComponent)
    {
        if (moeActions == null)
            moeActions = new MoeActions(textComponent);

        if (textComponent != null)
            textComponent.setKeymap(moeActions.keymap);
        return moeActions;
    }

    // ========================== INSTANCE METHODS ==========================

    /**
     * Constructor. Singleton, thus private.
     */
    private MoeActions(JTextComponent textComponent)
    {
        // sort out modifier keys...
        SHORTCUT_MASK = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

        if (SHORTCUT_MASK == Event.CTRL_MASK)
            ALT_SHORTCUT_MASK = Event.META_MASK; // alternate (second) modifier
        else
            ALT_SHORTCUT_MASK = Event.CTRL_MASK;

        SHIFT_SHORTCUT_MASK = SHORTCUT_MASK + Event.SHIFT_MASK;
        SHIFT_ALT_SHORTCUT_MASK = Event.SHIFT_MASK + ALT_SHORTCUT_MASK;
        DOUBLE_SHORTCUT_MASK = SHORTCUT_MASK + ALT_SHORTCUT_MASK;

        // install our own keymap, with the existing one as parent
        keymap = JTextComponent.addKeymap("BlueJ map", textComponent.getKeymap());

        createActionTable(textComponent);
        keyCatcher = new KeyCatcher();
        if (!load())
            setDefaultKeyBindings();
        lastActionWasCut = false;

        // for bug workaround (below)
        componentInputMap = textComponent.getInputMap();
    }

    public void setUndoEnabled(boolean enabled)
    {
        undoAction.setEnabled(enabled);
    }

    public void setRedoEnabled(boolean enabled)
    {
        redoAction.setEnabled(enabled);
    }

    public void setPasteEnabled(boolean enabled)
    {
        actions.get(DefaultEditorKit.pasteAction).setEnabled(enabled);
    }

    public FindNextAction getFindNextAction()
    {
        return findNextAction;
    }

    public FindNextBackwardAction getFindNextBackwardAction()
    {
        return findNextBackwardAction;
    }

    /**
     * Allow the enabling/disabling of an action. 
     * @param action  String representing name of action
     * @param flag  true to enable action from menu.
     */

    public void enableAction(String action, boolean flag)
    {
        Action moeAction = getActionByName(action);
        if (moeAction != null) {
            moeAction.setEnabled(flag);
        }
    }


    /**
     * Return an action with a given name.
     */
    public Action getActionByName(String name)
    {
        return (Action) (actions.get(name));
    }

    /**
     * Get a keystroke for an action. Return null is there is none.
     */
    public KeyStroke[] getKeyStrokesForAction(Action action)
    {
        KeyStroke[] keys = keymap.getKeyStrokesForAction(action);
        keys = addComponentKeyStrokes(action, keys); // BUG workaround
        if (keys != null && keys.length > 0)
            return keys;
        else
            return null;
    }

    /**
     * BUG WORKAROUND: currently, keymap.getKeyStrokesForAction() misses
     * keystrokes that come from JComponents inputMap. Here, we add those
     * ourselves...
     */
    public KeyStroke[] addComponentKeyStrokes(Action action, KeyStroke[] keys)
    {
        ArrayList<KeyStroke> keyStrokes = null;
        KeyStroke[] componentKeys = componentInputMap.allKeys();

        // find all component keys that bind to this action
        for (int i = 0; i < componentKeys.length; i++) {
            if (componentInputMap.get(componentKeys[i]).equals(action.getValue(Action.NAME))) {
                if (keyStrokes == null)
                    keyStrokes = new ArrayList<KeyStroke>();
                keyStrokes.add(componentKeys[i]);
            }
        }

        // test whether this keyStroke was redefined in keymap
        if (keyStrokes != null) {
            for (Iterator<KeyStroke> i = keyStrokes.iterator(); i.hasNext();) {
                if (keymap.getAction((KeyStroke) i.next()) != null) {
                    i.remove();
                }
            }
        }

        // merge found keystrokes into key array
        if ((keyStrokes == null) || (keyStrokes.size() == 0))
            return keys;

        KeyStroke[] allKeys;
        if (keys == null) {
            allKeys = new KeyStroke[keyStrokes.size()];
            keyStrokes.toArray(allKeys);
        }
        else { // merge new keystrokes into keys
            allKeys = new KeyStroke[keyStrokes.size() + keys.length];
            keyStrokes.toArray(allKeys);
            System.arraycopy(allKeys, 0, allKeys, keys.length, keyStrokes.size());
            System.arraycopy(keys, 0, allKeys, 0, keys.length);
        }
        return allKeys;
    }

    /**
     * Add a new key binding into the action table.
     */
    public void addActionForKeyStroke(KeyStroke key, Action a)
    {
        keymap.addActionForKeyStroke(key, a);
    }

    /**
     * Remove a key binding from the action table.
     */
    public void removeKeyStrokeBinding(KeyStroke key)
    {
        keymap.removeKeyStrokeBinding(key);
    }

    /**
     * Save the key bindings. Return true if successful.
     */
    public boolean save()
    {
        try {
            File file = Config.getUserConfigFile(KEYS_FILE);
            FileOutputStream ostream = new FileOutputStream(file);
            ObjectOutputStream stream = new ObjectOutputStream(ostream);
            KeyStroke[] keys = keymap.getBoundKeyStrokes();
            stream.writeInt(MoeEditor.version);
            stream.writeInt(keys.length);
            for (int i = 0; i < keys.length; i++) {
                stream.writeObject(keys[i]);
                stream.writeObject(keymap.getAction(keys[i]).getValue(Action.NAME));
            }
            stream.flush();
            ostream.close();
            return true;
        }
        catch (Exception exc) {
            Debug.message("Cannot save key bindings: " + exc);
            return false;
        }
    }

    /**
     * Load the key bindings. Return true if successful.
     */
    public boolean load()
    {
        try {
            File file = Config.getUserConfigFile(KEYS_FILE);
            FileInputStream istream = new FileInputStream(file);
            ObjectInputStream stream = new ObjectInputStream(istream);
            //KeyStroke[] keys = keymap.getBoundKeyStrokes();
            int version = 0;
            int count = stream.readInt();
            if (count > 100) { // it was new format: version number stored first
                version = count;
                count = stream.readInt();
            }
            if (Config.isMacOS() && (version < 140)) {
                // do not attempt to load old bindings on MacOS when switching
                // to jdk 1.4.1
                return false;
            }

            for (int i = 0; i < count; i++) {
                KeyStroke key = (KeyStroke) stream.readObject();
                String actionName = (String) stream.readObject();
                Action action = (Action) (actions.get(actionName));
                if (action != null) {
                    keymap.addActionForKeyStroke(key, action);
                }
            }
            istream.close();

            // set up bindings for new actions in recent releases

            if (version < 130) {
                keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), 
                        (Action) (actions.get("indent")));
                keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, Event.SHIFT_MASK),
                        (Action) (actions.get("insert-tab")));
                keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), 
                        (Action) (actions.get("new-line")));
                keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, Event.SHIFT_MASK),
                        (Action) (actions.get("insert-break")));
            }
            if (version < 200) {
                keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, Event.SHIFT_MASK), 
                        (Action) (actions.get("de-indent")));
                keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_I, SHORTCUT_MASK), 
                        (Action) (actions.get("insert-tab")));
            }
            if (version < 252) {
                keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, SHORTCUT_MASK),
                        (Action) (actions.get("increase-font")));
                keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, SHORTCUT_MASK),
                        (Action) (actions.get("decrease-font")));
            }
            if (version < 300) {
                keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, Event.CTRL_MASK), (Action) (actions
                        .get("code-completion")));
            }
            return true;
        }
        catch (Exception exc) {
            // ignore - file probably didn't exist (yet)
            return false;
        }
    }

    /**
     * Called to inform that any one of the user actions (text edit or caret
     * move) was executed.
     */
    public void userAction()
    {
        lastActionWasCut = false;
    }

    /**
     * Called at every insertion of text into the document.
     */
    public void textInsertAction(DocumentEvent evt, JTextComponent textPane)
    {
        try {
            if (evt.getLength() == 1) { // single character inserted
                Document doc = evt.getDocument();
                int offset = evt.getOffset();
                char ch = doc.getText(offset, 1).charAt(0);

                // 'ch' is the character that was just typed
                // currently, the only character upon which we act is the
                // closing brace ('}')

                if (ch == '}') {
                    closingBrace(textPane, doc, offset);
                }
            }
        }
        catch (BadLocationException e) {}
    }

    /**
     * We just typed a closing brace character - indent appropriately.
     */
    private void closingBrace(JTextComponent textPane, Document doc, int offset)
    throws BadLocationException
    {
        int lineIndex = getCurrentLineIndex(textPane);
        Element line = getLine(textPane, lineIndex);
        int lineStart = line.getStartOffset();
        String prefix = doc.getText(lineStart, offset - lineStart);

        if(prefix.trim().length() == 0) {  // only if there is no other text before '}'
            // Determine where the cursor appears horizontally (before insertion)
            Rectangle r = textPane.modelToView(textPane.getCaretPosition() - 1);
            Point p = r.getLocation();

            // Indent the line
            textPane.setCaretPosition(lineStart);
            doIndent(textPane, true);
            textPane.setCaretPosition(textPane.getCaretPosition() + 1);

            // Set the magic position to the original position. This means that
            // cursor up will go to the beginning of the previous line, which is much
            // nicer behaviour.
            textPane.getCaret().setMagicCaretPosition(p);
        }
    }

    // ============================ USER ACTIONS =============================

    abstract class MoeAbstractAction extends TextAction
    {

        public MoeAbstractAction(String name)
        {
            super(name);
        }

        /* side effect: clears message in editor! */
        protected final MoeEditor getEditor(ActionEvent e)
        {
            MoeEditor ed = null;

            // the source of the event is the first place to look
            Object source = e.getSource();
            if (source instanceof JComponent) {
                Container c = ((JComponent) source).getTopLevelAncestor(); 
                if (c instanceof MoeEditor)
                    ed = (MoeEditor) c;
            }

            // otherwise use 'getTextComponent'
            if (ed == null) {
                JTextComponent textComponent = getTextComponent(e);               
                if (textComponent != null) {
                    Container c = textComponent.getTopLevelAncestor();
                    if (c instanceof MoeEditor)
                        ed = (MoeEditor) c;
                }
            }
            if (ed != null)
                ed.clearMessage();
            return ed;
        }
    }

    // === File: ===
    // --------------------------------------------------------------------

    class SaveAction extends MoeAbstractAction
    {

        public SaveAction()
        {
            super("save");
        }

        public void actionPerformed(ActionEvent e)
        {
            getEditor(e).userSave();
        }
    }

    // --------------------------------------------------------------------

    /**
     * Reload has been chosen. Ask "Really?" and call "doReload" if the answer
     * is yes.
     */
    class ReloadAction extends MoeAbstractAction
    {

        public ReloadAction()
        {
            super("reload");
        }

        public void actionPerformed(ActionEvent e)
        {
            getEditor(e).reload();
        }
    }

    // --------------------------------------------------------------------

    class PrintAction extends MoeAbstractAction
    {

        public PrintAction()
        {
            super("print");
        }

        public void actionPerformed(ActionEvent e)
        {
            getEditor(e).print();
        }
    }

    // --------------------------------------------------------------------

    class PageSetupAction extends MoeAbstractAction
    {

        public PageSetupAction()
        {
            super("page-setup");
        }

        public void actionPerformed(ActionEvent e)
        {
            getEditor(e).pageSetup();
        }
    }

    // --------------------------------------------------------------------

    class CloseAction extends MoeAbstractAction
    {

        public CloseAction()
        {
            super("close");
        }

        public void actionPerformed(ActionEvent e)
        {
            getEditor(e).close();
        }
    }

    // === Edit: ===
    // --------------------------------------------------------------------

    class UndoAction extends MoeAbstractAction
    {

        public UndoAction()
        {
            super("undo");
            this.setEnabled(false);
        }

        public void actionPerformed(ActionEvent e)
        {
            MoeEditor editor = getEditor(e);
            try {
                editor.undoManager.undo();
            }
            catch (CannotUndoException ex) {
                Debug.message("moe: cannot undo...");
            }
            editor.updateUndoControls();
            editor.updateRedoControls();
        }
    }

    // --------------------------------------------------------------------

    class RedoAction extends MoeAbstractAction
    {

        public RedoAction()
        {
            super("redo");
            this.setEnabled(false);
        }

        public void actionPerformed(ActionEvent e)
        {
            MoeEditor editor = getEditor(e);
            try {
                editor.undoManager.redo();
            }
            catch (CannotRedoException ex) {
                Debug.message("moe: cannot redo...");
            }
            editor.updateUndoControls();
            editor.updateRedoControls();
        }
    }

    // --------------------------------------------------------------------

    class CommentBlockAction extends MoeAbstractAction
    {

        public CommentBlockAction()
        {
            super("comment-block");
        }

        public void actionPerformed(ActionEvent e)
        {
            MoeEditor editor = getEditor(e);
            editor.undoManager.beginCompoundEdit();
            blockAction(getTextComponent(e), new CommentLineAction());
            editor.undoManager.endCompoundEdit();
        }
    }

    // --------------------------------------------------------------------

    class UncommentBlockAction extends MoeAbstractAction
    {

        public UncommentBlockAction()
        {
            super("uncomment-block");
        }

        public void actionPerformed(ActionEvent e)
        {
            MoeEditor editor = getEditor(e);
            editor.undoManager.beginCompoundEdit();
            blockAction(getTextComponent(e), new UncommentLineAction());
            editor.undoManager.endCompoundEdit();
        }
    }

    // --------------------------------------------------------------------

    class IndentBlockAction extends MoeAbstractAction
    {

        public IndentBlockAction()
        {
            super("indent-block");
        }

        public void actionPerformed(ActionEvent e)
        {
            MoeEditor editor = getEditor(e);
            editor.undoManager.beginCompoundEdit();
            blockAction(getTextComponent(e), new IndentLineAction());
            editor.undoManager.endCompoundEdit();
        }
    }

    // --------------------------------------------------------------------

    class DeindentBlockAction extends MoeAbstractAction
    {

        public DeindentBlockAction()
        {
            super("deindent-block");
        }

        public void actionPerformed(ActionEvent e)
        {
            MoeEditor editor = getEditor(e);
            editor.undoManager.beginCompoundEdit();
            blockAction(getTextComponent(e), new DeindentLineAction());
            editor.undoManager.endCompoundEdit();
        }
    }

    // --------------------------------------------------------------------
    
    class AutoIndentAction extends MoeAbstractAction
    {
    	private MoeSyntaxDocument doc;

        public AutoIndentAction()
        {
            super("autoindent");
        }

        public void actionPerformed(ActionEvent e)
        {
        	MoeEditor editor = getEditor(e);
        	doc = editor.getSourceDocument();
                        
			editor.undoManager.beginCompoundEdit();
			List<DocumentAction> updates = calculateIndents(editor);
			for (DocumentAction update : updates) {
				update.apply(doc);
			}
			editor.undoManager.endCompoundEdit();
        }
        
		private List<DocumentAction> calculateIndents(MoeEditor editor)
		{
			Element rootElement = doc.getDefaultRootElement();
			List<DocumentAction> updates = new ArrayList<DocumentAction>(rootElement.getElementCount());

			IndentCalculator ii = new RootIndentCalculator();
			
			boolean lastLineWasBlank = false;
			boolean perfect = true;

			for (int i = 0; i < rootElement.getElementCount(); i++) {
				Element el = rootElement.getElement(i);

				boolean thisLineBlank = isWhiteSpaceOnly(getElementContents(
						doc, el));
				DocumentAction update = null;

				if (thisLineBlank) {
					if (lastLineWasBlank) {
						// Consecutive blank lines; remove this one:
						if (el.getEndOffset() <= doc.getLength())
						{
							update = new DocumentRemoveLineAction(el);
							perfect = false;
						}
					}
					else {
						// Single blank line (thus far), remove all spaces from
						// it (and don't interrupt perfect status):
						update = new DocumentIndentAction(el, "");
					}
				}
				else {
					NodeAndPosition root = new NodeAndPosition(doc.getParser(),
							0, doc.getParser().getSize());
					String indent = calculateIndent(el, root, ii);
					update = new DocumentIndentAction(el, indent);
					perfect = perfect && getElementContents(doc, el).startsWith(indent);
				}
				
				if (update != null)
					updates.add(update);
				lastLineWasBlank = thisLineBlank;
			}
			
			if (perfect)
				editor.writeMessage(Config.getString("editor.info.perfectIndent"));

			return updates;
		}

		/**
		 * Finds the indent for the given element by looking at the nodes in the parse tree
		 * 
		 * @param el The element to calculate the indent for
		 * @param start The Node that is either the one directly containing the given element,
		 *              or is an ancestor of the one that directly contains the given element,
		 *              or may not contain the element at all (in which case null will be returned)
		 * @param startIC The IndentCalculator corresponding to start
         * @return The indent that the element should have, up to the first non-whitespace character.
         *         Returns null if start does not contain the given element
		 */
		private String calculateIndent(Element el,
				NodeAndPosition start, IndentCalculator startIC)
		{
			int pos = el.getStartOffset()
					+ findFirstNonIndentChar(getElementContents(doc, el), true);
			if (pos >= start.getPosition() && pos < start.getEnd()) {

				// The slightly awkward way to loop through the children of "start":
				for (NodeAndPosition nap = start.getNode().findNodeAtOrAfter(start.getPosition(), start.getPosition())
				    ; nap != null
				    ; nap = start.getNode().findNodeAtOrAfter(nap.getEnd(), start.getPosition())
				    ) {
					
					String inner = calculateIndent(el, nap, startIC.getForChild(nap.getNode()));
					if (inner != null)
						return inner;
				}
				try {
					return startIC.getCurIndent(doc.getText(pos, 1).charAt(0));
				}
				catch (BadLocationException e) {
					return "";
				}
			}
			else {
				return null;
			}
		}
    }
    
    // --------------------------------------------------------------------
    
    
    class InsertMethodAction extends MoeAbstractAction
    {

        public InsertMethodAction()
        {
            super("insert-method");
        }

        public void actionPerformed(ActionEvent e)
        {
            MoeEditor editor = getEditor(e);
            editor.undoManager.beginCompoundEdit();
            insertTemplate(getTextComponent(e), "method");
            editor.undoManager.endCompoundEdit();
        }
    }

    // --------------------------------------------------------------------

    class IndentAction extends MoeAbstractAction
    {

        public IndentAction()
        {
            super("indent");
        }

        public void actionPerformed(ActionEvent e)
        {
            JTextComponent textPane = getTextComponent(e);
            MoeEditor ed = getEditor(e);

            // if necessary, convert all TABs in the current editor to spaces
            int converted = 0;
            if (ed.checkExpandTabs()) // do TABs need expanding?
                converted = convertTabsToSpaces(textPane);

            if (PrefMgr.getFlag(PrefMgr.AUTO_INDENT))
                doIndent(textPane, false);
            else
                insertSpacedTab(textPane);

            if (converted > 0)
                ed.writeMessage(Config.getString("editor.info.tabsExpanded"));
        }
    }

    // --------------------------------------------------------------------

    class DeIndentAction extends MoeAbstractAction
    {

        public DeIndentAction()
        {
            super("de-indent");
        }

        public void actionPerformed(ActionEvent e)
        {
            JTextComponent textPane = getTextComponent(e);
            MoeEditor ed = getEditor(e);

            // if necessary, convert all TABs in the current editor to spaces
            if (ed.checkExpandTabs()) { // do TABs need expanding?
                int converted = convertTabsToSpaces(textPane);

                if (converted > 0)
                    ed.writeMessage(Config.getString("editor.info.tabsExpanded"));
            }
            doDeIndent(textPane);
        }
    }

    // --------------------------------------------------------------------

    class NewLineAction extends MoeAbstractAction
    {

        public NewLineAction()
        {
            super("new-line");
        }

        public void actionPerformed(ActionEvent e)
        {

            Action action = (Action) (actions.get(DefaultEditorKit.insertBreakAction));
            action.actionPerformed(e);

            if (PrefMgr.getFlag(PrefMgr.AUTO_INDENT)) {
                JTextComponent textPane = getTextComponent(e);
                doIndent(textPane, true);
            }
        }
    }

    // --------------------------------------------------------------------

    class CopyLineAction extends MoeAbstractAction
    {

        public CopyLineAction()
        {
            super("copy-line");
        }

        public void actionPerformed(ActionEvent e)
        {
            boolean addToClipboard = lastActionWasCut;
            getActionByName("caret-begin-line").actionPerformed(e);
            getActionByName("selection-down").actionPerformed(e);
            if (addToClipboard)
                addSelectionToClipboard(getTextComponent(e));
            else
                getActionByName("copy-to-clipboard").actionPerformed(e);
            lastActionWasCut = true;
        }
    }

    // --------------------------------------------------------------------

    class CutLineAction extends MoeAbstractAction
    {

        public CutLineAction()
        {
            super("cut-line");
        }

        public void actionPerformed(ActionEvent e)
        {
            boolean addToClipboard = lastActionWasCut;
            getActionByName("caret-begin-line").actionPerformed(e);
            getActionByName("selection-down").actionPerformed(e);
            if (addToClipboard) {
                addSelectionToClipboard(getTextComponent(e));
                getActionByName("delete-previous").actionPerformed(e);
            }
            else
                getActionByName("cut-to-clipboard").actionPerformed(e);
            lastActionWasCut = true;
        }
    }

    // --------------------------------------------------------------------

    class IncreaseFontAction extends MoeAbstractAction
    {

        public IncreaseFontAction()
        {
            super("increase-font");
        }

        public void actionPerformed(ActionEvent e)
        {        	
            JTextComponent textPane = getTextComponent(e);
            Font textPFont= textPane.getFont();           
            int newFont=textPFont.getSize()+1;
            PrefMgr.setEditorFontSize(newFont);
            getTextComponent(e).setFont(textPane.getFont().deriveFont((float)newFont));            
        }
    }

    // --------------------------------------------------------------------

    class DecreaseFontAction extends MoeAbstractAction
    {

        public DecreaseFontAction()
        {
            super("decrease-font");
        }

        public void actionPerformed(ActionEvent e)
        {     
            JTextComponent textPane = getTextComponent(e);
            Font textPFont= textPane.getFont();            
            int newFont=textPFont.getSize()-1;
            PrefMgr.setEditorFontSize(newFont);
            getTextComponent(e).setFont(textPFont.deriveFont((float)newFont));
        }
    }

    // --------------------------------------------------------------------

    class CutEndOfLineAction extends MoeAbstractAction
    {

        public CutEndOfLineAction()
        {
            super("cut-end-of-line");
        }

        public void actionPerformed(ActionEvent e)
        {
            boolean addToClipboard = lastActionWasCut;

            getActionByName("selection-end-line").actionPerformed(e);
            JTextComponent textComponent = getTextComponent(e);
            String selection = textComponent.getSelectedText();
            if (selection == null)
                getActionByName("selection-forward").actionPerformed(e);

            if (addToClipboard) {
                addSelectionToClipboard(textComponent);
                getActionByName("delete-previous").actionPerformed(e);
            }
            else
                getActionByName("cut-to-clipboard").actionPerformed(e);
            lastActionWasCut = true;
        }
    }

    // --------------------------------------------------------------------

    class CutWordAction extends MoeAbstractAction
    {

        public CutWordAction()
        {
            super("cut-word");
        }

        public void actionPerformed(ActionEvent e)
        {
            boolean addToClipboard = lastActionWasCut;
            getActionByName("caret-previous-word").actionPerformed(e);
            getActionByName("selection-next-word").actionPerformed(e);
            if (addToClipboard) {
                addSelectionToClipboard(getTextComponent(e));
                getActionByName("delete-previous").actionPerformed(e);
            }
            else
                getActionByName("cut-to-clipboard").actionPerformed(e);
            lastActionWasCut = true;
        }
    }

    // --------------------------------------------------------------------

    class ContentAssistAction extends MoeAbstractAction
    {

        public ContentAssistAction()
        {
            super("code-completion");
        }

        public void actionPerformed(ActionEvent e)
        { 
            MoeEditor editor = getEditor(e);
            editor.createContentAssist();
        }
    }

    // --------------------------------------------------------------------

    class CutEndOfWordAction extends MoeAbstractAction
    {

        public CutEndOfWordAction()
        {
            super("cut-end-of-word");
        }

        public void actionPerformed(ActionEvent e)
        {
            boolean addToClipboard = lastActionWasCut;
            getActionByName("selection-next-word").actionPerformed(e);
            if (addToClipboard) {
                addSelectionToClipboard(getTextComponent(e));
                getActionByName("delete-previous").actionPerformed(e);
            }
            else
                getActionByName("cut-to-clipboard").actionPerformed(e);
            lastActionWasCut = true;
        }
    }

    // === Tools: ===
    // --------------------------------------------------------------------

    class FindAction extends MoeAbstractAction
    {

        public FindAction()
        {
            super("find");
        }

        public void actionPerformed(ActionEvent e)
        {
            //getEditor(e).find();
            MoeEditor editor=getEditor(e);
            if (editor!=null)
                editor.initFindPanel(editor);
        }
    }

    // --------------------------------------------------------------------

    class FindNextAction extends MoeAbstractAction
    {

        public FindNextAction()
        {
            super("find-next");
        }

        public void actionPerformed(ActionEvent e)
        {
            getEditor(e).findNext(false);
        }
    }

    // --------------------------------------------------------------------

    class FindNextBackwardAction extends MoeAbstractAction
    {

        public FindNextBackwardAction()
        {
            super("find-next-backward");
        }

        public void actionPerformed(ActionEvent e)
        {
            getEditor(e).findNext(true);
        }
    }

    // --------------------------------------------------------------------

    class ReplaceAction extends MoeAbstractAction
    {

        public ReplaceAction()
        {
            super("replace");
        }

        public void actionPerformed(ActionEvent e)
        {
            MoeEditor editor=getEditor(e);
            if (editor != null) {
                editor.setFindPanelVisible(true);
                editor.setReplacePanelVisible(true);
                editor.setReplaceIcon(true);
                if (editor.getSelectedText()!=null){
                    editor.setFindTextfield(editor.getSelectedText());
                }
            }
        }
    }

    // --------------------------------------------------------------------

    class CompileAction extends MoeAbstractAction
    {

        public CompileAction()
        {
            super("compile");
        }

        public void actionPerformed(ActionEvent e)
        {
            getEditor(e).compile();
        }
    }

    // --------------------------------------------------------------------

    class ToggleInterfaceAction extends MoeAbstractAction
    {

        public ToggleInterfaceAction()
        {
            super("toggle-interface-view");
        }

        public void actionPerformed(ActionEvent e)
        {
            Object source = e.getSource();
            if (source instanceof JComboBox)
                getEditor(e).toggleInterface();
            else
                getEditor(e).toggleInterfaceMenu();
        }
    }

    // === Debug: ===
    // --------------------------------------------------------------------

    class ToggleBreakPointAction extends MoeAbstractAction
    {

        public ToggleBreakPointAction()
        {
            super("toggle-breakpoint");
        }

        public void actionPerformed(ActionEvent e)
        {
            getEditor(e).toggleBreakpoint();
        }
    }

    // === Options: ===
    // --------------------------------------------------------------------

    class KeyBindingsAction extends MoeAbstractAction
    {

        public KeyBindingsAction()
        {
            super("key-bindings");
        }

        public void actionPerformed(ActionEvent e)
        {
            PrefMgrDialog.showDialog(1); // 1 is the index of the key bindings pane in the pref dialog
        }
    }

    // --------------------------------------------------------------------

    class PreferencesAction extends MoeAbstractAction
    {

        public PreferencesAction()
        {
            super("preferences");
        }

        public void actionPerformed(ActionEvent e)
        {
            PrefMgrDialog.showDialog(0); // 0 is the index of the editor pane in
            // the pref dialog
        }
    }

    // === Help: ===
    // --------------------------------------------------------------------

    class AboutAction extends MoeAbstractAction
    {

        public AboutAction()
        {
            super("about-editor");
        }

        public void actionPerformed(ActionEvent e)
        {
            JOptionPane.showMessageDialog(getEditor(e), new String[]{"Moe", "Version " + MoeEditor.versionString, " ",
                "Moe is the editor of the BlueJ programming environment.",
            "Written by Michael K\u00F6lling (mik@bluej.org)."}, "About Moe", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    // --------------------------------------------------------------------

    class DescribeKeyAction extends MoeAbstractAction
    {

        public DescribeKeyAction()
        {
            super("describe-key");
        }

        public void actionPerformed(ActionEvent e)
        {
            JTextComponent textComponent = getTextComponent(e);
            textComponent.addKeyListener(keyCatcher);
            MoeEditor ed = getEditor(e);
            keyCatcher.setEditor(ed);
            ed.writeMessage("Describe key: ");
        }
    }

    // --------------------------------------------------------------------

    class HelpMouseAction extends MoeAbstractAction
    {

        public HelpMouseAction()
        {
            super("help-mouse");
        }

        public void actionPerformed(ActionEvent e)
        {
            JOptionPane.showMessageDialog(getEditor(e), new String[]{"Moe Mouse Buttons:", " ", "left button:",
                "   click: place cursor", "   double-click: select word", "   triple-click: select line",
                "   drag: make selection", " ", "right button:", "   (currently unused)",}, "Moe Mouse Buttons",
                JOptionPane.INFORMATION_MESSAGE);
        }
    }

    // --------------------------------------------------------------------

    class ShowManualAction extends MoeAbstractAction
    {

        public ShowManualAction()
        {
            super("show-manual");
        }

        public void actionPerformed(ActionEvent e)
        {
            DialogManager.NYI(getEditor(e));
        }
    }

    // --------------------------------------------------------------------

    class GoToLineAction extends MoeAbstractAction
    {

        public GoToLineAction()
        {
            super("go-to-line");
        }

        public void actionPerformed(ActionEvent e)
        {
            getEditor(e).goToLine();
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

    /**
     * Add the current selection of the text component to the clipboard.
     */
    public void addSelectionToClipboard(JTextComponent textComponent)
    {
        Clipboard clipboard = textComponent.getToolkit().getSystemClipboard();

        // get text from clipboard
        Transferable content = clipboard.getContents(this);
        String clipContent = "";
        if (content != null) {
            try {
                clipContent = (String) (content.getTransferData(DataFlavor.stringFlavor));
            }
            catch (Exception exc) {} // content was not string
        }

        // add current selection and store back in clipboard
        StringSelection contents = new StringSelection(clipContent + textComponent.getSelectedText());
        clipboard.setContents(contents, contents);
    }

    // --------------------------------------------------------------------

    /**
     * Return the current column number.
     */
    private int getCurrentColumn(JTextComponent textPane)
    {
        Caret caret = textPane.getCaret();
        int pos = Math.min(caret.getMark(), caret.getDot());
        AbstractDocument doc = (AbstractDocument) textPane.getDocument();
        int lineStart = doc.getParagraphElement(pos).getStartOffset();
        return (pos - lineStart);
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
     * Return the number of the current line.
     */
    private int getCurrentLineIndex(JTextComponent text)
    {
        MoeSyntaxDocument document = (MoeSyntaxDocument) text.getDocument();
        return document.getDefaultRootElement().getElementIndex(text.getCaretPosition());
    }

    // ===================== ACTION IMPLEMENTATION ======================

    /**
     * Do some semi-intelligent indentation. That is: indent the current line to
     * the same depth, using the same characters (TABs or spaces) as the line
     * immediately above.
     */
    private void doIndent(JTextComponent textPane, boolean isNewLine)
    {
        int lineIndex = getCurrentLineIndex(textPane);
        if (lineIndex == 0) { // first line
            if(!isNewLine)
                insertSpacedTab(textPane);
            return;
        }

        MoeSyntaxDocument doc = (MoeSyntaxDocument) textPane.getDocument();

        Element line = getLine(textPane, lineIndex);
        int lineStart = line.getStartOffset();
        int pos = textPane.getCaretPosition();

        try {
            boolean isOpenBrace = false;
            boolean isCommentEnd = false, isCommentEndOnly = false;

            // if there is any text before the cursor, just insert a tab

            String prefix = doc.getText(lineStart, pos - lineStart);
            if (prefix.trim().length() > 0) {
                insertSpacedTab(textPane);
                return;
            }

            // get indentation string from previous line

            boolean foundLine = false;
            int lineOffset = 1;
            String prevLineText = null;
            while ((lineIndex - lineOffset >= 0) && !foundLine) {
                Element prevline = getLine(textPane, lineIndex - lineOffset);
                int prevLineStart = prevline.getStartOffset();
                int prevLineEnd = prevline.getEndOffset();
                prevLineText = doc.getText(prevLineStart, prevLineEnd - prevLineStart);
                if(!isWhiteSpaceOnly(prevLineText)) {
                    foundLine = true;
                }
                else {
                    lineOffset++; 
                }
            }
            if(!foundLine) {
                if(!isNewLine)
                    insertSpacedTab(textPane);
                return;
            }

            if (isOpenBrace(prevLineText))
                isOpenBrace = true;
            else {
                isCommentEnd = prevLineText.trim().endsWith("*/");
                isCommentEndOnly = prevLineText.trim().equals("*/");
            }

            int indentPos = findFirstNonIndentChar(prevLineText, isCommentEnd);

            // if the cursor is already past the indentation point, insert tab
            // (unless we just did a line break, then we just stop)

            int caretColumn = getCurrentColumn(textPane);
            if (caretColumn >= indentPos) {
                if (!isNewLine)
                    insertSpacedTab(textPane);
                return;
            }

            String indent = prevLineText.substring(0, indentPos);

            if (isNewLine && isNewCommentStart(indent, doc, lineStart)) {
                completeNewCommentBlock(textPane, indent);
                return;
            }

            // find and replace indentation of current line

            int lineEnd = line.getEndOffset();
            String lineText = doc.getText(lineStart, lineEnd - lineStart);
            indentPos = findFirstNonIndentChar(lineText, true);
            char firstChar = lineText.charAt(indentPos);
            doc.remove(lineStart, indentPos);
            String newIndent = nextIndent(indent, isOpenBrace, isCommentEndOnly);
            if (firstChar == '*')
            	newIndent = newIndent.replace('*', ' ');
            doc.insertString(lineStart, newIndent, null);
            if(firstChar == '}')
                removeTab(textPane, doc);
        }
        catch (BadLocationException exc) {}
    }

    /**
     * Return true if s contains only whitespace (or nothing).
     */
    private boolean isWhiteSpaceOnly(String s)
    {
        return s.trim().length() == 0;
    }

    /**
     * Do some semi-intelligent de-indentation. That is: indent the current line
     * one indentation level less that the line above, or less than it currently
     * is.
     */
    private void doDeIndent(JTextComponent textPane)
    {
        // set cursor to first non-blank character (or eol if none)
        // if indentation is more than line above: indent as line above
        // if indentation is same or less than line above: indent one level back

        int lineIndex = getCurrentLineIndex(textPane);
        MoeSyntaxDocument doc = (MoeSyntaxDocument) textPane.getDocument();

        try {
            Element line = getLine(textPane, lineIndex);
            int lineStart = line.getStartOffset();
            int lineEnd = line.getEndOffset();
            String lineText = doc.getText(lineStart, lineEnd - lineStart);

            int currentIndentPos = findFirstNonIndentChar(lineText, true);
            char firstChar = lineText.charAt(currentIndentPos);

            textPane.setCaretPosition(lineStart + currentIndentPos);

            if (lineIndex == 0) { // first line
                removeTab(textPane, doc);
                return;
            }

            // get indentation details from previous line

            Element prevline = getLine(textPane, lineIndex - 1);
            int prevLineStart = prevline.getStartOffset();
            int prevLineEnd = prevline.getEndOffset();
            String prevLineText = doc.getText(prevLineStart, prevLineEnd - prevLineStart);

            int targetIndentPos = findFirstNonIndentChar(prevLineText, true);

            if (currentIndentPos > targetIndentPos) {
                // indent same as line above
                String indent = prevLineText.substring(0, targetIndentPos);
                doc.remove(lineStart, currentIndentPos);
                doc.insertString(lineStart, indent, null);
                if(firstChar == '}')
                    removeTab(textPane, doc);
            }
            else {
                // we are at same level as line above or less - go one indentation
                // level back
                removeTab(textPane, doc);
            }
        }
        catch (BadLocationException exc) {}
    }

    /**
     * Check whether the indentation s opens a new multi-line comment
     * @param lineStart The position in the document of the (newly-added) line start
     */
    private boolean isNewCommentStart(String s, MoeSyntaxDocument doc, int lineStart)
    {
        s = s.trim();
        if (s.endsWith("/**") || s.endsWith("/*"))
        {
        	// The user has just pressed enter after the beginning of a comment
        	// We must now decide if their comment was already fine
        	// (and thus we shouldn't add the ending), or if they had, in fact,
        	// begun a new comment (and do need the ending)
        	
        	// Find the comment node that corresponds to our position:
        	NodeAndPosition curNode = doc.getParser().findNodeAt(lineStart, 0);
        	while (curNode != null && !(curNode.getNode() instanceof CommentNode))
        	{
        		curNode = curNode.getNode().findNodeAt(lineStart, curNode.getPosition());
        	}
        	
        	if (curNode == null)
        		//Can't work it out; it's probably a new comment that is unterminated:
        		return true;
        	
        	String comment = getNodeContents(doc, curNode);
        	
        	// If the comment has a comment begin inside it (after the first two characters)
        	// it is likely a new comment that has over-run and matched an ending further
        	// down.  If it has no comment begin inside it, it's probably a pre-existing
        	// valid comment.
        	comment = comment.substring(2);
        	boolean commentHasBeginning = comment.contains("/*");
        	
        	return commentHasBeginning;
        }
        else
        	return false;
    }

    /**
     * Insert text to complete a new, started block comment and place the cursor
     * appropriately.
     * 
     * The indentString passed in always ends with "/*".
     */
    private void completeNewCommentBlock(JTextComponent textPane, String indentString)
    {
        String nextIndent = indentString.substring(0, indentString.length() - 2);
        textPane.replaceSelection(nextIndent + " * ");
        int pos = textPane.getCaretPosition();
        textPane.replaceSelection("\n");
        textPane.replaceSelection(nextIndent + " */");
        textPane.setCaretPosition(pos);
    }

    /**
     * Check whether the given line ends with an opening brace.
     */
    private boolean isOpenBrace(String s)
    {
        int index = s.lastIndexOf('{');
        if (index == -1)
            return false;

        return s.indexOf('}', index + 1) == -1;
    }

    /**
     * Find the position of the first non-indentation character in a string.
     * Indentation characters are <whitespace>, //, *, /*, /**.
     */
    private static int findFirstNonIndentChar(String s, boolean whitespaceOnly)
    {
        int cnt = 0;
        char ch = s.charAt(0);

        // if this line ends a comment, indent whitepace only;
        // otherwise indent across whitespace, asterisks and comment starts

        if (whitespaceOnly) {
            while (ch == ' ' || ch == '\t') { // SPACE or TAB
                cnt++;
                ch = s.charAt(cnt);
            }
        }
        else {
            while (ch == ' ' || ch == '\t' || ch == '*') { // SPACE, TAB or *
                cnt++;
                ch = s.charAt(cnt);
            }
            if ((s.charAt(cnt) == '/') && (s.charAt(cnt + 1) == '*'))
                cnt += 2;
        }
        return cnt;
    }

    /**
     * Transform indentation string to ensure: after " / *" follows " *" after " / * *"
     * follows " *" after " * /" follows ""
     */
    private String nextIndent(String s, boolean openBrace, boolean commentEndOnly)
    {
        // after an opening brace, add some spaces to the indentation
        if (openBrace)
            return s + spaces.substring(0, tabSize);

        if (commentEndOnly)
            return s.substring(0, s.length() - 1);

        if (s.endsWith("/*"))
            return s.substring(0, s.length() - 2) + " * ";

        return s;
    }

    /**
     * Insert a spaced tab at the current caret position in to the textPane.
     */
    private void insertSpacedTab(JTextComponent textPane)
    {
        int numSpaces = tabSize - (getCurrentColumn(textPane) % tabSize);
        textPane.replaceSelection(spaces.substring(0, numSpaces));
    }

    /**
     * Remove characters before the current caret position to take the 
     * caret back to the previous TAB position. No check is made what kind
     * of characters those are - the caller should make sure they can be 
     * removed (usually they should be whitespace).
     */
    private void removeTab(JTextComponent textPane, Document doc)
    throws BadLocationException
    {
        int col = getCurrentColumn(textPane);
        if(col > 0) {
            int remove = col % tabSize;
            if(remove == 0)
                remove = tabSize;
            int pos = textPane.getCaretPosition();
            doc.remove(pos-remove, remove);
        }
    }

    /**
     * Convert all tabs in this text to spaces, maintaining the current
     * indentation.
     * 
     * @param textPane The text pane to convert
     * @return  The number of tab characters converted
     */
    private int convertTabsToSpaces(JTextComponent textPane)
    {
        int count = 0;
        int lineNo = 0;
        AbstractDocument doc = (AbstractDocument) textPane.getDocument();
        Element root = doc.getDefaultRootElement();
        Element line = root.getElement(lineNo);
        try {
            while (line != null) {
                int start = line.getStartOffset();
                int length = line.getEndOffset() - start;
                String text = doc.getText(start, length);
                int startCount = count;
                int tabIndex = text.indexOf('\t');
                while (tabIndex != -1) {
                    text = expandTab(text, tabIndex);
                    count++;
                    tabIndex = text.indexOf('\t');
                }
                if (count != startCount) { // there was a TAB in this line...
                    doc.remove(start, length);
                    doc.insertString(start, text, null);
                }
                lineNo++;
                line = root.getElement(lineNo);
            }
        }
        catch (BadLocationException exc) {
            Debug.reportError("stuffed up in 'convertTabsToSpaces'");
        }
        return count;
    }

    private String expandTab(String s, int idx)
    {
        int numSpaces = tabSize - (idx % tabSize);
        return s.substring(0, idx) + spaces.substring(0, numSpaces) + s.substring(idx + 1);
    }

    /**
     * Insert text from a named template into the editor at the current cursor
     * position. Every line in the template will be indented to the current
     * cursor position (in addition to possible indentation in the template
     * itself), and TAB characters at beginnings of lines in the template will
     * be converted to a spaced tab according to the current tabsize.
     * 
     * @param textPane
     *            The editor pane to enter the text into
     * @param templateName
     *            The name of the template (without path or suffix)
     */
    private void insertTemplate(JTextComponent textPane, String templateName)
    {
        try {
            File template = Config.getTemplateFile(templateName);
            BufferedReader in = new BufferedReader(new FileReader(template));
            int column = getCurrentColumn(textPane);
            if (column > 40)
                column = 40;
            String line = in.readLine();
            while (line != null) {
                while ((line.length() > 0) && (line.charAt(0) == '\t')) {
                    insertSpacedTab(textPane);
                    line = line.substring(1);
                }
                textPane.replaceSelection(line);
                textPane.replaceSelection("\n");
                textPane.replaceSelection(spaces.substring(0, column)); // indent
                line = in.readLine();
            }
            // The position of the caret should be in the right place now.
            // Previously it was set to the position it was at before adding the
            // template, but that resulted in errors when selecting the entire
            // contents of the class before inserting the template.
        }
        catch (IOException exc) {
            Debug.reportError("Could not read method template.");
            Debug.reportError("Exception: " + exc);
        }
    }

    /**
     *  
     */
    private void blockAction(JTextComponent textPane, LineAction lineAction)
    {
        Caret caret = textPane.getCaret();
        int selectionStart = caret.getMark();
        int selectionEnd = caret.getDot();
        if (selectionStart > selectionEnd) {
            int tmp = selectionStart;
            selectionStart = selectionEnd;
            selectionEnd = tmp;
        }
        if (selectionStart != selectionEnd)
            selectionEnd = selectionEnd - 1; // skip last position

        MoeSyntaxDocument doc = (MoeSyntaxDocument) textPane.getDocument();
        Element text = doc.getDefaultRootElement();

        int firstLineIndex = text.getElementIndex(selectionStart);
        int lastLineIndex = text.getElementIndex(selectionEnd);
        for (int i = firstLineIndex; i <= lastLineIndex; i++) {
            Element line = text.getElement(i);
            lineAction.apply(line, doc);
        }

        textPane.setCaretPosition(text.getElement(firstLineIndex).getStartOffset());
        textPane.moveCaretPosition(text.getElement(lastLineIndex).getEndOffset());
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
                new PageSetupAction(), 
                new PrintAction(),
                new CloseAction(),

                undoAction, 
                redoAction, 
                new CommentBlockAction(), 
                new UncommentBlockAction(), 
                new AutoIndentAction(),
                new IndentBlockAction(),
                new DeindentBlockAction(), 
                new InsertMethodAction(), 
                new IndentAction(),
                new DeIndentAction(),
                new NewLineAction(),
                new CopyLineAction(), 
                new CutLineAction(), 
                new CutEndOfLineAction(), 
                new CutWordAction(),
                new CutEndOfWordAction(),

                new FindAction(), 
                findNextAction=new FindNextAction(),
                findNextBackwardAction=new FindNextBackwardAction(),
                new ReplaceAction(),
                compileAction, 
                new GoToLineAction(), 
                new ToggleInterfaceAction(), 
                new ToggleBreakPointAction(),

                new KeyBindingsAction(), 
                new PreferencesAction(),

                new AboutAction(), 
                new DescribeKeyAction(), 
                new HelpMouseAction(), 
                new ShowManualAction(),

                new IncreaseFontAction(),
                new DecreaseFontAction(),

                new ContentAssistAction(),

        };

        // insert all actions into a hashtable

        actions = new Hashtable<Object, Action>();

        Action action;
        for (int i = 0; i < textActions.length; i++) {
            action = textActions[i];
            //Debug.message("a: " + action.getValue(Action.NAME));
            actions.put(action.getValue(Action.NAME), action);
        }
        for (int i = 0; i < myActions.length; i++) {
            action = myActions[i];
            actions.put(action.getValue(Action.NAME), action);
        }

        // sort all actions into a big, ordered table

        actionTable = new Action[] {

                // edit functions
                (Action) (actions.get(DefaultEditorKit.deletePrevCharAction)), // 0
                (Action) (actions.get(DefaultEditorKit.deleteNextCharAction)),
                (Action) (actions.get(DefaultEditorKit.copyAction)),
                (Action) (actions.get(DefaultEditorKit.cutAction)), 
                (Action) (actions.get("copy-line")),
                (Action) (actions.get("cut-line")), 
                (Action) (actions.get("cut-end-of-line")),
                (Action) (actions.get("cut-word")), 
                (Action) (actions.get("cut-end-of-word")),
                (Action) (actions.get(DefaultEditorKit.pasteAction)), 
                (Action) (actions.get("indent")),
                (Action) (actions.get("de-indent")),
                (Action) (actions.get(DefaultEditorKit.insertTabAction)), 
                (Action) (actions.get("new-line")),
                (Action) (actions.get(DefaultEditorKit.insertBreakAction)), 
                (Action) (actions.get("insert-method")),
                (Action) (actions.get("comment-block")), 
                (Action) (actions.get("uncomment-block")),
                (Action) (actions.get("autoindent")), 
                (Action) (actions.get("indent-block")), 
                (Action) (actions.get("deindent-block")),

                (Action) (actions.get(DefaultEditorKit.selectWordAction)), // 21
                (Action) (actions.get(DefaultEditorKit.selectLineAction)),
                (Action) (actions.get(DefaultEditorKit.selectParagraphAction)),
                (Action) (actions.get(DefaultEditorKit.selectAllAction)),
                (Action) (actions.get(DefaultEditorKit.selectionBackwardAction)),
                (Action) (actions.get(DefaultEditorKit.selectionForwardAction)),
                (Action) (actions.get(DefaultEditorKit.selectionUpAction)),
                (Action) (actions.get(DefaultEditorKit.selectionDownAction)),
                (Action) (actions.get(DefaultEditorKit.selectionBeginWordAction)),
                (Action) (actions.get(DefaultEditorKit.selectionEndWordAction)),
                (Action) (actions.get(DefaultEditorKit.selectionPreviousWordAction)), // 31
                (Action) (actions.get(DefaultEditorKit.selectionNextWordAction)),
                (Action) (actions.get(DefaultEditorKit.selectionBeginLineAction)),
                (Action) (actions.get(DefaultEditorKit.selectionEndLineAction)),
                (Action) (actions.get(DefaultEditorKit.selectionBeginParagraphAction)),
                (Action) (actions.get(DefaultEditorKit.selectionEndParagraphAction)),
                (Action) (actions.get("selection-page-up")), 
                (Action) (actions.get("selection-page-down")),
                (Action) (actions.get(DefaultEditorKit.selectionBeginAction)),
                (Action) (actions.get(DefaultEditorKit.selectionEndAction)), 
                (Action) (actions.get("unselect")),

                // move and scroll functions
                (Action) (actions.get(DefaultEditorKit.backwardAction)), // 42
                (Action) (actions.get(DefaultEditorKit.forwardAction)),
                (Action) (actions.get(DefaultEditorKit.upAction)), 
                (Action) (actions.get(DefaultEditorKit.downAction)),
                (Action) (actions.get(DefaultEditorKit.beginWordAction)),
                (Action) (actions.get(DefaultEditorKit.endWordAction)),
                (Action) (actions.get(DefaultEditorKit.previousWordAction)),
                (Action) (actions.get(DefaultEditorKit.nextWordAction)),
                (Action) (actions.get(DefaultEditorKit.beginLineAction)),
                (Action) (actions.get(DefaultEditorKit.endLineAction)),    // 51
                (Action) (actions.get(DefaultEditorKit.beginParagraphAction)),
                (Action) (actions.get(DefaultEditorKit.endParagraphAction)),
                (Action) (actions.get(DefaultEditorKit.pageUpAction)),
                (Action) (actions.get(DefaultEditorKit.pageDownAction)),
                (Action) (actions.get(DefaultEditorKit.beginAction)),
                (Action) (actions.get(DefaultEditorKit.endAction)),

                // class functions
                (Action) (actions.get("save")), // 58
                (Action) (actions.get("reload")), 
                (Action) (actions.get("close")), 
                (Action) (actions.get("print")),
                (Action) (actions.get("page-setup")),

                // customisation functions
                (Action) (actions.get("key-bindings")), // 63
                (Action) (actions.get("preferences")),

                // help functions
                (Action) (actions.get("describe-key")), // 65
                (Action) (actions.get("help-mouse")), 
                (Action) (actions.get("show-manual")),
                (Action) (actions.get("about-editor")),

                // misc functions
                undoAction, // 69
                redoAction, 
                (Action) (actions.get("find")), 
                (Action) (actions.get("find-next")),
                (Action) (actions.get("find-next-backward")), 
                (Action) (actions.get("replace")),
                (Action) (actions.get("compile")), 
                (Action) (actions.get("toggle-interface-view")),
                (Action) (actions.get("toggle-breakpoint")), 
                (Action) (actions.get("go-to-line")),
                (Action) (actions.get("increase-font")),
                (Action) (actions.get("decrease-font")),
                (Action) (actions.get("code-completion")),

        }; // 82

        categories = new String[] { 
                Config.getString("editor.functions.editFunctions"),
                Config.getString("editor.functions.moveScroll"), 
                Config.getString("editor.functions.classFunctions"),
                Config.getString("editor.functions.customisation"), 
                Config.getString("editor.functions.help"),
                Config.getString("editor.functions.misc")
        };

        categoryIndex = new int[] { 0, 42, 58, 63, 65, 69, 82 };
    }

    /**
     * Set up the default key bindings. Used for initial setup, or restoring the
     * default later on.
     */
    public void setDefaultKeyBindings()
    {
        keymap.removeBindings();

        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_S, SHORTCUT_MASK), 
                (Action) (actions.get("save")));
        // "reload" not bound
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_P, SHORTCUT_MASK), 
                (Action) (actions.get("print")));
        // "page-setup" not bound
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_W, SHORTCUT_MASK), (Action) (actions
                .get("close")));
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_Z, SHORTCUT_MASK), (Action) (actions
                .get("undo")));
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_Y, SHORTCUT_MASK), (Action) (actions
                .get("redo")));
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0), (Action) (actions
                .get("comment-block")));
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0), (Action) (actions
                .get("uncomment-block")));
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0), (Action) (actions.get("indent-block")));
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0),
                (Action) (actions.get("deindent-block")));
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_M, SHORTCUT_MASK), 
                (Action) (actions.get("insert-method")));
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), 
                (Action) (actions.get("indent")));
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, Event.SHIFT_MASK), 
                (Action) (actions.get("de-indent")));
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_I, SHORTCUT_MASK), 
                (Action) (actions.get("insert-tab")));
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), 
                (Action) (actions.get("new-line")));
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, Event.SHIFT_MASK), 
                (Action) (actions.get("insert-break")));
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_F, SHORTCUT_MASK), 
                (Action) (actions.get("find")));
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_G, SHORTCUT_MASK), 
                (Action) (actions.get("find-next")));
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_G, SHIFT_SHORTCUT_MASK), (Action) (actions
                .get("find-next-backward")));
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_R, SHORTCUT_MASK), (Action) (actions
                .get("replace")));
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_L, SHORTCUT_MASK), (Action) (actions
                .get("go-to-line")));
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_K, SHORTCUT_MASK), (Action) (actions
                .get("compile")));
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_J, SHORTCUT_MASK), (Action) (actions
                .get("toggle-interface-view")));
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_B, SHORTCUT_MASK), (Action) (actions
                .get("toggle-breakpoint")));
        // "key-bindings" not bound
        // "preferences" not bound
        // "about-editor" not bound
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_D, SHORTCUT_MASK), (Action) (actions
                .get("describe-key")));
        // "help-mouse" not bound
        // "show-manual" not bound

        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_C, SHORTCUT_MASK), (Action) (actions
                .get(DefaultEditorKit.copyAction)));
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_X, SHORTCUT_MASK), (Action) (actions
                .get(DefaultEditorKit.cutAction)));
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_V, SHORTCUT_MASK), (Action) (actions
                .get(DefaultEditorKit.pasteAction)));

        // F2, F3, F4
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), (Action) (actions.get("copy-line")));
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0), (Action) (actions
                .get(DefaultEditorKit.pasteAction)));
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0), (Action) (actions.get("cut-line")));

        // cursor block
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_UP, ALT_SHORTCUT_MASK), (Action) (actions
                .get(DefaultEditorKit.pasteAction)));
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, ALT_SHORTCUT_MASK), (Action) (actions
                .get(DefaultEditorKit.deletePrevCharAction)));
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, ALT_SHORTCUT_MASK), (Action) (actions
                .get(DefaultEditorKit.deleteNextCharAction)));
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, SHIFT_ALT_SHORTCUT_MASK),
                (Action) (actions.get("cut-line")));
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, SHIFT_ALT_SHORTCUT_MASK),
                (Action) (actions.get("cut-end-of-line")));
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, DOUBLE_SHORTCUT_MASK), (Action) (actions
                .get("cut-word")));
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, DOUBLE_SHORTCUT_MASK), (Action) (actions
                .get("cut-end-of-word")));
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, SHORTCUT_MASK), (Action) (actions
                .get("increase-font")));
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, SHORTCUT_MASK), (Action) (actions
                .get("decrease-font")));
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, SHORTCUT_MASK), (Action) (actions
                .get("code-completion")));

    }

    /**
     * Interface LineAction - a superclass for all line actions. Line actions
     * manipulate a single line of text and are used by the blockAction method.
     * The blockAction applies a LineAction to each line in a block of text.
     */
    interface LineAction
    {
        /**
         * Apply some action to a line in the document.
         */
        public void apply(Element line, MoeSyntaxDocument doc);
    }



    /**
     * Class CommentLineAction - add a comment symbol to the given line.
     */
    class CommentLineAction
    implements LineAction
    {
        /**
         * Comment the given line
         */
        public void apply(Element line, MoeSyntaxDocument doc)
        {
            int lineStart = line.getStartOffset();
            try {
                doc.insertString(lineStart, "// ", null);
            }
            catch (Exception exc) {}
        }
    }


    /**
     * Class UncommentLineAction - remove the comment symbol (if any) from the
     * given line.
     */
    class UncommentLineAction
    implements LineAction
    {
        public void apply(Element line, MoeSyntaxDocument doc)
        {
            int lineStart = line.getStartOffset();
            int lineEnd = line.getEndOffset();
            try {
                String lineText = doc.getText(lineStart, lineEnd - lineStart);
                if (lineText.trim().startsWith("//")) {
                    int cnt = 0;
                    while (lineText.charAt(cnt) != '/')
                        // whitespace chars
                        cnt++;
                    if (lineText.charAt(cnt + 2) == ' ')
                        doc.remove(lineStart, cnt + 3);
                    else
                        doc.remove(lineStart, cnt + 2);
                }
            }
            catch (Exception exc) {}
        }
    }

    /**
     * Class IndentLineAction - add one level of indentation to the given line.
     */
    class IndentLineAction
    implements LineAction
    {
        public void apply(Element line, MoeSyntaxDocument doc)
        {
            int lineStart = line.getStartOffset();
            try {
                doc.insertString(lineStart, spaces.substring(0, tabSize), null);
            }
            catch (Exception exc) {}
        }
    }

    /**
     * Class DeindentLineAction - remove one indentation level from the given
     * line.
     */
    class DeindentLineAction
    implements LineAction
    {
        public void apply(Element line, MoeSyntaxDocument doc)
        {
            int lineStart = line.getStartOffset();
            int lineEnd = line.getEndOffset();
            try {
                String lineText = doc.getText(lineStart, lineEnd - lineStart);
                String spacedTab = spaces.substring(0, tabSize);
                if (lineText.startsWith(spacedTab))
                    doc.remove(lineStart, tabSize); // remove spaced tab
                else if (lineText.charAt(0) == TAB_CHAR)
                    doc.remove(lineStart, 1); // remove hard tab
                else {
                    int cnt = 0;
                    while (lineText.charAt(cnt) == ' ')
                        // remove spaces
                        cnt++;
                    doc.remove(lineStart, cnt);
                }
            }
            catch (Exception exc) {}
        }
    }

    /**
     * Class KeyCatcher - used for implementation of "describe-key" command to
     * catch the next key press so that we can see what it does.
     */
    class KeyCatcher extends KeyAdapter
    {
        MoeEditor editor;

        public void keyPressed(KeyEvent e)
        {
            int keyCode = e.getKeyCode();

            if (keyCode == KeyEvent.VK_CAPS_LOCK || // the keys we want to
                    // ignore...
                    keyCode == KeyEvent.VK_SHIFT || keyCode == KeyEvent.VK_CONTROL || keyCode == KeyEvent.VK_META
                    || keyCode == KeyEvent.VK_ALT || keyCode == KeyEvent.VK_ALT_GRAPH || keyCode == KeyEvent.VK_COMPOSE
                    || keyCode == KeyEvent.VK_NUM_LOCK || keyCode == KeyEvent.VK_SCROLL_LOCK
                    || keyCode == KeyEvent.VK_UNDEFINED)
                return;

            KeyStroke key = KeyStroke.getKeyStrokeForEvent(e);
            String modifierName = KeyEvent.getKeyModifiersText(key.getModifiers());
            String keyName = KeyEvent.getKeyText(keyCode);
            if (modifierName.length() > 0)
                keyName = modifierName + "+" + keyName;

            Keymap map = keymap;
            Action action = null;

            while (map != null && action == null) {
                action = map.getAction(key);
                map = map.getResolveParent();
            }

            if (action == null) {
                // BUG workaround: bindings inhertited from component are not
                // found
                // through the keymap. we search for them explicitly here...
                Object binding = componentInputMap.get(key);
                if (binding == null){
                    //editor.writeMessage(keyName + " is not bound to a function.");
                    editor.writeMessage(keyName + Config.getString("editor.keypressed.keyIsNotBound"));
                }
                else {
                    editor.writeMessage(keyName + Config.getString("editor.keypressed.callsTheFunction") + binding + "\"");
                }
            }
            else {
                String name = (String) action.getValue(Action.NAME);
                editor.writeMessage(keyName + Config.getString("editor.keypressed.callsTheFunction") + name + "\"");
            }
            e.getComponent().removeKeyListener(keyCatcher);
            e.consume();
        }

        public void setEditor(MoeEditor ed)
        {
            editor = ed;
        }

    }


    // ---------------------------------------
    // Indent calculation:
    

    /**
     * An interface that calculates the indentation level that
     * the corresponding node should have.  You should use getForChild as you
     * descend the parse tree to get the indentation for child nodes.
     */
    private static interface IndentCalculator
    {
    	/**
    	 * Gets the IndentCalculator for the given child node of the node that this
    	 * IndentCalculator instance corresponds to
    	 */
    	public IndentCalculator getForChild(ParsedNode n);
    	/**
    	 * Gets the indent for a line in the current node that begins with the
    	 * given character.  This allows for comments (such as this one right here)
    	 * to have their leading asterisks indented by an extra space. 
    	 */
    	public String getCurIndent(char beginsWith);
    }
    
    /**
     * An implementation of IndentCalculator for the root node of the document.
     */
	private static class RootIndentCalculator implements IndentCalculator
	{
		public IndentCalculator getForChild(ParsedNode n)
		{
			return new NodeIndentCalculator("", n);
		}

		public String getCurIndent(char beginsWith)
		{
			return "";
		}
	}
	
	private static class NodeIndentCalculator implements IndentCalculator
	{
		private final String existingIndent;
		private final ParsedNode parent;

		private final static String STANDARD_INDENT = "    ";
		private final static String CONTINUATION_INDENT = "    ";
		// To make it line up like this:
		// /**
		//  *
		//  *
		//  */
		// This must be a single space:
		private final static String COMMENT_ASTERISK_INDENT = " ";

		public NodeIndentCalculator(String existingIndent, ParsedNode parent)
		{
			this.existingIndent = existingIndent;
			this.parent = parent;
		}

		public IndentCalculator getForChild(ParsedNode child)
		{
			String newIndent = existingIndent;
			
			// I realise that using instanceof is sinful, but because I need
			// to know the type of both the parent and the child node, there is no
			// easy way to fold this method into either the parent or child node type
			// (either would still use instanceof on the other), so I'm keeping
			// it here for now:			
			
			if (child instanceof TypeInnerNode)
				newIndent += STANDARD_INDENT;
			else if (parent instanceof MethodNode
					&& !(child instanceof CommentNode))
				// comments that are children of methods are actually the comment
				// before the method, and thus shouldn't be indented any differently
				newIndent += STANDARD_INDENT;
			else if (parent instanceof ContainerNode)
				newIndent += STANDARD_INDENT;
			else if (parent instanceof ExpressionNode
					&& child instanceof ExpressionNode)
				// Expressions that are children of expressions are function arguments,
				// and thus use the continuation indent:
				newIndent += CONTINUATION_INDENT;

			return new NodeIndentCalculator(newIndent, child);
		}

		public String getCurIndent(char beginsWith)
		{
			if (parent instanceof CommentNode && beginsWith == '*')
				return existingIndent + COMMENT_ASTERISK_INDENT;
			else
				return existingIndent;
		}
	}
	
	interface DocumentAction
	{
		public void apply(MoeSyntaxDocument doc);

	}

	private static class DocumentRemoveLineAction implements DocumentAction
	{
		private Element lineToRemove;

		public DocumentRemoveLineAction(Element lineToRemove)
		{
			this.lineToRemove = lineToRemove;
		}

		public void apply(MoeSyntaxDocument doc)
		{
			try {
				doc.remove(lineToRemove.getStartOffset(), lineToRemove.getEndOffset() - lineToRemove.getStartOffset());
			}
			catch (BadLocationException e) {
				Debug.reportError("Problem while trying to remove line from document: "
						+ lineToRemove.getStartOffset() + "->" + lineToRemove.getEndOffset()
						+ " in document of size " + doc.getLength(), e);
			}
		}
	}

	/**
	 * A class representing an update to the indentation on a line of the document.  This is different
	 * to a LineAction because it intrinsically knows which line it needs to update
	 */
    private static class DocumentIndentAction implements DocumentAction
    {
    	private Element el;
    	private String indent;
    	
    	public DocumentIndentAction(Element el, String indent)
    	{
			this.el = el;
			this.indent = indent;
		}

		// Because we keep element references, we don't have to worry about the offsets
    	// altering, because they will alter before we process the line, and thus
    	// everything works nicely.
    	public void apply(MoeSyntaxDocument doc)
    	{
			String line = getElementContents(doc, el);
			int lengthPrevWhitespace = findFirstNonIndentChar(line, true);
			boolean anyTabs = false;
			for (char c : line.substring(0, lengthPrevWhitespace).toCharArray()) {
				if (c == '\t')
					anyTabs = true;
			}
    		// If we want to put in 4 spaces, and there are already exactly 4 tabs,
    		// without the anyTabs check, we would leave the whitespace alone;
    		// hence why we need the check:
			if (indent != null && (anyTabs || (indent.length() != lengthPrevWhitespace))) {
				try {
					doc.replace(el.getStartOffset(), lengthPrevWhitespace,
						        indent, null);
				}
				catch (BadLocationException e) {
					Debug.reportError("Error doing indent in DocumentUpdate", e);
				}
			}
    	}
    }
    
    private static String getElementContents(MoeSyntaxDocument doc, Element el)
    {
		try {
			return doc.getText(el.getStartOffset(), el.getEndOffset() - el.getStartOffset());
		}
		catch (BadLocationException e) {
			Debug.reportError("Error getting element contents in document", e);
			return "";
		}
    }
    
    private static String getNodeContents(MoeSyntaxDocument doc, NodeAndPosition nap)
    {
		try {
			return doc.getText(nap.getPosition(), nap.getSize());
		}
		catch (BadLocationException e) {
			Debug.reportError("Error getting node contents in document", e);
			return "";
		}
    }

}
