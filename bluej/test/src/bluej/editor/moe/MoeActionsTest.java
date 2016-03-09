/*
 This file is part of the BlueJ program. 
 Copyright (C) 2016  Michael Kolling and John Rosenberg 
 
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
package bluej.editor.moe;

import javax.swing.Action;
import javax.swing.JTextPane;

import bluej.parser.InitConfig;
import junit.framework.TestCase;

/**
 * Some simple tests for MoeActions.
 * 
 * @author Davin McCall
 */
public class MoeActionsTest extends TestCase
{
    @Override
    protected void setUp() throws Exception
    {
        InitConfig.init();
    }
    
    public void testActionTable()
    {
        MoeActions actions = MoeActions.getActions(null, new JTextPane());
        Action[] actionTable = actions.getActionTable();
        
        // If actions are removed, they should also have no entries in the table - there should be no
        // null entries in the table.
        for (int i = 0; i < actionTable.length; i++) {
            assertNotNull("Action table entry " + i + " is null", actionTable[i]);
        }
        
        // The category indexes are indexes into the action table. Make sure that the last categories cover the
        // whole table:
        int[] categoryIndexes = actions.getCategoryIndex();
        assertEquals(categoryIndexes[categoryIndexes.length - 1], actionTable.length);
    }
}
