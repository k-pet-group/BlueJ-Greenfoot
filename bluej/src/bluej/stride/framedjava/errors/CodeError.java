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
package bluej.stride.framedjava.errors;

import java.util.List;

import bluej.stride.slots.EditableSlot;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.scene.Node;
import threadchecker.OnThread;
import threadchecker.Tag;
import bluej.stride.framedjava.ast.JavaFragment;
import bluej.stride.generic.InteractionManager;

public abstract class CodeError
{
    private boolean flaggedAsOld = false;
    private final BooleanProperty focusedProperty = new SimpleBooleanProperty(false);
    protected final JavaFragment relevantSlot;
    private final BooleanProperty freshProperty = new SimpleBooleanProperty(false);
    
    @OnThread(Tag.Any)
    protected CodeError(JavaFragment code)
    {
        if (code == null)
            throw new IllegalArgumentException("Slot for error cannot be null");
        relevantSlot = code;
        Platform.runLater(() -> code.addError(this));
    }

    public void flagAsOld()
    {
        flaggedAsOld = true;
    }
    
    public boolean isFlaggedAsOld()
    {
        return flaggedAsOld;
    }
    
    public abstract boolean isJavaPos();
    
    public abstract String getMessage();
    
    public abstract List<? extends FixSuggestion> getFixSuggestions();
    
    // Start position relative to slot (0 is before first char)
    public int getStartPosition()
    {
        return 0;
    }
    // End position relative to slot (1 is after first char)
    public int getEndPosition()
    {
        return Integer.MAX_VALUE;
    }
    
    // Only call this for two errors which refer to the same slot.    
    // This forms a partial ordering.
    // Returns -1 if this error is more specific than the other,
    //   either because they overlap, 
    //   and this is detected pre-compilation (and the other is a compile error)
    //   or they are detected at the same stage and this refers to an earlier or smaller region
    // Returns 0 if two errors can be shown alongside each other
    public static int compareErrors(CodeError a, CodeError b)
    {
        final boolean aIsCompile = a instanceof JavaCompileError;
        final boolean bIsCompile = b instanceof JavaCompileError;
        
        final boolean overlap = a.overlaps(b);
        final boolean aIsSmallerOrEqual = a.getEndPosition() - a.getStartPosition() <= b.getEndPosition() - b.getStartPosition(); 
        
        if (!overlap) {
            return 0; // No ordering if they don't overlap
        }
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

    public boolean overlaps(CodeError e)
    {
     // See http://stackoverflow.com/questions/3269434/whats-the-most-efficient-way-to-test-two-integer-ranges-for-overlap
        return getStartPosition() <= e.getEndPosition() && e.getStartPosition() <= getEndPosition();
    }
    
    public void jumpTo(InteractionManager editor)
    {
        Node n = getRelevantNode();
        if (n != null) {
            editor.scrollTo(n, -100);
            relevantSlot.getErrorShower().focusAndPositionAtError(this);
        }
    }

    public final Node getRelevantNode()
    {
        if (relevantSlot.getErrorShower() == null)
            return null;
        return relevantSlot.getErrorShower().getRelevantNodeForError(this);
    }

    public BooleanProperty focusedProperty()
    {
        return focusedProperty;
    }
    
    public ObservableBooleanValue visibleProperty()
    {
        return freshProperty.not();
    }
    
    public void bindFresh(ObservableBooleanValue fresh)
    {
        freshProperty.bind(fresh);
    }
}
