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


import bluej.BlueJTheme;
import bluej.Config;
import bluej.utility.DialogManager;
import bluej.utility.EscapeDialog;
import bluej.utility.FileUtility;

import greenfoot.util.GreenfootUtil;
import greenfoot.actions.BrowseImagesAction;
import greenfoot.core.GProject;
import greenfoot.gui.images.ImageLibList.ImageListEntry;
import greenfoot.util.ExternalAppLauncher;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * A dialog for managing the images in a project. Opened through the menu item
 * Edit->Scenario Images...
 * 
 * @author Michael Berry
 * @author Poul Henriksen
 * @version 03/07/09
 */
public class ImageEditFrame extends EscapeDialog implements ListSelectionListener, WindowListener,
        KeyListener
{
    /** Suffix used when creating a copy of an existing image (duplicate) */
    private static final String COPY_SUFFIX = "Copy";
    private File projImagesDir;
    private ImageLibList projImageList;
    private JButton removeFromProjectButton;
    private JButton editButton;
    private JButton dupButton;
    
    /** List of buttons that should be enabled when something is selected in the list */
    private List<JButton> listEditButtons = new LinkedList<JButton>();
    private GProject proj;
    private File newlyCreatedImage;
    protected ImageListEntry[] editedEntries;

    /**
     * Create a new ImageEditFrame.
     * @param proj the project this frame should be assigned to
     * @param owner the greenfoot main frame
     */
    public ImageEditFrame(GProject proj, JFrame owner)
    {
        super(owner, Config.getString("imagelib.scenario.frame.title"), true);
        setLocation(50,50);
        projImagesDir = proj.getImageDir();
        
        buildUI();
        this.addKeyListener(this);
        this.addWindowListener(this);
        this.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        this.setResizable(false);
    }

    /**
     * Build the visual elements of the frame.
     */
    private void buildUI()
    {
        int spacingLarge = BlueJTheme.componentSpacingLarge;
        int spacingSmall = BlueJTheme.componentSpacingSmall;

        JPanel framePanel = new JPanel();
        framePanel.setBorder(BlueJTheme.dialogBorder);
        framePanel.setLayout(new BoxLayout(framePanel, BoxLayout.Y_AXIS));
        this.setContentPane(framePanel);

        JPanel mainPanel = new JPanel();
        BoxLayout mainLayout = new BoxLayout(mainPanel, BoxLayout.X_AXIS);
        mainPanel.setLayout(mainLayout);

        // Project images panel
        {
            Box piPanel = new Box(BoxLayout.Y_AXIS);

            JLabel piLabel = new JLabel(Config.getString("imagelib.projectImages"));
            piLabel.setAlignmentX(0.0f);
            piPanel.add(piLabel);

            JScrollPane imageScrollPane = new JScrollPane();
            
            projImagesDir = proj.getImageDir();
            projImageList = new ImageLibList(projImagesDir, true);
            projImageList.addListSelectionListener(this);
            projImageList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            imageScrollPane.getViewport().setView(projImageList);

            imageScrollPane.setBorder(Config.normalBorder);
            imageScrollPane.setViewportBorder(BorderFactory.createLineBorder(projImageList.getBackground(), 4));
            imageScrollPane.setAlignmentX(0.0f);

            piPanel.add(imageScrollPane);
            
            piPanel.add(GreenfootUtil.createSpacer(GreenfootUtil.Y_AXIS, spacingSmall));          
            mainPanel.add(piPanel);
        }

        mainPanel.add(GreenfootUtil.createSpacer(GreenfootUtil.X_AXIS, spacingLarge));

        //buttons
        {
            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            
            editButton = new JButton(Config.getString("imagelib.edit.button"));
            editButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    editedEntries = projImageList.getSelectedValues();
                    for (ImageListEntry entry : editedEntries) {
                        ExternalAppLauncher.editImage(entry.imageFile);
                    }
                }
            });
            panel.add(editButton);
            listEditButtons.add(editButton);
            
            
            dupButton = new JButton(Config.getString("imagelib.duplicate.button"));
            dupButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    duplicateSelected();
                }
            });
            listEditButtons.add(dupButton);
            panel.add(dupButton);
            
            removeFromProjectButton = new JButton(Config.getString("imagelib.delete.button"));
            removeFromProjectButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        confirmDelete(projImageList.getSelectedValues());
                    }
            });
            listEditButtons.add(removeFromProjectButton);
            panel.add(removeFromProjectButton);
            
            JButton newButton = new JButton(Config.getString("imagelib.create.button"));
            newButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    NewImageDialog newImage = new NewImageDialog(ImageEditFrame.this, projImagesDir);
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
            newButton.setEnabled(true);
            panel.add(newButton);            

            JButton importButton = new JButton(Config.getString("imagelib.import.button"));
            importButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    GreenfootImageLibFrame f = new GreenfootImageLibFrame(ImageEditFrame.this);
                    File srcFile = f.showModal();
                    
                    if(srcFile != null) {
                        File dstFile = new File(projImagesDir, srcFile.getName());
                        try {
                            FileUtility.copyFile(srcFile, dstFile);
                            projImageList.select(dstFile);
                        }
                        catch (IOException e1) {
                            e1.printStackTrace();
                        }
                    }
                }
            });
            importButton.setEnabled(true);
            panel.add(importButton);
            
            JButton browseButton = new JButton(new BrowseImagesAction(Config.getString("imagelib.browse.button"), this,
                    projImagesDir, projImageList)); 
            browseButton.setEnabled(true);
            panel.add(browseButton);
            
            setButtonsEnabled(false);
            
            mainPanel.add(panel);
        }
        
        mainPanel.add(GreenfootUtil.createSpacer(GreenfootUtil.X_AXIS, spacingLarge));

        this.add(mainPanel);
        this.add(GreenfootUtil.createSpacer(GreenfootUtil.Y_AXIS, spacingLarge));
        JButton closeButton = BlueJTheme.getCloseButton();
        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });
        this.add(closeButton);
        
        setButtonsEnabled(false);
        
        pack();
        DialogManager.centreDialog(this);
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

    /**
     * Enables or disables all the buttons in the listEditButtons set.
     * 
     */
    private void setButtonsEnabled(boolean b)
    {
        for (JButton button : listEditButtons) {
            button.setEnabled(b);
        }
    }
    
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

    /**
     * Hide or show the frame. On showing the frame, refresh the contents of
     * the two lists.
     * @param val true if the frame should be made visible, false otherwise
     */
    @Override public void setVisible(boolean val)
    {
        if(val) {
            projImageList.refresh();
        }
        super.setVisible(val);
    }

    /*
     * A new image was selected in one of the ImageLibLists.
     */
    public void valueChanged(ListSelectionEvent lse)
    {

        if (projImageList.getSelectedValue() == null) {
            setButtonsEnabled(false);
        } else {
            setButtonsEnabled(true);
        }

    }

    /**
     * When F5 is pressed, refresh the contents of the lists.
     */
    public void keyTyped(KeyEvent e)
    {
        if(e.getKeyCode()==KeyEvent.VK_F5) {
            projImageList.refresh();
        }
    }

    //These are just here to satisfy the interfaces used
    public void keyPressed(KeyEvent e)
    {
    }

    public void keyReleased(KeyEvent e)
    {
    }
    

    /**
     * If we have been editing an image externally, we select that image.
     */
    public void windowActivated(WindowEvent e)
    {
        if(newlyCreatedImage != null) {
            projImageList.select(newlyCreatedImage);
            newlyCreatedImage = null;
        } 
        
        if(editedEntries != null) {
            editedEntries = null;
            projImageList.refresh();
        }
    }

    public void windowClosed(WindowEvent e)
    {
    }

    public void windowClosing(WindowEvent e)
    {
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

}
