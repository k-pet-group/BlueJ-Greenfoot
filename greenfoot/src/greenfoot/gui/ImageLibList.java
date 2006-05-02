package greenfoot.gui;

import greenfoot.util.GreenfootUtil;

import java.awt.Component;
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
import javax.swing.*;

/**
 * A list component which displays a list of images (found in a directory) with their
 * filenames.
 * 
 * @author Davin McCall
 * @version $Id: ImageLibList.java 4076 2006-05-02 14:32:42Z davmac $
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
            return item;
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
}
