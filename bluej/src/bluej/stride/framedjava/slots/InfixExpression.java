/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015 Michael Kölling and John Rosenberg 
 
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
package bluej.stride.framedjava.slots;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import bluej.stride.framedjava.ast.JavaFragment;
import bluej.stride.framedjava.ast.Parser;
import javafx.application.Platform;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableStringValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;

import bluej.stride.framedjava.ast.links.PossibleLink;
import bluej.stride.framedjava.ast.links.PossibleMethodUseLink;
import bluej.stride.framedjava.ast.links.PossibleTypeLink;
import bluej.stride.framedjava.ast.links.PossibleVarLink;
import bluej.stride.framedjava.elements.CodeElement;
import bluej.stride.framedjava.slots.ExpressionSlot.SplitInfo;
import bluej.stride.generic.Frame.View;
import bluej.stride.generic.InteractionManager;
import bluej.stride.slots.EditableSlot;
import bluej.utility.Debug;
import bluej.utility.Utility;
import bluej.utility.javafx.FXConsumer;
import bluej.utility.javafx.FXRunnable;
import bluej.utility.javafx.JavaFXUtil;
import bluej.utility.javafx.MultiListener;
import bluej.utility.javafx.SharedTransition;
import bluej.utility.javafx.TextFieldDelegate;
import bluej.utility.javafx.binding.DeepListBinding;

/**
 * This class is the major part of the display and logic for expressions.  Here's how the architecture works:
 * 
 * <h3>The Expression Tree</h3>
 *     
 * This gets turned into a rich tree of expressions.  The expressions nest as follows.
 * 
 *  - There is InfixExpression (this class), which holds a sequence of operands (see the "fields" field)
 *    and in the middle of each of these is optionally an operator (Operator class, "operators" field).
 *    An operand (which implements ExpressionSlotComponent) can be:
 *      - a fairly standard text field (ExpressionSlotField),
 *      - a string literal (StringLiteralExpressionSlot) or
 *      - a bracketed expression (BracketedExpression).
 *      
 *  - A BracketedExpression is really a thin wrapper around InfixExpression.  So an expression tree is
 *    really made up primarily of InfixExpression, with each one (excluding the top-level) wrapped in
 *    a BracketedExpression.
 *    
 *  Given an expression like:
 * 
 *    1 + 2 * getX() + convert(5 + 7) - (3 * (15 + 6))
 *  
 *  We have a top-level InfixExpression.  This has an array of eleven operands:
 *  
 *   [0]: ExpressionSlotField, content: "1"
 *   [1]: ExpressionSlotField, content: "2"
 *   [2]: ExpressionSlotField, content: "getX"
 *   [3]: BracketedExpression, with an InfixExpressionSlot containing one [empty] operand (and no operators):
 *           [0]: ExpressionSlotField, content: ""
 *   [4]: ExpressionSlotField, content: ""
 *   [5]: ExpressionSlotField, content: "convert"
 *   [6]: BracketedExpression, with an InfixExpressionSlot with two operands:
 *            [0]: ExpressionSlotField, content: "5"
 *            [1]: ExpressionSlotField, content: "7"
 *          And one operator:
 *            [0]: Operator, content: "+"
 *   [7]: ExpressionSlotField, content: ""
 *   [8]: ExpressionSlotField, content: ""
 *   [9]: BracketedExpression, with an InfixExpressionSlot with three operands:
 *            [0]: ExpressionSlotField, content "3"
 *            [1]: BracketedExpression, with two operands:
 *                    [0]: ExpressionSlotField, content: "15"
 *                    [1]: ExpressionSlotField, content: "6"
 *                 and one operator:
 *                    [0]: Operator, content: "+"
 *            [2]: ExpressionSlotField, content ""
 *         And two operators:
 *            [0]: Operator, content "*"
 *            [1]: null
 *   [10]:ExpressionSlotField, content ""
 *   
 * And an array of ten operators:
 *   [0]: Operator, content: "+"
 *   [1]: Operator, content: "*"
 *   [2]: null
 *   [3]: null
 *   [4]: Operator, content: "+"
 *   [5]: null
 *   [6]: null
 *   [7]: Operator, content: "-"
 *   [8]: null
 *   [9]: null
 *
 *  There are a few rules to these slots:
 *  
 *   - Operator N is between Operand N and N+1.  So operator 0 is between operand 0 and operand 1, etc
 *   - The size of the operators array is always exactly one less than the size of the operands array
 *     (although many operator entries may be null).
 *   - Each BracketedExpression is surrounded by two null operators.  See explanation below.
 *   - Operators are only null when they are adjacent to a BracketedExpression.
 *   - A BracketedExpression never appears in the very first or very last entry in the operands array. (See below.)
 *  
 *  The non-obvious rules relate to BracketedExpression.  To start with, consider the expression "1>=2".  In
 *  a standard text editor, this a string of length 4, and there are 5 possible caret positions (before each
 *  character, and after the last one).  If we turn this into an InfixExpression, we get two operands and an operator:
 *    field 0: ExpressionSlotField, content "1"
 *    operator 0: Operator, content ">="
 *    field 1: ExpressionSlotField, content "2"
 *    
 *  In our version, we have four valid caret positions: before the '1', after the '1', before the '2', after the '2'.
 *  We lose only the caret position in the middle of the operator, but you can always alter it by going before/after it
 *  and using delete/backspace, which will chip off one character at a time from the oprator.
 *  
 *  Now consider the same expression with some brackets: "1>=(2)".  In a standard text editor, it's a string
 *  of length 6, and you have 7 caret positions.  My first attempt at turning this into an InfixExpression
 *  [NOTE: THIS IS NOT HOW IT NOW WORKS!] was:
 *    field 0: ExpressionSlotField, content "1"
 *    operator 0: Operator, content ">="
 *    field 1: BracketedExpression, containing:
 *      field 0: ExpressionSlotField, content "2"
 *      
 *  This seems logical, but we would only have 4 caret positions -- again:  before the '1', after the '1', before the '2', after the '2'.
 *  If you want to add content after the closing bracket, there is no way to do so.  If you want to delete
 *  the '=' (to turn it into strictly greater than), there is no caret position from which to do so, and nor
 *  can you add any content between the operator and the bracket.  So while this scheme correctly contains all
 *  the current text, it *does not allow all possible edits to take place*.  The fix for this is as follows:
 *  
 *  Each BracketedExpression must always have an ExpressionSlotField before and after it, with no operator
 *  inbetween the brackets and field.  These fields correspond to the space just outside the brackets, to
 *  always allow editing in those positions.  Often, these extra fields will be empty (e.g. in the case of
 *  "1*(2-3)/5", these extra fields will be empty.  However, in the case of method calls such as "move(5)",
 *  the "move" occupies the field beforehand, and in the case of casts such as "(int)x", the "x" occupies
 *  the field afterwards.
 *  
 *  This rule is what leads to several of our rules above.  Since BracketedExpression always has these fields
 *  before and after with no operators, we store null in the corresponding operator fields, and BracketedExpression
 *  can never be first or last in the InfixExpression.
 *  
 *  <h3>Floating-Point Literals</h3>
 *  
 *  One special case in expressions is floating point literals.  Given an expression like "-x.y", we would usually
 *  turn this into an InfixExpression with a minus operator and dot operator.  However, for expressions like
 *  "-9.3", this is misleading; the dot is not really an operator here (though the minus is perhaps less clear;
 *  we could think of it as a unary minus).  So we have a special case that if a dot appears in a place
 *  where it looks like part of a numeric literal (roughly: if it is preceded by a series of digits, but it gets
 *  more complicated with floating hex literals, and we also must take care around "a415.y" etc) then we do
 *  not insert an operator, but rather allow the dot in the slot itself.  The insertion and deletion logic
 *  has to work hard to handle this -- not only must it handle inserting and deleting the dot itself, but
 *  for example deleting the "g" in "g325.7" changes the content from a syntax error into a valid numeric literal. 
 *  
 *  <h3>Deleting/Inserting Operators</h3>
 *  
 *  Adding text content to an existing text field is simple -- you just change the content of that field.
 *  However, adding or deleting an operator or bracket may require merging or splitting existing slots,
 *  especially so if selection is involved.  For example, in the field "12345", selecting the "234" and hitting
 *  "(" should split the slot into two, the first half containing "1", the second half containing "5", and
 *  inbetween those there will be a BracketedExpression inserted which contains "234".
 *  
 *  There is complicated logic to the insertion, which is covered in the comments for the insertChar method, below.
 *  
 *  <h3>Selection</h3>
 *  
 *  InfixExpression items support selection.  The logic for selection in expressions is similar to that for frames:
 *  a bracketed expression is a whole unit (like a frame is), and thus you either select the whole bracket,
 *  or none of it.  Other than brackets, you can select across any parts of an expression.  If you consider
 *  the expression "1+2+(3*4)+5", then the valid selections you can make from just before the 2 are
 *  (using curly brackets for selection):
 *    - "1{+}2+(3*4)+5" [heading backwards]
 *    - "{1+}2+(3*4)+5" [heading backwards]
 *    - "1+{2}+(3*4)+5" [this, and rest, heading forwards]
 *    - "1+{2+}(3*4)+5"
 *    - "1+{2+(3*4)}+5"
 *    - "1+{2+(3*4)+}5"
 *    - "1+{2+(3*4)+5}"
 *    
 *  The valid selections from just after the 3 are:
 *   - "1+2+({3}*4)+5" [heading backwards]
 *   - "1+2+(3{*}4)+5" [heading forwards]
 *   - "1+2+(3{*4})+5" [heading forwards]
 *   
 *  That is, your selection cannot leave a bracketed expression that you start inside of, because
 *  allowing the selection to leave the bracket would mean you were selecting part of a bracketed expression.
 * 
 *  <h3>Methods with Underscore Names</h3>
 *  
 *  In some cases, there are methods that we want to test, where we need to pass different parameters
 *  to the usual public methods.  To facilitate this, some methods (like insert), have a package-visible
 *  version with an underscore ("insert_") which is used directly by the tests, and a public version
 *  without the underscore ("insert") that should be used in all normal, non-test code
 *  
 *  <h3>Caret Position</h3>
 *  
 *  Because we have a rich structure in the expression, the notion of caret position has changed.  In a normal text slot,
 *  it would just be an integer, indicating the position along the string.  In our expression slots, it is
 *  a list of integers (singly linked list, in CaretPos).  The meaning of each integer depends on the component it is applied to.
 *  
 *  At the outer-most level, we have an InfixExpression.  Here, the first integer is an index into the "fields" array,
 *  indicating which slot we are in.  You then strip off the first integer, and use the rest of the list to find
 *  the position in the subfield.  If it's an ExpressionSlotField, the index is simply a standard caret position.
 *  If it's a BracketedExpression, it applies to the contained InfixExpression.
 *  
 *  So ultimately, the CaretPos is a list of integers where the last one applies to an ExpressionSlotField, and the preceding
 *  ones are indexes into InfixExpression's "fields" arrays.
 */

//package-visible
class InfixExpression implements TextFieldDelegate<ExpressionSlotField>
{
    // Regex matching JLS "Digits"; underscores can appear within
    private final static String DIGITS_REGEX = "\\d([0-9_]*\\d)?";
    private final static String HEX_DIGITS_REGEX = "[0-9A-Fa-f]([0-9A-Fa-f_]*[0-9A-Fa-f])?";
    
    // fields is always 1 longer than operators. Always an ExpressionSlotField in first and last position (which may be same, when size 1).
    private final ObservableList<ExpressionSlotComponent> fields = FXCollections.observableArrayList();
    // Operator 0 is between field 0 and field 1.  Operator N trails field N.
    // Can be null when the operator is effectively a bracket.
    private final ObservableList<Operator> operators = FXCollections.observableArrayList();

    private final ObservableList<Node> extraPrefix;
    private final ObservableList<Node> extraSuffix;
    
    //private final FlowPane components = new FlowPane();    
    //private final HBox components = new HBox();
    private final ObservableList<Node> components = FXCollections.observableArrayList();
    private final BracketedExpression parent; // null if top-level
    private final Set<Character> closingChars = new HashSet<>(); // Empty if we are top-level
    
    private final InteractionManager editor;
    private final ExpressionSlot<?> slot; // Can be null when testing, but otherwise non-null
    
    private final StringProperty textProperty = new SimpleStringProperty();
    private final BooleanProperty previewingJavaRange = new SimpleBooleanProperty(false);
    private final Label startRangeLabel;
    private final Label endRangeLabel;
    /**
     * The caret position for the start of the selection (null if and only if no selection)
     */
    private CaretPos anchorPos;
    private EditableSlot bindedSlot;

    /**
     * Create top-level InfixExpression, just inside the ExpressionSlot
     */
    // package-visible
    InfixExpression(InteractionManager editor, ExpressionSlot slot, String stylePrefix)
    {
        // TODO make use of stylePrefix
        this(editor, slot, "", null);
    }
    
    /**
     * Create InfixExpression just inside the given BracketedExpression.
     * 
     * @param initialContent The initial content.  This should be suitable for putting into a single
     *    ExpressionSlotField; it should not contain any operators or brackets, etc.  If you need
     *    to add rich content, pass "" for this parameter and insert the rich content afterwards.
     */
    //package-visible
    InfixExpression(InteractionManager editor, ExpressionSlot slot, String initialContent, BracketedExpression wrapper, Character... closingChars)
    {
        this.editor = editor;
        this.parent = wrapper;
        this.closingChars.addAll(Arrays.asList(closingChars));
        this.slot = slot;
        
        this.textProperty.set(initialContent);
        // When starting, add just one empty field:
        fields.add(makeNewField(initialContent, false));

        startRangeLabel = new Label(lang.stride.Utility.class.getName() + "(");
        endRangeLabel = new Label(")");
        extraPrefix = JavaFXUtil.listBool(previewingJavaRange, startRangeLabel);
        extraSuffix = JavaFXUtil.listBool(previewingJavaRange, endRangeLabel);
        
        new DeepListBinding<Node>(components) {

            @Override
            protected Stream<ObservableList<?>> getListenTargets()
            {
                return Stream.concat(Stream.of(fields, operators, extraPrefix, extraSuffix), fields.stream().map(ExpressionSlotComponent::getComponents));
            }

            @Override
            protected Stream<Node> calculateValues()
            {
                // Important that we flatMap after interleaving, to preserve ordering:
                return Utility.concat(
                        extraPrefix.stream(),
                        Utility.interleave(
                                fields.stream().map(c -> c.getComponents().stream()),
                                operators.stream().map(o -> o == null ? Stream.<Node>empty() : Stream.of(o.getNode())))
                             .flatMap(x -> x),
                        extraSuffix.stream()
                       );
            }

        }.startListening();
        
        // We want to bind the textProperty item to the expression content.
        // There's several ways to do this, but we do it as follows.
        
        final ObservableList<ObservableStringValue> strings = FXCollections.observableArrayList();
        
        // First, we use a MultiListener to attach listeners to each observable string
        // in the list, and when any change, we combine their values into textProperty.
        // This must come before we bind to strings, so that the listeners fire on the first
        // update of the strings list:
        ChangeListener<String> individualListener = (a, b, c) -> {
            textProperty.set(strings.stream().map(ObservableStringValue::get).collect(Collectors.joining()));
        };
        
        MultiListener<ObservableStringValue> stringListener = new MultiListener<ObservableStringValue>(s -> {
            s.addListener(individualListener);
            return () -> s.removeListener(individualListener);
        });
        
        strings.addListener((ListChangeListener<ObservableStringValue>)c -> {
            stringListener.listenOnlyTo(strings.stream());
            // Need to update content, too:
            individualListener.changed(null, null, null);
        });

        // Second, we bind a list of observable strings to the observable strings in fields and operators,
        // using a DeepListBinding similar to the one above:
        new DeepListBinding<ObservableStringValue>(strings) {
            
            @Override
            protected Stream<ObservableList<?>> getListenTargets()
            {
                return Stream.of(fields, operators);
            }

            @Override
            protected Stream<ObservableStringValue> calculateValues()
            {
                // Must filter out nulls after interleaving, to preserve ordering:
                return Utility.interleave(
                                fields.stream().map(f -> f.textProperty()),
                                operators.stream().map(o -> o == null ? null : o.textProperty()))
                       .filter(x -> x != null);
            }

        }.startListening();
        
        fields.addListener((ListChangeListener)c -> {
            // If we are not a single field, remove prompts on all fields:
            if (fields.size() != 1)
            {
                for (ExpressionSlotComponent comp : fields)
                {
                    if (comp instanceof ExpressionSlotField)
                    {
                        ((ExpressionSlotField)comp).setPromptText("");
                    }
                }
            }   
        });
        
        
        // Now we calculate precedence as follows:
        // - We find the lowest precedence operator (preferring leftmost).
        //   - We recursively traverse the LHS and RHS of this operator with same algorithm.
        // - On return, if lowest operator in LHS or RHS is identical to outer, we take
        components.addListener((ListChangeListener)c -> 
            calculatePrecedences(operators, fields.stream().map(ExpressionSlotComponent::isFieldAndEmpty).limit(operators.size()).collect(Collectors.toList()))
        );
        
        updateBreaks();
        JavaFXUtil.addChangeListener(textProperty, value -> updateBreaks());
    }

    private void updateBreaks()
    {
        // Update possible breaks.
        // Breaks are not allowed between a method name and the opening bracket:
        
        // Spot the method calls and inform the brackets:
        // Start at second item because first can't be a bracket:
        for (int i = 1; i < fields.size(); i++)
        {
            if (fields.get(i) instanceof BracketedExpression)
            {
                // A method call is a bracketed expression with non-empty
                // field directly before it:
                ((BracketedExpression)fields.get(i)).notifyIsMethodParams(
                    fields.get(i-1) instanceof ExpressionSlotField
                        && !fields.get(i-1).isFieldAndEmpty());
            }
        }
    }

    // package-visible and static for testing
    static Operator.OpPrec calculatePrecedences(List<Operator> ops, List<Boolean> isUnary)
    {   
        int lowestPrec = Integer.MAX_VALUE;
        int lowestIndex = -1;
        for (int i = 0; i < ops.size(); i++)
        {
            if (ops.get(i) == null)
                continue;
            if (ops.get(i).get().equals("."))
            {
                // always DOT priority; skip it:
                ops.get(i).setPrecedence(Operator.Precedence.DOT);
                continue;
            }
            else if (ops.get(i).get().equals(","))
            {
                // always COMMA priority; skip it:
                ops.get(i).setPrecedence(Operator.Precedence.COMMA);
                continue;
            }
            else if (ops.get(i).get().equals("new "))
            {
                // always COMMA priority; skip it:
                ops.get(i).setPrecedence(Operator.Precedence.NEW);
                continue;
            }
            
            int prec = Operator.getOperatorPrecedence(ops.get(i).get(), isUnary.get(i).booleanValue());
            
            // Prefer left-hand op, needs to be strictly lower
            if (prec < lowestPrec)
            {
                lowestPrec = prec;
                lowestIndex = i;
            }
        }
        if (lowestIndex != -1)
        {
            // Split the list into left and right:
            List<Operator> lhs = ops.subList(0, lowestIndex);
            List<Boolean> lhsUnary = isUnary.subList(0, lowestIndex);
            List<Operator> rhs = ops.subList(lowestIndex + 1, ops.size());
            List<Boolean> rhsUnary = isUnary.subList(lowestIndex + 1, ops.size());
            
            Operator.OpPrec lhsPrec = calculatePrecedences(lhs, lhsUnary);
            Operator.OpPrec rhsPrec = calculatePrecedences(rhs, rhsUnary);
            
            int ourLevel;
            if (lhsPrec.prec == lowestPrec || rhsPrec.prec == lowestPrec ||
                    (lhsPrec.prec == -1 && rhsPrec.prec == -1))
            {
                // Same precendence
                ourLevel = Math.max(lhsPrec.levels, rhsPrec.levels);
            }
            else
            {
                // One higher precedence:
                ourLevel = 1 + Math.max(lhsPrec.levels, rhsPrec.levels);
            }
            
            ops.get(lowestIndex).setPrecedence(Operator.getPrecForLevel(ourLevel));
            return new Operator.OpPrec(lowestPrec, ourLevel);
        }
        else
        {
            // No operators (none, or all are null)
            return new Operator.OpPrec(-1, 0);
        }
    }

    private static boolean precedesDotInFloatingPointLiteral(String before) {
        return before.matches("\\A\\s*[+-]?" + DIGITS_REGEX + "\\z")
                || before.matches("\\A\\s*0x" + HEX_DIGITS_REGEX + "\\z");
    }

    private ExpressionSlotField makeNewField(String content, boolean stringLiteral)
    {
        ExpressionSlotField f = new ExpressionSlotField(this, content, stringLiteral);
        if (editor != null) // Can be null during testing
            editor.setupFocusableSlotComponent(slot, f.getNodeForPos(null), true, slot.getHints());
        f.onKeyPressedProperty().set(event -> {
            //Which key?
            switch (event.getCode())
            {
                case ENTER:
                    slot.enter();
                    event.consume();
                    break;
                case UP:
                    slot.up();
                    event.consume();
                    break;
                case DOWN:
                    slot.down();
                    event.consume();
                    break;
                case SPACE:
                    if (event.isControlDown())
                    {
                        slot.showSuggestionDisplay(f, f.getCurrentPos().index, stringLiteral);
                        event.consume();
                    }
                    break;
                default:
                    if (slot.checkFilePreviewShortcut(event.getCode()))
                        event.consume();
                    break;
            }
        });
        
        // Put the handlers for links on the field:
        f.addEventHandler(MouseEvent.MOUSE_MOVED, e -> {
            CaretPos relNearest = getNearest(e.getSceneX(), e.getSceneY(), false, Optional.empty()).getPos();
            CaretPos absNearest = absolutePos(relNearest);
            f.setPseudoclass("bj-hyperlink", e.isShortcutDown() && slot.getOverlay().hoverAtPos(slot.getTopLevel().caretPosToStringPos(absNearest, false)) != null);
        });
        f.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            if (e.getClickCount() > 1) // Double and triple clicks will be handled by the text field 
                return;
            // check for click on underlined region
            CaretPos relNearest = getNearest(e.getSceneX(), e.getSceneY(), false, Optional.empty()).getPos();
            CaretPos absNearest = absolutePos(relNearest);
            Utility.ifNotNull(slot.getOverlay().hoverAtPos(slot.getTopLevel().caretPosToStringPos(absNearest, false)), FXRunnable::runLater);
        });
        
        return f;
    }
    
    /**
     * Position and focus the caret at the given position.  Returns the Node that will receive
     * focus.
     */
    // package-visible
    Node positionCaret(CaretPos pos)
    {
        if (pos == null) // This can happen if we don't want to focus, e.g. we are moving to next slot
            return null;
        pos = pos.normalise();
        if (pos.index == -1)
        {
            return parent.positionParentPos(pos.subPos);
        }
        else
        {
            ExpressionSlotComponent foc = fields.get(pos.index);
            return foc.focusAtPos(pos.subPos);
        }
    }
    
    /**
     * Draws the selection from anchorPos (if null, draw no selection) to the
     * position given as a parameter.  It does not matter whether anchorPos
     * is before or after cur.  If they are equal, no selection is drawn.
     */
    public void drawSelection(CaretPos cur)
    {
        if (anchorPos == null || anchorPos.equals(cur))
        {
            slot.clearSelection(false);
        }
        else
        {
            CaretPos start, end;
            if (anchorPos.before(cur)) {
                start = anchorPos;
                end = cur;
            }
            else {
                start = cur;
                end = anchorPos;
            }
            
            slot.drawSelection(getAllStartEndPositionsBetween(start, end).collect(Collectors.toList()));
        }
    }

    /**
     * Finds all the TextOverlayPosition items at the beginning and ends of all ExpressionSlotField
     * and Operator items (no matter how deeply nested) between the two caret positions, book-ended by the given caret
     * positions themselves.
     *
     * We need this to calculate where to draw the selection boxes and error underlines for
     * multi-field selections/errors.
     * 
     * @param start The start position, or null to mean from the start of this entire expression
     * @param end The end position, or null to mean to the end of this entire expression 
     */    
    // package-visible
    Stream<TextOverlayPosition> getAllStartEndPositionsBetween(CaretPos start, CaretPos end)
    {
        if (start == null) {
            start = new CaretPos(0, getFirstField().getStartPos());
        }
        boolean useVeryEnd = end == null;
        if (end == null) {
            end = new CaretPos(fields.size() - 1, getLastField().getEndPos());
        }
        
        // Single field is a simple case:
        int startIndex = start.index;
        int endIndex = end.index;
        if (startIndex == endIndex) {
            Stream<TextOverlayPosition> s =  fields.get(startIndex).getAllStartEndPositionsBetween(start.subPos, end.subPos);
            if (useVeryEnd)
                s = Stream.concat(s, Stream.of(((ExpressionSlotField) fields.get(fields.size() - 1)).calculateOverlayEnd()));
            return s;
        }
        
        Stream<TextOverlayPosition> s = fields.get(startIndex).getAllStartEndPositionsBetween(start.subPos, null);
        for (int i = startIndex + 1; i < endIndex; i++)
        {
            if (operators.get(i - 1) != null) {
                s = Stream.concat(s, operators.get(i - 1).getStartEndPositions(this));
            }
            s = Stream.concat(s, fields.get(i).getAllStartEndPositionsBetween(null, null));
        }
        if (operators.get(endIndex - 1) != null)
            s = Stream.concat(s, operators.get(endIndex - 1).getStartEndPositions(this));
        s = Stream.concat(s, fields.get(endIndex).getAllStartEndPositionsBetween(null, end.subPos));

        if (useVeryEnd)
            s = Stream.concat(s, Stream.of(((ExpressionSlotField) fields.get(fields.size() - 1)).calculateOverlayEnd()));

        return s;
    }
    
    // As position on overlay:
    public TextOverlayPosition calculateOverlayPos(CaretPos p)
    {
        //if (p == null)
        //    return new TextOverlayPosition(0.0, 0.0, 5.0);
        
        ExpressionSlotComponent f = fields.get(p.index);
        return f.calculateOverlayPos(p.subPos);
    }
    
    public double sceneToOverlayX(double sceneX)
    {
        return slot.sceneToOverlayX(sceneX);
    }
    
    public double sceneToOverlayY(double sceneY)
    {
        return slot.sceneToOverlayY(sceneY);
    }
    
    public void deselect()
    {
        anchorPos = null;
        drawSelection(null);
    }
    
    @Override
    public void backwardAtStart(ExpressionSlotField f)
    {
        backwardAtStart((ExpressionSlotComponent) f);
    }
    
    /**
     * Handle moving backwards (left) when at the start of the given field.
     * 
     * This will either move to the end of the appropriate preceding field, or if the passed
     * parameter is the first field, will take the appropriate action via its surrounding
     * BracketedExpression/ExpressionSlot
     */
    public void backwardAtStart(ExpressionSlotComponent f)
    {
        int i = findField(f);
        if (i == -1) throw new IllegalStateException();
        
        if (i > 0) {
            fields.get(i - 1).focusAtEnd();
        }
        else
        {
            if (parent != null) {
                parent.focusBefore();
            }
            else { // We are top-level:
                slot.getSlotParent().focusLeft(slot);
            }
        }
    }
    
    @Override
    public void forwardAtEnd(ExpressionSlotField f)
    {
        forwardAtEnd((ExpressionSlotComponent)f);
    }
    
    /**
     * Handle moving forwards (right) when at the start of the given field.
     * 
     * This will either move to the beginning of the appropriate following field, or if the passed
     * parameter is the last field, will take the appropriate action via its surrounding
     * BracketedExpression/ExpressionSlot
     */
    public void forwardAtEnd(ExpressionSlotComponent f)
    {
        int i = findField(f);
        if (i == -1) throw new IllegalStateException();
        
        if (i < fields.size() - 1) {
            fields.get(i + 1).focusAtStart();
        }
        else
        {
            if (parent != null)
                parent.focusAfter();
            else // We are top-level:
                slot.getSlotParent().focusRight(slot);
        }
    }

    /**
     * Handle Home being pressed in the given field
     */
    // TODO should home/end go up to parent?
    @Override
    public boolean home(ExpressionSlotField f)
    {
        getFirstField().focusAtStart();
        return true;
    }

    /**
     * Handle End being pressed in the given field
     */
    @Override
    public boolean end(ExpressionSlotField f, boolean asPartOfNextWordCommand)
    {
        if (asPartOfNextWordCommand)
            f.focusAtEnd();
        else
            end();
        return true;
    }

    /**
     * Handle Shift-Home being pressed in the given field
     */
    @Override
    public boolean selectHome(ExpressionSlotField id, int caretPos)
    {
        int i = findField(id);
        setAnchorIfUnset(new CaretPos(i, new CaretPos(caretPos, null)));
        int dest = fields.get(i) instanceof StringLiteralExpression ? i : 0;
        fields.get(dest).focusAtStart();
        drawSelection(new CaretPos(dest, new CaretPos(0, null)));
        return true;
    }

    /**
     * Handle Shift-End being pressed in the given field
     */
    @Override
    public boolean selectEnd(ExpressionSlotField id, int caretPos)
    {
        int i = findField(id);
        setAnchorIfUnset(new CaretPos(i, new CaretPos(caretPos, null)));
        int dest = fields.get(i) instanceof StringLiteralExpression ? i : fields.size() - 1;
        fields.get(dest).focusAtEnd();
        drawSelection(new CaretPos(dest, fields.get(dest).getEndPos()));
        return true;
    }
    
    //package-visible
    ExpressionSlotField getFirstField()
    {
        return (ExpressionSlotField)fields.get(0);
    }

    private ExpressionSlotField getLastField()
    {
        return (ExpressionSlotField)fields.get(fields.size() - 1);
    }

    @Override
    public boolean previousWord(ExpressionSlotField f, boolean atStart) 
    {
        if (atStart) {
            backwardAtStart(f);
            return true;
        }
        // Let the field handle it if not at start:
        return false;
    }
    
    @Override
    public boolean nextWord(ExpressionSlotField f, boolean atEnd)
    {
        if (atEnd) {
            forwardAtEnd(f);
            return true;
        }
     // Let the field handle it if not at end:
        return false;
    }
    
    @Override
    public boolean endOfNextWord(ExpressionSlotField f, boolean atEnd)
    {
        return nextWord(f, atEnd);
    }
    
    @Override
    public boolean selectAll(ExpressionSlotField f)
    {
        home(null);
        // Arguments are position we are selecting *from*, not to:
        selectEnd(getFirstField(), 0);
        return true;
    }

    @Override
    public boolean selectNextWord(ExpressionSlotField f)
    {
        setAnchorIfUnset(getCurrentPos());
        if (f.getCurrentPos().equals(f.getEndPos()))
        {
            int i = findField(f);
            if (fields.get(i) instanceof StringLiteralExpression)
                return false;
            else
                selectForward(f, f.getCurrentPos().index, true);
        }
        else
        {
            //nextWord can clobber our anchor in end(), so we save and restore it:
            CaretPos anch = this.anchorPos;
            f.nextWord();
            this.anchorPos = anch;
            drawSelection(new CaretPos(findField(f), f.getCurrentPos()));
        }
        return true;
    }
    
    @Override
    public boolean selectPreviousWord(ExpressionSlotField f)
    {
        setAnchorIfUnset(getCurrentPos());
        if (f.getCurrentPos().equals(f.getStartPos()))
        {
            int i = findField(f);
            if (fields.get(i) instanceof StringLiteralExpression)
                return false;
            else
                selectBackward(f, 0);
        }
        else
        {
            f.previousWord();
            drawSelection(new CaretPos(findField(f), f.getCurrentPos()));
        }
        return true;
    }
    
    @Override
    public boolean cut()
    {
        copy();
        deleteSelection();
        return true;
    }
    
    @Override
    public void moveTo(double sceneX, double sceneY, boolean setAnchor)
    {
        CaretPos pos = getNearest(sceneX, sceneY, true, Optional.empty()).getPos();
        positionCaret(pos);
        if (setAnchor) {
            anchorPos = pos;
        }
    }
    
    // Package-visible
    PosAndDist getNearest(double sceneX, double sceneY, boolean canDescend, Optional<Integer> restrictTo)
    {
        PosAndDist nearest = new PosAndDist();
        for (int i = 0; i < fields.size();i++) {
            final int index = i;
            if (restrictTo.isPresent() && restrictTo.get() != i)
                continue;
            nearest = PosAndDist.nearest(nearest, fields.get(i).getNearest(sceneX, sceneY, canDescend, anchorPos != null && anchorPos.index == i).copyAdjustPos(p -> new CaretPos(index, p)));
        }
        return nearest;
    }
    
    @Override
    public void selectTo(double sceneX, double sceneY)
    {
        // Anchor pos should already be set, so we just have to move caret
        
        // Rules are:
        // you can't select into brackets or string literals
        // you can only select at same level.
        CaretPos pos = getNearest(sceneX, sceneY, false, anchorPos != null && fields.get(anchorPos.index) instanceof StringLiteralExpression ? Optional.of(anchorPos.index) : Optional.empty()).getPos();
        positionCaret(pos);
        drawSelection(pos);
    }
    
    @Override
    public void selected()
    {
        if (anchorPos == null || anchorPos.equals(getCurrentPos()))
        {
            // If they drag and end up selecting nothing (drag ends where it began -- drag may
            // also just be an over-long mouse press) then don't treat it as a selection:
            anchorPos = null;
            drawSelection(null);
        }
    }
    
    private void setAnchorIfUnset(CaretPos caretPos)
    {
        if (anchorPos == null)
            anchorPos = caretPos;   
    }
    
    @Override
    public boolean selectBackward(ExpressionSlotField f, int posInSlot)
    {
        int start = findField(f);
        
        if (posInSlot > 0) {
            setAnchorIfUnset(new CaretPos(start, new CaretPos(posInSlot, null)));
            CaretPos newPos = new CaretPos(start, new CaretPos(posInSlot - 1, null));
            drawSelection(newPos);
            positionCaret(newPos);
            return true;
        }

        // Can't select beyond beginning of string literal:
        if (fields.get(start) instanceof StringLiteralExpression)
            return false;
        
        // Look at previous fields until we find one we can select into
        for (int i = start - 1; i >= 0; i--) {
            CaretPos pos = fields.get(i).getSelectIntoPos(true);
            if (pos != null) {
                pos = new CaretPos(i, pos);
                setAnchorIfUnset(new CaretPos(start, new CaretPos(posInSlot, null)));
                positionCaret(pos);
                drawSelection(pos);
                return true;
            }
        }
        return false;
    }
    
    @Override
    public boolean selectForward(ExpressionSlotField f, int posInSlot, boolean atEnd)
    {
        int start = findField(f);
        
        if (!atEnd) {
            setAnchorIfUnset(new CaretPos(start, new CaretPos(posInSlot, null)));
            CaretPos newPos = new CaretPos(start, new CaretPos(posInSlot + 1, null));
            drawSelection(newPos);
            positionCaret(newPos);
            return true;
        }

        // Can't select beyond end of string literal:
        if (fields.get(start) instanceof StringLiteralExpression)
            return false;
        
        // Let's look at next field:
        // Keep going right until we find a non-compound field:
        for (int i = start + 1; i < fields.size(); i++) {
            CaretPos pos = fields.get(i).getSelectIntoPos(false);
            if (pos != null) {
                pos = new CaretPos(i, pos);
                setAnchorIfUnset(new CaretPos(start, new CaretPos(posInSlot, null)));
                positionCaret(pos);
                drawSelection(pos);
                return true;
            }
        }
        return false;
    }
    
    public void delete(ExpressionSlotField f, int start, int end)
    {
        f.setText(f.getText().substring(0, start) + f.getText().substring(end));
    }
    
    @Override
    public boolean copy()
    {
        if (anchorPos == null)
            return true;
        
        CaretPos cur = getCurrentPos();
        CaretPos start, end;
        if (anchorPos.before(cur)) {
            start = anchorPos;
            end = cur;
        }
        else {
            start = cur;
            end = anchorPos;
        }
        
        String s = getCopyText(start, end);
        Clipboard.getSystemClipboard().setContent(Collections.singletonMap(DataFormat.PLAIN_TEXT, s));
        return true;
    }
    
    public String getCopyText(CaretPos start, CaretPos end)
    {
        if (start == null)
            start = new CaretPos(0, new CaretPos(0, null));
        if (end == null)
            end = new CaretPos(fields.size() - 1, getLastField().getEndPos());
        
        if (start.index == end.index) {
            return fields.get(start.index).getCopyText(start.subPos, end.subPos);
        }
        StringBuilder b = new StringBuilder();
        b.append(fields.get(start.index).getCopyText(start.subPos, null));
        if (operators.get(start.index) != null)
            b.append(operators.get(start.index).getCopyText());
        for (int i = start.index + 1; i < end.index; i++) {
            b.append(fields.get(i).getCopyText(null, null));
            if (operators.get(i) != null)
                b.append(operators.get(i).getCopyText());
        }
        b.append(fields.get(end.index).getCopyText(null, end.subPos));
        return b.toString();
    }

    // start is inclusive, end is exclusive
    private String getJavaCodeForFields(int start, int end)
    {
        StringBuilder b = new StringBuilder();
        
        // If we have any .. operators, they are converted into a function call.
        
        for (int i = start; i < end; i++)
        {
            b.append(fields.get(i).getJavaCode());
            if (i < operators.size() && operators.get(i) != null && i < end - 1)
                b.append(operators.get(i).getJavaCode());
        }
        return b.toString();
    }

    public String getJavaCode()
    {
        StringBuilder b = new StringBuilder();
        // If we have any .. operators, they are converted into a function call
        
        int closing = 0;
        
        int last = 0;
        for (int i = 0; i < operators.size(); i++)
        {
            if (operators.get(i) != null)
            {
                String op = operators.get(i).get();
                // The .. operator is the second lowest priority.  We might have an expression
                // like a, 6 .. 5 + x, c
                // In this case, the range operator binds lower than the commas, so we basically have
                // three parameters: "a", "6 .. 5 + x" and "c".  The "5 + x" binds tighter than .., so
                // forms the right-hand side.
                
                // Thus our logic is as follows.  If the operator is a comma, we add everything we have seen up
                // to that point, and reset the "last" index.  If the operator is a range, we
                // add everything since "last" (the beginning, or last comma) to the output, and again
                // reset last, to point to the range operator.  Next comma or range operator, we will add
                // everything from last on.
                if (op.equals(".."))
                {
                    b.append(lang.stride.Utility.class.getName() + ".makeRange(");
                    b.append(getJavaCodeForFields(last, i + 1));
                    b.append(", ");
                    last = i + 1;
                    closing += 1;
                }
                else if (op.equals(",")) // Commas are lower precedence
                {
                    b.append(getJavaCodeForFields(last, i + 1));
                    for (; closing > 0; closing--)
                        b.append(")");
                    b.append(", ");
                    last = i + 1;
                }
            }
        }
        // Add everything since last comma (if anything):
        b.append(getJavaCodeForFields(last, fields.size()));
        for (; closing > 0; closing--)
            b.append(")");
        
        return b.toString();
    }
    
    @Override
    public boolean deleteSelection()
    {
        return deleteSelection_(getCurrentPos()) != null;
    }
    
    CaretPos deleteSelection_(CaretPos cur)
    {
        if (anchorPos == null || anchorPos.equals(cur))
        {
            anchorPos = null;
            return null;
        }
        CaretPos start, end;
        if (anchorPos.before(cur))
        {
            start = anchorPos;
            end = cur;
        }
        else
        {
            start = cur;
            end = anchorPos;
        }
        anchorPos = null;
        // If the deletion is within a single field, and its not a plain text field, just delegate to that field:
        if (start.index == end.index)
        {
            if (fields.get(start.index) instanceof BracketedExpression)
            {
                InfixExpression nested = ((BracketedExpression)fields.get(start.index)).getContent();
                nested.setAnchorIfUnset(start.subPos);
                return new CaretPos(start.index, nested.deleteSelection_(end.subPos));
            }
            else if (fields.get(start.index) instanceof StringLiteralExpression)
            {
                StringLiteralExpression s = (StringLiteralExpression) fields.get(start.index);
                ExpressionSlotField f = s.getField();
                f.setText(f.getText().substring(0, start.subPos.index) + f.getText().substring(end.subPos.index));
                return start;
            }
        }
        // Collapse the remaining before/after content into startField, and delete necessary fields and operators:
        ExpressionSlotField startField = (ExpressionSlotField)fields.get(start.index);
        ExpressionSlotField endField = (ExpressionSlotField)fields.get(end.index);
        startField.setText(startField.getText().substring(0, start.subPos.index) + endField.getText().substring(end.subPos.index));
        for (int i = start.index + 1; i <= end.index;i++)
        {
            operators.remove(start.index);
            fields.remove(start.index + 1);
        }
        CaretPos pos = checkFieldChange(start.index, start);
        positionCaret(pos);
        if (slot != null)
        {
            slot.clearSelection(true);
        }
        return pos;
    }
    
    /**
     * Deletes the character before the given position in the text field.
     * The atStart parameter is a bit redundant (check if posInField == 0), but mirrors that of
     * deleteNext.
     */
    public boolean deletePrevious(ExpressionSlotField f, int posInField, boolean atStart)
    {
        positionCaret(deletePrevious_(f, posInField, atStart));
        return true;
    }
    
    public CaretPos deletePreviousAtPos(CaretPos p)
    {
        ExpressionSlotComponent c = fields.get(p.index);
        if (c instanceof ExpressionSlotField)
        {
            return deletePrevious_((ExpressionSlotField)c, p.subPos.index, p.subPos.index == 0);
        }
        else if (c instanceof StringLiteralExpression)
        {
            return deletePrevious_(((StringLiteralExpression)c).getField(), p.subPos.index, p.subPos.index == 0);
        }
        else if (c instanceof BracketedExpression)
        {
            return new CaretPos(p.index, ((BracketedExpression)c).getContent().deletePreviousAtPos(p.subPos));
        }
        throw new IllegalStateException();
    }

    // package-visible for testing, implementation of deletePrevious
    CaretPos deletePrevious_(ExpressionSlotField f, int posInField, boolean atStart)
    {
        int index = findField(f);
        if (atStart)
        {
            // If it is not the first field in the InfixExpression:
            if (index > 0)
            {
                Operator prev = operators.get(index - 1);
                
                if (prev == null)
                {
                    // No operator, must be closing bracket/quote beforehand
                    // In this case, we want to get rid of the brackets/quotes, and fold the contents into the slots adjacent to the brackets
                    boolean inString = fields.get(index) instanceof StringLiteralExpression;
                    return flattenCompound(inString ? index : index - 1, !inString);
                }
                else
                {
                    // Operator beforehand, work out what backspace will do to it:
                    String op = prev.get();
                    // If it's longer than 1 char (and not the new operator), just lop off
                    // the last character of the operator:
                    if (op.length() > 1 && !op.equals("new "))
                    {
                        prev.set(op.substring(0, op.length() - 1));
                        return checkFieldChange(index - 1, new CaretPos(index, new CaretPos(0, null)));
                    }
                    else
                    {
                        // Otherwise, we will delete the operator and fold the joined contents of the surrounding
                        // fields, and any remaining content from the operator
                        // (only applicable with "new " operator) into the joined field
                        operators.remove(index - 1);
                        ExpressionSlotField prevField = (ExpressionSlotField)fields.get(index - 1);
                        String opRemaining = "";
                        if (op.equals("new "))
                        {
                            opRemaining = "new"; // Delete the space
                        }
                        int newPos = prevField.getText().length() + opRemaining.length();
                        prevField.setText(prevField.getText() + opRemaining + f.getText());
                        fields.remove(index);
                        
                        //TODO act different if compound is involved
                        return checkFieldChange(index - 1, new CaretPos(index - 1, new CaretPos(newPos, null)));
                    }
                }
            }
            else
            {
                // If it is the first field, delegate to parent or slot:
                if (parent != null) {
                    return new CaretPos(-1, parent.flatten(false));
                }
                if (slot != null) {
                    if (slot.backspaceAtStart()) {
                        return null;
                    }
                    return new CaretPos(0, new CaretPos(0, null));
                }
                else {
                    if (editor != null) { // Only complain if we are not testing
                        throw new IllegalStateException("No parent nor slot");
                    }
                    return new CaretPos(0, new CaretPos(0, null));
                }
            }
        }
        else {
            // If we aren't at the start of our field, it's easy: delete character and call checkFieldChange
            String s = f.getText();
            f.setText(s.substring(0, posInField - 1) + s.substring(posInField));
            CaretPos p = checkFieldChange(index, new CaretPos(index, new CaretPos(posInField - 1, null)));
            return p;
        }
    }
    
    // Package visible
    CaretPos flattenCompound(ExpressionSlotComponent item, boolean caretAtEnd)
    {
        return flattenCompound(fields.indexOf(item), caretAtEnd);
    }
    
    /**
     * Given the index of a compound field (e.g. BracketedExpression, StringLiteralExpressionField),
     * takes its content and flattens it, i.e. unwraps it from the BracketedExpression and inserts it
     * into the "index" position in this InfixExpression.  Used when deleting brackets but you want
     * to retain the content.
     */
    private CaretPos flattenCompound(int index, boolean atEnd)
    {
        ExpressionSlotField fieldBefore = (ExpressionSlotField) fields.get(index - 1);
        String after = fields.get(index+1).getCopyText(null, null);
        String content = fields.get(index).getCopyText(fields.get(index).getStartPos(), fields.get(index).getEndPos());
        
        // Must remove second field first:
        fields.remove(index+1);
        if (operators.remove(index) != null) throw new IllegalStateException();
        fields.remove(index);
        if (operators.remove(index-1) != null) throw new IllegalStateException();
        
        int len = fieldBefore.getText().length();
        CaretPos mid = insert_(fieldBefore, len, content, false);
        // TODO stop using testing method here:
        testingInsert(mid, after);
        if (atEnd)
            return mid;
        return new CaretPos(index - 1, new CaretPos(len, null));
    }
    
    /**
     * Deletes the next character after the given position.  atEnd is a convenience
     * variable indicating if the deletion is requested at the last caret position
     * in the given field.
     */
    public boolean deleteNext(ExpressionSlotField f, int posInField, boolean atEnd)
    {
        positionCaret(deleteNext_(f, posInField, atEnd));
        return true;
    }
    
    // package-visible (for testing), implementation of deleteNext
    CaretPos deleteNext_(ExpressionSlotField f, int posInField, boolean atEnd)
    {
        int index = findField(f);
        // Have we requested a delete-next at the end of a field
        if (atEnd)
        {
            // If we are not the last field:
            if (index < fields.size() - 1)
            {
                Operator next = operators.get(index);
                if (next == null)
                {
                    // No operator, must be opening bracket/quote after
                    // In this case, we want to get rid of the brackets/quotes, and fold the contents into the slots adjacent to the brackets
                    return flattenCompound(index + 1, false);
                }
                else
                {
                    // Operator follows
                    String op = next.get();
                    // If it is still a valid operator without the first char, then just lop the first
                    // character off but leave as an operator: 
                    if (op.length() > 1 && Operator.isOperator(op.substring(1)))
                    {
                        next.set(op.substring(1));
                        return checkFieldChange(index, new CaretPos(index, new CaretPos(posInField, null)));
                    }
                    else
                    {
                        // Otherwise:
                        // - If it is new, retain the "ew" and put it into the text field
                        // - Any other operator, just delete the whole lot
                        String opRemaining = "";
                        if (op.equals("new "))
                        {
                            opRemaining = "ew"; // Remove 'n' because of delete, and space to avoid awkwardness
                        }
                        
                        operators.remove(index);
                        int newPos = f.getText().length();
                        f.setText(f.getText() + opRemaining + ((ExpressionSlotField)fields.get(index + 1)).getText());
                        fields.remove(index + 1);
                        
                        
                        return checkFieldChange(index, new CaretPos(index, new CaretPos(newPos, null)));
                    }
                }
            }
            else
            {
                if (parent != null)
                    return new CaretPos(-1, parent.flatten(true));
                else
                {
                    if (slot != null)
                    {
                        if (slot.deleteAtEnd())
                            return null;
                    }
                    return new CaretPos(index, new CaretPos(posInField, null));
                }
            }
        }
        else
        {
            // Not deleting at the end; no special complications
            String s = f.getText();
            f.setText(s.substring(0, posInField) + s.substring(posInField+1));
            return checkFieldChange(index, new CaretPos(index, new CaretPos(posInField, null)));
        }
    }
    
    public CaretPos getCurrentPos()
    {
        for (int i = 0; i < fields.size(); i++)
        {
            CaretPos pos = fields.get(i).getCurrentPos();
            if (pos != null)
                return new CaretPos(i, pos);
        }
        return null;
    }
    
    private int findField(ExpressionSlotComponent f)
    {
        for (int i = 0; i < fields.size(); i++)
        {
            if (fields.get(i) == f)
            {
                return i;
            }
            else if (fields.get(i) instanceof StringLiteralExpression)
            {
                if (f == ((StringLiteralExpression)fields.get(i)).getField())
                {
                    return i;
                }
            }
        }
        return -1;
    }
    
    /**
     * Handles inserting the given text at the given caret position in the given slot-field.
     */
    public void insert(ExpressionSlotField f, int posInField, String text)
    {
        insert_(f, posInField, text, true);
    }
    
    /**
     * Package-visible implementation of insert method, used directly by test classes.
     */
    CaretPos insert_(ExpressionSlotField f, int posInField, String text, boolean user)
    {
        if ( parent == null && !text.isEmpty() && text.length() > 0 && closingChars.contains(text.charAt(0)) ) {
            bindedSlot.requestFocus();
            return null;
        }
        int index = findField(f);
        
        if (index == -1)
        {
            return null; // Must be initialising; nothing to do here
        }        
        CaretPos pos = new CaretPos(index, new CaretPos(posInField, null));
        
        // Unless it's a opening bracket, any insertion must involve first deleting the selection:
        if (text.length() > 0 && text.charAt(0) != '(' && text.charAt(0) != '[' && text.charAt(0) != '\"')
        {
            CaretPos postDeletion = deleteSelection_(pos);
            if (postDeletion != null)
                pos = postDeletion;
        }
        
        for (int i = 0; i < text.length(); i++)
        {
            pos = insertChar(pos, text.charAt(i), user);
            
            // Blank selection after first insertion:
            // (We don't do it before first insertion, because you might have pressed '(' which should
            // take the selection and enclose it; the selection only becomes N/A after the first insertion):
            anchorPos = null;
            
            if (pos.index == Integer.MAX_VALUE)
            {
                if (parent == null)
                    throw new IllegalStateException();
                else
                {
                    parent.insertAfter(text.substring(i+1));
                    return null;
                }
            }
        }
        
        positionCaret(pos);
        if (slot != null)
        {
            slot.clearSelection(true);
        }
        
        return pos; // Returned for testing purposes
    }
    
    CaretPos insertAtPos(CaretPos p, String after)
    {
        ExpressionSlotComponent f = fields.get(p.index);
        if (f instanceof ExpressionSlotField) {
            return insert_((ExpressionSlotField)fields.get(p.index), p.subPos.index, after, false);
        }
        if (f instanceof StringLiteralExpression) {
            return insert_(((StringLiteralExpression)fields.get(p.index)).getField(), p.subPos.index, after, false);
        }
        return new CaretPos(p.index, ((BracketedExpression)fields.get(p.index)).testingContent().insertAtPos(p.subPos, after));
    }

    private CaretPos insertChar(CaretPos pos, char c, boolean user)
    {
        final ExpressionSlotComponent slot = fields.get(pos.index);
        // Bit hacky to use instanceof, but it's easier to have logic here than in subclasses:
        if (slot instanceof ExpressionSlotField)
        {
            final ExpressionSlotField f = (ExpressionSlotField)slot;
            final Operator prev = pos.index == 0 ? null : operators.get(pos.index - 1);
            final Operator next = pos.index >= operators.size() ? null : operators.get(pos.index);
            int posInField = pos.subPos.index;
            
            if (Character.isWhitespace(c) && !f.getText().substring(0, posInField).equals("new"))
            {
                // No whitespace allowed in slots (except string literals, handled later, and after "new")
                return pos;
            }
            if (posInField == 0 && prev != null &&
                    Operator.isOperator(prev.get() + c))
            {
                // We are at start of slot, with an operator before us,
                // and character can join with previous operator to form compound operator
                prev.set(prev.get() + c);
                return pos; // We effectively don't move
            }
            else if (posInField == f.getText().length() && next != null &&
                      Operator.isOperator("" + c + next.get()))
            {
                // We are at end of slot, with an operator after us,
                // and character can join with previous operator to form compound operator
                next.set("" + c + next.get());
                return pos; // We effectively don't move (or should we move beyond operator?)
            }
            else if (c == ',' && posInField == f.getText().length() && next != null &&
                    next.get().equals(",") && pos.index + 1 < fields.size() &&
                    fields.get(pos.index + 1).getCopyText(null, null).isEmpty())
            {
                // We are before a comma, we're typing a comma, and the field afterwards is empty.
                // In this case (which generally happens after code completion has created a skeleton),
                // we overtype the comma:
                return new CaretPos(pos.index + 1, new CaretPos(0, null));
            }
            else if (Operator.beginsOperator(c) && c != '.' && c != '+' && c != '-')
                // dot, plus and minus are inserted like normal chars, and checkFieldChange deals with them,
                // because they may be part of a floating point number
            {
                String before = f.getText().substring(0, posInField);
                String following = f.getText().substring(posInField);
                
                f.setText(before);
                operators.add(pos.index, new Operator("" + c, this));
                fields.add(pos.index + 1, makeNewField(following, false));
                return new CaretPos(pos.index + 1, new CaretPos(0, null));
            }
            else if (anchorPos != null && (c == '(' || c == '[' || c == '{' || c == '\"'))
            {
                // If there is a selection, enclose that in the bracket/quote:                
                String content = anchorPos.before(pos) ? getCopyText(anchorPos, pos) : getCopyText(pos, anchorPos);
                pos = deleteSelection_(pos);
                pos = insertChar(pos, c, false);
                insertAtPos(pos, content);
                return new CaretPos(pos.index + 1, new CaretPos(0, null));
            }
            else if (c == '(' || c == '[' || c == '{')
            {
                // If we are at end of field, and bracket follows:
                if (posInField == f.getText().length() &&
                        pos.index + 1 < fields.size() && fields.get(pos.index + 1) instanceof BracketedExpression)
                {
                    BracketedExpression following = (BracketedExpression) fields.get(pos.index + 1);
                    if (following.getOpening() == c)
                    {
                        // Just overtype the bracket, and move inside:
                        return new CaretPos(pos.index + 1, new CaretPos(0, new CaretPos(0, null)));
                    }
                }
                
                String following = f.getText().substring(posInField);
                f.setText(f.getText().substring(0, posInField));
                operators.add(pos.index, null);
                
                fields.add(pos.index + 1, new BracketedExpression(editor, this, this.slot, c, ""));
                if (pos.index + 1 >= operators.size() || operators.get(pos.index + 1) != null
                        || !(fields.get(pos.index + 2) instanceof ExpressionSlotField))
                {
                    // Used to be operator directly after this field (or we are at end), must add another field to pad
                    // RHS (not allowed to have operator after compound with no field inbetween)
                    operators.add(pos.index + 1, null);
                    fields.add(pos.index + 2, makeNewField(following, false));
                }
                else
                {
                    ExpressionSlotField follow = (ExpressionSlotField)fields.get(pos.index+2);
                    follow.setText(following + follow.getText());
                }
                return new CaretPos(pos.index + 1, new CaretPos(0, new CaretPos(0, null)));
            }
            else if (c == ')' || c == ']' || c == '}')
            {
                if (closingChars.contains(c) && posInField == f.getText().length())
                {
                    // Go to outer level
                    return new CaretPos(Integer.MAX_VALUE, null);
                }
                else
                {
                    // Ignore it
                    return pos;
                }
            }
            else if (c == '\"')
            {
                String following = f.getText().substring(posInField);
                f.setText(f.getText().substring(0, posInField));
                operators.add(pos.index, null);
                fields.add(pos.index + 1, new StringLiteralExpression(makeNewField("", true), this));
                if (pos.index + 1 >= operators.size() || operators.get(pos.index + 1) != null)
                {
                    // Used to be operator directly after this field (or we are at end), must add another field to pad
                    // RHS (not allowed to have operator after compound with no field inbetween)
                    operators.add(pos.index + 1, null);
                    fields.add(pos.index + 2, makeNewField(following, false));
                }
                else
                {
                    ExpressionSlotField follow = (ExpressionSlotField)fields.get(pos.index+2);
                    follow.setText(following + follow.getText());
                }
                return new CaretPos(pos.index + 1, new CaretPos(0, new CaretPos(0, null)));
            }
            else
            {
                if (f.getText().substring(0, posInField).equals("new")
                        && Character.isWhitespace(c))
                {
                    String following = f.getText().substring(posInField);
                
                    f.setText("");
                    operators.add(pos.index, new Operator("new ", this));
                    fields.add(pos.index + 1, makeNewField(following, false));
                    return new CaretPos(pos.index + 1, new CaretPos(0, null));
                }
                else
                {
                    // Just insert normally:
                    f.setText(f.getText().substring(0, posInField) + c + f.getText().substring(posInField));
                    
                    CaretPos overridePos = checkFieldChange(pos.index, new CaretPos(pos.index, new CaretPos(posInField+1, null)), c == '.', user);
                    return overridePos;
                }
            }
        }
        else if (slot instanceof BracketedExpression)
        {
            // This can occur when pasting/loading content:
            CaretPos newSubPos = ((BracketedExpression)slot).getContent().insertChar(pos.subPos, c, false);
            if (newSubPos.index == Integer.MAX_VALUE)
            {
                // Must be field following:
                return new CaretPos(pos.index + 1, new CaretPos(0, null));
            }
            else
            {
                return new CaretPos(pos.index, newSubPos);
            }
        }
        else if (slot instanceof StringLiteralExpression)
        {
            final ExpressionSlotField f = ((StringLiteralExpression) slot).getField();
            final int posInField = pos.subPos.index;
            if (c == '\"' && getEscapeStatus(f.getText().substring(0, posInField)) == EscapeStatus.NORMAL)
            {
                // Closing quote, not escaped
                if (posInField == f.getText().length())
                {
                    // Go past closing quote:
                    return new CaretPos(pos.index + 1, new CaretPos(0, null));
                }
                else
                {
                    // Not at end of field; ignore it
                    return pos;                       
                }
            }
            
            // If it is not a quote, or it is after a backslash, insert as-is:
            f.setText(f.getText().substring(0, posInField) + c + f.getText().substring(posInField));
            return new CaretPos(pos.index, new CaretPos(posInField+1, null));
        }
        return null;
    }

    public List<? extends PossibleLink> findLinks(Optional<Character> surroundingBracket, Map<String, CodeElement> vars, Function<Integer, JavaFragment.PosInSourceDoc> posCalculator, int offset)
    {
        final List<PossibleLink> r = new ArrayList<>();

        // Consume next compound identifier:
        int cur = 0;
        int beginningSlot = cur;
        int endSlot = cur;
        int endLength = -1;
        String curOperand = "";
        while (cur < fields.size())
        {
            if (fields.get(cur) instanceof ExpressionSlotField)
            {
                final ExpressionSlotField expressionSlotField = (ExpressionSlotField) fields.get(cur);

                if (expressionSlotField.getText().equals("class") && curOperand.endsWith("."))
                {
                    // What went before is assumed to be a type:
                    r.add(new PossibleTypeLink(curOperand.substring(0, curOperand.length() - 1),
                            offset+caretPosToStringPos(new CaretPos(beginningSlot, new CaretPos(0, null)), false),
                            offset+caretPosToStringPos(new CaretPos(endSlot, new CaretPos(endLength, null)), false), getSlot()));
                }

                if (curOperand.equals(""))
                    beginningSlot = cur;
                curOperand += expressionSlotField.getText();
                endSlot = cur;
                endLength = expressionSlotField.getText().length();

                if (cur < operators.size() && operators.get(cur) == null)
                {
                    // Must be a bracket next.  If round, this will be a method call, but handled below.
                }
                else if (cur < operators.size() && operators.get(cur).get().equals("."))
                {
                    // Fine, carry on building up the identifier
                    curOperand += ".";
                }
                else
                {
                    // end of operand, File it away:

                    if (cur == operators.size() && beginningSlot == 0 && surroundingBracket.equals("("))
                    {
                        // Item took up whole length of bracket; may well be a type:
                        r.add(new PossibleTypeLink(curOperand,
                            offset+caretPosToStringPos(new CaretPos(beginningSlot, new CaretPos(0, null)), false),
                            offset+caretPosToStringPos(new CaretPos(endSlot, new CaretPos(endLength, null)), false), getSlot()));
                    }

                    if (cur == beginningSlot)
                    {
                        // Plain (no dots):
                        if (vars != null)
                        {
                            CodeElement el = vars.get(curOperand);
                            if (el != null)
                                r.add(new PossibleVarLink(curOperand, el,
                                    offset+caretPosToStringPos(new CaretPos(beginningSlot, new CaretPos(0, null)), false),
                                    offset+caretPosToStringPos(new CaretPos(endSlot, new CaretPos(endLength, null)), false), getSlot()));
                        }
                    }

                    curOperand = "";
                }
            }
            else if (fields.get(cur) instanceof BracketedExpression)
            {
                ExpressionSlotField prev = (ExpressionSlotField) fields.get(cur - 1);
                int innerOffset = 1 + offset + caretPosToStringPos(new CaretPos(cur - 1, new CaretPos(prev.getText().length(), null)), false);
                BracketedExpression be = (BracketedExpression)fields.get(cur);
                r.addAll(be.getContent().findLinks(Optional.of(be.getOpening()), vars, posCalculator, innerOffset));

                if (!curOperand.equals("") && be.getOpening() == '(')
                {
                    // curOperand is assumed to have been a method call:
                    final int endSlotFinal = endSlot;
                    r.add(new PossibleMethodUseLink(curOperand.substring(curOperand.indexOf(".") + 1), be.getContent().getSimpleParameters().size(), () -> posCalculator.apply(offset+caretPosToStringPos(new CaretPos(endSlotFinal, new CaretPos(0, null)), true)),
                        offset+caretPosToStringPos(new CaretPos(endSlot, new CaretPos(0, null)), false),
                        offset+caretPosToStringPos(new CaretPos(endSlot, new CaretPos(endLength, null)), false), getSlot()));
                }
            }

            cur += 1;
        }

        return r;
    }

    public void setEditable(boolean editable)
    {
        fields.forEach(c -> c.setEditable(editable));
    }

    public boolean isNumericLiteral()
    {
        return fields.stream().allMatch(ExpressionSlotComponent::isNumericLiteral);
    }

    /**
     * Gets the EscapeStatus for the given String prefix.  See above comment for EscapeStatus
     */
    private EscapeStatus getEscapeStatus(String text)
    {
        EscapeStatus status = EscapeStatus.NORMAL;
        for (char c : text.toCharArray())
        {
            if (status == EscapeStatus.NORMAL)
            {
                if (c == '\\')
                {
                    status = EscapeStatus.AFTER_BACKSLASH;
                }
                // Otherwise, status still normal
            }
            else if (status == EscapeStatus.AFTER_BACKSLASH)
            {
                status = EscapeStatus.NORMAL; // They may start a unicode or octal escape,
                // but we flag that being shortened as invalid; it won't stop closing quote
            }
        }
        return status;
    }

    private CaretPos checkFieldChange(int index, CaretPos pos)
    {
        return checkFieldChange(index, pos, false, false);
    }
    /**
     * Checks the field at the given index, and performs any rearrangements necessary that
     * relate to floating point literals.  This may involve turning a dot in a field into an
     * operator, or turning an operator back into field content (for dots, pluses and minuses).
     * 
     * Returns the new position (adjustment of the passed pos)
     * 
     */
    private CaretPos checkFieldChange(int index, CaretPos pos, boolean addedDot, boolean user)
    {
        if (fields.get(index) instanceof StringLiteralExpression)
            return pos; // No need to do this for string literals
        ExpressionSlotField f = (ExpressionSlotField)fields.get(index);
        String prevOp = (index > 0 && operators.get(index - 1) != null)
                ? operators.get(index - 1).get() : "";
        String nextOp = (index < operators.size() && operators.get(index) != null)
                ? operators.get(index).get() : "";
        ExpressionSlotField prevField = index > 0 && fields.get(index - 1) instanceof ExpressionSlotField
                ? (ExpressionSlotField)fields.get(index - 1) : null;
        boolean precedingBracket = index > 0 && operators.get(index - 1) == null;        
        boolean bracketBeforePrevField = index > 1 && prevField != null && operators.get(index - 2) == null;
        
        int dotIndex = -1;   
        while ((dotIndex = f.getText().indexOf('.', dotIndex + 1)) != -1)
        {
            String beforeDot = f.getText().substring(0, dotIndex);
            String afterDot = f.getText().substring(dotIndex + 1);
            // We need to check if the start of the field is all numbers
            // If it is, then this dot is part of a floating point literal,
            // and we should not split
            boolean isDoubleDot = afterDot.startsWith(".");
            if (!precedesDotInFloatingPointLiteral(beforeDot) || isDoubleDot /* two dots is operator */)
            {
                f.setText(beforeDot);
                 
                if (isDoubleDot)
                {
                    operators.add(index, new Operator("..", this));
                    fields.add(index + 1, makeNewField(afterDot.substring(1), false));
                }
                else
                {
                    boolean wasShowingSuggestions = slot != null && slot.isShowingSuggestions();
                    
                    operators.add(index, new Operator(".", this));
                    fields.add(index + 1, makeNewField(afterDot, false));
                    if (beforeDot.equals("") && afterDot.equals("") && addedDot
                            && index >= 2
                            && fields.get(index - 1) instanceof BracketedExpression
                            && fields.get(index - 2) instanceof ExpressionSlotField
                            && !((ExpressionSlotField)fields.get(index - 2)).getText().equals("")
                            && operators.get(index - 2) == null
                            && operators.get(index - 1) == null
                            && false) // TODO: see tasks.txt
                    {
                        operators.add(index+1, null);
                        fields.add(index+2, new BracketedExpression(editor, this, slot, '(', ""));
                        operators.add(index+2, null);
                        fields.add(index+3, makeNewField("", false));
                    }
                    
                    // If a dot is entered and code completion was showing, re-trigger it in the new field:
                    // We use runLater to make sure the state is all settled before showing the completion:
                    if (wasShowingSuggestions && user && addedDot)
                        Platform.runLater(() -> slot.showSuggestionDisplay((ExpressionSlotField)fields.get(index+1), 0, false));
                }
                
                if (pos.index > index) // already after split field:
                    pos =  new CaretPos(pos.index + (isDoubleDot ? 2 : 1), pos.subPos);
                else if (pos.index == index)
                {
                    if (pos.subPos.index <= beforeDot.length())
                    {
                        // in field but before dot, stay as-is
                    }
                    else
                        pos = new CaretPos(index + 1, new CaretPos(pos.subPos.index - (beforeDot.length() + (isDoubleDot ? 2 : 1)), null));
                        // in field, after dot
                }
                // else before the dot field, leave as-is
                
                pos = checkFieldChange(index, pos);
                return checkFieldChange(index + 1, pos);
            }
            // else if it is a literal and it's in the slot already, leave in slot.
            // But we still need to continue in case plus or minus was just added, or second dot...
        }
                
        if (precedesDotInFloatingPointLiteral(f.getText()) && nextOp.equals("."))
        {
            // Need to merge dot back in:
            int prevLen = f.getText().length();
            f.setText(f.getText() + nextOp + ((ExpressionSlotField)fields.get(index + 1)).getText());
            operators.remove(index);
            fields.remove(index+1);
            
            if (pos.index > index + 1)
            {
                pos = new CaretPos(pos.index - 1, pos.subPos);
            }
            else if (pos.index == index + 1)
            {
                pos = new CaretPos(index, new CaretPos(pos.subPos.index + prevLen + 1, null));
            }
            // If it was in pos.index or an earlier slot, it can stay untouched.
            
            nextOp = (index < operators.size() && operators.get(index) != null)
                    ? operators.get(index).get() : "";
        }
        
        Function<Integer, Integer> findPlusMinus = prev -> {
            int plusIndex = f.getText().indexOf('+', prev + 1);
            int minusIndex = f.getText().indexOf('-', prev + 1);
            if (plusIndex == -1)
                return minusIndex;
            else if (minusIndex == -1)
                return plusIndex;
            else
                return Math.min(plusIndex, minusIndex);
        };
        int plusMinusIndex = -1;
        while ((plusMinusIndex = findPlusMinus.apply(plusMinusIndex)) != -1)
        {
            String before = f.getText().substring(0, plusMinusIndex);
            String after = f.getText().substring(plusMinusIndex + 1);
            // There are two points at which it is valid to have a plus/minus in a numeric literal:
            //  - At the very beginning of the literal (and assuming there is no operand before the plus/minus, i.e. it is unary)
            //  - After digits and an 'e' or hex digits and a 'p'.
            
            boolean atBeginningAndUnary = before.equals("") && !precedingBracket && (!prevOp.equals("") || prevField == null || prevField.getText().equals("")) && succeedsOpeningPlusMinusInFloatingPointLiteral(after);
            boolean midwayAfterEorP = precedesPlusMinusInFloatingPointLiteral(before); 
            
            // If it satisfies neither of these, split out into its own field:
            if (!atBeginningAndUnary && !midwayAfterEorP)
            {
                operators.add(index, new Operator(f.getText().substring(plusMinusIndex, plusMinusIndex+1), this));
                f.setText(before);
                fields.add(index + 1, makeNewField(after, false));
                if (pos.index > index) // already after split field:
                    return new CaretPos(pos.index + 1, pos.subPos);
                else if (pos.index == index)
                {
                    if (pos.subPos.index <= before.length())
                        return pos; // in field but before +/-
                    else
                        return new CaretPos(index + 1, new CaretPos(pos.subPos.index - (before.length() + 1), null));
                        // in field, after +/-
                }
                else // before the +/- field:
                    return pos;
            }
        }
        
        if (precedesPlusMinusInFloatingPointLiteral(f.getText()) && (nextOp.equals("+") || nextOp.equals("-")))
        {
            // Need to merge dot back in:
            int prevLen = f.getText().length();
            f.setText(f.getText() + nextOp + ((ExpressionSlotField)fields.get(index + 1)).getText());
            operators.remove(index);
            fields.remove(index+1);
            
            if (pos.index > index + 1)
            {
                pos = new CaretPos(pos.index - 1, pos.subPos);
            }
            else if (pos.index == index + 1)
            {
                pos = new CaretPos(index, new CaretPos(pos.subPos.index + prevLen + 1, null));
            }
            // If it was in pos.index or an earlier slot, it can stay untouched.
        }
        
        if ((prevOp.equals("+") || prevOp.equals("-")) && succeedsOpeningPlusMinusInFloatingPointLiteral(f.getText())
                && prevField != null && prevField.getText().equals("") && !bracketBeforePrevField)
        {
            // Merge operator back in:
            operators.remove(index - 1);
            fields.remove(index - 1);
            f.setText(prevOp + f.getText());
            pos = new CaretPos(pos.index - 1, new CaretPos(pos.subPos.index + 1, null));
        }
        
        return pos;
        
    }

    private boolean precedesPlusMinusInFloatingPointLiteral(String before) {
        return before.matches("\\A\\s*0x" + HEX_DIGITS_REGEX + "(\\.(" + HEX_DIGITS_REGEX +")?)?[pP]\\z") || 
                before.matches("\\A\\s*[+-]?" + DIGITS_REGEX + "(\\.(" + DIGITS_REGEX + ")?)?[eE]\\z");
    }
    
    private boolean succeedsOpeningPlusMinusInFloatingPointLiteral(String after)
    {
        return after.matches("\\A\\d.*");
    }

    public void focusAtStart()
    {
        getFirstField().focusAtStart();
    }
    
    public void focusAtEnd()
    {
        getLastField().focusAtEnd();
    }
    
    public CaretPos getStartPos()
    {
        return new CaretPos(0, getFirstField().getStartPos());
    }
    
    public CaretPos getEndPos()
    {
        return new CaretPos(fields.size() - 1, getLastField().getEndPos());
    }
    
    public void end()
    {
        getLastField().focusAtEnd();
    }
    
    // Delegate targets from Slot, via ExpressionSlot:
    
    public ObservableList<Node> getComponents()
    {
        return components;
    }
    
    public boolean isEmpty()
    {
        return fields.size() == 1 && getFirstField().isEmpty();
    }
    
    public void requestFocus()
    {
        getFirstField().requestFocus();
    }
    
    // package-visible
    void setPromptText(BiConsumer<List<ExpressionSlotComponent>, List<Operator>> setPrompts)
    {
        // Run later so that we only see complete updates:
        ListChangeListener<Object> listener = a -> Platform.runLater(() -> setPrompts.accept(fields, operators));
        fields.addListener(listener);
        operators.addListener(listener);
        //Trigger now, too:
        setPrompts.accept(fields, operators);
    }

    // Package-visible
    Region getNodeForPos(CaretPos pos)
    {
        ExpressionSlotComponent f = fields.get(pos.index);
        return f.getNodeForPos(pos.subPos);
    }

    /**
     * Creates mappings between a caret position and string position.  See CaretPosMap for more info.
     * 
     * If javaString is true, maps to generated Java code string.  If false,
     * maps to copy-string/frame source.  Difference is that if you want to write
     * "6 - -5", then in copy-string this must be "6--5" (no spaces supported) and in
     * Java code it must be "6- -5"; to prevent becoming joined -- operator.  I.e. in Java
     * code, a space is generated for each empty slot.
     */
    //package-visible
    List<CaretPosMap> mapCaretPosStringPos(IntCounter cur, boolean javaString)
    {
        List<CaretPosMap> r = new ArrayList<>();
        
        BiConsumer<Integer, Integer> addForRange = (startIncl, endExcl) -> {
            for (int i = startIncl; i < endExcl; i++)
            {
                for (CaretPosMap cpm : fields.get(i).mapCaretPosStringPos(cur, javaString))
                {
                    r.add(cpm.wrap(i));
                }
                if (i < operators.size() && i < endExcl - 1)
                {
                    Operator op = operators.get(i);
                    if (op != null)
                    {
                        cur.counter += javaString ? op.getJavaCode().length() : op.get().length();
                    }
                }
            }
        };
        
        if (!javaString)
        {
            addForRange.accept(0, fields.size());
        }
        else
        {
            
            int closing = 0;
            int last = 0;
            for (int i = 0; i < fields.size(); i++)
            {
                if (i < operators.size() && operators.get(i) != null)
                {
                    String op = operators.get(i).get();
                    int commaLength = ", ".length();
                    // See getJavaCode() for logic
                    if (op.equals(".."))
                    {
                        cur.counter += (lang.stride.Utility.class.getName() + ".makeRange(").length();
                        addForRange.accept(last, i + 1);
                        cur.counter += commaLength;
                        last = i + 1;
                        closing += 1;
                    }
                    else if (op.equals(","))
                    {
                        addForRange.accept(last, i+1);
                        cur.counter += closing + commaLength;
                        closing = 0;
                        last = i + 1;
                    }
                }
            }
            addForRange.accept(last, fields.size());
            cur.counter += closing;
        }
        return r;
    }

    /**
     * Maps a given string position into a CaretPos.
     * Only really applicable when called on the top-level InfixExpression.
     * Should probably be moved to ExpressionSlot.
     * 
     * If javaString is true, maps to generated Java code string.  If false,
     * maps to copy-string/frame source.  Difference is that if you want to write
     * "6 - -5", then in copy-string this must be "6--5" (no spaces supported) and in
     * Java code it must be "6- -5"; to prevent becoming joined -- operator.  I.e. in Java
     * code, a space is generated for each empty slot.
     */
    //package-visible
    CaretPos stringPosToCaretPos(int pos, boolean javaString)
    {
        List<CaretPosMap> mapping = mapCaretPosStringPos(new IntCounter(), javaString);
        for (CaretPosMap cpm : mapping) {
            if (pos <= cpm.endIndex) {
                if (pos >= cpm.startIndex || javaString /*For javaString, we want to find nearest pos*/)
                    return cpm.posOuter.append(new CaretPos(Math.max(0, pos - cpm.startIndex), null));
                else
                    return null;
            }
        }
        Debug.message("Could not find position for: " + pos);
        return null;
    }
    
    /**
     * Maps a given CaretPos into a position in the full string for the expression.
     * Only really applicable when called on the top-level InfixExpression.
     * Should probably be moved to ExpressionSlot.
     * 
     * If javaString is true, maps to generated Java code string.  If false,
     * maps to copy-string/frame source.  Difference is that if you want to write
     * "6 - -5", then in copy-string this must be "6--5" (no spaces supported) and in
     * Java code it must be "6- -5"; to prevent becoming joined -- operator.  I.e. in Java
     * code, a space is generated for each empty slot.
     */
    //package-visible
    int caretPosToStringPos(CaretPos pos, boolean javaString)
    {
        List<CaretPosMap> mapping = mapCaretPosStringPos(new IntCounter(), javaString);
        for (CaretPosMap cpm : mapping) {
            Optional<Integer> i = pos.getFollowing(cpm.posOuter); 
            if (i.isPresent()) {
                return i.get() + cpm.startIndex; 
            }
        }
        throw new IllegalStateException();
    }
    
    //package-visible
    double getBaseline()
    {
        // 3 is a hack/guess at the baseline
        TextField field = (TextField)components.get(0);
        double height = field.getHeight() - 3 - field.getPadding().getBottom();
        if (field.getBorder() != null && field.getBorder().getInsets() != null)
            height -= field.getBorder().getInsets().getBottom();
        return height;
    }
    
    public void blank()
    {
        operators.clear();
        fields.clear();
        fields.add(makeNewField("", false));
        anchorPos = null;
    }
    
    public boolean isFocused()
    {
        return fields.stream().anyMatch(ExpressionSlotComponent::isFocused);
    }
    
    public boolean isCollapsible(ExpressionSlotField f)
    {
        // A field is collapsible if:
        // - It occurs to the left-hand side of an operator that is, or can be, unary
        // - It occurs between a bracket (null operator) and an operator, or
        //   a bracket and the end, or two brackets (could be cast on lhs)
        //   - This includes single slots, but there is exception for only slot at top-level, unless we are parameters to constructor
        
        int index = findField(f);
        if (index == -1) {
            // This can occur, e.g. while field is being removed and has lost focus:
            return false;
        }
        
        boolean opBefore = index == 0 || operators.get(index - 1) != null;
        boolean opAfter = index == operators.size() || operators.get(index) != null;
        
        //boolean unaryBefore = index != 0 && canBeUnary(operators.get(index - 1));
        boolean unaryAfter = index < operators.size() && Operator.canBeUnary(Utility.orNull(operators.get(index), Operator::get));
        
        if (fields.size() == 1 && parent == null)
        {
            if (slot != null && slot.isConstructorParams())
                return true; // Can collapse in this case
            else
                return false; // Only field at top level, not constructor params
        }
        else if (fields.size() == 1 && parent != null)
            return true; // Only field in brackets
        else if (opBefore && opAfter)
            return unaryAfter;
        else
            return true;

    }

    public void insertNext(BracketedExpression bracketedExpression, String text)
    {
        int index = fields.indexOf(bracketedExpression);
        insert((ExpressionSlotField) fields.get(index + 1), 0, text);
    }

    public void replaceContent(CaretPos start, CaretPos end, String insertion)
    {
        anchorPos = end;
        deleteSelection_(start);
        insertAtPos(start, insertion);
    }

    // For testing purposes, package visible
    String testingGetState(CaretPos caret)
    {
        caret = Utility.orNull(caret, CaretPos::normalise);
        StringBuilder r = new StringBuilder();
        for (int i = 0; i < fields.size(); i++) {
            r.append(fields.get(i).testingGetState((caret != null && i == caret.index) ? caret.subPos : null));
            if (i < operators.size()) {
                if (operators.get(i) == null) {
                    r.append("_");
                }
                else {
                    r.append(operators.get(i).get());
                }
            }
        }
        return r.toString();
    }

    // If rememberPos is \0, give position after inserting whole string
    // If rememberPos is any other char, remember pos at first occurrence of that char in the string
    // e.g. "a+$b", '$' will give the caret position just before the b. The instance of rememberPos
    // will be ignored (it will not be inserted).
    CaretPos testingInsert(String text, char rememberPos)
    {
        int index = text.indexOf(rememberPos);
        if (rememberPos != '\0' && index != -1) {
            String before = text.substring(0, index);
            String after = text.substring(index + 1);
            CaretPos p = insert_((ExpressionSlotField)fields.get(0), 0, before, false);
            testingInsert(p, after);
            return p;
        }
        
        return insert_((ExpressionSlotField) fields.get(0), 0, text, false);
    }
    
    CaretPos testingInsert(CaretPos p, String after)
    {
        return insertAtPos(p, after);
    }

    CaretPos testingBackspace(CaretPos p)
    {
        ExpressionSlotComponent f = fields.get(p.index);
        if (f instanceof ExpressionSlotField)
            return deletePrevious_((ExpressionSlotField)fields.get(p.index), p.subPos.index, p.subPos.index == 0);
        else if (f instanceof StringLiteralExpression)
            return deletePrevious_(((StringLiteralExpression)fields.get(p.index)).getField(), p.subPos.index, p.subPos.index == 0);
        else
            return new CaretPos(p.index, ((BracketedExpression)fields.get(p.index)).testingContent().testingBackspace(p.subPos));
    }
    
    CaretPos testingDelete(CaretPos p)
    {
        ExpressionSlotComponent s = fields.get(p.index);
        ExpressionSlotField f;
        if (s instanceof ExpressionSlotField)
            f = (ExpressionSlotField)fields.get(p.index);
        else if (s instanceof StringLiteralExpression)
            f = ((StringLiteralExpression)fields.get(p.index)).getField();
        else
            return new CaretPos(p.index, ((BracketedExpression)fields.get(p.index)).testingContent().testingDelete(p.subPos));
        
        return deleteNext_(f, p.subPos.index, p.subPos.index == f.getText().length());
    }
    
    CaretPos testingDeleteSelection(CaretPos start, CaretPos end)
    {
        anchorPos = start;
        return deleteSelection_(end);
    }

    CaretPos testingInsertWithSelection(CaretPos start, CaretPos end, char c)
    {
        anchorPos = start;
        return insertAtPos(end, "" + c);
    }
    
    public void insertSuggestion(CaretPos p, String name, List<String> params)
    {
        ExpressionSlotComponent f = fields.get(p.index);
        if (f instanceof ExpressionSlotField)
        {
            // Blank the field (easier than working out prefixes etc)
            ((ExpressionSlotField) f).setText("");
            // And insert the new name.  It may have a dot in it, so caret may move to another field:
            p = insert_((ExpressionSlotField)f, 0, name, false);
            if (params != null)
            {
                StringBuilder commas = new StringBuilder();
                for (int i = 0; i < params.size() - 1; i++)
                    commas.append(',');
                
                if (p.index + 1 < fields.size() && fields.get(p.index + 1) instanceof BracketedExpression)
                {
                    BracketedExpression b = ((BracketedExpression)fields.get(p.index + 1));
                    if (b.getContent().isEmpty())
                    {
                        b.getContent().insertAtPos(new CaretPos(0, new CaretPos(0, null)), commas.toString());
                    }
                    // else if there are brackets with content, leave them alone.
                    
                    // We use a runLater as we need to request focus after the suggestion window has been hidden:
                    Platform.runLater(() -> 
                        b.focusAtStart()
                    );
                }
                else
                {
                    insertAtPos(new CaretPos(p.index, new CaretPos(p.subPos.index, null)), "(" + commas.toString() + ")");
                    // If no parameters, focus after the brackets.  Otherwise, focus first parameter
                    ExpressionSlotComponent focusField = fields.get(params.isEmpty() ? p.index + 2 : p.index + 1);
                    // We use a runLater as we need to request focus after the suggestion window has been hidden:
                    Platform.runLater(() ->
                        focusField.focusAtStart()
                    );
                }
            }
        }
        else
        {
            f.insertSuggestion(p.subPos, name, params);
        }
    }

    public void withTooltipFor(ExpressionSlotField expressionSlotField, FXConsumer<String> handler)
    {
        int slotIndex = fields.indexOf(expressionSlotField);

        // It can also have a tooltip if it is a method name, which means
        // it has a bracketedexpression immediately afterwards.
        if (!expressionSlotField.getText().equals("") &&
                slotIndex + 1 < fields.size() && fields.get(slotIndex + 1) instanceof BracketedExpression)
        {
            CaretPos relPos = new CaretPos(slotIndex, new CaretPos(0, null));
            slot.withMethodHint(absolutePos(relPos), fields.get(slotIndex).getCopyText(null, null),
                methodHints -> {
                    if (methodHints.size() == 1)
                    {
                        handler.accept(methodHints.get(0));
                    } else
                    {
                        // More than one set of hints (method is overloaded), play simple and give no tip:
                        handler.accept("");
                    }
                });
        }
        // It can have a tooltip if is a parameter, which means it is
        // inside a bracketedexpression, with a non-blank before it
        else if (parent != null || slot.isConstructorParams())
        {
            int paramIndex = 0;
            int totalParams = 1;

            for (int i = 0; i < operators.size(); i++)
            {
                if (operators.get(i) != null && operators.get(i).getCopyText().equals(","))
                {
                    totalParams += 1;
                    if (i < slotIndex)
                        paramIndex += 1;
                }
            }
            if (parent != null)
                parent.withTooltipAtPos(paramIndex, handler);
            else
            {
                final int finalParamIndex = paramIndex;
                // We are the top-level and we are constuctor params
                slot.withParamHintsForConstructor(totalParams, conHints -> {
                    // Only show if there's one overload of that arity:
                    if (conHints.size() == 1 && finalParamIndex < conHints.get(0).size())
                    {
                        handler.accept(conHints.get(0).get(finalParamIndex));
                    }
                });
            }

        }
        else
        {
            handler.accept("");
        }
    }

    public void withTooltipForParam(BracketedExpression bracketedExpression, int paramPos, FXConsumer<String> handler)
    {
        int expIndex = fields.indexOf(bracketedExpression);
        if (fields.get(expIndex - 1).getCopyText(null, null).equals(""))
        {
            // No text before bracket; not parameter so no valid tooltip:
            handler.accept("");
        }
        else
        {
            CaretPos relPos = new CaretPos(expIndex - 1, new CaretPos(0, null));
            slot.withParamHintsForPos(absolutePos(relPos), fields.get(expIndex - 1).getCopyText(null, null),
                    paramHints -> {
                if (paramHints.size() == 1)
                {
                    if (paramPos < paramHints.get(0).size())
                    {
                        handler.accept(paramHints.get(0).get(paramPos));
                        return;
                    }
                }
                // More than one set of hints (method is overloaded), play simple and give no tip:
                handler.accept("");
            });
        }
    }

    private CaretPos absolutePos(CaretPos p)
    {
        if (parent == null)
            return p;
        else
            return parent.absolutePos(p);
    }

    public CaretPos absolutePos(BracketedExpression bracketedExpression, CaretPos p)
    {
        return absolutePos(new CaretPos(fields.indexOf(bracketedExpression), p));
    }

    //package-visible
    InteractionManager getEditor()
    {
        return editor;
    }

    //package-visible
    void updatePromptsInMethodCalls(ExpressionSlotField from)
    {
        int index = findField(from);
        if (index < 0)
        {
            Debug.printCallStack("Asking to update prompts for non-existing field");
            return;
        }
        // We look for method calls, which means we need to look for brackets preceded by non-empty fields:
        // We look at the given field onwards, because e.g. getWorl().addObject() should update
        // the addObject call if "getWorl" gets editing to "getWorld".
        // However, we only check at the current level; a method can't affect the prompts
        // for further calls inside its parameters.
        // TODO: updating later calls doesn't seem to work right, for some reason?
        for (int i = index; i < fields.size(); i++)
        {
            if (i < fields.size() - 1 && 
                    fields.get(i) instanceof ExpressionSlotField &&
                    !fields.get(i).isFieldAndEmpty() &&
                    fields.get(i + 1) instanceof BracketedExpression)
            {
                // Text, non-empty, followed by brackets.  Must be a method call:
                BracketedExpression bracketedParams = (BracketedExpression) fields.get(i + 1);
                CaretPos absPos = absolutePos(new CaretPos(i, new CaretPos(0, null)));
                bracketedParams.getContent().treatAsParams_updatePrompts(fields.get(i).getCopyText(null, null), absPos);
            }
        }
        
    }

    // package-visible
    // Called to notify us that we are a set of parameters, and we should update our prompts for those parameters
    void treatAsParams_updatePrompts(String methodName, CaretPos absPosOfMethodName)
    {
        List<ExpressionSlotField> params = getSimpleParameters();
        
        if (params.stream().allMatch(p -> p == null))
            return; // Nothing needs prompts

        if (slot == null) // Can happen during testing
            return;

        slot.withParamNamesForPos(absPosOfMethodName, methodName,
            poss -> setPromptsFromParamNames(poss));
    }

    // package-visible
    // Called to notify us that we are a set of parameters, and we should update our prompts for those parameters
    void treatAsConstructorParams_updatePrompts()
    {
        List<ExpressionSlotField> params = getSimpleParameters();

        if (params.stream().allMatch(p -> p == null))
            return; // Nothing needs prompts
        
        slot.withParamNamesForConstructor(
            poss -> setPromptsFromParamNames(poss));
    }

    /**
     * A callback called when we have fetched information on parameter names, and want
     * to use it to update the prompts for method parameters.
     *
     * @param possibilities  This is the list of possible parameters.  If there is a single
     *                       method of that name, possibilities will be a singleton list, with
     *                       the content being parameter names, e.g.
     *                       Arrays.asList(Arrays.asList("x", "y")) for setLocation(int x, int y)
     *                       If possibilities is empty, there are no methods found.
     *                       If possibilities is not size 1, there are multiple overloads for that name.
     */
    private void setPromptsFromParamNames(List<List<String>> possibilities)
    {
        List<ExpressionSlotField> curParams = getSimpleParameters();
        int curArity = curParams.size();
        // Arity is fixed if any params are non-empty (i.e. null or !isEmpty())
        boolean arityFlexible = curParams.stream().allMatch(f -> f != null && f.isEmpty());
        
        List<List<String>> matchedPoss = possibilities.stream()
            .filter(ps -> arityFlexible || ps.size() == curArity)
            .sorted(Comparator.comparing(List::size)) // Put shortest ones first
            .collect(Collectors.toList());
        
        if (matchedPoss.size() != 1)
        {
            // No possibilities, remove all commas if empty:
            if (arityFlexible && !isEmpty())
            {
                blank();
            }
            curParams.stream().filter(f -> f != null).forEach(f -> f.setPromptText(""));
        }
        else
        {
            // Exactly one option; give prompts:
            List<String> match = matchedPoss.get(0);
            
            if (arityFlexible && match.size() != curArity)
            {
                // No fixed arity; we know field must be near-blank, so just
                // replace it with the right number of commas (may be zero):
                blank();
                for (int i = 0; i < match.size() - 1; i++)
                {
                    // We add at end to avoid the overtyping logic:
                    insertChar(getEndPos(), ',', false);
                }
                curParams = getSimpleParameters();
            }
            
            for (int i = 0; i < match.size(); i++)
            {
                String prompt = match.get(i);
                if (prompt == null || Parser.isDummyName(prompt))
                    prompt = "";
                // Due to the delay in calculating prompts, we may be trying to set a parameter
                // at an outdated index, so protect against that:
                if (i < curParams.size() && curParams.get(i) != null)
                    curParams.get(i).setPromptText(prompt);
            }
        }
    }

    // List is as long as there are parameters, but returns null if the parameter is non-simple,
    // i.e. does not consist of a single field
    public List<ExpressionSlotField> getSimpleParameters()
    {
        List<ExpressionSlotField> r = new ArrayList<>();
        int lastComma = -1; // Just before first parameter, in effect
                
        for (int i = 0; i < fields.size(); i++)
        {
            ExpressionSlotField f = fields.get(i) instanceof ExpressionSlotField ? (ExpressionSlotField)fields.get(i) : null;
            // Look at operator afterwards:
            if (i == fields.size() - 1 || (operators.get(i) != null && operators.get(i).getCopyText().equals(",")))
            {
                if (lastComma == i - 1 && f != null)
                {
                    r.add(f);
                }
                else
                {
                    r.add(null);
                }
                lastComma = i;
            }
        }
        
        return r;
    }

    @Override
    public void clicked()
    {
        slot.hideSuggestionDisplay();
    }

    @Override
    public void caretMoved()
    {
        if (slot != null) // Can be null during testing
        {
            slot.caretMoved();
        }
    }

    @Override
    public void escape()
    {
        slot.escape();        
    }

    public void bindClosingChar(EditableSlot anotherSlot, char closingChar)
    {
        this.closingChars.add(closingChar);
        bindedSlot = anotherSlot;
    }

    public Stream<InfixExpression> getAllExpressions()
    {
        return Stream.concat(Stream.of(this), fields.stream().flatMap(ExpressionSlotComponent::getAllExpressions));
    }

    public StringExpression textProperty()
    {
        return textProperty;
    }
    
    public TextOverlayPosition calculateOverlayEnd()
    {
        return getLastField().calculateOverlayEnd();
    }

    /**
     * Makes no change directly.  Always returns non-null, but list may be empty
     */
    //package-visible
    List<ExpressionSlot.PlainVarReference> findPlainVarUse(String name)
    {
        List<ExpressionSlot.PlainVarReference> refs = new ArrayList<>();
        // We need to find any ExpressionSlotField which contains oldName without being
        // preceded by a dot or succeeded by open bracket, and replace it with newName
        for (int i = 0; i < fields.size(); i++)
        {
            if (fields.get(i) instanceof ExpressionSlotField)
            {
                ExpressionSlotField f = (ExpressionSlotField)fields.get(i);
                if (f.getText().equals(name)
                        && (i == 0 || operators.get(i - 1) == null || !operators.get(i - 1).get().equals("."))
                        && (i == fields.size() - 1 || !(fields.get(i) instanceof BracketedExpression) || ((BracketedExpression)fields.get(i)).getOpening() != '('))
                {
                    // It doesn't have dot before it, and doesn't have a bracket afterwards -- it's a reference:
                    refs.add(new ExpressionSlot.PlainVarReference(f::setText, f.getNodeForPos(null)));
                }
            }
            else if (fields.get(i) instanceof BracketedExpression)
            {
                refs.addAll(((BracketedExpression)fields.get(i)).getContent().findPlainVarUse(name));
            }
        }
        return refs;
    }
    
    /**
     * Returns true iff the infix expression is of the form {1,2,3},
     * or that in some number of round brackets. 
     */
    public boolean isCurlyLiteral()
    {
        // We must have a single bracketed expression, which is curly
        if (fields.size() != 3)
            return false;
        if (operators.get(0) != null || operators.get(1) != null)
            return false;
        if (!fields.get(0).isFieldAndEmpty() || !fields.get(2).isFieldAndEmpty())
            return false;
        if (! (fields.get(1) instanceof BracketedExpression))
            return false;
        BracketedExpression e = (BracketedExpression) fields.get(1);
        if (e.getOpening() == '{')
            return true;
        // We don't allow brackets around it, but this code would allow that:
        //else if (e.getOpening() == '(')
            //return e.getContent().isCurlyLiteral();
        else
            return false;
        
    }

    // package-visible
    void showHighlightedBrackets(BracketedExpression wrapper, CaretPos pos)
    {
        // Are we at the begining of the whole expression; if so, highlight wrapper:
        if (wrapper != null && pos != null && pos.index == 0 && fields.get(0).getStartPos().equals(pos.subPos))
        {
            wrapper.highlightBrackets(true);
        }
        // Same logic for being at the end of the whole expression:
        // Note: both could be true if one empty field inside brackets
        else if (wrapper != null && pos != null && pos.index == fields.size() - 1 && fields.get(fields.size() - 1).getEndPos().equals(pos.subPos))
        {
            wrapper.highlightBrackets(true);
        }
        
        for (int i = 0; i < fields.size(); i++)
        {
            ExpressionSlotComponent f = fields.get(i);
            if (f instanceof BracketedExpression)
            {
                boolean cursorBefore =
                        i > 0 &&
                        pos != null && pos.index == i - 1 &&
                        fields.get(i - 1) instanceof ExpressionSlotField &&
                        fields.get(i - 1).getEndPos().equals(pos.subPos);
                boolean cursorAfter =
                        i < fields.size() - 1 &&
                        pos != null && pos.index == i + 1 &&
                        fields.get(i + 1) instanceof ExpressionSlotField &&
                        fields.get(i + 1).getStartPos().equals(pos.subPos);
                BracketedExpression e = (BracketedExpression)f;
                // Highlight if the cursor is before or after the bracketed expression:
                e.highlightBrackets(cursorBefore || cursorAfter);
                // Descend into the expression:
                e.getContent().showHighlightedBrackets(e, pos != null && pos.index == i ? pos.subPos : null);
            }
        }
        
    }
    
    // forLoopVarName is missing if we are not top-level in a for loop,
    // but present if we are.  If present, check for constant loop bounds, and display classic for loop instead.
    public void setView(View oldView, View newView, SharedTransition animate, Optional<String> forLoopVarName)
    {
        fields.forEach(f -> f.setView(oldView, newView, animate));
        operators.forEach(o -> { if (o != null) o.setView(newView, animate); });
        
        if (newView == View.NORMAL)
        {
            previewingJavaRange.set(false);
        }
        else
        {
            switch (checkRangeExpression())
            {
                case RANGE_CONSTANT:
                    // If we are top-level in a for loop, transform to classic for loop:
                    if (forLoopVarName.isPresent())
                    {
                        Operator rangeOp = operators.stream().filter(op -> op != null && op.get().equals("..")).findFirst().get();
                        previewingJavaRange.set(true);
                        startRangeLabel.setText("");
                        endRangeLabel.setText("; " + forLoopVarName.get() + "++");
                        rangeOp.setJavaPreviewRangeOverride("; " + forLoopVarName.get() + " <=");
                        break;
                    }
                    //Otherwise fall-through to case for general ranges:
                case RANGE_NON_CONSTANT:
                    previewingJavaRange.set(true);
                    startRangeLabel.setText(lang.stride.Utility.class.getName() + "(");
                    endRangeLabel.setText(")");
                    break;
                default:
                    previewingJavaRange.set(false);
                    break;
            }
        }
    }

    // package-visible:
    RangeType checkRangeExpression()
    {
        // If there are any commas, we just abandon ship:
        if (operators.stream().anyMatch(op -> op != null && op.get().equals(",")))
            return RangeType.NOT_RANGE;


        // It must have .. to be a range, so let's see if there is one:
        Optional<Operator> rangeOp = operators.stream().filter(op -> op != null && op.get().equals("..")).findFirst();

        if (!rangeOp.isPresent())
            return RangeType.NOT_RANGE;

        // Now we need to decide if it's a constant.  The simple rule we use is: if all the expressions throughout are numerical integer literals
        // then it's constant, otherwise it's not.

        if (fields.stream().allMatch(ExpressionSlotComponent::isNumericLiteral))
            return RangeType.RANGE_CONSTANT;
        else
            return RangeType.RANGE_NON_CONSTANT;
    }

    //package-visible
    void paste()
    {
        ExpressionSlotField focused = fields.stream().filter(f -> f instanceof ExpressionSlotField && f.isFocused()).map(f -> (ExpressionSlotField)f).findFirst().orElse(null);
        if (focused != null)
            focused.paste();
    }

    //package-visible
    ExpressionSlot<?> getSlot()
    {
        return slot;
    }

    public boolean suggestingFor(ExpressionSlotField f)
    {
        if (slot == null)
            return false; // Can be null during testing
        int index = findField(f);
        CaretPos fieldPos = absolutePos(new CaretPos(index, null));
        return slot.suggestingFor(fieldPos);
    }

    public SplitInfo trySplitOn(String target)
    {
        for (int i = 0; i < operators.size(); i++)
        {
            Operator op = operators.get(i);
            if (op != null && op.get().equals(target))
            {
                return new SplitInfo(getCopyText(null, new CaretPos(i, fields.get(i).getEndPos())), getCopyText(new CaretPos(i + 1, fields.get(i + 1).getStartPos()), null));
            }
        }
        return null;
    }

    public boolean isAlmostBlank()
    {
        // It is allowed to have structure (brackets, quotes, operators) as long as all text fields are blank:
        return fields.stream().allMatch(ExpressionSlotComponent::isAlmostBlank);
    }
    
    public void notifyLostFocus(ExpressionSlotField except)
    {
        fields.forEach(f -> { if (f != except) f.notifyLostFocus(except); });
    }

    boolean isInSelection()
    {
        return anchorPos != null || (parent != null && parent.isInSelection());
    }

    /**
     * The escape status in a String literal.  For example, in the string literal:
     *   "Hi!\n C:\\Program Files"
     * we can pass various substrings to find the escape status (using curly brackets to
     * surround the string, to avoid confusion):
     * 
     * {Hi!} : NORMAL
     * {Hi!\}: AFTER_BACKSLASH
     * {Hi!\n}: NORMAL
     * {Hi!\n C:\}: AFTER_BACKSLASH
     * {Hi!\n C:\\}: NORMAL
     * 
     */
    private static enum EscapeStatus { NORMAL, AFTER_BACKSLASH }

    // package-visible:
    static enum RangeType { RANGE_CONSTANT, RANGE_NON_CONSTANT, NOT_RANGE }

    /**
     * Stores a mapping between a CaretPos structure and a traditional
     * integer position into a string.  This is used to map between the two, for example
     * when dealing with error messages that came from a string but need to be mapped back
     * into a location in the InfixExpression
     *
     * The posOuter field holds the CaretPos that points to a given text field, but without a final location
     * in the field.  So if you have an InfixExpression like "abc", posOuter will actually just be null,
     * startIndex will be 0 and endIndex will be 2.  But if you have "get(abc)", then posOuter would be
     * new CaretPos(1, null) [because the brackets are index 1 in the InfixExpression] and startIndex would be
     * 3 and endIndex would be 5.
     */
    // Package-visible
    static class CaretPosMap
    {
        private final CaretPos posOuter; // Has null at inner point where you should put within-field caret index
        private final int startIndex; // The corresponding index within the entire String
        private final int endIndex; // The corresponding index within the entire String

        // Package-visible
        CaretPosMap(CaretPos posOuter, int startIndex, int endIndex)
        {
            this.posOuter = posOuter;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
        }
        
        /**
         * Makes a copy of this CaretPosMap, with the given index on the beginning of the posOuter field.
         */
        public CaretPosMap wrap(int index)
        {
            return new CaretPosMap(new CaretPos(index, posOuter), startIndex, endIndex);
        }
        
        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            CaretPosMap other = (CaretPosMap) obj;
            if (endIndex != other.endIndex)
                return false;
            if (posOuter == null) {
                if (other.posOuter != null)
                    return false;
            }
            else if (!posOuter.equals(other.posOuter))
                return false;
            if (startIndex != other.startIndex)
                return false;
            return true;
        }
        // For debugging:
        @Override
        public String toString()
        {
            return "CaretPosMap [posOuter=" + posOuter + ", startIndex="
                    + startIndex + ", endIndex=" + endIndex + "]";
        }
        
        
    }

    /**
     * A simple class to allow a mutable integer to be passed around while building a CaretPosMap
     */
    // Package-visible
    static class IntCounter
    {
        public int counter = 0;
    }
}
