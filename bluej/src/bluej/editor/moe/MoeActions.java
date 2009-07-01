/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
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
// Copyright (c) 2000, 2005 BlueJ Group, Deakin University
//
// This software is made available under the terms of the "MIT License"
// A copy of this license is included with this source distribution
// in "license.txt" and is also available at:
// http://www.opensource.org/licenses/mit-license.html 
// Any queries should be directed to Michael Kolling mik@bluej.org

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
    private int[] categoryIndex;

    private Keymap keymap; // the editor's keymap
    private KeyCatcher keyCatcher;

    private boolean lastActionWasCut; // true if last action was a cut action
    // undo helpers
    public UndoAction undoAction;
    public RedoAction redoAction;

    // frequently needed actions
    public Action compileAction;

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
            if (version <252) {
                keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, SHORTCUT_MASK), (Action) (actions
                        .get("increase-font")));
                keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, SHORTCUT_MASK), (Action) (actions
                        .get("decrease-font")));

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

    /**
     * Get a keystroke for an action by action name. Return null is there is
     * none.
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
            //PrefMgr.setEditorFontSize(newFont);
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
            //PrefMgr.setEditorFontSize(newFont);
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
            getEditor(e).find();
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
            getEditor(e).findNext();
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
            getEditor(e).findNextBackward();
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
            getEditor(e).replace();
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
            FunctionDialog dlg = new FunctionDialog(getEditor(e), actionTable, categories, categoryIndex);

            dlg.setVisible(true);
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
     * Return the current line.
     */
    //    private Element getCurrentLine(JTextComponent text)
    //    {
    //        MoeSyntaxDocument document = (MoeSyntaxDocument)text.getDocument();
    //        return document.getParagraphElement(text.getCaretPosition());
    //    }
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
     * Find and return a line by text position
     */
    //private Element getLineAt(JTextComponent text, int pos)
    //{
    //    MoeSyntaxDocument document = (MoeSyntaxDocument) text.getDocument();
    //    return document.getParagraphElement(pos);
    //}

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

            if (isNewLine && isCommentStart(indent)) {
                completeNewCommentBlock(textPane, indent);
                return;
            }

            // find and replace indentation of current line

            int lineEnd = line.getEndOffset();
            String lineText = doc.getText(lineStart, lineEnd - lineStart);
            indentPos = findFirstNonIndentChar(lineText, true);
            char firstChar = lineText.charAt(indentPos);
            doc.remove(lineStart, indentPos);
            doc.insertString(lineStart, nextIndent(indent, isOpenBrace, isCommentEndOnly), null);
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
     */
    private boolean isCommentStart(String s)
    {
        s = s.trim();
        return s.endsWith("/**") || s.endsWith("/*");
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
    private int findFirstNonIndentChar(String s, boolean whitespaceOnly)
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
                new FindNextAction(), 
                new FindNextBackwardAction(), 
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
                (Action) (actions.get("indent-block")), 
                (Action) (actions.get("deindent-block")),

                (Action) (actions.get(DefaultEditorKit.selectWordAction)), // 20
                (Action) (actions.get(DefaultEditorKit.selectLineAction)),
                (Action) (actions.get(DefaultEditorKit.selectParagraphAction)),
                (Action) (actions.get(DefaultEditorKit.selectAllAction)),
                (Action) (actions.get(DefaultEditorKit.selectionBackwardAction)),
                (Action) (actions.get(DefaultEditorKit.selectionForwardAction)),
                (Action) (actions.get(DefaultEditorKit.selectionUpAction)),
                (Action) (actions.get(DefaultEditorKit.selectionDownAction)),
                (Action) (actions.get(DefaultEditorKit.selectionBeginWordAction)),
                (Action) (actions.get(DefaultEditorKit.selectionEndWordAction)),
                (Action) (actions.get(DefaultEditorKit.selectionPreviousWordAction)), // 30
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

                (Action) (actions.get(DefaultEditorKit.backwardAction)), // 41
                (Action) (actions.get(DefaultEditorKit.forwardAction)),
                (Action) (actions.get(DefaultEditorKit.upAction)), 
                (Action) (actions.get(DefaultEditorKit.downAction)),
                (Action) (actions.get(DefaultEditorKit.beginWordAction)),
                (Action) (actions.get(DefaultEditorKit.endWordAction)),
                (Action) (actions.get(DefaultEditorKit.previousWordAction)),
                (Action) (actions.get(DefaultEditorKit.nextWordAction)),
                (Action) (actions.get(DefaultEditorKit.beginLineAction)),
                (Action) (actions.get(DefaultEditorKit.endLineAction)),    // 50
                (Action) (actions.get(DefaultEditorKit.beginParagraphAction)),
                (Action) (actions.get(DefaultEditorKit.endParagraphAction)),
                (Action) (actions.get(DefaultEditorKit.pageUpAction)),
                (Action) (actions.get(DefaultEditorKit.pageDownAction)),
                (Action) (actions.get(DefaultEditorKit.beginAction)),
                (Action) (actions.get(DefaultEditorKit.endAction)),

                // class functions
                (Action) (actions.get("save")), // 57
                (Action) (actions.get("reload")), 
                (Action) (actions.get("close")), 
                (Action) (actions.get("print")),
                (Action) (actions.get("page-setup")),

                // customisation functions
                (Action) (actions.get("key-bindings")), // 62
                (Action) (actions.get("preferences")),

                // help functions
                (Action) (actions.get("describe-key")), // 64
                (Action) (actions.get("help-mouse")), 
                (Action) (actions.get("show-manual")),
                (Action) (actions.get("about-editor")),

                // misc functions
                undoAction, // 68
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
                
        }; // 80

        categories = new String[] { 
                Config.getString("editor.functions.editFunctions"),
                Config.getString("editor.functions.moveScroll"), 
                Config.getString("editor.functions.classFunctions"),
                Config.getString("editor.functions.customisation"), 
                Config.getString("editor.functions.help"),
                Config.getString("editor.functions.misc")
        };

        categoryIndex = new int[] { 0, 41, 57, 62, 64, 68, 80 };
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
        keymap
                .addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0), (Action) (actions
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

} // end class MoeActions
