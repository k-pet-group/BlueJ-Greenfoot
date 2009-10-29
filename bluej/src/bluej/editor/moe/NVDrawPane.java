/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
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

import java.awt.Font;

import javax.swing.JEditorPane;

/**
 * A JEditorPane implementation to provide the root view for the document. This is used
 * by the NaviView component.
 * 
 * @author Davin McCall
 */
public class NVDrawPane extends JEditorPane
{
    public NVDrawPane()
    {
        Font smallFont = new Font("Monospaced", Font.BOLD, 1);
        setFont(smallFont);
        setEditorKit(new NaviviewEditorKit());
    }

    @Override
    public void repaint(long tm, int x, int y, int width, int height)
    {
        // TODO Auto-generated method stub
        // super.repaint(tm, x, y, width, height);
    }
}
