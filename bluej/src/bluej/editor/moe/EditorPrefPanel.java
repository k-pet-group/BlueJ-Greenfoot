/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kšlling and John Rosenberg 
 
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
// Copyright (c) 2000, 2005, 2007 BlueJ Group, Deakin University
//
// This software is made available under the terms of the "MIT License"
// A copy of this license is included with this source distribution
// in "license.txt" and is also available at:
// http://www.opensource.org/licenses/mit-license.html 
// Any queries should be directed to Michael Kolling mik@bluej.org

package bluej.editor.moe;

import java.awt.FlowLayout;
import java.awt.GridLayout;

import javax.swing.*;

import bluej.BlueJTheme;
import bluej.Config;
import bluej.prefmgr.PrefMgr;
import bluej.prefmgr.PrefPanelListener;

/**
 * A PrefPanel subclass to allow the user to interactively edit
 * editor settings
 *
 * @author  Michael Kolling
 * @version $Id: EditorPrefPanel.java 6163 2009-02-19 18:09:55Z polle $
 */
public class EditorPrefPanel extends JPanel implements PrefPanelListener
{
    private JTextField editorFontField;
    private JCheckBox hilightingBox;
    private JCheckBox autoIndentBox;
    private JCheckBox lineNumbersBox;
    private JCheckBox makeBackupBox;
    private JCheckBox matchBracketsBox;

    /**
     * Setup the UI for the dialog and event handlers for the buttons.
     */
    public EditorPrefPanel()
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BlueJTheme.generalBorder);

//        add(Box.createVerticalGlue());

        JPanel editorPanel = new JPanel(new GridLayout(4,2,0,0));
        {
            String editorTitle = Config.getString("prefmgr.edit.editor.title");
            editorPanel.setBorder(BorderFactory.createCompoundBorder(
                                        BorderFactory.createTitledBorder(editorTitle),
                                        BlueJTheme.generalBorder));
            editorPanel.setAlignmentX(LEFT_ALIGNMENT);

            JPanel fontPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
            {
                fontPanel.add(new JLabel(Config.getString("prefmgr.edit.editorfontsize")));
                editorFontField = new JTextField(4);
                fontPanel.add(editorFontField);
            }
            editorPanel.add(fontPanel);
            editorPanel.add(new JLabel(" "));
            autoIndentBox = new JCheckBox(Config.getString("prefmgr.edit.autoindent"));
            editorPanel.add(autoIndentBox);
            lineNumbersBox = new JCheckBox(Config.getString("prefmgr.edit.displaylinenumbers"));
            editorPanel.add(lineNumbersBox);
            hilightingBox = new JCheckBox(Config.getString("prefmgr.edit.usesyntaxhilighting"));
            editorPanel.add(hilightingBox);
            makeBackupBox = new JCheckBox(Config.getString("prefmgr.edit.makeBackup"));
            editorPanel.add(makeBackupBox);
            matchBracketsBox= new JCheckBox(Config.getString("prefmgr.edit.matchBrackets"));
            editorPanel.add(matchBracketsBox);
        }
        add(editorPanel);

        add(Box.createVerticalStrut(BlueJTheme.generalSpacingWidth));

        add(Box.createVerticalGlue());
        add(Box.createVerticalGlue());
        add(Box.createVerticalGlue());
    }

    public void beginEditing()
    {
        editorFontField.setText(String.valueOf(PrefMgr.getEditorFontSize()));
        hilightingBox.setSelected(PrefMgr.getFlag(PrefMgr.HILIGHTING));
        autoIndentBox.setSelected(PrefMgr.getFlag(PrefMgr.AUTO_INDENT));
        lineNumbersBox.setSelected(PrefMgr.getFlag(PrefMgr.LINENUMBERS));
        makeBackupBox.setSelected(PrefMgr.getFlag(PrefMgr.MAKE_BACKUP));
        matchBracketsBox.setSelected(PrefMgr.getFlag(PrefMgr.MATCH_BRACKETS));
    }

    public void revertEditing()
    {
    }

    public void commitEditing()
    {
        int newFontSize = 0;

        try {
            newFontSize = Integer.parseInt(editorFontField.getText());
            PrefMgr.setEditorFontSize(newFontSize);
        }
        catch (NumberFormatException nfe) { }

        PrefMgr.setFlag(PrefMgr.HILIGHTING, hilightingBox.isSelected());
        PrefMgr.setFlag(PrefMgr.AUTO_INDENT, autoIndentBox.isSelected());
        PrefMgr.setFlag(PrefMgr.LINENUMBERS, lineNumbersBox.isSelected());
        PrefMgr.setFlag(PrefMgr.MAKE_BACKUP, makeBackupBox.isSelected());
        PrefMgr.setFlag(PrefMgr.MATCH_BRACKETS, matchBracketsBox.isSelected());
    }
}
