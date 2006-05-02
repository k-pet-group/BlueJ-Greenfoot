package greenfoot.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.io.File;
import java.io.FileFilter;

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
 * @version $Id: ImageCategorySelector.java 4076 2006-05-02 14:32:42Z davmac $
 */
public class ImageCategorySelector extends JList
    implements ListSelectionListener
{
    private ImageLibList imageLibList;
    
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

        for (int i = 0; i < imageFiles.length; i++) {
            listModel.addElement(imageFiles[i]);
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
            categoryNameLabel = new JLabel();
            categoryNameLabel.setOpaque(true);
            add(categoryNameLabel);
            iconLabel = new JLabel(openRightIcon);
            iconLabel.setOpaque(true);
            
            // Set maximum size on the icon label so that the category
            // name label uses up all the extra space
            Dimension iconSize = iconLabel.getPreferredSize();
            iconLabel.setMaximumSize(iconSize);
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
            size.width = list.getWidth();
            categoryNameLabel.setPreferredSize(size);
            
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
            
            item.setEnabled(list.isEnabled());
            item.setFont(list.getFont());
            item.setOpaque(true);
            return item;
        }
    }

}
