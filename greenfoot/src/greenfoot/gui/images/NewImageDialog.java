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
import greenfoot.util.ExternalAppLauncher;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

import java.io.IOException;
import java.io.File;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

/**
 * A new image dialog, used for specifying the properties of an image before its
 * creation. After it has been created it will automatically be opened in the
 * default image editing program so the user can edit it.
 * 
 * @author Michael Berry (mjrb4)
 * @version 09/08/09
 */
public class NewImageDialog extends EscapeDialog
{
    private static final int DEFAULT_HEIGHT = 100;
    private static final int DEFAULT_WIDTH = 100;
    private static final String DEFAULT_IMAGE_TYPE = "png";
    
    private JTextField name;
    private JSpinner width;
    private JSpinner height;
    private JComboBox type;
    private JButton okButton;
    
    private File projImagesDir;
    private JDialog parent;
    
    private File file;
    
    private int imageWidth;
    private int imageHeight;
    private String imageType;

    /**
     * Create a new image dialog. This is used for specifying the properties for
     * creating a new image, which will then be opened in the image editor.
     * @param parent the parent frame associated with this dialog
     * @param projImagesDir the directory in which the images for the project are placed.
     */
    public NewImageDialog(JDialog parent, File projImagesDir)
    {
        super(parent, Config.getString("imagelib.new.image.title"));
        this.parent = parent;
        this.projImagesDir = projImagesDir;

        imageWidth = Config.getPropInteger("greenfoot.image.create.width", DEFAULT_WIDTH);
        imageHeight = Config.getPropInteger("greenfoot.image.create.height", DEFAULT_HEIGHT);
        imageType = Config.getPropString("greenfoot.image.create.type", DEFAULT_IMAGE_TYPE);
        buildUI();
    }

    /**
     * Build the user interface for the dialog.
     */
    private void buildUI()
    {
        JPanel mainPanel = new JPanel();
        setContentPane(mainPanel);

        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        JPanel namePanel = new JPanel();
        namePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        namePanel.add(new JLabel(Config.getString("imagelib.new.image.name") + " "));
        name = new JTextField(10);
        name.addKeyListener(new KeyListener() {
            @Override public void keyPressed(KeyEvent e) {
                checkName();
            }

            @Override public void keyReleased(KeyEvent e) {
                checkName();
            }

            @Override public void keyTyped(KeyEvent e) {
                checkName();
            }
        });
        namePanel.add(name);
        mainPanel.add(namePanel);

        mainPanel.add(fixHeight(Box.createVerticalStrut(BlueJTheme.componentSpacingLarge)));

        JPanel widthPanel = new JPanel();
        widthPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        widthPanel.setLayout(new BoxLayout(widthPanel, BoxLayout.X_AXIS));
        widthPanel.add(new JLabel(Config.getString("imagelib.new.image.width")));
        width = new JSpinner(new SpinnerNumberModel(imageWidth, 1, 1000, 1));
        widthPanel.add(width);
        mainPanel.add(widthPanel);

        mainPanel.add(fixHeight(Box.createVerticalStrut(BlueJTheme.componentSpacingLarge)));

        JPanel heightPanel = new JPanel();
        heightPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        heightPanel.setLayout(new BoxLayout(heightPanel, BoxLayout.X_AXIS));
        heightPanel.add(new JLabel(Config.getString("imagelib.new.image.height")));
        height = new JSpinner(new SpinnerNumberModel(imageHeight, 1, 1000, 1));
        heightPanel.add(height);
        mainPanel.add(heightPanel);

        mainPanel.add(fixHeight(Box.createVerticalStrut(BlueJTheme.componentSpacingLarge)));
        
        JPanel typePanel = new JPanel();
        typePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        typePanel.setLayout(new BoxLayout(typePanel, BoxLayout.X_AXIS));
        typePanel.add(new JLabel(Config.getString("imagelib.new.image.type")));
        type = new JComboBox(getImageTypes());
        type.setSelectedItem(imageType);
        typePanel.add(type);
        mainPanel.add(typePanel);

        mainPanel.add(fixHeight(Box.createVerticalStrut(BlueJTheme.componentSpacingLarge)));

        okButton = BlueJTheme.getOkButton();
        okButton.setEnabled(false);
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                createAndEdit();
            }            
        });
        mainPanel.add(okButton);

        setLocation(parent.getX()+parent.getWidth()/2, parent.getY()+parent.getHeight()/2);
        getRootPane().setDefaultButton(okButton);
        pack();
    }

    /**
     * Get available writable image types. If both jpg and jpeg is represented
     * jpeg will be filtered out.
     * 
     */
    private String[] getImageTypes()
    {
        String[] suffixes = ImageIO.getWriterFileSuffixes();      
        Set<String> suffixSet= new TreeSet<String>();
        for (String string : suffixes) {
            suffixSet.add(string.toLowerCase());
        }        
        if(suffixSet.contains("jpeg") && suffixSet.contains("jpg")) {
            suffixSet.remove("jpeg");
        }
        return suffixSet.toArray(new String[suffixSet.size()]);
    }
    
    public File displayModal()
    {
        setModal(true);  
        DialogManager.centreDialog(this);
        setVisible(true);
        dispose();
        setModal(false);
        return file;
    }

    public File getFile() 
    {
        return file;
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
     * Check that the name specified in the name field is valid, and enable or
     * disable the ok button accordingly.
     */
    private void checkName()
    {
        if(name.getText().trim().isEmpty()) {
            okButton.setEnabled(false);
        }
        else {
            okButton.setEnabled(true);
        }
    }

    private void createAndEdit()
    {
        BufferedImage im = new BufferedImage((Integer) width.getValue(), (Integer) height.getValue(),
                BufferedImage.TYPE_INT_ARGB);
        String fileName = name.getText();
        if (!fileName.endsWith("." + type.getSelectedItem())) {
            fileName += "." + type.getSelectedItem();
        }
        file = new File(projImagesDir, fileName);

        if (file.exists()) {
            int r = JOptionPane.showOptionDialog(this, Config.getString("imagelib.write.exists.part1") + file
                    + Config.getString("imagelib.write.exists.part2"), Config.getString("imagelib.write.exists.title"),
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null, null, null);

            if (r == JOptionPane.OK_OPTION) {
                try {
                    ImageIO.write(im, type.getSelectedItem().toString(), file);
                    ExternalAppLauncher.editImage(file);
                }
                catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
        setVisible(false);
    }
}
