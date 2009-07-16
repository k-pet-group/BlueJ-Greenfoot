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

import greenfoot.util.GreenfootUtil;
import greenfoot.core.GProject;
import greenfoot.util.ExternalAppLauncher;

import java.io.File;

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
import javax.swing.JDialog;
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
 * @version 03/07/09
 */
public class ImageEditFrame extends JDialog implements ListSelectionListener, WindowListener,
        KeyListener
{

    private File projImagesDir;
    private File projDir;
    private ImageLibList projImageList;
    private ImageLibList greenfootImageList;
    private JButton addButton;
    private JButton removeFromProjectButton;
    private JButton removeFromLibraryButton;
    private JButton editButton;
    private ImageCategorySelector imageCategorySelector;
    private boolean catNull;

    /**
     * Create a new ImageEditFrame.
     * @param proj the project this frame should be assigned to
     * @param owner the greenfoot main frame
     */
    public ImageEditFrame(GProject proj, JFrame owner)
    {
        super(owner, Config.getString("imageedit.title"), true);
        setLocation(50,50);
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
            projImageList = new ImageLibList(projImagesDir);
            projImageList.addListSelectionListener(this);
            imageScrollPane.getViewport().setView(projImageList);

            imageScrollPane.setBorder(Config.normalBorder);
            imageScrollPane.setViewportBorder(BorderFactory.createLineBorder(projImageList.getBackground(), 4));
            imageScrollPane.setAlignmentX(0.0f);

            piPanel.add(imageScrollPane);

            piPanel.add(GreenfootUtil.createSpacer(GreenfootUtil.Y_AXIS, spacingSmall));

            editButton = new JButton("Edit");
            editButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    ExternalAppLauncher.editFile(projImageList.getSelectedEntry().imageFile);
                }
            });
            editButton.setEnabled(false);
            piPanel.add(editButton);

            piPanel.add(GreenfootUtil.createSpacer(GreenfootUtil.Y_AXIS, spacingSmall));

            removeFromProjectButton = new JButton("Delete from project");
            removeFromProjectButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        confirmDelete(projImageList.getSelectedEntry().imageFile);
                    }
            });
            removeFromProjectButton.setEnabled(false);
            piPanel.add(removeFromProjectButton);
            mainPanel.add(piPanel);
        }

        mainPanel.add(GreenfootUtil.createSpacer(GreenfootUtil.X_AXIS, spacingLarge));

        //Add and remove buttons
        {
            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            addButton = new JButton("Add to project") {
                @Override public boolean isValidateRoot() {
                    return true;
                }
            };
            addButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if(greenfootImageList.getSelectedValues().length==0) {
                        File file = projImageList.getSelectedEntry().imageFile;
                        File newFile = new File(greenfootImageList.getDirectory(), file.getName());
                        GreenfootUtil.copyFile(file, newFile);
                        greenfootImageList.refresh();
                    }
                    else {
                        File file = greenfootImageList.getSelectedEntry().imageFile;
                        File newFile = new File(projImagesDir, file.getName());
                        GreenfootUtil.copyFile(file, newFile);
                        projImageList.refresh();
                    }
                }
            });
            addButton.setEnabled(false);
            panel.add(addButton);

            panel.add(GreenfootUtil.createSpacer(GreenfootUtil.Y_AXIS, spacingSmall));
            mainPanel.add(panel);
        }
        
        mainPanel.add(GreenfootUtil.createSpacer(GreenfootUtil.X_AXIS, spacingLarge));

        // Category selection panel
        {
            Box piPanel = new Box(BoxLayout.Y_AXIS);

            JLabel piLabel = new JLabel(Config.getString("imagelib.categories"));
            piLabel.setAlignmentX(0.0f);
            piPanel.add(piLabel);

            File imageDir = Config.getGreenfootLibDir();
            imageDir = new File(imageDir, "imagelib");
            imageCategorySelector = new ImageCategorySelector(imageDir);
            imageCategorySelector.addListSelectionListener(this);

            JScrollPane jsp = new JScrollPane(imageCategorySelector);

            jsp.setBorder(Config.normalBorder);
            jsp.setViewportBorder(BorderFactory.createLineBorder(imageCategorySelector.getBackground(), 4));
            jsp.setAlignmentX(0.0f);

            piPanel.add(jsp);
            mainPanel.add(piPanel);
        }

        mainPanel.add(GreenfootUtil.createSpacer(GreenfootUtil.X_AXIS, spacingSmall));

        // Greenfoot images panel
        {
            Box piPanel = new Box(BoxLayout.Y_AXIS);

            JLabel piLabel = new JLabel(Config.getString("imagelib.images"));
            piLabel.setAlignmentX(0.0f);
            piPanel.add(piLabel);

            JScrollPane jsp = new JScrollPane();

            greenfootImageList = new ImageLibList();
            greenfootImageList.addListSelectionListener(this);
            jsp.getViewport().setView(greenfootImageList);

            jsp.setBorder(Config.normalBorder);
            jsp.setViewportBorder(BorderFactory.createLineBorder(greenfootImageList.getBackground(), 4));
            jsp.setAlignmentX(0.0f);

            piPanel.add(jsp);

            piPanel.add(GreenfootUtil.createSpacer(GreenfootUtil.Y_AXIS, spacingSmall));

            removeFromLibraryButton = new JButton("Delete from library");
            removeFromLibraryButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    confirmDelete(greenfootImageList.getSelectedEntry().imageFile);
                }
            });
            removeFromLibraryButton.setEnabled(false);
            piPanel.add(removeFromLibraryButton);

            mainPanel.add(piPanel);

            imageCategorySelector.setImageLibList(greenfootImageList);
        }

        this.add(mainPanel);
        this.add(GreenfootUtil.createSpacer(GreenfootUtil.Y_AXIS, spacingLarge));
        JButton okButton = BlueJTheme.getOkButton();
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });
        this.add(okButton);
        pack();
    }

    /**
     * Confirms whether to delete a file or not.
     * @param file the file to delete
     */
    private void confirmDelete(File file)
    {
        int result = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete " + file.getName() + "?",
                "Really delete?", JOptionPane.YES_NO_OPTION);
        if(result==JOptionPane.YES_OPTION) {
            file.delete();
            greenfootImageList.refresh();
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
            greenfootImageList.refresh();
            projImageList.refresh();
        }
        super.setVisible(val);
    }

    /*
     * A new image was selected in one of the ImageLibLists.
     */
    public void valueChanged(ListSelectionEvent lse)
    {
        if(lse.getSource()==imageCategorySelector) {
            catNull = true;
        }

        if(greenfootImageList.getSelectedEntry()==null) {
            removeFromLibraryButton.setEnabled(false);
        }
        else {
            if(catNull) addButton.setEnabled(true);
            addButton.setText("Add to project");
            removeFromLibraryButton.setEnabled(true);
        }

        if(projImageList.getSelectedEntry()==null) {
            removeFromProjectButton.setEnabled(false);
            editButton.setEnabled(false);
        }
        else {
            if(catNull) addButton.setEnabled(true);
            addButton.setText("Add to library");
            removeFromProjectButton.setEnabled(true);
            editButton.setEnabled(true);
        }

        if(projImageList.getSelectedEntry()==null && greenfootImageList.getSelectedEntry()==null) {
            addButton.setEnabled(false);
            removeFromProjectButton.setEnabled(false);
            editButton.setEnabled(false);
            removeFromLibraryButton.setEnabled(false);
        }

        if(lse.getValueIsAdjusting() && lse.getSource()!=imageCategorySelector) {
            if(lse.getSource()==projImageList) {
                greenfootImageList.clearSelection();
            }
            else {
                projImageList.clearSelection();
            }
        }
    }

    /**
     * When F5 is pressed, refresh the contents of the lists.
     */
    public void keyTyped(KeyEvent e)
    {
        if(e.getKeyCode()==KeyEvent.VK_F5) {
            greenfootImageList.refresh();
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
    public void windowActivated(WindowEvent e)
    {
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
