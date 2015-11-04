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
package bluej.stride.generic;

import java.util.List;
import java.util.stream.Collectors;

public interface CanvasParent extends CursorFinder
{
    boolean acceptsType(FrameCanvas canvasBase, Class<? extends Frame> class1);

    default FrameCursor getCursorBefore(FrameCanvas c)
    {
        List<FrameCanvas> canvases = getFrame().getCanvases().collect(Collectors.toList());
        int index = canvases.indexOf(c);
        if (index == -1)
            throw new IllegalStateException("Canvas not known by parent frame");

        FrameCursor candidate = null;
        while (index > 0 && (candidate = canvases.get(index - 1).getLastCursor()) == null)
        {
            index -= 1;
        }

        if (candidate != null)
            return candidate;
        else
            return getFrame().getCursorBefore();
    }

    default FrameCursor getCursorAfter(FrameCanvas c)
    {
        List<FrameCanvas> canvases = getFrame().getCanvases().collect(Collectors.toList());
        int index = canvases.indexOf(c);
        if (index == -1)
            throw new IllegalStateException("Canvas not known by parent frame");

        FrameCursor candidate = null;
        while (index + 1 < canvases.size() && (candidate = canvases.get(index + 1).getFirstCursor()) == null)
        {
            index += 1;
        }

        if (candidate != null)
            return candidate;
        else
            return getFrame().getCursorAfter();
    }

    List<ExtensionDescription> getAvailableInnerExtensions(FrameCanvas canvas, FrameCursor cursor);

    InteractionManager getEditor();

    default void modifiedCanvasContent()
    {
        // By default, notify editor of all changes:
        getEditor().modifiedFrame(null);
    }
    
    static boolean processInnerExtensionKey(CanvasParent p, FrameCanvas canvas, FrameCursor cursor, char c, RecallableFocus rc, boolean atTop)
    {
        List<ExtensionDescription> candidates = p.getAvailableInnerExtensions(canvas, cursor).stream()
                .filter(e -> e.getShortcutKey() == c && (e.worksThroughout() || atTop))
                .collect(Collectors.toList());
        
        if (candidates.size() == 0) {
            return false;
        }
        if (candidates.size() > 1) {
            throw new IllegalStateException("Ambiguous inner extension for: " + (int)c);
        }
        
        if (candidates.get(0).isAvailable()) {
            p.getEditor().beginRecordingState(rc);
            candidates.get(0).activate();
            p.getEditor().endRecordingState(rc);
            return true;
        }
        return false;
    }

    Frame getFrame();
    
    /**
     * If true, this key press has been redirected to another cursor, and should be ignored.
     */
    default boolean tryRedirectCursor(FrameCanvas canvas, char c) { return false; }

    enum CanvasKind
    {
        FIELDS, CONSTRUCTORS, METHODS, STATEMENTS;
    }
    
    default CanvasKind getChildKind(FrameCanvas c) { return CanvasKind.STATEMENTS; }
}
