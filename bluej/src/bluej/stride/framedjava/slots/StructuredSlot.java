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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import bluej.collect.StrideEditReason;
import bluej.editor.stride.FrameCatalogue;

import bluej.stride.framedjava.ast.StructuredSlotFragment;
import bluej.stride.framedjava.ast.links.PossibleLink;
import bluej.stride.framedjava.slots.InfixStructured.RangeType;
import bluej.stride.generic.ExtensionDescription;
import bluej.utility.javafx.*;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.BoundingBox;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.Clipboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import bluej.Config;
import bluej.editor.stride.CodeOverlayPane;
import bluej.stride.framedjava.ast.JavaFragment.PosInSourceDoc;
import bluej.stride.framedjava.errors.CodeError;
import bluej.stride.framedjava.errors.ErrorAndFixDisplay;
import bluej.stride.framedjava.errors.ErrorAndFixDisplay.ErrorFixListener;
import bluej.stride.framedjava.frames.CodeFrame;
import bluej.stride.framedjava.slots.TextOverlayPosition.Line;
import bluej.stride.generic.Frame;
import bluej.stride.generic.Frame.View;
import bluej.stride.generic.FrameContentRow;
import bluej.stride.generic.InteractionManager;
import bluej.stride.generic.InteractionManager.FileCompletion;
import bluej.stride.slots.EditableSlot;
import bluej.stride.slots.Focus;
import bluej.stride.slots.FocusParent;
import bluej.stride.slots.HeaderItem;
import bluej.stride.slots.LinkedIdentifier;
import bluej.stride.slots.SlotLabel;
import bluej.editor.fixes.SuggestionList;
import bluej.editor.fixes.SuggestionList.SuggestionDetails;
import bluej.editor.fixes.SuggestionList.SuggestionDetailsWithCustomDoc;
import bluej.editor.fixes.SuggestionList.SuggestionListListener;
import bluej.utility.Utility;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * The StructuredSlot class is used where a single expression is wanted as a slot.  For example,
 * in the condition of an if statement, the LHS and the RHS of an assignment, and so on.
 * 
 * StructuredSlot implements the EditableSlot interface and also handles any behaviour
 * that should be common to the whole slot.  For example, error display is handled by StructuredSlot
 * because we only want to display at most one error per expression at any time.  Similarly, code
 * completion is handled here because we only want one code completion showing, and we may want the
 * code completion to keep showing even as the user navigates (and moves focus) among the contents of
 * the expression.
 *
 * All the actual graphical components and logic are not directly implemented by this class, but are
 * handled by InfixStructured (StructuredSlot always has one, via the topLevel field), so you should
 * see that class for more details.  StructuredSlot has some role in bridging the gap between all the logic
 * bundled in InfixStructured and the "outside world", e.g. when InfixStructured is changed, it calls
 * StructuredSlot.modified to notify the editor, etc.
 *
 * This class is abstract, as a few details must be specified by some slim, more specific subclasses
 */
public abstract class StructuredSlot<SLOT_FRAGMENT extends StructuredSlotFragment, INFIX extends InfixStructured<?, INFIX>, COMPLETION_CALCULATOR extends StructuredCompletionCalculator> implements EditableSlot, ErrorFixListener, SuggestionListListener
{
    // The overlay on which to draw errors, underlines, etc
    private final ErrorUnderlineCanvas overlay;
    // Same item, but stored twice due to types:
    private final Frame parentFrame;
    protected final CodeFrame<?> parentCodeFrame;
    // The row containing us:
    private final FrameContentRow row;

    // All errors associated with the slot
    @OnThread(Tag.FXPlatform)
    private final List<CodeError> allErrors = new ArrayList<>();
    // Shown errors associated with the slot.  If two errors overlap, they will both appear
    // in allErrors, but only one will be shown and appear in shownErrors.
    @OnThread(Tag.FXPlatform)
    private final List<CodeError> shownErrors = new ArrayList<>();
    // The display showing errors and quick fixes (null if not showing)
    private ErrorAndFixDisplay errorAndFixDisplay;
    // The error currently being shown due to mouse hover:
    private CodeError hoverErrorCurrentlyShown;
    // The code generated by this slot (depends on subclass):
    private SLOT_FRAGMENT slotElement;

    // The top level infix expression which makes up the whole expression slot:
    protected final INFIX topLevel;
    // The editor in which we lie:
    protected final InteractionManager editor;

    // Any underlines being shown (as link sources in the slot)
    private final List<Underline> underlines = new ArrayList<>();

    // The calculator which will be used to calculate completions.  Should not be null.
    protected final COMPLETION_CALCULATOR completionCalculator;
    // The suggestion list showing the auto complete (null if none)
    private SuggestionList suggestionDisplay;
    // The node (i.e. text field) which the suggestion list is shown above/below
    private Region suggestionNode;
    // Keeps track of whether we are currently code completing, for the purposes of
    // deciding whether to generate code for optional slots.  This is not necessarily the same
    // as suggestionDisplayProperty.get() != null, because we set this flag before
    // we show the GUI
    private boolean currentlyCompleting = false;
    // Some fields for file completions when we are completing string literals:
    private List<FileCompletion> fileCompletions;
    private Map<KeyCode, Runnable> fileCompletionShortcuts;

    // Contains the fully-qualified name of a target type for this expression, e.g.
    // "boolean", "java.lang.String".  Matching completions will be shown at the top
    // of the suggestion list.
    private StringExpression targetType;
    // Whether the slot has been modified since the last time slotElement was generated.
    private boolean beenModified = false;
    // Keeps a mirror of the text content of the complete content of the slot, mirrored
    // from topLevel, and used to call listeners who want to know about changes in the slot's content.
    protected final StringProperty textMirror = new SimpleStringProperty("");
    // Because the selection in an expression slot can span multiple text fields, we must
    // draw it on ourselves as an overlay.  This list keeps track of all the positions in
    // the selection, i.e. the beginning before the first character, after the first character,
    // after the second, third, etc and after the final one.  These positions may be spread
    // across multiple graphical lines if the slot has wrapped in the flow pane.
    private List<TextOverlayPosition> selectionDrawPositions;
    // A list of actions to take when this slot loses focus:
    private final List<FXRunnable> lostFocusActions = new ArrayList<>();
    // The position at which code completion is currently taking place.  We can't just
    // ask for the current position, because that is not valid while the slot is not focused,
    // and when the code completion window is showing and focused, the slot will not be focused.
    private CaretPos suggestionLocation;
    // The field in which code completion is currently taking place
    private StructuredSlotField suggestionField;
    // Whether a fake caret is showing.  Because code completion involves focusing the window
    // the cursor disappears.  To still show the user where they are completing in the slot,
    // we draw a fake cursor
    private final SimpleBooleanProperty fakeCaretShowing = new SimpleBooleanProperty(false);
    // The hints that will be shown when fields of this expression are focused
    private final List<FrameCatalogue.Hint> hints;
    // Recent values of the whole slot, for local undo
    private final ObservableList<String> recentValues = FXCollections.observableArrayList();
    // The most recent caret position, used for recording undo location just after focus was lost
    private CaretPos mostRecentPos;
    // The content of the slot when focus was gained, used for storing local undo values
    private String valueOnGain;
    // Whether the slot is editable  (slots are made uneditable when their enclosing frame gets disabled)
    private boolean editable = true;
    // Keep track of whether we are focused.  We are focused if any of the fields anywhere
    // within the slot is focused, which is awkward to query repeatedly, so we also keep
    // track here at the very top level:
    private final BooleanProperty focusedProperty = new SimpleBooleanProperty(false);
    // We must keep a reference to this to avoid problems with GC and weak listeners:
    private final BooleanBinding effectivelyFocusedProperty;
    private final List<ModificationToken> modificationTokens = new ArrayList<>();
    protected final List<FXRunnable> afterModify = new ArrayList<>();

    public StructuredSlot(InteractionManager editor,
                          Frame parentFrame, CodeFrame<?> parentCodeFrame, FrameContentRow row, String stylePrefix, COMPLETION_CALCULATOR completionCalculator, List<FrameCatalogue.Hint> hints)
    {
        this.editor = editor;
        this.parentFrame = parentFrame;
        this.parentCodeFrame = parentCodeFrame;
        this.row = row;
        this.completionCalculator = completionCalculator;
        this.hints = hints;
        // Since there's no content at this stage, we don't have to worry
        // about updating textMirror or running after actions.  In fact,
        // they may be problematic at this stage because topLevel isn't
        // even initialized until this returns:
        topLevel = newInfix(editor, new ModificationToken());

        effectivelyFocusedProperty = focusedProperty.or(fakeCaretShowing);
        
        JavaFXUtil.addChangeListener(textMirror, t -> {
            if (!editor.isLoading())
            {
                modified();
            }
            else
            {
                parentFrame.trackBlank();
            }
        });

        this.overlay = row.getOverlay();
        overlay.addExtraRedraw(gc -> {
            gc.save();
            if (selectionDrawPositions != null && !selectionDrawPositions.isEmpty())
            {
                gc.setFill(editor.getHighlightColor());
                
                for (Line l : TextOverlayPosition.groupIntoLines(selectionDrawPositions)) {
                    l.transform(overlay::sceneToLocal);
                    gc.fillRect(l.startX + 1.0 /* fudge factor */, l.topY, l.endX - l.startX, l.bottomY - l.topY);
                }
            }
            
            if (fakeCaretShowing.get())
            {
                gc.setStroke(Color.BLACK);
                double x = overlay.sceneToLocal(topLevel.calculateOverlayPos(suggestionLocation).getSceneX(), 0).getX();
                gc.strokeLine(x, 0, x, suggestionField.heightProperty().get());
            }
            gc.restore();
        });
        
        JavaFXUtil.addChangeListener(fakeCaretShowing, b -> JavaFXUtil.runNowOrLater(() -> overlay.redraw()));
    }
    
    
    
 // Errors:

    @OnThread(Tag.FXPlatform)
    @Override
    public void flagErrorsAsOld()
    {
        allErrors.forEach(CodeError::flagAsOld);
    }

    @OnThread(Tag.FXPlatform)
    @Override
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

        sortedErrors.forEach(e -> {
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
        });
        
        clearErrorMarkers();
        shownErrors.forEach(e -> drawErrorMarker(e.getStartPosition(), e.getEndPosition(), e.isJavaPos(), b -> showErrorHover(b ? e : null), e.visibleProperty()));
        
        CaretPos curPos = topLevel.getCurrentPos();
        if (curPos != null)
            JavaFXUtil.runNowOrLater(() -> showErrorAtCaret(curPos));
    }

    @OnThread(Tag.FXPlatform)
    private void showErrorHover(CodeError error)
    {
        if (errorAndFixDisplay != null)
        {
            if (error != null && errorAndFixDisplay.getError().equals(error)){
                hoverErrorCurrentlyShown = error; //update current error
                return; // Already showing that error
            }
            
            CaretPos caretPos = topLevel.getCurrentPos();
            if (caretPos != null)
            {
                final int caretPosition = topLevel.caretPosToStringPos(caretPos, true);
                Optional<CodeError> errorAtCaret = shownErrors.stream()
                        .filter(e -> e.getStartPosition() <= caretPosition && caretPosition <= e.getEndPosition())
                        .findFirst();
                // If we are turning off the hover error, but the caret is in the error,
                // we do not stop showing it.
                if (error == null && errorAtCaret.isPresent() && errorAtCaret.get().equals(errorAndFixDisplay.getError())){
                    hoverErrorCurrentlyShown = null; //update current error
                    return;
                }
            }
            
            errorAndFixDisplay.hide();
            errorAndFixDisplay = null;
        }
        
        if (error != null && error.visibleProperty().get())
        {
            hoverErrorCurrentlyShown = error; //update current error
            errorAndFixDisplay = new ErrorAndFixDisplay(editor, error, this);
            errorAndFixDisplay.showBelow((Region)error.getRelevantNode(), Duration.ZERO);
        }
    }

    @OnThread(Tag.FXPlatform)
    private void showErrorAtCaret(CaretPos curPos)
    {
        if (curPos == null) {
            //Stop showing error:
            if (errorAndFixDisplay != null)
            {
                errorAndFixDisplay.hide();
                errorAndFixDisplay = null;
            }
            return;
        }
        
        int caretPosition = topLevel.caretPosToStringPos(curPos, true);
        // Note: we do want <= and <= here, so that the explanation shows
        // if the caret is at either end of the error (visually, if the caret is touching
        // the red underline from either side)
        Optional<CodeError> errorAtCaret = shownErrors.stream()
                .filter(e -> e.getStartPosition() <= caretPosition && caretPosition <= e.getEndPosition())
                .findFirst();
        
        if (errorAtCaret.isPresent() && errorAndFixDisplay != null && errorAndFixDisplay.getError().equals(errorAtCaret.get()))  {
            // Already displaying that error; fine:
            return;
        }
        // In all other cases, hide the current display:
        if (errorAndFixDisplay != null) {
            errorAndFixDisplay.hide();
            errorAndFixDisplay = null;
        }
        
        // If there is now a (new) error to show, do so:
        if (errorAtCaret.isPresent() && errorAtCaret.get().visibleProperty().get()) {
            errorAndFixDisplay = new ErrorAndFixDisplay(editor, errorAtCaret.get(), this);
            errorAndFixDisplay.showBelow(topLevel.getNodeForPos(curPos));
        }
    }

    @OnThread(Tag.FXPlatform)
    public void addError(CodeError err)
    {
        allErrors.add(err);
        err.bindFresh(getFreshExtra(err).or(getParentFrame().freshProperty()), editor);
        recalculateShownErrors();
    }

    @OnThread(Tag.FXPlatform)
    /**
     * Updates an error in the error list, required when errors are first added without being
     * fully ready : when there are several errors in a slot, not updating them may cause issues
     */
    public void updateError(CodeError err)
    {
        allErrors.removeIf(codeError -> codeError.getIdentifier()==err.getIdentifier());
        addError(err);
    }


    protected BooleanExpression getFreshExtra(CodeError err)
    {
        return new ReadOnlyBooleanWrapper(false);
    }
    
    @Override
    @OnThread(Tag.FXPlatform)
    public void fixedError(CodeError err)
    {
        allErrors.remove(err);
        recalculateShownErrors();
    }

    @OnThread(Tag.FXPlatform)
    public void drawErrorMarker(int startPos, int endPos, boolean javaPos, FXPlatformConsumer<Boolean> onHover, ObservableBooleanValue visible)
    {
        // If we are trying to highlight an empty slot, highlight whole width
        if ((startPos == 0 && endPos == 0) || getText().length() == 0)
            overlay.addErrorMarker(this, 0, Integer.MAX_VALUE, false, onHover, visible);
        else
            overlay.addErrorMarker(this, startPos, endPos, javaPos, onHover, visible);
    }


    protected abstract SLOT_FRAGMENT makeSlotFragment(String content, String javaCode);
    
    public SLOT_FRAGMENT getSlotElement()
    {
        if (slotElement == null || beenModified) {
            slotElement = makeSlotFragment(getText(), getJavaCode());
            beenModified = false;
        }
        return slotElement;
    }

    @OnThread(Tag.FXPlatform)
    public void clearErrorMarkers()
    {
        overlay.clearErrorMarkers(this);
    }

    public void setSimplePromptText(String t)
    {
        FXRunnable action = () -> {
            topLevel.withContent((fields, ops) -> {
                // If there's just one field:
                if (ops.isEmpty() && fields.size() == 1 && fields.get(0) instanceof StructuredSlotField)
                    ((StructuredSlotField)fields.get(0)).setPromptText(t);
                else if (fields.size() > 0)
                    ((StructuredSlotField)fields.get(0)).setPromptText("");
            });
        };
        // We only act after complete updates:
        afterModify.add(action);
        // And trigger now:
        action.run();
    }
    
    public void setMethodCallPromptText(String t)
    {
        FXRunnable action = () -> {
            topLevel.withContent((fields, ops) -> {
                // Can happen during blanking:
                if (ops.size() == 0 || fields.size() == 0)
                    return;

                // Scan for fields that are followed by brackets (with no operator inbetween),
                // and preceded by start or a dot:
                for (int i = 0; i < fields.size() - 1 /* Don't look at last field */; i++)
                {
                    if (fields.get(i) instanceof StructuredSlotField)
                    {
                        StructuredSlotField f = (StructuredSlotField)fields.get(i);
                        // Brackets after, with no op inbetween:
                        if (ops.get(i) == null && fields.get(i + 1) instanceof BracketedStructured)
                        {
                            // No op beforehand, or a dot:
                            if (i == 0 || (ops.get(i - 1) != null && ops.get(i - 1).get().equals(".")))
                            {
                                f.setPromptText(t);
                            }
                        }
                    }
                }
            });
        };
        // We only act after complete updates:
        afterModify.add(action);
        // And trigger now:
        action.run();
    }

    public void onTextPropertyChange(FXConsumer<String> listener)
    {
        JavaFXUtil.addChangeListener(textMirror, listener);
    }
    public void onTextPropertyChangeOld(FXBiConsumer<String, String> listener)
    {
        textMirror.addListener((a, oldVal, newVal) -> listener.accept(oldVal, newVal));
    }
    
    public String getText()
    {
        return topLevel.getCopyText(null, null);
    }

    @Override
    public boolean isFocused()
    {
        return topLevel.isFocused();
    }

    @Override
    public int getFocusInfo()
    {
        CaretPos pos = topLevel.getCurrentPos();
        if (pos == null)
            pos = mostRecentPos;

        if (pos == null)
            return 0;
        else
            return topLevel.caretPosToStringPos(pos, false);
    }

    @Override
    public Node recallFocus(int info)
    {
        return topLevel.positionCaret(topLevel.stringPosToCaretPos(info, false));
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public Stream<CodeError> getCurrentErrors()
    {
        return shownErrors.stream();
    }

    @OnThread(Tag.FXPlatform)
    public void setText(String text)
    {
        modificationPlatform(token -> {
            topLevel.blank(token);
            if (!"".equals(text))
            {
                topLevel.insert(topLevel.getFirstField(), 0, text);
            }
        });
    }

    @Override
    public void focusAndPositionAtError(CodeError err)
    {
        requestFocus();
        topLevel.positionCaret(javaPosToCaretPos(err.getStartPosition()));        
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public void addUnderline(Underline u)
    {
        underlines.add(u);
        drawUnderlines();
    }
    
    @Override
    @OnThread(Tag.FXPlatform)
    public void removeAllUnderlines()
    {
        underlines.clear();
        drawUnderlines();
    }

    @OnThread(Tag.FXPlatform)
    private void drawUnderlines()
    {
        overlay.clearUnderlines();
        underlines.forEach(u -> overlay.addUnderline(this, u.getStartPosition(), u.getEndPosition(), u.getOnClick()));
    }

    @Override
    public void cleanup()
    {
        if (suggestionDisplay != null)
            hideSuggestionDisplay();
        
        if (errorAndFixDisplay != null)
        {
            final ErrorAndFixDisplay errorAndFixDisplayToHide = this.errorAndFixDisplay;
            JavaFXUtil.runNowOrLater(() -> errorAndFixDisplayToHide.hide());
            this.errorAndFixDisplay = null;
        }
        JavaFXUtil.runNowOrLater(() -> {
            shownErrors.clear();
            clearErrorMarkers();
            overlay.clearUnderlines();
        });
    }

    @OnThread(Tag.FXPlatform)
    public void replace(int startPosInSlot, int endPosInSlot, boolean javaPos, String s)
    {
        if (javaPos)
        {
            // Convert back to normal pos:
            startPosInSlot = topLevel.caretPosToStringPos(javaPosToCaretPos(startPosInSlot), false);
            endPosInSlot = topLevel.caretPosToStringPos(javaPosToCaretPos(endPosInSlot), false);
        }

        String prev = getText();
        String updated = prev.substring(0, startPosInSlot) + s + prev.substring(endPosInSlot);
        setText(updated);
    }   
    
/*
    @Override
    public int getSlotElementPositionInLine()
    {
        return topLevel.getSlotElement().getPositionInLine();
    }
    
    @Override
    public boolean hasSlotElementPosition()
    {
        return topLevel.getSlotElement().positionRecorded();
    }
*/
    @Override
    public void requestFocus(Focus on)
    {
        topLevel.requestFocus();
        if (on == Focus.LEFT) {
            topLevel.home(null);
        }
        else if (on == Focus.RIGHT) {
            topLevel.end();
        }
        else if (on == Focus.SELECT_ALL) {
            topLevel.selectAll(null);
        }
    }

    public boolean isEmpty()
    {
        return topLevel.isEmpty();
    }

    protected void modified()
    {
        beenModified = true;
        editor.modifiedFrame(parentFrame, false);
        JavaFXUtil.runNowOrLater(() -> editor.afterRegenerateAndReparse(null));
    }
    
    // package-visible
    double sceneToOverlayX(double sceneX)
    {
        return overlay.sceneToLocal(sceneX, 0.0).getX();
    }
    
    // package-visible
    double sceneToOverlayY(double sceneY)
    {
        return overlay.sceneToLocal(0.0, sceneY).getY();
    }
    
    // package-visible
    void clearSelection(boolean invalidateErrors)
    {
        selectionDrawPositions = null;
        JavaFXUtil.runNowOrLater(() -> {
            if (invalidateErrors)
            {
                clearErrorMarkers();
            }
            overlay.redraw();
        });
    }
    
    // package-visible
    void drawSelection(List<TextOverlayPosition> positions)
    {
        selectionDrawPositions = positions;
        JavaFXUtil.runNowOrLater(overlay::redraw);
    }
    
    public void bindTargetType(StringExpression targetTypeBinding)
    {
        this.targetType = targetTypeBinding;
    }

    public void setTargetType(String targetType)
    {
        this.targetType = new SimpleStringProperty(targetType);
    }

    public static Label makeBracket(String content, boolean opening, InfixStructured parent)
    {
        Label l = new Label(content);
        JavaFXUtil.addStyleClass(l, "expression-bracket", opening ? "expression-bracket-opening" : "expression-bracket-closing");
        l.setOnMousePressed(e -> {
            if (parent != null)
                parent.moveTo(e.getSceneX(), e.getSceneY(), true);
            e.consume();
        });
        l.setOnMouseMoved(e -> { if (e.isShortcutDown()) parent.getSlot().getOverlay().hoverAtPos(-1); });
        l.setOnMouseReleased(MouseEvent::consume);
        l.setOnMouseClicked(MouseEvent::consume);
        l.setOnMouseDragged(MouseEvent::consume);
        l.setOnDragDetected(MouseEvent::consume);
        return l;
    }

    public static SlotLabel makeBracketSlot(String content, boolean opening, InfixStructured parent)
    {
        SlotLabel l = new SlotLabel(content);
        JavaFXUtil.addStyleClass(l, "expression-bracket", opening ? "expression-bracket-opening" : "expression-bracket-closing");
        l.setOnMousePressed(e -> {
            if (parent != null)
                parent.moveTo(e.getSceneX(), e.getSceneY(), true);
            e.consume();
        });
        l.setOnMouseReleased(MouseEvent::consume);
        l.setOnMouseClicked(MouseEvent::consume);
        l.setOnMouseDragged(MouseEvent::consume);
        l.setOnDragDetected(MouseEvent::consume);
        return l;
    }
    
    //package-visible
    void hideSuggestionDisplay()
    {
        if (suggestionDisplay != null) {
            //suggestionDisplay.hide();
            suggestionDisplay = null;
        }
        fileCompletions = null;
    }

    /**
     * Shows code completion for this slot (intended for use in new, blank slots)
     */
    @OnThread(Tag.FXPlatform)
    public void showSuggestion()
    {
        showSuggestionDisplay(topLevel.getFirstField(), 0, false);
    }

    // package-visible
    @OnThread(Tag.FXPlatform)
    void showSuggestionDisplay(StructuredSlotField field, int caretPosition, boolean stringLiteral)
    {
        if (suggestionDisplay != null) {
            hideSuggestionDisplay();
        }
        
        suggestionField = field;
        suggestionNode = field.getComponents().get(0);
        
        FXPlatformConsumer<SuggestionList> withSuggList = suggList -> {
            suggestionDisplay = suggList;
            updateSuggestions(true);
            suggestionDisplay.highlightFirstEligible();
            suggestionDisplay.show(suggestionNode, new BoundingBox(0, 0, 0, field.heightProperty().get()));
            fakeCaretShowing.set(true);
        };
        
        suggestionLocation = topLevel.getCurrentPos();
        
        if (stringLiteral) {
            // They are just inside a string; complete image file names:
            fileCompletions = editor.getAvailableFilenames();
            // Have to have this function on a separate line to work around Eclipse bug (sigh):
            Function<FileCompletion, SuggestionDetails> func = f -> new SuggestionDetailsWithCustomDoc(f.getFile().getName(), null, f.getType(), SuggestionList.SuggestionShown.COMMON, () -> makeFileCompletionPreview(f)) {
                @Override
                public boolean hasDocs()
                {
                    // Bit of a hack; Greenfoot sounds/images have doc, but
                    // BlueJ CSS files have no doc
                    return Config.isGreenfoot();
                }
            };
            withSuggList.accept(new SuggestionList(editor, Utility.mapList(fileCompletions, func), null, SuggestionList.SuggestionShown.RARE, null, StructuredSlot.this));
            
        }
        else {
            currentlyCompleting = true;
            // TODO we shouldn't need to regen whole code repeatedly if they only modify this slot:
            editor.afterRegenerateAndReparse(() -> {
                final int stringPos = topLevel.caretPosToStringPos(topLevel.getCurrentPos(), true);
                PosInSourceDoc posInFile = getSlotElement().getPosInSourceDoc(stringPos);
                completionCalculator.withCalculatedSuggestionList(posInFile, this.asExpressionSlot(), parentCodeFrame.getCode(), StructuredSlot.this, (targetType == null /* || not at start getStartOfCurWord() != 0 */) ? null : targetType.get(), field == topLevel.getFirstField(), suggList -> {
                    editor.recordCodeCompletionStarted(getSlotElement(), stringPos, field.getText().substring(0, caretPosition), suggList.getRecordingId());
                    withSuggList.accept(suggList);
                });

            });
        }
        
    }

    private Pane makeFileCompletionPreview(FileCompletion fc)
    {
        Pane javadocDisplay = new BorderPane(fc.getPreview(300, 250));
        JavaFXUtil.addStyleClass(javadocDisplay, "suggestion-file-preview");
        CodeOverlayPane.setDropShadow(javadocDisplay);
        
        fileCompletionShortcuts = fc.getShortcuts();
        return javadocDisplay;
    }

    private String getCurSuggestionWord()
    {
        if (suggestionLocation == null)
            return null;
        else
            return topLevel.getCopyText(replaceLastWithZero(suggestionLocation), suggestionLocation);
    }
    
    private static CaretPos replaceLastWithZero(CaretPos p)
    {
        if (p.subPos == null)
            return new CaretPos(0, null);
        else
            return new CaretPos(p.index, replaceLastWithZero(p.subPos));
    }

    @OnThread(Tag.FXPlatform)
    private void updateSuggestions(boolean initialState)
    {
        if (suggestionDisplay != null)
        {
            String prefix = getCurSuggestionWord();
            if (prefix == null)
            {
                // Lost focus:
                hideSuggestionDisplay();
            }
            else
            {
                suggestionDisplay.calculateEligible(prefix, true, initialState);
                suggestionDisplay.updateVisual(prefix);
            }
            //lastBeforePrefix = getText().substring(0, getStartOfCurWord());
        }
    }

    @OnThread(Tag.FXPlatform)
    private void executeSuggestion(int selected, ModificationToken token, int codeCompletionId)
    {
        if (selected == -1)
            return;

        String name;
        List<String> params;
        char opening;
        if (fileCompletions != null)
        {
            FileCompletion fc = fileCompletions.get(selected);
            name = fc.getFile().getName();
            params = null;
            opening = '\0';
        }
        else
        {
            name = completionCalculator.getName(selected);
            params = completionCalculator.getParams(selected);
            opening = completionCalculator.getOpening(selected);
        }
        topLevel.insertSuggestion(suggestionLocation, name, opening, params, token);
        modified();
        String completion = name + (params == null ? "" : "(" + params.stream().collect(Collectors.joining(",")) + ")");
        editor.recordCodeCompletionEnded(getSlotElement(), topLevel.caretPosToStringPos(suggestionLocation, false), getCurSuggestionWord(), completion, codeCompletionId);
    }
    
    // Package-visible
    void up()
    {
        if (errorAndFixDisplay != null && errorAndFixDisplay.hasFixes() && errorAndFixDisplay.isShowing())
        {
            errorAndFixDisplay.up();
        }
        else
        {
            
            // See if there is content above us within the expression:
            List<TextOverlayPosition> overlayPositions = topLevel.getAllStartEndPositionsBetween(null, null).collect(Collectors.toList());

            // First find us:
            TextOverlayPosition cur = topLevel.calculateOverlayPos(topLevel.getCurrentPos());
            List<Line> lines = TextOverlayPosition.groupIntoLines(overlayPositions);
            int curLine = -1;
            for (int i = 0; i < lines.size();i++)
            {
                if (lines.get(i).topY <= cur.getSceneTopY() && lines.get(i).bottomY >= cur.getSceneBottomY())
                {
                    curLine = i;
                    break;
                }
            }
            
            if (curLine > 0)
            {
                double nearestDist = 9999.0;
                StructuredSlotField nearest = null;
                for (int i = 0; i < lines.get(curLine - 1).positions.size(); i++)
                {
                    TextOverlayPosition p = lines.get(curLine - 1).positions.get(i);
                    double dist = Math.abs(p.getSceneX() - cur.getSceneX());
                    if (dist < nearestDist && p.getSource() != null)
                    {
                        nearestDist = dist;
                        nearest = p.getSource();
                    }
                }
                if (nearest != null)
                {
                    nearest.focusAtPos(nearest.getNearest(cur.getSceneX(), cur.getSceneTopY(), false, false).getPos());
                    return;
                }
            }
            
            // In all other cases, just focus cursor above:
            row.focusUp(this, false);
        }
    }

    // Package-visible
    void down()
    {
        if (errorAndFixDisplay != null && errorAndFixDisplay.hasFixes() && errorAndFixDisplay.isShowing())
        {
            errorAndFixDisplay.down();
        }
        else
        {
            // See if there is content below us within the expression:
            List<TextOverlayPosition> overlayPositions = topLevel.getAllStartEndPositionsBetween(null, null).collect(Collectors.toList());

            // First find us:
            TextOverlayPosition cur = topLevel.calculateOverlayPos(topLevel.getCurrentPos());
            List<Line> lines = TextOverlayPosition.groupIntoLines(overlayPositions);
            int curLine = -1;
            for (int i = 0; i < lines.size();i++)
            {
                if (lines.get(i).topY <= cur.getSceneTopY() && lines.get(i).bottomY >= cur.getSceneBottomY())
                {
                    curLine = i;
                    break;
                }
            }
            
            if (curLine < lines.size() - 1)
            {
                double nearestDist = 9999.0;
                StructuredSlotField nearest = null;
                for (int i = 0; i < lines.get(curLine + 1).positions.size(); i++)
                {
                    TextOverlayPosition p = lines.get(curLine + 1).positions.get(i);
                    double dist = Math.abs(p.getSceneX() - cur.getSceneX());
                    if (dist < nearestDist && p.getSource() != null)
                    {
                        nearestDist = dist;
                        nearest = p.getSource();
                    }
                }
                if (nearest != null)
                {
                    nearest.focusAtPos(nearest.getNearest(cur.getSceneX(), cur.getSceneBottomY(), false, false).getPos());
                    return;
                }
            }
            
            // In all other cases, just focus cursor above:
            
            row.focusDown(this);
        }
    }
    
    // Package-visible
    @OnThread(Tag.FXPlatform)
    void enter()
    {
        if (errorAndFixDisplay != null && errorAndFixDisplay.hasFixes())
        {
            errorAndFixDisplay.executeSelected();
        }
        else
        {
            row.focusEnter(this);
        }
    }

    public String getJavaCode()
    {
        if (topLevel.isCurlyLiteral())
            return getCurlyLiteralPrefix() + topLevel.getJavaCode();
        else
            return topLevel.getJavaCode();
    }

    @OnThread(Tag.FXPlatform)
    public void caretMoved()
    {
        CaretPos pos = topLevel.getCurrentPos();
        showErrorAtCaret(pos);
        topLevel.showHighlightedBrackets(null, pos);
        if (pos != null)
            mostRecentPos = pos;
    }

    @OnThread(Tag.FXPlatform)
    public void escape()
    {
        // We want the error to hide until the user moves out and in, but not re-show if
        // the user moves within the error.
        // Easiest thing to do is notionally keep it as the display, but actually hide it:
        if (errorAndFixDisplay != null)
        {
            errorAndFixDisplay.hide();
        }
        else
        {
            row.escape(this);
        }
    }

    @Override
    public ObservableList<Node> getComponents()
    {
        return topLevel.getComponents();
    }

    @Override
    public TextOverlayPosition getOverlayLocation(int stringCaretPos, boolean javaPos)
    {
        CaretPos p;
        if (stringCaretPos == Integer.MAX_VALUE)
            return topLevel.calculateOverlayEnd();
        else if (stringCaretPos < 0)
            p = topLevel.getStartPos();
        else
        {
            p = javaPos ? javaPosToCaretPos(stringCaretPos) : topLevel.stringPosToCaretPos(stringCaretPos, false);
            if (p == null)
                p = topLevel.getEndPos();
        }
        return topLevel.calculateOverlayPos(p);
    }

    public List<Line> getAllLines(int start, int end, boolean javaPos)
    {
        CaretPos startPos = topLevel.stringPosToCaretPos(start, javaPos);
        CaretPos endPos;
        if (end == Integer.MAX_VALUE)
        {
            endPos = null;
        }
        else
        {
            endPos = topLevel.stringPosToCaretPos(end, javaPos);
        }
        return TextOverlayPosition.groupIntoLines(topLevel.getAllStartEndPositionsBetween(startPos, endPos).collect(Collectors.toList()));
    }

    // package-visible
    CaretPos javaPosToCaretPos(int pos)
    {
        if (topLevel.isCurlyLiteral())
            pos -= getCurlyLiteralPrefix().length();
        return topLevel.stringPosToCaretPos(Math.max(0, pos), true);
    }

    public InfixStructured getTopLevel()
    {
        return topLevel;
    }

    public FocusParent<HeaderItem> getSlotParent()
    {
        return row;
    }

    /**
     * Returns true if the method has transferred focus out of the slot
     */
    @OnThread(Tag.FXPlatform)
    public boolean backspaceAtStart()
    {
        return row.backspaceAtStart(this);
    }

    public void addClosingChar(char c)
    {
        topLevel.addClosingChar(c);
    }

    public boolean checkFilePreviewShortcut(KeyCode code)
    {
        if (fileCompletionShortcuts != null && fileCompletionShortcuts.containsKey(code))
        {
            fileCompletionShortcuts.get(code).run();
            return true;
        }
        return false;
    }
    
    @OnThread(Tag.FXPlatform)
    public boolean isShowingSuggestions()
    {
        return suggestionDisplay != null && suggestionDisplay.isShowing() && !suggestionDisplay.isInMiddleOfHiding();
    }

    public abstract List<? extends PossibleLink> findLinks();
    
    @Override
    public void lostFocus()
    {
        if (focusedProperty.get())
        {

            // Don't show new value as old:
            recentValues.removeAll(getText());
            // Remove any old value from middle, re-add at top:
            recentValues.removeAll(valueOnGain);
            recentValues.add(0, valueOnGain);
            // Trim list to last three:
            while (recentValues.size() > 3)
                recentValues.remove(3);

            editor.endRecordingState(this);

            topLevel.getAllExpressions().forEach(InfixStructured::deselect);
            notifyLostFocusExcept(null);
            lostFocusActions.forEach(FXRunnable::run);
        }
        focusedProperty.set(false);
    }

    void notifyGainFocus(StructuredSlotField focus)
    {
        // Tell other slots they've lost focus:
        notifyLostFocusExcept(focus);
        if (!focusedProperty.get())
        {
            valueOnGain = getText();
            editor.beginRecordingState(this);
        }
        focusedProperty.set(true);
    }

    private void notifyLostFocusExcept(StructuredSlotField except)
    {
        topLevel.getAllExpressions().forEach(e -> e.notifyLostFocus(except));
    }
    
    public void onLostFocus(FXRunnable action)
    {
        lostFocusActions.add(action);
    }

    public void addFocusListener(Frame frame)
    {
        onLostFocus(frame::checkForEmptySlot);
    }
    
    // package-visible
    ErrorUnderlineCanvas getOverlay()
    {
        return overlay;
    }
    
    @Override
    public Frame getParentFrame()
    {
        return parentFrame;
    }

    public List<StructuredSlot.PlainVarReference> findPlainVarReferences(String name)
    {
        return topLevel.findPlainVarUse(name);        
    }
    
    public String getCurlyLiteralPrefix()
    {
        return "";
    }
    
    @Override
    public void setView(View oldView, View newView, SharedTransition animate)
    {
        topLevel.setView(oldView, newView, animate, Optional.empty());
    }

    public boolean isConstantRange()
    {
        return topLevel.checkRangeExpression() == RangeType.RANGE_CONSTANT;
    }
    
    @Override
    public Map<TopLevelMenu, AbstractOperation.MenuItems> getMenuItems(boolean contextMenu)
    {
        HashMap<TopLevelMenu, AbstractOperation.MenuItems> itemMap = new HashMap<>();

        // We must have at least one dummy item for the menu to be shown:
        final Menu recentMenu = new Menu(Config.getString("frame.slot.recent"));
        recentMenu.setDisable(true);
        recentValues.addListener((ListChangeListener)c -> {
            recentMenu.getItems().clear();
            if (recentValues.isEmpty())
            {
                recentMenu.setDisable(true);
            }
            else
            {
                recentMenu.setDisable(false);
                recentValues.forEach(v -> {
                    MenuItem item = new MenuItem(v);
                    item.setOnAction(e -> {
                        editor.recordEdits(StrideEditReason.FLUSH);
                        setText(v);
                        modified();
                        editor.recordEdits(StrideEditReason.UNDO_LOCAL);
                    });
                    recentMenu.getItems().add(item);
                });
            }
        });

        final ObservableList<AbstractOperation.SortedMenuItem> originalItems = FXCollections.observableArrayList();
        final FXConsumer<ObservableList<AbstractOperation.SortedMenuItem>> setToOriginal = l -> {
            if (contextMenu)
                l.setAll(AbstractOperation.MenuItemOrder.RECENT_VALUES.item(recentMenu));
            else
                l.clear();
        };
        setToOriginal.accept(originalItems);
        itemMap.put(TopLevelMenu.EDIT, new AbstractOperation.MenuItems(originalItems) {
            @OnThread(Tag.FXPlatform)
            public void onShowing()
            {
                Stream<InfixStructured<?, ?>> allExpressions = getTopLevel().getAllExpressions();
                InfixStructured exp = allExpressions.filter(i -> i.isFocused()).findFirst().orElse(null);
                if (exp == null)
                {
                    setToOriginal.accept(items);
                }
                else
                {
                    MenuItem cut = JavaFXUtil.makeMenuItem(Config.getString("frame.slot.cut"), exp::cut, new KeyCodeCombination(KeyCode.X, KeyCodeCombination.SHORTCUT_DOWN));
                    MenuItem copy = JavaFXUtil.makeMenuItem(Config.getString("frame.slot.copy"), exp::copy, new KeyCodeCombination(KeyCode.C, KeyCodeCombination.SHORTCUT_DOWN));
                    MenuItem paste = JavaFXUtil.makeMenuItem(Config.getString("frame.slot.paste"), exp::paste, Config.isMacOS() ? null : new KeyCodeCombination(KeyCode.V, KeyCodeCombination.SHORTCUT_DOWN));

                    boolean inSelection = exp.isInSelection();

                    cut.setDisable(!inSelection);
                    copy.setDisable(!inSelection);
                    paste.setDisable(!Clipboard.getSystemClipboard().hasString());

                    setToOriginal.accept(items);
                    items.addAll(
                        AbstractOperation.MenuItemOrder.CUT.item(cut),
                        AbstractOperation.MenuItemOrder.COPY.item(copy),
                        AbstractOperation.MenuItemOrder.PASTE.item(paste)
                    );
                }
                if (hoverErrorCurrentlyShown != null ){
                    errorAndFixDisplay.hide();
                }
            }
        });

        if (contextMenu)
        {
            final AbstractOperation.SortedMenuItem scanningItem = AbstractOperation.MenuItemOrder.GOTO_DEFINITION.item(new MenuItem("Scanning..."));
            scanningItem.getItem().setDisable(true);

            itemMap.put(TopLevelMenu.VIEW, new AbstractOperation.MenuItems(FXCollections.observableArrayList())
            {

                private void removeScanning()
                {
                    if (items.size() == 1 && items.get(0) == scanningItem)
                        items.clear();
                }

                @OnThread(Tag.FXPlatform)
                public void onShowing()
                {
                    items.setAll(scanningItem);

                    CaretPos caretPos = getTopLevel().getCurrentPos();

                    FXPlatformConsumer<Optional<LinkedIdentifier>> withLink = optLink -> {
                        removeScanning();
                        optLink.ifPresent(defLink -> {
                            items.add(AbstractOperation.MenuItemOrder.GOTO_DEFINITION.item(JavaFXUtil.makeMenuItem(Config.getString("frame.slot.goto")
                                    .replace("$", defLink.getName()), defLink.getOnClick(), null)));
                        });
                    };

                    withLinksAtPos(caretPos, withLink);

                    // Hack for now, to make scanning disappear if nothing is found:
                    JavaFXUtil.runAfter(Duration.millis(1000), this::removeScanning);
                }

                public void onHidden()
                {
                    items.clear();
                }
            });
        }
        
        return itemMap;
    }

    //package-visible
    @OnThread(Tag.FXPlatform)
    void withLinksAtPos(CaretPos caretPos, FXPlatformConsumer<Optional<LinkedIdentifier>> withLink)
    {
        List<? extends PossibleLink> possibleLinks = findLinks();

        possibleLinks.removeIf(possLink -> {
            CaretPos startCaretPos = javaPosToCaretPos(possLink.getStartPosition());
            CaretPos endCaretPos = javaPosToCaretPos(possLink.getEndPosition());

            return !CaretPos.between(startCaretPos, endCaretPos, caretPos);
        });

        possibleLinks.forEach(possLink -> editor.searchLink(possLink, withLink));
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public Response suggestionListKeyPressed(SuggestionList suggestionList, KeyEvent event, int highlighted)
    {
        switch (event.getCode())
        {
            case ENTER:
                if (highlighted != -1)
                {
                    modificationPlatform(token -> executeSuggestion(highlighted, token, suggestionList.getRecordingId()));
                    return Response.DISMISS;
                }
            case ESCAPE:
                return Response.DISMISS;
            case BACK_SPACE:
                CaretPos updatedLocation = modificationReturnPlatform(token -> topLevel.deletePreviousAtPos(suggestionLocation, token));
                if (updatedLocation == null || !updatedLocation.init().equals(suggestionLocation.init()))
                {
                    JavaFXUtil.runAfterCurrent(() -> topLevel.positionCaret(updatedLocation));
                    return Response.DISMISS;
                }
                else
                {
                    suggestionLocation = updatedLocation;
                    overlay.redraw();
                    updateSuggestions(false);
                    return Response.CONTINUE;
                }
            case TAB:
                if (event.isShiftDown())
                    row.focusLeft(StructuredSlot.this);
                else
                {
                    row.focusRight(StructuredSlot.this);
                    completeIfPossible(highlighted, suggestionList);
                }
                return Response.DISMISS;
        }
        return Response.CONTINUE;
    }

    @OnThread(Tag.FXPlatform)
    private void completeIfPossible(int highlighted, SuggestionList suggestionList)
    {
        // Pick a value if one was available to complete:
        if (highlighted != -1)
        {
            modificationPlatform(token -> executeSuggestion(highlighted, token, suggestionList.getRecordingId()));
        }
        else if (suggestionDisplay.eligibleCount() == 1  && getText().length() > 0)
        {
            modificationPlatform(token -> executeSuggestion(suggestionDisplay.getFirstEligible(), token, suggestionList.getRecordingId()));
        }
    }

    @OnThread(Tag.FXPlatform)
    @Override
    public Response suggestionListKeyTyped(SuggestionList suggestionList, KeyEvent event, int highlighted)
    {
        return modificationReturnPlatform(token -> {
            CaretPos updatedLocation = null;
            if (!"\b".equals(event.getCharacter()))
            {
                updatedLocation = topLevel.insertAtPos(suggestionLocation, event.getCharacter(), token);
            }
            else
                return Response.CONTINUE;

            if (!updatedLocation.init().equals(suggestionLocation.init()))
            {
                return Response.DISMISS;
            }
            else
            {
                suggestionLocation = updatedLocation;
                overlay.redraw();
                updateSuggestions(true);
                return Response.CONTINUE;
            }
        });
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public void suggestionListChoiceClicked(SuggestionList suggestionList, int highlighted)
    {
        modificationPlatform(token -> executeSuggestion(highlighted, token, suggestionList.getRecordingId()));
    }
    
    @Override
    public void hidden() {
        fakeCaretShowing.set(false);
    }

    @OnThread(Tag.FXPlatform)
    public boolean suggestingFor(CaretPos fieldPos)
    {
        return fieldPos != null && suggestionLocation != null && fieldPos.equals(suggestionLocation.init())
                && suggestionDisplay != null && suggestionDisplay.isShowing();
    }

    @OnThread(Tag.FXPlatform)
    public boolean deleteAtEnd()
    {
        if (row != null)
        {
            return row.deleteAtEnd(this);
        }
        return false;
    }

    @OnThread(Tag.FXPlatform)
    public void setSplitText(String beforeCursor, String afterCursor)
    {
        modificationPlatform(token -> {
            topLevel.blank(token);
            CaretPos p = topLevel.insertImpl(topLevel.getFirstField(), 0, beforeCursor, false, token);
            topLevel.insertAtPos(p, afterCursor, token);
            token.after(() -> topLevel.positionCaret(p));
        });
    }

    public boolean isCurrentlyCompleting()
    {
        return currentlyCompleting;
    }

    //package-visible:
    List<ExtensionDescription> getExtensions()
    {
        return row.getExtensions();
    }

    //package-visible:
    @OnThread(Tag.FXPlatform)
    void notifyModifiedPress(KeyCode c)
    {
        row.notifyModifiedPress(c);
    }

    //package-visible:
    void focusNext()
    {
        row.focusRight(this);
    }

    public abstract boolean canCollapse();

    public static class SplitInfo
    {
        public final String lhs;
        public final String rhs;
        
        public SplitInfo(String lhs, String rhs) { this.lhs = lhs; this.rhs = rhs; }
    }

    public SplitInfo trySplitOnEquals()
    {
        return topLevel.trySplitOn("=");
    }

    public final List<FrameCatalogue.Hint> getHints()
    {
        return hints;
    }

    @Override
    public boolean isAlmostBlank()
    {
        return topLevel.isAlmostBlank();
    }

    @Override
    public boolean isEditable()
    {
        return editable;
    }

    @Override
    public void setEditable(boolean editable)
    {
        this.editable = editable;
        topLevel.setEditable(editable);
    }

    @Override
    public ObservableBooleanValue effectivelyFocusedProperty()
    {
        return effectivelyFocusedProperty;
    }

    @Override
    public int calculateEffort()
    {
        return topLevel.calculateEffort();
    }

    /**
     * Helper pair class for recording references to plain variables: gives
     * the graphical node containing the variable, and a callback which will rename
     * the use of that variable.
     */
    public static class PlainVarReference
    {
        public final FXConsumer<String> rename;
        public final Region refNode; // The Region of the textfield referencing the var
        PlainVarReference(FXConsumer<String> rename, Region refNode)
        {
            this.rename = rename;
            this.refNode = refNode;
        }
    }
    
    protected abstract INFIX newInfix(InteractionManager editor, ModificationToken token);

    /**
     * Runs a block of code (passed as parameter) which modifies the slot.
     * 
     * Calls to this function can be safely nested, although it should be avoided
     * where possible.  You should make sure that all related modifications are encompassed
     * within a single modificationReturn call, so that the outside world only
     * sees a single complete modification. 
     * 
     * @param modificationAction A block of code to run which modifies the slot.
     * @param <T> The return type of the inner function.
     * @return The return value of the inner function.
     */
    //package-visible
    @OnThread(Tag.FX)
    <T> T modificationReturn(FXFunction<ModificationToken, T> modificationAction)
    {
        ModificationToken token = new ModificationToken();
        modificationTokens.add(token);
        T ret = modificationAction.apply(token);
        if (modificationTokens.get(modificationTokens.size() - 1) != token)
            throw new IllegalStateException("Modifications did not nest"); // Should not be possible
        modificationTokens.remove(token);
        //We only update when outermost modification finishes, i.e. the modification stack is empty:
        if (modificationTokens.isEmpty())
        {
            // All modifications finished; update and run after actions:
            textMirror.set(topLevel.calculateText());
            // Actions specific to this modification:
            token.runAfters();
            // And our persistent actions:
            afterModify.forEach(FXRunnable::run);
        }
        else
        {
            // Add after actions to top of stack token, rather than running them now:
            modificationTokens.get(0).afters.addAll(token.afters);
        }
        return ret;
    }

    @OnThread(Tag.FXPlatform)
    <T> T modificationReturnPlatform(FXPlatformFunction<ModificationToken, T> modificationAction)
    {
        // Defeat thread-checker:
        return modificationReturn((FXFunction<ModificationToken, T>)(modificationAction::apply));
    }
    
    //package-visible
    // See modificationReturn
    void modification(FXConsumer<ModificationToken> modificationAction)
    {
        modificationReturn(t -> {modificationAction.accept(t);return 0;});
    }

    //package-visible
    @OnThread(Tag.FXPlatform)
    void modificationPlatform(FXPlatformConsumer<ModificationToken> modificationAction)
    {
        modificationReturnPlatform(t -> {modificationAction.accept(t);return 0;});
    }
    
    //package-visible
    // Only used for testing:
    static <T> T testingModification(FXFunction<ModificationToken, T> modificationAction)
    {
        return modificationAction.apply(new ModificationToken());
    }

    //package-visible
    void afterCurrentModification(FXRunnable action)
    {
        if (modificationTokens.isEmpty())
            action.run();
        else
            modificationTokens.get(0).after(action);
    }

    /**
     * We want to make sure that modifications to the slot's content only
     * occur inside the modification/modificationReturn functions above.
     * To this end, we require a ModificationToken parameter to any actions
     * which modify a structured slot (StructuredSlotField.setText, fields.add, etc).
     * 
     * To get an instance of this class, you must use the modification/modificationReturn
     * function rather than constructing one directly.  This ensures the post-modification
     * actions are run correctly after a complete change.
     */
    public static class ModificationToken
    {
        // Actions to run when the modification completes:
        private List<FXRunnable> afters = new ArrayList<>();
        
        private ModificationToken() { }

        // Doesn't do anything, but you'll get an exception if you call it
        // on a null token. 
        public void check()
        {

        }

        // Specify an action to run once the outermost current ongoing
        // modification is complete.
        public void after(FXRunnable action)
        {
            afters.add(action);
        }
        
        // Run the after actions.
        private void runAfters()
        {
            afters.forEach(FXRunnable::run);
        }
    }
}
