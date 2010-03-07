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

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

import bluej.utility.DBoxLayout;


public class CodeCompleteCellRenderer extends JPanel implements ListCellRenderer
{
    private JLabel typeLabel = new JLabel();
    private JLabel descriptionLabel = new JLabel();
    private JLabel ellipsisLabel = new JLabel("\u2026 ");
    private Dimension rtypeSize;
    private Dimension collapsedRtypeSize;
    
    CodeCompleteCellRenderer()
    {
        setBorder(null);
        setLayout(new DBoxLayout(DBoxLayout.X_AXIS));
        add(typeLabel);
        typeLabel.setText("String1234"); // for assigning width
        rtypeSize = typeLabel.getPreferredSize();
        collapsedRtypeSize = new Dimension(rtypeSize.width - ellipsisLabel.getPreferredSize().width,
                rtypeSize.height);
        
        add(ellipsisLabel);
        
        typeLabel.setMaximumSize(rtypeSize);
        typeLabel.setPreferredSize(rtypeSize);
        add(descriptionLabel);
        add(Box.createHorizontalGlue());
    }
    
    public Component getListCellRendererComponent(JList list, Object value,
            int index, boolean isSelected, boolean cellHasFocus)
    {
        if (value != null) {
            AssistContent content = (AssistContent) value;
            typeLabel.setText(content.getReturnType().toString());
            descriptionLabel.setText(content.getDisplayName());
            typeLabel.setPreferredSize(null);
            int prefWidth = typeLabel.getPreferredSize().width;
            //if (prefWidth + 5 > rtypeSize.width) {
            //    typeLabel.setPreferredSize(collapsedRtypeSize);
            //    typeLabel.setMaximumSize(collapsedRtypeSize);
            //    ellipsisLabel.setVisible(true);
            //}
            //else {
                typeLabel.setPreferredSize(rtypeSize);
                typeLabel.setMaximumSize(rtypeSize);
                ellipsisLabel.setVisible(false);                
            //}
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
