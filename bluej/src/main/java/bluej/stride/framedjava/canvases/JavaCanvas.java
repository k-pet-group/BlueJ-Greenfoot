/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2016,2018 Michael Kölling and John Rosenberg
 
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
package bluej.stride.framedjava.canvases;


import bluej.stride.framedjava.frames.BreakpointFrame;
import bluej.stride.generic.InteractionManager;
import bluej.utility.javafx.JavaFXUtil;
import threadchecker.OnThread;
import threadchecker.Tag;
import javafx.scene.layout.VBox;
import bluej.stride.framedjava.ast.HighlightedBreakpoint;
import bluej.stride.framedjava.frames.DebugInfo;
import bluej.stride.framedjava.frames.DebugInfo.Display;
import bluej.stride.generic.CanvasParent;
import bluej.stride.generic.Frame;
import bluej.stride.generic.FrameCanvas;
import bluej.stride.generic.FrameCursor;
import bluej.stride.generic.SingleCanvasFrame;
import bluej.utility.Debug;

@OnThread(Tag.FX)
public class JavaCanvas extends FrameCanvas
{
    @SuppressWarnings("unused")
    private boolean methodCanvas;

    public JavaCanvas(InteractionManager editor,
            CanvasParent parent, String stylePrefix, boolean methodCanvas)
    {
        super(editor, parent, stylePrefix);
        this.methodCanvas = methodCanvas;
    }

    @OnThread(Tag.FXPlatform)
    public HighlightedBreakpoint showDebugBefore(Frame f, DebugInfo info)
    {
        Display disp;
        VBox special;
        if (f != null) {
            JavaFXUtil.setPseudoclass("bj-debug-before", true, f.getNode());
            disp = info.getInfoDisplay(f.getCursorBefore(), f.getNode(), f.getStylePrefix(), f instanceof BreakpointFrame);
            //removeSpecialsAfter(info, f.getCursorBefore());
            special = getSpecialBefore(f.getCursorBefore());
        }
        else {
            // Add at very end of canvas:
            disp = info.getInfoDisplay(getLastCursor(), null, null, false);
            special = getSpecialAfter(null);
            //removeSpecialsAfter(info, null);
        }
        
        if (special.getChildren().contains(disp) == false) {
            special.getChildren().add(disp);
        }
        
        editorFrm.ensureNodeVisible(special);
        
        return disp;

    }
    
    private void removeSpecialsAfter(DebugInfo info, FrameCursor fc)
    {
        while (fc != null) {
            VBox special = getSpecialAfter(fc);
            //Debug.message("Clearing children for: " + special + " after " + fc);
            info.removeAllDisplays(special.getChildren());
            special.getChildren().clear();
            // Descend to get rid of children:
            Frame f = getFrameAfter(fc);
            if (f instanceof SingleCanvasFrame /* TODO make this more robust */) {
                SingleCanvasFrame scf = (SingleCanvasFrame)f;
                ((JavaCanvas)scf.getCanvas()).removeSpecialsAfter(info, scf.getFirstInternalCursor());
            }
            fc = getNextCursor(fc, false);
        }
        
        /*
         * This block has been commented as it was causing a bug in the steps while in
         *  'Debug' mode.
         *  It was firing a ClassCastException form IfFrame$1 to Frame, as well. 
        // f must be null here:
        if (methodCanvas == false) {
            // Ascend as long as we're not a method:
            Frame parent = (Frame)getParent();
            JavaCanvas grandParent = (JavaCanvas) parent.getParentCanvas();
            grandParent.removeSpecialsAfter(info, parent.getCursorAfter());
        }
        */
    }
}
