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
package bluej.stride.framedjava.errors;

import java.util.List;

import bluej.editor.fixes.FixSuggestion;
import bluej.utility.javafx.JavaFXUtil;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.scene.Node;
import threadchecker.OnThread;
import threadchecker.Tag;
import bluej.stride.framedjava.ast.JavaFragment;
import bluej.stride.generic.InteractionManager;

/**
 * The base class of errors shown in the Stride editor.  There are two subclasses,
 * one (JavaCompileError) for Java compiler errors which result from the generated
 * Java code, and another (DirectSlotError) for errors which we generate directly
 * from Stride before generating the Java.
 */
public abstract class CodeError
{
    /** Flag to keep track of whether we've been flagged as old.  This is a simple
     * mechanism where errors are flagged as old (i.e. from previous compile) then
     * later all old errors are removed.
     */
    private boolean flaggedAsOld = false;
    /** A property to keep track of whether this error is currently focused
     *  (i.e. whether the message and fix display are showing). */
    private final BooleanProperty focusedProperty = new SimpleBooleanProperty(false);
    /** The slot which this error pertains to.  Cannot be null. */
    protected final JavaFragment relevantSlot;
    
    /**
     * A property to keep track of whether the error is attached to a fresh frame.
     * Errors should not be shown on frames until they become non-fresh.
     */ 
    private final BooleanProperty freshProperty = new SimpleBooleanProperty(false);

    // To avoid the weak-reference-listener issue with JavaFX, keep a strong reference
    // to the complete of the freshProperty binding:
    private BooleanBinding staleBinding = freshProperty.not(); 
    
    /** A property to keep track of whether the error indicator (i.e. the red underline)
     *  is currently showing for this error, or would be if the frame was non-fresh. */
    private final BooleanProperty showingIndicatorProperty = new SimpleBooleanProperty(false);
    /** An expression for whether the red underline is actually drawn: requires the
     *  attached frame to be non-fresh, and for the showingIndicatorProperty to be true
     *  (i.e. for us to not be overlapped by another red underline error which takes precedence.)
     */
    private final BooleanExpression visible;
    /**
     * The XML xpath for this error, used for data recording.
     */
    @OnThread(value = Tag.Any, requireSynchronized = true)
    protected String path;
    /**
     * The error identifier, for data recording purposes.
     */
    private final int identifier;

    /**
     * Creates a new CodeError (only called by subclasses, since we are an abstract class).
     *
     * Note for data recording: callers of this constructor are responsible for calling
     * visibleProperty().get() after this constructor has complete, and if and only if
     * the value is true, they should record the error.  If the value is false, this constructor
     * is responsible for installing a listener to record the error later on.
     *
     * @param code The fragment which this error is attached to.
     * @param errorIdentifier The identifier of the error (only for data recording purposes)
     */
    @OnThread(Tag.FXPlatform)
    protected CodeError(JavaFragment code, int errorIdentifier)
    {
        if (code == null)
            throw new IllegalArgumentException("Slot for error cannot be null");
        this.identifier = errorIdentifier;
        relevantSlot = code;
        // These parts must be run on the FX thread:
        visible = staleBinding.and(showingIndicatorProperty);
        code.addError(this);
    }

    /**
     * Flag the error as old.  From now on, isFlaggedAsOld will return true.
     */
    public void flagAsOld()
    {
        flaggedAsOld = true;
    }

    /**
     * Check if the error has been flagged as old (i.e. from a compilation which
     * is no longer the latest compilation).
     *
     * @return true if flagAsOld has ever been called on this object.
     */
    public boolean isFlaggedAsOld()
    {
        return flaggedAsOld;
    }

    /**
     * Whether the positions (as returned by getStartPosition and getEndPosition)
     * are in the Java code (as they will be for a javac compiler error) or in
     * the original Stride.  This matters, for example, if the code features
     * a range or an instanceof, which will occupy different numbers of characters
     * in Stride or in Java expressions.  Thus we need to know which side of
     * the Stride->Java transition these positions come from.
     *
     * @return True if the positions relate to Java code, false if it relates to Stride.
     */
    public abstract boolean isJavaPos();

    /** Gets the text of the error message */
    @OnThread(Tag.Any)
    public abstract String getMessage();

    /**
     * Gets the list of quick-fix suggestions for this error.  May be empty list
     * if none available, will not be null.
     */
    public abstract List<? extends FixSuggestion> getFixSuggestions();
    
    /**
     * Gets the start position relative to slot in characters (0 is before first char)
     */
    public int getStartPosition()
    {
        return 0;
    }

    /**
     * Gets the end position relative to slot in characters (1 is after first char).
     *
     * The special value Integer.MAX_VALUE indicates the error extends to the end of the whole slot.
     */
    public int getEndPosition()
    {
        return Integer.MAX_VALUE;
    }

    /**
     * Only call this for two errors which refer to the same slot.
     * This forms a partial ordering.
     * Returns -1 if this error is more specific than the other,
     *   either because they overlap,
     *   and this is detected pre-compilation (and the other is a compile error)
     *   or they are detected at the same stage and this refers to an earlier or smaller region
     */
    public static int compareErrors(CodeError a, CodeError b)
    {
        final boolean aIsCompile = a instanceof JavaCompileError;
        final boolean bIsCompile = b instanceof JavaCompileError;
        
        final boolean overlap = a.overlaps(b);
        final boolean aIsSmallerOrEqual = a.getEndPosition() - a.getStartPosition() <= b.getEndPosition() - b.getStartPosition(); 

        if (aIsCompile && !bIsCompile) {
            return 1;
        }
        if (!aIsCompile && bIsCompile) {
            return -1;
        }
        if (aIsSmallerOrEqual) {
            return -1;
        }
        return 1;
    }

    /**
     * Checks whether this error overlaps that error, i.e. whether their red underlines
     * would overlap or meet.  Only valid if the two errors refer to the same slot, and
     * are both Java errors or both Stride errors.
     */
    public boolean overlaps(CodeError e)
    {
     // See http://stackoverflow.com/questions/3269434/whats-the-most-efficient-way-to-test-two-integer-ranges-for-overlap
        return getStartPosition() <= e.getEndPosition() && e.getStartPosition() <= getEndPosition();
    }

    /**
     * Moves focus to the error (i.e. plants the text cursor into the red underline and focuses
     * the slot)
     * @param editor The editor which the slot lies in.
     */
    public void jumpTo(InteractionManager editor)
    {
        Node n = getRelevantNode();
        if (n != null) {
            editor.scrollTo(n, -100);
            relevantSlot.getErrorShower().focusAndPositionAtError(this);
        }
    }

    /**
     * The graphical node for the slot which this error relates to.
     * @return The node, or null if it cannot be calculated.
     */
    public final Node getRelevantNode()
    {
        if (relevantSlot.getErrorShower() == null)
            return null;
        return relevantSlot.getErrorShower().getRelevantNodeForError(this);
    }

    /**
     * The property tracking whether the error is focused (i.e. whether
     * the error display and optional quick fixes is showing)
     */
    public BooleanProperty focusedProperty()
    {
        return focusedProperty;
    }

    /**
     * The read-only property tracking if the error underline is currently visible.
     * See documentation for the visible field.
     */
    public ObservableBooleanValue visibleProperty()
    {
        return visible;
    }

    /**
     * Sets whether the indicator is showing (ignores fresh/non-fresh) state, i.e.
     * whether this error is not overlapped, or had the highest precedence of a set of overlapped errors.
     */
    public void setShowingIndicator(boolean showing)
    {
        showingIndicatorProperty.set(showing);
    }

    /**
     * Binds the fresh state of this error to the given observable value.
     *
     * When the error indicator is visible, and the given fresh property is false,
     * records the shown-error-indicator event with the editor.
     */
    @OnThread(Tag.FXPlatform)
    public void bindFresh(ObservableBooleanValue fresh, InteractionManager editor)
    {
        freshProperty.bind(fresh);
        if (!visibleProperty().get())
        {
            // Add a listener to send an event when we become visible:
            JavaFXUtil.listenOnce(visibleProperty(), vis -> {
                if (vis) // Should always be true, but check anyway
                {
                    editor.recordErrorIndicatorShown(getIdentifier());
                }
            });
        }
        else
        {
            editor.recordErrorIndicatorShown(getIdentifier());
        }
    }

    /**
     * Gets the identifier of the error, for data recording purposes.
     */
    @OnThread(Tag.Any)
    public final int getIdentifier()
    {
        return identifier;
    }

    /**
     * Gets the XML xpath of the error, for data recording purposes.
     * @param path
     */
    @OnThread(Tag.Any)
    public synchronized void recordPath(String path)
    {
        this.path = path;
    }
}
