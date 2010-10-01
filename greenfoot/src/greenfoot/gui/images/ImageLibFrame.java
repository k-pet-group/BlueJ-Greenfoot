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

import greenfoot.actions.BrowseImagesAction;
import greenfoot.core.GClass;
import greenfoot.core.GPackage;
import greenfoot.core.GProject;
import greenfoot.event.ValidityEvent;
import greenfoot.event.ValidityListener;
import greenfoot.gui.ClassNameVerifier;
import greenfoot.gui.classbrowser.ClassView;
import greenfoot.gui.images.ImageLibList.ImageListEntry;
import greenfoot.util.ExternalAppLauncher;
import greenfoot.util.GraphicsUtilities;
import greenfoot.util.GreenfootUtil;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.swing.Timer;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import bluej.BlueJTheme;
import bluej.Config;
import bluej.prefmgr.PrefMgr;
import bluej.utility.EscapeDialog;
import bluej.utility.FileUtility;

/**
 * A (modal) dialog for selecting a class image. The image can be selected from either the
 * project image library, or the greenfoot library, or an external location.
 *
 * @author Davin McCall
 */
public class ImageLibFrame extends EscapeDialog implements ListSelectionListener, WindowListener
{
    /** Label displaying the currently selected image. */
    private JLabel imageLabel;
    private JLabel imageTextLabel;
    private GClass gclass;
    private GProject proj;
    /** The default image icon - the greenfoot logo */
    private Icon defaultIcon;
    private JScrollPane imageScrollPane;
    
    private ImageLibList projImageList;
    private ImageLibList greenfootImageList;
    private Action okAction;

    private File selectedImageFile;
    private File projImagesDir;

    public static final int OK = 0;
    public static final int CANCEL = 1;
    private int result = CANCEL;

    private JTextField classNameField;
    
    /** Menu items that are in the drop down button,
     *  which we want to alter the enabled state of. */
    private JMenuItem editItem;
    private JMenuItem duplicateItem;
    private JMenuItem deleteItem;

    private Timer refreshTimer;
    
    /** Suffix used when creating a copy of an existing image (duplicate) */
    private static final String COPY_SUFFIX = "Copy"; //TODO move to labels
    
    /** JPopupMenu icon */
    private static final String DROPDOWN_ICON_FILE = "menu-button.png";
    
    /** A watcher that goes notified when an image is selected, to allow for previewing. May be null */
    private ImageSelectionWatcher selectionWatcher;

    /**
     * Construct an ImageLibFrame for changing the image of an existing class.
     *
     * @param owner      The parent frame
     * @param classView  The ClassView of the existing class
     */
    public ImageLibFrame(JFrame owner, ClassView classView, ImageSelectionWatcher watcher)
    {
        super(owner, Config.getString("imagelib.title") + " " + classView.getClassName(), true);

        this.selectionWatcher = watcher;
        this.gclass = classView.getGClass();
        this.proj = gclass.getPackage().getProject();
        defaultIcon = getPreviewIcon(new File(GreenfootUtil.getGreenfootLogoPath()));

        buildUI(proj, false);       
    }

    /**
     * Construct an ImageLibFrame to be used for creating a new class.
     *
     * @param owner        The parent frame
     * @param superClass   The superclass of the new class
     */
    public ImageLibFrame(JFrame owner, GClass superClass)
    {
        super(owner, Config.getString("imagelib.newClass"), true);
        this.gclass = superClass;
        this.proj = gclass.getPackage().getProject();
        defaultIcon = getClassIcon(superClass, getPreviewIcon(new File(GreenfootUtil.getGreenfootLogoPath())));
        
        buildUI(proj, true);        
    }

    private void buildUI(GProject project, final boolean includeClassNameField)
    {
        this.addWindowListener(this);
        JPanel contentPane = new JPanel();
        this.setContentPane(contentPane);
        contentPane.setLayout(new BoxLayout(this.getContentPane(), BoxLayout.Y_AXIS));
        contentPane.setBorder(BlueJTheme.dialogBorder);

        // int spacingSmall = BlueJTheme.componentSpacingSmall;
        int spacingLarge = BlueJTheme.componentSpacingLarge;

        okAction = getOkAction();

        // Class details - name, current icon
        contentPane.add(buildClassDetailsPanel(includeClassNameField, project.getDefaultPackage()));

        // Image selection panels - project and greenfoot image library
        {
            Box imageSelPanels = new Box(BoxLayout.X_AXIS);

            // Project images panel
            {
                Box piPanel = new Box(BoxLayout.Y_AXIS);

                JLabel piLabel = new JLabel(Config.getString("imagelib.projectImages"));
                piLabel.setAlignmentX(0.0f);
                piPanel.add(piLabel);

                imageScrollPane = new JScrollPane();

                File projDir = project.getDir();
                projImagesDir = new File(projDir, "images");
                projImageList = new ImageLibList(projImagesDir, false, this);
                imageScrollPane.getViewport().setView(projImageList);

                imageScrollPane.setBorder(Config.normalBorder);
                imageScrollPane.setViewportBorder(BorderFactory.createLineBorder(projImageList.getBackground(), 4));
                imageScrollPane.setAlignmentX(0.0f);

                piPanel.add(imageScrollPane);
                imageSelPanels.add(piPanel);
            }

            imageSelPanels.add(GreenfootUtil.createSpacer(GreenfootUtil.X_AXIS, spacingLarge));
            
            // Category selection panel
            File imageDir = new File(Config.getGreenfootLibDir(), "imagelib");
            ImageCategorySelector imageCategorySelector = new ImageCategorySelector(imageDir);

            // List of images
            greenfootImageList = new ImageLibList(false, this);
            
            JComponent greenfootLibPanel = new GreenfootImageLibPanel(imageCategorySelector, greenfootImageList);
            
            imageSelPanels.add(greenfootLibPanel);
            
            imageSelPanels.setAlignmentX(0.0f);
            contentPane.add(imageSelPanels);

            projImageList.addListSelectionListener(this);
            greenfootImageList.addListSelectionListener(this);
            imageCategorySelector.setImageLibList(greenfootImageList);
        }
        
        // Creates the PopupMenuButton, adding the edit, duplicate, delete and new
        // menu items, along with their actions to it. Also creates the browse button
        // and adds both of these components to a flow panel to display in the content
        // panel.
        {
            JPanel borderPanel = new JPanel();
            BorderLayout layout = new BorderLayout();
            borderPanel.setLayout(layout);
            
            JPopupMenu popupMenu = new JPopupMenu();
            
            editItem = new JMenuItem(Config.getString("imagelib.edit"));
            editItem.setToolTipText(Config.getString("imagelib.edit.tooltip")); 
            editItem.setEnabled(false);
            editItem.setFont(PrefMgr.getPopupMenuFont());
            editItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e)
                {
                    ImageListEntry entry = projImageList.getSelectedValue();
                    if (entry != null && entry.imageFile != null) {
                        ExternalAppLauncher.editImage(entry.imageFile);
                    }
                }
            });
            
            duplicateItem = new JMenuItem(Config.getString("imagelib.duplicate"));
            duplicateItem.setToolTipText(Config.getString("imagelib.duplicate.tooltip")); 
            duplicateItem.setEnabled(false);
            duplicateItem.setFont(PrefMgr.getPopupMenuFont());
            duplicateItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e)
                {
                    ImageListEntry entry = projImageList.getSelectedValue();
                    if (entry != null && entry.imageFile != null) {
                        duplicateSelected(entry);
                    }
                }
            });
            
            deleteItem = new JMenuItem(Config.getString("imagelib.delete"));
            deleteItem.setToolTipText(Config.getString("imagelib.delete.tooltip"));
            deleteItem.setEnabled(false);
            deleteItem.setFont(PrefMgr.getPopupMenuFont());
            deleteItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e)
                {
                    ImageListEntry entry = projImageList.getSelectedValue();
                    if (entry != null && entry.imageFile != null) {
                        confirmDelete(entry);
                    }
                }
            });

            JMenuItem newImageItem = new JMenuItem(Config.getString("imagelib.create.button"));
            newImageItem.setToolTipText(Config.getString("imagelib.create.tooltip")); 
            newImageItem.setFont(PrefMgr.getPopupMenuFont());
            newImageItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e)
                {
                    String name = includeClassNameField ? getClassName() : gclass.getName();
                    NewImageDialog newImage = new NewImageDialog(ImageLibFrame.this, projImagesDir, name);
                    final File file = newImage.displayModal();
                    if (file != null) {
                        projImageList.refresh();
                        projImageList.select(file);
                        selectImage(file);
                    }                                           
                }                
            });
            
            popupMenu.add(fixHeight(editItem));
            popupMenu.add(fixHeight(duplicateItem));
            popupMenu.add(fixHeight(deleteItem));
            popupMenu.add(fixHeight(newImageItem));
            
            JButton dropDownButton = new PopupMenuButton(
                    new ImageIcon(ImageLibFrame.class.getClassLoader().getResource(DROPDOWN_ICON_FILE)), 
                    popupMenu);
            JButton browseButton = new JButton(
                    new BrowseImagesAction(Config.getString("imagelib.browse.button"), this,
                    projImagesDir, projImageList));
        
           
            borderPanel.setAlignmentX(0.0f);
            borderPanel.add(fixHeight(dropDownButton), BorderLayout.LINE_START);
            borderPanel.add(fixHeight(browseButton), BorderLayout.LINE_END);

            contentPane.add(fixHeight(Box.createVerticalStrut(spacingLarge)));
            contentPane.add(fixHeight(borderPanel));
            contentPane.add(fixHeight(Box.createVerticalStrut(spacingLarge)));
            contentPane.add(fixHeight(new JSeparator()));
        }

        // Ok and cancel buttons
        {
            JPanel okCancelPanel = new JPanel();
            okCancelPanel.setLayout(new BoxLayout(okCancelPanel, BoxLayout.X_AXIS));

            JButton okButton = BlueJTheme.getOkButton();
            okButton.setAction(okAction);

            JButton cancelButton = BlueJTheme.getCancelButton();
            cancelButton.setVerifyInputWhenFocusTarget(false);
            cancelButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    result = CANCEL;
                    setVisible(false);
                    dispose();
                }
            });

            okCancelPanel.add(Box.createHorizontalGlue());
            
            if (Config.isMacOS()) {
                okCancelPanel.add(cancelButton);
                okCancelPanel.add(Box.createHorizontalStrut(spacingLarge));
                okCancelPanel.add(okButton);
            }
            else {
                okCancelPanel.add(okButton);
                okCancelPanel.add(Box.createHorizontalStrut(spacingLarge));
                okCancelPanel.add(cancelButton);
            }
            
            okCancelPanel.setAlignmentX(0.0f);
            okCancelPanel.validate();
            contentPane.add(fixHeight(Box.createVerticalStrut(spacingLarge)));
            contentPane.add(fixHeight(okCancelPanel));

            getRootPane().setDefaultButton(okButton);
        }
        
        ActionListener refreshTask = new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                projImageList.refreshPreviews();
            };
        };
            
        refreshTimer = new Timer(2000, refreshTask);
        refreshTimer.start();

        pack();
    }

    /**
     * Build the class details panel.
     *
     * @param includeClassNameField  Whether to include a field for
     *                              specifying the class name.
     * @param pkg
     */
    private JPanel buildClassDetailsPanel(boolean includeClassNameField, GPackage pkg)
    {
        JPanel classDetailsPanel = new JPanel();
        classDetailsPanel.setLayout(new BoxLayout(classDetailsPanel, BoxLayout.Y_AXIS));

        int spacingLarge = BlueJTheme.componentSpacingLarge;
        int spacingSmall = BlueJTheme.componentSpacingSmall;

        // Show current image
        {
            JPanel currentImagePanel = new JPanel();
            currentImagePanel.setLayout(new BoxLayout(currentImagePanel, BoxLayout.X_AXIS));

            if (includeClassNameField) {
                Box b = new Box(BoxLayout.X_AXIS);
                JLabel classNameLabel = new JLabel(Config.getString("imagelib.className"));
                b.add(classNameLabel);

                // "ok" button should be disabled until class name entered
                okAction.setEnabled(false);

                classNameField = new JTextField(12);
                final JLabel errorMsgLabel = new JLabel();
                errorMsgLabel.setVisible(false);
                errorMsgLabel.setForeground(Color.RED);

                final ClassNameVerifier classNameVerifier = new ClassNameVerifier(classNameField, pkg);
                classNameVerifier.addValidityListener(new ValidityListener() {
                    public void changedToInvalid(ValidityEvent e)
                    {
                        errorMsgLabel.setText(e.getReason());
                        errorMsgLabel.setVisible(true);
                        okAction.setEnabled(false);
                    }

                    public void changedToValid(ValidityEvent e)
                    {
                        errorMsgLabel.setVisible(false);
                        okAction.setEnabled(true);
                    }
                });

                b.add(Box.createHorizontalStrut(spacingLarge));

                b.add(fixHeight(classNameField));
                b.setAlignmentX(0.0f);
                classDetailsPanel.add(b);

                classDetailsPanel.add(errorMsgLabel);

                classDetailsPanel.add(Box.createVerticalStrut(spacingLarge));
            }

            // help label
            JLabel helpLabel = new JLabel(Config.getString("imagelib.help.selectImage"));

            Font smallFont = helpLabel.getFont().deriveFont(Font.ITALIC, 11.0f);
            helpLabel.setFont(smallFont);
            classDetailsPanel.add(fixHeight(helpLabel));

            classDetailsPanel.add(fixHeight(Box.createVerticalStrut(spacingLarge)));

            classDetailsPanel.add(fixHeight(new JSeparator()));
            classDetailsPanel.add(Box.createVerticalStrut(spacingSmall));

            // new class image display
            JLabel classImageLabel = new JLabel(Config.getString("imagelib.newClass.image"));
            currentImagePanel.add(classImageLabel);

            Icon icon = getClassIcon(gclass, defaultIcon);
            
            currentImagePanel.add(Box.createHorizontalStrut(spacingSmall));
            imageLabel = new JLabel(icon) {
                // We don't want changing the image to re-layout the
                // whole frame
                public boolean isValidateRoot()
                {
                    return true;
                }
            };
            currentImagePanel.add(imageLabel);
            currentImagePanel.add(Box.createHorizontalStrut(spacingSmall));

            imageTextLabel = new JLabel() {
                // We don't want changing the text to re-layout the
                // whole frame
                public boolean isValidateRoot()
                {
                    return true;
                }
            };
            currentImagePanel.add(imageTextLabel);
            currentImagePanel.setAlignmentX(0.0f);

            classDetailsPanel.add(fixHeight(currentImagePanel));
        }

        classDetailsPanel.setAlignmentX(0.0f);
        return classDetailsPanel;
    }

    /**
     * A new image was selected in one of the ImageLibLists
     */
    public void valueChanged(ListSelectionEvent lse)
    {
        Object source = lse.getSource();
        if (! lse.getValueIsAdjusting() && source instanceof ImageLibList) {
            imageTextLabel.setText("");
            ImageLibList sourceList = (ImageLibList) source;
            ImageLibList.ImageListEntry ile = sourceList.getSelectedValue();

            // handle the no-image image entry.
            if (ile != null && ile.imageFile != null) {
                File imageFile = ile.imageFile;
                selectImage(imageFile);
                setItemButtons(true);
            } else {
                selectImage(null);
                setItemButtons(false);
            }
            
        }

        if (lse.getValueIsAdjusting() && source instanceof ImageLibList) {
            if(lse.getSource()==projImageList) {
                greenfootImageList.clearSelection();
            }
            else {
                projImageList.clearSelection();
            }
        }
    }

    /**
     * Change the three selection based menu items to the
     * parameter provided.
     * @param state To enable or disable the menu item buttons.
     */
    private void setItemButtons(boolean state)
    {
        editItem.setEnabled(state);
        duplicateItem.setEnabled(state);
        deleteItem.setEnabled(state);
    }

    /**
     * Selects the given file (or no file) for use in the preview.
     * 
     * @param imageFile  The file to select, and to show in the small preview box in the
     *                   ImageLibFrame. If null, then "no image" is selected.
     */
    private void selectImage(File imageFile)
    {
        if (imageFile == null || GreenfootUtil.isImage(imageFile)) {
            imageLabel.setIcon(getPreviewIcon(imageFile));
            selectedImageFile = imageFile;
            if (selectionWatcher != null) {
                selectionWatcher.imageSelected(selectedImageFile);
            }
        }
        else if (imageFile != null) {
            JOptionPane.showMessageDialog(this, imageFile.getName() +
                    " " + Config.getString("imagelib.image.invalid.text"), Config.getString("imagelib.image.invalid.title"), JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Get a preview icon for a class. This is a fixed size image. The
     * user-specified image is normally used; if none exists, the class
     * hierarchy is searched.
     *
     * @param gclass   The class whose icon to get
     * @param defaultIcon  The icon to return if none can be found
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

        if (fname == null) {
            BufferedImage bi = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            return new ImageIcon(bi);
        }

        try {
            BufferedImage bi = ImageIO.read(fname);
            return new ImageIcon(GreenfootUtil.getScaledImage(bi, dpi/2, dpi/2));
        }
        catch (IOException ioe) {
            BufferedImage bi = GraphicsUtilities.createCompatibleTranslucentImage(dpi/2, dpi/2);
            return new ImageIcon(bi);
        }
    }
    
    private static Icon getHalfinchScaledImage(BufferedImage bi)
    {
        int dpi = Toolkit.getDefaultToolkit().getScreenResolution();
        return new ImageIcon(GreenfootUtil.getScaledImage(bi, dpi/2, dpi/2));
    }

    /**
     * Fix the maximum height of the component equal to its preferred size, and
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
     * Get the selected image file
     */
    public File getSelectedImageFile()
    {
        return selectedImageFile;
    }

    /**
     * Get the result from the dialog: OK or CANCEL
     */
    public int getResult()
    {
        return result;
    }

    /**
     * Notification that a file changed on disk.
     */
    public void imageFileRefreshed(File f, BufferedImage image)
    {
        if (f.equals(selectedImageFile)) {
            imageLabel.setIcon(getHalfinchScaledImage(image));
        }
    }
    
    /**
     * Get the name of the class as entered in the dialog.
     */
    public String getClassName()
    {
        return classNameField.getText();
    }

    /**
     * Get the action for the "ok" button.
     */
    private AbstractAction getOkAction()
    {
        return new AbstractAction(Config.getString("okay")) {
            public void actionPerformed(ActionEvent e)
            {
                result = OK;
                setVisible(false);
                dispose();
            }
        };
    }

    /**
     * If we have been editing an image externally, we select that image.
     */
    public void windowActivated(WindowEvent e)
    {
    }

    public void windowClosed(WindowEvent e)
    {
        refreshTimer.stop();
    }

    public void windowClosing(WindowEvent e)
    {
        refreshTimer.stop();
    }

    public void windowDeactivated(WindowEvent e) 
    {
    }

    public void windowDeiconified(WindowEvent e) 
    {
    }

    public void windowIconified(WindowEvent e) 
    {
    }

    public void windowOpened(WindowEvent e) 
    {
    }

    /**
     * Create a new file which is an exact copy of the
     * parameter image and select it if successful in creating
     * it.
     * @param entry Cannot be null, nor can its imageFile.
     */
    protected void duplicateSelected(ImageListEntry entry)
    {
        File srcFile = entry.imageFile;
        File dstFile = null;
        File dir = srcFile.getParentFile();
        String fileName = srcFile.getName();
        int index = fileName.indexOf('.');
        
        String baseName = null;
        String ext = null;
        if (index != -1) {
            baseName = fileName.substring(0, index);
            ext = fileName.substring(index + 1);
        } 
        else {
            baseName = fileName;
            ext = "";
        }
        baseName += COPY_SUFFIX;
        
        try {
            dstFile = GreenfootUtil.createNumberedFile(dir, baseName, ext);
            FileUtility.copyFile(srcFile, dstFile);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        if (dstFile != null) {
            projImageList.select(dstFile);
        }
    }
    
    /**
     * Confirms whether or not to delete the selected file.
     * @param entry Cannot be null, nor can its imageFile.
     */
    private void confirmDelete(ImageListEntry entry)
    {
        String text = Config.getString("imagelib.delete.confirm.text") + 
                      " " + entry.imageFile.getName() + "?";
        int optionResult = JOptionPane.showConfirmDialog(this, text,
              Config.getString("imagelib.delete.confirm.title"), JOptionPane.YES_NO_OPTION);
        if (optionResult == JOptionPane.YES_OPTION) {
            entry.imageFile.delete();
            projImageList.refresh();
        }
    }
}
