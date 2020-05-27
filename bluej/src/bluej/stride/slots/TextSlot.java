/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2016,2017,2019,2020 Michael Kölling and John Rosenberg
 
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
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package bluej.stride.slots;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import bluej.collect.StrideEditReason;
import bluej.editor.fixes.SuggestionList;
import bluej.editor.stride.FrameCatalogue;
import bluej.stride.framedjava.ast.links.PossibleLink;
import bluej.utility.javafx.*;
import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.BoundingBox;
import javafx.scene.Node;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.util.Duration;
import bluej.Config;
import bluej.stride.framedjava.ast.TextSlotFragment;
import bluej.stride.framedjava.elements.CodeElement;
import bluej.stride.framedjava.errors.CodeError;
import bluej.stride.framedjava.errors.ErrorAndFixDisplay;
import bluej.stride.framedjava.errors.ErrorAndFixDisplay.ErrorFixListener;
import bluej.stride.framedjava.frames.CodeFrame;
import bluej.stride.framedjava.slots.TextOverlayPosition;
import bluej.stride.generic.Frame;
import bluej.stride.generic.Frame.View;
import bluej.stride.generic.FrameContentRow;
import bluej.stride.generic.InteractionManager;
import bluej.editor.fixes.SuggestionList.SuggestionListListener;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A slot which handles single-field text input, for example variable name definition.
 *
 * The class is abstract, with some abstract methods to do with specific context behaviour that
 * are implemented by slim subclasses, but the vast majority of the functionality lies in this class.
 */
public abstract class TextSlot<SLOT_FRAGMENT extends TextSlotFragment> implements EditableSlot, ErrorFixListener, CopyableHeaderItem
{
    // Listeners which get informed of changes in the content of the slot
    private final List<SlotValueListener> listeners = new ArrayList<SlotValueListener>();
    // The editor in which this slot ultimately lies.
    protected final InteractionManager editor;
    
    // These two variables always point to the same thing, but due to Java's type
    // system, we can't declare a type for a single variable to hold them both
    protected final Frame frameParent;
    private final CodeFrame<? extends CodeElement> codeFrameParent;

    // The row in which we lie
    private final FrameContentRow row;

    // The class dealing with the actual GUI text field item
    private final SlotTextField field;
    // The suggestion list/autocomplete (contains null if not currently showing)
    private final ObjectProperty<SuggestionList> suggestionDisplayProperty = new SimpleObjectProperty<>();
    // The calculator which works out which completions are valid.  May be null if completion is not possible here.
    private final CompletionCalculator completionCalculator;
    // The X offset at which to show the autocomplete GUI item
    private final SimpleDoubleProperty suggestionXOffset = new SimpleDoubleProperty();
    // The code which is generated from this slot, depends on the kind of slot
    private SLOT_FRAGMENT slotElement;
    // The display showing any errors and quick fixes.  Null if not showing.
    private ErrorAndFixDisplay errorAndFixDisplay;

    // The list of all errors currently associated with the slot
    @OnThread(Tag.FXPlatform)
    private final List<CodeError> allErrors = new ArrayList<>();
    // The list of all errors actually showing for the slot.  (If two errors overlap,
    // // only one of them is shown, so allErrors may have more than shownErrors)
    @OnThread(Tag.FXPlatform)
    private final List<CodeError> shownErrors = new ArrayList<>();
    // The underlines currently being shown (for indicating link sources)
    private final List<Underline> underlines = new ArrayList<>();
    // List of recent values of the slot, for local undo
    private final ObservableList<String> recentValues = FXCollections.observableArrayList();
    // The error currently being shown due to mouse hover, if any (null if none)
    private CodeError hoverErrorCurrentlyShown;
    // We must keep a reference to this to avoid problems with GC and weak listeners:
    private final BooleanBinding effectivelyFocusedProperty;

    /**
     * Creates a text slot.  Will be called from subclasses only
     *
     * @param editor The editor in which we lie
     * @param frameParent The frame in which we lie
     * @param codeFrameParent Ditto, but typed as CodeFrame
     * @param row The row in which we lie
     * @param completionCalculator The completion calculator to be used for auto-completion.  Null iff auto-completion should be disabled
     * @param stylePrefix The prefix to use for CSS style classes
     * @param hints Hints to show in the cheat sheet when this slot is focused.
     */
    protected TextSlot(InteractionManager editor, Frame frameParent, CodeFrame<? extends CodeElement> codeFrameParent, FrameContentRow row, CompletionCalculator completionCalculator, String stylePrefix, List<FrameCatalogue.Hint> hints)
    {
        this.editor = editor;
        this.completionCalculator = completionCalculator;
        this.frameParent = frameParent;
        this.codeFrameParent = codeFrameParent;
        if (frameParent != codeFrameParent)
            throw new IllegalArgumentException("frameParent and codeFrameParent are not same object");
        this.row = row;
        field = new SlotTextField(stylePrefix, row.getOverlay());
        editor.setupFocusableSlotComponent(this, field.getFocusableNode(), completionCalculator != null, row::getExtensions, hints);
        
        // Always disallow semi-colons:
        listeners.add((slot, oldValue, newValue, parent) -> {
            if (newValue.contains(";"))
                return false;
            else
                return true;
        });

        effectivelyFocusedProperty = field.focusedProperty().or(suggestionDisplayProperty.isNotNull());
    }

    /**
     * A property reflecting whether the field is "effectively focused"
     *
     * "Effectively focused" means that either the field has actual JavaFX GUI
     * focus, or code completion is showing for this slot, meaning it doesn't
     * have GUI focus, but for our purposes it is logically the focus owner
     * within the editor.
     */
    public ObservableBooleanValue effectivelyFocusedProperty()
    {
        return effectivelyFocusedProperty;
    }

    /**
     * The class dealing with the actual GUI component
     */
    public class SlotTextField extends AnnotatableTextField
    {
        // The piece of text from the beginning of the slot up until the beginning of
        // the current word we are completing on.  So for example if we are completing
        // "hello|" (pipe indicates cursor), lastBeforePrefix will be "".  If we complete
        // "he.said.hello|", lastBeforePrefix will be "he.said."  See getStartOfCurWord for more
        // info on detecting the current word.
        private String lastBeforePrefix;
        // Value when we gained focus, used for recording local undo
        private String valueOnGain;

        /**
         * Constructor.
         * @param stylePrefix  The prefix to use for CSS style classes
         * @param overlay The overlay on which to draw errors, underlines, etc
         */
        private SlotTextField(String stylePrefix, ErrorUnderlineCanvas overlay)
        {
            super(overlay);
            addStyleClasses("text-slot", stylePrefix + "text-slot");
            prefWidthProperty().set(10);

            SuggestionListListener suggestionListener = new SuggestionListListener() {
                @Override
                @OnThread(Tag.FXPlatform)
                public void suggestionListChoiceClicked(SuggestionList suggestionList, int highlighted)
                {
                    executeSuggestion(suggestionList, highlighted);
                    row.focusRight(TextSlot.this);
                }

                @Override
                @OnThread(Tag.FXPlatform)
                public Response suggestionListKeyTyped(SuggestionList suggestionList, KeyEvent event, int highlighted)
                {
                    // Space completes single selections and moves to next slot:
                    if (event.getCharacter().equals(" ") && completeIfPossible(suggestionList, highlighted))
                    {
                        row.focusRight(TextSlot.this);
                        return Response.DISMISS;
                    }
                    else if (!event.getCharacter().equals("\b"))
                        injectEvent(event);
                    return Response.CONTINUE;
                }

                @OnThread(Tag.FXPlatform)
                private boolean completeIfPossible(SuggestionList suggestionList, int highlighted)
                {
                    // Pick a value if one was available to complete:
                    if (highlighted != -1)
                    {
                        return executeSuggestion(suggestionList, highlighted);
                    }
                    else if (suggestionDisplayProperty.get().eligibleCount() == 1  && getText().length() > 0)
                    {
                        return executeSuggestion(suggestionList, suggestionDisplayProperty.get().getFirstEligible());
                    }
                    return false;
                }

                @Override
                @OnThread(Tag.FXPlatform)
                public Response suggestionListKeyPressed(SuggestionList suggestionList, KeyEvent event, int highlighted)
                {
                    switch (event.getCode())
                    {
                        case BACK_SPACE:
                            backspace();
                            return Response.CONTINUE;
                        case LEFT:
                            if (getCaretPosition() == 0)
                            {
                                row.focusLeft(TextSlot.this);
                                return Response.DISMISS;
                            }
                            else
                            {
                                positionCaret(getCaretPosition() - 1);
                                return Response.DISMISS;
                            }
                        case RIGHT:
                            // Pressing right inserts the string common to all current completions:
                            Optional<String> common = suggestionDisplayProperty.get().getLongestCommonPrefix();
                            if (common.isPresent())
                            {
                                boolean single = suggestionDisplayProperty.get().eligibleCount() == 1;
                                field.replaceText(getStartOfCurWord(), field.getCaretPosition(), common.get());
                                // If this was the only completion, we've inserted all of it, so dismiss:
                                if (single)
                                    return Response.DISMISS;
                            }
                            else
                            {
                                // They pressed right when no suggestions available; move to next slot.
                                row.focusRight(TextSlot.this);
                                return Response.DISMISS;
                            }
                            break;
                        case ENTER:
                            if (executeSuggestion(suggestionList, highlighted))
                            {
                                row.focusRight(TextSlot.this);
                                return Response.DISMISS;
                            }
                            break;
                        case ESCAPE:
                            setTransparent(false);
                            return Response.DISMISS;
                        case TAB:
                            // Make Tab/Shift-Tab still work when code completion is shown:
                            if (event.isShiftDown())
                                row.focusLeft(TextSlot.this);
                            else
                            {
                                row.focusRight(TextSlot.this);
                                completeIfPossible(suggestionList, highlighted);
                            }
                            return Response.DISMISS;
                    }
                    return Response.CONTINUE;
                }

                @Override
                public void hidden()
                {
                    suggestionDisplayProperty.set(null);
                    setFakeCaretShowing(false);
                }
            };
            
            //React to up/down arrows, and ENTER in the same way as tabs (move focus on)
            this.onKeyPressedProperty().set(event -> {
                    if (event.isShiftDown() && event.isControlDown() && event.getCharacter().length() > 0 && event.getCode() != KeyCode.CONTROL && event.getCode() != KeyCode.SHIFT)
                    {
                        row.notifyModifiedPress(event.getCode());
                        event.consume();
                        return;
                    }

                    //Which key?
                    switch (event.getCode())
                    {
                        case UP:
                            if (errorAndFixDisplay != null && errorAndFixDisplay.hasFixes() && errorAndFixDisplay.isShowing())
                            {
                                errorAndFixDisplay.up();
                            }
                            else
                            {
                                row.focusUp(TextSlot.this, false);
                            }
                            event.consume();
                            break;
                        case DOWN:
                            if (errorAndFixDisplay != null && errorAndFixDisplay.hasFixes() && errorAndFixDisplay.isShowing())
                            {
                                errorAndFixDisplay.down();
                            }
                            else
                            {
                                row.focusDown(TextSlot.this);
                            }
                            event.consume();
                            break;
                        case LEFT:
                            if (getSelection().getStart() == 0) {
                                row.focusLeft(TextSlot.this);
                                event.consume();
                            }
                            break;
                        case RIGHT:
                            if (getSelection().getEnd() == getLength())
                            {
                                row.focusRight(TextSlot.this);
                                event.consume();
                            }
                            break;
                        case ENTER:
                            if (errorAndFixDisplay != null)
                            {
                                errorAndFixDisplay.executeSelected();
                            }
                            else
                            {
                                row.focusEnter(TextSlot.this);
                            }
                            event.consume();
                            break;
                        case BACK_SPACE:
                            if (getCaretPosition() == 0 && !hasSelection()) {
                                for (SlotValueListener listener : listeners) {
                                    listener.backSpacePressedAtStart(TextSlot.this);
                                }
                                event.consume();
                            }
                            break;
                        case DELETE:
                            // If they are at the end and have no text selected:
                            if (getCaretPosition() == getLength() && anchorProperty().get() == getCaretPosition()) {
                                for (SlotValueListener listener : listeners) {
                                    listener.deletePressedAtEnd(TextSlot.this);
                                }
                                event.consume();
                            }
                            break;
                        case SPACE:
                            if (event.isControlDown())
                            {
                                showSuggestionDisplay(suggestionListener);
                                event.consume();
                            }
                            break;
                        case ESCAPE:
                            row.escape(TextSlot.this);
                            break;
                        default:
                            break;
                    }
            });
            
            
            
            //When focus leaves, if this is still blank, keep white. If has been filled in, blend in transparent with background.
            JavaFXUtil.addFocusListener(getFocusableNode(), newValue -> {
                    if (newValue)
                    {
                        valueOnGain = getText();
                        editor.beginRecordingState(TextSlot.this);
                        setTransparent(false);
                        //Stop the behaviour of selecting text when tabbing to a field:
                        //Need to wrap in runLater as selection happens after this method
                        Platform.runLater(this::deselect);
                        showErrorAtCaret(getCaretPosition());                        
                    }
                    else {
                        setTransparent(!getText().isEmpty() && suggestionDisplayProperty.get() == null);
                        editor.endRecordingState(TextSlot.this);
                        if (errorAndFixDisplay != null)
                        {
                            errorAndFixDisplay.hide();
                            errorAndFixDisplay = null;
                        }
                        if (!getText().equals(valueOnGain))
                        {
                            // Don't show new value as old:
                            recentValues.removeAll(getText());
                            // Remove any old value from middle, re-add at top:
                            recentValues.removeAll(valueOnGain);
                            recentValues.add(0, valueOnGain);
                            // Trim list to last three:
                            while (recentValues.size() > 3)
                                recentValues.remove(3);
                            valueChangedLostFocus(valueOnGain, getText());
                        }
                    }
            });
            
            //Text changes
            this.textProperty().addListener((observable, oldValue, newValue) -> {
                slotElement = null;

                //Unless still focused (or notionally focused because code completion is showing), go transparent
                if (!isFocused() && suggestionDisplayProperty.get() == null)
                {
                    if (newValue.length() > 0)
                    {
                        setTransparent(true);
                    }
                }

                boolean allowed = true;
                for (SlotValueListener listener : listeners)
                {
                    boolean listenerAllow = listener.valueChanged(TextSlot.this, oldValue, newValue, row);
                    allowed = allowed && listenerAllow;
                }

                if (!allowed)
                {
                    setText(oldValue);
                } else
                {
                    // After update has taken effect, update suggestions:
                    // It doesn't matter if we run this while loading because
                    // we won't be showing code completion:
                    JavaFXUtil.runPlatformLater(() -> {
                        if (suggestionDisplayProperty.get() != null)
                        {
                            String beforeNewPrefix = getText().substring(0, getStartOfCurWord());
                            if (!beforeNewPrefix.equals(lastBeforePrefix))
                            {
                                // The type we are completing on may have changed, need to refresh
                                // But if the change ends in a bracket, cancel code completion.  Only
                                // re-show if not bracket
                                if (!beforeNewPrefix.endsWith("("))
                                    showSuggestionDisplay(suggestionListener);
                            } else
                            {
                                // Same prefix, just update existing completion:
                                updateSuggestions(true);
                            }
                        }
                    });
                    editor.modifiedFrame(frameParent, false);
                }
            });
            
            // Autosizing the slot to fit contents:
            minWidthProperty().bind(new DoubleBinding() {
                { super.bind(textProperty());
                  super.bind(promptTextProperty());
                  super.bind(fontProperty()); }

                private String lastText;
                private double monospaceWidth;

                @Override
                protected double computeValue()
                {
                    String effectiveText = textProperty().get().length() > 0 ? textProperty().get() : promptTextProperty().get();
                    return Math.max(10, 5 + measureString(effectiveText, true));
                }
            });
            prefWidthProperty().bind(minWidthProperty());
            
            caretPositionProperty().addListener( (observable, oldValue, newVal) -> {
                    if (isFocused())
                        JavaFXUtil.runNowOrLater(() -> showErrorAtCaret(newVal.intValue()));
                    // TODO cancel code completion if we've moved away from it
            });
            
            // Need to allow parent's constructor to execute, and
            // need to be in the scene:
            JavaFXUtil.onceInScene(getNode(), () -> setContextMenu(AbstractOperation.MenuItems.makeContextMenu(getMenuItems(true))));
        }

        public final int getCaretPosition()
        {
            return caretPositionProperty().get();
        }    
        
        protected void setTransparent(boolean transparent)
        {
            field.setPseudoclass("bj-transparent", transparent);
        }
        
        public String getCurWord()
        {
            return getText().substring(getStartOfCurWord(), getCaretPosition());
        }

        @OnThread(Tag.FXPlatform)
        private void updateSuggestions(boolean initialState)
        {
            String prefix = getCurWord();
            suggestionDisplayProperty.get().calculateEligible(prefix, true, initialState);
            suggestionDisplayProperty.get().updateVisual(prefix);
            lastBeforePrefix = getText().substring(0, getStartOfCurWord());
        }

        @OnThread(Tag.FXPlatform)
        private void showSuggestionDisplay(SuggestionListListener listener)
        {
            if (completionCalculator == null)
                return; // Completion not possible in this slot

            suggestionXOffset.set(calculateCaretPosition(getStartOfCurWord()));
            FXPlatformConsumer<SuggestionList> handler = s ->
            {
                suggestionDisplayProperty.set(s);
                updateSuggestions(true);
                suggestionDisplayProperty.get().highlightFirstEligible();
                //Debug.time("!!! Showing");
                suggestionDisplayProperty.get().show(field.getNode(), new BoundingBox(suggestionXOffset.get(), 0, 0, field.heightProperty().get()));
                //Debug.time("!!! Shown");
                field.setFakeCaretShowing(true);
            };
            //Debug.time("!!! Requesting suggestion");
            // TODO we shouldn't need to regen whole code repeatedly if they only modify this slot:
            editor.afterRegenerateAndReparse(() -> {
                final int stringPos = field.getCaretPosition();
                //Debug.time("!!! Calculating suggestions");
                completionCalculator.withCalculatedSuggestionList(getSlotElement().getPosInSourceDoc(stringPos), codeFrameParent.getCode(), listener, suggList -> {
                    editor.recordCodeCompletionStarted(getSlotElement(), stringPos, getCurWord(), suggList.getRecordingId());
                    handler.accept(suggList);
                });

            });
        }


        // Make the parent method visible in this class:
        @Override
        protected double calculateCaretPosition(int beforeIndex)
        {
            return super.calculateCaretPosition(beforeIndex);
        }


        public TextOverlayPosition getOverlayLocation(int caretPos)
        {
            double x;
            if (caretPos == Integer.MAX_VALUE)
                x = widthProperty().get();
            else
            {
                caretPos = Math.max(0, Math.min(caretPos, getLength()));
                x = calculateCaretPosition(caretPos);
            }
            
            return TextOverlayPosition.nodeToOverlay(field.getNode(), x, 0, getBaseline(), field.heightProperty().get());
        }
    }

    public Region getNode()
    {
        return field.getNode();
    }

    public void addValueListener(SlotValueListener listener)
    {
        listeners.add(listener);
    }

    @Override
    public Frame getParentFrame()
    {
        return frameParent;
    }

    public final String getText()
    {
        return field.textProperty().get();
    }

    @Override
    public final SLOT_FRAGMENT getSlotElement()
    {
        if (slotElement == null)
            slotElement = createFragment(getText());
        return slotElement;
    }

    @Override
    public void focusAndPositionAtError(CodeError err)
    {
        requestFocus();
        field.positionCaret(err.getStartPosition());
    }


    public final void setPromptText(String arg0)
    {
        field.promptTextProperty().set(arg0);
    }

    public final void setText(String arg0)
    {
        field.textProperty().set(arg0);
    }
    
    public void setText(SLOT_FRAGMENT f)
    {
        field.textProperty().set(f.getContent());
        f.registerSlot(this);
    }

    public final ReadOnlyStringProperty textProperty()
    {
        return field.textProperty();
    }

    @Override
    public void requestFocus(Focus on)
    {
        field.requestFocus();
        if (null != on) switch (on) {
            case LEFT:
                field.positionCaret(0);
                break;
            case RIGHT:
                field.positionCaret(field.getLength());
                break;
            case SELECT_ALL:
                field.selectAll();
                break;
            default:
        }
    }

    @OnThread(Tag.FXPlatform)
    @Override
    public void addError(CodeError err)
    {
        allErrors.add(err);
        err.bindFresh(getFreshExtra(err).or(getParentFrame().freshProperty()), editor);
        recalculateShownErrors();
    }

    protected BooleanExpression getFreshExtra(CodeError err)
    {
        return new ReadOnlyBooleanWrapper(false);
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public void flagErrorsAsOld()
    {
        allErrors.forEach(CodeError::flagAsOld);
    }
    
    @Override
    @OnThread(Tag.FXPlatform)
    public void removeOldErrors()
    {
        allErrors.removeIf(CodeError::isFlaggedAsOld);
        recalculateShownErrors();
    }

    @OnThread(Tag.FXPlatform)
    private void recalculateShownErrors()
    {
        shownErrors.clear();
        
        // We need to find all non-overlapping errors, preferring our own errors
        // to those of javac, and then preferring shorter errors (as probably being more specific)
        List<CodeError> sortedErrors = allErrors.stream()
            .sorted((a, b) -> CodeError.compareErrors(a, b)).collect(Collectors.toList());
        
        for (CodeError e : sortedErrors)
        {
            // Add the error if it doesn't overlap:
            if (shownErrors.stream().allMatch(shown -> !shown.overlaps(e)))
            {
                shownErrors.add(e);
                e.setShowingIndicator(true);
            }
            else
            {
                e.setShowingIndicator(false);
            }
        }
        
        field.clearErrorMarkers(this);
        shownErrors.forEach(e -> field.drawErrorMarker(this, e.getStartPosition(), e.getEndPosition(), e.isJavaPos(), b -> showErrorHover(b ? e : null), e.visibleProperty()));
        
        if (field.isFocused())
            showErrorAtCaret(field.getCaretPosition());
    }

    @OnThread(Tag.FXPlatform)
    private void showErrorHover(CodeError error)
    {
        if (errorAndFixDisplay != null)
        {
            if (error != null && errorAndFixDisplay.getError().equals(error)){
                hoverErrorCurrentlyShown = error; //update current error
                return; // Already showing
            }
            final int caretPosition = field.getCaretPosition();
            Optional<CodeError> errorAtCaret = shownErrors.stream()
                    .filter(e -> e.getStartPosition() <= caretPosition && caretPosition <= e.getEndPosition())
                    .findFirst();
            // If we are turning off the hover error, but the caret is in the error,
            // we do not stop showing it.
            if (error == null && field.isFocused() && errorAtCaret.isPresent() && errorAtCaret.get().equals(errorAndFixDisplay.getError())){
                hoverErrorCurrentlyShown = null; //update current error
                return;
            }
            errorAndFixDisplay.hide();
            errorAndFixDisplay = null;
        }
        
        if (error != null && error.visibleProperty().get())
        {
            hoverErrorCurrentlyShown = error; //update current error
            errorAndFixDisplay = new ErrorAndFixDisplay(editor, error, this);
            errorAndFixDisplay.showBelow(field.getNode(), Duration.ZERO);
        }
    }

    @OnThread(Tag.FXPlatform)
    private void showErrorAtCaret(int caretPosition)
    {   
        // Note: we do want <= and <= here, so that the explanation shows
        // if the caret is at either end of the error (visually, if the caret is touching
        // the red underline from either side)
        Optional<CodeError> errorAtCaret = shownErrors.stream()
                .filter(e -> e.getStartPosition() <= caretPosition && caretPosition <= e.getEndPosition())
                .findFirst();
        
        if (errorAtCaret.isPresent() && errorAndFixDisplay != null && errorAndFixDisplay.getError().equals(errorAtCaret.get()))
        {
            // Already displaying that error; fine:
            return;
        }
        // In all other cases, hide the current display:
        if (errorAndFixDisplay != null)
        {
            errorAndFixDisplay.hide();
            errorAndFixDisplay = null;
        }
        
        // If there is now a (new) error to show, do so:
        if (errorAtCaret.isPresent() && errorAtCaret.get().visibleProperty().get())
        {
            errorAndFixDisplay = new ErrorAndFixDisplay(editor, errorAtCaret.get(), this);
            errorAndFixDisplay.showBelow(field.getNode());
        }
    }

    // Gets index of start of current word.  0 means start of String.
    // In outer class to allow overriding
    public int getStartOfCurWord()
    {
        // It seems that caret position can report beyond the length of the text, so start at end in that case:
        for (int i = Math.min(field.getCaretPosition(), getText().length()) - 1; i >= 0; i--)
        {
            if (!Character.isJavaIdentifierPart(getText().charAt(i)))
            {
                return i + 1;
            }
        }
        return 0;
    }
    
    @Override
    public void cleanup()
    {
        if (editor.getCodeOverlayPane() != null)
        {
            if (errorAndFixDisplay != null)
            {
                final ErrorAndFixDisplay errorAndFixDisplayToHide = this.errorAndFixDisplay;
                JavaFXUtil.runNowOrLater(() -> errorAndFixDisplayToHide.hide());
                this.errorAndFixDisplay = null;
            }
        }
        JavaFXUtil.runNowOrLater(() -> field.clearErrorMarkers(this));
    }

    public void replace(int startPosInSlot, int endPosInSlot, String replacement)
    {
        String before = getText().substring(0, startPosInSlot);
        String after = getText().substring(endPosInSlot);
        setText(before + replacement + after);
        field.positionCaret(before.length() + replacement.length());
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public void fixedError(CodeError err)
    {
        allErrors.remove(err);
        recalculateShownErrors();
    }

    // Returns true if should be dismissed
    @OnThread(Tag.FXPlatform)
    private boolean executeSuggestion(SuggestionList suggestionList, int highlighted)
    {
        final int position = getStartOfCurWord();
        String word = field.getCurWord();
        final boolean success = field.executeCompletion(completionCalculator, highlighted, position);
        if (success)
        {
            editor.recordCodeCompletionEnded(getSlotElement(), position, word, getText(), suggestionList.getRecordingId());
        }
        return success;
    }

    public boolean isEmpty()
    {
        return field.textProperty().get().isEmpty();
    }

    public void addFocusListener(Frame frame)
    {
        field.focusedProperty().addListener( (observable, oldValue, newValue) -> {
                if (!newValue) {
                   frame.checkForEmptySlot();
                }
        });
    }

    @Override
    public boolean isFocused()
    {
        return field.isFocused();
    }

    @Override
    public int getFocusInfo()
    {
        return field.getCaretPosition();
    }

    @Override
    public Node recallFocus(int info)
    {
        requestFocus(Focus.LEFT);
        field.positionCaret(info);
        return field.getNode();
    }

    @OnThread(Tag.FXPlatform)
    public Stream<CodeError> getCurrentErrors()
    {
        return shownErrors.stream();
    }

    @OnThread(Tag.FXPlatform)
    public void addUnderline(Underline u)
    {
        underlines.add(u);
        drawUnderlines();
    }

    @OnThread(Tag.FXPlatform)
    public void removeAllUnderlines()
    {
        underlines.clear();
        drawUnderlines();
    }

    @OnThread(Tag.FXPlatform)
    private void drawUnderlines()
    {
        field.clearUnderlines();
        underlines.forEach(u -> field.drawUnderline(this, u.getStartPosition(), u.getEndPosition(), u.getOnClick()));
    }

    @Override
    public void saved()
    {
        // Nothing to do
    }

    @Override
    public ObservableList<Node> getComponents()
    {
        return FXCollections.observableArrayList(field.getNode());
    }

    @Override
    public TextOverlayPosition getOverlayLocation(int caretPos, boolean javaPos)
    {
        return field.getOverlayLocation(caretPos);
    }

    public abstract List<? extends PossibleLink> findLinks();
    
    public void lostFocus()
    {
        // No extra work to do; losing focus is enough to deselect

        // Need to set transparent state, in case user dismissed code completion by clicking on another field:
        field.setTransparent(!getText().isEmpty());
    }
    
    protected abstract SLOT_FRAGMENT createFragment(String content);
    
    /**
     * Called when the slot has lost focus, and the value has changed since focus was gained.
     * 
     * Allows us to perform actions like pop-up prompts or renaming the compilation unit. 
     */
    @OnThread(Tag.FXPlatform)
    public abstract void valueChangedLostFocus(String oldValue, String newValue);

    @Override
    public void setView(View oldView, View newView, SharedTransition animate)
    {
        // If Java preview or bird's eye, disallow editing and focusing:
        field.editableProperty().set(newView == View.NORMAL);
        field.disableProperty().set(newView != View.NORMAL);

        if (newView == Frame.View.JAVA_PREVIEW)
        {
            animate.addOnStopped(() -> {
                JavaFXUtil.setPseudoclass("bj-java-preview", newView == Frame.View.JAVA_PREVIEW, field.getFocusableNode());
            });
        }
        else
        {
            JavaFXUtil.setPseudoclass("bj-java-preview", newView == Frame.View.JAVA_PREVIEW, field.getFocusableNode());
        }
    }
        
    protected Map<TopLevelMenu, AbstractOperation.MenuItems> getExtraContextMenuItems()
    {
        return Collections.emptyMap();
    }

    @Override
    public final Map<TopLevelMenu, AbstractOperation.MenuItems> getMenuItems(boolean contextMenu) {
        Map<TopLevelMenu, AbstractOperation.MenuItems> itemMap = new HashMap<>(getExtraContextMenuItems());
        final ObservableList<AbstractOperation.SortedMenuItem> menuItems = FXCollections.observableArrayList();
        if (contextMenu)
        {
            menuItems.add(getRecentValuesMenu());
        }
        final MenuItem cutItem = JavaFXUtil.makeMenuItem(Config.getString("frame.slot.cut"), field::cut, new KeyCodeCombination(KeyCode.X, KeyCodeCombination.SHORTCUT_DOWN));
        final MenuItem copyItem = JavaFXUtil.makeMenuItem(Config.getString("frame.slot.copy"), field::copy, new KeyCodeCombination(KeyCode.C, KeyCodeCombination.SHORTCUT_DOWN));

        final MenuItem pasteItem = JavaFXUtil.makeMenuItem(Config.getString("frame.slot.paste"), field::paste,
            // Work around a behaviour on Mac where pressing Cmd-V will paste
            // in TextField even without the existing of the accelerator.
            // Thus, the existing of the accelerator will make it paste twice.
            Config.isMacOS() ? null : new KeyCodeCombination(KeyCode.V, KeyCodeCombination.SHORTCUT_DOWN));

        menuItems.addAll(
            AbstractOperation.MenuItemOrder.CUT.item(cutItem),
            AbstractOperation.MenuItemOrder.COPY.item(copyItem),
            AbstractOperation.MenuItemOrder.PASTE.item(pasteItem));
        itemMap.put(TopLevelMenu.EDIT, AbstractOperation.MenuItems.concat(
                new AbstractOperation.MenuItems(menuItems) {

            @Override
            @OnThread(Tag.FXPlatform)
            public void onShowing() {
                if (hoverErrorCurrentlyShown != null ){
                    errorAndFixDisplay.hide();
                }
                // Cut & copy are available if there is a selection:
                boolean selectionPresent = field.hasSelection();
                cutItem.setDisable(!selectionPresent);
                copyItem.setDisable(!selectionPresent);
                // Paste is available if there is plain text data on the clipboard:
                pasteItem.setDisable(!Clipboard.getSystemClipboard().hasString());
            }

        },
                itemMap.get(TopLevelMenu.EDIT)
        ));
        return itemMap;
    }

    private AbstractOperation.SortedMenuItem getRecentValuesMenu()
    {
        final Menu recent = new Menu(Config.getString("frame.slot.recent"));
        recent.setDisable(true);
        recentValues.addListener((ListChangeListener)c -> {
            recent.getItems().clear();
            if (recentValues.isEmpty())
            {
                recent.setDisable(true);
            }
            else
            {
                recent.setDisable(false);
                recentValues.forEach(v -> {
                    MenuItem item = new MenuItem(v);
                    item.setOnAction(e -> {
                        editor.recordEdits(StrideEditReason.FLUSH);
                        setText(v);
                        editor.recordEdits(StrideEditReason.UNDO_LOCAL);
                    });
                    recent.getItems().add(item);
                });
            }
        });
        return AbstractOperation.MenuItemOrder.RECENT_VALUES.item(recent);
    }

    @Override
    public boolean isAlmostBlank()
    {
        return getText().isEmpty();
    }

    @Override
    public boolean isEditable()
    {
        // JavaFX docs warn to use disabledProperty to get effective state.  But actually
        // I think we're fine to query disable; we just want to know if setEditable(true/false)
        // was previously called, for which this works fine:
        return !field.disableProperty().get();
    }

    @Override
    public void setEditable(boolean editable)
    {
        field.disableProperty().set(!editable);
    }

    @Override
    public Stream<Node> makeDisplayClone(InteractionManager editor)
    {
        TextField f = new TextField();
        f.textProperty().bind(field.textProperty());
        f.prefWidthProperty().bind(field.prefWidthProperty());
        JavaFXUtil.bindList(f.getStyleClass(), field.getStyleClass());
        JavaFXUtil.bindPseudoclasses(f, field.getPseudoClassStates());
        JavaFXUtil.setPseudoclass("bj-pinned", true, f);
        f.styleProperty().bind(field.styleProperty().concat(editor.getFontCSS()));
        return Stream.of(f);
    }

    @Override
    public int calculateEffort()
    {
        // We put a ceiling of 4 keypresses, approximating code completion:
        return Math.min(4, getText().length());
    }
}
