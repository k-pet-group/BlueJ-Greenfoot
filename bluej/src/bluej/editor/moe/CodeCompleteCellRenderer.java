/*
 This file is part of the BlueJ program. 
 Copyright (C) 2010  Michael Kolling and John Rosenberg 
 
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

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;


public class CodeCompleteCellRenderer extends JLabel implements ListCellRenderer
{
    public CodeCompleteCellRenderer()
    {
        setBorder(null);
    }
    
    public Component getListCellRendererComponent(JList list, Object value,
            int index, boolean isSelected, boolean cellHasFocus)
    {
        if (value != null) {
            AssistContent content = (AssistContent) value;
            setText(content.getDisplayName() + " : " + content.getReturnType() + " - " +
                    content.getDeclaringClass());
        }
        
        if (isSelected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
            setOpaque(true);
        }
        else {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
            setOpaque(false);
        }
        
        return this;
    }
}
