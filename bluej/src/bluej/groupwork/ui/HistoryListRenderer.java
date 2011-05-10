/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2011  Michael Kolling and John Rosenberg 
 
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
package bluej.groupwork.ui;

import java.awt.Component;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Rectangle;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import bluej.groupwork.HistoryInfo;
import bluej.utility.DBox;
import bluej.utility.MultiWrapLabel;

/**
 * Renderer for cells in the log/history list.
 * 
 * <p>This is a little complicated because the renderer wraps text at the width
 * of the list. This means that the preferred height of a cell is dependent on the
 * width.
 * 
 * @author Davin McCall
 */
public class HistoryListRenderer extends DBox implements ListCellRenderer
{
    private HistoryListModel model;
    
    private JLabel topLabel;
    private MultiWrapLabel commentArea;
    private JLabel spacerLabel;
    private JTextArea filesArea;
    
    private JScrollPane container;
    
    private int index;
    
    private Box filesBox;
    private Box commentBox;
    
    /**
     * Create a new list renderer.
     */
    public HistoryListRenderer(HistoryListModel model)
    {
        //super(BoxLayout.Y_AXIS);
        super(DBox.Y_AXIS, 0f);
        
        this.model = model;
        
        topLabel = new JLabel();
        Font font = topLabel.getFont();
        topLabel.setAlignmentX(0f);
        topLabel.setFont(font.deriveFont(Font.BOLD));
        add(topLabel);
        
        filesBox = new Box(BoxLayout.X_AXIS);
        JLabel spaceLabel = new JLabel("    ");
        filesBox.add(spaceLabel);
        filesArea = new JTextArea();
        filesArea.setAlignmentX(0f);
        filesArea.setFont(font.deriveFont(0));
        filesBox.add(filesArea);
        filesBox.setAlignmentX(0f);
        add(filesBox);
        
        commentBox = new Box(BoxLayout.X_AXIS);
        spacerLabel = new JLabel("        ");
        commentBox.add(spacerLabel);
        commentArea = new MultiWrapLabel();
        commentArea.setAlignmentX(0f);
        //commentArea.setFont(font.deriveFont(font.getSize2D() * 0.9f).deriveFont(0));
        commentArea.setFont(font.deriveFont(0));
        commentBox.add(commentArea);
        commentBox.setAlignmentX(0f);
        add(commentBox);
    }
    
    /**
     * Set the containing scroll pane. This is needed to be able to wrap
     * comment text correctly according to the width of the scroll pane.
     */
    public void setWrapMode(JScrollPane container)
    {
        this.container = container;
    }
    
    /* (non-Javadoc)
     * @see javax.swing.ListCellRenderer#getListCellRendererComponent(javax.swing.JList, java.lang.Object, int, boolean, boolean)
     */
    public Component getListCellRendererComponent(
      JList list,
      Object value,            // value to display
      int index,               // cell index
      boolean isSelected,      // is the cell selected
      boolean cellHasFocus)    // the list and the cell have the focus
    {
        HistoryInfo info = (HistoryInfo) value;
        String topText = info.getDate() + "  "  + info.getRevision() + "  " + info.getUser();
        String [] files = info.getFiles();
        String filesText = files[0];
        for (int i = 1; i < files.length; i++) {
            filesText += "\n" + files[i];
        }
        filesArea.setText(filesText);
        filesArea.invalidate();
        filesBox.invalidate();
        topLabel.setText(topText);
        
        String commentText = info.getComment();
        // commentArea.setText("");
        //commentArea.setLineWrap(false);
        //commentArea.setLineWrap(true);
        commentArea.setText(commentText);
        commentBox.invalidate();

        if (isSelected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
            filesArea.setForeground(list.getSelectionForeground());
            filesArea.setBackground(list.getSelectionBackground());
            setOpaque(true);
        }
        else {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
            filesArea.setForeground(list.getForeground());
            filesArea.setBackground(list.getBackground());
            setOpaque(false);
        }
        
        if (cellHasFocus) {
            setBorder(new LineBorder(list.getSelectionForeground(), 1));
        }
        else {
            setBorder(new EmptyBorder(1,1,1,1));
        }
        
        setEnabled(list.isEnabled());
        setFont(list.getFont());
        
        if (container != null) {
            Rectangle bbounds = container.getViewportBorderBounds();
            int listWidth = bbounds.width;
            Insets containerInsets = container.getViewport().getInsets();
            listWidth -= containerInsets.left + containerInsets.right;
            listWidth -= spacerLabel.getPreferredSize().width;
            listWidth -= getInsets().left + getInsets().right;
            
            commentArea.setWrapWidth(listWidth);
            commentArea.invalidate();
        }

        // We need to validate to ensure that we have the correct preferred
        // size.
        invalidate();
        validate();
        
        // Inform the model of our desired height. If this has changed, the model
        // fires a "cell changed" event so that the list will detect the change.
        model.setCellHeight(index, this.getPreferredSize().height);
        this.index = index;
        
        // Once we return, the list should ask what our preferred size is; it
        // will then call setBounds to set a size.
        return this;
    }
    
    public void setBounds(int x, int y, int width, int height)
    {
        super.setBounds(x, y, width, height);
        
        // Ok, the list has assigned us a size. However, the horizontal size
        // we told the list we needed may now be incorrect, which can cause
        // a horizontal scrollbar to needlessly appear.
                
        validate();
        if (getPreferredSize().getWidth() < width) {
            model.changeChell(index);
        }
    }
    
    public boolean isValidateRoot()
    {
        return true;
    }
}
