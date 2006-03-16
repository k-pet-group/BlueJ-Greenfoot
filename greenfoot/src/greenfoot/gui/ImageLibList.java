package greenfoot.gui;

import greenfoot.util.GreenfootUtil;

import java.awt.Component;
import java.awt.Image;
import java.awt.Toolkit;
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
 * @version $Id: ImageLibList.java 3830 2006-03-16 05:36:04Z davmac $
 */
public class ImageLibList extends JList
{
    public ImageLibList(File directory)
    {
        DefaultListModel listModel = new DefaultListModel();
        setModel(listModel);
        setCellRenderer(new MyCellRenderer());
        setLayoutOrientation(JList.VERTICAL);
        
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
            String fileName = imageFiles[i].getName();
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
            //JLabel item = (JLabel) value;
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
