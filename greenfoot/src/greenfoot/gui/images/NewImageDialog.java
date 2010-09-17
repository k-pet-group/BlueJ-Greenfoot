/*
 This file is part of the Greenfoot program.
 Copyright (C) 2009,2010  Poul Henriksen and Michael Kolling

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
import bluej.utility.Debug;
import bluej.utility.DialogManager;
import bluej.utility.EscapeDialog;
import greenfoot.util.ExternalAppLauncher;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

import java.io.IOException;
import java.io.File;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
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
 */
public class NewImageDialog extends EscapeDialog
{
    private static final int MAX_IMAGE_HEIGHT = 2000;
    private static final int MAX_IMAGE_WIDTH = 2000;
    private static final int DEFAULT_HEIGHT = 100;
    private static final int DEFAULT_WIDTH = 100;
    
    private JTextField name;
    private JSpinner width;
    private JSpinner height;
    private JButton okButton;
    
    private File projImagesDir;
    private JDialog parent;
    
    private File file;
    
    private int imageWidth;
    private int imageHeight;

    /**
     * Create a new image dialog. This is used for specifying the properties for
     * creating a new image, which will then be opened in the image editor.
     * @param parent the parent frame associated with this dialog
     * @param projImagesDir the directory in which the images for the project are placed.
     */
    public NewImageDialog(JDialog parent, File projImagesDir, String rootName)
    {
        super(parent, Config.getString("imagelib.new.image.title"));
        this.parent = parent;
        this.projImagesDir = projImagesDir;

        imageWidth = Config.getPropInteger("greenfoot.image.create.width", DEFAULT_WIDTH);
        imageHeight = Config.getPropInteger("greenfoot.image.create.height", DEFAULT_HEIGHT);
        buildUI(rootName);
    }

    /**
     * Build the user interface for the dialog.
     */
    private void buildUI(String rootName)
    {
        JPanel mainPanel = new JPanel();
        setContentPane(mainPanel);
        //int space = BlueJTheme.dialogBorder;
        //mainPanel.setBorder(BorderFactory.createEmptyBorder(space, space, space, space));
        mainPanel.setBorder(BlueJTheme.dialogBorder);

        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        JPanel detailsPanel = new JPanel(new GridLayout(3, 2, BlueJTheme.componentSpacingSmall,
                BlueJTheme.componentSpacingSmall));
        detailsPanel.add(new JLabel(Config.getString("imagelib.new.image.name") + " "));
        name = new JTextField(10);
        name.setText(rootName);
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
        
        JPanel fileNamePanel = new JPanel();
        fileNamePanel.setLayout(new BoxLayout(fileNamePanel, BoxLayout.X_AXIS));
        fileNamePanel.add(name);
        fileNamePanel.add(new JLabel(".png"));
        
        detailsPanel.add(fileNamePanel);

        //mainPanel.add(fixHeight(Box.createVerticalStrut(BlueJTheme.componentSpacingLarge)));

        detailsPanel.add(new JLabel(Config.getString("imagelib.new.image.width")));
        width = new JSpinner(new SpinnerNumberModel(imageWidth, 1, MAX_IMAGE_WIDTH, 1));
        detailsPanel.add(width);

        //mainPanel.add(fixHeight(Box.createVerticalStrut(BlueJTheme.componentSpacingLarge)));

        detailsPanel.add(new JLabel(Config.getString("imagelib.new.image.height")));
        height = new JSpinner(new SpinnerNumberModel(imageHeight, 1, MAX_IMAGE_HEIGHT, 1));
        detailsPanel.add(height);        

        //mainPanel.add(fixHeight(Box.createVerticalStrut(BlueJTheme.componentSpacingLarge)));

        detailsPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, detailsPanel.getPreferredSize().height));
        
        mainPanel.add(detailsPanel);
        mainPanel.add(Box.createVerticalGlue());
        mainPanel.add(fixHeight(Box.createVerticalStrut(BlueJTheme.componentSpacingLarge)));
        

        Box buttonBox = new Box(BoxLayout.X_AXIS);
        buttonBox.add(Box.createHorizontalGlue());
        
        okButton = BlueJTheme.getOkButton();
        okButton.setEnabled(false);
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                createAndEdit();
            }            
        });
        okButton.setAlignmentY(1.0f);        
        
        JButton cancelButton = BlueJTheme.getCancelButton();
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                dispose();                
            }
        });
        cancelButton.setAlignmentY(1.0f);
        
        if (Config.isMacOS()) {
            buttonBox.add(cancelButton);
            buttonBox.add(Box.createHorizontalStrut(10));
            buttonBox.add(okButton);
        } else {
            buttonBox.add(okButton);
            buttonBox.add(Box.createHorizontalStrut(10));
            buttonBox.add(cancelButton);
        }
        
        mainPanel.add(buttonBox);

        setLocation(parent.getX()+parent.getWidth()/2, parent.getY()+parent.getHeight()/2);
        getRootPane().setDefaultButton(okButton);
        pack();
        checkName();
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
        fileName += ".png";
        file = new File(projImagesDir, fileName);

        if (file.exists()) {
            int r = JOptionPane.showOptionDialog(this, Config.getString("imagelib.write.exists.part1") + file
                    + Config.getString("imagelib.write.exists.part2"), Config.getString("imagelib.write.exists.title"),
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null, null, null);

            if (r == JOptionPane.OK_OPTION) {
                writeAndEdit(im);
            } else {
                setVisible(false);
            }
        } else {
            writeAndEdit(im);
        }
    }
    
    private void writeAndEdit(BufferedImage im)
    {
        try {
            if (ImageIO.write(im, "png", file)) {
                ExternalAppLauncher.editImage(file);
                setVisible(false);
            } else {
                JOptionPane.showMessageDialog(this, "png " +
                        Config.getString("imagelib.image.unsupportedformat.text"), Config.getString("imagelib.image.unsupportedformat.title"), JOptionPane.ERROR_MESSAGE);
            }
        }
        catch (IOException ex) {
            Debug.reportError("Error editing new image", ex);
        }
    }
}
