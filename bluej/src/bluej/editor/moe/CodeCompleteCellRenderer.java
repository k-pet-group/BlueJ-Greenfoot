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
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

import bluej.prefmgr.PrefMgr;
import bluej.utility.DBoxLayout;


public class CodeCompleteCellRenderer extends JPanel implements ListCellRenderer
{
    private JLabel typeLabel = new JLabel();
    private JLabel descriptionLabel = new JLabel();
    private Dimension rtypeSize;
    private String immediateType;
    
    CodeCompleteCellRenderer(String immediateType)
    {
        setBorder(null);
        setLayout(new DBoxLayout(DBoxLayout.X_AXIS));
        typeLabel.setFont(PrefMgr.getStandardEditorFont());
        typeLabel.setText("String123456"); // for assigning width
        rtypeSize = typeLabel.getPreferredSize();
        typeLabel.setMaximumSize(rtypeSize);
        typeLabel.setPreferredSize(rtypeSize);
        add(typeLabel);
        
        descriptionLabel.setFont(PrefMgr.getStandardEditorFont());
        add(descriptionLabel);
        add(Box.createHorizontalGlue());
        
        this.immediateType = immediateType;
    }
    
    public Component getListCellRendererComponent(JList list, Object value,
            int index, boolean isSelected, boolean cellHasFocus)
    {
        if (value != null) {
            AssistContent content = (AssistContent) value;
            typeLabel.setText(content.getReturnType().toString());
            descriptionLabel.setText(content.getDisplayName());
            
            Font font = PrefMgr.getStandardEditorFont();

            if (content.getDeclaringClass().equals(immediateType)) {
                font = font.deriveFont(Font.BOLD);
            }
            
            descriptionLabel.setFont(font);
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
