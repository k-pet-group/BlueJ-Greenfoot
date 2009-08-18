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
package greenfoot.gui;


import bluej.BlueJTheme;
import bluej.Config;
import bluej.utility.DialogManager;
import bluej.utility.EscapeDialog;

import greenfoot.util.GreenfootUtil;
import greenfoot.core.GProject;
import greenfoot.util.ExternalAppLauncher;

import java.io.File;
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
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * A dialog for managing the images in a project.
 *
 * @author Michael Berry
 * @author Poul Henriksen
 * @version 03/07/09
 */
public class ImageEditFrame extends EscapeDialog implements ListSelectionListener, WindowListener,
        KeyListener
{

    private File projImagesDir;
    private File projDir;
    private ImageLibList projImageList;
    private JButton removeFromProjectButton;
    private JButton editButton;
    private JButton dupButton;
    
    /** List of buttons that should be enabled when something is selected in the list */
    private List<JButton> listEditButtons = new LinkedList<JButton>();
    private GProject proj;

    /**
     * Create a new ImageEditFrame.
     * @param proj the project this frame should be assigned to
     * @param owner the greenfoot main frame
     */
    public ImageEditFrame(GProject proj, JFrame owner)
    {
        super(owner, Config.getString("imageedit.title"), true);
        setLocation(50,50);
        this.proj = proj;
        projImagesDir = proj.getImageDir();
        try {
            projDir = proj.getDir();
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
        buildUI();
        this.addKeyListener(this);
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

            projImagesDir = new File(projDir, "images");
            projImageList = new ImageLibList(projImagesDir, true);
            projImageList.addListSelectionListener(this);
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
            

            editButton = new JButton("Edit");
            editButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    ExternalAppLauncher.editImage( projImageList.getSelectedValue().imageFile);
                }
            });
            panel.add(editButton);
            listEditButtons.add(editButton);
            
            
            dupButton = new JButton("Duplicate");
            dupButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    //ExternalAppLauncher.editImage(projImageList.getSelectedEntry().imageFile);
                }
            });
            listEditButtons.add(dupButton);
            panel.add(dupButton);
            
            removeFromProjectButton = new JButton("Delete");
            removeFromProjectButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        confirmDelete(projImageList.getSelectedValue().imageFile);
                    }
            });
            listEditButtons.add(removeFromProjectButton);
            panel.add(removeFromProjectButton);
            
            JButton newButton = new JButton("Create new...");
            newButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    NewImageDialog newImage = new NewImageDialog(ImageEditFrame.this, projImagesDir, proj);
                    //TODO modal?
                }
            });
            newButton.setEnabled(true);
            panel.add(newButton);
            

            JButton importButton = new JButton("Import...");
            importButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    //ExternalAppLauncher.editImage(projImageList.getSelectedEntry().imageFile);
                }
            });
            importButton.setEnabled(true);
            panel.add(importButton);
            
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
     * @param file the file to delete
     */
    private void confirmDelete(File file)
    {
        int result = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete " + file.getName() + "?",
                "Confirm delete", JOptionPane.YES_NO_OPTION);
        if(result==JOptionPane.YES_OPTION) {
            if(file.delete()) {
                projImageList.refresh();
            }
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
            removeFromProjectButton.setEnabled(false);
            editButton.setEnabled(false);
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
       /* if(newlyCreatedImage != null) {
            refresh();
            selectImage(newlyCreatedImage);
            projImageList.setSelectedFile(newlyCreatedImage);
            newlyCreatedImage = null;
        }*/
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
