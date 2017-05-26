/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2015,2016  Michael Kolling and John Rosenberg
 
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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Label;

import bluej.stride.generic.Frame;
import bluej.stride.generic.InteractionManager;
import bluej.utility.Utility;
import bluej.utility.javafx.FXRunnable;
import bluej.utility.javafx.HangingFlowPane;
import bluej.utility.javafx.JavaFXUtil;
import bluej.utility.javafx.SharedTransition;

/**
 * Like SlotLabel, but makes each word a separate Label, to support wrapping in the flow pane.
 * Thus WrappableSlotLabel is one HeaderItem, but contains many Nodes (each one a Label).
 */
public class WrappableSlotLabel implements HeaderItem, CopyableHeaderItem
{
    /** 
     * The list of CSS style classes to apply to each Label.
     * Observable so that we can listen to it, and update each Label when it's changed.
     */ 
    private final ObservableList<String> styleClasses = FXCollections.observableArrayList("wrappable-slot-label");
    /**
     * The list of Label items being used for display (one per word).
     */
    private final ObservableList<Label> words = FXCollections.observableArrayList();
    /**
     * Each Label in "words" listens to the "styleClasses" list for changes.  When
     * we change the words, we must also remember to unbind/cancel this listening process,
     * which is why we must keep this list of actions to run in order to do the unbinding.
     * Should be one-to-one mapping between items in "words" and items in "unbinds"
     */
    
    private final List<FXRunnable> unbinds = new ArrayList<>();
    /**
     * The alignment of all the words within the flow pane.  Using this, you can align
     * the whole set of labels to the left, or to the right.
     */
    private HangingFlowPane.FlowAlignment alignment;

    /**
     * Creates a wrappable label with the given text.  See setText for semantics.
     */
    public WrappableSlotLabel(String fullText)
    {
        setText(fullText);
    }

    /**
     * Uses the given text for the wrappable label, by splitting it into words and putting
     * one label in each word.  The splitting is done using String.split(" ").  Thus multiple
     * consecutive spaces are treated as one, but tabs or newlines are ignored for splitting
     * purposes, and no splitting is done around any other punctuation.
     * 
     * Note that no check is performed in this method for being equivalent to the current
     * contents; a complete update is always performed.
     */
    public void setText(String fullText)
    {
        // Words is observable, so just altering it will automatically update display.
        words.clear();
        unbinds.forEach(FXRunnable::run);
        unbinds.clear();
        boolean first = true;
        for (String word : fullText.split(" "))
        {
            // If they setText to blank, we will get one blank in result, but ignore it:
            if (!word.equals(""))
            {
                Label l = new Label(word);
                l.setMinWidth(0.0);
                unbinds.add(JavaFXUtil.bindList(l.getStyleClass(), styleClasses));
                JavaFXUtil.setPseudoclass("bj-first", first, l);
                first = false;
                words.add(l);
            }
        }

        // Make sure alignment is set right on all words:
        setAlignment(this.alignment);
    }

    /**
     * Adds a style-class to all Labels
     */
    public void addStyleClass(String styleClass)
    {
        if (!styleClasses.contains(styleClass))
            styleClasses.add(styleClass);
    }

    @Override
    public EditableSlot asEditable()
    {
        return null;
    }

    @Override
    public ObservableList<? extends Node> getComponents()
    {
        return words;
    }

    @Override
    public void setView(Frame.View oldView, Frame.View newView, SharedTransition animate)
    {
        // TODO do we need to do anything here?
    }

    /**
     * Fades the opacity of all labels with
     * the opposite progress of "animate" (i.e. from 1 to 0)
     * @param setInvisible says whether to set the visible property to false afterwards
     *                     setInvisible==true means call setVisible(false)
     *                     setInvisible==false means don't call anything.
     */
    public void fadeOut(SharedTransition animate, boolean setInvisible)
    {
        for (Label l : words)
        {
            l.opacityProperty().bind(animate.getOppositeProgress());
        }
        animate.addOnStopped(() -> {
            for (Label l : words)
            {
                l.opacityProperty().unbind();
                if (setInvisible)
                    l.setVisible(false);
            }
        });
    }

    /**
     * Sets all Labels to visible and fades their opacity with
     * the progress of "animate" (i.e. from 0 to 1)
     */
    public void fadeIn(SharedTransition animate)
    {
        for (Label l : words)
        {
            l.setVisible(true);
            l.opacityProperty().bind(animate.getProgress());
        }
        animate.addOnStopped(() -> {
            for (Label l : words)
            {
                l.opacityProperty().unbind();
            }
        });
    }


    public void setAlignment(HangingFlowPane.FlowAlignment alignment)
    {
        this.alignment = alignment;
        words.forEach(l -> HangingFlowPane.setAlignment(l, alignment));
    }

    @Override
    public Stream<Node> makeDisplayClone(InteractionManager editor)
    {
        List<Node> copies = Utility.mapList(words, l -> JavaFXUtil.cloneLabel(l, editor.getFontCSS()));
        copies.forEach(n -> HangingFlowPane.setAlignment(n, alignment));
        return copies.stream();
    }
}
