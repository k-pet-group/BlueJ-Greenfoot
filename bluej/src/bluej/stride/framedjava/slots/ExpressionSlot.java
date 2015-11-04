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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import bluej.editor.stride.FrameCatalogue;

import bluej.stride.framedjava.ast.SuperThis;
import bluej.stride.framedjava.ast.links.PossibleLink;
import bluej.stride.framedjava.slots.InfixExpression.RangeType;
import javafx.application.Platform;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
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
import bluej.stride.framedjava.ast.ExpressionSlotFragment;
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
import bluej.stride.slots.ChoiceSlot;
import bluej.stride.slots.EditableSlot;
import bluej.stride.slots.Focus;
import bluej.stride.slots.FocusParent;
import bluej.stride.slots.HeaderItem;
import bluej.stride.slots.SlotLabel;
import bluej.stride.slots.SuggestionList;
import bluej.stride.slots.SuggestionList.SuggestionDetails;
import bluej.stride.slots.SuggestionList.SuggestionDetailsWithCustomDoc;
import bluej.stride.slots.SuggestionList.SuggestionListListener;
import bluej.utility.Debug;
import bluej.utility.Utility;
import bluej.utility.javafx.ErrorUnderlineCanvas;
import bluej.utility.javafx.FXConsumer;
import bluej.utility.javafx.FXRunnable;
import bluej.utility.javafx.JavaFXUtil;
import bluej.utility.javafx.SharedTransition;

/**
 * The ExpressionSlot class is used where a single expression is wanted as a slot.  For example,
 * in the condition of an if statement, the LHS and the RHS of an assignment, and so on.
 * 
 * ExpressionSlot implements the Slot interface (and EditableSlot) and also handles any behaviour
 * that should be common to the whole slot.  For example, error display is handled by ExpressionSlot
 * because we only want to display at most one error per expression at any time.  Similarly, code
 * completion is handled here because we only want one code completion showing, and we may want the
 * code completion to keep showing even as the user navigates (and moves focus) among the contents of
 * the expression.
 *
 * All the actual graphical components and logic are not directly implemented by this class, but are
 * handled by InfixExpression (ExpressionSlot always has one, via the topLevel field), so you should
 * see that class for more details.  ExpressionSlot has some role in bridging the gap between all the logic
 * bundled in InfixExpression and the "outside world", e.g. when InfixExpression is changed, it calls
 * ExpressionSlot.modified to notify the editor, etc.
 */
public abstract class ExpressionSlot<SLOT_FRAGMENT extends ExpressionSlotFragment> implements EditableSlot, ErrorFixListener, SuggestionListListener
{
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

    private final ErrorUnderlineCanvas overlay;
    //private final StackPane pane = new StackPane();
    // Same item, but stored twice due to types:
    private final Frame parentFrame;
    private final CodeFrame<?> parentCodeFrame;
    private final FrameContentRow row;
    
    private final List<CodeError> allErrors = new ArrayList<>();
    private final List<CodeError> shownErrors = new ArrayList<>();
    private ErrorAndFixDisplay errorAndFixDisplay;
    private CodeError hoverErrorCurrentlyShown;
    private SLOT_FRAGMENT slotElement;
    
    private final InfixExpression topLevel;
    private final InteractionManager editor;
    
    private final List<Underline> underlines = new ArrayList<>();
    
    private final ExpressionCompletionCalculator completionCalculator;
    private SuggestionList suggestionDisplay;
    private Region suggestionNode;
    private List<FileCompletion> fileCompletions;
    private Map<KeyCode, Runnable> fileCompletionShortcuts;
    
    private StringExpression targetType;
    private boolean beenModified = false;
    private final StringProperty textMirror = new SimpleStringProperty("");
    private List<TextOverlayPosition> selectionDrawPositions;
    private boolean modifyQueued = false;
    private final List<FXRunnable> lostFocusActions = new ArrayList<>();
    private CaretPos suggestionLocation;
    private ExpressionSlotField suggestionField;
    private final SimpleBooleanProperty fakeCaretShowing = new SimpleBooleanProperty(false);
    private final List<FrameCatalogue.Hint> hints;
    private final ObservableList<String> recentValues = FXCollections.observableArrayList();
    private CaretPos mostRecentPos;
    private boolean hadFocus;
    private String valueOnGain;
    private boolean editable = true;
    private ChoiceSlot<SuperThis> paramsToConstructor;

    public ExpressionSlot(InteractionManager editor,
            Frame parentFrame, CodeFrame<?> parentCodeFrame, FrameContentRow row, String stylePrefix, List<FrameCatalogue.Hint> hints)
    {
        this.editor = editor;
        this.parentFrame = parentFrame;
        this.parentCodeFrame = parentCodeFrame;
        this.row = row;
        this.completionCalculator = new ExpressionCompletionCalculator(editor);
        this.hints = hints;
        topLevel = new InfixExpression(editor, this, stylePrefix);
        
        this.textMirror.bind(topLevel.textProperty());
        textMirror.addListener((a, b, c) -> {
            // We delay the modified call so that the current code gets a chance to finish its change:
            if (!modifyQueued && !editor.isLoading()) // Only need to queue one at any time
            {
                modifyQueued = true;
                Platform.runLater(() -> {
                    modified();
                    modifyQueued = false;
                });
            }
            else if (editor.isLoading())
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
        
        JavaFXUtil.addChangeListener(fakeCaretShowing, b -> overlay.redraw());
        
        //overlay = new ErrorUnderlineCanvas(p -> topLevel.calculateOverlayX(topLevel.stringPosToCaretPos(p, true)), topLevel::getBaseline);
        
        //overlay.setMouseTransparent(true);
        //overlay.widthProperty().bind(topLevel.getNode().widthProperty());
        //overlay.heightProperty().bind(topLevel.getNode().heightProperty());

        //pane.getChildren().addAll(topLevel.getNode(), overlay);
    }
    
    
    
 // Errors:

    public void flagErrorsAsOld()
    {
        allErrors.forEach(CodeError::flagAsOld);
    }
    
    public void removeOldErrors()
    {
        allErrors.removeIf(CodeError::isFlaggedAsOld);
        recalculateShownErrors();
    }
    
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
            }
        });
        
        clearErrorMarkers();
        shownErrors.forEach(e -> drawErrorMarker(e.getStartPosition(), e.getEndPosition(), e.isJavaPos(), b -> showErrorHover(b ? e : null), e.visibleProperty()));
        
        CaretPos curPos = topLevel.getCurrentPos();
        if (curPos != null)
            showErrorAtCaret(curPos);
    }
    
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
                    hoverErrorCurrentlyShown = error; //update current error
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
    
    public void addError(CodeError err)
    {
        allErrors.add(err);
        err.bindFresh(getParentFrame().freshProperty());
        recalculateShownErrors();
    }
    
    @Override
    public void fixedError(CodeError err)
    {
        allErrors.remove(err);
        recalculateShownErrors();
    }
    
    public void drawErrorMarker(int startPos, int endPos, boolean javaPos, FXConsumer<Boolean> onHover, ObservableBooleanValue visible)
    {
        // If we are trying to highlight an empty slot, highlight whole width
        if ((startPos == 0 && endPos == 0) || getText().length() == 0)
            overlay.addErrorMarker(this, 0, Integer.MAX_VALUE, false, onHover, visible);
        else
            overlay.addErrorMarker(this, startPos, endPos, javaPos, onHover, visible);
    }    


    public void setText(ExpressionSlotFragment rhs)
    {
        rhs.registerSlot(this);
        setText(rhs.getContent());        
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
    

    public void clearErrorMarkers()
    {
        overlay.clearErrorMarkers(this);
    }

    public void setSimplePromptText(String t)
    {
        topLevel.setPromptText((fields, ops) -> {
            // If there's just one field:
            if (ops.isEmpty() && fields.size() == 1 && fields.get(0) instanceof ExpressionSlotField)
                ((ExpressionSlotField) fields.get(0)).setPromptText(t);
            else if (fields.size() > 0)
                ((ExpressionSlotField) fields.get(0)).setPromptText("");
        });
    }
    
    public void setMethodCallPromptText(String t)
    {
        topLevel.setPromptText((fields, ops) -> {
            // Can happen during blanking:
            if (ops.size() == 0 || fields.size() == 0)
                return;

            // Scan for fields that are followed by brackets (with no operator inbetween),
            // and preceded by start or a dot:
            for (int i = 0; i < fields.size() - 1 /* Don't look at last field */; i++)
            {
                if (fields.get(i) instanceof ExpressionSlotField)
                {
                    ExpressionSlotField f = (ExpressionSlotField)fields.get(i);
                    // Brackets after, with no op inbetween:
                    if (ops.get(i) == null && fields.get(i+1) instanceof BracketedExpression)
                    {
                        // No op beforehand, or a dot:
                        if (i == 0 || (ops.get(i-1) != null && ops.get(i-1).get().equals(".")))
                        {
                            f.setPromptText(t);
                        }
                    }
                }
            }
        });
    }

    public void onTextPropertyChange(FXConsumer<String> listener)
    {
        // Really this property can show incomplete states, so we use runLater to make sure update is complete:
        JavaFXUtil.addChangeListener(textMirror, s -> Platform.runLater(() -> listener.accept(textMirror.get())));
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
    public Stream<CodeError> getCurrentErrors()
    {
        return shownErrors.stream();
    }

    public void setText(String text)
    {
        topLevel.blank();
        topLevel.insert(topLevel.getFirstField(), 0, text);        
    }

    @Override
    public void focusAndPositionAtError(CodeError err)
    {
        requestFocus();
        topLevel.positionCaret(javaPosToCaretPos(err.getStartPosition()));        
    }

    @Override
    public void addUnderline(Underline u)
    {
        underlines.add(u);
        drawUnderlines();
    }
    
    @Override
    public void removeAllUnderlines()
    {
        underlines.clear();
        drawUnderlines();
    }

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
            errorAndFixDisplay.hide();
            errorAndFixDisplay = null;
        }
    }

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
        editor.modifiedFrame(parentFrame);
        editor.regenerateAndReparse(null);
    }
    
    private double overlayToSceneX(double overlayX)
    {
        return overlay.localToScene(overlayX, 0.0).getX();
    }
    
    private double overlayToSceneY(double overlayY)
    {
        return overlay.localToScene(0.0, overlayY).getX();
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
        if (invalidateErrors)
        {
            clearErrorMarkers();
        }
        selectionDrawPositions = null;
        overlay.redraw();
    }
    
    // package-visible
    void drawSelection(List<TextOverlayPosition> positions)
    {
        selectionDrawPositions = positions;
        overlay.redraw();
    }
    
    public void bindTargetType(StringExpression targetTypeBinding)
    {
        this.targetType = targetTypeBinding;
    }

    public void setTargetType(String targetType)
    {
        this.targetType = new SimpleStringProperty(targetType);
    }

    public static Label makeBracket(String content, boolean opening, InfixExpression parent)
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

    public static SlotLabel makeBracketSlot(String content, boolean opening, InfixExpression parent)
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

    // package-visible
    void showSuggestionDisplay(ExpressionSlotField field, int caretPosition, boolean stringLiteral)
    {
        if (suggestionDisplay != null) {
            hideSuggestionDisplay();
        }
        
        suggestionField = field;
        suggestionNode = field.getComponents().get(0);
        
        FXConsumer<SuggestionList> withSuggList = suggList -> {
            suggestionDisplay = suggList;
            updateSuggestions(true);
            suggestionDisplay.highlightFirstEligible();
            suggestionDisplay.show(suggestionNode, new ReadOnlyDoubleWrapper(0.0), field.heightProperty());
            fakeCaretShowing.set(true);
        };
        
        suggestionLocation = topLevel.getCurrentPos();
        
        if (Config.isGreenfoot() && stringLiteral) {
            // They are just inside a string; complete image file names:
            fileCompletions = editor.getAvailableFilenames();
            // Have to have this function on a separate line to work around Eclipse bug (sigh):
            Function<FileCompletion, SuggestionDetails> func = f -> new SuggestionDetailsWithCustomDoc(f.getFile().getName(), null, f.getType(), SuggestionList.SuggestionShown.COMMON, () -> makeFileCompletionPreview(f));
            withSuggList.accept(new SuggestionList(editor, Utility.mapList(fileCompletions, func), null, SuggestionList.SuggestionShown.RARE, null, ExpressionSlot.this));
            
        }
        else {
            // TODO we shouldn't need to regen whole code repeatedly if they only modify this slot:
            editor.regenerateAndReparse(this);
            PosInSourceDoc posInFile = getSlotElement().getPosInSourceDoc(topLevel.caretPosToStringPos(topLevel.getCurrentPos(), true));
            completionCalculator.withCalculatedSuggestionList(posInFile, this, parentCodeFrame.getCode(), ExpressionSlot.this, (targetType == null /* || not at start getStartOfCurWord() != 0 */) ? null : targetType.get(), withSuggList);
        }
        
    }

    // package-visible
    void withParamNamesForConstructor(FXConsumer<List<List<String>>> handler)
    {
        editor.regenerateAndReparse(this);
        completionCalculator.withConstructorParamNames(paramsToConstructor.getValue(SuperThis.EMPTY), handler);
    }
    
    // package-visible
    void withParamNamesForPos(CaretPos pos, String methodName, FXConsumer<List<List<String>>> handler)
    {
        editor.regenerateAndReparse(this);
        PosInSourceDoc posJava = getSlotElement().getPosInSourceDoc(topLevel.caretPosToStringPos(pos, true));
        completionCalculator.withParamNames(posJava, this, methodName, parentCodeFrame.getCode(), handler);
    }

    // package-visible
    void withParamHintsForPos(CaretPos pos, String methodName, FXConsumer<List<List<String>>> handler)
    {
        editor.regenerateAndReparse(this);
        PosInSourceDoc posJava = getSlotElement().getPosInSourceDoc(topLevel.caretPosToStringPos(pos, true));
        completionCalculator.withParamHints(posJava, this, methodName, parentCodeFrame.getCode(), handler);
    }

    // package-visible
    void withParamHintsForConstructor(int totalParams, FXConsumer<List<List<String>>> handler)
    {
        editor.regenerateAndReparse(this);
        completionCalculator.withConstructorParamHints(paramsToConstructor.getValue(SuperThis.EMPTY), totalParams, handler);
    }

    // package-visible
    void withMethodHint(CaretPos pos, String methodName, FXConsumer<List<String>> handler)
    {
        editor.regenerateAndReparse(this);
        PosInSourceDoc posJava = getSlotElement().getPosInSourceDoc(topLevel.caretPosToStringPos(pos, true));
        completionCalculator.withMethodHints(posJava, this, methodName, parentCodeFrame.getCode(), handler);
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
                suggestionDisplay.updateVisual(prefix, false);
            }
            //lastBeforePrefix = getText().substring(0, getStartOfCurWord());
        }
    }
    
    private void executeSuggestion(int selected)
    {
        if (fileCompletions != null && selected != -1)
        {
            FileCompletion fc = fileCompletions.get(selected);
            topLevel.insertSuggestion(suggestionLocation, fc.getFile().getName(), null);
        }
        else
        {
            topLevel.insertSuggestion(suggestionLocation, completionCalculator.getName(selected), completionCalculator.getParams(selected));
        }
        modified();
    }
    
    // Package-visible
    void up()
    {
        if (errorAndFixDisplay != null && errorAndFixDisplay.hasFixes())
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
                ExpressionSlotField nearest = null;
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
        if (errorAndFixDisplay != null && errorAndFixDisplay.hasFixes())
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
                ExpressionSlotField nearest = null;
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

    @Override
    public void saved()
    {
        if (getParentFrame().isFrameEnabled())
        {
            if (paramsToConstructor != null)
            {
                topLevel.treatAsConstructorParams_updatePrompts();
            }
        }
    }
    
    public void caretMoved()
    {
        CaretPos pos = topLevel.getCurrentPos();
        showErrorAtCaret(pos);
        topLevel.showHighlightedBrackets(null, pos);
        if (pos != null)
            mostRecentPos = pos;
    }

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

    public InfixExpression getTopLevel()
    {
        return topLevel;
    }

    //package-visible
    FocusParent<HeaderItem> getSlotParent()
    {
        return row;
    }

    /**
     * Returns true if the method has transferred focus out of the slot
     */
    public boolean backspaceAtStart()
    {
        return row.backspaceAtStart(this);
    }

    public void bindClosingChar(EditableSlot anotherSlot, char c)
    {
        topLevel.bindClosingChar(anotherSlot, c);
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
    
    public boolean isShowingSuggestions()
    {
        return suggestionDisplay != null && suggestionDisplay.isShowing() && !suggestionDisplay.isInMiddleOfHiding();
    }

    public List<? extends PossibleLink> findLinks()
    {
        return topLevel.findLinks(Optional.empty(), getSlotElement().getVars(), offset -> getSlotElement().getPosInSourceDoc(offset), 0);
    }
    
    @Override
    public void lostFocus()
    {
        if (hadFocus)
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
        }
        hadFocus = false;
        topLevel.getAllExpressions().forEach(InfixExpression::deselect);
        notifyLostFocusExcept(null);
        lostFocusActions.forEach(FXRunnable::run);
    }

    void notifyGainFocus(ExpressionSlotField focus)
    {
        // Tell other slots they've lost focus:
        notifyLostFocusExcept(focus);
        if (!hadFocus)
        {
            valueOnGain = getText();
            editor.beginRecordingState(this);
        }
        hadFocus = true;
    }

    private void notifyLostFocusExcept(ExpressionSlotField except)
    {
        topLevel.getAllExpressions().forEach(e -> e.notifyLostFocus(except));
    }
    
    public void onLostFocus(FXRunnable action)
    {
        lostFocusActions.add(action);
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
    
    @Override
    public ExpressionSlot asExpressionSlot() { return this; }

    public List<ExpressionSlot.PlainVarReference> findPlainVarReferences(String name)
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
    public Map<TopLevelMenu, MenuItems> getMenuItems(boolean contextMenu)
    {
        HashMap<TopLevelMenu, MenuItems> itemMap = new HashMap<>();

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
                    item.setOnAction(e -> setText(v));
                    recentMenu.getItems().add(item);
                });
            }
        });

        final ObservableList<SortedMenuItem> originalItems = FXCollections.observableArrayList();
        final FXConsumer<ObservableList<SortedMenuItem>> setToOriginal = l -> {
            if (contextMenu)
                l.setAll(MenuItemOrder.RECENT_VALUES.item(recentMenu));
            else
                l.clear();
        };
        setToOriginal.accept(originalItems);
        itemMap.put(TopLevelMenu.EDIT, new MenuItems(originalItems) {
            public void onShowing()
            {
                InfixExpression exp = getTopLevel().getAllExpressions().filter(InfixExpression::isFocused).findFirst().orElse(null);
                if (exp == null)
                {
                    setToOriginal.accept(items);
                }
                else
                {
                    MenuItem cut = JavaFXUtil.makeMenuItem(Config.getString("frame.slot.cut"), exp::cut, new KeyCodeCombination(KeyCode.X, KeyCodeCombination.SHORTCUT_DOWN));
                    MenuItem copy = JavaFXUtil.makeMenuItem(Config.getString("frame.slot.copy"), exp::copy, new KeyCodeCombination(KeyCode.C, KeyCodeCombination.SHORTCUT_DOWN));
                    MenuItem paste = JavaFXUtil.makeMenuItem(Config.getString("frame.slot.paste"), exp::paste, new KeyCodeCombination(KeyCode.V, KeyCodeCombination.SHORTCUT_DOWN));

                    boolean inSelection = exp.isInSelection();

                    cut.setDisable(!inSelection);
                    copy.setDisable(!inSelection);
                    paste.setDisable(!Clipboard.getSystemClipboard().hasString());

                    setToOriginal.accept(items);
                    items.addAll(
                        MenuItemOrder.CUT.item(cut),
                        MenuItemOrder.COPY.item(copy),
                        MenuItemOrder.PASTE.item(paste)
                    );
                }
                if (hoverErrorCurrentlyShown != null ){
                    errorAndFixDisplay.hide();
                }
            }
        });

        if (contextMenu)
        {
            final SortedMenuItem scanningItem = MenuItemOrder.GOTO_DEFINITION.item(new MenuItem("Scanning..."));
            scanningItem.getItem().setDisable(true);

            itemMap.put(TopLevelMenu.VIEW, new MenuItems(FXCollections.observableArrayList())
            {

                private void removeScanning()
                {
                    if (items.size() == 1 && items.get(0) == scanningItem)
                        items.clear();
                }

                public void onShowing()
                {
                    items.setAll(scanningItem);

                    CaretPos caretPos = getTopLevel().getCurrentPos();
                    Debug.message("Scanning position: " + caretPos);

                    List<? extends PossibleLink> possibleLinks = findLinks();

                    possibleLinks.removeIf(possLink -> {
                        CaretPos startCaretPos = javaPosToCaretPos(possLink.getStartPosition());
                        CaretPos endCaretPos = javaPosToCaretPos(possLink.getEndPosition());

                        return !CaretPos.between(startCaretPos, endCaretPos, caretPos);
                    });

                    possibleLinks.forEach(possLink ->
                            editor.searchLink(possLink, optLink -> {
                                removeScanning();
                                optLink.ifPresent(defLink -> {
                                    items.add(MenuItemOrder.GOTO_DEFINITION.item(JavaFXUtil.makeMenuItem("Go to definition of \"" + defLink.getName() + "\"", defLink.getOnClick(), null)));
                                });
                            })
                    );

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
    
    @Override
    public Response suggestionListKeyPressed(KeyEvent event, int highlighted)
    {
        switch (event.getCode())
        {
            case ENTER:
                if (highlighted != -1)
                {
                    executeSuggestion(highlighted);
                    return Response.DISMISS;
                }
            case ESCAPE:
                return Response.DISMISS;
            case BACK_SPACE:
                CaretPos updatedLocation = topLevel.deletePreviousAtPos(suggestionLocation);
                if (!updatedLocation.init().equals(suggestionLocation.init()))
                {
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
                    row.focusLeft(ExpressionSlot.this);
                else
                {
                    row.focusRight(ExpressionSlot.this);
                    completeIfPossible(highlighted);
                }
                return Response.DISMISS;
        }
        return Response.CONTINUE;
    }

    private void completeIfPossible(int highlighted)
    {
        // Pick a value if one was available to complete:
        if (highlighted != -1)
        {
            executeSuggestion(highlighted);
        }
        else if (suggestionDisplay.eligibleCount() == 1  && getText().length() > 0)
        {
            executeSuggestion(suggestionDisplay.getFirstEligible());
        }
    }

    @Override
    public Response suggestionListKeyTyped(KeyEvent event, int highlighted)
    {
        CaretPos updatedLocation = null;
        if (!"\b".equals(event.getCharacter()))
        {
            updatedLocation = topLevel.insertAtPos(suggestionLocation, event.getCharacter());
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
    }

    @Override
    public void suggestionListChoiceClicked(int highlighted)
    {
        executeSuggestion(highlighted);
    }
    
    

    @Override
    public void hidden() {
        fakeCaretShowing.set(false);
    }



    // package visible
    boolean suggestingFor(CaretPos fieldPos)
    {
        return fieldPos != null && suggestionLocation != null && fieldPos.equals(suggestionLocation.init())
                && suggestionDisplay != null && suggestionDisplay.isShowing();
    }



    public boolean deleteAtEnd()
    {
        if (row != null)
        {
            return row.deleteAtEnd(this);
        }
        return false;
    }



    public void setSplitText(String beforeCursor, String afterCursor)
    {
        topLevel.blank();
        CaretPos p = topLevel.insert_(topLevel.getFirstField(), 0, beforeCursor, false);
        topLevel.insertAtPos(p, afterCursor);
        Platform.runLater(() -> topLevel.positionCaret(p));
    }


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

    public List<FrameCatalogue.Hint> getHints()
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

    public void setParamsToConstructor(ChoiceSlot<SuperThis> paramsToConstructor)
    {
        this.paramsToConstructor = paramsToConstructor;
    }

    // package-visible
    boolean isConstructorParams()
    {
        return paramsToConstructor != null;
    }
}
