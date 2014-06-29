/*
 This file is part of the BlueJ program. 
 Copyright (C) 2010,2011,2014  Michael Kolling and John Rosenberg 
 
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

import bluej.Config;
import bluej.parser.AssistContent;
import bluej.prefmgr.PrefMgr;
import bluej.utility.DBoxLayout;
import java.awt.Color;

/**
 * A cell renderer for the code completion popup list.
 * 
 * @author Davin McCall
 */
public class CodeCompleteCellRenderer extends JPanel implements ListCellRenderer
{
    /** label showing the return type */
    private final JLabel typeLabel = new JLabel();
    /** label showing method name and parameters */
    private final JLabel descriptionLabel = new JLabel();
    
    private final Dimension rtypeSize;
    private final String immediateType;
    private final Font cfont;
    private final Font cfontBold;
    
    CodeCompleteCellRenderer(String immediateType)
    {
        setBorder(null);
        setLayout(new DBoxLayout(DBoxLayout.X_AXIS));
        int fontSize = PrefMgr.getStandardEditorFont().getSize();
        cfont = Config.getFont("bluej.codecompletion.font", "Monospaced", fontSize);
        cfontBold = cfont.deriveFont(Font.BOLD);
        typeLabel.setFont(cfont);
        typeLabel.setText("String123456"); // for assigning width
        rtypeSize = typeLabel.getPreferredSize();
        typeLabel.setMaximumSize(rtypeSize);
        typeLabel.setMinimumSize(rtypeSize);
        typeLabel.setPreferredSize(rtypeSize);
        typeLabel.setForeground(new Color(90, 80, 45));
        add(typeLabel);

        add(descriptionLabel);
        add(Box.createHorizontalGlue());
        setBorder(new javax.swing.border.EmptyBorder(2, 2, 2, 2));

        this.immediateType = immediateType;
    }

    public Component getListCellRendererComponent(JList list, Object value,
            int index, boolean isSelected, boolean cellHasFocus)
    {
        if (value != null && list.isValid() && index <= list.getLastVisibleIndex() && index >= list.getFirstVisibleIndex()) {
            AssistContent content = (AssistContent) value;
            typeLabel.setText(content.getReturnType().toString());
            descriptionLabel.setText(content.getDisplayName());

            if (content.getDeclaringClass().equals(immediateType)) {
                descriptionLabel.setFont(cfontBold);
            }
            else {
                descriptionLabel.setFont(cfont);
            }
        }

        if (isSelected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
            if (!Config.isRaspberryPi()) {
                setOpaque(true);
            }
        }
        else {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
            if (!Config.isRaspberryPi()) {
                setOpaque(false);
            }
        }

        return this;
    }
}
