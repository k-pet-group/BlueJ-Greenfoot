/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2010,2011,2012,2013,2014,2015,2016,2017,2018,2019  Michael Kolling and John Rosenberg

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
import bluej.utility.Debug;
import bluej.utility.Utility;
import bluej.utility.javafx.FXAbstractAction;
import bluej.utility.javafx.FXPlatformConsumer;
import bluej.utility.javafx.FXPlatformFunction;
import bluej.utility.javafx.FXPlatformRunnable;
import bluej.utility.javafx.JavaFXUtil;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.scene.input.DataFormat;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyCombination.Modifier;
import javafx.scene.input.KeyCombination.ModifierValue;
import org.fxmisc.richtext.NavigationActions.SelectionPolicy;
import org.fxmisc.richtext.model.TwoDimensional.Bias;
import org.fxmisc.wellbehaved.event.EventPattern;
import org.fxmisc.wellbehaved.event.InputMap;
import org.fxmisc.wellbehaved.event.Nodes;
import threadchecker.OnThread;
import threadchecker.Tag;

import javax.swing.KeyStroke;
import javax.swing.text.DefaultEditorKit;
import java.awt.Event;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.*;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A set of actions supported by the Moe editor. This has a single instance
 * per editor.
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
    private static Modifier[] SHIFT_SHORTCUT_MASK = { KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN};

    // -------- INSTANCE VARIABLES --------
    private static final IdentityHashMap<MoeEditor, MoeActions> moeActions = new IdentityHashMap<>();
    private final MoeEditor editor;
    // frequently needed actions
    public MoeAbstractAction compileOrNextErrorAction;
    public MoeAbstractAction contentAssistAction;
    private HashMap<String, MoeAbstractAction> actions; // All of the actions in a hash-map by their name
    // The built-in fixed key combinations we add:
    private final ObservableMap<KeyCodeCombination, MoeAbstractAction> builtInKeymap = FXCollections.observableHashMap();
    // The user-configurable key combinations:
    // LinkedHashMap retains the order so that the shortcut which becomes the menu accelerator is consistent.
    // Shouldn't matter for behaviour, but a bit nicer than showing arbitrary shortcut on the menu item every time you load:
    private final ObservableMap<KeyCodeCombination, MoeAbstractAction> keymap = FXCollections.observableMap(new LinkedHashMap<>());
    private org.fxmisc.wellbehaved.event.InputMap<javafx.scene.input.KeyEvent> curKeymap; // the editor's keymap
    private boolean lastActionWasCut; // true if last action was a cut action
    private MoeAbstractAction[] overrideActions;

    private MoeActions(MoeEditor editor)
    {
        this.editor = editor;
        // sort out modifier keys...
        createActionTable();
        if (!load())
            setDefaultKeyBindings();
        lastActionWasCut = false;

        if (Config.isMacOS())
        {
            builtInKeymap.put(new KeyCodeCombination(KeyCode.LEFT, KeyCombination.ALT_DOWN), actions.get(DefaultEditorKit.previousWordAction));
            builtInKeymap.put(new KeyCodeCombination(KeyCode.RIGHT, KeyCombination.ALT_DOWN), actions.get(DefaultEditorKit.nextWordAction));
            builtInKeymap.put(new KeyCodeCombination(KeyCode.LEFT, KeyCombination.ALT_DOWN, KeyCombination.SHIFT_DOWN), actions.get(DefaultEditorKit.selectionPreviousWordAction));
            builtInKeymap.put(new KeyCodeCombination(KeyCode.RIGHT, KeyCombination.ALT_DOWN, KeyCombination.SHIFT_DOWN), actions.get(DefaultEditorKit.selectionNextWordAction));

            builtInKeymap.put(new KeyCodeCombination(KeyCode.LEFT, KeyCombination.META_DOWN), actions.get(DefaultEditorKit.beginLineAction));
            builtInKeymap.put(new KeyCodeCombination(KeyCode.RIGHT, KeyCombination.META_DOWN), actions.get(DefaultEditorKit.endLineAction));
            builtInKeymap.put(new KeyCodeCombination(KeyCode.LEFT, KeyCombination.META_DOWN, KeyCombination.SHIFT_DOWN), actions.get(DefaultEditorKit.selectionBeginLineAction));
            builtInKeymap.put(new KeyCodeCombination(KeyCode.RIGHT, KeyCombination.META_DOWN, KeyCombination.SHIFT_DOWN), actions.get(DefaultEditorKit.selectionEndLineAction));
        }
        else
        {
            builtInKeymap.put(new KeyCodeCombination(KeyCode.LEFT, KeyCombination.CONTROL_DOWN), actions.get(DefaultEditorKit.previousWordAction));
            builtInKeymap.put(new KeyCodeCombination(KeyCode.RIGHT, KeyCombination.CONTROL_DOWN), actions.get(DefaultEditorKit.nextWordAction));
            builtInKeymap.put(new KeyCodeCombination(KeyCode.LEFT, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN), actions.get(DefaultEditorKit.selectionPreviousWordAction));
            builtInKeymap.put(new KeyCodeCombination(KeyCode.RIGHT, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN), actions.get(DefaultEditorKit.selectionNextWordAction));
        }

        // RichTextFX has some default bindings for actions which we have on menu accelerators
        // (Plus, we want to allow users to override them.)
        // So we override those bindings to do nothing, letting our menu accelerators/defined shortcuts take over:
        builtInKeymap.put(new KeyCodeCombination(KeyCode.X, KeyCombination.SHORTCUT_DOWN), null);
        builtInKeymap.put(new KeyCodeCombination(KeyCode.C, KeyCombination.SHORTCUT_DOWN), null);
        builtInKeymap.put(new KeyCodeCombination(KeyCode.V, KeyCombination.SHORTCUT_DOWN), null);
        builtInKeymap.put(new KeyCodeCombination(KeyCode.Z, KeyCombination.SHORTCUT_DOWN), null);
        builtInKeymap.put(new KeyCodeCombination(KeyCode.Y, KeyCombination.SHORTCUT_DOWN), null);

        // install our own keymap, with the existing one as parent:
        updateKeymap();
    }

    // package-visible
    void updateKeymap()
    {
        if (getTextComponent() != null)
        {
            if (curKeymap != null)
                Nodes.removeInputMap(getTextComponent(), curKeymap);
            // Join built-in keymap (important that it goes first in order, so it can get overridden) with custom keymap:
            Stream<Entry<KeyCodeCombination, MoeAbstractAction>> joinedKeyMapStream = Stream.concat(
                builtInKeymap.entrySet().stream(),
                keymap.entrySet().stream()
                    // Only use keymap if item isn't on a menu accelerator:
                    .filter(e -> !e.getValue().hasMenuItemWithAccelerator(e.getKey()))
            );

            // We need to make sure later items overwrite earlier ones.  I thought InputMap.sequence
            // would do this, but it appears not to have the desired effect (an early ignore is not
            // overridden by a later consume).  Stream.distinct() won't do what we want because we want
            // to distinguish only by key, so we use an interim map to make sure we only keep the last
            // value for each key:
            HashMap<KeyCodeCombination, MoeAbstractAction> all = new HashMap<>();
            joinedKeyMapStream.forEach(kv -> all.put(kv.getKey(), kv.getValue()));
            joinedKeyMapStream = all.entrySet().stream();

            curKeymap = InputMap.sequence(joinedKeyMapStream.map(e -> {
                // If no action, it means ignore the keypress
                if (e.getValue() == null)
                    return InputMap.ignore(EventPattern.keyPressed(e.getKey()));
                else
                {
                    return InputMap.consume(EventPattern.keyPressed(e.getKey()), ev -> e.getValue().actionPerformed(false));
                }
            }).toArray(InputMap[]::new));
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
        char curChar = c.getText(pos, pos + 1).charAt(0);
        if (Character.isWhitespace(curChar)) {
            while (Character.isWhitespace(curChar)) {
                if (forwards) pos++; else pos--;
                if (pos == maxLen) return pos;
                if (pos == 0) return 0;
                curChar = c.getText(pos, pos + 1).charAt(0);
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
                curChar = c.getText(pos, pos + 1).charAt(0);
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

    /**
     * Return the current column number.
     */
    private int getCurrentColumn()
    {
        int pos = Math.min(editor.getSourcePane().getCaretMark(), editor.getSourcePane().getCaretDot());
        return editor.getSourcePane().offsetToPosition(pos, Bias.Forward).getMinor();
    }

    /**
     * Find and return a line by line number
     */
    private Element getLine(int lineNo)
    {
        return editor.getSourceDocument().getDefaultRootElement().getElement(lineNo);
    }

    /**
     * Return the number of the current line.
     */
    private int getCurrentLineIndex()
    {
        MoeSyntaxDocument document = editor.getSourceDocument();
        return document.getDefaultRootElement().getElementIndex(editor.getSourcePane().getCaretPosition());
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
    private void insertSpacedTab()
    {
        int numSpaces = tabSize - (getCurrentColumn() % tabSize);
        editor.getSourcePane().replaceSelection(spaces.substring(0, numSpaces));
    }

    /**
     * Remove characters before the current caret position to take the
     * caret back to the previous TAB position. No check is made what kind
     * of characters those are - the caller should make sure they can be
     * removed (usually they should be whitespace).
     */
    private void removeTab()
    {
        int col = getCurrentColumn();
        if(col > 0) {
            int remove = col % tabSize;
            if(remove == 0) {
                remove = tabSize;
            }
            int pos = editor.getSourcePane().getCaretPosition();
            editor.getSourcePane().replaceText(pos-remove, pos, "");
            editor.getSourcePane().setCaretPosition(pos-remove);
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

        int firstLineIndex = editor.getLineColumnFromOffset(selectionStart).getLine() - 1;
        int lastLineIndex = editor.getLineColumnFromOffset(selectionEnd).getLine();
        for (int i = firstLineIndex; i < lastLineIndex; i++) {
            Element line = text.getElement(i);
            lineAction.apply(line, doc);
        }
        if (selectionStart == selectionEnd)
        {
            Element line = text.getElement(selectionStart);
            editor.getSourcePane().deselect();
        }
    }

    private static String getNodeContents(MoeSyntaxDocument doc, NodeAndPosition<ParsedNode> nap)
    {
        return doc.getText(nap.getPosition(), nap.getSize());
    }

    // --------------------------------------------------------------------

    /**
     * Add a new key binding into the action table.
     */
    public static void addKeyCombinationForActionToAllEditors(KeyCodeCombination key, String actionName)
    {
        moeActions.values().forEach(moeAction -> moeAction.addKeyCombinationForAction(key, actionName));
    }

    /**
     * Add a new key binding into the action table.
     */
    private void addKeyCombinationForAction(KeyCodeCombination key, String actionName)
    {
        MoeAbstractAction action = actions.get(actionName);
        if (action != null)
        {
            keymap.put(key, action);
            updateKeymap();
        }
    }

    /**
     * Return an action with a given name.
     */
    public MoeAbstractAction getActionByName(String name)
    {
        return actions.get(name);
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
                if (lines.isEmpty() || !lines.get(0).startsWith("version"))
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
                            ), split[6]);
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
                        addKeyCombinationForAction(keyCombination, actionName);
                    }
                }
                istream.close();

                // set up bindings for new actions in recent releases

                if (version < 252)
                {
                    addKeyCombinationForAction(new KeyCodeCombination(KeyCode.EQUALS, KeyCombination.SHORTCUT_DOWN), "increase-font");
                    addKeyCombinationForAction(new KeyCodeCombination(KeyCode.MINUS, KeyCombination.SHORTCUT_DOWN), "decrease-font");
                }
                if (version < 300)
                {
                    addKeyCombinationForAction(new KeyCodeCombination(KeyCode.SPACE, KeyCombination.CONTROL_DOWN), "code-completion");
                    addKeyCombinationForAction(new KeyCodeCombination(KeyCode.I, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN), "autoindent");
                }
                if (version < 320)
                {
                    addKeyCombinationForAction(new KeyCodeCombination(KeyCode.K, KeyCombination.SHORTCUT_DOWN), "compile");
                }
                if (version < 330)
                {
                    addKeyCombinationForAction(new KeyCodeCombination(KeyCode.COMMA, KeyCombination.SHORTCUT_DOWN), "preferences");
                }
                if (version < 400)
                {
                    addKeyCombinationForAction(new KeyCodeCombination(KeyCode.DIGIT0, KeyCombination.SHORTCUT_DOWN), "reset-font");
                }
                return true;
            }
        }
        catch (IOException | ClassNotFoundException exc) {
            // ignore - file probably didn't exist (yet)
            return false;
        }
    }

    // We can't use the recommended Java 9 replacement of getModifiersEx()
    // because that is on the key event, and we are loading a KeyStroke
    // object from a file, saved on an old BlueJ.  So we must continue
    // checking against the old modifiers:
    @SuppressWarnings("deprecation")
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

        KeyCode code = JavaFXUtil.awtKeyCodeToFX(swing.getKeyCode());
        if (code != null)
        {
            return new KeyCodeCombination(code, modifiers.toArray(new Modifier[0]));
        }
        else
        {
            return null;
        }
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
     * We just typed a closing brace character - indent appropriately.
     */
    public void closingBrace(int offset)
    {
        int lineIndex = getCurrentLineIndex();
        Element line = getLine(lineIndex);
        int lineStart = line.getStartOffset();
        String prefix = editor.getSourcePane().getText(lineStart, offset);

        if(prefix.trim().length() == 0) {  // only if there is no other text before '}'
            // Indent the line
            editor.getSourcePane().setCaretPosition(lineStart);
            doIndent(true);
            editor.getSourcePane().setCaretPosition(editor.getSourcePane().getCaretPosition() + 1);
        }
    }

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
    private void doIndent(boolean isNewLine)
    {
        MoeEditorPane textPane = editor.getSourcePane();
        int lineIndex = getCurrentLineIndex();
        if (lineIndex == 0) { // first line
            if(!isNewLine) {
                insertSpacedTab();
            }
            return;
        }

        MoeSyntaxDocument doc = editor.getSourceDocument();

        Element line = getLine(lineIndex);
        int lineStart = line.getStartOffset();
        int pos = textPane.getCaretPosition();

        boolean isOpenBrace = false;
        boolean isCommentEnd = false, isCommentEndOnly = false;

        // if there is any text before the cursor, just insert a tab

        String prefix = doc.getText(lineStart, pos - lineStart);
        if (prefix.trim().length() > 0) {
            insertSpacedTab();
            return;
        }

        // get indentation string from previous line

        boolean foundLine = false;
        int lineOffset = 1;
        String prevLineText = "";
        while ((lineIndex - lineOffset >= 0) && !foundLine) {
            Element prevline = getLine(lineIndex - lineOffset);
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
                insertSpacedTab();
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

        int caretColumn = getCurrentColumn();
        if (caretColumn >= indentPos) {
            if (!isNewLine) {
                insertSpacedTab();
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
        char firstChar = lineText.isEmpty() ? '\u0000' : lineText.charAt(indentPos);
        doc.remove(lineStart, indentPos);
        String newIndent = nextIndent(indent, isOpenBrace, isCommentEndOnly);
        if (firstChar == '*') {
            newIndent = newIndent.replace('*', ' ');
        }
        textPane.replaceText(lineStart, lineStart, newIndent);
        if(firstChar == '}') {
            removeTab();
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

        int lineIndex = getCurrentLineIndex();
        MoeSyntaxDocument doc = editor.getSourceDocument();

        Element line = getLine(lineIndex);
        int lineStart = line.getStartOffset();
        int lineEnd = line.getEndOffset();
        String lineText = doc.getText(lineStart, lineEnd - lineStart);

        int currentIndentPos = MoeIndent.findFirstNonIndentChar(lineText, true);
        char firstChar = lineText.charAt(currentIndentPos);

        textPane.setCaretPosition(lineStart + currentIndentPos);

        if (lineIndex == 0) { // first line
            removeTab();
            return;
        }

        // get indentation details from previous line

        Element prevline = getLine(lineIndex - 1);
        int prevLineStart = prevline.getStartOffset();
        int prevLineEnd = prevline.getEndOffset();
        String prevLineText = doc.getText(prevLineStart, prevLineEnd - prevLineStart);

        int targetIndentPos = MoeIndent.findFirstNonIndentChar(prevLineText, true);

        if (currentIndentPos > targetIndentPos) {
            // indent same as line above
            String indent = prevLineText.substring(0, targetIndentPos);
            doc.remove(lineStart, currentIndentPos);
            doc.insertString(lineStart, indent);
            if(firstChar == '}')
                removeTab();
        }
        else {
            // we are at same level as line above or less - go one indentation
            // level back
            removeTab();
        }
    }

    // --------------------------------------------------------------------

    /**
     * Indent a block of lines (defined by the current selection) by one
     * additional level.
     */
    private void doBlockIndent(MoeEditor editor)
    {
        editor.undoManager.compoundEdit(() -> blockAction(editor, new IndentLineAction()));
    }

    // --------------------------------------------------------------------

    /**
     * De-indent a block of lines (defined by the current selection) by one
     * level.
     */
    private void doBlockDeIndent(MoeEditor editor)
    {
        editor.undoManager.compoundEdit(() -> blockAction(editor, new DeindentLineAction()));
    }

    // --------------------------------------------------------------------

    /**
     * Convert all tabs in this text to spaces, maintaining the current
     * indentation.
     *
     * @param editor  Reference to the editor
     * @return  The number of tab characters converted
     */
    private static int convertTabsToSpaces(MoeEditor editor)
    {
        int count = 0;
        int lineNo = 0;
        MoeSyntaxDocument doc = editor.getSourceDocument();
        Element root = doc.getDefaultRootElement();
        Element line = root.getElement(lineNo);
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
                doc.insertString(start, text);
            }
            lineNo++;
            line = root.getElement(lineNo);
        }
        return count;
    }

    // --------------------------------------------------------------------

    /**
     * Create the table of action supported by this editor
     */
    private void createActionTable()
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

                //With and without selection for each:
                new EndLineAction(false),
                new EndLineAction(true),
                new BeginLineAction(false),
                new BeginLineAction(true),
                
                deleteWordAction(),

                selectWordAction()
        };

        MoeAbstractAction[] myActions = {
                saveAction(),
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

                cutAction(),
                copyAction(),
                pasteAction(),
                copyLineAction(),
                cutLineAction(),
                cutEndOfLineAction(),
                cutWordAction(),
                cutEndOfWordAction(),

                findAction(),
                findNextAction(),
                findPrevAction(),
                replaceAction(),
                compileOrNextErrorAction,
                goToLineAction(),
                toggleInterfaceAction(),
                toggleBreakPointAction(),

                keyBindingsAction(),
                preferencesAction(),

                increaseFontAction(),
                decreaseFontAction(),
                resetFontAction(),

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

    /**
     * Makes all actions unavailable (i.e. force disable) except the given actions
     */
    public void makeAllUnavailableExcept(String... actionNames)
    {
        Set<String> keepEnabled = new HashSet<>(Arrays.asList(actionNames));
        for (Entry<String, MoeAbstractAction> action : this.actions.entrySet())
        {
            action.getValue().setAvailable(keepEnabled.contains(action.getKey()));
        }
    }

    public void makeAllAvailable()
    {
        for (Entry<String, MoeAbstractAction> action : this.actions.entrySet())
        {
            action.getValue().setAvailable(true);
        }
    }

    public static enum Category
    {
        EDIT("editor.functions.editFunctions"), MOVE_SCROLL("editor.functions.moveScroll"), CLASS("editor.functions.classFunctions"), MISC("editor.functions.misc");

        private final String label;

        private Category(String labelKey)
        {
            this.label = Config.getString(labelKey);
        }

        @OnThread(Tag.Any)
        @Override
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


        addKeyCombinationForAction(new KeyCodeCombination(KeyCode.S, SHORTCUT_MASK), "save");
        addKeyCombinationForAction(new KeyCodeCombination(KeyCode.P, SHORTCUT_MASK), "print");
        // "page-setup" not bound
        addKeyCombinationForAction(new KeyCodeCombination(KeyCode.W, SHORTCUT_MASK), "close");
        addKeyCombinationForAction(new KeyCodeCombination(KeyCode.Z, SHORTCUT_MASK), "undo");
        addKeyCombinationForAction(new KeyCodeCombination(KeyCode.Y, SHORTCUT_MASK), "redo");
        addKeyCombinationForAction(new KeyCodeCombination(KeyCode.F8), "comment-block");
        addKeyCombinationForAction(new KeyCodeCombination(KeyCode.F7), "uncomment-block");
        addKeyCombinationForAction(new KeyCodeCombination(KeyCode.F6), "indent-block");
        addKeyCombinationForAction(new KeyCodeCombination(KeyCode.F5), "deindent-block");
        addKeyCombinationForAction(new KeyCodeCombination(KeyCode.M, SHORTCUT_MASK), "insert-method");
        addKeyCombinationForAction(new KeyCodeCombination(KeyCode.TAB), "indent");
        addKeyCombinationForAction(new KeyCodeCombination(KeyCode.TAB, KeyCombination.SHIFT_DOWN), "de-indent");
        addKeyCombinationForAction(new KeyCodeCombination(KeyCode.I, SHORTCUT_MASK), "insert-tab");
        addKeyCombinationForAction(new KeyCodeCombination(KeyCode.ENTER), "new-line");
        addKeyCombinationForAction(new KeyCodeCombination(KeyCode.ENTER, KeyCombination.SHIFT_DOWN), "insert-break");
        addKeyCombinationForAction(new KeyCodeCombination(KeyCode.F, SHORTCUT_MASK), "find");
        addKeyCombinationForAction(new KeyCodeCombination(KeyCode.G, SHORTCUT_MASK), "find-next");
        addKeyCombinationForAction(new KeyCodeCombination(KeyCode.G, SHORTCUT_MASK, KeyCombination.SHIFT_DOWN), "find-next-backward");
        addKeyCombinationForAction(new KeyCodeCombination(KeyCode.R, SHORTCUT_MASK), "replace");
        addKeyCombinationForAction(new KeyCodeCombination(KeyCode.L, SHORTCUT_MASK), "go-to-line");
        addKeyCombinationForAction(new KeyCodeCombination(KeyCode.K, SHORTCUT_MASK), "compile");
        addKeyCombinationForAction(new KeyCodeCombination(KeyCode.J, SHORTCUT_MASK), "toggle-interface-view");
        addKeyCombinationForAction(new KeyCodeCombination(KeyCode.B, SHORTCUT_MASK), "toggle-breakpoint");
        // "key-bindings" not bound
        addKeyCombinationForAction(new KeyCodeCombination(KeyCode.COMMA, SHORTCUT_MASK), "preferences");
        // "about-editor" not bound
        addKeyCombinationForAction(new KeyCodeCombination(KeyCode.D, SHORTCUT_MASK), "describe-key");
        // "help-mouse" not bound

        addKeyCombinationForAction(new KeyCodeCombination(KeyCode.C, SHORTCUT_MASK), DefaultEditorKit.copyAction);
        addKeyCombinationForAction(new KeyCodeCombination(KeyCode.X, SHORTCUT_MASK), DefaultEditorKit.cutAction);
        addKeyCombinationForAction(new KeyCodeCombination(KeyCode.V, SHORTCUT_MASK), DefaultEditorKit.pasteAction);

        // F2, F3, F4
        addKeyCombinationForAction(new KeyCodeCombination(KeyCode.F2), "copy-line");
        addKeyCombinationForAction(new KeyCodeCombination(KeyCode.F3), DefaultEditorKit.pasteAction);
        addKeyCombinationForAction(new KeyCodeCombination(KeyCode.F4), "cut-line");

        // cursor block
        addKeyCombinationForAction(new KeyCodeCombination(KeyCode.EQUALS, SHORTCUT_MASK), "increase-font");
        addKeyCombinationForAction(new KeyCodeCombination(KeyCode.MINUS, SHORTCUT_MASK), "decrease-font");
        addKeyCombinationForAction(new KeyCodeCombination(KeyCode.DIGIT0, SHORTCUT_MASK), "reset-font");
        addKeyCombinationForAction(new KeyCodeCombination(KeyCode.SPACE, KeyCombination.CONTROL_DOWN), "code-completion");
        addKeyCombinationForAction(new KeyCodeCombination(KeyCode.I, SHIFT_SHORTCUT_MASK), "autoindent");
    }

    private MoeAbstractAction action(String name, Category category, FXPlatformRunnable action)
    {
        return new MoeAbstractAction(name, category)
        {
            @Override
            public @OnThread(value = Tag.FXPlatform) void actionPerformed(boolean viaContextMenu)
            {
                action.run();
            }
        };
    }

    /**
     * Creates an action that can act differently if it
     * is called via a context menu.
     * 
     * @param name The action name
     * @param category The category for the preferences
     * @param action Called with true if called via a context menu, false otherwise.
     * @return
     */
    private MoeAbstractAction contextSensitiveAction(String name, Category category, FXPlatformConsumer<Boolean> action)
    {
        return new MoeAbstractAction(name, category)
        {
            @Override
            public @OnThread(value = Tag.FXPlatform) void actionPerformed(boolean viaContextMenu)
            {
                action.accept(viaContextMenu);
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

    @OnThread(Tag.FXPlatform)
    abstract class MoeAbstractAction extends FXAbstractAction
    {
        private final Category category;

        public MoeAbstractAction(String name, Category category)
        {
            super(name);
            this.accelerator.bind(Bindings.createObjectBinding(() -> {
                return keymap.entrySet().stream().filter(e -> e.getValue().equals(this)).map(e -> e.getKey()).findFirst().orElse(null);
            }, keymap));
            this.category = category;
        }

        public Category getCategory()
        {
            return category;
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

    private MoeAbstractAction printAction()
    {
        return action("print", Category.CLASS, () -> getEditor().print());
    }

    // --------------------------------------------------------------------

    private MoeAbstractAction closeAction()
    {
        return action("close", Category.CLASS, () -> getEditor().close());
    }

    // --------------------------------------------------------------------

    private MoeAbstractAction undoAction()
    {
        MoeAbstractAction action = action("undo", Category.MISC, () ->
        {
            MoeEditor editor = getEditor();
            editor.undoManager.undo();
        });
        action.bindEnabled(editor == null ? null : editor.undoManager.canUndo());
        return action;
    }

    private MoeAbstractAction redoAction()
    {
        MoeAbstractAction action =  action("redo", Category.MISC, () -> {
            MoeEditor editor = getEditor();
            editor.undoManager.redo();
        });
        action.bindEnabled(editor == null ? null : editor.undoManager.canRedo());
        return action;
    }

    private MoeAbstractAction commentBlockAction()
    {
        return action("comment-block", Category.EDIT, () -> {
            MoeEditor editor = getEditor();
            editor.undoManager.compoundEdit(() -> blockAction(editor, new CommentLineAction()));
        });
    }

    // --------------------------------------------------------------------    

    private MoeAbstractAction uncommentBlockAction()
    {
        return action("uncomment-block", Category.EDIT, () -> {
            MoeEditor editor = getEditor();
            editor.undoManager.compoundEdit(() -> blockAction(editor, new UncommentLineAction()));
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
            editor.undoManager.compoundEdit(() -> {
                AutoIndentInformation info = MoeIndent.calculateIndentsAndApply(doc, prevCaretPos);
                editor.setCaretPositionForward(info.getNewCaretPosition() - prevCaretPos);
                if (info.isPerfect()) {
                    editor.writeMessage(Config.getString("editor.info.perfectIndent"));
                }
            });
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
            editor.undoManager.compoundEdit(() -> insertTemplate("method"));
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

                    NodeAndPosition<ParsedNode> nodeFinal = node;
                    editor.undoManager.compoundEdit(() -> {
                        editor.getCurrentTextPane().setCaretPosition(nodeFinal.getPosition());
                        editor.getCurrentTextPane().replaceSelection(newComment.toString());
                        editor.getCurrentTextPane().setCaretPosition((caretPos + newComment.length()));
                    });
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
                    converted = convertTabsToSpaces(ed);
                }

                if (PrefMgr.getFlag(PrefMgr.AUTO_INDENT)) {
                    doIndent(false);
                }
                else {
                    insertSpacedTab();
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
                    int converted = convertTabsToSpaces(ed);

                    if (converted > 0)
                        ed.writeMessage(Config.getString("editor.info.tabsExpanded"));
                }
                doDeIndent(ed.getCurrentTextPane());
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

            editor.getSourcePane().replaceSelection("\n");
            // We seem to need to force the layout now after requesting to follow,
            // or otherwise sometimes it doesn't scroll down to track the new caret position
            // if we just request but don't layout:
            editor.getSourcePane().requestFollowCaret();
            editor.getSourcePane().layout();

            if (PrefMgr.getFlag(PrefMgr.AUTO_INDENT))
            {
                doIndent(true);
            }
            editor.undoManager.breakEdit();
        });
    }

    // --------------------------------------------------------------------

    private MoeAbstractAction cutAction()
    {
        return contextSensitiveAction("cut-to-clipboard", Category.EDIT, viaContextMenu -> {
            // Menu shortcut can trigger when e.g. find pane is focused, don't act if not focused:
            if (viaContextMenu || editor.getSourcePane().isFocused())
            {
                editor.getSourcePane().cut();
                if (viaContextMenu)
                {
                    editor.getSourcePane().requestFocus();
                }
            }
        });
    }

    private MoeAbstractAction copyAction()
    {
        return contextSensitiveAction("copy-to-clipboard", Category.EDIT, viaContextMenu -> {
            // Menu shortcut can trigger when e.g. find pane is focused, don't act if not focused:
            if (viaContextMenu || editor.getSourcePane().isFocused())
            {
                editor.getSourcePane().copy();
                if (viaContextMenu)
                {
                    editor.getSourcePane().requestFocus();
                }
            }
        });
    }

    private MoeAbstractAction pasteAction()
    {
        return contextSensitiveAction("paste-from-clipboard", Category.EDIT, viaContextMenu -> {
            // Menu shortcut can trigger when e.g. find pane is focused, don't act if not focused:
            if (viaContextMenu || editor.getSourcePane().isFocused())
            {
                editor.getSourcePane().paste();
                if (viaContextMenu)
                {
                    editor.getSourcePane().requestFocus();
                }
                editor.getSourcePane().requestFollowCaret();
            }
        });
    }

    private MoeAbstractAction copyLineAction()
    {
        return action("copy-line", Category.EDIT, () -> {
            boolean addToClipboard = lastActionWasCut;
            editor.getSourcePane().lineStart(SelectionPolicy.CLEAR);
            editor.getSourcePane().lineEnd(SelectionPolicy.EXTEND);
            editor.getSourcePane().nextChar(SelectionPolicy.EXTEND);
            if (addToClipboard) {
                addSelectionToClipboard(editor);
            }
            else {
                editor.getSourcePane().copy();
            }
            // This will keep us on next line, but with no selection:
            editor.getSourcePane().setCaretPosition(editor.getSourcePane().getSelection().getEnd());
            lastActionWasCut = true;
        });
    }

    // === Help: ===
    // --------------------------------------------------------------------

    private MoeAbstractAction cutLineAction()
    {
        return action("cut-line", Category.EDIT, () -> {
            boolean addToClipboard = lastActionWasCut;
            editor.getSourcePane().lineStart(SelectionPolicy.CLEAR);
            editor.getSourcePane().lineEnd(SelectionPolicy.EXTEND);
            editor.getSourcePane().nextChar(SelectionPolicy.EXTEND);
            if (addToClipboard) {
                addSelectionToClipboard(editor);
                editor.getSourcePane().replaceSelection("");
            }
            else {
                editor.getSourcePane().cut();
            }
            lastActionWasCut = true;
        });
    }

    // --------------------------------------------------------------------

    private MoeAbstractAction increaseFontAction()
    {
        return action("increase-font", Category.MISC, () -> {
            Utility.increaseFontSize(PrefMgr.getEditorFontSize());
        });
    }

    // --------------------------------------------------------------------

    private MoeAbstractAction decreaseFontAction()
    {
        return action("decrease-font", Category.MISC, () -> {
            Utility.decreaseFontSize(PrefMgr.getEditorFontSize());
        });
    }

    private MoeAbstractAction resetFontAction()
    {
        return action("reset-font", Category.MISC, () -> {
            PrefMgr.getEditorFontSize().set(PrefMgr.DEFAULT_JAVA_FONT_SIZE);
        });
    }

    // --------------------------------------------------------------------

    private MoeAbstractAction cutEndOfLineAction()
    {
        return action("cut-end-of-line", Category.EDIT, () -> {
            boolean addToClipboard = lastActionWasCut;
            MoeEditorPane textComponent = getTextComponent();
            textComponent.paragraphEnd(SelectionPolicy.ADJUST);

            if (addToClipboard) {
                addSelectionToClipboard(editor);
                textComponent.replaceSelection("");
            }
            else {
                textComponent.cut();
            }
            lastActionWasCut = true;
        });
    }

    // ========================= SUPPORT ROUTINES ==========================

    private MoeAbstractAction cutWordAction()
    {
        return action("cut-word", Category.EDIT, () -> {
            boolean addToClipboard = lastActionWasCut;
            getActionByName("caret-previous-word").actionPerformed(false);
            getActionByName("selection-next-word").actionPerformed(false);
            if (addToClipboard) {
                addSelectionToClipboard(editor);
                getActionByName("delete-previous").actionPerformed(false);
            }
            else {
                getActionByName("cut-to-clipboard").actionPerformed(false);
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
            getActionByName("selection-next-word").actionPerformed(false);
            if (addToClipboard) {
                addSelectionToClipboard(editor);
                getActionByName("delete-previous").actionPerformed(false);
            }
            else {
                getActionByName("cut-to-clipboard").actionPerformed(false);
            }
            lastActionWasCut = true;
        });
    }

    // --------------------------------------------------------------------

    private abstract class MoeActionWithOrWithoutSelection extends MoeAbstractAction
    {
        protected final boolean withSelection;

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
        public void actionPerformed(boolean viaContextMenu)
        {
            MoeEditorPane c = getTextComponent();
            int origPos = c.getCaretDot();
            int end = findWordLimit(c, origPos, true);
            if (Character.isWhitespace(c.getText(end, end + 1).charAt(0))) {
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
        public void actionPerformed(boolean viaContextMenu)
        {
            MoeEditorPane c = getTextComponent();
            int origPos = c.getCaretDot();
            if (origPos == 0) return;
            if (Character.isWhitespace(c.getText(origPos - 1, origPos).charAt(0))) {
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
        public void actionPerformed(boolean viaContextMenu)
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
        public void actionPerformed(boolean viaContextMenu)
        {
            MoeEditorPane c = getTextComponent();
            int origPos = c.getCaretDot();
            int start = findWordLimit(c, origPos, false);
            moveCaret(c, start);
        }
    }

    private class BeginLineAction extends MoeActionWithOrWithoutSelection
    {
        public BeginLineAction(boolean withSelection)
        {
            super(withSelection ? DefaultEditorKit.selectionBeginLineAction : DefaultEditorKit.beginLineAction, Category.MOVE_SCROLL, withSelection);
        }

        @Override
        public void actionPerformed(boolean viaContextMenu)
        {
            MoeEditorPane ed = getTextComponent();
            if (ed.getCaretColumn() > 1)
            {
                ed.lineStart(withSelection ? SelectionPolicy.EXTEND : SelectionPolicy.CLEAR);
            }
            else
            {
                // Already at start, try going next word
                int line = ed.getCurrentParagraph();
                int oldPos = ed.getCaretPosition();
                ed.wordBreaksForwards(1, withSelection ? SelectionPolicy.EXTEND : SelectionPolicy.CLEAR);
                if (ed.getCurrentParagraph() != line)
                {
                    // That took us to next line; our line must have been all whitespace,
                    // so move back:
                    ed.setCaretPosition(oldPos);
                }

            }
        }
    }

    private class EndLineAction extends MoeActionWithOrWithoutSelection
    {
        public EndLineAction(boolean withSelection)
        {
            super(withSelection ? DefaultEditorKit.selectionEndLineAction : DefaultEditorKit.endLineAction, Category.MOVE_SCROLL, withSelection);
        }

        @Override
        public void actionPerformed(boolean viaContextMenu)
        {
            getTextComponent().lineEnd(withSelection ? SelectionPolicy.EXTEND : SelectionPolicy.CLEAR);
        }
    }

    // --------------------------------------------------------------------
    private MoeAbstractAction deleteWordAction()
    {
        return action("delete-previous-word", Category.EDIT, () -> {
            MoeEditorPane c = getTextComponent();
            MoeAbstractAction prevWordAct = actions.get(DefaultEditorKit.previousWordAction);
            int end = c.getCaretDot();
            prevWordAct.actionPerformed(false);
            int begin = c.getCaretDot();
            c.replaceText(begin, end, "");
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

    private MoeAbstractAction findAction()
    {
        return action("find", Category.MISC, () -> {
            //getEditor(e).find();
            MoeEditor editor=getEditor();
            editor.initFindPanel();
        });
    }

    private MoeAbstractAction findNextAction()
    {
        return action("find-next", Category.MISC, () -> {
            getEditor().findNext(false);
        });
    }

    private MoeAbstractAction findPrevAction()
    {
        return action("find-next-backward", Category.MISC, () -> {
            getEditor().findNext(true);
        });
    }

    private MoeAbstractAction replaceAction()
    {
        return action("replace", Category.MISC, () ->
        {
            MoeEditor editor = getEditor();
            editor.setFindPanelVisible();
            editor.showReplacePanel();
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
            getEditor().toggleInterface();
        });
    }

    private MoeAbstractAction toggleBreakPointAction()
    {
        return action("toggle-breakpoint", Category.MISC, () -> getEditor().toggleBreakpoint());
    }

    private MoeAbstractAction keyBindingsAction()
    {
        return action("key-bindings", Category.MISC, () -> editor.showPreferences(1)); // 1 is the index of the key bindings pane in the pref dialog
    }

    private MoeAbstractAction preferencesAction()
    {
        return action("preferences", Category.MISC, () -> editor.showPreferences(0)); // 0 is the index of the editor pane in the pref dialog
    }

    private MoeAbstractAction goToLineAction()
    {
        return action("go-to-line", Category.MISC, () -> getEditor().goToLine());
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
                    doc.insertString(lineStart+textStart, "// ");
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
            doc.insertString(lineStart, spaces.substring(0, tabSize));
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
}
