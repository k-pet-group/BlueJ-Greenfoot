/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2017,2019 Michael Kölling and John Rosenberg
 
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

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.stream.Collectors;

import javafx.scene.Node;

import nu.xom.Builder;
import bluej.parser.entity.EntityResolver;
import bluej.stride.framedjava.elements.ClassElement;
import bluej.stride.framedjava.elements.TopLevelCodeElement;
import bluej.stride.framedjava.frames.TopLevelFrame;
import nu.xom.ParsingException;

/**
 * Stores a ClassElement's state as XML
 */
public class FrameState
{
    private String classElementXML;
    private int cursorIndex; // Which cursor
    private int cursorInfo; // Saved state, e.g. caret position

    public FrameState(TopLevelCodeElement topLevelElement)
    {
        this.classElementXML = topLevelElement.toXML().toXML();
        cursorIndex = -1;
        cursorInfo = -1;
    }
    
    public FrameState(TopLevelFrame<?> frame, TopLevelCodeElement classElement, RecallableFocus focusOverride)
    {
        this.classElementXML = classElement.toXML().toXML();
        List<RecallableFocus> focusables = frame.getFocusables().collect(Collectors.toList());
        this.cursorIndex = -1;    

        if (focusOverride != null) {
            this.cursorIndex = focusables.indexOf(focusOverride);
            this.cursorInfo = focusOverride.getFocusInfo();
        }
        else {
            for (int i = 0; i < focusables.size(); i++) {
                RecallableFocus recallableFocus = focusables.get(i);
                if (recallableFocus!= null && recallableFocus.isFocused()) {
                    this.cursorIndex = i;
                    this.cursorInfo = recallableFocus.getFocusInfo();
                    break;
                }
            }
        }
    }
    
    /**
     * Create a ClassElement corresponding to this FrameState.
     * 
     * @param resolver   The resolver used to resolve identifiers
     * @param packageName  The name of the package containing the class (empty string for default package)
     * @return  A new ClassElement.
     */
    public ClassElement getClassElement(EntityResolver resolver, String packageName)
    {
        try
        {
            return new ClassElement(new Builder().build(new StringReader(classElementXML))
                    .getRootElement(), resolver, packageName);
        }
        catch (IOException | ParsingException e)
        {
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public boolean equals(Object obj)
    {
        if ( obj == null || !(obj instanceof FrameState) ) {
            return false;
        }
        FrameState otherState = (FrameState)obj;
        // Currently, it is equality on the contents only, not on the cursor position.
        return otherState.classElementXML.equals(classElementXML);//&& cursorPosition == otherState.cursorPosition;
    }

    @Override
    public int hashCode()
    {
        return classElementXML.hashCode();
    }

    public Node recallFocus(TopLevelFrame<?> frame)
    {
        List<RecallableFocus> focusables = frame.getFocusables().collect(Collectors.toList());
        if (cursorIndex >= 0 && cursorIndex < focusables.size()) {
            return focusables.get(cursorIndex).recallFocus(cursorInfo);
        }
        return null;
    }
}
