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

import java.util.stream.Stream;

import bluej.stride.generic.InteractionManager;
import nu.xom.Element;
import threadchecker.OnThread;
import threadchecker.Tag;
import bluej.stride.framedjava.ast.JavaSource;
import bluej.stride.framedjava.ast.SlotFragment;
import bluej.stride.framedjava.frames.CommentFrame;
import bluej.stride.generic.Frame;
import bluej.stride.generic.Frame.ShowReason;

public class CommentElement extends CodeElement
{
    public static final String ELEMENT = "comment";
    private CommentFrame frame;
    private final String comment;
    
    public CommentElement(Element xmlEl)
    {
        if (xmlEl.getChildCount() > 0)
            comment = xmlEl.getChild(0).getValue();
        else
            comment = ""; // Empty
    }
    
    @OnThread(Tag.FX)
    public CommentElement(CommentFrame frame)
    {
        this.frame = frame;
        this.comment = frame.getComment();
    }

    public CommentElement(String s)
    {
        this.comment = s;
    }

    @Override
    public JavaSource toJavaSource()
    {
        // We keep the comment in the source so that it's there if the user
        // converts the whole class to Java or previews as Java
        
        // We must remove any initial star (to avoid making it a Javadoc comment)
        // and any /* or */ inside.  Prefixing space removes initial star  
        String sanitised = " " +comment.replace("/*", "").replace("*/","").replace("\n",""); // TODO make newlines work again
        return new JavaSource(null, f(frame, "/*"), f(frame, sanitised), f(frame, "*/"));
    }

    @Override
    public LocatableElement toXML()
    {
        LocatableElement commentEl = new LocatableElement(this, ELEMENT);
        preserveWhitespace(commentEl);
        commentEl.appendChild(comment);
        return commentEl;
    }

    @Override
    public Frame createFrame(InteractionManager editor)
    {
        frame = new CommentFrame(editor, comment);
        return frame;
    }
    
    @Override
    public void show(ShowReason reason)
    {
        frame.show(reason);        
    }
    
    @Override
    protected Stream<SlotFragment> getDirectSlotFragments()
    {
        return Stream.empty();
    }
}
