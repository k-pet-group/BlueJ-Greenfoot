/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010  Poul Henriksen and Michael Kolling 
 
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
package greenfoot.gui.images;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * A list which allows selecting image categories. The categories
 * available are determined by scanning a directory for subdirectories.
 * Selecting a category will make a corresponding ImageLibList show
 * the contents of that category.
 * 
 * @author davmac
 */
public class ImageCategorySelector extends JList
    implements ListSelectionListener
{
    private ImageLibList imageLibList;
    
    /**
     * The expected number of categories. Our preferred scrollport
     * size is set to be large enough to show this many categories.
     */
    private static int NUMBER_OF_CATEGORIES = 10;
    
    private int preferredHeight;
    
    /**
     * Construct an ImageCategorySelector to show categories from the
     * given directory.
     * 
     * @param categoryDir  The directory containing the categories
     *                     (subdirectories)
     */
    public ImageCategorySelector(File categoryDir)
    {
        DefaultListModel listModel = new DefaultListModel();
        setModel(listModel);
        setLayoutOrientation(JList.VERTICAL);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setCellRenderer(new MyCellRenderer());
        addListSelectionListener(this);
        
        FileFilter filter = new FileFilter() {
            public boolean accept(File path)
            {
                // Show directories only
                return path.isDirectory();
            }
        };
        
        File [] imageFiles = categoryDir.listFiles(filter);
        if (imageFiles == null) {
            return;
        }
        
        Arrays.sort(imageFiles);

        for (int i = 0; i < imageFiles.length; i++) {
            listModel.addElement(imageFiles[i]);
            if (i == (NUMBER_OF_CATEGORIES - 1)) {
                preferredHeight = getPreferredSize().height;
            }
        }
        
        if (preferredHeight == 0) {
            preferredHeight = getPreferredSize().height;
        }
    }

    /**
     * Set the ImageLibList to be associated with this category selector.
     * When a category is selected, the associated ImageLibList will be
     * made to show images from the category.
     * 
     * @param imageLibList  The ImageLibList to associate with this category
     *                      selector
     */
    public void setImageLibList(ImageLibList imageLibList)
    {
        this.imageLibList = imageLibList;
    }
    
    /**
     * Get the currently selected image directory.
     */
    public File getSelectedDirectory()
    {
        return (File) getSelectedValue();
    }

    /* (non-Javadoc)
     * @see javax.swing.event.ListSelectionListener#valueChanged(javax.swing.event.ListSelectionEvent)
     */
    public void valueChanged(ListSelectionEvent e)
    {
        if (imageLibList != null) {
            File selected = getSelectedDirectory();
            if (selected != null) {
                imageLibList.setDirectory(selected);
            }
        }
    }
    
    private static class MyCellRenderer extends Box
    implements ListCellRenderer
    {
        private static final String iconFile = "openRight.png"; 
        private static final Icon openRightIcon = new ImageIcon(ImageCategorySelector.class.getClassLoader().getResource(iconFile));
        
        private JLabel categoryNameLabel;
        private JLabel iconLabel;
        
        public MyCellRenderer()
        {
            super(BoxLayout.X_AXIS);

            iconLabel = new JLabel(openRightIcon);
            Dimension iconSize = iconLabel.getPreferredSize();
            // Set maximum size on the icon label so that the category
            // name label uses up all the extra space
            iconLabel.setMaximumSize(iconSize);
            
            categoryNameLabel = new JLabel(" ");
            // name label height the same as the icon height (for selection painting)
            Dimension preferredSize = categoryNameLabel.getPreferredSize();
            preferredSize.height = iconSize.height;
            categoryNameLabel.setPreferredSize(preferredSize);
            
            add(categoryNameLabel);
            add(iconLabel);
        }
        
        public Component getListCellRendererComponent(
                JList list,
                Object value,            // value to display
                int index,               // cell index
                boolean isSelected,      // is the cell selected
                boolean cellHasFocus)    // the list and the cell have the focus
        {
            File entry = (File) value;
            
            categoryNameLabel.setText(entry.getName());
            categoryNameLabel.setFont(list.getFont());
            
            // Mess with sizes to make sure the name label fills as
            // much space as possible, pushing the icon over to the
            // right.
            Dimension size = categoryNameLabel.getPreferredSize();
            size.width = Integer.MAX_VALUE;
            categoryNameLabel.setMaximumSize(size);
            
            // Set foreground and background colors according to 
            // selection status.
            Box item = this;
            Color foregroundColor, backgroundColor;
            if (isSelected) {
                backgroundColor = list.getSelectionBackground();
                foregroundColor = list.getSelectionForeground();
            }
            else {
                backgroundColor = list.getBackground();
                foregroundColor = list.getForeground();
            }
            categoryNameLabel.setBackground(backgroundColor);
            categoryNameLabel.setForeground(foregroundColor);
            iconLabel.setBackground(backgroundColor);
            iconLabel.setForeground(foregroundColor);
            categoryNameLabel.setOpaque(isSelected);
            iconLabel.setOpaque(isSelected);
            
            item.setEnabled(list.isEnabled());
            item.setFont(list.getFont());
            item.setOpaque(true);
            return item;
        }
    }

    /* (non-Javadoc)
     * @see javax.swing.Scrollable#getPreferredScrollableViewportSize()
     */
    public Dimension getPreferredScrollableViewportSize()
    {
        // Limit the preferred viewport width to the preferred width
        Dimension d = super.getPreferredScrollableViewportSize();
        Dimension preferredSize = getPreferredSize();
        
        d.height = Math.max(d.height, preferredHeight);
        d.width = Math.min(d.width, preferredSize.width);
        return d;
    }
}
