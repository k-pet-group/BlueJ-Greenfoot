/*
* MoeSyntaxDocument.java - inherits from
* DefaultSyntaxDocument.java - Simple implementation of SyntaxDocument
* Copyright (C) 1999 Slava Pestov
* modified by Bruce Quig to add Syntax highlighting to the BlueJ
* programming environment.
*
* This library is free software; you can redistribute it and/or
* modify it under the terms of the GNU Lesser General Public
* License as published by the Free Software Foundation; either
* version 2 of the License, or (at your option) any later version.
*
* This library is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this library; if not, write to the Free Software
* Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA.
*/
package bluej.editor.moe;

import javax.swing.event.*;
import javax.swing.text.*;
import java.awt.Color;
import javax.swing.undo.*;

import bluej.utility.*;

import org.gjt.sp.jedit.syntax.*;

/**
* A simple implementation of <code>SyntaxDocument</code> that 
* inherits from DefaultSyntaxDocument. It takes
* care of inserting and deleting lines from the token marker's state.
* It adds the ability to handle paragraph attributes on a per line basis.
*
* @author Bruce Quig
*
*/
public class MoeSyntaxDocument extends DefaultSyntaxDocument
{

    public MoeSyntaxDocument()
    {
        // should pick up number from definitions file
        putProperty(tabSizeAttribute, new Integer(4));
    }

    /**
     * Sets attributes for a paragraph.  This method was added to 
     * provide the ability to replicate DefaultStyledDocument's ability to 
     * set each lines attributes easily.
     * This is an added method for the BlueJ adaption of jedit's Syntax package   
     *
     * @param offset the offset into the paragraph >= 0
     * @param length the number of characters affected >= 0
     * @param s the attributes
     * @param replace whether to replace existing attributes, or merge them
     */
    public void setParagraphAttributes(int offset, int length, AttributeSet s, 
                                       boolean replace)
    {

        // code closely resembles method from DefaultStyleDocument
        try {
            writeLock();
            //DefaultDocumentEvent changes = 
            //    new DefaultDocumentEvent(offset, length, DocumentEvent.EventType.CHANGE);

            //AttributeSet sCopy = s.copyAttributes();

            Element section = getDefaultRootElement();
            int index0 = section.getElementIndex(offset);
            int index1 = section.getElementIndex(offset + ((length > 0) ? length - 1 : 0));

            for (int i = index0; i <= index1; i++) {
                Element paragraph = section.getElement(i);
                MutableAttributeSet attr = (MutableAttributeSet) paragraph.getAttributes();
                //   changes.addEdit(new AttributeUndoableEdit(paragraph, sCopy, replace));
                if (replace) {
                    attr.removeAttributes(attr);
                }
                attr.addAttributes(s);
            }
            //changes.end();
            //fireChangedUpdate(changes);
            //fireUndoableEditUpdate(new UndoableEditEvent(this, changes));
        } finally {
            writeUnlock();
        }

    }
}

