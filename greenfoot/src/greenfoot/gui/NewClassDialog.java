/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2013,2014,2015  Poul Henriksen and Michael Kolling 
 
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

import greenfoot.core.GPackage;
import greenfoot.event.ValidityEvent;
import greenfoot.event.ValidityListener;
import greenfoot.util.GreenfootUtil;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import bluej.BlueJTheme;
import bluej.Config;
import bluej.extensions.SourceType;
import bluej.utility.EscapeDialog;

/**
 * Dialog that asks for the name of a new class. This is only used for non Actor
 * classes.
 * 
 * @author Poul Henriksen
 */
public class NewClassDialog extends EscapeDialog
{
    private JTextField classNameTextField;
    private JComboBox<SourceType> languageSelectionBox;
    private boolean okPressed = false;
    private boolean useInterface;

    /**
     * Creates new dialog for creating a new class.
     * 
     * @param parent The parent frame
     * @param pkg The package the class belongs to.
     */
    public NewClassDialog(JFrame parent, GPackage pkg)
    {
        super(parent, Config.getString("newclass.dialog.title"), true);

        JPanel mainPanel = new JPanel();
        setContentPane(mainPanel);
        
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BlueJTheme.generalBorder);

        // help labels
        JLabel helpLabel1 = GreenfootUtil.createHelpLabel();
        JLabel helpLabel2 = GreenfootUtil.createHelpLabel();
        helpLabel1.setText(Config.getString("newclass.dialog.help1"));
        helpLabel2.setText(Config.getString("newclass.dialog.help2"));
        helpLabel1.setAlignmentX(0.0f);
        helpLabel2.setAlignmentX(0.0f);
        mainPanel.add(helpLabel1);
        mainPanel.add(helpLabel2);
        mainPanel.add(Box.createVerticalStrut(BlueJTheme.generalSpacingWidth));
        mainPanel.add(GreenfootUtil.createSpacer(GreenfootUtil.Y_AXIS, 2 * BlueJTheme.generalSpacingWidth));
        
        JLabel label = new JLabel(Config.getString("newclass.dialog.className"));
        label.setAlignmentX(0.0f);
        mainPanel.add(label);
                
        mainPanel.add(Box.createVerticalStrut(4));

        classNameTextField = new JTextField();
        classNameTextField.setAlignmentX(0.0f);
        Dimension classNameMax = classNameTextField.getMaximumSize();
        classNameMax.height = classNameTextField.getPreferredSize().height;
        classNameTextField.setMaximumSize(classNameMax);
        mainPanel.add(classNameTextField);
        
        mainPanel.add(GreenfootUtil.createSpacer(GreenfootUtil.Y_AXIS, BlueJTheme.generalSpacingWidth));

        final JLabel errorMsgLabel = new JLabel();
        errorMsgLabel.setAlignmentX(0.0f);
        errorMsgLabel.setVisible(false);
        errorMsgLabel.setForeground(Color.RED);
        mainPanel.add(errorMsgLabel);

        mainPanel.add(GreenfootUtil.createSpacer(GreenfootUtil.Y_AXIS, BlueJTheme.generalSpacingWidth));
        
        JPanel slectionPanel = new JPanel();
        slectionPanel.setLayout(new BoxLayout(slectionPanel, BoxLayout.X_AXIS));
        slectionPanel.setAlignmentX(0.0f);
        
        JLabel selectionLabel = new JLabel(Config.getString("newclass.dialog.selectionLabel"));
        selectionLabel.setAlignmentX(0.0f);
        slectionPanel.add(selectionLabel);
        
        slectionPanel.add(Box.createHorizontalStrut(3 * BlueJTheme.generalSpacingWidth));
        
        SourceType[] items = { SourceType.Stride, SourceType.Java };
        languageSelectionBox = new JComboBox<>(items);
        setSelectedLanguage(pkg.getDefaultSourceType());
        slectionPanel.add(languageSelectionBox);

        mainPanel.add(slectionPanel);
        
        // create the ok/cancel button panel
        JPanel buttonPanel = new JPanel();

        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));

        // push buttons over to the right using a glue component
        buttonPanel.add(Box.createHorizontalGlue());

        final JButton okButton = BlueJTheme.getOkButton();
        okButton.addActionListener(evt -> ok());
        okButton.setEnabled(false);

        JButton cancelButton = BlueJTheme.getCancelButton();
        cancelButton.setVerifyInputWhenFocusTarget(false);
        cancelButton.addActionListener(evt -> cancel());
        
        if (Config.isMacOS()) {
            buttonPanel.add(cancelButton);
            buttonPanel.add(Box.createHorizontalStrut(BlueJTheme.generalSpacingWidth));
            buttonPanel.add(okButton);
        }
        else {
            buttonPanel.add(okButton);
            buttonPanel.add(Box.createHorizontalStrut(BlueJTheme.generalSpacingWidth));
            buttonPanel.add(cancelButton);
        }

        getRootPane().setDefaultButton(okButton);
        buttonPanel.setAlignmentX(0.0f);

        // Limit the growth of the button panel
        Dimension buttonPanelMax = buttonPanel.getMaximumSize();
        buttonPanelMax.height = buttonPanel.getPreferredSize().height;
        buttonPanel.setMaximumSize(buttonPanelMax);
        
        ClassNameVerifier classNameVerifier = new ClassNameVerifier(classNameTextField, pkg);
        classNameVerifier.addValidityListener(new ValidityListener(){
            @Override
            public void changedToInvalid(ValidityEvent e)
            {
                errorMsgLabel.setText(e.getReason());
                errorMsgLabel.setVisible(true);
                okButton.setEnabled(false);
            }

            @Override
            public void changedToValid(ValidityEvent e)
            {
                errorMsgLabel.setVisible(false);
                okButton.setEnabled(true);
            }});
        
        mainPanel.add(Box.createVerticalStrut(BlueJTheme.generalSpacingWidth));
        mainPanel.add(GreenfootUtil.createSpacer(GreenfootUtil.Y_AXIS,  2 * BlueJTheme.generalSpacingWidth));
        mainPanel.add(Box.createVerticalGlue());
        mainPanel.add(buttonPanel);
        pack();

        this.setLocationRelativeTo(parent);
    }

    private void ok()
    {
        okPressed = true;
        dispose();
    }

    private void cancel()
    {
        okPressed = false;
        dispose();
    }

    public String getClassName()
    {
        return classNameTextField.getText();
    }
    
    public void setSuggestedClassName(String suggestedClassName)
    {
        classNameTextField.setText(suggestedClassName);
    }
    
    /**
     * Get the selected language of the class.
     */
    public SourceType getSelectedLanguage()
    {
        return (SourceType) languageSelectionBox.getSelectedItem();
    }
    
    /**
     * select the language of the class.
     */
    public void setSelectedLanguage(SourceType type)
    {
        languageSelectionBox.setSelectedItem(type);
    }
    
    /**
     * disable the language selection box.
     */
    public void disableLanguageSelectionBox()
    {
        languageSelectionBox.setEnabled(false);
    }

    public boolean okPressed()
    {
        return okPressed;
    }

    public boolean getInterface()
    {
        return useInterface;
    }
}