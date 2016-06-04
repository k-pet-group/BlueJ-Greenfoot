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
package bluej.stride.framedjava.elements;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import threadchecker.OnThread;
import threadchecker.Tag;
import bluej.stride.framedjava.frames.CodeFrame;
import bluej.stride.generic.FrameCanvas;

public abstract class ContainerCodeElement extends CodeElement
{
    public abstract List<CodeElement> childrenUpTo(CodeElement c);
            
    @OnThread(Tag.FX)
    private static <T> Stream<T> streamCodeFrame(FrameCanvas canvas, Function<CodeElement, Stream<T>> f)
    {
        return canvas.getBlocksSubtype(CodeFrame.class).stream()
                   .map(CodeFrame::getCode)
                   .flatMap(f);
    }
    
    //Gets variables declared by this block that are in scope of its children
    public List<LocalParamInfo> getDeclaredVariablesWithin(CodeElement child)
    {
        return Collections.emptyList();
    }
    
    // Returns "this" if top level element, null otherwise
    public TopLevelCodeElement getTopLevelElement()
    {
        return null;
    }
    
    // Returns this if ClassElement, null otherwise
    public MethodWithBodyElement getMethodElement()
    {
        return null;
    }
    
    // Force sub-classes to implement this method:
    public abstract Stream<CodeElement> streamContained();
    
    // Helper function:
    protected static Stream<CodeElement> streamContained(List<CodeElement> els)
    {
        if (els == null) {
            return Stream.empty();
        }
        return els.stream().flatMap(el -> Stream.concat(Stream.of(el), el.streamContained()));
    }
}
