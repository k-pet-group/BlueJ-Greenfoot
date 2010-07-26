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
package greenfoot.gui.images;

import greenfoot.GreenfootImage;
import greenfoot.World;
import greenfoot.actions.BrowseImagesAction;
import greenfoot.core.GClass;
import greenfoot.core.GPackage;
import greenfoot.core.GProject;
import greenfoot.core.WorldHandler;
import greenfoot.event.ValidityEvent;
import greenfoot.event.ValidityListener;
import greenfoot.gui.ClassNameVerifier;
import greenfoot.gui.classbrowser.ClassView;
import greenfoot.gui.images.ImageLibList.ImageListEntry;
import greenfoot.util.ExternalAppLauncher;
import greenfoot.util.GraphicsUtilities;
import greenfoot.util.GreenfootUtil;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Timer;
import java.util.TimerTask;

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
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import bluej.BlueJTheme;
import bluej.Config;
import bluej.extensions.ProjectNotOpenException;
import bluej.utility.Debug;
import bluej.utility.DialogManager;
import bluej.utility.EscapeDialog;
import bluej.utility.FileUtility;

/**
 * A dialog for selecting a class image. The image can be selected from either the
 * project image library, or the greenfoot library, or an external location.
 *
 * @author Davin McCall
 * @version $Id: ImageLibFrame.java 7924 2010-07-26 12:57:15Z nccb $
 */
public class ImageLibFrame extends EscapeDialog implements ListSelectionListener, WindowListener, MouseListener
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
    private GreenfootImage originalImage;
    private File newlyCreatedImage;

    private TimerTask refreshTask;

    /**
     * Construct an ImageLibFrame for changing the image of an existing class.
     *
     * @param owner      The parent frame
     * @param classView  The ClassView of the existing class
     */
    public ImageLibFrame(JFrame owner, ClassView classView)
    {
        super(owner, Config.getString("imagelib.title") + " " + classView.getClassName(), true);

        this.gclass = classView.getGClass();
        try {
            this.proj = gclass.getPackage().getProject();
        }
        catch (ProjectNotOpenException ex) {
            ex.printStackTrace();
        }
        catch (RemoteException ex) {
            ex.printStackTrace();
        }
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
        try {
            this.proj = gclass.getPackage().getProject();
        }
        catch (ProjectNotOpenException ex) {
            ex.printStackTrace();
        }
        catch (RemoteException ex) {
            ex.printStackTrace();
        }
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

        int spacingLarge = BlueJTheme.componentSpacingLarge;
        int spacingSmall = BlueJTheme.componentSpacingSmall;

        okAction = getOkAction();

        // Class details - name, current icon
        try {
            contentPane.add(buildClassDetailsPanel(includeClassNameField, project.getDefaultPackage()));
        }
        catch (ProjectNotOpenException e1) {
            e1.printStackTrace();
        }
        catch (RemoteException e1) {
            e1.printStackTrace();
        }

        // Image selection panels - project and greenfoot image library
        {
            //JPanel imageSelPanels = new JPanel();
            //imageSelPanels.setLayout(new GridLayout(1, 2, BlueJTheme.componentSpacingSmall, 0));
            Box imageSelPanels = new Box(BoxLayout.X_AXIS);

            // Project images panel
            {
                Box piPanel = new Box(BoxLayout.Y_AXIS);

                JLabel piLabel = new JLabel(Config.getString("imagelib.projectImages"));
                piLabel.setAlignmentX(0.0f);
                piPanel.add(piLabel);

                imageScrollPane = new JScrollPane();

                try {
                    File projDir = project.getDir();
                    projImagesDir = new File(projDir, "images");
                    projImageList = new ImageLibList(projImagesDir, false);
                    projImageList.addMouseListener(this);
                    imageScrollPane.getViewport().setView(projImageList);
                }
                catch (ProjectNotOpenException pnoe) {}
                catch (RemoteException re) { re.printStackTrace(); }

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
            greenfootImageList = new ImageLibList(false);
            
            JComponent greenfootLibPanel = new GreenfootImageLibPanel(imageCategorySelector, greenfootImageList);
            
            imageSelPanels.add(greenfootLibPanel);
            
            
            imageSelPanels.setAlignmentX(0.0f);
            contentPane.add(imageSelPanels);

            projImageList.addListSelectionListener(this);
            greenfootImageList.addListSelectionListener(this);
            imageCategorySelector.setImageLibList(greenfootImageList);
        }

        // Browse and new panel. Select image file from arbitrary location.
        {
            final JPanel flowPanel = new JPanel();
            FlowLayout layout = new FlowLayout();
            layout.setAlignment(FlowLayout.LEFT);
            flowPanel.setLayout(layout);

            JButton newButton = new JButton(Config.getString("imagelib.create.button"));
            newButton.setToolTipText(Config.getString("imagelib.create.tooltip")); 
            
            newButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e)
                {
                    String name = null;
                    if(includeClassNameField) {
                        name = getClassName();
                    }
                    else {
                        name = gclass.getName();
                    }
                    
                    NewImageDialog newImage = new NewImageDialog(ImageLibFrame.this, projImagesDir, name);
                    final File file = newImage.displayModal();
                    if(file != null) {
                        projImageList.refresh();
                        SwingUtilities.invokeLater(new Runnable(){
                            @Override
                            public void run()
                            {
                                newlyCreatedImage = file;
                            }});
                    }                                           
                }                
            });

            JButton browseButton = new JButton(new BrowseImagesAction(Config.getString("imagelib.browse.button"), this,
                    projImagesDir, projImageList));           
           
            contentPane.add(fixHeight(Box.createVerticalStrut(spacingLarge)));
            flowPanel.setAlignmentX(0.0f);
            flowPanel.add(fixHeight(newButton));
            flowPanel.add(GreenfootUtil.createSpacer(GreenfootUtil.X_AXIS, spacingSmall));
            flowPanel.add(fixHeight(browseButton));

            contentPane.add(fixHeight(flowPanel));

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
                    selectedImageFile = null;
                    restoreOriginalImage();
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
        
        refreshTask = new TimerTask() {
            public void run()
            {
                Debug.message("Refreshing previews");
                projImageList.refreshPreviews();
            }
        };
        new Timer().schedule(refreshTask, 2000, 2000);

        pack();
        DialogManager.centreDialog(this);
        setVisible(true);
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
                classNameVerifier.addValidityListener(new ValidityListener(){
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
                    }});


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
      * Copy an image to the project's images directory.
      * @file the image file to copy
      */
    private File copyToProject(File file) throws IOException
    {
        File imageDir = null;
        try {
            imageDir = gclass.getPackage().getProject().getImageDir();
        }
        catch (ProjectNotOpenException e) {
            e.printStackTrace();
        }
        if(file.getParentFile().equals(imageDir)) return file;
        File newFile = new File(imageDir, file.getName());
        GreenfootUtil.copyFile(file, newFile);
        return newFile;
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

            if (ile != null) {
                File imageFile = ile.imageFile;
                selectImage(imageFile);
            }
        }

        if(lse.getValueIsAdjusting() && source instanceof ImageLibList) {
            if(lse.getSource()==projImageList) {
                greenfootImageList.clearSelection();
            }
            else {
                projImageList.clearSelection();
            }
        }
    }

    /**
     * Selects the given file for use in the preview.
     */
    private void selectImage(File imageFile)
    {
        if(GreenfootUtil.isImage(imageFile)) {
            imageLabel.setIcon(getPreviewIcon(imageFile));
            selectedImageFile = imageFile;
            if(gclass != null && gclass.isWorldSubclass()) {
                World world = WorldHandler.getInstance().getWorld();
                if(world != null) {
                    if(originalImage == null) {
                        originalImage = world.getBackground();
                    }
                    world.setBackground(new GreenfootImage(imageFile.toString()));
                    WorldHandler.getInstance().repaint();
                }
            }
        }
        else if (imageFile != null) {
            JOptionPane.showMessageDialog(this, imageFile.getName() +
                    " " + Config.getString("imagelib.image.invalid.text"), Config.getString("imagelib.image.invalid.title"), JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Restores the original image on the world.
     */
    private void restoreOriginalImage()
    {
        if (originalImage != null) {
            if (gclass != null && gclass.isWorldSubclass()) {
                World world = WorldHandler.getInstance().getWorld();
                if (world != null) {
                    world.setBackground(originalImage);
                    WorldHandler.getInstance().repaint();
                }
            }
        }
        originalImage = null;
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

        if(fname==null) {
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

    /**
     * Get the name of the class as entered in the dialog.
     */
    public String getClassName()
    {
        return classNameField.getText();
    }

    /**
     * Refresh the content of the project image list.
     */
    public void refresh()
    {
        projImageList.refresh();
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
                originalImage = null;
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
        if(newlyCreatedImage != null) {
            projImageList.select(newlyCreatedImage);
            selectImage(newlyCreatedImage);
            newlyCreatedImage = null;
        }
    }

    public void windowClosed(WindowEvent e)
    {
        restoreOriginalImage();
        refreshTask.cancel();
    }

    public void windowClosing(WindowEvent e)
    {
        restoreOriginalImage();
        refreshTask.cancel();
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

    public void mouseClicked(MouseEvent e)
    {       
    }

    public void mouseEntered(MouseEvent e)
    {
    }

    public void mouseExited(MouseEvent e)
    {
    }

    public void mousePressed(MouseEvent e)
    {
        maybeShowPopup(e);
    }

    public void mouseReleased(MouseEvent e)
    {
        maybeShowPopup(e);
    }

    private void maybeShowPopup(MouseEvent e)
    {
        if (e.isPopupTrigger() && e.getComponent() == projImageList) {
            final int row = projImageList.rowAtPoint(e.getPoint());
            
            if (false == projImageList.isRowSelected(row)) {
                projImageList.setSelectedRow(row);
                greenfootImageList.clearSelection();
            }
            
            JPopupMenu menu = new JPopupMenu();
            menu.setInvoker(this);
            menu.setLocation(e.getXOnScreen(), e.getYOnScreen());
            
            menu.add(Config.getString("imagelib.edit")).addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e)
                {
                    ImageListEntry[] editedEntries = projImageList.getSelectedValues();
                    for (ImageListEntry entry : editedEntries) {
                        ExternalAppLauncher.editImage(entry.imageFile);
                    }
                }
            });
            
            menu.add(Config.getString("imagelib.duplicate")).addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e)
                {
                    duplicateSelected();
                }
            });
            
            menu.add(Config.getString("imagelib.delete")).addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e)
                {
                    confirmDelete(projImageList.getSelectedValues());
                }
            });
            
            menu.setVisible(true);
        }
    }
    
    protected void duplicateSelected()
    {
        ImageListEntry[] srcEntries = projImageList.getSelectedValues();
        for (ImageListEntry srcEntry : srcEntries) {
            File srcFile = srcEntry.imageFile;
            File dstFile = null;
            if(srcFile != null) {
                File dir = srcFile.getParentFile();
                String fileName = srcFile.getName();
                int index = fileName.indexOf('.');
                
                String baseName = null;
                String ext = null;
                if(index != -1) {
                    baseName = fileName.substring(0, index);
                    ext = fileName.substring(index + 1);
                } else {
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
            }
            if(dstFile != null) {
                projImageList.select(dstFile);
            }
        }
       
    }
    
    /** Suffix used when creating a copy of an existing image (duplicate) */
    private static final String COPY_SUFFIX = "Copy"; //TODO move to labels
    
    /**
     * Confirms whether to delete a file or not.
     * @param imageListEntries the file to delete
     */
    private void confirmDelete(ImageListEntry[] imageListEntries)
    {
        String text = Config.getString("imagelib.delete.confirm.text") + " ";
        
        int count = 0;
        for (ImageListEntry imageListEntry : imageListEntries) {
            count++;
            if(count < imageListEntries.length) {
                text += imageListEntry.imageFile.getName() + ", ";
            }
            else {
                text += imageListEntry.imageFile.getName();
            }
        }
        text += "?";
            
        int result = JOptionPane.showConfirmDialog(this,text,
                Config.getString("imagelib.delete.confirm.title"), JOptionPane.YES_NO_OPTION);
        
        if(result==JOptionPane.YES_OPTION) {
            for (ImageListEntry imageListEntry : imageListEntries) {
                imageListEntry.imageFile.delete();
            }
            projImageList.refresh();
        }
    }
}
