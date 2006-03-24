package greenfoot.gui;

import greenfoot.core.GClass;
import greenfoot.core.Greenfoot;
import greenfoot.gui.classbrowser.ClassView;
import greenfoot.util.GreenfootUtil;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import bluej.BlueJTheme;
import bluej.Config;
import bluej.extensions.ProjectNotOpenException;
import bluej.utility.DialogManager;
import bluej.utility.EscapeDialog;

/**
 * A dialog for selecting a class image. The image can be selected from either the
 * project image library, or the greenfoot library, or an external location.
 * 
 * @author Davin McCall
 * @version $Id: ImageLibFrame.java 3865 2006-03-24 00:08:15Z davmac $
 */
public class ImageLibFrame extends EscapeDialog implements ListSelectionListener
{
    /** label displaying the currently selected image */
    private JLabel imageLabel;
    private GClass gclass;
    
    private ImageLibList projImageList;
    private ImageLibList greenfootImageList;
    
    private File selectedImageFile;
    private File projImagesDir;
    
    public static int OK = 0;
    public static int CANCEL = 1;
    private int result = CANCEL;
    
    public ImageLibFrame(JFrame owner, ClassView classView)
    {
        // TODO i18n
        // super("Select class image: " + classView.getClassName());
        super(owner, "Select class image: " + classView.getClassName(), true);
        // setIconImage(BlueJTheme.getIconImage());
        
        this.gclass = classView.getGClass();
        
        buildUI();
    }
    
    /**
     * Construct an ImageLibFrame to be used for creating a new class.
     * 
     * @param owner   The parent frame
     */
    public ImageLibFrame(JFrame owner)
    {
        super(owner, "New class", true);
        
        // this.classView = new ClassView()
    }
    
    private void buildUI()
    {
        JPanel contentPane = new JPanel();
        this.setContentPane(contentPane);
        contentPane.setLayout(new BoxLayout(this.getContentPane(), BoxLayout.Y_AXIS));
        contentPane.setBorder(BlueJTheme.dialogBorder);
        
        int spacingLarge = BlueJTheme.componentSpacingLarge;
        int spacingSmal = BlueJTheme.componentSpacingSmall;
        
        // Show current image
        {
            JPanel currentImagePanel = new JPanel();
            currentImagePanel.setLayout(new BoxLayout(currentImagePanel, BoxLayout.X_AXIS));
            
            JLabel classImageLabel = new JLabel("Class Image:");
            currentImagePanel.add(classImageLabel);
            
            currentImagePanel.add(Box.createHorizontalStrut(spacingLarge));
            
            Icon icon = getClassIcon(gclass);
            imageLabel = new JLabel(icon);
            currentImagePanel.add(imageLabel);
            currentImagePanel.setAlignmentX(0.0f);
            
            contentPane.add(fixHeight(currentImagePanel));
        }
        
        contentPane.add(fixHeight(Box.createVerticalStrut(spacingLarge)));
        
        // Image selection panels - project and greenfoot image library
        {
            JPanel imageSelPanels = new JPanel();
            imageSelPanels.setLayout(new GridLayout(1, 2, BlueJTheme.componentSpacingSmall, 0));
            
            // Project images panel
            {
                Box piPanel = new Box(BoxLayout.Y_AXIS);
                
                JLabel piLabel = new JLabel("Project images:");
                piLabel.setAlignmentX(0.0f);
                piPanel.add(piLabel);
                
                JScrollPane jsp = new JScrollPane();
                
                try {
                    Greenfoot greenfootInstance = Greenfoot.getInstance();
                    File projDir = greenfootInstance.getProject().getDir();
                    projImagesDir = new File(projDir, "images");
                    projImageList = new ImageLibList(projImagesDir);
                    jsp.getViewport().setView(projImageList);
                }
                catch (ProjectNotOpenException pnoe) {}
                catch (RemoteException re) { re.printStackTrace(); }
                
                jsp.setBorder(Config.normalBorder);
                jsp.setAlignmentX(0.0f);
                
                piPanel.add(jsp);
                imageSelPanels.add(piPanel);
            }
            
            // Greenfoot images panel
            {
                Box piPanel = new Box(BoxLayout.Y_AXIS);
                
                JLabel piLabel = new JLabel("Greenfoot images:");
                piLabel.setAlignmentX(0.0f);
                piPanel.add(piLabel);
                
                JScrollPane jsp = new JScrollPane();
                
                File imageDir = Config.getBlueJLibDir();
                imageDir = new File(imageDir, "imagelib");
                greenfootImageList = new ImageLibList(imageDir);
                jsp.getViewport().setView(greenfootImageList);
                
                jsp.setBorder(Config.normalBorder);
                jsp.setAlignmentX(0.0f);
                
                piPanel.add(jsp);
                imageSelPanels.add(piPanel);
            }
            
            imageSelPanels.setAlignmentX(0.0f);
            contentPane.add(imageSelPanels);
            
            projImageList.addListSelectionListener(this);
            greenfootImageList.addListSelectionListener(this);
        }

        // Browse button. Select image file from arbitrary location.
        JButton browseButton = new JButton("Browse for more images ...");
        browseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                JFileChooser chooser = new JFileChooser();
                new ImageFilePreview(chooser);
                int choice = chooser.showDialog(ImageLibFrame.this, "Select");
                if (choice == JFileChooser.APPROVE_OPTION) {
                    selectedImageFile = chooser.getSelectedFile();
                    imageLabel.setIcon(getPreviewIcon(selectedImageFile));
                }
            }
        });
        browseButton.setAlignmentX(0.0f);
        contentPane.add(fixHeight(Box.createVerticalStrut(spacingLarge)));
        contentPane.add(fixHeight(browseButton));
        
        contentPane.add(fixHeight(Box.createVerticalStrut(spacingLarge)));
        contentPane.add(fixHeight(new JSeparator()));
        
        // Ok and cancel buttons
        {
            JPanel okCancelPanel = new JPanel();
            okCancelPanel.setLayout(new BoxLayout(okCancelPanel, BoxLayout.X_AXIS));

            JButton okButton = BlueJTheme.getOkButton();
            okButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e)
                {
                    result = OK;
                    setVisible(false);
                    dispose();
                }
            });
            
            JButton cancelButton = BlueJTheme.getCancelButton();
            cancelButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    result = CANCEL;
                    selectedImageFile = null;
                    setVisible(false);
                    dispose();
                }
            });
            
            okCancelPanel.add(Box.createHorizontalGlue());
            okCancelPanel.add(okButton);
            okCancelPanel.add(Box.createHorizontalStrut(spacingLarge));
            okCancelPanel.add(cancelButton);
            okCancelPanel.setAlignmentX(0.0f);
            okCancelPanel.validate();
            contentPane.add(fixHeight(Box.createVerticalStrut(spacingLarge)));
            contentPane.add(fixHeight(okCancelPanel));
            
            getRootPane().setDefaultButton(okButton);
        }
        
        pack();
        DialogManager.centreDialog(this);
        setVisible(true);
    }
    
    /*
     * A new image was selected in one of the ImageLibLists
     */
    public void valueChanged(ListSelectionEvent lse)
    {
        Object source = lse.getSource();
        if (! lse.getValueIsAdjusting() && source instanceof ImageLibList) {
            ImageLibList sourceList = (ImageLibList) source;
            ImageLibList.ImageListEntry ile = sourceList.getSelectedEntry();
            imageLabel.setIcon(getPreviewIcon(ile.imageFile));
            selectedImageFile = ile.imageFile;
        }
    }
    
    /**
     * Get a preview icon for a class. This is a fixed size image.
     * 
     * @param gclass   The class whose icon to get
     */
    public static Icon getClassIcon(GClass gclass)
    {
        String imageName = gclass.getClassProperty("image");
        if (imageName == null) {
            imageName = "greenfoot-logo.png";
        }
        File imageFile = new File(new File("images"), imageName);
        return getPreviewIcon(imageFile);
    }
    
    /**
     * Load an image from a file and scale it to preview size.
     * @param fname  The file to load the image from
     */
    private static Icon getPreviewIcon(File fname)
    {
        int dpi = Toolkit.getDefaultToolkit().getScreenResolution();
        
        try {
            BufferedImage bi = ImageIO.read(fname);
            return new ImageIcon(GreenfootUtil.getScaledImage(bi, dpi/2, dpi/2));
        }
        catch (IOException ioe) {
            BufferedImage bi = new BufferedImage(dpi/2, dpi/2, BufferedImage.TYPE_INT_ARGB);
            return new ImageIcon(bi);
        }
    }
    
    /**
     * Fix the maxiumum height of the component equal to its preferred size, and
     * return the component.
     */
    private static Component fixHeight(Component src)
    {
        Dimension d = src.getMaximumSize();
        d.height = src.getPreferredSize().height;
        src.setMaximumSize(d);
        return src;
    }
    
    /**
     * Get the selected image file (null if dialog was canceled)
     */
    public File getSelectedImageFile()
    {
        if (result == OK) {
            return selectedImageFile;
        }
        else {
            return null;
        }
    }
    
    /**
     * Get the result from the dialog: OK or CANCEL
     */
    public int getResult()
    {
        return result;
    }
}
