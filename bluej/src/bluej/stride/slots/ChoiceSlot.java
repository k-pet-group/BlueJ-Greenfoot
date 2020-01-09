/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2016,2018,2019,2020 Michael Kölling and John Rosenberg
 
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
package bluej.stride.slots;


import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import bluej.editor.fixes.SuggestionList;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.BoundingBox;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.IndexRange;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

import bluej.editor.stride.FrameCatalogue;
import bluej.stride.framedjava.ast.JavaFragment;
import bluej.stride.framedjava.ast.links.PossibleLink;
import bluej.stride.framedjava.errors.CodeError;
import bluej.stride.framedjava.slots.TextOverlayPosition;
import bluej.stride.generic.Frame;
import bluej.stride.generic.Frame.View;
import bluej.stride.generic.FrameContentRow;
import bluej.stride.generic.InteractionManager;
import bluej.editor.fixes.SuggestionList.SuggestionDetails;
import bluej.editor.fixes.SuggestionList.SuggestionListListener;
import bluej.utility.Utility;
import bluej.utility.javafx.ErrorUnderlineCanvas;
import bluej.utility.javafx.JavaFXUtil;
import bluej.utility.javafx.SharedTransition;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A choice slot has three overlaid elements (not counting the error underline) that make up
 * the display.  There is curDisplay, a Label which shows, in black, the text typed so far by the user
 * since they entered the slot.  Exactly underneath that is futureDisplay, which shows in cyan what
 * the current completion would be if selected.  There is also dummyField, which serves to show a cursor,
 * but is always empty.
 * 
 * You might think we could combine curDisplay and dummyField, but we don't want the cursor to be able
 * to leave the end of the line; editing the start of a choice slot makes very little sense, especially
 * since it effectively blanks when you enter it.
 */
public class ChoiceSlot<T extends Enum<T>> implements EditableSlot, CopyableHeaderItem
{
    private final InteractionManager editor;
    private final Frame parentFrame;
    private final FrameContentRow row;
    private final ObjectProperty<SuggestionList> dropdown = new SimpleObjectProperty<>(null);
    private final List<T> choices;
    private T previousSelection;
    private T selection;
    private final StackPane pane; // The GUI element that encompasses the whole slot
    private final SlotLabel curDisplay; // The actual current value
    private final Label futureDisplay; // The grey version of what would be completed
    private final DummyTextField dummyField; // An empty text field, just used to show a cursor and handle input
    private final ErrorUnderlineCanvas errorMarker;
    private Function<T, Boolean> isValid;
    // We must keep a reference to this to avoid problems with GC and weak listeners:
    private final BooleanBinding effectivelyFocusedProperty;

    public ChoiceSlot(final InteractionManager editor, Frame parentFrame, FrameContentRow row, final List<T> choices, Function<T, Boolean> isValid, final String stylePrefix, Map<T, String> hints)
    {
        this.editor = editor;
        this.parentFrame = parentFrame;
        this.row = row;
        this.choices = choices;
        this.isValid = isValid;
        dummyField = new DummyTextField();
        curDisplay = new SlotLabel("");
        futureDisplay = new Label();
        pane = new StackPane();
        errorMarker = new ErrorUnderlineCanvas(pane);
        //completeDisplay goes first because it must be underneath curDisplay, which must be underneath the dummy text field 
        pane.getChildren().addAll(futureDisplay, curDisplay.getNode(), dummyField, errorMarker.getNode());
        //curDisplay must be exactly on top of futureDisplay to get right visual effect: 
        StackPane.setAlignment(curDisplay.getNode(), Pos.CENTER_LEFT);
        StackPane.setAlignment(futureDisplay, Pos.CENTER_LEFT);
                
        editor.setupFocusableSlotComponent(this, dummyField, false, row::getExtensions, hints.entrySet().stream().map(e -> new FrameCatalogue.Hint(e.getKey().toString(), e.getValue())).collect(Collectors.toList()));
        
        pane.getStyleClass().addAll("choice-slot", stylePrefix + "choice-slot");
        
        JavaFXUtil.addStyleClass(curDisplay, "choice-current", stylePrefix + "choice-current");
        JavaFXUtil.addStyleClass(futureDisplay, "choice-future", stylePrefix + "choice-future");
        JavaFXUtil.addStyleClass(dummyField, "choice-dummy", stylePrefix + "choice-dummy");
        
        pane.setOnMouseClicked(e -> {
            if (!dummyField.isDisabled()) {
                dummyField.requestFocus();
                // Only consume if it's enabled (and the click will do something):
                e.consume();
            }
        });

        JavaFXUtil.addFocusListener(dummyField, focused -> {
            if (focused)
            {
                // We must show the dropdown in a runLater, as otherwise we get into weird
                // loops as focus notifications are sent across the windows when the user
                // clicks somewhere while the dropdown is displaying.
                JavaFXUtil.runAfterCurrent(() -> {
                    if (dummyField.isFocused())
                    {
                        editor.beginRecordingState(ChoiceSlot.this);
                        curDisplay.setText("");
                        previousSelection = selection;
                        selection = null;
                        showSuggestions(previousSelection);
                        JavaFXUtil.setPseudoclass("bj-transparent", false, pane);
                    }
                });
            }
        });
        
        DoubleBinding calcWidth = new DoubleBinding() {
            { super.bind(curDisplay.fontProperty());
              super.bind(curDisplay.textProperty());
              super.bind(futureDisplay.fontProperty());
              super.bind(futureDisplay.textProperty());
            }

            @Override
            protected double computeValue()
            {
                return Math.max(10, Math.max(curDisplay.measureString(curDisplay.getText()),
                                             JavaFXUtil.measureString(futureDisplay, futureDisplay.getText())));
            }  
        };
        curDisplay.prefWidthProperty().bind(calcWidth);
        futureDisplay.prefWidthProperty().bind(calcWidth);
        dummyField.prefWidthProperty().bind(calcWidth);
        dummyField.translateXProperty().bind(new DoubleBinding() {
            { super.bind(curDisplay.fontProperty());
              super.bind(curDisplay.textProperty());
            }

            @Override
            protected double computeValue()
            {
                return curDisplay.measureString(curDisplay.getText());
            }            
        });
        curDisplay.minWidthProperty().set(Region.USE_PREF_SIZE);
        futureDisplay.setMinWidth(Region.USE_PREF_SIZE);
        dummyField.setMinWidth(Region.USE_PREF_SIZE);
        pane.heightProperty().addListener((a, b, c) -> JavaFXUtil.runNowOrLater(() -> refreshError()));
        pane.widthProperty().addListener((a, b, c) -> JavaFXUtil.runNowOrLater(() -> refreshError()));

        effectivelyFocusedProperty = dummyField.focusedProperty().or(dropdown.isNotNull());

        setValue(null);
    }
    
    @OnThread(Tag.FXPlatform)
    class ChoiceSuggestionListener implements SuggestionListListener
    {
        public void suggestionListChoiceClicked(SuggestionList suggestionList, int highlighted)
        {
            if (highlighted != -1)
                setValue(choices.get(highlighted));
            editor.endRecordingState(ChoiceSlot.this);
            row.focusRight(ChoiceSlot.this);
        }
        
        @Override
        public Response suggestionListKeyTyped(SuggestionList suggestionList, KeyEvent event, int highlighted)
        {
            if (event.getCharacter().equals(" "))
            {
                Optional<T> completion = getCompletion(highlighted);
                if (completion.isPresent())
                {
                    setValue(completion.get());
                    row.focusRight(ChoiceSlot.this);
                    return Response.DISMISS;
                }
                else
                {
                    // Ignore the space, and continue:
                    return Response.CONTINUE;
                }
            }
            else
            {
                dummyField.fireEvent(event.copyFor(null, dummyField));
                return Response.CONTINUE;
            }
        }
        

        @Override
        public Response suggestionListKeyPressed(SuggestionList suggestionList, KeyEvent event, int highlighted)
        {
            switch (event.getCode())
            {
                case ENTER:
                    row.focusRight(ChoiceSlot.this);
                    suggestionListFocusStolen(highlighted);
                    return Response.DISMISS;
                case ESCAPE:
                    setValue(previousSelection);
                    row.focusRight(ChoiceSlot.this);
                    return Response.DISMISS;
                case LEFT:
                    row.focusLeft(ChoiceSlot.this);
                    suggestionListFocusStolen(highlighted);
                    return Response.DISMISS;
                case RIGHT:
                    row.focusRight(ChoiceSlot.this);
                    suggestionListFocusStolen(highlighted);
                    return Response.DISMISS;
                case TAB:
                    if (event.isShiftDown())
                        row.focusLeft(ChoiceSlot.this);
                    else
                        row.focusRight(ChoiceSlot.this);
                    suggestionListFocusStolen(highlighted);
                    return Response.DISMISS;
                default:
                    return Response.CONTINUE;
            }
        }

        @Override
        public void hidden()
        {
            JavaFXUtil.setPseudoclass("bj-transparent", true, pane);
            editor.endRecordingState(ChoiceSlot.this);
            dropdown.set(null);
        }
        
        private Optional<T> getCompletion(int highlighted)
        {
            // Pick a value if one was available to complete:
            if (highlighted != -1)//&& curDisplay.getText().length() > 0)
            {
                return Optional.of(choices.get(highlighted));
            }
            else if (dropdown.get().eligibleCount() == 1  && curDisplay.getText().length() > 0)
            {
                return Optional.of(choices.get(dropdown.get().getFirstEligible()));
            }
            return Optional.empty();
        }

        @Override
        public void suggestionListFocusStolen(int highlighted)
        {
            // Pick a value if one was available to complete:
            Optional<T> completion = getCompletion(highlighted);
            if (completion.isPresent())
            {
                setValue(completion.get());
            }
            else
            {
                setValue(previousSelection);
            }
        }
    }
    
    /**
     * Shows the suggestions dropdown, and highlights the given item (null means no highlight)
     */
    @OnThread(Tag.FXPlatform)
    public void showSuggestions(T curHighlight)
    {
        dropdown.set(
            new SuggestionList(editor,
                Utility.mapList(choices, t -> new SuggestionDetails(t.toString())), null,
                SuggestionList.SuggestionShown.RARE, i -> {
                    i = i < 0 ? 0 : i; futureDisplay.setText(choices.get(i).toString());
                },
                new ChoiceSuggestionListener())
            );
                
        dropdown.get().show(pane, new BoundingBox(0, 0, 0, pane.heightProperty().get()));
        
        dropdown.get().calculateEligible(curDisplay.getText(), false, false);
        dropdown.get().setHighlighted(curHighlight == null ? -1 : choices.indexOf(curHighlight), true);
        // Must come after we've set highlight:
        dropdown.get().updateVisual(curDisplay.getText());
    }

    /**
     * If no selection has been made, defaultVal is returned.  Usually
     * you don't want a null value, you want some suitable empty value.
     */
    public T getValue(T defaultVal)
    {
        if (selection == null)
            return defaultVal;
        else
            return selection;
    }
    
    public void setValue(T value)
    {
        selection = value;
        curDisplay.setText(value == null ? "" : value.toString());
        futureDisplay.setText(curDisplay.getText());
        JavaFXUtil.runNowOrLater(() -> refreshError());
        JavaFXUtil.setPseudoclass("bj-transparent", isValid.apply(selection) && !dummyField.isFocused(), pane);
        editor.modifiedFrame(parentFrame, false);
    }

    @OnThread(Tag.FXPlatform)
    private void refreshError()
    {
        if (!isValid.apply(selection))
        {
            // Work around for combination of https://javafx-jira.kenai.com/browse/RT-32242
            // and https://javafx-jira.kenai.com/browse/RT-37434  Without this if, an error marker
            // drawn on a zero-height Canvas is later visible!
            if (errorMarker.getHeight() > 0.0)
            {
                errorMarker.addErrorMarker(this, 0, Integer.MAX_VALUE, false, b -> {}, new ReadOnlyBooleanWrapper(true));
            }
        }
        else
        {
            errorMarker.clearErrorMarkers(this);
        }
    }
    
    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    private class DummyTextField extends TextField
    {
        @OnThread(Tag.FX)
        public DummyTextField()
        {
            // Nothing to do.
        }
        
        @Override
        public void appendText(String s)
        {
            insertText(0, s);
        }

        @Override
        public void clear()
        {
            update("");
        }

        @Override
        public void copy()
        {
            ClipboardContent c = new ClipboardContent();
            c.putString(futureDisplay.getText());
            Clipboard.getSystemClipboard().setContent(c);
        }

        @Override
        public void cut()
        {
            copy();
            dropdown.get().setHighlighted(-1, false);
            update("");
        }

        @Override
        public boolean deletePreviousChar()
        {
            if (curDisplay.getText().length() > 0) {
                update(curDisplay.getText().substring(0, Math.max(0, curDisplay.getText().length() - 1)));
                return true;
            }
            return false;
        }

        @Override
        public void insertText(int pos, String s)
        {
            if (pos != 0)
                throw new IllegalStateException();
            else
                update(curDisplay.getText() + s);
        }

        @Override
        public void paste() {
            String clip = Clipboard.getSystemClipboard().getString();
            if (clip != null && !clip.equals(""))
                update(clip);
        }
        
        @Override
        public void replaceSelection(String s)
        {
            appendText(s);
        }
        
        @Override
        public void replaceText(IndexRange arg0, String s)
        {
            update(curDisplay.getText() + s);
        }

        @Override
        public void replaceText(int arg0, int arg1, String s)
        {
            update(curDisplay.getText() + s);
        }

        private void update(String newVal)
        {
            dropdown.get().calculateEligible(newVal, false, false);
            if (dropdown.get().eligibleCount() == 0)
            {
                //Invalid input; does not further complete any possible completions.  Rollback:
                dropdown.get().calculateEligible(curDisplay.getText(), false, false);
            }
            else
            {
                curDisplay.setText(newVal);
                dropdown.get().updateVisual(newVal);
            }
        }
    }

    @Override
    public ObservableList<Node> getComponents()
    {
        return FXCollections.observableArrayList((Node)pane);
    }

    @Override
    public void requestFocus(Focus on)
    {
        // The on parameter makes no difference in a choice field
        dummyField.requestFocus();
        
        // TODO if decided the place of the caret is important, replace the previous line with the next.
        /*
        if (on == Focus.LEFT)
            dummyField.positionCaret(0);
        else if (on == Focus.RIGHT)
            dummyField.positionCaret(dummyField.getLength());
        else if (on == Focus.SELECT_ALL)
            dummyField.selectAll();
         */
    }
    
    public void requestFocus()
    {
        // The on parameter makes no difference in a choice field
        requestFocus(null);
    }
    
    @Override
    public boolean isFocused()
    {
        return dummyField.isFocused();
    }

    public void flagErrorsAsOld()
    {
        // TODO 
    }
    
    public void removeOldErrors()
    {
        // TODO
    }

    public void cleanup()
    {
    }

    @Override
    public int getFocusInfo()
    {
        return -1; // No info of interest
    }

    @Override
    public Node recallFocus(int info)
    {
        requestFocus();
        return dummyField;
    }

    public Stream<CodeError> getCurrentErrors()
    {
        // TODO 
        return Stream.empty();
    }


    public Node getPrimaryFocus()
    {
        return dummyField;
    }


    @Override
    public TextOverlayPosition getOverlayLocation(int caretPos, boolean javaPos)
    {
        return TextOverlayPosition.nodeToOverlay(pane, 0.0, 0.0, curDisplay.fontProperty().get().getSize(), pane.getHeight());
    }


    @Override
    public void addError(CodeError err)
    {
        //TODO
    }
    
    @Override
    public void focusAndPositionAtError(CodeError err)
    {
        // Nothing to do: only one position in the slot when focused
    }


    @Override
    public void addUnderline(Underline u)
    {
    }


    @Override
    public void removeAllUnderlines()
    {
    }


    @Override
    public void saved()
    {        
    }

    @Override
    public List<? extends PossibleLink> findLinks()
    {
        // No links in a choice slot        
        return Collections.emptyList();
    }
    
    public void lostFocus()
    {        
    }
    
    public Frame getParentFrame()
    {
        return parentFrame;
    }
    
    public void setView(View oldView, View newView, SharedTransition animate)
    {
        dummyField.setDisable(newView != View.NORMAL);

        curDisplay.setView(oldView, newView, animate);
        if (newView != View.JAVA_PREVIEW)
        {
            animate.addOnStopped(() -> {
                curDisplay.minWidthProperty().set(Region.USE_PREF_SIZE);
                futureDisplay.setOpacity(1.0);
            });
        }
        else
        {
            futureDisplay.setOpacity(0.0);
        }
    }

    @Override
    public JavaFragment getSlotElement()
    {
        return null;
    }

    @Override
    public boolean isAlmostBlank()
    {
        return true; // Choice slots tend to not be actually empty, and they are easy to put a value in, so we count them as always near-blank
    }

    @Override
    public boolean isEditable()
    {
        return !dummyField.disableProperty().get();
    }

    @Override
    public void setEditable(boolean editable)
    {
        dummyField.setDisable(!editable);
        pane.setDisable(!editable);
    }

    @Override
    public Stream<? extends Node> makeDisplayClone(InteractionManager editor)
    {
        Stream<Label> labelClone = curDisplay.makeDisplayClone(editor);
        // Should only be one node anyway
        StackPane clone = new StackPane();
        clone.getChildren().addAll(labelClone.peek(l -> {
            l.setAlignment(Pos.CENTER_LEFT);
            l.prefWidthProperty().bind(curDisplay.prefWidthProperty());
        }).collect(Collectors.toList()));
        JavaFXUtil.bindList(clone.getStyleClass(), pane.getStyleClass());
        JavaFXUtil.bindPseudoclasses(clone, pane.getPseudoClassStates());
        clone.minHeightProperty().bind(pane.heightProperty());
        clone.minWidthProperty().bind(pane.widthProperty());
        clone.prefHeightProperty().bind(pane.heightProperty());
        return Stream.of(clone);
    }

    @Override
    public ObservableBooleanValue effectivelyFocusedProperty()
    {
        return effectivelyFocusedProperty;
    }

    @Override
    public int calculateEffort()
    {
        // Not much effort to select choice, and often left as-is; approximate as one keypress:
        return 1;
    }
}
