/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010,2012  Poul Henriksen and Michael Kolling 
 
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

import greenfoot.gui.EditableList;
import greenfoot.gui.MessageDialog;
import greenfoot.util.GreenfootUtil;
import greenfoot.util.Selectable;

import java.awt.Color;
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
import java.util.Arrays;
import java.util.LinkedList;

import javax.imageio.ImageIO;
import javax.swing.DefaultCellEditor;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

import bluej.BlueJTheme;
import bluej.Config;

/**
 * A list component which displays a list of images (found in a directory) with their
 * filenames.
 * 
 * @author Davin McCall
 * @author Poul Henriksen
 */
public class ImageLibList extends EditableList<ImageLibList.ImageListEntry> implements Selectable<File>
{   
    /** The directory whose images are currently displayed in this list */
    private File directory;
    
    /** The preferred height of this list */
    private int prefHeight = 200;
    
    private LinkedList<ImageListEntry> data;
    
    /** The frame containing this ImageLibList */
    private ImageLibFrame imageLibFrame;
    
    /** The minimum width we'd like */
    private int minWidth;
    
    /**
     * Construct an empty ImageLibList.
     */
    public ImageLibList(final boolean editable, ImageLibFrame imageLibFrame)
    {      
        super(editable);
        this.imageLibFrame = imageLibFrame;
        
        JLabel widthCalc = new JLabel("XXXXXXXXXXXXXXXXXXXXXXX");
        minWidth = widthCalc.getPreferredSize().width;
        
        TableColumn tableColumn = getColumnModel().getColumn(0);
        tableColumn.setCellRenderer(new MyCellRenderer());
        
        tableColumn.setCellEditor(new MyCellEditor(new JTextField()));
        if (!editable) {
            // A double-click executes the default action for the enclosing
            // frame. But only if it is not editable, because if it is, double
            // click will be used to initiate and edit.
            addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e)
                {
                    if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() >= 2) {
                        if (getSelectedValue() != null && getRootPane().getDefaultButton() != null) {
                            getRootPane().getDefaultButton().doClick();
                        }
                    }
                }
            });
        }
    }

    /**
     * Construct an empty ImageLibList, and populate it with entries from
     * the given directory.
     * 
     * @param directory  The directory to retrieve images from
     */
    public ImageLibList(File directory, final boolean editable, ImageLibFrame imageLibFrame)
    {
        this(editable, imageLibFrame);
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
        this.directory = directory;
        
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
        
        Arrays.sort(imageFiles);
                
        data = new LinkedList<ImageListEntry>();
        data.add(new ImageListEntry(null));
        
        for (int i = 0; i < imageFiles.length; i++) {
            ImageListEntry entry = new ImageListEntry(imageFiles[i]);
            data.add(entry);
            Icon icon = entry.imageIcon;
            if (icon != null) {
                int height = entry.imageIcon.getIconHeight();
                if (height > getRowHeight()) {
                    setRowHeight(height);
                }
            }
        }
        setListData(data);
    }

    /**
     * Get the current directory this list is pointing to.
     */
    public File getDirectory()
    {
        return directory;
    }

    /**
     * Refresh the contents of the list.
     */
    public void refresh()
    {
        if(getDirectory()!=null) {
            setDirectory(getDirectory());
        }
    }
    
    /**
     * Refreshes the previews of any existing images in the list
     * that have been modified since the previews were last loaded
     */
    public void refreshPreviews()
    {
        boolean anyReloaded = false;
        for (ImageListEntry entry : data)
        {
            boolean reloaded = entry.refreshPreview();
            anyReloaded |= reloaded;
        }
        
        if (anyReloaded) {
            repaint();
        }
    }

    /**
     * If the given file exists in this list, it will be selected.
     */
    public void select(File imageFile)
    {
        refresh();
        int row = setSelectedValue(new ImageListEntry(imageFile, false));
        ensureIndexIsVisible(row);
    }
        
    public Dimension getPreferredScrollableViewportSize()
    {
        // Limit the preferred viewport height to the preferred height
        Dimension d = super.getPreferredScrollableViewportSize();
        if(d.height > prefHeight) {
            d.height = prefHeight;
        }
        return d;
    }
    
    @Override
    public Dimension getPreferredSize()
    {
        Dimension d = super.getPreferredSize();
        d.width = Math.max(d.width, minWidth);
        return d;
    }
    
    /*
     * @see greenfoot.gui.EditableList#getSelectedValues()
     */
    public ImageListEntry[] getSelectedValues()
    {
        Object[] list = super.getSelectedValues();
        ImageListEntry[] entries = new ImageListEntry[list.length];
        for (int i = 0; i < list.length; i++) {
            entries[i] = (ImageListEntry) list[i];            
        }  
        return entries;
    }
    
    private static class MyCellEditor extends DefaultCellEditor          
    {
        /**
         * Constructs an editor that uses a text field.
         * 
         * @param textField a <code>JTextField</code> object
         */
        public MyCellEditor(final JTextField textField)
        {
            // Use the default JTextField editor, but with out own delegate
            // installed.
            super(textField);
            textField.removeActionListener(delegate);
            delegate = new EditorDelegate() {
                private ImageListEntry value;

                public void setValue(Object value)
                {
                    this.value = (ImageListEntry) value;
                    textField.setText((this.value != null) ? this.value.imageFile.getName() : "");
                }

                public Object getCellEditorValue()
                {
                    String fileName = textField.getText();
                    File oldFile = value.imageFile;
                    File newFile = new File(oldFile.getParent(), fileName);
                    if (!oldFile.equals(newFile) && newFile.exists()) {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run()
                            {
                                MessageDialog msg = new MessageDialog((JFrame) null,
                                        Config.getString("imagelib.rename.exists.text"),
                                        Config.getString("imagelib.rename.exists.title"), 100, new JButton[]{BlueJTheme.getCloseButton()});
                                msg.displayModal();
                            }
                        });
                    }
                    else {
                        boolean success = oldFile.renameTo(newFile);
                        if (success) {
                            value.imageFile = newFile;
                        } else {
                        }
                    }
                    return value;
                }
            };
            textField.addActionListener(delegate);
        }
    }

    private static class MyCellRenderer extends DefaultTableCellRenderer         
    {
        protected static Border noFocusBorder = new EmptyBorder(1, 1, 1, 1); 

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column)
        {
            JLabel item = this;
            if(value != null) {
                ImageListEntry entry = (ImageListEntry) value;
                if (null == entry.imageFile) {
                    item.setText("No image");
                } else {
                    item.setText(entry.imageFile.getName());
                }
                item.setIcon(entry.imageIcon);
            }
            if (isSelected) {
                item.setBackground(table.getSelectionBackground());
                item.setForeground(table.getSelectionForeground());
            }
            else {
                item.setBackground(table.getBackground());
                item.setForeground(table.getForeground());
            }
            item.setEnabled(table.isEnabled());
            item.setFont(table.getFont());
            
            if (hasFocus) {
                Border border = null;
                if (isSelected) {
                    border = UIManager.getBorder("Table.focusSelectedCellHighlightBorder");
                }
                if (border == null) {
                    border = UIManager.getBorder("Table.focusCellHighlightBorder");
                }
                item.setBorder(border);

                if (!isSelected && table.isCellEditable(row, column)) {
                    Color col;
                    col = UIManager.getColor("Table.focusCellForeground");
                    if (col != null) {
                        super.setForeground(col);
                    }
                    col = UIManager.getColor("Table.focusCellBackground");
                    if (col != null) {
                        super.setBackground(col);
                    }
                }
            }
            else {
                item.setBorder(noFocusBorder);
            }
            
            item.setOpaque(true);
            return item;       
        }        
      
        public Dimension getPreferredSize()
        {
            Dimension d = super.getPreferredSize();
            d.width += BlueJTheme.generalSpacingWidth;
            return d;
        }
    }
    
    
    public class ImageListEntry
    {
        public File imageFile;
        public Icon imageIcon;
        private long lastModified;
        
        private ImageListEntry(File file)
        {
            this(file, true);
        }
        
        /**
         * Checks if the file has been modified since the preview was last generated,
         * and reloads if it has been modified.
         * 
         * @return true if the preview did need to be reloaded
         */
        public boolean refreshPreview()
        {
            if (imageFile != null &&
                imageFile.lastModified() != lastModified)
            {
                loadPreview();
                return true;
            }
            return false;
        }

        private ImageListEntry(File file, boolean loadImage)
        {
            imageFile = file;
            
            if (loadImage) {
                loadPreview();
            }
        }

        private void loadPreview()
        {
            if (imageFile != null) {
                lastModified = imageFile.lastModified();
                int dpi = Toolkit.getDefaultToolkit().getScreenResolution();
    
                try {
                    BufferedImage image = ImageIO.read(imageFile);
                    if (image != null) {
                        Image scaledImage = GreenfootUtil.getScaledImage(image, dpi / 3, dpi / 3);
                        imageIcon = new ImageIcon(scaledImage);
                    }
                    
                    imageLibFrame.imageFileRefreshed(imageFile, image);
                }
                catch (IllegalArgumentException iae) {
                    // Some versions of the JDK seem to throw this when the image is malformed.
                }
                catch (IOException ioe) {}
            }
        }
        
        public boolean equals(Object other) 
        {
            if(!(other instanceof ImageListEntry)) {
                return false;
            }
            ImageListEntry otherEntry = (ImageListEntry) other;
            //other cannot be null here because it passed the instanceof check above:
            if (otherEntry.imageFile == null || this.imageFile == null) {
                return false;
            }
            else {
                return otherEntry.imageFile.equals(this.imageFile);
            }
        }
        
        public int hashCode() 
        {
            return imageFile.hashCode();
        }
    }

}
