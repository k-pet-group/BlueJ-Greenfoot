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
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import bluej.editor.stride.FrameCatalogue;
import bluej.stride.framedjava.ast.links.PossibleLink;
import javafx.application.Platform;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.effect.BlendMode;
import javafx.scene.input.Clipboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.text.Font;
import javafx.scene.web.WebView;
import javafx.util.Duration;
import bluej.Config;
import bluej.editor.stride.CodeOverlayPane;
import bluej.stride.framedjava.ast.TextSlotFragment;
import bluej.stride.framedjava.elements.CodeElement;
import bluej.stride.framedjava.errors.CodeError;
import bluej.stride.framedjava.errors.ErrorAndFixDisplay;
import bluej.stride.framedjava.errors.ErrorAndFixDisplay.ErrorFixListener;
import bluej.stride.framedjava.frames.CodeFrame;
import bluej.stride.framedjava.slots.TextOverlayPosition;
import bluej.stride.generic.AssistContentThreadSafe;
import bluej.stride.generic.Frame;
import bluej.stride.generic.Frame.View;
import bluej.stride.generic.FrameContentRow;
import bluej.stride.generic.InteractionManager;
import bluej.stride.generic.InteractionManager.FileCompletion;
import bluej.stride.slots.SuggestionList.SuggestionDetails;
import bluej.stride.slots.SuggestionList.SuggestionListListener;
import bluej.utility.JavaUtils;
import bluej.utility.Utility;
import bluej.utility.javafx.AnnotatableTextField;
import bluej.utility.javafx.ErrorUnderlineCanvas;
import bluej.utility.javafx.FXConsumer;
import bluej.utility.javafx.JavaFXUtil;
import bluej.utility.javafx.SharedTransition;

/**
 * A custom text box with handling for arrow keys etc. for moving between blocks/cursor (pseudo-)"modes"
 * @author Fraser McKay
 */
public abstract class TextSlot<SLOT_FRAGMENT extends TextSlotFragment> implements EditableSlot, ErrorFixListener
{
    private final List<SlotValueListener> listeners = new ArrayList<SlotValueListener>();
    protected final InteractionManager editor;
    
    // These two variables always point to the same thing, but due to Java's type
    // system, we can't declare a type for a single variable to hold them both
    protected final Frame frameParent;
    private final CodeFrame<? extends CodeElement> codeFrameParent;

    private final FrameContentRow row;
 
    private final SlotTextField field;
    private SuggestionList suggestionDisplay;
    private List<FileCompletion> fileCompletions;
    private Map<KeyCode, Runnable> fileCompletionShortcuts;
    private final CompletionCalculator completionCalculator;
    private final SimpleDoubleProperty suggestionXOffset = new SimpleDoubleProperty();
    private SLOT_FRAGMENT slotElement;
    private ErrorAndFixDisplay errorAndFixDisplay;
    
    private final List<CodeError> allErrors = new ArrayList<>();
    private final List<CodeError> shownErrors = new ArrayList<>();
    private Timer showJavadocTimer = new Timer();
    private Pane javadocDisplay;
    private long highlightChangeCounter = 0;
    private StringExpression targetType;
    private final List<Underline> underlines = new ArrayList<>();
    private final ObservableList<String> recentValues = FXCollections.observableArrayList();
    private CodeError hoverErrorCurrentlyShown;

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
        editor.setupFocusableSlotComponent(this, field.getFocusableNode(), completionCalculator != null, hints);
    }

    public class SlotTextField extends AnnotatableTextField
    {
        private String lastBeforePrefix;
        private String valueOnGain; // Value when we gained focus
        private final DoubleProperty towardsMonospaceProperty = new SimpleDoubleProperty(0.0);
        /**
         * Default constructor.
         */
        private SlotTextField(String stylePrefix, ErrorUnderlineCanvas overlay)
        {
            super(overlay);
            addStyleClasses("text-slot", stylePrefix + "text-slot");
            prefWidthProperty().set(10);
            
            /*
             * getNode().setOnKeyPressed(new EventHandler<KeyEvent>(){
                @Override
                public void handle(KeyEvent event){
                    //If 'delete'/backspace
                    if (event.getCode() == KeyCode.BACK_SPACE)
                    {
             */
            
            SuggestionListListener suggestionListener = new SuggestionListListener() {
                @Override
                public void suggestionListChoiceClicked(int highlighted)
                {
                    executeSuggestion(highlighted);
                    row.focusRight(TextSlot.this);
                }

                @Override
                public Response suggestionListKeyTyped(KeyEvent event, int highlighted)
                {
                    if (event.getCharacter().equals(" ") && completeIfPossible(highlighted))
                    {
                        row.focusRight(TextSlot.this);
                        return Response.DISMISS;
                    }
                    else if (!event.getCharacter().equals("\b"))
                        injectEvent(event);
                    return Response.CONTINUE;
                }
                
                private boolean completeIfPossible(int highlighted)
                {
                    // Pick a value if one was available to complete:
                    if (highlighted != -1)
                    {
                        return executeSuggestion(highlighted);
                    }
                    else if (suggestionDisplay.eligibleCount() == 1  && getText().length() > 0)
                    {
                        return executeSuggestion(suggestionDisplay.getFirstEligible());
                    }
                    return false;
                }

                @Override
                public Response suggestionListKeyPressed(KeyEvent event, int highlighted)
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
                            Optional<String> common = suggestionDisplay.getLongestCommonPrefix();
                            if (common.isPresent())
                            {
                                field.replaceText(getStartOfCurWord(), field.getCaretPosition(), common.get());
                            }
                            else
                            {
                                // They pressed right when no suggestions available; move to next slot.
                                row.focusRight(TextSlot.this);
                                return Response.DISMISS;
                            }
                            break;
                        case ENTER:
                            if (executeSuggestion(highlighted))
                            {
                                row.focusRight(TextSlot.this);
                                return Response.DISMISS;
                            }
                            break;
                        case ESCAPE:
                            setTransparent(false);
                            return Response.DISMISS;
                        case TAB:
                            if (event.isShiftDown())
                                row.focusLeft(TextSlot.this);
                            else
                            {
                                row.focusRight(TextSlot.this);
                                completeIfPossible(highlighted);
                            }
                            return Response.DISMISS;
                    }
                    return Response.CONTINUE;
                }

                @Override
                public void hidden()
                {
                    suggestionDisplay = null;
                    setFakeCaretShowing(false);
                }
            };
            
            //React to up/down arrows, and ENTER in the same way as tabs (move focus on)
            this.onKeyPressedProperty().set(event -> {
                    //Which key?
                    switch (event.getCode())
                    {
                        case UP:
                            if (errorAndFixDisplay != null && errorAndFixDisplay.hasFixes())
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
                            if (errorAndFixDisplay != null && errorAndFixDisplay.hasFixes())
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
                            if (getCaretPosition() == 0) {
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
                            if (fileCompletionShortcuts != null && fileCompletionShortcuts.containsKey(event.getCode()))
                            {
                                Platform.runLater(fileCompletionShortcuts.get(event.getCode()));
                            }
                            break;
                    }
            });
            
            
            
            //When focus leaves, if this is still blank, keep white. If has been filled in, blend in transparent with background.
            this.focusedProperty().addListener( (observable, oldValue, newValue) -> {
                    if (newValue)
                    {
                        valueOnGain = getText();
                        editor.beginRecordingState(TextSlot.this);
                        setTransparent(false);
                        //Stop the behaviour of selecting text when tabbing to a field:
                        //Need to wrap in runLater as selection happens after this method
                        Platform.runLater(new Runnable(){
                            @Override
                            public void run()
                            {
                                deselect();
                            }
                        });
                        showErrorAtCaret(getCaretPosition());                        
                    }
                    else {
                        setTransparent(!getText().isEmpty() && suggestionDisplay == null);
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

                //Unless still focused, go transparent
                if (!isFocused())
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
                    Platform.runLater(() -> {
                        if (suggestionDisplay != null)
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
                    editor.modifiedFrame(frameParent);
                }
            });
            
            // Autosizing the slot to fit contents:
            minWidthProperty().bind(new DoubleBinding() {
                { super.bind(textProperty());
                  super.bind(promptTextProperty());
                  super.bind(fontProperty());
                  super.bind(towardsMonospaceProperty()); }

                private String lastText;
                private double monospaceWidth;

                @Override
                protected double computeValue()
                {
                    String effectiveText = textProperty().get().length() > 0 ? textProperty().get() : promptTextProperty().get();
                    double varSpaceWidth = Math.max(10, 5 + measureString(effectiveText));

                    double m = towardsMonospaceProperty().get();
                    if (m == 1.0)
                    {
                        // If we have reached 1, our font will have switched to monospace, so just store that and we're done
                        lastText = effectiveText;
                        monospaceWidth = varSpaceWidth;
                        return varSpaceWidth;
                    }
                    else if (m == 0.0)
                    {
                        return varSpaceWidth; // Don't need to consider monospace at all
                    }
                    else
                    {
                        // Animating; need to calc width:
                        if (!effectiveText.equals(lastText))
                        {
                            lastText = effectiveText;
                            Font font = Font.font("monospace", getFont().getSize());
                            monospaceWidth = Math.max(10, 5 + measureString(lastText, font));
                        }
                        //Debug.message("Animating \"" + getText() + "\" from: " + varSpaceWidth + " to: " + monospaceWidth + " via " + m);
                        return varSpaceWidth + m * (monospaceWidth - varSpaceWidth);
                    }
                }
            });
            prefWidthProperty().bind(minWidthProperty());
            
            caretPositionProperty().addListener( (observable, oldValue, newVal) -> {
                    if (isFocused())
                        showErrorAtCaret(newVal.intValue());
                    // TODO cancel code completion if we've moved away from it
            });
            
            // runLater, to allow parent's constructor to execute:
            Platform.runLater(() -> setContextMenu(MenuItems.makeContextMenu(getMenuItems(true))));
        }

        public DoubleProperty towardsMonospaceProperty()
        {
            return towardsMonospaceProperty;
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
        
        private void updateSuggestions(boolean initialState)
        {
            String prefix = getCurWord();
            suggestionDisplay.calculateEligible(prefix, true, initialState);
            suggestionDisplay.updateVisual(prefix, false);
            lastBeforePrefix = getText().substring(0, getStartOfCurWord());
        }
        
        private void showSuggestionDisplay(SuggestionListListener listener)
        {
            suggestionXOffset.set(calculateCaretPosition(getStartOfCurWord()));
            FXConsumer<SuggestionList> handler = s ->
            {
                suggestionDisplay = s;
                updateSuggestions(true);
                suggestionDisplay.highlightFirstEligible();
                suggestionDisplay.show(field.getNode(), suggestionXOffset, field.heightProperty());
                field.setFakeCaretShowing(true);
            };
            if (Config.isGreenfoot() && getStartOfCurWord() > 0 && field.textProperty().get().charAt(getStartOfCurWord() - 1) == '\"')
            {
                // They are just inside a string; complete image file names:
                fileCompletions = editor.getAvailableFilenames();
                handler.accept(new SuggestionList(editor, Utility.mapList(fileCompletions, f -> new SuggestionDetails(f.getFile().getName(), null, f.getType(), SuggestionList.SuggestionShown.COMMON)), null, SuggestionList.SuggestionShown.RARE, TextSlot.this::previewFileCompletion, listener));
            }
            else
            {
                // TODO we shouldn't need to regen whole code repeatedly if they only modify this slot:
                editor.regenerateAndReparse(null);
                completionCalculator.withCalculatedSuggestionList(getSlotElement().getPosInSourceDoc(field.getCaretPosition()), codeFrameParent.getCode(), listener, (targetType == null || getStartOfCurWord() != 0) ? null : targetType.get(), handler);
            }
        }


        // Make this visible in this class:
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
    
    private void previewFileCompletion(Integer index)
    {
        hideJavadocDisplay();
        
        if (index >= 0)
        {
            FileCompletion fc = fileCompletions.get(index);
            javadocDisplay = new BorderPane(fc.getPreview(300, 300));
            JavaFXUtil.addStyleClass(javadocDisplay, "suggestion-file-preview");
            CodeOverlayPane.setDropShadow(javadocDisplay);
            
            fileCompletionShortcuts = fc.getShortcuts();
            editor.getCodeOverlayPane().addOverlay(javadocDisplay, field.getNode(), suggestionXOffset.subtract(suggestionDisplay.typeWidthProperty()).add(suggestionDisplay.widthProperty()), field.heightProperty());
        }
    }
    
    private synchronized void showJavadocForSuggestion(AssistContentThreadSafe ac)
    {
        showJavadocTimer.cancel();
        hideJavadocDisplay();
        highlightChangeCounter += 1;
        //TODO do the display on a timer and cancel when user moves selection        
        if (ac != null)
        {
            // Schedule the Javadoc to show after a delay, assuming they don't move before then:
            showJavadocTimer = new Timer();
            final long ourCounter = highlightChangeCounter;
            TimerTask tt = new TimerTask() { public void run() {
                // We have to do the actual manipulation on the platform thread:
                Platform.runLater(() -> { synchronized (TextSlot.this) {
                    // By now, we could be running when we no longer need the Javadoc; check that
                    // no-one has closed the display or changed the highlight in the meantime:
                    if (suggestionDisplay != null && highlightChangeCounter == ourCounter)
                    {
                        javadocDisplay = new Pane();
                        WebView webView = new WebView();
                        javadocDisplay.getChildren().add(webView);
                        String header = (ac.getType() == null ? "" : Utility.escapeAngleBrackets(ac.getType()))
                                          + " <b>" + ac.getName() + "</b>";
                        if (ac.getParams() != null)
                        {
                            header += "(" + ac.getParams().stream().map(p -> { 
                               String type = Utility.escapeAngleBrackets(p.getUnqualifiedType());
                               if (p.getFormalName() != null)
                                   return type + "&nbsp;" + p.getFormalName();
                               else
                                   return type;
                            }).collect(Collectors.joining(", ")) + ")";
                        }
                        header += "<br><br>"; // TODO make this proper HTML spacing
                        JavaFXUtil.addStyleClass(javadocDisplay, "suggestion-javadoc");
                        webView.getEngine().setJavaScriptEnabled(false);
                        // Match font in WebView with that of Label:
                        Font font = new Label().getFont();
                        String start = "<html><body style='font-family:" + font.getFamily() + ";font-size:" + font.getSize() + ";'>";
                        String end = "</body></html>";
                        String javadoc = ac.getJavadoc() != null ? ac.getJavadoc() : "";
                        webView.getEngine().loadContent(start + header + JavaUtils.javadocToHtml(javadoc.replace("\n\n", "<br><br>")) + end);
                        
                        webView.setMaxWidth(400);
                        webView.setMaxHeight(300);
                        // Workaround to get transparent background, from:
                        // http://stackoverflow.com/questions/12421250/transparent-background-in-the-webview-in-javafx
                        webView.setBlendMode(BlendMode.DARKEN);
                        
                        CodeOverlayPane.setDropShadow(javadocDisplay);
                        
                        editor.getCodeOverlayPane().addOverlay(javadocDisplay, field.getNode(), suggestionXOffset.subtract(suggestionDisplay.typeWidthProperty()).add(suggestionDisplay.widthProperty()), field.heightProperty());
                    }
                }});
            }};
            showJavadocTimer.schedule(tt, 500);
        }
    }

    private synchronized void hideJavadocDisplay()
    {
        if (javadocDisplay != null)
        {
            editor.getCodeOverlayPane().removeOverlay(javadocDisplay);
            fileCompletionShortcuts = null;
            javadocDisplay = null;
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

    @Override
    public void addError(CodeError err)
    {
        allErrors.add(err);
        err.bindFresh(getFreshExtra(err).or(getParentFrame().freshProperty()));
        recalculateShownErrors();
    }

    protected BooleanExpression getFreshExtra(CodeError err)
    {
        return new ReadOnlyBooleanWrapper(false);
    }

    @Override
    public void flagErrorsAsOld()
    {
        allErrors.forEach(CodeError::flagAsOld);
    }
    
    @Override
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
        
        for (CodeError e : sortedErrors)
        {
            // Add the error if it doesn't overlap:
            if (shownErrors.stream().allMatch(shown -> !shown.overlaps(e)))
            {
                shownErrors.add(e);
            }
        }
        
        field.clearErrorMarkers(this);
        shownErrors.forEach(e -> field.drawErrorMarker(this, e.getStartPosition(), e.getEndPosition(), e.isJavaPos(), b -> showErrorHover(b ? e : null), e.visibleProperty()));
        
        if (field.isFocused())
            showErrorAtCaret(field.getCaretPosition());
    }

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
                hoverErrorCurrentlyShown = error; //update current error
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
                errorAndFixDisplay.hide();
                errorAndFixDisplay = null;
            }
        }
        field.clearErrorMarkers(this);
    }

    public void replace(int startPosInSlot, int endPosInSlot, String replacement)
    {
        String before = getText().substring(0, startPosInSlot);
        String after = getText().substring(endPosInSlot);
        setText(before + replacement + after);
        field.positionCaret(before.length() + replacement.length());
    }

    @Override
    public void fixedError(CodeError err)
    {
        allErrors.remove(err);
        recalculateShownErrors();
    }

    // Returns true if should be dismissed
    private boolean executeSuggestion(int highlighted)
    {
        if (fileCompletions != null && highlighted != -1)
        {
            FileCompletion fc = fileCompletions.get(highlighted);
            field.replaceText(getStartOfCurWord(), field.getCaretPosition(), fc.getFile().getName());
            if (field.getCaretPosition() == field.getLength() || field.textProperty().get().charAt(field.getCaretPosition()) != '\"')
            {
                field.replaceText(field.getCaretPosition(), field.getCaretPosition(), "\"");
            }
            return true;
        }
        else
        {
            return field.executeCompletion(completionCalculator, highlighted, getStartOfCurWord());
        }
    }

    private void hideSuggestionDisplay__()
    {
        // Cancel timer first, to make sure it is not making the display at the same time:
        showJavadocTimer.cancel();
        hideJavadocDisplay();
        fileCompletions = null;
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
    
    public void bindTargetType(StringExpression targetTypeBinding)
    {
        this.targetType = targetTypeBinding;
    }

    public void setTargetType(String targetType)
    {
        this.targetType = new SimpleStringProperty(targetType);        
    }

    public Stream<CodeError> getCurrentErrors()
    {
        return shownErrors.stream();
    }
    /*
    @Override
    public Stream<EditableSlot> getHeaderItems()
    {
        return Stream.of(this);
    }
    */
    
    public void addUnderline(Underline u)
    {
        underlines.add(u);
        drawUnderlines();
    }
    
    public void removeAllUnderlines()
    {
        underlines.clear();
        drawUnderlines();
    }

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
        
    protected Map<TopLevelMenu, MenuItems> getExtraContextMenuItems()
    {
        return Collections.emptyMap();
    }

    @Override
    public final Map<TopLevelMenu, MenuItems> getMenuItems(boolean contextMenu) {
        Map<TopLevelMenu, MenuItems> itemMap = new HashMap<>(getExtraContextMenuItems());
        final ObservableList<SortedMenuItem> menuItems = FXCollections.observableArrayList();
        if (contextMenu)
        {
            menuItems.add(getRecentValuesMenu());
        }
        final MenuItem cutItem = JavaFXUtil.makeMenuItem(Config.getString("frame.slot.cut"), field::cut, new KeyCodeCombination(KeyCode.X, KeyCodeCombination.SHORTCUT_DOWN));
        final MenuItem copyItem = JavaFXUtil.makeMenuItem(Config.getString("frame.slot.copy"), field::copy, new KeyCodeCombination(KeyCode.C, KeyCodeCombination.SHORTCUT_DOWN));
        final MenuItem pasteItem = JavaFXUtil.makeMenuItem(Config.getString("frame.slot.paste"), field::paste, new KeyCodeCombination(KeyCode.V, KeyCodeCombination.SHORTCUT_DOWN));
        menuItems.addAll(
            MenuItemOrder.CUT.item(cutItem),
            MenuItemOrder.COPY.item(copyItem),
            MenuItemOrder.PASTE.item(pasteItem));
        itemMap.put(TopLevelMenu.EDIT, MenuItems.concat(
                new MenuItems(menuItems) {

            @Override
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

    private SortedMenuItem getRecentValuesMenu()
    {
        final Menu recent = new Menu(Config.getString("frame.slot.recent"));
        recent.setDisable(true);
        recentValues.addListener((ListChangeListener)c -> {
            recent.getItems().clear();
            if (recentValues.isEmpty())
            {
                recent.setDisable(true);
            } else
            {
                recent.setDisable(false);
                recentValues.forEach(v -> {
                    MenuItem item = new MenuItem(v);
                    item.setOnAction(e -> setText(v));
                    recent.getItems().add(item);
                });
            }
        });
        return MenuItemOrder.RECENT_VALUES.item(recent);
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
}