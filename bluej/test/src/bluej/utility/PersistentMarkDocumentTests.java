/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014  Michael Kolling and John Rosenberg 
 
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
package bluej.utility;

import java.util.ArrayList;
import java.util.List;

import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.Element;

import junit.framework.TestCase;

public class PersistentMarkDocumentTests extends TestCase
{
    @Override
    protected void setUp() throws Exception
    {
    }
    
    @Override
    protected void tearDown() throws Exception
    {
    }
    
    public void testUndoRedo() throws Exception
    {
        String[] docLines = {
                "abc",
                "  // 1",
                "  // 2",
                "  // 3",
                "def"
        };
        
        int [] lineStart = new int[docLines.length];
        int i = 0;
        
        PersistentMarkDocument doc = new PersistentMarkDocument();
        for (String line : docLines) {
            lineStart[i++] = doc.getLength();
            doc.insertString(doc.getLength(), line + "\n", null);
        }
        
        final List<UndoableEditEvent> editList = new ArrayList<UndoableEditEvent>();

        doc.addUndoableEditListener(new UndoableEditListener() {
            @Override
            public void undoableEditHappened(UndoableEditEvent e)
            {
                editList.add(0, e);
            }
        });
        
        doc.remove(lineStart[1], lineStart[4] - lineStart[1]);
        doc.insertString(lineStart[1], "a", null);
        
        for (UndoableEditEvent event : editList) {
            event.getEdit().undo();
        }
        
        Element root = doc.getDefaultRootElement();
        assertEquals(docLines.length + 1, root.getElementCount());
        
        for (i = 0; i < docLines.length; i++) {
            Element line = root.getElement(i);
            String lineText = doc.getText(line.getStartOffset(), line.getEndOffset() - line.getStartOffset() - 1);
            assertEquals(docLines[i], lineText);
        }
    }
}
