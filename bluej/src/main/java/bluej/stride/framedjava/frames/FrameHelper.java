/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2016 Michael Kölling and John Rosenberg
 
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
package bluej.stride.framedjava.frames;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import bluej.stride.generic.Frame;
import bluej.stride.generic.FrameCanvas;
import bluej.stride.generic.FrameCursor;
import bluej.utility.Utility;
import threadchecker.OnThread;
import threadchecker.Tag;

public class FrameHelper
{
    @OnThread(Tag.FXPlatform)
    public static void flagErrorsAsOld(FrameCanvas canvas)
    {
        canvas.getBlockContents().forEach(f -> f.flagErrorsAsOld());
    }

    @OnThread(Tag.FXPlatform)
    public static void removeOldErrors(FrameCanvas canvas)
    {
        canvas.getBlockContents().forEach(f -> f.removeOldErrors());
    }

    static void pullUpContents(Frame f, FrameCanvas innerCanvas)
    {
        // Put our contents in place of us:
        FrameCursor cursorBefore = f.getCursorBefore();
        // Make copy because we're about to modify the contents:
        ArrayList<Frame> contents = new ArrayList<>(innerCanvas.getBlockContents());
        contents.forEach(c -> innerCanvas.removeBlock(c));
        cursorBefore.insertFramesAfter(contents);
        f.getParentCanvas().removeBlock(f);
    }

    public static void processVarScopesAfter(FrameCanvas parentCanvas,
            Frame afterFrame, BiConsumer<Map<String, List<Frame>> /* scopes, excluding cur */, Frame /* cur */> process)
    {
        processVarScopesAfter(parentCanvas, afterFrame, new HashMap<>(), process);
    }
        
    private static void processVarScopesAfter(FrameCanvas parentCanvas,
                Frame afterFrame, Map<String, List<Frame>> parentVars, BiConsumer<Map<String, List<Frame>> /* scopes, excluding cur */, Frame /* cur */> process)
    {
        // Must make a copy:
        Map<String, List<Frame>> vars = new HashMap<>(parentVars);
        for (Frame f : Utility.iterableStream(parentCanvas.getFramesAfter(afterFrame)))
        {
            process.accept(vars, f);
            
            for (FrameCanvas c : Utility.iterableStream(f.getCanvases()))
            {
                Map<String, List<Frame>> extraVarsWithin = new HashMap<>();
                for (String v : f.getDeclaredVariablesWithin(c))
                {
                    extraVarsWithin.put(v, Arrays.asList(f));
                }
                Map<String, List<Frame>> varsInside = Utility.mergeMaps(vars, extraVarsWithin, Utility::concat);
                processVarScopesAfter(c, null, varsInside, process);
            }
            
            for (String var : f.getDeclaredVariablesAfter())
            {
                vars.merge(var, new ArrayList<Frame>(Arrays.asList(f)), Utility::concat);
            }
        }
    }
}
