/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2010,2011,2012,2013,2014,2015,2016,2017  Michael Kolling and John Rosenberg

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


import bluej.Config;
import bluej.debugger.gentype.JavaType;
import bluej.editor.moe.MoeIndent.AutoIndentInformation;
import bluej.editor.moe.MoeSyntaxDocument.Element;
import bluej.parser.entity.JavaEntity;
import bluej.parser.nodes.CommentNode;
import bluej.parser.nodes.MethodNode;
import bluej.parser.nodes.NodeTree.NodeAndPosition;
import bluej.parser.nodes.ParsedNode;
import bluej.prefmgr.PrefMgr;
import bluej.prefmgr.PrefMgrDialog;
import bluej.utility.Debug;
import bluej.utility.javafx.FXRunnable;
import bluej.utility.javafx.JavaFXUtil;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.input.DataFormat;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyCombination.Modifier;
import javafx.scene.input.KeyCombination.ModifierValue;
import org.fxmisc.richtext.model.TwoDimensional.Bias;
import org.fxmisc.wellbehaved.event.EventPattern;
import org.fxmisc.wellbehaved.event.Nodes;
import threadchecker.OnThread;
import threadchecker.Tag;

import javax.swing.*;
import javax.swing.text.DefaultEditorKit;
import java.awt.*;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.*;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

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

    // We only load from the old file name "editor.keys" if the new one "editor_fx.keys" is not present
    private static final String KEYS_FILE = "editor.keys";
    private static final String KEYS_FILE_FX = "editor_fx.keys";
    private static final int tabSize = Config.getPropInteger("bluej.editor.tabsize", 4);
    private static final String spaces = "                                        ";
    private static final char TAB_CHAR = '\t';
    private static Modifier SHORTCUT_MASK = KeyCombination.SHORTCUT_DOWN;
    private static int ALT_SHORTCUT_MASK;
    private static Modifier[] SHIFT_SHORTCUT_MASK = { KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN};
    private static int SHIFT_ALT_SHORTCUT_MASK;
    private static int DOUBLE_SHORTCUT_MASK; // two masks (ie. CTRL + META)

    // -------- INSTANCE VARIABLES --------
    private static final IdentityHashMap<MoeEditor, MoeActions> moeActions = new IdentityHashMap<>();
    private final MoeEditor editor;
    //MOEFX
    //public FindNextAction findNextAction;
    //public FindNextBackwardAction findNextBackwardAction;
    // frequently needed actions
    public MoeAbstractAction compileOrNextErrorAction;
    public MoeAbstractAction contentAssistAction;
    private HashMap<String, MoeAbstractAction> actions; // All of the actions in a hash-map by their name
    private final Map<KeyCodeCombination, MoeAbstractAction> keymap = new HashMap<>();
    private org.fxmisc.wellbehaved.event.InputMap<javafx.scene.input.KeyEvent> curKeymap; // the editor's keymap
    //MOEFX private final KeyCatcher keyCatcher;
    private boolean lastActionWasCut; // true if last action was a cut action
    private MoeAbstractAction[] overrideActions;

    private MoeActions(MoeEditor editor)
    {
        this.editor = editor;
        // sort out modifier keys...

        /*MOEFX
        if (SHORTCUT_MASK == Event.CTRL_MASK)
            ALT_SHORTCUT_MASK = Event.META_MASK; // alternate (second) modifier
        else
            ALT_SHORTCUT_MASK = Event.CTRL_MASK;

        SHIFT_SHORTCUT_MASK = SHORTCUT_MASK + Event.SHIFT_MASK;
        SHIFT_ALT_SHORTCUT_MASK = Event.SHIFT_MASK + ALT_SHORTCUT_MASK;
        DOUBLE_SHORTCUT_MASK = SHORTCUT_MASK + ALT_SHORTCUT_MASK;
        */
        createActionTable(editor);
        //MOEFX
        //keyCatcher = new KeyCatcher();
        if (!load())
            setDefaultKeyBindings();
        lastActionWasCut = false;

        // install our own keymap, with the existing one as parent:
        updateKeymap();
    }

    private void updateKeymap()
    {
        if (getTextComponent() != null)
        {
            if (curKeymap != null)
                Nodes.removeInputMap(getTextComponent(), curKeymap);
            curKeymap = org.fxmisc.wellbehaved.event.InputMap.sequence(keymap.entrySet().stream()
                    .map(e -> org.fxmisc.wellbehaved.event.InputMap.consume(EventPattern.keyPressed(e.getKey()), ev -> e.getValue().actionPerformed())).collect(Collectors.toList()).toArray(new org.fxmisc.wellbehaved.event.InputMap[0]));
            Nodes.addInputMap(getTextComponent(), curKeymap);
        }
    }

    /**
     * Get the actions object for the given editor.
     */
    public static MoeActions getActions(MoeEditor editor)
    {
        return moeActions.computeIfAbsent(editor, MoeActions::new);
    }

    private static int findWordLimit(MoeEditorPane c, int pos, boolean forwards)
    {
        int maxLen = c.getDocument().length();
        if (forwards && pos >= maxLen) return maxLen;
        if (! forwards && pos <= 0) return 0;
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

    /**
     * Check whether any text is currently selected.
     * @return True, if a selection is active.
     */
    private static boolean haveSelection(MoeEditor ed)
    {
        MoeEditorPane textPane = ed.getSourcePane();
        return textPane.getCaretMark() != textPane.getCaretDot();
    }

    // =========================== STATIC METHODS ===========================

    /**
     * Return the current column number.
     */
    private static int getCurrentColumn(MoeEditorPane textPane)
    {
        int pos = Math.min(textPane.getCaretMark(), textPane.getCaretDot());
        return textPane.offsetToPosition(pos, Bias.Forward).getMinor();
    }

    /**
     * Find and return a line by line number
     */
    private Element getLine(MoeEditorPane text, int lineNo)
    {
        return editor.getSourceDocument().getDefaultRootElement().getElement(lineNo);
    }

    /**
     * Return the number of the current line.
     */
    private int getCurrentLineIndex(MoeEditorPane text)
    {
        MoeSyntaxDocument document = editor.getSourceDocument();
        return document.getDefaultRootElement().getElementIndex(text.getCaretPosition());
    }

    // ========================== INSTANCE METHODS ==========================

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
    private static void completeNewCommentBlock(MoeEditorPane textPane, String indentString)
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
    private static void insertSpacedTab(MoeEditorPane textPane)
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
    private static void removeTab(MoeEditorPane textPane, MoeSyntaxDocument doc)
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
    private void insertTemplate(String templateName)
    {
        try {
            MoeEditorPane textPane = getTextComponent();
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

        int selectionStart = editor.getSourcePane().getCaretMark();
        int selectionEnd = editor.getSourcePane().getCaretDot();
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

        // Only select the lines afterwards if there was a selection beforehand:
        if (selectionStart != selectionEnd)
        {
            editor.setSelection(firstLineIndex + 1, 1,
            text.getElement(lastLineIndex).getEndOffset()
                - text.getElement(firstLineIndex).getStartOffset());
        }

        editor.setCaretActive(true);
    }

    private static String getNodeContents(MoeSyntaxDocument doc, NodeAndPosition<ParsedNode> nap)
    {
        return doc.getText(nap.getPosition(), nap.getSize());
    }

    // ============================ USER ACTIONS =============================

    public void setPasteEnabled(boolean enabled)
    {
        //MOEFX
        //actions.get(DefaultEditorKit.pasteAction).setEnabled(enabled);
    }

    // === File: ===
    // --------------------------------------------------------------------


    // --------------------------------------------------------------------
    //MOEFX
/*
    public FindNextAction getFindNextAction()
    {
        return findNextAction;
    }

    // --------------------------------------------------------------------

    public FindNextBackwardAction getFindNextBackwardAction()
    {
        return findNextBackwardAction;
    }
*/
    // --------------------------------------------------------------------

    /**
     * Allow the enabling/disabling of an action.
     * @param action  String representing name of action
     * @param flag  true to enable action from menu.
     */

    public void enableAction(String action, boolean flag)
    {
        MoeAbstractAction moeAction = getActionByName(action);
        if (moeAction != null) {
            moeAction.setEnabled(flag);
        }
    }

    // --------------------------------------------------------------------

    /**
     * Return an action with a given name.
     */
    public MoeAbstractAction getActionByName(String name)
    {
        return actions.get(name);
    }

    /**
     * Add a new key binding into the action table.
     */
    public void addKeyCombinationForAction(KeyCodeCombination key, String actionName, boolean allEditors)
    {
        if (allEditors)
        {
            moeActions.values().forEach(moeAction -> moeAction.addKeyCombinationForAction(key, actionName, false));
        }
        else
        {
            MoeAbstractAction action = actions.get(actionName);
            if (action != null)
            {
                keymap.put(key, action);
                updateKeymap();
            }
        }
    }

    // --------------------------------------------------------------------

    /**
     * Remove a key binding from the action table.
     */
    public void removeKeyStrokeBinding(KeyCombination key)
    {
        keymap.remove(key);
    }

    // --------------------------------------------------------------------

    /**
     * Save the key bindings. Return true if successful.
     */
    public boolean save()
    {
        try {
            File file = Config.getUserConfigFile(KEYS_FILE_FX);
            ArrayList<String> lines = new ArrayList<>();
            lines.add("version " + MoeEditor.version);
            lines.add("# ALT CTRL META SHIFT SHORT KEYCODE ACTION");
            for (Entry<KeyCodeCombination, MoeAbstractAction> binding : keymap.entrySet()) {
                KeyCodeCombination k = binding.getKey();
                lines.add(k.getAlt().name() + " " + k.getControl().name() + " " + k.getMeta() + " " + k.getShift() + " " + k.getShortcut() + " " + k.getCode().name() + " " + binding.getValue().getName());
            }
            Files.write(file.toPath(), lines, Charset.forName("UTF-8"));
            return true;
        }
        catch (Exception exc) {
            Debug.message("Cannot save key bindings: " + exc);
            return false;
        }
    }

    // --------------------------------------------------------------------

    /**
     * Load the key bindings. Return true if successful.
     */
    public boolean load()
    {
        try {
            File file = Config.getUserConfigFile(KEYS_FILE_FX);
            if (file.exists())
            {
                List<String> lines = Files.readAllLines(file.toPath(), Charset.forName("UTF-8")).stream()
                        .filter(l -> !l.startsWith("#") && !l.trim().isEmpty()).collect(Collectors.toList());
                if (!lines.get(0).startsWith("version"))
                    return false;
                // Skip first line:
                try
                {
                    for (int i = 1; i < lines.size() ; i++)
                    {
                        String line = lines.get(i);
                        String[] split = line.split(" +");
                        //# ALT CTRL META SHIFT SHORT KEYCODE ACTION;
                        addKeyCombinationForAction(new KeyCodeCombination(
                                KeyCode.valueOf(split[5]),
                                ModifierValue.valueOf(split[3]),
                                ModifierValue.valueOf(split[1]),
                                ModifierValue.valueOf(split[0]),
                                ModifierValue.valueOf(split[2]),
                                ModifierValue.valueOf(split[4])
                            ), split[6], false);
                    }
                    return true;
                }
                catch (IllegalArgumentException | ArrayIndexOutOfBoundsException e)
                {
                    Debug.reportError(e);
                    return false;
                }
            }
            else
            {
                file = Config.getUserConfigFile(KEYS_FILE);
                FileInputStream istream = new FileInputStream(file);
                ObjectInputStream stream = new ObjectInputStream(istream);
                //KeyStroke[] keys = keymap.getBoundKeyStrokes();
                int version = 0;
                int count = stream.readInt();
                if (count > 100)
                { // it was new format: version number stored first
                    version = count;
                    count = stream.readInt();
                }
                if (Config.isMacOS() && (version < 140))
                {
                    // do not attempt to load old bindings on MacOS when switching
                    // to jdk 1.4.1
                    istream.close();
                    return false;
                }

                for (int i = 0; i < count; i++)
                {
                    Object keyBinding = stream.readObject();
                    KeyCodeCombination keyCombination = null;
                    if (keyBinding instanceof KeyStroke)
                    {
                        keyCombination = convertSwingBindingToFX((KeyStroke) keyBinding);
                    }
                    String actionName = (String) stream.readObject();
                    if (actionName != null && keyCombination != null)
                    {
                        addKeyCombinationForAction(keyCombination, actionName, false);
                    }
                }
                istream.close();

                // set up bindings for new actions in recent releases

                if (version < 252)
                {
                    addKeyCombinationForAction(new KeyCodeCombination(KeyCode.EQUALS, KeyCombination.SHORTCUT_DOWN), "increase-font", false);
                    addKeyCombinationForAction(new KeyCodeCombination(KeyCode.MINUS, KeyCombination.SHORTCUT_DOWN), "decrease-font", false);
                }
                if (version < 300)
                {
                    addKeyCombinationForAction(new KeyCodeCombination(KeyCode.SPACE, KeyCombination.CONTROL_DOWN), "code-completion", false);
                    addKeyCombinationForAction(new KeyCodeCombination(KeyCode.I, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN), "autoindent", false);
                }
                if (version < 320)
                {
                    addKeyCombinationForAction(new KeyCodeCombination(KeyCode.K, KeyCombination.SHORTCUT_DOWN), "compile", false);
                }
                if (version < 330)
                {
                    addKeyCombinationForAction(new KeyCodeCombination(KeyCode.COMMA, KeyCombination.SHORTCUT_DOWN), "preferences", false);
                }
                return true;
            }
        }
        catch (IOException | ClassNotFoundException exc) {
            // ignore - file probably didn't exist (yet)
            return false;
        }
    }

    private static KeyCodeCombination convertSwingBindingToFX(KeyStroke swing)
    {
        List<Modifier> modifiers = new ArrayList<>();
        if ((swing.getModifiers() & Event.CTRL_MASK) != 0)
            modifiers.add(KeyCombination.CONTROL_DOWN);
        if ((swing.getModifiers() & Event.SHIFT_MASK) != 0)
            modifiers.add(KeyCombination.SHIFT_DOWN);
        if ((swing.getModifiers() & Event.META_MASK) != 0)
            modifiers.add(KeyCombination.META_DOWN);
        if ((swing.getModifiers() & Event.ALT_MASK) != 0)
            modifiers.add(KeyCombination.ALT_DOWN);
        return new KeyCodeCombination(JavaFXUtil.awtKeyCodeToFX(swing.getKeyCode()), modifiers.toArray(new Modifier[0]));
    }

    // --------------------------------------------------------------------

    /**
     * Called to inform that any one of the user actions (text edit or caret
     * move) was executed.
     */
    public void userAction()
    {
        lastActionWasCut = false;
    }

    // --------------------------------------------------------------------

    /**
     * Called at every insertion of text into the document.
     */
    public void textInsertAction(Object documentEvent, Object textPane)
    {
        //MOEFX
        /*
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
        */
    }

    // --------------------------------------------------------------------

    /**
     * We just typed a closing brace character - indent appropriately.
     */
    /*MOEFX
    private void closingBrace(MoeEditorPane textPane, Document doc, int offset) throws BadLocationException
    {
        int lineIndex = getCurrentLineIndex(textPane);
        Element line = getLine(textPane, lineIndex);
        int lineStart = line.getStartOffset();
        String prefix = doc.getText(lineStart, offset - lineStart);

        if(prefix.trim().length() == 0) {  // only if there is no other text before '}'
            // Determine where the cursor appears horizontally (before insertion)
            //MOEFX
            Rectangle r = null;//textPane.modelToView(textPane.getCaretPosition() - 1);
            Point p = r.getLocation();

            // Indent the line
            textPane.setCaretPosition(lineStart);
            doIndent(textPane, true);
            textPane.setCaretPosition(textPane.getCaretPosition() + 1);

            // Set the magic position to the original position. This means that
            // cursor up will go to the beginning of the previous line, which is much
            // nicer behaviour.
            //MOEFX
            //textPane.getCaret().setMagicCaretPosition(p);
        }
    }
    */

    // --------------------------------------------------------------------

    /**
     * Add the current selection of the text component to the clipboard.
     */
    public static void addSelectionToClipboard(MoeEditor ed)
    {
        javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();

        // get text from clipboard
        String clipContent = clipboard.getString();
        if (clipContent == null)
            clipContent = "";
        // add current selection and store back in clipboard
        clipboard.setContent(Collections.singletonMap(DataFormat.PLAIN_TEXT, clipContent + ed.getSourcePane().getSelectedText()));
    }

    // --------------------------------------------------------------------

    /**
     * Do some semi-intelligent indentation. That is: indent the current line to
     * the same depth, using the same characters (TABs or spaces) as the line
     * immediately above.
     *
     * @param isNewLine   true if the action was to insert a line or closing brace;
     *                     false if the action was to tab/indent
     */
    private void doIndent(MoeEditorPane textPane, boolean isNewLine)
    {
        int lineIndex = getCurrentLineIndex(textPane);
        if (lineIndex == 0) { // first line
            if(!isNewLine) {
                insertSpacedTab(textPane);
            }
            return;
        }

        MoeSyntaxDocument doc = editor.getSourceDocument();

        Element line = getLine(textPane, lineIndex);
        int lineStart = line.getStartOffset();
        int pos = textPane.getCaretPosition();

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
        String prevLineText = "";
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
        textPane.replaceText(lineStart, lineStart, newIndent);
        if(firstChar == '}') {
            removeTab(textPane, doc);
        }
    }

    // --------------------------------------------------------------------

    /**
     * Do some semi-intelligent de-indentation. That is: indent the current line
     * one indentation level less that the line above, or less than it currently
     * is.
     */
    private void doDeIndent(MoeEditorPane textPane)
    {
        // set cursor to first non-blank character (or eol if none)
        // if indentation is more than line above: indent as line above
        // if indentation is same or less than line above: indent one level back

        int lineIndex = getCurrentLineIndex(textPane);
        MoeSyntaxDocument doc = editor.getSourceDocument();

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

    // --------------------------------------------------------------------

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

    // --------------------------------------------------------------------

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

    // --------------------------------------------------------------------

    /**
     * Convert all tabs in this text to spaces, maintaining the current
     * indentation.
     *
     * @param textPane The text pane to convert
     * @return  The number of tab characters converted
     */
    private static int convertTabsToSpaces(MoeEditor editor)
    {
        /*MOEFX
        int count = 0;
        int lineNo = 0;
        AbstractDocument doc = (AbstractDocument) textPane.getDocument();
        //MOEFX
        Element root = null;//doc.getDefaultRootElement();
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
        */
        return 0;
    }

    // --------------------------------------------------------------------

    /**
     * Create the table of action supported by this editor
     */
    private void createActionTable(MoeEditor editor)
    {
        compileOrNextErrorAction = compileOrNextErrorAction();

        // get all actions into arrays

        overrideActions = new MoeAbstractAction[]{
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

                deleteWordAction(),

                selectWordAction()
        };

        MoeAbstractAction[] myActions = {
                saveAction(),
                reloadAction(),
                pageSetupAction(),
                printAction(),
                closeAction(),

                undoAction(),
                redoAction(),
                commentBlockAction(),
                uncommentBlockAction(),
                autoIndentAction(),
                indentBlockAction(),
                deindentBlockAction(),
                insertMethodAction(),
                addJavadocAction(),
                indentAction(),
                deIndentAction(),
                newLineAction(),
                copyLineAction(),
                cutLineAction(),
                cutEndOfLineAction(),
                cutWordAction(),
                cutEndOfWordAction(),

                //MOEFX
                //new FindAction(editor),
                //findNextAction=new FindNextAction(editor),
                //findNextBackwardAction=new FindNextBackwardAction(editor),
                replaceAction(),
                compileOrNextErrorAction,
                goToLineAction(),
                toggleInterfaceAction(),
                toggleBreakPointAction(),

                keyBindingsAction(),
                preferencesAction(),

                describeKeyAction(),

                increaseFontAction(),
                decreaseFontAction(),

                contentAssistAction()
        };

        // insert all actions into a hash map (and retain insertion order)
        actions = new LinkedHashMap<>();

        for (MoeAbstractAction action : overrideActions)
        {
            actions.put(action.getName(), action);
        }

        for (MoeAbstractAction action : myActions)
        {
            actions.put(action.getName(), action);
        }
    }

    public List<MoeAbstractAction> getAllActions()
    {
        return new ArrayList<>(actions.values());
    }

    public List<KeyCodeCombination> getKeyStrokesForAction(String actionName)
    {
        return keymap.entrySet().stream().filter(e -> Objects.equals(e.getValue().getName(), actionName)).map(e -> e.getKey()).collect(Collectors.toList());
    }

    public static enum Category
    {
        EDIT("editor.functions.editFunctions"), MOVE_SCROLL("editor.functions.moveScroll"), CLASS("editor.functions.classFunctions"), MISC("editor.functions.misc");

        private final String label;

        private Category(String labelKey)
        {
            this.label = Config.getString(labelKey);
        }

        public String toString()
        {
            return label;
        }
    }

    // --------------------------------------------------------------------

    /**
     * Set up the default key bindings. Used for initial setup, or restoring the
     * default later on.
     */
    public void setDefaultKeyBindings()
    {
        keymap.clear();

        // Previously in the Swing editor, no distinction was made between accelerators
        // and custom key bindings.  Now in the FX editor, we do distinguish.  The rule is:
        // if an item has a menu item and a command key which can act as an accelerator then
        // it is set as an accelerator.  If not, it is set as a custom key binding.


        setAccelerator(new KeyCodeCombination(KeyCode.S, SHORTCUT_MASK), actions.get("save"));
        // "reload" not bound
        setAccelerator(new KeyCodeCombination(KeyCode.P, SHORTCUT_MASK), actions.get("print"));
        // "page-setup" not bound
        setAccelerator(new KeyCodeCombination(KeyCode.W, SHORTCUT_MASK), actions.get("close"));
        setAccelerator(new KeyCodeCombination(KeyCode.Z, SHORTCUT_MASK), actions.get("undo"));
        setAccelerator(new KeyCodeCombination(KeyCode.Y, SHORTCUT_MASK), actions.get("redo"));
        setAccelerator(new KeyCodeCombination(KeyCode.F8), actions.get("comment-block"));
        setAccelerator(new KeyCodeCombination(KeyCode.F7), actions.get("uncomment-block"));
        setAccelerator(new KeyCodeCombination(KeyCode.F6), actions.get("indent-block"));
        setAccelerator(new KeyCodeCombination(KeyCode.F5), actions.get("deindent-block"));
        setAccelerator(new KeyCodeCombination(KeyCode.M, SHORTCUT_MASK), actions.get("insert-method"));
        addKeyCombinationForAction(new KeyCodeCombination(KeyCode.TAB), "indent", false);
        addKeyCombinationForAction(new KeyCodeCombination(KeyCode.TAB, KeyCombination.SHIFT_DOWN), "de-indent", false);
        addKeyCombinationForAction(new KeyCodeCombination(KeyCode.I, SHORTCUT_MASK), "insert-tab", false);
        addKeyCombinationForAction(new KeyCodeCombination(KeyCode.ENTER), "new-line", false);
        addKeyCombinationForAction(new KeyCodeCombination(KeyCode.ENTER, KeyCombination.SHIFT_DOWN), "insert-break", false);
        setAccelerator(new KeyCodeCombination(KeyCode.F, SHORTCUT_MASK), actions.get("find"));
        setAccelerator(new KeyCodeCombination(KeyCode.G, SHORTCUT_MASK), actions.get("find-next"));
        setAccelerator(new KeyCodeCombination(KeyCode.G, KeyCombination.SHIFT_DOWN), actions.get("find-next-backward"));
        setAccelerator(new KeyCodeCombination(KeyCode.R, SHORTCUT_MASK), actions.get("replace"));
        setAccelerator(new KeyCodeCombination(KeyCode.L, SHORTCUT_MASK), actions.get("go-to-line"));
        setAccelerator(new KeyCodeCombination(KeyCode.K, SHORTCUT_MASK), actions.get("compile"));
        setAccelerator(new KeyCodeCombination(KeyCode.J, SHORTCUT_MASK), actions.get("toggle-interface-view"));
        setAccelerator(new KeyCodeCombination(KeyCode.B, SHORTCUT_MASK), actions.get("toggle-breakpoint"));
        // "key-bindings" not bound
        setAccelerator(new KeyCodeCombination(KeyCode.COMMA, SHORTCUT_MASK), actions.get("preferences"));
        // "about-editor" not bound
        setAccelerator(new KeyCodeCombination(KeyCode.D, SHORTCUT_MASK), actions.get("describe-key"));
        // "help-mouse" not bound

        setAccelerator(new KeyCodeCombination(KeyCode.C, SHORTCUT_MASK), actions.get(DefaultEditorKit.copyAction));
        setAccelerator(new KeyCodeCombination(KeyCode.X, SHORTCUT_MASK), actions.get(DefaultEditorKit.cutAction));
        setAccelerator(new KeyCodeCombination(KeyCode.V, SHORTCUT_MASK), actions.get(DefaultEditorKit.pasteAction));

        // F2, F3, F4
        setAccelerator(new KeyCodeCombination(KeyCode.F2), actions.get("copy-line"));
        setAccelerator(new KeyCodeCombination(KeyCode.F3), actions.get(DefaultEditorKit.pasteAction));
        setAccelerator(new KeyCodeCombination(KeyCode.F4), actions.get("cut-line"));

        // cursor block
        /*MOEFX
        keymap.put(new KeyCodeCombination(KeyCode.UP, ALT_SHORTCUT_MASK), actions.get(DefaultEditorKit.pasteAction));
        keymap.put(new KeyCodeCombination(KeyCode.LEFT, ALT_SHORTCUT_MASK), actions.get(DefaultEditorKit.deletePrevCharAction));
        keymap.put(new KeyCodeCombination(KeyCode.RIGHT, ALT_SHORTCUT_MASK), actions.get(DefaultEditorKit.deleteNextCharAction));
        keymap.put(new KeyCodeCombination(KeyCode.LEFT, SHIFT_ALT_SHORTCUT_MASK), actions.get("cut-line"));
        keymap.put(new KeyCodeCombination(KeyCode.RIGHT, SHIFT_ALT_SHORTCUT_MASK), actions.get("cut-end-of-line"));
        keymap.put(new KeyCodeCombination(KeyCode.LEFT, DOUBLE_SHORTCUT_MASK), actions.get("cut-word"));
        keymap.put(new KeyCodeCombination(KeyCode.RIGHT, DOUBLE_SHORTCUT_MASK), actions.get("cut-end-of-word"));
        */
        setAccelerator(new KeyCodeCombination(KeyCode.EQUALS, SHORTCUT_MASK), actions.get("increase-font"));
        setAccelerator(new KeyCodeCombination(KeyCode.MINUS, SHORTCUT_MASK), actions.get("decrease-font"));
        addKeyCombinationForAction(new KeyCodeCombination(KeyCode.SPACE, KeyCombination.CONTROL_DOWN), "code-completion", false);
        setAccelerator(new KeyCodeCombination(KeyCode.I, SHIFT_SHORTCUT_MASK), actions.get("autoindent"));
    }

    private void setAccelerator(KeyCombination accelerator, MoeAbstractAction action)
    {
        if (action == null)
            Debug.printCallStack("Setting accelerator for unfound action");
        else
            action.setAccelerator(accelerator);
    }

    private MoeAbstractAction action(String name, Category category, FXRunnable action)
    {
        return new MoeAbstractAction(name, category)
        {
            @Override
            public @OnThread(value = Tag.FX, ignoreParent = true) void actionPerformed()
            {
                action.run();
            }
        };
    }

    // --------------------------------------------------------------------

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

    // --------------------------------------------------------------------

    @OnThread(Tag.FX)
    abstract class MoeAbstractAction
    {
        private final String name;
        private final BooleanProperty disabled = new SimpleBooleanProperty(false);
        private final ObjectProperty<KeyCombination> accelerator = new SimpleObjectProperty<>(null);
        private final Category category;

        public MoeAbstractAction(String name, Category category)
        {
            this.name = name;
            this.category = category;
        }

        public abstract void actionPerformed();

        public MoeAbstractAction bindEnabled(BooleanExpression enabled)
        {
            if (enabled != null)
                disabled.bind(enabled.not());
            return this;
        }

        public void setEnabled(boolean enabled)
        {
            if (disabled.isBound())
                disabled.unbind();
            disabled.set(!enabled);
        }

        public void setAccelerator(KeyCombination accelerator)
        {
            this.accelerator.set(accelerator);
        }

        public String getName()
        {
            return name;
        }

        public Button makeButton()
        {
            Button button = new Button(name);
            button.disableProperty().bind(disabled);
            button.setOnAction(e -> actionPerformed());
            return button;
        }

        public MenuItem makeMenuItem()
        {
            MenuItem menuItem = new MenuItem(name);
            menuItem.disableProperty().bind(disabled);
            menuItem.setOnAction(e -> actionPerformed());
            menuItem.acceleratorProperty().bind(accelerator);
            return menuItem;
        }

        public String toString()
        {
            return name;
        }

        public Category getCategory()
        {
            return category;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            MoeAbstractAction that = (MoeAbstractAction) o;

            return name.equals(that.name);
        }

        @Override
        public int hashCode()
        {
            return name.hashCode();
        }
    }


    /* retained side effect: clears message in editor! */
    private final MoeEditor getEditor()
    {
        editor.clearMessage();
        return editor;
    }


    // --------------------------------------------------------------------

    private MoeAbstractAction saveAction()
    {
        return action("save", Category.CLASS, () -> getEditor().userSave());
    }

    // --------------------------------------------------------------------

    /**
     * Reload has been chosen. Ask "Really?" and call "doReload" if the answer
     * is yes.
     */
    private MoeAbstractAction reloadAction()
    {
        return action("reload", Category.CLASS, () -> getEditor().reload());
    }

    // --------------------------------------------------------------------

    private MoeAbstractAction printAction()
    {
        return action("print", Category.CLASS, () -> getEditor().print());
    }

    private MoeAbstractAction pageSetupAction()
    {
        return action("page-setup", Category.CLASS, () -> MoeEditor.pageSetup());
    }

    // --------------------------------------------------------------------

    private MoeAbstractAction closeAction()
    {
        return action("close", Category.CLASS, () -> getEditor().close());
    }

    // --------------------------------------------------------------------

    private MoeAbstractAction undoAction()
    {
        return action("undo", Category.MISC, () ->
        {
            MoeEditor editor = getEditor();
            editor.undoManager.undo();
        }).bindEnabled(editor == null ? null : editor.undoManager.canUndo());
    }

    private MoeAbstractAction redoAction()
    {
        return action("redo", Category.MISC, () -> {
            MoeEditor editor = getEditor();
            editor.undoManager.redo();
        }).bindEnabled(editor == null ? null : editor.undoManager.canRedo());
    }

    private MoeAbstractAction commentBlockAction()
    {
        return action("comment-block", Category.EDIT, () -> {
            MoeEditor editor = getEditor();
            editor.undoManager.beginCompoundEdit();
            blockAction(editor, new CommentLineAction());
            editor.undoManager.endCompoundEdit();
        });
    }

    // --------------------------------------------------------------------    

    private MoeAbstractAction uncommentBlockAction()
    {
        return action("uncomment-block", Category.EDIT, () -> {
            MoeEditor editor = getEditor();
            editor.undoManager.beginCompoundEdit();
            blockAction(editor, new UncommentLineAction());
            editor.undoManager.endCompoundEdit();
        });
    }

    // === Tools: ===
    // --------------------------------------------------------------------

    private MoeAbstractAction indentBlockAction()
    {
        return action("indent-block", Category.EDIT, () -> doBlockIndent(getEditor()));
    }

    // --------------------------------------------------------------------

    private MoeAbstractAction deindentBlockAction()
    {
        return action("deindent-block", Category.EDIT, () -> doBlockDeIndent(getEditor()));
    }

    // --------------------------------------------------------------------

    private MoeAbstractAction autoIndentAction()
    {
        return action("autoindent", Category.EDIT, () -> {
            MoeEditor editor = getEditor();
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
        });
    }

    // --------------------------------------------------------------------

    private MoeAbstractAction insertMethodAction()
    {
        return action("insert-method", Category.EDIT, () -> {
            MoeEditor editor = getEditor();
            //this method should not be actioned if the editor is not displaying source code
            if (!editor.containsSourceCode()){
                return;
            }
            editor.undoManager.beginCompoundEdit();
            insertTemplate("method");
            editor.undoManager.endCompoundEdit();
        });
    }

    // --------------------------------------------------------------------

    private MoeAbstractAction addJavadocAction()
    {
        return action("add-javadoc", Category.EDIT, () -> {
            MoeEditor editor = getEditor();
            //this method should not be actioned if the editor is not displaying source code
            if (!editor.containsSourceCode()) {
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
        });
    }

    // --------------------------------------------------------------------

    private MoeAbstractAction indentAction()
    {
        return action("indent", Category.EDIT, () -> {
            MoeEditor ed = getEditor();

            if(haveSelection(ed)) {
                doBlockIndent(ed);
            }
            else {
                // if necessary, convert all TABs in the current editor to spaces
                int converted = 0;
                if (ed.checkExpandTabs()) {
                    // do TABs need expanding?
                    ed.setCaretActive(false);
                    converted = convertTabsToSpaces(ed);
                    ed.setCaretActive(true);
                }

                if (PrefMgr.getFlag(PrefMgr.AUTO_INDENT)) {
                    //MOEFX
                    //doIndent(textPane, false);
                }
                else {
                    //MOEFX
                    //insertSpacedTab(textPane);
                }

                if (converted > 0) {
                    ed.writeMessage(Config.getString("editor.info.tabsExpanded"));
                }
            }
        });
    }

    // --------------------------------------------------------------------

    private MoeAbstractAction deIndentAction()
    {
        return action("de-indent", Category.EDIT, () -> {
            MoeEditor ed = getEditor();

            if(haveSelection(ed)) {
                doBlockDeIndent(ed);
            }
            else {
                // if necessary, convert all TABs in the current editor to spaces
                if (ed.checkExpandTabs()) { // do TABs need expanding?
                    ed.setCaretActive(false);
                    int converted = convertTabsToSpaces(ed);
                    ed.setCaretActive(true);

                    if (converted > 0)
                        ed.writeMessage(Config.getString("editor.info.tabsExpanded"));
                }
                //MOEFX
                //doDeIndent(textPane);
            }
        });
    }

    // === Options: ===
    // --------------------------------------------------------------------

    private MoeEditorPane getTextComponent()
    {
        return editor == null ? null : editor.getSourcePane();
    }

    private MoeAbstractAction newLineAction()
    {
        return action("new-line", Category.EDIT, () -> {

            editor.getSourcePane().insertText(editor.getSourcePane().getCaretPosition(), "\n");

            if (PrefMgr.getFlag(PrefMgr.AUTO_INDENT))
            {
                doIndent(getTextComponent(), true);
            }
        });
    }

    // --------------------------------------------------------------------

    private MoeAbstractAction copyLineAction()
    {
        return action("copy-line", Category.EDIT, () -> {
            boolean addToClipboard = lastActionWasCut;
            getActionByName("caret-begin-line").actionPerformed();
            getActionByName("selection-down").actionPerformed();
            if (addToClipboard) {
                addSelectionToClipboard(editor);
            }
            else {
                getActionByName("copy-to-clipboard").actionPerformed();
            }
            lastActionWasCut = true;
        });
    }

    // === Help: ===
    // --------------------------------------------------------------------

    private MoeAbstractAction cutLineAction()
    {
        return action("cut-line", Category.EDIT, () -> {
            boolean addToClipboard = lastActionWasCut;
            getActionByName("caret-begin-line").actionPerformed();
            getActionByName("selection-down").actionPerformed();
            if (addToClipboard) {
                addSelectionToClipboard(editor);
                getActionByName("delete-previous").actionPerformed();
            }
            else {
                getActionByName("cut-to-clipboard").actionPerformed();
            }
            lastActionWasCut = true;
        });
    }

    // --------------------------------------------------------------------

    private MoeAbstractAction increaseFontAction()
    {
        return action("increase-font", Category.MISC, () -> {
            MoeEditorPane textPane = getTextComponent();
            /*MOEFX
            Font textPFont= textPane.getFont();           
            int newFont=textPFont.getSize()+1;
            PrefMgr.setEditorFontSize(newFont);
            getTextComponent(e).setFont(textPane.getFont().deriveFont((float)newFont));
            */
        });
    }

    // --------------------------------------------------------------------

    private MoeAbstractAction decreaseFontAction()
    {
        return action("decrease-font", Category.MISC, () -> {
            /*MOEFX
            JTextComponent textPane = getTextComponent(e);
            Font textPFont= textPane.getFont();            
            int newFont=textPFont.getSize()-1;
            PrefMgr.setEditorFontSize(newFont);
            getTextComponent(e).setFont(textPFont.deriveFont((float)newFont));
            */
        });
    }

    // --------------------------------------------------------------------

    private MoeAbstractAction cutEndOfLineAction()
    {
        return action("cut-end-of-line", Category.EDIT, () -> {
            boolean addToClipboard = lastActionWasCut;

            getActionByName("selection-end-line").actionPerformed();
            MoeEditorPane textComponent = getTextComponent();
            String selection = textComponent.getSelectedText();
            if (selection == null)
                getActionByName("selection-forward").actionPerformed();

            if (addToClipboard) {
                addSelectionToClipboard(editor);
                getActionByName("delete-previous").actionPerformed();
            }
            else {
                getActionByName("cut-to-clipboard").actionPerformed();
            }
            lastActionWasCut = true;
        });
    }

    // ========================= SUPPORT ROUTINES ==========================

    private MoeAbstractAction cutWordAction()
    {
        return action("cut-word", Category.EDIT, () -> {
            boolean addToClipboard = lastActionWasCut;
            getActionByName("caret-previous-word").actionPerformed();
            getActionByName("selection-next-word").actionPerformed();
            if (addToClipboard) {
                addSelectionToClipboard(editor);
                getActionByName("delete-previous").actionPerformed();
            }
            else {
                getActionByName("cut-to-clipboard").actionPerformed();
            }
            lastActionWasCut = true;
        });
    }

    private MoeAbstractAction contentAssistAction()
    {
        return action("code-completion", Category.MISC, () -> {
            MoeEditor editor = getEditor();
            if (Config.getPropBoolean("bluej.editor.codecompletion", true)){
                editor.createContentAssist();
            }
        });
    }

    // --------------------------------------------------------------------

    private MoeAbstractAction cutEndOfWordAction()
    {
        return action("cut-end-of-word", Category.EDIT, () -> {
            boolean addToClipboard = lastActionWasCut;
            getActionByName("selection-next-word").actionPerformed();
            if (addToClipboard) {
                addSelectionToClipboard(editor);
                getActionByName("delete-previous").actionPerformed();
            }
            else {
                getActionByName("cut-to-clipboard").actionPerformed();
            }
            lastActionWasCut = true;
        });
    }

    // --------------------------------------------------------------------

    private abstract class MoeActionWithOrWithoutSelection extends MoeAbstractAction
    {
        private final boolean withSelection;

        protected MoeActionWithOrWithoutSelection(String actionName, Category category, boolean withSelection)
        {
            super(actionName, category);
            this.withSelection = withSelection;
        }

        protected void moveCaret(MoeEditorPane c, int pos)
        {
            if (withSelection) {
                c.moveCaretPosition(pos);
            }
            else {
                c.setCaretPosition(pos);
            }
        }
    }

    // -------------------------------------------------------------------

    class NextWordAction extends MoeActionWithOrWithoutSelection
    {
        public NextWordAction(boolean withSelection)
        {
            super(withSelection ? DefaultEditorKit.selectionNextWordAction : DefaultEditorKit.nextWordAction, Category.MOVE_SCROLL, withSelection);
        }

        @Override
        public void actionPerformed()
        {
            MoeEditorPane c = getTextComponent();
            int origPos = c.getCaretDot();
            int end = findWordLimit(c, origPos, true);
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
    }

    // ===================== ACTION IMPLEMENTATION ======================

    class PrevWordAction extends MoeActionWithOrWithoutSelection
    {
        public PrevWordAction(boolean withSelection)
        {
            super(withSelection ? DefaultEditorKit.selectionPreviousWordAction : DefaultEditorKit.previousWordAction, Category.MOVE_SCROLL, withSelection);
        }

        @Override
        public void actionPerformed()
        {
            MoeEditorPane c = getTextComponent();
            int origPos = c.getCaretDot();
            if (origPos == 0) return;
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
    }

    class EndWordAction extends MoeActionWithOrWithoutSelection
    {
        public EndWordAction(boolean withSelection)
        {
            super(withSelection ? DefaultEditorKit.selectionEndWordAction : DefaultEditorKit.endWordAction, Category.MOVE_SCROLL, withSelection);
        }

        @Override
        public void actionPerformed()
        {
            MoeEditorPane c = getTextComponent();
            int origPos = c.getCaretDot();
            int end = findWordLimit(c, origPos, true);
            moveCaret(c, end);
        }
    }

    class BeginWordAction extends MoeActionWithOrWithoutSelection
    {
        public BeginWordAction(boolean withSelection)
        {
            super(withSelection ? DefaultEditorKit.selectionBeginWordAction : DefaultEditorKit.beginWordAction, Category.MOVE_SCROLL, withSelection);
        }

        @Override
        public void actionPerformed()
        {
            MoeEditorPane c = getTextComponent();
            int origPos = c.getCaretDot();
            int start = findWordLimit(c, origPos, false);
            moveCaret(c, start);
        }
    }

    // --------------------------------------------------------------------
    private MoeAbstractAction deleteWordAction()
    {
        return action("delete-previous-word", Category.EDIT, () -> {
            MoeEditorPane c = getTextComponent();
            MoeAbstractAction prevWordAct = actions.get(DefaultEditorKit.previousWordAction);
            int end = c.getCaretDot();
            prevWordAct.actionPerformed();
            int begin = c.getCaretDot();
            c.replaceText(begin, end - begin, "");
        });
    }

    private MoeAbstractAction selectWordAction()
    {
        return action(DefaultEditorKit.selectWordAction, Category.MOVE_SCROLL, () -> {
            MoeEditorPane c = getTextComponent();
            int origPos = c.getCaretDot();
            int newStart = findWordLimit(c, origPos, false);
            int newEnd = findWordLimit(c, origPos, true);
            c.setCaretPosition(newStart);
            c.moveCaretPosition(newEnd);
        });
    }
    //MOEFX
    /*
    class FindAction extends MoeAbstractAction
    {
        public FindAction(MoeEditor editor)
        {
            super("find", editor);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            //getEditor(e).find();
            MoeEditor editor=getEditor();
            editor.initFindPanel();
        }
    }

    public class FindNextAction extends MoeAbstractAction
    {
        public FindNextAction(MoeEditor editor)
        {
            super("find-next", editor);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            getEditor().findNext(false);
        }
    }

    public class FindNextBackwardAction extends MoeAbstractAction
    {
        public FindNextBackwardAction(MoeEditor editor)
        {
            super("find-next-backward", editor);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            getEditor().findNext(true);
        }
    }
    */

    private MoeAbstractAction replaceAction()
    {
        return action("replace", Category.MISC, () ->
        {
            MoeEditor editor = getEditor();
            editor.setFindPanelVisible();
            editor.setReplacePanelVisible(true);
            if (editor.getSourcePane().getSelectedText() != null)
            {
                editor.setFindTextfield(editor.getSourcePane().getSelectedText());
            }
        });
    }

    private MoeAbstractAction compileOrNextErrorAction()
    {
        return action("compile", Category.MISC, () -> getEditor().compileOrShowNextError());
    }

    private MoeAbstractAction toggleInterfaceAction()
    {
        return action("toggle-interface-view", Category.MISC, () -> {
            /*MOEFX
            Object source = e.getSource();
            if (source instanceof JComboBox) {
                getEditor().toggleInterface();
            }
            else {
                getEditor().toggleInterfaceMenu();
            }
            */
        });
    }

    private MoeAbstractAction toggleBreakPointAction()
    {
        return action("toggle-breakpoint", Category.MISC, () -> getEditor().toggleBreakpoint());
    }

    private MoeAbstractAction keyBindingsAction()
    {
        return action("key-bindings", Category.MISC, () -> PrefMgrDialog.showDialog(1)); // 1 is the index of the key bindings pane in the pref dialog
    }

    private MoeAbstractAction preferencesAction()
    {
        return action("preferences", Category.MISC, () -> PrefMgrDialog.showDialog(0)); // 0 is the index of the editor pane in the pref dialog
    }

    // --------------------------------------------------------------------

    private MoeAbstractAction describeKeyAction()
    {
        return action("describe-key", Category.MISC, () -> {
            /*MOEFX
            JTextComponent textComponent = getTextComponent(e);
            textComponent.addKeyListener(keyCatcher);
            MoeEditor ed = getEditor();
            keyCatcher.setEditor(ed);
            ed.writeMessage("Describe key: ");
            */
        });
    }

    private MoeAbstractAction goToLineAction()
    {
        return action("go-to-line", Category.MISC, () -> getEditor().goToLine());
    }

    private MoeAbstractAction doNothingAction()
    {
        return action("", Category.MISC, () -> {});
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
            int lineEnd = line.getEndOffset();
                String lineText = doc.getText(lineStart, lineEnd - lineStart);
                if (lineText.trim().length() > 0) {
                    int textStart = MoeIndent.findFirstNonIndentChar(lineText, true);
                    doc.insertString(lineStart+textStart, "// ", null);
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
                        doc.remove(lineStart+cnt, 3);
                    }
                    else {
                        doc.remove(lineStart+cnt, 2);
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
            doc.insertString(lineStart, spaces.substring(0, tabSize), null);
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
    /*MOEFX
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
    */
}
