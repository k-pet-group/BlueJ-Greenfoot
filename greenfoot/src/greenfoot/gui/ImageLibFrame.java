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
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.BadLocationException;

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
 * @version $Id: ImageLibFrame.java 3867 2006-03-24 04:51:45Z davmac $
 */
public class ImageLibFrame extends EscapeDialog implements ListSelectionListener
{
    /** label displaying the currently selected image */
    private JLabel imageLabel;
    private GClass gclass;
    private Icon defaultIcon;
    
    private ImageLibList projImageList;
    private ImageLibList greenfootImageList;
    private Action okAction;
    
    private File selectedImageFile;
    private File projImagesDir;
    private String className;
    
    public static int OK = 0;
    public static int CANCEL = 1;
    private int result = CANCEL;
    
    /**
     * Construct an ImageLibFrame for changing the image of an existing class.
     * 
     * @param owner      The parent frame
     * @param classView  The ClassView of the existing class
     */
    public ImageLibFrame(JFrame owner, ClassView classView)
    {
        // TODO i18n
        // super("Select class image: " + classView.getClassName());
        super(owner, "Select class image: " + classView.getClassName(), true);
        // setIconImage(BlueJTheme.getIconImage());
        
        this.gclass = classView.getGClass();
        defaultIcon = getPreviewIcon(new File(new File("images"), "greenfoot-logo.png"));
        
        buildUI(false);
    }
    
    /**
     * Construct an ImageLibFrame to be used for creating a new class.
     * 
     * @param owner        The parent frame
     * @param superClass   The superclass of the new class
     */
    public ImageLibFrame(JFrame owner, GClass superClass)
    {
        super(owner, "New class", true);
        
        defaultIcon = getClassIcon(superClass, getPreviewIcon(new File(new File("images"), "greenfoot-logo.png")));
        
        // this.classView = new ClassView()
        buildUI(true);
    }
    
    private void buildUI(boolean includeClassNameField)
    {
        JPanel contentPane = new JPanel();
        this.setContentPane(contentPane);
        contentPane.setLayout(new BoxLayout(this.getContentPane(), BoxLayout.Y_AXIS));
        contentPane.setBorder(BlueJTheme.dialogBorder);
        
        int spacingLarge = BlueJTheme.componentSpacingLarge;
        int spacingSmal = BlueJTheme.componentSpacingSmall;
        
        okAction = new AbstractAction("Ok") {
            public void actionPerformed(ActionEvent e)
            {
                result = OK;
                setVisible(false);
                dispose();
            }
        };
        
        {
            JPanel classDetailsPanel = new JPanel();
            classDetailsPanel.setLayout(new BoxLayout(classDetailsPanel, BoxLayout.Y_AXIS));
            
            // Show current image
            {
                JPanel currentImagePanel = new JPanel();
                currentImagePanel.setLayout(new BoxLayout(currentImagePanel, BoxLayout.X_AXIS));
                
                if (includeClassNameField) {
                    Box b = new Box(BoxLayout.X_AXIS);
                    JLabel classNameLabel = new JLabel("New class name:");
                    b.add(classNameLabel);
                    
                    // "ok" button should be disabled until class name entered
                    okAction.setEnabled(false);
                    
                    final JTextField classNameField = new JTextField(12);
                    classNameField.getDocument().addDocumentListener(new DocumentListener() {
                        private void change()
                        {
                            int length = classNameField.getDocument().getLength();
                            okAction.setEnabled(length != 0);
                            try {
                                className = classNameField.getDocument().getText(0, length);
                            }
                            catch (BadLocationException ble) {}
                        }
                        
                        public void changedUpdate(DocumentEvent e)
                        {
                            // Nothing to do
                        }
                        
                        public void insertUpdate(DocumentEvent e)
                        {
                            change();
                        }
                        
                        public void removeUpdate(DocumentEvent e)
                        {
                            change();
                        }
                    });
                    
                    b.add(Box.createHorizontalStrut(spacingLarge));
                    b.add(fixHeight(classNameField));
                    b.setAlignmentX(0.0f);
                    
                    classDetailsPanel.add(b);
                    classDetailsPanel.add(Box.createVerticalStrut(spacingLarge));
                }
                
                JLabel classImageLabel = new JLabel("Class Image:");
                currentImagePanel.add(classImageLabel);
                
                currentImagePanel.add(Box.createHorizontalStrut(spacingLarge));
                
                Icon icon = getClassIcon(gclass, defaultIcon);
                imageLabel = new JLabel(icon);
                currentImagePanel.add(imageLabel);
                currentImagePanel.setAlignmentX(0.0f);
                
                classDetailsPanel.add(fixHeight(currentImagePanel));
            }
            
            classDetailsPanel.setAlignmentX(0.0f);
            contentPane.add(fixHeight(classDetailsPanel));
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
            okButton.setAction(okAction);
            
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
    private static Icon getClassIcon(GClass gclass, Icon defaultIcon)
    {
        String imageName = null;
        
        if (gclass == null) {
            return defaultIcon;
        }
        
        while (gclass != null) {
            imageName = gclass.getClassProperty("image");
            
            // If an image is specified for this class, and we can read it, return
            if (imageName != null) {
                File imageFile = new File(new File("images"), imageName);
                if (imageFile.canRead()) {
                    return getPreviewIcon(imageFile);
                }
            }
            
            gclass = gclass.getSuperclass();
        }
        
        return defaultIcon;
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
    
    public String getClassName()
    {
        return className;
    }
}
