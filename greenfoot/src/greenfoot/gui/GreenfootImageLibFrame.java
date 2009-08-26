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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import bluej.BlueJTheme;
import bluej.Config;
import bluej.utility.DialogManager;
import bluej.utility.EscapeDialog;

/**
 * Dialog that shows the images in the Greenfoot library of images.
 * 
 * @author Poul Henriksen
 */
public class GreenfootImageLibFrame extends EscapeDialog implements ListSelectionListener
{
    public static final int OK = 0;
    public static final int CANCEL = 1;
    private int result = CANCEL;

    private ImageLibList greenfootImageList;
    private File selected;
    
    public GreenfootImageLibFrame(JDialog owner)
    {
        super(owner, Config.getString("greenfootimagelibframe.title"), true);
        buildUI();
    }
    
    private void buildUI() 
    { 
        JPanel contentPane = new JPanel();
        this.setContentPane(contentPane);
        contentPane.setLayout(new BoxLayout(this.getContentPane(), BoxLayout.Y_AXIS));
        contentPane.setBorder(BlueJTheme.dialogBorder);
       
        int spacingLarge = BlueJTheme.componentSpacingLarge;        
        {
            // Category selection panel
            File imageDir = new File(Config.getGreenfootLibDir(), "imagelib");
            ImageCategorySelector imageCategorySelector = new ImageCategorySelector(imageDir);

            // List of images
            greenfootImageList = new ImageLibList(false);
            greenfootImageList.addListSelectionListener(this);

            imageCategorySelector.setImageLibList(greenfootImageList);
            
            JComponent greenfootLibPanel = new GreenfootImageLibPanel(imageCategorySelector, greenfootImageList);
            contentPane.add(greenfootLibPanel);
        }
        
        // Ok and cancel buttons
        {
            JPanel okCancelPanel = new JPanel();
            okCancelPanel.setLayout(new BoxLayout(okCancelPanel, BoxLayout.X_AXIS));

            JButton okButton = BlueJTheme.getOkButton();
            okButton.setAction(getOkAction());

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
     * Get the result from the dialog: OK or CANCEL
     */
    private int getResult()
    {
        return result;
    }

    /**
     * Show this dialog as modal.
     * 
     * @return The selected file or null if nothing was selected or the dialog
     *         was cancelled.
     */
    public File showModal()
    {
        setModal(true);
        setVisible(true);
        if(getResult() == OK) {
            return selected;
        }
        else {
            return null;
        }
    }
    
    /**
     * A new image was selected
     */
    public void valueChanged(ListSelectionEvent lse)
    {
        Object source = lse.getSource();
        if (! lse.getValueIsAdjusting() && source instanceof ImageLibList) {
            ImageLibList sourceList = (ImageLibList) source;
            ImageLibList.ImageListEntry ile = sourceList.getSelectedValue();

            if (ile != null) {
                selected = ile.imageFile;
            }
        }        

        if(lse.getValueIsAdjusting() && source instanceof ImageLibList) {
            selected = null;
        }
    }
}
