/*
 This file is part of the BlueJ program. 
 Copyright (C) 2019  Michael Kolling and John Rosenberg 

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
package bluej.parser;

import bluej.editor.flow.HoleDocument;
import bluej.editor.flow.JavaSyntaxView;
import bluej.editor.flow.ScopeColors;
import bluej.parser.entity.EntityResolver;
import bluej.parser.nodes.ReparseableDocument;
import javafx.beans.property.ReadOnlyBooleanWrapper;

public class TestableDocument extends JavaSyntaxView implements ReparseableDocument
{
    boolean parsingSuspended = false;
    
    public TestableDocument(EntityResolver entityResolver)
    {
        super(new HoleDocument(), null, ScopeColors.dummy(), entityResolver, new ReadOnlyBooleanWrapper(true));
    }
    
    public TestableDocument()
    {
        this(null);
    }

    public void insertString(int pos, String content)
    {
        document.replaceText(pos, pos, content);
    }
    
    public void remove(int start, int length)
    {
        document.replaceText(start, start + length, "");
    }

    @Override
    public void flushReparseQueue()
    {
        if (!parsingSuspended)
            super.flushReparseQueue();
    }
}
