/*
This file is part of the BlueJ program. 
Copyright (C) 1999-2011  Michael Kolling and John Rosenberg 

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
package bluej.editor.moe;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import bluej.BlueJTheme;
import bluej.Config;
import bluej.prefmgr.PrefMgr;
import bluej.utility.DBox;
import bluej.utility.DBoxLayout;

/**
 * ReplacePanel display and functionality for replace
 * 
 * @author Marion Zalk
 * @author  Michael Kölling
 */
public class ReplacePanel extends JPanel implements ActionListener, DocumentListener
{

    private MoeEditor editor;
    private FindPanel finder;
    private Font font;
    private JTextField replaceText;
    private String replaceString = "";
    private JButton replaceButton;
    private JButton replaceAllButton;
    private final static String REPLACE_BUTTON_NAME = "replaceBtn";
    private final static String REPLACE_ALL_BUTTON_NAME = "replaceAllBtn";
    private final static String REPLACE_TEXTFIELD = "replaceTextField";

    public ReplacePanel(MoeEditor ed, FindPanel finder)
    {
        super(new BorderLayout());
        this.finder = finder;
        font = PrefMgr.getStandardFont();
        addReplaceBody();
        editor = ed;
    }

    protected JTextField getReplaceText()
    {
        return replaceText;
    }

    public void actionPerformed(ActionEvent e)
    {
        JComponent src = (JComponent) e.getSource();
        setReplaceString(replaceText.getText());
        if (src.getName().equals(REPLACE_BUTTON_NAME)) {
            if (getReplaceString() != null) {
                editor.replace(getReplaceString());
            }
        }
        if (src.getName().equals(REPLACE_ALL_BUTTON_NAME)) {
            if (getReplaceString() != null) {
                editor.replaceAll(getReplaceString());
            }
        }

    }

    /**
     * Populates the replace string and enables the buttons if there 
     * is selected text or the find text field is populated
     */
    public void replaceEvent()
    {
        setReplaceString(replaceText.getText());
        enableButtons();
    }

    /**
     * Determines whether the buttons should be enabled 
     *  or not and enables them accordingly
     */
    public void enableButtons()
    {
        //Only enable the once and all buttons if both find and replace are populated
        //(note: an empty replace string is a valid value)
        //and if there is selected text which is the same as the find string.
        //This eliminates the situation when the caret is moved and a replace in the new position 
        //may not be the intention of the user
        if (editor.getFindSearchString() != null && editor.getFindSearchString().length() != 0
                && editor.getSourcePane().getSelectedText() != null
                && editor.getSourcePane().getSelectedText().equals(editor.getFindSearchString())) {
            enableButtons(true);
        } else {
            enableButtons(false);
        }
    }

    /**
     * Display the replace panel
     */
    private void addReplaceBody()
    {
        JComponent rBody = new DBox(DBox.X_AXIS, 0, BlueJTheme.componentSpacingLarge, 0.5f);
        DBox replaceBody = new DBox(DBoxLayout.X_AXIS, 0, BlueJTheme.commandButtonSpacing, 0.0f);
        DBox optionsBody = new DBox(DBoxLayout.X_AXIS, 0, BlueJTheme.commandButtonSpacing, 0.0f);

        if (!Config.isRaspberryPi()) rBody.setOpaque(false);
        if (!Config.isRaspberryPi()) replaceBody.setOpaque(false);
        if (!Config.isRaspberryPi()) optionsBody.setOpaque(false);

        JLabel replaceLabel = new JLabel(Config.getString("editor.replacePanel.replaceLabel") + " ");
        replaceLabel.setFont(font);
        DBox replaceLabelBox = new DBox(DBox.X_AXIS, 0.5f);
        if (!Config.isRaspberryPi()) replaceLabelBox.setOpaque(false);
        replaceLabelBox.add(Box.createHorizontalGlue());
        replaceLabelBox.add(replaceLabel);
        Dimension d = replaceLabelBox.getPreferredSize();
        d.width = finder.getLabelBoxWidth();
        replaceLabelBox.setPreferredSize(d);
        replaceLabelBox.setMaximumSize(d);

        replaceText = new JTextField(11);
        replaceText.setMaximumSize(replaceText.getPreferredSize());
        replaceText.setFont(font);
        replaceText.setText(getReplaceString());
        replaceText.getDocument().addDocumentListener(this);
        replaceText.setName(REPLACE_TEXTFIELD);

        replaceButton = new JButton();
        replaceButton.setName(REPLACE_BUTTON_NAME);
        replaceButton.setText(Config.getString("editor.replacePanel.replaceOnce"));
        replaceButton.setFont(font);
        replaceButton.addActionListener(this);
        replaceButton.setEnabled(false);

        replaceAllButton = new JButton();
        replaceAllButton.setName(REPLACE_ALL_BUTTON_NAME);
        replaceAllButton.setText(" " + Config.getString("editor.replacePanel.replaceAll") + "  ");
        replaceAllButton.setFont(font);
        replaceAllButton.addActionListener(this);
        replaceAllButton.setEnabled(false);

        if (Config.isMacOS()) {
            replaceButton.putClientProperty("JButton.buttonType", "segmentedTextured");
            replaceButton.putClientProperty("JButton.segmentPosition", "only");
            replaceAllButton.putClientProperty("JButton.buttonType", "segmentedTextured");
            replaceAllButton.putClientProperty("JButton.segmentPosition", "only");
        }

        replaceBody.add(replaceLabelBox);
        replaceBody.add(replaceText);
        optionsBody.add(replaceButton);
        optionsBody.add(replaceAllButton);
        rBody.add(replaceBody);
        rBody.add(optionsBody);
        rBody.setMaximumSize(rBody.getPreferredSize());

        add(rBody, BorderLayout.WEST);
    }

    public void requestReplaceTextFocus()
    {
        replaceText.requestFocus();
        replaceText.setText(getReplaceString());
    }

    protected String getReplaceString()
    {
        return replaceString;
    }

    protected void setReplaceString(String replaceString)
    {
        this.replaceString = replaceString;
    }

    /**
     * enableButtons enable the once and all buttons
     * @param enable
     */
    protected void enableButtons(boolean enable)
    {
        replaceAllButton.setEnabled(enable);
        replaceButton.setEnabled(enable);
    }

    /**
     * 
     */
    protected void requestReplaceFocus()
    {
        replaceText.requestFocus();
    }

    /**
     * Sets the replace textfield to the replaceString passed to the method
     * @param replaceString the new replace string
     */
    protected void populateReplaceField(String replaceString)
    {
        replaceText.setText(replaceString);
    }

    public void changedUpdate(DocumentEvent e)
    {
    }

    /**
     * A document change triggers a replace
     */
    public void insertUpdate(DocumentEvent e)
    {
        replaceEvent();
    }

    /**
     * A document change triggers a replace
     */
    public void removeUpdate(DocumentEvent e)
    {
        replaceEvent();
    }
}
