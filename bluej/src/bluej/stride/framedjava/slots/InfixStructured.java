/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2016,2017,2018,2019,2020 Michael Kölling and John Rosenberg
 
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

import bluej.stride.framedjava.slots.StructuredSlot.SplitInfo;
import bluej.stride.generic.Frame.View;
import bluej.stride.generic.InteractionManager;
import bluej.utility.Debug;
import bluej.utility.Utility;
import bluej.utility.javafx.*;
import bluej.utility.javafx.binding.DeepListBinding;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.*;
import javafx.scene.layout.Region;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class is the major part of the display and logic for expressions.  Here's how the architecture works:
 * 
 * <h3>The Expression Tree</h3>
 *     
 * This gets turned into a rich tree of expressions.  The expressions nest as follows.
 * 
 *  - There is InfixStructured (this class), which holds a sequence of operands (see the "fields" field)
 *    and in the middle of each of these is optionally an operator (Operator class, "operators" field).
 *    An operand (which implements StructuredSlotComponent) can be:
 *      - a fairly standard text field (StructuredSlotField),
 *      - a string literal (StringLiteralExpressionSlot) or
 *      - a bracketed expression (BracketedStructured).
 *      
 *  - A BracketedStructured is really a thin wrapper around InfixStructured.  So an expression tree is
 *    really made up primarily of InfixStructured, with each one (excluding the top-level) wrapped in
 *    a BracketedStructured.
 *    
 *  Given an expression like:
 * 
 *    1 + 2 * getX() + convert(5 + 7) - (3 * (15 + 6))
 *  
 *  We have a top-level InfixStructured.  This has an array of eleven operands:
 *  
 *   [0]: StructuredSlotField, content: "1"
 *   [1]: StructuredSlotField, content: "2"
 *   [2]: StructuredSlotField, content: "getX"
 *   [3]: BracketedStructured, with an InfixExpressionSlot containing one [empty] operand (and no operators):
 *           [0]: StructuredSlotField, content: ""
 *   [4]: StructuredSlotField, content: ""
 *   [5]: StructuredSlotField, content: "convert"
 *   [6]: BracketedStructured, with an InfixExpressionSlot with two operands:
 *            [0]: StructuredSlotField, content: "5"
 *            [1]: StructuredSlotField, content: "7"
 *          And one operator:
 *            [0]: Operator, content: "+"
 *   [7]: StructuredSlotField, content: ""
 *   [8]: StructuredSlotField, content: ""
 *   [9]: BracketedStructured, with an InfixExpressionSlot with three operands:
 *            [0]: StructuredSlotField, content "3"
 *            [1]: BracketedStructured, with two operands:
 *                    [0]: StructuredSlotField, content: "15"
 *                    [1]: StructuredSlotField, content: "6"
 *                 and one operator:
 *                    [0]: Operator, content: "+"
 *            [2]: StructuredSlotField, content ""
 *         And two operators:
 *            [0]: Operator, content "*"
 *            [1]: null
 *   [10]:StructuredSlotField, content ""
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
 *   - Each BracketedStructured is surrounded by two null operators.  See explanation below.
 *   - Operators are only null when they are adjacent to a BracketedStructured.
 *   - A BracketedStructured never appears in the very first or very last entry in the operands array. (See below.)
 *  
 *  The non-obvious rules relate to BracketedStructured.  To start with, consider the expression "1>=2".  In
 *  a standard text editor, this a string of length 4, and there are 5 possible caret positions (before each
 *  character, and after the last one).  If we turn this into an InfixStructured, we get two operands and an operator:
 *    field 0: StructuredSlotField, content "1"
 *    operator 0: Operator, content ">="
 *    field 1: StructuredSlotField, content "2"
 *    
 *  In our version, we have four valid caret positions: before the '1', after the '1', before the '2', after the '2'.
 *  We lose only the caret position in the middle of the operator, but you can always alter it by going before/after it
 *  and using delete/backspace, which will chip off one character at a time from the oprator.
 *  
 *  Now consider the same expression with some brackets: "1>=(2)".  In a standard text editor, it's a string
 *  of length 6, and you have 7 caret positions.  My first attempt at turning this into an InfixStructured
 *  [NOTE: THIS IS NOT HOW IT NOW WORKS!] was:
 *    field 0: StructuredSlotField, content "1"
 *    operator 0: Operator, content ">="
 *    field 1: BracketedStructured, containing:
 *      field 0: StructuredSlotField, content "2"
 *      
 *  This seems logical, but we would only have 4 caret positions -- again:  before the '1', after the '1', before the '2', after the '2'.
 *  If you want to add content after the closing bracket, there is no way to do so.  If you want to delete
 *  the '=' (to turn it into strictly greater than), there is no caret position from which to do so, and nor
 *  can you add any content between the operator and the bracket.  So while this scheme correctly contains all
 *  the current text, it *does not allow all possible edits to take place*.  The fix for this is as follows:
 *  
 *  Each BracketedStructured must always have an StructuredSlotField before and after it, with no operator
 *  inbetween the brackets and field.  These fields correspond to the space just outside the brackets, to
 *  always allow editing in those positions.  Often, these extra fields will be empty (e.g. in the case of
 *  "1*(2-3)/5", these extra fields will be empty.  However, in the case of method calls such as "move(5)",
 *  the "move" occupies the field beforehand, and in the case of casts such as "(int)x", the "x" occupies
 *  the field afterwards.
 *  
 *  This rule is what leads to several of our rules above.  Since BracketedStructured always has these fields
 *  before and after with no operators, we store null in the corresponding operator fields, and BracketedStructured
 *  can never be first or last in the InfixStructured.
 *  
 *  <h3>Floating-Point Literals</h3>
 *  
 *  One special case in expressions is floating point literals.  Given an expression like "-x.y", we would usually
 *  turn this into an InfixStructured with a minus operator and dot operator.  However, for expressions like
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
 *  inbetween those there will be a BracketedStructured inserted which contains "234".
 *  
 *  There is complicated logic to the insertion, which is covered in the comments for the insertChar method, below.
 *  
 *  <h3>Selection</h3>
 *  
 *  InfixStructured items support selection.  The logic for selection in expressions is similar to that for frames:
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
 *  At the outer-most level, we have an InfixStructured.  Here, the first integer is an index into the "fields" array,
 *  indicating which slot we are in.  You then strip off the first integer, and use the rest of the list to find
 *  the position in the subfield.  If it's an StructuredSlotField, the index is simply a standard caret position.
 *  If it's a BracketedStructured, it applies to the contained InfixStructured.
 *  
 *  So ultimately, the CaretPos is a list of integers where the last one applies to an StructuredSlotField, and the preceding
 *  ones are indexes into InfixStructured's "fields" arrays.
 */
public abstract class InfixStructured<SLOT extends StructuredSlot<?, INFIX, ?>, INFIX extends InfixStructured<SLOT, INFIX>>
        implements TextFieldDelegate<StructuredSlotField>
{
    // Regex matching JLS "Digits"; underscores can appear within
    private final static String DIGITS_REGEX = "\\d([0-9_]*\\d)?";
    private final static String HEX_DIGITS_REGEX = "[0-9A-Fa-f]([0-9A-Fa-f_]*[0-9A-Fa-f])?";
    private static final String DEFAULT_RANGE_START = lang.stride.Utility.class.getName() + "(";

    // fields is always 1 longer than operators. Always an StructuredSlotField in first and last position (which may be same, when size 1).
    protected final ProtectedList<StructuredSlotComponent> fields = new ProtectedList<>();
    // Operator 0 is between field 0 and field 1.  Operator N trails field N.
    // Can be null when the operator is effectively a bracket.
    protected final ProtectedList<Operator> operators = new ProtectedList<>();
    
    //private final FlowPane components = new FlowPane();    
    //private final HBox components = new HBox();
    private final ObservableList<Node> components = FXCollections.observableArrayList();
    protected final BracketedStructured<INFIX, SLOT> parent; // null if top-level
    // The characters which cause us to go up a level (e.g. a closing bracket) or
    // move to the next slot
    private final Set<Character> closingChars = new HashSet<>();
    
    private final InteractionManager editor;
    protected final SLOT slot; // Can be null when testing, but otherwise non-null
    
    private final StringProperty textProperty = new SimpleStringProperty();
    private final BooleanProperty previewingJavaRange = new SimpleBooleanProperty(false);
    private final StringProperty startRangeText = new SimpleStringProperty(DEFAULT_RANGE_START);
    private final StringProperty endRangeText = new SimpleStringProperty(")");
    /**
     * The caret position for the start of the selection (null if and only if no selection)
     */
    private CaretPos anchorPos;

    /**
     * Create top-level InfixStructured, just inside the StructuredSlot
     */
    public InfixStructured(InteractionManager editor, SLOT slot, StructuredSlot.ModificationToken token)
    {
        this(editor, slot, "", null, token);
    }
    
    /**
     * Create InfixStructured just inside the given BracketedStructured.
     * 
     * @param initialContent The initial content.  This should be suitable for putting into a single
     *    StructuredSlotField; it should not contain any operators or brackets, etc.  If you need
     *    to add rich content, pass "" for this parameter and insert the rich content afterwards.
     */
    public InfixStructured(InteractionManager editor, SLOT slot, String initialContent, BracketedStructured wrapper,
            StructuredSlot.ModificationToken token, Character... closingChars)
    {
        this.editor = editor;
        this.parent = wrapper;
        this.closingChars.addAll(Arrays.asList(closingChars));
        this.slot = slot;
        
        this.textProperty.set(initialContent);
        // When starting, add just one empty field:
        fields.add(makeNewField(initialContent, false), token);

        final ObservableList<Node> extraPrefix = FXCollections.observableArrayList();
        final ObservableList<Node> extraSuffix = FXCollections.observableArrayList();

        JavaFXUtil.addChangeListener(previewingJavaRange, previewing -> {
            if (previewing)
            {
                Label start = new Label();
                start.textProperty().bind(startRangeText);
                extraPrefix.setAll(start);
                Label end = new Label();
                end.textProperty().bind(endRangeText);
                extraSuffix.setAll(end);
            }
            else
            {
                extraPrefix.clear();
                extraSuffix.clear();
            }
        });

        new DeepListBinding<Node>(components) {

            @Override
            protected Stream<ObservableList<?>> getListenTargets()
            {
                return Stream.concat(Stream.of(fields.observable(), operators.observable(), extraPrefix, extraSuffix), fields.stream().map(StructuredSlotComponent::getComponents));
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
        
        fields.observable().addListener((ListChangeListener)c -> {
            // If we are not a single field, remove prompts on all fields:
            if (fields.size() != 1)
            {
                fields.forEach(comp -> {
                    if (comp instanceof StructuredSlotField)
                    {
                        ((StructuredSlotField)comp).setPromptText("");
                    }
                });
            }   
        });
        
        
        // Now we calculate precedence as follows:
        // - We find the lowest precedence operator (preferring leftmost).
        //   - We recursively traverse the LHS and RHS of this operator with same algorithm.
        // - On return, if lowest operator in LHS or RHS is identical to outer, we take
        components.addListener((ListChangeListener)c -> 
            calculatePrecedences(operators.stream().collect(Collectors.toList()), fields.stream().map(StructuredSlotComponent::isFieldAndEmpty).limit(operators.size()).collect(Collectors.toList()))
        );
        
        updateBreaks();
        JavaFXUtil.addChangeListener(textProperty, value -> updateBreaks());
    }


    public Stream<? extends Node> makeDisplayClone(InteractionManager editor)
    {
        // Important that we flatMap after interleaving, to preserve ordering:
        return Utility.interleave(
                fields.stream().map(c -> c.makeDisplayClone(editor)),
                operators.stream().map(o -> o == null ? Stream.<Node>empty() : Stream.of(o.makeDisplayClone(editor))))
                .flatMap(x -> x);
    }
    
    private void updateBreaks()
    {
        // Update possible breaks.
        // Breaks are not allowed between a method name and the opening bracket:
        
        // Spot the method calls and inform the brackets:
        // Start at second item because first can't be a bracket:
        for (int i = 1; i < fields.size(); i++)
        {
            if (fields.get(i) instanceof BracketedStructured)
            {
                // A method call is a bracketed expression with non-empty
                // field directly before it:
                ((BracketedStructured)fields.get(i)).notifyIsMethodParams(
                    fields.get(i-1) instanceof StructuredSlotField
                        && !fields.get(i-1).isFieldAndEmpty());
            }
        }
    }

    // package-visible and static for testing
    static OpPrec calculatePrecedences(List<Operator> ops, List<Boolean> isUnary)
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
            
            OpPrec lhsPrec = calculatePrecedences(lhs, lhsUnary);
            OpPrec rhsPrec = calculatePrecedences(rhs, rhsUnary);
            
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
            return new OpPrec(lowestPrec, ourLevel);
        }
        else
        {
            // No operators (none, or all are null)
            return new OpPrec(-1, 0);
        }
    }

    private static boolean precedesDotInFloatingPointLiteral(String before) {
        return before.matches("\\A\\s*[+-]?" + DIGITS_REGEX + "\\z")
                || before.matches("\\A\\s*0x" + HEX_DIGITS_REGEX + "\\z");
    }

    protected StructuredSlotField makeNewField(String content, boolean stringLiteral)
    {
        StructuredSlotField f = new StructuredSlotField(this, content, stringLiteral);
        if (editor != null) // Can be null during testing
            editor.setupFocusableSlotComponent(slot, f.getNodeForPos(null), true, () -> slot.getExtensions(), slot.getHints());
        f.onKeyPressedProperty().set(event -> {
            if (event.isShiftDown() && event.isControlDown() && event.getText().length() > 0 && event.getCode() != KeyCode.CONTROL && event.getCode() != KeyCode.SHIFT)
            {
                slot.notifyModifiedPress(event.getCode());
                event.consume();
                return;
            }

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
            if (!(e.isShortcutDown() || e.getButton() == MouseButton.MIDDLE))
                return; // Either they must be holding shortcut button, or middle clicking
            
            // check for click on underlined region
            CaretPos relNearest = getNearest(e.getSceneX(), e.getSceneY(), false, Optional.empty()).getPos();
            CaretPos absNearest = absolutePos(relNearest);
            // First we check existing underlines:
            FXPlatformRunnable linkRunnable = slot.getOverlay().hoverAtPos(slot.getTopLevel().caretPosToStringPos(absNearest, false));
            if (linkRunnable != null)
            {
                // We've found an existing underline; run the action:
                linkRunnable.run();
            }
            else
            {
                // No existing underline; this may be because:
                //  - there isn't anything of interest in this position
                //  - the mouse click has changed things (e.g. in var frames, clicking the type slot focuses it and changes the frame content)
                //  - the user middle-clicked, so we didn't scan in advance
                // Thus to cover the latter two options, we scan again to be sure:
                slot.withLinksAtPos(absNearest, optLink -> 
                    optLink.ifPresent(link -> {
                        FXPlatformRunnable onClick = link.getOnClick();
                        if (onClick != null)
                            onClick.run();
                    })
                );
            }
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
            StructuredSlotComponent foc = fields.get(pos.index);
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
     * Finds all the TextOverlayPosition items at the beginning and ends of all StructuredSlotField
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
                s = Stream.concat(s, Stream.of(((StructuredSlotField) fields.get(fields.size() - 1)).calculateOverlayEnd()));
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
            s = Stream.concat(s, Stream.of(((StructuredSlotField) fields.get(fields.size() - 1)).calculateOverlayEnd()));

        return s;
    }
    
    // As position on overlay:
    public TextOverlayPosition calculateOverlayPos(CaretPos p)
    {
        //if (p == null)
        //    return new TextOverlayPosition(0.0, 0.0, 5.0);
        
        StructuredSlotComponent f = fields.get(p.index);
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
    public void backwardAtStart(StructuredSlotField f)
    {
        backwardAtStart((StructuredSlotComponent) f);
    }
    
    /**
     * Handle moving backwards (left) when at the start of the given field.
     * 
     * This will either move to the end of the appropriate preceding field, or if the passed
     * parameter is the first field, will take the appropriate action via its surrounding
     * BracketedStructured/StructuredSlot
     */
    public void backwardAtStart(StructuredSlotComponent f)
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
    public void forwardAtEnd(StructuredSlotField f)
    {
        forwardAtEnd((StructuredSlotComponent)f);
    }
    
    /**
     * Handle moving forwards (right) when at the start of the given field.
     * 
     * This will either move to the beginning of the appropriate following field, or if the passed
     * parameter is the last field, will take the appropriate action via its surrounding
     * BracketedStructured/StructuredSlot
     */
    public void forwardAtEnd(StructuredSlotComponent f)
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
    public boolean home(StructuredSlotField f)
    {
        getFirstField().focusAtStart();
        return true;
    }

    /**
     * Handle End being pressed in the given field
     */
    @Override
    public boolean end(StructuredSlotField f, boolean asPartOfNextWordCommand)
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
    public boolean selectHome(StructuredSlotField id, int caretPos)
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
    public boolean selectEnd(StructuredSlotField id, int caretPos)
    {
        int i = findField(id);
        setAnchorIfUnset(new CaretPos(i, new CaretPos(caretPos, null)));
        int dest = fields.get(i) instanceof StringLiteralExpression ? i : fields.size() - 1;
        fields.get(dest).focusAtEnd();
        drawSelection(new CaretPos(dest, fields.get(dest).getEndPos()));
        return true;
    }
    
    //package-visible
    StructuredSlotField getFirstField()
    {
        return (StructuredSlotField)fields.get(0);
    }

    private StructuredSlotField getLastField()
    {
        return (StructuredSlotField)fields.get(fields.size() - 1);
    }

    @Override
    public boolean previousWord(StructuredSlotField f, boolean atStart) 
    {
        if (atStart) {
            backwardAtStart(f);
            return true;
        }
        // Let the field handle it if not at start:
        return false;
    }
    
    @Override
    public boolean nextWord(StructuredSlotField f, boolean atEnd)
    {
        if (atEnd) {
            forwardAtEnd(f);
            return true;
        }
     // Let the field handle it if not at end:
        return false;
    }
    
    @Override
    public boolean endOfNextWord(StructuredSlotField f, boolean atEnd)
    {
        return nextWord(f, atEnd);
    }
    
    @Override
    public boolean selectAll(StructuredSlotField f)
    {
        home(null);
        // Arguments are position we are selecting *from*, not to:
        selectEnd(getFirstField(), 0);
        return true;
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public boolean selectNextWord(StructuredSlotField f)
    {
        setAnchorIfUnset(getCurrentPos());
        if (Objects.equals(f.getCurrentPos(), f.getEndPos()))
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
    @OnThread(Tag.FXPlatform)
    public boolean selectPreviousWord(StructuredSlotField f)
    {
        setAnchorIfUnset(getCurrentPos());
        if (Objects.equals(f.getCurrentPos(), f.getStartPos()))
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
    @OnThread(Tag.FXPlatform)
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
    public boolean selectBackward(StructuredSlotField f, int posInSlot)
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
    public boolean selectForward(StructuredSlotField f, int posInSlot, boolean atEnd)
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
    
    public void delete(StructuredSlotField f, int start, int end)
    {
        modification(token -> f.setText(f.getText().substring(0, start) + f.getText().substring(end), token));
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
    @OnThread(Tag.FXPlatform)
    public boolean deleteSelection()
    {
        return modificationReturnPlatform(token -> deleteSelectionImpl(getCurrentPos(), token) != null);
    }
    
    @OnThread(Tag.FXPlatform)
    private CaretPos deleteSelectionImpl(CaretPos cur, StructuredSlot.ModificationToken token)
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
            if (fields.get(start.index) instanceof BracketedStructured)
            {
                InfixStructured nested = ((BracketedStructured)fields.get(start.index)).getContent();
                nested.setAnchorIfUnset(start.subPos);
                return new CaretPos(start.index, nested.deleteSelectionImpl(end.subPos, token));
            }
            else if (fields.get(start.index) instanceof StringLiteralExpression)
            {
                StringLiteralExpression s = (StringLiteralExpression) fields.get(start.index);
                StructuredSlotField f = s.getField();
                f.setText(f.getText().substring(0, start.subPos.index) + f.getText().substring(end.subPos.index), token);
                return start;
            }
        }
        // Collapse the remaining before/after content into startField, and delete necessary fields and operators:
        StructuredSlotField startField = (StructuredSlotField)fields.get(start.index);
        StructuredSlotField endField = (StructuredSlotField)fields.get(end.index);
        startField.setText(startField.getText().substring(0, start.subPos.index) + endField.getText().substring(end.subPos.index), token);
        for (int i = start.index + 1; i <= end.index;i++)
        {
            operators.remove(start.index, token);
            fields.remove(start.index + 1, token);
        }
        CaretPos pos = checkFieldChange(start.index, start, token);
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
    @OnThread(Tag.FXPlatform)
    public boolean deletePrevious(StructuredSlotField f, int posInField, boolean atStart)
    {
        modificationPlatform(token -> positionCaret(deletePrevious_(f, posInField, atStart, token)));
        return true;
    }
    
    @OnThread(Tag.FXPlatform)
    public CaretPos deletePreviousAtPos(CaretPos p, StructuredSlot.ModificationToken token)
    {
        StructuredSlotComponent c = fields.get(p.index);
        if (c instanceof StructuredSlotField)
        {
            return deletePrevious_((StructuredSlotField)c, p.subPos.index, p.subPos.index == 0, token);
        }
        else if (c instanceof StringLiteralExpression)
        {
            return deletePrevious_(((StringLiteralExpression)c).getField(), p.subPos.index, p.subPos.index == 0, token);
        }
        else if (c instanceof BracketedStructured)
        {
            return new CaretPos(p.index, ((BracketedStructured)c).getContent().deletePreviousAtPos(p.subPos, token));
        }
        throw new IllegalStateException();
    }

    // package-visible for testing, implementation of deletePrevious
    @OnThread(Tag.FXPlatform)
    CaretPos deletePrevious_(StructuredSlotField f, int posInField, boolean atStart, StructuredSlot.ModificationToken token)
    {
        int index = findField(f);
        if (atStart)
        {
            // If it is not the first field in the InfixStructured:
            if (index > 0)
            {
                Operator prev = operators.get(index - 1);
                
                if (prev == null)
                {
                    // No operator, must be closing bracket/quote beforehand
                    // In this case, we want to get rid of the brackets/quotes, and fold the contents into the slots adjacent to the brackets
                    boolean inString = fields.get(index) instanceof StringLiteralExpression;
                    return flattenCompound(inString ? index : index - 1, !inString, token);
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
                        return checkFieldChange(index - 1, new CaretPos(index, new CaretPos(0, null)), token);
                    }
                    else
                    {
                        // Otherwise, we will delete the operator and fold the joined contents of the surrounding
                        // fields, and any remaining content from the operator
                        // (only applicable with "new " operator) into the joined field
                        operators.remove(index - 1, token);
                        StructuredSlotField prevField = (StructuredSlotField)fields.get(index - 1);
                        String opRemaining = "";
                        if (op.equals("new "))
                        {
                            opRemaining = "new"; // Delete the space
                        }
                        int newPos = prevField.getText().length() + opRemaining.length();
                        prevField.setText(prevField.getText() + opRemaining + f.getText(), token);
                        fields.remove(index, token);
                        
                        //TODO act different if compound is involved
                        return checkFieldChange(index - 1, new CaretPos(index - 1, new CaretPos(newPos, null)), token);
                    }
                }
            }
            else
            {
                // If it is the first field, delegate to parent or slot:
                if (parent != null) {
                    return new CaretPos(-1, parent.flatten(false, token));
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
            f.setText(s.substring(0, posInField - 1) + s.substring(posInField), token);
            CaretPos p = checkFieldChange(index, new CaretPos(index, new CaretPos(posInField - 1, null)), token);
            return p;
        }
    }
    
    @OnThread(Tag.FXPlatform)
    public CaretPos flattenCompound(StructuredSlotComponent item, boolean caretAtEnd,
            StructuredSlot.ModificationToken token)
    {
        return flattenCompound(fields.indexOf(item), caretAtEnd, token);
    }
    
    /**
     * Given the index of a compound field (e.g. BracketedStructured, StringLiteralExpressionField),
     * takes its content and flattens it, i.e. unwraps it from the BracketedStructured and inserts it
     * into the "index" position in this InfixStructured.  Used when deleting brackets but you want
     * to retain the content.
     */
    @OnThread(Tag.FXPlatform)
    private CaretPos flattenCompound(int index, boolean atEnd, StructuredSlot.ModificationToken token)
    {
        StructuredSlotField fieldBefore = (StructuredSlotField) fields.get(index - 1);
        String after = fields.get(index+1).getCopyText(null, null);
        String content = fields.get(index).getCopyText(fields.get(index).getStartPos(), fields.get(index).getEndPos());
        
        // Must remove second field first:
        fields.remove(index+1, token);
        if (operators.remove(index, token) != null) throw new IllegalStateException();
        fields.remove(index, token);
        if (operators.remove(index-1, token) != null) throw new IllegalStateException();
        
        int len = fieldBefore.getText().length();
        CaretPos mid = insertImpl(fieldBefore, len, content, false, token);
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
    @OnThread(Tag.FXPlatform)
    public boolean deleteNext(StructuredSlotField f, int posInField, boolean atEnd)
    {
        modificationPlatform(token -> positionCaret(deleteNextImpl(f, posInField, atEnd, token)));
        return true;
    }
    
    @OnThread(Tag.FXPlatform)
    private CaretPos deleteNextImpl(StructuredSlotField f, int posInField, boolean atEnd,
            StructuredSlot.ModificationToken token)
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
                    boolean inString = fields.get(index) instanceof StringLiteralExpression;
                    return flattenCompound(inString ? index : index + 1, inString, token);
                }
                else
                {
                    // Operator follows
                    String op = next.get();
                    // If it is still a valid operator without the first char, then just lop the first
                    // character off but leave as an operator: 
                    if (op.length() > 1 && isOperator(op.substring(1)))
                    {
                        next.set(op.substring(1));
                        return checkFieldChange(index, new CaretPos(index, new CaretPos(posInField, null)), token);
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
                        
                        operators.remove(index, token);
                        int newPos = f.getText().length();
                        f.setText(f.getText() + opRemaining + ((StructuredSlotField)fields.get(index + 1)).getText(), token);
                        fields.remove(index + 1, token);
                        
                        
                        return checkFieldChange(index, new CaretPos(index, new CaretPos(newPos, null)), token);
                    }
                }
            }
            else
            {
                if (parent != null)
                    return new CaretPos(-1, parent.flatten(true, token));
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
            f.setText(s.substring(0, posInField) + s.substring(posInField+1), token);
            return checkFieldChange(index, new CaretPos(index, new CaretPos(posInField, null)), token);
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
    
    private int findField(StructuredSlotComponent f)
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
    @OnThread(Tag.FXPlatform)
    public void insert(StructuredSlotField f, int posInField, String text)
    {
        modificationPlatform(token -> insertImpl(f, posInField, text, true, token));
    }
    
    /**
     * Implementation of insert method, used directly by test classes.
     */
    @OnThread(Tag.FXPlatform)
    public CaretPos insertImpl(StructuredSlotField f, int posInField, String text, boolean user,
            StructuredSlot.ModificationToken token)
    {
        if ( parent == null && !text.isEmpty() && text.length() > 0 && closingChars.contains(text.charAt(0)) ) {
            slot.focusNext();
            return null;
        }
        int index = findField(f);
        
        if (index == -1)
        {
            return null; // Must be initialising; nothing to do here
        }        
        CaretPos pos = new CaretPos(index, new CaretPos(posInField, null));
        
        // Unless it's a opening bracket, any insertion must involve first deleting the selection:
        if (text.length() > 0 && !isOpeningBracket(text.charAt(0)) && text.charAt(0) != '\"' && text.charAt(0) != '\'')
        {
            CaretPos postDeletion = deleteSelectionImpl(pos, token);
            if (postDeletion != null)
                pos = postDeletion;
        }
        
        for (int i = 0; i < text.length(); i++)
        {
            pos = insertChar(pos, text.charAt(i), user, token);
            
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
    
    @OnThread(Tag.FXPlatform)
    CaretPos insertAtPos(CaretPos p, String after, StructuredSlot.ModificationToken token)
    {
        StructuredSlotComponent f = fields.get(p.index);
        if (f instanceof StructuredSlotField) {
            return insertImpl((StructuredSlotField)fields.get(p.index), p.subPos.index, after, false, token);
        }
        if (f instanceof StringLiteralExpression) {
            return insertImpl(((StringLiteralExpression)fields.get(p.index)).getField(), p.subPos.index, after, false, token);
        }
        return new CaretPos(p.index, ((BracketedStructured)fields.get(p.index)).testingContent().insertAtPos(p.subPos, after, token));
    }

    @OnThread(Tag.FXPlatform)
    protected CaretPos insertChar(CaretPos pos, char c, boolean user, StructuredSlot.ModificationToken token)
    {
        final StructuredSlotComponent slot = fields.get(pos.index);
        // Bit hacky to use instanceof, but it's easier to have logic here than in subclasses:
        if (slot instanceof StructuredSlotField)
        {
            final StructuredSlotField f = (StructuredSlotField)slot;
            final Operator prev = pos.index == 0 ? null : operators.get(pos.index - 1);
            final Operator next = pos.index >= operators.size() ? null : operators.get(pos.index);
            int posInField = pos.subPos.index;
            
            if (isDisallowed(c))
                // No semi-colons allowed, except in string literals:
                return pos;
            
            if (Character.isWhitespace(c) && !f.getText().substring(0, posInField).equals("new"))
            {
                // No whitespace allowed in slots (except string literals, handled later, and after "new")
                return pos;
            }
            if (posInField == 0 && prev != null &&
                    isOperator(prev.get() + c))
            {
                // We are at start of slot, with an operator before us,
                // and character can join with previous operator to form compound operator
                prev.set(prev.get() + c);
                return pos; // We effectively don't move
            }
            else if (posInField == f.getText().length() && next != null &&
                      isOperator("" + c + next.get()))
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
            else if (beginsOperator(c) && c != '.' && c != '+' && c != '-')
                // dot, plus and minus are inserted like normal chars, and checkFieldChange deals with them,
                // because they may be part of a floating point number
            {
                String before = f.getText().substring(0, posInField);
                String following = f.getText().substring(posInField);
                
                f.setText(before, token);
                operators.add(pos.index, new Operator("" + c, this), token);
                fields.add(pos.index + 1, makeNewField(following, false), token);
                return new CaretPos(pos.index + 1, new CaretPos(0, null));
            }
            else if (anchorPos != null && (isOpeningBracket(c) || c == '\"' || c == '\''))
            {
                // If there is a selection, enclose that in the bracket/quote:                
                String content = anchorPos.before(pos) ? getCopyText(anchorPos, pos) : getCopyText(pos, anchorPos);
                pos = deleteSelectionImpl(pos, token);
                pos = insertChar(pos, c, false, token);
                insertAtPos(pos, content, token);
                return new CaretPos(pos.index + 1, new CaretPos(0, null));
            }
            else if (isOpeningBracket(c))
            {
                // If we are at end of field, and bracket follows:
                if (posInField == f.getText().length() &&
                        pos.index + 1 < fields.size() && fields.get(pos.index + 1) instanceof BracketedStructured)
                {
                    BracketedStructured following = (BracketedStructured) fields.get(pos.index + 1);
                    if (following.getOpening() == c)
                    {
                        // Just overtype the bracket, and move inside:
                        return new CaretPos(pos.index + 1, new CaretPos(0, new CaretPos(0, null)));
                    }
                }
                
                String following = f.getText().substring(posInField);
                f.setText(f.getText().substring(0, posInField), token);
                operators.add(pos.index, null, token);
                
                fields.add(pos.index + 1, new BracketedStructured(editor, this, this.slot, c, "", token), token);
                if (pos.index + 1 >= operators.size() || operators.get(pos.index + 1) != null
                        || !(fields.get(pos.index + 2) instanceof StructuredSlotField))
                {
                    // Used to be operator directly after this field (or we are at end), must add another field to pad
                    // RHS (not allowed to have operator after compound with no field inbetween)
                    operators.add(pos.index + 1, null, token);
                    fields.add(pos.index + 2, makeNewField(following, false), token);
                }
                else
                {
                    StructuredSlotField follow = (StructuredSlotField)fields.get(pos.index+2);
                    follow.setText(following + follow.getText(), token);
                }
                return new CaretPos(pos.index + 1, new CaretPos(0, new CaretPos(0, null)));
            }
            else if (isClosingBracket(c))
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
            else if (c == '\"' || c == '\'')
            {
                String following = f.getText().substring(posInField);
                f.setText(f.getText().substring(0, posInField), token);
                operators.add(pos.index, null, token);
                fields.add(pos.index + 1, new StringLiteralExpression(c, makeNewField("", true), this), token);
                if (pos.index + 1 >= operators.size() || operators.get(pos.index + 1) != null || fields.get(pos.index + 2) instanceof StringLiteralExpression || fields.get(pos.index + 2) instanceof BracketedStructured)
                {
                    // Used to be operator directly after this field (or we are at end), must add another field to pad
                    // RHS (not allowed to have operator after compound with no field inbetween)
                    operators.add(pos.index + 1, null, token);
                    fields.add(pos.index + 2, makeNewField(following, false), token);
                }
                else
                {
                    StructuredSlotField follow = (StructuredSlotField)fields.get(pos.index+2);
                    follow.setText(following + follow.getText(), token);
                }
                return new CaretPos(pos.index + 1, new CaretPos(0, new CaretPos(0, null)));
            }
            else
            {
                if (f.getText().substring(0, posInField).equals("new")
                        && Character.isWhitespace(c))
                {
                    String following = f.getText().substring(posInField);
                
                    f.setText("", token);
                    operators.add(pos.index, new Operator("new ", this), token);
                    fields.add(pos.index + 1, makeNewField(following, false), token);
                    return new CaretPos(pos.index + 1, new CaretPos(0, null));
                }
                else
                {
                    // Just insert normally:
                    f.setText(f.getText().substring(0, posInField) + c + f.getText().substring(posInField), token);
                    
                    CaretPos overridePos = checkFieldChange(pos.index, new CaretPos(pos.index, new CaretPos(posInField+1, null)), c == '.', user, token);
                    return overridePos;
                }
            }
        }
        else if (slot instanceof BracketedStructured)
        {
            // This can occur when pasting/loading content:
            CaretPos newSubPos = ((BracketedStructured)slot).getContent().insertChar(pos.subPos, c, false, token);
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
            StringLiteralExpression lit = (StringLiteralExpression)slot;
            final StructuredSlotField f = lit.getField();
            final int posInField = pos.subPos.index;
            if ((c == '\"' || c == '\'') && ("" + c).equals(lit.getQuote()) && getEscapeStatus(f.getText().substring(0, posInField)) == EscapeStatus.NORMAL)
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
            f.setText(f.getText().substring(0, posInField) + c + f.getText().substring(posInField), token);
            return new CaretPos(pos.index, new CaretPos(posInField+1, null));
        }
        return null;
    }

    abstract protected boolean isDisallowed(char c);
    abstract protected boolean isOpeningBracket(char c);
    abstract protected boolean isClosingBracket(char c);

    public void setEditable(boolean editable)
    {
        fields.forEach(c -> c.setEditable(editable));
    }

    public boolean isNumericLiteral()
    {
        return fields.stream().allMatch(StructuredSlotComponent::isNumericLiteral);
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

    @OnThread(Tag.FXPlatform)
    private CaretPos checkFieldChange(int index, CaretPos pos, StructuredSlot.ModificationToken token)
    {
        return checkFieldChange(index, pos, false, false, token);
    }
    
    /**
     * Checks the field at the given index, and performs any rearrangements necessary that
     * relate to floating point literals.  This may involve turning a dot in a field into an
     * operator, or turning an operator back into field content (for dots, pluses and minuses).
     * 
     * Returns the new position (adjustment of the passed pos)
     * 
     */
    @OnThread(Tag.FXPlatform)
    private CaretPos checkFieldChange(int index, CaretPos pos, boolean addedDot, boolean user, StructuredSlot.ModificationToken token)
    {
        if (fields.get(index) instanceof StringLiteralExpression)
            return pos; // No need to do this for string literals

        StructuredSlotField f = (StructuredSlotField)fields.get(index);
        String prevOp = (index > 0 && operators.get(index - 1) != null)
                ? operators.get(index - 1).get() : "";
        String nextOp = (index < operators.size() && operators.get(index) != null)
                ? operators.get(index).get() : "";
        StructuredSlotField prevField = index > 0 && fields.get(index - 1) instanceof StructuredSlotField
                ? (StructuredSlotField)fields.get(index - 1) : null;
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
            boolean isDoubleDot = isOperator("..") && afterDot.startsWith(".");
            if (!supportsFloatLiterals() || !precedesDotInFloatingPointLiteral(beforeDot) || isDoubleDot /* two dots is operator */)
            {
                f.setText(beforeDot, token);
                 
                if (isDoubleDot)
                {
                    operators.add(index, new Operator("..", this), token);
                    fields.add(index + 1, makeNewField(afterDot.substring(1), false), token);
                }
                else
                {
                    boolean wasShowingSuggestions = slot != null && slot.isShowingSuggestions();
                    
                    operators.add(index, new Operator(".", this), token);
                    fields.add(index + 1, makeNewField(afterDot, false), token);
                    
                    // If a dot is entered and code completion was showing, re-trigger it in the new field:
                    // We use runLater to make sure the state is all settled before showing the completion:
                    // (we must be on FX thread, not loading, because we are modified while showing suggestions)
                    if (wasShowingSuggestions && user && addedDot)
                        JavaFXUtil.runPlatformLater(() -> slot.showSuggestionDisplay((StructuredSlotField)fields.get(index+1), 0, false));
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
                
                pos = checkFieldChange(index, pos, token);
                return checkFieldChange(index + 1, pos, token);
            }
            // else if it is a literal and it's in the slot already, leave in slot.
            // But we still need to continue in case plus or minus was just added, or second dot...
        }
                
        if (supportsFloatLiterals() && precedesDotInFloatingPointLiteral(f.getText()) && nextOp.equals("."))
        {
            // Need to merge dot back in:
            int prevLen = f.getText().length();
            f.setText(f.getText() + nextOp + ((StructuredSlotField)fields.get(index + 1)).getText(), token);
            operators.remove(index, token);
            fields.remove(index+1, token);
            
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
                operators.add(index, new Operator(f.getText().substring(plusMinusIndex, plusMinusIndex+1), this), token);
                f.setText(before, token);
                fields.add(index + 1, makeNewField(after, false), token);
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
            f.setText(f.getText() + nextOp + ((StructuredSlotField)fields.get(index + 1)).getText(), token);
            operators.remove(index, token);
            fields.remove(index+1, token);
            
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
            operators.remove(index - 1, token);
            fields.remove(index - 1, token);
            f.setText(prevOp + f.getText(), token);
            pos = new CaretPos(pos.index - 1, new CaretPos(pos.subPos.index + 1, null));
        }
        
        return pos;
        
    }

    /**
     * Does this structured slot support floating point literals?  True for expression slots, false for type slots
     */
    protected abstract boolean supportsFloatLiterals();

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
    
    // Delegate targets from Slot, via StructuredSlot:
    
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
    void withContent(BiConsumer<ProtectedList<StructuredSlotComponent>, ProtectedList<Operator>> setPrompts)
    {
        setPrompts.accept(fields, operators);
    }

    // Package-visible
    Region getNodeForPos(CaretPos pos)
    {
        StructuredSlotComponent f = fields.get(pos.index);
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
     * Only really applicable when called on the top-level InfixStructured.
     * Should probably be moved to StructuredSlot.
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
     * Only really applicable when called on the top-level InfixStructured.
     * Should probably be moved to StructuredSlot.
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
    
    public void blank(StructuredSlot.ModificationToken token)
    {
        token.check();
        operators.clear(token);
        fields.clear(token);
        fields.add(makeNewField("", false), token);
        anchorPos = null;
    }
    
    public boolean isFocused()
    {
        return fields.stream().anyMatch(StructuredSlotComponent::isFocused);
    }
    
    public boolean isCollapsible(StructuredSlotField f)
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
        boolean unaryAfter = index < operators.size() && canBeUnary(Utility.orNull(operators.get(index), Operator::get));
        
        if (fields.size() == 1 && parent == null)
        {
            if (slot != null && slot.canCollapse())
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

    @OnThread(Tag.FXPlatform)
    public void insertNext(BracketedStructured bracketedExpression, String text)
    {
        int index = fields.indexOf(bracketedExpression);
        insert((StructuredSlotField) fields.get(index + 1), 0, text);
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
    @OnThread(Tag.FXPlatform)
    public CaretPos testingInsert(String text, char rememberPos)
    {
        return modificationReturnPlatform(token -> {
            int index = text.indexOf(rememberPos);
            if (rememberPos != '\0' && index != -1)
            {
                String before = text.substring(0, index);
                String after = text.substring(index + 1);
                CaretPos p = insertImpl((StructuredSlotField)fields.get(0), 0, before, false, token);
                testingInsert(p, after);
                return p;
            }

            return insertImpl((StructuredSlotField)fields.get(0), 0, text, false, token);
        });
    }
    
    @OnThread(Tag.FXPlatform)
    CaretPos testingInsert(CaretPos p, String after)
    {
        return modificationReturnPlatform(token -> insertAtPos(p, after, token));
    }

    @OnThread(Tag.FXPlatform)
    CaretPos testingBackspace(CaretPos p)
    {
        return modificationReturnPlatform(token -> {
            StructuredSlotComponent f = fields.get(p.index);
            if (f instanceof StructuredSlotField)
                return deletePrevious_((StructuredSlotField)fields.get(p.index), p.subPos.index, p.subPos.index == 0, token);
            else if (f instanceof StringLiteralExpression)
                return deletePrevious_(((StringLiteralExpression)fields.get(p.index)).getField(), p.subPos.index, p.subPos.index == 0, token);
            else
                return new CaretPos(p.index, ((BracketedStructured)fields.get(p.index)).testingContent().testingBackspace(p.subPos));
        });
    }
    
    @OnThread(Tag.FXPlatform)
    CaretPos testingDelete(CaretPos p)
    {
        StructuredSlotComponent s = fields.get(p.index);
        StructuredSlotField f;
        if (s instanceof StructuredSlotField)
            f = (StructuredSlotField)fields.get(p.index);
        else if (s instanceof StringLiteralExpression)
            f = ((StringLiteralExpression)fields.get(p.index)).getField();
        else
            return new CaretPos(p.index, ((BracketedStructured)fields.get(p.index)).testingContent().testingDelete(p.subPos));
        
        return modificationReturnPlatform(token ->
                deleteNextImpl(f, p.subPos.index, p.subPos.index == f.getText().length(), token));
    }
    
    @OnThread(Tag.FXPlatform)
    CaretPos testingDeleteSelection(CaretPos start, CaretPos end)
    {
        anchorPos = start;
        return modificationReturnPlatform(token -> deleteSelectionImpl(end, token));
    }

    @OnThread(Tag.FXPlatform)
    CaretPos testingInsertWithSelection(CaretPos start, CaretPos end, char c)
    {
        anchorPos = start;
        return modificationReturnPlatform(token -> insertAtPos(end, "" + c, token));
    }

    @OnThread(Tag.FXPlatform)
    public void insertSuggestion(CaretPos p, String name, char opening, List<String> params, StructuredSlot.ModificationToken token)
    {
        StructuredSlotComponent f = fields.get(p.index);
        if (f instanceof StructuredSlotField)
        {
            // Blank the field (easier than working out prefixes etc)
            ((StructuredSlotField) f).setText("", token);
            // And insert the new name.  It may have a dot in it, so caret may move to another field:
            p = insertImpl((StructuredSlotField)f, 0, name, false, token);
            if (params != null)
            {
                StringBuilder commas = new StringBuilder();
                for (int i = 0; i < params.size() - 1; i++)
                    commas.append(',');
                
                if (p.index + 1 < fields.size() && fields.get(p.index + 1) instanceof BracketedStructured && ((BracketedStructured)fields.get(p.index + 1)).getOpening() == opening)
                {
                    BracketedStructured b = ((BracketedStructured)fields.get(p.index + 1));
                    if (b.getContent().isEmpty())
                    {
                        b.getContent().insertAtPos(new CaretPos(0, new CaretPos(0, null)), commas.toString(), token);
                    }
                    // else if there are brackets with content, leave them alone.

                    // We use a runLater as we need to request focus after the suggestion window has been hidden:
                    JavaFXUtil.runAfterCurrent(() ->
                    {
                        //In a method call with no params, focus the cursor after the parenthesis
                        if(params.size()==0)
                        {
                            b.focusAfter();
                        }
                        else //otherwise focus inside the parentesis
                        {
                            b.focusAtStart();
                        }
                    });
                }
                else
                {
                    insertAtPos(new CaretPos(p.index, new CaretPos(p.subPos.index, null)), "(" + commas.toString() + ")", token);
                    // If no parameters, focus after the brackets.  Otherwise, focus first parameter
                    StructuredSlotComponent focusField = fields.get(params.isEmpty() ? p.index + 2 : p.index + 1);
                    // We use a runLater as we need to request focus after the suggestion window has been hidden:
                    JavaFXUtil.runAfterCurrent(() ->
                        focusField.focusAtStart()
                    );
                }
            }
        }
        else
        {
            f.insertSuggestion(p.subPos, name, opening, params, token);
        }
    }

    @OnThread(Tag.FXPlatform)
    public abstract void calculateTooltipFor(StructuredSlotField expressionSlotField, FXConsumer<String> handler);
        
    protected CaretPos absolutePos(CaretPos p)
    {
        if (parent == null)
            return p;
        else
            return parent.absolutePos(p);
    }

    public CaretPos absolutePos(BracketedStructured bracketedExpression, CaretPos p)
    {
        return absolutePos(new CaretPos(fields.indexOf(bracketedExpression), p));
    }

    //package-visible
    InteractionManager getEditor()
    {
        return editor;
    }

    // List is as long as there are parameters, but contains null if the parameter is non-simple,
    // i.e. does not consist of a single field
    public List<StructuredSlotField> getSimpleParameters()
    {
        List<StructuredSlotField> r = new ArrayList<>();
        int lastComma = -1; // Just before first parameter, in effect
                
        for (int i = 0; i < fields.size(); i++)
        {
            StructuredSlotField f = fields.get(i) instanceof StructuredSlotField ? (StructuredSlotField)fields.get(i) : null;
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
            JavaFXUtil.ifOnPlatform(() -> slot.caretMoved());
        }
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public void escape()
    {
        slot.escape();        
    }

    public void addClosingChar(char closingChar)
    {
        this.closingChars.add(closingChar);
    }

    public Stream<InfixStructured<?, ?>> getAllExpressions()
    {
        return Stream.concat(Stream.of(this), fields.stream().flatMap(StructuredSlotComponent::getAllExpressions));
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
    List<StructuredSlot.PlainVarReference> findPlainVarUse(String name)
    {
        List<StructuredSlot.PlainVarReference> refs = new ArrayList<>();
        // We need to find any StructuredSlotField which contains oldName without being
        // preceded by a dot or succeeded by open bracket, and replace it with newName
        for (int i = 0; i < fields.size(); i++)
        {
            if (fields.get(i) instanceof StructuredSlotField)
            {
                StructuredSlotField f = (StructuredSlotField)fields.get(i);
                if (f.getText().equals(name)
                        && (i == 0 || operators.get(i - 1) == null || !operators.get(i - 1).get().equals("."))
                        && (i == fields.size() - 1 || !(fields.get(i) instanceof BracketedStructured) || ((BracketedStructured)fields.get(i)).getOpening() != '('))
                {
                    // It doesn't have dot before it, and doesn't have a bracket afterwards -- it's a reference:
                    refs.add(new StructuredSlot.PlainVarReference(text -> modification(token -> f.setText(text, token)), f.getNodeForPos(null)));
                }
            }
            else if (fields.get(i) instanceof BracketedStructured)
            {
                refs.addAll(((BracketedStructured)fields.get(i)).getContent().findPlainVarUse(name));
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
        if (! (fields.get(1) instanceof BracketedStructured))
            return false;
        BracketedStructured e = (BracketedStructured) fields.get(1);
        if (e.getOpening() == '{')
            return true;
        // We don't allow brackets around it, but this code would allow that:
        //else if (e.getOpening() == '(')
            //return e.getContent().isCurlyLiteral();
        else
            return false;
        
    }

    // package-visible
    void showHighlightedBrackets(BracketedStructured wrapper, CaretPos pos)
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
            StructuredSlotComponent f = fields.get(i);
            if (f instanceof BracketedStructured)
            {
                boolean cursorBefore =
                        i > 0 &&
                        pos != null && pos.index == i - 1 &&
                        fields.get(i - 1) instanceof StructuredSlotField &&
                        fields.get(i - 1).getEndPos().equals(pos.subPos);
                boolean cursorAfter =
                        i < fields.size() - 1 &&
                        pos != null && pos.index == i + 1 &&
                        fields.get(i + 1) instanceof StructuredSlotField &&
                        fields.get(i + 1).getStartPos().equals(pos.subPos);
                BracketedStructured e = (BracketedStructured)f;
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
                        startRangeText.set("");
                        endRangeText.set("; " + forLoopVarName.get() + "++");
                        rangeOp.setJavaPreviewRangeOverride("; " + forLoopVarName.get() + " <=");
                        break;
                    }
                    //Otherwise fall-through to case for general ranges:
                case RANGE_NON_CONSTANT:
                    previewingJavaRange.set(true);
                    startRangeText.set(lang.stride.Utility.class.getName() + "(");
                    endRangeText.set(")");
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

        if (fields.stream().allMatch(StructuredSlotComponent::isNumericLiteral))
            return RangeType.RANGE_CONSTANT;
        else
            return RangeType.RANGE_NON_CONSTANT;
    }

    @OnThread(Tag.FXPlatform)
    public void paste()
    {
        StructuredSlotField focused = fields.stream()
                .filter(f -> f instanceof StructuredSlotField && f.isFocused())
                .map(f -> (StructuredSlotField)f)
                .findFirst()
                .orElse(null);
        if (focused != null)
        {
            focused.paste();
        }
    }

    //package-visible
    StructuredSlot<?, ?, ?> getSlot()
    {
        return slot;
    }

    @OnThread(Tag.FXPlatform)
    public boolean suggestingFor(StructuredSlotField f)
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
        return fields.stream().allMatch(StructuredSlotComponent::isAlmostBlank);
    }
    
    public void notifyLostFocus(StructuredSlotField except)
    {
        fields.forEach(f -> { if (f != except) f.notifyLostFocus(except); });
    }

    boolean isInSelection()
    {
        return anchorPos != null || (parent != null && parent.isInSelection());
    }

    public int calculateEffort()
    {
        return fields.stream().filter(f -> f != null).mapToInt(StructuredSlotComponent::calculateEffort).sum() + operators.stream().filter(op -> op != null).mapToInt(op -> op.get().length()).sum();
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
     * into a location in the InfixStructured
     *
     * The posOuter field holds the CaretPos that points to a given text field, but without a final location
     * in the field.  So if you have an InfixStructured like "abc", posOuter will actually just be null,
     * startIndex will be 0 and endIndex will be 2.  But if you have "get(abc)", then posOuter would be
     * new CaretPos(1, null) [because the brackets are index 1 in the InfixStructured] and startIndex would be
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

        @Override
        public int hashCode()
        {
            return startIndex % 31;
        }

        // For debugging:
        @Override
        public String toString()
        {
            return "CaretPosMap [posOuter=" + posOuter + ", startIndex="
                    + startIndex + ", endIndex=" + endIndex + "]";
        }
        
        
    }


    abstract boolean isOperator(String s);

    abstract boolean beginsOperator(char c);
    
    abstract boolean canBeUnary(String s);
    
    abstract INFIX newInfix(InteractionManager editor, SLOT slot, String initialContent, BracketedStructured<?, SLOT> wrapper, StructuredSlot.ModificationToken token, Character... closingChars);

    private <T> T modificationReturn(FXFunction<StructuredSlot.ModificationToken, T> modificationAction)
    {
        if (slot != null) // can be null during testing
            return slot.modificationReturn(modificationAction);
        else
            return StructuredSlot.testingModification(modificationAction);
    }

    protected <T> void modification(FXConsumer<StructuredSlot.ModificationToken> modificationAction)
    {
        if (slot != null) // can be null during testing
            slot.modificationReturn(t -> {modificationAction.accept(t);return 0;});
        else
            StructuredSlot.testingModification(t -> {modificationAction.accept(t); return 0;});
    }

    @OnThread(Tag.FXPlatform)
    protected <T> void modificationPlatform(FXPlatformConsumer<StructuredSlot.ModificationToken> modificationAction)
    {
        // Defeat thread checker:
        modification(modificationAction::accept);
    }

    @OnThread(Tag.FXPlatform)
    private <T> T modificationReturnPlatform(FXPlatformFunction<StructuredSlot.ModificationToken, T> modificationAction)
    {
        // Defeat thread checker:
        return modificationReturn(modificationAction::apply);
    }
    
    //package-visible:
    String calculateText()
    {
        // Must filter out nulls after interleaving, to preserve ordering:
        return Utility.interleave(
            fields.stream().map(f -> f.getText()),
            operators.stream().map(o -> o == null ? null : o.get()))
            .filter(x -> x != null).collect(Collectors.joining());
    }
    
    /**
     * A simple class to allow a mutable integer to be passed around while building a CaretPosMap
     */
    // Package-visible
    static class IntCounter
    {
        public int counter = 0;
    }

    private static class OpPrec
    {
        final int prec;
        final int levels;
        OpPrec(int prec, int levels)
        {
            this.prec = prec;
            this.levels = levels;
        }
    }
}

