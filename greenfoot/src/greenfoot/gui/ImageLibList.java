/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009  Poul Henriksen and Michael Kolling 
 
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
package greenfoot.gui;

import greenfoot.util.GraphicsUtilities;
import greenfoot.util.GreenfootUtil;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;

import javax.imageio.ImageIO;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;

import bluej.BlueJTheme;

/**
 * A list component which displays a list of images (found in a directory) with their
 * filenames.
 * 
 * @author Davin McCall
 * @version $Id: ImageLibList.java 6216 2009-03-30 13:41:07Z polle $
 */
public class ImageLibList extends JList
{
    private DefaultListModel listModel;
    
    /**
     * Construct an empty ImageLibList.
     */
    public ImageLibList()
    {
        listModel = new DefaultListModel();
        setModel(listModel);
        setCellRenderer(new MyCellRenderer());
        setLayoutOrientation(JList.VERTICAL);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // A double-click executes the default action for the enclosing frame
        addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e)
            {
                if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() >= 2) {
                    if (getSelectedEntry() != null) {
                        getRootPane().getDefaultButton().doClick();
                    }
                }
            }
        });
    }
    
    /**
     * Construct an empty ImageLibList, and populate it with entries from
     * the given directory.
     * 
     * @param directory  The directory to retrieve images from
     */
    public ImageLibList(File directory)
    {
        this();
        setDirectory(directory);
    }

    /**
     * Clear the list and re-populate it with images from the given
     * directory.
     * 
     * @param directory   The directory to retrieve images from
     */
    public void setDirectory(File directory)
    {
        listModel.removeAllElements();
        
        FilenameFilter filter = new FilenameFilter() {
            public boolean accept(File dir, String name)
            {
                // We can accept all files. We try to load them all as an image.
                return true;
            }
        };
        
        File [] imageFiles = directory.listFiles(filter);
        if (imageFiles == null) {
            imageFiles = new File[0];
        }
        
        int dpi = Toolkit.getDefaultToolkit().getScreenResolution();
        
        for (int i = 0; i < imageFiles.length; i++) {
            try {
                BufferedImage image = ImageIO.read(imageFiles[i]);
                if (image != null) {
                    Image scaledImage = GreenfootUtil.getScaledImage(image, dpi / 3, dpi / 3);
                    
                    Icon icon = new ImageIcon(scaledImage);
                    ImageListEntry entry = new ImageListEntry(imageFiles[i], icon);
                    listModel.addElement(entry);
                }
            }
            catch (MalformedURLException mfue) { }
            catch (IOException ioe) { }
        }
    }
        
    /**
     * Get the currently selected entry.
     */
    public ImageListEntry getSelectedEntry()
    {
        return (ImageListEntry) getSelectedValue();
    }
        
    private static class MyCellRenderer extends JLabel
        implements ListCellRenderer
    {
//        public MyCellRenderer()
//        {
//           imageLabel = new JLabel();
//           add(imageLabel);
//           add(GreenfootUtil.createSpacer(GreenfootUtil.X_AXIS, BlueJTheme.generalSpacingWidth));
//        }
        
        public Component getListCellRendererComponent(
                JList list,
                Object value,            // value to display
                int index,               // cell index
                boolean isSelected,      // is the cell selected
                boolean cellHasFocus)    // the list and the cell have the focus
        {
            ImageListEntry entry = (ImageListEntry) value;
            JLabel item = this;
            
            item.setText(entry.imageFile.getName());
            item.setIcon(entry.imageIcon);
            
            if (isSelected) {
                item.setBackground(list.getSelectionBackground());
                item.setForeground(list.getSelectionForeground());
            }
            else {
                item.setBackground(list.getBackground());
                item.setForeground(list.getForeground());
            }
            item.setEnabled(list.isEnabled());
            item.setFont(list.getFont());
            item.setOpaque(true);
            return this;
        }
        
        public Dimension getPreferredSize()
        {
            // TODO Auto-generated method stub
            //return super.getPreferredSize();
            Dimension d = super.getPreferredSize();
            d.width += BlueJTheme.generalSpacingWidth;
            return d;
        }
    }
    
    public static class ImageListEntry
    {
        public File imageFile;
        public Icon imageIcon;
        
        private ImageListEntry(File file, Icon icon)
        {
            imageFile = file;
            imageIcon = icon;
        }
    }
    
    /* (non-Javadoc)
     * @see java.awt.Component#getPreferredSize()
     */
    public Dimension getPreferredSize()
    {
        if (getModel().getSize() != 0) {
            return super.getPreferredSize();
        }
        else {
            // If there are no items in the list, try and guess an
            // appropriate preferred with. The default guess isn't very
            // good.
            Dimension d = super.getPreferredSize();
            ListCellRenderer renderer = getCellRenderer();
            
            int dpi = Toolkit.getDefaultToolkit().getScreenResolution();
            ImageListEntry fakeEntry = new ImageListEntry(new File("abcdefghijklmnopqrstuvw.jpg"), new ImageIcon(GraphicsUtilities.createCompatibleTranslucentImage(dpi/3, dpi/3)));
            Component component = renderer.getListCellRendererComponent(this, fakeEntry, 0, false, false);
            d.width = component.getPreferredSize().width;
            return d;
        }
    }
    
    /* (non-Javadoc)
     * @see javax.swing.Scrollable#getPreferredScrollableViewportSize()
     */
    public Dimension getPreferredScrollableViewportSize()
    {
        // Limit the preferred viewport width to the preferred width
        Dimension d = super.getPreferredScrollableViewportSize();
        d.width = Math.min(d.width, getPreferredSize().width);
        return d;
    }

}
