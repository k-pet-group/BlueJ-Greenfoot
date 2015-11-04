/*
 This file is part of the Greenfoot program.
 Copyright (C) 2005-2009,2010,2014,2015  Poul Henriksen and Michael Kolling

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
import greenfoot.util.GreenfootUtil;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
//import javax.swing.Icon;
//import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import bluej.BlueJTheme;
import bluej.Config;
import bluej.extensions.SourceType;
import bluej.prefmgr.PrefMgr;
import bluej.utility.Debug;
import bluej.utility.EscapeDialog;
import bluej.utility.FileUtility;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;

/**
 * A (modal) dialog for selecting a class image. The image can be selected from either the
 * project image library, or the greenfoot library, or an external location.
 *
 * @author Davin McCall
 */
public class ImageLibFrame extends EscapeDialog implements ListSelectionListener, WindowListener
{
    private GClass gclass;
    private GProject proj;
    /** The default image icon - none, or parent's image */
    private BufferedImage defaultIcon;
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
    private SourceType language;
    
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
        GClass superClass = gclass.getSuperclass();
        defaultIcon = getClassImage(superClass);

        buildUI(owner, proj, false, null);
        projImageList.select(getSpecifiedImage(gclass));
    }

    /**
     * Construct an ImageLibFrame to be used for creating a new class.
     *
     * @param owner        The parent frame
     * @param superClass   The superclass of the new class
     * @param title        The title of the dialog
     * @param defaultName  The default name of the new class (or blank if null)
     * @param description  A helper prompt to display at the top of dialog (or none if null)
     */
    public ImageLibFrame(JFrame owner, GClass superClass, String title, String defaultName, List<String> description)
    {
        super(owner, title, true);
        this.gclass = superClass;
        this.proj = gclass.getPackage().getProject();
        defaultIcon = getClassImage(superClass);
        
        buildUI(owner, proj, true, description);
        projImageList.select(null);
        if (defaultName != null)
        {
            classNameField.setText(defaultName);
        }
        classNameField.requestFocus();
    }

    private void buildUI(JFrame owner, GProject project, final boolean includeClassNameField, List<String> description)
    {
        this.addWindowListener(this);
        JPanel contentPane = new JPanel();
        this.setContentPane(contentPane);
        contentPane.setLayout(new BoxLayout(this.getContentPane(), BoxLayout.Y_AXIS));
        contentPane.setBorder(BlueJTheme.dialogBorder);

        // int spacingSmall = BlueJTheme.componentSpacingSmall;
        int spacingLarge = BlueJTheme.componentSpacingLarge;

        okAction = getOkAction();
        
        if (description != null)
        {
            description.forEach(t -> {
                JLabel l = new JLabel(t);
                l.setFocusable(false);
                l.setAlignmentX(0.0f);
                l.setFont(l.getFont().deriveFont(Font.BOLD));
                l.setBackground(new Color(0, 0, 0, 0));
                l.setBorder(null);
                contentPane.add(l);
            });
            contentPane.add(fixHeight(Box.createVerticalStrut(spacingLarge)));
        }

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
                projImageList = new ImageLibList(projImagesDir, false, this, defaultIcon);
                imageScrollPane.getViewport().setView(projImageList);

                imageScrollPane.setBorder(Config.getNormalBorder());
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
            editItem.addActionListener(e -> {
                ImageListEntry entry = projImageList.getSelectedValue();
                if (entry != null && entry.imageFile != null) {
                    ExternalAppLauncher.editImage(entry.imageFile);
                }
            });
            
            duplicateItem = new JMenuItem(Config.getString("imagelib.duplicate"));
            duplicateItem.setToolTipText(Config.getString("imagelib.duplicate.tooltip")); 
            duplicateItem.setEnabled(false);
            duplicateItem.setFont(PrefMgr.getPopupMenuFont());
            duplicateItem.addActionListener(e -> {
                ImageListEntry entry = projImageList.getSelectedValue();
                if (entry != null && entry.imageFile != null) {
                    duplicateSelected(entry);
                }
            });
            
            deleteItem = new JMenuItem(Config.getString("imagelib.delete"));
            deleteItem.setToolTipText(Config.getString("imagelib.delete.tooltip"));
            deleteItem.setEnabled(false);
            deleteItem.setFont(PrefMgr.getPopupMenuFont());
            deleteItem.addActionListener(e -> {
                ImageListEntry entry = projImageList.getSelectedValue();
                if (entry != null && entry.imageFile != null) {
                    confirmDelete(entry);
                }
            });
            
//            JMenuItem pasteImageItem = new JMenuItem(Config.getString("paste.image"));
//            pasteImageItem.setToolTipText(Config.getString("imagelib.paste.tooltip"));
//            pasteImageItem.setEnabled(true);
//            pasteImageItem.setFont(PrefMgr.getPopupMenuFont());
//            pasteImageItem.addActionListener(e -> {
//                if (PasteImageAction.pasteImage(owner, project))
//                    projImageList.refresh();
//            });

            
            
            JMenuItem newImageItem = new JMenuItem(Config.getString("imagelib.create.button"));
            newImageItem.setToolTipText(Config.getString("imagelib.create.tooltip")); 
            newImageItem.setFont(PrefMgr.getPopupMenuFont());
            newImageItem.addActionListener(e -> {
                String name = includeClassNameField ? getClassName() : gclass.getName();
                NewImageDialog newImage = new NewImageDialog(ImageLibFrame.this, projImagesDir, name);
                final File file = newImage.displayModal();
                if (file != null) {
                    projImageList.refresh();
                    projImageList.select(file);
                    selectImage(file);
                }                                           
            });
            
            JMenuItem importImageItem = new JMenuItem(Config.getString("imagelib.browse.button"));
            importImageItem.setFont(PrefMgr.getPopupMenuFont());
            importImageItem.setAction(new BrowseImagesAction(Config.getString("imagelib.browse.button"), this,
                    projImagesDir, projImageList));
            
            popupMenu.add(fixHeight(editItem));
            popupMenu.add(fixHeight(duplicateItem));
            popupMenu.add(fixHeight(deleteItem));
            popupMenu.add(fixHeight(newImageItem));
            popupMenu.add(fixHeight(importImageItem));
//            popupMenu.add(fixHeight(pasteImageItem));
            
            JButton dropDownButton = new PopupMenuButton(
                    new ImageIcon(ImageLibFrame.class.getClassLoader().getResource(DROPDOWN_ICON_FILE)), 
                    popupMenu);
            
//            JButton dropDownButton = new PopupMenuButton(
//                    Config.getString("imagelib.more"), 
//                    popupMenu);
            
        
           
            borderPanel.setAlignmentX(0.0f);
            borderPanel.add(fixHeight(dropDownButton), BorderLayout.LINE_START);

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
            cancelButton.addActionListener(e -> {
                result = CANCEL;
                setVisible(false);
                dispose();
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
        
        refreshTimer = new Timer(2000, e -> projImageList.refreshPreviews());
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

        {
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
                    @Override
                    public void changedToInvalid(ValidityEvent e)
                    {
                        errorMsgLabel.setText(e.getReason());
                        errorMsgLabel.setVisible(true);
                        okAction.setEnabled(false);
                    }

                    @Override
                    public void changedToValid(ValidityEvent e)
                    {
                        errorMsgLabel.setVisible(false);
                        okAction.setEnabled(true);
                    }
                });

                b.add(Box.createHorizontalStrut(spacingLarge));

                b.add(fixHeight(classNameField));
                
                b.add(Box.createHorizontalStrut(spacingLarge));
                
                SourceType[] items = { SourceType.Stride, SourceType.Java };
                
                JComboBox<SourceType> languageSelectionBox = new JComboBox<SourceType>(items);
                languageSelectionBox.addActionListener(e -> {language = (SourceType) languageSelectionBox.getSelectedItem();});
                languageSelectionBox.setSelectedItem(pkg.getDefaultSourceType());
                b.add(languageSelectionBox);
                
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

        }

        classDetailsPanel.setAlignmentX(0.0f);
        return classDetailsPanel;
    }

    /**
     * A new image was selected in one of the ImageLibLists
     */
    @Override
    public void valueChanged(ListSelectionEvent lse)
    {
        Object source = lse.getSource();
        if (! lse.getValueIsAdjusting() && source instanceof ImageLibList) {
            ImageLibList sourceList = (ImageLibList) source;
            ImageLibList.ImageListEntry ile = sourceList.getSelectedValue();

            // handle the no-image image entry.
            if (ile != null && ile.imageFile != null) {
                File imageFile = ile.imageFile;
                selectImage(imageFile);
                setItemButtons(sourceList == projImageList);
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
            selectedImageFile = imageFile;
            if (selectionWatcher != null) {
                selectionWatcher.imageSelected(selectedImageFile);
            }
        }
        else {
            JOptionPane.showMessageDialog(this, imageFile.getName() +
                    " " + Config.getString("imagelib.image.invalid.text"), Config.getString("imagelib.image.invalid.title"), JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Gets specified image file (which will be project images/ directory) for this specific
     * class, without searching super classes (see getClassImage for that).  Returns null if none
     * specified.
     */
    private static File getSpecifiedImage(GClass gclass)
    {
        String imageName = gclass.getClassProperty("image");
        
        // If an image is specified for this class, and we can read it, return
        if (imageName != null && !imageName.equals(""))
        {
            return new File(new File("images"), imageName).getAbsoluteFile();
        }
        
        return null;
    }

    /**
     * Get a preview icon for a class. This is a fixed size image. The
     * user-specified image is normally used; if none exists, the class
     * hierarchy is searched.
     *
     * @param gclass   The class whose icon to get
     * @param defaultIcon  The icon to return if none can be found
     */
    private static BufferedImage getClassImage(GClass gclass)
    {
        try
        {
            while (gclass != null) {
                File imageFile = getSpecifiedImage(gclass);
                if (imageFile != null && imageFile.canRead())
                {
                    try
                    {
                        return ImageIO.read(imageFile);
                    }
                    catch (IOException e)
                    {
                        Debug.reportError("Can't read image file: " + imageFile.getAbsolutePath(), e);
                    }
                }
                // Otherwise, search up class hierarchy to see if we find an image:
                gclass = gclass.getSuperclass();
            }
    
            return ImageIO.read(new File(GreenfootUtil.getGreenfootLogoPath()));
        }
        catch (IOException e)
        {
            Debug.reportError("Can't read Greenfoot logo image", e);
            return null;
        }
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
     * Get the name of the class as entered in the dialog.
     */
    public String getClassName()
    {
        return classNameField.getText();
    }
    
    /**
     * Get the selected language of the class.
     */
    public SourceType getSelectedLanguage()
    {
        return language;
    }

    /**
     * Get the action for the "ok" button.
     */
    private AbstractAction getOkAction()
    {
        return new AbstractAction(Config.getString("okay")) {
            @Override
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
    @Override
    public void windowActivated(WindowEvent e)
    {
    }

    @Override
    public void windowClosed(WindowEvent e)
    {
        refreshTimer.stop();
    }

    @Override
    public void windowClosing(WindowEvent e)
    {
        refreshTimer.stop();
    }

    @Override
    public void windowDeactivated(WindowEvent e) 
    {
    }

    @Override
    public void windowDeiconified(WindowEvent e) 
    {
    }

    @Override
    public void windowIconified(WindowEvent e) 
    {
    }

    @Override
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
