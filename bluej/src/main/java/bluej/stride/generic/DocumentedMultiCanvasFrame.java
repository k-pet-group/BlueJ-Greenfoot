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
import javafx.beans.property.StringProperty;
import javafx.scene.Node;

public abstract class DocumentedMultiCanvasFrame extends MultiCanvasFrame
{
    protected DocumentationTextArea documentationPane;

    protected DocumentedMultiCanvasFrame(InteractionManager editor, String caption, String stylePrefix)
    {
        super(editor, caption, stylePrefix);
        documentationPane = new DocumentationTextArea(editor, this, this, stylePrefix);
    }

    public String getDocumentation()
    {
        return documentationPane.getText();
    }
    
    public void setDocumentation(String s)
    {
        documentationPane.setText(s);
    }

    public StringProperty documentationPromptTextProperty()
    {
        return documentationPane.promptTextProperty();
    }

    @Override
    public void pullUpContents()
    {
        // This kind of frames can't be removed keeping their contents.
    }

    @Override
    protected void modifyChildren(List<FrameContentItem> updatedChildren)
    {
        updatedChildren.add(updatedChildren.indexOf(getHeaderRow()), documentationPane);
    }

    @Override
    protected double getRightMarginFor(Node n)
    {
        if (n == documentationPane.getNode())
            return 6.0;
        else
            return super.getRightMarginFor(n);
    }

    @Override
    protected double getLeftMarginFor(Node n)
    {
        if (n == documentationPane.getNode())
            return 6.0;
        else
            return super.getLeftMarginFor(n);
    }

    @Override
    protected double getBottomMarginFor(Node n)
    {
        if (n == documentationPane.getNode())
            return 4.0;
        else
            return super.getBottomMarginFor(n);
    }
}
