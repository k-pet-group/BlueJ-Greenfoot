/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2010,2011,2012,2013,2014  Michael Kolling and John Rosenberg 

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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
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
import bluej.debugger.gentype.JavaType;
import bluej.editor.moe.MoeIndent.AutoIndentInformation;
import bluej.parser.entity.JavaEntity;
import bluej.parser.nodes.CommentNode;
import bluej.parser.nodes.MethodNode;
import bluej.parser.nodes.NodeTree.NodeAndPosition;
import bluej.parser.nodes.ParsedNode;
import bluej.prefmgr.PrefMgr;
import bluej.prefmgr.PrefMgrDialog;
import bluej.utility.Debug;

/**
 * A set of actions supported by the Moe editor. This is a singleton: the
 * actions are shared between all editor instances.
 * 
 * Actions are stored both in a hash-map and in an array. The hash-map is used
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
    private HashMap<Object, Action> actions; // the same actions in a hash-map
    private String[] categories;
    public Action[] getActionTable()
    {
        return actionTable;
    }

    public void setActionTable(Action[] actionTable)
    {
        this.actionTable = actionTable;
    }

    public String[] getCategories()
    {
        return categories;
    }

    public void setCategories(String[] categories)
    {
        this.categories = categories;
    }

    public int[] getCategoryIndex()
    {
        return categoryIndex;
    }

    public void setCategoryIndex(int[] categoryIndex) 
    {
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

    private Action[] overrideActions;

    /**
     * Get the actions object (a singleton) and, at the same time, install the
     * action keymap as the main keymap for the given textComponent..
     */
    public static MoeActions getActions(JTextComponent textComponent)
    {
        if (moeActions == null) {
            moeActions = new MoeActions(textComponent);
        }

        if (textComponent != null) {
            textComponent.setKeymap(moeActions.keymap);
            moeActions.overrideActions(textComponent);
        }
       
        return moeActions;
    }

    // ========================== INSTANCE METHODS ==========================

    private void overrideActions(JTextComponent textComponent)
    {       
        for (Action action : overrideActions) {
            textComponent.getActionMap().put(action.getValue(Action.NAME), action);
        }
    }

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
        Keymap origKeymap = textComponent.getKeymap();
        keymap = JTextComponent.addKeymap("BlueJ map", origKeymap);

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
        return actions.get(name);
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
                if (keymap.getAction(i.next()) != null) {
                    i.remove();
                }
            }
        }

        // merge found keystrokes into key array
        if ((keyStrokes == null) || (keyStrokes.isEmpty())) {
            return keys;
        }

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
                istream.close();
                return false;
            }

            for (int i = 0; i < count; i++) {
                KeyStroke key = (KeyStroke) stream.readObject();
                String actionName = (String) stream.readObject();
                Action action = actions.get(actionName);
                if (action != null) {
                    keymap.addActionForKeyStroke(key, action);
                }
            }
            istream.close();

            // set up bindings for new actions in recent releases

            if (version < 252) {
                keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, SHORTCUT_MASK), actions.get("increase-font"));
                keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, SHORTCUT_MASK), actions.get("decrease-font"));
            }
            if (version < 300) {
                keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, Event.CTRL_MASK), actions.get("code-completion"));
                keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_I, SHIFT_SHORTCUT_MASK ), actions.get("autoindent"));
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

                if (ch == '}' && PrefMgr.getFlag(PrefMgr.AUTO_INDENT)) {
                    closingBrace(textPane, doc, offset);
                }
            }
        }
        catch (BadLocationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * We just typed a closing brace character - indent appropriately.
     */
    private void closingBrace(JTextComponent textPane, Document doc, int offset) throws BadLocationException
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
            if (ed != null) {
                ed.clearMessage();
            }
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

        @Override
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

        @Override
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

        @Override
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

        @Override
        public void actionPerformed(ActionEvent e)
        {
            MoeEditor.pageSetup();
        }
    }

    // --------------------------------------------------------------------

    class CloseAction extends MoeAbstractAction
    {

        public CloseAction()
        {
            super("close");
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            getEditor(e).close();
        }
    }

    // === Edit: ===
    // --------------------------------------------------------------------

    public class UndoAction extends MoeAbstractAction
    {

        public UndoAction()
        {
            super("undo");
            this.setEnabled(false);
        }

        @Override
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

    public class RedoAction extends MoeAbstractAction
    {

        public RedoAction()
        {
            super("redo");
            this.setEnabled(false);
        }

        @Override
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

        @Override
        public void actionPerformed(ActionEvent e)
        {
            MoeEditor editor = getEditor(e);
            editor.undoManager.beginCompoundEdit();
            blockAction(editor, new CommentLineAction());
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

        @Override
        public void actionPerformed(ActionEvent e)
        {
            MoeEditor editor = getEditor(e);
            editor.undoManager.beginCompoundEdit();
            blockAction(editor, new UncommentLineAction());
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

        @Override
        public void actionPerformed(ActionEvent e)
        {
            doBlockIndent(getEditor(e));
        }
    }

    // --------------------------------------------------------------------

    class DeindentBlockAction extends MoeAbstractAction
    {

        public DeindentBlockAction()
        {
            super("deindent-block");
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            doBlockDeIndent(getEditor(e));
        }
    }

    // --------------------------------------------------------------------
    
    class AutoIndentAction extends MoeAbstractAction
    {
        public AutoIndentAction()
        {
            super("autoindent");
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            MoeEditor editor = getEditor(e);
            MoeSyntaxDocument doc = editor.getSourceDocument();
            if (doc.getParsedNode() == null) {
                // The Readme, or some other file which isn't parsed
                return;
            }

            int prevCaretPos = editor.getSourcePane().getCaretPosition();
            editor.setCaretActive(false);
            editor.undoManager.beginCompoundEdit();
            AutoIndentInformation info = MoeIndent.calculateIndentsAndApply(doc, prevCaretPos);
            editor.undoManager.endCompoundEdit();
            editor.setCaretPositionForward(info.getNewCaretPosition() - prevCaretPos);
            editor.setCaretActive(true);
            
            if (info.isPerfect()) {
                editor.writeMessage(Config.getString("editor.info.perfectIndent"));
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

        @Override
        public void actionPerformed(ActionEvent e)
        {
            MoeEditor editor = getEditor(e);
            //this method should not be actioned if the editor is not displaying source code
            if (!editor.containsSourceCode()){
                return;
            }
            editor.undoManager.beginCompoundEdit();
            insertTemplate(getTextComponent(e), editor, "method");
            editor.undoManager.endCompoundEdit();
        }
    }

    // --------------------------------------------------------------------

    class AddJavadocAction extends MoeAbstractAction
    {
        public AddJavadocAction()
        {
            super ("add-javadoc");
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            MoeEditor editor = getEditor(e);
            //this method should not be actioned if the editor is not displaying source code
            if (!editor.containsSourceCode()){
                return;
            }
            int caretPos = editor.getCurrentTextPane().getCaretPosition();
            NodeAndPosition<ParsedNode> node = editor.getParsedNode().findNodeAt(caretPos, 0);
            while (node != null && node.getNode().getNodeType() != ParsedNode.NODETYPE_METHODDEF) {
                node = node.getNode().findNodeAt(caretPos, node.getPosition());
            }
            if (node == null || !(node.getNode() instanceof MethodNode)) {
                editor.writeMessage(Config.getString("editor.addjavadoc.notAMethod"));
            } else {
                MethodNode methodNode = ((MethodNode)node.getNode());
                
                boolean hasJavadocComment = false;
                Iterator<NodeAndPosition<ParsedNode>> it = methodNode.getChildren(node.getPosition());
                while (it.hasNext()) {
                    ParsedNode subNode = it.next().getNode();
                    if (subNode instanceof CommentNode) {
                        hasJavadocComment = hasJavadocComment || ((CommentNode)subNode).isJavadocComment();
                    }
                }
                
                if (hasJavadocComment) {
                    editor.writeMessage(Config.getString("editor.addjavadoc.hasJavadoc"));
                } else {
                    StringBuilder indent = new StringBuilder();
                    int column = editor.getLineColumnFromOffset(node.getPosition()).getColumn();
                    for (int i = 0;i < column-1;i++)
                        indent.append(' ');
                    StringBuilder newComment = new StringBuilder();
                    newComment.append("/**\n");
                    
                    JavaEntity retTypeEntity = methodNode.getReturnType();
                    
                    if (retTypeEntity == null) {
                        // It's a constructor:
                        newComment.append(indent).append(" * ").append(methodNode.getName()).append(" ");
                        newComment.append(Config.getString("editor.addjavadoc.constructor")).append("\n");
                    } else {
                        // It's a method:
                        newComment.append(indent).append(" * ").append(Config.getString("editor.addjavadoc.method"));
                        newComment.append(" ").append(methodNode.getName()).append("\n");
                    }
                    newComment.append(indent).append(" *\n");

                    for (String s: methodNode.getParamNames()) {
                        newComment.append(indent).append(" * @param ").append(s).append(" ");
                        newComment.append(Config.getString("editor.addjavadoc.parameter")).append("\n");
                    }
                    
                    if (retTypeEntity != null) {
                        JavaType retType = retTypeEntity.resolveAsType().getType();
                        if (retType != null && !retType.isVoid()) {
                            newComment.append(indent).append(" * @return ");
                            newComment.append(Config.getString("editor.addjavadoc.returnValue")).append("\n");
                        }
                    }
                    
                    newComment.append(indent).append(" */\n").append(indent);
                    
                    editor.undoManager.beginCompoundEdit();
                    editor.getCurrentTextPane().setCaretPosition(node.getPosition());
                    editor.getCurrentTextPane().replaceSelection(newComment.toString());
                    editor.getCurrentTextPane().setCaretPosition((caretPos + newComment.length()));
                    editor.undoManager.endCompoundEdit();
                }
            }
        }
    }

    // --------------------------------------------------------------------

    class IndentAction extends MoeAbstractAction
    {

        public IndentAction()
        {
            super("indent");
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            JTextComponent textPane = getTextComponent(e);
            MoeEditor ed = getEditor(e);

            if(haveSelection(textPane)) {
                doBlockIndent(ed);
            }
            else {
                // if necessary, convert all TABs in the current editor to spaces
                int converted = 0;
                if (ed.checkExpandTabs()) {
                    // do TABs need expanding?
                    ed.setCaretActive(false);
                    converted = convertTabsToSpaces(textPane);
                    ed.setCaretActive(true);
                }

                if (PrefMgr.getFlag(PrefMgr.AUTO_INDENT)) {
                    doIndent(textPane, false);
                }
                else {
                    insertSpacedTab(textPane);
                }

                if (converted > 0) {
                    ed.writeMessage(Config.getString("editor.info.tabsExpanded"));
                }
            }
        }
    }

    // --------------------------------------------------------------------

    class DeIndentAction extends MoeAbstractAction
    {
        public DeIndentAction()
        {
            super("de-indent");
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            JTextComponent textPane = getTextComponent(e);
            MoeEditor ed = getEditor(e);

            if(haveSelection(textPane)) {
                doBlockDeIndent(ed);
            }
            else {
                // if necessary, convert all TABs in the current editor to spaces
                if (ed.checkExpandTabs()) { // do TABs need expanding?
                    ed.setCaretActive(false);
                    int converted = convertTabsToSpaces(textPane);
                    ed.setCaretActive(true);

                    if (converted > 0)
                        ed.writeMessage(Config.getString("editor.info.tabsExpanded"));
                }
                doDeIndent(textPane);
            }
        }
    }

    // --------------------------------------------------------------------

    class NewLineAction extends MoeAbstractAction
    {
        public NewLineAction()
        {
            super("new-line");
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {

            Action action = actions.get(DefaultEditorKit.insertBreakAction);
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

        @Override
        public void actionPerformed(ActionEvent e)
        {
            boolean addToClipboard = lastActionWasCut;
            getActionByName("caret-begin-line").actionPerformed(e);
            getActionByName("selection-down").actionPerformed(e);
            if (addToClipboard) {
                addSelectionToClipboard(getTextComponent(e));
            }
            else {
                getActionByName("copy-to-clipboard").actionPerformed(e);
            }
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

        @Override
        public void actionPerformed(ActionEvent e)
        {
            boolean addToClipboard = lastActionWasCut;
            getActionByName("caret-begin-line").actionPerformed(e);
            getActionByName("selection-down").actionPerformed(e);
            if (addToClipboard) {
                addSelectionToClipboard(getTextComponent(e));
                getActionByName("delete-previous").actionPerformed(e);
            }
            else {
                getActionByName("cut-to-clipboard").actionPerformed(e);
            }
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

        @Override
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

        @Override
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

        @Override
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
            else {
                getActionByName("cut-to-clipboard").actionPerformed(e);
            }
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

        @Override
        public void actionPerformed(ActionEvent e)
        {
            boolean addToClipboard = lastActionWasCut;
            getActionByName("caret-previous-word").actionPerformed(e);
            getActionByName("selection-next-word").actionPerformed(e);
            if (addToClipboard) {
                addSelectionToClipboard(getTextComponent(e));
                getActionByName("delete-previous").actionPerformed(e);
            }
            else {
                getActionByName("cut-to-clipboard").actionPerformed(e);
            }
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

        @Override
        public void actionPerformed(ActionEvent e)
        { 
            MoeEditor editor = getEditor(e);
            if (Config.getPropBoolean("bluej.editor.codecompletion", true)){
                editor.createContentAssist();
            }
        }
    }

    // --------------------------------------------------------------------

    class CutEndOfWordAction extends MoeAbstractAction
    {
        public CutEndOfWordAction()
        {
            super("cut-end-of-word");
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            boolean addToClipboard = lastActionWasCut;
            getActionByName("selection-next-word").actionPerformed(e);
            if (addToClipboard) {
                addSelectionToClipboard(getTextComponent(e));
                getActionByName("delete-previous").actionPerformed(e);
            }
            else {
                getActionByName("cut-to-clipboard").actionPerformed(e);
            }
            lastActionWasCut = true;
        }
    }
    
    // --------------------------------------------------------------------
    
    private abstract class MoeActionWithOrWithoutSelection extends MoeAbstractAction
    {
        private boolean withSelection;
        
        protected MoeActionWithOrWithoutSelection(String actionName, boolean withSelection)
        {
            super(actionName);
            this.withSelection = withSelection;
        }

        protected void moveCaret(JTextComponent c, int pos)
        {
            if (withSelection) {
                c.getCaret().moveDot(pos);
            }
            else {
                c.setCaretPosition(pos);
            }
        }       
    }
    
    // --------------------------------------------------------------------
    
    class NextWordAction extends MoeActionWithOrWithoutSelection
    {
        public NextWordAction(boolean withSelection)
        {
            super(withSelection ? DefaultEditorKit.selectionNextWordAction : DefaultEditorKit.nextWordAction, withSelection);
        }
        
        @Override
        public void actionPerformed(ActionEvent e)
        {
            JTextComponent c = getTextComponent(e);
            int origPos = c.getCaret().getDot();
            int end = findWordLimit(c, origPos, true);
            try {
                if (Character.isWhitespace(c.getText(end, 1).charAt(0))) {
                    // Whitespace region follows, find the end of it:
                    int endOfWS = findWordLimit(c, end, true);
                    moveCaret(c, endOfWS);
                }
                else {
                    // A different "word" follows immediately, stay where we are:
                    moveCaret(c, end);
                }
            }
            catch (BadLocationException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
    
    
    private static int findWordLimit(JTextComponent c, int pos, boolean forwards)
    {
        int maxLen = c.getDocument().getLength();
        if (forwards && pos >= maxLen) return maxLen;
        if (! forwards && pos <= 0) return 0;
        
        try {
            char curChar = c.getText(pos, 1).charAt(0);
            if (Character.isWhitespace(curChar)) { 
                while (Character.isWhitespace(curChar)) {
                    if (forwards) pos++; else pos--;
                    if (pos == maxLen) return pos;
                    if (pos == 0) return 0;
                    curChar = c.getText(pos, 1).charAt(0);
                }
                // If we are going back, we'll have gone one character too far
                // so adjust for that; but if going forwards, the limit is exclusive
                return forwards ? pos : pos + 1;
            }
            else if (Character.isJavaIdentifierPart(curChar)) {
                while (Character.isJavaIdentifierPart(curChar)) {
                    if (forwards) pos++; else pos--;
                    if (pos == maxLen) return pos;
                    if (pos == 0) return 0;
                    curChar = c.getText(pos, 1).charAt(0);
                }
                // If we are going back, we'll have gone one character too far
                // so adjust for that; but if going forwards, the limit is exclusive
                return forwards ? pos : pos + 1;
            }
            else {
                // Can't form an identifier, isn't a space, therefore
                // this char is a word by itself.  If we're looking for the start,
                // this is it, and the end is one character on 
                return forwards ? pos + 1 : pos;
            }
        }
        catch (BadLocationException e) {
            throw new RuntimeException(e);
        }
    }

    // --------------------------------------------------------------------    
    
    class PrevWordAction extends MoeActionWithOrWithoutSelection
    {       
        public PrevWordAction(boolean withSelection)
        {
            super(withSelection ? DefaultEditorKit.selectionPreviousWordAction : DefaultEditorKit.previousWordAction, withSelection);
        }
        
        @Override
        public void actionPerformed(ActionEvent e)
        {
            JTextComponent c = getTextComponent(e);
            int origPos = c.getCaret().getDot();
            if (origPos == 0) return;
            try {
                if (Character.isWhitespace(c.getText(origPos - 1, 1).charAt(0))) {
                    // Whitespace region precedes, find the beginning of it:
                    int startOfWS = findWordLimit(c, origPos - 1, false);
                    int startOfPrevWord = findWordLimit(c, startOfWS - 1, false);
                    moveCaret(c, startOfPrevWord);
                }
                else {
                    // We're in the middle of a word already, find the start:
                    int startOfWord = findWordLimit(c, origPos - 1, false);
                    moveCaret(c, startOfWord);
                }
            }
            catch (BadLocationException ex) {
                throw new RuntimeException(ex);
            }            
        }
    }
    
    // --------------------------------------------------------------------
    
    class EndWordAction extends MoeActionWithOrWithoutSelection
    {
        public EndWordAction(boolean withSelection)
        {
            super(withSelection ? DefaultEditorKit.selectionEndWordAction : DefaultEditorKit.endWordAction, withSelection);
        }
        
        @Override
        public void actionPerformed(ActionEvent e)
        {
            JTextComponent c = getTextComponent(e);
            int origPos = c.getCaret().getDot();
            int end = findWordLimit(c, origPos, true);
            moveCaret(c, end);
        }
    }

    // --------------------------------------------------------------------    
    
    class BeginWordAction extends MoeActionWithOrWithoutSelection
    {
        public BeginWordAction(boolean withSelection)
        {
            super(withSelection ? DefaultEditorKit.selectionBeginWordAction : DefaultEditorKit.beginWordAction, withSelection);
        }
        
        @Override
        public void actionPerformed(ActionEvent e)
        {
            JTextComponent c = getTextComponent(e);
            int origPos = c.getCaret().getDot();
            int start = findWordLimit(c, origPos, false);
            moveCaret(c, start);
        }
    }
    
    // --------------------------------------------------------------------
    class DeleteWordAction extends MoeAbstractAction
    {
        public DeleteWordAction()
        {
            super("delete-previous-word");
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            JTextComponent c = getTextComponent(e);
            Action prevWordAct = actions.get(DefaultEditorKit.previousWordAction);
            int end = c.getCaret().getDot();
            prevWordAct.actionPerformed(e);
            int begin = c.getCaret().getDot();
            try {
                c.getDocument().remove(begin, end - begin);
            }
            catch (BadLocationException ex) {
                throw new RuntimeException(ex);
            }
        }
        
    }
    
    // --------------------------------------------------------------------    
    
    class SelectWordAction extends MoeAbstractAction
    {
        public SelectWordAction()
        {
            super(DefaultEditorKit.selectWordAction);
        }
        
        @Override
        public void actionPerformed(ActionEvent e)
        {
            JTextComponent c = getTextComponent(e);
            int origPos = c.getCaret().getDot();
            int newStart = findWordLimit(c, origPos, false);
            int newEnd = findWordLimit(c, origPos, true);
            c.getCaret().setDot(newStart);
            c.getCaret().moveDot(newEnd);
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

        @Override
        public void actionPerformed(ActionEvent e)
        {
            //getEditor(e).find();
            MoeEditor editor=getEditor(e);
            if (editor!=null) {
                editor.initFindPanel();
            }
        }
    }

    // --------------------------------------------------------------------

    public class FindNextAction extends MoeAbstractAction
    {
        public FindNextAction()
        {
            super("find-next");
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            getEditor(e).findNext(false);
        }
    }

    // --------------------------------------------------------------------

    public class FindNextBackwardAction extends MoeAbstractAction
    {
        public FindNextBackwardAction()
        {
            super("find-next-backward");
        }

        @Override
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

        @Override
        public void actionPerformed(ActionEvent e)
        {
            MoeEditor editor=getEditor(e);
            if (editor != null) {
                editor.setFindPanelVisible();
                editor.setReplacePanelVisible(true);
                if (editor.getSourcePane().getSelectedText()!=null){
                    editor.setFindTextfield(editor.getSourcePane().getSelectedText());
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

        @Override
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

        @Override
        public void actionPerformed(ActionEvent e)
        {
            Object source = e.getSource();
            if (source instanceof JComboBox) {
                getEditor(e).toggleInterface();
            }
            else {
                getEditor(e).toggleInterfaceMenu();
            }
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

        @Override
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

        @Override
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

        @Override
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

        @Override
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

        @Override
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

        @Override
        public void actionPerformed(ActionEvent e)
        {
            JOptionPane.showMessageDialog(getEditor(e), new String[]{"Moe Mouse Buttons:", " ", "left button:",
                "   click: place cursor", "   double-click: select word", "   triple-click: select line",
                "   drag: make selection", " ", "right button:", "   (currently unused)",}, "Moe Mouse Buttons",
                JOptionPane.INFORMATION_MESSAGE);
        }
    }

    // --------------------------------------------------------------------

    class GoToLineAction extends MoeAbstractAction
    {
        public GoToLineAction()
        {
            super("go-to-line");
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            getEditor(e).goToLine();
        }
    }

    // --------------------------------------------------------------------
    //     class Action extends MoeAbstractAction {
    //
    //       public Action() {
    //       super("");
    //       }
    //
    //       public void actionPerformed(ActionEvent e) {
    //       DialogManager.NYI(editor);
    //       }
    //     }

    // ========================= SUPPORT ROUTINES ==========================

    /**
     * Check whether any text is currently selected.
     * @return True, if a selection is active.
     */
    private static boolean haveSelection(JTextComponent textPane)
    {
        Caret caret = textPane.getCaret();
        return caret.getMark() != caret.getDot();
    }

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
    private static int getCurrentColumn(JTextComponent textPane)
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
    private static Element getLine(JTextComponent text, int lineNo)
    {
        return text.getDocument().getDefaultRootElement().getElement(lineNo);
    }

    // -------------------------------------------------------------------
    /**
     * Return the number of the current line.
     */
    private static int getCurrentLineIndex(JTextComponent text)
    {
        MoeSyntaxDocument document = (MoeSyntaxDocument) text.getDocument();
        return document.getDefaultRootElement().getElementIndex(text.getCaretPosition());
    }

    // ===================== ACTION IMPLEMENTATION ======================

    /**
     * Do some semi-intelligent indentation. That is: indent the current line to
     * the same depth, using the same characters (TABs or spaces) as the line
     * immediately above.
     * 
     * @param isNewLine   true if the action was to insert a line or closing brace;
     *                     false if the action was to tab/indent
     */
    private void doIndent(JTextComponent textPane, boolean isNewLine)
    {
        int lineIndex = getCurrentLineIndex(textPane);
        if (lineIndex == 0) { // first line
            if(!isNewLine) {
                insertSpacedTab(textPane);
            }
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
                if(!MoeIndent.isWhiteSpaceOnly(prevLineText)) {
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

            if (isOpenBrace(prevLineText)) {
                isOpenBrace = true;
            }
            else {
                isCommentEnd = prevLineText.trim().endsWith("*/");
                isCommentEndOnly = prevLineText.trim().equals("*/");
            }

            int indentPos = MoeIndent.findFirstNonIndentChar(prevLineText, isCommentEnd);
            String indent = prevLineText.substring(0, indentPos);
            
            if (isOpenBrace) {
                indentPos += tabSize;
            }

            // if the cursor is already past the indentation point, insert tab
            // (unless we just did a line break, then we just stop)

            int caretColumn = getCurrentColumn(textPane);
            if (caretColumn >= indentPos) {
                if (!isNewLine) {
                    insertSpacedTab(textPane);
                }
                return;
            }

            if (isNewLine && isNewCommentStart(indent, doc, lineStart)) {
                completeNewCommentBlock(textPane, indent);
                return;
            }

            // find and replace indentation of current line

            int lineEnd = line.getEndOffset();
            String lineText = doc.getText(lineStart, lineEnd - lineStart);
            indentPos = MoeIndent.findFirstNonIndentChar(lineText, true);
            char firstChar = lineText.charAt(indentPos);
            doc.remove(lineStart, indentPos);
            String newIndent = nextIndent(indent, isOpenBrace, isCommentEndOnly);
            if (firstChar == '*') {
                newIndent = newIndent.replace('*', ' ');
            }
            doc.insertString(lineStart, newIndent, null);
            if(firstChar == '}') {
                removeTab(textPane, doc);
            }
        }
        catch (BadLocationException exc) {
            throw new RuntimeException(exc);
        }
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

            int currentIndentPos = MoeIndent.findFirstNonIndentChar(lineText, true);
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

            int targetIndentPos = MoeIndent.findFirstNonIndentChar(prevLineText, true);

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
        catch (BadLocationException exc) {
            throw new RuntimeException(exc);
        }
    }

    /**
     * Indent a block of lines (defined by the current selection) by one
     * additional level.
     */
    private void doBlockIndent(MoeEditor editor)
    {
        editor.undoManager.beginCompoundEdit();
        blockAction(editor, new IndentLineAction());
        editor.undoManager.endCompoundEdit();
    }

    /**
     * De-indent a block of lines (defined by the current selection) by one
     * level.
     */
    private void doBlockDeIndent(MoeEditor editor)
    {
        editor.undoManager.beginCompoundEdit();
        blockAction(editor, new DeindentLineAction());
        editor.undoManager.endCompoundEdit();
    }

    /**
     * Check whether the indentation s opens a new multi-line comment
     * @param lineStart The position in the document of the (newly-added) line start
     */
    private static boolean isNewCommentStart(String s, MoeSyntaxDocument doc, int lineStart)
    {
        s = s.trim();
        if (s.endsWith("/**") || s.endsWith("/*"))
        {
            // The user has just pressed enter after the beginning of a comment
            // We must now decide if their comment was already fine
            // (and thus we shouldn't add the ending), or if they had, in fact,
            // begun a new comment (and do need the ending)
            
            // Find the comment node that corresponds to our position:
            NodeAndPosition<ParsedNode> curNode = doc.getParser().findNodeAt(lineStart, 0);
            while (curNode != null && !(curNode.getNode() instanceof CommentNode))
            {
                curNode = curNode.getNode().findNodeAt(lineStart, curNode.getPosition());
            }
            
            if (curNode == null) {
                //Can't work it out; it's probably a new comment that is unterminated:
                return true;
            }
            
            String comment = getNodeContents(doc, curNode);
            
            // If the comment has a comment begin inside it (after the first two characters)
            // it is likely a new comment that has over-run and matched an ending further
            // down.  If it has no comment begin inside it, it's probably a pre-existing
            // valid comment.
            comment = comment.substring(2);
            // if comment has beginning return true
            return comment.contains("/*");
        }
        return false;
    }

    /**
     * Insert text to complete a new, started block comment and place the cursor
     * appropriately.
     * 
     * The indentString passed in always ends with "/*".
     */
    private static void completeNewCommentBlock(JTextComponent textPane, String indentString)
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
    private static boolean isOpenBrace(String s)
    {
        int index = s.lastIndexOf('{');
        if (index == -1) {
            return false;
        }

        return s.indexOf('}', index + 1) == -1;
    }

    /**
     * Transform indentation string to ensure:
     * <ul>
     * <li>after " / *" follows " *"
     * <li>after " / * *" follows " *"
     * <li>after " * /" follows ""
     * </ul>
     */
    private static String nextIndent(String s, boolean openBrace, boolean commentEndOnly)
    {
        // after an opening brace, add some spaces to the indentation
        if (openBrace) {
            return s + spaces.substring(0, tabSize);
        }

        if (commentEndOnly) {
            return s.substring(0, s.length() - 1);
        }

        if (s.endsWith("/*")) {
            return s.substring(0, s.length() - 2) + " * ";
        }

        return s;
    }

    /**
     * Insert a spaced tab at the current caret position in to the textPane.
     */
    private static void insertSpacedTab(JTextComponent textPane)
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
    private static void removeTab(JTextComponent textPane, Document doc) throws BadLocationException
    {
        int col = getCurrentColumn(textPane);
        if(col > 0) {
            int remove = col % tabSize;
            if(remove == 0) {
                remove = tabSize;
            }
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
            throw new RuntimeException(exc);
        }
        return count;
    }

    private static String expandTab(String s, int idx)
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
     * @param editor 
     * @param templateName
     *            The name of the template (without path or suffix)
     */
    private static void insertTemplate(JTextComponent textPane, MoeEditor editor, String templateName)
    {
        try {
            File template = Config.getTemplateFile(templateName);
            
            InputStream fileStream = new FileInputStream(template);
            BufferedReader in = new BufferedReader(new InputStreamReader(fileStream, "UTF-8"));
            
            int addedTextLength = 0;
            String line = in.readLine();
            while (line != null) {
                while ((line.length() > 0) && (line.charAt(0) == '\t')) {
                    line = line.substring(1);
                }
                addedTextLength += line.length() + 1;
                textPane.replaceSelection(line);
                textPane.replaceSelection("\n");
                line = in.readLine();
            }
            // The position of the caret should be in the right place now.
            // Previously it was set to the position it was at before adding the
            // template, but that resulted in errors when selecting the entire
            // contents of the class before inserting the template.
            int caretPos = editor.getSourcePane().getCaretPosition();
            AutoIndentInformation info = MoeIndent.calculateIndentsAndApply(editor.getSourceDocument(),caretPos - addedTextLength,caretPos+2,caretPos);
            editor.setCaretPositionForward(info.getNewCaretPosition() - editor.getSourcePane().getCaretPosition());
            
            in.close();
        }
        catch (IOException exc) {
            Debug.reportError("Could not read method template.");
            Debug.reportError("Exception: " + exc);
        }
    }

    /**
     * Perform an action on all selected lines in the source document.
     */
    private static void blockAction(MoeEditor editor, LineAction lineAction)
    {
        editor.setCaretActive(false);
        
        Caret caret = editor.getSourcePane().getCaret();
        int selectionStart = caret.getMark();
        int selectionEnd = caret.getDot();
        if (selectionStart > selectionEnd) {
            int tmp = selectionStart;
            selectionStart = selectionEnd;
            selectionEnd = tmp;
        }
        if (selectionStart != selectionEnd)
            selectionEnd = selectionEnd - 1; // skip last position

        MoeSyntaxDocument doc = editor.getSourceDocument();
        Element text = doc.getDefaultRootElement();

        int firstLineIndex = text.getElementIndex(selectionStart);
        int lastLineIndex = text.getElementIndex(selectionEnd);
        for (int i = firstLineIndex; i <= lastLineIndex; i++) {
            Element line = text.getElement(i);
            lineAction.apply(line, doc);
        }

        editor.setSelection(firstLineIndex + 1, 1,
                text.getElement(lastLineIndex).getEndOffset()
                - text.getElement(firstLineIndex).getStartOffset());
        
        editor.setCaretActive(true);
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
        
        overrideActions = new Action[] {
                //With and without selection for each:
                new NextWordAction(false),
                new NextWordAction(true),
                new PrevWordAction(false),                
                new PrevWordAction(true),
                
              //With and without selection for each:
                new EndWordAction(false),
                new EndWordAction(true),
                new BeginWordAction(false),                
                new BeginWordAction(true),
                
                new DeleteWordAction(),
                
                new SelectWordAction()
        };
        
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
                new AddJavadocAction(),
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

                new IncreaseFontAction(),
                new DecreaseFontAction(),

                new ContentAssistAction()
        };

        // insert all actions into a hash map

        actions = new HashMap<Object, Action>();

        for (Action action : textActions) {
            actions.put(action.getValue(Action.NAME), action);
        }

        for (Action action : overrideActions) {
            actions.put(action.getValue(Action.NAME), action);
        }
       
        for (Action action : myActions) {
            actions.put(action.getValue(Action.NAME), action);
        }

        // sort all actions into a big, ordered table

        actionTable = new Action[] {

                actions.get(DefaultEditorKit.deletePrevCharAction), // 0
                actions.get(DefaultEditorKit.deleteNextCharAction),
                actions.get("delete-previous-word"),
                actions.get(DefaultEditorKit.copyAction),
                actions.get(DefaultEditorKit.cutAction), 
                actions.get("copy-line"),
                actions.get("cut-line"), 
                actions.get("cut-end-of-line"),
                actions.get("cut-word"), 
                actions.get("cut-end-of-word"),
                actions.get(DefaultEditorKit.pasteAction), 
                actions.get("indent"),
                actions.get("de-indent"),
                actions.get(DefaultEditorKit.insertTabAction), 
                actions.get("new-line"),
                actions.get(DefaultEditorKit.insertBreakAction), 
                actions.get("insert-method"),
                actions.get("comment-block"), 
                actions.get("uncomment-block"),
                actions.get("autoindent"), 
                actions.get("indent-block"), 
                actions.get("deindent-block"),

                actions.get(DefaultEditorKit.selectWordAction), // 22
                actions.get(DefaultEditorKit.selectLineAction),
                actions.get(DefaultEditorKit.selectParagraphAction),
                actions.get(DefaultEditorKit.selectAllAction),
                actions.get(DefaultEditorKit.selectionBackwardAction),
                actions.get(DefaultEditorKit.selectionForwardAction),
                actions.get(DefaultEditorKit.selectionUpAction),
                actions.get(DefaultEditorKit.selectionDownAction),
                actions.get(DefaultEditorKit.selectionBeginWordAction),
                actions.get(DefaultEditorKit.selectionEndWordAction),
                actions.get(DefaultEditorKit.selectionPreviousWordAction), // 32
                actions.get(DefaultEditorKit.selectionNextWordAction),
                actions.get(DefaultEditorKit.selectionBeginLineAction),
                actions.get(DefaultEditorKit.selectionEndLineAction),
                actions.get(DefaultEditorKit.selectionBeginParagraphAction),
                actions.get(DefaultEditorKit.selectionEndParagraphAction),
                actions.get("selection-page-up"), 
                actions.get("selection-page-down"),
                actions.get(DefaultEditorKit.selectionBeginAction),
                actions.get(DefaultEditorKit.selectionEndAction), 
                actions.get("unselect"),

                actions.get(DefaultEditorKit.backwardAction), // 43
                actions.get(DefaultEditorKit.forwardAction),
                actions.get(DefaultEditorKit.upAction), 
                actions.get(DefaultEditorKit.downAction),
                actions.get(DefaultEditorKit.beginWordAction),
                actions.get(DefaultEditorKit.endWordAction),
                actions.get(DefaultEditorKit.previousWordAction),
                actions.get(DefaultEditorKit.nextWordAction),
                actions.get(DefaultEditorKit.beginLineAction),
                actions.get(DefaultEditorKit.endLineAction),    // 52
                actions.get(DefaultEditorKit.beginParagraphAction),
                actions.get(DefaultEditorKit.endParagraphAction),
                actions.get(DefaultEditorKit.pageUpAction),
                actions.get(DefaultEditorKit.pageDownAction),
                actions.get(DefaultEditorKit.beginAction),
                actions.get(DefaultEditorKit.endAction),

                actions.get("save"), // 59
                actions.get("reload"), 
                actions.get("close"), 
                actions.get("print"),
                actions.get("page-setup"),

                actions.get("key-bindings"), // 64
                actions.get("preferences"),

                actions.get("describe-key"), // 66
                actions.get("help-mouse"), 
                actions.get("show-manual"),
                actions.get("about-editor"),

                // misc functions
                undoAction, // 70
                redoAction, 
                actions.get("find"), 
                actions.get("find-next"),
                actions.get("find-next-backward"), 
                actions.get("replace"),
                actions.get("compile"), 
                actions.get("toggle-interface-view"),
                actions.get("toggle-breakpoint"), 
                actions.get("go-to-line"),
                actions.get("increase-font"),
                actions.get("decrease-font"),
                actions.get("code-completion"),

        }; // 83

        categories = new String[] { 
                Config.getString("editor.functions.editFunctions"),
                Config.getString("editor.functions.moveScroll"), 
                Config.getString("editor.functions.classFunctions"),
                Config.getString("editor.functions.customisation"), 
                Config.getString("editor.functions.help"),
                Config.getString("editor.functions.misc")
        };

        categoryIndex = new int[] { 0, 43, 59, 64, 66, 70, 83 };
    }

    /**
     * Set up the default key bindings. Used for initial setup, or restoring the
     * default later on.
     */
    public void setDefaultKeyBindings()
    {
        keymap.removeBindings();

        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_S, SHORTCUT_MASK), actions.get("save"));
        // "reload" not bound
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_P, SHORTCUT_MASK), actions.get("print"));
        // "page-setup" not bound
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_W, SHORTCUT_MASK), actions.get("close"));
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_Z, SHORTCUT_MASK), actions.get("undo"));
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_Y, SHORTCUT_MASK), actions.get("redo"));
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0), actions.get("comment-block"));
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0), actions.get("uncomment-block"));
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0), actions.get("indent-block"));
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), actions.get("deindent-block"));
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_M, SHORTCUT_MASK), actions.get("insert-method"));
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), actions.get("indent"));
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, Event.SHIFT_MASK), actions.get("de-indent"));
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_I, SHORTCUT_MASK), actions.get("insert-tab"));
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), actions.get("new-line"));
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, Event.SHIFT_MASK), actions.get("insert-break"));
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_F, SHORTCUT_MASK), actions.get("find"));
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_G, SHORTCUT_MASK), actions.get("find-next"));
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_G, SHIFT_SHORTCUT_MASK), actions.get("find-next-backward"));
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_R, SHORTCUT_MASK), actions.get("replace"));
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_L, SHORTCUT_MASK), actions.get("go-to-line"));
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_K, SHORTCUT_MASK), actions.get("compile"));
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_J, SHORTCUT_MASK), actions.get("toggle-interface-view"));
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_B, SHORTCUT_MASK), actions.get("toggle-breakpoint"));
        // "key-bindings" not bound
        // "preferences" not bound
        // "about-editor" not bound
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_D, SHORTCUT_MASK), actions.get("describe-key"));
        // "help-mouse" not bound
        // "show-manual" not bound

        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_C, SHORTCUT_MASK), actions.get(DefaultEditorKit.copyAction));
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_X, SHORTCUT_MASK), actions.get(DefaultEditorKit.cutAction));
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_V, SHORTCUT_MASK), actions.get(DefaultEditorKit.pasteAction));

        // F2, F3, F4
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), actions.get("copy-line"));
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0), actions.get(DefaultEditorKit.pasteAction));
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0), actions.get("cut-line"));

        // cursor block
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_UP, ALT_SHORTCUT_MASK), actions.get(DefaultEditorKit.pasteAction));
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, ALT_SHORTCUT_MASK), actions.get(DefaultEditorKit.deletePrevCharAction));
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, ALT_SHORTCUT_MASK), actions.get(DefaultEditorKit.deleteNextCharAction));
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, SHIFT_ALT_SHORTCUT_MASK), actions.get("cut-line"));
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, SHIFT_ALT_SHORTCUT_MASK), actions.get("cut-end-of-line"));
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, DOUBLE_SHORTCUT_MASK), actions.get("cut-word"));
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, DOUBLE_SHORTCUT_MASK), actions.get("cut-end-of-word"));
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, SHORTCUT_MASK), actions.get("increase-font"));
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, SHORTCUT_MASK), actions.get("decrease-font"));
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, Event.CTRL_MASK), actions.get("code-completion"));
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_I, SHIFT_SHORTCUT_MASK ), actions.get("autoindent"));
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
        @Override
        public void apply(Element line, MoeSyntaxDocument doc)
        {
            int lineStart = line.getStartOffset();
            try {
                doc.insertString(lineStart, "// ", null);
            }
            catch (BadLocationException exc) {
                throw new RuntimeException(exc);
            }
        }
    }


    /**
     * Class UncommentLineAction - remove the comment symbol (if any) from the
     * given line.
     */
    class UncommentLineAction implements LineAction
    {
        @Override
        public void apply(Element line, MoeSyntaxDocument doc)
        {
            int lineStart = line.getStartOffset();
            int lineEnd = line.getEndOffset();
            try {
                String lineText = doc.getText(lineStart, lineEnd - lineStart);
                if (lineText.trim().startsWith("//")) {
                    int cnt = 0;
                    while (lineText.charAt(cnt) != '/') {
                        // whitespace chars
                        cnt++;
                    }
                    if (lineText.charAt(cnt + 2) == ' ') {
                        doc.remove(lineStart, cnt + 3);
                    }
                    else {
                        doc.remove(lineStart, cnt + 2);
                    }
                }
            }
            catch (Exception exc) {}
        }
    }

    /**
     * Class IndentLineAction - add one level of indentation to the given line.
     */
    class IndentLineAction implements LineAction
    {
        @Override
        public void apply(Element line, MoeSyntaxDocument doc)
        {
            int lineStart = line.getStartOffset();
            try {
                doc.insertString(lineStart, spaces.substring(0, tabSize), null);
            }
            catch (BadLocationException exc) {
                throw new RuntimeException(exc);
            }
        }
    }

    /**
     * Class DeindentLineAction - remove one indentation level from the given
     * line.
     */
    class DeindentLineAction implements LineAction
    {
        @Override
        public void apply(Element line, MoeSyntaxDocument doc)
        {
            int lineStart = line.getStartOffset();
            int lineEnd = line.getEndOffset();
            try {
                String lineText = doc.getText(lineStart, lineEnd - lineStart);
                String spacedTab = spaces.substring(0, tabSize);
                if (lineText.startsWith(spacedTab)) {
                    doc.remove(lineStart, tabSize); // remove spaced tab
                }
                else if (lineText.charAt(0) == TAB_CHAR) {
                    doc.remove(lineStart, 1); // remove hard tab
                }
                else {
                    int cnt = 0;
                    while (lineText.charAt(cnt) == ' ') {
                        // remove spaces
                        cnt++;
                    }
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

        @Override
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
                    editor.writeMessage(keyName + " " + Config.getString("editor.keypressed.keyIsNotBound").trim());
                }
                else {
                    editor.writeMessage(keyName + " " +Config.getString("editor.keypressed.callsTheFunction").trim() + binding + "\"");
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
   
    private static String getNodeContents(MoeSyntaxDocument doc, NodeAndPosition<ParsedNode> nap)
    {
        try {
            return doc.getText(nap.getPosition(), nap.getSize());
        }
        catch (BadLocationException e) {
            throw new RuntimeException(e);
        }
    }

}
